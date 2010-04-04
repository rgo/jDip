//
//  @(#)HTMLFormat.java		4/2002
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
package dip.gui;

import java.util.*;
import java.text.*;

/**
*	Formats HTML text (or, really, any text) similar to MessageFormat but with
*	the following features:
*
<pre>
	1) pass a hashtable of key/value pairs
		keys MUST be String objects
		values can be any object that has a toString() method
	
	2) markup input text as follows:
		Simple Insertion:
		================
		{name} = insert 'name', via the 'toString' method
		
		Formatted Insertion:
		===================
		{date:name,<specifier>}
		{decimal:name,<specifier>}
			
			specifiers are optional; locale-specific formatting used.
		
		Loops:
		=====
		{for:name
			
			&#64;&#64;name&#64; 	<== insert name[position]
		}
		
		
		Reserved words:
			"date:"
			"decimal:"
			"for:"
		Reserved Symbols:
			"{", "}" 
			'&#64;' (in for loops only)
		
		
		
		what about arrays?
		index directly
		{name:3} 	3rd position of "name"
		{name:name2}  position "name2" in name (name2 must be an Integer)
		
		or:
		
		'for' loops for arrays
		{for:name
			blah blah blah HTML here
			&#64;name (inserts name[i])
		}
		
		keywords:
			for:name 			(uses array "name" for start/end)
			for:start:end
			
						conditions is always "&lt;"
						
						&#64;&#64;index 		current position
						&#64;&#64;indexplus	current position + 1 (for display purposes)
						&#64;&#64;variable&#64; 	
						&#64;&#64;variable&#64; 	
						
						
						
			date:		// format-specifier: date
			decimal: 	// format-specifier: decimal
</pre>
*/
public class HTMLFormat
{
	//
	private static final String VAR_PREFIX = "@@";
	
	// 
	private Map 			map = null;
	private StringBuffer	sb = null;
	
	// formatters
	private DecimalFormat decimalFormat = null;
	private SimpleDateFormat dateFormat = null;
	private String defaultDecimalPattern = null;
	private String defaultDatePattern = null;
	
	
	
	public static HTMLFormat getInstance()
	{
		return new HTMLFormat();
	}// getInstance()
	
	
	public String format(String html, Map map)
	{
		this.map = map;
		sb.setLength(0);
		
		StringBuffer accum = new StringBuffer(1024);
		
		boolean inBrace = false;
		StringTokenizer st = new StringTokenizer(html,"{}",true);
		while(st.hasMoreTokens())
		{
			String tok = st.nextToken();
			if("{".equals(tok) && !inBrace)
			{
				inBrace = true;
			}
			else if("}".equals(tok) && inBrace)
			{
				inBrace = false;
				parseBetweenBraces(accum.toString());
				accum.setLength(0);
			}
			else
			{
				if(inBrace)
				{
					accum.append(tok);
				}
				else
				{
					sb.append(tok);
				}
			}
		}
		
		return sb.toString();
	}// format()
	
	private void parseBetweenBraces(String text)
	{
		// decision loop: check for keywords
		if(text.startsWith("decimal:"))
		{
			replaceDecimal(text);
		}
		else if(text.startsWith("date:"))
		{
			replaceDate(text);
		}
		else if(text.startsWith("for:"))
		{
			replaceFor(text);
		}
		else 
		{
			// simple replace, unless we have a ':'; 
			// then we need to see if we've got an array...
			int colonIndex = text.indexOf(':');
			if(colonIndex != -1)
			{
				replaceArray(text, colonIndex);
			}
			else
			{
				replaceSimple(text);
			}
		}
	}// parseBetweenBraces()
	
	
	private void replaceSimple(String key)
	{
		Object lookup = map.get(key);
		sb.append(lookup.toString());
	}// replaceSimple()
	
	
	private void replaceDate(String text)
	{
		int colonIdx = text.indexOf(':');
		int commaIdx = text.indexOf(',');
		String key = null;
		String spec = null;
		
		if(commaIdx == -1)
		{
			key = text.substring(colonIdx+1);
			dateFormat.applyLocalizedPattern(defaultDatePattern);			
		}
		else
		{
			key = text.substring(colonIdx+1, commaIdx);
			spec = text.substring(commaIdx+1);
			dateFormat.applyLocalizedPattern(spec);
		}
		
		Date date = (Date) map.get(key);
		sb.append(dateFormat.format(date));
	}// replaceDate()	
	
	
	private void replaceDecimal(String text)
	{
		int colonIdx = text.indexOf(':');
		int commaIdx = text.indexOf(',');
		String key = null;
		String spec = null;
		
		if(commaIdx == -1)
		{
			key = text.substring(colonIdx+1);
			decimalFormat.applyLocalizedPattern(defaultDecimalPattern);			
		}
		else
		{
			key = text.substring(colonIdx+1, commaIdx);
			spec = text.substring(commaIdx+1);
			decimalFormat.applyLocalizedPattern(spec);
		}
		
		Number num = (Number) map.get(key);
		sb.append(decimalFormat.format(num));
	}// replaceDecimal()


	
	private void replaceArray(String text, int colonIdx)
	{
		String key = text.substring(0,colonIdx);
		String index = text.substring(colonIdx+1);
		Object[] objs = null;
		
		try
		{
			objs = (Object[]) map.get(key);
		}
		catch(ClassCastException e)
		{
			System.err.println("ERROR: HTMLFormat: value for key \""+key+"\" not an array.");
			return;
		}
		
		if(index.length() > 0)
		{
			sb.append( objs[parseOrLookupInt(index)] );
		}
		else
		{
			System.err.println("ERROR: HTMLFormat: invalid index given for array value key \""+key+"\"");
		}
	}// replaceArray()
	
	
	private void replaceFor(String text)
	{
		// get 'for' parameters
		//	for:name 			(uses array "name" for start/end)
		//	for:start:end
		int start = 0;
		int end = 0;
		
		String tok0 = null;
		String tok1 = null;
		
		StringTokenizer st = new StringTokenizer(text.substring(0,text.indexOf(' ')), ":", false);
		st.nextToken(); 	// for:  this has already been detected
		
		if(st.hasMoreTokens())
		{
			tok0 = st.nextToken();
		}
		else
		{
			System.err.println("HTMLFormat: for: loop without start/end parameters!");
			return;
		}
		
		if(st.hasMoreTokens())
		{
			tok1 = st.nextToken();
		}
		
		// name based or index-based
		if(tok1 == null)
		{
			// only one token; name-based
			// derive start & end from this variable (which must be an array)				
			try
			{
				Object[] objs = (Object[]) map.get(tok0);
				start = 0;
				end = objs.length;
			}
			catch(ClassCastException e)
			{
				System.err.println("ERROR: HTMLFormat: for: parameter \""+tok0+"\" is not an array!");
				return;
			}
		}
		else
		{
			// two tokens; index-based (start:end)
			start = parseOrLookupInt(tok0);
			end = parseOrLookupInt(tok1);
		}
		
		// start of text block for loop; everything before this
		// is for::: statement; first space cuts this off.
		text = text.substring(text.indexOf(' '));
		
		// create stringbuffer
		StringBuffer iterText = new StringBuffer(text.length() + 256);
		
		// iterate the loop; each time, go through and replace @@ variables
		// with the new values, and add this to the main string buffer.
		for(int i=start; i<end; i++)
		{
			// copy text
			iterText.setLength(0);
			iterText.append(text);
			
			// perform replacements
			replaceAll(iterText, "@@indexplus", String.valueOf(i+1));
			replaceAll(iterText, "@@index", String.valueOf(i));
			replaceAllVariables(iterText, i);
			
			// append
			sb.append(iterText);
		}
	}// replaceFor()
	
	
	
	private int parseInt(String text)
	{
		try
		{
			return Integer.parseInt(text);
		}
		catch(NumberFormatException e)
		{
			System.err.println("HTMLFormat: could not parse \""+text+"\" as Integer");
		}
		
		return 0;
	}// parseInt()
	
	private int parseIntegerFromMap(String key)
	{
		Object obj = map.get(key);
		if(obj instanceof Integer)
		{
			try
			{
				return ((Integer)obj).intValue();
			}
			catch(NumberFormatException e)
			{
			}
		}
		
		System.err.println("HTMLFormat: could not parse key \""+key+"\" as Integer");
		return 0;
	}// parseIntegerFromMap()
	
	// if 'in' starts with a valid java identifier, look it up, and return the value
	// if 'in' starts with a digit, parse it, and return the value
	private int parseOrLookupInt(String in)
	{
		if(Character.isJavaIdentifierStart(in.charAt(0)))
		{
			return parseIntegerFromMap(in);
		}
		
		// index is a constant
		return parseInt(in);
	}// parseOrLookupInt
	
	
	// probably should be in Utils.java :: also used by OrderParser
	//
	private void replaceAll(StringBuffer in, String find, String replace)
	{
		int idx = 0;
		int start = in.indexOf(find, idx);
		
		while(start != -1)
		{
			int end = start + find.length();
			in.replace(start, end, replace);
			
			// repeat search
			idx = start + replace.length();
			start = in.indexOf(find, idx);
		}
	}// replacedAll()
	
	// looks for "@@" and if followed by any text, followed by "@";
	// looks it up; if array, replace with indexed, otherwise, just print.
	//
	private void replaceAllVariables(StringBuffer in, int forIndex)
	{
		int idx = 0;
		int start = in.indexOf(VAR_PREFIX, idx);
		
		while(start != -1)
		{
			String replace = "";
			int end = in.indexOf( "@", (start + VAR_PREFIX.length()) );
			
			if(end - start > 48)
			{
				// probably not valid; skip
				continue;
			}
			
			if(end == -1)
			{
				break;
			}
			
			String key = in.substring(start+VAR_PREFIX.length(), end);
			Object obj = map.get(key);
			
			if(obj != null)
			{
				if(obj.getClass().isArray())
				{
					Object[] array = (Object[]) obj;
					replace = array[forIndex].toString();
				}
				else
				{
					replace = obj.toString();
				}
			}
			
			in.replace(start, end+1, replace);
			
			
			// repeat search
			idx = start + replace.length();
			start = in.indexOf(VAR_PREFIX, idx);
		}
	}// replaceAllVariables()
	
	protected HTMLFormat()
	{
		sb = new StringBuffer(8192);
		decimalFormat = new DecimalFormat();
		dateFormat = new SimpleDateFormat();
		
		defaultDecimalPattern = decimalFormat.toPattern();
		defaultDatePattern = dateFormat.toPattern();
	}// HTMLFormat()
	
}// class HTMLFormat///
