//
//  @(#)ParserUtils.java	1.00	6/2002
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

import java.io.*;
import java.util.*;
/**
*	Utilities & Constants used by the parser classes
*
*
*
*/
public class ParserUtils
{
	/** Lines less than this long are ignored. */
	public static final int SHORT_LINE = 10;
	
	
	
	/**
	*	Begins reading text line-by-line<p>
	*	Starts with first non-short line, adds text until we get a blank line, returns
	*	text in-between. Lines are trimmed.
	*/
	public static String parseBlock(BufferedReader br)
	throws IOException
	{
		// create first block: the ownership block
		StringBuffer accum = new StringBuffer(2048);
		boolean inBlock = false;
		
		String line = br.readLine();
		while(line != null)
		{
			if(line.length() > SHORT_LINE)
			{
				accum.append(line.trim());	// NOTE: we trim lines!
				accum.append('\n');
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
		
		return accum.toString();
	}// parseBlock()
	
	
	/** Coalesces whitespace, and ensures that it's only spaces and not any other type */
	public static String filter(String in)
	{
		StringBuffer sb = new StringBuffer(in.length());
		for(int i=0; i<in.length(); i++)
		{
			char c = in.charAt(i);
			if(Character.isWhitespace(c))
			{
				if(i > 1 && !Character.isWhitespace(in.charAt(i-1)))
				{
					sb.append(' ');	// space
				}
			}
			else
			{
				sb.append(c);
			}
		}
		return sb.toString();
	}// filter()
	
	
	/** Gets the next non-short line from a buffered reader. Trims it as well. Returns null if EOF. */
	public static String getNextLongLine(BufferedReader br)
	throws IOException
	{
		String line = br.readLine();
		while(line != null)
		{
			if(line.length() > SHORT_LINE)
			{
				line = line.trim();
				break;
			}
			
			line = br.readLine();
		}
		
		return line;
	}// getNextLongLine()
	
	
	
	private ParserUtils()
	{
	}// class ParserUtils
}// class ParserUtils
