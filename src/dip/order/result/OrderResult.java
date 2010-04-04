//
// 	@(#)OrderResult.java		4/2002
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
package dip.order.result;

import dip.order.Orderable;

import java.io.Serializable;

/**
*	A message sent to a specific Power that refers to a specific order.
*	The message is classified according to ResultType (see for details).
*	<p>
*	More than one OrderResult may exist for a single order.

*
*/
public class OrderResult extends Result
{
	// instance fields
	/** The ResultType */
	protected ResultType resultType = null;
	/** The Order to which this Result refers */
	protected Orderable order = null;
	
	
	/** no-arg constructor for subclasses */
	protected OrderResult()
	{
	}// OrderResult()
	
	/** 
	*	Create an OrderResult with the given Order and Message.
	*	A null order is not permissable.
	*/
	public OrderResult(Orderable order, String message)
	{
		this(order, ResultType.TEXT, message);
	}// OrderResult()
	
	
	/** 
	*	Create an OrderResult with the given Order, ResultType, and Message.
	*	A null Order or ResultType is not permissable.
	*/
	public OrderResult(Orderable order, ResultType type, String message)
	{
		super(order.getPower(), message);
		if(type == null || order == null)
		{
			throw new IllegalArgumentException("null type or order");
		}
		
		this.resultType = type;
		this.order = order;
	}// OrderResult()
	
	/** Get the ResultType. Never returns null. */
	public ResultType getResultType()
	{
		return resultType;
	}// getResultType()
	
	/** Get the Order. Never return null. */
	public Orderable getOrder()
	{
		return order;
	}// getOrder()
	
	/** For debugging */
	public String toString()
	{
		StringBuffer sb = new StringBuffer(180);
		sb.append(power);
		sb.append(": [");
		sb.append(resultType);
		sb.append("] [order: ");
		sb.append(order);
		sb.append("] ");
		sb.append(message);
		return sb.toString();
	}// toString()
	
	
	/**
	*	Compare in the following order:
	*	<ol>
	*		<li>Power (null first)
	*		<li>Orderable source province	[null first]
	*		<li>ResultType	[never null]
	*		<li>message [never null, but may be empty]
	*	</ol>
	*	<p>
	*	If power is null, it will be first in ascending order.
	* 	If message may be empty, but never is null.
	*/
	public int compareTo(Object o)
	{
		if(o instanceof OrderResult)
		{
			OrderResult result = (OrderResult) o;
			
			// 1: compare powers
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
			
			if(compareResult != 0)
			{
				return compareResult;
			}
			
			// 2: compare Order Source province
			// null orders come first.
			if(this.order == null && result.order != null)
			{
				return -1;
			}
			else if(this.order != null && result.order == null)
			{
				return +1;
			}
			else if(this.order != null && result.order != null)
			{
				// neither are null
				compareResult = this.order.getSource().getProvince().compareTo(result.order.getSource().getProvince());
				if(compareResult != 0)
				{
					return compareResult;
				}
			}
			
			// 3: compare ResultType
			compareResult = this.resultType.compareTo(result.resultType);
			if(compareResult != 0)
			{
				return compareResult;
			}
			
			// 4: compare message 
			return this.message.compareTo(result.message);
		}
		else
		{
			return super.compareTo(o);
		}
	}// compareTo()
	
	
	
	
	
	
	/**
	*	Type-Safe enumerated categories of OrderResults.
	*	
	*	
	*/
	public static class ResultType implements Serializable, Comparable
	{
		// key constants
		private static final String KEY_VALIDATION_FAILURE = "VALIDATION_FAILURE";
		private static final String KEY_SUCCESS = "SUCCESS";
		private static final String KEY_FAILURE = "FAILURE";
		private static final String KEY_DISLODGED = "DISLODGED";
		private static final String KEY_CONVOY_PATH_TAKEN = "CONVOY_PATH_TAKEN";
		private static final String KEY_TEXT = "TEXT";
		private static final String KEY_SUBSTITUTED = "SUBSTITUTED";
		
		// enumerated constants
		/** ResultType indicating that order validation failed */
		public static final ResultType VALIDATION_FAILURE = new ResultType(KEY_VALIDATION_FAILURE, 10);
		/** ResultType indicating the order was successful */
		public static final ResultType SUCCESS = new ResultType(KEY_SUCCESS, 20);
		/** ResultType indicating the order has failed */
		public static final ResultType FAILURE = new ResultType(KEY_FAILURE, 30);
		/** ResultType indicating the order's source unit has been dislodged */
		public static final ResultType DISLODGED = new ResultType(KEY_DISLODGED, 40);
		/** ResultType indicating what convoy path a convoyed unit used */
		public static final ResultType CONVOY_PATH_TAKEN = new ResultType(KEY_CONVOY_PATH_TAKEN, 50);
		/** ResultType for a general (not otherwise specified) message */
		public static final ResultType TEXT = new ResultType(KEY_TEXT, 60); 		// text message only
		/** ResultType indicating that the order was substituted with another order */
		public static final ResultType SUBSTITUTED = new ResultType(KEY_SUBSTITUTED, 70); 		
		
		// instance variables
		private final String key;
		private final int ordering;
		
		protected ResultType(String key, int ordering)
		{
			if(key == null)
			{
				throw new IllegalArgumentException("null key");
			}
			
			this.ordering = ordering;
			this.key = key;
		}// ResultType()
		
		
		/*
			equals():
			
			We use Object.equals(), which just does a test of 
			referential equality. 
			
		*/
		
		/** For debugging: return the name */
		public String toString()
		{
			return key;
		}// toString()
		
		/** Sorts the result type */
		public int compareTo(Object obj)
		{
			ResultType rt = (ResultType) obj;
			return (ordering - rt.ordering);
		}// compareTo()
		
		
		/** Assigns serialized objects to a single constant reference */
		protected Object readResolve()
		throws java.io.ObjectStreamException
		{
			ResultType rt = null;
			
			if(key.equals(KEY_VALIDATION_FAILURE))
			{
				rt = VALIDATION_FAILURE;
			}
			else if(key.equals(KEY_SUCCESS))
			{
				rt = SUCCESS;
			}
			else if(key.equals(KEY_FAILURE))
			{
				rt = FAILURE;
			}
			else if(key.equals(KEY_DISLODGED))
			{
				rt = DISLODGED;
			}
			else if(key.equals(KEY_CONVOY_PATH_TAKEN))
			{
				rt = CONVOY_PATH_TAKEN;
			}
			else if(key.equals(KEY_TEXT))
			{
				rt = TEXT;
			}
			else if(key.equals(KEY_SUBSTITUTED))
			{
				rt = SUBSTITUTED;
			}
			else
			{
				throw new java.io.InvalidObjectException("Unknown ResultType: "+key);
			}
			
			return rt;
		}// readResolve()
	}// nested class ResultType
	
}// class OrderResult
