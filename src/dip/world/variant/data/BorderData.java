//
//  @(#)BorderData.java			11/2002
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

import java.util.List;

/**

	Temporary holder for Border data after XML parsing.


	
*/
public class BorderData
{
	private String id = null;
	private String description = null;
	private String unitTypes = null;
	private String from = null;
	private String orderTypes = null;
	private String baseMoveModifier = null;
	private String year = null;
	private String phase = null;
	private String season = null;
	
	/** Sets the Border ID */
	public void setID(String value)						{ id = value; }
	/** Sets the Border description */
	public void setDescription(String value)			{ description = value; }
	/** Sets the Border unit types  */
	public void setUnitTypes(String value)				{ unitTypes = value; }
	/** Sets the Border From locations */
	public void setFrom(String value)					{ from = value; }
	/** Sets the Border Order types  */
	public void setOrderTypes(String value)				{ orderTypes = value; }
	/** Sets the Border  Support modifier */
	public void setBaseMoveModifier(String value)		{ baseMoveModifier = value; }
	/** Sets the Border Year*/
	public void setYear(String value)					{ year = value; }
	/** Sets the Border Phase */
	public void setPhase(String value)					{ phase = value; }
	/** Sets the Border Season */
	public void setSeason(String value)					{ season = value; }
	
	/** Gets the Border ID */
	public String getID()						{ return id; }
	/** Gets the Border description */
	public String getDescription()				{ return description; }
	/** Gets the Border unit types */
	public String getUnitTypes()				{ return unitTypes; }
	/** Gets the Border From locations */
	public String getFrom()						{ return from; }
	/** Gets the Border Order types */
	public String getOrderTypes()				{ return orderTypes; }
	/** Gets the Border Support modifier */
	public String getBaseMoveModifier()			{ return baseMoveModifier; }
	/** Gets the Border Year*/
	public String getYear()						{ return year; } 
	/** Gets the Border Phase */
	public String getPhase()					{ return phase; }
	/** Gets the Border Season */
	public String getSeason()					{ return season; }
	


	
}// class BorderData
