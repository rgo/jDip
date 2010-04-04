//
// 	@(#)Convoy.java		4/2002
//
// 	Copyright 2002 Zachary DelProposto. All rights reserved.
// 	Use is subject to license terms.
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
*
*	Implementation of the Convoy order.
*
*
*
*
*/

public class Convoy extends Order
{
	// il8n constants
	private static final String CONVOY_SEA_FLEETS = "CONVOY_SEA_FLEETS";
	private static final String CONVOY_ONLY_ARMIES = "CONVOY_ONLY_ARMIES";
	private static final String CONVOY_NO_ROUTE = "CONVOY_NO_ROUTE";
	private static final String CONVOY_VER_NOMOVE = "CONVOY_VER_NOMOVE";
	private static final String CONVOY_FORMAT = "CONVOY_FORMAT";
	private static final String CONVOY_SELF_ILLEGAL = "CONVOY_SELF_ILLEGAL";
	private static final String CONVOY_TO_SAME_PROVINCE = "CONVOY_TO_SAME_PROVINCE";
	
	// constants: names
	private static final String orderNameBrief 	= "C";
	private static final String orderNameFull 	= "Convoy";
	private static final transient String orderFormatString = Utils.getLocalString(CONVOY_FORMAT);
	
	// instance variables	
	protected Location convoySrc = null;
	protected Location convoyDest = null;
	protected Unit.Type convoyUnitType = null;
	protected Power convoyPower = null;
	
	/** Creates a Convoy order */
	protected Convoy(Power power, Location src, Unit.Type srcUnit, 
		Location convoySrc, Power convoyPower, Unit.Type convoyUnitType, 
		Location convoyDest)
	{
		super(power, src, srcUnit);

		if(convoySrc == null || convoyUnitType == null || convoyDest == null)
		{
			throw new IllegalArgumentException("null argument(s)");
		}
				
		this.convoySrc = convoySrc;
		this.convoyUnitType = convoyUnitType;
		this.convoyPower = convoyPower;
		this.convoyDest = convoyDest;
	}// Convoy()
	
	
	/** Creates a Convoy order */
	protected Convoy()
	{
		super();
	}// Convoy()
	
	
	/** Returns the Location of the Unit to be Convoyed */
	public Location getConvoySrc() 			{ return convoySrc; }
	
	/** 
	*	Returns the Unit Type of the Unit to be Convoyed 
	*	<b>Warning:</b> this can be null, if no unit type was set, and
	*	no strict validation was performed (via <code>validate()</code>).
	*/
	public Unit.Type getConvoyUnitType() 		{ return convoyUnitType; }
	
	/**
	*	Returns the Power of the Unit we are Convoying.
	*	<b>Warning:</b> this can be null, if no unit type was set, and
	*	no strict validation was performed (via <code>validate()</code>).
	*	<p>
	*	<b>Important Note:</b> This also may be null only when a saved game
	*	from 1.5.1 or prior versions are loaded into a recent version,
	*	since prior versions did not support this field.
	*/
	public Power getConvoyedPower()				{ return convoyPower; }
	
	
	/** Returns the Location of the Convoy destination */
	public Location getConvoyDest() 			{ return convoyDest; }
	
	
	
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
		return orderFormatString;
	}// getFormatBrief()
	
	
	public String toBriefString()
	{
		StringBuffer sb = new StringBuffer(64);
		
		super.appendBrief(sb);
		sb.append(' ');
		sb.append(orderNameBrief);
		sb.append(' ');
		sb.append(convoyUnitType.getShortName());
		sb.append(' ');
		convoySrc.appendBrief(sb);
		sb.append('-');
		convoyDest.appendBrief(sb);
		
		return sb.toString();
	}// toBriefString()
	
	
	public String toFullString()
	{
		StringBuffer sb = new StringBuffer(128);
		
		super.appendFull(sb);
		sb.append(' ');
		sb.append(orderNameFull);
		sb.append(' ');
		sb.append(convoyUnitType.getFullName());
		sb.append(' ');
		convoySrc.appendFull(sb);
		sb.append(" -> ");
		convoyDest.appendFull(sb);
		
		return sb.toString();
	}// toFullString()
	
	
	
	
	
	public boolean equals(Object obj)
	{
		if(obj instanceof Convoy)
		{
			Convoy convoy = (Convoy) obj;
			if(	super.equals(convoy)
				&& this.convoySrc.equals(convoy.convoySrc)
				&& this.convoyUnitType.equals(convoy.convoyUnitType)
				&& this.convoyDest.equals(convoy.convoyDest) )
			{
				return true;
			}
		}
		return false;
	}// equals()	
	
	
	
	public void validate(TurnState state, ValidationOptions valOpts, RuleOptions ruleOpts)
	throws OrderException
	{
		// v.0: 	check phase, basic validation
		checkSeasonMovement(state, orderNameFull);
		checkPower(power, state, true);
		super.validate(state, valOpts, ruleOpts);

		if(valOpts.getOption(ValidationOptions.KEY_GLOBAL_PARSING).equals(ValidationOptions.VALUE_GLOBAL_PARSING_STRICT))
		{
			Position position = state.getPosition();
			Province srcProvince = src.getProvince();
			
			// v.1: src unit type must be a fleet, in a body of water
			// OR in a convoyable coast.
			if(!srcUnitType.equals(Unit.Type.FLEET) || (!srcProvince.isSea() && !srcProvince.isConvoyableCoast()))
			{
				throw new OrderException(Utils.getLocalString(CONVOY_SEA_FLEETS));
			}
			
			// validate Borders
			Border border = src.getProvince().getTransit(src, srcUnitType, state.getPhase(), this.getClass());
			if(border != null)
			{
				throw new OrderException( Utils.getLocalString(ORD_VAL_BORDER, src.getProvince(), border.getDescription()) );
			}
			
			// v.2: 	a) type-match unit type with current state, and unit must exist
			// 		b) unit type must be ARMY		
			Unit convoyUnit = position.getUnit( convoySrc.getProvince() );
			convoyUnitType = getValidatedUnitType(convoySrc.getProvince(), convoyUnitType, convoyUnit);
			if( !convoyUnitType.equals(Unit.Type.ARMY) )
			{
				throw new OrderException(Utils.getLocalString(CONVOY_ONLY_ARMIES));
			}
			
			// v.3.a: validate locations: convoySrc & convoyDest
			convoySrc = convoySrc.getValidatedAndDerived(convoyUnitType, convoyUnit);
			convoyDest = convoyDest.getValidated(convoyUnitType);
			
			// v.3.b: convoying to self (if we are in a convoyable coast) is illegal!
			if(srcProvince.isConvoyableCoast() && src.isProvinceEqual(convoyDest))
			{
				throw new OrderException(Utils.getLocalString(CONVOY_SELF_ILLEGAL));
			}
			
			// v.3.c: origin/destination of convoy must not be same province.
			if(convoySrc.isProvinceEqual(convoyDest))
			{
				throw new OrderException(Utils.getLocalString(CONVOY_TO_SAME_PROVINCE));
			}
			
			// v.4:	a *theoretical* convoy route must exist between 
			//		convoySrc and convoyDest
			Path path = new Path(position);
			if( !path.isPossibleConvoyRoute(convoySrc, convoyDest) )
			{
				throw new OrderException(Utils.getLocalString(CONVOY_NO_ROUTE, 
						convoySrc.toLongString(), convoyDest.toLongString() ));
			}
			
			// validate Borders
			border = convoySrc.getProvince().getTransit(convoySrc, convoyUnitType, state.getPhase(), this.getClass());
			if(border != null)
			{
				throw new OrderException( Utils.getLocalString(ORD_VAL_BORDER, src.getProvince(), border.getDescription()) );
			}
			
			border = convoyDest.getProvince().getTransit(convoyDest, convoyUnitType, state.getPhase(), this.getClass());
			if(border != null)
			{
				throw new OrderException( Utils.getLocalString(ORD_VAL_BORDER, src.getProvince(), border.getDescription()) );
			}
		}				
	}// validate();
	
	
	
	/**
	*	Checks for matching Move orders.
	*
	*/
	public void verify(Adjudicator adjudicator)
	{
		OrderState thisOS = adjudicator.findOrderStateBySrc(getSource());
		if(thisOS.getEvalState() == Tristate.UNCERTAIN)
		{
			// check for a matching move order. 
			//
			// note that the move must have its isByConvoy() flag set, so we don't
			// kidnap armies that prefer not to be convoyed.
			boolean foundMatchingMove = false;
			
			OrderState matchingOS = adjudicator.findOrderStateBySrc( getConvoySrc() );
			if(matchingOS != null)
			{
				if(matchingOS.getOrder() instanceof Move)
				{
					Move convoyedMove = (Move) matchingOS.getOrder();
					
					// check that Move has been verified; if it has not, 
					// we should just immediately verify it (though we could
					// wait for the adjudicator to do so).
					if(!matchingOS.isVerified())
					{
						convoyedMove.verify(adjudicator);
						
						// but if it doesn't verify, then we have a 
						// dependency-error.
						if(!matchingOS.isVerified())
						{
							throw new IllegalStateException("Verify dependency error.");
						}
					}
					
					if( convoyedMove.isConvoying() 
						&& getConvoyDest().isProvinceEqual(convoyedMove.getDest()) )
					{
						foundMatchingMove = true;
					}
				}
			}
			
			if(!foundMatchingMove)
			{
				thisOS.setEvalState(Tristate.FAILURE);
				adjudicator.addResult(thisOS, ResultType.FAILURE, Utils.getLocalString(CONVOY_VER_NOMOVE));
			}
		}
		
		thisOS.setVerified(true);
	}// verify()	
	
	/**
	*	Dependencies for a Convoy order are:
	*	<ol>
	*		<li>Moves to this space (to determine dislodgement)
	*		<li>Supports to this space (only considered if attacked, to prevent dislodgement)
	*	</ol>
	*/
	public void determineDependencies(Adjudicator adjudicator)
	{
		addSupportsOfAndMovesToSource(adjudicator);
	}// determineDependencies()
	
	
	/** Convoy order evaluation logic */
	public void evaluate(Adjudicator adjudicator)
	{
		Log.println("--- evaluate() dip.order.Convoy ---");
		
		OrderState thisOS = adjudicator.findOrderStateBySrc(getSource());
		
		// calculate support
		thisOS.setDefMax( thisOS.getSupport(false) );
		thisOS.setDefCertain( thisOS.getSupport(true) );
		
		if(Log.isLogging())
		{
			Log.println("   order: ",this);
			Log.println("   initial evalstate: ",thisOS.getEvalState());
			Log.println("     def-max: ",thisOS.getDefMax());
			Log.println("    def-cert: ",thisOS.getDefCertain());		
			Log.println("  # supports: ",thisOS.getDependentSupports().length);
		}
		
		// determine evaluation state. This is important for Convoy orders, since
		// moves depend upon them. If we cannot determine, we will remain uncertain.
		if(thisOS.getEvalState() == Tristate.UNCERTAIN)
		{
			// if for some reason we were dislodged, but not marked as failure, 
			// mark as failure.
			if(thisOS.getDislodgedState() == Tristate.YES)
			{
				thisOS.setEvalState(Tristate.FAILURE);
				return;
			}
			
			// we will also succeed if there are *no* moves against us, or if all the
			// moves against us have failed.
			boolean isSuccess = true;
			OrderState[] depMovesToSrc = thisOS.getDependentMovesToSource();
			for(int i=0; i<depMovesToSrc.length; i++)
			{
				if(depMovesToSrc[i].getEvalState() != Tristate.FAILURE)
				{
					isSuccess = false;
					break;
				}
			}
			
			if(isSuccess)
			{
				thisOS.setEvalState(Tristate.SUCCESS);
				thisOS.setDislodgedState(Tristate.NO);
			}
		}
		
		Log.println("  final evalState: ", thisOS.getEvalState());
	}// evaluate()	
	
}// class Convoy
