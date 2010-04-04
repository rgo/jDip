//
//  @(#)AdjustmentParser.java	1.00	6/2002
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
package dip.judge.parser;

import dip.world.Phase;
import dip.world.Power;
import dip.world.Map;

import java.io.*;
import java.util.regex.*;
import java.util.*;
/**
*	Parses the Adjustment information block. 
*	<br>
*	This includes both supply center ownership, and build/removes.
*
*
*
*/
public class AdjustmentParser
{
	// CONSTANTS
	// empty string
	private static final String[] EMPTY = new String[0];
	
	/** Header text to look for */
	public static final String HEADER_REGEX = "(?i)ownership of supply centers:";
	
	/** Adjustment regex
	*	Capture groups: 1:power 2:# supply centers 3:#units 4:# to build or remove
	*	This is always fed a trimmed string, and assumed that it is always on one line. But we search a whole block.
	*	case-insensitive. Power names: alphanumeric + "-" and "_" supported.
	* 	Parsed on a per-line basis.
	*/
	public static final String ADJUST_REGEX = "(?i)([\\p{Alnum}\\-_]*):\\D*(\\d+)\\D+(\\d+)\\D+((\\d+)).*\\.";
	
	
	
	
	// INSTANCE VARIABLES
	private dip.world.Map map = null;
	
	private List ownerList = null;
	private List adjustList = null;
	private Pattern regexAdjust = null;
	
	private OwnerInfo[] ownerInfo = null;
	private AdjustInfo[] adjustInfo = null;
	
	/*
	// TEST
	public static void main(String args[])
	throws IOException
	{
		// NOTE: 2 stp. because one is split, as it's been seen on floc.net and 
		// thus is a good test.
		String in = 
			"The deadline for orders will be Wed Apr 10 2002 17:54:23 -0500.\n"+
			"asdfjafsdkjfadslk: sdljfjldksfkjfdlsls \n"+
			"fadslkfkjdlsafslkj\n"+
			"\n"+
			"Ownership of supply centers:\n"+
			"\n"+
			"Austria:   Greece, Serbia.\n"+
			"England:   Edinburgh, Liverpool, London.\n"+
			"France:    Belgium, Brest, Marseilles, Paris, \n"+
			"           Portugal, Spain.\n"+
			"Germany:   Berlin, Denmark, Holland, Kiel, Munich, Norway.\n"+
			"Italy:     Naples, Rome, Trieste, Tunis, Venice, St.\n"+
			"           Petersburg.\n"+
			"Russia:    Moscow, Norway, Rumania, Sevastopol,\n"+
			"	    St. Petersburg, Vienna, Warsaw.\n"+
			"Turkey:    Ankara, Bulgaria, Constantinople, Smyrna.\n"+
			"\n"+
			"Austria:   2 Supply centers,  3 Units:  Removes  1 unit.\n"+
			"England:   3 Supply centers,  4 Units:  Removes  1 unit.\n"+
			"France:    6 Supply centers,  4 Units:  Builds   2 units.\n"+
			"Germany:   6 Supply centers,  5 Units:  Builds   1 unit.\n"+
			"Italy:     5 Supply centers,  5 Units:  Builds   0 units.\n"+
			"Russia:    8 Supply centers,  7 Units:  Builds   1 unit.\n"+
			"Turkey:    4 Supply centers,  4 Units:  Builds   0 units.\n"+
			"\n"+
			"The next phase of 'delphi' will be Adjustments for Winter of 1902.\n";

		
		
		
		AdjustmentParser ap = new AdjustmentParser(in);
		OwnerInfo[] oi = ap.getOwnership();
		AdjustInfo[] ai = ap.getAdjustments();
		System.out.println("oi = "+oi);
		System.out.println("oi.length = "+oi.length);
		for(int i=0; i<oi.length; i++)
		{
			System.out.println(oi[i]);
			System.out.println("");
		}
		System.out.println("ai = "+ai);
		System.out.println("ai.length = "+ai.length);
		for(int i=0; i<ai.length; i++)
		{
			System.out.println(ai[i]);
			System.out.println("");
		}
	}// main()
	
	*/
	
	/** Creates a AdjustmentParser object, which parses the given input for an Ownership and Adjustment info blocks */
	public AdjustmentParser(Map map, String input)
	throws IOException
	{
		if(map == null || input == null)
		{
			throw new IllegalArgumentException();
		}
		
		this.map = map;
		parseInput(input);
	}// AdjustmentParser()
	
	
	/** Returns an array of OwnerInfo objects; this never returns null. */
	public OwnerInfo[] getOwnership()
	{
		return ownerInfo;
	}// getOwnership()
	
	/** Returns an array of AdjustInfo objects; this never returns null. */
	public AdjustInfo[] getAdjustments()
	{
		return adjustInfo;
	}// getAdjustments()
	
	
	/**
	*	An OwnerInfo object is created for each power.
	*	<p>
	*
	*/
	public static class OwnerInfo
	{
		private final String power;
		private final String[] locations;
		
		/** Create a OwnerInfo object */
		public OwnerInfo(String power, String[] locations)
		{
			this.power = power;
			this.locations = (locations == null) ? EMPTY : locations;
		}// OwnerInfo()
		
		/** Name of the Power */
		public String getPowerName()					{ return power; }
		
		/** Names of provinces with owned supply centers */
		public String[] getProvinces()					{ return locations; }
		
		/** String output for debugging; may change between versions. */
		public String toString()
		{
			StringBuffer sb = new StringBuffer();
			sb.append("OwnerInfo[power=");
			sb.append(power);
			sb.append(", locations=");
			for(int i=0; i<locations.length; i++)
			{
				sb.append(locations[i]);
				sb.append(',');
			}
			sb.append(']');
			return sb.toString();
		}// toString()
	}// nested class OwnerInfo
	
	
	/**
	*	An AdjustInfo object is created for each power, and contains adjustment information
	*	<p>
	*
	*/
	public static class AdjustInfo
	{
		private final String power;
		private final int numSC;
		private final int numUnits;
		private final int toBuildOrRemove;
		
		/** Create an AdjustInfo object */
		public AdjustInfo(String power, int numSC, int numUnits, int toBuildOrRemove)
		{
			if(numSC < 0 || numUnits < 0 || toBuildOrRemove < 0 || power == null)
			{
				throw new IllegalArgumentException("bad arguments");
			}
			
			this.power = power;
			this.numSC = numSC;
			this.numUnits = numUnits;
			this.toBuildOrRemove = toBuildOrRemove;
		}// AdjustInfo()
		
		/** Name of the Power */
		public String getPowerName()					{ return power; }
		
		/** Current number of supply centers */
		public int getNumSupplyCenters()				{ return numSC; }
		
		/** Current number of units */
		public int getNumUnits()						{ return numUnits; }
		
		/** Number of units to build or remove */
		public int getNumBuildOrRemove()				{ return toBuildOrRemove; }
		
		/** String output for debugging; may change between versions. */
		public String toString()
		{
			StringBuffer sb = new StringBuffer();
			sb.append("AdjustInfo[power=");
			sb.append(power);
			sb.append(", SC=");
			sb.append(numSC);
			sb.append(", units=");
			sb.append(numUnits);
			sb.append(", change=");
			sb.append(toBuildOrRemove);
			sb.append(']');
			return sb.toString();
		}// toString()
	}// nested class AdjustInfo
	
	
	
	
	
	
	/** Parse the input, and create the appropriate Info objects (or a zero-length arrays) */
	private void parseInput(String input)
	throws IOException
	{
		// create lists
		ownerList = new LinkedList();
		adjustList = new LinkedList();
		
		// create patterns
		regexAdjust = Pattern.compile(ADJUST_REGEX);
		
		// Create HEADER_REGEX pattern
		Pattern header = Pattern.compile(HEADER_REGEX);
		
		// search for HEADER_REGEX
		// create a block of text                      
		BufferedReader br = new BufferedReader(new StringReader(input));
		
		String line = br.readLine();
		while(line != null)
		{
			Matcher m = header.matcher(line);
			if(m.lookingAt())
			{
				parseOwnerBlock( ParserUtils.parseBlock(br) );
				parseAdjustmentBlock( ParserUtils.parseBlock(br) );
				break;
			}
			
			line = br.readLine();
		}
		
		
		// create the output array
		ownerInfo = (OwnerInfo[]) ownerList.toArray(new OwnerInfo[ownerList.size()]);
		adjustInfo = (AdjustInfo[]) adjustList.toArray(new AdjustInfo[adjustList.size()]);
		
		// cleanup
		br.close();
		ownerList.clear();
		adjustList.clear();
		ownerList = null;
		adjustList = null;
		regexAdjust = null;
	}// parseInput()
	
	
	
	/** 
	*	Given a trimmed block, determines ownership
	*/
	private void parseOwnerBlock(String text)
	throws IOException
	{
		// map of Powers to StringBuffers
		HashMap pmap = new HashMap();
		
		// parse and re-formulate
		// into a new string
		//
		Power currentPower = null;
		StringTokenizer st = new StringTokenizer(text, " \f\t\n\r");
		while(st.hasMoreTokens())
		{
			String tok = st.nextToken();
			if(tok.equalsIgnoreCase("unowned:"))
			{
				// we don't process unowned SC yet. I'm not sure that
				// all judges support this??
				currentPower = null;
			}
			else if(tok.endsWith(":"))
			{
				// should be a Power
				//
				Power p = map.getPower(tok.substring(0, tok.length() - 1));
				if(p == null)
				{
					throw new IOException("Adjustment Block: Power "+tok+" not recognized.");
				}
				
				// toss into the map
				currentPower = p;
				pmap.put(p, new StringBuffer());
			}
			else
			{
				if(currentPower != null)
				{
					StringBuffer sb = (StringBuffer) pmap.get(currentPower);
					sb.append(tok);
					sb.append(" ");
				}
			}
		}
		
		// now, iterate through the powers
		// parse the province
		// there may be a "." on the end of some
		// which should be eliminated.
		//
		final Power[] allPowers = map.getPowers();
		for(int i=0; i<allPowers.length; i++)
		{
			StringBuffer sb = (StringBuffer) pmap.get(allPowers[i]);
			if(sb != null)
			{
				final String[] provs = sb.toString().split("[\\,]");
				
				// clean up province tokens
				for(int pi=0; pi<provs.length; pi++)
				{
					provs[pi] = provs[pi].trim();
					if(provs[pi].endsWith("."))
					{
						provs[pi] = provs[pi].substring(0, provs[pi].length()-1);
					}
				}
				
				// create OwnerInfo
				ownerList.add( new OwnerInfo(allPowers[i].getName(), provs) );
			}
		}
	}// parseOwnerBlock()
	
	
	/** 
	*	Given a trimmed block, determines adjustment
	*/
	private void parseAdjustmentBlock(String text)
	{
		String[] lines = text.split("\\n");
		
		for(int i=0; i<lines.length; i++)
		{
			Matcher m = regexAdjust.matcher(lines[i]);
			
			if(m.find())
			{
				adjustList.add(new AdjustInfo( 
						m.group(1),
						Integer.parseInt(m.group(2)),
						Integer.parseInt(m.group(3)),
						Integer.parseInt(m.group(4)) ));
			}
			else
			{
				break;
			}
		}
	}// parseAdjustmentBlock()
	
	

	
}// class AdjustmentParser
