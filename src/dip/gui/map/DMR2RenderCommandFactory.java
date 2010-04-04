//
//  @(#)DMR2RenderCommandFactory.java		5/2003
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
 
import dip.gui.map.RenderCommandFactory.*;

import dip.misc.Log;

import dip.world.TurnState;
import dip.world.Power;
import dip.world.Province;

import org.w3c.dom.svg.SVGElement;

public class DMR2RenderCommandFactory extends RenderCommandFactory
{
	
	/** */
	public RenderCommandFactory.RCRenderAll createRCRenderAll(MapRenderer2 mr)
	{
		return new RCRenderAll(mr);
	}// RCRenderAll()
	
	/** */
	public RenderCommandFactory.RCSetTurnstate createRCSetTurnstate(MapRenderer2 mr, TurnState ts)
	{
		return new DMR2RenderCommandFactory.RCSetTurnstate(mr, ts);
	}// RCSetTurnstate()
	
	/** 
	*	Updates the province, based on the current Position object 
	*	<p>
	*	This typically will change units, dislodged units, and supply center 
	*	information for the given Province. It can add or remove items. 
	*	<p>
	*	Essentially, it synchronizes the visual state with the current state
	*	of the Province.
	*/
	public RenderCommandFactory.RCRenderProvince createRCRenderProvince(MapRenderer2 mr, Province province)
	{
		return new RCRenderProvince(mr, province);
	}// RCRenderProvince()
	
	/** */
	public RenderCommandFactory.RCSetLabel createRCSetLabel(MapRenderer2 mr, Object labelValue)
	{
		return new RCSetLabel(mr, labelValue);
	}// RCSetLabel()
	
	/** */
	public RenderCommandFactory.RCSetDisplaySC createRCSetDisplaySC(MapRenderer2 mr, boolean value)
	{
		return new RCSetDisplaySC(mr, value);
	}// RCSetDisplaySC()
	
	/** */
	public RenderCommandFactory.RCSetDisplayUnits createRCSetDisplayUnits(MapRenderer2 mr, boolean value)
	{
		return new RCSetDisplayUnits(mr, value);
	}// RCSetDisplayUnits()
	
	/** */
	public RenderCommandFactory.RCSetDisplayDislodgedUnits createRCSetDisplayDislodgedUnits(MapRenderer2 mr, boolean value)
	{
		return new RCSetDisplayDislodgedUnits(mr, value);
	}// RCSetDisplayDislodgedUnits()
	
	/** */
	public RenderCommandFactory.RCSetDisplayUnordered createRCSetDisplayUnordered(MapRenderer2 mr, boolean value)
	{
		return new RCSetDisplayUnordered(mr, value);
	}// RCSetDisplayUnordered()
	
	/** */
	public RenderCommandFactory.RCSetInfluenceMode createRCSetInfluenceMode(MapRenderer2 mr, boolean value)
	{
		return new RCSetInfluenceMode(mr, value);
	}// RCSetInfluenceMode()
	
	/** */
	public RenderCommandFactory.RCSetPowerOrdersDisplayed createRCSetPowerOrdersDisplayed(MapRenderer2 mr, Power[] displayedPowers)
	{
		return new RCSetPowerOrdersDisplayed(mr, displayedPowers);
	}// RCSetPowerOrdersDisplayed()
	
	/** */
	public RenderCommandFactory.RCShowMap createRCShowMap(MapRenderer2 mr, boolean value)
	{
		return new RCShowMap(mr, value);
	}// RCSetPowerOrdersDisplayed()
	
	
	
	/** Force re-rendering of all orders (erase then update). The Turnstate better have been set. */
	public RenderCommand createRCRenderAllForced(MapRenderer2 mr)
	{
		return new RenderCommand(mr)
		{
			public void execute()
			{
				Log.println("DMR2RCF::createRCRenderAllForced()");
				DefaultMapRenderer2 dmr2 = (DefaultMapRenderer2) mr;
				
				// destroy the existing orders
				dmr2.unsyncDestroyAllOrders();
				
				// for each province, update the province and orders.
				dmr2.unsyncRecreateAllOrders();
				dmr2.unsyncUpdateAllProvinces();
				Log.println("  DMR2RCF::createRCRenderAllForced() complete.");
			}// execute()
		};
	}// RCRenderProvince()
	
	/** Force-update the unit or dislodged unit information */
	public RenderCommand createRCRenderProvinceForced(MapRenderer2 mr, final Province province)
	{
		return new RenderCommand(mr)
		{
			public void execute()
			{
				Log.println("DMR2RCF::createRCRenderProvinceForced(): ", province);
				DefaultMapRenderer2 dmr2 = (DefaultMapRenderer2) this.mr;
				dmr2.unsyncUpdateProvince(province, true);
			}// execute()
		};
	}// RCRenderProvince()
	
	
	/** Update a Supply Center (position) */
	public RenderCommand createRCUpdateSC(MapRenderer2 mr, final Province province)
	{
		return new RenderCommand(mr)
		{
			public void execute()
			{
				Log.println("DMR2RCF::createRCUpdateSC(): ", province);
				DefaultMapRenderer2 dmr2 = (DefaultMapRenderer2) this.mr;
				dmr2.unsyncUpdateSC(province);
			}// execute()
		};
	}// RCRenderProvince()
	
	
	
	
	/** Render (Refresh) the entire map */
	protected static class RCRenderAll extends RenderCommandFactory.RCRenderAll
	{
		public RCRenderAll(MapRenderer2 dmr2) { super(dmr2); }
		
		public void execute()
		{
			Log.println("DMR2RCF::RCRenderAll()");
			DefaultMapRenderer2 dmr2 = (DefaultMapRenderer2) mr;
			
			// for each province, update the province and orders.
			// TODO: consider destroying all orders before rendering
			// (slower, but less error-prone??) 
			// see RCRenderAllForced for details
			dmr2.unsyncRecreateAllOrders();
			dmr2.unsyncUpdateAllProvinces();
			Log.println("  DMR2RCF::RCRenderAll() complete.");
		}// execute()
	}// nested class RCRenderAll
	
	
	/** Render the entire map */
	protected static class RCSetTurnstate extends RenderCommandFactory.RCSetTurnstate
	{
		public RCSetTurnstate(MapRenderer2 dmr2, TurnState ts) { super(dmr2, ts); }
		
		public void execute()
		{
			Log.println("DMR2RCF::RCSetTurnstate()");
			DefaultMapRenderer2 dmr2 = (DefaultMapRenderer2) mr;
			
			// set the TurnState
			dmr2.setTurnState(ts);
			
			// remove all orders from the DOM
			dmr2.unsyncRemoveAllOrdersFromDOM();
		}// execute()
	}// nested class RCSetTurnstate
	
	
	/** Render a particular Province */
	protected static class RCRenderProvince extends RenderCommandFactory.RCRenderProvince
	{
		public RCRenderProvince(MapRenderer2 dmr2, Province province) { super(dmr2, province); }
		
		public void execute()
		{
			Log.println("DMR2RCF::RCRenderProvince(): ", province);
			DefaultMapRenderer2 dmr2 = (DefaultMapRenderer2) mr;
			dmr2.unsyncUpdateProvince(province);
		}// execute()
	}// nested class RCRenderProvince
	
	/** Change how labels are displayed */
	protected static class RCSetLabel extends RenderCommandFactory.RCSetLabel
	{
		public RCSetLabel(MapRenderer2 dmr2, Object value) { super(dmr2, value); }
		
		public void execute()
		{
			Log.println("DMR2RCF::RCSetLabel(): ", labelValue);
			DefaultMapRenderer2 dmr2 = (DefaultMapRenderer2) mr;
			
			Object newLabelValue = labelValue;
			final MapMetadata mmd = dmr2.getMapMetadata();
			
			// not all maps have all label settings. All will have 
			// NONE, but not all will have BRIEF and FULL. So, degrade
			// gracefully based upon map metadata.
			// full->brief->none is how we degrade.
			//
			if( newLabelValue == MapRenderer2.VALUE_LABELS_FULL
				&& !mmd.getDisplayParamBoolean(MapMetadata.ATT_LABELS_FULL, false) )
			{
				newLabelValue = MapRenderer2.VALUE_LABELS_BRIEF;
				Log.println("  degrading label to: ", newLabelValue);
			}
			
			if( newLabelValue == MapRenderer2.VALUE_LABELS_BRIEF
				&& !mmd.getDisplayParamBoolean(MapMetadata.ATT_LABELS_BRIEF, false) )
			{
				newLabelValue = MapRenderer2.VALUE_LABELS_NONE;
				Log.println("  degrading label to: ", newLabelValue);
			}
			
			final SVGElement elBrief = (SVGElement) dmr2.layerMap.get(DefaultMapRenderer2.LABEL_LAYER_BRIEF);
			final SVGElement elFull = (SVGElement) dmr2.layerMap.get(DefaultMapRenderer2.LABEL_LAYER_FULL);
			
			if(newLabelValue == MapRenderer2.VALUE_LABELS_NONE)
			{
				dmr2.setElementVisibility(elBrief, false);
				dmr2.setElementVisibility(elFull, false);
			}
			else if(newLabelValue == MapRenderer2.VALUE_LABELS_BRIEF)
			{
				dmr2.setElementVisibility(elBrief, true);
				dmr2.setElementVisibility(elFull, false);
			}
			else if(newLabelValue == MapRenderer2.VALUE_LABELS_FULL)
			{
				dmr2.setElementVisibility(elBrief, false);
				dmr2.setElementVisibility(elFull, true);
			}
			
			dmr2.setRenderSetting(MapRenderer2.KEY_LABELS, labelValue);
		}// execute()
	}// nested class RCSetLabel
	
	
	
	
	/** */
	protected static class RCSetDisplaySC extends RenderCommandFactory.RCSetDisplaySC
	{
		public RCSetDisplaySC(MapRenderer2 dmr2, boolean value) { super(dmr2, value); }
		
		public void execute()
		{
			Log.println("DMR2RCF::RCSetDisplaySC()");
			DefaultMapRenderer2 dmr2 = (DefaultMapRenderer2) mr;
			dmr2.setRenderSetting(MapRenderer2.KEY_SHOW_SUPPLY_CENTERS, Boolean.valueOf(value));
			dmr2.setElementVisibility((SVGElement) dmr2.layerMap.get(DefaultMapRenderer2.LAYER_SC), value);
			dmr2.unsyncUpdateAllProvinces();	// update provinces, but not orders
		}// execute()
	}// nested class RCSetDisplaySC
	
	/** */
	protected static class RCSetDisplayUnits extends RenderCommandFactory.RCSetDisplayUnits
	{
		public RCSetDisplayUnits(MapRenderer2 dmr2, boolean value) { super(dmr2, value); }
		
		public void execute()
		{
			Log.println("DMR2RCF::RCSetDisplayUnits()");
			DefaultMapRenderer2 dmr2 = (DefaultMapRenderer2) mr;
			dmr2.setRenderSetting(MapRenderer2.KEY_SHOW_UNITS, Boolean.valueOf(value));
			dmr2.setElementVisibility((SVGElement) dmr2.layerMap.get(DefaultMapRenderer2.LAYER_UNITS), value);
		}// execute()
	}// nested class RCSetDisplayUnits
	
	/** */
	protected static class RCSetDisplayDislodgedUnits extends RenderCommandFactory.RCSetDisplayDislodgedUnits
	{
		public RCSetDisplayDislodgedUnits(MapRenderer2 dmr2, boolean value) { super(dmr2, value); }
		
		public void execute()
		{
			Log.println("DMR2RCF::RCSetDisplayDislodgedUnits()");
			DefaultMapRenderer2 dmr2 = (DefaultMapRenderer2) mr;
			dmr2.setRenderSetting(MapRenderer2.KEY_SHOW_DISLODGED_UNITS, Boolean.valueOf(value));
			dmr2.setElementVisibility((SVGElement) dmr2.layerMap.get(DefaultMapRenderer2.LAYER_DISLODGED_UNITS), value);
		}// execute()
	}// nested class RCSetDisplayDislodgedUnits
	
	/** */
	protected static class RCSetDisplayUnordered extends RenderCommandFactory.RCSetDisplayUnordered
	{
		public RCSetDisplayUnordered(MapRenderer2 dmr2, boolean value) { super(dmr2, value); }
		
		public void execute()
		{
			Log.println("DMR2RCF::RCSetDisplayUnordered()");
			DefaultMapRenderer2 dmr2 = (DefaultMapRenderer2) mr;
			dmr2.setRenderSetting(MapRenderer2.KEY_SHOW_UNORDERED, Boolean.valueOf(value));
			dmr2.unsyncUpdateAllProvinces();	// update provinces, but not orders
		}// execute()
	}// nested class RCSetDisplayUnordered
	
	/** */
	protected static class RCSetInfluenceMode extends RenderCommandFactory.RCSetInfluenceMode
	{
		public RCSetInfluenceMode(MapRenderer2 dmr2, boolean value) { super(dmr2, value); }
		
		public void execute()
		{
			Log.println("DMR2RCF::RCSetInfluenceMode()");
			DefaultMapRenderer2 dmr2 = (DefaultMapRenderer2) mr;
			dmr2.unsyncSetInfluenceMode(value);
		}// execute()
	}// nested class RCSetInfluenceMode
	
	/**  */
	protected static class RCShowMap extends RenderCommandFactory.RCShowMap
	{
		public RCShowMap(MapRenderer2 dmr2, boolean value) { super(dmr2, value); }
		
		public void execute()
		{
			Log.println("DMR2RCF::RCShowMap()");
			DefaultMapRenderer2 dmr2 = (DefaultMapRenderer2) mr;
			dmr2.setRenderSetting(MapRenderer2.KEY_SHOW_MAP, Boolean.valueOf(value));
			SVGElement mapLayer = (SVGElement) dmr2.layerMap.get(DefaultMapRenderer2.LAYER_MAP);
			assert(mapLayer != null);
			dmr2.setElementVisibility(mapLayer, value);
		}// execute()
	}// nested class RCShowMap
	
	
	
	/** */
	protected static class RCSetPowerOrdersDisplayed extends RenderCommandFactory.RCSetPowerOrdersDisplayed
	{
		public RCSetPowerOrdersDisplayed(MapRenderer2 dmr2, Power[] powers) { super(dmr2, powers); }
		
		public void execute()
		{
			Log.println("DMR2RCF::RCSetPowerOrdersDisplayed()");
			DefaultMapRenderer2 dmr2 = (DefaultMapRenderer2) mr;
			dmr2.setRenderSetting(MapRenderer2.KEY_SHOW_ORDERS_FOR_POWERS, displayedPowers);
			dmr2.unsyncSetVisiblePowers();
		}// execute()
	}// nested class RCSetPowerOrdersDisplayed	
	
	
	
}// class RenderCommandFactory

	
