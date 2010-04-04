//
//  @(#)Help.java	2/2003
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
package dip.misc;

import dip.gui.dialog.ErrorDialog;
import dip.gui.swing.SwingWorker;

import java.awt.Frame;
import java.awt.Component;
import java.net.URL;

import javax.swing.JRootPane;
import javax.swing.JDialog;
import javax.swing.AbstractButton;

import javax.help.HelpBroker;
import javax.help.HelpSet;
import javax.help.CSH;
import javax.help.DefaultHelpBroker;
import javax.help.WindowPresentation;

/**
*	
*	Encapsulates JavaHelp for the utmost in simplicity of adding
*	help to GUI elements.
*	
*/
public class Help
{
	private static final String HELP_FILE_NAME = "help/applicationhelp.hs";
	private static HKeeper 		hk = null;
	private static SwingWorker	loaderThread = null;
	
	
	/** Mini-Class for keeping HelpBroker and HelpSet */
	private static class HKeeper
	{
		public HelpBroker helpBroker = null;
		public HelpSet helpSet = null;
	}// inner class HKeeper
	
	
	/** 
	*	Initializes the Help system, to use the default Help file, 
	*	for the given Locale. If a Help file cannot be found, an
	*	error message is displayed, and help will be unavailable.
	*/
	public synchronized static void init()
	{
		loaderThread = new SwingWorker()
		{
			public Object construct()
			{
				long time = System.currentTimeMillis();
				HKeeper keeper = new HKeeper();
				
				try
				{ 
					final String helpFileName = Utils.getResourceBasePrefix() + HELP_FILE_NAME;
					
					URL url = HelpSet.findHelpSet(Utils.getClassLoader(), helpFileName, Utils.getLocale());
					Log.println("HelpSet URL: ", url);
					keeper.helpSet = new HelpSet(null, url);
				}
				catch(Exception e)
				{
					Log.println("Help not available: ", e);
					//ErrorDialog.displaySerious(null, e);
					return null;
				}
				
				keeper.helpBroker = keeper.helpSet.createHelpBroker("main_help_window");
				keeper.helpBroker.initPresentation();
				Log.printTimed(time, "Help construct() complete: ");
				return keeper;
			}// construct()
		};
		
		loaderThread.start(Thread.MIN_PRIORITY);
 	}// init()
	
	
	/** Checks if init() done; if not init, do nothing. */
	private synchronized static void checkInit()
	{
		if(hk == null)
		{
			if(loaderThread != null)
			{
				hk = (HKeeper) loaderThread.get();
				loaderThread = null;
			}
		}
	}// checkInit()
	
	/** Sets the context-sensitive Dialog-Level help */
	public static void enableDialogHelp(JDialog dialog, HelpID id)
	{
		checkInit();
		if(hk != null)
		{
			String sID = (id == null) ? null : id.toString();
			hk.helpBroker.enableHelpKey(dialog.getRootPane(), sID, hk.helpSet);
		}
	}// enableWindowHelp()
	
	
	/** Set the Help for a button (and Swing menu items) */
	public static void enableHelpOnButton(AbstractButton button, HelpID id)
	{
		checkInit();
		if(hk != null)
		{
			String sID = (id == null) ? null : id.toString();
			hk.helpBroker.enableHelpOnButton(button, sID, null);
		}
	}// enableHelpOnButton()
	
	
	/** Constant Class which provides a master index of Help IDs */
	public static class HelpID
	{
		/** Welcome (main help contents) */
		public static final HelpID Contents 	= new HelpID("Welcome");
		/** Printing help */
		public static final HelpID Printing		= new HelpID("Printing");
		/** Preferences help */
		public static final HelpID Preferences	= new HelpID("Preferences");
		/** New Game help */
		public static final HelpID NewGame		= new HelpID("Starting_A_New_Game");
		/** New F2F Game help */
		public static final HelpID Dialog_NewF2f			= new HelpID("StartF2F");
		/** Game and Player Info (metadata) dialog help */
		public static final HelpID Dialog_Metadata			= new HelpID("Metadata");
		/** Multiple-Order-Entry dialog help */
		public static final HelpID Dialog_MultiOrder		= new HelpID("Multiorder");
		/** Status Report dialog help */
		public static final HelpID Dialog_StatusReport		= new HelpID("Reports_Status");
		/** Result Report dialog help */
		public static final HelpID Dialog_ResultReport		= new HelpID("Reports_Results");
		/** Phase Select dialog help */
		public static final HelpID Dialog_PhaseSelect		= new HelpID("History_Select");
		/** Order Checking (validation) dialog help */
		public static final HelpID Dialog_OrderChecking		= new HelpID("Order_Checking");
		/** Judge Import Help */
		public static final HelpID Dialog_ImportJudge		= new HelpID("import_judge_htm");
		/** floc.net Import Help */
		public static final HelpID Dialog_ImportFloc		= new HelpID("import_floc_htm");
		
		// instance fields
		private final String id;
		
		private HelpID(String value)
		{
			if(value == null)
			{
				throw new IllegalArgumentException();
			}
			this.id = value;
		}// HelpID()
		
		public String toString()
		{
			return id;
		}// toString()
		
	}// nested class HelpID
	
	
	
	
	/** Constructor */
	private Help() {}
	
}// class Help
