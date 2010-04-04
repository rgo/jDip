//
//  @(#)Power.java		4/2002
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
package dip.world;


/**
*	A Power represents player in the game.
*
*/
public class Power implements Comparable, java.io.Serializable
{
	// constants for name array; always stored in this order.
	private static final int FULL_NAME	= 0;	// required

	
	// immutable fields
	private final String[] names;		// length >= 1
	private final boolean isActive;
	private final String adjective;
	
	
	// transient fields
	private transient int hashCode = 0;
	
	/**
	*	Create a new Power.
	*	<p>
	*	The first name in the names array (index position 0) must be the full display
	*	name of the power. Names beyond index 0 are optional, and are "alternate" names
	*	(e.g., "Britain" instead of "England").
	*	<p>
	*	All fields are required and <b>must</b> be non-null/non-zero-length;
	*	Names (not adjectives) should not contain whitespace, and must not 
	*	be empty ("") or null.
	*	<p>
	*	This should generally not be used, except for when a game is first created. Note that
	*	Power uses instance equality, so two Power() objects created with the same arguments
	*	will NOT be the same.
	*/
	public Power(String[] names, String adjective, boolean isActive)
	{
		if(names == null || adjective == null)
		{
			throw new IllegalArgumentException("null argument(s)");
		}
		
		if(names.length == 0)
		{
			throw new IllegalArgumentException("no names");
		}
		
		if(adjective.length() == 0)
		{
			throw new IllegalArgumentException("empty adjective");
		}
		
		this.names = names;
		this.adjective = adjective;
		this.isActive = isActive;
	}// Power()
	
	
	/** Returns the name of the power. Never returns null. */
	public String getName()
	{
		return names[FULL_NAME];
	}// getName()
	
	/** Returns the adjective of the power (e.g., power France, adjective is French) */
	public String getAdjective()
	{
		return adjective;
	}// getAdjective()
	
	/** Get all names. There is always at least one. Does not include adjectives. */
	public String[] getNames()
	{
		return names;
	}// getAllNames()
	
	
	/** Determines if this power is active. Only active powers can order units. */
	public boolean isActive()						{ return isActive; }
	
	
	/** Implementation of Object.hashCode() */
	public int hashCode()
	{
		if(hashCode == 0)
		{
			hashCode = getName().hashCode();
		}
		
		return hashCode;
	}// hashCode()
	
	
	/* 	
		Implementation of Object.equals()
	 	NOTE: we just use default referential equality, since these objects are immutable!
	*/
	
	/** Implementation of Object.toString() */
	public String toString()
	{
		return getName();
	}// toString()
	
	
	/** Implementation of Comparable interface */
	public int compareTo(Object obj)
	{
		Power power = (Power) obj;
		return getName().compareTo(power.getName());
	}// compareTo()
	
	
}// class Power
