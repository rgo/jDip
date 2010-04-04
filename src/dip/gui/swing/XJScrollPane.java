//
//  @(#)XJScrollPane.java	1/2004
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

import dip.misc.Utils;

import java.awt.Component;
import javax.swing.JScrollPane;

/**
*	eXtended JScrollPane.
*	<p>
*	Modified to always show scrollbars (both) on Mac OS X. Other platforms
*	default to AS_NEEDED.
*/
public class XJScrollPane extends JScrollPane
{
	private final static int defaultHsbPolicy = ((Utils.isOSX()) ? 
		JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS : JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
	private final static int defaultVsbPolicy = ((Utils.isOSX()) ? 
		JScrollPane.VERTICAL_SCROLLBAR_ALWAYS : JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
	
	/** Create an XJScrollPane */
	public XJScrollPane()
	{
		super(defaultVsbPolicy, defaultHsbPolicy);
	}// XJScrollPane()
	
	/** Create an XJScrollPane */
	public XJScrollPane(Component view)
	{
		super(view, defaultVsbPolicy, defaultHsbPolicy);
	}// XJScrollPane()
	
}// class XJScrollPane
