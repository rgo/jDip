//
//  @(#)GUIConvoy.java		12/2002
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

import dip.order.Orderable;
import dip.order.Convoy;
import dip.order.ValidationOptions;

import dip.misc.Utils;

import dip.world.Position;
import dip.world.Location;
import dip.world.Province;
import dip.world.Coast;
import dip.world.Path;
import dip.world.Unit;
import dip.world.Power;
import dip.world.RuleOptions;

import dip.gui.order.GUIOrder.MapInfo;

import dip.process.Adjustment.AdjustmentInfoMap;
import dip.process.RetreatChecker;

import dip.gui.map.MapMetadata;

import java.awt.geom.Point2D;

import org.apache.batik.util.SVGConstants;
import org.apache.batik.util.CSSConstants;
import org.apache.batik.dom.svg.SVGDOMImplementation;
import org.w3c.dom.svg.*;
import org.w3c.dom.*;

/**
*
*	GUIOrder subclass of Convoy order.
*
*/
public class GUIConvoy extends Convoy implements GUIOrder
{
	// i18n keys
	private final static String ONLY_SEA_OR_CC_FLEETS_CAN_CONVOY = "GUIConvoy.only_fleets_can_convoy";
	private final static String CLICK_TO_CONVOY = "GUIConvoy.click_to_convoy";
	private final static String NO_UNIT = "GUIConvoy.no_unit";
	private final static String CLICK_TO_CONVOY_ARMY = "GUIConvoy.click_to_convoy_army";
	private final static String CANNOT_CONVOY_LANDLOCKED = "GUIConvoy.no_convoy_landlocked";
	private final static String MUST_CONVOY_FROM_COAST = "GUIConvoy.must_convoy_from_coast";
	private final static String CLICK_TO_CONVOY_FROM = "GUIConvoy.click_to_convoy_from";
	private final static String NO_POSSIBLE_CONVOY_PATH = "GUIConvoy.no_path";
	private final static String MUST_CONVOY_TO_COAST = "GUIConvoy.must_convoy_to_coast";
	
	// instance variables
	private transient static final int REQ_LOC = 3;
	private transient int currentLocNum = 0;
	private transient Point2D.Float failPt = null;
	private transient SVGGElement group = null;
	
	
	/** Creates a GUIConvoy */
	protected GUIConvoy()
	{
		super();
	}// GUIConvoy()
	
	/** Creates a GUIConvoy */
	protected GUIConvoy(Power power, Location src, Unit.Type srcUnitType, 
		Location convoySrc, Power convoyPower, Unit.Type convoySrcUnitType, 
		Location convoyDest)
	{
		super(power, src, srcUnitType, convoySrc, convoyPower, 
			convoySrcUnitType, convoyDest);
	}// GUIConvoy()
	
	/** This only accepts Convoy orders. All others will throw an IllegalArgumentException. */
	public void deriveFrom(Orderable order)
	{
		if( !(order instanceof Convoy) )
		{
			throw new IllegalArgumentException();
		}
		
		Convoy convoy = (Convoy) order;
		power = convoy.getPower();
		src = convoy.getSource();
		srcUnitType = convoy.getSourceUnitType();
		
		convoySrc = convoy.getConvoySrc();
		convoyDest = convoy.getConvoyDest();
		convoyPower = convoy.getConvoyedPower();
		convoyUnitType = convoy.getConvoyUnitType();
		
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
		
		if(currentLocNum == 0)
		{
			// set Convoy origin (supporting unit)
			// We will check unit ownership too, if appropriate
			Unit unit = position.getUnit(province);
			if(unit != null)
			{
				if( !stateInfo.canIssueOrder(unit.getPower()) )
				{
					sb.append( Utils.getLocalString(GUIOrder.NOT_OWNER, unit.getPower()) );
					return false;
				}
				
				// we require a Fleet in a sea space or convoyable coast to be present. 
				if(unit.getType() == Unit.Type.FLEET)
				{
					if(province.isSea() || province.isConvoyableCoast())
					{
						// check borders
						if( !GUIOrderUtils.checkBorder(this, new Location(province, unit.getCoast()), unit.getType(), stateInfo.getPhase(), sb) )
						{
							return false;
						}
						
						// order is acceptable
						sb.append( Utils.getLocalString(GUIOrder.CLICK_TO_ISSUE, getFullName()) );
						return true;
					}
					else
					{
						sb.append( Utils.getLocalString(ONLY_SEA_OR_CC_FLEETS_CAN_CONVOY) );
						return false;
					}
				}
				else
				{
					sb.append( Utils.getLocalString(ONLY_SEA_OR_CC_FLEETS_CAN_CONVOY) );
					return false;
				}
			}
			
			// no unit in province
			sb.append( Utils.getLocalString(GUIOrder.NO_UNIT, getFullName()) );
			return false;
		}
		else if(currentLocNum == 1)
		{
			// set Convoy source (unit being convoyed)
			// - If we are not validating, any location with a unit is acceptable (even source)
			// - If we are validating, 
			//
			if(stateInfo.getValidationOptions().getOption(ValidationOptions.KEY_GLOBAL_PARSING).equals(ValidationOptions.VALUE_GLOBAL_PARSING_LOOSE))
			{
				// lenient parsing enabled; we'll take anything with a unit!
				if(position.hasUnit(province))
				{
					sb.append( Utils.getLocalString(CLICK_TO_CONVOY));
					return true;
				}
				
				// no unit in province
				sb.append( Utils.getLocalString(NO_UNIT));
				return false;
			}
			
			// strict parsing is enabled. We are more selective.
			// The location must contain a coastal Army unit
			//
			Unit unit = position.getUnit(province);
			if(unit != null)
			{
				if(unit.getType() == Unit.Type.ARMY)
				{
					if(province.isCoastal())
					{
						// check borders
						if( !GUIOrderUtils.checkBorder(this, new Location(province, unit.getCoast()), unit.getType(), stateInfo.getPhase(), sb) )
						{
							return false;
						}
						
						sb.append( Utils.getLocalString(CLICK_TO_CONVOY_ARMY) );
						return true;
					}
					else
					{
						sb.append( Utils.getLocalString(CANNOT_CONVOY_LANDLOCKED) );
						return false;
					}
				}
				else
				{
					sb.append( Utils.getLocalString(MUST_CONVOY_FROM_COAST) );
					return false;
				}
			}
			
			// no unit in province
			sb.append( Utils.getLocalString(NO_UNIT) );
			return false;
		}
		else if(currentLocNum == 2)
		{
			// set Convoy destination
			// - If we are not validating, any destination is acceptable (even source)
			// - If we are validating, we check that a theoretical (possible) convoy route to
			// 		the destination exists (could exist)
			//
			if(stateInfo.getValidationOptions().getOption(ValidationOptions.KEY_GLOBAL_PARSING).equals(ValidationOptions.VALUE_GLOBAL_PARSING_LOOSE))
			{
				// lenient parsing enabled; we'll take anything!
				sb.append( Utils.getLocalString(CLICK_TO_CONVOY_FROM, province.getFullName()) );
				return true;
			}
			
			// strict parsing is enabled. We are more selective. Check for a possible convoy route.
			if(province.isCoastal())
			{
				Path path = new Path(position);
				if(path.isPossibleConvoyRoute(convoySrc, new Location(province, Coast.NONE)))
				{
					// check borders
					if( !GUIOrderUtils.checkBorder(this, location, convoyUnitType, stateInfo.getPhase(), sb) )
					{
						return false;
					}
					
					sb.append( Utils.getLocalString(CLICK_TO_CONVOY_FROM, province.getFullName()) );
					return true;
				}
				else
				{
					sb.append( Utils.getLocalString(NO_POSSIBLE_CONVOY_PATH, convoySrc.getProvince().getFullName()) );
					return false;
				}
			}
			else
			{
				sb.append( Utils.getLocalString(MUST_CONVOY_TO_COAST) );
				return false;
			}
		}
		else
		{
			// should not occur.
			throw new IllegalStateException();
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
		srcUnitType = null;
		convoySrc = null;
		convoyDest = null;
		convoyPower = null;
		convoyUnitType = null;
		
		return true;
	}// clearLocations()
	
	
	public boolean setLocation(StateInfo stateInfo, Location location, StringBuffer sb)
	{
		if(isComplete())
		{
			return false;
		}
		
		if(testLocation(stateInfo, location, sb))
		{
			if(currentLocNum == 0)
			{
				Unit unit = stateInfo.getPosition().getUnit(location.getProvince());
				src = new Location(location.getProvince(), unit.getCoast());
				power = unit.getPower();
				srcUnitType = unit.getType();
				currentLocNum++;
				return true;
			}
			else if(currentLocNum == 1)
			{
				Unit unit = stateInfo.getPosition().getUnit(location.getProvince());
				convoySrc = new Location(location.getProvince(), unit.getCoast());
				convoyUnitType = unit.getType();
				
				sb.setLength(0);
				sb.append("Convoying this unit.");
				currentLocNum++;
				return true;
			}
			else if(currentLocNum == 2)
			{
				convoyDest = new Location(location.getProvince(), location.getCoast());
				
				sb.setLength(0);
				sb.append( Utils.getLocalString(GUIOrder.COMPLETE, getFullName()) );
				currentLocNum++;
				return true;
			}
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
			SVGGElement powerGroup = mapInfo.getPowerSVGGElement(power, LAYER_LOWEST);
			GUIOrderUtils.removeChild(powerGroup, group);
			group = null;
		}
	}// removeFromDOM()
	
	
	/** 
	*	Draws a dashed line to a triangle surrounding convoyed unit, and then a 
	*	dashed line from convoyed unit to destination.
	*/
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
		
			mapInfo.getPowerSVGGElement(power, LAYER_LOWEST).appendChild(group);
		}
		else
		{
			// remove group children
			GUIOrderUtils.deleteChildren(group);
		}
		
		// now, render the order
		//
		SVGElement[] elements = null;
		
		// create hilight line
		String cssStyle = mapInfo.getMapMetadata().getOrderParamString(MapMetadata.EL_CONVOY, MapMetadata.ATT_HILIGHT_CLASS);
		if(!cssStyle.equalsIgnoreCase("none"))
		{
			float offset = mapInfo.getMapMetadata().getOrderParamFloat(MapMetadata.EL_CONVOY, MapMetadata.ATT_HILIGHT_OFFSET);	
			elements = drawOrder(mapInfo, offset, false);
			GUIOrderUtils.makeHilight(elements, mapInfo.getMapMetadata(), MapMetadata.EL_CONVOY);
			for(int i=0; i<elements.length; i++)
			{
				group.appendChild(elements[i]);
			}
		}
		
		// create real line
		elements = drawOrder(mapInfo, 0, true);
		GUIOrderUtils.makeStyled(elements, mapInfo.getMapMetadata(), MapMetadata.EL_CONVOY, power);
		for(int i=0; i<elements.length; i++)
		{
			group.appendChild(elements[i]);
		}
		
		// draw 'failed' marker, if appropriate.
		if(!mapInfo.getTurnState().isOrderSuccessful(this))
		{
			SVGElement useElement = GUIOrderUtils.createFailedOrderSymbol(mapInfo, failPt.x, failPt.y);
			group.appendChild(useElement);
		}
	}// updateDOM()
	
	
	private SVGElement[] drawOrder(MapInfo mapInfo, float offset, boolean addMarker)
	{
		// setup
		SVGElement[] elements = new SVGElement[3];
		
		Position position = mapInfo.getTurnState().getPosition();
		MapMetadata mmd = mapInfo.getMapMetadata();
		Point2D.Float ptSrc = mmd.getUnitPt(src.getProvince(), src.getCoast());
		Point2D.Float ptConvoySrc = mmd.getUnitPt(convoySrc.getProvince(), convoySrc.getCoast());
		Point2D.Float ptConvoyDest = mmd.getUnitPt(convoyDest.getProvince(), convoyDest.getCoast());
		
		ptSrc.x += offset;
		ptSrc.y += offset;
		ptConvoySrc.x += offset;
		ptConvoySrc.y += offset;
		ptConvoyDest.x += offset;
		ptConvoyDest.y += offset;
		
		// radius
		float radius = mmd.getOrderRadius(MapMetadata.EL_CONVOY, mapInfo.getSymbolName(getConvoyUnitType()));
		
		// draw line to convoyed unit
		Point2D.Float newPtTo = GUIOrderUtils.getLineCircleIntersection(ptSrc.x, ptSrc.y, 
								ptConvoySrc.x, ptConvoySrc.y, ptConvoySrc.x, ptConvoySrc.y, radius);
								
		elements[0] = 	(SVGLineElement) 
						mapInfo.getDocument().createElementNS(
						SVGDOMImplementation.SVG_NAMESPACE_URI, 
						SVGConstants.SVG_LINE_TAG);
		
		elements[0].setAttributeNS(null, SVGConstants.SVG_X1_ATTRIBUTE, GUIOrderUtils.floatToString(ptSrc.x));
		elements[0].setAttributeNS(null, SVGConstants.SVG_Y1_ATTRIBUTE, GUIOrderUtils.floatToString(ptSrc.y));
		elements[0].setAttributeNS(null, SVGConstants.SVG_X2_ATTRIBUTE, GUIOrderUtils.floatToString(newPtTo.x));
		elements[0].setAttributeNS(null, SVGConstants.SVG_Y2_ATTRIBUTE, GUIOrderUtils.floatToString(newPtTo.y));
		elements[0].setAttributeNS(null, SVGConstants.SVG_CLASS_ATTRIBUTE, 
			mmd.getOrderParamString(MapMetadata.EL_CONVOY, MapMetadata.ATT_STROKESTYLE));
		
		// draw triangle around supported unit
		Point2D.Float[] triPts = GUIOrderUtils.makeTriangle(ptConvoySrc, radius);
		
		StringBuffer sb = new StringBuffer(160);
		for(int i=0; i<triPts.length; i++)
		{
			GUIOrderUtils.appendFloat(sb, triPts[i].x);
			sb.append(',');
			GUIOrderUtils.appendFloat(sb, triPts[i].y);
			sb.append(' ');
		}
		
		
		elements[1] = 	(SVGPolygonElement) 
						mapInfo.getDocument().createElementNS(
						SVGDOMImplementation.SVG_NAMESPACE_URI, 
						SVGConstants.SVG_POLYGON_TAG);
		
		elements[1].setAttributeNS(null, SVGConstants.SVG_POINTS_ATTRIBUTE, sb.toString());
		elements[1].setAttributeNS(null, SVGConstants.SVG_CLASS_ATTRIBUTE, mmd.getOrderParamString(MapMetadata.EL_CONVOY, MapMetadata.ATT_STROKESTYLE));
		
		// failPt will be a triangle vertext (12 o'clock vertex)
		failPt = new Point2D.Float(triPts[0].x, triPts[0].y);
		
		// draw line from triangle to convoyDest.
		// line will come from the closest triangular vertext, by distance.
		//
		Point2D.Float newPtFrom = null;
		float maxDistSquared = 0.0f;
		for(int i=0; i<triPts.length; i++)
		{
			float distSquared = (float) (Math.pow((ptConvoyDest.x - triPts[i].x), 2.0) + Math.pow((ptConvoyDest.y - triPts[i].y), 2.0));
			if(distSquared > maxDistSquared)
			{
				maxDistSquared = distSquared;
				newPtFrom = triPts[i];
			}
		}
		
		// only respect convoyDest iff there is a unit present.
		if(position.hasUnit(convoyDest.getProvince()))
		{
			// use 'move' (EL_MOVE) order radius; 'hold' could also be appropriate.
			// we do this because the destination unit may have an order, and this
			// results in a better display.
			//
			Unit.Type destUnitType = position.getUnit(convoyDest.getProvince()).getType();
			float moveRadius = mmd.getOrderRadius(MapMetadata.EL_MOVE, mapInfo.getSymbolName(destUnitType));
			newPtTo = GUIOrderUtils.getLineCircleIntersection(newPtFrom.x, newPtFrom.y, ptConvoyDest.x, ptConvoyDest.y, ptConvoyDest.x, ptConvoyDest.y, moveRadius);
		}
		else
		{
			newPtTo = ptConvoyDest;
		}
		
		elements[2] = 	(SVGLineElement) 
						mapInfo.getDocument().createElementNS(
						SVGDOMImplementation.SVG_NAMESPACE_URI, 
						SVGConstants.SVG_LINE_TAG);
		
		elements[2].setAttributeNS(null, SVGConstants.SVG_X1_ATTRIBUTE, GUIOrderUtils.floatToString(newPtFrom.x));
		elements[2].setAttributeNS(null, SVGConstants.SVG_Y1_ATTRIBUTE, GUIOrderUtils.floatToString(newPtFrom.y));
		elements[2].setAttributeNS(null, SVGConstants.SVG_X2_ATTRIBUTE, GUIOrderUtils.floatToString(newPtTo.x));
		elements[2].setAttributeNS(null, SVGConstants.SVG_Y2_ATTRIBUTE, GUIOrderUtils.floatToString(newPtTo.y));
		elements[2].setAttributeNS(null, SVGConstants.SVG_CLASS_ATTRIBUTE, 
			mmd.getOrderParamString(MapMetadata.EL_CONVOY, MapMetadata.ATT_STROKESTYLE));
		
		// marker
		if(addMarker || offset != 0.0f)
		{
			GUIOrderUtils.addMarker(elements[2], mmd, MapMetadata.EL_CONVOY);
		}
		
		
		// add to parent
		return elements;
	}// drawOrder()
	
	
	public boolean isDependent()	{ return false; }
	
	
}// class GUIConvoy
