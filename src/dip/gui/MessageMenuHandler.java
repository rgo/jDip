//
//  @(#)MessageMenuHandler.java	9/2003
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

import dip.gui.ClientMenu.Item;
import dip.net.message.PressChannel;

import dip.gui.dialog.PressConfigDialog;
import dip.gui.dialog.PressDialog;

import javax.swing.*;

/**
*	The ClientMenu class requires a MessageMenuHandler to support 
*	(and display) the Message Menu. The menu item constants, though,
*	are still defined 
*	
*/
public class MessageMenuHandler
{
	// i18n: item constants
	public static final Item MESSAGE					= new Item("MESSAGE");
	public static final Item MESSAGE_PRESS_WRITE		= new Item("MESSAGE_PRESS_WRITE");
	public static final Item MESSAGE_PRESS_VIEW			= new Item("MESSAGE_PRESS_VIEW");
	public static final Item MESSAGE_PRESS_PROPERTIES	= new Item("MESSAGE_PRESS_PROPERTIES");
	
	
	// instance variables
	private transient PressChannel pressChannel = null;
	private transient ClientFrame clientFrame = null;
	
	/** Create a Message Menu Handler object */
	public MessageMenuHandler(ClientFrame cf)
	{
		this.clientFrame = cf;
	}// MessageMenuHandler()
	
	
	/** 
	*	Bind the MessageMenuHandler to the Client Menu.
	*	This creates or removes the menu and appropriate menu 
	*	items, depending upon what has been set (if no PressChannel
	*	has been set, Press menu options are not displayed, for example).
	*	<p>
	*	Therefore, this should be called only AFTER appropriate 
	*	Press/Message handlers have been set.
	*/
	public synchronized void bind(ClientMenu cm)
	{
		// REMOVE menu / menu items, if they exist.
		JMenu menu = (JMenu) cm.getMenuItem(MESSAGE);
		if(menu != null)
		{
			menu.removeAll();
			cm.getJMenuBar().remove(menu);
			cm.getJMenuBar().validate();
		}
		
		// create menu / items, based on handler settings.
		//
		if(pressChannel != null)
		{
			// create menu / menu items
			menu = cm.makeMenu(MESSAGE, false);
			menu.add(cm.makeMenuItem(MESSAGE_PRESS_WRITE));
			menu.add(cm.makeMenuItem(MESSAGE_PRESS_VIEW));
			menu.add(new JSeparator());
			menu.add(cm.makeMenuItem(MESSAGE_PRESS_PROPERTIES));
			
			// set handlers
			cm.setActionMethod(MESSAGE_PRESS_WRITE, this, "onMsgPressWrite");
			cm.setActionMethod(MESSAGE_PRESS_VIEW, this, "onMsgPressView");
			cm.setActionMethod(MESSAGE_PRESS_PROPERTIES, this, "onMsgPressProperties");
			
			// add to JMenuBar & validate
			cm.getJMenuBar().add(menu,  (cm.getJMenuBar().getMenuCount() - 2));
			cm.getJMenuBar().validate();
		}
	}// bind()
	
	
	/** Set the PressChannel object. Null sets no press channel. */
	public synchronized void setPressChannel(PressChannel channel)
	{
		pressChannel = channel;
	}// setPressChannel()
	
	
	
	
	
	
	
	/** Handler for composing (new) press messages */
	public void onMsgPressWrite()
	{
		assert(pressChannel != null);
		throw new IllegalStateException("PRESS WRITE: NOT YET IMPLEMENTED");
	}// onMsgPressWrite()
	
	
	/** Handler for viewing press messages */
	public void onMsgPressView()
	{
		assert(pressChannel != null);
		PressDialog.displayDialog(clientFrame);
	}// onMsgPressView()
	
	
	/** Handler for viewing press properties */
	public void onMsgPressProperties()
	{
		assert(pressChannel != null);
		PressConfigDialog.displayDialog(clientFrame, pressChannel.getPressConfiguration());
	}// onMsgPressProperties()
	
	
}// class MessageMenuHandler
