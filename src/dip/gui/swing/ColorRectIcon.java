//
//  @(#)ColorRectIcon.java		3/2004
//
//  Copyright 2004 Zachary DelProposto. All rights reserved.
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
package dip.gui.swing;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;

import javax.swing.Icon;

/**
*	Creates an Icon that is a solid color within
*	a 1-pixel black rectangular border.
*	<p>
*	NOTE: if this is going to be used by ANY other class,
*	it should be taken out instead of be a nested class.
*/
public class ColorRectIcon implements Icon
{
	private final int h;
	private final int w;
	private final Color color;
	
	/** Create a ColorRect */
	public ColorRectIcon(int height, int width, Color color)
	{
		this.h = height;
		this.w = width;
		this.color = color;
	}// ColorRectIcon()
	
	/** Icon height */
	public int getIconHeight()	{ return h; }
	
	/** Icon width */
	public int getIconWidth()	{ return w; }
	
	/** Draw the Icon */
	public void paintIcon(Component c, Graphics g, int x, int y)
	{
		g.setColor(color);
		g.fillRect(x, y, w, h);
		
		g.setColor( c.getBackground() );
		g.draw3DRect(x, y, w, h, false);
		//g.setColor(Color.black);
		//g.drawRect(x, y, w, h);
	}// paintIcon()
	
}// class ColorRectIcon
