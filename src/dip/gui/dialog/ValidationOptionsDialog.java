//
//  @(#)ValidationOptionsDialog.java	1.00	4/1/2002
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
import dip.order.ValidationOptions;
import dip.gui.ClientFrame;

import cz.autel.dmi.HIGConstraints;
import cz.autel.dmi.HIGLayout;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.ButtonGroup;
import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JSeparator;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.ListSelectionModel;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;


/**
*	Allows setting of validation options
*	<p>
*	(from any legal validation options object)
*	<p>
*	LIMITATIONS<br>
*		Only 6 radiobuttons (and thus max of 6 choices) per option, in this incarnation.
*		
*/
public class ValidationOptionsDialog extends HeaderDialog
{
	// i18n constants
	private static final String DIALOG_TITLE = "VOD.title";
	private static final String INIT_TEXT = "VOD.starttext";
	private static final String HEADER_LOCATION = "VOD.location.header";
	
	// instance variables
	private ValidationOptions oldOpts = null;
	private ValidationOptions valOpts = null;
	private ValidationOptions returnedOpts = null;
	private ClientFrame parent = null;
	private ValidationOptions.DescriptiveOption[] dopts = null;
	
	// GUI components
	private JList 	optionList = null;
	
	// GUI components: on radio button panel
	private JEditorPane		description = null;
	private JRadioButton[]	radioButtons = null;		
	private ButtonGroup		buttonGroup = null;
	private RBListener		rbListener = null;
	
	private StringBuffer sb = new StringBuffer(1024);
	
	
	/** 
	*	Display the ValidationOptions dialog, and return the chosen options
	*	(or the old options, if cancelled).
	*/
	public static ValidationOptions displayDialog(ClientFrame parent, ValidationOptions oldOptions)
	{
		ValidationOptionsDialog vod = new ValidationOptionsDialog(parent, oldOptions);
		vod.pack();
		vod.setSize(Utils.getScreenSize(0.45f));
		Utils.centerIn(vod, parent);
		vod.setVisible(true);
		return vod.getValidationOptions();
	}// displayDialog()
	
	
	/** Display the ValidationOptions dialog */
	public static ValidationOptions displayDialog(ClientFrame parent)
	{
		return displayDialog(parent, null);
	}// displayDialog()
	
	
	/** Get the ValidationOptions selected by the user. */
	public ValidationOptions getValidationOptions()
	{
		return returnedOpts;
	}// getValidationOptions()
	
	
	private ValidationOptionsDialog(ClientFrame parent, ValidationOptions oldOptions)
	{
		super(parent, Utils.getLocalString(DIALOG_TITLE), true);
		this.parent = parent;
		this.oldOpts = (oldOptions == null) ? (new ValidationOptions()) : oldOptions;
		
		// clone old options into new validation options.
		try
		{
			valOpts = (ValidationOptions) oldOpts.clone();
		}
		catch(CloneNotSupportedException e)
		{
			System.err.println(e);	// print error; should not occur
			valOpts = oldOpts; 		// we'll continue, but no protection of old data.
		}
		
		// description setup
		description = Utils.createTextLabel(true);
		
		// rb setup
		buttonGroup = new ButtonGroup();
		rbListener = new RBListener();
		radioButtons = new JRadioButton[6];
		for(int i=0; i<radioButtons.length; i++)
		{
			radioButtons[i] = new JRadioButton();
			radioButtons[i].addActionListener(rbListener);
			buttonGroup.add(radioButtons[i]);
		}
		
		// HeaderDialog setup
		setHeaderText( Utils.getText(Utils.getLocalString(HEADER_LOCATION)) );
		addTwoButtons( makeCancelButton(), makeOKButton(), false, true );
		setHelpID(dip.misc.Help.HelpID.Dialog_OrderChecking);
		
		// listbox setup
		setupList();
		
		// make the rest of the dialog, including the update panel.
		makeVODLayout();
		
		updatePanel();
	}// ValidationOptionsDialog()
	
	
	public void close(String actionCommand)
	{
		returnedOpts = (isOKorAccept(actionCommand)) ? valOpts : oldOpts;
		dispose();
	}// close()
	
	
	/**
		given the index of the 'descriptiveOption'
		creates a GUI panel
		listens for GUI input
			any input change == option change
		shows descriptions for options (as tooltips)
		and for main option (at top of panel)
	*/
	private void updatePanel()
	{
		int currentIndex = optionList.getSelectedIndex();
		if(currentIndex < 0)
		{
			for(int i=0; i<radioButtons.length; i++)
			{
				radioButtons[i].setVisible(false);
			}
			description.setText( makeHTML(Utils.getLocalString(INIT_TEXT)) );
			return;
		}
		
		// description
		description.setText( makeHTML(dopts[currentIndex].getDescription()) );
		
		// radio-button setup
		String[] bText = dopts[currentIndex].getDisplayValues();
		String[] bTips = dopts[currentIndex].getValueDescriptions();
		Object[] oVals = dopts[currentIndex].getValues(); 	// possible values
		
		if(bTips.length != bText.length)
		{
			Utils.popupError(parent, "Resource Error", "Validation Options "+dopts[currentIndex].getKey()+"; values ("+bText.length+") / description ("+bTips.length+") mismatch; must have same number of items.");
			return;
		}
		
		int nButtons = bText.length;
		Object value = valOpts.getOption(dopts[currentIndex].getKey());	// currently selected value	
		
		for(int i=0; i<radioButtons.length; i++)
		{
			if(i < nButtons)
			{
				radioButtons[i].setText(bText[i]);
				radioButtons[i].setSelected(((oVals[i].equals(value)) ? true : false));
				radioButtons[i].setToolTipText(bTips[i]);
				radioButtons[i].setActionCommand(String.valueOf(i));
				radioButtons[i].setVisible(true);
			}
			else
			{
				radioButtons[i].setSelected(false);
				radioButtons[i].setText("");
				radioButtons[i].setVisible(false);
			}
		}
		
		repaint();
	}// updatePanel()
	
	
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
				int listIdx = optionList.getSelectedIndex();
				int idx = Integer.parseInt(e.getActionCommand());
				valOpts.setOption(dopts[listIdx].getKey(), dopts[listIdx].getValues()[idx]);
			}
		}
	}// nested class RBListener
	
	
	private void setupList()
	{
		// get descriptive options
		dopts = valOpts.getOptions();
		
		// create an array of option-names for the list.
		String[] options = new String[dopts.length];
		for(int i=0; i<options.length; i++)
		{
			options[i] = dopts[i].getDisplayName();
		}
		
		optionList = new JList(options);
		optionList.setBorder(new EtchedBorder());
		optionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		optionList.addListSelectionListener(new ListSelectionListener()
		{
			public void valueChanged(ListSelectionEvent e)
			{
				updatePanel();
			}
		});
	}// setupList()
	
	
	private void makeVODLayout()
	{
		// layout subpanel (description + radio buttons)
		int w1[] = { 25, 0 };
		int h1[] = { 10, 0, 3, 0, 3, 0, 3, 0, 3, 0, 3, 0, 3,  0 };
		
		HIGLayout l1 = new HIGLayout(w1, h1);
		l1.setColumnWeight(2, 1);
		l1.setRowWeight(14, 1);
		
		JPanel subPanel = new JPanel();
		subPanel.setLayout(l1);
		HIGConstraints c = new HIGConstraints();
		
		subPanel.add(new JPanel(), c.rcwh(14,1,2,1));
		for(int i=0; i<radioButtons.length; i++)
		{
			subPanel.add(radioButtons[i], c.rc((2*(i+1)),2,"l"));		
		}
		
		JPanel rightPanel = new JPanel();
		rightPanel.setLayout(new BorderLayout());
		rightPanel.add(description, BorderLayout.NORTH);
		rightPanel.add(subPanel, BorderLayout.CENTER);
		rightPanel.add(new JSeparator(), BorderLayout.SOUTH);
		rightPanel.setBorder(BorderFactory.createEmptyBorder(0,10,0,0));
		
		JPanel contentPanel = new JPanel(new BorderLayout());
		contentPanel.add(optionList, BorderLayout.WEST);
		contentPanel.add(rightPanel, BorderLayout.CENTER);
		
		createDefaultContentBorder(contentPanel);
		setContentPane(contentPanel);
	}// makeVODLayout()
	
	private String makeHTML(String in)
	{
		sb.setLength(0);
		sb.append("<html><font face=\"Arial, Helvetica\" size=\"-1\">");
		sb.append(in);
		sb.append("</html>");
		return sb.toString();
	}// makeHTML()
	
	
	
	
	
	
	
	
	
	
	
	
	
}// class ValidationOptionsDialog
