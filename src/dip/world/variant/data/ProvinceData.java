//
//  @(#)ProvinceData.java	1.00	7/2002
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




*/
public class ProvinceData
{
	private String fullName = null;
	private String[] shortNames = null;
	private String[] adj_provinces = null;
	private String[] adj_types = null;
	private boolean isConvoyableCoast = false;
	private String[] borders = null;
	
	/** Full name of Province (e.g., Mid-Atlantic Ocean) */
	public String getFullName() 				{ return fullName; }
	
	/** Short (abbreviated) name of Province; (e.g., "mao" or "mid-atlantic") */
	public String[] getShortNames()			{ return shortNames; }
	
	/** Province Adjacency array. */
	public String[] getAdjacentProvinceNames()	{ return adj_provinces; }
	
	/** Prvoince Adjacency type array. */
	public String[] getAdjacentProvinceTypes()	{ return adj_types; }
	
	/** Set full name of province. */
	public void setFullName(String value) 				{ fullName = value; }
	
	/** Set all adjacent province names. */
	public void setAdjacentProvinceNames(String[] values) 	{ adj_provinces = values; }
	
	/** Set all adjacent province types. */
	public void setAdjacentProvinceTypes(String[] values) 	{ adj_types = values; }
	
	/** Set all short (abbreviated) names, from a List. */
	public void setShortNames(List list) 				
	{ 
		shortNames = (String[]) list.toArray(new String[list.size()]); 
	}// setShortNames()
	
	/** Sets whether this Province is a convoyable coastal province. */
	public void setConvoyableCoast(boolean value) 	{ isConvoyableCoast = value; }
	
	/** Gets whether this Province is a convoyable coastal province. */
	public boolean getConvoyableCoast() 			{ return isConvoyableCoast; }
	
	/** Sets the Border ID names for this province (if any) */
	public void setBorders(List list)
	{
		borders = (String[]) list.toArray(new String[list.size()]); 
	}// setBorders()
	
	/** Gets the Border ID names for this province (if any) */
	public String[] getBorders()					{ return borders; }
	

	
	/** For debugging only! */
	public String toString()
	{
		StringBuffer sb = new StringBuffer(256);
		sb.append(this.getClass().getName());
		sb.append('[');
		sb.append("fullName=");
		sb.append(fullName);
		sb.append(",#shortNames=");
		sb.append(shortNames.length);
		sb.append(",#adj_provinces=");
		sb.append(adj_provinces.length);
		sb.append(",#adj_types=");
		sb.append(adj_types.length);
		sb.append(",isConvoyableCoast=");
		sb.append(isConvoyableCoast);
		sb.append(",#borders=");
		sb.append(borders.length);
		sb.append(']');
		return sb.toString();
	}// toString()
}// nested class ProvinceData	

