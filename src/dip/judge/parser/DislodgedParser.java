//
//  @(#)DislodgedParser.java	1.00	6/2002
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
import dip.misc.Log;

import java.io.*;
import java.util.regex.*;
import java.util.*;
/**
	Parses the Dislodged block
*/	
public class DislodgedParser
{
	// CONSTANTS
	// empty string
	private static final String[] EMPTY = new String[0];
	
	/** Header text to look for */
	public static final String HEADER_REGEX = "(?i)the following units were dislodged:";
	
	/** End of header text to look for */
	public static final String HEADER_END_REGEX = "(?i)the next phase of";
	
	/** Dislodged line start text; all dislodged lines must start with this. Lowercase. NOT a REGEX!. */
	public static final String DISLODGED_LINE_START = "the";
	
	/**
	*	capture groups: 1:power, 2:unit, 3:unit location, 4:retreat list predicate (example: "xxx or yyy or zzz") w/o trailing '.'<br>
	*	the location list predicate is split by the DISLODGED_SPLIT_REGEX<br> 
	*	Lines are trimmed prior to parsing.<br>
	*	"in" or "over" used (Wing units are "over" provinces; armies/fleets "in")<br>
	*/
	public static final String DISLODGED_REGEX = "(?i)^the\\s+(\\p{Graph}*)\\s+(\\p{Graph}*)\\s*(?:in|over)\\s+(?:the)?\\s*([\\p{Graph}\\p{Blank}]*)\\s*can\\s+retreat\\s+to\\s+((.*))\\.$";
	
	/** splits the retreat list predicate; must be suitable for String.split() */
	public static final String DISLODGED_SPLIT_REGEX = "\\s+or\\s+";
	
	
	/**
	*	capture groups: 1:power, 2:unit, 3:unit location (may be multi-word, needs to be trim()'d)<br>
	*	NOTE: this may span >1 line; it ends with the period ('.')<br>
	*	Lines are trimmed prior to parsing.<br>
	*	"in" or "over" used (Wing units are "over" provinces; armies/fleets "in")<br>
	*/
	public static final String DESTROYED_REGEX = "(?i)^the\\s+(\\p{Graph}*)\\s+(\\p{Graph}*)\\s*(?:in|over)\\s+(?:the)?\\s*(([\\p{Graph}\\p{Blank}]*))with\\s+no\\s+valid\\s+retreats.*\\.";
	
	
	// INSTANCE VARIABLES
	private DislodgedInfo[] dislodgedInfo = null;
	private Phase phase = null;
	private String inputText = null;
	
	/*	// TEST PATTERN: DO NOT DELETE
	public static void main(String args[])
	throws IOException
	{
		
		// this is a GOOD test pattern
		//
		String in = 
			"Italy: Fleet Naples SUPPORT Fleet Tunis -> Ionian Sea.\n"+
			"Italy: Fleet Tunis -> Ionian Sea.\n"+
			"Italy: Army Rome SUPPORT Army Venice -> Apulia.\n"+
			"\n"+
			"Russia: Army Smyrna -> Constantinople.\n"+
			"\n"+
			"\n"+
			"The following units were dislodged:\n"+
			"\n"+
			"The 1Austrian Army in Apulia with no valid retreats was destroyed.\n"+
			"The 2Austrian Fleet in the Ionian Sea can retreat to Tyrrhenian Sea or Albania\n"+
			"or Greece or Eastern Mediterranean.\n"+
			"The 3French Army in Paris with no valid retreats was destroyed.\n"+
			"The 4German Fleet in the North Sea can retreat to Norwegian Sea or Skagerrak or\n"+
			"Belgium or London.\n"+
			"The 5Italian Fleet in Spain (south coast) can retreat to Portugal (north coast) or Western\n"+
			"Mediterranean.\n"+
			"The 6Chinese Fleet in the South Atlantic Ocean with no valid retreats was\n"+	
			"destroyed.\n"+
			"The 7Russian Army in Constantinople can retreat to Ankara or Smyrna.\n"+
			"The 8Soviet Army in St. Petersburg can retreat to Here or There.\n"+
			"The 9Unlucky Army in Badlands can retreat to St. Elsewhere or Picardy.\n"+	
			"The 10Germany Army in AAAAAA can retreat to BBBBBBB.\n"+
			"The 11Germany Army in NewAAAA can retreat to St. BBBBBBB.\n"+				
			"The 12Germany Army in ReallyNewAAAA can retreat to St.\n"+					
			"BBBBBBB.\n"+					
			"\n"+
			"The next phase of 'ferret' will be Retreats for Fall of 1906.\n"+
			"The deadline for orders will be Wed Apr 10 2002 17:54:23 -0500.\n";		

		
		DislodgedParser dp = new DislodgedParser(in);
		DislodgedInfo[] di = dp.getDislodgedInfo();
		System.out.println("di = "+di);
		System.out.println("length = "+di.length);
		for(int i=0; i<di.length; i++)
		{
			System.out.println(di[i]);
			System.out.println("");
		}
	}
	*/
	
	
	/** Creates a DislodgedParser object, which parses the given input for a Dislodged (retreat) information block */
	public DislodgedParser(Phase phase, String input)
	throws IOException
	{
		this.phase = phase;
		this.inputText = input;
		parseInput(input);
	}// DislodgedParser()
	
	
	/** Returns the dislodged units, or a zero-length array if no units were dislodged */
	public DislodgedInfo[] getDislodgedInfo()
	{
		return dislodgedInfo;
	}// getRetreatInfo()
	
	
	
	/**
	*	A DislodgedInfo object is created for each dislodged unit.
	*	<p>
	*	Dislodged units may be destroyed (and have no valid retreat locations) or 
	*	may have a retreat location
	*
	*
	*/
	public static class DislodgedInfo
	{
		private final String power;
		private final String src;
		private final String unit;
		private final String[] retreatLocs;	// zero-length if destroyed 
		
		/** Create a DislodgedInfo object */
		public DislodgedInfo(String power, String unit, String src, String[] retreatLocs)
		{
			this.power = power;
			this.unit = unit;
			this.src = src;
			this.retreatLocs = (retreatLocs == null) ? EMPTY : retreatLocs;
		}// DislodgedInfo()
		
		/** Name of the Power */
		public String getPowerName()				{ return power; }
		/** Location of the unit */
		public String getSourceName()				{ return src; }
		/** Type Name of the unit (e.g., "Fleet") */
		public String getUnitName()					{ return unit; }
		/** Names of valid retreat locations; zero-length if no valid retreats */
		public String[] getRetreatLocationNames()	{ return retreatLocs; }
		/** Indicates if unit was destroyed */
		public boolean isDestroyed() 				{ return (retreatLocs.length == 0); }
		
		
		/** String output for debugging; may change between versions. */
		public String toString()
		{
			StringBuffer sb = new StringBuffer();
			sb.append("DislodgedInfo[power=");
			sb.append(power);
			sb.append(", src=");
			sb.append(src);
			sb.append(", unit=");
			sb.append(unit);
			sb.append(", locNames=");
			for(int i=0; i<retreatLocs.length; i++)
			{
				sb.append(retreatLocs[i]);
				sb.append(',');
			}
			sb.append(']');
			return sb.toString();
		}// toString()
	}// nested class DislodgedInfo
	
	
	private void parseInput(String input)
	throws IOException
	{
		// Create HEADER_REGEX pattern, HEADER_END_REGEX pattern 
		Pattern header = Pattern.compile(HEADER_REGEX);
		Pattern endHeader = Pattern.compile(HEADER_END_REGEX);
		
		// search for HEADER_REGEX
		// keep searching until we find an empty line, or HEADER_END_REGEX.
		//
		BufferedReader br = new BufferedReader(new StringReader(input));
		StringBuffer accum = new StringBuffer(2048);
		
		String line = br.readLine();
		while(line != null)
		{
			Matcher m = header.matcher(line);
			if(m.lookingAt())
			{
				boolean inBlock = false;
				line = br.readLine();
				while(line != null)
				{
					line = line.trim().toLowerCase();
					if(line.length() > 0)
					{
						// if we are 'end header regex', we end
						// though typically having a zero-length trimmed line will do that too
						//
						Matcher endM = endHeader.matcher(line);
						if(endM.lookingAt())
						{
							break;
						}
						
						// If a trimmed line doesn't start with "the" (DISLODGED_LINE_START) (ignoring case) 
						// then add it to the line above. Otherwise, start a new line.
						// this makes regex matching MUCH easier, since all lines are 
						// 'normalized'; recognized patterns are not split
						if(line.startsWith(DISLODGED_LINE_START))
						{
							accum.append('\n');
						}
						else
						{
							accum.append(' ');
						}
						
						accum.append(line);
					}
					else
					{
						if(inBlock)
						{
							inBlock = false;
							break;	// escape inner while
						}
						else
						{
							inBlock = true;
						}
					}
					
					line = br.readLine();
				}
				
				accum.append('\n');	// end-of-text newline
				
				break;	// escape outer while
			}
			
			line = br.readLine();
		}
		
		// cleanup
		br.close();
		line = null;
		header = null;
		
		//System.out.println("(DislodgedParser) text:");
		//System.out.println("||>>"+accum.toString()+"<<||");
		//System.out.println("-----");
		
		// create a list of Dislodged units
		List disList = new LinkedList();
		
		// Create patterns
		Pattern destroyed = Pattern.compile(DESTROYED_REGEX);
		Pattern dislodged = Pattern.compile(DISLODGED_REGEX);
		
		
		// parse accum line-by-line, looking for DESTROYED_REGEX and
		// DISLODGED_REGEX.
		//
		StringTokenizer st = new StringTokenizer(accum.toString(), "\n");
		while(st.hasMoreTokens())
		{
			line = st.nextToken();
			
			//System.out.println("LINE: "+line);
			
			Matcher m = destroyed.matcher(line);
			if(m.lookingAt())
			{
				disList.add(new DislodgedInfo( 
					m.group(1),
					m.group(2),
					ParserUtils.filter( m.group(3).trim() ),
					null));
			}
			else
			{
				m = dislodged.matcher(line);
				if(m.lookingAt())
				{
					// parse location-list predicate
					String[] retreatLocs = m.group(4).split(DISLODGED_SPLIT_REGEX);
					for(int i=0; i<retreatLocs.length; i++)
					{
						retreatLocs[i] = ParserUtils.filter(retreatLocs[i]);
					}
					
					disList.add(new DislodgedInfo( 
						m.group(1),
						m.group(2),
						ParserUtils.filter(m.group(3).trim()),
						retreatLocs));
				}
			}
		}
		
		dislodgedInfo = (DislodgedInfo[]) disList.toArray(new DislodgedInfo[disList.size()]);
	}// parseInput()
		
	
	
	
}// class RetreatParser
	
	
	


		
