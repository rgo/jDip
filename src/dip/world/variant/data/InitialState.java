//
//  @(#)InitialState.java	1.00	7/2002
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
package dip.world.variant.data;

import dip.world.Unit;
import dip.world.Coast;

/**

Sets the Initial State (position) for a province.
<p>



*/	
public class InitialState
{
	private String provinceName = null;
	private String power = null;
	private Unit.Type unit = null;
	private Coast coast = null;
	
	/** Name of province to which this InitialState refers. */
	public String getProvinceName() 	{ return provinceName; }
	
	/** Power of unit owner */
	public String getPowerName() 		{ return power; }
	
	/** Type of unit */
	public Unit.Type getUnitType() 		{ return unit; }
	
	/** Coast of unit */
	public Coast getCoast() 		{ return coast; }
	
	/** Set the Province name */
	public void setProvinceName(String value) 	{ provinceName = value; }
	
	/** Set the Power name */
	public void setPowerName(String value) 		{ power = value; }
	
	/** Sets the unit type. */
	public void setUnitType(Unit.Type value) 	{ unit = value; }
	
	/** Sets the coast for the unit. */
	public void setCoast(Coast value) 		{ coast = value; }
	
	
	/** For debugging only! */
	public String toString()
	{
		StringBuffer sb = new StringBuffer(256);
		sb.append(this.getClass().getName());
		sb.append('[');
		sb.append("provinceName=");
		sb.append(provinceName);
		sb.append(",power=");
		sb.append(power);
		sb.append(",unit=");
		sb.append(unit);
		sb.append(",coast=");
		sb.append(coast);
		sb.append(']');
		return sb.toString();
	}// toString()
}// nested class InitialState

