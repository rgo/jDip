//
//  @(#)XJSVGCanvas.java		2/2004
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

import dip.gui.StatusBar;
import dip.gui.dialog.ErrorDialog;
import dip.misc.Utils;


import java.awt.Rectangle;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.Container;
import java.awt.Window;
import java.awt.Cursor;

import java.awt.geom.AffineTransform;

import javax.swing.JFrame;
import javax.swing.ActionMap;

import org.apache.batik.swing.JSVGCanvas;
import org.apache.batik.swing.svg.SVGUserAgent;
import org.apache.batik.bridge.UserAgent;
import org.apache.batik.bridge.BridgeContext;



import java.awt.geom.AffineTransform;
import java.awt.event.ActionEvent;
import java.awt.Dimension;
import org.apache.batik.*;
import org.apache.batik.dom.*;
import org.apache.batik.util.*;
import org.w3c.dom.svg.*;
import org.w3c.dom.svg.SVGSVGElement;

import org.apache.batik.swing.gvt.GVTTreeRendererListener;
import org.apache.batik.swing.gvt.GVTTreeRendererEvent;
import org.apache.batik.bridge.ViewBox;
import org.apache.batik.gvt.CanvasGraphicsNode;


/**
*	Provides for enhanced functionality over a standard JSVGCanvas.
*	<p>
*	Consists of a modified mouse/key listners and a special transform
*	listener which allows the XSVGScroller to properly function.
*	<p>
*	Furthermore, it modifies the JSVGCanvas.ZOOM_OUT_ACTION and 
*	JSVGCanvas.ZOOM_IN_ACTION to reflect the scale factor, as set
*	with setZoomScaleFactor().
*/
public class XJSVGCanvas extends JSVGCanvas 
{
	// constants
	private static final String I18N_ZOOM_FACTOR	= "XJSVGScroller.zoom.text";
	private static final int	MIN_DRAG_DELTA = 5;		// min pixels to count as a drag
	
	// instance variables
	/** The default unit scroll increment */
	private int unitIncrement = 10;
	/** The default block scroll increment */
	private int blockIncrement = 30;
	/** Minimum scale value (if > 0.0) */
	private double minScale = 0.0f;
	/** Maximum scale value (if > 0.0) */
	private double maxScale = 0.0f;
	
	private Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
	private boolean isValidating = false;
	private double lsx,lsy;						// last scale x, y values
	private final StatusBar statusBar;
	
	/**
    *	Creates a new XJSVGCanvas.
    *
    *	@param ua a SVGUserAgent instance or null.
    *	@param eventsEnabled Whether the GVT tree should be reactive to mouse and
    *	key events.
    *	@param selectableText Whether the text should be selectable.
    */
	public XJSVGCanvas(final MapPanel mapPanel, StatusBar statusBar, SVGUserAgent ua, boolean eventsEnabled, boolean selectableText)
	{
		super(ua, eventsEnabled, selectableText);
		this.statusBar = statusBar;
		setMaximumSize(screenSize);
		
		// fix for incorrect setting of initial SVG size by JSVGScrollPane
		addGVTTreeRendererListener(new GVTTreeRendererListener()
		{
			public void gvtRenderingCompleted(GVTTreeRendererEvent e) 
			{
				AffineTransform iat = getInitialTransform();
				SVGSVGElement elt = getSVGDocument().getRootElement();
				if(iat != null || elt == null) // very very defensive... but iat null check is important
				{
					// rescale viewbox transform to reflect viewbox size
					Dimension vbSize = mapPanel.getScrollerSize();	// size of the canvas' scrolling container (don't include scroll bars)
					CanvasGraphicsNode cgn = getCanvasGraphicsNode(); 
					
					// ViewBox.getViewTransform is essential for calculating the correct transform,
					// AND accounting for any viewBox attribute of the root SVG element, if present.
					AffineTransform vt = ViewBox.getViewTransform
						(getFragmentIdentifier(), elt, vbSize.width, vbSize.height);
					cgn.setViewingTransform(vt);
					
					// set rendering transform to 'unscaled'
					AffineTransform t = AffineTransform.getScaleInstance(1,1);
					XJSVGCanvas.super.setRenderingTransform(t);
				}
			}// gvtRenderingCompleted()
			
			public void gvtRenderingCancelled(GVTTreeRendererEvent e) {}
			public void gvtRenderingFailed(GVTTreeRendererEvent e) {}
			public void gvtRenderingPrepare(GVTTreeRendererEvent e) {}
			public void gvtRenderingStarted(GVTTreeRendererEvent e)  {}		
		});
		
	}// XJSVGCanvas()
	
	
	
	/** Sets if this is we should validate SVG or not */
	public void setValidating(boolean value)
	{
		isValidating = value;
	}// setValidating()
	
	
	/**
	*	Overrides createListener() to return our own Listener, with 
	*	several new features we need.
	*
	*/
	protected Listener createListener()
	{
		return new XJSVGCanvasListener();
	}// createListener()
	
	
	/**
	*	Overrides createUserAgent() to return our own UserAgent, which 
	*	allows selectable validation control of the parser.
	*
	*/
	protected UserAgent createUserAgent()
	{
		return new XJSVGUserAgent();
	}// createUserAgent()
	
	
	/** 
	*	Sets the parent component, to which key events are sent.
	*
	*/
	public void setParent(Component c)
	{
		((XJSVGCanvasListener) listener).setParent(c);
	}// setParent()
	
	
	/**
	*	Specialized Listener that provides additional functionality.
	*	<p>
	*	New Features:
	*	<ul>
	*		<li>Short drags are interpreted as clicks
	*		<li>Key events can be passed to the parent (as defined by setParent())
	*	</ul>
	*/
	protected class XJSVGCanvasListener extends JSVGCanvas.CanvasSVGListener
	{
			private int dragX;					// start drag X coord
			private int dragY;					// start drag Y coord
			private boolean inDrag = false;		// 'true' if we are in a drag (versus a click)
			private Component parent = null;	// parent component for events
			
			
			public XJSVGCanvasListener()
			{
			}// XJSVGCanvasListener()
			
			
			public void mouseDragged(MouseEvent e)
			{
				inDrag = true;
				super.mouseDragged(e);
			}// mouseDragged()
			
			
			public void mousePressed(java.awt.event.MouseEvent e)
			{
				// set drag start coordinates
				dragX = e.getX();
				dragY = e.getY();
				super.mousePressed(e);
			}// mousePressed()
			
			
			public void mouseReleased(java.awt.event.MouseEvent e)
			{
				if(inDrag)
				{
					int dx = Math.abs(e.getX() - dragX);
					int dy = Math.abs(e.getY() - dragY);
					
					if(dx < MIN_DRAG_DELTA && dy < MIN_DRAG_DELTA)
					{
						// our drag was short! dispatch a CLICK event.
						//
						MouseEvent click = new MouseEvent(
							e.getComponent(),
							MouseEvent.MOUSE_CLICKED,
							e.getWhen(),
							e.getModifiersEx(),		// modifiers
							e.getX(),
							e.getY(),
							e.getClickCount(),
							e.isPopupTrigger(),
							e.getButton() );
							
						super.mouseClicked(click);
					}
					else
					{
						// not a short drag; return original event
						super.mouseReleased(e);
					}
				}
				
				// reset drag
				inDrag = false;
			}// mouseReleased()
			
			
			public void keyPressed(java.awt.event.KeyEvent e)
			{
				if(parent != null)
				{
					parent.dispatchEvent(e);
				}
				
				super.keyPressed(e);
			}// keyPressed()
			
			
			public void keyReleased(java.awt.event.KeyEvent e)
			{
				if(parent != null)
				{
					parent.dispatchEvent(e);
				}
				
				super.keyReleased(e);
			}// keyReleased()
			
			
			public void keyTyped(java.awt.event.KeyEvent e)
			{
				if(parent != null)
				{
					parent.dispatchEvent(e);
				}
				
				super.keyTyped(e);
			}// keyTyped()
			
			
			/** Set parent to receive key events; null if none. */
			public void setParent(Component c)
			{
				parent = c;
			}// setParent()
			
	}// nested class XJSVGCanvasListener
	
	
	/**
	*	Specialized UserAgent that checks outer class for validation parameter
	*	and subclasses error and message dialogs.
	*/
	protected class XJSVGUserAgent extends JSVGCanvas.CanvasUserAgent
	{
		public XJSVGUserAgent()
		{
			super();
		}// XJSVGUserAgent()
		
 		public boolean isXMLParserValidating()
		{
			return XJSVGCanvas.this.isValidating;
		}// isXMLParserValidating()
		
		/** 
		*	Do nothing. We don't want the Batik 
		*	CursorManager updating our cursor.
		*/
		public void setSVGCursor(Cursor c)
		{
			// do nothing.
		}// setSVGCursor()
		
		
		/** Displays an SVG error Exception using an ErrorDialog */
		public void displayError(Exception ex)
		{
			ErrorDialog.displaySerious(findParent(), ex);
		}// displayError()
		
		
		/** Displays an SVG error String using an ErrorDialog */
		public void displayError(String message)
		{
			ErrorDialog.displaySerious(findParent(), new Exception(message));
		}// message()
		
		
		/** Find the parent frame, if possible. */
		private JFrame findParent()
		{
			// find parent frame, if possible
			Component comp = XJSVGCanvas.this.getParent();
			while(comp != null)
			{
				if(comp instanceof JFrame)
				{
					return (JFrame) comp;
				}
				
				comp = comp.getParent();
			}
			
			return null;
		}// findParent()
		
	}// nested class XJSVGUserAgent
  	
	
	/**
	*	Calls GVTTransformListener.transformChanged(), after setting
	*	the rendering transform of the JSVGCanvas.
    *
	*	@param at an AffineTransform.
	*/
	public void setRenderingTransform(AffineTransform at) 
	{
		// check to see that we are not zooming too little
		if(minScale > 0.0 && (at.getScaleX() < minScale || at.getScaleY() < minScale))
		{
			return; 	// reject transform
		}
		
		// check to see that we are not zooming too much
		if(maxScale > 0.0 && (at.getScaleX() > maxScale || at.getScaleY() > maxScale))
		{
			return;		// reject transform
		}
		
		if(!isEquivalent(lsx, at.getScaleX()))
		{
			statusBar.setText(Utils.getLocalString(I18N_ZOOM_FACTOR, new Double(at.getScaleX())));
		}
		else if(!isEquivalent(lsy, at.getScaleY()))
		{
			statusBar.setText(Utils.getLocalString(I18N_ZOOM_FACTOR, new Double(at.getScaleY())));
		}
		
		lsx = at.getScaleX();
		lsy = at.getScaleY();
		
		// proceed with setting the rendering transform...
		super.setRenderingTransform(at);
	}// setRenderingTransform()
	
	
	/** Sets the minimum allowable scale size. 1.0 == no scaling. any negative value or 0 disables. */
	public void setMinimumScale(double value)
	{
		minScale = value;
	}// setMinimumScale()
	
	
	/** Sets the maximum allowable scale size. 1.0 == no scaling. any negative value or 0 disables. */
	public void setMaximumScale(double value)
	{
		maxScale = value;
	}// setMaximumScale()
	
	/**
	*	Sets the zoom in/out scale factor to that given. For example, if
	*	set to 2.0, zoom in will be x2 and zoom out will be 0.5 (1/2).
	*/
	public void setZoomScaleFactor(float scaleFactor)
	{
		final ActionMap actionMap = getActionMap();
        actionMap.put(JSVGCanvas.ZOOM_IN_ACTION, new ZoomAction(scaleFactor));
        actionMap.put(JSVGCanvas.ZOOM_OUT_ACTION, new ZoomAction(1.0 / scaleFactor));
	}// setZoomScaleFactor()
	
	
	/** Test for floating-point "equivalence" */
	private boolean isEquivalent(double a, double b)
	{
		return(Math.abs(a-b) <= 0.0001);
	}// isEquivalent()
	
	
	
}// class XJSVGCanvas
