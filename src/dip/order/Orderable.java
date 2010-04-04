//
//  @(#)Order.java	12/2002
//
//  Copyright 2002 Zachary DelProposto. All rights reserved.
//  Use is subject to license terms.
//
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
//  Or from http://www.gnu.org/
//

package dip.order;

import dip.world.Location;
import dip.world.Unit;
import dip.world.TurnState;
import dip.world.Power;
import dip.world.RuleOptions;
import dip.process.Adjudicator;

/**
 *  All Order objects must implement this interface.
 *	<p>
 *	All classes that use Orders must use Orderable objects, rather than
 *	Order objects, for compatibility with the OrderFactory system.
 * 	<p>
 *	Please note that the Order class provides default implementations
 *	for some of these methods, and a number of protected internal methods
 *	for convenience. 
 *
 *
 *
 */
public interface Orderable 
{
	// 
	// Basic Order Information
	// 
	/** Gets the Location of the ordered unit */
	public Location getSource();
	
	/** Gets the Type of the ordered unit */
	public Unit.Type getSourceUnitType();
	
	/** Gets the Power ordering the ordered Source unit */
	public Power getPower();
	
	
	// 
	// Order Name output
	// 
	/** Returns the Full name of the Order (e.g., "Hold" for a Hold order) */
	public String getFullName();
	
	/** Returns the Brief name of the Order (e.g., "H" for a Hold order) */ 
	public String getBriefName();
	
	/** Prints the entire order, in a brief syntax */
	public String toBriefString();
	
	/** Prints the entire order, in a verbose syntax */
	public String toFullString();
	
	/** Get the default OrderFormat String used to custom-format orders. */
	public String getDefaultFormat();
	
	/** 
	*	Formats the order using the OrderFormat format string.
	*	<p>
	*	Note that this is equivalent to calling <code>OrderFormat.format(ofo, this, getDefaultFormat())</code>
	*	To print with default (not user-specified) options, use 
	*	<code>OrderFormat.format(OrderFormatOptions.DEFAULT, this, getDefaultFormat())</code>
	*	<p>
	*	While this enables better formatting, it is slower than using the toBriefString() and toFullString() 
	*	methods.
	*/	
	public String toFormattedString(OrderFormatOptions ofo);
	
	
	
	// 
	// Adjudication
	// 
	/**
	 * Validate the order; state-independent.
	 * <p>
	 * This checks the order for legality. An order is not considered to be 
	 * well-formed until it has been validated.
	 * It validates locations (at the least) and other order
	 * syntax as appropriate, throwing exceptions if there is a conflict.
	 * <p>
	 * This is used by the UI to flag bad orders, as well as by the adjudicator
	 * to eliminate bad orders before processing.
	 * <p>
	 * Important Note:
	 * <p>
	 * The only state that should be assumed is the following:
	 *	<ol>
	 * 		<li>orders for this power (if entered)
	 *		<li>the positions of all units.
	 *	</ol>
	 * Thus, the orders of units not controlled by this power may
	 * not be known (in multiplayer games, this information would not
	 * be available until adjudication took place). DO NOT assume or attempt 
	 * to discern the orders for any other power or other orders for this power.
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
	 *@param state               Current turn state
	 *@param valOpts			 Current validation options
	 *@exception OrderException  
	 */
	public void validate(TurnState state, ValidationOptions valOpts, RuleOptions ruleOpts)
	throws OrderException;
	
	/**
	*	Ensure that the order can be processed, assuming the complete turn state is known
	*	(i.e., all unit positions AND all orders for all powers). This is state-dependent.
	*	<p>
	*	For example, this is useful for:
	*	<ol>
	*		<li>Checking if support orders correctly match a move order
	*		<li>Checking if convoy orders correctly match a move order
	*		<li>etc.
	*	</ol>
	*	<br>
	*	This method should have no side effects, and should have same results if 
	*	called multiple times
	*	<p>
	*	By default, this method does nothing.<br> 
	*	DO NOT call validate() within this method; there is no need! The Adjudicator must validate()
	*	all orders prior to verification.
	*	<p>
	*	Remember, an order that fails validation is an illegal order (and usually converts to a Hold)
	*	while an order that fails verification is just a failed order (e.g., a power supports
	*	another power's unit move, but the other power's unit is ordered to hold), NOT an illegal
	*	order. 
	*	<p>
	*	THIS is called BEFORE dependency information is available for the order.
	*	so do not assume that dependent order information is available.
	*	<p>
	*	This method should NEVER throw checked exceptions (e.g., an OrderException)
	*/
	public void verify(Adjudicator adjudicator);
	
	/**
	*	This method is called once all orders are known. It determines
	*	what, if any, orders this order requires to be evaluated in order
	*	for this order to be successfully evaluated
	*	<p>	
	*	This is typically only used by orders generated in the Movement
	*	phase.
	*
	*/
	public void determineDependencies(Adjudicator adjudicator);
	
	/** 
	*	Evaluates an Order.
	*	<p>
	*	When an order is evaluated, it must change its OrderState. The OrderState cannot
	*	be changed once it is set. If an OrderState is uncertain, then it may be re-evaluated
	*	until the OrderState is certain (either Success or Failure).
	*	<p>
	*	<b>NOTE:</b> this method assumes that all dependency information is complete.
	*/
	public void evaluate(Adjudicator adjudicator);
	
}// interface Order

