// 	
//  @(#)Move.java	4/2002
// 	
//  Copyright 2002-2004 Zachary DelProposto. All rights reserved.
//  Use is subject to license terms.
// 	
//  This program is free software; you can redistribute it and/or modify
//  it under the terms of the GNU General Public License as published by
//  the Free Software Foundation; either version 2 of the License, or
//  (at your option) any later version.
//	
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU General Public License for more details.
//
//  You should have received a copy of the GNU General Public License
//  along with this program; if not, write to the Free Software
//  Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
//  Or from http://www.gnu.org/package dip.order.result;
//
package dip.order;

import dip.order.result.OrderResult.ResultType;
import dip.order.result.DependentMoveFailedResult;
import dip.order.result.ConvoyPathResult;

import dip.world.*;

import dip.process.Adjudicator;
import dip.process.OrderState;
import dip.process.Tristate;

import dip.misc.Log;
import dip.misc.Utils;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Collections;


/**
*
*	Implementation of the Move order.
*	<p>
*	This has been updated to support the 2003-Dec-16 DATC, specifically, 
*	section 4.A.3.
*
*/
public class Move extends Order
{
	// il8n constants
	/*
	private static final String MOVE_VAL_CONVOY_WARNING = "MOVE_VAL_CONVOY_WARNING";
	private static final String MOVE_VAL_ARMY_CONVOY = "MOVE_VAL_ARMY_CONVOY";
	*/
	private static final String MOVE_VAL_SRC_EQ_DEST = "MOVE_VAL_SRC_EQ_DEST";
	private static final String MOVE_VAL_UNIT_ADJ = "MOVE_VAL_UNIT_ADJ";
	private static final String MOVE_VAL_ADJ_UNLESS_CONVOY = "MOVE_VAL_ADJ_UNLESS_CONVOY";
	private static final String MOVE_VAL_BAD_ROUTE_SRCDEST = "MOVE_VAL_BAD_ROUTE_SRCDEST";
	private static final String MOVE_VAL_BAD_ROUTE = "MOVE_VAL_BAD_ROUTE";
	private static final String MOVE_VER_NO_ROUTE = "MOVE_VER_NO_ROUTE";
	private static final String MOVE_VER_CONVOY_INTENT = "MOVE_VER_CONVOY_INTENT";
	private static final String MOVE_EVAL_BAD_ROUTE = "MOVE_EVAL_BAD_ROUTE";
	private static final String MOVE_FAILED = "MOVE_FAILED";
	private static final String MOVE_FAILED_NO_SELF_DISLODGE = "MOVE_FAILED_NO_SELF_DISLODGE";
	private static final String MOVE_FORMAT = "MOVE_FORMAT";
	private static final String MOVE_FORMAT_EXPLICIT_CONVOY = "MOVE_FORMAT_EXPLICIT_CONVOY";
	private static final String CONVOY_PATH_MUST_BE_EXPLICIT = "CONVOY_PATH_MUST_BE_EXPLICIT";
	private static final String CONVOY_PATH_MUST_BE_IMPLICIT = "CONVOY_PATH_MUST_BE_IMPLICIT";
	
	
	// constants: names
	private static final String orderNameBrief 	= "M";
	private static final String orderNameFull 	= "Move";
	private static final transient String orderFormatString = Utils.getLocalString(MOVE_FORMAT);
	private static final transient String orderFormatExCon = Utils.getLocalString(MOVE_FORMAT_EXPLICIT_CONVOY);	// explicit convoy format
	
	// instance variables
	protected Location dest = null;
	protected ArrayList convoyRoutes = null;	// contains *defined* convoy routes; null if none. 
	protected boolean _isViaConvoy = false;					// 'true' if army was explicitly ordered to convoy.
	protected boolean _isConvoyIntent = false;				// 'true' if we determine that intent is to convoy. MUST be set to same initial value as _isViaConvoy
	protected boolean _isAdjWithPossibleConvoy = false;		// 'true' if an army with an adjacent move has a possible convoy route move too
	protected boolean _fmtIsAdjWithConvoy = false;			// for OrderFormat ONLY. 'true' if explicit convoy AND has land route.
	protected boolean _hasLandRoute = false;					// 'true' if move has an overland route.
	
	/** Creates a Move order */
	protected Move()
	{
		super();
	}// Move()
	
	/** Creates a Move order */
	protected Move(Power power, Location src, Unit.Type srcUnitType, Location dest)
	{
		this(power, src, srcUnitType, dest, false);
	}// Move()

	/** Creates a Move order, with optional convoy preference. */
	protected Move(Power power, Location src, Unit.Type srcUnitType, Location dest, boolean isConvoying)
	{
		super(power, src, srcUnitType);
		
		if(dest == null)
		{	
			throw new IllegalArgumentException("null argument(s)");
		}
		
		this.dest = dest;
		this._isViaConvoy = isConvoying;
		this._isConvoyIntent = this._isViaConvoy;		// intent: same initial value as _isViaConvoy
	}// Move()
	
	/** 
	*	Creates a Move order with an explicit convoy route. 
	*	The convoyRoute array must have a length of 3 or more, and not be null.
	*/
	protected Move(Power power, Location src, Unit.Type srcUnitType, Location dest, Province[] convoyRoute)
	{
		this(power, src, srcUnitType, dest, true);
		
		if(convoyRoute == null || convoyRoute.length < 3)
		{
			throw new IllegalArgumentException("bad or missing route");
		}
		
		convoyRoutes = new ArrayList(1);
		convoyRoutes.add(convoyRoute);
	}// Move()
	
	
	/**
	*	Creates a Move order with multiple explicit convoy routes.
	*	Each entry in routes must be a single-dimensional Province array.
	*/
	protected Move(Power power, Location src, Unit.Type srcUnitType, Location dest, List routes)
	{
		this(power, src, srcUnitType, dest, true);
		
		if(routes == null)
		{
			throw new IllegalArgumentException("null routes");
		}
		
		// TODO: we don't check the routes very strictly.
		convoyRoutes = new ArrayList(routes);
	}// Move()
	
	
	
	/** Returns the destination Location of this Move */
	public Location getDest() 					{ return dest; }
	
	
	/** 
	*	Returns true if this Move was explicitly ordered to be by convoy, 
	*	either by specifying "by convoy" or "via convoy" after the move 
	*	order, or, by giving an explicit convoy path.
	*	<p>
	*	Note that this is <b>not</b> always true for all convoyed moves;
	*	to check if a move is convoyed, see isConvoying().
	*	<p>
	*	Note that explicitly ordering a convoy doesn't really matter
	*	unless there are <b>both</b> a land route and a convoy route. See
	*	Dec-16-2003 DATC 6.G.8.
	*/
	public boolean isViaConvoy() 				
	{ 
		return _isViaConvoy;
	}// isExplicitConvoy()
	
	
	/**
	*	Returns true if an Army can possibly Move to its destination with a convoy,
	*	even though it is adjacent to its destination. This is only really important
	*	when a Move to an adjacent province could occur by land or by convoy.
	*	<p>
	*	<b>Important Note:</b> This value will not be properly determined
	*	until <code>validate()</code> has been called.
	*/
	public boolean isAdjWithPossibleConvoy()
	{
		return _isAdjWithPossibleConvoy;
	}// isAdjWithPossibleConvoy()
	
	
	/**
	*	Returns true if the Intent of this Move order is to Convoy.
	*	This is true when:
	*	<ul>
	*		<li>isViaConvoy() is false, Source and Dest are not adjacent, 
	*			both coastal, and there is
	*			a theoretical convoy path between them. <b>Note:</b> this can
	*			only be determined after <code>validate()</code> has been
	*			called.</li>
	*		<li>hasDualRoute() is true, isViaConvoy() is false, and there is a
	*			matching convoy path between source and dest with at least one
	*			Convoying Fleet of the same Power as this Move (thus signalling
	*			"intent to Convoy"). <b>Note:</b> this can only be determined 
	*			after <code>verify()</code> has been called.</li>
	*		<li>isViaConvoy is true, and hasDualRoute() are true. This also can
	*			only be determined after <code>verify()</code> has been called.</li>
	*	</ul>
	*	<b>Note:</b> if this method (or isConvoying()) is to be used during
	*	the verify() stage by other orders, they <b>absolutely</b> must check that
	*	the Move has already been verified, since move verification can change
	*	the value of this method.
	*/
	public boolean isConvoyIntent()
	{
		return _isConvoyIntent;
	}// isConvoyIntent()
	
	
	/**
	*	This is implemented for compatibility; it is no different than
	*	<code>isConvoyIntent()</code>.
	*/
	public boolean isConvoying()
	{
		return isConvoyIntent();
	}// isConvoying()
	
	
	
	/** 
	*	Returns, if set, an explicit convoy route (or the first explicit
	*	route if there are multiple routes). Returns null if not convoying
	*	or no explicit route was defined.
	*/
	public Province[] getConvoyRoute()
	{
		return (convoyRoutes != null) ? (Province[]) convoyRoutes.get(0) : null;
	}// getConvoyRoute()
	
	/** 
	*	Returns, if set, all explicit convoy routes as an unmodifiable List.
	*	Returns null if not convoying or no explicit route(s) were defined.
	*/
	public List getConvoyRoutes()
	{
		return (convoyRoutes != null) ? Collections.unmodifiableList(convoyRoutes) : null;
	}// getConvoyRoute()
	
	
	public String getFullName()
	{
		return orderNameFull;
	}// getFullName()
	
	public String getBriefName()
	{
		return orderNameBrief;
	}// getBriefName()
	
	
	// order formatting
	public String getDefaultFormat()
	{
		return (convoyRoutes == null) ? orderFormatString : orderFormatExCon;
	}// getDefaultFormat()
	
	
	public String toBriefString()
	{
		StringBuffer sb = new StringBuffer(64);
		
		
		if(convoyRoutes != null)
		{
			// print all explicit routes
			sb.append(power);
			sb.append(": ");
			sb.append(srcUnitType.getShortName());
			sb.append(' ');
			final int size = convoyRoutes.size();
			for(int i=0; i<size; i++)
			{
				final Province[] path = (Province[]) convoyRoutes.get(i);
				formatConvoyRoute(sb, path, true, true);
				
				// prepare for next path
				if(i < (size - 1))
				{
					sb.append(", ");
				}
			}
		}
		else
		{
			super.appendBrief(sb);
			sb.append('-');
			dest.appendBrief(sb);
			
			if(isViaConvoy())
			{
				sb.append(" by convoy");
			}
		}
		
		return sb.toString();
	}// toBriefString()
	
	
	public String toFullString()
	{
		StringBuffer sb = new StringBuffer(128);
		
		if(convoyRoutes != null)
		{
			// print all explicit routes
			sb.append(power);
			sb.append(": ");
			sb.append(srcUnitType.getFullName());
			sb.append(' ');
			final int size = convoyRoutes.size();
			for(int i=0; i<size; i++)
			{
				final Province[] path = (Province[]) convoyRoutes.get(i);
				formatConvoyRoute(sb, path, false, true);
				
				// prepare for next path
				if(i < (size - 1))
				{
					sb.append(", ");
				}
			}
		}
		else
		{
			super.appendFull(sb);
			sb.append(" -> ");
			dest.appendFull(sb);
			
			if(isViaConvoy())
			{
				sb.append(" by convoy");
			}
		}
		
		return sb.toString();
	}// toFullString()	
	
	
	
	
	
	public boolean equals(Object obj)
	{
		if(obj instanceof Move)
		{
			Move move = (Move) obj;
			if(	super.equals(move)
				&& this.dest.equals(move.dest) 
				&& this.isViaConvoy() == move.isViaConvoy() )
			{
				return true;
			}
		}
		return false;
	}// equals()	
	
	
	public void validate(TurnState state, ValidationOptions valOpts, RuleOptions ruleOpts)
	throws OrderException
	{
		// NOTE: the first time we validate(), _isViaConvoy == _isConvoyIntent.
		// if we re-validate, that assertion may not be true.
		
		// basic checks
		//
		checkSeasonMovement(state, orderNameFull);
		checkPower(power, state, true);
		super.validate(state, valOpts, ruleOpts);
		
		// first, validate the unit type and destination, if we are 
		// using strict validation.
		//
		if(valOpts.getOption(ValidationOptions.KEY_GLOBAL_PARSING).equals(ValidationOptions.VALUE_GLOBAL_PARSING_STRICT))
		{
			final Position position = state.getPosition();
			
			// a.1
			if(src.isProvinceEqual(dest))
			{
				throw new OrderException(Utils.getLocalString(MOVE_VAL_SRC_EQ_DEST));
			}
			
			// validate Borders
			Border border = src.getProvince().getTransit(src, srcUnitType, state.getPhase(), this.getClass());
			if(border != null)
			{
				throw new OrderException( Utils.getLocalString(ORD_VAL_BORDER, src.getProvince(), border.getDescription()) );
			}
			
			// a.2
			dest = dest.getValidatedWithMove(srcUnitType, src);
			
			// check that we can transit into destination (check borders)
			border = dest.getProvince().getTransit(src, srcUnitType, state.getPhase(), this.getClass());
			if(border != null)
			{
				throw new OrderException( Utils.getLocalString(ORD_VAL_BORDER, src.getProvince(), border.getDescription()) );
			}
			
			// Determine convoying intent for nonadjacent moves that are not explicitly 
			// convoyed (e.g., isViaConvoy() == false). All nonadjacent fleet moves
			// fail. All nonadjacent army moves without theoretical convoy paths
			// also fail. _isConvoyIntent is set if there is a theoretical convoy route
			// and we are not explicitly ordered to convoy.
			//
			if(!src.isAdjacent(dest))
			{
				// nonadjacent moves with Fleets/Wings always fail (cannot convoy fleets)
				if(srcUnitType != Unit.Type.ARMY)
				{
					throw new OrderException(Utils.getLocalString(MOVE_VAL_UNIT_ADJ, srcUnitType.getFullNameWithArticle()));
				}
				
				// determine if explicit/implicit convoys are required
				final RuleOptions.OptionValue convoyRule = ruleOpts.getOptionValue(RuleOptions.OPTION_CONVOYED_MOVES);
				if(convoyRule == RuleOptions.VALUE_PATHS_EXPLICIT && convoyRoutes == null)
				{
					// no explicit route defined, and at least one should be
					throw new OrderException(Utils.getLocalString(CONVOY_PATH_MUST_BE_EXPLICIT));
				}
				else if(convoyRule == RuleOptions.VALUE_PATHS_IMPLICIT && convoyRoutes != null)
				{
					// explicit route IS defined, and shouldn't be
					throw new OrderException(Utils.getLocalString(CONVOY_PATH_MUST_BE_IMPLICIT));
				}
				
				// nonadjacent moves must have a theoretical convoy path! 
				// (this throws an exception if there is no theoretical convoy path)
				validateTheoreticalConvoyRoute(position);
				
				// we didn't fail; thus, we intend to convoy (because it is at least possible).
				if(!isViaConvoy())
				{
					_isConvoyIntent = true;
				}
			}
			else
			{
				// we are adjacent
				//
				// _isAdjWithPossibleConvoy is true iff we are both adjacent 
				// (after all validation/borders checked) and an army, and 
				// there is a theoretical convoy path from src->dest. Also,
				// this CANNOT be true if we are EXPLICITLY being convoyed
				// (isViaConvoy() == true); in that case, the convoy is preferred
				// and will be used despite the land route.
				//
				if( !isViaConvoy() 
					&& srcUnitType == Unit.Type.ARMY )
				{
					Path path = new Path(position);
					_isAdjWithPossibleConvoy = path.isPossibleConvoyRoute(src, dest);
				}
				
				// for order format:
				// set _fmtIsAdjWithConvoy iff we are EXPLICITLY ordered to convoy, 
				// and we are an adjacent move (we are an adjacent move if we
				// reached this point in the code)
				_fmtIsAdjWithConvoy = isViaConvoy(); 
				
				// set if we can move via a land route.
				_hasLandRoute = true;
			}
		}
	}// validate()
	
	
	/**
	*	Determines if this move has a theoretical explicit or implicit
	*	convoy route. Throws an exception if 
	*	<p>
	*	This will only throw an OrderException if there is an Explicit
	*	convoy path that is bad (doesn't contain src and dest in route,
	*	or doesn't form a route from src->dest), or if there is no
	*	implicit theoretical route from src->dest.
	*	<p>
	*	An implicit route is assumed if no explicit route has been set.
	*/
	protected void validateTheoreticalConvoyRoute(Position position)
	throws OrderException
	{
		if(convoyRoutes != null)
		{
			// if we have defined routes, check all of them to make sure 
			// they are (all) theoretically valid
			for(int routeIdx=0; routeIdx<convoyRoutes.size(); routeIdx++)
			{
				final Province[] route = (Province[]) convoyRoutes.get(routeIdx);
				
				// check that src, dest are included in path
				if( route[0] != src.getProvince() 
					|| route[route.length-1] != dest.getProvince() )
				{
					throw new OrderException(Utils.getLocalString(MOVE_VAL_BAD_ROUTE_SRCDEST,
						formatConvoyRoute(route, true, false)));
				}
				
				// check route validity
				if(!Path.isRouteValid(position, src, dest, route))
				{
					throw new OrderException(Utils.getLocalString(MOVE_VAL_BAD_ROUTE,
						formatConvoyRoute(route, true, false)));
				}
			}
		}
		else
		{
			// check that a *possible* convoy path exists
			// (enough fleets to span src-dest)
			Path path = new Path(position);
			if( !path.isPossibleConvoyRoute(src, dest) )
			{
				throw new OrderException(Utils.getLocalString(MOVE_VAL_ADJ_UNLESS_CONVOY));
			}
		}
	}// validateTheoreticalConvoyRoute()
	
	
	/** Format a convoy route into a String */
	protected String formatConvoyRoute(final Province[] route, boolean isBrief, boolean useHyphen)
	{
		StringBuffer sb = new StringBuffer(128);
		formatConvoyRoute(sb, route, isBrief, useHyphen);
		return sb.toString();
	}// formatConvoyRoute()
	
	
	/** Format a convoy route into a StringBuffer */
	protected void formatConvoyRoute(StringBuffer sb, final Province[] route, boolean isBrief, boolean useHyphen)
	{
		if(isBrief)
		{
			sb.append(route[0].getShortName());
		}
		else
		{
			sb.append(route[0].getFullName());
		}
		
		for(int i=1; i<route.length; i++)
		{
			if(useHyphen)
			{
				sb.append('-');
			}
			else
			{
				sb.append(" -> ");
			}
			
			
			if(isBrief)
			{
				sb.append(route[i].getShortName());
			}
			else
			{
				sb.append(route[i].getFullName());
			}
		}
	}// formatConvoyRoute
	
	
	
	/**
	*	Verify this move given completely-known game state.
	*	<p>
	*	Verification must always be performed after strict order validation.
	*	<p>
	*	Verify does the following for Move orders:
	*	<ul>
	*		<li>Moves involving Fleets or Wings always verify successfully.</li>
	*		<li>Moves involving armies without theoretical convoy routes
	*			always verify successfully.</li>
	*		<li>Moves with theoretical convoy routes are evaluated as such:</li>
	*		<ul>
	*			<li>Moves with explicit convoy routes or "convoy-preferred" moves
	*				(isViaConvoy() is true) are evaluated to make sure there is
	*				a Legal convoy route. If there is no convoy route, but a
	*				land route is available, the land route is used (DATC case 
	*				6.G.8)</li>
	*			<li>Adjacent moves that have a possible ("theoretical") convoy route 
	*				(therefore, isAdjWithPossibleConvoy() is true) are evaluated to 
	*				determine intent (2000 rules/DATC 4.A.3); 
	*				an army will move by land unless there is "intent to convoy".
	*				Intent to Convoy is true iff there is both a Legal convoy 
	*				route <b>and</b> one of the Fleets in that route is of the
	*				same Power as the Move order we are evaluating.</li>
	*		</ul>
	*	</ul>
	*	<p>
	*	<b>Legal</b> convoy routes are defined as a possible (or "theoretical")
	*	convoy route (i.e., an unbroken chain of adjacent fleets briding the source
	*	and	destination), that also have Convoy orders that match this Move.
	*
	*/
	public void verify(Adjudicator adjudicator)
	{
		OrderState thisOS = adjudicator.findOrderStateBySrc(getSource());
		if(!thisOS.isVerified())
		{
			// if we have already failed, do not evaluate.
			if( thisOS.getEvalState() == Tristate.UNCERTAIN )
			{
				if(isConvoying())	// intent to convoy already determined (e.g., _isViaConvoy is true, so _isConvoyIntent initiall is true)
				{
					if(convoyRoutes != null) // static (explicit) paths
					{
						// if we have multiple routes, we don't fail until *all* paths fail.
						boolean overall = false;
						for(int routeIdx=0; routeIdx<convoyRoutes.size(); routeIdx++)
						{
							final Province[] route = (Province[]) convoyRoutes.get(routeIdx);
							overall = Path.isRouteLegal(adjudicator, route);
							if(overall)	// if at least one is true, then we are OK
							{
								break;
							}
						}
						
						if(!overall)
						{
							// if we are explicitly being convoyed, and there is a land route,
							// but no convoy route, we use the land route.
							//
							if(isViaConvoy() && _hasLandRoute)
							{
								// we don't fail, but mention that there is no convoy route. (text order result)
								_isConvoyIntent = false;
								adjudicator.addResult(thisOS, Utils.getLocalString(MOVE_VER_NO_ROUTE));
							}
							else
							{
								// all paths failed.
								thisOS.setEvalState(Tristate.FAILURE);
								adjudicator.addResult(thisOS, ResultType.FAILURE, Utils.getLocalString(MOVE_VER_NO_ROUTE));
							}
						}
					}
					else	// implicit path
					{
						Path path = new Path(adjudicator);
						if( !path.isLegalConvoyRoute(getSource(), getDest()) )
						{
							// As for static (explicit) paths, if we are explicitly
							// ordered to convoy ("by convoy") and there is a land route,
							// but no convoy route, we use the land route.
							//
							if(isViaConvoy() && _hasLandRoute)
							{
								_isConvoyIntent = false;
								adjudicator.addResult(thisOS, Utils.getLocalString(MOVE_VER_NO_ROUTE));
							}
							else
							{
								thisOS.setEvalState(Tristate.FAILURE);
								adjudicator.addResult(thisOS, ResultType.FAILURE, Utils.getLocalString(MOVE_VER_NO_ROUTE));
							}
						}
					}
				}
				else if(isAdjWithPossibleConvoy())	// intent must be determined
				{
					// first, we need to find all paths with possible convoy orders
					// between src and dest. If we have an order, by the same power,
					// on ONE of these paths, then intent to convoy will be 'true'
					// 
					// Note: this could be put in validate(), where _isAdjWithPossibleConvoy
					// is set, for efficiency reasons. However, it is more appropriate and
					// makes more sense here.
					//
					final Province srcProv = getSource().getProvince();
					final Province destProv = getDest().getProvince();
					final Position pos = adjudicator.getTurnState().getPosition();
					Path.FAPEvaluator evaluator = new Path.FleetFAPEvaluator(pos);
					Province[][] paths = Path.findAllSeaPaths(evaluator, srcProv, destProv);
					
					// now, we need to evaluate each path, to see if that province
					// has a fleet of the same power as this order in any legal path.
					// If so, the intent is to convoy.
					for(int i=0; i<paths.length; i++)
					{
						Province p = evalPath(adjudicator, paths[i]);
						if(p != null)
						{
							_isConvoyIntent = true;
							adjudicator.addResult(thisOS, ResultType.TEXT, Utils.getLocalString(MOVE_VER_CONVOY_INTENT, p));
							break;
						}
					}
				}
			}
			
			// we have been verified.
			thisOS.setVerified(true);
		}
	}// verify()
	
	
	/**
	*	Evaluate a Province path (length must be >= 3)
	*	for the presence of a Fleet of the given Power.
	*	<p>
	*	This does NOT check to see if the Fleet was ordered
	*	to convoy, or if that convoy order matches a particular 
	*	Move order.
	*	<p>
	*	Returns the Province with the Fleet of own Power if found;
	*	otherwise returns null..
	*/
	private Province evalPath(Position pos, final Province[] path, Power fleetPower)
	{
		if(path.length >= 3)
		{
			for(int i=1; i<(path.length-1); i++)
			{
				Unit unit = pos.getUnit(path[i]);
				if(unit.getPower().equals(fleetPower))
				{
					return path[i];
				}
			}
		}
		
		return null;
	}// evalPath()
	
	
	/**
	*	Evaluate a Province path (length must be >= 3)
	*	for the presence of a Fleet of the given Power with
	*	appropriate convoy orders. This assumes the given Path
	*	contains provinces with Fleets.
	*	<p>
	*	This <b>does</b> check to see if the Fleet was ordered
	*	to convoy, and that that convoy order matches <b>this</b>
	*	Move order.
	*	<p>
	*	Returns the Province with the Fleet of own Power if found;
	*	otherwise returns null..
	*/
	private Province evalPath(Adjudicator adj, final Province[] path)
	{
		if(path.length >= 3)
		{
			final Position pos = adj.getTurnState().getPosition();
			
			for(int i=1; i<(path.length-1); i++)
			{
				Province prov = path[i];
				Unit unit = pos.getUnit(path[i]);
				if(unit.getPower().equals(this.getPower()))
				{
					final OrderState os = adj.findOrderStateBySrc(prov);
					final Order order = os.getOrder();
					if(order instanceof Convoy)
					{
						final Convoy convoy = (Convoy) order;
						if(	convoy.getConvoySrc().isProvinceEqual(this.getSource()) 
							&& convoy.getConvoyDest().isProvinceEqual(this.getDest()) )
						{
							return prov;
						}
					}
				}
			}
		}
		
		return null;
	}// evalPath()
	
	
	
	
	
	/**
	*	Dependencies for a Move order:
	*	<ol>
	*		<li><b>NOT ADDED:</b>
	*			<ol>
	*			<li>convoy route, if it is a convoyed move<br>
	*				note that while a move would depend upon a convoy route,
	*				individual convoy orders are not helpful because there may be
	*				multiple paths to a destination. A Path object and path iterator
	*				is used to determine convoy-dependency, as required.
	*			
	*			<li>Moves to this space<br>
	*				we are not concerned with moves to this space, unless it is a head-to-head
	*				move, which is taken care of below, by setting OrderState appropriately.
	*				
	*				Move is a special case. Since calculating the 'moves' is the difficult
	*				part, when a move is evaluated, the move looks at it's destination space.
	*			</ol>		
	*		<li><b>ADDED:</b>
	*			<ol>
	*				<li>Supports of this move
	*				<li>Moves to destination space
	*			</ol>
	*	</ol>
	*	<p>		
	*		If destination is a move order, then that move order must be evaluated
	*		for an order to succeed. (taken care of by evaluate)
	*	<p>
	*	We also determine if this is a head-to-head move order; if so,
	*	the head-to-head flag of OrderState is set. A Head-To-Head move is
	*	defined as: 
	*	<pre>
	*	 	A->B and B->A, where neither B nor A is convoyed.
	*	</pre>
	*	Note that head-to-head determination may not be complete until 
	*	verification is complete, as it depends upon whether this and/or an
	*	opposing move is convoyed.
	*/
	public void determineDependencies(Adjudicator adjudicator)
	{
		// add moves to destination space, and supports of this space
		OrderState thisOS = adjudicator.findOrderStateBySrc(getSource());
		
		ArrayList depMTDest = null;
		ArrayList depSup = null;
		ArrayList depSelfSup = null;
		
		OrderState[] orderStates = adjudicator.getOrderStates();
		for(int osIdx=0; osIdx<orderStates.length; osIdx++)
		{
			OrderState dependentOS = orderStates[osIdx];
			Order order = dependentOS.getOrder();
			
			if(order instanceof Move && order != this)
			{
				Move move = (Move) order;
				
				// move to *destination* space (that are not this order)
				if( move.getDest().isProvinceEqual(this.getDest()) )
				{
					if(depMTDest == null) { depMTDest = new ArrayList(5); }
					depMTDest.add(dependentOS);
				}
				
				// check if this is a head-to-head move
				// note that isConvoying() may not yet be properly set, so the
				// "headToHeadness" will have to be re-evaluated sometime AFTER
				// order verification (via verify()) has been performed.
				if( move.getDest().isProvinceEqual(this.getSource())
					&& move.getSource().isProvinceEqual(this.getDest()) 
					&& !this.isConvoying() && !move.isConvoying() )
				{
					Log.println("Head2Head possible between: ", this, ", ", dependentOS.getOrder());
					thisOS.setHeadToHead(dependentOS);
				}
			}
			else if(order instanceof Support)
			{
				Support support = (Support) order;
				if(	support.getSupportedSrc().isProvinceEqual(this.getSource())
					&& support.getSupportedDest().isProvinceEqual(this.getDest()) )
				{
					if( adjudicator.isSelfSupportedMove(dependentOS) )
					{
						if(depSelfSup == null) { depSelfSup = new ArrayList(5); }
						depSelfSup.add(dependentOS);
					}
					else
					{
						if(depSup == null) { depSup = new ArrayList(5); }
						depSup.add(dependentOS);
					}
				}
			}
		}
		
		// set supports / competing moves in OrderState
		if(depMTDest != null)
		{
			thisOS.setDependentMovesToDestination(depMTDest);
		}
		
		if(depSup != null)
		{
			thisOS.setDependentSupports(depSup);
		}
		
		if(depSelfSup != null)
		{
			thisOS.setDependentSelfSupports(depSelfSup);
		}
	}// determineDependencies()	
	
	
	
	
	
	
	/**
		NOTE: this description may be slightly out of date
		<pre>
	
		evaluation of Move orders. The algorithm is as follows:
		======================================================
		1) calculate support of this move (certain & uncertain)
		
		2) if this is a convoyed move, evaluate convoy route
			a) if convoy route fails, move fails
			b) if convoy route uncertain, cannot evaluate move yet
			c) if convoy route ok, move can be evaluated.
		
		3) determine order of unit in destination space
			a) no unit, or unit with Support/Convoy/Hold order
				1) calculate strengths of all other moves to destination (if present)
					this is calculated by the evaluate() method for each Move order, so
					several iterations may be required before strengths are in the 'useful'
					area.
				2) calculate defending unit strength (if present; if not, defense_max == 0)
					this is calculated by the evaluate() method for the respective order, similar
					to 3.a.1
				3) compare attack_certain (of this move) to attack_max of all other attacks,
					and defense_max of destination.
					a) if another Move order to destination succeeded, then FAILURE
					b) if attack_certain > *all* of the defenders' (attack_max && defense_max)
						SUCCESS, unless defender is of the same power, in which case FAILURE 
						if SUCCESS, defender (if present) is dislodged
					c) if attack_max <= *any* of the defenders' attack_certain or defense_certain
						FAILURE (since there would be no way to overcome this difference, 
							 regardless of support!) 
							 this is a "BOUNCE" [note: this is a key point]
					d) otherwise, we remain UNCERTAIN
			b) self support
				in cases with self support, the following changes to 3.a are noted below:
					1) self support is used to determine strength against other moves (standoffs)
						to the destination province. 
					2) self support is NOT used to determine strength against *this* move to the
						destination province. Self-support can never be used to dislodge a unit,
						however, if a unit has enough strength to dislodge, self-support does not
						prohibit dislodgement.	
						
						MODIFICATION (6/2/02): Self support *may* be used to determine the strength
						of this move to the destination province; if the unit in the destination province
						has succesfully moved out, we must compare against all other moves to dest (as in 1) but the
						self support can cause us to prevail against other moves to dest as well. Self-support
						cannot be used in the dislodge calculation, nor can it prohibit dislodgement.
						
							
			b) unit with Move order, NOT head-to-head (see below for definition)
				evaluate as 3.a.1-3 however:
				1)	if we are stronger: (guaranteed success, unless self)
						a) if destination move FAILURE, unit is dislodged, unless self; if self, we fail
						b) if destination move SUCCESS, unit is not dislodged, we succeed (if self or not)
						c) if destination move UNCERTAIN, unit is "maybe" dislodged, unless self;
							if self, we remain uncertain
				2)	if we are not stronger  (equal-strength)
						a) we fail, ONLY if we are 'definately' not stronger (atk.max < def.certain)
						b) if destination move SUCCESS, we succeed
						c) if destination move UNCERTAIN, we remain uncertain.
			c) unit is a head-to-head MOVE order
				definition: 	destination unit is moving to this space, and NEITHER unit is convoyed
								(note: this is set when dependencies are determined)
						 
				1) evaluate as 3.a.1-3, with the following caveats applied to this vs. head-to-head move:
					- we use atk_certain/atk_max of 'head-to-head' unit
					a) same as 3.a.3.a
					b) same as 3.a.3.b [opposing unit dislodged; NOT a 'maybe' dislodged]
						BUT, opposing unit move is marked FAILURE
					c) same as 3.a.3.c
					d) same as 3.a.3.d	
							
			d) comparing against head to head:
				if comparing against a head-to-head battle, where a unit may be dislodged, remain
				uncertain until we know if the unit is dislodged; if unit dislodged by head-to-head
				player, it cannot affect other battles
					A->B
					B->A
					D->B	(and dislodges B)	: no change
					HOWEVER, 
					A->B, B->A, C->A, and A dislodges B (head to head), C can move to A. 
					B does not standoff C because it was dislodged by A in a head-to-head battle.
					
					This is seen in DATC cases 5.A and 7.H (if no "by convoy" is used).
						1) isHeadToHead() && EvalState == UNCERTAIN *or* head-to-head part unevaluated:
							UNCERTAIN result.
						2) isHeadToHead() && EvalState == FAILURE:
								if disloger == head-to-head, then ignore (do nothing)
						3) otherwise, process normally.
					
					 
		
		
		IMPORTANT: 	A move that is 'dislodged' or 'fails' may still offset other moves, and 
					still cut support.
		
		ALSO IMPORTANT: we create dislodged results, instead of failed results, for moves
					that are definately dislodged. The adjudicator will create, later, 
					dislodged results for 'maybe' dislodged orders. 
		</pre>				
	*/
	public void evaluate(Adjudicator adjudicator)
	{
		Log.println("--- evaluate() dip.order.Move ---");
		
		OrderState thisOS = adjudicator.findOrderStateBySrc(getSource());
				
		// 1) calculate support of this Move
		Move order = (Move) thisOS.getOrder();
		int mod = order.getDest().getProvince().getBaseMoveModifier(getSource());
		// If the move isn't by convoy, perhaps there are border issues to deal with.
		if(!isConvoying()){
			thisOS.setAtkMax( thisOS.getSupport(false) );
			thisOS.setAtkCertain( thisOS.getSupport(true) );	
		} else {
			// Bypass the modification to AtkMax, AtkCertain
			// If the mod was positive, subtracting it will take it away.
			// If the mod was negitive, subtracting it will add it back. 
			thisOS.setAtkMax( thisOS.getSupport(false) - mod);
			thisOS.setAtkCertain( thisOS.getSupport(true) - mod);
		}
		thisOS.setAtkSelfSupportMax( thisOS.getSelfSupport(false) );
		thisOS.setAtkSelfSupportCertain( thisOS.getSelfSupport(true) );
		
		
		if(Log.isLogging())
		{
			Log.println("   order: ", this);
			Log.println("   initial evalstate: ",thisOS.getEvalState());
			Log.println("     atk-max: ",thisOS.getAtkMax());
			Log.println("    atk-cert: ",thisOS.getAtkCertain());
			Log.println("     self-atk-max: ",thisOS.getAtkSelfSupportMax());
			Log.println("    self-atk-cert: ",thisOS.getAtkSelfSupportCertain());
			Log.println("  # nonself supports: ",thisOS.getDependentSupports().length);
			Log.println("  #    self supports: ",thisOS.getDependentSelfSupports().length);
			Log.println("  dislodged?: ",thisOS.getDislodgedState());
		}
		
		
		// evaluate 
		if(thisOS.getEvalState() == Tristate.UNCERTAIN)
		{
			// re-evaluate head-to-head status. we may be convoyed, so, 
			// this could be a head-to-head move.
			//
			if(thisOS.isHeadToHead())
			{
				Move h2hMove = (Move) thisOS.getHeadToHead().getOrder();
				if(this.isConvoying() || h2hMove.isConvoying())
				{
					// we need to change h2h status!
					Log.println("     HeadToHead removed (convoy detected)");
					thisOS.setHeadToHead(null);
				}
			}
			
			
			// 2.a-c
			if(isConvoying())
			{
				// NOTE: convoy path result may return 'false' if we are uncertain. 
				Path path = new Path(adjudicator);
				Tristate convoyPathResult = path.getConvoyRouteEvaluation(this, null, null);
				
				Log.println("  isByConvoy() true; convoyPathRouteEval() = ", convoyPathResult);
				
				if(convoyPathResult == Tristate.FAILURE)
				{
					// 2.a
					thisOS.setEvalState(Tristate.FAILURE);
					adjudicator.addResult(thisOS, ResultType.FAILURE, Utils.getLocalString(MOVE_EVAL_BAD_ROUTE));
					return;
				}
				else if(convoyPathResult == Tristate.UNCERTAIN)
				{
					return;	// 2.b (can't evaluate this move yet!)
				}
				else if(!thisOS.hasFoundConvoyPath())
				{
					// else: we just continue (2.c)
					// HOWEVER, we can indicate the path taken as a result of this move, 
					// if we haven't already.
					//
					List validPath = new ArrayList(10);
					path.getConvoyRouteEvaluation(this, null, validPath);
					adjudicator.addResult(new ConvoyPathResult(this, validPath));
					thisOS.setFoundConvoyPath(true);
				}
			}
			
			// setup: 3.a, 3.b, and 3.c are very similar, except for how dislodged units are
			// handled. To use the same basic logic, we must determine some things up front.
			boolean isDestAMove = false;
			boolean isDestEmpty = false;
			OrderState destOS = adjudicator.findOrderStateBySrc(getDest());
			
			if(destOS != null)
			{
				if(destOS.getOrder() instanceof Move)
				{
					isDestAMove = true;
				}
			}
			else
			{
				isDestEmpty = true;
			}
			
			Log.println("   isDestAMove: ", String.valueOf(isDestAMove));
			Log.println("   isDestEmpty: ", String.valueOf(isDestEmpty));
			Log.println("   isHeadToHead: ", String.valueOf(thisOS.isHeadToHead()));
			
			
			// some setup
			final int attack_certain = thisOS.getAtkCertain();
			final int attack_max = thisOS.getAtkMax();
			final int self_attack_certain = thisOS.getAtkSelfSupportCertain();
			final int self_attack_max = thisOS.getAtkSelfSupportMax();
			
			
			// 3.a.3
			//
			// note: this block will complete 3.a.3.a (only applies to other moves to destination)
			// first, compare to 'all other' moves to the destination province
			// this must be done for all cases
			// "dml" = "destination move list"
			boolean isBetterThanAllOtherMoves = true;
			
			OrderState[] dml = thisOS.getDependentMovesToDestination();
			
			Log.println("  # dep dest moves: ", dml.length);
			
			for(int i=0; i<dml.length; i++)
			{
				OrderState os = dml[i];
				
				
				
				if(Log.isLogging())
				{
					Log.println(" checking against dependent move: ",os.getOrder());
					Log.println("       :(dep) atkMax = "+os.getAtkMax()+";  atkCertain = "+os.getAtkCertain());
					Log.println("       :(dep) selfAtkMax = "+os.getAtkSelfSupportMax()+";  selfAtkCertain = "+os.getAtkSelfSupportCertain());
					Log.println("       : isHeadToHead() = "+os.isHeadToHead()+"; evalState() = "+os.getEvalState()+";");
					if(os.getDislodger() != null)
					{
						Log.println("       : dislodger = "+os.getDislodger().getOrder());
					}
					else
					{
						Log.println("       : dislodger = "+os.getDislodger());
					}
				}
				
				if(os.getEvalState() == Tristate.SUCCESS)
				{
					// 3.a.3.a: someone's already better than us.
					Log.println("    -- they're better than us!");
					isBetterThanAllOtherMoves = false;
					thisOS.setEvalState(Tristate.FAILURE);
					adjudicator.addResult(thisOS, ResultType.FAILURE, Utils.getLocalString(MOVE_FAILED));
					return;
				}
				else // other order is UNCERTAIN or FAILURE eval state
				{				
					// 3.d
					if( os.isHeadToHead() && (os.getEvalState() == Tristate.UNCERTAIN || !isDependentHTHResolved(os)) )
					{
						// we can't evaluate yet; remain uncertain (3.d.1)
						Log.println("   -- can't tell if head-to-head battle caused dislodgement!");
						isBetterThanAllOtherMoves = false;
					}
					else if( !os.isHeadToHead() 
							 || (os.isHeadToHead() && os.getDislodger() != os.getHeadToHead()) )	// 3.d.3
					{
						// 3.b.1, 3.b.2 are accounted for within this else block
						//
						if( (attack_max + self_attack_max) <= (os.getAtkCertain() + os.getAtkSelfSupportCertain()) )
						{
							// 3.a.3.c: we can never be better than this pairing. Ever. Fail, unless destination
							// is part of a head-to-head battle which was dislodged by a unit involved in the
							// head-to-head battle. [3.d]
							Log.println("   -- attack_max <= os.getAtkCertain() + getAtkSelfSupportCertain(); must fail!");
							isBetterThanAllOtherMoves = false;
							thisOS.setEvalState(Tristate.FAILURE);
							//adjudicator.addResult(thisOS, ResultType.FAILURE, Utils.getLocalString(MOVE_BOUNCE));
							adjudicator.addBouncedResult(thisOS, os);
							return;
						}
						
						
						if( (attack_certain + self_attack_certain) <= (os.getAtkMax() + os.getAtkSelfSupportMax()) )
						{
							// 3.a.3.b: we are not better than *all* the unevaluated moves to destination
							// this doesn't mean we fail, though, since the other moves strength calculations
							// may not be final
							Log.println("   -- atk_certain <= os.getAtkMax() + getAtkSelfSupportMax(); not conclusively better!");
							isBetterThanAllOtherMoves = false;
						}
					}
					// "else" os.isHeadToHead() && os.getDislodger() == os.getHeadToHead() :: we ignore the unit! (3.d.2)
				}
			}// while()
			
			Log.println("  isBetterThanAllOtherMoves: ", String.valueOf(isBetterThanAllOtherMoves));
			
			
			// Note that if we are not better than all other moves to the destination province, we 
			// cannot be successful, but we can be *unsuccessful* if we are definately (certainly)
			// worse then the defending unit, if any.
			//
			// 3.a.3.c: for defending unit (if present)
			// see if we are "definately worse"
			// we don't check destination NON-head-to-head moves, since they defend at strength==1. And
			// we attack at (minimum) strength==1. Thus we can never be "definately worse", (but we can tie).
			if(!isDestEmpty)
			{
				if(thisOS.isHeadToHead())
				{
					if(attack_max <= thisOS.getHeadToHead().getAtkCertain())
					{
						thisOS.setEvalState(Tristate.FAILURE);
						//adjudicator.addResult(thisOS, ResultType.FAILURE, Utils.getLocalString(MOVE_BOUNCE));
						adjudicator.addBouncedResult(thisOS, thisOS.getHeadToHead());
						Log.println("    (hth) final evalState() = ", thisOS.getEvalState());
						return;
					}
				}
				else if(!isDestAMove)	// less priority than isHeadToHead()
				{
					if(attack_max <= destOS.getDefCertain())
					{
						thisOS.setEvalState(Tristate.FAILURE);
						//adjudicator.addResult(thisOS, ResultType.FAILURE, Utils.getLocalString(MOVE_BOUNCE));
						adjudicator.addBouncedResult(thisOS, destOS);
						Log.println("    (dam) final evalState() = ", thisOS.getEvalState());
						return;
					}
				}
			}
			
			// see if we are better w/o self support.
			// this will influence dislodges
			final boolean isBwoss = isBetterWithoutSelfSupport(thisOS);
			Log.println("       isBetterWithoutSelfSupport() = ", String.valueOf(isBwoss));
			
			// at this point, 3.a.3.a is complete, and 3.a.3.c is complete (for defender & other move orders).
			// however, we must complete 3.a.3.b
			//
			// now compare to the destination province. 
			// there are 4 cases: 1) empty, 2) Move, 3) head-to-head Move, and 4) (support/hold/convoy)
			// each is similar, but case 1 always succeeds (depending on other moves, above), cases
			// 2 & 4 are similar except for dislodge calculations, and case 3 is similar except the
			// 'attack' instead of 'defense' parameter is used, since it itself is a move.
			if(isBetterThanAllOtherMoves)
			{
				if(isDestEmpty)
				{
					// 3.a.3.b: case 1. [empty province: special case of 3.a.3.b]
					Log.println("  isDestEmpty(): prior eval state: ", thisOS.getEvalState());
					thisOS.setEvalState(Tristate.SUCCESS);
				}
				else if(thisOS.isHeadToHead())
				{
					// 3.a.3.b: case 3. [also known as: 3.a.3.c.1.b]
					// CHANGED: 10/2002 to fix a couple of bugs
					Log.println("  isHTH evaluation");
					OrderState hthOS = thisOS.getHeadToHead();
					if( (attack_certain + 0) > (hthOS.getAtkMax() + hthOS.getAtkSelfSupportMax()) )
					{
						if( !isBwoss || isDestSamePower(hthOS) )
						{
							thisOS.setEvalState(Tristate.FAILURE); // we fail--no self dislodgement!
							adjudicator.addResult(thisOS, ResultType.FAILURE, Utils.getLocalString(MOVE_FAILED_NO_SELF_DISLODGE));
						}
						else
						{
							thisOS.setEvalState(Tristate.SUCCESS); 		// we win
							hthOS.setDislodgedState(Tristate.YES);	// they are dislodged
							hthOS.setDislodger(thisOS);
							adjudicator.addDislodgedResult(hthOS);
							
							if(hthOS.getEvalState() == Tristate.UNCERTAIN)
							{
								hthOS.setEvalState(Tristate.FAILURE);	// they lose
								//adjudicator.addResult(hthOS, ResultType.FAILURE, Utils.getLocalString(MOVE_DISLODGED_HTH));
							}
						}
					}
				}
				else if(isDestAMove)
				{
					Log.println("     dest is a Move");
					if(destOS.getEvalState() == Tristate.SUCCESS)
					{
						// regardless of our strength (1 or >1) we will succeed if destination unit moved out.
						// this covers parts of 3.a.3.b/4 and 3.b.2 self support
						thisOS.setEvalState(Tristate.SUCCESS);
					}
					else if(attack_certain == 1)
					{
						// 3.a.3.b: case 4	[typical case of 3.a.3.b]
						// if destination evalstate is uncertain, we too are uncertain
						if(destOS.getEvalState() == Tristate.FAILURE)
						{
							// we only fail for certain iff we are 'definately weaker'
							// which is defined as attack_max <= defense_certain 
							// remember, our "certain support" could increase later
							if(attack_max <= 1)
							{
								thisOS.setEvalState(Tristate.FAILURE);
								//adjudicator.addResult(thisOS, ResultType.FAILURE, Utils.getLocalString(MOVE_FAILED_DEP)); // OLD
								adjudicator.addResult(new DependentMoveFailedResult(thisOS.getOrder(), destOS.getOrder())); 
							}
							// else: we remain uncertain.
						}
						// else: we remain uncertain
					}
					else
					{
						// now we are covering attack_certain > 1, and dest eval state is not a success
						//
						// 3.a.3.b.1: we are stronger; we will succeed, regardless of destination move result.
						// unless, of course, we could be dislodging ourselves. In that case, we cannot 
						// complete the evaluation.
						if( isDestSamePower(destOS) )
						{
							Log.println("      dest is the same power!");
							
							// cannot dislodge self; but we will succeed unless other unit failed; if
							// other unit is uncertain, then we remain uncertain.
							if(destOS.getEvalState() == Tristate.SUCCESS)
							{
								Log.println("           but left the province.");
								thisOS.setEvalState(Tristate.SUCCESS);
							}
							else if(destOS.getEvalState() == Tristate.FAILURE)
							{
								Log.println("           and failed, so we can't self-dislodged!.");
								thisOS.setEvalState(Tristate.FAILURE);
								adjudicator.addResult(thisOS, ResultType.FAILURE, Utils.getLocalString(MOVE_FAILED_NO_SELF_DISLODGE));
							}
						}
						else
						{
							if(isBwoss)
							{
								thisOS.setEvalState(Tristate.SUCCESS);
								
								if(destOS.getEvalState() == Tristate.FAILURE)
								{
									destOS.setDislodgedState(Tristate.YES);
									destOS.setDislodger(thisOS);
									Log.println("       Dislodged. (3.a.3.b.1)");
									//adjudicator.addResult(destOS, ResultType.FAILURE, Utils.getLocalString(MOVE_DISLODGED));
									adjudicator.addDislodgedResult(destOS);
								}
								else if(destOS.getEvalState() == Tristate.UNCERTAIN)
								{
									destOS.setDislodgedState(Tristate.MAYBE);
									destOS.setDislodger(thisOS);
								}
							}
							else if(destOS.getEvalState() == Tristate.UNCERTAIN)
							{
								// we are better than all other moves, but with 
								// self-support. This normally fails, unless the
								// unit actually moves out (which includes a convoy,
								// if head-to-head).
								Log.println("       Dest unit not eval'd; remaining uncertain.");
							}
							else
							{
								thisOS.setEvalState(Tristate.FAILURE);
								Log.println("       Failed. (not better w/o self support)");
								adjudicator.addResult(thisOS, ResultType.FAILURE, Utils.getLocalString(MOVE_FAILED));
							}
						}
					}
				}
				else
				{
					Log.println("     dest is not a Move");
					// 3.a.3.b: case 4	[typical case of 3.a.3.b]
					if(attack_certain > destOS.getDefMax())
					{
						//OLD: if( isDestSamePower(destOS) )
						if(!isBwoss || isDestSamePower(destOS))
						{
							thisOS.setEvalState(Tristate.FAILURE); 
							adjudicator.addResult(thisOS, ResultType.FAILURE, Utils.getLocalString(MOVE_FAILED_NO_SELF_DISLODGE));
						}
						else
						{
							thisOS.setEvalState(Tristate.SUCCESS);
							destOS.setDislodgedState(Tristate.YES);
							destOS.setDislodger(thisOS);
							Log.println("           Dislodged. (3.a.3.b typical)");
							//adjudicator.addResult(destOS, ResultType.FAILURE, Utils.getLocalString(MOVE_DISLODGED));
							adjudicator.addDislodgedResult(destOS);
						}
					}
				}
			}// if(isBetterThanAllOtherMoves)			
		}
		
		// If we have been marked as a 'maybe dislodged' and we are successfull,
		// we cannot be dislodged.
		if(thisOS.getEvalState() == Tristate.SUCCESS && thisOS.getDislodgedState() == Tristate.MAYBE)
		{
			Log.println("    -- successfull; MAYBE dislodged converted to NOT dislodged.");
			thisOS.setDislodgedState(Tristate.NO);
		}
		
		Log.println("    final evalState() = ", thisOS.getEvalState());
	}// evaluate()
	
	
	/** Determines if the given orderstate is the same Power as this order */
	private boolean isDestSamePower(OrderState os)
	{
		if(os != null)
		{
			if( os.getPower().equals(this.getPower()) )
			{
				return true;
			}
		}
		
		return false;
	}// isDestSamePower()
	
	
	
	/**
		Used to determine some beleagured-garrison cases. 
		<p>
		Compares this move to all other moves to destination, but does NOT 
		use self-support when calculating.
		<p>
		Returns 'false' if not better than all other moves w/o self support
		Returns 'true' if it is (thus we will likely dislodge)
		<p>
		NOTE: this contains some code from 3.d in move algorithm.
		<p>
		we should probably create this with a true/false boolean to determine if
		we should use self support or not--would reduce code duplicatio & bugs.
		It would be used instead of the dml iterator code in move.evaluate()
		
	*/
	private boolean isBetterWithoutSelfSupport(OrderState thisOS)
	{
		OrderState[] dml = thisOS.getDependentMovesToDestination();
		Log.println("   Move::isBetterWithoutSelfSupport(); dml.length: ", dml.length);
		
		for(int i=0; i<dml.length; i++)
		{
			OrderState os = dml[i];	
			
			// 3.d
			if( os.isHeadToHead() && (os.getEvalState() == Tristate.UNCERTAIN || !isDependentHTHResolved(os)) )
			{
				// we can't evaluate yet; remain uncertain
				Log.println("          -- but we're not sure yet...");
				return false;
			}
			else if( !os.isHeadToHead() 
					 || (os.isHeadToHead() && os.getDislodger() != os.getHeadToHead()) )
			{
				Log.println("          checking atkCertain <= atk max + atk selfsupport max");
				if( thisOS.getAtkCertain() <= (os.getAtkMax() + os.getAtkSelfSupportMax()) )
				{
					Log.println("          -- but we're definately worse...");
					return false;
				}
			}
			// else.......
		}// while()
		
		return true;
	}// isBetterWithoutSelfSupport()
	
	
	/**
		Given a dependent head-to-head orderstate, 
		see if the opposing battle has been resolved.
	*/
	private boolean isDependentHTHResolved(OrderState depOS)
	{
		OrderState opposingOS = depOS.getHeadToHead();
		if(opposingOS != null)
		{
			return (opposingOS.getEvalState() != Tristate.UNCERTAIN);
		}
		
		// non head-to-head OrderState;
		throw new IllegalArgumentException("non head to head orderstate");
	}// isDependentHTHResolved()
	
		
}// class Move
