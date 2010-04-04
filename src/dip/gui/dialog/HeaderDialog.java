//
//  @(#)HeaderDialog.java	1.00	4/1/2002
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

import dip.gui.swing.*;
import dip.misc.Utils;
import dip.gui.*;

// HIGLayout
import cz.autel.dmi.HIGConstraints;
import cz.autel.dmi.HIGLayout;

import java.awt.Container;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Graphics;
import java.awt.Paint;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.geom.Line2D;
import java.awt.Insets;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Component;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.AbstractButton;
import javax.swing.JSeparator;
import javax.swing.BoxLayout;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JComponent;
import javax.swing.border.EtchedBorder;
import javax.swing.event.*;
import javax.swing.text.Document;
import javax.swing.text.html.HTMLDocument;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.BorderFactory;


/**
*	A Dialog with an HTML message header at the top, 
*	which may be of fixed height (or adjust to fit).
*	
*	<p>
*	A configurable lower bar (usually for buttons)
*	is also available. Components other than buttons
*	may be added to this lower bar. Any component that
*	implements JButton can be used as a default button,
*	and/or close button. Components are added from the
*	left to right.
*
*	<p>
*	All buttons <b>must</b> have an action command set
*	if they call the close() method. Buttons created with
*	the convenience methods will have an action command set.
*
*/
public class HeaderDialog extends XDialog
{
	/** Default margin between button bar edge and components */
	public final static int BTN_BAR_EDGE = 5;
	/** Default spacint between buttun bar buttons */
	public final static int BTN_BAR_BETWEEN = 10;
	
	/** OK Button Action Command; constant across languages */
	public final static String ACTION_OK = "ACTION_OK";	
	/** Close Button Action Command; constant across languages */
	public final static String ACTION_CLOSE = "ACTION_CLOSE";	
	/** Cancel Button Action Command; constant across languages */
	public final static String ACTION_CANCEL = "ACTION_CANCEL";	
	/** Accept button Action Command; constant across languages */
	public final static String ACTION_ACCEPT = "ACTION_ACCEPT";	
	
	
	private static JButton	sizerButton = new JButton(TEXT_CANCEL);		// used to size buttons
	
	private String	 		defaultCloseButtonAction = null;
	private String	 		returnedAction = null;
	private JPanel			btnPanel = null;
	private JComponent		separator = null;
	private Container		content = new JPanel();
	private JPanel 			btnPanelHolder = null;	// holds btnPanel, and separator (if present)
	private ArrayList		btnList = null;
	protected JEditorPane 	header = null;
	
	
	
	/** Create a Dialog with a HTML-aware Header */
	public HeaderDialog(JFrame parent, String title, boolean isModal)
	{
		super(parent, title, isModal);
		
		// create main components
		header = new GradientXJEditorPane();
		btnPanelHolder = new JPanel(new BorderLayout());
		btnPanel = new JPanel();
		btnPanel.setLayout( new BoxLayout(btnPanel, BoxLayout.X_AXIS) );
		btnPanel.setBorder(BorderFactory.createEmptyBorder(BTN_BAR_EDGE,BTN_BAR_EDGE,BTN_BAR_EDGE,BTN_BAR_EDGE));
		btnList = new ArrayList();
		
		// do layout
		makeLayout();
	}// HeaderDialog()
	
	
	/** 
	*	Set the dialog content; this may be 
	*	called at any time.
	*/
	public void setContentPane(Container container)
	{
		if(container == null) { throw new IllegalArgumentException(); }
		
		Container contentPanel = super.getContentPane();
		
		// remove old content
		contentPanel.remove(content);
		
		// add new content
		content = container;
		contentPanel.add(content, BorderLayout.CENTER);
	}// setContentPane()
	
	
	/** Get the content panel */
	public Container getContentPane()
	{
		return content;
	}// getContentPane()
	
	
	/** Add a component to the button bar (left to right) */
	public void addToButtonPanel(Component component)
	{
		btnPanel.add(component);
		
		if(component instanceof JButton)
		{
			btnList.add(component);
			equalizeButtons();
		}
	}// addToButtonPanel()
	
	
	/** Set the button that is focus-selected by default */
	public void setDefaultButton(JButton btn)
	{
		if(btn == null) { throw new IllegalArgumentException(); }
		getRootPane().setDefaultButton(btn);
	}// setDefaultReturn()
	
	
	/** Get the default button */
	public JButton getDefaultButton()
	{
		return getRootPane().getDefaultButton();
	}// getDefaultButton()
	
	/** 
	*	Get the JButton of the given index; throws an 
	*	exception if index is out of bounds. 
	*	<p>
	*	Index 0 is the leftmost button.
	*/
	public JButton getButton(int i)
	{
		return (JButton) btnList.get(i);
	}// getButton()
	
	/** 
	*	Sets the JButton that the close(actionCommand) 
	*	method will have as its argument.
	*/
	public void setDefaultCloseButton(JButton btn)
	{
		if(btn == null) { throw new IllegalArgumentException(); }
		defaultCloseButtonAction = btn.getActionCommand();
	}// setDefaultCloseValue()
	
	
	/**
	*	Called when the dialog is closing. Subclasses should
	*	subclass this method instead of close(); to close
	*	the dialog, subclasses 
	*/
	protected void close(String actionCommand)
	{
		returnedAction = actionCommand;
		super.close();
	}// close()
	
	
	/**
	*	Do not override this method!
	*/
	protected final void close()
	{
		close(defaultCloseButtonAction);
	}// close()
	
	
	
	/** Make an "OK" button (i18n); calls close with self. */
	public JButton makeOKButton()
	{
		return makeButton(TEXT_OK, ACTION_OK, true);
	}// makeOKButton()
	
	
	/** Make an "Cancel" button (i18n); calls close with self. */
	public JButton makeCancelButton()
	{
		return makeButton(TEXT_CANCEL, ACTION_CANCEL, true);
	}// makeCancelButton()
	
	
	/** Make an "Close" button (i18n); calls close with self. */
	public JButton makeCloseButton()
	{
		return makeButton(TEXT_CLOSE, ACTION_CLOSE, true);
	}// makeCloseButton()
	
	
	/** Make an "Accept" button (i18n); calls close with self. */
	public JButton makeAcceptButton()
	{
		return makeButton(TEXT_ACCEPT, ACTION_ACCEPT, true);
	}// makeAcceptButton()
	
	
	/** Convenience method: check if action command is OK or Accept */
	public boolean isOKorAccept(String actionCommand)
	{
		if( actionCommand.equals(ACTION_OK) || 
			actionCommand.equals(ACTION_ACCEPT) )
		{
			return true;
		}
		
		return false;
	}// isOKorAccept()
	
	/** Convenience method: check if action command is Close or Cancel */
	public boolean isCloseOrCancel(String actionCommand)
	{
		if( actionCommand.equals(ACTION_CLOSE) || 
			actionCommand.equals(ACTION_CANCEL) )
		{
			return true;
		}
		
		return false;
	}// isCloseOrCancel()
	
	
	
	/** 
	*	Make a button with the given text; calls close() with self 
	*	if boolean flag is set to 'true'. 
	*/
	public JButton makeButton(String text, String actionCommand, boolean doClose)
	{
		final JButton btn = new JButton(text);
		btn.setMinimumSize(btn.getPreferredSize());
		btn.setActionCommand(actionCommand);
		
		if(doClose)
		{
			btn.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					close(btn.getActionCommand());
				}
			});
		}
		
		return btn;
	}// makeButton()
	
	
	/** 
	*	Enable or Disable a button based upon actionCommand.
	*	Button must have been added to the button panel.
	*	ActionCommand CANNOT be null.
	*/
	public void setButtonEnabled(String actionCommand, boolean enabled)
	{
		if(actionCommand == null)
		{
			throw new IllegalArgumentException();
		}
		
		Iterator iter = btnList.iterator();
		while(iter.hasNext())
		{
			JButton b = (JButton) iter.next();
			if( actionCommand.equals(b.getActionCommand()) )
			{
				b.setEnabled(enabled);
				break;
			}
		}
	}// setButtonEnabled()
	
	
	/** Make a spacer of the desired width in pixels */
	public static Component makeSpacer(int width)
	{
		return Box.createHorizontalStrut(width);
	}// makeSpacer()
	
	
	/** Make a glue component (takes up as much space as possible) */
	public static Component makeGlue()
	{
		return Box.createHorizontalGlue();
	}// makeGlue()
	
	/** 
	*	Convenience method: for dialogs with ONE button, and 
	*	no other components to be added to the button bar, adds
	*	the given button to the button bar, with the appropriate
	*	spacing. It also sets it as the default and default close
	*	button.
	*/
	public void addSingleButton(JButton button)
	{
		addToButtonPanel( makeGlue() );
		addToButtonPanel(button);
		setDefaultButton(button);
		setDefaultCloseButton(button);
	}// addSingleButton()
	
	/** 
	*	Convenience method: for dialogs with TWO buttons, and 
	*	no other components to be added to the button bar, adds
	*	the given buttons to the button bar, with the appropriate
	*	spacing. It also sets it as the default and default close
	*	button, depending upon the boolean value.
	*	<p>
	*	Typically, the "cancel" button is the rightmost, and is
	*	the same as the default dialog close button.
	*/
	public void addTwoButtons(JButton rightMost, JButton leftMost, 
		boolean rightDefault, boolean rightClose)
	{
		addToButtonPanel( makeGlue() );
		addToButtonPanel(leftMost);
		addToButtonPanel(makeSpacer(BTN_BAR_BETWEEN));
		addToButtonPanel(rightMost);
		setDefaultButton((rightDefault) ? rightMost : leftMost);
		setDefaultCloseButton((rightClose) ? rightMost : leftMost);
	}// addTwoButtons()
	
	/**
	*	Adds 3 buttons:<br>
	*		Right	(right-aligned)<br>
	*		Middle	(closer to Right button)<br>
	*		Left	(left-aligned)<br>
	*	Default and Default Close actions are also settable.
	*/
	public void addThreeButtons(JButton left, JButton center, JButton right,
		JButton defaultButton, JButton closeButton)
	{
		addToButtonPanel(left);
		addToButtonPanel( makeGlue() );
		addToButtonPanel(makeSpacer(BTN_BAR_BETWEEN));
		addToButtonPanel(center);
		addToButtonPanel(makeSpacer(BTN_BAR_BETWEEN));
		addToButtonPanel(right);
		setDefaultButton(defaultButton);
		setDefaultCloseButton(closeButton);
	}// addThreeButtons()
	
	/** 
	*	Creates a default border around the given component, 
	*	of BTN_BAR_EDGE size, except at bottom.
	*/
	public void createDefaultContentBorder(JComponent comp)
	{
		comp.setBorder( BorderFactory.createEmptyBorder(BTN_BAR_EDGE,
			BTN_BAR_EDGE, 0, BTN_BAR_EDGE) );
	}// createDefaultContentBorder()
	
	/** 
	*	Show or Hide the separator between the button and content panels. 
	*	No separator is shown by default. Margin values must be 0 or
	*	greater.
	*/
	public void setSeparatorVisible(boolean value, int horizontalMargin, int verticalMargin)
	{
		if(horizontalMargin < 0 || verticalMargin < 0) { throw new IllegalArgumentException(); }
		
		// remove old separator, if any
		if(separator != null)
		{
			btnPanelHolder.remove(separator);
		}
		
		// create and add a new separator
		if(value)
		{
			if(horizontalMargin > 0 || verticalMargin > 0)
			{
				separator = new JPanel(new BorderLayout());
				separator.setBorder(new EmptyBorder(verticalMargin, horizontalMargin, 
					verticalMargin, horizontalMargin));
				separator.add(new JSeparator());
			}
			else
			{
				separator = new JSeparator();
			}
			
			btnPanelHolder.add(separator, BorderLayout.NORTH);
		}
	}// setSeparatorVisible()
	
	
	/** 
	*	Show or hide the header. By default, the header is visible.
	*/
	public void setHeaderVisible(boolean value)
	{
		Container contentPanel = super.getContentPane();
		
		if(value)
		{
			contentPanel.add(header, BorderLayout.NORTH);
		}
		else
		{
			contentPanel.remove(header);
		}
	}// setHeaderVisible()
	
	
	/** Get the returned JButton ActionCommand used to close the dialog. */
	public String getReturnedActionCommand()
	{
		return returnedAction;
	}// getReturnedActionCommand()
	
	
	/** Set the header text */
	public void setHeaderText(String text)
	{
		header.setText(text);
		header.setCaretPosition(0);	
	}// setText()
	
	
	/** Called by the constructor to layout the dialog. */
	private void makeLayout()
	{
		btnPanelHolder.add(btnPanel, BorderLayout.CENTER);
		
		Container contentPanel = super.getContentPane();
		contentPanel.setLayout(new BorderLayout());
		contentPanel.add(header, BorderLayout.NORTH);
		contentPanel.add(content, BorderLayout.CENTER);
		contentPanel.add(btnPanelHolder, BorderLayout.SOUTH);
	}// makeLayout()
	
	
	/** Equalize the size of the buttons; at least as big as 'Cancel' button. */
	private void equalizeButtons()
	{
		Dimension maxPref = sizerButton.getPreferredSize();
		
		Iterator iter = btnList.iterator();
		while(iter.hasNext())
		{
			JButton btn = (JButton) iter.next();
			Dimension size = btn.getPreferredSize();
			maxPref.width = (size.width > maxPref.width) ? size.width : maxPref.width;
			maxPref.height = (size.height > maxPref.height) ? size.height : maxPref.height;
		}
		
		iter = btnList.iterator();
		while(iter.hasNext())
		{
			JButton btn = (JButton) iter.next();
			btn.setPreferredSize(maxPref);
		}
	}// equalizeButtons()
	
	
	/** 
	*	A gradient-shaded background XJEditorPane with a stylish
	*	bottom separator.
	*/
	public static class GradientXJEditorPane extends XJEditorPane
	{
		// header inset (inner-margin)
		private static final int MARGIN = 5;
		
		
		/** Create a GradientXJEditorPane */
		public GradientXJEditorPane()
		{
			super();
			setOpaque(false);
			customize();
		}// GradientXJEditorPane()
		
		
		/** We are not opaque; we will paint the background. */
		public boolean isOpaque()	{ return false; }
		
		/** We are not focusable. */
		public boolean isFocusable()	{ return false; }
		
		
		/** Overridden to provide painting functionality. */
		protected void paintComponent(Graphics g)
		{
			final int width = getWidth();
			final int height = getHeight();
			
			Graphics2D g2d = (Graphics2D) g;
			
			// save old paint.
			Paint oldPaint = g2d.getPaint();
			
			// paint the gradient.
			g2d.setPaint(new GradientPaint(0, 0, 
				UIManager.getColor("TextField.highlight"), 
				width, height, 
				UIManager.getColor("Label.background"), false));
			g2d.fillRect(0, 0, width, height);
			
			// paint the separator
			g2d.setPaint(UIManager.getColor("Separator.foreground"));
			g2d.draw(new Line2D.Float(0,height-2, width,height-2));
			
			g2d.setPaint(UIManager.getColor("Separator.background"));
			g2d.draw(new Line2D.Float(0,height-1, width,height-1));
			
			// restore the original paint
			g2d.setPaint(oldPaint);
			
			// paint foreground
			super.paintComponent(g);
		}// paintComponent()
		
		
		/** jDip-specific setup */
		private void customize()
		{
			setContentType("text/html");
			Document doc = getDocument();
			if(doc instanceof HTMLDocument)
			{
				((HTMLDocument)doc).setBase(Utils.getResourceBase());
			}
		
			setMargin(new Insets(MARGIN, MARGIN, MARGIN, MARGIN));
			setEditable(false);
			setHighlighter(null);
			setSelectedTextColor(null);	// per BugID 4532590
		}// customize()
		
	}// nested class GradientXJEditorPane
	
	
}// class HeaderDialog


