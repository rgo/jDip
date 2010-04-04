//
//  @(#)MapMetadata.java		10/2002
//
//  Copyright 2002,2003 Zachary DelProposto. All rights reserved.
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
package dip.gui.map;

import dip.world.Power;
import dip.world.Province;
import dip.world.Coast;

import dip.world.variant.data.Symbol;
import dip.world.variant.data.SymbolPack;

import dip.misc.Log;

import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.NoSuchElementException;
import java.util.Properties;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.io.IOException;

import java.awt.geom.Point2D;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.svg.SVGDocument;
import org.w3c.dom.svg.SVGElement;

import org.apache.batik.util.SVGConstants;


/**
*	Extracts map information, and SVG Elements, from the SVG file.
*	Metadata (jDip-specific, in its own XML namespace) is also extracted.
*	<p>
*
*
*
*
*/
public class MapMetadata
{
	/** jDip namespace constant */
	public static final String JDIP_NAMESPACE = "http://jdip.sourceforge.org/jdipNS";
	
	/** jDip namespace element: for display control */
	public static final String EL_DISPLAY	= "DISPLAY";
	/** jDip namespace element: for display control (zoom) */
	public static final String EL_ZOOM		= "ZOOM";
	/** jDip namespace element: for display control (labels) */
	public static final String EL_LABELS	= "LABELS";
	
	/** jDip namespace attributes: display: zoom */
	public static final String ATT_ZOOM_MIN		= "min";
	/** jDip namespace attributes: display: zoom */
	public static final String ATT_ZOOM_MAX		= "max";
	/** jDip namespace attributes: display: zoom */
	public static final String ATT_ZOOM_FACTOR	= "factor";
	/** jDip namespace attributes: display: labels */
	public static final String ATT_LABELS_BRIEF	= "brief";
	/** jDip namespace attributes: display: labels */
	public static final String ATT_LABELS_FULL	= "full";
	
	/** jDip namespace element: for province metadata (main grouping) */
	public static final String EL_PROVINCE_DATA 	= "PROVINCE_DATA";
	/** jDip namespace element: for province metadata: province */
	public static final String EL_PROVINCE 			= "PROVINCE";
	/** jDip namespace element: for province metadata: units */
	public static final String EL_UNIT 				= "UNIT";
	/** jDip namespace element: for province metadata: dislodged units */
	public static final String EL_DISLODGED_UNIT 	= "DISLODGED_UNIT";
	/** jDip namespace element: for province metadata: supply centers */
	public static final String EL_SC 				= "SUPPLY_CENTER";
	
	/** jDip namespace attributes: province */
	public static final String ATT_X	= "x";
	/** jDip namespace attributes: province */
	public static final String ATT_Y	= "y";
	/** jDip namespace attributes: province, symbolsize */
	public static final String ATT_NAME	= "name";
	
	/** jDip namespace for Order metadata */
	public static final String EL_ORDERDRAWING 		= "ORDERDRAWING";	
	/** Hold order drawing parameter element */
	public static final String EL_HOLD				= "HOLD";
	/** Disband order drawing parameter element */
	public static final String EL_DISBAND			= "DISBAND";
	/** Remove order drawing parameter element */
	public static final String EL_REMOVE			= "REMOVE";
	/** Build order drawing parameter element */
	public static final String EL_BUILD				= "BUILD";
	/** Move order drawing parameter element */
	public static final String EL_MOVE				= "MOVE";
	/** Retreat order drawing parameter element */
	public static final String EL_RETREAT			= "RETREAT";
	/** Support order drawing parameter element */
	public static final String EL_SUPPORT			= "SUPPORT";
	/** Convoy order drawing parameter element */
	public static final String EL_CONVOY			= "CONVOY";
	/** Waive order drawing parameter element */
	public static final String EL_WAIVE				= "WAIVE";
	/** Power Colors group */
	public static final String EL_POWERCOLORS		= "POWERCOLORS";
	/** Power Color item */
	public static final String EL_POWERCOLOR		= "POWERCOLOR";
	/** Symbol Size element */
	public static final String EL_SYMBOLSIZE		= "SYMBOLSIZE";
	
	
	/** jDip namespace attribute: (optional) province data: dislodgedUnitOffset */
	private static final String ATT_DISLODGED_OFFSET	= "dislodgedUnitOffset";
	/** jDip namespace attribute: order drawing: deltaRadius */
	public static final String ATT_DELTA_RADIUS		= "deltaRadius";
	/** jDip namespace attribute: order drawing: stroke CSS style */
	public static final String ATT_STROKESTYLE		= "strokeCSSStyle";
	/** jDip namespace attribute: order drawing: filter ID */
	public static final String ATT_FILTERID			= "filterID";
	/** jDip namespace attribute: order drawing: marker ID */
	public static final String ATT_MARKERID			= "markerID";
	/** jDip namespace attribute: power colors: power name */
	public static final String ATT_POWER			= "power";
	/** jDip namespace attribute: power colors: color name/value */
	public static final String ATT_COLOR			= "color";
	/** jDip namespace attribute: order drawing: highlight offset (float) */
	public static final String ATT_HILIGHT_OFFSET	= "hilightOffset";
	/** jDip namespace attribute: order drawing: highlight class */
	public static final String ATT_HILIGHT_CLASS	= "hilightCSSClass";
	/** jDip namespace attribute: order drawing: order line widths */
	public static final String ATT_WIDTHS			= "widths";
	/** jDip namespace attribute: order drawing: order shadow line widths */
	public static final String ATT_SHADOW_WIDTHS	= "shadowWidths";
	/** jDip namespace attribute: symbol size: symbol width */
	public static final String ATT_WIDTH			= "width";
	/** jDip namespace attribute: symbol size: symbol height */
	public static final String ATT_HEIGHT			= "height";
	
	
	/** Internal constant for a coordinate at (0.0, 0.0) */
	private static final Point2D.Float POINT_ZERO = new Point2D.Float(0.0f, 0.0f);
	
	
	// instance variables
	private Map infoMap;				// placement info
	private HashMap displayProps;		// display info
	private final MapPanel mp;
	private Point2D.Float dislodgedUnitOffset = null;
	private boolean supressPlacementErrors = false;
	private SymbolPack sp = null;
	
	/*
	*	Display Props is also used for order info. Except that
	*	there are 2 keys (element, attribute), and no default value, since 
	*	all values are required. 
	*
	*
	*/
	
	
	/** 
	*	Create a MapMetadata Object, by parsing the
	*	SVG placement id group metadata 
	*	<p>
	*	If supressPlacementErrors are suppressed, using the 
	*	appropriate boolean flag, only valid parsed placement
	*	data will be used. Data which is missing, or invalid,
	*	will be replaced with 0 values. Display and Order Drawing
	*	metadata parsing is unaffected by this flag.
	*	
	*/
	public MapMetadata(MapPanel mp, SymbolPack sp, boolean supressPlacementErrors)
	throws MapException
	{
		this.mp = mp;
		this.sp = sp;
		this.supressPlacementErrors = supressPlacementErrors;
		infoMap = new HashMap(113);
		displayProps = new HashMap(47);
		
		Element root = mp.getSVGDocument().getRootElement();
		parseDisplayMetadata(root);
		parsePlacements(root);
		parseOrderDrawingData(root);
	}// MapMetadata()
	
	
	/** Clean up any resources used by this object */
	public void close()
	{
		mp.getClientFrame().fireMMDReady(null);	// VERY important
		infoMap.clear();
		displayProps.clear();
	}// close()
	
	
	/** Get an InfoEntry */
	public InfoEntry getInfoEntry(Province key)
	{
		return (InfoEntry) infoMap.get(key);
	}// getInfoEntry()
	
	/** 
	*	Set an InfoEntry
	*	<p>
	*	This generally should NOT be used. It is intended for map editors
	*	and what not. 
	*/
	public void setInfoEntry(Province key, InfoEntry value)
	{
		infoMap.put(key, value);
	}// setInfoEntry()
	
	
	/** Convenience method: get Unit placement point for this Province */
	public Point2D.Float getUnitPt(Province key, Coast coast)
	{ return getInfoEntry(key).getUnitPt(coast); }
	
	/** Convenience method: get Dislodged Unit placement point for this Province */
	public Point2D.Float getDislodgedUnitPt(Province key, Coast coast)	
	{ return getInfoEntry(key).getDislodgedUnitPt(coast); }
	
	/** Convenience method: get Supply Center placement point for this Province */
	public Point2D.Float getSCPt(Province key)
	{ return getInfoEntry(key).getSCPt(); }
	
	
	/**
	*	Stores coordinate information for Symbol placement within a province.
	*	<p>
	*	Rectangles are used; while the x,y position is most important, the width
	*	and height information can be used to scale.
	*/
	public static class InfoEntry
	{
		private final Point2D.Float unit;
		private final Point2D.Float dislodgedUnit;
		private final Point2D.Float sc;
		private Map unitCoasts;
		private Map dislodgedUnitCoasts;
		
		/** Create an InfoEntry object; if directional coasts, use setCoastMapings as well. */
		public InfoEntry(Point2D.Float unit, Point2D.Float dislodgedUnit, 
						 Point2D.Float sc)
		{
			// safety-check
			if(unit == null || dislodgedUnit == null || sc == null)
			{
				throw new IllegalArgumentException();
			}
			
			this.unit = makePt(unit);
			this.dislodgedUnit = makePt(dislodgedUnit);
			this.sc = makePt(sc);
		}// InfoEntry()
		
		
		/** Sets coast data maps for multi-coastal provinces; if not set, default placement data is used. */
		public void setCoastMappings(Map unitCoasts, Map dislodgedUnitCoasts)
		{
			this.unitCoasts = unitCoasts;
			this.dislodgedUnitCoasts = dislodgedUnitCoasts;	
		}// setCoastData()
		
		
		/** Adds data to coast mapping */
		public void addCoastMapping(Coast coast, Point2D.Float unitPt, Point2D.Float dislodgedPt)
		{
			if(unitPt == null || dislodgedPt == null)
			{
				throw new IllegalArgumentException();
			}
			
			if(unitCoasts == null)
			{
				unitCoasts = new HashMap(3);
			}
			
			if(dislodgedUnitCoasts == null)
			{
				dislodgedUnitCoasts = new HashMap(3);
			}
			
			unitCoasts.put(coast, unitPt);
			dislodgedUnitCoasts.put(coast, dislodgedPt);
		}// addCoastMapping()
		
		/** Location where units are placed */
		public Point2D.Float getUnitPt(Coast coast)
		{
			if(unitCoasts == null)
			{
				return makePt(unit);
			}
			
			Point2D.Float pt = (Point2D.Float) unitCoasts.get(coast);
			return (pt == null) ? makePt(unit) : makePt(pt);
		}// getUnitPt()
		
		/** Location where dislodged units are placed */
		public Point2D.Float getDislodgedUnitPt(Coast coast)
		{
			if(dislodgedUnitCoasts == null)
			{
				return makePt(dislodgedUnit);
			}
			
			Point2D.Float pt = (Point2D.Float) dislodgedUnitCoasts.get(coast);
			return (pt == null) ? makePt(dislodgedUnit) : makePt(pt);
		}// getDislodgedUnitPt()
		
		/** Location where supply centers are placed */
		public Point2D.Float getSCPt()							{ return makePt(sc); }
		
		/** Makes a new point from an existing point, since Point2D objects are mutable. */
		private final Point2D.Float makePt(Point2D.Float p)
		{
			return new Point2D.Float(p.x, p.y);
		}// makePt()
	}// nested class InfoEntry
	
	
	
	/** 
	*	Gets the SymbolSize for a symbol; null if symbol
	*	is not recognized. Case sensitive.
	*/
	public SymbolSize getSymbolSize(String symbolName)
	{
		StringBuffer sbKey = new StringBuffer(64);
		sbKey.append( EL_SYMBOLSIZE );
		sbKey.append( symbolName );
		return (SymbolSize) displayProps.get(sbKey.toString());
	}// getSymbolSize()
	
	
	
	/** Gets a float metadata value */
	public float getDisplayParamFloat(String key, float defaultValue)
	{
		String value = (String) displayProps.get(key);
		if(value != null)
		{
			try
			{
				return Float.parseFloat(value.trim());
			}
			catch(NumberFormatException e)
			{
			}
		}
		
		return defaultValue;
	}// getDisplayParamFloat()
	
	/** Gets an int metadata value */
	public int getDisplayParamInt(String key, int defaultValue)
	{
		String value = (String) displayProps.get(key);
		if(value != null)
		{
			try
			{
				return Integer.parseInt(value.trim());
			}
			catch(NumberFormatException e)
			{
			}
		}
		
		
		return defaultValue;
	}// getDisplayParamInt()
	
	/** Gets a boolean display metadata value */
	public boolean getDisplayParamBoolean(String key, boolean defaultValue)
	{
		String value = (String) displayProps.get(key);
		if(value != null)
		{
			value = value.trim();
			if("false".equalsIgnoreCase(value))
			{
				return false;
			}
			else if("true".equalsIgnoreCase(value))
			{
				return true;
			}
		}
		
		return defaultValue;
	}// getDisplayParamBoolean()
	
	
	/**
	*	Gets the String version of a parameter. 
	*	Throws an IllegalArgumentException if parameter is not found; 
	*	will never return null.
	*	<p>
	*	Example usage:
	*	String id = getOrderParamString(EL_BUILD, ATT_FILTERID);
	*/
	public String getOrderParamString(String orderElement, String attribute)
	{
		return (String) getOrderParam(orderElement, attribute);
	}// getOrderParamString()
	
	/**
	*	Gets the float version of a parameter. 
	*	Throws an IllegalArgumentException if parameter is not found.
	*/
	public float getOrderParamFloat(String orderElement, String attribute)
	{
		return ((Float) getOrderParam(orderElement, attribute)).floatValue();
	}// getOrderParamFloat()
	
	
	/**
	*	Gets the adjusted radius for an order. This is a convenience method.
	*	It is equivalent to calling <code>getOrderParamFloat(<order>, ATT_DELTA_RADIUS)</code> and
	*	passing that value into <code>SymbolSize.getRadius()</code>.
	*	<p>
	*	symbolName is typically a Unit symbol name (e.g., "Wing", "Army", "DislodgedFleet", etc.).<br>
	*	orderElement is a order element constant (e.g., EL_HOLD, EL_MOVE).<br>
	*/
	public float getOrderRadius(String orderElement, String symbolName)
	{
		final float deltaRadius = ((Float) getOrderParam(orderElement, ATT_DELTA_RADIUS)).floatValue();
		return getSymbolSize(symbolName).getRadius(deltaRadius);
	}// getOrderRadius()
	
	
	/**
	*	Gets the float array version of a parameter. 
	*	Throws an IllegalArgumentException if parameter is not found.
	*/
	public float[] getOrderParamFloatArray(String orderElement, String attribute)
	{
		return ((float[]) getOrderParam(orderElement, attribute));
	}// getOrderParamFloat()
	
	/** 
	*	Helper method for getting order drawing parameters. NEVER RETURNS NULL.
	*	<p>
	*	For filter parameter, if no filter is supplied, returns an empty string.
	*/
	private Object getOrderParam(String el, String att)
	{
		StringBuffer sb = new StringBuffer(64);
		sb.append(el);
		sb.append(att);
		Object value = displayProps.get(sb.toString());
		
		if(value == null)
		{
			throw new IllegalArgumentException("order parameters: key/attribute: "+el+","+att+" not found!");
		}
		
		return value;
	}// getOrderParam()
	
	
	
	
	/** Get power color (the color of orders associated w/a power */
	public String getPowerColor(Power power)
	{
		if(power == null)
		{
			throw new IllegalArgumentException("null power");
		}
		
		return (String) displayProps.get(power); 
	}// getPowerColor()
	
	
	/** 
	*	Gets a single element from a root-level element; returns null if no tag exists; returns last tag if
	*	multiple tags exist. This will go multiple levels deep!!!! Note that. 
	*	<p>
	*	the root element's namespace is used automatically.
	*/
	private Element getElement(Element root, String elementName)
	{
		NodeList nl = root.getElementsByTagNameNS(root.getNamespaceURI(), elementName);
		if(nl.getLength() == 0)
		{
			return null;
		}
		
		return (Element) nl.item(nl.getLength() - 1);
	}// getElement()
	
	
	/** 
	*	Parses an element that has 2 attributes, (x and y) and returns a Point2D.
	*	This only looks ONE level deep. If element is not found, (0,0) is returned.
	*	parent namespace is assumed. An Exception is thrown if bad coordinate values
	*	are passed.
	*/
	private Point2D.Float parseCoordElement(Element root, String elementName)
	throws MapException
	{
		Node child = root.getFirstChild();
		while(child != null)
		{
			if(elementName.equals(child.getLocalName()) && child.getNodeType() == Node.ELEMENT_NODE)
			{
				try
				{
					Element el = (Element) child;
					float x = Float.parseFloat(el.getAttribute(ATT_X).trim());
					float y = Float.parseFloat(el.getAttribute(ATT_Y).trim());
					return new Point2D.Float(x, y);
				}
				catch(NumberFormatException e)
				{
					throw new MapException("Bad coordinate value for element "+elementName+"; must be a decimal value. "+e.getMessage());
				}
			}
			
			child = child.getNextSibling();
		}
		
		return POINT_ZERO;
	}// parseCoordElement()
	
	/** Converts PROVINCE element & sub-element data to parsed placement information. */
	private void parsePlacements(Element root)
	throws MapException
	{
		// get the PROVINCE_DATA element, to see if a ATT_DISLODGED_OFFSET 
		// was specified. If so, we will use that offset.
		NodeList nl = root.getElementsByTagNameNS(JDIP_NAMESPACE, EL_PROVINCE_DATA);
		if(nl.getLength() != 1)
		{
			throw new MapException("Missing "+EL_PROVINCE_DATA+" element.");
		}
		
		Element elProvData = (Element) nl.item(0);
		if(!"".equals(elProvData.getAttribute(ATT_DISLODGED_OFFSET)))
		{
			dislodgedUnitOffset = parseCoord(
				EL_PROVINCE_DATA,
				ATT_DISLODGED_OFFSET,
				elProvData.getAttribute(ATT_DISLODGED_OFFSET)
			);
		}
		
		// now process province elements
		//
		nl = root.getElementsByTagNameNS(JDIP_NAMESPACE, EL_PROVINCE);
		for(int i=0; i<nl.getLength(); i++)
		{
			try
			{
				Element elProvince = (Element) nl.item(i);
				String provinceName = elProvince.getAttribute(ATT_NAME);
				
				// Strip of coast text, and lookup Province
				Province province = mp.getWorld().getMap().getProvince(Coast.getProvinceName(provinceName));
				if(province == null)
				{
					throw new MapException("SVG error in PROVINCE tag: Province name=\""+provinceName+"\" not recognized.");
				}
				
				// parse coast; if no directional coast is present, the coast will be 
				// UNDEFINED and isCoastSpecified == false
				Coast coast = Coast.parse(provinceName);
				boolean isCoastSpecified = coast.isDirectional();
				
				// parse coordinate data elements
				Point2D.Float unit = parseCoordElement(elProvince, EL_UNIT);
				Point2D.Float dislodged = parseCoordElement(elProvince, EL_DISLODGED_UNIT);
				Point2D.Float sc = parseCoordElement(elProvince, EL_SC);
				
				// fix unset dislodged units to use dislodgedUnitOffset, if present.
				if(dislodgedUnitOffset != null && POINT_ZERO.equals(dislodged))
				{
					dislodged.x = unit.x + dislodgedUnitOffset.x;
					dislodged.y = unit.y + dislodgedUnitOffset.y;
				}
				
				// create InfoMap
				if(!isCoastSpecified)
				{
					InfoEntry ie = new InfoEntry(unit, dislodged, sc);
					infoMap.put(province, ie);
				}
				else
				{
					InfoEntry ie = (InfoEntry) infoMap.get(province);
					if(ie == null)
					{
						throw new MapException("Error in PROVINCE: "+provinceName+"; province metadata with coast must succeed those without; e.g., stp-sc must come AFTER stp");
					}
					
					ie.addCoastMapping(coast, unit, dislodged);
				}
			}
			catch(MapException me)
			{
				// do not throw an exception if we are suppressing errors.
				if(!supressPlacementErrors)
				{
					throw me;
				}
			}
		}
		
		// verify: make sure each province has at least one InfoEntry.
		// if we are supressing errors, fill in with empty data.
		Province[] provinces = mp.getWorld().getMap().getProvinces();
		for(int i=0; i<provinces.length; i++)
		{
			if(infoMap.get(provinces[i]) == null)
			{
				if(supressPlacementErrors)
				{
					InfoEntry ie = new InfoEntry( new Point2D.Float(0,0),
										new Point2D.Float(0,0),
										new Point2D.Float(0,0) );
					infoMap.put(provinces[i], ie);
					Log.println("MMD: added empty entry for province ", provinces[i]);
				}
				else
				{
					throw new MapException("Missing PROVINCE placement information for province: "+provinces[i]);
				}
			}
		}
	}// parsePlacements()
	
	
	/**
	*	Parses the Display metadata XML elements.
	*
	*	root: the top-level tag from which to start parsing. We put these in a 
	*	HashMap. the HashMap is indexed by ATTRIBUTE (ATT_*). 
	*/
	private void parseDisplayMetadata(Element root)
	throws MapException
	{
		NodeList nl = root.getElementsByTagNameNS(JDIP_NAMESPACE, EL_DISPLAY);
		if(nl.getLength() != 1)
		{
			throw new MapException("There are "+nl.getLength()+" DISPLAY elements in the SVG file; a single DISPLAY group is required.");
		}
		
		Element displayRoot = (Element) nl.item(0);
		
		Element el = getElement(displayRoot, EL_ZOOM);
		if(el != null)
		{
			displayProps.put(ATT_ZOOM_MIN, el.getAttribute(ATT_ZOOM_MIN).trim());
			displayProps.put(ATT_ZOOM_MAX, el.getAttribute(ATT_ZOOM_MAX).trim());
			displayProps.put(ATT_ZOOM_FACTOR, el.getAttribute(ATT_ZOOM_FACTOR).trim());
		}
		
		el = getElement(displayRoot, EL_LABELS);
		if(el != null)
		{
			displayProps.put(ATT_LABELS_BRIEF, el.getAttribute(ATT_LABELS_BRIEF).trim());
			displayProps.put(ATT_LABELS_FULL, el.getAttribute(ATT_LABELS_FULL).trim());
		}
	}// parseDisplayMetadata()
	
	/**
	*	Parses the ORDERDRAWING metadata XML subelements.
	*
	*	root: the top-level tag from which to start parsing. We put these in a 
	*	HashMap. the HashMap is indexed by ELEMENT+ATTRIBUTE 
	*/
	private void parseOrderDrawingData(Element root)
	throws MapException
	{
		// get ORDERDRAWING element
		NodeList nl = root.getElementsByTagNameNS(JDIP_NAMESPACE, EL_ORDERDRAWING);
		if(nl.getLength() != 1)
		{
			throw new MapException("There are "+nl.getLength()+" "+EL_ORDERDRAWING+" elements in the SVG file; a single element is required.");
		}
		
		Element orderRoot = (Element) nl.item(0);
		
		// parse SYMBOLSIZE info
		NodeList ssnl = orderRoot.getElementsByTagNameNS(JDIP_NAMESPACE, EL_SYMBOLSIZE);
		for(int i=0; i<ssnl.getLength(); i++)
		{
			parseAndAddSymbolSize( (Element) ssnl.item(i) );
		}
		
		// HOLD
		Element el = getElement(orderRoot, EL_HOLD);
		checkElement(EL_HOLD, el);
		putOrderParam(EL_HOLD, ATT_DELTA_RADIUS, parseFloat(EL_HOLD, ATT_DELTA_RADIUS, el.getAttribute(ATT_DELTA_RADIUS)));
		putOrderParam(EL_HOLD, ATT_STROKESTYLE, el.getAttribute(ATT_STROKESTYLE).trim());
		putOrderParam(EL_HOLD, ATT_HILIGHT_OFFSET, parseFloat(EL_HOLD, ATT_HILIGHT_OFFSET, el.getAttribute(ATT_HILIGHT_OFFSET)));
		putOrderParam(EL_HOLD, ATT_HILIGHT_CLASS, el.getAttribute(ATT_HILIGHT_CLASS).trim());
		putOrderParam(EL_HOLD, ATT_WIDTHS, parseFloatArray(EL_HOLD, ATT_WIDTHS, el.getAttribute(ATT_WIDTHS)));
		putOrderParam(EL_HOLD, ATT_SHADOW_WIDTHS, parseFloatArray(EL_HOLD, ATT_SHADOW_WIDTHS, el.getAttribute(ATT_SHADOW_WIDTHS)));
		putOptionalOrderParam(EL_HOLD, ATT_FILTERID, el.getAttribute(ATT_FILTERID).trim());
		
		// DISBAND
		el = getElement(orderRoot, EL_DISBAND);
		checkElement(EL_DISBAND, el);
		putOrderParam(EL_DISBAND, ATT_DELTA_RADIUS, parseFloat(EL_DISBAND, ATT_DELTA_RADIUS, el.getAttribute(ATT_DELTA_RADIUS)));
		
		// REMOVE
		el = getElement(orderRoot, EL_REMOVE);
		checkElement(EL_REMOVE, el);
		putOrderParam(EL_REMOVE, ATT_DELTA_RADIUS, parseFloat(EL_REMOVE, ATT_DELTA_RADIUS, el.getAttribute(ATT_DELTA_RADIUS)));
		
		// BUILD
		el = getElement(orderRoot, EL_BUILD);
		checkElement(EL_BUILD, el);
		putOrderParam(EL_BUILD, ATT_DELTA_RADIUS, parseFloat(EL_BUILD, ATT_DELTA_RADIUS, el.getAttribute(ATT_DELTA_RADIUS)));
		
		// WAIVE
		el = getElement(orderRoot, EL_WAIVE);
		checkElement(EL_WAIVE, el);
		putOrderParam(EL_WAIVE, ATT_DELTA_RADIUS, parseFloat(EL_WAIVE, ATT_DELTA_RADIUS, el.getAttribute(ATT_DELTA_RADIUS)));
		
		// MOVE
		el = getElement(orderRoot, EL_MOVE);
		checkElement(EL_MOVE, el);
		putOrderParam(EL_MOVE, ATT_DELTA_RADIUS, parseFloat(EL_MOVE, ATT_DELTA_RADIUS, el.getAttribute(ATT_DELTA_RADIUS)));
		putOrderParam(EL_MOVE, ATT_STROKESTYLE, el.getAttribute(ATT_STROKESTYLE).trim());
		putOrderParam(EL_MOVE, ATT_MARKERID, el.getAttribute(ATT_MARKERID).trim());
		putOrderParam(EL_MOVE, ATT_HILIGHT_OFFSET, parseFloat(EL_MOVE, ATT_HILIGHT_OFFSET, el.getAttribute(ATT_HILIGHT_OFFSET)));
		putOrderParam(EL_MOVE, ATT_HILIGHT_CLASS, el.getAttribute(ATT_HILIGHT_CLASS).trim());
		putOrderParam(EL_MOVE, ATT_WIDTHS, parseFloatArray(EL_MOVE, ATT_WIDTHS, el.getAttribute(ATT_WIDTHS)));
		putOrderParam(EL_MOVE, ATT_SHADOW_WIDTHS, parseFloatArray(EL_MOVE, ATT_SHADOW_WIDTHS, el.getAttribute(ATT_SHADOW_WIDTHS)));
		putOptionalOrderParam(EL_MOVE, ATT_FILTERID, el.getAttribute(ATT_FILTERID).trim());
		
		// RETREAT
		el = getElement(orderRoot, EL_RETREAT);
		checkElement(EL_RETREAT, el);
		putOrderParam(EL_RETREAT, ATT_DELTA_RADIUS, parseFloat(EL_RETREAT, ATT_DELTA_RADIUS, el.getAttribute(ATT_DELTA_RADIUS)));
		putOrderParam(EL_RETREAT, ATT_STROKESTYLE, el.getAttribute(ATT_STROKESTYLE).trim());
		putOrderParam(EL_RETREAT, ATT_MARKERID, el.getAttribute(ATT_MARKERID).trim());
		putOrderParam(EL_RETREAT, ATT_HILIGHT_OFFSET, parseFloat(EL_RETREAT, ATT_HILIGHT_OFFSET, el.getAttribute(ATT_HILIGHT_OFFSET)));
		putOrderParam(EL_RETREAT, ATT_HILIGHT_CLASS, el.getAttribute(ATT_HILIGHT_CLASS).trim());
		putOptionalOrderParam(EL_RETREAT, ATT_FILTERID, el.getAttribute(ATT_FILTERID).trim());
		
		// SUPPORT
		el = getElement(orderRoot, EL_SUPPORT);
		checkElement(EL_SUPPORT, el);
		putOrderParam(EL_SUPPORT, ATT_DELTA_RADIUS, parseFloat(EL_SUPPORT, ATT_DELTA_RADIUS, el.getAttribute(ATT_DELTA_RADIUS)));
		putOrderParam(EL_SUPPORT, ATT_STROKESTYLE, el.getAttribute(ATT_STROKESTYLE).trim());
		putOrderParam(EL_SUPPORT, ATT_MARKERID, el.getAttribute(ATT_MARKERID).trim());
		putOrderParam(EL_SUPPORT, ATT_HILIGHT_OFFSET, parseFloat(EL_SUPPORT, ATT_HILIGHT_OFFSET, el.getAttribute(ATT_HILIGHT_OFFSET)));
		putOrderParam(EL_SUPPORT, ATT_HILIGHT_CLASS, el.getAttribute(ATT_HILIGHT_CLASS).trim());
		putOptionalOrderParam(EL_SUPPORT, ATT_FILTERID, el.getAttribute(ATT_FILTERID).trim());
		
		// CONVOY
		el = getElement(orderRoot, EL_CONVOY);
		checkElement(EL_CONVOY, el);
		putOrderParam(EL_CONVOY, ATT_DELTA_RADIUS, parseFloat(EL_CONVOY, ATT_DELTA_RADIUS, el.getAttribute(ATT_DELTA_RADIUS)));
		putOrderParam(EL_CONVOY, ATT_STROKESTYLE, el.getAttribute(ATT_STROKESTYLE).trim());
		putOrderParam(EL_CONVOY, ATT_MARKERID, el.getAttribute(ATT_MARKERID).trim());
		putOrderParam(EL_CONVOY, ATT_HILIGHT_OFFSET, parseFloat(EL_CONVOY, ATT_HILIGHT_OFFSET, el.getAttribute(ATT_HILIGHT_OFFSET)));
		putOrderParam(EL_CONVOY, ATT_HILIGHT_CLASS, el.getAttribute(ATT_HILIGHT_CLASS).trim());
		putOptionalOrderParam(EL_CONVOY, ATT_FILTERID, el.getAttribute(ATT_FILTERID).trim());
		
		// POWERCOLOR(S)
		el = getElement(orderRoot, EL_POWERCOLORS);
		checkElement(EL_POWERCOLORS, el);
		dip.world.Map map = mp.getWorld().getMap();
		nl = el.getChildNodes();
		for(int i=0; i<nl.getLength(); i++)
		{
			Node node = nl.item(i);
			if(node.getNodeType() == Node.ELEMENT_NODE)
			{
				el = (Element) nl.item(i);
				
				Power power = map.getPower( el.getAttribute(ATT_POWER).trim() );
				if(power != null)
				{
					String color = el.getAttribute(ATT_COLOR).trim();
					if(color == null)
					{
						throw new MapException(EL_POWERCOLOR+" color \""+el.getAttribute(ATT_COLOR)+"\" not specified.");
					}
				
					displayProps.put(power, color);
				}
			}
		}
		
		// verify all powers have a color
		Power[] powers = map.getPowers();
		for(int i=0; i<powers.length; i++)
		{
			if(displayProps.get(powers[i]) == null)
			{
				throw new MapException(EL_POWERCOLORS+": no color defined for power "+powers[i]);
			}
		}
		
	}// parseOrderDrawingData()	
	
	
	/** Check element method; checks if element is null. */
	private void checkElement(String name, Element el)
	throws MapException
	{
		if(el == null)
		{
			throw new MapException("Missing required element: "+name);
		}
	}// checkElement()
	
	/** Helper method: set an order parameter */
	private void putOrderParam(String el, String att, Object value)
	throws MapException
	{
		if(el == null || att == null)
		{
			throw new IllegalArgumentException();
		}
		
		if(value == null || "".equals(value))
		{
			throw new MapException(el+" attribute "+att+" is missing!");
		}
		
		StringBuffer sb = new StringBuffer(64);
		sb.append(el);
		sb.append(att);
		displayProps.put(sb.toString(), value);
	}// putOrderParam()
	
	
	/** Helper method: set an order parameter, but if it doesn't exist, don't complain. */
	private void putOptionalOrderParam(String el, String att, Object value)
	throws MapException
	{
		if(el == null || att == null)
		{
			throw new IllegalArgumentException();
		}
		
		StringBuffer sb = new StringBuffer(64);
		sb.append(el);
		sb.append(att);
		displayProps.put(sb.toString(), value);
	}// putOptionalOrderParam()
	
	
	/** 
	*	Parses a SYMBOLSIZE element and adds it to the display properties
	*	HashMap, with a prefix of SYMBOLSIZE. Symbolsizes are not allowed
	*	to contain "%" as a unit type. The Value inserted is a SymbolSize
	*	object (contains two strings: width, height).
	*	<p>
	*	Furthermore, this method also ensures that the given symbol is 
	*	in fact defined in the SymbolPack, and scales it appropriately.
	*/
	private void parseAndAddSymbolSize(Element el)
	throws MapException
	{
		String name = el.getAttribute(ATT_NAME).trim();
		String w = el.getAttribute(ATT_WIDTH).trim();
		String h = el.getAttribute(ATT_HEIGHT).trim();
		
		Symbol symbol = sp.getSymbol(name);
		if(symbol == null)
		{
			throw new MapException("Element "+el.getTagName()+" symbol named \""+name+"\" not found in symbol pack! Case sensitive.");
		}
		
		StringBuffer sbKey = new StringBuffer(64);
		sbKey.append( EL_SYMBOLSIZE );
		sbKey.append( name );
		
		displayProps.put(sbKey.toString(), 
			new SymbolSize(w,h, symbol.getScale(), el));
	}// parseAndAddSymbolSize()
	
	/** 
	*	Textual width/height of a symbol, as valid SVG dimensions. 
	*/
	public static class SymbolSize
	{
		private final String w;
		private final String h;
		private final String r;
		private final float rFloat;
		private final String units;
		
		/** 
		*	Create a SymbolSize element 
		*	<p>
		*	Throws an exception if the value cannot be parsed, contains
		*	a "%" sign (cannot be relative), or, the units (if present)
		*	are different between the width and height attributes (e.g.,
		*	"px" and "cm"). And numbers cannot be negative, either.
		*/
		public SymbolSize(String w, String h, float scale, Element element)
		throws MapException
		{
			Object[] tmp = makeValues(w, h, scale, element);
			
			this.w = (String) tmp[0];
			this.h = (String) tmp[1];
			this.r = (String) tmp[2];
			this.rFloat = ((Float) tmp[3]).floatValue();
			this.units = (String) tmp[4];
		}// SymbolSize()
		
		/** Get the Width */
		public String getWidth() { return w; }
		
		/** Get the Height */
		public String getHeight() { return h; }
		
		/** 
		*	Get the Radius of a circle centered over the width/height that
		*	will circumscribe width/height.
		*/
		public String getRadius() { return r; }
		
		/** Gets the Units (never null, but may be empty). */
		public String getUnits() { return units; }
		
		/**
		*	Computes a Radius given the known radius and a 
		*	delta (smaller/larger). Delta and Radius are 
		*	assumed to have the	same units.
		*/
		public float getRadius(float delta)
		{
			return (rFloat + delta);
		}// getRadius()
		
		/**
		*	Returns the Radius as a String (formatted float + units)
		*/
		public String getRadiusString(float delta)
		{
			return (SVGUtils.floatToString(rFloat + delta) + getUnits());
		}// getRadiusString()
		
		
		/**
		*	Makes all values. Returns an array of length 3;
		*	0:width, 1:height, 2:radius, 3:radius (as a float), 
		*	4: units ("" if none)
		*
		*/
		private Object[] makeValues(String w, String h, float scale, Element el)
		throws MapException
		{
			Object[] obj = parseDim(w, el, ATT_WIDTH);
			float width = ((Float) obj[0]).floatValue();
			String widthUnits = (String) obj[1];
			
			obj = parseDim(h, el, ATT_HEIGHT);
			float height = ((Float) obj[0]).floatValue();
			String heightUnits = (String) obj[1];
			
			// check that units are identical
			if(!widthUnits.equals(heightUnits))
			{
				throw new MapException("Element "+el.getTagName()+
					" width and height attributes must both have the same (or no) unit specifiers.");
			}
			
			// scale
			width *= scale;
			height *= scale;
			
			// get radius (1/2 of diagonal)
			float radius = (float) (Math.sqrt((width*width) + (height*height)) / 2.0);
			
			// return strings
			Object[] values = new Object[5];
			values[0] = (SVGUtils.floatToString(width) + widthUnits);
			values[1] = (SVGUtils.floatToString(height) + widthUnits);
			values[2] = (SVGUtils.floatToString(radius) + widthUnits);
			values[3] = new Float(radius);
			values[4] = widthUnits;
			return values;
		}// makeValues()
		
		
		/** 
		*	Parse a Dimension into a Float and Unit specifier (empty if not present);
		*	Object[0] : Float, Object[1] : String. Neither value will be null.
		*/
		private Object[] parseDim(String in, Element el, String attributeName)
		throws MapException
		{
			if(in.length() == 0 || in.indexOf('%') >= 0 || in.indexOf('-') >= 0)
			{
				throw new MapException("Element "+el.getTagName()+" attribute "+attributeName+" cannot have a % (relative size) in width or height attributes, or be zero or negative.");
			}
			
			// otherwise, first extract the numeric part (digits, decimal); 
			// then extract (if any) the unit part (save for later)
			//
			int idx = 0;
			while(idx < in.length())
			{
				char c = in.charAt(idx);
				if(!Character.isDigit(c) && c != '.')
				{
					break;
				}
				idx++;
			}
			
			String num = in.substring(0, idx);
			String units = in.substring(idx);
			
			Object[] obj = new Object[2];
			obj[0] = parseFloat(el.getTagName(), attributeName, num);
			obj[1] = units;
			return obj;
		}// parseDim()
	}// nested class SymbolSize
	
	
	/** Parse a float; if fails, returns a MapException */
	private static Float parseFloat(String el, String att, String value)
	throws MapException
	{
		try
		{
			return new Float(value.trim());
		}
		catch(NumberFormatException e)
		{
			throw new MapException(el+" attribute "+att+" value \""+value+"\" is not specified or not a valid floating point value.");
		}
	}// parseFloat()
	
	/** Parse a coordinate: e.g. n n or n,n format. */
	private Point2D.Float parseCoord(String el, String att, String in)
	throws MapException
	{
		try
		{
			Point2D.Float val = new Point2D.Float();
			StringTokenizer st = new StringTokenizer(in, ", )(;");
			if(st.hasMoreTokens())
			{
				val.x = ( Float.parseFloat(st.nextToken().trim()) );
			}
			else
			{
				throw new NumberFormatException();
			}
			
			if(st.hasMoreTokens())
			{
				val.y = ( Float.parseFloat(st.nextToken().trim()) );
			}
			else
			{
				throw new NumberFormatException();
			}
			
			return val;
		}
		catch(NumberFormatException e)
		{
		}
		
		throw new MapException(el+" attribute "+att+" value \""+in+"\" is not specified or not a valid floating point value pair. (e.g., \"3.0, 2.01\")");
	}// parseCoord()
	
	
	/** Parse a float array; if fails, returns a MapException */
	private float[] parseFloatArray(String el, String att, String value)
	throws MapException
	{
		StringTokenizer st = new StringTokenizer(value,",; \n\r\t");
		final float[] arr = new float[st.countTokens()];
		
		if(arr.length == 0)
		{
			throw new MapException(el+" attribute "+att+" value \""+value+"\" must contain at least one positive floating point value.");
		}
		
		int count = 0;
		while(st.hasMoreTokens())
		{
			try
			{
				float fVal = Float.parseFloat(st.nextToken().trim());
				if(fVal <= 0.0f)
				{
					throw new MapException(el+" attribute "+att+" value \""+value+"\" only values > 0 are valid.");
				}
				
				arr[count] = fVal;
				count++;
			}
			catch(NumberFormatException e)
			{
				throw new MapException(el+" attribute "+att+" value \""+value+"\" is not a valid postive floating point value.");
			}
		}
		
		return arr;
	}// parseFloatArray()
	
}// class MapMetadata

	

