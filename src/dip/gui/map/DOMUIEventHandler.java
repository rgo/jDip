//
//  @(#)DOMUIEventHandler.java		6/2003
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

import dip.world.Location;
import org.w3c.dom.events.MouseEvent;
import org.apache.batik.dom.events.DOMKeyEvent;
/**
*
*	Handles MapPanel DOM UI Events, such as 
*	<code>KeyEvent</code>s and <code>MouseEvent</code>s.
*	<p>
*	NOTE: <code>Location</code> objects may be null
*
*/
public interface DOMUIEventHandler
{
	
	/** Key Pressed event */
	public void keyPressed(DOMKeyEvent ke, Location loc);
	
	
	/** Mouse Over event: Mouse over a province */
	public void mouseOver(MouseEvent me, Location loc);
	/** Mouse Out event: Mouse out of a province */
	public void mouseOut(MouseEvent me, Location loc);
	/** Mouse clicked */
	public void mouseClicked(MouseEvent me, Location loc);
	/** Mouse button pressed */
	public void mouseDown(MouseEvent me, Location loc);
	/** Mouse button released */
	public void mouseUp(MouseEvent me, Location loc);
	
	/* Mouse moved 
	public void mouseMoved(MouseEvent me, Location loc)
	*/
	
	
}// interface DOMUIEventHandler
