//
//  @(#)SVGColorParser.java		11/2003
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
package dip.gui.map;


import dip.misc.Utils;

import java.awt.Color;

import org.apache.batik.css.engine.value.svg.ColorManager;
import org.apache.batik.css.engine.value.StringMap;
import org.apache.batik.css.engine.value.RGBColorValue; 
import org.apache.batik.css.engine.value.FloatValue; 

/**
*	Parses SVG colors, and returns a java.awt.Color color.
*	<p>
*	This uses some Batik trickery to allow the use of SVG 
*	and CSS color keywords. If a color keyword is not found,
*	SVG color definitions (e.g., #aabbcc or rgb(3,10,200)) 
*	are parsed. If all parsing fails, the Color.BLACK is
*	returned.
*
*/
public class SVGColorParser
{
	private static XColorStringMap cm = null;
	
	
	/** Get the color; returns Color.BLACK if all parsing fails. */
	public static Color parseColor(String cssColor)
	{
		init();
		Color color = cm.getColor(cssColor);
		if(color != null)
		{
			return color;
		}
		
		return Utils.parseColor(cssColor, Color.BLACK);
	}// parseColor()
	
	
	/** Singleton pattern */
	private SVGColorParser()	{}
	
	
	/** Initialize the SVGColorParser */
	private static synchronized void init()
	{
		if(cm == null)
		{
			XColorManager xcm = new XColorManager();
			cm = xcm.getXColorStringMap();
			xcm = null;
		}
	}// init()
	
	
	/** ColorManager internal abstraction */
	private static class XColorManager extends ColorManager
	{
		/** Create an XColorManager */
		public XColorManager()
		{ 
			super();
		}// XColorManager()
		
		/** Get the XColorStringMap */
		public XColorStringMap getXColorStringMap()
		{
			return new XColorStringMap(computedValues);
		}
		
	}// inner class XColorManager
	
	
	/** StringMap that does NOT use referential equality for get() */
	private static class XColorStringMap extends StringMap
	{
		
		/** Create an XStringMap */
		public XColorStringMap(StringMap sm)
		{
			super(sm);
		}// XColorStringMap()
		
		/** Uses String.equals() instead of referential equality */
		public Object get(String key)
		{
			int hash  = key.hashCode() & 0x7FFFFFFF;
			int index = hash % table.length;
			
			for (Entry e = table[index]; e != null; e = e.next)
			{
				if ((e.hash == hash) && e.key.equals(key)) 
				{
					return e.value;
				}
			}
			
			return null;
		}// get()
		
		/** Uses get() and returns a Color, or null */
		public Color getColor(String key)
		{
			RGBColorValue value = (RGBColorValue) get(key);
			if(value != null)
			{
				float r = ((FloatValue) value.getRed()).getFloatValue();
				float g = ((FloatValue) value.getGreen()).getFloatValue();
				float b = ((FloatValue) value.getBlue()).getFloatValue();
				return new Color((int) r, (int) g, (int) b);
			}
			
			return null;
		}// getColor()
	}// inner class XStringMap
	
	
}// class SVGColorParser
