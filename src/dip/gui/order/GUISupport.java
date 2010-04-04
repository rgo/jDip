//
//  @(#)GUISupport.java		12/2002
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

import dip.gui.map.DefaultMapRenderer2;
import dip.gui.map.MapMetadata;
import dip.gui.order.GUIOrder.MapInfo;
import dip.misc.Utils;
import dip.order.Move;
import dip.order.Orderable;
import dip.order.Support;
import dip.order.ValidationOptions;
import dip.process.Adjustment.AdjustmentInfoMap;
import dip.process.RetreatChecker;
import dip.world.Coast;
import dip.world.Location;
import dip.world.Path;
import dip.world.Position;
import dip.world.Power;
import dip.world.Province;
import dip.world.RuleOptions;
import dip.world.TurnState;
import dip.world.Unit;
import java.awt.geom.Point2D;
import org.apache.batik.dom.svg.SVGDOMImplementation;
import org.apache.batik.dom.util.XLinkSupport;
import org.apache.batik.util.CSSConstants;
import org.apache.batik.util.SVGConstants;
import org.w3c.dom.svg.SVGCircleElement;
import org.w3c.dom.svg.SVGElement;
import org.w3c.dom.svg.SVGLineElement;
import org.w3c.dom.svg.SVGPathElement;
import org.w3c.dom.svg.SVGPolygonElement;
import org.w3c.dom.svg.*;
import org.w3c.dom.*;

/**
*
*	GUIOrder subclass of Support order.
*	<p>
*	Narrowing-order input via the GUI is not yet supported.
*
*/
public class GUISupport extends Support implements GUIOrder
{
	// i18n keys
	private final static String CLICK_TO_SUPPORT_UNIT 	= "GUISupport.click_to_sup";
	private final static String NO_UNIT_TO_SUPPORT 		= "GUISupport.no_unit_to_sup";
	private final static String CANNOT_SUPPORT_SELF 	= "GUISupport.no_self_sup";
	private final static String CLICK_TO_SUPPORT_FROM 	= "GUISupport.click_to_sup_from";
	private final static String CLICK_TO_SUPPORT_HOLD	= "GUISupport.click_to_sup_hold";
	private final static String SUP_DEST_NOT_ADJACENT 	= "GUISupport.sup_dest_not_adj";
	private final static String SUPPORTING_THIS_UNIT 	= "GUISupport.sup_this_unit";
	private final static String CLICK_TO_SUPPORT_MOVE 	= "GUISupport.click_to_sup_move";
	private final static String CLICK_TO_SUPPORT_CONVOYED_MOVE 	= "GUISupport.click_to_sup_conv_move";
	private final static String CANNOT_SUPPORT_MOVE_NONADJACENT = "GUISupport.move_nonadj";
	private final static String CANNOT_SUPPORT_MOVE_GENERAL 	= "GUISupport.move_bad";
	private final static String CANNOT_SUPPORT_ACROSS_DPB 	= "GUISupport.over_dpb";
	
	
	
	// instance variables
	private transient static final int REQ_LOC = 3;
	private transient int currentLocNum = 0;
	private transient boolean dependentFound = false;	// true associated Move or Support order found
	private transient Point2D.Float failPt = null;
	private transient SVGGElement group = null;
	
	
	/** Creates a GUISupport */
	protected GUISupport()
	{
		super();
	}// GUISupport()
	
	
	/** Creates a GUISupport */
	protected GUISupport(Power power, Location src, Unit.Type srcUnitType, 
		Location supSrc, Power supPower, Unit.Type supUnitType)
	{
		super(power, src, srcUnitType, supSrc, supPower, supUnitType);
	}// GUISupport()
	
	
	/** Creates a GUISupport */
	protected GUISupport(Power power, Location src, Unit.Type srcUnitType, 
		Location supSrc, Power supPower, Unit.Type supUnitType, Location supDest)
	{
		super(power, src, srcUnitType, supSrc, supPower, supUnitType, supDest);
	}// GUISupport()
	
	
	/** 
	*	This only accepts Support orders. 
	*	All others will throw an IllegalArgumentException. 
	*/
	public void deriveFrom(Orderable order)
	{
		if( !(order instanceof Support) )
		{
			throw new IllegalArgumentException();
		}
		
		Support support = (Support) order;
		power = support.getPower();
		src = support.getSource();
		srcUnitType = support.getSourceUnitType();
		
		supSrc = support.getSupportedSrc();
		supDest = support.getSupportedDest();
		supPower = support.getSupportedPower();
		supUnitType = support.getSupportedUnitType();
		narrowingOrder = support.getNarrowingOrder();
		
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
			// set Support origin (supporting unit)
			// we require a unit present. We will check unit ownership too, if appropriate
			Unit unit = position.getUnit(province);
			if(unit != null)
			{
				if( !stateInfo.canIssueOrder(unit.getPower()) )
				{
					sb.append( Utils.getLocalString(GUIOrder.NOT_OWNER, unit.getPower()) );
					return false;
				}
				
				// check borders
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
			// set Support source (unit receiving support)
			// - If we are not validating, any location with a unit is acceptable (even source)
			// - If we are validating, 
			//
			if(stateInfo.getValidationOptions().getOption(ValidationOptions.KEY_GLOBAL_PARSING).equals(ValidationOptions.VALUE_GLOBAL_PARSING_LOOSE))
			{
				// lenient parsing enabled; we'll take anything with a unit!
				if(position.hasUnit(province))
				{
					sb.append( Utils.getLocalString(CLICK_TO_SUPPORT_UNIT) );
					return true;
				}
				
				// no unit in province
				sb.append( Utils.getLocalString(NO_UNIT_TO_SUPPORT) );
				return false;
			}
			
			// strict parsing is enabled. We are more selective.
			// This location must contain a unit, and not be the same as the unit originating support.
			//
			if(province == src.getProvince())
			{
				sb.append( Utils.getLocalString(CANNOT_SUPPORT_SELF) );
				return false;
			}
			else if(position.hasUnit(province))
			{
				// check borders
				Unit supUnit = position.getUnit(province);
				if( !GUIOrderUtils.checkBorder(this, new Location(province, supUnit.getCoast()), supUnit.getType(), stateInfo.getPhase(), sb) )
				{
					return false;
				}
				
				// check base movement modifier (DPB)
				if(province.getBaseMoveModifier(getSource()) < 0)
				{
					sb.append( Utils.getLocalString(CANNOT_SUPPORT_ACROSS_DPB) );
					return false;
				}
				
				sb.append( Utils.getLocalString(CLICK_TO_SUPPORT_UNIT) );
				return true;
			}
			
			// no unit in province
			sb.append( Utils.getLocalString(NO_UNIT_TO_SUPPORT) );
			return false;
		}
		else if(currentLocNum == 2)
		{
			// set Supporting-into Location
			// - If we are not validating, any destination is acceptable (even source)
			// - If we are validating, we check that the Support is adjacent
			//
			if(stateInfo.getValidationOptions().getOption(ValidationOptions.KEY_GLOBAL_PARSING).equals(ValidationOptions.VALUE_GLOBAL_PARSING_LOOSE))
			{
				// lenient parsing enabled; we'll take anything!
				sb.append( Utils.getLocalString(CLICK_TO_SUPPORT_FROM, province.getFullName()) );
				return true;
			}
			
			// strict parsing is enabled. We are more selective.
			// This location must be adjacent to the origin
			//
			if(src.isAdjacent(province))
			{
				// special case: supportSrc == supportDest; we are supporting a hold.
				if(supSrc.getProvince() == province)
				{
					// NOTE: no border check required here.
					// we are supporting a unit holding in place.
					sb.append( Utils.getLocalString(CLICK_TO_SUPPORT_HOLD) );
					return true;
				}
				
				// for supported moves, we must do a Border check
				if( !GUIOrderUtils.checkBorder(this, location, supUnitType, stateInfo.getPhase(), sb) )
				{
					return false;
				}
				
				
				// all other cases (supported move)
				return checkMove(position, supSrc, location, sb);
			}
			
			// supDest is not adjacent to src province
			sb.append( Utils.getLocalString(SUP_DEST_NOT_ADJACENT, src.toLongString()) );
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
		supSrc = null;
		supDest = null;
		supPower = null;
		supUnitType = null;
		
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
				supSrc = new Location(location.getProvince(), unit.getCoast());
				supPower = unit.getPower();
				supUnitType = unit.getType();
				
				sb.setLength(0);
				sb.append( Utils.getLocalString(SUPPORTING_THIS_UNIT) );
				currentLocNum++;
				return true;
			}
			else if(currentLocNum == 2)
			{
				if(supSrc.getProvince() == location.getProvince())
				{
					supDest = null;	// this means we are supporting a Hold
				}
				else
				{
					supDest = new Location(location.getProvince(), location.getCoast());
				}
				
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
	*	For supported holds: draws a dashed line to a dashed octagon
	*	<p>
	*	For supported moves: draws a dashed line to the supported unit, 
	*	then draws a dashed circle around the unit, then 
	*	draws a dashed line with arrow representing the move.
	*
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
		// check dependent order status.
		boolean found = false;
		if(isSupportingHold())
		{
			// Support a hold
			found = (GUIOrderUtils.findMatchingHold(mapInfo, 
								supSrc.getProvince()) != null);
		}
		else
		{
			// Support a move
			found = (GUIOrderUtils.findMatchingMove(mapInfo, 
								supSrc.getProvince(), supDest.getProvince()) != null);
		}
		
		if(group != null && dependentFound == found)
		{
			return;	// no change, and we are not newly created
		}
		
		// we are only at this point if a change has occured.
		//
		dependentFound = found;
		
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
		
		// draw after  determining if we are supporting a hold or not
		if(isSupportingHold())
		{
			updateDOMHold(mapInfo);
		}
		else
		{
			updateDOMMove(mapInfo);
		}
		
		// draw 'failed' marker, if appropriate.
		if(!mapInfo.getTurnState().isOrderSuccessful(this))
		{
			SVGElement useElement = GUIOrderUtils.createFailedOrderSymbol(mapInfo, failPt.x, failPt.y);
			group.appendChild(useElement);
		}
	}// updateDOM()
	
	
	/** Draw Supported Hold order */
	private void updateDOMHold(MapInfo mapInfo)
	{
		SVGElement[] elements = null;
		
		// create hilight line
		String cssStyle = mapInfo.getMapMetadata().getOrderParamString(MapMetadata.EL_SUPPORT, MapMetadata.ATT_HILIGHT_CLASS);
		if(!cssStyle.equalsIgnoreCase("none"))
		{
			float offset = mapInfo.getMapMetadata().getOrderParamFloat(MapMetadata.EL_SUPPORT, MapMetadata.ATT_HILIGHT_OFFSET);	
			elements = drawSupportedHold(mapInfo, offset);
			GUIOrderUtils.makeHilight(elements, mapInfo.getMapMetadata(), MapMetadata.EL_SUPPORT);
			for(int i=0; i<elements.length; i++)
			{
				group.appendChild(elements[i]);
			}
		}
		
		// create real line
		elements = drawSupportedHold(mapInfo, 0);
		GUIOrderUtils.makeStyled(elements, mapInfo.getMapMetadata(), MapMetadata.EL_SUPPORT, power);
		for(int i=0; i<elements.length; i++)
		{
			group.appendChild(elements[i]);
		}
	}// updateDOMHold()
	
	
	/** Draw Supported Move order */
	private void updateDOMMove(MapInfo mapInfo)
	{
		SVGElement[] elements = null;
		
		// create hilight line
		String cssStyle = mapInfo.getMapMetadata().getOrderParamString(MapMetadata.EL_SUPPORT, MapMetadata.ATT_HILIGHT_CLASS);
		if(!cssStyle.equalsIgnoreCase("none"))
		{
			float offset = mapInfo.getMapMetadata().getOrderParamFloat(MapMetadata.EL_SUPPORT, MapMetadata.ATT_HILIGHT_OFFSET);	
			elements = drawSupportedMove(mapInfo, offset, false);
			GUIOrderUtils.makeHilight(elements, mapInfo.getMapMetadata(), MapMetadata.EL_SUPPORT);
			for(int i=0; i<elements.length; i++)
			{
				group.appendChild(elements[i]);
			}
		}
		
		// create real line
		elements = drawSupportedMove(mapInfo, 0, true);
		GUIOrderUtils.makeStyled(elements, mapInfo.getMapMetadata(), MapMetadata.EL_SUPPORT, power);
		for(int i=0; i<elements.length; i++)
		{
			group.appendChild(elements[i]);
		}
	}// updateDOMMove()	
	
	
	/**
	*	Note: we don't draw the 3rd part (supSrc->supDest) if moveFound == false.
	*
	*
	*/
	private SVGElement[] drawSupportedMove(MapInfo mapInfo, float offset, boolean addMarker)
	{
		// setup
		//SVGElement[] elements = new SVGElement[ ((dependentFound) ? 1 : 3) ];
		SVGElement[] elements = new SVGElement[1];
		
		Position position = mapInfo.getTurnState().getPosition();
		
		MapMetadata mmd = mapInfo.getMapMetadata();
		Point2D.Float ptSrc = mmd.getUnitPt(src.getProvince(), src.getCoast());
		Point2D.Float ptSupSrc = mmd.getUnitPt(supSrc.getProvince(), supSrc.getCoast());
		Point2D.Float ptSupDest = mmd.getUnitPt(supDest.getProvince(), supDest.getCoast());
		
		
		
		// adjust for offset
		ptSrc.x += offset;
		ptSrc.y += offset;
		ptSupSrc.x += offset;
		ptSupSrc.y += offset;
		ptSupDest.x += offset;
		ptSupDest.y += offset;
		
		// destination. If no unit, use the size of an army unit radius divided
		// by 2 (as we do in GUIMove)
		//
		Point2D.Float newSupDest = null;
		if(position.hasUnit(supDest.getProvince()))
		{
			// since we're supporting a Move, we should use the Move radius
			Unit.Type destUnitType = position.getUnit(supDest.getProvince()).getType();
			float moveRadius = mmd.getOrderRadius(MapMetadata.EL_MOVE, mapInfo.getSymbolName(destUnitType));
			newSupDest = GUIOrderUtils.getLineCircleIntersection(ptSupSrc.x, ptSupSrc.y, ptSupDest.x, ptSupDest.y, ptSupDest.x, ptSupDest.y, moveRadius);
		}
		else
		{
			float moveRadius = (mmd.getOrderRadius(MapMetadata.EL_MOVE, mapInfo.getSymbolName(Unit.Type.ARMY)) / 2);
			newSupDest = GUIOrderUtils.getLineCircleIntersection(ptSupSrc.x, ptSupSrc.y, ptSupDest.x, ptSupDest.y, ptSupDest.x, ptSupDest.y, moveRadius);
		}
		
		// calculate failed point marker
		// this is sort of a hack; may not really coincide with bezier curve!!
		// seems to work fairly well, though. keep for now.
		failPt = GUIOrderUtils.getLineMidpoint(ptSrc.x, ptSrc.y, ptSupSrc.x, ptSupSrc.y);
		
		// new test code -- bezier
		elements[0] = 	(SVGPathElement) 
						mapInfo.getDocument().createElementNS(
						SVGDOMImplementation.SVG_NAMESPACE_URI, 
						SVGConstants.SVG_PATH_TAG);
						
						
		StringBuffer sb = new StringBuffer();
		
		sb.append("M ");
		GUIOrderUtils.appendFloat(sb, ptSrc.x);	// unit start
		sb.append(',');
		GUIOrderUtils.appendFloat(sb, ptSrc.y);
		
		sb.append(" C ");
		
		GUIOrderUtils.appendFloat(sb, ptSupSrc.x);	// supporting unit
		sb.append(',');
		GUIOrderUtils.appendFloat(sb, ptSupSrc.y);
		sb.append(' ');
		
		GUIOrderUtils.appendFloat(sb, ptSupSrc.x);	// supporting unit
		sb.append(',');
		GUIOrderUtils.appendFloat(sb, ptSupSrc.y);
		sb.append(' ');
		
		GUIOrderUtils.appendFloat(sb, newSupDest.x);	// destination
		sb.append(',');
		GUIOrderUtils.appendFloat(sb, newSupDest.y);
		sb.append(' ');
		
		elements[0].setAttributeNS(null, SVGConstants.SVG_D_ATTRIBUTE, sb.toString());
		elements[0].setAttributeNS(null, SVGConstants.SVG_CLASS_ATTRIBUTE, 
					mmd.getOrderParamString(MapMetadata.EL_SUPPORT, MapMetadata.ATT_STROKESTYLE));
		
		
		if(addMarker || offset != 0.0f)
		{
			GUIOrderUtils.addMarker(elements[0], mmd, MapMetadata.EL_SUPPORT);
		}
		
		// return
		return elements;
	}// drawSupportedMove()
	
	
	private SVGElement[] drawSupportedHold(MapInfo mapInfo, float offset)
	{
		// setup
		SVGElement[] elements = new SVGElement[ ((dependentFound) ? 1 : 2) ];
		
		MapMetadata mmd = mapInfo.getMapMetadata();
		Point2D.Float ptSrc = mmd.getUnitPt(src.getProvince(), src.getCoast());
		Point2D.Float ptSupSrc = mmd.getUnitPt(supSrc.getProvince(), supSrc.getCoast());
		
		// supUnitType shouldn't be null here...
		float radius = mmd.getOrderRadius(MapMetadata.EL_SUPPORT, mapInfo.getSymbolName(supUnitType));
		
		// adjust for offset
		ptSrc.x += offset;
		ptSrc.y += offset;
		ptSupSrc.x += offset;
		ptSupSrc.y += offset;
		
		// draw line to the octagon
		Point2D.Float newPtTo = GUIOrderUtils.getLineCircleIntersection(ptSrc.x, ptSrc.y, 
								ptSupSrc.x, ptSupSrc.y, ptSupSrc.x, ptSupSrc.y, radius);
								
		elements[0] = 	(SVGLineElement) 
						mapInfo.getDocument().createElementNS(
						SVGDOMImplementation.SVG_NAMESPACE_URI, 
						SVGConstants.SVG_LINE_TAG);
		
		elements[0].setAttributeNS(null, SVGConstants.SVG_X1_ATTRIBUTE, GUIOrderUtils.floatToString(ptSrc.x));
		elements[0].setAttributeNS(null, SVGConstants.SVG_Y1_ATTRIBUTE, GUIOrderUtils.floatToString(ptSrc.y));
		elements[0].setAttributeNS(null, SVGConstants.SVG_X2_ATTRIBUTE, GUIOrderUtils.floatToString(newPtTo.x));
		elements[0].setAttributeNS(null, SVGConstants.SVG_Y2_ATTRIBUTE, GUIOrderUtils.floatToString(newPtTo.y));
		elements[0].setAttributeNS(null, SVGConstants.SVG_CLASS_ATTRIBUTE, 
					mmd.getOrderParamString(MapMetadata.EL_SUPPORT, MapMetadata.ATT_STROKESTYLE));
		
		// calculate (but don't yet use) failPt
		failPt = GUIOrderUtils.getLineMidpoint(ptSrc.x, ptSrc.y, newPtTo.x, newPtTo.y);
		
		// draw octagon
		if(!dependentFound)
		{
			Point2D.Float[] pts = GUIOrderUtils.makeOctagon(ptSupSrc, radius);
			
			StringBuffer sb = new StringBuffer(160);
			for(int i=0; i<pts.length; i++)
			{
				GUIOrderUtils.appendFloat(sb, pts[i].x);
				sb.append(',');
				GUIOrderUtils.appendFloat(sb, pts[i].y);
				sb.append(' ');
			}
			
			elements[1] = 	(SVGPolygonElement) 
							mapInfo.getDocument().createElementNS(
							SVGDOMImplementation.SVG_NAMESPACE_URI, 
							SVGConstants.SVG_POLYGON_TAG);
							
			elements[1].setAttributeNS(null, SVGConstants.SVG_POINTS_ATTRIBUTE, sb.toString());
			elements[1].setAttributeNS(null, SVGConstants.SVG_CLASS_ATTRIBUTE, mmd.getOrderParamString(MapMetadata.EL_SUPPORT, MapMetadata.ATT_STROKESTYLE));
		}
		
		// return
		return elements;
	}// drawSupportedHold()
	
	/** We are dependent upon other orders to determine how we render this order. */
	public boolean isDependent()	{ return true; }
	
	
	/** Check a Move between two locations, for adjacency (even by theoretical convoy route). */
	private boolean checkMove(Position position, Location from, Location to, StringBuffer sb)
	{
		if(from.isAdjacent(to.getProvince()))
		{
			sb.append( Utils.getLocalString(CLICK_TO_SUPPORT_MOVE) );
			return true;
		}
		else if(from.getProvince().isCoastal() && supUnitType == Unit.Type.ARMY)
		{
			// NOTE: assume destination coast is Coast.NONE
			Path path = new Path(position);
			if(path.isPossibleConvoyRoute(from, new Location(to.getProvince(), Coast.NONE)))
			{
				sb.append( Utils.getLocalString(CLICK_TO_SUPPORT_CONVOYED_MOVE) );
				return true;
			}
			else
			{
				sb.append( Utils.getLocalString(CANNOT_SUPPORT_MOVE_NONADJACENT, from.getProvince().getFullName()) );
				return false;
			}
		}
		
		sb.append(Utils.getLocalString(CANNOT_SUPPORT_MOVE_GENERAL, from.getProvince().getFullName()) );
		return false;
	}// checkMove()


	
}// class GUISupport
