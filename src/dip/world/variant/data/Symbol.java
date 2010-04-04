//
//  @(#)Symbol.java		11/2003
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
package dip.world.variant.data;

import org.w3c.dom.Element;


/**
*	A Symbol
*
*
*/
public class Symbol
{
	/** Default (identity) scale factor */
	public final static float IDENTITY_SCALE = 1.0f;
	
	private final String name;
	private final Element svgData;
	private final float scale; 
	
	/** 
	*	Create a new Symbol.
	*	<p>
	*	Scale value must be a positive non-zero floating-point value. 
	*	A value of 1.0f (IDENTITY_SCALE) should be the default.
	*/
	public Symbol(String name, float scale, Element svgData)
	{
		if(name == null || scale <= 0.0f)
		{
			throw new IllegalArgumentException();
		}
		
		this.name = name;
		this.scale = scale;
		this.svgData = svgData;
	}// Symbol()
	
	/** Gets the Symbol name. */
	public String getName()			{ return name; }
	
	/** Returns the scaling factor (IDENTITY_SCALE is the default) */
	public float getScale()			{ return scale; }
	
	/** 
	*	Returns the SVG group of symbol definition
	*	with an identical id attribute as the Symbol 
	*	name. This will return a (deep) cloned Element, 
	*	and thus has no Parent set.
	*/
	public Element getSVGData()
	{ 
		return (Element) svgData.cloneNode(true); 
	}// getSVGData()
	
	
}// class Symbol
