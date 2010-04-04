//
//  @(#)PreferenceDialog.java	1.00	4/1/2002
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

package dip.gui.dialog.prefs;

import dip.gui.dialog.HeaderDialog;
import dip.gui.ClientFrame;
import dip.misc.Utils;

import java.awt.Dimension;
import javax.swing.JTabbedPane;

import dip.gui.dialog.prefs.ExportPreferencePanel;


/**
*
*	Creates a Preferences Dialog.
*	<p>
*	Loads PreferencePanels, displays on a page.
*	<p>
*	In the future, these will be loadable at run-time.
*
*
*/
public class PreferenceDialog extends HeaderDialog
{
	// constants
	private static final String TITLE = "PreferenceDialog.title";
	private static final String HEADER_LOCATION = "PreferenceDialog.header";
	
	// GUI
	private ClientFrame			parent;
	private JTabbedPane 		tabPane;
	private PreferencePanel[]	tabbedPanels;		// make static, & create just once?
	
	
	
	
	/** Show the Preferences dialog. */
	public static void displayDialog(ClientFrame parent)
	{
		PreferenceDialog pd = new PreferenceDialog(parent);
		pd.createPanels();
		pd.pack();		
		pd.setSize(new Dimension(450, 550));
		Utils.centerInScreen(pd);
		pd.setVisible(true);
	}// displayDialog()
	
	
	
	private PreferenceDialog(ClientFrame parent)
	{
		super(parent, Utils.getLocalString(TITLE), true);
		this.parent = parent;
		
		tabPane = new JTabbedPane(JTabbedPane.TOP);
		
		setHeaderText( Utils.getText(Utils.getLocalString(HEADER_LOCATION)) );
		setContentPane(tabPane);
		createDefaultContentBorder(tabPane);
		addTwoButtons( makeCancelButton(), makeOKButton(), false, true );
		setHelpID(dip.misc.Help.HelpID.Preferences);
	}// AboutDialog()
	
	
	private void createPanels()
	{
		tabbedPanels = new PreferencePanel[3];
		tabbedPanels[0] = new GeneralPreferencePanel(parent);
		tabbedPanels[1] = new DisplayPreferencePanel(parent);
		tabbedPanels[2] = new ExportPreferencePanel(parent);
		
		for(int i=0; i<tabbedPanels.length; i++)
		{
			tabPane.addTab(tabbedPanels[i].getName(), tabbedPanels[i]);
		}
	}// createPanels()
	
	
	/** Apply or Cancel settings after closing dialog */
	public void close(String actionCommand)
	{
		super.close(actionCommand);
		
		if(isCloseOrCancel(actionCommand))
		{
			for(int i=0; i<tabbedPanels.length; i++)
			{
				tabbedPanels[i].cancel();
			}
		}
		else
		{
			for(int i=0; i<tabbedPanels.length; i++)
			{
				tabbedPanels[i].apply();
			}
		}
	}// close()
	
	
	
}// class PreferenceDialog
