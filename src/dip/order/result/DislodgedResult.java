//	
//	@(#)DislodgedResult.java	5/2003
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
*		<li>valid Locations to which a unit may retreat (if any)</li>
*		<li>the unit that dislodged this unit</li>
*		<li>the attack and defense strengths</li>
*	</ul>
*
*/
public class DislodgedResult extends OrderResult
{
	// instance fields
	private Location[] retreatLocations = null; 
	private Province dislodger = null;
	private int atkStrength = -1;
	private int defStrength = -1;
	
	
	public DislodgedResult(Orderable order, Location[] retreatLocations)
	{
		this(order, null, retreatLocations);
	}// DislodgedResult()
	
	
	public DislodgedResult(Orderable order, String message, Location[] retreatLocations)
	{
		super();
		if(order == null)
		{
			throw new IllegalArgumentException("null order");
		}
		
		this.power = order.getPower();
		this.message = message;
		this.order = order;
		this.resultType = OrderResult.ResultType.DISLODGED;
		this.retreatLocations = retreatLocations;
	}// DislodgedResult()
	
	
	/**
	*	Returns the valid retreat locations, if set. If no retreat
	*	locations have been defined, this will return null.
	*/
	public Location[] getRetreatLocations()
	{
		return retreatLocations;
	}// getRetreatLocations()
	
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
	*	Returns the dislodging units location, or null if it has not
	*	been set.
	*/
	public Province getDislodger()
	{
		return dislodger;
	}// getDislodger()
	
	
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
	*	Set the dislodger. A value of <code>null</code>
	*	indicates that this has not been set.
	*/
	public void setDislodger(Province value)
	{
		dislodger = value;
	}// setDislodger()
	
	
	/**
	*	Creates an appropriate internationalized text message given the 
	*	set and unset parameters.
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
		{4} : retreats number (-1, 0, or >0)
		{5} : retreats (comma-separated)
		*/
		
		// create formated dislodged present (if any)
		String fmtDislodger = null;
		if(dislodger != null)
		{
			fmtDislodger = OrderFormat.format(ofo, dislodger);
		}
		
		// create retreat list
		StringBuffer retreats = new StringBuffer(128);
		if(retreatLocations != null)
		{
			for(int i=0; i<retreatLocations.length; i++)
			{
				retreats.append(' ');
				
				retreats.append( OrderFormat.format(ofo, retreatLocations[i]) );
				
				if(i < (retreatLocations.length-1))
				{
					retreats.append(',');
				}
			}
		}
		
		// create messageformat arguments
		Object[] args = 
		{
			((dislodger == null) ? new Integer(0) : new Integer(1)), 	// {0}; 0 if no province specified
			fmtDislodger,												// {1}
			new Integer(atkStrength),									// {2}
			new Integer(defStrength),									// {3}
			((retreatLocations == null) ? new Integer(-1) : new Integer(retreatLocations.length)),  // {4} 
			retreats.toString() // {5}
		};
		
		// return formatted message
		return Utils.getLocalString("DislodgedResult.message", args);
	}// getMessage()
	
	
	/**
	*	Primarily for debugging.
	*/
	public String toString()
	{
		StringBuffer sb = new StringBuffer(256);
		sb.append(super.toString());
		
		// add retreats
		sb.append(" retreat locations:");
		if(retreatLocations == null)
		{
			sb.append(" null");
		}
		else if(retreatLocations.length == 0)
		{
			sb.append(" none");
		}
		else
		{
			for(int i=0; i<retreatLocations.length; i++)
			{
				sb.append(' ');
				retreatLocations[i].appendBrief(sb);
			}
		}
		
		// add dislodged info
		sb.append(". Dislodged from ");
		sb.append(dislodger);
		sb.append(' ');
		sb.append(atkStrength);
		sb.append(':');
		sb.append(defStrength);
		sb.append('.');
		
		return sb.toString();
	}// toString()
	
	
	
}// class DislodgedResult
