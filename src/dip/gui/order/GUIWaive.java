//
//  @(#)GUIWaive.java	12/2003
//
//  Copyright 2003 Zachary DelProposto. All rights reserved.
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
package dip.gui.order;

import dip.order.Waive;
import dip.order.Orderable;
import dip.order.ValidationOptions;

import dip.gui.order.GUIOrder.MapInfo;

import dip.misc.Utils;

import dip.world.Position;
import dip.world.Location;
import dip.world.Province;
import dip.world.Unit;
import dip.world.Power;
import dip.world.RuleOptions;

import dip.process.Adjustment.AdjustmentInfoMap;
import dip.process.Adjustment;

import java.awt.geom.Point2D;

import org.apache.batik.util.SVGConstants;
import org.apache.batik.util.CSSConstants;
import org.apache.batik.dom.svg.SVGDOMImplementation;
import org.w3c.dom.svg.*;

import dip.gui.map.MapMetadata;
import dip.gui.map.DefaultMapRenderer2;
import dip.gui.map.SVGUtils;

import java.awt.geom.Point2D;

import org.apache.batik.util.SVGConstants;
import org.apache.batik.util.CSSConstants;
import org.apache.batik.dom.svg.SVGDOMImplementation;
import org.apache.batik.dom.util.XLinkSupport;
import org.w3c.dom.svg.*;
import org.w3c.dom.*;

/**
*	GUIOrder implementation of the Waive order.
*	
*
*/
public class GUIWaive extends Waive implements GUIOrder
{
	
	// i18n keys
	private static final String NOWAIVE_MUST_BE_AN_OWNED_SC = "GUIWaive.bad.must_own_sc";
	private static final String NOWAIVE_NOT_OWNED_HOME_SC	= "GUIWaive.bad.now_owned_home_sc";
	private static final String NOWAIVE_NEED_ONE_OWNED_SC 	= "GUIWaive.bad.need_one_owned_sc";
	private static final String NOWAIVE_NO_BUILDS_AVAILABLE = "GUIWaive.bad.no_builds_available";
	private static final String NOWAIVE_SC_NOT_CONTROLLED 	= "GUIWaive.bad.sc_not_controlled";
	private static final String NOWAIVE_UNIT_PRESENT 		= "GUIWaive.bad.unit_already_present";
	private static final String NOWAIVE_UNOWNED_SC 			= "GUIWaive.bad.unowned_sc";
	
	// instance variables
	private transient final static int REQ_LOC = 1;
	private transient int currentLocNum = 0;
	private transient Point2D.Float failPt = null;
	private transient SVGGElement group = null;
	
	/** Creates a GUIWaive */
	protected GUIWaive()
	{
		super();
	}// GUIWaive()
	
	/** Creates a GUIWaive */
	protected GUIWaive(Power power, Location source)
	{
		super(power, source);
	}// GUIWaive()
	
	
	/** This only accepts Waive orders. All others will throw an IllegalArgumentException. */
	public void deriveFrom(Orderable order)
	{
		if( !(order instanceof Waive) )
		{
			throw new IllegalArgumentException();
		}
		
		Waive waive = (Waive) order;
		power = waive.getPower();
		src = waive.getSource();
		srcUnitType = waive.getSourceUnitType();
		
		// set completed
		currentLocNum = REQ_LOC;
	}// deriveFrom()
	
	
	public boolean testLocation(StateInfo stateInfo, Location location, StringBuffer sb)
	{
		sb.setLength(0);
		
		if(isComplete())
		{
			sb.append( Utils.getLocalString(GUIOrder.COMPLETE, getFullName()) );
			return false;
		}
		
		
		Position position = stateInfo.getPosition();
		Province province = location.getProvince();
		
		if(province.hasSupplyCenter())
		{
			Power SCOwner = position.getSupplyCenterOwner(province);
			
			// general screening, applicable to all build options
			//
			if(SCOwner == null)
			{
				sb.append( Utils.getLocalString(NOWAIVE_UNOWNED_SC) );
				return false;
			}
			
			if(position.hasUnit(province))
			{
				sb.append( Utils.getLocalString(NOWAIVE_UNIT_PRESENT) );
				return false;
			}
			
			if( !stateInfo.canIssueOrder(SCOwner) )
			{
				sb.append( Utils.getLocalString(NOWAIVE_SC_NOT_CONTROLLED) );
				return false;
			}
			
			// indicate if we have no builds available
			//
			Adjustment.AdjustmentInfo adjInfo = stateInfo.getAdjustmenInfoMap().get(SCOwner);
			if(adjInfo.getAdjustmentAmount() <= 0)
			{
				sb.append( Utils.getLocalString(NOWAIVE_NO_BUILDS_AVAILABLE, SCOwner.getName()) );
				return false;
			}
			
			
			// build-option-specific, based upon RuleOptions
			//
			RuleOptions ruleOpts = stateInfo.getRuleOptions();
			if(ruleOpts.getOptionValue(RuleOptions.OPTION_BUILDS) == RuleOptions.VALUE_BUILDS_ANY_OWNED)
			{
				sb.append( Utils.getLocalString(GUIOrder.COMPLETE, getFullName()) );
				return true;
			}
			else if(ruleOpts.getOptionValue(RuleOptions.OPTION_BUILDS) == RuleOptions.VALUE_BUILDS_ANY_IF_HOME_OWNED)
			{
				// check if we have ONE owned home supply center before buidling
				// in a non-home supply center.
				//
				if( SCOwner != position.getSupplyCenterHomePower(province)
					&& !position.hasAnOwnedHomeSC(SCOwner) )
				{
					sb.append( Utils.getLocalString(NOWAIVE_NEED_ONE_OWNED_SC) );
					return false;	// failed
				}
				
				sb.append( Utils.getLocalString(GUIOrder.COMPLETE, getFullName()) );
				return true;
			}
			else	
			{
				// build only in owned HOME supply centers
				//
				if(SCOwner == position.getSupplyCenterHomePower(province))
				{
					sb.append( Utils.getLocalString(GUIOrder.COMPLETE, getFullName()) );
					return true;
				}
				
				// build failure.
				sb.append( Utils.getLocalString(NOWAIVE_NOT_OWNED_HOME_SC) );
				return false;
			}
		}
		else
		{
			sb.append( Utils.getLocalString(NOWAIVE_MUST_BE_AN_OWNED_SC) );
			return false;
		}
		
		// NO return here: thus we must appropriately exit within an if/else block above.
	}// testLocation()
	
	
	public boolean clearLocations()
	{
		if(isComplete())
		{
			return false;
		}
		
		currentLocNum = 0;
		power = null;
		src = null;
		//srcUnitType: not cleared
		
		return true;
	}// clearLocations()
	
	
	
	public boolean setLocation(StateInfo stateInfo, Location location, StringBuffer sb)
	{
		if(testLocation(stateInfo, location, sb))
		{
			currentLocNum++;
			
			src = new Location(location.getProvince(), location.getCoast());
			power = stateInfo.getPosition().getSupplyCenterOwner(location.getProvince());
			
			// srcUnitType: already defined
			assert (srcUnitType != null);
			
			sb.setLength(0);
			sb.append( Utils.getLocalString(GUIOrder.COMPLETE, getFullName()) );
			return true;
		}
		
		return false;
	}// setLocation()
	
	
	public boolean isComplete()
	{
		assert (currentLocNum <= getNumRequiredLocations());
		return (currentLocNum == getNumRequiredLocations());
	}// isComplete()
	
	public int getNumRequiredLocations()		{ return REQ_LOC; }
	
	public int getCurrentLocationNum()			{ return currentLocNum; }
	
	/** Always throws an IllegalArgumentException */
	public void setParam(Parameter param, Object value)	{ throw new IllegalArgumentException(); }
	
	/** Always throws an IllegalArgumentException */
	public Object getParam(Parameter param)	{ throw new IllegalArgumentException(); }
	
	public void removeFromDOM(MapInfo mapInfo)
	{
		if(group != null)
		{
			SVGGElement powerGroup = mapInfo.getPowerSVGGElement(power, LAYER_HIGHEST);
			GUIOrderUtils.removeChild(powerGroup, group);
			group = null;
		}
	}// removeFromDOM()
	
	
	/** Places a unit in the desired area. */
	public void updateDOM(MapInfo mapInfo)
	{
		// if we are not displayable, we exit, after remove the order (if
		// it was created)
		if( !GUIOrderUtils.isDisplayable(power, mapInfo) )
		{
			removeFromDOM(mapInfo);
			return;
		}
		
		// determine if any change has occured. If no change has occured,
		// we will not change the DOM.
		//
		// we have nothing (yet) to check for change; isDependent() == false.
		// so just return if we have not been drawn.
		if(group != null)
		{
			return;
		}
		
		// there has been a change, if we are at this point.
		// 
		
		// if we've not yet been created, we will create; if we've 
		// already been created, we must remove the existing elements 
		// in our group
		if(group == null)
		{
			// create group
			group = (SVGGElement) mapInfo.getDocument().createElementNS(
								SVGDOMImplementation.SVG_NAMESPACE_URI, SVGConstants.SVG_G_TAG);
		
			mapInfo.getPowerSVGGElement(power, LAYER_HIGHEST).appendChild(group);
		}
		else
		{
			// remove group children
			GUIOrderUtils.deleteChildren(group);
		}
		
		// now, render the order
		// (no highlight + shadow required here)
		// no offset required
		//
		group.appendChild(drawOrder(mapInfo));
		
		// draw 'failed' marker, if appropriate.
		if(!mapInfo.getTurnState().isOrderSuccessful(this))
		{
			SVGElement useElement = GUIOrderUtils.createFailedOrderSymbol(mapInfo, failPt.x, failPt.y);
			group.appendChild(useElement);
		}
	}// updateDOM()
	
	
	private SVGElement drawOrder(MapInfo mapInfo)
	{
		MapMetadata mmd = mapInfo.getMapMetadata();
		
		// center point is over the Supply Center -- not over the 'unit' or 'dislodged unit' position.
		Point2D.Float center = mmd.getSCPt(src.getProvince());
		
		// get 'integer' and float data
		float radius = mmd.getOrderRadius(MapMetadata.EL_WAIVE, DefaultMapRenderer2.SYMBOL_WAIVEDBUILD);
		
		// calculate failPt. 
		failPt = new Point2D.Float(center.x + radius, center.y - radius);
		
		
		// A Waive consists of a WaivedBuidl symbol
		//
		MapMetadata.SymbolSize symbolSize = mmd.getSymbolSize(DefaultMapRenderer2.SYMBOL_WAIVEDBUILD);
		
		SVGElement element = SVGUtils.createUseElement(
			mapInfo.getDocument(), 
			"#"+DefaultMapRenderer2.SYMBOL_WAIVEDBUILD,
			null,	// no ID
			null,	// no special style 
			center.x,
			center.y,
			symbolSize);
		
		return element;
	}// drawOrder()	
	
	
	public boolean isDependent()	{ return false; }
	
}// class GUIWaive
