//	
//	 @(#)OrderFormat.java			4/2002
//	
//	 Copyright 2002-2004 Zachary DelProposto. All rights reserved.
//	 Use is subject to license terms.
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
//  Or from http://www.gnu.org/package dip.order.result;
//
package dip.order;

import dip.world.*;
import dip.misc.Log;

import java.util.*;
import java.lang.reflect.*;

/**
*	OrderFormat formats orders according to the specified format string. 
*	<p>
*	While OrderFormat is more flexible than using Order.toBriefString() or
*	Order.toFullString(), it is also considerably slower. This is should not 
*	generally be a problem, unless one is doing multiple adjudications
*	(e.g., for an AI).	
*	<p>
*	OrderFormat uses keywords that are delimited by braces "{}". Valid keywords
*	are described below. Any text (including whitespace) not in braces will be
*	output literally, without modification.
*	<p>
*	Example: (Hold order)<br>
*	<code>{getPower()}: {getSourceUnitType()} {getSource()} {:orderName}</code>
*	<p>
*	<ul>
*		<li><b>{field}</b> inserts the class field, after converting 
*			to a String. If null, an empty String ("") is returned. Fields
*			need not be public, but, they cannot be within a superclass.</li>
*		<li><b>{method()}</b> inserts the value returned by a class method, 
*			after converting to a String. If null, an empty String ("") 
*			is returned. Methods must be public, and without paremeters. The
*			current class and all superclasses are searched.</li>
*		<li><b>{_keyword_}</b> inserts a defined OrderFormat keyword. These
*			are also known as "static" keywords, since they are not preceded
*			by a field or method.</li>
*		<li><b>{field:modifier}</b> or <b>{method():modifier}</b> handle the 
*			output from the given field or method in a given manner. See
*			below for defined modifiers.</li>
*		<li><b>{field:?TRUE:FALSE}</b> or <b>{method():?TRUE:FALSE}</b> handles
*			the output from a given field or method as a boolean. If the result
*			is null, or the primitive boolean value <code>false</code>, 
*			the text in "FALSE" is displayed. If non-null or boolean-true, the
*			text in TRUE may be displayed. Note that the following expressions
*			{field:?:FALSE} and {field:?TRUE:} are valid; the former will 
*			display an empty value when <code>true</code>, and the latter 
*			an empty value when <code>false</code></li>. Nested statements are
*			will cause an error. However, keywords may be present in a true
*			or false clause, but only by themselves.
*	</ul>
*	<p>
*	Keywords (these do not modify fields or methods)
*	<ul>
*		<li><b>{_arrow_}</b> displays the Movement arrow, as defined by
*				OrderFormatOptions.</li>
*		<li><b>{_orderName_}</b> displays the name of an Order (e.g., "Hold")
*	</ul>
*	<p>
*	Modifiers; these must be preceded by a field or method.
*	<ul>
*		<li><b>:showPossesivePower</b> if preceded by a method or field that
*				is a Power, only displays if OrderFormatOptions allows.</li>
*		<li><b>:adjective</b> if preceded by a method or field that is a Power,
*				displays the adjective rather than the noun (e.g., "French"
*				instead of "France").</li>
*		<li><b>:coast</b> if preceded by a Location, 
*				returns just the Coast.</li>
*		<li><b>:province</b> if preceded by a Location, returns just the 
*				Province.</li>
*		<li><b>:path</b> if preceded by an Array of Locations, or Array of
*				Provinces, displays the provinces (no coasts) separated by
*				arrows (as defined by {:arrow}).
*	</ul>
*	<p>
*	If an object is of a type usable by format() (e.g., Location, Coast,
*	Province, Power, Unit, or OrderName), it will be formatted according to
*	OrderFormatOptions before being output. If not, it will be converted to
*	a String (via Object.toString()) before being output.
*	<p>
*	Methods are also available to format individual (non-order) components 
*	(such as Provinces, Locations, and Coasts) according to rules defined
*	by OrderFormatOptions.
*/
public class OrderFormat
{
	
	
	
	// keywords
	private final static String ARROW = "_arrow_";
	private final static String ORDERNAME = "_orderName_";
	
	
	// variable-modifying keywords
	private final static String SHOW_POWER	= "showPossesivePower";
	private final static String POWER_ADJECTIVE	= "adjective";
	private final static String LOC_COAST = "coast";
	private final static String LOC_PROVINCE = "province";
	private final static String PATH = "path";
	
	// all non-modifying keywords
	private final static String[] ALL_NONMOD_KEYWORDS = {
		ARROW,
		ORDERNAME
	};
	
	// all modifying keywords
	private final static String[] ALL_MOD_KEYWORDS = {
		SHOW_POWER,
		POWER_ADJECTIVE,
		LOC_COAST,
		LOC_PROVINCE,
		PATH
	};
	
	
	// misc. constants
	private final static String EMPTY = "";
	private final static String KEYWORD_ERROR = "!keyword_error!";
	
	
	/**
	*	For null values, when debugging, print the word "null"
	*	followed by the type (indicated by cls).
	*/
	private static String handleNull(Class cls)
	{
		assert (cls != null);
		StringBuffer sb = new StringBuffer(64);
		sb.append("null(");
		sb.append(cls.getName());
		sb.append(")");
		return sb.toString();
	}// handleNull()
	
	
	/**
	*	Apply style transformations to the given String, 
	*	returning a transformed String.
	*/
	private static String applyStyle(final int originalStyle, final String input)
	{
		if(input == EMPTY)
		{
			return input;
		}
		
		// style MUST be a valid OrderFormat STYLE_ constant
		//
		final boolean isPlural = ((originalStyle - 10) >= 0);
		final int style = (isPlural ? (originalStyle - 10) : originalStyle);
		String text = input;
		
		// pluralize, but not if input is empty
		if(isPlural && input.length() > 0)
		{
			text = text+"s";
		}
		
		switch(style)
		{
			case OrderFormatOptions.STYLE_LOWER:
				text = text.toLowerCase();
				break;
			case OrderFormatOptions.STYLE_UPPER:
				text = text.toUpperCase();
				break;
			case OrderFormatOptions.STYLE_TITLE:
				text = toTitleCase(text, false);
				break;
			case OrderFormatOptions.STYLE_TITLE_ALL:
				text = toTitleCase(text, true);
				break;
			default:
				// do nothing
		}
		
		
		return text;
	}// applyStyle()
	
	
	/**
	*	Converts the input to Title case. If allWords is true, all
	*	words are converted to Title case, instead of just the first
	*	word. 
	*/
	private static String toTitleCase(final String input, boolean allWords)
	{
		final StringBuffer sb = new StringBuffer(input.length());
		
		boolean isInWord = false;
		boolean lastState = false;
		for(int i=0; i<input.length(); i++)
		{
			char c = input.charAt(i);
			isInWord = Character.isLetterOrDigit(c);
			
			if(isInWord && lastState != isInWord)
			{
				c = Character.toTitleCase(c);
			}
			
			sb.append(c);
			lastState = isInWord;
			
			// if not all words, loop no more.
			if(!allWords)
			{
				sb.append(input.substring(i+1, input.length()));
				break;
			}
		}
		
		return sb.toString();
	}// toTitleCase()
	
	
	/**
	*	Format a Coast given the order formatting parameters.
	*/
	public static String format(OrderFormatOptions ofo, Coast coast)
	{
		if(ofo == null)
		{
			throw new IllegalArgumentException();
		}
		
		String text = null;
		
		if(coast == null)
		{
			text = (ofo.isDebug() ? handleNull(Coast.class) : EMPTY);
		}
		else
		{
			if(Coast.isDisplayable(coast))
			{
				switch(ofo.getCoastFormat())
				{
					case OrderFormatOptions.FORMAT_BRIEF:
						text = coast.getAbbreviation();
						break;
					case OrderFormatOptions.FORMAT_FULL:
						text = coast.getName();
						break;
					case OrderFormatOptions.FORMAT_COAST_PAREN_BRIEF:
						{
							StringBuffer sb = new StringBuffer(4);
							sb.append('(');
							sb.append(coast.getAbbreviation());
							sb.append(')');
							text = sb.toString();
						}
						break;
					case OrderFormatOptions.FORMAT_COAST_PAREN_FULL:
						{
							StringBuffer sb = new StringBuffer(16);
							sb.append('(');
							sb.append(coast.getName());
							sb.append(')');
							text = sb.toString();
						}
						break;
					default:
						throw new IllegalStateException();
				}
			}
			else if(ofo.isDebug())
			{
				text = coast.getAbbreviation();
			}
			else
			{
				text = EMPTY;
			}
		}
		
		text = applyStyle(ofo.getCoastStyle(), text);
		
		assert (text != null);
		return text;
	}// format()
	
	
	
	/**
	*	Format a Province given the order formatting parameters.
	*/
	public static String format(OrderFormatOptions ofo, Province province)
	{
		if(ofo == null)
		{
			throw new IllegalArgumentException();
		}
		
		String text = null;
		
		if(province == null)
		{
			text = (ofo.isDebug() ? handleNull(Province.class) : EMPTY);
		}
		else
		{
			switch(ofo.getProvinceFormat())
			{
				case OrderFormatOptions.FORMAT_BRIEF:
					text = province.getShortName();
					break;
				case OrderFormatOptions.FORMAT_FULL:
					text = province.getFullName();
					break;
				default:
					throw new IllegalStateException();
			}
		}
		
		text = applyStyle(ofo.getProvinceStyle(), text);
		
		assert (text != null);
		return text;
	}// format()
	
	
	/**
	*	Format a Unit Type given the order formatting parameters.
	*/
	public static String format(OrderFormatOptions ofo, Unit.Type unitType)
	{
		if(ofo == null)
		{
			throw new IllegalArgumentException();
		}
		
		String text = null;
		
		if(unitType == null)
		{
			text = (ofo.isDebug() ? handleNull(Unit.Type.class) : EMPTY);
		}
		else
		{
			switch(ofo.getUnitFormat())
			{
				case OrderFormatOptions.FORMAT_BRIEF:
					text = unitType.getShortName();
					break;
				case OrderFormatOptions.FORMAT_FULL:
					text = unitType.getFullName();
					break;
				default:
					throw new IllegalStateException();
			}
		}
		
		text = applyStyle(ofo.getUnitStyle(), text);
		
		assert (text != null);
		return text;
	}// format()
	
	/**
	*	Format a Power given the order formatting parameters.
	*	<p>
	*	<b>Note:</b> FORMAT_BRIEF is not yet supported for Power names.
	*/
	public static String format(OrderFormatOptions ofo, Power power)
	{
		if(ofo == null)
		{
			throw new IllegalArgumentException();
		}
		
		String text = null;
		
		if(power == null)
		{
			text = (ofo.isDebug() ? handleNull(Unit.Type.class) : EMPTY);
		}
		else
		{
			switch(ofo.getPowerFormat())
			{
				case OrderFormatOptions.FORMAT_BRIEF:
					text = power.getName();
					break;
				case OrderFormatOptions.FORMAT_FULL:
					text = power.getName();
					break;
				default:
					throw new IllegalStateException();
			}
		}
		
		text = applyStyle(ofo.getPowerStyle(), text);
		
		assert (text != null);
		return text;
	}// format()
	
	
	/**
	*	Format a Location given the order formatting parameters.
	*/
	public static String format(OrderFormatOptions ofo, Location loc)
	{
		if(ofo == null)
		{
			throw new IllegalArgumentException();
		}
		
		if(loc == null)
		{
			return (ofo.isDebug() ? handleNull(Location.class) : EMPTY);
		}
		else
		{
			StringBuffer sb = new StringBuffer(64);
			
			sb.append( format(ofo, loc.getProvince()) );
			
			final String coastText = format(ofo, loc.getCoast());
			if(!EMPTY.equals(coastText))
			{
				sb.append( ofo.getCoastSeparator() );
				sb.append( coastText );
			}
			
			
			return sb.toString();
		}
	}// format()
	
	
	/**
	*	Formats an Order Name (obtained from an Order) 
	*	given the order formatting parameters.
	*/
	public static String formatOrderName(OrderFormatOptions ofo, Orderable order)
	{
		if(ofo == null)
		{
			throw new IllegalArgumentException();
		}
		
		String text = null;
		
		if(order == null)
		{
			text = (ofo.isDebug() ? handleNull(Orderable.class) : EMPTY);
		}
		else
		{
			switch(ofo.getOrderNameFormat())
			{
				case OrderFormatOptions.FORMAT_BRIEF:
					text = order.getBriefName();
					break;
				case OrderFormatOptions.FORMAT_FULL:
					text = order.getFullName();
					break;
				default:
					throw new IllegalStateException();
			}
		}
		
		text = applyStyle(ofo.getOrderNameStyle(), text);
		
		assert (text != null);
		return text;
	}// formatOrderName()
	
	
	
	
	
	
	
	
	
	
	
	
	
	/**
	*	Process text within braces.
	*	1) check if non-modifying keyword
	*	2) parse, check for method/variable, +/- boolean, +/- modifier
	*	3) return resultant text
	*/
	private static String procBraceText(final OrderFormatOptions ofo, 
		final Orderable order, final String text)
	{
		if(text == null)
		{
			throw new IllegalArgumentException();
		}
		
		Object out = null;
		
		out = procStaticKeyword(ofo, order, text);
		if(out == null)
		{
			final String[] tokens = text.split(":", 3);
			
			if(tokens.length == 0)
			{
				Log.println("OrderFormat: cannot parse: {", text, "}");
				return EMPTY;
			}
			
			// proc first token
			out = getViaReflection(order, tokens[0]);
			
			// process modifier OR boolean
			if(tokens.length > 1)
			{
				// evaluate boolean expression
				if(tokens[1].startsWith("?"))
				{
					boolean isTrue = false;
					if(out instanceof Boolean)
					{
						isTrue = ((Boolean) out).booleanValue();
					}
					else
					{
						isTrue = (out != null);
					}
					
					if(isTrue)
					{
						assert (tokens[1].length() > 0);
						final String tok = tokens[1].substring(1);
						final Object obj = procStaticKeyword(ofo, order, tok);
						return (obj == null) ? tok : obj.toString();
					}
					else
					{
						if(tokens.length == 2)
						{
							// {xxx:?true:} [empty 'false' clause]
							return EMPTY;
						}
						else
						{
							assert (tokens.length == 3);
							final Object obj = procStaticKeyword(ofo, order, tokens[2]);
							return (obj == null) ? tokens[2] : obj.toString();
						}
					}
				}
				else
				{
					//process via modifier
					out = procModKeyword(ofo, order, out, tokens[1]);
				}
			}
		}
		
		// process Object into a (formatted) String
		if(out == null)
		{
			return EMPTY;
		}
		else if(out instanceof Power)
		{
			return format(ofo, (Power) out);
		}
		else if(out instanceof Coast)
		{
			return format(ofo, (Coast) out);
		}
		else if(out instanceof Province)
		{
			return format(ofo, (Province) out);
		}
		else if(out instanceof Location)
		{
			return format(ofo, (Location) out);
		}
		else if(out instanceof Unit.Type)
		{
			return format(ofo, (Unit.Type) out);
		}
		else
		{
			// convert object to a String
			return out.toString();
		}
	}// procBraceText
	
	
	/**
	*	Get the method or field via reflection. Returns null if an 
	*	error occured.
	*/
	private static Object getViaReflection(final Orderable order, final String name)
	{
		assert (order != null);
		assert (name != null);
		
		final Class cls = order.getClass();
		final boolean isMethod = (name.endsWith("()"));
		
		if(isMethod)
		{
			try
			{
                                /*
                                 * NOTE: Originally second argment in getMethod was "null"
                                 *  but it's deprecated. Delete this comment when commit.
                                 *
                                 * Original:
                                 *  return cls.getMethod(name.substring(0, name.length()-2),
				 *	null).invoke(order, null);
                                 * More info:
                                 * http://blogs.sun.com/sundararajan/entry/varargs_and_reflection_1_5
                                 */
				return cls.getMethod(name.substring(0, name.length()-2), 
					(Class [])null).invoke(order, (Object[])null);
			}
			catch(Exception e)
			{
				Log.println("OrderFormat::getViaReflection() cannot reflect method \"",name,"\"");
				Log.println("OrderFormat::getViaReflection() exception details:\n",e);
			}
		}
		else
		{
			try
			{
				return cls.getDeclaredField(name).get(order);
			}
			catch(Exception e)
			{
				Log.println("OrderFormat::getViaReflection() cannot reflect field \"",name,"\"");
				Log.println("OrderFormat::getViaReflection() exception details:\n",e);
			}
		}
		
		return null;
	}// getViaReflection()
	
	
	/**
	*	Process a keyword that does NOT require any input.
	*	These are, essentially, constants.
	*/
	private static Object procStaticKeyword(final OrderFormatOptions ofo, 
		Orderable order, final String keyWord)
	{
		if(keyWord.startsWith("_") && keyWord.endsWith("_"))
		{
			if(keyWord.equals(ARROW))
			{
				return ofo.getArrow();
			}
			else if(keyWord.equals(ORDERNAME))
			{
				return formatOrderName(ofo, order);
			}
		}
		
		return null;
	}// procStaticKeyword()
	
	
	private static Object procModKeyword(final OrderFormatOptions ofo, 
		final Orderable order, final Object input, final String keyword)
	{
		if(keyword.equals(SHOW_POWER))
		{
			if(input == null)
			{
				return EMPTY;
			}
			else if(input instanceof Power && order != null)
			{
				// only show possessive power if it is not the same as the
				// source power AND we are set to show posessive powers.
				if( ofo.getShowPossessivePower()
					&& !order.getPower().equals((Power) input) )
				{
					return input;
				}
				else
				{
					return EMPTY;
				}
			}
		}
		else if(keyword.equals(POWER_ADJECTIVE))
		{
			if(input instanceof Power)
			{
				// get adjective for the power, instead of the power name.
				// apply power-formatting style
				String adj = ((Power) input).getAdjective();
				return applyStyle(ofo.getPowerStyle(), adj);
			}
		}
		else if(keyword.equals(LOC_COAST))
		{
			if(input instanceof Location)
			{
				return ((Location) input).getCoast();
			}
		}
		else if(keyword.equals(LOC_PROVINCE))
		{
			if(input instanceof Location)
			{
				return ((Location) input).getProvince();
			}
		}
		else if(keyword.equals(PATH))
		{
			if(input == null)
			{
				return EMPTY;
			}
			else if(input instanceof Location[])
			{
				final StringBuffer sb = new StringBuffer(128);
				final Location[] locs = (Location[]) input;
				for(int i=0; i<locs.length; i++)
				{
					sb.append( format(ofo, locs[i].getProvince()) );
					if(i < (locs.length - 1))
					{
						sb.append(' ');
						sb.append( ofo.getArrow() );
						sb.append(' ');
					}
				}
				return sb.toString();
			}
			else if(input instanceof Province[])
			{
				final StringBuffer sb = new StringBuffer(128);
				final Province[] provs = (Province[]) input;
				for(int i=0; i<provs.length; i++)
				{
					sb.append( format(ofo, provs[i]) );
					if(i < (provs.length - 1))
					{
						sb.append(' ');
						sb.append( ofo.getArrow() );
						sb.append(' ');
					}
				}
				return sb.toString();
			}
		}
		
		return KEYWORD_ERROR+keyword;
	}// procModKeyword()
	
	
	/**
	*	Formats an Order
	*/
	public static String format(final OrderFormatOptions ofo, 
		final Orderable order)
	{
		if(order == null)
		{
			return EMPTY;
		}
		
		return format(ofo, order.getDefaultFormat(), order);
	}// format()
	
	/**
	*	Formats an Order according to the specified order format options, 
	*	and the specified order format String
	*/
	public static String format(final OrderFormatOptions ofo, 
		final String format, final Orderable order)
	{
		if(ofo == null || format == null)
		{
			throw new IllegalArgumentException(ofo+","+format);
		}
		
		if(order == null)
		{
			return EMPTY;
		}
		
		
		StringBuffer output = new StringBuffer(256);
		StringBuffer accum = new StringBuffer(32);
		
		boolean inBrace = false;
		StringTokenizer st = new StringTokenizer(format, "{}", true);
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
				output.append( procBraceText(ofo, order, accum.toString()) );
				accum = new StringBuffer();
			}
			else
			{
				if(inBrace)
				{
					accum.append(tok);
				}
				else
				{
					output.append(tok);
				}
			}
		}
		
		if(ofo.getEndWithDot())
		{
			// only append a dot if we think the order is complete; this means
			// it should not end with a space or arrow.
			//
			final String str = output.toString();
			if(str.endsWith(" ") || str.endsWith(ofo.getArrow()))
			{
				return str;
			}
			else
			{
				output.append('.');
			}
		}
		
		return output.toString();
	}// format()
	
	
	/**
	*	Gets an example order, suitable for display in a user interface,
	*	using the given OrderFormatOptions. 
	*/
	public static String getFormatExample(OrderFormatOptions ofo, OrderFactory of)
	{
		// this is about the ONLY time Province or Power objects are
		// created using 'new'
		Province prov1 = new Province("Livonia", new String[]{"lvn"}, 0, false);
		Province prov2 = new Province("St. Petersburg", new String[]{"stp"}, 0, false);
		Province prov3 = new Province("Golf of Bothnia", new String[]{"gob"}, 0, false);
		
		Power power1 = new Power(new String[]{"Russia"}, "Russian", true);
		Power power2 = new Power(new String[]{"German"}, "German", true);
		
		Location src = 		new Location(prov1, Coast.SEA);
		Location supSrc = 	new Location(prov2, Coast.SOUTH);
		Location supDest = 	new Location(prov3, Coast.SEA);
		
		Support support = of.createSupport(power1, src, Unit.Type.FLEET, 
			supSrc, power2, Unit.Type.FLEET, supDest);
		
		return format(ofo, support);
	}// getFormatExample()
	
}// class OrderFormat
