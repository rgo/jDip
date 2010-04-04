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
import dip.order.OrderFormat;

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
import java.util.LinkedList;
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
*	This differs from GUIMove in that Explicit convoy routes are <b>always</b>
*	created. Implicit convoy routes <b>cannot</b> be created via GUI entry.
*	<p>
*	This should be used instead of GUIMove for games with RuleOptions that
*	enforce explicit convoy routes (such as Judge-based games).
*
*/
public class GUIMoveExplicit extends Move implements GUIOrder
{
	
	// i18n keys
	private final static String CLICK_TO_SET_DEST  		= "GUIMove.set.dest";
	private final static String CANNOT_MOVE_TO_ORIGIN 	= "GUIMove.cannot_to_origin";
	private final static String NO_CONVOY_ROUTE  		= "GUIMove.no_convoy_route";
	private final static String CANNOT_MOVE_HERE  		= "GUIMove.cannot_move_here";
	
	// i18n keys for convoys
	private final static String CANNOT_BACKTRACK		= "GUIMoveExplicit.convoy.backtrack";
	private final static String FINAL_DESTINATION		= "GUIMoveExplicit.convoy.location.destination";
	private final static String OK_CONVOY_LOCATION		= "GUIMoveExplicit.convoy.location.ok";
	private final static String BAD_CONVOY_LOCATION		= "GUIMoveExplicit.convoy.location.bad";
	private final static String NONADJACENT_CONVOY_LOCATION		= "GUIMoveExplicit.convoy.location.nonadjacent";
	private final static String ADDED_CONVOY_LOCATION	= "GUIMoveExplicit.convoy.location.added";
	
	// instance variables
	private transient static final int REQ_LOC = 2;
	private transient boolean isConvoyableArmy = false;
	private transient boolean isComplete = false;
	private transient LinkedList tmpConvoyPath = null;
	private transient int currentLocNum = 0;
	private transient int numSupports = -9999;
	private transient Point2D.Float failPt = null;
	private transient SVGGElement group = null;
	
	
	
	
	
	/** Creates a GUIMoveExplicit */
	protected GUIMoveExplicit()
	{
		super();
	}// GUIMoveExplicit()
	
	/** Creates a GUIMoveExplicit */
	protected GUIMoveExplicit(Power power, Location source, Unit.Type srcUnitType, Location dest, boolean isConvoying)
	{
		super(power, source, srcUnitType, dest, isConvoying);
	}// GUIMoveExplicit()
	
	/** Creates a GUIMoveExplicit */
	protected GUIMoveExplicit(Power power, Location src, Unit.Type srcUnitType, Location dest, Province[] convoyRoute)
	{
		super(power, src, srcUnitType, dest, convoyRoute);
	}// GUIMoveExplicit()
	
	
	/** Creates a GUIMoveExplicit */
	protected GUIMoveExplicit(Power power, Location src, Unit.Type srcUnitType, Location dest, List routes)
	{
		super(power, src, srcUnitType, dest, routes);
	}// GUIMoveExplicit()
	
	
	
	
	
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
		isComplete = true;
	}// GUIMove()
	
	
	public boolean testLocation(StateInfo stateInfo, Location location, StringBuffer sb)
	{
		final LocationTestResult result = testLocationLTR(stateInfo, location, sb);
		return result.isValid;
	}// testLocation()
	
	/**
	*	More complex version of testLocation(), that returns extended 
	*	results that can be used by setLocation(). 
	*	
	*/
	private LocationTestResult testLocationLTR(StateInfo stateInfo, Location location, StringBuffer sb)
	{
		sb.setLength(0);
		
		final LocationTestResult result = new LocationTestResult();
		
		if(isComplete())
		{
			sb.append( Utils.getLocalString(GUIOrder.COMPLETE, getFullName()) );
			return result;
		}
		
		
		final Position position = stateInfo.getPosition();
		final Province province = location.getProvince();
		
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
					result.isValid = false;
					return result;
				}
				
				if( !GUIOrderUtils.checkBorder(this, new Location(province, unit.getCoast()), unit.getType(), stateInfo.getPhase(), sb) )
				{
					result.isValid = false;
					return result;
				}
				
				sb.append( Utils.getLocalString(GUIOrder.CLICK_TO_ISSUE, getFullName()) );
				result.isValid = true;
				return result;
			}
			
			// no unit in province
			sb.append( Utils.getLocalString(GUIOrder.NO_UNIT, getFullName()) );
			result.isValid = false;
			return result;
		}
		else if(currentLocNum >= 1)
		{
			// If the convoy route is explicit only, we MUST create 
			// a defined path, if we are actually convoying/convoyable.
			if(currentLocNum == 1)
			{
				StringBuffer sbTmp = new StringBuffer();
				if(testNonConvoyDest(stateInfo, location, sbTmp))
				{
					// not a convoyed move. successful.
					sb.append(sbTmp);
					result.isValid = true;
					result.isFinalDest = true;
					return result;
				}
				else
				{
					if(!isConvoyableArmy)
					{
						// invalid destination, and not a convoyable unit
						sb.append( Utils.getLocalString(CANNOT_MOVE_HERE) );
						result.isValid = false;
						return result;
					}
				}
			}
			
			
			// we now have to check for convoy-path acceptability.
			assert (isConvoyableArmy);
			assert (tmpConvoyPath != null);
			result.isConvoy = true;
			
			// the last location must be adjacent to this location. We use 
			// 'correct' adjacency; e.g., Coast.SEA or Coast.(direction) for
			// first check
			//
			final Province lastProv = (Province) tmpConvoyPath.getLast();
			Coast coast = Coast.SEA;
			
			if(lastProv.equals(getSource()) && lastProv.isMultiCoastal())
			{
				coast = getSource().getCoast();
			}
			
			// current province cannot already be in our tmpConvoyPath.
			if( tmpConvoyPath.contains(province) )
			{
				if(location.isProvinceEqual(getSource()))
				{
					// kinder, gentler message for src-location
					sb.append( Utils.getLocalString(CANNOT_MOVE_TO_ORIGIN) );
				}
				else
				{
					// cannot backtrack message
					sb.append( Utils.getLocalString(CANNOT_BACKTRACK) );	
				}
				
				result.isValid = false;
				return result;
			}
			
			if(lastProv.isAdjacent(coast, province))
			{
				// this location must be a sea, convoyable coast, and contain a fleet
				// unless it is the final destination, which should be coastal land
				if(province.isCoastal())
				{
					result.isValid = true;
					result.isFinalDest = true;
					sb.append( Utils.getLocalString(FINAL_DESTINATION) );	// final destination message
					return result;
				}
				else
				{
					if(province.isConvoyable() && position.hasUnit(province, Unit.Type.FLEET))
					{
						sb.append( Utils.getLocalString(OK_CONVOY_LOCATION) );	// OK location for convoy path
						result.isValid = true;
						return result;
					}
					else
					{
						sb.append( Utils.getLocalString(BAD_CONVOY_LOCATION) );	// invalid location for convoy path 
						result.isValid = false;
						return result;
					}
				}
			}
			else
			{
				sb.append( Utils.getLocalString(NONADJACENT_CONVOY_LOCATION) );	// non-adjacent location
				result.isValid = false;
				return result;
			}
		}
		else
		{
			throw new IllegalStateException();
		}
		
		// NO return here: thus we must appropriately exit within an if/else block above.
	}// testLocationLTR()
	
	/**
	*	Tests a destination location for acceptability. Does not 
	*	check for convoy-acceptability; thus will return false
	*	in that case.
	*/
	private boolean testNonConvoyDest(StateInfo stateInfo, Location location, StringBuffer sb)
	{
		assert (currentLocNum == 1);
		
		final Province province = location.getProvince();
		
		// set move destination
		// - If we are not validating, any destination is acceptable (even source)
		// - If we are validating, we check that the move is adjacent 
		//
		if(stateInfo.getValidationOptions().getOption(ValidationOptions.KEY_GLOBAL_PARSING).equals(ValidationOptions.VALUE_GLOBAL_PARSING_LOOSE))
		{
			// lenient parsing enabled; we'll take anything!
			sb.append( Utils.getLocalString(CLICK_TO_SET_DEST) );
			return true;
		}
		
		// strict parsing is enabled. We are more selective.
		if(province.equals(src.getProvince()))
		{
			sb.append( Utils.getLocalString(CANNOT_MOVE_TO_ORIGIN) );
			return false;
		}
		else if(src.isAdjacent(province))
		{
			sb.append( Utils.getLocalString(CLICK_TO_SET_DEST) );
			return true;
		}
		else if( !GUIOrderUtils.checkBorder(this, location, srcUnitType, stateInfo.getPhase(), sb) )
		{
			// text already set by checkBorder() method
			return false;
		}
		
		
		sb.append( Utils.getLocalString(CANNOT_MOVE_HERE) );
		return false;
	}// testNonConvoyDest()
	
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
		
		isConvoyableArmy = false;
		isComplete = false;
		
		return true;
	}// clearLocations()
	
	
	private class LocationTestResult
	{
		public boolean isValid = false;		// true if this location is valid
		public boolean isConvoy = false;	// true if this is part of a convoy path--or could be
		public boolean isFinalDest = false;	// true if Location is a possible destination (convoyed or not)
	}// inner class LocationTestResult
	
	
	
	public boolean setLocation(StateInfo stateInfo, Location location, StringBuffer sb)
	{
		// WE need to manage isComplete here, as well as 
		// setting the tmpConvoyPath
		
		// use testLocationLTR
		// and currentLocNum....
		// 
		final LocationTestResult ltr = testLocationLTR(stateInfo, location, sb);
		if(ltr.isValid)
		{
			if(currentLocNum == 0)
			{
				Unit unit = stateInfo.getPosition().getUnit(location.getProvince());
				src = new Location(location.getProvince(), unit.getCoast());
				power = unit.getPower();
				srcUnitType = unit.getType();
				currentLocNum++;
				
				// we're good to go. If this unit is a coastal army, it is
				// considered "possibly convoyable". We may use this later.
				isConvoyableArmy = (location.getProvince().isCoastal() &&  Unit.Type.ARMY.equals(srcUnitType));
				if(isConvoyableArmy)
				{
					assert (tmpConvoyPath == null);
					tmpConvoyPath = new LinkedList();
					tmpConvoyPath.add( getSource().getProvince() );
				}
				
				return true;
			}
			else if(currentLocNum > 0)
			{
				if(ltr.isFinalDest && currentLocNum == 1)
				{
					// nonconvoyed; we are done.
					// 
					dest = new Location(location.getProvince(), location.getCoast());
					
					sb.setLength(0);
					sb.append( Utils.getLocalString(GUIOrder.COMPLETE, getFullName()) );
					currentLocNum++;
					isComplete = true;
					return true;
				}
				
				// we are convoyed.... 
				//
				// add to tmp path
				assert (isConvoyableArmy);
				tmpConvoyPath.add( location.getProvince() );
				currentLocNum++;
				updateConvoyPath();
				
				if(ltr.isFinalDest)
				{
					dest = new Location(location.getProvince(), location.getCoast());
					sb.setLength(0);
					sb.append( Utils.getLocalString(GUIOrder.COMPLETE, getFullName()) );
					isComplete = true;
				}
				else
				{
					sb.setLength(0);
					sb.append( Utils.getLocalString(ADDED_CONVOY_LOCATION, location.getProvince()) );					
				}
				
				return true;
			}
		}
		
		return false;
	}// setLocation()
	
	
	/** Updates convoyPath() from tmpConvoyPath() */
	private void updateConvoyPath()
	{
		if(tmpConvoyPath == null)
		{
			convoyRoutes = null;
		}
		else
		{
			final Province[] provinceRoute = (Province[]) tmpConvoyPath.toArray(
				new Province[tmpConvoyPath.size()]);
			convoyRoutes = new ArrayList(1);
			convoyRoutes.add(provinceRoute);
		}
	}// updateConvoyPath()
	
	public boolean isComplete()
	{
		return isComplete;
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
		Point2D.Float newPtTo = ptTo;
		Position position = mapInfo.getTurnState().getPosition();
		if(position.hasUnit(dest.getProvince()))
		{
			Unit.Type destUnitType = position.getUnit(dest.getProvince()).getType();
			float r = mmd.getOrderRadius(MapMetadata.EL_MOVE, mapInfo.getSymbolName(destUnitType));
			newPtTo = GUIOrderUtils.getLineCircleIntersection(ptFrom.x+offset, ptFrom.y+offset, 
				ptTo.x+offset, ptTo.y+offset, ptTo.x+offset, ptTo.y+offset, r);
		}
		
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
	
	
}// class GUIMoveExplicit
