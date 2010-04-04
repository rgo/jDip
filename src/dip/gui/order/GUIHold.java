//
//  @(#)GUIHold.java	12/2002
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
package dip.gui.order;

import dip.order.Hold;
import dip.order.Orderable;
import dip.order.ValidationOptions;

import dip.gui.order.GUIOrder.MapInfo;

import dip.misc.Utils;
import dip.misc.Log;

import dip.world.Position;
import dip.world.Location;
import dip.world.Province;
import dip.world.Unit;
import dip.world.Power;
import dip.world.RuleOptions;

import dip.process.Adjustment.AdjustmentInfoMap;

import dip.gui.map.MapMetadata;

import java.awt.geom.Point2D;

import org.apache.batik.util.SVGConstants;
import org.apache.batik.util.CSSConstants;
import org.apache.batik.dom.svg.SVGDOMImplementation;
import org.w3c.dom.svg.*;
import org.w3c.dom.*;

/**                        
*
*	GUIOrder subclass of Hold order.
*
*
*/
public class GUIHold extends Hold implements GUIOrder
{
	// i18n keys
	
	// instance variables
	private transient final static int REQ_LOC = 1;
	private transient int currentLocNum = 0;
	private transient int numSupports = -1;	// WARNING: this will become '0' when de-serialized; not -1
	private transient Point2D.Float failPt = null;
	private transient SVGGElement group = null;
	
	
	/** Creates a GUIHold */
	protected GUIHold()
	{
		super();
	}// GUIHold()
	
	/** Creates a GUIHold */
	protected GUIHold(Power power, Location source, Unit.Type sourceUnitType)
	{
		super(power, source, sourceUnitType);
	}// GUIHold()
	
	
	/** This only accepts Hold orders. All others will throw an IllegalArgumentException. */
	public void deriveFrom(Orderable order)
	{
		if( !(order instanceof Hold) )
		{
			throw new IllegalArgumentException();
		}
		
		Hold hold = (Hold) order;
		power = hold.getPower();
		src = hold.getSource();
		srcUnitType = hold.getSourceUnitType();
		
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
		Unit unit = position.getUnit(province);
		
		if(unit != null)
		{
			if( !stateInfo.canIssueOrder(unit.getPower()) )
			{
				sb.append( Utils.getLocalString(GUIOrder.NOT_OWNER, unit.getPower()) );
				return false;
			}
			
			if( !GUIOrderUtils.checkBorder(this, new Location(province, unit.getCoast()), unit.getType(), stateInfo.getPhase(), sb) )
			{
				return false;
			}
			
			sb.append( Utils.getLocalString(GUIOrder.CLICK_TO_ISSUE, getFullName()) );
			return true;
		}
		
		// no unit in province
		sb.append( Utils.getLocalString(GUIOrder.NO_UNIT, getFullName()) );
		return false;
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
		srcUnitType = null;
		
		return true;
	}// clearLocations()
	
	public boolean setLocation(StateInfo stateInfo, Location location, StringBuffer sb)
	{
		if(testLocation(stateInfo, location, sb))
		{
			currentLocNum++;
			Unit unit = stateInfo.getPosition().getUnit(location.getProvince());
			src = new Location(location.getProvince(), unit.getCoast());
			power = unit.getPower();
			srcUnitType = unit.getType();
			
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
			Log.println("GUIHold: removeFromDOM(): group=", group);
			SVGGElement powerGroup = mapInfo.getPowerSVGGElement(power, LAYER_TYPICAL);
			GUIOrderUtils.removeChild(powerGroup, group);
			group = null;
			numSupports = -1;
		}
	}// removeFromDOM()
	
	
	public void updateDOM(MapInfo mapInfo)
	{
		Log.println("GUIHold::updateDOM(): group,support: ", group, String.valueOf(numSupports));
		
		// if we are not displayable, we exit, after remove the order (if
		// it was created)
		if( !GUIOrderUtils.isDisplayable(power, mapInfo) )
		{
			Log.println("GUIHold::updateDOM(): not displayable.");
			removeFromDOM(mapInfo);
			return;
		}
		
		// determine if any change has occured. If no change has occured,
		// we will not change the DOM.
		//
		// check supports
		int support = GUIOrderUtils.getMatchingSupportCount(mapInfo, 
								src.getProvince(), src.getProvince());
		if(numSupports == support && group != null)
		{
			Log.println("GUIHold::updateDOM(): no change. returning. calc support: ", String.valueOf(support));
			return;	// no change
		}
		
		// we are only at this point if a change has occured.
		//
		numSupports = support;
		
		// if we've not yet been created, we will create; if we've 
		// already been created, we must remove the existing elements 
		// in our group
		if(group == null)
		{
			// create group
			Log.println("GUIHold::updateDOM(): creating group.");
			group = (SVGGElement) mapInfo.getDocument().createElementNS(
								SVGDOMImplementation.SVG_NAMESPACE_URI, SVGConstants.SVG_G_TAG);
		
			mapInfo.getPowerSVGGElement(power, LAYER_TYPICAL).appendChild(group);
		}
		else
		{
			// remove group children
			Log.println("GUIHold::updateDOM(): removing group children.");
			GUIOrderUtils.deleteChildren(group);
		}
		
		// now, render the order
		//
		Log.println("GUIHold::updateDOM(): rendering order.");
		SVGElement element = null;
		
		// create hilight line
		String cssStyle = mapInfo.getMapMetadata().getOrderParamString(MapMetadata.EL_HOLD, MapMetadata.ATT_HILIGHT_CLASS);
		if(!cssStyle.equalsIgnoreCase("none"))
		{
			float offset = mapInfo.getMapMetadata().getOrderParamFloat(MapMetadata.EL_HOLD, MapMetadata.ATT_HILIGHT_OFFSET);	
			float width = GUIOrderUtils.getLineWidth(mapInfo, MapMetadata.EL_HOLD, MapMetadata.ATT_SHADOW_WIDTHS, numSupports);
			
			element = drawOrder(mapInfo, offset);
			element.setAttributeNS(null, SVGConstants.SVG_STROKE_WIDTH_ATTRIBUTE, GUIOrderUtils.floatToString(width));
			
			GUIOrderUtils.makeHilight(element, mapInfo.getMapMetadata(), MapMetadata.EL_HOLD);
			group.appendChild(element);
		}
		
		// create hold polygon
		float width = GUIOrderUtils.getLineWidth(mapInfo, MapMetadata.EL_HOLD, MapMetadata.ATT_WIDTHS, numSupports);
		
		element = drawOrder(mapInfo, 0);
		element.setAttributeNS(null, SVGConstants.SVG_STROKE_WIDTH_ATTRIBUTE, GUIOrderUtils.floatToString(width));
		
		GUIOrderUtils.makeStyled(element, mapInfo.getMapMetadata(), MapMetadata.EL_HOLD, power);
		group.appendChild(element);
		
		// draw 'failed' marker, if appropriate.
		if(!mapInfo.getTurnState().isOrderSuccessful(this))
		{
			SVGElement useElement = GUIOrderUtils.createFailedOrderSymbol(mapInfo, failPt.x, failPt.y);
			group.appendChild(useElement);
		}
	}// updateDOM()
	
	
	private SVGElement drawOrder(MapInfo mi, float offset)
	{
		MapMetadata mmd = mi.getMapMetadata();
		
		// attributes
		Point2D.Float center = mmd.getUnitPt(src.getProvince(), src.getCoast());
		float radius = mmd.getOrderRadius(MapMetadata.EL_HOLD, mi.getSymbolName(srcUnitType));
		
		// create the polygon
		SVGElement elem = 	(SVGPolygonElement) 
							mi.getDocument().createElementNS(
							SVGDOMImplementation.SVG_NAMESPACE_URI, 
							SVGConstants.SVG_POLYGON_TAG);
		
		// add offset (if any) to elements
		center.x += offset;
		center.y += offset;
		
		Point2D.Float[] pts = GUIOrderUtils.makeOctagon(center, radius);
		
		StringBuffer sb = new StringBuffer(160);
		for(int ptsIdx=0; ptsIdx<pts.length; ptsIdx++)
		{
			GUIOrderUtils.appendFloat(sb, pts[ptsIdx].x);
			sb.append(',');
			GUIOrderUtils.appendFloat(sb, pts[ptsIdx].y);
			sb.append(' ');
		}
		
		// create failure marker point, halfway between first & second points.
		failPt = GUIOrderUtils.getLineMidpoint(pts[0].x, pts[0].y, pts[1].x, pts[1].y);
		
		elem.setAttributeNS(null, SVGConstants.SVG_POINTS_ATTRIBUTE, sb.toString());
		elem.setAttributeNS(null, SVGConstants.SVG_CLASS_ATTRIBUTE, mmd.getOrderParamString(MapMetadata.EL_HOLD, MapMetadata.ATT_STROKESTYLE));
		
		return elem;
	}// drawOrder()
	
	
	/** We are dependent upon Support orders for proper rendering */
	public boolean isDependent()	{ return true; }
	
	
	
	
	
	
}// class GUIHold
