//
//  @(#)Adjudicator.java	8/2003
//
//  Copyright 2003 Zachary DelProposto. All rights reserved.
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
package dip.process;

import dip.world.TurnState;
import dip.world.Province;
import dip.world.Location;
import dip.world.Unit;
import dip.world.Power;

import dip.order.Order;
import dip.order.Support;
import dip.order.Move;
import dip.order.Convoy;

import dip.order.result.Result;
import dip.order.result.OrderResult;
import dip.order.result.BouncedResult;
import dip.order.result.DislodgedResult;
import dip.order.result.OrderResult.ResultType;

import java.util.*;


/**
*	Adjudicator interface.
*
*
*/
public interface Adjudicator
{
	// 	
	//	Basic methods, used by everyone
	// 	
	
	/** 
	*	Get the TurnState that is currently being adjudicated, and
	*	that will have its results and flags set appropriately when
	*	adjudication is complete.
	*/
	public TurnState getTurnState();
	
	/** Start adjudication. */
	public void process();
	
	/** Returns <code>true</code> if an unresolved paradox was detected. */
	public boolean isUnresolvedParadox();
	
	/** 
	*	Returns the next TurnState, or <code>null</code> 
	*	if an error occured or the game has been won. 
	*/
	public abstract TurnState getNextTurnState();
	
	/** Enable or disable reporting of failure statistics. */
	public void setStatReporting(boolean value);
	
	/**
	*	If enabled, checks to make sure that each Power's 
	*	list of orders only contains orders from that Power.
	*	This is important for networked games, to prevent
	*	illicit order injection.
	*/
	public void setPowerOrderChecking(boolean value);
	
	// 	
	//	Methods used by Orders and Adjudicator implementations
	// 	
	
	/** 
	*	Find the OrderState with the given source Province. Returns null if
	*	no corresponding order was found. <b>Note:</b> Coast is not relevent
	*	here; only the Province in the given Location is used.
	*/
	public OrderState findOrderStateBySrc(Location location);
	
	/** 
	*	Find the OrderState with the given source Province. Returns null if
	*	no corresponding order was found.
	*/
	public OrderState findOrderStateBySrc(Province src);
	
	/** Get all OrderStates */
	public OrderState[] getOrderStates();
	
	/**
	*	Returns 'true' if The Orderstate in question is a support order
	*	that is supporting a move against itself.		
	*	<ol>
	*		<li>Support order is supporting a Move
	*		<li>unit in supportedDest must be present
	*		<li>power of unit in supported dest == power of support order
	*	</ol>
	*
	*/
	public boolean isSelfSupportedMove(OrderState os);
	
	
	/**
	*	Returns a list of substituted orders. This is a list of OrderStates.
	*	Note that all OrderStates in this list will be marked "illegal". Also
	*	note that this will <b>not</b> contain 'null' substitutions (e.g., 
	*	no order was specified, and a Hold order was automatically generated).
	*/
	public List getSubstitutedOrderStates();
	
	// 	
	//	Result-adding methods
	// 	
	
	/** Add a Result to the result list */
	public void addResult(Result result);
	
	/** Add a BouncedResult to the result list */
	public void addBouncedResult(OrderState os, OrderState bouncer);
	
	/** Add a DislodgedResult to the result list */
	public void addDislodgedResult(OrderState os);
	
	/** Add a Result to the result list */
	public void addResult(OrderState os, String message);
	
	/** Add a Result to the result list */
	public void addResult(OrderState os, ResultType type, String message);
}// interface Adjudicator
