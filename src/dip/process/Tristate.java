//
//  @(#)Tristate.java	1.00	4/1/2002
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

package dip.process;

/**
*
*	Trinary state object. Immutable.
*	<p>
*	This class is not serializable; if this is required, 
*	readResolve() must be implemented to preserve the integrity
*	of referential equality.
*
*/
final public class Tristate
{
	// constants
	public static final Tristate TRUE		= new Tristate("True");
	public static final Tristate FALSE		= new Tristate("False");
	public static final Tristate UNCERTAIN	= new Tristate("Uncertain");
	
	// alternate set of constants, fully equivalent and interchangeable with above
	// constants.
	public static final Tristate MAYBE 		= UNCERTAIN;
	public static final Tristate YES 		= TRUE;
	public static final Tristate NO 		= FALSE;
	public static final Tristate SUCCESS	= TRUE;
	public static final Tristate FAILURE	= FALSE;
	
	
	// instance variables
	private transient String text = null;
	
	
	/** Create a TriState object */
	private Tristate(String value)
	{
		this.text = value;
	}// Tristate()
	
	
	public String toString()
	{
		return text;
	}// toString()
	
	/** Compares a Tristate to a boolean */
	public boolean equals(boolean value)
	{
		if( (value && (this == TRUE))
			|| (!value && (this == FALSE)) )
		{
			return true;
		}
		
		return false;
	}// equals()
	
	/** Get a Tristate that is equivalent to the boolean */
	public static Tristate getTristate(boolean value)
	{
		return ((value) ? TRUE : FALSE);
	}// getTristate()
	
}// class Tristate
