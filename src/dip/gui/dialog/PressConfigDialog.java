//
//  @(#)PressDialog.java	9/2003
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
package dip.gui.dialog;

import dip.world.Phase;

import dip.net.message.PressConfiguration;
import dip.net.message.MID;

import cz.autel.dmi.HIGConstraints;
import cz.autel.dmi.HIGLayout;

import dip.gui.*;
import dip.misc.Utils;
import dip.misc.Help;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;
import javax.swing.border.*;

import java.util.*;
import java.text.*;


public class PressConfigDialog
{
	// i18n constants: locations for HTML text & templates
	private static final String HEADER_LOCATION 	= "pcd.header.location";
	private static final String TEMPLATE_LOCATION 	= "pcd.template.location";
	
	// i18n constants: press types, descriptions, and misc.
	private static final String TITLE 			= "pcd.dialog.title";
	
	
	/** Display the dialog */
	public static void displayDialog(JFrame parent, PressConfiguration pc)
	{
		TextViewer tv = new TextViewer(parent);
		tv.setModal(false);
		tv.setTitle(Utils.getLocalString(TITLE));
		tv.setContentType("text/html");
		tv.setEditable(false);
		tv.setHighlightable(true);
		tv.setText( makeText(pc) );
		tv.addSingleButton( tv.makeOKButton() );
		tv.setHeaderText( Utils.getText(Utils.getLocalString(HEADER_LOCATION)) );
		tv.displayDialog();
	}// displayDialog()
	
	
	/** Create the HTML that displays the press configuration options */
	private static String makeText(PressConfiguration pc)
	{
		// insert arguments into template. Some args require 
		// some pre-processing
		String templateText = Utils.getText(Utils.getLocalString(TEMPLATE_LOCATION));
		
		
		// get template objects
		Object[] templateData = new Object[]
		{
			pc.getPlayerPTName(),		// {0} Player press type.
			pc.getPlayerPTDesc(),		// {1} Player press type description
			pc.getObserverPTName(),		// {2} Observer press type
			pc.getOBserverPTDesc(),		// {3} Observer press type description
			pc.getProhibitedTimes(),	// {4} text describing when press is prohibited
			pc.getMasterName()			// {5} Master nick/name, or suitable 'none' indicator
		};
		
		// format into template
		return Utils.format(templateText, templateData);
	}// makeText()
	
	
}// class PressConfigDialog
