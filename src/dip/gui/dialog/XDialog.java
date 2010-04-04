//
//  @(#)XDialog.java	1.00	4/1/2002
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

import dip.gui.*;
import dip.misc.Utils;
import dip.misc.Help;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.awt.*;
/**
*	Extended JDialog
*	<p>
*	Features:
*	<ol>
*		<li>Automatically disposes dialog if close-button pressed, although
*			this behavior can be changed by over-riding close()
*		<li>Closes dialog if ESC pressed (calls close())
*		<li>Internationalized button text, by default
*		<li>Button constants
*		<li>Help support
*	</ol>
*	<p>
*	To add a default button, use JRootPane.setDefaultButton(); note that if a 
*	text panel/field is present then this will not work. If a read-only (non-editable)
*	text component is present, it can be sub-classed to avoid receiving any keyboard
*	input (override isFocusable()). See TextViewer for an example.
*/
public class XDialog extends JDialog
{
	// common dialog constants
	
	/** Internationalized button text for "OK" */
	public static final String TEXT_OK = Utils.getLocalString("XDialog.button.ok");
	/** Internationalized button text for "Cancel" */
	public static final String TEXT_CANCEL = Utils.getLocalString("XDialog.button.cancel");
	/** Internationalized button text for "Close" */
	public static final String TEXT_CLOSE = Utils.getLocalString("XDialog.button.close");
	/** Internationalized button text for "Accept" */
	public static final String TEXT_ACCEPT = Utils.getLocalString("XDialog.button.accept");
	
	
	/** Create an XDialog */
	public XDialog()
	{
		super();
	}// XDialog()
	
	/** Create an XDialog */
	public XDialog(Frame owner)
	{
		super(owner );
	}// UniverseJDialog()
	
	/** Create an XDialog */
	public XDialog(Frame owner, String title)
	{
		super(owner, title);
	}// XDialog()
	
	/** Create an XDialog */
	public XDialog(Frame owner, boolean modal)
	{
		super(owner, modal);
	}// XDialog()
	
	/** Create an XDialog */
	public XDialog(Frame owner, String title, boolean modal) 	
	{
		super(owner, title, modal);
	}// XDialog()

	/** Create an XDialog */
	public XDialog(Dialog owner)
	{
		super(owner );
	}// UniverseJDialog()
	
	/** Create an XDialog */
	public XDialog(Dialog owner, String title)
	{
		super(owner, title);
	}// XDialog()
	
	/** Create an XDialog */
	public XDialog(Dialog owner, boolean modal)
	{
		super(owner, modal);
	}// XDialog()
	
	/** Create an XDialog */
	public XDialog(Dialog owner, String title, boolean modal) 	
	{
		super(owner, title, modal);
	}// XDialog()
	
	
	/** Called when closing. By default, calls dispose(). */
	protected void close()
	{
		dispose();
	}// close()	
	
	
	/** Dialog setup, including adding Window-Close listener */
	protected void dialogInit()
	{	
		super.dialogInit();
		
		// install close/close-button handling
		super.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) 
			{
				XDialog.this.close();
			}
		});		
	}// dialogInit()	
	
	
	/** Adds the ESC key listener */
	protected JRootPane createRootPane() 
	{
		JRootPane rootPane = super.createRootPane();
		
		// install ESC key checking.
		ActionListener actionListener = new ActionListener() {
			public void actionPerformed(ActionEvent actionEvent) {
				XDialog.this.close();
			}
		};
		
		KeyStroke strokeESC = KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0);
		
		rootPane.registerKeyboardAction(actionListener, strokeESC, JComponent.WHEN_IN_FOCUSED_WINDOW);
		
		return rootPane;
	}// createRootPane()
	
	
	/** Throws an IllegalArgumentException() */
	public void setDefaultCloseOperation(int operation)
	{
		throw new IllegalArgumentException("override close() instead");
	}// setDefaultCloseOperation()
	
	
	/** 
	*	Set the HelpID (see dip.misc.Help).  If non-null, sets the Window-Level
	*	help for this dialog.
	*/
	public void setHelpID(Help.HelpID helpID)
	{
		Help.enableDialogHelp(this, helpID);
	}// setHelpID()
	
	
	
	
}// class XDialog

