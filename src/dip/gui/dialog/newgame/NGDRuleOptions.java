//
//  @(#)NGDRuleOptions.java		10/2002
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
package dip.gui.dialog.newgame;

import dip.world.RuleOptions.Option;
import dip.world.RuleOptions.OptionValue;
import dip.world.*;
import dip.world.variant.data.Variant;
import dip.gui.dialog.ErrorDialog;
import dip.misc.Utils;
import dip.gui.swing.*;

// HIGLayout
import cz.autel.dmi.HIGConstraints;
import cz.autel.dmi.HIGLayout;

import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.Set;

/**
*
*	Defines the game starting options for the New Game dialog.
*	<p>
*	This allows the user to alter any game parameters before the 
*	game starts.
*	<p>
*	Reset button allows reversion to defaults.
*/
public class NGDRuleOptions extends JPanel implements NewGameDialog.NGDTabPane
{
	// constants
	private static final String TAB_NAME			= "NGDRuleOpts.tab.name";
	private static final String BUTTON_RESET 		= "NGDRuleOpts.button_reset";
	private static final String INTRO_TEXT			= "NGDRuleOpts.text.intro";
	
	// instance variables
	private RuleOptions ruleOpts;
	private Variant 	variant;
	private DefaultListModel 	optionListModel;
	private ButtonGroup		buttonGroup;
	private RBListener		rbListener;
	private InvalidWorldException heldException = null;
	
	// GUI controls
	private JList 			optionList;
	private JEditorPane		description;
	private JRadioButton[]	radioButtons;
	private JButton			reset;
	
	/** Create the RuleOptions panel for the New Game dialog */
	public NGDRuleOptions()
	{
		// option list
		optionListModel = new DefaultListModel();
		optionList = new JList(optionListModel);
		optionList.setFixedCellWidth(100);
		optionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		optionList.addListSelectionListener(new ListSelectionListener()
		{
			public void valueChanged(ListSelectionEvent e)
			{
				updateChoices();
			}
		});
		
		// reset button
		reset = new JButton( Utils.getLocalString(BUTTON_RESET) );
		reset.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				resetData();
			}
		});
		
		// description text
		description = Utils.createTextLabel(true);
		description.setText( makeHTML(Utils.getLocalString(INTRO_TEXT)) );
		
		// rb setup
		buttonGroup = new ButtonGroup();
		rbListener = new RBListener();
		radioButtons = new JRadioButton[6];
		for(int i=0; i<radioButtons.length; i++)
		{
			radioButtons[i] = new JRadioButton();
			radioButtons[i].addActionListener(rbListener);
			radioButtons[i].setVisible(false);
			buttonGroup.add(radioButtons[i]);
		}
		
		// layout
		makeLayout();
	}// NGDStartOptions()
	
	
	/** Gets the (possibly) modified RuleOptions. This will never return null. */
	public synchronized RuleOptions getRuleOptions()
	throws InvalidWorldException
	{
		// give same error we got before.
		if(heldException != null)
		{
			throw heldException;
		}
		
		return ruleOpts;
	}// getVariant()
	
	
	/** Enables & Disables controls on this panel */
	public void setEnabled(boolean value)
	{
		optionList.setEnabled(value);
		reset.setEnabled(value);
		for(int i=0; i<radioButtons.length; i++)
		{
			radioButtons[i].setEnabled(value);
		}
		
		super.setEnabled(value);
	}// setEnabled()
	
	
	/** Set data from originally passed reference */
	private void resetData()
	{
		try
		{
			heldException = null;
			ruleOpts = RuleOptions.createFromVariant(variant);
			refreshList();
			setEnabled(true);
		}
		catch(InvalidWorldException e)
		{
			heldException = e;
			setEnabled(false);
			ruleOpts = null;
			ErrorDialog.displayGeneral(null, e);
		}
	}// resetData()
	
	
	private void refreshList()
	{
		// remove old list data
		optionListModel.removeAllElements();
		optionList.clearSelection();
		
		// create a list of OptListItems, and refresh list data
		Set options = ruleOpts.getAllOptions();
		Iterator iter = options.iterator();
		while(iter.hasNext())
		{
			optionListModel.addElement( new OptListItem((Option) iter.next()) );
		}
	}// refreshList()
	
	
	/** Updates the OptionValue choices (upto 6) associated with the selected Option */
	private void updateChoices()
	{
		OptListItem optListItem = (OptListItem) optionList.getSelectedValue();
		if(optListItem == null)
		{
			for(int i=0; i<radioButtons.length; i++)
			{
				radioButtons[i].setVisible(false);
			}
			
			description.setText( makeHTML(Utils.getLocalString(INTRO_TEXT)) );
			
			for(int i=0; i<radioButtons.length; i++)
			{
				radioButtons[i].setText("");
				radioButtons[i].setSelected(false);
				radioButtons[i].setVisible(false);
			}
			
			revalidate();
			return;
		}
		
		// get option
		Option option = optListItem.getOption();
		
		// set description
		description.setText( makeHTML(option.getDescriptionI18N()) );
		
		// radio-button setup
		OptionValue[] 	allowedOptVals = option.getAllowed();
		OptionValue 	current = ruleOpts.getOptionValue(option);	// currently selected value	
		
		for(int i=0; i<radioButtons.length; i++)
		{
			if(i < allowedOptVals.length)
			{
				radioButtons[i].setText( allowedOptVals[i].getNameI18N() );
				radioButtons[i].setToolTipText( allowedOptVals[i].getDescriptionI18N() );
				radioButtons[i].setActionCommand(String.valueOf(i));
				radioButtons[i].setVisible(true);
				
				// select, if we are selected
				radioButtons[i].setSelected( ((current == allowedOptVals[i]) ? true : false) );
			}
			else
			{
				radioButtons[i].setText("");
				radioButtons[i].setSelected(false);
				radioButtons[i].setVisible(false);
			}
		}
		
		repaint();
	}// updateChoices()
	
	
	/** Layout the panel */
	private void makeLayout()
	{
		// layout subpanel (description + radio buttons)
		int w1[] = { 25, 0, 5 };	//            9  10 11 12 13 14 15  16,17  18,19
		int h1[] = { 10, 0, 3, 0, 3, 0, 3, 0, 3, 0, 3, 0, 3, 0, 10, 0,5,   0,5 };
		
		HIGLayout l1 = new HIGLayout(w1, h1);
		l1.setColumnWeight(2, 1);
		l1.setRowWeight(14, 1);
		
		JPanel ruleListPanel = new JPanel(new BorderLayout());
		ruleListPanel.setBorder(new EmptyBorder(5,5,5,5));
		ruleListPanel.add(new XJScrollPane(optionList), BorderLayout.CENTER); 
		
		JPanel subPanel = new JPanel();
		subPanel.setLayout(l1);
		HIGConstraints c = new HIGConstraints();
		
		subPanel.add(reset, c.rcwh(18, 1, 2, 1, "l"));
		subPanel.add(new JSeparator(), c.rcwh(16,1,2,1, "lr"));
		subPanel.add(new JPanel(), c.rcwh(14,1,2,1));
		for(int i=0; i<radioButtons.length; i++)
		{
			subPanel.add(radioButtons[i], c.rc((2*(i+1)),2,"l"));		
		}
		
		JPanel rightPanel = new JPanel();
		rightPanel.setLayout(new BorderLayout());
		rightPanel.add(description, BorderLayout.NORTH);
		rightPanel.add(subPanel, BorderLayout.CENTER);
		
		this.setLayout(new BorderLayout());
		this.add(ruleListPanel, BorderLayout.WEST);
		this.add(rightPanel, BorderLayout.CENTER);
	}// makeLayout()
	
	
	/** makes the text HTML */
	private String makeHTML(String in)
	{
		StringBuffer sb = new StringBuffer(in.length()+64);
		sb.append("<html><font face=\"Arial, Helvetica\" size=\"-1\">");
		sb.append(in);
		sb.append("</html>");
		return sb.toString();
	}// makeHTML()
	
	
	/** Get the tab name. */
	public String getTabName()
	{
		return Utils.getLocalString(TAB_NAME);
	}// getTabName()

	/** The Variant has Changed. */
	public void variantChanged(Variant variant)
	{
		this.variant = variant;
		resetData();
		revalidate();
	}// variantChanged()
	
	/** The Enabled status has Changed. */
	public void enablingChanged(boolean enabled)
	{
		setEnabled(enabled);
	}// enablingChanged()
	
	
	/** Private class to encapsulate an Option and have it display the il8n name in a JList */
	private class OptListItem extends Object
	{
		private Option option;
		
		public OptListItem(Option option)
		{
			this.option = option;
		}
		
		public String toString()	{ return option.getNameI18N(); }
		public Option getOption() 	{ return option; }
	}// inner class OptListItem
	
	
	/** RB listener */
	private class RBListener implements ActionListener
	{
		public void actionPerformed(ActionEvent e) 
		{
			// set options according to what we found from the button group.
			// if possible. The action command corresponds to the button index,
			// which corresponds to the index in the values array, since all
			// arrays in ValidationOptions correspond.
			if(radioButtons != null)
			{
				OptListItem oli = (OptListItem) optionList.getSelectedValue();
				if(oli != null)
				{
					int idx = Integer.parseInt(e.getActionCommand());
					ruleOpts.setOption(oli.getOption(), oli.getOption().getAllowed()[idx]);
				}
			}
		}
	}// inner class RBListener
	
	
}// class NGDRuleOptions
