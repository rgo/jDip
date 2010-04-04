//	
//	@(#)SubstitutedResult.java	5/2003
//	
//	Copyright 2003 Zachary DelProposto. All rights reserved.
//	Use is subject to license terms.
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
package dip.order.result;

import dip.order.Orderable;

/**
*	If an adjudicator replaces an order (because it is invalid, for
*	example) or creates an order (because no order was given), a
*	SubstitutedResult is created.
*	<p>
*	<code>getOrder()</code> may return <code>null</code>
*	if an order was added, and no previous order existed. However, 
*	<code>getPower()</code> will not; the power is set from the 
*	new order.
*	
*/
public class SubstitutedResult extends OrderResult
{
	private Orderable newOrder = null;
	
	
	/** 
	*	Create a SubstitutedResult. Note that oldOrder may be null, but
	*	newOrder is not allowed to be null.
	*/
	public SubstitutedResult(Orderable oldOrder, Orderable newOrder, String message)
	{
		super();
		if(newOrder == null)
		{
			throw new IllegalArgumentException();
		}
		
		// keep message inline with general contract of dip.order.Result
		if(message != null)
		{
			this.message = message;
		}
		
		this.order = oldOrder;
		this.resultType = ResultType.SUBSTITUTED;
		this.power = newOrder.getPower();
		this.newOrder = newOrder;
	}// SubstitutedResult()
	
	
	/**
	*	Returns the substituted (new) order that replaces the 
	*	old order (or no order, if an order was created).
	*/
	public Orderable getSubstitutedOrder()
	{
		return newOrder;
	}// getSubstitutedOrder()
	
	/** This is intended for debugging only. */
	public String toString()
	{
		StringBuffer sb = new StringBuffer(256);
		sb.append(power);
		sb.append(": [");
		sb.append(resultType);
		sb.append("] [order: ");
		sb.append(order);
		sb.append("] [new order: ");
		sb.append(newOrder);
		sb.append("] ");
		sb.append(message);
		return sb.toString();
	}// toString()
}// class SubstitutedResult

