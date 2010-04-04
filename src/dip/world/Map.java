//
//  @(#)Map.java		4/2002
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

import dip.order.*;

import java.util.*;
import java.io.*;

import org.xml.sax.*;
import org.xml.sax.helpers.*;
import javax.xml.parsers.*;


/**
*	A Map is a list of Provinces and Powers, and methods for obtaining and parsing
*	these Provinces and Powers.
*
*
*	
*
*/
public class Map implements Serializable
{
	// constants
	private static final int MAP_SIZE = 211; 	// should be prime
	private static final int POWER_SIZE = 17;	// should be prime
	
	// internal constant arrays
	// all this data is serialized.
	private final Power[] 		powers;
	private final Province[]	provinces;
	
	// None of the data below here is serialized; it can be derived from
	// the above (serialized) data.
	//
	// Province-related
	private transient HashMap nameMap = null; 	// map of all (short & full) names to a province; names in lower case
	private transient String[] names = null;	// list of all province names [short & full]; names in lower case
	
	// Power-related
	private transient HashMap powerNameMap = null;		// created by createMappings()
	
	// fields created on first-use (by a method)
	private transient String[] lcPowerNames = null;		// lower case power names & adjectives
	private transient String[] wsNames = null;			// list of all province names that contain whitespace, "-", or " "
	
	
	
	/**
	*	Constructs a Map object.                   
	*
	*
	*/
	protected Map(Power[] powerArray, Province[] provinceArray)
	{
		// define constant arrays.
		powers = powerArray;
		provinces = provinceArray;
		
		// check provinceArray: index must be >= 0 and < provinceArray.length
		int len = provinceArray.length;
		for(int i=0; i<provinceArray.length; i++)
		{
			final int idx = provinceArray[i].getIndex();
			if(idx < 0 || idx >= len)
			{
				throw new IllegalArgumentException("Province: "+provinceArray[i]+": illegal Index: "+idx);
			}
			
			if(idx != i)
			{
				throw new IllegalArgumentException("Province: "+provinceArray[i]+": out of order (index: "+idx+"; position: "+i+")");
			}
		}
		
		// create mappings
		createMappings();
	}// Map()
	
	
	
	/**
	*	Creates the name->power and name->province mappings.
	*	<p>
	*	After de-serialization, this method MUST be called, since 
	*	the mappings aren't saved by default.
	*
	*/
	private void createMappings()
	{
		// create powerNameMap
		powerNameMap = new HashMap(POWER_SIZE);
		for(int i=0; i<powers.length; i++)
		{
			Power power = powers[i];
			String[] tmp = power.getNames();
			for(int nmIdx=0; nmIdx<tmp.length; nmIdx++)
			{
				powerNameMap.put(tmp[nmIdx].toLowerCase(), power);
			}
			
			// also map adjectives
			powerNameMap.put(power.getAdjective().toLowerCase(), power);
		}
		
		// create lcPowerNameList
		createLCPowerNameList();
		
		// province-related namemap
		//
		nameMap = new HashMap(MAP_SIZE);
		ArrayList namesAL = new ArrayList(MAP_SIZE);
		for(int i=0; i<provinces.length; i++)
		{
			Province province = provinces[i];
			String lcName = province.getFullName().toLowerCase();
			
			// map long name, and add to list
			nameMap.put(lcName, province);
			namesAL.add(lcName);
			
			// map short names, and add to list
			String[] lcShortNames = province.getShortNames();
			for(int j=0; j<lcShortNames.length; j++)
			{
				lcName = lcShortNames[j].toLowerCase();
				nameMap.put(lcName, province);
				namesAL.add(lcName);
			}
		}
		
		// create names array from ArrayList
		names = (String[]) namesAL.toArray(new String[namesAL.size()]);
	}// createMappings()
	
	
	
	
	
	
	/**
	*	Returns an Array of all Powers.
	*
	*/
	public final Power[] getPowers()
	{
		return powers;
	}// getPowers()
	
	
	/**
	*	Returns the power that matches name. Returns null if no
	*	match found.
	*	<p>
	*	The match must be exact, but is case-insensitive.
	*/
	public Power getPower(String name)
	{
		return (Power) powerNameMap.get(name.toLowerCase());
	}// getPower()
	
	
	/**
	*	Returns the closest Power to the given input String.
	*	If no reasonable match is found, or multiple matches are found,
	*	returns null.
	*	<p>
	*	This is different from getPowerMatching() in that this method
	*	assumes <i>a priori</i> that the input is a power; it therefore
	*	has looser parsing requirements. Likewise, if used on non-power tokens 
	*	(e.g., Provinces), it may be sufficiently close to a Power that it will
	*	match; such improper (mis)matches would occur much LESS often 
	*	with getPowerMatching().
	*	<p>
	*	As few as a single character can be matched (if it's unique); 
	*	e.g., "E" for England.
	*/
	public Power getClosestPower(String powerName)
	{
		// return 'null' if powerName is empty
		if("".equals(powerName))
		{
			return null;
		}
		
		// 1) check for an exact match.
		//
		Power matchPower = null;
		matchPower = getPower(powerName);
		if(matchPower != null)
		{
			return matchPower;
		}
		
		// make lowercase
		powerName = powerName.toLowerCase();
		
		// 2) check for a unique partial match
		//
		List list = findPartialPowerMatch(powerName);
		if(list.size() == 1)
		{
			return (Power) list.get(0);
		}
		
		// 3) perform a Levenshtein match against power names.
		// 
		int bestMatch = Integer.MAX_VALUE;
		matchPower = null;
		for(int i=0; i<lcPowerNames.length; i++)
		{
			String name = lcPowerNames[i];
			
			final int distance = Distance.getLD(powerName, name);
			if(distance < bestMatch)
			{
				matchPower = getPower(name);
				bestMatch = distance;
			}
			else if(distance == bestMatch)
			{
				if(matchPower != getPower(name))
				{
					matchPower = null;
				}
			}	
		}
		
		// if absolute error rate is too high, discard.
		if(bestMatch <= ((int) (powerName.length() / 2)))
		{
			return matchPower;
		}
		
		// 4) nothing sufficiently close. Return null.
		return null;
	}// getClosestPower()
	
	
	/**
	*	Returns the Power that matches the powerName. Returns 
	*	null if no best match found.
	*	<p>
	*	This will match the closest power but requires at least
	*	5 characters for a match.
	*/
	public Power getPowerMatching(String powerName)
	{
		// return 'null' if powerName is empty
		if("".equals(powerName))
		{
			return null;
		}
		
		// first, check for exact match.
		Power bestMatchingPower = null;
		bestMatchingPower = getPower(powerName);
		if(bestMatchingPower != null)
		{
			return bestMatchingPower;
		}
		
		powerName = powerName.toLowerCase();
		
		// no exact match. 
		// otherwise we check for the 'max' matched characters, and go with this
		// if there are multiple equivalent matches (ties), without a clear winner,
		// return null.
		if(powerName.length() >= 4)
		{
			List list = findPartialPowerMatch(powerName);
			if(list.size() == 1)
			{
				return (Power) list.get(0);
			}
		}
		
		// 3) perform a levenshtein match against power names.
		// 
		int bestMatch = Integer.MAX_VALUE;
		String bestMatchPowerName = null;
		for(int i=0; i<lcPowerNames.length; i++)
		{
			String name = lcPowerNames[i];
			
			final int distance = Distance.getLD(powerName, name);
			if(distance < bestMatch)
			{
				bestMatchPowerName = name;
				bestMatch = distance;
			}
			else if(distance == bestMatch)
			{
				bestMatchPowerName = null;
			}	
		}
		
		// if absolute error rate is too high, discard.
		// we are stricter than in getClosestPower()
		if(bestMatch <= ((int) (powerName.length() / 3)))
		{
			return getPower(bestMatchPowerName);	// should never return null
		}
		
		// nothing is close
		return null;
	}// getPowerMatching()
	
	
	
	/**
	*	Returns an Array of all Provinces.
	*
	*/
	public final Province[] getProvinces()
	{
		return provinces;	
	}// getProvinces()
	
	
	/**
	*	Returns the Province that matches name. Returns null if
	*	no match found.
	*	<p>
	*	The match must be exact, but is case-insensitive.
	*/
	public Province getProvince(String name) 
	{
		return (Province) nameMap.get(name.toLowerCase());
	}// getProvince()
	
	
	/**
	*	Returns the Province that matches the input name. Returns 
	*	null if no best match found.
	*	<p>
	*	This will match the closest power but requires at least
	*	3 characters for a match. Ties result in no match at all.
	*	This method uses the Levenshtein distance algorithm
	*	to determine closeness.
	*/
	public Province getProvinceMatching(String input)
	{
		// return 'null' if input is empty
		if(input == null || input.length() == 0)
		{
			return null;
		}
		
		// first, try exact match.
		// (fastest, if it works)
		Province province = getProvince(input);
		if(province != null)
		{
			return province;
		}
		
		// we must be at least 3 chars
		if(input.length() < 3)
		{
			return null;
		}
		
		// input converted to lower case
		input = input.toLowerCase().trim();
		
		
		// Do a partial match against the name list. 
		// If we tie, return no match. This is a 'partial first match'
		// This is tried BEFORE we try Levenshtein
		//
		List list = findPartialProvinceMatch(input);
		if(list.size() == 1)
		{
			return (Province) list.get(0);
		}
		
		// tie list. Use a Set so that we get no dupes
		Set ties = new HashSet();
		
		// compute Levenshteins on the match
		// if there are ties, keep them.. for now
		ties.clear();
		int bestDist = Integer.MAX_VALUE;
		for(int i=0; i<names.length; i++)
		{
			String name = names[i];
			
			// check closeness. Smaller is better.
			final int distance = Distance.getLD(input, name);
			if(distance < bestDist)
			{
				ties.clear();
				ties.add( getProvince(name) );
				bestDist = distance;
			}
			else if(distance == bestDist)
			{
				ties.add( getProvince(name) );
			}	
		}	
		
		/*
		System.out.println("LD input: "+input);
		System.out.println("   ties: "+ties);
		System.out.println("   bestDist: "+bestDist);
		System.out.println("   maxbest: "+((int) (input.length() / 2)));
		*/
		
		// if absolute error rate is too high, discard.
		// if we have >1 unique ties, (or none at all) no match
		if(bestDist <= ((int) (input.length() / 2)) && ties.size() == 1)
		{
			// there is but one
			return (Province) ties.iterator().next(); 
		}
		
		return null;
	}// getProvinceMatching
	
	
	/**
	*	Finds the Province(s) that best match the given input.
	*	Returns a List of Provinces that match. If an empty list,
	*	nothing was close (e.g., less than three characters). 
	*	If the list contains a single Province,
	*	it is the closest match. If the list contains multiple Provinces,
	*	there were several equally-close matches (ties).
	*	<p>
	*	This method uses the Levenshtein distance algorithm
	*	to determine closeness.
	*	<p>
	*	
	*/
	public Collection getProvincesMatchingClosest(String input)
	{
		// return empty list
		if(input == null || input.length() == 0)
		{
			return new ArrayList(1);
		}
		
		// first, try exact match.
		// (fastest, if it works)
		Province province = getProvince(input);
		if(province != null)
		{
			ArrayList matches = new ArrayList(1);
			matches.add(province);
			return matches;
		}
		
		// input converted to lower case
		input = input.toLowerCase().trim();
		
		// tie list. Use a Set so that we get no dupes
		Set ties = new HashSet();
		
		// if 2 or less, do no processing
		if(input.length() <= 2)
		{
			return new ArrayList(1);
		}
		else if(input.length() == 3)
		{
			// if we are only 3 chars, do a partial-first match
			// against provinces and return that tie list (or,
			// if no tie, return the province)
			// 
			// This works better than Levenshtein
			// which can return some very odd results.
			// for short strings...
			//
			for(int i=0; i<names.length; i++)
			{
				String name = names[i];
				if(name.startsWith(input))
				{
					ties.add(getProvince(name));
				}
			}
		}
		else
		{
			// compute Levenshteins on the match
			// if there are ties, keep them.. for now
			int bestDist = Integer.MAX_VALUE;
			for(int i=0; i<names.length; i++)
			{
				String name = names[i];
				
				// check closeness. Smaller is better.
				final int distance = Distance.getLD(input, name);
				
				if(distance < bestDist)
				{
					ties.clear();
					ties.add( getProvince(name) );
					bestDist = distance;
				}
				else if(distance == bestDist)
				{
					ties.add( getProvince(name) );
				}	
			}	
		}
		
		return ties;
	}// getProvincesMatchingClosest()
	
	
	
	
	/**
	*	Parses text into a Location. This will discern coast
	*	information, if present, as per Coast.normalize() followed 
	*	by Coast.parse().
	*
	*/
	public Location parseLocation(String input)
	{
		Coast coast = null;
		try
		{
			input = Coast.normalize(input);
			coast = Coast.parse(input);
		}
		catch(OrderException e)
		{
			return null;
		}
		
		Province province = getProvinceMatching( Coast.getProvinceName(input) );
		if(province != null)
		{
			return new Location(province, coast);
		}
		
		return null;
	}// parseLocation()
	
	
	
	/**
	*	Searches the input string for any province names that contain
	*	hyphens or whitespace ('-' or ' ') and replaces it with a short name.
	*	this simplifies parsing, later, and allows the parser to better understand
	*	multi-word names. ASSUMES input is all lower-case.
	*	<p>
	*	This is a special-purpose method for Order parsing.
	*/
	public void replaceProvinceNames(StringBuffer sb)
	{
		// create the whitespace list, if it doesn't exist.
		if(wsNames == null)
		{
			List list = new ArrayList(50);
			for(int i=0; i<names.length; i++)
			{
				String name = names[i];
				if(name.indexOf(' ') != -1 || name.indexOf('-') != -1)
				{
					list.add(name.toLowerCase());
				}
			}
			wsNames = (String[]) list.toArray(new String[list.size()]);
			
			// sort array from longest entries to shortest. This 
			// eliminates errors in partial replacements.
			Arrays.sort(wsNames, new Comparator()
			{
				// longer strings are more negative, thus rise to top
				public int compare(Object o1, Object o2)
				{
					String s1 = (String) o1;
					String s2 = (String) o2;
					return (s2.length() - s1.length());
				}// compare()
				
				public boolean equals(Object obj) { return false; }
			});
			
		}
		
		// search & replace.
		for(int i=0; i<wsNames.length; i++)
		{
			String currentName = wsNames[i];
			
			int idx = 0;
			int start = sb.indexOf(currentName, idx);
			
			while(start != -1)
			{
				int end = start + currentName.length();				
				sb.replace(start, end, getProvince(currentName).getShortName());
				// repeat search
				idx = start + currentName.length();
				start = sb.indexOf(currentName, idx);
			}
		}
	}// replaceProvinceNames()
	
	
	
	/**
	*	Eliminates any Power Names (e.g., "France") after the first whitespace
	*	character or colon(this is done to prevent elimination of the first power, 
	*	which is required). 
	*	<p>
	*	<b>NOTE: assumes StringBuffer is all lower-case.</b>
	*	<p>
	*	This is a special-purpose method for Order parsing.
	*/
	public void filterPowerNames(StringBuffer sb)
	{
		// find first white space or colon
		int wsIdx = -1;
		for(int i=0; i<sb.length(); i++)
		{
			final char c = sb.charAt(i);
			if(c == ':' || Character.isWhitespace(c))
			{
				wsIdx = i;
				break;
			}
		}
				
		// search / delete all names.
		// just looks for a single power name.
		// 
		// preceding character MUST be a whitespace character.
		// thus "prussia" would not become "p"
		if(wsIdx >= 0)
		{
			for(int i=0; i<lcPowerNames.length; i++)
			{
				final int idx = sb.indexOf(lcPowerNames[i], wsIdx);
				if(idx >= 0)
				{
					if(idx != 0 && Character.isWhitespace(sb.charAt(idx-1)))
					{
						sb.delete(idx, (idx + lcPowerNames[i].length()));				
					}
				}
			}
		}
	}// filterPowerNames()
	
	
	/**
	*	If a power token is specified (e.g., France), returns the token as a String.
	*	If no token is specified, returns null. If a colon is present, this is
	*	much looser than if no colon is present.
	*	<p>
	*	<b>NOTE: assumes StringBuffer is all lower-case, is trimmed, and 
	*	that power names DO NOT contain whitespace.</b>
	*	<p>
	*	This is a special-purpose method for Order parsing.
	*	<p>
	*	examples:
	*	<code>
	*		France: xxx-yyy     // returns "France"<br>
	*		Fra: xxx-yyy		// returns "Fra" (assumed; it's before the colon)
	*		Fra xxx-yyy			// returns null (Fra not recognized)
	*		xxx-yyy				// returns null (xxx doesn't match a power)
	*	</code>
	*	
	*/
	public String getFirstPowerToken(StringBuffer sb)
	{
		assert(lcPowerNames != null);
		
		// if we find a colon, we will ASSUME that the first token
		// is a power, and use getClosestPower(); otherwise, we will
		// just check against the lcPowerNames list.
		boolean hasColon = false;
		
		// find first white space (or ':')
		int wsIdx = -1;
		for(int i=0; i<sb.length(); i++)
		{
			final char c = sb.charAt(i);
			if(c == ':')
			{
				hasColon = true;
				wsIdx = i;
				break;
			}
			if(Character.isWhitespace(c))
			{
				wsIdx = i;
				break;
			}
		}
		
		// return token iff we match a power
		if(wsIdx >= 0)
		{
			String nameToTest = sb.substring(0, wsIdx).trim();
			
			if(hasColon)
			{
				// looser: assume prior-to-colon is a power name.
				// no testing.
				return nameToTest;
			}
			else
			{
				// stricter: no ':'; first token may or may not be a power.
				for(int i=0; i<lcPowerNames.length; i++)
				{
					if( nameToTest.startsWith(lcPowerNames[i]) )
					{
						return nameToTest;
					}
				}
			}
		}
		
		return null;
	}// getFirstPowerToken()
	
	
	/**
	*	If a power token is specified (e.g., France), returns the token as a String.
	*	If no token is specified, returns null. If a colon is present, this is
	*	much looser than if no colon is present.
	*	<p>
	*	<b>NOTE: assumes StringBuffer is all lower-case, is trimmed, and 
	*	that power names DO NOT contain whitespace.</b>
	*	<p>
	*	This is a special-purpose method for Order parsing.
	*	<p>
	*	examples:
	*	<code>
	*		France: xxx-yyy     // returns "France"<br>
	*		Fra: xxx-yyy		// returns "France" (assumed; it's before the colon)
	*		Fra xxx-yyy			// returns null (Fra not recognized)
	*		xxx-yyy				// returns null (xxx doesn't match a power)
	*	</code>
	*	
	*/
	public Power getFirstPower(String input)
	{
		assert(lcPowerNames != null);
		
		// if we find a colon, we will ASSUME that the first token
		// is a power, and use getClosestPower(); otherwise, we will
		// just check against the lcPowerNames list.
		boolean hasColon = false;
		
		// find first white space (or ':')
		int wsIdx = -1;
		for(int i=0; i<input.length(); i++)
		{
			final char c = input.charAt(i);
			if(c == ':')
			{
				hasColon = true;
				wsIdx = i;
				break;
			}
			if(Character.isWhitespace(c))
			{
				wsIdx = i;
				break;
			}
		}
		
		// return token iff we match a power
		if(wsIdx >= 0)
		{
			String nameToTest = input.substring(0, wsIdx).trim();
			if(hasColon)
			{
				// looser: assume prior-to-colon is a power name.
				return getClosestPower(nameToTest);
			}
			else
			{
				// stricter: no ':'; first token may or may not be a power.
				for(int i=0; i<lcPowerNames.length; i++)
				{
					if( nameToTest.startsWith(lcPowerNames[i]) )
					{
						return getPowerMatching(nameToTest);
					}
				}
			}
		}
		
		return null;
	}// getFirstPower()
	
	
	
	/** 
	*	Given an index, returns the Province to which that index corresponds.
	*/
	public final Province reverseIndex(int i)
	{
		return provinces[i];
	}// reverseIndex()
	
	
	/** 
	*	Creats the reverse-sorted power name list required by 
	*	getFirstPowerToken(), filterPowerNames(), and other methods.
	*	<p>
	*	Includes power adjectives.
	*/
	private void createLCPowerNameList()
	{
		List tmpNames = new ArrayList(powers.length);
		
		for(int i=0; i<powers.length; i++)
		{
			Power power = powers[i];
			String[] tmp = power.getNames();
			for(int nmIdx=0; nmIdx<tmp.length; nmIdx++)
			{
				tmpNames.add(tmp[nmIdx].toLowerCase());
			}
			
			tmpNames.add(power.getAdjective().toLowerCase());
		}
		
		// sort collection, in reverse alpha order.
		// Why? because we need to ensure power names (and adjectives) like
		// "Russian" come before "Russia"; otherwise, the replacement will be f'd up.
		Comparator reverseComp = Collections.reverseOrder();
		Collections.sort( tmpNames, reverseComp );
		
		lcPowerNames = (String[]) tmpNames.toArray(new String[tmpNames.size()]);
	}// createLCPowerNameList()
	
	
	
	
	/*
		Deprecated
		
		match string against another.
		if src > dest, -1
		higher number == closer!
		not ideal for checking exact match.
		we stop checking at the first letter that doesn't compare.
		assumes: SRC is lower case
		DEST lower case (now...)
	
	
	
	private int getCloseness(String src, String dest)
	{
		if(src.length() > dest.length())
		{
			return -1;
		}
				
		int numCharsMatching = 0;
		for(int i=0; i<src.length(); i++)
		{
			//if(src.charAt(i) != Character.toLowerCase(dest.charAt(i)))		// OLD
			if(src.charAt(i) != dest.charAt(i))
			{
				break;
			}
			
			numCharsMatching++;
		}
		
		return numCharsMatching;
	}// getCloseness()
	*/
	
	/**
	*	Performs a 'best partial match' with a province name (trimmed, all 
	*	lower case). Returns a List which will be:
	*	<ol>
	*		<li>Empty, if no match occurs</li>
	*		<li>One item, if a single ("best") match occured</li>
	*		<li>Multiple items, if ties occur</li>
	* 	</ol>
	*	Null is never returned.
	*	<p>
	*	For example: given provinces "Liverpool" and "Livonia", and "Loveland":<br>
	*	"Li", and "Liv" will return a List of 2 items<br>
	*	"Liver" will return a List of 1 item (Liverpool)<br>
	*	"Xsdf" will return a List of 0 items.<br>
	*	<p>
	*	If there are multiple provinces with alternate names that 
	*	completely match, (different names, same object), only ONE reference
	*	to the object will be returned in the collection.
	*	<p>
	*	The reason this is important is it is more reliable than Levenshtein
	*	for matching some types of short strings 
	*	<p>
	*	THIS METHOD REPLACES getCloseness() FOR PROVINCE MATCHING.
	*/
	private List findPartialProvinceMatch(String input)
	{
		HashSet ties = new HashSet(41);
		
		for(int i=0; i<lcPowerNames.length; i++)
		{
			String provName = names[i];
			
			if(provName.startsWith(input))
			{
				ties.add( getProvince(provName) );	// should NEVER be null
			}
		}
		
		ArrayList al = new ArrayList(ties.size());
		al.addAll(ties);
		return al;
	}// findClosestProvince()
	
	
	/**
	*	Same as findPartialProvinceMatch(), but for matching powers.
	*	
	*	THIS METHOD REPLACES getCloseness() FOR POWER MATCHING.
	*/
	private List findPartialPowerMatch(String input)
	{
		HashSet ties = new HashSet(41);
		
		for(int i=0; i<lcPowerNames.length; i++)
		{
			String powerName = lcPowerNames[i];
			
			if(powerName.startsWith(input))
			{
				ties.add( getPower(powerName) );	// should NEVER be null
			}
		}
		
		ArrayList al = new ArrayList(ties.size());
		al.addAll(ties);
		return al;
	}// findPartialPowerMatch()
	
	
	
	/**
	*	Gets a Levenshtein Edit Distance
	*	Code by Michael Gilleland, Merriam Park Software
	*
	*/
	private static class Distance
	{
		/** Get minimum of three values */
		private static int getMin(int a, int b, int c)
		{
			int mi;
			
			mi = a;
			if (b < mi) 
			{
				mi = b;
			}
			
			if (c < mi)
			{
				mi = c;
			}
			
			return mi;
		}// getMin()
		
		/** Compute Levenshtein distance */
		public static int getLD(String s, String t)
		{
			int d[][]; // matrix
			int n; // length of s
			int m; // length of t
			int i; // iterates through s
			int j; // iterates through t
			char s_i; // ith character of s
			char t_j; // jth character of t
			int cost; // cost
			
			// Step 1
			n = s.length ();
			m = t.length ();
			if(n == 0)
			{
				return m;
			}
			
			if(m == 0)
			{
				return n;
			}
			
			d = new int[n+1][m+1];
			
			// Step 2
			
			for(i = 0; i <= n; i++)
			{
				d[i][0] = i;
			}
			
			for(j = 0; j <= m; j++)
			{
				d[0][j] = j;
			}
			
			// Step 3
			for(i = 1; i <= n; i++)
			{
				s_i = s.charAt(i - 1);
	
				// Step 4
				for(j = 1; j <= m; j++)
				{
					t_j = t.charAt(j - 1);
					
					// Step 5
					cost = (s_i == t_j) ? 0 : 1;
					
					// Step 6
					d[i][j] = getMin(d[i-1][j]+1, d[i][j-1]+1, d[i-1][j-1] + cost);
				}// for(j)
			}// for(i)
			
			// Step 7
			return d[n][m];
		}// getLD()
	}// inner class Distance
	
	
	
	// reserialization: re-create mappings
	private void readObject(java.io.ObjectInputStream in)
	throws IOException, ClassNotFoundException
	{
		in.defaultReadObject();
		
		// re-create transient data.
		createMappings();
	}// readObject()
	
	
}// class Map
///////
