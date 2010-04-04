//
//  @(#)GUIMove.java	12/2002
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

import dip.gui.map.MapMetadata;
import dip.gui.map.DefaultMapRenderer2;

import dip.gui.order.GUIOrder.MapInfo;

import dip.order.Orderable;
import dip.order.Move;
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

import dip.process.Adjustment.AdjustmentInfoMap;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.awt.geom.Point2D;

import org.apache.batik.util.SVGConstants;
import org.apache.batik.util.CSSConstants;
import org.apache.batik.dom.svg.SVGDOMImplementation;
import org.apache.batik.dom.util.XLinkSupport;
import org.w3c.dom.svg.*;
import org.w3c.dom.*;

/**
*
*	GUIOrder subclass of Move order.
*	<p>
*	Note that GUIMove fully supports moves with explicit convoy routes, 
*	but their GUI entry is not supported.
*/
public class GUIMove extends Move implements GUIOrder
{
	// MoveParameter constants
	/** Optional. Sets this Move to be by convoy. Value must be a Boolean object (Boolean.TRUE or Boolean.FALSE) */
	public transient static final MoveParameter BY_CONVOY = new MoveParameter("BY_CONVOY");
	
	// i18n keys
	private final static String CLICK_TO_SET_DEST  		= "GUIMove.set.dest";
	private final static String CANNOT_MOVE_TO_ORIGIN 	= "GUIMove.cannot_to_origin";
	private final static String NO_CONVOY_ROUTE  		= "GUIMove.no_convoy_route";
	private final static String CANNOT_MOVE_HERE  		= "GUIMove.cannot_move_here";
	
	// instance variables
	private transient static final int REQ_LOC = 2;
	private transient int currentLocNum = 0;
	private transient int numSupports = -9999;
	private transient Point2D.Float failPt = null;
	private transient SVGGElement group = null;
	
	/** Creates a GUIMove */
	protected GUIMove()
	{
		super();
	}// GUIMove()
	
	/** Creates a GUIMove */
	protected GUIMove(Power power, Location source, Unit.Type srcUnitType, Location dest, boolean isConvoying)
	{
		super(power, source, srcUnitType, dest, isConvoying);
	}// GUIMove()
	
	/** Creates a GUIMove */
	protected GUIMove(Power power, Location src, Unit.Type srcUnitType, Location dest, Province[] convoyRoute)
	{
		super(power, src, srcUnitType, dest, convoyRoute);
	}// GUIMove()
	
	
	/** Creates a GUIMove */
	protected GUIMove(Power power, Location src, Unit.Type srcUnitType, Location dest, List routes)
	{
		super(power, src, srcUnitType, dest, routes);
	}// GUIMove()
	
	
	
	
	
	/** This only accepts Move orders. All others will throw an IllegalArgumentException. */
	public void deriveFrom(Orderable order)
	{
		if( !(order instanceof Move) )
		{
			throw new IllegalArgumentException();
		}
		
		Move move = (Move) order;
		power = move.getPower();
		src = move.getSource();
		srcUnitType = move.getSourceUnitType();
		dest = move.getDest();
		_isViaConvoy = move.isViaConvoy();				
		_isAdjWithPossibleConvoy = move.isAdjWithPossibleConvoy();
		_isConvoyIntent = isConvoyIntent();
		
		// set completed
		currentLocNum = REQ_LOC;
	}// GUIMove()
	
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
			// set Move source
			// we require a unit present. We will check unit ownership too, if appropriate
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
		}
		else if(currentLocNum == 1)
		{
			// set move destination
			// - If we are not validating, any destination is acceptable (even source)
			// - If we are validating, we check that the move is adjacent or a possible convoy
			//		route exists.
			//
			if(stateInfo.getValidationOptions().getOption(ValidationOptions.KEY_GLOBAL_PARSING).equals(ValidationOptions.VALUE_GLOBAL_PARSING_LOOSE))
			{
				// lenient parsing enabled; we'll take anything!
				sb.append( Utils.getLocalString(CLICK_TO_SET_DEST) );
				return true;
			}
			
			// strict parsing is enabled. We are more selective.
			if(province == src.getProvince())
			{
				sb.append( Utils.getLocalString(CANNOT_MOVE_TO_ORIGIN) );
				return false;
			}
			else if(src.isAdjacent(province))
			{
				sb.append( Utils.getLocalString(CLICK_TO_SET_DEST) );
				return true;
			}
			else if(province.isCoastal() && srcUnitType == Unit.Type.ARMY)
			{
				// we may have a possible convoy route; if not, say so
				// NOTE: assume destination coast is Coast.NONE
				Path path = new Path(position);
				if(path.isPossibleConvoyRoute(src, new Location(province, Coast.NONE)))
				{
					sb.append( Utils.getLocalString(CLICK_TO_SET_DEST) );
					return true;
				}
				else
				{
					sb.append( Utils.getLocalString(NO_CONVOY_ROUTE) );
					return false;
				}
			}
			else if( !GUIOrderUtils.checkBorder(this, location, srcUnitType, stateInfo.getPhase(), sb) )
			{
				// text already set by checkBorder() method
				return false;
			}
			
			
			sb.append( Utils.getLocalString(CANNOT_MOVE_HERE) );
			return false;
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
		dest = null;
		_isViaConvoy = false;
		_isAdjWithPossibleConvoy = false;
		_isConvoyIntent = false;
		
		return true;
	}// clearLocations()
	
	
	public boolean setLocation(StateInfo stateInfo, Location location, StringBuffer sb)
	{
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
				dest = new Location(location.getProvince(), location.getCoast());
				
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
	
	
	/**
	*	Sets optional Move parameters.
	*
	*/
	public void setParam(Parameter param, Object value)
	{
		if(param == BY_CONVOY)
		{
			if(value instanceof Boolean)
			{
				_isViaConvoy = ((Boolean) value).booleanValue();
			}
			else
			{
				throw new IllegalArgumentException();
			}
		}
		else
		{
			throw new IllegalArgumentException();
		}
	}// setParam()
	

	/**
	*	Get optional Move parameters.
	*/
	public Object getParam(Parameter param)
	{
		if(param == BY_CONVOY)
		{
			return Boolean.valueOf(isViaConvoy());
		}
		else
		{
			throw new IllegalArgumentException();
		}
	}// getParam()
	
	
	
	public void removeFromDOM(MapInfo mapInfo)
	{
		if(group != null)
		{
			SVGGElement powerGroup = mapInfo.getPowerSVGGElement(power, LAYER_TYPICAL);
			GUIOrderUtils.removeChild(powerGroup, group);
			group = null;
			numSupports = -9999;
		}
	}// removeFromDOM()
	
	
	/** Draws a line with an arrow. */
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
		// check supports
		int support = GUIOrderUtils.getMatchingSupportCount(mapInfo, 
							src.getProvince(), dest.getProvince());
		
		// modify move support with BaseMoveModifier (if any)
		support += getDest().getProvince().getBaseMoveModifier(getSource());
		
		if(numSupports == support && group != null)
		{
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
			group = (SVGGElement) mapInfo.getDocument().createElementNS(
									SVGDOMImplementation.SVG_NAMESPACE_URI, SVGConstants.SVG_G_TAG);
			mapInfo.getPowerSVGGElement(power, LAYER_TYPICAL).appendChild(group);
		}
		else
		{
			// remove group children
			GUIOrderUtils.deleteChildren(group);
		}
		
		// now, render the order
		//
		SVGElement element = null;
		
		// create hilight line
		String cssStyle = mapInfo.getMapMetadata().getOrderParamString(MapMetadata.EL_MOVE, MapMetadata.ATT_HILIGHT_CLASS);
		if(!cssStyle.equalsIgnoreCase("none"))
		{
			float offset = mapInfo.getMapMetadata().getOrderParamFloat(MapMetadata.EL_MOVE, MapMetadata.ATT_HILIGHT_OFFSET);	
			float width = GUIOrderUtils.getLineWidth(mapInfo, MapMetadata.EL_MOVE, MapMetadata.ATT_SHADOW_WIDTHS, numSupports);
			
			element = drawOrder(mapInfo, offset, false);
			element.setAttributeNS(null, SVGConstants.SVG_STROKE_WIDTH_ATTRIBUTE, GUIOrderUtils.floatToString(width));
			
			GUIOrderUtils.makeHilight(element, mapInfo.getMapMetadata(), MapMetadata.EL_MOVE);
			group.appendChild(element);
		}
		
		// create real line
		float width = GUIOrderUtils.getLineWidth(mapInfo, MapMetadata.EL_MOVE, MapMetadata.ATT_WIDTHS, numSupports);
		
		element = drawOrder(mapInfo, 0, true);
		element.setAttributeNS(null, SVGConstants.SVG_STROKE_WIDTH_ATTRIBUTE, GUIOrderUtils.floatToString(width));
		
		GUIOrderUtils.makeStyled(element, mapInfo.getMapMetadata(), MapMetadata.EL_MOVE, power);
		group.appendChild(element);
		
		// draw 'failed' marker, if appropriate.
		if(!mapInfo.getTurnState().isOrderSuccessful(this))
		{
			SVGUseElement useElement = GUIOrderUtils.createFailedOrderSymbol(mapInfo, failPt.x, failPt.y);
			group.appendChild(useElement);
		}
	}// updateDOM()
	
	
	/** draws convoyed or non-convoyed order, depending upon flag */
	private SVGElement drawOrder(MapInfo mapInfo, float offset, boolean addMarker)
	{
		/*
		if(isByConvoy())
		{
			return drawConvoyedOrder(mapInfo, offset, addMarker);
		}
		else
		{
			return drawNCOrder(mapInfo, offset, addMarker);
		}
		*/
		
		return drawNCOrder(mapInfo, offset, addMarker);
	}// drawOrder()
	
	
	/** if addMarker == true, ALWAYS add marker; otherwise, only added if offset is non-zero */
	private SVGElement drawNCOrder(MapInfo mapInfo, float offset, boolean addMarker)
	{
		MapMetadata mmd = mapInfo.getMapMetadata();
		Point2D.Float ptFrom = mmd.getUnitPt(src.getProvince(), src.getCoast());
		Point2D.Float ptTo = mmd.getUnitPt(dest.getProvince(), dest.getCoast());
		
		// respect radius, if there is a unit present in destination.
		// if there is no unit, use radius / 2. (for an Army)
		//
		Point2D.Float newPtTo = ptTo;
		Position position = mapInfo.getTurnState().getPosition();
		float r = 0.0f;
		if(position.hasUnit(dest.getProvince()))
		{
			Unit.Type destUnitType = position.getUnit(dest.getProvince()).getType();
			r = mmd.getOrderRadius(MapMetadata.EL_MOVE, mapInfo.getSymbolName(destUnitType));
			
			// TODO: radius should depend upon order (none, hold, support, etc.)
			// we should have each GUIOrder type return its radius via 
			// a method. Then as long as it's not null, we can just get() the radius.
			// OR, perhaps, each GUIOrder type should return it's metadata.
			// this would make things easy.... and object oriented.
			//
			// e.g.: 
			//		getOrder().getMetadata().getOrderRadius()
			// or something like that.
			// GUIOrder could define the OrderMetadata class.
			//
		}
		else
		{
			r = (mmd.getOrderRadius(MapMetadata.EL_MOVE, mapInfo.getSymbolName(Unit.Type.ARMY)) / 2);
		}
			
		newPtTo = GUIOrderUtils.getLineCircleIntersection(ptFrom.x+offset, ptFrom.y+offset, 
					ptTo.x+offset, ptTo.y+offset, ptTo.x+offset, ptTo.y+offset, r);
		
		
		// calculate (but don't yet use) failPt
		failPt = GUIOrderUtils.getLineMidpoint(ptFrom.x, ptFrom.y, newPtTo.x, newPtTo.y);
		
		// create SVG element(s)
		SVGLineElement line = (SVGLineElement) 
							mapInfo.getDocument().createElementNS(
							SVGDOMImplementation.SVG_NAMESPACE_URI, 
							SVGConstants.SVG_LINE_TAG);
		
		line.setAttributeNS(null, SVGConstants.SVG_X1_ATTRIBUTE, GUIOrderUtils.floatToString(ptFrom.x+offset));
		line.setAttributeNS(null, SVGConstants.SVG_Y1_ATTRIBUTE, GUIOrderUtils.floatToString(ptFrom.y+offset));
		line.setAttributeNS(null, SVGConstants.SVG_X2_ATTRIBUTE, GUIOrderUtils.floatToString(newPtTo.x+offset));
		line.setAttributeNS(null, SVGConstants.SVG_Y2_ATTRIBUTE, GUIOrderUtils.floatToString(newPtTo.y+offset));
		
		// style
		line.setAttributeNS(null, SVGConstants.SVG_CLASS_ATTRIBUTE, 
			mmd.getOrderParamString(MapMetadata.EL_MOVE, MapMetadata.ATT_STROKESTYLE));
		
		
		
		// marker
		if(addMarker || offset != 0.0f)
		{
			GUIOrderUtils.addMarker(line, mmd, MapMetadata.EL_MOVE);
		}
		
		// end
		return line;
	}// drawNCOrder()
	
	
	
	/* Draw a convoyed order. Note that this doesn't really work yet (problems with getConvoyRoute()) 
	private SVGElement drawConvoyedOrder(MapInfo mapInfo, float offset, boolean addMarker)
	{
		// if we are convoyed, and no theoretical path, just use a regular 
		// move draw
		Position position = mapInfo.getTurnState().getPosition();
		Path path = new Path(position);
		List route = path.getConvoyRoute(src, dest);
		if(!route.isEmpty())
		{
			drawNCOrder(mapInfo, offset, addMarker);
		}
		
		// a valid convoy route exists. 
		// get that route so we can draw the order.
		// the unit position of each route is required.
		// create the path data string
		//
		MapMetadata mmd = mapInfo.getMapMetadata();
		
		StringBuffer sb = new StringBuffer(256);
		
		Iterator iter = route.iterator();
		int count = 0;
		final int last = route.size() - 1;
		Point2D.Float lastPoint = null;
		final float convoyRadius = mmd.getOrderParamFloat(MapMetadata.EL_CONVOY, MapMetadata.ATT_RADIUS);
		while(iter.hasNext())
		{
			Location loc = (Location) iter.next();
			
			// append path type
			if(count == 0)
			{
				sb.append(" M ");	// MoveTo
			}
			else
			{
				sb.append(" L ");	// LineTo
			}
			
			// append coordinate
			Point2D.Float currentPoint;
			if(count == 0)
			{
				// use source point directly
				currentPoint = mmd.getUnitPt(src.getProvince(), src.getCoast());
			}
			else if(count == last)
			{
				// respect radius for final path segment, if unit present
				Point2D.Float ptTo = mmd.getUnitPt(loc.getProvince(), loc.getCoast());
				if(position.hasUnit(dest.getProvince()))
				{
					float r  = mmd.getOrderParamFloat(MapMetadata.EL_MOVE, MapMetadata.ATT_RADIUS);
					currentPoint = GUIOrderUtils.getLineCircleIntersection(
						lastPoint.x+offset, lastPoint.y+offset, 
						ptTo.x+offset, ptTo.y+offset, 
						ptTo.x+offset, ptTo.y+offset, r);
					
				}
				else
				{
					currentPoint = ptTo;
				}
			}
			else
			{
				// use 12-o'clock triangle point of convoy order for convoying unit
				currentPoint = mmd.getUnitPt(loc.getProvince(), loc.getCoast());
				currentPoint.y -= convoyRadius;
			}
			
			// append currentPoint
			GUIOrderUtils.appendFloat(currentPoint.x + offset);
			sb.append(',');
			GUIOrderUtils.appendFloat(currentPoint.y + offset);
			
			// set last point
			lastPoint = currentPoint;
			
			count++;
		}
		
		
		// create SVG element(s)
		SVGPathElement pathElement = (SVGPathElement) 
							mapInfo.getDocument().createElementNS(
							SVGDOMImplementation.SVG_NAMESPACE_URI, 
							SVGConstants.SVG_PATH_TAG);
		
		pathElement.setAttributeNS(null, SVGConstants.SVG_D_ATTRIBUTE, sb.toString());
		
		// style
		pathElement.setAttributeNS(null, SVGConstants.SVG_CLASS_ATTRIBUTE, 
			mmd.getOrderParamString(MapMetadata.EL_MOVE, MapMetadata.ATT_STROKESTYLE));
		
		// marker
		if(addMarker || offset != 0.0f)
		{
			GUIOrderUtils.addMarker(pathElement, mmd, MapMetadata.EL_MOVE);
		}
		
		// end
		return pathElement;
	}// drawConvoyedOrder()
	*/
	
	/** We are dependent on the presence of Support orders for certain drawing parameters. */
	public boolean isDependent()	{ return true; }
	
	
	/**
	*	Typesafe Enumerated Parameter class for setting
	*	optional Move parameters.
	*
	*/
	protected static class MoveParameter extends Parameter
	{
		/** Creates a MoveParameter */
		public MoveParameter(String name)
		{
			super(name);
		}// MoveParameter()
	}// nested class MoveParameter
	
	
	
}// class GUIMove
