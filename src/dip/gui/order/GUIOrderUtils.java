//
//  @(#)GUIOrderUtils.java	12/2002
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

import dip.world.Location;
import dip.world.Unit;
import dip.world.Phase;
import dip.world.Power;
import dip.world.Province;
import dip.world.Border;
import dip.world.TurnState;

import dip.order.Orderable;
import dip.order.Move;
import dip.order.Hold;
import dip.order.Support;

import dip.misc.Utils;

import dip.gui.map.MapMetadata;
import dip.gui.map.DefaultMapRenderer2;
import dip.gui.map.SVGUtils;

import dip.gui.order.GUIOrder.MapInfo;

import java.awt.geom.Point2D;
import java.util.List;
import java.util.Iterator;

import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

import org.apache.batik.util.SVGConstants;
import org.apache.batik.util.CSSConstants;
import org.apache.batik.dom.svg.SVGDOMImplementation;
import org.apache.batik.dom.util.XLinkSupport;
import org.w3c.dom.svg.*;
import org.w3c.dom.*;

/**
*	Utility methods for GUIOrder subclasses.
*	
*
*
*
*
*
*
*/
final class GUIOrderUtils
{
	/** Amount to make DifficultPassableBorder Lines, compared to normal. Should be in the (0, 1.0) range. */
	private final static float DPB_LINE_WIDTH = 0.333f;
	
	
	/**
	*	Determines if the given border allows a given action. 
	*	<p>
	*	Please note that this uses the superclass for any passed GUIOrder
	*	object. (e.g., dip.order.Hold for dip.gui.order.GUIHold)
	*	<p>
	*	GUIOrder.BORDER_INVALID is appended to the StringBuffer, if 'false' is returned.
	*/
	public static boolean checkBorder(GUIOrder guiOrder, Location location, Unit.Type unitType, Phase phase, StringBuffer sb)
	{
		Class baseClass = guiOrder.getClass().getSuperclass();
		Border border = location.getProvince().getTransit(location, unitType, phase, baseClass);
		if(border != null)
		{
			sb.append( Utils.getLocalString(GUIOrder.BORDER_INVALID, guiOrder.getFullName(), border.getDescription()) );
			return false;
		}
		
		return true;
	}// checkBorder()
	
	
	/** Creates the points for an Octagon about center point of a given radius. */
	public static Point2D.Float[] makeOctagon(Point2D.Float center, float radius)
	{
		// A polygon has 8 sides. All sides are equal lengths. The top/bottom & r/l sides if 
		// bisected will form a right triangle with angles (67.5, 90, and 22.5). One side is
		// the radius; thus the length ('a') of the bisected side of the triangle is:
		// 		a = tan(22.5 deg) * radius.
		// thus the top/left/bottom/right sides have a length of 2*a. However, all we need is
		// 'a' since we can derive all polygon points once 'a' is known. (they are all a combination
		// of (center point +/- radius),(center point +/- a) and (center point +/- a),(center point +/- radius)
		//
		// compute a:
		// note: 22.5 degrees = 360 / 8 / 2. or 2*PI/8/2 = PI/8.
		float a = (float) Math.tan(Math.PI/8) * radius;
		
		Point2D.Float[] points = new Point2D.Float[8];
		for(int i=0; i<points.length; i++)
		{
			points[i] = new Point2D.Float();
		}
		
		// 8 points starting clockwise; first point would be approximately 1 o'clock position
		// this is just more clear then reflection. Precalc could occur if required. But whatever.
		points[0].x = center.x + a;
		points[0].y = center.y - radius;
		
		points[1].x = center.x + radius;
		points[1].y = center.y - a;
		
		points[2].x = center.x + radius;
		points[2].y = center.y + a;
		
		points[3].x = center.x + a;
		points[3].y = center.y + radius;
		
		points[4].x = center.x - a;
		points[4].y = center.y + radius;
		
		points[5].x = center.x - radius;
		points[5].y = center.y + a;
		
		points[6].x = center.x - radius;
		points[6].y = center.y - a;
		
		points[7].x = center.x - a;
		points[7].y = center.y - radius;
		
		// return
		return points;
	}// makeOctagon()
	
	
	/** Creates the points for an equilateral Triangle about center point of a given radius. */
	public static Point2D.Float[] makeTriangle(Point2D.Float center, float radius)
	{
		float a = (float) (Math.cos(Math.PI/6) * radius);
		float b = (float) (Math.sin(Math.PI/6) * radius);
		
		Point2D.Float[] points = new Point2D.Float[3];
		for(int i=0; i<points.length; i++)
		{
			points[i] = new Point2D.Float();
		}
		
		// 3 points starting clockwise at 12'o'clock
		points[0].x = center.x;
		points[0].y = center.y - radius;
		
		points[1].x = center.x + a;
		points[1].y = center.y + b;
		
		points[2].x = center.x - a;
		points[2].y = center.y + b;
		
		// return
		return points;
	}// makeTriangle()

	
	/** 
	*	Respect a Radius
	*	<p>
	*	Computes a point-line intersection and returns the result as a Point2D.Float
	*	object. This returns the intersected point. 
	*	<p>
	*	x1,y1 and x2,y2 are line coordinates
	*	x3,y3 and R are the center and Radius of the circle to check for intersection.
	*
	*	<p>
	*	<b>NOTE:</b> This assumes that the point and line intersect. 
	*	<p>
	*	If x1,y1 == x2,y2 (this can happen if order verification is lenient) or other 
	*	condition occurs where the internal value mu is NaN, we will return the 
	*	point (x1,y1).
	*
	*/
	public static Point2D.Float getLineCircleIntersection(float x1, float y1, float x2, float y2, float x3, float y3, float r)
	{
		// how to respect radius: 
		/*
			via circle-line intersection:
			
			line: (x1,y1) to (x2,y2)
			circle: radius (r), center (x3,y3)
			
			intersection coordinates in ix, iy
		*/
		float a =  square(x2 - x1) + square(y2 - y1);
		float b =  2* ((x2 - x1)*(x1 - x3)+ (y2 - y1)*(y1 - y3));
		float c =  square(x3) + square(y3) + square(x1) + square(y1) - 2*(x3*x1 + y3*y1) - square(r);
		
		// x,y are calculated points of intersection
		// these put it 'past' the unit
		/*
		float mu = (float) ((-b + Math.sqrt( square(b) - 4*a*c )) / (2*a));
		float x = x1 + mu*(x2-x1);
		float y = y1 + mu*(y2-y1);
		*/
		
		// these put it 'before' the unit
		//
		float mu = (float) ((-b - Math.sqrt(square(b) - 4*a*c )) / (2*a));
		
		if(Float.isNaN(mu))
		{
			return new Point2D.Float(x1, y1);
		}
		
		Point2D.Float pt = new Point2D.Float();
		pt.x = x1 + mu*(x2-x1);
		pt.y = y1 + mu*(y2-y1);
		
		// debug code
		/*
		if(Float.isNaN(pt.x) || Float.isNaN(pt.y))
		{
			System.out.println("NAN! in getLineCircleIntersection()");
			System.out.println("args: "+x1+","+y1+","+x2+","+y2+","+x3+","+y3+","+r);
			System.out.println("a,b,c: "+a+","+b+","+c);
			System.out.println("mu: "+mu);
			System.out.println("pt: "+pt);
		}
		*/
		
		assert(!Float.isNaN(pt.x));
		assert(!Float.isNaN(pt.y));
		
		//System.out.println("args: L: "+x1+","+y1+","+x2+","+y2+", C: "+x3+","+y3+","+r);
		//System.out.println("   result: "+pt);
		
		return pt;
	}// getLineCircleIntersection()
	
	
	/** 
	*	Respect a Radius II
	*	<p>
	*	Computes a point-line intersection and returns the result as a Point2D.Float
	*	object. This returns the second intersected point, or if there is only a single
	*	point, the second point the line would hit if it was extended to intersect 
	*	the circle again.
	*	<p>
	*	<b>NOTE:</b> This assumes that the point and line intersect. 
	*	<p>
	*	If x1,y1 == x2,y2 (this can happen if order verification is lenient) or other 
	*	condition occurs where the internal value mu is NaN, we will return the 
	*	point (x1,y1).
	*
	*/
	public static Point2D.Float getLineCircleIntersectOuter(float x1, float y1, float x2, float y2, float x3, float y3, float r)
	{
		// how to respect radius: 
		/*
			via circle-line intersection:
			
			line: (x1,y1) to (x2,y2)
			circle: radius (r), center (x3,y3)
			
			intersection coordinates in ix, iy
		*/
		float a =  square(x2 - x1) + square(y2 - y1);
		float b =  2* ((x2 - x1)*(x1 - x3)+ (y2 - y1)*(y1 - y3));
		float c =  square(x3) + square(y3) + square(x1) + square(y1) - 2*(x3*x1 + y3*y1) - square(r);
		
		// x,y are calculated points of intersection
		// these put it 'past' the unit
		float mu = (float) ((-b + Math.sqrt( square(b) - 4*a*c )) / (2*a));
		
		if(Float.isNaN(mu))
		{
			return new Point2D.Float(x1, y1);
		}
		
		Point2D.Float pt = new Point2D.Float();
		pt.x = x1 + mu*(x2-x1);
		pt.y = y1 + mu*(y2-y1);
		
		// these put it 'before' the unit
		/*
		float mu = (float) ((-b - Math.sqrt(square(b) - 4*a*c )) / (2*a));
		
		Point2D.Float pt = new Point2D.Float();
		pt.x = x1 + mu*(x2-x1);
		pt.y = y1 + mu*(y2-y1);
		*/
		
		// debug code
		/*
		if(Float.isNaN(pt.x) || Float.isNaN(pt.y))
		{
			System.out.println("NAN! in getLineCircleIntersectOuter()");
			System.out.println("args: "+x1+","+y1+","+x2+","+y2+","+x3+","+y3+","+r);
			System.out.println("a,b,c: "+a+","+b+","+c);
			System.out.println("mu: "+mu);
			System.out.println("pt: "+pt);
		}
		*/
		
		assert(!Float.isNaN(pt.x));
		assert(!Float.isNaN(pt.y));
		
		return pt;
	}// getLineCircleIntersectOuter()
	
	
	/** 
	*	Applies the appropriate Stroke color and Filter (if any) to an element. 
	*	<p>	
	*	mmdOrderElementName = e.g., MapMetadata.EL_HOLD
	*/
	public static void makeStyled(SVGElement element, MapMetadata mmd, String mmdOrderElementName, Power power)
	{
		element.setAttributeNS(null, CSSConstants.CSS_STROKE_PROPERTY, mmd.getPowerColor(power));
		String filter = mmd.getOrderParamString(mmdOrderElementName, MapMetadata.ATT_FILTERID);
		if(filter.length() > 0)
		{
			StringBuffer sb = new StringBuffer(filter.length() + 6);
			sb.append("url(#");
			sb.append(filter);
			sb.append(')');
			element.setAttributeNS(null, SVGConstants.SVG_FILTER_ATTRIBUTE, sb.toString());
		}
	}// makeStyled()
	
	/** 
	*	Applies the appropriate Stroke color and Filter (if any) to an array of elements
	*	<p>	
	*	mmdOrderElementName = e.g., MapMetadata.EL_HOLD
	*/
	public static void makeStyled(SVGElement[] elements, MapMetadata mmd, String mmdOrderElementName, Power power)
	{
		String filter = mmd.getOrderParamString(mmdOrderElementName, MapMetadata.ATT_FILTERID);
		if(filter.length() > 0)
		{
			StringBuffer sb = new StringBuffer(filter.length() + 6);
			sb.append("url(#");
			sb.append(filter);
			sb.append(')');
			filter = sb.toString();
		}
		else
		{
			filter = null;
		}
		
		String powerColor = mmd.getPowerColor(power);
		
		for(int i=0; i<elements.length; i++)
		{
			elements[i].setAttributeNS(null, CSSConstants.CSS_STROKE_PROPERTY, powerColor);
			if(filter != null)
			{
				elements[i].setAttributeNS(null, SVGConstants.SVG_FILTER_ATTRIBUTE, filter);
			}
		}
	}// makeStyled()
	
	/** 
	*	Sets the higlight of an element. Assumes that hilight is not set to 'none'
	*/
	public static void makeHilight(SVGElement element, MapMetadata mmd, String mmdOrderElementName)
	{
		String cssStyle = mmd.getOrderParamString(mmdOrderElementName, MapMetadata.ATT_HILIGHT_CLASS);
		element.setAttributeNS(null, SVGConstants.SVG_CLASS_ATTRIBUTE, cssStyle);
	}// makeHilight()
	
	/** 
	*	Sets the higlight of an array of elements. Assumes that hilight is not set to 'none'
	*/
	public static void makeHilight(SVGElement[] elements, MapMetadata mmd, String mmdOrderElementName)
	{
		String cssStyle = mmd.getOrderParamString(mmdOrderElementName, MapMetadata.ATT_HILIGHT_CLASS);
		for(int i=0; i<elements.length; i++)
		{
			elements[i].setAttributeNS(null, SVGConstants.SVG_CLASS_ATTRIBUTE, cssStyle);
		}
	}// makeHilight()
	
	
	/**
	*	Adds the given end-Marker to an element (usually a Line)
	*
	*/
	public static void addMarker(SVGElement element, MapMetadata mmd, String mmdOrderElementName)
	{
		element.setAttributeNS(null, CSSConstants.CSS_MARKER_END_PROPERTY, 
			"url(#"+mmd.getOrderParamString(mmdOrderElementName, MapMetadata.ATT_MARKERID)+')');
	}// addMarker()
	
	
	/**
	* Given a TurnState, determines if any order exists that matches the
	* given Move order. Returns null if no matching Move order found.
	* 
	*/
	public static Move findMatchingMove(MapInfo mapInfo, Province src, Province dest)
	{
		Power[] powers = mapInfo.getDisplayablePowers();
		for(int i=0; i<powers.length; i++)
		{
			List orders = mapInfo.getTurnState().getOrders(powers[i]);
			Iterator iter = orders.iterator();
			while(iter.hasNext())
			{
				Orderable o = (Orderable) iter.next();
				if(o instanceof Move)
				{
					Move mv = (Move) o;
					if( mv.getSource().isProvinceEqual(src) 
						&& mv.getDest().isProvinceEqual(dest) )
					{
						return mv;
					}
				}
			}
		}
		
		return null;
	}// findMatchingMove()	
	
	
	/**
	* Given a TurnState, determines if any order exists that matches the
	* given Hold order. Returns null if no matching Hold order found.
	* 
	*/
	public static Hold findMatchingHold(MapInfo mapInfo, Province src)
	{
		Power[] powers = mapInfo.getDisplayablePowers();
		for(int i=0; i<powers.length; i++)
		{
			List orders = mapInfo.getTurnState().getOrders(powers[i]);
			Iterator iter = orders.iterator();
			while(iter.hasNext())
			{
				Orderable o = (Orderable) iter.next();
				if(o instanceof Hold && o.getSource().isProvinceEqual(src))
				{
					return (Hold) o;
				}
			}
		}
		
		return null;
	}// findMatchingHold()	
	
	/**
	* 	Given a TurnState, determines if the number (if any) of .
	* 	Support orders that match the given Move or Hold order.
	* 	(use src == dest for Hold orders)
	*	<p>
	*	Note that only the displayable powers are used to check
	* 	the support.
	*/
	public static int getMatchingSupportCount(MapInfo mapInfo, Province supSrc, Province supDest)
	{
		int count = 0;
		
		Power[] powers = mapInfo.getDisplayablePowers();
		for(int i=0; i<powers.length; i++)
		{
			List orders = mapInfo.getTurnState().getOrders(powers[i]);
			Iterator iter = orders.iterator();
			while(iter.hasNext())
			{
				Orderable o = (Orderable) iter.next();
				if(o instanceof Support)
				{
					Support sup = (Support) o;
					if( sup.getSupportedSrc().isProvinceEqual(supSrc) 
						&& sup.getSupportedDest().isProvinceEqual(supDest) )
					{
						count++;
					}
				}
			}
		}
		
		return count;
	}// findMatchingSupports()
	
	
	/**
	*	Checks that support width is in-bounds. Note that negative widths are possible (see the 
	*	base move modifier; e.g., Loeb9). If the support is out-of-bounds (max), the largest 
	*	width is returned.
	*	<p>
	*	All negative widths are treated alike; DPB_LINE_WIDTH times the value of the 
	*	smallest width in the line-width list (index 0).
	*/
	public static float getLineWidth(MapInfo mapInfo, String mmdElementName, String mmdElementType, int support)
	{
		int idx = support;
		if(support < 0)
		{
			idx = 0;
		}
		
		final float[] widths = mapInfo.getMapMetadata().getOrderParamFloatArray(mmdElementName, mmdElementType);
		if(support >= widths.length)
		{
			return widths[widths.length-1];
		}
		
		return (support >= 0) ? widths[idx] : (widths[idx] * DPB_LINE_WIDTH);
	}// getLineWidth()
	
	
	/**
	*	Returns the midpoint of a line.
	*
	*/
	public static Point2D.Float getLineMidpoint(float x1, float y1, float x2, float y2)
	{
		Point2D.Float p2d = new Point2D.Float();
		
		p2d.x = (x1 + x2) / 2.0f;
		p2d.y = (y1 + y2) / 2.0f;
		
		return p2d;
	}// getLineMidpoint()
	
	/**
	* 	Create a &lt;use&gt; element with a SYMBOL_FAILEDORDER at the
	*	given coordinates, sized appropriately.
	*/
	public static SVGUseElement createFailedOrderSymbol(MapInfo mapInfo, float x, float y)
	{
		MapMetadata.SymbolSize symbolSize = 
			mapInfo.getMapMetadata().getSymbolSize(DefaultMapRenderer2.SYMBOL_FAILEDORDER);
		
		return SVGUtils.createUseElement(
			mapInfo.getDocument(),
			"#"+DefaultMapRenderer2.SYMBOL_FAILEDORDER,
			null, // no id
			null, // no special style
			x,
			y,
			symbolSize);
	}// createFailedOrderSymbol()

	
	/**
	*	Determine if the passed SVGGElement has any children. If it does, 
	*	delete them. 
	*/
	public static void deleteChildren(SVGGElement g)
	{
		if(g == null)
		{
			throw new IllegalArgumentException();
		}
		
		Node child = g.getFirstChild();
		while(child != null)
		{
			g.removeChild( child );
			child = g.getFirstChild();
		}
	}// deleteChildren()
	
	
	/**
	*	Returns true if the given power is a member of the displayble
	*	powers group, <b>or</b> the TurnState is resolved (and orders
	*	for all powers can be shown)
	*/
	public static boolean isDisplayable(final Power power, final MapInfo mapInfo)
	{
		if(mapInfo.getTurnState().isResolved())
		{
			return true;
		}
		
		final Power[] displayedPowers = mapInfo.getDisplayablePowers();
		for(int i=0; i<displayedPowers.length; i++)
		{
			if(displayedPowers[i] == power)
			{
				return true;
			}
		}
		
		return false;
	}// isDisplayable()
	
	
	/**
	*	Removes a child element from the parent. If child is present,
	*	and succesfully removed, returns <code>true</code>. Otherwise,
	*	this method returns <code>false</code>. Null arguments are not
	*	permissable. No exceptions will be returned.
	*/
	public static boolean removeChild(final Element parent, final Element child)
	{
		Node node = parent.getFirstChild();
		while(node != null)
		{
			if(node == child)
			{
				try
				{
					parent.removeChild(node);
					return true;
				}
				catch(DOMException e)
				{
					return false;
				}
			}
			
			node = node.getNextSibling();
		}
		
		return false;
	}// removeChild()
	
	
	
	
	/** Squares the given value */
	private static float square(float v)
	{
		return (v*v);
	}// square()
	
	
	/** 
	*	Formats a Floating-Point value into a String,
	*	using the jDip default precision.
	*/
	public static String floatToString(float v)
	{
		return SVGUtils.floatToString(v);
	}// toString()
	
	/** 
	*	Formats a Floating-Point value into a StringBuffer,
	*	using the jDip default precision.
	*/
	public static void appendFloat(StringBuffer sb, float v)
	{
		SVGUtils.appendFloat(sb, v);
	}// appendFloat()
	
}// class GUIOrderUtils
