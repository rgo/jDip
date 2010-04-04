//
//  @(#)Variant.java	1.00	7/2002
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

import dip.world.Power;
import dip.world.Phase;
import dip.misc.Utils;

import java.util.List;

/**

A Variant.


*/
public class Variant implements Cloneable, Comparable
{
	// the arrays in general should not be null. They are defined as null initially
	// to make it more apparent should a field not be initialized properly.
	// 
	private String name = null;
	private boolean isDefault = false;
	private String description = null;
	private Power[] powers = null;
	private Phase phase = null;
	private InitialState[] istate = null;
	private SupplyCenter[] supplyCenters = null;
	private ProvinceData[] provinceData = null;
	private int vcNumSCForVictory = 0;
	private int vcMaxYearsNoSCChange = 0;
	private int vcMaxGameTimeYears = 0;
	private MapGraphic[] mapGraphics = null;
	private float version = 0.0f;
	private NameValuePair[] roNVPs = null;
	private BorderData[] borderData = null;
	private boolean allowBCYears = false;
	private String[] aliases = new String[0];
	
	/** Class of Rule Option name/value pairs */
	public static class NameValuePair
	{
		private final String name;
		private final String value;
		
		/** Create a NameValuePair. Neither name or value may be null. */
		public NameValuePair(String name, String value)
		{
			if(name == null || value == null)
			{
				throw new IllegalArgumentException();
			}
			this.name = name;
			this.value = value;
		}// NameValuePair()
		
		/** Return the Name */
		public String getName()		{ return name; }
		/** Return the Value */
		public String getValue()	{ return value; }
	}// nested class NameValuePair
	
	
	
	/** Construct a new Variant object */
	public Variant() {}
	
	/** The name of the variant. */
	public String getName() 				{ return name; }
	/** The aliases (alternate names) of the variant. Never null. */
	public String[] getAliases()			{ return aliases; }
	/** Version of this variant */
	public float getVersion() 	{ return version; }
	/** Whether this is the default variant. */
	public boolean isDefault() 				{ return isDefault; }
	/** Description for variant; this is typically HTML encoded. */
	public String getDescription()			{ return description; }
	/** The starting time. */
	public Phase getStartingPhase()			{ return phase; }
	/** The starting InitialStates. */
	public InitialState[] getInitialStates(){ return istate; }
	/** Returns Powers associated with this Variant. */
	public Power[] getPowers() 				{ return powers; }
	/** Returns SupplyCenter objects */
	public SupplyCenter[] getSupplyCenters(){ return supplyCenters; }
	/** Victory Conditions: Number of Supply Centers required for victory. */
	public int getNumSCForVictory() 		{ return vcNumSCForVictory; }
	/** Victory Conditions: Maximum years without a supply-center ownership change before game ends. */
	public int getMaxYearsNoSCChange() 		{ return vcMaxYearsNoSCChange; }
	/** Victory Conditions: Maximum game duration, in years. */
	public int getMaxGameTimeYears() 		{ return vcMaxGameTimeYears; }
	/** The mapGraphics associated with this Variant. */
	public MapGraphic[] getMapGraphics()	{ return mapGraphics; }
	/** The ProvinceData associated with this Variant */
	public ProvinceData[] getProvinceData()	{ return provinceData; }
	/** The RuleOptions (as name-value pairs) associated with this Variant */
	public NameValuePair[] getRuleOptionNVPs() 		{ return roNVPs; }
	/** Gets the BorderData associated with this Variant */
	public BorderData[] getBorderData() 	{ return borderData; }
	/** Gets if BC Years are allowed with this Variant */
	public boolean getBCYearsAllowed() 		{ return allowBCYears; }
	
	
	/** Set the variant name. */
	public void setName(String value)				{ name = value; }
	/** Set the alises. Null is not allowed. */
	public void setAliases(String[] aliases)
	{
		if(aliases == null)
		{
			throw new IllegalArgumentException();
		}
		this.aliases = aliases;
	}
	
	/** Set the version of this variant */
	public void setVersion(float value) 			{ version = value; }
	/** Set if this variant is the default variant. */
	public void setDefault(boolean value) 			{ isDefault = value; }
	/** Set the description for this variant. */
	public void setDescription(String value) 		{ description = value; }
	/** Set the starting phase for this variant. */
	public void setStartingPhase(Phase value) 		{ phase = value; }
	/** Victory Conditions: Number of Supply Centers required for victory. */
	public void setNumSCForVictory(int value) 		{ vcNumSCForVictory = value; }
	/** Victory Conditions: Maximum years without a supply-center ownership change before game ends. */
	public void setMaxYearsNoSCChange(int value) 	{ vcMaxYearsNoSCChange = value; }
	/** Victory Conditions: Maximum game duration, in years. */
	public void setMaxGameTimeYears(int value) 		{ vcMaxGameTimeYears = value; }
	/** Sets the ProvinceData associated with this Variant */
	public void setProvinceData(ProvinceData[] value)	{ provinceData = value; }
	/** Sets the BorderData associated with this Variant */
	public void setBorderData(BorderData[] value)		{ borderData = value; }
	/** Sets whether BC years (negative years) are allowed */
	public void setBCYearsAllowed(boolean value) 	{ allowBCYears = value; }
	
	
	/** Sets the MapGraphics, from a List */
	public void setMapGraphics(List mgList)
	{
		mapGraphics = (MapGraphic[]) mgList.toArray(new MapGraphic[mgList.size()]); 			
	}// setPowers()
	
	/** Sets the Powers, from a List */
	public void setPowers(List powerList)
	{
		powers = (Power[]) powerList.toArray(new Power[powerList.size()]); 			
	}// setPowers()
	
	/** Sets the InitialStates, from a List */
	public void setInitialStates(List stateList) 			
	{ 
		istate = (InitialState[]) stateList.toArray(new InitialState[stateList.size()]); 
	}// setInitialStates()
	
	/** Sets the supply centers, from a List */
	public void setSupplyCenters(List supplyCenterList) 	
	{ 
		supplyCenters = (SupplyCenter[]) supplyCenterList.toArray(new SupplyCenter[supplyCenterList.size()]); 
	}// setSupplyCenters()
	
	/** Sets the RuleOptions (as a List of name-value pairs) associated with this Variant */
	public void setRuleOptionNVPs(List nvpList)
	{
		roNVPs = (NameValuePair[])  nvpList.toArray(new NameValuePair[nvpList.size()]); 
	}// setRuleOptionNVPs()
	
	
	/** Changes the active/inactive state of a power. The number of values <b>must</b> equal the number of powers. */
	public void setActiveState(boolean[] values)
	{
		if(values.length != powers.length)
		{
			throw new IllegalArgumentException();
		}
		
		for(int i=0; i<powers.length; i++)
		{
			if(powers[i].isActive() != values[i])
			{
				// Powers are constant; we must create a new one.
				Power old = powers[i];
				powers[i] = new Power(	old.getNames(),
										old.getAdjective(),
										values[i] );
			}
		}
	}// setActiveState()
	
	
	/** 
		Compares based on Name
	*/
	public int compareTo(Object o) 
	{
		return this.getName().compareTo( ((Variant) o).getName() );
	}// compareTo()
	
	
	/** Finds the MapGraphic by name; case insensitive. */
	public MapGraphic getMapGrapic(String mgName)
	{
		if(mapGraphics != null)
		{
			for(int i=0; i<mapGraphics.length; i++)
			{
				if(mapGraphics[i].getName().equalsIgnoreCase(mgName))
				{
					return mapGraphics[i];
				}
			}
		}
		return null;
	}// getVariant()
	
	
	/** Gets the default MapGraphic; if there is no default, returns the first one. */
	public MapGraphic getDefaultMapGraphic()
	{
		MapGraphic mg = null;
		
		if(mapGraphics != null && mapGraphics.length > 0)
		{
			mg = mapGraphics[0];
			
			for(int i=0; i<mapGraphics.length; i++)
			{
				if(mapGraphics[i].isDefault())
				{
					mg = mapGraphics[i];
					break;
				}
			}
		}
		
		return mg;
	}// getDefaultMapGraphic()
	
	
	
	/** 
	*	Gets the arguments for an HTML description, suitable for insertion 
	*	inside an appropriately-marked HTML template (arguments are 
	*	surrounded by curly braces).
	*	<p>
	*	Arguments for the HTML template are:
	*	<ol>
	*		<li>Variant name</li>
	*		<li>variant-description (note: may be in html)</li>
	*		<li>Supply centers for victory</li>
	*		<li>Starting season</li>
	*		<li>Starting year</li>
	*		<li>Starting phase</li>
	*		<li>Powers (comma-separated list)</li>
	*		<li>Number of Powers</li>
	* 	</ol>
	*	8 arguments are given in total.
	*/
	public Object[] getHTMLSummaryArguments()
	{
		Object args[] = new Object[8];
		args[0] = getName();
		args[1] = getDescription();
		args[2] = String.valueOf(getNumSCForVictory());
		if(getStartingPhase() == null)
		{
			args[3] = "{bad phase}";
			args[4] = "{bad phase}";
			args[5] = "{bad phase}";
		}
		else
		{
			args[3] = getStartingPhase().getSeasonType();
			args[4] = getStartingPhase().getYearType();
			args[5] = getStartingPhase().getPhaseType();
		}
		
		// create list of powers
		StringBuffer sb = new StringBuffer(512);
		for(int i=0; i<powers.length; i++)
		{
			if(powers[i].isActive())
			{
				sb.append(powers[i].getName());
			}
			else
			{
				sb.append('(');
				sb.append(powers[i].getName());
				sb.append(')');
			}
			
			if(i < (powers.length -1))
			{
				sb.append(", ");
			}
		}
		args[6] = sb.toString();
		args[7] = String.valueOf(powers.length);
		
		return args;
	}// getHTMLSummaryArguments()
	
	
	/** Creates a deep clone of all data EXCEPT InitialState / SupplyCenter data / Name / Description */
	public Object clone()
	throws CloneNotSupportedException
	{
		// shallow clone
		Variant variant = (Variant) super.clone();
		
		// deep clone
		// 
		// phase
		if(this.phase != null)
		{
			// cheap...
			variant.phase = Phase.parse(this.phase.toString());
		}
		
		// powers
		if(this.powers != null)
		{
			variant.powers = new Power[powers.length];
			for(int i=0; i<powers.length; i++)
			{
				Power thisPower = powers[i];
				variant.powers[i] = new Power(	thisPower.getNames(),
												thisPower.getAdjective(),
												thisPower.isActive() );
			}
		}
		
		return variant;
	}// clone()
	
	/** For debugging only! */
	public String toString()
	{
		StringBuffer sb = new StringBuffer(256);
		sb.append(this.getClass().getName());
		sb.append('[');
		sb.append("name=");
		sb.append(name);
		sb.append(",isDefault=");
		sb.append(isDefault);
		sb.append("powers=");
		for(int i=0; i<powers.length; i++)
		{
			sb.append(powers[i]);
			sb.append(',');
		}
		sb.append(",phase=");
		sb.append(phase);
		sb.append(",istate=");
		for(int i=0; i<istate.length; i++)
		{
			System.out.println(istate[i]);
		}
		sb.append(",supplyCenters=");
		for(int i=0; i<supplyCenters.length; i++)
		{
			System.out.println(supplyCenters[i]);
		}
		sb.append(",provinceData=");
		for(int i=0; i<provinceData.length; i++)
		{
			System.out.println(provinceData[i]);
		}
		sb.append("mapGraphics=");
		for(int i=0; i<mapGraphics.length; i++)
		{
			System.out.println(mapGraphics[i]);
		}
		sb.append(",vcNumSCForVictory=");
		sb.append(vcNumSCForVictory);
		sb.append(",vcMaxGameTimeYears=");
		sb.append(vcMaxGameTimeYears);
		sb.append(",vcMaxYearsNoSCChange=");
		sb.append(vcMaxYearsNoSCChange);
		sb.append(",version=");
		sb.append(version);
		sb.append(']');
		return sb.toString();
	}// toString()
}// class Variant


