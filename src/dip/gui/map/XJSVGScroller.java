//
//  @(#)XJSVGScroller.java		2/2004
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
package dip.gui.map;

import dip.misc.Log;

import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import javax.swing.JScrollBar;

import org.apache.batik.swing.JSVGCanvas;
import org.apache.batik.swing.JSVGScrollPane;




/**
*	Modified Batik JSVGScrollPane class
*
*/
public class XJSVGScroller extends JSVGScrollPane
{
	
	/**
	*	Creates a XJSVGScroller. This adds support for Wheel mice, too.
	*/
	public XJSVGScroller(JSVGCanvas canvas)
	{
		super(canvas);
		addMouseWheelListener(new WheelListener());
	}// XJSVGScroller()
	
	/** Inner class to catch mouse wheel events */
	private class WheelListener implements MouseWheelListener
	{
		public void mouseWheelMoved(MouseWheelEvent e)
		{
			final JScrollBar sb = (vertical.isVisible()) ? 
				vertical : horizontal;	// vertical is preferred
			
			if(sb.isVisible())
			{
				if(e.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL) {
					final int amt = e.getUnitsToScroll() * sb.getUnitIncrement();
					sb.setValue(sb.getValue() + amt);
				} else if(e.getScrollType() == MouseWheelEvent.WHEEL_BLOCK_SCROLL){
					final int amt = e.getWheelRotation() * sb.getBlockIncrement();
					sb.setValue(sb.getValue() + amt);
				}
			}
		}// mouseWheelMoved()
	}// inner class WheelListener
	
}// class XJSVGScroller
