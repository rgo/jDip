//
//  @(#)AssocJComboBox.java		9/2004
//
//  Copyright 2004 Zachary DelProposto. All rights reserved.
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
package dip.gui.swing;

import dip.misc.Utils;

import java.util.Arrays;
import java.util.Comparator;
import java.text.Collator;
import javax.swing.JComboBox;
import javax.swing.ComboBoxModel;

/**
*	This is an Associated JCombobox, which associates a set of objects with
*	a set of labels. The labels can be localized. The get/set methods work with
*	the object set (typically, constant values or the like). 
*	<p>
*	See GeneralPreferencePanel for examples usage.
*	
*	
*/
public class AssocJComboBox extends JComboBox
{
	
	/** Create an AssocJComboBox */
	public AssocJComboBox()
	{
		super();
	}// AssocJComboBox()
	
	/** Create an AssocJComboBox with the given AssociatedObj(s) */
	public AssocJComboBox(final AssociatedObj[] objs)
	{
		super(objs);
		
		// longest is the prototype
		int len = 0;
		AssociatedObj longestAO = null;
		for(int i=0; i<objs.length; i++)
		{
			if(objs[i].getDisplay().length() > len)
			{
				len = objs[i].getDisplay().length();
				longestAO = objs[i];
			}
		}
		
		setPrototypeDisplayValue(longestAO+"M");
	 }// AssocJComboBox()
	
	
	/**
	*	Remove an item, by comparing its AssociatedObj <b>value</b>.
	*/
	public void removeItem(final Object obj) 
	{
		super.removeItem( getAOForValue(obj) );
	}// removeItem()
	
	/** Remove an AssociatedObj */
	public void removeItem(final AssociatedObj ao)
	{
		super.removeItem(ao);
	}// removeItem()
	
	/**
	*	Add an AssociatedObj. Other objects are not allowed to be
	*	added, and will throw an exception.
	*/
	public void addItem(final Object obj)
	{
		if(obj instanceof AssociatedObj)
		{
			super.addItem(obj);
		}
		else
		{
			throw new IllegalArgumentException("Not an AssociatedObj!");
		}
	}// addItem()
	
	
	/** Set to the Default AssociatedObj, or index(0) if none. */
	public void reset()
	{
		AssociatedObj ao = getDefaultAO();
		if(ao == null)
		{
			setSelectedIndex(0);
		}
		else
		{
			setSelectedItem(ao);
		}
	}// reset()
	
	
	/** Get the selected AssociatedObj; null if none selected. */
	public Object getSelectedItem()
	{
		return (AssociatedObj) super.getSelectedItem();
	}// getSelectedItem
	
	
	/** 
	*	Get the selected AssociatedObj value; if null, it uses the
	*	default. If there is no default, it uses the 
	*	first value.
	*/
	public Object getSelectedValue()
	{
		AssociatedObj ao = (AssociatedObj) getSelectedItem();
		if(ao == null)
		{
			ao = getDefaultAO();
			if(ao == null)
			{
				assert (getItemAt(0) != null);
				ao = (AssociatedObj) getItemAt(0);
			}
		}
		
		return ao.getValue();
	}// getSelectedValue()
	
	/** 
	*	Sets the selected item by its AssociatedObj <b>Value</b>; 
	*/
	public void setSelectedItem(Object obj)
	{
		super.setSelectedItem( getAOForValue(obj) );
	}// setSelectedItem()
	
	
	/**
	*	Returns the Default AssociatedObj, if any; otherwise,
	*	returns null.
	*/
	private AssociatedObj getDefaultAO()
	{
		ComboBoxModel model = getModel();
		final int len = model.getSize();
		for(int i=0; i<len; i++)
		{
			AssociatedObj ao = (AssociatedObj) model.getElementAt(i);
			if(ao.isDefault())
			{
				return ao;
			}
		}
		
		return null;
	}// getDefaultAO()
	
	/**
	*	Returns the AssociatedObj for a given value/display
	*	If none, returns null. If an AssociatedObj is passed in,
	*	that same AssociatedObj is returned.
	*/
	private AssociatedObj getAOForValue(Object value)
	{
		// short circuit
		if(value instanceof AssociatedObj)
		{
			return (AssociatedObj) value;
		}
		else if(value != null)
		{
			ComboBoxModel model = getModel();
			final int len = model.getSize();
			for(int i=0; i<len; i++)
			{
				AssociatedObj ao = (AssociatedObj) model.getElementAt(i);
				if(value.equals(ao.getValue()) ||
					value.equals(ao.getDisplay()))
				{
					return ao;
				}
			}
		}
		
		return null;
	}// getAOForValue()
	
	
	/**
	*	Associates a given Value with a given String. This is essentially
	*	a name-value pair, and is useful for localizing internal program
	*	constants (Strings, Enums, ints, etc.) to a value suitable for UI display.
	*	<p>
	*	The Value object needs to have a constant toString() representation
	*	for persitence to work properly.
	*	<p>
	*	Note: this class has a natural ordering that is inconsistent with equals
	*/
	public static class AssociatedObj
	{
		private final String display;
		private final Object obj;
		private final boolean isDefault;
		
		/** Create an AssociatedObj */
		public AssociatedObj(Object obj, String display)
		{
			this(obj, display, false);
		}// AssociatedObj()
		
		/** Create an AssociatedObj */
		public AssociatedObj(Object obj, String display, boolean isDefault)
		{
			if(obj == null || display == null)
			{
				throw new IllegalArgumentException("null obj or display value");
			}
			
			this.obj = obj;
			this.display = display;
			this.isDefault = isDefault;
		}// AssociatedObj()
		
		/** Returns if this is the Default value */
		public boolean isDefault()	{ return isDefault; }
		
		/** Returns the Display String associated with the Value */
		public String getDisplay()  { return display; }
		
		/** Returns the Value */
		public Object getValue()	{ return obj; }
		
		
		/**
		*	Collates (sorts) but only if display values are Strings.
		*	Otherwise, nothing is done.
		*/
		public static void collate(AssociatedObj[] array, Collator c)
		{
			Arrays.sort(array, new AssocObjComparator(c));
		}// collates()
		
		/**
		*	Collate internal display strings
		*/
		private static class AssocObjComparator implements Comparator
		{
			private final Collator c;
			
			public AssocObjComparator(Collator c)
			{
				this.c = c;
			}// AssocObjComparator()
			
			public int compare(Object o1, Object o2)
			{
				String display1 = ((AssociatedObj) o1).getDisplay();
				String display2 = ((AssociatedObj) o2).getDisplay();
				return c.compare(display1, display2);
			}// compare()
			
			public boolean equals(Object obj)
			{
				return c.equals(obj);
			}// equals()
		}// inner class AssocObjComparator
		
		/** Returns getDisplay(), so no JComboBox renderer chane is required. */
		public String toString()
		{
			return getDisplay();
		}// toString()
		
		
		// NO array val can be null
		// default can be null
		// assumes TEXT is defined for key. (could be HTML?)
		// MUST include trailing '.' on prefix if req'd
		// objs must be toString()-able
		public static AssociatedObj[] createAssociatedObjects(final Object[] objs, 
			final String prefix, Object defaultValue, boolean sort)
		{
			if(objs == null || prefix == null)
			{
				throw new IllegalArgumentException();
			}
			
			AssociatedObj[] assocObjs = new AssociatedObj[objs.length];
			for(int i=0; i<assocObjs.length; i++)
			{
				boolean isDefault = objs[i].equals(defaultValue);
				String text = Utils.getLocalString(prefix + objs[i].toString());
				assocObjs[i] = new AssociatedObj(objs[i], text, isDefault);
			}
			
			if(sort)
			{
				Collator collator = Collator.getInstance();
				AssociatedObj.collate(assocObjs, collator);
			}
			
			return assocObjs;
		}// createAssociatedObjects()
		
		// NO array val can be null
		// default can be null
		public static AssociatedObj[] createAssociatedObjects(final Object[] objs,
			final String[] display, Object defaultValue, boolean sort)
		{
			if(objs == null)
			{
				throw new IllegalArgumentException();
			}
			
			if(objs.length != display.length)
			{
				throw new IllegalArgumentException();
			}
			
			AssociatedObj[] assocObjs = new AssociatedObj[objs.length];
			for(int i=0; i<assocObjs.length; i++)
			{
				boolean isDefault = objs[i].equals(defaultValue);
				assocObjs[i] = new AssociatedObj(objs[i], display[i], isDefault);
			}
			
			if(sort)
			{
				Collator collator = Collator.getInstance();
				AssociatedObj.collate(assocObjs, collator);
			}
			
			return assocObjs;
		}// createAssociatedObjects()		
		
		
	}// nested class AssociatedObj
	
	
}// class AssocJComboBox
