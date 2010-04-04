/*
*  @(#)Result.java	1.00	4/1/2002
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
package dip.order.result;


import dip.order.OrderFormat;
import dip.order.OrderFormatOptions;
import dip.world.Power;
import dip.misc.Utils;

import java.io.Serializable;
import java.util.Date;

/**
*		TimeResult<p> 
*		Timestamp result, with an optional message.
*		Time is always in UTC.
*/
public class TimeResult extends Result
{
	// instance variables
	private final long timeStamp;	// milliseconds since midnight, January 1, 1970 UTC.
	
	
	/** A TimeStamped result, applicable to a particular power. 
	*	<p>
	*	Note that resource must correspond to an il8n resource!
	*
	*/
	public TimeResult(Power power, String resource)
	{
		super(power, resource);
		
		// create timestamp
		timeStamp = System.currentTimeMillis();
	}// Result()
	
	
	/** A TimeStamped result, applicable to all powers. */
	public TimeResult(String resource)
	{
		this(null, resource);
	}// Result()
	
	
	/** Get the milliseconds since midnight, January 1, 1970 UTC. */
	public long getGMTMillis()
	{
		return timeStamp;
	}// getGMTMillis()
	
	
	/** 
	*	Converts the Resource to a properly-internationlized text message.
	*	argument {0} is always the time. 
	*/
	public String getMessage(OrderFormatOptions ofo)
	{
		return Utils.getLocalString(message, new Date(timeStamp));
	}// getMessage()
	
	
	/** Convert the output to a String */
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
		sb.append(getMessage());
		return sb.toString();
	}// toString()
	
	
}// class TimeResult
