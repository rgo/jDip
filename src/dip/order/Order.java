//	
// 	@(#)Order.java	12/2002
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

import dip.world.*;
import dip.process.Adjudicator;
import dip.process.OrderState;
import dip.misc.Utils;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import java.io.*;

/**
 *  This is the base class for all Order objects.
 *	<p>
 * 	<b>When referring to an Order subclass, it is best to refer to an object
 *	as an "Orderable" object rather than an "Order" object.
 *	</b>
 *	<br>	
 *	For example:
 *	<pre>
 *		GUIOrderFactory gof = new GUIOrderFactory();
 *		
 * 		Order order = OrderFactory.getDefault().createHold(a,b,c);			// ok
 *		Orderable order = OrderFactory.getDefault().createHold(a,b,c);		// dangerous
 *		
 *		Order order = gof.createHold(a,b,c);			// this may fail!!
 *		Orderable order = gof.createHold(a,b,c);		// ok
 *	</pre>
 *	<p>
 *	This class provides default implementations for many Order interface
 *	methods, as well as protected convenience methods.
 *	<p>
 *	A note on serialization: provided are several internal fields
 *	which can be used to 'upgrade' objects as needed. These fields
 *	are for future use, but their presence enables future upgradibility.
 *
 *
 *
 */
public abstract class Order extends Object implements Orderable, java.io.Serializable
{
	// resource keys
	private static final String ORD_VAL_NOUNIT = "ORD_VAL_NOUNIT";
	private static final String ORD_VAL_BADPOWER = "ORD_VAL_BADPOWER";
	private static final String ORD_VAL_BADTYPE = "ORD_VAL_BADTYPE";
	private static final String ORD_VAL_SZ_RETREAT = "ORD_VAL_SZ_RETREAT";
	private static final String ORD_VAL_SZ_ADJUST = "ORD_VAL_SZ_ADJUST";
	private static final String ORD_VAL_SZ_MOVEMENT = "ORD_VAL_SZ_MOVEMENT";
	//private static final String ORD_VAL_SZ_SETUP = "ORD_VAL_SZ_SETUP";
	//private static final String ORD_VAL_PWR_DISORDER = "ORD_VAL_PWR_DISORDER";
	private static final String ORD_VAL_PWR_ELIMINATED = "ORD_VAL_PWR_ELIMINATED";
	private static final String ORD_VAL_PWR_INACTIVE = "ORD_VAL_PWR_INACTIVE";
	protected static final String ORD_VAL_BORDER = "ORD_VAL_BORDER";
	
	
	/** Power who gave the order to the unit */
	protected Power power   = null;
	
	/** Location of the ordered unit */
	protected Location src  = null;
	
	/** Type of the ordered unit */
	protected Unit.Type srcUnitType  = null;
	
	
	/**
	*	No-arg constructor
	*/
	protected Order()
	{
	}// Order()
	
	
	/**
	 *  Constructor for the Order object
	 *
	 *@param  power    Power giving the Order
	 *@param  src      Location of the ordered unit
	 *@param  srcUnit  Unit type
	 *@since
	 */
	protected Order(Power power, Location src, Unit.Type srcUnit)
	{
		
		if(power == null || src == null || srcUnit == null)
		{
			throw new IllegalArgumentException("null parameter(s)");
		}
		
		this.power = power;
		this.src = src;
		this.srcUnitType = srcUnit;
	}// Order()
	
	
	public final Location getSource()
	{
		return src;
	}// getSource()
	
	
	public final Unit.Type getSourceUnitType()
	{
		return srcUnitType;
	}// getSourceUnitType()
	
	
	public final Power getPower()
	{
		return power;
	}// getPower()
	
	
	// 
	// Format methods
	// 
	public String toFormattedString(OrderFormatOptions ofo)
	{
		return OrderFormat.format(ofo, getDefaultFormat(), this);
	}// toFormattedString()
	
	
	// 
	// Adjudicator methods
	// 
	
	/**
	 * Validate the order. 
	 * <p>
	 * This checks the order for legality. An order is not considered to be 
	 * well-formed until it has been validated.
	 * It validates locations (at the least), and other order
	 * syntax as appropriate, throwing exceptions if there is a conflict.
	 * <p>
	 * This is used by the UI to flag bad orders, as well as by the adjudicator
	 * to eliminate bad orders before processing.
	 * <p>
	 * Important Note:
	 * <p>
	 * The only state that should be assumed is the following:
	 * 		1) orders for this power (if entered)
	 *		2) the positions of all units.
	 * Thus, the orders of units not controlled by this power may
	 * not be known (in multiplayer games, this information would not
	 * be available until adjudication took place). DO NOT assume order
	 * information for the units of any power other than that of this unit/order!
	 * <p>
	 * Usage Notes:
	 * <p>
	 * Subclasses should generally call super.validate() before performing
	 * additional validation. 
	 * <p>
	 * There should be no side effects of calling validate() multiple times, nor
	 * should behavior differ between multiple calls of validate(), given the same
	 * order.
	 * 
	 *
	 *@param  state               Current turn state
	 *@param valOpts			 Current validation options
	 *@exception  OrderException  
	 */
	public void validate(TurnState state, ValidationOptions valOpts, RuleOptions ruleOpts)
	throws OrderException
	{
		Position position = state.getPosition();
		Unit unit = position.getUnit( src.getProvince() );
		validate(valOpts, unit);
	}// validate()
	
	
	//
	//	Convenience methods
	//
	//
	
	/**
	*	Convenience method typically used in determineDependencies(); this 
	*	method gets all Support and Move orders to this space and adds 
	*	them to the dependency list. 
	*	<p>
	*	Only "hold" supports are added. Invalid Move orders are NEVER added.
	*
	*/
	protected final void addSupportsOfAndMovesToSource(Adjudicator adjudicator)
	{
		OrderState thisOS = adjudicator.findOrderStateBySrc(getSource());
		ArrayList depMTS = null;
		ArrayList depSup = null;
		
		OrderState[] orderStates = adjudicator.getOrderStates();
		for(int osIdx=0; osIdx<orderStates.length; osIdx++)
		{
			OrderState dependentOS = orderStates[osIdx];
			Order order = dependentOS.getOrder();
			
			if(order != this) // always exclude self
			{
				if( order instanceof Move
					&& ((Move) order).getDest().isProvinceEqual(this.getSource()) )
				{
					if(depMTS == null) { depMTS = new ArrayList(5); }
					depMTS.add(dependentOS);
				}
				else if(order instanceof Support)
				{
					Support support = (Support) order;
					
					// if we don't check for hold-type support (Support.isSupportingHold() == true)
					// we will accidentally add move-supports! (bad)
					if(	support.isSupportingHold()
						&& support.getSupportedSrc().isProvinceEqual(this.getSource()) )
					{
						if(depSup == null) { depSup = new ArrayList(5); }
						depSup.add(dependentOS);
					}
				}
			}
		}
		
		// set supports / endangering moves in OrderState
		if(depMTS != null)
		{
			thisOS.setDependentMovesToSource(depMTS);
		}
		
		if(depSup != null)
		{
			thisOS.setDependentSupports(depSup);
		}
	}// addSupportsOfAndMovesToSource()		
	
	
	
	
	
	/** 
	*	Validates a dislodged or non-dislodged unit, depending upon what type of
	*	unit is specified
	*/
	protected void validate(ValidationOptions valOpts, Unit unit)
	throws OrderException
	{
		// Basic Validation: [not in correct order]
		// 	1) must be a unit in the province ordered.
		//	2) Power of unit in Province must == Power of this Order
		//	3) if unit type is undefined, determine correct type.
		// 		if type mismatch, throw exception.
		//	4) validate the source location given the unit type.
		//		(ensure coast is correct)
		//
		Province srcProvince = src.getProvince();
		
		// unit-existence
		if(unit == null)
		{
			throw new OrderException( Utils.getLocalString(ORD_VAL_NOUNIT, srcProvince) );
		}
		
		// power-matching
		if( !power.equals(unit.getPower()) )
		{
			throw new OrderException( Utils.getLocalString(ORD_VAL_BADPOWER) );
		}
		
		// unit type matching
		srcUnitType = getValidatedUnitType(srcProvince, srcUnitType, unit);
		
		// Location verification; derive info if missing from unit, since 
		// we know it to exist.
		src = src.getValidatedAndDerived(srcUnitType, unit);
	}// validate()
	
	
	/** 
	*	Convenience method for matching unit types.
	*	<p>
	*	If a type is undefined, the type is derived from the existing unit. If the 
	*	existing unit is not found (or mismatched), an exception is thrown.
	*/
	protected final Unit.Type getValidatedUnitType(Province province, Unit.Type unitType, Unit unit)
	throws OrderException
	{
		if(unit == null)
		{
			throw new OrderException( Utils.getLocalString(ORD_VAL_NOUNIT, province) );
		}
		
		if(unitType.equals(Unit.Type.UNDEFINED))
		{
			// make unitType correct.
			return unit.getType();
		}
		else
		{
			if( !unitType.equals(unit.getType()) )
			{
				throw new OrderException( Utils.getLocalString(ORD_VAL_BADTYPE, province, 
						unit.getType().getFullNameWithArticle(), unitType.getFullNameWithArticle() ));
			}
		}
		
		return unitType;
	}// getValidatedUnitType()
	
	
	/** Validates the given Power */
	protected final void checkPower(Power power, TurnState turnState, boolean checkIfActive)
	throws OrderException
	{
		Position position = turnState.getPosition();
		if(position.isEliminated(power))
		{
			throw new OrderException( Utils.getLocalString(ORD_VAL_PWR_ELIMINATED, power) );
		}
		if(!power.isActive() && checkIfActive)
		{
			throw new OrderException( Utils.getLocalString(ORD_VAL_PWR_INACTIVE, power) );
		}
	}// checkPower()
	
	
	/** Convenience method to check that we are in the Retreat phase */
	protected final void checkSeasonRetreat(TurnState state, String orderName)
	throws OrderException
	{
		if( state.getPhase().getPhaseType() != Phase.PhaseType.RETREAT )
		{
			throw new OrderException(Utils.getLocalString(ORD_VAL_SZ_RETREAT, orderName));
		}
	}// checkSeasonRetreat()
	
	
	/** Convenience method to check that we are in the Adjustment phase */
	protected final void checkSeasonAdjustment(TurnState state, String orderName)
	throws OrderException
	{
		if( state.getPhase().getPhaseType() != Phase.PhaseType.ADJUSTMENT )
		{
			throw new OrderException(Utils.getLocalString(ORD_VAL_SZ_ADJUST, orderName));
		}
	}// checkSeasonAdjustment()
	
	
	/** Convenience method to check that we are in the Movement phase */
	protected final void checkSeasonMovement(TurnState state, String orderName)
	throws OrderException
	{
		if( state.getPhase().getPhaseType() != Phase.PhaseType.MOVEMENT )
		{
			throw new OrderException(Utils.getLocalString(ORD_VAL_SZ_MOVEMENT, orderName));
		}
	}// checkSeasonMovement()
	
	
	/** 
	*	Convenience Method: prints the beginning of an order in a verbose format.
	*	<br>
	*	Example: France: Army Spain/sc
	*/
	protected final void appendFull(StringBuffer sb)
	{
		sb.append(power);
		sb.append(": ");
		sb.append(srcUnitType.getFullName());
		sb.append(' ');
		src.appendFull(sb);
	}// appendFull()
	
	
	/** 
	*	Convenience Method: prints the beginning of an order in a brief format.
	*	<br>
	*	Example: France: Army spa/sc
	*/
	protected final void appendBrief(StringBuffer sb)
	{
		sb.append(power);
		sb.append(": ");
		sb.append(srcUnitType.getShortName());
		sb.append(' ');
		src.appendBrief(sb);
	}// appendBrief()
	
	
	//
	// 
	// java.lang.Object method implementations
	// 
	//
	
	/** For debugging: calls toBriefString(). Note this will fail if order is null. */
	public String toString()
	{
		return toBriefString();
	}// toString()
	
	
	/** 
	*	Determines if the orders are equal.
	*	<p>
	*	Note that full equality MUST be implemented for each 
	*	subclassed Order object! Subclasses are advised to call
	*	the super method for assistance.
	*/
	public boolean equals(Object obj)
	{
		// speedy reference check
		if(this == obj)
		{
			return true;
		}
		else if(obj instanceof Order)
		{
			Order o = (Order) obj;
			
			if(	power.equals(o.power) &&
				src.equals(o.src) &&
				srcUnitType.equals(o.srcUnitType) )
			{
				return true;
			}
		}
		
		return false;
	}// equals()
	
}// abstract class Order


