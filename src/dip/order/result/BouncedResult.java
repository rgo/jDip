//	
//	@(#)BouncedResult.java	5/2003
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
import dip.order.OrderFormat;
import dip.order.OrderFormatOptions;
import dip.world.Location;
import dip.world.Province;
import dip.misc.Utils;


/**
*	
*	Similar to an OrderResult, but allows the <b>optional</b> specification of:
*	<ul>
*		<li>the unit with which this unit bounces</li>
*		<li>the attack and defense strengths</li>
*	</ul>
*
*/
public class BouncedResult extends OrderResult
{
	// instance fields
	private Province bouncer = null;
	private int atkStrength = -1;
	private int defStrength = -1;
	
	public BouncedResult(Orderable order)
	{
		super(order, OrderResult.ResultType.FAILURE, null);
	}// BouncedResult()
	
	
	/**
	*	Returns the attack strength, or -1 if it has not
	*	been set.
	*/
	public int getAttackStrength()
	{
		return atkStrength;
	}// getAttackStrength()
	
	
	/**
	*	Returns the defense strength, or -1 if it has not
	*	been set.
	*/
	public int getDefenseStrength()
	{
		return defStrength;
	}// getDefenseStrength()
	
	
	/**
	*	Returns the location of the unit with whom we bounced,
	*	or null if it has not been set.
	*/
	public Province getBouncer()
	{
		return bouncer;
	}// getBouncer()
	
	
	/**
	*	Set the attack strength. A value of -1 indicates
	*	that this has not been set.
	*/
	public void setAttackStrength(int value)
	{
		if(value < -1)
		{
			throw new IllegalArgumentException();
		}
		
		atkStrength = value;
	}// setAttackStrength()
	
	
	/**
	*	Set the defense strength. A value of -1 indicates
	*	that this has not been set.
	*/
	public void setDefenseStrength(int value)
	{
		if(value < -1)
		{
			throw new IllegalArgumentException();
		}
		
		defStrength = value;
	}// setDefenseStrength()
	
	
	/**
	*	Set the unit which which we had bounced. 
	*	A value of <code>null</code> indicates that this 
	*	has not been set.
	*/
	public void setBouncer(Province value)
	{
		bouncer = value;
	}// setBouncer()
	
	
	/**
	*	Creates an appropriate internationalized text 
	*	message given the set and unset parameters.
	*/
	public String getMessage(OrderFormatOptions ofo)
	{
		/*
		0 : province not specified
		1 : province specified
		
		{0} : dislodge province yes/no (1/0)
		{1} : dislodge province
		{2} : atk
		{3} : def
		*/
		
		String fmtProvince = null;
		if(bouncer != null)
		{
			fmtProvince = OrderFormat.format(ofo, bouncer);
		}
		
		// create messageformat arguments
		Object[] args = 
		{
			((bouncer == null) ? new Integer(0) : new Integer(1)), 	// {0}; 0 if no province specified
			fmtProvince,											// {1}
			new Integer(atkStrength),								// {2}
			new Integer(defStrength),								// {3}
		};
		
		// return formatted message
		return Utils.getLocalString("BouncedResult.message", args);
	}// getMessage()

	
	
	/**
	*	Primarily for debugging.
	*/
	public String toString()
	{
		StringBuffer sb = new StringBuffer(256);
		sb.append(super.toString());
		
		sb.append("Bounced with: ");
		sb.append(bouncer);
		
		sb.append(". Stats: ");
		sb.append(atkStrength);
		sb.append(':');
		sb.append(defStrength);
		sb.append('.');
		
		return sb.toString();
	}// toString()
	
	
	
}// class BouncedResult
