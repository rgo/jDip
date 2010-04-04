//
//  @(#)RenderCommandFactory.java		5/2003
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

import dip.world.TurnState;
import dip.world.Power;
import dip.world.Province;

public abstract class RenderCommandFactory
{
	
	/** */
	public abstract RCRenderAll createRCRenderAll(MapRenderer2 mr);
	/** */
	public abstract RCSetTurnstate createRCSetTurnstate(MapRenderer2 mr, TurnState ts);
	/** */
	public abstract RCRenderProvince createRCRenderProvince(MapRenderer2 mr, Province province);
	/** */
	public abstract RCSetLabel createRCSetLabel(MapRenderer2 mr, Object labelValue);
	/** */
	public abstract RCSetDisplaySC createRCSetDisplaySC(MapRenderer2 mr, boolean value);
	/** */
	public abstract RCSetDisplayUnits createRCSetDisplayUnits(MapRenderer2 mr, boolean value);
	/** */
	public abstract RCSetDisplayDislodgedUnits createRCSetDisplayDislodgedUnits(MapRenderer2 mr, boolean value);
	/** */
	public abstract RCSetDisplayUnordered createRCSetDisplayUnordered(MapRenderer2 mr, boolean value);
	/** */
	public abstract RCSetInfluenceMode createRCSetInfluenceMode(MapRenderer2 mr, boolean value);
	/** */
	public abstract RCSetPowerOrdersDisplayed createRCSetPowerOrdersDisplayed(MapRenderer2 mr, Power[] displayedPowers);
	/** */
	public abstract RCShowMap createRCShowMap(MapRenderer2 mr, boolean value);
	
	
	
	/**
	*	All Render updates occur via RenderCommands. 
	*	
	*/
	public static abstract class RenderCommand implements Runnable
	{
		private boolean alive = true;
		protected final MapRenderer2 mr;
		
		/** Constructor */
		public RenderCommand(MapRenderer2 mr) { this.mr = mr; }
		
		/** Do the work. Does nothing by default. Must be subclassed. */
		public abstract void execute();
		
		/** 
		*	Runnable interface. Checks if this object is alive 
		*	and if so, calls the execute() method. Otherwise,
		*	returns immediately. Subclasses should subclass 
		*	execute instead of this method..
		*/
		public final void run()
		{
			if(alive)
			{
				// before executing, lock on turnstate, since
				// we may modify it or use modifications
				//synchronized(mr.getClientFrame().getLock())	// DISABLED
				{
					execute();
				}
			}
		}// run()
		
		/** 
		*	Irreversibly prevents the execute() method from 
		*	being executed by run().
		*/
		public void die()
		{
			alive = false;
		}// die()
		
		/** For debugging */
		public String toString()
		{
			StringBuffer sb = new StringBuffer();
			sb.append("[");
			sb.append(this.getClass().getName());
			return sb.toString();
		}
	}// abstract nested class RenderCommand
	
	
	/**
	*	All Render updates occur via RenderCommands. 
	*	
	*/
	protected static abstract class BooleanRenderCommand extends RenderCommand
	{
		protected final boolean value; 
		
		/** Constructor */
		public BooleanRenderCommand(MapRenderer2 mr, boolean value) 
		{ 
			super(mr);
			this.value = value;
		}// BooleanRenderCommand()
		
	}// abstract nested class RenderCommand
	
	
	
	/** Render the entire map */
	protected static abstract class RCRenderAll extends RenderCommand
	{
		/** Constructor */
		public RCRenderAll(MapRenderer2 mr)
		{
			super(mr);
		}// RCRenderAll()
	}// abstract nested class RCRenderAll
	
	
	/** Show/hide the map (as opposed to background) */
	protected static abstract class RCShowMap extends BooleanRenderCommand
	{
		/** Constructor */
		public RCShowMap(MapRenderer2 mr, boolean value)
		{
			super(mr, value);
		}// RCShowMap()
	}// abstract nested class RCShowMap
	
	
	/** Render the entire map */
	protected static abstract class RCSetTurnstate extends RenderCommand
	{
		protected final TurnState ts;
		
		/** Constructor */
		public RCSetTurnstate(MapRenderer2 mr, TurnState ts)
		{
			super(mr);
			if(ts == null)
			{
				throw new IllegalArgumentException("null TurnState");
			}
			
			this.ts = ts;
		}// RCSetTurnstate()
		
	}// abstract nested class RCSetTurnstate
	
	
	/** Render a particular Province */
	protected static abstract class RCRenderProvince extends RenderCommand
	{
		protected final Province province;
		
		/** Constructor */
		public RCRenderProvince(MapRenderer2 mr, Province province)
		{
			super(mr);
			
			if(province == null)
			{
				throw new IllegalArgumentException("null province");
			}
			
			this.province = province;
		}// RCRenderProvince()
	}// abstract nested class RCRenderProvince
	
	/** Change how labels are displayed */
	protected static abstract class RCSetLabel extends RenderCommand
	{
		protected final Object labelValue;
		
		/** Constructor */
		public RCSetLabel(MapRenderer2 mr, Object labelValue)
		{
			super(mr);
			if( labelValue != MapRenderer2.VALUE_LABELS_NONE 
				&& labelValue != MapRenderer2.VALUE_LABELS_FULL 
				&& labelValue != MapRenderer2.VALUE_LABELS_BRIEF )
			{
				throw new IllegalArgumentException("bad labelValue");
			}
			
			this.labelValue = labelValue;
		}// RCSetLabel()
	}// abstract nested class RCSetLabel
	
	
	
	/** Sets whether Supply Centers are displayed or not. */
	protected static abstract class RCSetDisplaySC extends BooleanRenderCommand
	{
		/** Constructor */
		public RCSetDisplaySC(MapRenderer2 mr, boolean value)
		{
			super(mr, value);
		}// RCSetDisplaySC()
	}// abstract nested class RCSetDisplaySC
	
	/** Sets if Units are displayed or not. */
	protected static abstract class RCSetDisplayUnits extends BooleanRenderCommand
	{
		/** Constructor */
		public RCSetDisplayUnits(MapRenderer2 mr, boolean value)
		{
			super(mr, value);
		}// RCSetDisplaySC()
	}// abstract nested class RCSetDisplayUnits
	
	/** Sets if Dislodged Units are displayed or not. */
	protected static abstract class RCSetDisplayDislodgedUnits extends BooleanRenderCommand
	{
		/** Constructor */
		public RCSetDisplayDislodgedUnits(MapRenderer2 mr, boolean value)
		{
			super(mr, value);
		}// RCSetDisplaySC()
	}// abstract nested class RCSetDisplayDislodgedUnits
	
	/** Sets if we highlight units without orders. */
	protected static abstract class RCSetDisplayUnordered extends BooleanRenderCommand
	{
		/** Constructor */
		public RCSetDisplayUnordered(MapRenderer2 mr, boolean value)
		{
			super(mr, value);
		}// RCSetDisplaySC()
	}// abstract nested class RCSetDisplayUnordered
	
	/** Sets if we are in Influence mode or not. */
	protected static abstract class RCSetInfluenceMode extends BooleanRenderCommand
	{
		/** Constructor */
		public RCSetInfluenceMode(MapRenderer2 mr, boolean value)
		{
			super(mr, value);
		}// RCSetDisplaySC()
	}// abstract nested class RCSetInfluenceMode
	
	/** Sets which Powers have their orders displayed. */
	protected static abstract class RCSetPowerOrdersDisplayed extends RenderCommand
	{
		protected final Power[] displayedPowers;
		
		/** Constructor */
		public RCSetPowerOrdersDisplayed(MapRenderer2 mr, Power[] displayedPowers)
		{
			super(mr);
			if(displayedPowers == null)
			{
				throw new IllegalArgumentException("powers null");
			}
			
			this.displayedPowers = displayedPowers;
		}// RCSetDisplaySC()
	}// abstract nested class RCSetPowerOrdersDisplayed	
}// abstract class RenderCommandFactory


