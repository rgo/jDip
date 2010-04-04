
//  @(#)DisplayPreferencePanel.java		2/2003
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

package dip.gui.dialog.prefs;

import dip.gui.ClientFrame;
import dip.gui.OrderDisplayPanel;
import dip.gui.swing.GradientJLabel;
import dip.order.OrderFormat;
import dip.order.OrderFormatOptions;
import dip.misc.SharedPrefs;
import dip.misc.Utils;



// HIGLayout
import cz.autel.dmi.HIGConstraints;
import cz.autel.dmi.HIGLayout;

import java.util.prefs.Preferences;
import java.util.prefs.BackingStoreException;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JComboBox;
import javax.swing.JRadioButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import javax.swing.*;
import java.awt.*;
import javax.swing.event.*;
import java.awt.event.*;

import java.awt.Component;

/**
*	Display preferences
*	<p>
*	Static methods are included to access (in a controlled manner) preference-controlled
*	functionality.
*
*
*/
public class DisplayPreferencePanel extends PreferencePanel
{
	// i18n keys
	private static final String I18N_TAB_NAME		= "DPP.tab.text";
	
	private static final String I18N_ORDFMT_GROUP_NAME = "DPP.orderformat.group.text";
	private static final String I18N_FULL_NAME 		= "DPP.radiobutton.value.full";	
	private static final String I18N_BRIEF_NAME		= "DPP.radiobutton.value.brief";
	
	private static final String I18N_ORDFMT_UNIT		= "DPP.radiobutton.unit.text";
	private static final String I18N_ORDFMT_COAST		= "DPP.radiobutton.coast.text";
	private static final String I18N_ORDFMT_PROVINCE	= "DPP.radiobutton.province.text";
	private static final String I18N_ORDFMT_ORDERNAME	= "DPP.radiobutton.ordername.text";
	private static final String I18N_ORDFMT_POWERNAME	= "DPP.radiobutton.power.text";
	
	private static final String I18N_STYLE 			= "DPP.label.style";
	private static final String I18N_ARROW_STYLE 	= "DPP.label.arrowStyle";
	private static final String I18N_LABLE_PLURAL 	= "DPP.label.plural";
	private static final String I18N_LABLE_PAREN 	= "DPP.label.parentheses";
	private static final String I18N_LABLE_POSSESSIVE 	= "DPP.label.possessivePowers";
	private static final String I18N_LABLE_END_DOT 		= "DPP.label.enddot";
	private static final String I18N_STYLE_NAMES	= "DPP.combobox.stylenames";
	
	
	
	// Preference Node Keys
	private static final String NODE_ORDERFORMAT_ENCODED	= "orderFormat.format";
	                                                    
	// misc
	private static final int BORDER = 10;
	private static final int INDENT = 20;
	private ClientFrame	clientFrame = null;
	private OrderFormatOptions orderFormat = null;
	
	// GUI items
	private ChoiceSelection		csPower;
	private ChoiceSelection		csUnit;
	private ChoiceSelection		csOrderName;
	private ChoiceSelection		csProvince;
	private ChoiceSelection		csCoast;
	
	private JCheckBox cbPossessive;
	private JCheckBox cbDot;
	private JComboBox arrowBox;
	
	private JLabel example;
	
	
	
	public DisplayPreferencePanel(final ClientFrame cf)
	{
		super();
		clientFrame = cf;
		
		// get OFO from client
		orderFormat = clientFrame.getOFO();
		
		// create GUI components
		makeChoiceSelections();
		example = new JLabel(""); //Utils.createTextLabel("", false);
		updateExampleAndFormatOptions();
		
		// layout 
		//                   2      4      6    8   10     13   15       18    20     22     23
		int h1[] = { BORDER, 0,15,  35,10,  0,2, 0,2, 0,0,2, 0,2, 0,0,15,  0,5,  0,10,  0,15,  0,  BORDER };
		int w1[] = { BORDER, INDENT, 0,10,  0,15,  0,25,  0,4,0,  0, 0,  BORDER };
		
		HIGLayout l1 = new HIGLayout(w1, h1);
		l1.setColumnWeight(13, 1);
		l1.setRowWeight(23, 1);
		setLayout(l1);
		
		HIGConstraints c = new HIGConstraints();
		
		add(new GradientJLabel(Utils.getLocalString(I18N_ORDFMT_GROUP_NAME)), c.rcwh(2,2,12,1,"lr"));
		
		add(example, c.rcwh(4,3,11,1,"lr"));
		
		add(csPower.getLabel(), c.rcwh(6,3,1,1,"l"));
		/* power names cannot be full/brief (yet); show no radiobuttons
		add(csPower.getChoice1(), c.rcwh(6,5,1,1,"l"));
		add(csPower.getChoice2(), c.rcwh(6,7,1,1,"l"));
		*/
		add(new JLabel(Utils.getLocalString(I18N_STYLE)), c.rcwh(6,9,1,1,"r"));
		add(csPower.getComboBox(), c.rcwh(6,11,1,1,"l"));
		
		add(csUnit.getLabel(), c.rcwh(8,3,1,1,"l"));
		add(csUnit.getChoice1(), c.rcwh(8,5,1,1,"l"));
		add(csUnit.getChoice2(), c.rcwh(8,7,1,1,"l"));
		add(new JLabel(Utils.getLocalString(I18N_STYLE)), c.rcwh(8,9,1,1,"r"));
		add(csUnit.getComboBox(), c.rcwh(8,11,1,1,"l"));
		
		add(csOrderName.getLabel(), c.rcwh(10,3,1,1,"l"));
		add(csOrderName.getChoice1(), c.rcwh(10,5,1,1,"l"));
		add(csOrderName.getChoice2(), c.rcwh(10,7,1,1,"l"));
		add(new JLabel(Utils.getLocalString(I18N_STYLE)), c.rcwh(10,9,1,1,"r"));
		add(csOrderName.getComboBox(), c.rcwh(10,11,1,1,"l"));
		add(csOrderName.getCheckBox(), c.rcwh(11, 5,9,1,"l"));
		
		add(csProvince.getLabel(), c.rcwh(13,3,1,1,"l"));
		add(csProvince.getChoice1(), c.rcwh(13,5,1,1,"l"));
		add(csProvince.getChoice2(), c.rcwh(13,7,1,1,"l"));
		add(new JLabel(Utils.getLocalString(I18N_STYLE)), c.rcwh(13,9,1,1,"r"));
		add(csProvince.getComboBox(), c.rcwh(13,11,1,1,"l"));
		
		add(csCoast.getLabel(), c.rcwh(15,3,1,1,"l"));
		add(csCoast.getChoice1(), c.rcwh(15,5,1,1,"l"));
		add(csCoast.getChoice2(), c.rcwh(15,7,1,1,"l"));
		add(new JLabel(Utils.getLocalString(I18N_STYLE)), c.rcwh(15,9,1,1,"r"));
		add(csCoast.getComboBox(), c.rcwh(15,11,1,1,"l"));
		add(csCoast.getCheckBox(), c.rcwh(16, 5,9,1,"l"));
		
		add(cbPossessive, c.rcwh(18, 3, 11, 1, "l"));
		add(cbDot, c.rcwh(20, 3, 11, 1, "l"));
		
		JPanel arrowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		arrowPanel.add(new JLabel(Utils.getLocalString(I18N_ARROW_STYLE)));
		arrowPanel.add(Box.createHorizontalStrut(4));
		arrowPanel.add(arrowBox);
		
		add(arrowPanel, c.rcwh(22, 3, 11, 1, "l"));
		
	}// DisplayPreferencePanel()
	
	
	
	
	
	
	
	
	public String getName()
	{
		return Utils.getLocalString(I18N_TAB_NAME);
	}// getName()
	
	
	public void apply()
	{
		Preferences prefs = SharedPrefs.getUserNode();
		
		// set preference nodes
		prefs.putByteArray(NODE_ORDERFORMAT_ENCODED, orderFormat.encode());
		
		// apply OrderFormat to ClientFrame
		clientFrame.setOFO(orderFormat);
		
		try { prefs.flush(); } catch (BackingStoreException bse) {}
		
		// refresh OrderPanel
		final OrderDisplayPanel odp = clientFrame.getOrderDisplayPanel();
		if(odp != null)
		{
			odp.refresh();
		}
	}// apply()
	
	
	public void setDefault()
	{
		orderFormat = OrderFormatOptions.createDefault();
	}// setDefault()
	
	
	public void cancel()
	{
		// do nothing
	}// cancel()
	
	
	/**
	*	Get the OrderFormatOptions from the stored preferences, or
	*	return a default set of OrderFormatOptions.
	*/
	public static OrderFormatOptions getOrderFormatOptions()
	{
		OrderFormatOptions ofo = OrderFormatOptions.createDefault();
		
		Preferences prefs = SharedPrefs.getUserNode();
		try { prefs.sync(); } catch (BackingStoreException bse) {}
		
		final byte[] encodedBytes = prefs.getByteArray(NODE_ORDERFORMAT_ENCODED, 
			null);
		
		if(encodedBytes != null)
		{
			ofo = OrderFormatOptions.decode(encodedBytes, 
				OrderFormatOptions.createDefault());
		}
		
		return ofo;
	}// getOrderFormatOptions()
	
	
	
	/** Make ChoiceSelections, from preferences (or default values). */
	private void makeChoiceSelections()
	{
		Preferences prefs = SharedPrefs.getUserNode();
		try { prefs.sync(); } catch (BackingStoreException bse) {}
		
		// defaults: 
		orderFormat = getOrderFormatOptions();
		
		// change listener
		ExampleChangeListener ecl = new ExampleChangeListener();
		
		// order-format radiobutton setings
		csPower = new ChoiceSelection(Utils.getLocalString(I18N_ORDFMT_POWERNAME),
			orderFormat.getPowerFormat(), orderFormat.getPowerStyle(), false, false, ecl);	
		
		csUnit = new ChoiceSelection(Utils.getLocalString(I18N_ORDFMT_UNIT),
			orderFormat.getUnitFormat(), orderFormat.getUnitStyle(), false, false, ecl);	
		
		csProvince = new ChoiceSelection(Utils.getLocalString(I18N_ORDFMT_PROVINCE),
			orderFormat.getProvinceFormat(), orderFormat.getProvinceStyle(), false, false, ecl);	
			
		csCoast = new ChoiceSelection(Utils.getLocalString(I18N_ORDFMT_COAST),
			orderFormat.getCoastFormat(), orderFormat.getCoastStyle(), true, false, ecl);	
			
		csOrderName = new ChoiceSelection(Utils.getLocalString(I18N_ORDFMT_ORDERNAME),
				orderFormat.getOrderNameFormat(), orderFormat.getOrderNameStyle(), false, true, ecl);	
		
		// misc. options
		cbPossessive = new JCheckBox(Utils.getLocalString(I18N_LABLE_POSSESSIVE));
		cbPossessive.setSelected(orderFormat.getShowPossessivePower());
		cbPossessive.addChangeListener(ecl);
		
		cbDot = new JCheckBox(Utils.getLocalString(I18N_LABLE_END_DOT));
		cbDot.setSelected(orderFormat.getEndWithDot());
		cbDot.addChangeListener(ecl);
		
		arrowBox = new JComboBox(OrderFormatOptions.ARROWS);
		arrowBox.setEditable(false);
		arrowBox.setPrototypeDisplayValue("MMM");
		arrowBox.setSelectedItem(orderFormat.getArrow());
		arrowBox.addActionListener(ecl);
	}// makeChoiceSelections()
	
	
	/** 
	*	Updates the Example text order.
	*
	*/
	private void updateExampleAndFormatOptions()
	{
		assert (orderFormat != null);
		
		orderFormat.setPowerFormat(csPower.getFormat());
		orderFormat.setPowerStyle(csPower.getStyle());
		
		orderFormat.setUnitFormat(csUnit.getFormat());
		orderFormat.setUnitStyle(csUnit.getStyle());
		
		orderFormat.setOrderNameFormat(csOrderName.getFormat());
		orderFormat.setOrderNameStyle(csOrderName.getStyle());
		
		orderFormat.setProvinceFormat(csProvince.getFormat());
		orderFormat.setProvinceStyle(csProvince.getStyle());
		
		orderFormat.setCoastFormat(csCoast.getFormat());
		orderFormat.setCoastStyle(csCoast.getStyle());
		
		orderFormat.setShowPossessivePower(cbPossessive.isSelected());
		orderFormat.setEndWithDot(cbDot.isSelected());
		
		orderFormat.setArrow((String) arrowBox.getSelectedItem());
		
		// update the example text
		StringBuffer sb = new StringBuffer(128);
		sb.append("<html><b>");
		sb.append(OrderFormat.getFormatExample(orderFormat, clientFrame.getGUIOrderFactory()));
		sb.append("</b></html>");
		example.setText( sb.toString() );
	}// updateExampleAndFormatOptions()
	
	
	
	/** 
	*	Inner class that implements a description plus 2 choices
	*	which are radiobuttons. Allows the getting() and setting()
	*	of the radiobuttons based upon OrderFormat FMT constants.
	*/
	private static class ChoiceSelection extends JPanel
	{
		private ButtonGroup bg;
		private JRadioButton brief;
		private JRadioButton full;
		private JComboBox styleBox;
		private JLabel label;
		private JCheckBox checkBox = null;
		private boolean allowPlural = false;
		private boolean allowParens = false;
		
		private static final int PLURAL_DELTA = (OrderFormatOptions.STYLE_PLURAL_NONE - OrderFormatOptions.STYLE_NONE);
		
		/** 
		*	Create a ChoiceSelection with the given category label "label", and
		*	the given format value: brief (true) or full (false)).
		*/
		public ChoiceSelection(final String labelText, final int formatValue, 
			final int styleValue, final boolean allowParentheses,
			final boolean allowPlural, final ExampleChangeListener cl)
		{
			super();
			
			if(allowPlural && allowParentheses)
			{
				throw new IllegalArgumentException();
			}
			
			this.allowPlural = allowPlural;
			this.allowParens = allowParentheses;
			
			// create and add components
			bg = new ButtonGroup();
			full = new JRadioButton(Utils.getLocalString(I18N_FULL_NAME));
			full.getModel().addChangeListener(cl);
			brief = new JRadioButton(Utils.getLocalString(I18N_BRIEF_NAME));
			brief.getModel().addChangeListener(cl);
			
			// label
			label = new JLabel(labelText);
			
			// combobox 
			styleBox = new JComboBox((String[]) Utils.parseCSV(
				Utils.getLocalString(I18N_STYLE_NAMES)));
			styleBox.setPrototypeDisplayValue("MMMMMMMMMM");
			styleBox.setEditable(false);
			styleBox.addActionListener(cl);
			
			// add buttongroup components
			bg.add(full);
			bg.add(brief);
			
			
			if(allowParens)
			{
				checkBox = new JCheckBox(Utils.getLocalString(I18N_LABLE_PAREN));
			}
			else if(allowPlural)
			{
				checkBox = new JCheckBox(Utils.getLocalString(I18N_LABLE_PLURAL));
			}
			
			
			if(checkBox != null)
			{
				checkBox.addChangeListener(cl);
			}
			
			// set components with initial settings
			update(formatValue, styleValue);
		}// ChoiceSelection()
		
		
		/** Get label */
		public Component getLabel()			{ return label; }
		/** Get choice 1 */
		public Component getChoice1()		{ return full; }
		/** Get choice 2 */
		public Component getChoice2()		{ return brief; }
		/** Get style combobox */
		public Component getComboBox()		{ return styleBox; }
		/** Get checkbox (if any); may return null */
		public Component getCheckBox()		{ return checkBox; }
		
		
		
		public void update(final int formatValue, final int styleValue)
		{
			if( formatValue == OrderFormatOptions.FORMAT_BRIEF 
				|| formatValue == OrderFormatOptions.FORMAT_COAST_PAREN_BRIEF )
			{
				brief.setSelected(true);
			}
			else if(formatValue == OrderFormatOptions.FORMAT_FULL 
					|| formatValue == OrderFormatOptions.FORMAT_COAST_PAREN_FULL)
			{
				full.setSelected(true);
			}
			
			if(allowParens)
			{
				if( formatValue == OrderFormatOptions.FORMAT_COAST_PAREN_FULL
					|| formatValue == OrderFormatOptions.FORMAT_COAST_PAREN_BRIEF )
				{
					checkBox.setSelected(true);
				}
				else
				{
					checkBox.setSelected(false);
				}
			}
			
			if(allowPlural)
			{
				if(styleValue >= PLURAL_DELTA)
				{
					styleBox.setSelectedIndex(styleValue - PLURAL_DELTA);
					checkBox.setSelected(true);
				}
				else
				{
					styleBox.setSelectedIndex(styleValue);
					checkBox.setSelected(false);
				}
			}
			else
			{
				styleBox.setSelectedIndex(styleValue);
			}
		}// update()
		
		
		/** 
		*	Returns the OrderFormat FORMAT specifier
		*/
		public int getFormat()
		{
			final int mod = (allowParens && checkBox.isSelected()) ? 10 : 0;
			
			if(bg.isSelected(brief.getModel()))
			{
				return OrderFormatOptions.FORMAT_BRIEF + mod;
			}
			else if(bg.isSelected(full.getModel()))
			{
				return OrderFormatOptions.FORMAT_FULL + mod;
			}
			else
			{
				throw new IllegalStateException();
			}
		}// getBriefOrFull()
		
		/**
		*	Returns the OrderFormat STYLE specifier
		*/
		public int getStyle()
		{
			final int mod = (allowPlural && checkBox.isSelected()) ? PLURAL_DELTA : 0;
			return styleBox.getSelectedIndex() + mod;
		}// getStyle()
	}// inner class ChoiceSelection
	
	
	/** Listens for radiobutton changes, and updates the Example text. */
	private class ExampleChangeListener implements ChangeListener, ActionListener
	{
		public void stateChanged(ChangeEvent e)
		{
			if(example != null)
			{
				updateExampleAndFormatOptions();
			}
		}// stateChanged()
		
		public void actionPerformed(ActionEvent e)
		{
			if(example != null)
			{
				updateExampleAndFormatOptions();
			}
		}// actionPerformed()
	}// ExampleChangeListener()
	
}// class DisplayPreferencePanel
