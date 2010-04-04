//
//  @(#)MapPicker.java		9/2002
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
package dip.gui.dialog;

import dip.misc.Utils;
import dip.gui.ClientFrame;
import dip.world.World;
import dip.world.variant.VariantManager;
import dip.world.variant.data.MapGraphic;
import dip.world.variant.data.Variant;
import dip.world.variant.data.SymbolPack;
import dip.gui.dialog.newgame.NGDMapAndUnits;

import cz.autel.dmi.*;		// HIGLayout

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.*;
import java.net.URL;
import java.net.URI;
import javax.swing.border.*;
import javax.swing.text.*;
import javax.swing.text.html.*;


/**
*	This redisplays the Map and Symbols tab from the New Game Dialog,
*	allowing the user to change Map and Symbol parameters for a game
*	that is in-progress.
*/
public class MapPicker extends HeaderDialog
{
	
	// i18n constants
	private static final String TITLE = "MapPicker.title";
	private static final String HEADER_LOCATION = "MapPicker.location.header";
	
	// instance variables
	private ClientFrame		clientFrame;
	private NGDMapAndUnits	mauSelector = null;
	private World			world = null;
	private final String	originalMapName;
	private final String 	originalSymbolPackName;
	
	/**
	*	Displays the MapPicker dialog.
	*	<p>
	*	Returns a boolean indicating if the displayed map was 
	*	changed.
	*/
	public static boolean displayDialog(ClientFrame cf, World world)
	{
		if(cf == null || world == null)
		{
			throw new IllegalArgumentException();
		}
		
		MapPicker mp = new MapPicker(cf, world);
		mp.pack();		
		mp.setSize(Utils.getScreenSize(0.60f, 0.75f));
		Utils.centerInScreen(mp);
		mp.setVisible(true);		
		return mp.getAndChangeMap();
	}// displayDialog()
	
	
	/** Create a MapPicker dialog */
	private MapPicker(ClientFrame clientFrame, World world)
	{
		super(clientFrame, Utils.getLocalString(TITLE), true);
		this.clientFrame = clientFrame;
		this.world = world;
		
		World.VariantInfo vi = world.getVariantInfo();
		Variant variant = VariantManager.getVariant(vi.getVariantName(), vi.getVariantVersion());
		mauSelector = new NGDMapAndUnits();
		mauSelector.variantChanged(variant);
		
		// find which map graphic is selected
		originalMapName = vi.getMapName();
		originalSymbolPackName = vi.getSymbolPackName();
		mauSelector.setSelectedMap(originalMapName);
		mauSelector.setSelectedSymbolPack(originalSymbolPackName);
		
		// complete the dialog setup
		setHeaderText( Utils.getText(Utils.getLocalString(HEADER_LOCATION)) );
		setContentPane(mauSelector);
		setSeparatorVisible(true, 10, 0);
		addTwoButtons( makeCancelButton(), makeOKButton(), false, true );
	}// SelectPhaseDialog()
	
	
	/** Returns whether the Map has been changed, and sets world object map URI / redraws. */
	private boolean getAndChangeMap()
	{
		if(getReturnedActionCommand().equals(ACTION_OK))
		{
			final MapGraphic mg = mauSelector.getSelectedMap();
			final SymbolPack sp = mauSelector.getSelectedSymbolPack();
			
			// if graphic OR symbols have changed, reload map.
			//
			if( !mg.getName().equalsIgnoreCase(originalMapName)
				|| !sp.getName().equalsIgnoreCase(originalSymbolPackName) )
			{
				// set the new URI in World object.
				World.VariantInfo vi = world.getVariantInfo();
				vi.setMapName(mg.getName());
				
				vi.setSymbolPackName(sp.getName());
				vi.setSymbolPackVersion(sp.getVersion());
				
				// re-render the map
				clientFrame.getMapPanel().reloadMap();
				
				// we've created a change that requires a save to persist.
				clientFrame.fireStateModified();
				return true;
			}
		}
		
		return false;
	}// getAndChangeMap()
	
	
}// class MapPicker


