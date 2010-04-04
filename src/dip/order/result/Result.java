//	
//	@(#)Result.java		4/2002
//	
//	Copyright 2002 Zachary DelProposto. All rights reserved.
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

import dip.order.OrderFormat;
import dip.order.OrderFormatOptions;
import dip.world.Power;

import java.io.Serializable;

/**
*		A Result is a message that is sent from the adjudicator back to a power
*		or all powers concerning turn processing.
*		<p>
*		Result and subclasses have a toString() method, which is intended
*		for debugging. To obtain a properly-formatted localized message, use
*		getMessage().
*/
public class Result extends Object implements Serializable, Comparable
{
	// constants
	private static final OrderFormatOptions DEFAULT_OFO = OrderFormatOptions.createDefault();
	
	// instance variables
	/** The Power to whom this Result applies; null if it applies to everyone */
	protected Power power = null;
	/** The Message text; this must <b>never</b> be null */
	protected String message = "";	// message is never null
	
	/** no-arg constructor for subclasses */
	protected Result()
	{
	}// Result()
	
	/** 
	*	Create a Result, that is for the given Power. 
	*	A null Power indicates the result applies to
	*	all Powers.
	*/
	public Result(Power power, String message)
	{
		this.power = power;
		
		if(message != null)
		{
			this.message = message;
		}
	}// Result()
	
	
	/**
	*	Create a Result that is applicable to all
	*	Powers.
	*/
	public Result(String message)
	{
		this(null, message);
	}// Result()
	
	/** Get the Power (or null if none) for whom this result is intended. */
	public Power getPower() 			{ return power; }
	
	
	/** 
	*	Get the message. Never returns null. This is equivalent to calling
	*	<code>getMessage(OrderFormatOptions.DEFAULT)</code>.
	*	<p>
	*	This method is marked <code>final</code> so that subclasses more
	*	properly override the <code>getMessage(OrderFormatOptions)</code>
	*	method.
	*/
	public final String getMessage()
	{ 
		return getMessage(DEFAULT_OFO); 
	}// getMessage()
	
	/** 
	*	Get the message. <b>Never</b> returns <code>null</code>. 
	*	<p>
	*	Uses the given order format
	*	options (if applicable) for formatting Province and Order names.
	*	Subclasses must override this method to implement this.
	*/
	public String getMessage(OrderFormatOptions ofo)
	{
		return message;
	}// getMessage()
	
	/** For debugging. Use getPower() and getMessage() for general use. */
	public String toString()
	{
		StringBuffer sb = new StringBuffer(128);
		
		if(power == null)
		{
			sb.append("(none)");
		}
		else
		{
			sb.append(power);
		}
		
		sb.append(": ");
		sb.append(message);
		return sb.toString();
	}// toString()
	
	
	/**
	*	Compare first by Power, then by Message.
	*	<p>
	*	If power is null, it will be first in ascending order.
	* 	If message may be empty, but never is null.
	*/
	public int compareTo(Object o)
	{
		Result result = (Result) o;
		
		// first: compare powers
		int compareResult = 0;
		if(result.power == null && this.power == null)
		{
			compareResult = 0;
		}
		else if(this.power == null && result.power != null)
		{
			return -1;
		}
		else if(this.power != null && result.power == null)
		{
			return +1;
		}
		else
		{
			// if these are equal, could be 0
			compareResult = this.power.compareTo(result.power);
		}
		
		// finally: compare messages
		return ((compareResult != 0) ? compareResult : message.compareTo(result.message));
	}// compareTo()
	
	
}// class Result
