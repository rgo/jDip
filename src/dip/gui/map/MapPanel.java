//
//  @(#)MapPanel.java		4/2002
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

import dip.gui.map.RenderCommandFactory.RenderCommand;

import dip.world.Position;
import dip.world.Province;
import dip.world.TurnState;
import dip.world.World;
import dip.world.RuleOptions;
import dip.world.variant.data.Variant;
import dip.world.variant.data.SymbolPack;

import dip.gui.ClientFrame;
import dip.gui.OrderDisplayPanel;
import dip.gui.ClientMenu;
import dip.gui.StatusBar;
import dip.gui.AbstractCFPListener;
import dip.gui.dialog.ErrorDialog;
import dip.gui.dialog.prefs.GeneralPreferencePanel;

import dip.world.variant.VariantManager;
import dip.world.variant.data.MapGraphic;

import dip.order.ValidationOptions;

import dip.misc.Utils;
import dip.misc.Log;
import java.util.Date;

import java.awt.Cursor;
import java.awt.geom.AffineTransform;
import java.net.URL;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Dimension;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

import javax.swing.JPanel;
import javax.swing.*;

import org.w3c.dom.svg.SVGDocument;
import org.w3c.dom.Document;

import org.apache.batik.swing.JSVGCanvas;
import org.apache.batik.swing.gvt.GVTTreeRendererAdapter;
import org.apache.batik.swing.gvt.GVTTreeRendererEvent;
import org.apache.batik.swing.svg.GVTTreeBuilderAdapter;
import org.apache.batik.swing.svg.GVTTreeBuilderEvent;
import org.apache.batik.swing.svg.SVGDocumentLoaderAdapter;
import org.apache.batik.swing.svg.SVGDocumentLoaderEvent;
import org.apache.batik.bridge.UpdateManagerListener;
import org.apache.batik.bridge.UpdateManagerEvent;
import org.apache.batik.util.ParsedURL;
import org.apache.batik.dom.svg.SVGOMDocument;

import org.apache.batik.bridge.BridgeContext;
import org.apache.batik.gvt.*;

import java.io.*;

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.batik.dom.svg.*;
import org.apache.batik.dom.util.*;
import org.apache.batik.util.*;
import org.w3c.dom.*;

/**
*	The Main Map display component.
*	<p>
*	Takes care of setting up the Batik rendering component, as well as loading 
*	the map (in concert with SymbolInjector)
*	<p>
*	This is also the container for the Control Bar.
*	<p>
*/
public class MapPanel extends JPanel
{
	// constants
	// il8n localizers
	private static final String MP_VARIANT_NOT_FOUND = "MapPanel.error.novariant";
	
	private static final String DOC_LOAD_STARTED = "MapPanel.doc.load.start";
	private static final String DOC_LOAD_COMPLETED = "MapPanel.doc.load.complete";
	private static final String DOC_LOAD_FAILED = "MapPanel.doc.load.failed";
	
	private static final String GVT_RENDER_STARTED = "MapPanel.gvt.render.start";
	private static final String GVT_RENDER_EXTRACTING = "MapPanel.gvt.render.extract";
	private static final String GVT_RENDER_COMPLETED = "MapPanel.gvt.render.complete";
	
	private static final String GVT_BUILD_STARTED = "MapPanel.gvt.build.start";
	private static final String GVT_BUILD_COMPLETED = "MapPanel.gvt.build.complete";
	private static final String GVT_BUILD_FAILED = "MapPanel.gvt.build.failed";
	
	
	// misc
	private static final String PSEUDO_ATTRIBUTE_TYPE = "type";
	private static final String PSEUDO_ATTRIBUTE_HREF = "href";
	private static final String XSL_PROCESSING_INSTRUCTION_TYPE = "text/xsl";

	

	
	
	
	/** update text message */
	private static final String updateMessage = Utils.getLocalString("MapPanel.update.text");
	
	/** Default cursor */
	public static final Cursor DEFAULT_CURSOR = java.awt.Cursor.getDefaultCursor();
	
	/** Cursor for display when order input is invalid. */
	public static final Cursor BAD_ACTION;
	public static final Cursor CURSOR_DRAG_ARMY;
	public static final Cursor CURSOR_DRAG_FLEET;
	public static final Cursor CURSOR_DRAG_WING;
	public static final Cursor CURSOR_DRAG_ARMY_NO;
	public static final Cursor CURSOR_DRAG_FLEET_NO;
	public static final Cursor CURSOR_DRAG_WING_NO;
	
	
	/** Default scale factor */
	public static final float DEFAULT_SCALE_FACTOR = 1.2f;
	
	// final variables
	private final ClientFrame clientFrame;
	private final XJSVGCanvas svgCanvas;
	
	protected final StatusBarUtils statusBarUtils;
	private final StatusBar statusBar;
	private final XJSVGScroller scroller;
	
	// non-final variables
	private MenuController menuController = null;
	private MapRenderer2 mapRenderer = null;
	private Position position = null;
	private TurnState turnState = null;
	private World world = null;
	private MP_PropertyListener propListener = null;
	private ControlBar controlBar = null;
	private DOMUIEventListener eventListener = null;
	private float scaleFactor = DEFAULT_SCALE_FACTOR;
	private JPanel bottomPanel = null;
	private MP_UpdateManagerListener uml = null;
	private boolean isLoaded = false;
	private boolean isReloading = false;	// if we are reloading a map
	private SymbolPack symbolPack = null;
	
	private MP_GVTRenderListener gvtRenderListener = null;
	private MP_DocumentListener documentListener = null;
	private MP_GVTTreeBuilderListener treeBuilderListener = null;
	
	private Object oldLabelLevel = null;
	
	// for timing information
	protected long startTime = 0L;
	
	
	/** Set Default Cursor */
	static
	{
		// Attempt to set default cursor.
		java.awt.Toolkit tk = java.awt.Toolkit.getDefaultToolkit();
		Dimension d = tk.getBestCursorSize(32, 32);
		int colors = tk.getMaximumCursorColors();
		
		// determine if the platform supports custom cursors.
		if (!d.equals(new Dimension(0, 0)) && (colors != 0))
		{
			ImageIcon ii = Utils.getImageIcon("resource/common/cursors/nodrop.gif");
			if(ii == null)
			{
				// error loading cursor! just use default cursor.
				// this may not be the best cursor to use.
				BAD_ACTION = java.awt.dnd.DragSource.DefaultCopyNoDrop;
			}
			else
			{
				BAD_ACTION = tk.createCustomCursor(ii.getImage(), new java.awt.Point(16, 16), "custom_nodrop");
			}
			
			// Drag cursors
			ii = Utils.getImageIcon("resource/common/cursors/drag_army" + (colors == 2? "_2" : "") + ".gif");
			if(ii == null)
			{
				// error loading cursor! just use default cursor.
				// this may not be the best cursor to use.
				CURSOR_DRAG_ARMY = DEFAULT_CURSOR;
			}
			else
			{
				CURSOR_DRAG_ARMY = tk.createCustomCursor(ii.getImage(), new java.awt.Point(5, 5),
						"cursor_drag_army");
			}
			
			ii = Utils.getImageIcon("resource/common/cursors/drag_fleet" + (colors == 2? "_2" : "") + ".gif");
			if(ii == null)
			{
				// error loading cursor! just use default cursor.
				CURSOR_DRAG_FLEET = DEFAULT_CURSOR;
			}
			else
			{
				CURSOR_DRAG_FLEET = tk.createCustomCursor(ii.getImage(), new java.awt.Point(5, 5),
						"cursor_drag_fleet");
			}
			
			ii = Utils.getImageIcon("resource/common/cursors/drag_wing" + (colors == 2? "_2" : "") + ".gif");
			if(ii == null)
			{
				// error loading cursor! just use default cursor.
				CURSOR_DRAG_WING = DEFAULT_CURSOR;
			}
			else
			{
				CURSOR_DRAG_WING = tk.createCustomCursor(ii.getImage(), new java.awt.Point(5, 5),
						"cursor_drag_wing");
			}
			
			ii = Utils.getImageIcon("resource/common/cursors/drag_army_no" + (colors == 2? "_2" : "") + ".gif");
			if(ii == null)
			{
				// error loading cursor! just use default cursor.
				CURSOR_DRAG_ARMY_NO = DEFAULT_CURSOR;
			}
			else
			{
				CURSOR_DRAG_ARMY_NO = tk.createCustomCursor(ii.getImage(), new java.awt.Point(5, 5),
						"cursor_drag_army_no");
			}
			
			ii = Utils.getImageIcon("resource/common/cursors/drag_fleet_no" + (colors == 2? "_2" : "") + ".gif");
			if(ii == null)
			{
				// error loading cursor! just use default cursor.
				CURSOR_DRAG_FLEET_NO = DEFAULT_CURSOR;
			}
			else
			{
				CURSOR_DRAG_FLEET_NO = tk.createCustomCursor(ii.getImage(), new java.awt.Point(5, 5),
						"cursor_drag_fleet_no");
			}

			ii = Utils.getImageIcon("resource/common/cursors/drag_wing_no" + (colors == 2? "_2" : "") + ".gif");
			if(ii == null)
			{
				// error loading cursor! just use default cursor.
				CURSOR_DRAG_WING_NO = DEFAULT_CURSOR;
			}
			else
			{
				CURSOR_DRAG_WING_NO = tk.createCustomCursor(ii.getImage(), new java.awt.Point(5, 5),
						"cursor_drag_wing_no");
			}
		}
		else
		{
			BAD_ACTION = java.awt.dnd.DragSource.DefaultCopyNoDrop;
			CURSOR_DRAG_ARMY = DEFAULT_CURSOR;
			CURSOR_DRAG_ARMY_NO = BAD_ACTION;
			CURSOR_DRAG_FLEET = DEFAULT_CURSOR;
			CURSOR_DRAG_FLEET_NO = BAD_ACTION;
			CURSOR_DRAG_WING = DEFAULT_CURSOR;
			CURSOR_DRAG_WING_NO = BAD_ACTION;
		}
	}
	
	
	/** Creates a MapPanel */
	public MapPanel(ClientFrame clientFrame)
	{
		super(new BorderLayout());
		
		startTime = System.currentTimeMillis();
		Log.printTimed(startTime, "MapPanel() constructor start.");
		
		this.clientFrame = clientFrame;
		this.statusBar = clientFrame.getStatusBar();
		
		this.setMinimumSize(new Dimension(10,10));
		
		// bottomPanel: holds control bar / any other components
		bottomPanel = new JPanel(new BorderLayout());
		this.add(bottomPanel, BorderLayout.SOUTH);
		
		// add components to frame that do not depend upon GVT
		statusBarUtils = new StatusBarUtils(this, clientFrame.getStatusBar());
		
		// setup JSVGCanvas
		svgCanvas = new XJSVGCanvas(this, statusBar, null, true, false);
		svgCanvas.setValidating(clientFrame.getValidating());
		svgCanvas.setDocumentState(JSVGCanvas.ALWAYS_DYNAMIC);
		svgCanvas.setRecenterOnResize(false);	// batik 1.5.1
		svgCanvas.setDoubleBufferedRendering(true);
		svgCanvas.setProgressivePaint(true);	// faster??
		svgCanvas.setEnableImageZoomInteractor(true);
		svgCanvas.setEnableResetTransformInteractor(true);
		svgCanvas.setEnablePanInteractor(false);
		svgCanvas.setEnableRotateInteractor(false);	// rotation disabled
		svgCanvas.setParent(this);	// send all key events here
		
		// setup scroller
		scroller = new XJSVGScroller(svgCanvas);
		add(scroller, BorderLayout.CENTER);
		
		// setup DOMUIEventListener
		eventListener = new DOMUIEventListener();
		
		// setup property listener
		propListener = new MP_PropertyListener();
		clientFrame.addPropertyChangeListener(propListener);
		
		// setup default controlbar
		setControlBar(null);
		
		Log.printTimed(startTime, "MapPanel() constructor end.");
	}// MapPanel()
	
	
	/** Set the SVG Document from XML Document */ 
	private void setDocument(Document xmlDoc, Variant variant)
	{
		Log.println("MP: setDocument()");
		
		// setup private loader-listeners
		gvtRenderListener = new MP_GVTRenderListener();
		documentListener = new MP_DocumentListener();       
		treeBuilderListener = new MP_GVTTreeBuilderListener();
		
		svgCanvas.addGVTTreeRendererListener(gvtRenderListener);
		svgCanvas.addSVGDocumentLoaderListener(documentListener);
		svgCanvas.addGVTTreeBuilderListener(treeBuilderListener);
		           
		// setup update listener
		uml = new MP_UpdateManagerListener();
		svgCanvas.addUpdateManagerListener(uml);
		
		// load map. Note that this bypasses the DocumentLoader
		// listener, as it createas an SVG document synchronously 
		// from the XML document we submit.
		svgCanvas.setDocumentState(JSVGCanvas.ALWAYS_DYNAMIC);
		
		// we MUST transform the document, prior to setDocument()!
		// subtle bugs emerge if we do not.
		//
		try
		{
			svgCanvas.setDocument( transform(xmlDoc, 
				VariantManager.getVariantPackageJarURL(variant).toString()) );
		}
		catch(Exception e)
		{
			ErrorDialog.displaySerious(clientFrame, e);
		}
		
		// fix the URI; if we don't do this, we aren't able to load 
		// files that are referred to by the SVG. For example, the bitmap
		// for detailed standard map.
		//
		if(svgCanvas.getSVGDocument() instanceof SVGOMDocument)
		{
			final SVGOMDocument omd = (SVGOMDocument) svgCanvas.getSVGDocument();
			omd.setURLObject(VariantManager.getVariantPackageJarURL(variant));
		}
		else              
		{
			// shouldn't happen.
			Log.println("ERROR: MapPanel::setDocument(): object model replacement? SVGOMDocument not found. URI not set.");
		}
	}// setDocument()
	
	
	
	/** Get the current Scroller container size */
	public Dimension getScrollerSize()
	{
		return scroller.getSize();
	}// getScrollerSize()
	
	/** gets URI of current document */
	public String getURI()
	{
		return svgCanvas.getURI();
	}// getURI()
	
	
	/** Get JSVGCanvas */
	public JSVGCanvas getJSVGCanvas()
	{
		return svgCanvas;
	}// getJSVGCanvas()
	
	/** Get XJSVGCanvas */
	public XJSVGCanvas getXJSVGCanvas()
	{
		return svgCanvas;
	}// getJSVGCanvas()
	
	/** Gets the map scale factor (for zooming) */
	public float getScaleFactor()
	{
		return scaleFactor;
	}// getScaleFactor()
	
	public final ClientFrame getClientFrame()
	{
		return clientFrame;
	}// getClientFrame()
	
	/** Convenience method: get the order display panel. */
	public OrderDisplayPanel getOrderDisplayPanel()
	{
		return clientFrame.getOrderDisplayPanel();
	}// getOrderDisplayPanel()
	
	/** Convenience method: get the map renderer. */
	public MapRenderer2 getMapRenderer()
	{
		return mapRenderer;
	}// getMapRenderer()
	
	/** Convenience method: get the MapMetadata object */
	public MapMetadata getMapMetadata()
	{
		if(getMapRenderer() != null)
		{
			return getMapRenderer().getMapMetadata();
		}
		
		return null;
	}// getMapMetadata()
	
	
	/** Convenience method: get map renderer command factory class. */
	public RenderCommandFactory getRenderCommandFactory()
	{
		return mapRenderer.getRenderCommandFactory();
	}// getRenderCommandFactory()
	
	/** Convenience method: get the status bar */
	public StatusBar getStatusBar()
	{
		return statusBar;
	}// getStatusBar()
	
	/** Convenience method: get the status bar utility class */
	public StatusBarUtils getStatusBarUtils()
	{
		return statusBarUtils;
	}// getStatusBarUtils()
	
	
	/** Convenience method: get the World */
	public World getWorld()
	{
		return world;
	}// getWorld()
	
	
	/** DOMUI event hook */
	protected void setDOMUIEventListener(DOMUIEventListener domEventListener)
	{
		eventListener = domEventListener;
	}// setDOMUIEventListener()
	
	
	/** DOMUI event hook */
	protected DOMUIEventListener getDOMUIEventListener()
	{
		return eventListener;
	}// getDOMUIEventListener()
	
	
	
	/** Sets View or Order control bar, as appropriate, based on the mode */
	private void setControlBar()
	{
		Log.println("MP::setControlBar())");
		ControlBar cb = null;
		
		if(turnState == null)
		{
			cb = new ViewControlBar(this);
		}
		else
		{
			String mode = clientFrame.getMode();
			
			if( mode == ClientFrame.MODE_NONE
				|| mode == ClientFrame.MODE_REVIEW )
			{
				cb = new ViewControlBar(this);
			}
			else if(mode == ClientFrame.MODE_ORDER)
			{
				cb = new OrderControlBar(this);
			}
			else if(mode == ClientFrame.MODE_EDIT)
			{
				cb = new EditControlBar(this);
			}
			else
			{
				throw new IllegalArgumentException("unknown mode");
			}
		}
		
		// remove old controlBar, and replace, if not null.
		setControlBar(cb);
	}// setControlBar()
	
	
	/** 
	*	Explicitly set a ControlBar. 
	*	If null, uses the default ControlBar, which has
	*	no buttons and does nothing.
	*/
	public void setControlBar(ControlBar cb)
	{
		// remove old controlBar, and replace, if not null.
		if(cb == null)
		{
			cb = new EmptyControlBar(this);
		}
		
		if(controlBar != null)
		{
			bottomPanel.remove(controlBar);
		}
		
		controlBar = cb;
		eventListener.setDOMUIEventHandler(controlBar);
		bottomPanel.add(controlBar, BorderLayout.NORTH);
		bottomPanel.revalidate();
	}// setControlBar()
	
	
	/** Gets the current ControlBar */
	public ControlBar getControlBar()
	{
		return controlBar;
	}// getControlBar()
	
	
	/** 
	*	Sets the cursor over the JSVGScrollCanvas.
	*	<p>
	*	NOTE: this is currently disabled for OS X due to 
	*	weird bugs.
	*/
	public void setMapCursor(Cursor cursor)
	{
		if(!Utils.isOSX())
		{
			svgCanvas.setCursor(cursor);
		}
	}// setCursor()
	
	
	/** Get SVGDocument [may be null!] */
	public SVGDocument getSVGDocument()
	{
		return svgCanvas.getSVGDocument();
	}// getJSVGCanvas()
	
	/** Get the current Position */
	public Position getPosition()
	{
		return position;
	}
	
	/** Get the current TurnState */
	public TurnState getTurnState()
	{
		return turnState;
	}// getTurnState()
	
	/** Gets the RuleOptions */
	public RuleOptions getRuleOptions()
	{
		return world.getRuleOptions();
	}// getRuleOptions()
	
	/** Release any resources held by this component / reset state */
	public void close()
	{
		try
		{
			svgCanvas.flushImageCache();
			svgCanvas.setSVGDocument(null);
			svgCanvas.stopProcessing();
			svgCanvas.dispose();
			
			if(uml != null)
			{
				svgCanvas.removeUpdateManagerListener(uml);
				uml.resetText();
				uml = null;
			}
			
			if(gvtRenderListener != null)
			{
				svgCanvas.removeGVTTreeRendererListener(gvtRenderListener);
			}
			
			if(documentListener != null)
			{
				svgCanvas.removeSVGDocumentLoaderListener(documentListener);
			}
			
			if(treeBuilderListener != null)
			{
				svgCanvas.removeGVTTreeBuilderListener(treeBuilderListener);
			}
			
			
			if(menuController != null)
			{
				menuController.close();
				menuController = null;
			}
			
			if(mapRenderer != null)
			{
				mapRenderer.close();
				mapRenderer = null;
			}
			
			
			controlBar = null;
			eventListener = null;
			symbolPack = null;
			world = null;
			turnState = null;
		}
		catch(Exception e)
		{
			Log.println("MapPanel::close() exception...");
			Log.println(e);
		}
	}// close()
	
	/**
	*	Convenience method: updates the rendering of a province.
	*
	*/
	public void updateProvince(Province province)
	{
		RenderCommand rc = mapRenderer.getRenderCommandFactory().createRCRenderProvince(mapRenderer, province);
		mapRenderer.execRenderCommand(rc);
	}// updateProvince()        



    /** Sets which labels are enabled/disabled */
    private void setMenuLabelOptions()
	{
		MapMetadata mmd = mapRenderer.getMapMetadata();
		if(mmd == null)
		{
			return;
		}
		
		ClientMenu cm = clientFrame.getClientMenu();
		boolean fullLabels = mmd.getDisplayParamBoolean(MapMetadata.ATT_LABELS_FULL, false);
		boolean briefLabels = mmd.getDisplayParamBoolean(MapMetadata.ATT_LABELS_BRIEF, false);
		
		// if no label layers, disable all label support
		if(!fullLabels && !briefLabels)
		{
			cm.setEnabled(ClientMenu.VIEW_NAMES, false);
		}
		
		// disable individual label support
		cm.setEnabled(ClientMenu.VIEW_NAMES_FULL, fullLabels);
		cm.setEnabled(ClientMenu.VIEW_NAMES_SHORT, briefLabels);
	}// setMenuLabelOptions()
	
	/** Sets min, max, and scale factor variables from metadata (if found) */
	private void setScalingParameters()
	{
		MapMetadata mmd = mapRenderer.getMapMetadata();
		if(mmd == null)
		{
			return;
		}
		
		scaleFactor = mmd.getDisplayParamFloat(MapMetadata.ATT_ZOOM_FACTOR, DEFAULT_SCALE_FACTOR);
		
		int minZoom = mmd.getDisplayParamInt(MapMetadata.ATT_ZOOM_MIN, 1);		// default minimum: 1% 
		int maxZoom = mmd.getDisplayParamInt(MapMetadata.ATT_ZOOM_MAX, 1600);	// default maximum: 1600%
		
		// swap values if min/max switched.
		if(minZoom > maxZoom)
		{
			int tmp = minZoom;
			minZoom = maxZoom;
			maxZoom = tmp;
		}
		
		svgCanvas.setMinimumScale( ((double)minZoom / 100.0) );
		svgCanvas.setMaximumScale( ((double)maxZoom / 100.0) );
	}// setScalingParameters()
	
	
	/**
	*	This class sets up object/components that depend upon the canvas/map to 
	*	be loaded, and the GVT tree set up. It is to execute only a single time.
	*/
	private class MP_GVTRenderListener extends GVTTreeRendererAdapter
	{
		private boolean loaded = false;
		
		public void gvtRenderingStarted(GVTTreeRendererEvent e)
		{
			Log.printTimed(startTime, "MapPanel() GVTRender start.");
			if(!loaded)
			{
				statusBar.incPBValue();
				statusBar.setText(Utils.getLocalString(GVT_RENDER_STARTED));
			}
		}// gvtRenderingStarted()
		
		public void gvtRenderingCompleted(GVTTreeRendererEvent e)
		{
			Log.printTimed(startTime, "MapPanel() GVTRender completing...");
			if(!loaded)
			{
				statusBar.incPBValue();
				statusBar.setText(Utils.getLocalString(GVT_RENDER_EXTRACTING));
				// DOM - requring events here
			
				try
				{
					mapRenderer = new DefaultMapRenderer2(MapPanel.this, symbolPack);
				}
				catch(MapException me)
				{
					ErrorDialog.displaySerious(clientFrame, me);
					svgCanvas.stopProcessing();
					statusBar.hidePB();
					svgCanvas.removeGVTTreeRendererListener(this);
					statusBar.setText(Utils.getLocalString(DOC_LOAD_FAILED));
					return;
				}
				
				// alter menu items depending upon metadata value for labels.
				clientFrame.getClientMenu().setViewRenderItemsEnabled(true);
				setMenuLabelOptions();
				
				// configure scaling
				setScalingParameters();
				
				// add components:
				menuController = new MenuController(MapPanel.this);
				
				if(isReloading)
				{
					scroller.reset();
				}
				
				final RenderCommandFactory rcf = mapRenderer.getRenderCommandFactory();
				
				// add property listener for setting position / turnstate information
				// and set the current position.
				//
				// we do a 'force' if we are reloading
				RenderCommand rc = rcf.createRCSetTurnstate(mapRenderer, turnState);
				mapRenderer.execRenderCommand(rc);
				
				if(isReloading)
				{
					// if we have an old label level set, we'll set the
					// same for the new map
					//
					if(oldLabelLevel != null)
					{
						rc = rcf.createRCSetLabel(mapRenderer, oldLabelLevel);
						mapRenderer.execRenderCommand(rc);
					}
					
					// Because we created a new DMR2, it didn't re-create the 
					// orders, because it thought the 'old' turnstate was null
					// and the new turnstate won't update, because the orders
					// think that they are already drawn.
					//
					// To fix this, we must destroy the existing orders, and
					// then re-render them.
					scroller.revalidate();
					rc = ((DMR2RenderCommandFactory) rcf).createRCRenderAllForced(mapRenderer);
					mapRenderer.execRenderCommand(rc);
				}
				else
				{
					rc = mapRenderer.getRenderCommandFactory().createRCRenderAll(mapRenderer);
					mapRenderer.execRenderCommand(rc);
					
					// set default label level
					final String defaultLabelLevel = GeneralPreferencePanel.getMapLabelSetting();
					rc = rcf.createRCSetLabel(mapRenderer, defaultLabelLevel);
					mapRenderer.execRenderCommand(rc);
				}
				
				// set default control bar (View Control Bar)
				setControlBar();
				
				// set focus
				//MapPanel.this.requestFocusInWindow();
				//svgCanvas.requestFocusInWindow();
				controlBar.requestFocusInWindow();
				
				statusBar.setText(Utils.getLocalString(GVT_RENDER_COMPLETED));
				//svgCanvas.removeGVTTreeRendererListener(this);
				statusBar.hidePB();
				Log.printTimed(startTime, "MapPanel() GVTRender completed.");
			}
			loaded = true;
		}// gvtRenderingCompleted()
	}// inner class GVTRenderListener
	
	
	/**
	*	This class sets up object/components that depend upon the DOM
	*	NOTE: we use setDocument(), and thus this really isn't used.
	*/
	private class MP_DocumentListener extends SVGDocumentLoaderAdapter
	{
		public void documentLoadingStarted(SVGDocumentLoaderEvent e)
		{
			Log.printTimed(startTime, "MapPanel() DocumentLoad started.");
			clientFrame.getClientMenu().setViewRenderItemsEnabled(false);
			statusBar.incPBValue();
			//statusBar.setText(Utils.getLocalString(DOC_LOAD_STARTED));
		}// documentLoadingStarted()
 		
		public void documentLoadingFailed(SVGDocumentLoaderEvent e)
		{
			statusBar.setText(Utils.getLocalString(DOC_LOAD_FAILED));
			statusBar.hidePB();
		}// documentLoadingFailed()
		
		public void documentLoadingCompleted(SVGDocumentLoaderEvent e)
		{
			Log.printTimed(startTime, "MapPanel() DocumentLoad completed.");
			statusBar.incPBValue();
			statusBar.setText(Utils.getLocalString(DOC_LOAD_COMPLETED));
			svgCanvas.removeSVGDocumentLoaderListener(this);
		}// documentLoadingCompleted()
	}// inner class GVTRenderListener
	
	
	
	/**
	*	Statusbar messages
	*/
	private class MP_GVTTreeBuilderListener extends GVTTreeBuilderAdapter
	{
		public void gvtBuildStarted(GVTTreeBuilderEvent e)
		{
			Log.printTimed(startTime, "MapPanel() GVTTreeBuild completed.");
			statusBar.incPBValue();
			statusBar.setText(Utils.getLocalString(GVT_BUILD_STARTED));
		}// documentLoadingStarted()
 		
		public void gvtBuildFailed(GVTTreeBuilderEvent e)
		{
			statusBar.setText(Utils.getLocalString(GVT_BUILD_FAILED));
			statusBar.hidePB();
		}// documentLoadingFailed()
		
		public void gvtBuildCompleted(GVTTreeBuilderEvent e)
		{
			Log.printTimed(startTime, "MapPanel() GVTTreeBuild completed.");
			statusBar.incPBValue();
			statusBar.setText(Utils.getLocalString(GVT_BUILD_COMPLETED));
			svgCanvas.removeGVTTreeBuilderListener(this);
		}// documentLoadingCompleted()
	}// inner class MP_GVTTreeBuilderListener	
	
	
	/** ClientFrame PropertyChangeListener */
	private class MP_PropertyListener extends AbstractCFPListener
	{
		
		public void actionWorldCreated(World w)			
		{
			if(mapRenderer != null)
			{
				throw new IllegalStateException();
			}
			else
			{
				world = w;
			}
		}// actionWorldCreated()
		
		public void actionWorldDestroyed(World w)		
		{
			if(mapRenderer != null)
			{
				close();
			}
		}// actionWorldDestroyed()
		
		public void actionValOptsChanged(ValidationOptions options)		
		{
			if(mapRenderer != null)
			{
				// if we have an OrderControl bar or derivitive
				// update its order validation options
				if(getControlBar() instanceof OrderControlBar)
				{
					((OrderControlBar) getControlBar()).setValidationOptions(options);
				}
			}
		}// actionValOptsChanged()
		
		public void actionModeChanged(String mode)	
		{
			if(mapRenderer != null)
			{
				setControlBar();
				statusBar.clearText();
			}
		}// actionModeChanged()
		
		public synchronized void actionTurnstateChanged(TurnState ts)	
		{
			if(mapRenderer != null)
			{
				turnState = ts;
				position = turnState.getPosition();
				setControlBar();
				statusBar.clearText();
			}
			else
			{
				if(!isLoaded)
				{
					// set turnstate & position
					turnState = ts;
					position = turnState.getPosition();
					
					// load URL and resolve
					World.VariantInfo vi = world.getVariantInfo();
					Variant variant = VariantManager.getVariant( vi.getVariantName(), vi.getVariantVersion() );
					
					// TODO: clean this loading logic up
					if(variant == null)
					{
						Exception e = new IllegalStateException(
							Utils.getLocalString(MP_VARIANT_NOT_FOUND, 
								"?",
								"?"));
						
						ErrorDialog.displayGeneral(clientFrame, e);
					}
					
					MapGraphic mg = variant.getMapGrapic( vi.getMapName() );
					if(mg == null)
					{
						// try a default map graphic
						mg = variant.getDefaultMapGraphic();
						
						if(mg == null)
						{
							Exception e = new IllegalStateException(
								Utils.getLocalString(MP_VARIANT_NOT_FOUND, 
									vi.getVariantName(),
									vi.getMapName()) );
							
							ErrorDialog.displayGeneral(clientFrame, e);
						}
					}
					
					URL url = VariantManager.getResource( variant, mg.getURI() );
					if(url == null)
					{
						
						Exception e = new IllegalStateException(
							Utils.getLocalString(MP_VARIANT_NOT_FOUND, 
								vi.getVariantName(),
								vi.getMapName()) );
						
						ErrorDialog.displayGeneral(clientFrame, e);
					}
					
					
					symbolPack = VariantManager.getSymbolPack(mg, 
						vi.getSymbolPackName(), vi.getSymbolPackVersion());
					
					// actual loading starts here
					//
					// setup progress bar
					// 
					statusBar.showPB(0, 9);	// was 7, may make 8 or 9
					statusBar.setText(Utils.getLocalString(DOC_LOAD_STARTED));
					
					try
					{
						SymbolInjector si = new SymbolInjector(clientFrame, variant, 
							mg, symbolPack);
						statusBar.incPBValue();
						si.inject();
						statusBar.incPBValue();
						setDocument( si.getDocument(), variant );
					}
					catch(Exception e)
					{
						statusBar.setText(Utils.getLocalString(DOC_LOAD_FAILED));
						statusBar.hidePB();
						ErrorDialog.displayGeneral(clientFrame, e);
					}
					isLoaded = true;
				}
			}
		}// actionTurnstateChanged()
	}// inner class MP_PropertyListener
	
	
	
	
	/** Inner class that indicates in the Status Bar when we are drawing */
	private class MP_UpdateManagerListener implements UpdateManagerListener
	{
		private String lastModeText = null;
		
		public void managerResumed(org.apache.batik.bridge.UpdateManagerEvent e)
		{
		}
		
		public void managerStarted(org.apache.batik.bridge.UpdateManagerEvent e)
		{
		}
		
		public void managerStopped(org.apache.batik.bridge.UpdateManagerEvent e)
		{
		}
		
		public void managerSuspended(org.apache.batik.bridge.UpdateManagerEvent e)
		{
		}
		
		public void updateCompleted(org.apache.batik.bridge.UpdateManagerEvent e)
		{
			resetText();
		}// updateCompleted()
		
		public void updateFailed(org.apache.batik.bridge.UpdateManagerEvent e)
		{
			resetText();
		}// updateFailed()
		
		public void updateStarted(org.apache.batik.bridge.UpdateManagerEvent e) 		
		{
			lastModeText = statusBar.getModeText();
			statusBar.setModeText(updateMessage);
		}// updateStarted()
		
		
		public void resetText()
		{
			// if updateMessage was the last message, we also should clear, otherwise
			// we are always stuck on updateMessage
			if(lastModeText == null || lastModeText.equals(updateMessage))
			{
				statusBar.clearModeText();
			}
			else
			{
				statusBar.setModeText(lastModeText);
			}
		}// resetText()
		
	}// inner class MP_UpdateManagerListener
	
	
	
	
	
	/**
	*	Reloads the map, using the map URI as defined in World.
	*	This is most useful if the map URI has changed.
	*/
	public void reloadMap()
	{
		svgCanvas.flushImageCache();
		svgCanvas.suspendProcessing();
		
		// cleanup this
		if(mapRenderer != null)
		{
			oldLabelLevel = mapRenderer.getRenderSetting(MapRenderer2.KEY_LABELS);
			mapRenderer.close();
			mapRenderer = null;
		}
		
		if(uml != null)
		{
			svgCanvas.removeUpdateManagerListener(uml);
			uml.resetText();
			uml = null;
		}
		
		if(gvtRenderListener != null)
		{
			svgCanvas.removeGVTTreeRendererListener(gvtRenderListener);
		}
		
		if(documentListener != null)
		{
			svgCanvas.removeSVGDocumentLoaderListener(documentListener);
		}
		
		if(treeBuilderListener != null)
		{
			svgCanvas.removeGVTTreeBuilderListener(treeBuilderListener);
		}
		
		// reload the map
		assert(turnState != null);
		svgCanvas.resumeProcessing();
		isLoaded = false;
		isReloading = true;	// this activates some additional code
		clientFrame.fireTurnstateChanged(turnState);
	}// reloadMap()
	
	
	
	/**	 
	* 	This code is based on: <br>
	* 	http://cvs.apache.org/viewcvs.cgi/xml-batik/sources/org/apache/batik/apps/svgbrowser/XMLInputHandler.java?rev=1.7&view=auto
	*	<p>
	*	Essentially, it fixes the namespaces so that resolving URIs to 
	*	the variant-pack jar's actually works. This fixes bug #900714.
	*
	*/
    private Document transform(final Document inDoc, final String uri)
	throws TransformerException, TransformerConfigurationException, IOException 
	{
        TransformerFactory tFactory = TransformerFactory.newInstance();
		
        Transformer transformer = tFactory.newTransformer();
		
		
        // Now, apply the transformation to the input document.
        //
        // <!> Due to issues with namespaces, the transform creates the 
        //     result in a stream which is parsed. This is sub-optimal
        //     but this was the only solution found to be able to 
        //     generate content in the proper namespaces.
        //
        // SVGOMDocument outDoc = 
        //   (SVGOMDocument)impl.createDocument(svgNS, "svg", null);
        // outDoc.setURLObject(new URL(uri));
        // transformer.transform
        //     (new DOMSource(inDoc),
        //     new DOMResult(outDoc.getDocumentElement()));
        //
        StringWriter sw = new StringWriter();
        StreamResult result = new StreamResult(sw);
        transformer.transform(new DOMSource(inDoc),
                              result);
        sw.flush();
        sw.close();

        String parser = XMLResourceDescriptor.getXMLParserClassName();
        SAXSVGDocumentFactory f = new SAXSVGDocumentFactory(parser);
        SVGDocument outDoc = f.createSVGDocument(uri, new StringReader(sw.toString()));
		
		return outDoc;
    }
	
}// class MapPanel



