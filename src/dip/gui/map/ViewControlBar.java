//
//  @(#)ViewControlBar.java	1.00	4/1/2002
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

import dip.misc.Utils;
import dip.gui.order.GUIOrder;

import java.awt.geom.*;

import javax.swing.JButton;
import javax.swing.ActionMap;

import javax.swing.*;
import org.apache.batik.swing.*;
import java.awt.geom.AffineTransform;
import java.awt.event.ActionEvent;
import java.awt.Dimension;

import org.apache.batik.swing.JSVGCanvas; 
import org.apache.batik.dom.events.DOMKeyEvent;
import org.w3c.dom.events.MouseEvent;

import org.apache.batik.*;
import org.apache.batik.dom.*;
import org.apache.batik.util.*;
import org.w3c.dom.svg.*;
import org.w3c.dom.svg.SVGSVGElement;

import org.apache.batik.bridge.ViewBox;
import org.apache.batik.gvt.CanvasGraphicsNode;


/**
*	View Control Bar
*	<p>
*	This is the control bar that implements basic zooming and revert capability.
*	<p>
*	Typically, it is used when not in edit mode, and when no orders can be entered.
*
*/
public class ViewControlBar extends ControlBar
{
	// i18n resources
	private static final String I18N_ZOOMIN_TIP		= "ViewConBar.button.zoomin.tooltip";
	private static final String I18N_ZOOMOUT_TIP	= "ViewConBar.button.zoomout.tooltip";
	private static final String I18N_FIT_TIP		= "ViewConBar.button.fit.tooltip";
	
	// key descriptors
	private static final char KEY_ZOOM_IN		= '+';
	private static final char KEY_ZOOM_OUT		= '-';
	private static final char KEY_FIT			= '=';
	
	// icons
	private static final String ICON_ZOOM_IN 	= "resource/common/icons/24x24/stock_zoom_in_24.png";
	private static final String ICON_ZOOM_OUT 	= "resource/common/icons/24x24/stock_zoom_out_24.png";
	private static final String ICON_ZOOM_FIT	= "resource/common/icons/24x24/stock_zoom_fit_24.png";
	
	
	
	// instance variables
	private JButton fit;
	private JButton zoomIn;
	private JButton zoomOut;
	private final int keyFit;
	private final int keyZoomIn;
	private final int keyZoomOut;
	
	/** Creates the View control bar. */
	public ViewControlBar(MapPanel mp)
	{
		super(mp);
		makeLayout();
		
		// NULL test when we actually do IL8N
		keyFit = KEY_FIT;
		keyZoomIn = KEY_ZOOM_IN;
		keyZoomOut = KEY_ZOOM_OUT;
	}// ViewControlBar()
	
	/** Called when the mouse pointer enters a province */
	public void mouseOver(MouseEvent me, Location loc)
	{
		if(loc == null)
		{
			mapPanel.statusBarUtils.setText( Utils.getLocalString(GUIOrder.NOT_IN_PROVINCE) );
		}
		else
		{
			mapPanel.statusBarUtils.displayProvinceInfo(loc);
		}
	}// mouseOver()
	
	/** Called when the mouse pointer leaves a province */
	public void mouseOut(MouseEvent me, Location loc)
	{
		mapPanel.statusBarUtils.clearText();
	}// mouseOut()
	
	
	/** Handles ZoomIn / ZoomOut / Revert key functionality. */
	public void keyPressed(DOMKeyEvent ke, Location loc)
	{
		int charCode = ke.getCharCode();	// note: getKeyCode() DOES NOT WORK
		if(charCode == keyZoomIn)
		{
				zoomIn.doClick();
		}
		else if(charCode == keyZoomOut)
		{
				zoomOut.doClick();
		}
		else if(charCode == keyFit)
		{
				fit.doClick();
		}
	}// keyPressed()
	
	/** Add control bar icons */
	private void makeLayout()
	{
		// set Zoom scale factor -- BEFORE we get the resulting actions.
		final XJSVGCanvas canvas = mapPanel.getXJSVGCanvas();
		canvas.setZoomScaleFactor(mapPanel.getScaleFactor());
		
		// This is the be-all end-all of reset transforms. It accounts
		// for everything, and will never fail (famous last words)
		// 
		fit = add(new AbstractAction()
		{
			public void actionPerformed(ActionEvent evt)
			{
				JSVGCanvas canvas = mapPanel.getJSVGCanvas();
				if(canvas != null)	// we are being very defensive
				{
					AffineTransform iat = canvas.getInitialTransform();
					SVGSVGElement elt = canvas.getSVGDocument().getRootElement();
					if(iat != null || elt == null) // very very defensive... but iat null check is important
					{
						// rescale viewbox transform to reflect viewbox size
						Dimension vbSize = mapPanel.getScrollerSize();	// size of the canvas' scrolling container (don't include scroll bars)
						CanvasGraphicsNode cgn = canvas.getCanvasGraphicsNode(); 
						
						// ViewBox.getViewTransform is essential for calculating the correct transform,
						// AND accounting for any viewBox attribute of the root SVG element, if present.
						AffineTransform vt = ViewBox.getViewTransform
							(canvas.getFragmentIdentifier(), elt, vbSize.width, vbSize.height);
						cgn.setViewingTransform(vt);
						
						// set rendering transform to 'unscaled'
						AffineTransform t = AffineTransform.getScaleInstance(1,1);
						mapPanel.getJSVGCanvas().setRenderingTransform(t);
					}
				}
			}
 		});
		
		fit.setIcon(Utils.getIcon(ICON_ZOOM_FIT));
		fit.setToolTipText(Utils.getLocalString(I18N_FIT_TIP));
		
		addSeparator();
		
		zoomOut = add(canvas.getActionMap().get(JSVGCanvas.ZOOM_OUT_ACTION));
		zoomOut.setIcon(Utils.getIcon(ICON_ZOOM_OUT));
		zoomOut.setToolTipText(Utils.getLocalString(I18N_ZOOMOUT_TIP));
		
		zoomIn = add(canvas.getActionMap().get(JSVGCanvas.ZOOM_IN_ACTION));
		zoomIn.setIcon(Utils.getIcon(ICON_ZOOM_IN));
		zoomIn.setToolTipText(Utils.getLocalString(I18N_ZOOMIN_TIP));
		
	}// makeLayout()
	
}// class ViewControlBar
