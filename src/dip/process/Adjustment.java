//
//  @(#)Adjustment.java	1.00	4/1/2002
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
package dip.process;

import dip.world.Power;
import dip.world.Province;
import dip.world.TurnState;
import dip.world.Position;
import dip.world.Unit;
import dip.world.RuleOptions;

import java.util.List;
import java.util.Iterator;
import java.util.HashMap;




/**
*
*	Calculates Adjustments (how many units a power may build or must remove).
*	
*
*
*/
public class Adjustment
{
	
	
	private Adjustment()
	{
	}// Adjustment()
	
	/** 
	*	Determines AdjustmentInfo for a given power
	*	<p>
	*	Note that this will work during any phase. 
	*/
	public static AdjustmentInfo getAdjustmentInfo(TurnState turnState, RuleOptions ruleOpts, Power power)
	{
		if(power == null || turnState == null)
		{
			throw new IllegalArgumentException();
		}
		
		AdjustmentInfo ai = new AdjustmentInfo(ruleOpts);
		
		Position position = turnState.getPosition();
		final Province[] provinces = position.getProvinces();
		
		for(int i=0; i<provinces.length; i++)
		{
			Province province = provinces[i];
			
			// tally units
			Unit unit = position.getUnit(province);
			if( unit != null
				&& unit.getPower() == power ) 
			{
				ai.numUnits++;
			}
			
			unit = position.getDislodgedUnit(province);
			if( unit != null
				&& unit.getPower() == power ) 
			{
				ai.numDislodgedUnits++;
			}
			
			// tally supply centers
			if(power == position.getSupplyCenterOwner(province))
			{
				ai.numSC++;
				
				if(power == position.getSupplyCenterHomePower(province))
				{
					ai.numHSC++;
				}
			}
		}
		
		return ai;
	}// getAdjustmentInfo()
	
	
	/** 
	*	Determines AdjustmentInfo for all Powers;
	*	<p>
	*	Note that this will work during any phase. 
	*	<p>
	*	Results are returned as an AdjustmentInfoMap
	*/
	public static AdjustmentInfoMap getAdjustmentInfo(TurnState turnState, RuleOptions ruleOpts, Power[] powers)
	{
		if(powers == null || turnState == null)
		{
			throw new IllegalArgumentException();
		}
		
		// setup AdjustmentInfoMap
		AdjustmentInfoMap adjMap = new AdjustmentInfoMap();
		for(int i=0; i<powers.length; i++)
		{
			adjMap.put(powers[i], new AdjustmentInfo(ruleOpts));
		}
		
		// Iterate for all Powers
		Position position = turnState.getPosition();
		final Province[] provinces = position.getProvinces();
		
		for(int i=0; i<provinces.length; i++)
		{
			Province province = provinces[i];
			boolean hasUnit = false;
			
			// tally units
			Unit unit = position.getUnit(province);
			if(unit != null)
			{
				adjMap.get(unit.getPower()).numUnits++;
				hasUnit = true;
			}
			
			unit = position.getDislodgedUnit(province);
			if(unit != null)
			{
				adjMap.get(unit.getPower()).numDislodgedUnits++;
			}
			
			// tally supply centers
			Power power = position.getSupplyCenterOwner(province);
			if(power != null)
			{
				adjMap.get(power).numSC++;
				
				if(hasUnit)
				{
					adjMap.get(power).numOccSC++;
				}
				
				
				power = position.getSupplyCenterHomePower(province);
				if(power != null)
				{
					adjMap.get(power).numHSC++;
					
					if(hasUnit)
					{
						adjMap.get(power).numOccHSC++;
					}
				}
			}
		}
		
		return adjMap;
	}// getAdjustmentInfo()	
	
	
	
	/** Class containing various information about Adjustments, for a given Power */
	public static class AdjustmentInfo
	{
		// these private fields may still be set by enclosing class
		//
		private int numUnits = 0;		// # of units total
		private int numSC = 0;		// # of owned supply centers
		private int numHSC = 0;		// # of owned home supply centers
		private int numOccHSC = 0;	// # of occupied owned home supply centers
		private int numOccSC = 0;		// # of occupied non-home supply centers
		private int numDislodgedUnits = 0;	// # of dislodged units
		
		private int adj = 0;			// the adjustment amount, as determined by calculate()
		private boolean isCalc = false;	// if we have been calculated
		private RuleOptions ruleOpts; // for calculating
		
		
		/** Construct an AdjustmentInfo object */
		protected AdjustmentInfo(RuleOptions ruleOpts)
		{
			if(ruleOpts == null)
			{
				throw new IllegalArgumentException();
			}
			
			this.ruleOpts = ruleOpts;
		}// AdjustmentInfo()
		
		
		/**	# of units to adjust (+/0/-) from current unit count.
		*
		*	If units cannot be built, because supply center is occupied, 
		*	that is taken into account 
		*/
		public int getAdjustmentAmount()			
		{ 
			checkCalc(); 
			return adj;
		}// getAdjustmentAmount()
		
		/** 
		*	Force calculation of adjustment amount, and adjust supply-center
		*	information according to Build Options
		*	<p>
		*	IT IS VITAL for this method to be called after values are set, and
		*	before any values are read back.
		*	<p>
		*	There should be no effect if this method is called multiple times, 
		*	as long as RuleOptions do NOT change in between callings.
		*/
		private void calculate()
		{
			RuleOptions.OptionValue buildOpt = ruleOpts.getOptionValue(RuleOptions.OPTION_BUILDS);
			
			assert(numOccSC <= numSC);
			
			if(buildOpt == RuleOptions.VALUE_BUILDS_HOME_ONLY)
			{
				// Adjustment = number of SC gained. But, if we have gained more adjustments
				// than we have home supply centers to build on, those builds are discarded.
				// Or, if some are occupied, those builds are discarded.
				// e.g.:
				// 		3 builds, 3 empty owned home supply centers: adjustments: +3
				// 		3 builds, 2 empty owned home supply centers: adjustments: +2
				adj = numSC - numUnits;
				adj = (adj > (numHSC - numOccHSC)) ? (numHSC - numOccHSC) : adj;
			}
			else if(buildOpt == RuleOptions.VALUE_BUILDS_ANY_OWNED)
			{
				// We can build in any owned supply center. Effectively, then, 
				// ALL owned supply centers are home supply centers.
				numHSC = numSC;
				adj = (numSC - numUnits);
				adj = (adj > (numSC - numOccSC)) ? (numSC - numOccSC) : adj;
			}
			else if(buildOpt == RuleOptions.VALUE_BUILDS_ANY_IF_HOME_OWNED)
			{
				// We can build in any supply center, if at least ONE home supply
				// center is owned.
				adj = numSC - numUnits;
				adj = (adj > 0 && numHSC < 1) ? 0 : adj;
				adj = (adj > (numSC - numOccSC)) ? (numSC - numOccSC) : adj;
			}
			else
			{
				// should not occur
				throw new IllegalStateException();
			}
			
			isCalc = true;
		}// calculate()
		
		/** # of units for this power */
		public int getUnitCount()					{ checkCalc(); return numUnits; }
		
		/** # of dislodged units for this power */
		public int getDislodgedUnitCount()			{ checkCalc(); return numDislodgedUnits; }
		
		/** # of supply centers for this power (includes home supply centers) */
		public int getSupplyCenterCount()			{ checkCalc(); return numSC; }
		
		/** # of home supply centers */
		public int getHomeSupplyCenterCount()		{ checkCalc(); return numHSC; }
		
		/** mostly for debugging */
		public String toString()
		{
			StringBuffer sb = new StringBuffer(128);
			sb.append("[AdjustmentInfo: units=");
			sb.append(numUnits);
			sb.append("; supplycenters=");
			sb.append(numSC);
			sb.append("; home supplycenters=");
			sb.append(numHSC);
			sb.append("; adjustment=");
			sb.append(getAdjustmentAmount());
			sb.append(']');
			return sb.toString();
		}// toString()
		
		
		/** Checks if we have been calculated. */
		private void checkCalc()
		{
			if(!isCalc)
			{
				calculate();
			}
		}// checkCalc()
		
		
	}// nested class AdjustmentInfo
	
	
	
	/**
	*	Aggregation of HashMap that contains only AdjustmentInfo objects, 
	*	mapped by Power.
	*
	*/
	public static class AdjustmentInfoMap
	{
		private HashMap map;
		
		/** Create an AdjustmentInfoMap */
		public AdjustmentInfoMap()
		{
			map = new HashMap(13);
		}// AdjustmentInfoMap()
		
		/** Create an AdjustmentInfoMap */
		public AdjustmentInfoMap(int size)
		{
			map = new HashMap(size);
		}// AdjustmentInfoMap()
		
		/** Set AdjustmentInfo for a power. */
		private void put(Power power, AdjustmentInfo ai)
		{
			map.put(power, ai);
		}// put()
		
		/** Gets AdjustmentInfo for a power. */
		public AdjustmentInfo get(Power power)
		{
			return (AdjustmentInfo) map.get(power);
		}// get()
		
		/** Clears all information from this object. */
		public void clear()
		{
			map.clear();
		}// clear()
		
	}// nested class AdjustmentInfoMap
	
}// class Adjustment
