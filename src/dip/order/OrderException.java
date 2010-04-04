/*
*  @(#)OrderException.java	1.00	4/1/2002
*
*  Copyright 2002 Zachary DelProposto. All rights reserved.
*  Use is subject to license terms.
*/
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

import dip.misc.Log;

/**
*	An OrderException indicates that an Order could not be created,
*	or contains invalid parameters.
*	<p>
*
*/
public class OrderException extends Exception implements Cloneable
{
	private Order order = null;
	
	
	/** Create an OrderException, with a Message only. */
	public OrderException(String text)
	{
		super(text);
	}// OrderException()
	
	
	/** Create an OrderException, with the given Order and Message. */
	public OrderException(Order order, String text)
	{
		super(text);
		this.order = order;
	}// OrderException()
	
	
	/** The Order that generated the Exception; null if not set. */
	public Order getOrder()
	{
		return order;
	}// getOrder()
	
	
}// class OrderException
