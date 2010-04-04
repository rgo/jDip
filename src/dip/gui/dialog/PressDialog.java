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
import dip.gui.swing.XJScrollPane;

import dip.net.message.PressMessage;
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

import dip.net.message.*;

import java.util.*;
import java.text.*;


public class PressDialog extends XDialog
{
	// i18n constants
	//
	private static final String PD_GETMAIL_ICON 	= "press.button.getmail.icon";
	private static final String PD_WRITEMAIL_ICON 	= "press.button.writemail.icon";
	private static final String PD_REPLYMAIL_ICON 	= "press.button.replymail.icon";
	private static final String PD_PRINTMAIL_ICON 	= "press.button.printmail.icon";
	
	private static final String PD_GETMAIL_LABEL 	= "press.button.getmail.label";
	private static final String PD_WRITEMAIL_LABEL 	= "press.button.writemail.label";
	private static final String PD_REPLYMAIL_LABEL 	= "press.button.replymail.label";
	private static final String PD_PRINTMAIL_LABEL 	= "press.button.printmail.label";
	
	private static final String LABEL_REPLY 	= "press.label.reply";
	private static final String LABEL_SUBJECT 	= "press.label.subject";
	private static final String LABEL_SENDER 	= "press.label.sender";
	private static final String LABEL_DATE 		= "press.label.date";
	private static final String LABEL_PHASE 	= "press.label.phase";
	private static final String LABEL_FROM 		= "press.label.from";
	private static final String LABEL_TO 		= "press.label.to";
	
	private static final String DIALOG_TITLE = "press.dialog.title";
		
	// column headers
	private final static String[] HEADERS = {
		Utils.getLocalString(LABEL_REPLY),
		Utils.getLocalString(LABEL_SUBJECT),
		Utils.getLocalString(LABEL_SENDER),
		Utils.getLocalString(LABEL_DATE),
		Utils.getLocalString(LABEL_PHASE)
	};
	
	
	// instance variables
	//
	private JToolBar toolBar;
	private JSplitPane splitPane;
	private JTable msgTable;
	private MessagePanel msgPanel;		
	private SortedPressTM tableModel;
	private Dimension optBtnSize = new Dimension();
	
	private JButton getMail;
	private JButton printMail;
	private JButton writeMail;
	private JButton replyMail;
	
	
	public static void main(String[] adsf)
	{
		JFrame frame = new JFrame("test");
		frame.setVisible(true);
		displayDialog(frame);
		
		
		
		
		
	}
	
	public static void displayDialog(JFrame cf)
	{
		PressDialog pd = new PressDialog(cf);
		pd.setup();
		pd.pack();
		pd.setVisible(true);
	}// showPressDialog()
	
	
	
	private PressDialog(JFrame cf)
	{
		super(cf, Utils.getLocalString(DIALOG_TITLE), false);
		
		// create toolBar
		toolBar = new JToolBar();
		
		// create empty message window
		msgPanel = new MessagePanel();
		
		// create splitpane
		splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
	}// PressDialog()
	
	
	
	/** Setup and Layout the dialog. Could be made protected for extensibility. */
	private void setup()
	{
		// toolbar button setup
		getMail = makeButton(PD_GETMAIL_LABEL, PD_GETMAIL_ICON);
		printMail = makeButton(PD_PRINTMAIL_LABEL, PD_PRINTMAIL_ICON);
		writeMail = makeButton(PD_WRITEMAIL_LABEL, PD_WRITEMAIL_ICON);
		replyMail = makeButton(PD_REPLYMAIL_LABEL, PD_REPLYMAIL_ICON);
		
		// optimally size buttons
		getMail.setMinimumSize(optBtnSize);
		getMail.setMaximumSize(optBtnSize);
		printMail.setMinimumSize(optBtnSize);
		printMail.setMaximumSize(optBtnSize);
		writeMail.setMinimumSize(optBtnSize);
		writeMail.setMaximumSize(optBtnSize);
		replyMail.setMinimumSize(optBtnSize);
		replyMail.setMaximumSize(optBtnSize);
		
		// toolBar setup
		toolBar.setMargin(new Insets(5,5,5,5));
		toolBar.setFloatable(false);
		toolBar.setRollover(true);
		toolBar.add(getMail);
		toolBar.addSeparator();
 		toolBar.add(printMail);
		toolBar.addSeparator();
		toolBar.add(writeMail);
		toolBar.addSeparator();
		toolBar.add(replyMail);
		
		// table setup
		setupTable();
		JScrollPane tableScroll = new XJScrollPane(msgTable);
		tableScroll.getViewport().setBackground(msgTable.getBackground());
		
		// splitPane setup
		splitPane.setOneTouchExpandable(true); 
		splitPane.setTopComponent(tableScroll);
		splitPane.setBottomComponent(msgPanel);
		splitPane.setResizeWeight(0.5f);
		
		// layout
		JPanel mainPanel = new JPanel(new BorderLayout());
		mainPanel.add(toolBar, BorderLayout.NORTH);
		mainPanel.add(splitPane, BorderLayout.CENTER);
		setContentPane(mainPanel);
		pack();
	}// setup()
	
	/** Make a toolbar button */
	private JButton makeButton(String i18nKey, String i18nIconKey)
	{
		JButton button = new JButton(Utils.getLocalString(i18nKey));
		button.setIcon(Utils.getIcon(Utils.getLocalString(i18nIconKey)));
		button.setHorizontalTextPosition(SwingConstants.CENTER);
		button.setHorizontalAlignment(SwingConstants.CENTER);
		button.setVerticalTextPosition(SwingConstants.BOTTOM);
		button.setVerticalAlignment(SwingConstants.CENTER);
		button.setMargin( new Insets(0, 0, 0, 0) );
		
		Dimension size = button.getPreferredSize();
		optBtnSize.height = (size.height > optBtnSize.height) ? size.height : optBtnSize.height;
		optBtnSize.width = (size.width > optBtnSize.width) ? size.width : optBtnSize.width;
		
		return button;
	}// makeButton()
	
	
	/** Setup the Message Header Table. Could be made protected for extensibility. */
	private void setupTable()
	{
		tableModel = new SortedPressTM();
		
		msgTable = new JTable(tableModel);
		msgTable.setDragEnabled(false);
		msgTable.setRowSelectionAllowed(true);
		msgTable.setColumnSelectionAllowed(false);
		msgTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		msgTable.setShowGrid(false);
		msgTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		
		PMCellRenderer cellRenderer = new PMCellRenderer(msgTable);
		msgTable.setDefaultRenderer(Object.class, cellRenderer);
		
		SortHeaderRenderer renderer = new SortHeaderRenderer();
		TableColumnModel model = msgTable.getColumnModel();
		
		for(int i=0; i<HEADERS.length; i++)
		{
			model.getColumn(i).setHeaderRenderer(renderer);
		}
		
		final JTableHeader header = msgTable.getTableHeader();
		header.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent e)
			{
				int col = header.columnAtPoint(e.getPoint());
				tableModel.sortByColumn(col);
			}// mouseClicked()
		});
		
		ListSelectionModel rowSM = msgTable.getSelectionModel();
		rowSM.addListSelectionListener(new ListSelectionListener()
		{
			public void valueChanged(ListSelectionEvent e)
			{
				//Ignore extra messages.
				if(e.getValueIsAdjusting())
				{
					return;
				}
				
				ListSelectionModel lsm = (ListSelectionModel) e.getSource();
				if(lsm.isSelectionEmpty())
				{
					msgPanel.setMessage(null);
				}
				else
				{
					int selectedRow = lsm.getMinSelectionIndex();
					PressMessage pm = tableModel.getRow(selectedRow);
					msgPanel.setMessage(pm);
				}
			}
		});

		
		
		
		
		System.out.println("TESTING: adding example data");
		
		Phase phase = new Phase(Phase.SeasonType.FALL, 1914, Phase.PhaseType.MOVEMENT);
		
		tableModel.addMessage(new PMT(
			"Hello there",
			"this is the body of the message",
			new MID("user","user"),
			new MID("hello","kitty"),
			phase,
			true,
			false
		));
		
		phase = phase.getNext();
		tableModel.addMessage(new PMT(
			"Hello there, assface",
			"<html>This one is in HTML! <b>see what i mean?</b><p><u>this is the body of the message</u>",
			new MID("hello","kitty"),
			new MID("user","user"),
			phase,
			true,
			true
		));
		
		phase = phase.getNext();
		tableModel.addMessage(new PMT(
			"YO! Hello there",
			"this is the body of the message\nline\nline\nline\nEND",
			new MID("fred","fred@fredo.com"),
			new MID("jon","johnson@johnson.com"),
			phase,
			false,
			true
		));
		
		// test
		msgPanel.setMessage(new PMT(
			"Hello there, assface",
			"this is the body of the message.\n How do you like it? \n I thought so.",
			new MID("hello","kitty"),
			new MID("user","user"),
			phase,
			true,
			true
		));
		
		System.out.println("END of example data");
		
    }// setupTable()
    
	// TESTING ONLY
	private class PMT implements PressMessage
	{
		String subj;
		String body;
		MID from;
		MID to;
		Phase phase;
		boolean isReplied;
		boolean isRead;
		
		public PMT(String subj, String body, MID from, MID to, Phase phase, boolean isReplied, boolean isRead)
		{
			this.subj = subj;
			this.body = body;
			this.from = from;
			this.to = to;
			this.phase = phase;
			this.isReplied = isReplied;
			this.isRead = isRead;
		}
		
		
		/** Message sender. May never be null. */
		public MID getFrom() { return from; }
		
		/** Message recipients. Never null. May be zero-length if broadcast. */
		public MID[] getTo() { return new MID[] {to, to}; }
		
		/** Message subject. May be null if there is no subject. */
		public String getSubject() { return subj; }
		
		/** Message body. Never null; may be empty (""). */
		public String getMessage() { return body; }
		
		/** 
		*	Phase during which message was sent. 
		*	May be null if sent before a game has started, or after
		*	a game has ended.
		*/
		public Phase getPhase() { return phase; }
		
		/** Time when message arrived. 0 if unknown. */
		public long getTimeReceived() { return System.currentTimeMillis(); }
		
		/** Time when message was sent. 0 if unknown. */
		public long getTimeSent()  { return System.currentTimeMillis(); }
		
		/** True if this message has been read */
		public boolean isRead() { return isRead; }
		
		/** True if this message has been replied to */
		public boolean isRepliedTo() { return isReplied; }
		
		/** Set whether this message has been read */
		public void setRead(boolean value) {}
		
		/** Set whether this message has been replied to */
		public void setRepliedTo(boolean value){}		
	}
	// END TESTING
	
	
	/** 
	*	Inner TableModel class that takes PressMessages 
	*	columnizes them, and handles sorting. It is not
	*	editable.
	*	<p>
	*	Columns are:
	*	<ol>
	*		<li>Reply-indicator: Boolean</li>
	*		<li>Subject: String</li>
	*		<li>Sender: MID</li>
	*		<li>Date/Time: Long</li>
	*		<li>Phase: Phase</li>
	*	</ol>
	*
	*/
 	private class SortedPressTM extends AbstractTableModel 
	{
		private final int NUM_COLS = 5; 
		private final java.util.List data;
		private int lastSortedCol = -1;
		private boolean isLastSortedAscending = false;
		
		/** Create a Sorted Press Table Model */
		public SortedPressTM() 
		{   
			data = Collections.synchronizedList(new ArrayList(50)); 
		}// SortedPressTM
		
		/** Add a single PressMessage. */
		public void addMessage(PressMessage pm)
		{
			data.add(pm);
			resort();
		}// addMessage()
		
		/** Add multiple PressMessages */
		public void addMessages(PressMessage[] pms)
		{
			for(int i=0; i<pms.length; i++)
			{
				data.add(pms[i]);
			}
			resort();
		}// addMessages()
		
		
		/** Remove a single PressMessage */
		public void removeMessage(PressMessage pm)
		{
			data.remove(pm);
			resort();
		}// removeMessage()
		
		
		/** Remove multiple PressMessages */
		public void removeMessages(PressMessage[] pms)
		{
			data.removeAll( Arrays.asList(pms) );
			resort();
		}// removeMessages()
		
		/** Get the PressMessage for a Row */
		public PressMessage getRow(int row)
		{
			return (PressMessage) data.get(row);
		}// getRow()
		
		/** Get the value at the corresponding row and column. */
		public Object getValueAt(int row, int col)
		{
			PressMessage pm = (PressMessage) data.get(row);
			
			switch(col)
			{
				case 0:
					return Boolean.valueOf(pm.isRepliedTo());
				
				case 1: 
					return pm.getSubject();
				
				case 2:
					return pm.getFrom();
					
				case 3:
					return new Date(pm.getTimeSent());
					
				case 4:
					return pm.getPhase();
					
				default:
					throw new IllegalArgumentException("Invalid column: "+col);
			}
		}// getValueAt()
		
		
		/** Return the column name */
		public String getColumnName(int column) 
		{
			return HEADERS[column];
		}// getColumnName()
		
		
		/** Always returns Object.class so we can use our fancy cell renderer */
		public Class getColumnClass(int columnIndex)
		{
			return Object.class;
		}// getColumnClass()
		
		/** Returns the number of rows */
		public int getRowCount()
		{
			return data.size();
		}// getRowCount()
		
		
		/** Returns the number of columns (constant) */
		public int getColumnCount()
		{
			return NUM_COLS;
		}// getColumnCount()
		
		/** 
		*	Returns the currently sorted column, or -1 if
		*	no column is being sorted.
		*/
		public int getCurrentSortedColumn()
		{
			return lastSortedCol;
		}// getCurrentSortedColumn()
		
		/** Returns the currently-sorted-column direction */
		public boolean isCurrentSortAscending()
		{
			return isLastSortedAscending;
		}// isCurrentSortAscending()
		
		/** 
		*	Resort the table, according the the last sorted
		*	column and order (ascending/descending).
		*/
		public void resort()
		{
			if(lastSortedCol >= 0)
			{
				sortByColumn(lastSortedCol, isLastSortedAscending);   
			}
		}// resort()
		
		
		/** 
		*	Sort the table by the given column, but determines the
		*	direction all by itself.
		*/
		public void sortByColumn(int column)
		{
			if(column < 0 || column >= getColumnCount())
			{
				return;
			}
			
			if(lastSortedCol == column)
			{
				isLastSortedAscending = !isLastSortedAscending;
				sort(column, isLastSortedAscending);
			}
			else
			{
				// new sort; make it ascending
				isLastSortedAscending = true;
				lastSortedCol = column;
				sort(column, isLastSortedAscending);
			}
		}// sortByColumn()
		
		
		/** 
		*	Sort the table by the given column, in the given direction.
		*	This assumes the given column is, in fact, Comparable or 
		*	sortable. If it is not, this will do nothing.
		*/
		public void sortByColumn(int column, boolean isAscending)
		{
			lastSortedCol = column;
			isLastSortedAscending = isAscending;
			sort(column, isAscending);   
		}// sortByColumn()
		
		
		/** Sort implementation */
		private void sort(int col, boolean isAscending)
		{
			Comparator comparator = new PressMessageComparator(col, isAscending);
			Collections.sort(data, comparator);
		}// sort()
		
	}// inner class SortedPressTM
	
	
	/** Render the sorted columns with an up/down arrow. */
	private class SortHeaderRenderer extends DefaultTableCellRenderer
	{
		public Component getTableCellRendererComponent(JTable table, Object value, 
			boolean isSelected, boolean hasFocus, int row, int column)
		{
			JLabel c = (JLabel) super.getTableCellRendererComponent(table, 
					value, isSelected, hasFocus, row, column);
			
			final JTableHeader header = table.getTableHeader();
			
			c.setHorizontalAlignment(SwingConstants.CENTER);
			c.setHorizontalTextPosition(SwingConstants.LEFT);
			c.setForeground(header.getForeground());
			c.setBackground(header.getBackground());
			c.setFont(header.getFont());			
			c.setBorder(UIManager.getBorder("TableHeader.cellBorder"));
			c.setBackground(UIManager.getColor("control"));
			
			if(column == tableModel.getCurrentSortedColumn())
			{
				// paint an arrow, in the proper direction
				if(tableModel.isCurrentSortAscending())
				{
					setIcon(BevelArrowIcon.UP);
				}
				else
				{
					setIcon(BevelArrowIcon.DOWN);
				}
			}
			else
			{
				// paint NO arrow
				c.setIcon(null);
			}
			
			return c;
		}// getTableCellRendererComponent()
		
	}// inner class SortHeaderRenderer
	
	/**
	*	Table Cell Renderer
	*	handles read/unread message flag
	*	can do more, too...
	*/
	private class PMCellRenderer extends DefaultTableCellRenderer
	{
		private final Font boldFont;
		private final DateFormat dtInstance;
		
		/** Create the PMCellRenderer */
		public PMCellRenderer(JTable table)
		{
			boldFont = table.getFont().deriveFont(Font.BOLD);
			dtInstance = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);
		}// PMCellRenderer()
		
		/** Render. Assumes tableModel is a SortedPressTM, and knows the columns, too. */
		public Component getTableCellRendererComponent(JTable table, Object value,
                 boolean isSelected, boolean hasFocus, int row, int column) 
		{
			PressMessage pm = ((SortedPressTM) table.getModel()).getRow(row);
			
			// for when we add color
			Color fgColor = null;
			Color bgColor = null;
			
			if(isSelected)
			{
				setForeground((fgColor == null) ? table.getForeground() : fgColor);
				setBackground(table.getSelectionBackground());
			}
			else
			{
				setForeground((fgColor == null) ? table.getForeground() : fgColor);
				setBackground((bgColor == null) ? table.getBackground() : bgColor);
			}
			
			// set Boldness if read or unread. 
			setFont( (pm.isRead()) ? table.getFont() : boldFont );
			
			// set text
			setTextValue(value);
			
			return this;
		}// getTableCellRendererComponent()
		
		/** Handle any null values and special objects. */
		private void setTextValue(Object value)
		{
			String text = "";
			
			if(value instanceof Date)
			{
				// format date
				text = dtInstance.format((Date) value);
			}
			else if(value instanceof Boolean)
			{
				if(value == Boolean.TRUE)
				{
					text = " + ";
				}
			}
			else
			{
				text = (value != null) ? value.toString() : text;
			}
			
			setText(text);
		}// setTextValue()
		
	}// inner class PMCellRenderer
	
	
	
	/** Generic Reversible Comparator, with null handling. */
	private class PressMessageComparator implements Comparator
	{
		private int col = -1;
		private boolean isAscending = false;
		
		public PressMessageComparator(int column, boolean isAscending)
		{
			this.col = column;
			this.isAscending = isAscending;
		}// PressMessageComparator()
		
		/** Compare! */
		public int compare(Object o1, Object o2)
		{
			int val = 0;
			
			PressMessage pm1 = (PressMessage) o1;
			PressMessage pm2 = (PressMessage) o2;
			
			Object cmp1 = null;
			Object cmp2 = null;
			
			switch(col)
			{
				case 0:
					// special boolean handling
					if(pm1.isRepliedTo() == pm2.isRepliedTo())
					{
						val =  0;
					}
					else if(pm1.isRepliedTo())
					{
						val = 1;
					}
					else
					{
						val = -1;
					}
					
					if(!isAscending)
					{
						val = -val;
					}
					
					return val;
				case 1:
					cmp1 = pm1.getSubject();
					cmp2 = pm2.getSubject();
					break;
				case 2:
					cmp1 = pm1.getFrom().getNick();
					cmp2 = pm2.getFrom().getNick();
					break;
				case 3:
					cmp1 = new Date(pm1.getTimeSent());
					cmp2 = new Date(pm2.getTimeSent());
					break;
				case 4:
					cmp1 = pm1.getPhase();
					cmp2 = pm2.getPhase();
					break;
				default:
					throw new IllegalStateException();
			}
			
			// handle nulls
			if(cmp1 == null && cmp2 == null) 
			{
				val = 0; 
			}
			else if(cmp1 == null)
			{
				val = -1; 
			} 
			else if(cmp2 == null)
			{ 
				val = 1; 
			}
			else
			{
				val = ((Comparable) cmp1).compareTo(cmp2);
			}
			
			if(!isAscending)
			{
				val = -val;
			}
			
			return val;
		}// compare()
		
		/** 
		*	Indicates if the *Comparator* is equal to another Comparator
		*/
		public boolean equals(Object obj)
		{
			return false;	// for safety.
		}// equals()
	}// inner class PressMessageComparator()
	
	/** Draws a beveled arrow as an Icon */
	private static class BevelArrowIcon implements Icon
	{
		/** UP icon */
  		public static final BevelArrowIcon UP = new BevelArrowIcon(true);
  		/** DOWN icon */
		public static final BevelArrowIcon DOWN = new BevelArrowIcon(false);
		
		private static final int DEFAULT_SIZE = 11;
		
		private Color edge1;
		private Color edge2;
		private Color fill;
		private int size;
		private boolean isUp;
		
		/** Create a BevelArrowIcon */
		public BevelArrowIcon(boolean isUp)
		{
			this.edge1 = UIManager.getColor("controlHighlight");
			this.edge2 = UIManager.getColor("controlShadow");
			this.fill = UIManager.getColor("control");
			this.size = DEFAULT_SIZE;
			this.isUp = isUp;
		}
		
		/** Paint the Icon */
		public void paintIcon(Component c, Graphics g, int x, int y)
		{
			if(isUp)
			{
				drawDownArrow(g, x, y);
			}
			else
			{
				drawUpArrow(g, x, y);
			}
		}// paintIcon()
		
		/** Width */
		public int getIconWidth()
		{
			return size;
		}// getIconWidth()
		
		/** Height */
		public int getIconHeight()
		{
			return size;
		}// getIconHeight()
		
		/** Down-arrow */
		private void drawDownArrow(Graphics g, int xo, int yo)
		{
			g.setColor(edge1);
			g.drawLine(xo, yo,   xo+size-1, yo);
			g.drawLine(xo, yo+1, xo+size-3, yo+1);
			g.setColor(edge2);
			g.drawLine(xo+size-2, yo+1, xo+size-1, yo+1);
			
			int x = xo+1;
			int y = yo+2;
			int dx = size-6;      
			
			while (y+1 < yo+size)
			{
				g.setColor(edge1);
				g.drawLine(x, y,   x+1, y);
				g.drawLine(x, y+1, x+1, y+1);
				
				if (0 < dx) 
				{
					g.setColor(fill);
					g.drawLine(x+2, y,   x+1+dx, y);
					g.drawLine(x+2, y+1, x+1+dx, y+1);
				}
				
				g.setColor(edge2);
				g.drawLine(x+dx+2, y,   x+dx+3, y);
				g.drawLine(x+dx+2, y+1, x+dx+3, y+1);
				x += 1;
				y += 2;
				dx -= 2;     
			}
			g.setColor(edge1);
			g.drawLine(xo+(size/2), yo+size-1, xo+(size/2), yo+size-1); 
		}// drawDownArrow()
		
		private void drawUpArrow(Graphics g, int xo, int yo)
		{
			g.setColor(edge1);
			int x = xo+(size/2);
			g.drawLine(x, yo, x, yo); 
			x--;
			int y = yo+1;
			int dx = 0;
			
			while (y+3 < yo+size)
			{
				g.setColor(edge1);
				g.drawLine(x, y,   x+1, y);
				g.drawLine(x, y+1, x+1, y+1);
				
				if (0 < dx)
				{
					g.setColor(fill);
					g.drawLine(x+2, y,   x+1+dx, y);
					g.drawLine(x+2, y+1, x+1+dx, y+1);
				}
				
				g.setColor(edge2);
				g.drawLine(x+dx+2, y,   x+dx+3, y);
				g.drawLine(x+dx+2, y+1, x+dx+3, y+1);
				x -= 1;
				y += 2;
				dx += 2;     
			}
			
			g.setColor(edge1);
			g.drawLine(xo, yo+size-3,   xo+1, yo+size-3);
			g.setColor(edge2);
			g.drawLine(xo+2, yo+size-2, xo+size-1, yo+size-2);
			g.drawLine(xo, yo+size-1, xo+size, yo+size-1);
		}// drawUpArrow()
	}// inner class BevelArrowIcon	
	
	
	/** 
	*	Display a Message 
	*	<p>
	*	Once created, the message can be changed via setMessage().
	*	setMessage(null) will clear the panel.
	*/
	private class MessagePanel extends JPanel
	{
		private JEditorPane body;
		
		private JTextField subject;
		private JTextField from;
		private JTextField date;
		private JTextField to;
		
		
		public MessagePanel()
		{
			JPanel top = setupTop();
			setupBody();
			
			JScrollPane jsp = new XJScrollPane(body);
			jsp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER); 
			
			setLayout(new BorderLayout());
			
			add(top, BorderLayout.NORTH);
			add(jsp, BorderLayout.CENTER);
		}// MessagePanel()
		
		
		public void setMessage(PressMessage pm)
		{
			if(pm == null)
			{
				subject.setText("");
				from.setText("");
				date.setText("");
				to.setText("");
				body.setText("");
			}
			else
			{
				// subject
				subject.setText(pm.getSubject());
				
				// from
				from.setText(pm.getFrom().getNickAndName());
				
				// to (may be multiple); separate by commas.
				// format: nick (name), nick2 (name2). Omit name if null.
				// if none, indicate broadcast.
				//
				if(pm.getTo().length > 0)
				{
					StringBuffer sb = new StringBuffer(128);
					for(int i=0; i<pm.getTo().length; i++)
					{
						MID mid = pm.getTo()[i];
						
						// print nick+name
						sb.append(mid.getNickAndName());
						
						// append a comma+space, if not the last.
						if(i < pm.getTo().length - 1)
						{
							sb.append(", ");
						}
					}
					
					to.setText(sb.toString());
				}
				else
				{
					to.setText("All Players [Broadcast]");	// I18N this!!!!
				}
				
				// set date and phase
				StringBuffer dt = new StringBuffer(80);
				if(pm.getTimeSent() != 0)
				{
					dt.append(DateFormat.getDateTimeInstance(DateFormat.SHORT, 
						DateFormat.SHORT).format(new Date(pm.getTimeSent())));
				}
				
				dt.append(" ");
				if(pm.getPhase() != null)
				{
					dt.append("(");
					dt.append(pm.getPhase().getSeasonType());
					dt.append(" ");
					dt.append(pm.getPhase().getYearType());
					dt.append(", ");
					dt.append(pm.getPhase().getPhaseType());
					dt.append(")");
				}
				else
				{
					dt.append("(sent before or after game)"); // i18n this too!!
				}
				date.setText(dt.toString());
				
				
				// set body content type depending upon if starts with <html>
				// or not.
				String msg = pm.getMessage();
				if(msg.startsWith("<html>") || msg.startsWith("<HTML>"))
				{
					body.setContentType("text/html");
				}
				else
				{
					body.setContentType("text/plain");
				}
				
				body.setText(pm.getMessage());
			}
		}// setMessage()
		
		
		/** Basic layout of top panel */
		private JPanel setupTop()
		{
			JPanel top = new JPanel();
			final int w1[] = { 20, 0, 5, 0, 5 };
			final int h1[] = { 5, 0, 0, 0, 0, 5 };
			
			HIGLayout hl = new HIGLayout(w1, h1);
			hl.setColumnWeight(4, 1);
			top.setLayout(hl);
			
			HIGConstraints c = new HIGConstraints();
			
			// add ":" after labels
			top.add(new JLabel(Utils.getLocalString(LABEL_SUBJECT)+":"), c.rcwh(2,2,1,1,"r"));
			top.add(new JLabel(Utils.getLocalString(LABEL_FROM)+":"), c.rcwh(3,2,1,1,"r"));
			top.add(new JLabel(Utils.getLocalString(LABEL_DATE)+":"), c.rcwh(4,2,1,1,"r"));
			top.add(new JLabel(Utils.getLocalString(LABEL_TO)+":"), c.rcwh(5,2,1,1,"r"));
			
			subject = makeTextField();
			from = makeTextField();
			date = makeTextField();
			to = makeTextField();
			
			// subject font is always bold.
			subject.setFont( subject.getFont().deriveFont(Font.BOLD) );
			
			top.add(subject, c.rcwh(2,4,1,1,"l"));
			top.add(from, c.rcwh(3,4,1,1,"l"));
			top.add(date, c.rcwh(4,4,1,1,"l"));
			top.add(to, c.rcwh(5,4,1,1,"l"));
			
			return top;
		}// setupTop()
		
		private void setupBody()
		{
			body = new JEditorPane();
			body.setEditable(false);
		}// setupBody()
		
		private JTextField makeTextField()
		{
			JTextField jtf = new JTextField(40);
			jtf.setBackground(UIManager.getColor("Label.text"));
			jtf.setEditable(false);
			jtf.setBorder(null);
			return jtf;
		}// makeTextField()
		
	}// inner class MessagePanel
	
}// class PressDialog
