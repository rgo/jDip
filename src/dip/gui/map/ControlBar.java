//
//  @(#)ControlBar.java		4/2002
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
package dip.gui.map;

import dip.world.Location;

import java.awt.Insets;

import javax.swing.JToolBar;
import javax.swing.border.EtchedBorder;

import org.w3c.dom.events.MouseEvent;
import org.apache.batik.dom.events.DOMKeyEvent;


/**
*	All ControlBars must extend this Control bar.
*	Implements all DOMUIEventHandler methods with empty methods.
*
*/
public abstract class ControlBar extends JToolBar implements DOMUIEventHandler
{
	protected final MapPanel mapPanel;
	
	
	/** Create a ControlBar */
	public ControlBar(MapPanel mp)
	{
		super();
		setMargin(new Insets(5,5,5,5));
		setFloatable(false);
		setRollover(true);
		setBorder(new EtchedBorder(EtchedBorder.LOWERED)); 
		this.mapPanel = mp;
	}// ControlBar()
	
	
	/** Key Pressed event. Does Nothing by default. */
	public void keyPressed(DOMKeyEvent ke, Location loc)	{}
	/** Mouse Over event: Mouse over a province. Does Nothing by default. */
	public void mouseOver(MouseEvent me, Location loc)	{}
	/** Mouse Out event: Mouse out of a province. Does Nothing by default. */
	public void mouseOut(MouseEvent me, Location loc)	{}
	/** Mouse clicked. Does Nothing by default. */
	public void mouseClicked(MouseEvent me, Location loc)	{}
	/** Mouse button pressed. Does Nothing by default. */
	public void mouseDown(MouseEvent me, Location loc)	{}
	/** Mouse button released. Does Nothing by default. */
	public void mouseUp(MouseEvent me, Location loc)	{}
	
}// class ControlBar	

