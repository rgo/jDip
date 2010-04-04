//
// @(#)Support.java		4/2002
//
// Copyright 2002 Zachary DelProposto. All rights reserved.
// Use is subject to license terms.
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

import dip.world.*;

import dip.process.Adjudicator;
import dip.process.OrderState;
import dip.process.Tristate;

import dip.misc.Log;
import dip.misc.Utils;

import java.util.List;
import java.util.Iterator;


/**
*	Implementation of the Support order.
*	<p>
*	While the ability to specify a narrowing order exists, it is 
*	not currently used. A narrowing order would be used to support
*	a specific type of support/hold/convoy order [typically of another power].
*	
*/
public class Support extends Order
{
	// il8n constants
	private static final String SUPPORT_VAL_NOSELF = "SUPPORT_VAL_NOSELF";
	private static final String SUPPORT_VAL_NOMOVE = "SUPPORT_VAL_NOMOVE";
	private static final String SUPPORT_VAL_INPLACEMOVE = "SUPPORT_VAL_INPLACEMOVE";
	private static final String SUPPORT_VER_FAILTEXT = "SUPPORT_VER_FAILTEXT";
	private static final String SUPPORT_VER_MOVE_ERR = "SUPPORT_VER_MOVE_ERR";
	private static final String SUPPORT_VER_NOMATCH = "SUPPORT_VER_NOMATCH";
	private static final String SUPPORT_VER_MOVE_BADCOAST = "SUPPORT_VER_MOVE_BADCOAST";
	private static final String SUPPORT_EVAL_CUT = "SUPPORT_EVAL_CUT";
	private static final String SUPPORT_DIFF_PASS = "SUPPORT_DIFF_PASS";
	
	private static final String SUPPORT_FORMAT_MOVE = "SUPPORT_FORMAT_MOVE";
	private static final String SUPPORT_FORMAT_NONMOVE = "SUPPORT_FORMAT_NONMOVE";
	
	// constants: names
	private static final String orderNameBrief 	= "S";
	private static final String orderNameFull 	= "Support";
	private static final transient String orderFormatString_move = Utils.getLocalString(SUPPORT_FORMAT_MOVE);
	private static final transient String orderFormatString_nonmove = Utils.getLocalString(SUPPORT_FORMAT_NONMOVE);
	
	// instance variables
	protected Location 	supSrc = null;
	protected Location 	supDest = null;
	protected Unit.Type supUnitType = null;
	protected Order 	narrowingOrder = null; 
	protected Power 	supPower = null;
	
	
	/** Creates a Support order, for supporting a Hold or other <b>non</b>-movement order. */
	protected Support(Power power, Location src, Unit.Type srcUnit, 
		Location supSrc, Power supPower, Unit.Type supUnit)
	{
		this(power, src, srcUnit, supSrc, supPower, supUnit, null);
	}// Support()
	
	/** 
	*	Creates a Support order, for Supporting a Move order. 
	*	<p>
	*	If supDest == null, a Hold support will be generated. Note that If supSrc == supDest,
	*	(or even if provinces are equal), this will be a Supported Move to the same location.
	*	Note that a supported Move to the same location will fail, since a Move to the same
	*	location is not a valid order.
	*	<p>
	*/
	protected Support(Power power, Location src, Unit.Type srcUnit, 
		Location supSrc, Power supPower, Unit.Type supUnit, Location supDest)
	{
		super(power, src, srcUnit);
		
		if(supSrc == null || supUnit == null)
		{
			throw new IllegalArgumentException("null argument(s)");
		}
		
		this.supPower = supPower;
		this.supSrc = supSrc;
		this.supUnitType = supUnit;
		this.supDest = supDest;
	}// Support()
	
	
	/** Creates a Support order */
	protected Support()
	{
		super();
	}// Support()
	
	/** 
	*	A narrowing order only applies to non-move Supports, 
	*	to make it more specific. 
	*	<p>
	*	<b>Note:</b> this can be set, but narrowing order usage is 
	*	not currently implemented.
	*
	*	@throws IllegalArgumentException if this is a Move support.
	*/
	public void setNarrowingOrder(Order o)
	{
		if(!isSupportingHold())
		{
			throw new IllegalArgumentException("Cannot narrow a supported move order.");
		}
		
		narrowingOrder = o;
	}// setNarrowingOrder()
	
	
	
	/** Returns the Location of the Unit we are Supporting */
	public Location getSupportedSrc() 				{ return supSrc; }
	
	/** 
	*	Returns the Unit Type of the Unit we are Supporting 
	*	<b>Warning:</b> this can be null, if no unit type was set, and
	*	no strict validation was performed (via <code>validate()</code>).
	*/
	public Unit.Type getSupportedUnitType() 		{ return supUnitType; }
	
	/** Returns the Narrowing order, or null if none was specified. */
	public Order getNarrowingOrder() 				{ return narrowingOrder; }
	
	/**
	*	Returns the Power of the Unit we are Supporting.
	*	<b>Warning:</b> this can be null, if no unit type was set, and
	*	no strict validation was performed (via <code>validate()</code>).
	*	<p>
	*	<b>Important Note:</b> This also may be null only when a saved game
	*	from 1.5.1 or prior versions are loaded into a recent version,
	*	since prior versions did not support this field.
	*/
	public Power getSupportedPower()				{ return supPower; }
	
	/** 
	*	Returns true if we are supporting a non-Move order.
	*	This is the preferred method of determining if we are truly
	*	supporting a Move order verses a non-Move (Hold) order.
	*/
	public final boolean isSupportingHold()			{ return (supDest == null); }	
	
	/**
	*	Returns true if we are supporting a non-Move order.
	*	<p>
	*	Note: isSupportingHold() should be deprecated. There is no 
	*	difference (other than name) between this method and
	*	isSupportingHold()).
	*/
	public final boolean isNonMoveSupport()			{ return (supDest == null); }	
	
	
	/** 
	*	Returns the Location that we are Supporting into; 
	*	if this is a non-move Support, this will return 
	*	the same (referentially!) location as getSupportedSrc().
	*	It will not return null.
	*/
	public Location getSupportedDest() 				
	{ 
		if(isSupportingHold())
		{
			return supSrc;
		}
		
		return supDest;
	}// getSupportedDest()
	
	
	
	
	public String getFullName()
	{
		return orderNameFull;
	}// getName()
	
	public String getBriefName()				
	{
		return orderNameBrief;
	}// getBriefName()
	
	
	public String getDefaultFormat()
	{
		if(isSupportingHold())
		{
			return orderFormatString_nonmove;
		}
		
		return orderFormatString_move;
	}// getFormatBrief()
	
	
	public String toBriefString()
	{
		StringBuffer sb = new StringBuffer(64);
		
		super.appendBrief(sb);
		sb.append(' ');
		sb.append(orderNameBrief);
		sb.append(' ');
		sb.append(supUnitType.getShortName());
		sb.append(' ');
		supSrc.appendBrief(sb);
		
		if(!isSupportingHold())
		{
			sb.append('-');
			supDest.appendBrief(sb);
		}
		
		return sb.toString();
	}// toBriefString()
	
	
	public String toFullString()
	{
		StringBuffer sb = new StringBuffer(128);
		
		super.appendFull(sb);
		sb.append(' ');
		sb.append(orderNameFull);
		sb.append(' ');
		sb.append(supUnitType.getFullName());
		sb.append(' ');
		supSrc.appendFull(sb);
		
		if(!isSupportingHold())
		{
			sb.append(" -> ");
			supDest.appendFull(sb);
		}
		
		return sb.toString();
	}// toFullString()	
	
	
	
	
	public boolean equals(Object obj)
	{
		if(obj instanceof Support)
		{
			Support support = (Support) obj;
			if(	super.equals(support)
				&& supUnitType == support.supUnitType
				&& supSrc.equals(support.supSrc) 
				&& supPower == support.supPower
				&& ((supDest == support.supDest) || ((supDest != null) && (supDest.equals(support.supDest)))) )
			{
				return true;
			}
		}
		return false;
	}// equals()	
	
	
	public void validate(TurnState state, ValidationOptions valOpts, RuleOptions ruleOpts)
	throws OrderException
	{
		// v.0: 	check season/phase, basic validation
		checkSeasonMovement(state, orderNameFull);
		checkPower(power, state, true);
		super.validate(state, valOpts, ruleOpts);
		
		if(valOpts.getOption(ValidationOptions.KEY_GLOBAL_PARSING).equals(ValidationOptions.VALUE_GLOBAL_PARSING_STRICT))
		{
			Position position = state.getPosition();
			
			// validate Borders
			Border border = src.getProvince().getTransit(src, srcUnitType, state.getPhase(), this.getClass());
			if(border != null)
			{
				throw new OrderException( Utils.getLocalString(ORD_VAL_BORDER, src.getProvince(), border.getDescription()) );
			}
			
			// v.1: unit existence / matching
			Unit supUnit = position.getUnit( supSrc.getProvince() );
			supUnitType = getValidatedUnitType(supSrc.getProvince(), supUnitType, supUnit);
			
			// v.1.5: supported unit power matching. As per DATC, if specified
			// supporting Power is missing, we'll add it. If it's incorrect, we'll
			// change it to the correct power, without throwing an exception.
			supPower = supUnit.getPower();
			
			// v.2: location validation
			supSrc = supSrc.getValidatedAndDerived(supUnitType, supUnit);
			
			// (v.3) this checks for same-province support, like F trieste SUPPORT trieste
			// note that this would be caught by the standard adjacency check, but the error
			// message is then less clear.
			if(src.isProvinceEqual(supSrc))
			{
				throw new OrderException(Utils.getLocalString(SUPPORT_VAL_NOSELF));
			}
			
			// validate Borders
			border = supSrc.getProvince().getTransit(supSrc, supUnitType, state.getPhase(), this.getClass());
			if(border != null)
			{
				throw new OrderException( Utils.getLocalString(ORD_VAL_BORDER, src.getProvince(), border.getDescription()) );
			}
			
			// destination validation (if applicable)
			if(!isSupportingHold())
			{
				// v.2: location validation
				supDest = supDest.getValidated(supUnitType);
				
				// v.3: adjacency check
				if( !src.isAdjacent(supDest.getProvince()) )
				{
					throw new OrderException(Utils.getLocalString(SUPPORT_VAL_NOMOVE, 
									src.toLongString(), supDest.toLongString()));
				}
				
				// v.4: check that supSrc and supDest are not the same province
				// (to support a Hold, supDest must be null)
				if( supSrc.isProvinceEqual(supDest) )
				{
					throw new OrderException(Utils.getLocalString(SUPPORT_VAL_INPLACEMOVE));
				}
				
				// destination border validation
				border = supDest.getProvince().getTransit(supDest, supUnitType, state.getPhase(), this.getClass());
				if(border != null)
				{
					throw new OrderException( Utils.getLocalString(ORD_VAL_BORDER, src.getProvince(), border.getDescription()) );
				}
			}
			
		}
	}// validate();
	
	
	/**
	*	Checks if support orders are appropriately matched, since at this time
	*	all orders are known.
	*	
	*	Matching of Support
	*	<ul>
	*		<li>!isSupportingHold() : supported Move<br>
	*			supportSrc unit must have a matching move order, else fails
	*			
	*		<li>isSupportingHold() : supported Hold/Convoy/Support [anti-dislodgement]<br>
	*			 supportSrc must NOT be a MOVE order
	*	</ul>
	*	<p>
	*	At this time, we do not check for narrowing conventions in a support order.
	*
	*/
	public void verify(Adjudicator adjudicator)
	{
		String failureText = null;					
		boolean isMatched = false;
		
		final OrderState matchingOS = adjudicator.findOrderStateBySrc(getSupportedSrc());
		final OrderState thisOS = adjudicator.findOrderStateBySrc(getSource());
		
		if(matchingOS == null)
		{
			failureText = Utils.getLocalString(SUPPORT_VER_FAILTEXT);
		}
		else if(isSupportingHold())
		{
			// Support: supporting a unit that is not moving (Hold/Support/Convoy)
			//
			failureText = Utils.getLocalString(SUPPORT_VER_MOVE_ERR);
			
			if( !(matchingOS.getOrder() instanceof Move) )
			{
				isMatched = true;
			}
		}
		else
		{
			// Support: supporting a Move
			failureText = Utils.getLocalString(SUPPORT_VER_NOMATCH);
			if(matchingOS.getOrder() instanceof Move)
			{
				Move matchingMove = (Move) matchingOS.getOrder();
				if( matchingMove.getDest().isProvinceEqual(getSupportedDest()) )
				{
					// NOTE: if a coast is specified in the destination, it MUST match the move order.
					// (see 2001 DATC 2.E)
					if( !getSupportedDest().getCoast().equals(Coast.UNDEFINED) )
					{
						if( matchingMove.getDest().equals(getSupportedDest()) )
						{
							isMatched = true;
						}
						else
						{
							failureText = Utils.getLocalString(SUPPORT_VER_MOVE_BADCOAST);
						}
					}
					else
					{
						isMatched = true;
					}
				}
			}
		}
		
		if(!isMatched)
		{
			thisOS.setEvalState(Tristate.FAILURE);
			adjudicator.addResult(thisOS, ResultType.FAILURE, failureText);
		}
		
		// we have been verified.
		thisOS.setVerified(true);
	}// verify()
	
	
	/**
	*	Dependencies for a Support order:
	*	<ol>
	*		<li>Moves to this space (for cuts, and dislodgement)
	*		<li>Support to this space (only considered if attacked, to prevent dislodgement)
	*	</ol>
	*/
	public void determineDependencies(Adjudicator adjudicator)
	{
		addSupportsOfAndMovesToSource(adjudicator);
	}// determineDependencies()
	
	
	/**
		NOTE: this description may be slightly out of date
		<pre>
		evaluation of Support orders
	
		1) calculate support (in this case, support to prevent dislodgement)
			this is done by all evaluate() methods
			
		2) determine if this support is cut, not cut, or undecided.
				(NOTE: (a) is OBSOLETE; it was considered to be a special case in an older, incorrect
				version of this algorithm).
				a) determine if this support is supporting an attack on our own unit,
					self-support is completely legal, and is treated like any other support.
					Thus it is cut as in 2.b, below.
					(Note: Move orders treat self-support differently when determining strength calculations)
				
				b) non-convoyed attacks (moves) *OR* no attack; 
				   these should be evaluated first, even if there is a possible convoyed attack.
				   evaluate in this order:
						1) from where support is going into: 
							Note: although support cannot be cut by an attack to where support is being given,
							that attack can cut support if it will (or COULD) *dislodge* the supporting unit.
							To determine this, we must analyze the move against this support.
								a) if we are dislodged, (via move.evaluate()), then support is cut, becomes FAILURE
								b) if the move COULD have enough strength to dislodge this unit, become UNCERTAIN
									move.attackMax > support.defense_certain
									note that we do not count self-support (cannot self-dislodge)
									although we could cut support, that is NOT our perogative. Move.evaluate()
									must dislodge this support.
								c) if the move NEVER could have enough strength to dislodge this unit, SUCCESS
									 move.attackMax <= support.defense_certain
									 note that we do not count self-support (cannot self-dislodge)
						2) if attack is from own unit (power): SUCCESS; support not cut
						3) otherwise, support is cut, if an attack exists, or support suceeds,
							if there is no attack.
						
				c) convoyed attacks (moves); eval in this order:
						1) SUCCESS if move fails (because there is no convoy); support not cut
						2) SUCCESS if attack is from own power (can't cut support)
						3) determine if support is to an attack on a fleet convoying the attacking army:
							a) if NOT, support is cut
							b) if it is, we must apply rule 21/22 as follows:
								1) support is NOT cut if fleet is necessary for unit to convoy (SUCCESS)
								2) support IS cut if there are multiple convoy routes, and at least
									one convoy route is successful. (FAILURE)
								3) if this cannot yet be determined, UNCERTAIN
													
				
				NOTE: for multiple attacks:
					FAILURE >> UNCERTAIN >> SUCCESS
					
				only 2.c.3.b.3, 2.b.1.b result in UNCERTAIN success results.
		</pre>
	*/
	public void evaluate(Adjudicator adjudicator)
	{
		Log.println("--- evaluate() dip.order.Support ---");
		
		OrderState thisOS = adjudicator.findOrderStateBySrc(getSource());
		// 1) calculate support (to prevent dislodgement)
		
		/* 
		 * The baseMoveModifier is identified here. It is used later in the code to determine
		 * if support was given over a DPB
		 * If there is a border baseMoveModifer, it needs to be recompensated here.
		 * If the modification was negitive, subtract it to add it back.
		 * If the modification was positive, subtract it to remove it.
		 */
		int mod = getSupportedDest().getProvince().getBaseMoveModifier(getSource());
		thisOS.setDefMax( thisOS.getSupport(false) - mod);
		thisOS.setDefCertain( thisOS.getSupport(true) - mod);
	
		if(Log.isLogging())
		{
			Log.println("   order: ",this);
			Log.println("   initial evalstate: ",thisOS.getEvalState());
			Log.println("     def-max: ",thisOS.getDefMax());
			Log.println("    def-cert: ",thisOS.getDefCertain());	
			Log.println("	atk-max:	", thisOS.getAtkMax());
			Log.println("	atk-cert:	", thisOS.getAtkCertain());	
			Log.println("  # supports: ",thisOS.getDependentSupports().length);
			Log.println("  # supports to hold: ",thisOS.getDependentSupports().length);
			Log.println("  # of possible cuts: ",thisOS.getDependentMovesToSource().length);
			Log.println("  dislodged?: ",thisOS.getDislodgedState());
		}
		
		// 2) evaluate whether we are cut or not
		if(thisOS.getEvalState() == Tristate.UNCERTAIN)
		{
			// 2.b.1.a if we have been dislodged by a move, then we cannot support.
			if(thisOS.getDislodgedState() == Tristate.YES)
			{
				thisOS.setEvalState(Tristate.FAILURE);
				return;
			}			
			
			// support starts as a SUCCESS, unless we can't tell (UNCERTAIN) or it is definately cut (FAILURE)
			Tristate evalResult = Tristate.SUCCESS;
			Move cuttingMove = null;
			
			OrderState[] depMovesToSrc = thisOS.getDependentMovesToSource();

			for(int i=0; i<depMovesToSrc.length; i++)
			{
				OrderState depMoveOS = depMovesToSrc[i];
				Move depMove = (Move) depMoveOS.getOrder();
												
				Log.println("  checking against move: ", depMove);

				if( getPower().equals(depMove.getPower()) )
				{
					// 2.b.2, 2.c.2
					Log.println("   -- move is of same power; not cut");
					evalResult = pickState(evalResult, Tristate.SUCCESS);
				}
				else
				{	
					if(!depMove.isConvoying())
					{
						// 2.b non-convoyed move
						//
						if( getSupportedDest().isProvinceEqual(depMove.getSource()) )
						{
							Log.println("     --Unit attacking destination; not cut");
							// 2.b.1 [support cannot be cut by an attack to where support is being given]
							// determine if we *could* become dislodged. 2.b.1.a (we *are* dislodged)
							// is determined earlier; 2.b.1.b/c are determined here. Self-supports not considered.
							if( depMoveOS.getAtkMax() <= thisOS.getDefCertain() )
							{
								// 2.b.1.c
								// if the support is not given over a difficult passable border
								if(mod >= 0){
									Log.println("     -- Support is not over DPB; not cut");
									evalResult = pickState(evalResult, Tristate.SUCCESS);
								}
							}
							else
							{
								// 2.b.1.b
								evalResult = pickState(evalResult, Tristate.UNCERTAIN);
							}
						}
						else
						{
							Log.println("     --Unit attacking source; checking...");
							// 2.b.3 with border rule check possible.
							// If the dependant move succeeded, support cut;
							if(depMoveOS.getEvalState() == Tristate.SUCCESS) {
								Log.println("\t --Dependant move succeeded; support cut");
								evalResult = pickState(evalResult, Tristate.FAILURE);
								cuttingMove = depMove;
							} else {
								// If the move failed, but did have the possibility of strength
								if(depMoveOS.getAtkMax() != 0){
									Log.println("\t -- Unit move failed, but could still cut support...");
									// If there was a definate attack strength; support cut
									if(depMoveOS.getAtkCertain() != 0){
										Log.println("\t -- Unit has some strength left...");
										evalResult = pickState(evalResult, Tristate.FAILURE);
										cuttingMove = depMove;	
									// If there was a possibility of strength, but its not appeared yet, we are uncertain.
									} else {
										evalResult = pickState(evalResult, Tristate.UNCERTAIN);
									}
								}										
							}
						}
							
					}
					else
					{
						// 2.c convoyed move
						//
						Log.println("     -- move is convoying:");
						
						if(depMoveOS.getEvalState() == Tristate.FAILURE)
						{
							// 2.c.1
							Log.println("        -- but move failed, so won't cut support.");
							evalResult = pickState(evalResult, Tristate.SUCCESS);
						}
						else 
						{
							Order convoy = getSupportingAConvoyAttack(adjudicator, depMove);
							Path path = new Path(adjudicator);
							
							Log.println("     supporting convoy attack = ", convoy);
							
							if(convoy != null)
							{
								// 2.c.3.b (1,2,3)
								// if pathEvalResult == uncertain, we are uncertain. However, 
								// if pathEvalResult == SUCCESS, convoy must have more than one route;
								// 						support is cut.
								// if pathEvalResult == FAILURE, convoy must have ONLY one route;
								//						support is not cut.
								Tristate pathEvalResult = path.getConvoyRouteEvaluation(depMove,
															convoy.getSource(), null );
								cuttingMove = depMove;	// if we don't cut, this will just be ignored.
								
								Log.println("     convoy eval result = ", pathEvalResult);
								
								if(pathEvalResult == Tristate.SUCCESS)
								{
									// if the dependant move has any attacking power...this support fails
									if(depMoveOS.getAtkMax() != 0){
										evalResult = pickState(evalResult, Tristate.FAILURE);
									}
								}
								else if(pathEvalResult == Tristate.FAILURE)
								{
									evalResult = pickState(evalResult, Tristate.SUCCESS);
								}
								else
								{
									evalResult = pickState(evalResult, Tristate.UNCERTAIN);
								}
								
								// if we ever decide to just use the Szykman rule, instead of the
								// 2000 rules, comment out the above and just leave this order
								// uncertain.
								/*
								Log.println("** no 2000 rule used: leaving uncertain");
								evalResult = pickState(evalResult, Tristate.UNCERTAIN);
								*/
							}
							else
							{
								// 2.c.3.a
								// depends upon route; if route is SUCCESS, we fail; if route is FAILURE, 
								// not cut, if route is uncertain, so are we.
								Tristate pathEvalResult = path.getConvoyRouteEvaluation(depMove, 
															null, null);
								if(pathEvalResult == Tristate.SUCCESS)
								{
									// If the dependant move has any attacking power...this support fails
									if(depMoveOS.getAtkMax() != 0){
										evalResult = pickState(evalResult, Tristate.FAILURE);
										cuttingMove = depMove;
									}
									
								}
								else if(pathEvalResult == Tristate.FAILURE)
								{
									evalResult = pickState(evalResult, Tristate.SUCCESS);
								}
								else
								{
									// uncertain
									evalResult = pickState(evalResult, Tristate.UNCERTAIN);
								}
							}	
						}
					}
				}
			}// while()
			
			// If we manage to pass the tests but are supporting over a DPB, fail.
			if( mod < 0){
				evalResult = pickState(evalResult, Tristate.FAILURE);
			}
						
			// set evaluation state, inform user
			thisOS.setEvalState(evalResult);
			if(evalResult == Tristate.FAILURE)
			{
				// If we ARE supporting over a difficult passable border...
				if(mod < 0){
					Log.println(" Unable to support through difficult passable border");
					adjudicator.addResult(thisOS, ResultType.FAILURE, 
							Utils.getLocalString(SUPPORT_DIFF_PASS));		
				} else {
					Log.println(" ** support cut by move from ", cuttingMove.getSource());				
					adjudicator.addResult(thisOS, ResultType.FAILURE, 
							Utils.getLocalString(SUPPORT_EVAL_CUT, cuttingMove.getSource().getProvince()));
				}
				
			}

			Log.println("   final evalstate: ",thisOS.getEvalState());
		}
	}// evaluate()	
	
	
	
	/**
		<pre>
		Given multiple attacks (moves) on an order,
		determine what the result should be, per this table.
		This method is needed because there could be multiple attacks against a support, 
			and if one attack cuts support, then the result of the other attacks don't
			matter.
		
		success == support is not cut
		failure == support IS cut
		
		Tristate:
			oldstate		newstate		result
			========		========		======
			UNCERTAIN		UNCERTAIN		UNCERTAIN
			UNCERTAIN		FAILURE			FAILURE
			UNCERTAIN		SUCCESS			UNCERTAIN
			
			FAILURE			UNCERTAIN		FAILURE
			FAILURE			FAILURE			FAILURE
			FAILURE			SUCCESS			FAILURE
			
			SUCCESS			UNCERTAIN		UNCERTAIN
			SUCCESS			FAILURE			FAILURE
			SUCCESS			SUCCESS			SUCCESS
			
			
			more succinctly:
			
			FAILURE >> UNCERTAIN >> SUCCESS
		</pre>
	*/
	private Tristate pickState(Tristate oldState, Tristate newState)
	{
		// any failure == failure
		if(newState == Tristate.FAILURE || oldState == Tristate.FAILURE)
		{
			return Tristate.FAILURE;
		}
		
		// any uncertain == uncertain, if no failure
		if(newState == Tristate.UNCERTAIN || oldState == Tristate.UNCERTAIN)
		{
			return Tristate.UNCERTAIN;
		}
		
		// else, success
		return Tristate.SUCCESS;
	}// pickState()
	
	
	
	/**
	*
	*	Determines if we are supporting a MOVE to attack a 
	*		convoy in the path of the move we are checking
	*	<p>
	*	Returns OrderState or null 
	*		
	*/
	private Order getSupportingAConvoyAttack(Adjudicator adjudicator, Move convoyedMove)
	{
		// are we even supporting a Move?
		if(!isSupportingHold())
		{
			// if so, get the OrderState at the destination
			OrderState destOS = adjudicator.findOrderStateBySrc( getSupportedDest() );
			if(destOS != null)
			{
				if(destOS.getOrder() instanceof Convoy)
				{
					// destination of the supported move has a convoy
					// see if it matches the convoyedMove
					Convoy convoy = (Convoy) destOS.getOrder();
					if(	convoy.getConvoySrc().equals(convoyedMove.getSource()) 
						&& convoy.getConvoyDest().equals(convoyedMove.getDest()) )
					{
						return convoy;
					}
				}
			}
		}
		
		return null;
	}// getSupportingAConvoyAttack()
	
}// class Support
