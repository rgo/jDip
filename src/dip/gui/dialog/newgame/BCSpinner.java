//
//  @(#)BCSpinner.java		2/2003
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
import dip.world.Phase.YearType;

import javax.swing.AbstractSpinnerModel;
import javax.swing.JFormattedTextField;
import javax.swing.JSpinner;
/**
*	Creates a Year spinner that (if enabled) correctly handles 
*	BC years ('negative' years), and uses YearType objects to
*	increment/decrement. 
*	<p>
*	To disable negative (BC) years, set the minimum value 
*	[setMinimum()] to 1.
*	
*/
public class BCSpinner extends JSpinner
{

	/** Create a BCSpinner */
	public BCSpinner(int initialYear, int minimum, int maximum)
	{
		this(new YearType(initialYear), minimum, maximum);
	}// BCSpinner()
	
	/** Create a BCSpinner */
	public BCSpinner(YearType initialYear, int minimum, int maximum)
	{
		super(new SpinnerYearTypeModel(initialYear, minimum, maximum));
		setEditor(new YearTypeEditor(this));
	}// BCSpinner()
	
	/** Sets the current value */
	public void setValue(int value)
	{
		setValue(new YearType(value));
	}// setValue()
	
	/** Sets the minimum value */
	public void setMinimum(int value)
	{
		((SpinnerYearTypeModel) getModel()).setMinimum(value);
	}// setMinimum()
	
	/** Sets the maximum value */
	public void setMaximum(int value)
	{
		((SpinnerYearTypeModel) getModel()).setMaximum(value);
	}// setMaximum()
	
	/** Type-Safe version of getValue() */
	public YearType getYearTypeValue()
	{
		return (YearType) getValue();
	}// getYearTypeValue()
	
	
	
	private static class YearTypeEditor extends JSpinner.DefaultEditor
	{
		
		YearTypeEditor(JSpinner spinner)
		{
			super(spinner);
			JFormattedTextField ftf = getTextField();
			ftf.setEditable(true);
			ftf.setColumns(4);
		}// YearTypeEditor()
		
	}// inner class YearTypeEditor
			
	
	
	private static class SpinnerYearTypeModel extends AbstractSpinnerModel
	{
		private YearType value;
		private int minimum, maximum;
		
		
		/** Create a SpinnerYearTypeModel. 0 is not a valid min, max, or stepSize value. */
		SpinnerYearTypeModel(YearType initialYear, int minimum, int maximum) 
		{
			if(minimum > maximum || initialYear.getYear() > maximum || initialYear.getYear() < minimum)
			{
				throw new IllegalArgumentException("Bad min/max/initial values");
			}
			
			if(minimum == 0 || maximum == 0)
			{
				throw new IllegalArgumentException("Bad min/max values; cannot be 0");
			}
			
			this.value = initialYear;
			this.minimum = minimum;
			this.maximum = maximum;
		}// SpinnerYearTypeModel()
		
		
		/** Sets the minimum value. Note that 0 is not allowed. */
		public void setMinimum(int min)
		{
			if(min == 0 || min > maximum)
			{
				throw new IllegalArgumentException("invalid min value");
			}
			
			this.minimum = min;
			fireStateChanged();
		}// setMinimum()
		
		/** Sets the maximum value. Note that 0 is not allowed. */
		public void setMaximum(int max)
		{
			if(max == 0 || max < minimum)
			{
				throw new IllegalArgumentException("invalid max value");
			}
			
			this.maximum = max;
			fireStateChanged();
		}// setMaximum()
		
		/** Get the minimum value */
		public int getMinimum()			{ return minimum; }
		
		/** Get the maximum value */
		public int getMaximum()			{ return maximum; }
		
		
		/** Returns the next number in the sequence. Bounds-checks.*/
		public Object getNextValue()
		{
			YearType incremented = value.getNext();
			if(incremented.getYear() > maximum)
			{
				return null;
			}
			
			return incremented;
		}// getNextValue()
		
		
		/** Returns the previous number in the sequence. Bounds-checks.*/
		public Object getPreviousValue()
		{
			YearType decremented = value.getPrevious();
			if(decremented.getYear() < minimum)
			{
				return null;
			}
			
			return decremented;
		}// getNextValue()
		
		/** Returns the current value. */
		public Object getValue()
		{
			return value;
		}// getValue().
		
		
		/** 
		*	Sets the current value. If this is not a YearType, it
		*	is converted to a String and parsing is attempted. 
		*	If the value is not valid, the current value is used.
		*/
		public void setValue(Object newValue)
		{
			// separate by type
			YearType yt = null;
			if(newValue instanceof YearType)
			{
				yt = (YearType) newValue;
			}
			else if(newValue != null)
			{
				// attempt String conversion (parsing)
				// parse() will return null if year is invalid (includes '0')
				yt = YearType.parse( newValue.toString().toLowerCase() );
			}
			
			// if legal, use it
			if(yt != null && yt.getYear() >= minimum && yt.getYear() <= maximum)
			{
				this.value = yt;
			}
			
			// update
			fireStateChanged();
		}// setValue()
		
	}// inner class SpinnerYearTypeModel
	
}// class BCSpinner







