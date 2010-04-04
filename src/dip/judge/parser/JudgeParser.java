//
//  @(#)JudgeParser.java	1.00	6/2002
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
import dip.misc.Utils;
import dip.order.OrderFactory;

import java.io.*;
import java.util.regex.*;
import java.util.*;
/**
*	First stage of Judge output parsing. Looks for the "::" line, determines 
*	game name, judge name, and variant type.
*	<p>
*	Then, looks to see (if present) the list of players and their email addresses.
*	<p>
*	Then, determines if input is a game listing or a game history. Sets flags, and 
*	if it is a game listing, returns the rest of the text. Determines if it is a 
*	game history by looking for a consecutive Date: / Subject: line pair
*	<p>
*	Parses the rest of the file into a String for sub-parsing.
*
*/
public class JudgeParser
{
	// constants
	private static final String JP_NO_COLONS = "JP.jp.nocolons";
	
	// instance variables
	private final static int READ_AHEAD_LENGTH = 7200;
	private BufferedReader reader;
	private OrderFactory orderFactory;
	
	private String judgeName;
	private String variantName;
	private String gameName;
	private Phase phase = null;
	
	private String[] playerEmails;
	private String[] playerNames;
	
	private String text = null;
	private String initialText = null;
	
	public static final String JP_TYPE_LISTING = "Listing";
	public static final String JP_TYPE_HISTORY = "History";
	public static final String JP_TYPE_RESULTS = "Results";
	public static final String JP_TYPE_GAMESTART = "Gamestart";
	public static final String JP_TYPE_UNDEFINED = "Undefined";
	
	private String type = JP_TYPE_UNDEFINED;
	
	/** Create a JudgeParser, and start parsing. */
	public JudgeParser(OrderFactory orderFactory, Reader r)
	throws IOException, PatternSyntaxException
	{
		this.orderFactory = orderFactory;
		reader = new BufferedReader(r, 8192);
		
		findDoubleColonLine();
		findPlayerList();
		determineType();
	}// JudgeParser()
	
	
	
	/** Get the name of the Judge */
	public String getJudgeName()		{ return judgeName; }
	/** Get the name of the Variant */
	public String getVariantName()		{ return variantName; }
	/** Get the name of the Game */
	public String getGameName()			{ return gameName; }
	/** Get the phase of the game */
	public Phase getPhase()			{ return phase; }
	
	/** Returns the list of players in the game, or a zero-length-array */
	public String[] getPlayerPowerNames()	{ return playerNames; }
	
	/** Returns the email address for each player in the game, or a zero-length array */
	public String[] getPlayerEmails()	{ return playerEmails; }
	
	/** Returns the type of this input */
	public String getType()				{ return type; }
	
	/** 
	*	Returns the rest of the text for further parsing.
	*	<p>
	*	This the "rest" of the text after parsing judge/player info; <br>
	*	if it is a history, it is every line INCLUDING and AFTER the first Date: line.
	*
	*/
	public String getText()				{ return text; }
	
	/** Prepend the given string in front of the stored text */
	public String prependText(String s) { text = s + text; return text; }
	
	/**
	*	For Listings, this is null. For Histories, this is the text after parsing game & player 
	*	information but PRIOR to parsing Date: lines and turns. It is useful for some judges, 
	*	where it will contain starting positions. 
	*
	*/
	public String getInitialText()		{ return initialText; }
	
	
	
	/**
	*	Searches the file for the first matching line starting with "::" that 
	*	has the Judge, Game, and Variant information. If these lines cannot be 
	*	found, an IOException is thrown.
	*
	*/
	private void findDoubleColonLine()
	throws IOException, PatternSyntaxException
	{
		// regex pattern
		// regex is case-insensitive
		// capture groups are in order
		// double () on last capture group because of buggy behavior!? WTF?
		Pattern pattern = Pattern.compile("\\W*judge\\W*(\\S*)\\W*game\\W*(\\S*)\\W*variant\\W*((\\S*))", Pattern.CASE_INSENSITIVE);
		
		// find :: line
		String line = reader.readLine();
		while(line != null)
		{
			if(line.trim().indexOf("::") >= 0)
			{
				// attempt to parse via regex. If it fails, read another line.
				Matcher m = pattern.matcher(line);
				if(m.find())
				{
					judgeName = m.group(1);
					gameName = m.group(2);
					variantName = m.group(3);
					return;
				}
			}
			
			line = reader.readLine();
		}
		
		// if we did not find any such line, indicate.
		throw new IOException(Utils.getLocalString(JP_NO_COLONS));
	}// findDoubleColonLine()
	
	
	/** 
	*	Searches the file for the player name list. The stream is marked, and
	*	is rewound if the list is not found. The list need not be present. 
	*/
	private void findPlayerList()
	throws IOException, PatternSyntaxException
	{
		// our pattern for finding the player list
		Pattern pattern = Pattern.compile("(?i)following players");
		
		reader.mark(READ_AHEAD_LENGTH);
		int count = 0;
		
		String line = reader.readLine();
		while(line != null && count < READ_AHEAD_LENGTH)
		{
			count += line.length();
			
			Matcher m = pattern.matcher(line);
			if(m.find())
			{
				LinkedList names = new LinkedList();
				LinkedList email = new LinkedList();
				
				// now read each player UNTIL we get an empty line (or of length < 4)
				line = reader.readLine();
				while(line != null)
				{
					if(line.length() < 4)
					{
						break;
					}
					else
					{
						StringTokenizer st = new StringTokenizer(line, ": \t\n\r\f");
						int nTok = st.countTokens();
						if(nTok == 0)
						{
							break;	// if line is somehow garbage....
						}
						
						for(int i=0; i<nTok; i++)
						{
							String tok = st.nextToken();
							
							if(i == 0)
							{
								names.add(tok);		// name is first token
							}
							else if(i == (nTok-1))
							{
								email.add(tok);		// email is the last token
							}	
						}
					}
					
					line = reader.readLine();
				}
				
				playerNames = (String[]) names.toArray(new String[names.size()]);
				playerEmails = (String[]) email.toArray(new String[email.size()]);
				return;
			}
			
			line = reader.readLine();
		}
		
		reader.reset();
		playerEmails = new String[0];
		playerNames = new String[0];
	}// findPlayerList()
	
	
	/**
	*	Determine if this is a listing or a history.
	*	History files will have a Date: and Subject: line; Listings will not.
	*	
	*	Resets mark as required 
	*/
	private void determineType()
	throws IOException, PatternSyntaxException
	{
		// are we are a history?
		// we will also be reading lines in pairs.
		String line = null;
		reader.reset();
		reader.mark(READ_AHEAD_LENGTH);
		int count = 0;
		
		// save the initial text; we will need this if we are a history;
		// if we are a listing, this will be null. This is all the text UP TO and EXCLUDING
		// the first "Date:" line.
		StringBuffer initSB = new StringBuffer(2048);
		
		line = reader.readLine();
		while(line != null && count < READ_AHEAD_LENGTH)
		{
			// first check: Date
			count += line.length();
			int pos1 = line.toLowerCase().indexOf("date:");
			if(pos1 >= 0 && pos1 < 10)
			{
				//System.out.println("Date Found");
				// read next line; should be "subject" line
				String line2 = reader.readLine();
				if(line2 == null)
				{
					break;
				}
				
				count += line2.length();
				
				// second check: Subject:
				int pos2 = line2.toLowerCase().indexOf("subject:");
				if(pos2 >= 0 && pos2 < 10)
				{
					type = JP_TYPE_HISTORY;
					
					StringBuffer sb = new StringBuffer(256);
					sb.append(line);
					sb.append('\n');
					sb.append(line2);
					
					// set the rest of the text.
					// prepend the already-parsed Date: and Subject: lines
					initialText = initSB.toString();
					makeRestOfText(sb.toString());
					return;
				}
			}
			else
			{
				// accumulate line
				initSB.append(line);
				initSB.append('\n');
			}
			
			line = reader.readLine();
		}
		
		
		// we are not a history. 
		// Next we try to find a result header.
		Pattern hm = Pattern.compile(JudgeOrderParser.MOVE_ORDER_HEADER);
		Pattern hr = Pattern.compile(JudgeOrderParser.RETREAT_ORDER_HEADER);
		Pattern ha = Pattern.compile(JudgeOrderParser.ADJUSTMENT_ORDER_HEADER);
		
		reader.reset();
		count = 0;
		line = reader.readLine();
		while(line != null && count < READ_AHEAD_LENGTH)
		{
			count += line.length();
			line = line.trim();	// needed for Patterns to work properly
			Matcher m_hm = hm.matcher(line);
			Matcher m_hr = hr.matcher(line);
			Matcher m_ha = ha.matcher(line);
			if(m_hm.lookingAt() || 
			   m_hr.lookingAt() ||
			   m_ha.lookingAt())
			{
				type = JP_TYPE_RESULTS;
				phase = Phase.parse(line.substring(0,line.indexOf(".")));
				break;
			}
			line = reader.readLine();
		}
		if (type == JP_TYPE_RESULTS)
		{
			// We are results. Get the text including the results header.
			initialText = null;
			reader.reset();
			makeRestOfText(null);
			return;
		}
		
		// Try to find a game starting message
		Pattern gs = Pattern.compile(JudgeOrderParser.GAME_STARTING_HEADER);
		Pattern sp = Pattern.compile(JudgeOrderParser.STARTING_POSITION_REGEX);

		reader.reset();
		count = 0;
		line = reader.readLine();
		while(line != null && count < READ_AHEAD_LENGTH)
		{
			count += line.length();
			Matcher m_gs = gs.matcher(line);
			Matcher m_sp = sp.matcher(line);
			if (m_gs.lookingAt())
			{
				type = JP_TYPE_GAMESTART;
			}
			if (m_sp.lookingAt() && (type == JP_TYPE_GAMESTART))
			{	
				phase = Phase.parse("Movement "+line.substring(0,line.indexOf(".")));
				break;
			}
			line = reader.readLine();
		}
		if (type == JP_TYPE_GAMESTART)
		{
			// the game is starting. Get the text.
			initialText = null;
			reader.reset();
			makeRestOfText(null);
			return;
		}
		
		// Assume we are a listing. Get the text.
		type = JP_TYPE_LISTING;
		initialText = null;
		reader.reset();
		makeRestOfText(null);
	}// determineType()
	
	
	/** Given the current position in the reader, get the rest of the text to the end of the input */
	private void makeRestOfText(String toPrepend)
	throws IOException
	{
		StringBuffer sb = new StringBuffer(16384);
		
		// prepend text first, if any
		if(toPrepend != null)
		{
			sb.append(toPrepend);
			sb.append('\n');
		}
		
		// read rest of text
		String line = reader.readLine();
		while(line != null)
		{
			sb.append(line);
			sb.append('\n');
			line = reader.readLine();
		}
		
		text = sb.toString();
	}// makeRestOfText()
	
}// class JudgeParser
