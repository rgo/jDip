//
//  @(#)Coast.java		4/2002
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

import dip.order.OrderException;

import java.io.InvalidObjectException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
/**
*	Coasts are essential to determining connectivity between Provinces.
*	<p>
*	Coast constants should be used.
*
*
*
*
*
*
*
*
*/

public final class Coast implements java.io.Serializable
{
	// transient data
	transient static private Pattern[] patterns = null;
	
	// internal constants
	// TODO: these need to be properly internationalized.
	// To do that, we need internationlization support for coast 
	// normalization and parsing also
	// 
	private static final String NORTH_FULL 		= "North Coast";
	private static final String NORTH_ABBREV 	= "nc";
	private static final String SOUTH_FULL 		= "South Coast";
	private static final String SOUTH_ABBREV 	= "sc";
	private static final String WEST_FULL 		= "West Coast";
	private static final String WEST_ABBREV		= "wc";
	private static final String EAST_FULL 		= "East Coast";
	private static final String EAST_ABBREV 	= "ec";
	private static final String NONE_FULL 		= "None";
	private static final String NONE_ABBREV 	= "mv";
	private static final String SINGLE_FULL 	= "Single";
	private static final String SINGLE_ABBREV 	= "xc";
	private static final String WING_FULL 		= "Wing";
	private static final String WING_ABBREV		= "wx";
	private static final String UNDEFINED_FULL 		= "Undefined";
	private static final String UNDEFINED_ABBREV 	= "?";	// perhaps make it "?c" ??
	
	/* To be used in the future .... parsing to accomodate
	private static final String NW_FULL 		= "Northwest Coast";
	private static final String NE_FULL 		= "Northeast Coast";
	private static final String SW_FULL 		= "Southwest Coast";
	private static final String SE_FULL 		= "Southeast Coast";
	private static final String NW_ABBREV		= "nw";
	private static final String NE_ABBREV		= "ne";
	private static final String SW_ABBREV		= "sw";
	private static final String SE_ABBREV		= "se";
	*/
	
	
	
	// constants
	/** Constant indicated an Undefined coast */
	public static final Coast UNDEFINED = new Coast(UNDEFINED_FULL, UNDEFINED_ABBREV, 0);
	/** Constant indicating Wing coast (for Wing movement) */
	public static final Coast WING = new Coast(WING_FULL, WING_ABBREV, 1);
	/** Constant indicating no coast (Army movement) */
	public static final Coast NONE = new Coast(NONE_FULL, NONE_ABBREV, 2);
	/** Constant indicating a single Coast (for fleets in coastal land areas, or sea-only provinces) */
	public static final Coast SINGLE = new Coast(SINGLE_FULL, SINGLE_ABBREV, 3);
	/** Constant indicating North Coast */
	public static final Coast NORTH = new Coast(NORTH_FULL, NORTH_ABBREV, 4);
	/** Constant indicating South Coast */
	public static final Coast SOUTH = new Coast(SOUTH_FULL, SOUTH_ABBREV, 5);
	/** Constant indicating West Coast */
	public static final Coast WEST = new Coast(WEST_FULL, WEST_ABBREV, 6);
	/** Constant indicating East Coast */
	public static final Coast EAST = new Coast(EAST_FULL, EAST_ABBREV, 7);
	
	/** Alias for Coast.WING */
	public static final Coast TOUCHING = WING;
	/** Alias for Coast.NONE */
	public static final Coast LAND = NONE;
	/** Alias for Coast.SINGLE */
	public static final Coast SEA = SINGLE;	
	
	// index-to-coast array
	private static final Coast[] IDX_ARRAY = {
		UNDEFINED, WING, NONE, SINGLE, NORTH, SOUTH, WEST, EAST
	};
	
	
	/** 
	*	Array of Coasts that are not typically displayed
	*	<b>Warning: this should not be mutated.</b>
	*/
	public static final Coast[] NOT_DISPLAYED = { NONE, SINGLE, UNDEFINED, WING };
	
	/** 
	*	Array of the 6 main coast types (NONE, SINGLE, NORTH, SOUTH, WEST, EAST)
	*	<b>Warning: this should not be mutated.</b>
	*/
	public static final Coast[] ALL_COASTS = { NONE, SINGLE, NORTH, SOUTH, WEST, EAST };
	/** 
	*	Array of sea coasts (SINGLE, NORTH, SOUTH, WEST, EAST)
	*	<b>Warning: this should not be mutated.</b>
	*/
	public static final Coast[] ANY_SEA = { SINGLE, NORTH, SOUTH, WEST, EAST };
	/**
	*	Array of directional coasts (NORTH, SOUTH, WEST, EAST) 
	*	<b>Warning: this should not be mutated.</b>
	*/
	public static final Coast[] ANY_DIRECTIONAL = { NORTH, SOUTH, WEST, EAST };
	
	
	// class variables
	private final String name;
	private final String abbreviation;
	private final int index;
	
	// TODO: ?? hashCode should be == index (since index is unique)
	private transient int hashCode = 0;	// cache the hashCode
	
	/**
	*	Constructs a Coast
	*/
	private Coast(String name, String abbreviation, int index)
	{
		if(index < 0)
		{
			throw new IllegalArgumentException();
		}
		
		this.name = name;
		this.abbreviation = abbreviation;
		this.index = index;
	}// Coast()
	
	
	/**
	*	Returns the full name (long name) of a coast; e.g., "North Coast"
	*/
	public String getName()
	{
		return name;
	}// getName()
	
	/**
	*	Returns the abbreviated coast name (e.g., "nc")
	*/
	public String getAbbreviation()
	{
		return abbreviation;
	}// getAbbreviation()
	
	/** Gets the index of a Coast. Indices are &gt;= 0. */
	public int getIndex()
	{
		return index;
	}// getIndex()
	
	
	/** Gets the Coast corresponding to an index; null if index is out of range. */
	public static Coast getCoast(int idx)
	{
		if(idx >= 0 && idx < IDX_ARRAY.length)
		{
			return IDX_ARRAY[idx];
		}
		
		return null;
	}// getCoast()
	
	/**
	*	Returns the full name of the coast
	*/
	public String toString()
	{
		return name;
	}// toString()
	
	/**
	*	Returns if this Coast is typically displayed
	*/
	public static boolean isDisplayable(Coast coast)
	{
		for(int i=0; i<NOT_DISPLAYED.length; i++)
		{
			if(coast == NOT_DISPLAYED[i])
			{
				return false;
			}
		}
		return true;
	}// isDisplayable()
	
	/**
	*
	* Parses the coast from a text token.
	* <p>
	* Given a text token such as "spa-sc" or "spa/nc", 
	* Returns the Coast constant. Coasts must begin with
	* a '/', '-', or '\'; parenthetical notation e.g., "(nc)"
	* is not supported.
	* <p>
	* This method never returns null; for nonexistent or
	* unparsable coasts, Coast.UNDEFINED is returned.
	* <p>
	*/
	public static Coast parse(String text)
	{
		String input = text.toLowerCase().trim();
		
		// check if it is just a coast (2-letter) or
		// part of a province name. If we don't check
		// for -/\, then we could be processing part
		// of a province name
		if(input.length() >= 3)
		{
			char c = input.charAt(input.length() - 3);
			if(c != '-' && c != '/' && c != '\\')
			{
				return UNDEFINED;
			}
		}
		
		if(input.endsWith("nc"))
		{
			return NORTH;
		}
		else if(input.endsWith("sc"))
		{
			return SOUTH;
		}
		else if(input.endsWith("wc"))
		{
			return WEST;
		}
		else if(input.endsWith("ec"))
		{
			return EAST;
		}
		else if(input.endsWith("xc"))
		{
			return SINGLE;
		}
		else if(input.endsWith("mv"))
		{
			return LAND;
		}
		
		return Coast.UNDEFINED;
	}// parse()
	
	
	/**
	* Returns the Province name upto the first Coast seperator
	* character ('-', '/', or '\'); Parentheses are not supported.
	*/
	public static String getProvinceName(String input)
	{
		if(input.length() > 3)
		{
			final int idx = (input.length() - 3);
			final char c = input.charAt(idx);
			if(c == '-' || c == '/' || c == '\\')
			{
				return input.substring(0, idx);
			}
		}
		return input;
	}// getProvinceName()
	
	
	/**
	*	Normalizes coasts to standard format "/xx".
	*	<p>
	*	The following applies:
	*	<pre>
		a) input must be lower-case
		b) normalizes:
				axy     where a = "/" "\" or "-"
				x       where x = any alphanumeric [but is later checked]; a "." may follow
				y       where y = "c" or (if x="m", "v"); a "." my follow
		c) parenthetical coasts
			coalesces preceding spaces (before parenthesis), so
			"stp(sc)", "stp( sc)", "stp(.s.c.)", "stp (sc)", and "stp    (sc)" all would become "stp/sc"
			coast depends upon FIRST character
			stp(qoieru)    ==> invalid!  
	*	</pre>	
	*	<p>
	*	An OrderException is thrown if the coast is not recognized. The OrderException will contain
	*	the invalid coast text only.
	*	<p>
	*	Bug note: the following "xxx-n.c." will be converted to "xxx-nc ." Note the extra period.
	*	
	*/
	public static String normalize(String input)
	throws OrderException
	{
		// create patterns, if we have none.
		// these are threadsafe
		if(patterns == null)
		{
			patterns = new Pattern[2];
			
			// match /xx, -xx, \xx coasts; also takes care of periods.
			// also matches /x; will not match /xxx (or -xxx) 
			patterns[0] = Pattern.compile("\\s*[\\-\\\\/](\\p{Alnum}\\.?)(\\p{Alnum}\\.?)\\b");
			//
			// match parenthetical coasts. 
			//patterns[1] = Pattern.compile("\\s*\\([^\\p{Alnum}]*(\\p{Alnum})[^\\p{Alnum}]*(\\p{Alnum})[^)]*\\)");
			patterns[1] = Pattern.compile("\\s*\\(([.[^)]]*)(\\))\\s*");
		}
		
		// start matching.
		String matchInput = input;
		for(int i=0; i<patterns.length; i++)
		{
			Matcher m = patterns[i].matcher(matchInput);
			StringBuffer sb = new StringBuffer(matchInput.length());
			
			boolean result = m.find();
			while(result) 
			{
				if(m.groupCount() == 2)
				{
					final char c1 = m.group(1).charAt(0);
					final char c2 = m.group(2).charAt(0);
					
					//System.out.println("1: "+m.group(1)+";  2: "+m.group(2));
					
					if(c2 == ')')
					{
						String group1 = superTrim(m.group(1));
						
						// test 'full name' and abbreviated coasts inside parentheses
						if(group1.startsWith("north") || "nc".equals(group1))
						{
							m.appendReplacement(sb, "/nc ");
						}
						else if(group1.startsWith("south") || "sc".equals(group1))
						{
							m.appendReplacement(sb, "/sc ");
						}
						else if(group1.startsWith("west") || "wc".equals(group1))
						{
							m.appendReplacement(sb, "/wc ");
						}
						else if(group1.startsWith("east") || "ec".equals(group1))
						{
							m.appendReplacement(sb, "/ec ");
						}
						else if("mv".equals(group1))
						{
							m.appendReplacement(sb, "/mv ");
						}
						else if("xc".equals(group1))
						{
							m.appendReplacement(sb, "/xc ");
						}
					}
					else if( (c2 == 'c' && (c1 == 'n' || c1 == 's' || c1 == 'w' || c1 == 'e' || c1 == 'x'))
						|| (c1 == 'm' && c2 == 'v') )
					{
						StringBuffer rep = new StringBuffer(4);
						rep.append('/');
						rep.append(c1);
						rep.append(c2);
						rep.append(' ');    // space added afterwards--essential!
						m.appendReplacement(sb, rep.toString());
					}
					else
					{
						throw new OrderException(m.group(0));
					}
				}
				else
				{
					throw new OrderException(m.group(0));
				}
				
				result = m.find();
			}
			
			m.appendTail(sb);
			matchInput = sb.toString();
		}
		
		return matchInput.trim();
	}// normalize()
	
	/**
	*	Trims the following characters before, within, and after a given string.
	*	<br>
	*	space, tab, '.'
	*
	*/
	private static String superTrim(String in)
	{
		return in.replaceAll("\\.*\\s*\\t*", "");
	}// superTrim()
	
	
	/**
	* Returns <code>true</code> if coast is one of 
	* Coast.NORTH, Coast.SOUTH, Coast.WEST, or Coast.EAST
	*/
	public boolean isDirectional()
	{
		for(int i=0; i<ANY_DIRECTIONAL.length; i++)
		{
			if(this == ANY_DIRECTIONAL[i])
			{
				return true;
			}
		}
		
		return false;
	}// isDirectionalCoast()


	/** Implementation of Object.hashCode() */
	public int hashCode()
	{
		if(hashCode == 0)
		{
			hashCode = name.hashCode();
		}
		
		return hashCode;
	}// hashCode()
	
	
	/*
		equals():
		
		We use Object.equals(), which just does a test of 
		referential equality. 
		
	*/
	
	
	/** Assigns serialized objects to a single constant reference */
	protected Object readResolve()
	throws java.io.ObjectStreamException
	{
		Coast coast = null;
		
		if(name.equals(NORTH_FULL))
		{
			coast = NORTH;
		}
		else if(name.equals(SOUTH_FULL))
		{
			coast = SOUTH;
		}
		else if(name.equals(WEST_FULL))
		{
			coast = WEST;
		}
		else if(name.equals(EAST_FULL))
		{
			coast = EAST;
		}
		else if(name.equals(NONE_FULL))
		{
			coast = NONE;
		}
		else if(name.equals(SINGLE_FULL))
		{
			coast = SINGLE;
		}
		else if(name.equals(WING_FULL))
		{
			coast = WING;
		}
		else if(name.equals(UNDEFINED_FULL))
		{
			coast = UNDEFINED;
		}
		else
		{
			throw new InvalidObjectException("Unknown coast type: "+name);
		}
		
		return coast;
	}// readResolve()
		
}// class Coast()
