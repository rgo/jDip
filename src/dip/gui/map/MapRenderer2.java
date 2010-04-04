//
//  @(#)MapRenderer2.java		5/2003
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

import dip.gui.map.RenderCommandFactory.RenderCommand;

import dip.gui.ClientFrame;
import dip.gui.AbstractCFPListener;

import dip.gui.order.GUIOrder;
import dip.order.Orderable;

import dip.misc.Log;

import dip.world.Province;
import dip.world.TurnState;
import dip.world.Location;
import dip.world.Power;
import dip.world.Unit;

import java.util.LinkedList;
import java.util.Iterator;

import org.w3c.dom.svg.SVGDocument;

import org.apache.batik.swing.JSVGCanvas;
import org.apache.batik.util.RunnableQueue;
import org.apache.batik.bridge.UpdateManagerListener;
import org.apache.batik.bridge.UpdateManagerEvent;

/**
*	Base class for the new MapRenderer.
*	<p>
*	Implementation notes: remember to <b>always</b> synchronize around the 
*	<code>renderQueue</code> object.
*
*/
public abstract class MapRenderer2 
{
	
	// constant key values for getRenderSetting()
	//
	/** Key for getSettings(): */
	public static final String KEY_LABELS = "KEY_LABELS";
	/** Key for getSettings(): */
	public static final String KEY_SHOW_SUPPLY_CENTERS = "KEY_SHOW_SUPPLY_CENTERS";
	/** Key for getSettings(): */
	public static final String KEY_SHOW_UNITS = "KEY_SHOW_UNITS";
	/** Key for getSettings(): */
	public static final String KEY_SHOW_DISLODGED_UNITS = "KEY_SHOW_DISLODGED_UNITS";
	/** Key for getSettings(): */
	public static final String KEY_SHOW_UNORDERED = "KEY_SHOW_UNORDERED";
	/** Key for getSettings(): */
	public static final String KEY_SHOW_ORDERS_FOR_POWERS = "KEY_SHOW_ORDERS_FOR_POWERS";
	/** Key for getSettings(): */
	public static final String KEY_INFLUENCE_MODE = "KEY_INFLUENCE_MODE";
	/** Key for getSettings(): */
	public static final String KEY_SHOW_MAP = "KEY_SHOW_MAP";
	
	
	// constant return values for getRenderSetting()
	//
	/** Value returned from getSettings(): */
	public static final String VALUE_LABELS_NONE = "VALUE_LABELS_NONE";
	/** Value returned from getSettings(): */
	public static final String VALUE_LABELS_FULL = "VALUE_LABELS_FULL";
	/** Value returned from getSettings(): */
	public static final String VALUE_LABELS_BRIEF = "VALUE_LABELS_BRIEF";
	
	
	// instance variables
	//
	private boolean isReady = false;		// internal flag indicating if turnstate has been set
	private LinkedList tempQueue = null;
	protected final MapPanel mapPanel;
	protected CFPropertyListener propListener = null;
	protected final JSVGCanvas svgCanvas;
	protected final SVGDocument doc;
	
	
	/**
	*	Default Constructor
	*	JSVGCanvas and SVGDocument of MapPanel <b>must not be null</b>
	*/
	public MapRenderer2(MapPanel mp)
	throws MapException
	{
		Log.printTimed(mp.startTime, "MR2 constructor start");
		mapPanel = mp;
		
		svgCanvas = mapPanel.getJSVGCanvas();
		doc = svgCanvas.getSVGDocument();
		
		propListener = new CFPropertyListener();
		mapPanel.getClientFrame().addPropertyChangeListener(propListener);
		
		tempQueue = new LinkedList();
		
		Log.printTimed(mp.startTime, "MR2 constructor end");
	}// MapRenderer()
	
	/** Convenience method */
	public final ClientFrame getClientFrame()
	{
		return mapPanel.getClientFrame();
	}// getClientFrame()
	
	/** 
	*	Gets the Runnable Queue for the canvas. 
	*	Return null if this cannot be done (e.g., we are exiting).
	*/
	public final RunnableQueue getRunnableQueue()
	{
		if(svgCanvas != null)
		{
			if(svgCanvas.getUpdateManager() != null)
			{
				return svgCanvas.getUpdateManager().getUpdateRunnableQueue();
			}
		}
		
		Log.println("MR2::getRunnableQueue(): RQ null ... exiting?");
		return null;
	}// getRunnableQueue()
	
	/**
	*	Get a setting (as defined by the KEY_ constants)
	*/
	public abstract Object getRenderSetting(final Object key);
	
	/**
	*	Execute a RenderCommand. No commands are executed until the TurnState
	*	has been set.
	*/
	public synchronized void execRenderCommand(RenderCommand rc)
	{
		if(rc instanceof RenderCommandFactory.RCSetTurnstate)
		{
			// focus
			mapPanel.requestFocusInWindow();
			
			// dequeue pending events
			isReady = true;
			Log.println("MR2:execRenderCommand(): first RCSetTurnstate: ", rc);
			clearAndExecute(rc, null);
			
			// dequeue pending events, if any
			if(!tempQueue.isEmpty())
			{
				Log.println("MR2::execRenderCommand(): removing pending events from queue. size: ", 
					String.valueOf(tempQueue.size()));
					
				RunnableQueue rq = getRunnableQueue();
				if(rq != null)
				{
					Iterator iter = tempQueue.iterator();
					while(iter.hasNext())
					{
						rq.invokeLater( ((RenderCommand) iter.next()) );
					}
					
					tempQueue.clear();
				}
			}
		}
		else if(isReady)
		{
			// a RCSetTurnstate() has been issued. We can accept render events.
			// if we have queued events, add them.
			Log.println("MR2::execRenderCommand(): adding to RunnableQueue: ", rc);
			RunnableQueue rq = getRunnableQueue();
			if(rq != null)
			{
				rq.invokeLater(rc);
			}
		}
		else
		{
			// we are not yet ready -- add the rendering events to a temporary queue.
			Log.println("MR2::execRenderCommand(): adding to tempQueue: ", rc);
			tempQueue.add(rc);
		}
	}// execRenderCommand()
	
	
	/** Clean up any resources used by the MapRenderer. */
	public void close() 
	{
		isReady = false;
		mapPanel.getClientFrame().removePropertyChangeListener(propListener);
	}// close()
	
	
	/** Get the RenderCommandFactory */
	public abstract RenderCommandFactory getRenderCommandFactory();
	
	
	/** Get the MapMetadata object */
	public abstract MapMetadata getMapMetadata();
	
	/** Get the Symbol Name for the given unit type */
	public abstract String getSymbolName(Unit.Type unitType);
	
	/** Get a location that corresponds to an ID */
	public abstract Location getLocation(String id);
	
	/** Called when an order has been deleted from the order list */
	protected abstract void orderDeleted(GUIOrder order);
	
	/** Called when an order has been added to the order list */
	protected abstract void orderCreated(GUIOrder order);
	
	/** Called when multiple orders have been deleted from the order list */
	protected abstract void multipleOrdersDeleted(GUIOrder[] orders);
	
	/** Called when multiple orders have been added from the order list */
	protected abstract void multipleOrdersCreated(GUIOrder[] orders);
	
	/** Called when the displayable powers have changed */
	protected abstract void displayablePowersChanged(Power[] powers);
	
	
	
	/** 
	*	Prevents any enqueued RenderCommands from being executed.
	*	If a command is currently executing, it is not affected.
	*	Adds the given commands (or none, if null) to the queue.
	*/
	protected void clearAndExecute(RenderCommand rc1, RenderCommand rc2)
	{
		Log.println("MR2::clearAndExecute()");
		
		RunnableQueue rq = getRunnableQueue();
		if(rq != null)
		{
			synchronized(rq.getIteratorLock())
			{
				// kill our pending render events
				Iterator iter = rq.iterator();
				while(iter.hasNext())
				{
					Object obj = iter.next();
					if(obj instanceof RenderCommand)
					{
						Log.println("   killing: ", obj);
						((RenderCommand) obj).die();
					}
				}
				
				// add our new render commands
				// NOTE: not sure if this should be in synchronized block.
				if(rc1 != null)
				{
					rq.invokeLater(rc1);
				}
				
				if(rc2 != null)
				{
					rq.invokeLater(rc2);
				}
			}
		}
	}// clearAndExecute()
	
	
	
	/** 
	*	Listener class for order updates and TurnState changes
	*
	*/
	private class CFPropertyListener extends AbstractCFPListener 
	{
		public void actionOrderCreated(Orderable order)		
		{
			orderCreated((GUIOrder) order);
		}
		
		public void actionOrderDeleted(Orderable order)		
		{
			orderDeleted((GUIOrder) order);
		}
		
		public void actionOrdersCreated(Orderable[] orders)	
		{
			GUIOrder[] guiOrders = new GUIOrder[orders.length];
			for(int i=0; i<guiOrders.length; i++)
			{
				guiOrders[i] = (GUIOrder) orders[i];
			}
			
			multipleOrdersCreated(guiOrders);
		}
		
		public void actionOrdersDeleted(Orderable[] orders)	
		{
			GUIOrder[] guiOrders = new GUIOrder[orders.length];
			for(int i=0; i<guiOrders.length; i++)
			{
				guiOrders[i] = (GUIOrder) orders[i];
			}
			
			multipleOrdersDeleted(guiOrders);
		}
		
		public void actionDisplayablePowersChanged(Power[] oldPowers, Power[] newPowers)
		{
			displayablePowersChanged(newPowers);
		}
		
		public void actionTurnstateChanged(TurnState ts)
		{
			// OPTIMIZATION:
			// any pending queued events may be deleted, because 
			// we are changing the turnstate and doing a complete re-render.
			//
			RenderCommand rc1 = 
					getRenderCommandFactory().createRCSetTurnstate(
						MapRenderer2.this, ts);
					
			RenderCommand rc2 = 
					getRenderCommandFactory().createRCRenderAll(
						MapRenderer2.this);
				
			clearAndExecute(rc1, rc2);
		}
		
	}// inner class CFPropertyListener
	
	
	/**
	*	Returns a Label-Level constant for the given label level.
	*	Does a case-insensitive compare. Instance equality is preserved.
	* 	Returns the given default if parsing fails.
	*/
	public static String parseLabelValue(String in, String defaultValue)
	{
		if(defaultValue != VALUE_LABELS_NONE 
			&& defaultValue != VALUE_LABELS_FULL 
			&& defaultValue != VALUE_LABELS_BRIEF
			&& defaultValue != null)
		{
			throw new IllegalArgumentException();
		}
		
		if(VALUE_LABELS_NONE.equalsIgnoreCase(in))
		{
			return VALUE_LABELS_NONE;
		}
		else if(VALUE_LABELS_BRIEF.equalsIgnoreCase(in))
		{
			return VALUE_LABELS_BRIEF;
		}
		else if(VALUE_LABELS_FULL.equalsIgnoreCase(in))
		{
			return VALUE_LABELS_FULL;
		}
		
		return defaultValue;
	}// parseLabelValue()
	
	
	
}// abstract class MapRenderer2
