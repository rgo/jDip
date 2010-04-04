//
//  @(#)OrderStatusPanel.java		5/2003
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
package dip.gui;

import dip.order.*;
import dip.world.*;
import dip.gui.undo.*;
import dip.gui.swing.*;
import dip.gui.order.GUIOrder;
import dip.misc.Utils;
import dip.process.Adjustment;
import dip.misc.Log;

import dip.order.result.Result;
import dip.order.result.OrderResult;

import cz.autel.dmi.*;		// HIGLayout

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
import javax.swing.table.*;
import javax.swing.undo.*;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.*;
import java.util.*;
import java.text.MessageFormat;

/**
*	OrderStatusPanel: contains a label that displays the current phase,
*	if a game is loaded. Also displays (when appropriate) a text field
*	where the user may enter orders in text format.
*	
*/
public class OrderStatusPanel extends XJPanel
{
	// i18n constnats
	private final static String LABEL_ORDER			= "OP.label.order";
	private final static String EMPTY				= "";
	
	// instance variables
	private JLabel					orderFieldLabel;
	private JLabel					phase;
	private JTextField 				orderField;
	private OSPPropertyListener 	propListener = null;
	private ClientFrame 			cf = null;
	
	
	
	/**
	*	Creates an OrderStatusPanel object.
	*/
	public OrderStatusPanel(ClientFrame clientFrame)
	{
		this.cf = clientFrame;
		
		// setup labels
		phase = new JLabel(EMPTY);
		orderFieldLabel = new JLabel(Utils.getLocalString(LABEL_ORDER));
		
		
		// setup text field
		orderField = new dip.gui.swing.XJTextField();
		orderField.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				String text = orderField.getText();
				
				if(text.equals(EMPTY))
				{
					return;
				}
				
				// add an order; if no error occured, clear 
				// the textfield
				if(cf.getOrderDisplayPanel() != null)
				{
					if(cf.getOrderDisplayPanel().addOrder(text, true))
					{
						orderField.setText(EMPTY);
						orderField.repaint();
					}
				}
			}
		});
		
		// setup propety listener
		propListener = new OSPPropertyListener();
		cf.addPropertyChangeListener(propListener);
		
		// do layout
		makeLayout();
	}// OrderStatusPanel()
	
	
	/** Performs any cleanup. */
	public void close()
	{
		cf.removePropertyChangeListener(propListener);
	}// close()
	
	
	/**
	*	Sets the text in the order text field.
	*	<p>
	*	Note that this does not parse the text; 
	*	however this text is "live", and the user may edit it.
	*/
	public void setOrderText(String value)
	{
		orderField.setText(value);
	}// setOrderText()
	
	
	/**
	*	Clears the order TextField of any text
	*/
	public void clearOrderText()
	{
		orderField.setText(EMPTY);
	}// clearOrderText()
	
	
	/**
	*	Property change event listener
	*
	*/
	private class OSPPropertyListener extends AbstractCFPListener
	{
		public void actionOrderCreated(Orderable order)
		{
			clearOrderText();
		}
		
		public void actionOrderDeleted(Orderable order)
		{
			clearOrderText();
		}
		
		public void actionOrdersCreated(Orderable[] orders)
		{
			clearOrderText();
		}
		
		public void actionOrdersDeleted(Orderable[] orders)
		{
			clearOrderText();
		}
		
		public void actionModeChanged(String mode)
		{
			if(mode == ClientFrame.MODE_ORDER)
			{
				orderField.setVisible(true);
				orderFieldLabel.setVisible(true);
			}
			else
			{
				orderField.setVisible(false);
				orderFieldLabel.setVisible(false);
			}
		}// actionModeChanged()
		
		public void actionTurnstateChanged(TurnState turnState)
		{
			Phase tsPhase = turnState.getPhase();
			
			// set game time
			StringBuffer sb = new StringBuffer(32);
			sb.append("<html><h2>");
			sb.append(tsPhase.toString());
			sb.append("</h2></html>");
			phase.setText(sb.toString());
		}// actionTurnstateChanged()
		
		
		public void actionWorldCreated(World w)
		{
			phase.setText(EMPTY);
		}
		
		public void actionWorldDestroyed(World w)
		{
			phase.setText(EMPTY);
		}
	}// inner class OSPPropertyListener
	
	/** Layout components */
	private void makeLayout()
	{
		// start layout
		int w1[] = { 0, 5, 0 };
		int h1[] = { 5, 0, 25, 0, 10};
		
		HIGLayout hl = new HIGLayout(w1, h1);
		hl.setColumnWeight(3, 1);
		hl.setRowWeight(2, 1);
		setLayout(hl);
		
		HIGConstraints c = new HIGConstraints();
		
		add(phase, c.rcwh(2,1,3,1,"lr"));
		add(orderFieldLabel, c.rc(4,1,"l"));
		add(orderField, c.rc(4,3,"lr"));
	}// makeLayout()
	
}// class OrderStatusPanel
