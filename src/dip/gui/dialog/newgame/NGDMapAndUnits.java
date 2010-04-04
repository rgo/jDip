//
//  @(#)NGDMapAndUnits.java		10/2003
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
package dip.gui.dialog.newgame;

import dip.misc.Utils;
import dip.world.variant.*;
import dip.world.*;
import dip.world.variant.data.*;
import dip.gui.ClientFrame;
import dip.gui.swing.*;

import cz.autel.dmi.HIGConstraints;
import cz.autel.dmi.HIGLayout;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.net.*;


/**
*	Allows for the selection of the Variant Map and Units,
*	and Unit options.
*	<p>
*
*/
public class NGDMapAndUnits extends JPanel implements NewGameDialog.NGDTabPane
{
	private static final String TAB_NAME		= "NGDMAU.tab.name";
	private static final String MAP_LABEL		= "NGDMAU.label.map";
	private static final String SYMBOL_LABEL	= "NGDMAU.label.symbol";
	
	private static final int BORDER_10 = 10;
	private static final int BORDER_5 = 5;
	private static final int MAX_ICON_WIDTH = 125;
	private static final int MAX_ICON_HEIGHT = 125;
	private static final int LIST_LABEL_WIDTH = 140;
	private static final int LIST_LABEL_HEIGHT = 140;
	
	private SelectorPanel	mapSelector = null;
	private SelectorPanel	symbolSelector = null;
	
	
	/** Create a Map and Units (symbols) Panel */
	public NGDMapAndUnits()
	{
		// component creation
		mapSelector = new SelectorPanel();
		mapSelector.setLabel(Utils.getLocalString(MAP_LABEL));
		
		symbolSelector = new SelectorPanel();
		symbolSelector.setLabel(Utils.getLocalString(SYMBOL_LABEL));
		
		
		/*
			ListSelectionListener for MapGraphics:
			This will change the SymbolPack to the preferred
			SymbolPack, if any; if there isn't a preferred Symbolpack, 
			then no SymbolPack change is made.
		*/
		mapSelector.addListSelectionListener(new ListSelectionListener()
		{
			public void valueChanged(ListSelectionEvent e)
			{
				if(!e.getValueIsAdjusting())
				{
					ListItem item = mapSelector.getSelectedItem();
					if(item != null)
					{
						MapGraphic mg = (MapGraphic) item.getReference();
						if(mg.getPreferredSymbolPackName() != null)
						{
							symbolSelector.setSelectedItemByName(mg.getPreferredSymbolPackName());
						}
					}
				}
			}// valueChanged()
		});
		
		// fill *after* we add all change listeners
		symbolSelector.setItemsWithSymbolPacks();
		
		// layout
		makeLayout();
	}// NGDMapAndUnits()
	
	
	
	/** Panel Layout */
	private void makeLayout()
	{
		// create unit option panel with checkboxes inside
		//
		JPanel symbolPanel = new JPanel();
		int w1[] = { 50, 0, 0 };		// cols
		int h1[] = { 0, 0, 0, 0 };		// rows
		
		HIGLayout l1 = new HIGLayout(w1, h1);
		l1.setColumnWeight(3, 1);
		l1.setRowWeight(1, 1);
		l1.setRowWeight(4, 1);
		symbolPanel.setLayout(l1);
		
		// main panel layout
		//
		this.setLayout(new GridLayout(2,1,0,15));
		this.setBorder(new EmptyBorder(10,10,10,10));
		this.add(mapSelector);
		this.add(symbolSelector);
	}// makeLayout()
	
	
	
	
	/** Get the tab name. */
	public String getTabName()
	{
		return Utils.getLocalString(TAB_NAME);
	}// getTabName()

	/** The Variant has Changed. */
	public void variantChanged(Variant variant)
	{
		if(variant != null)
		{
			// create the list of map graphics
			mapSelector.setItems(variant, variant.getMapGraphics());
		}
	}// variantChanged()
	
	
	/** The Enabled status has Changed. */
	public void enablingChanged(boolean enabled)
	{
	}// enablingChanged()
	
	
	/** Get the selected Map Graphic */
	public MapGraphic getSelectedMap()
	{
		return (MapGraphic) mapSelector.getSelectedItem().getReference();
	}// getSelectedMap()
	
	
	/** Get the selected SymbolPack */
	public SymbolPack getSelectedSymbolPack()
	{
		return (SymbolPack) symbolSelector.getSelectedItem().getReference();
	}// getSelectedSymbolPack()
	
	
	/** Set the selected Map Graphic */
	public void setSelectedMap(MapGraphic mg)
	{
		final ListItem[] items = mapSelector.getItems();
		if(items != null)
		{
			for(int i=0; i<items.length; i++)
			{
				if(mg == items[i].getReference())
				{
					mapSelector.setSelectedItem(items[i]);
					break;
				}
			}
		}
	}// setSelectedMap()
	
	/** Set the selected Map Graphic */
	public void setSelectedMap(String mgName)
	{
		final ListItem[] items = mapSelector.getItems();
		if(items != null)
		{
			for(int i=0; i<items.length; i++)
			{
				MapGraphic mg = (MapGraphic) items[i].getReference();
				if(mg.getName().equalsIgnoreCase(mgName))
				{
					mapSelector.setSelectedItem(items[i]);
					break;
				}
			}
		}
	}// setSelectedMap()
	
	/** Set the Selected Symbol Pack */
	public void setSelectedSymbolPack(SymbolPack sp)
	{
		final ListItem[] items = symbolSelector.getItems();
		if(items != null)
		{
			for(int i=0; i<items.length; i++)
			{
				if(sp == items[i].getReference())
				{
					symbolSelector.setSelectedItem(items[i]);
					break;
				}
			}
		}
	}// setSelectedSymbolPack()
	
	/** Set the Selected Symbol Pack */
	public void setSelectedSymbolPack(String symbolPackName)
	{
		final ListItem[] items = symbolSelector.getItems();
		if(items != null)
		{
			for(int i=0; i<items.length; i++)
			{
				if( ((SymbolPack) items[i].getReference()).getName().equalsIgnoreCase(symbolPackName) )
				{
					symbolSelector.setSelectedItem(items[i]);
					break;
				}
			}
		}
	}// setSelectedSymbolPack()
	
	
	/**
	*	This creates a class which has a scrollable List of graphical
	*	items with their name underneath, and to the right, a 
	*	detailed description of said item. An additional area remains
	*	underneath the description which can contain other components.
	*/
	private class SelectorPanel extends JPanel implements ListSelectionListener
	{
		private final GradientJLabel 	label;
		private final JList 			list;
		private final JEditorPane		description;
		private final JPanel			underDesc;
		private final JScrollPane		jsp;
		private ListItem[]				items = new ListItem[0];
		
		/** Create a SelectorPanel */
		public SelectorPanel()
		{
			label = new GradientJLabel("");
			
			list = new JList(new ListItem[0]);
			list.setCellRenderer(new GraphicJListCellRenderer());
			list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			list.setVisibleRowCount(1);
			list.addListSelectionListener(this);
			
			description = Utils.createTextLabel(true);
			
			underDesc = new JPanel(new BorderLayout());
			
			jsp = new XJScrollPane(list);
			jsp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			
			makeSPLayout();
		}// SelectorPanel()
		
		/** Add a List Selection Listener */
		public void addListSelectionListener(ListSelectionListener lsl)
		{
			list.addListSelectionListener(lsl);
		}// addListSelectionListener()
		
		/** Set the text of the main label. */
		public void setLabel(String text)
		{
			label.setText(text);
		}// setLabel()
		
		/** Get the selected item. */
		public ListItem getSelectedItem()
		{
			return (ListItem) list.getSelectedValue();
		}// getSelectedItem()
		
		
		/** Set the selected item. */
		public void setSelectedItem(ListItem item)
		{
			list.setSelectedValue(item, true);
		}// setSelectedItem()
		
		
		/** Set the selected index. */
		public void setSelectedIndex(int idx)
		{
			list.setSelectedIndex(idx);
		}// setSelectedIndex()
		
		/** Set the seleted index, by the Label name; case insensitive. */
		public void setSelectedItemByName(String name)
		{
			if(name != null)
			{
				for(int i=0; i<items.length; i++)
				{
					if(items[i].getLabel().equalsIgnoreCase(name))
					{
						setSelectedItem(items[i]);
						break;
					}
				}
			}
		}// setSelectedItemByName()
		
		/** Set the Items */
		public void setItems(ListItem[] items)
		{
			this.items = items;
			list.setListData(items);
		}// setItems()
		
		public ListItem[] getItems()
		{
			return items;
		}// getItems()
		
		
		/** Set the Items (from MapGraphics) */
		public void setItems(final Variant variant, final MapGraphic[] mgs)
		{
			int defaultItem = 0;
			ListItem[] items = new ListItem[mgs.length];
			for(int i=0; i<items.length; i++)
			{
				items[i] = new ListItem(variant, mgs[i]);
				defaultItem = (mgs[i].isDefault()) ? i : defaultItem;
			}
			
			setItems(items);
			setSelectedIndex(defaultItem);
		}// setItems()
		
		
		/** Set the Items, from the available SymbolPacks */
		public void setItemsWithSymbolPacks()
		{
			SymbolPack[] symbolPacks = VariantManager.getSymbolPacks();
			ListItem[] items = new ListItem[symbolPacks.length];
			for(int i=0; i<items.length; i++)
			{
				items[i] = new ListItem(symbolPacks[i]);
			}
			
			setItems(items);
			list.setSelectedValue(items[0], true);
		}// setItemsWithSymbolPacks()
		
		/** Set the 'under-description' panel */
		public void setUnderDescriptionPanel(Component c)
		{
			underDesc.add(c, BorderLayout.CENTER);
		}// setUnderDescriptionPanel()
		
		
		/** layout the SelectorPanel */
		private void makeSPLayout()
		{
			int w1[] = { 0, BORDER_10, 0 };			// cols
			int h1[] = { 0, BORDER_5, 0, 0 };		// rows
			
			HIGLayout l1 = new HIGLayout(w1, h1);
			l1.setColumnWeight(3, 1);
			l1.setRowWeight(4, 1);
			l1.setRowWeight(3, 1);
			
			setLayout(l1);
			HIGConstraints c = new HIGConstraints();
			
			add(label, c.rcwh(1,1,3,1,"lr"));
			add(jsp, c.rcwh(3,1,1,2,"lrtb"));
			add(description, c.rcwh(3,3,1,1,"lrtb"));
			add(underDesc, c.rcwh(4,3,1,1,"lrb"));
		}// makeSPLayout()
		
		/** Receive selection events. */
		public void valueChanged(ListSelectionEvent e)
		{
			// change the description if not adjusting.
			if(!e.getValueIsAdjusting())
			{
				ListItem item = getSelectedItem();
				if(item != null)
				{
					String text = item.getDescription();
					if(text.startsWith("<html>") || text.startsWith("<HTML>"))
					{
						description.setText(text);
					}
					else if(text == null || "".equals(text))
					{
						description.setText("");
					}
					else
					{
						StringBuffer sb = new StringBuffer(text.length() + 48);
						sb.append("<html><font face=\"arial,helvetica,sansserif\">");
						sb.append(text);
						description.setText(sb.toString());
					}
				}
				else
				{
					description.setText("");
				}
			}
		}
		
	}// inner class SelectorPanel
	
	
	/**
	*	A graphical list item. There are two displayed components;
	*	the graphic itself, and the graphic label, which is displayed
	*	beneath the graphic. An additional Object field is available
	*	for storing a reference.
	*/
	private class ListItem
	{
		private final Icon icon;
		private final String label;
		private final String description;
		private Object reference = null;
		
		/** Create a ListItem */
		public ListItem(Icon icon, String label, String description)
		{
			this(icon, label, description, null);
		}// ListItem()
		
		/** Create a ListItem */
		public ListItem(Icon icon, String label, String description, Object ref)
		{
			this.icon = icon;
			this.label = label;
			this.description = description;
			this.reference = ref;
		}// ListItem()
		
		/** Create a ListItem */
		public ListItem(SymbolPack sp)
		{
			// resolve thumbnail URI to load icon
			URL iconURL = VariantManager.getResource(sp, sp.getThumbnailURI());
			ImageIcon ii = null;
			if(iconURL != null)
			{
				ii = new ImageIcon(iconURL);
				if(ii != null)
				{
					if(ii.getIconWidth() > MAX_ICON_WIDTH || ii.getIconHeight() > MAX_ICON_HEIGHT)
					{
						ii = Utils.scaleDown(ii, MAX_ICON_WIDTH, MAX_ICON_HEIGHT);
					}
				}
			}
			
			this.icon = ii;
			this.label = sp.getName();
			this.description = sp.getDescription();
			this.reference = sp;
		}// ListItem()
		
		/** Create a ListItem */
		public ListItem(Variant variant, MapGraphic mg)
		{
			// resolve thumbnail URI to load icon
			URL iconURL = VariantManager.getResource(variant, mg.getThumbnailURI());
			ImageIcon ii = null;
			if(iconURL != null)
			{
				ii = new ImageIcon(iconURL);
				if(ii != null)
				{
					if(ii.getIconWidth() > MAX_ICON_WIDTH || ii.getIconHeight() > MAX_ICON_HEIGHT)
					{
						ii = Utils.scaleDown(ii, MAX_ICON_WIDTH, MAX_ICON_HEIGHT);
					}
				}
			}
			
			this.icon = ii;
			this.label = mg.getName();
			this.description = mg.getDescription();
			this.reference = mg;
		}// ListItem()
		
		/** Get the label */
		public String getLabel()				{ return label; }
		/** Get the icon */
		public Icon getIcon()					{ return icon; }
		/** Get the icon */
		public String getDescription()			{ return description; }
		/** Get the reference */
		public Object getReference()			{ return reference; }
		/** Set the reference */
		public void setReference(Object obj)	{ reference = obj; }
		
	}// inner class ListItem
	
	
	
	/** Graphic cell renderer for JList ListItems */
	private static class GraphicJListCellRenderer extends JLabel implements ListCellRenderer 
	{ 
		private static final Dimension MIN_SIZE = new Dimension(LIST_LABEL_WIDTH, LIST_LABEL_HEIGHT);
		
		public GraphicJListCellRenderer()
		{
			super();
			this.setIconTextGap(3);
			this.setVerticalTextPosition(SwingConstants.BOTTOM);
			this.setHorizontalTextPosition(SwingConstants.CENTER);
			this.setHorizontalAlignment(SwingConstants.CENTER);
			this.setVerticalAlignment(SwingConstants.CENTER);
			this.setOpaque(true);
		}// GraphicJListCellRenderer()
		
		
		public Component getListCellRendererComponent(JList list, Object value, int index,
														boolean isSelected, boolean cellHasFocus) 
		{ 
			ListItem item = (ListItem) value;
			
			setText( item.getLabel() );
			
			if(item.getIcon() != null)
			{
				setIcon( item.getIcon() );
			}
			
			setEnabled(list.isEnabled());
			setFont(list.getFont());
	   		
			if(isSelected)
			{
				setBackground(list.getSelectionBackground());
				setForeground(list.getSelectionForeground());
			}
			else 
			{
				setBackground(list.getBackground());
				setForeground(list.getForeground());
			}
			
			setMinimumSize(MIN_SIZE);
			setPreferredSize(MIN_SIZE);
			
			return this;
		}// getListCellRendererComponent()
		
	}// inner class GraphicJListCellRenderer
	
}// class NGDMapAndUnits

