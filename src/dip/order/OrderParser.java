//
// @(#)OrderParser.java	12/2002
//
// Copyright 2002 Zachary DelProposto. All rights reserved.
// Use is subject to license terms.
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
//  Or from http://www.gnu.org/package dip.order.result;
//
package dip.order;

import dip.world.*;
import java.util.StringTokenizer;
import java.util.Collection;
import java.util.ArrayList;
import dip.misc.Utils;
import dip.misc.Log;

/**
*	Parses text to create an Order object.
*	<p>
*	NOTE: this code is rather hackish (and in no way reflective of the 
*	rest of the code base). It is not expandable, or modular. However, it
*	is a pretty flexible parser in terms of what it will accept. As a point 
*	of history, it's the first piece of code written for jDip.
*	<p>
*	I am gradually replacing the most crusty parts with better code. For example, Coast now
*	normalizes coasts with regular expressions, and the code is far cleaner.
*	<p>
*	In the future, I anticipate we will have some sort of command-normalization (Order.normalize)
*	which will be implemented by Order subclasses, and some sort of pattern matching
*	that will allow the order to be matched to the tokens. This will allow Order classes to be modfied
*	or added without rewriting the OrderParser.
*	<p>
*	<b>note:</b> The parser is extremely tolerant of misspellings in single-word provinces. However,
*	it is not at all tolerant of multiword province misspellings. This is because it cannot easily 
*	recognize multi-word provinces. A pattern based parser could, and would be more robust in this
*	regard.
*	<p>
*	<pre>
*	HOLD:
*	<power>: <type> <s-prov> h 
*	
*	MOVE, RETREAT:
*	<power>: <type> <s-prov> m <d-prov>
*	<power>: m <s-prov> (to) <d-prov>
*	
*	SUPPORT:
*	<power>: <type> <s-prov> s <type> <s-prov>
*	<power>: <type> <s-prov> s <type> <s-prov> m <d-prov>
*	
*	CONVOY:
*	<power>: <type> <s-prov> c <type> <s-prov> m <d-prov>
*	
*	DISBAND:	
*	<power>: <type> <s-prov> d 
*
*	BUILD:	
*	<power>: <build> <type> <s-prov>
*
*	REMOVE:	
*	<power>: <remove> <type> <s-prov> 
*	
*
*	Where: 
*
*	<type> = "army", "a", "fleet", "f" or <empty>
*	<s-prov> = Source province.
*	<d-prov> = Destination province.
*	<power> = Power name or abbreviation of two or more characters.
*	<holds> = "h", "hold", "holds", "stand", "stands".
*	<moves> = "-", "->", "=>", "m", "move", "moves", "move to", "moves to".
*	<support> = "s", "support", "supports".
*	<convoy> = "c", "convoy", "convoys".
*	<disband> = "d", "disband".
*	<build> = "b", "build"
*	<remove> = "r", "remove" 
*
*	</pre>
*/
public class OrderParser 
{
	private static OrderParser instance = null;
	
	
	// il8n constants
	private static final String OF_POWER_NOT_RECOGNIZED = "OF_POWER_NOT_RECOGNIZED";
	private static final String OF_UNIT_NOT_RECOGNIZED = "OF_UNIT_NOT_RECOGNIZED";
	private static final String OF_PROVINCE_NOT_RECOGNIZED = "OF_PROVINCE_NOT_RECOGNIZED";
	private static final String OF_PROVINCE_UNCLEAR = "OF_PROVINCE_UNCLEAR";
	private static final String OF_NO_UNIT_IN_PROVINCE = "OF_NO_UNIT_IN_PROVINCE";
	private static final String OF_TOO_SHORT = "OF_TOO_SHORT";
	private static final String OF_INTERNAL_ERROR = "OF_INTERNAL_ERROR";
	private static final String OF_UNKNOWN_ORDER = "OF_UNKNOWN_ORDER";
	private static final String OF_CONVOY_NO_MOVE_OR_DEST = "OF_CONVOY_NO_MOVE_OR_DEST";
	private static final String OF_CONVOY_NO_DEST = "OF_CONVOY_NO_DEST";
	private static final String OF_CONVOY_NO_MOVE_SPEC = "OF_CONVOY_NO_MOVE_SPEC";
	private static final String OF_SUPPORT_NO_DEST = "OF_SUPPORT_NO_DEST";
	private static final String OF_SUPPORT_NO_MOVE = "OF_SUPPORT_NO_MOVE";
	private static final String OF_MISSING_DEST = "OF_MISSING_DEST";
	private static final String OF_BAD_FOR_POWER = "OF_BAD_FOR_POWER";
	private static final String OF_NO_ORDER_TYPE = "OF_NO_ORDER_TYPE";
	private static final String OF_POWER_LOCKED = "OF_POWER_LOCKED";
	private static final String OF_COAST_INVALID = "OF_COAST_INVALID";
	
	
	private static final String WHITESPACE = ": \t\n\r";
	
	
	// the order of replacements is very important!
	// all must be in lower case!
	private static final String REPLACEMENTS[][] = 
	{
		// misc tiny words that people add
		// should NOT include 'to' because to can mean move; it's not always extraneous
		// must have spaces before and after
		{" in ", " "},
		{" an ", " "},
		{" of ", " "},
		{" on ", " "},
		{" is ", " "},
		// convert unit-type specifiers
		{"fleet", " f "},
		{"army", " a "},		
		{"wing", " w "},		
		// WAIVE orders. Waive may NOT be abbreviated as "W"; otherwise, 
		// w xxx (build a wing) may be confused as 'waive' 
		{"waives"," waive "},
		{"waive build "," waive "},		// e.g., waive build [province]; must come before "BUILD"
		{"waives build "," waive "},
		{"waive builds "," waive "},	// e.g., waive build [province]; must come before "BUILD"
		{"waives builds "," waive "},
		// adjustment order Remove (since it contains "move", must come before)
		{"remove"," r "},
		{"removes", " r "},
		// for MOVE orders; note that "->" must come before "-"
		// also, we MUST replace any "-" in coasts with a "/" first.
		{"-=>", " m "},
		{"=->", " m "},
		{"==>", " m "},
		{"-->", " m "},
		{"->", " m "},
		{"=>", " m "},
		{"-", " m "},
		{"\u2192", " m "},			// unicode ARROW as used by jDip
		{"retreats to ", " m "}, // NOTE: space after "to" to avoid ambiguity e.g., "army bre retreats tol"
		{"retreat to ", " m "},
		{"retreats", " m "}, 	// plural first
		{"retreat", " m "},
		{"moving to ", " m "},	// NOTE: space after "to" ...
		{"moves to ", " m "}, 	// NOTE: space after "to" to avoid ambiguity e.g., "army bre moves tol"
		{"moves to ", " m "}, 	// NOTE: space after "to" to avoid ambiguity e.g., "army bre moves tol"
		{"move to ", " m "}, 	// NOTE: plurals and longer entries MUST come before shorter entries
		{"moves", " m "}, 	
		{"move", " m "},
		{" mv ", " m "},		// for those that like unix
		{" attacks on ", " m "},	// we precede the following with a space, since they are nonstandard keywords
		{" attacks to ", " m "},
		{" attacks into ", " m "},
		{" attacks of ", " m "},
		{" attack on ", " m "},
		{" attack to ", " m "},
		{" attack into ", " m "},
		{" attack of ", " m "},
		{" attacks ", " m "},
		{" attack ", " m "},
		{" into ", " m "},		// prefixed with space (don't want to get the end of a province)
		{" to ", " m "},		// used as a substitute for 'move to'; space prefix here is also important
		// SUPPORT orders
		{"supports", " s "},
		{"support", " s "},
		{" to support", " s "},	// prefixed with space (to not get the end of another word)
		// HOLD orders
		{"holds", " h "},
		{"hold", " h "},
		{"stands", " h "},
		{"stand", " h "},
		// CONVOY orders
		{"convoys", " c "},
		{"convoy", " c "},
		{"transports", " c "},
		{"transport", " c "},
		// DISBAND orders	NOTE: 'remove' is up above (before 'move')
		{"disbands"," d "},
		{"disband"," d "},
		// various adjustment orders
		{"build"," b "},
		// this occurs after coast-normalization, so convert parens to spaces.
		{"(", " "},
		{")", " "}
	};
	
	
	// DELETION strings for preprocessor; must occur after coast normalization
	private static final String TODELETE[] = 
	{
		".", 	// periods often occur in coast specifiers (e.g., "n.c.")
		",",	// shouldn't have any commas, but shouldn't be harmful, either.
		"\"", 	// double-quotes filtered out
		"\'s",	// filter out possesives 
		"\'",	// filter out possesives / single quotes
		"(",	// parentheses will only get in the way.
		")",
	};
	
	
	private OrderParser()
	{
	}// OrderParser()
	
	
	
	/**
	*	Gets an OrderParser instance
	*
	*/
	public static synchronized OrderParser getInstance()
	{	
		if(instance == null)
		{
			instance = new OrderParser();
		}
		
		return instance;
	}// getInstance()
	
	
	/**
	* 	Parse an order to an Order object.
	* 	<p>
	*	There are several options to control parsing. 
	*	<pre>
	*	power: 	
	*			"default" power to assume; null if not required
	*	World:
	*			current world; needed for province/power matching
	*
	*	locked:	
	*			if true, only orders for the specified power are legal.
	*			if power==null then an IAE is thrown.
	*	guess:
	*			only works if (power==null) and power is NOT locked
	*			guesses power based upon source province
	*			Position (derived from world) must be accurate; since guessing depends 
	*			upon knowing the current position information, and phase information.
	*
	*
	*	States:
	*	
	*	Power		Locked	Guess	Result
	*	=====   	======	=====	=================================================
	*	null		false	false	VALID: but power must always be present in text to parse!
	*	null		false	true	VALID: power is based on source province, whether specified or not
	*	null		true	false	illegal
	*	null		true	true	illegal
	*
	*	(defined)	false	false	VALID: power must be specified, if not, assumes "Power" given
	*	(defined)	false	true	VALID: if power not specified, it is based on source province
	*	(defined)	true	false	VALID: power *always* is "Power" given
	*	(defined)	true	true	illegal
	*	</pre>
	*/
	public Order parse(OrderFactory orderFactory, String text, Power power, TurnState turnState, boolean locked, boolean guess)
	throws OrderException
	{
		if(orderFactory == null)
		{
			throw new IllegalArgumentException("null OrderFactory");
		}
		
		// check arguments
		if(locked && power == null)
		{
			throw new IllegalArgumentException("power/lock disagreement");
		}
		
		if(guess && (power != null || locked || turnState == null))
		{
			throw new IllegalArgumentException("if guess == true, conditions: turnState != null, power == null, and !locked must all be true");
		}
		
		Position position = turnState.getPosition();
		Map map = turnState.getWorld().getMap();
		String preText = preprocess(text, map);
		
		Log.println("OP: Input:", text);
		Log.println("OP: preprocessed:", preText);
		
		return parse(preText, position, map, power, turnState, orderFactory, locked, guess);				
	}// parse()
	
	
	
	
	
	
	/**
	*	The preprocessor normalizes the orders, converting various order entry 
	*	formats to a single order entry format that is more easily parsed.
	*/
	private String preprocess(String ord, Map map) throws OrderException
	{
		// create StringBuffer, after filtering the input string.
		// note that this step includes lower-case conversion.
		StringBuffer sb = filterInput(ord);
		
		// replace any long (2-word, via space or hyphen) province names
		// with shorter version. 
		// NOTE: this may be overkill, especially since it won't replace
		// *partial* province names, like "North-atl"
		map.replaceProvinceNames(sb);
		
		// filter out power names [required at beginning to filter out power names
		// with odd characters such as hyphens]. Excludes first token.
		map.filterPowerNames(sb);
		
		// normalize coasts (Converts to /Xc format)
		//Log.println("OP: pre-coast normalization:", sb);
		
		try
		{
			String ncOrd = Coast.normalize( sb.toString() );
			sb.setLength(0);
			sb.append(ncOrd);
		}
		catch(OrderException e)
		{
			Log.println("OrderException: order: ",sb);
			throw new OrderException(Utils.getLocalString(OF_COAST_INVALID, e.getMessage()));
		}
		
		//Log.println("OP: post-coast normalization:", sb);
		
		
		// get the 'power token' (or null).
		// this is so if a power name has odd characters in it (e.g., chaos map)
		// they do not undergo replacement.
		String ptok = map.getFirstPowerToken(sb);
		final int startIdx = (ptok == null) ? 0 : ptok.length();
		
		// string replacement
		for(int i=0; i<REPLACEMENTS.length; i++)
		{
			int idx = startIdx;
			int start = sb.indexOf(REPLACEMENTS[i][0], idx);
			
			while(start != -1)
			{
				int end = start + REPLACEMENTS[i][0].length();
				sb.replace(start, end, REPLACEMENTS[i][1]);
				
				// repeat search
				idx = start + REPLACEMENTS[i][1].length();
				start = sb.indexOf(REPLACEMENTS[i][0], idx);
			}
		}
		
		// delete unwanted characters
		delChars(sb, TODELETE);
		
		// re-replace, after conversion
		map.replaceProvinceNames(sb);
		
		// filter out power names; often occurs in 'support' orders.
		// could also appear in a convoy order as well
		// e.g.: France: F gas SUPPORT British F iri HOLD
		// or 						   "Britain's"   which would be converted to "Britain" by delChars()
		// this does NOT filter out the first power name!! (which may be required)
		map.filterPowerNames(sb);
		
		return sb.toString();
	}// preprocess()
	
	
	private Order parse(String ord, Position position, Map map, Power defaultPower, 
						TurnState turnState, OrderFactory orderFactory, boolean locked, 
						boolean guessing) 
	throws OrderException
	{
		// Objects common to ALL order types.
		String srcName = null;
		String srcUnitTypeName = null;
		Power power = defaultPower;
		
		// current token for parsing
		String token = null; 
		
		StringTokenizer st = new StringTokenizer(ord, WHITESPACE, false);
		
		
		// Power parsing
		
		// see if first token is a power; if so, parse it
		power = map.getFirstPower(ord);
		//Log.println("OP:parse(): first token a power? ", power);
		
		// eat up the token (we don't want to reparse it), but 
		// only if it's NOT null (probably not a power)
		if(power != null)
		{
			getToken(st);	// eat token
			//String pTok = getToken(st);
			//Log.println("  OP:parse(): eating token: ", pTok);
		}
		
		// if we're not allowed to guess, and power is null, error.
		if(!guessing && power == null)
		{
			Log.println("OrderException: order: ", ord);
			String pTok = getToken(st);
			throw new OrderException(Utils.getLocalString(OF_POWER_NOT_RECOGNIZED, pTok));
		}
		
		// reset power, if null, to default (if specified)
		power = (power == null) ? defaultPower : power;
		
		// if we are locked, the power must be the default power
		if(locked)
		{
			assert(power != null);
			
			if(!power.equals(defaultPower))	
			{
				Log.println("OrderException: order: ", ord);
				throw new OrderException(Utils.getLocalString(OF_POWER_LOCKED, defaultPower));
			}
		}
		
		// NOTE: power may be null at this point, iff guessing==true.
		// in this case, it is upto the order-processing logic to parse
		// get the correct order from the source region.
		// decide if first token is src, src type, or adjustment order
		// adjustment orders have a different syntax from other orders
		// parse the src type [if any]
		//
		token = getToken(st);
		if(isTypeToken(token))
		{
			srcUnitTypeName = token;
			token = getToken(st);
		}
		else if(isCommandPrefixed(token))
		{
			return parseCommandPrefixedOrders(orderFactory, position, map, power, token, st, guessing, turnState);
		}
		
		
		// parse the src province
		srcName = token;
		
		// parse the order type -- if this is missing, we
		// have a 'defineState' order type
		String orderType = null;
		if(st.hasMoreTokens())
		{
			orderType = getToken(st, Utils.getLocalString(OF_NO_ORDER_TYPE));
		}
		else
		{
			orderType = "definestate";
		}
		
		
		// create objects for Source, and SourceUnit which 
		// occur for all orders.
		Unit.Type srcUnitType = parseUnitType(srcUnitTypeName);
		Location src = parseLocation(map, srcName);
		assert(src != null);
		assert(srcUnitType != null);
		
		// if we are guessing, guess the power from the source.
		// return an error if we cannot..
		//
		// ALSO, if user specified a power, and "guess" is true,
		// and the guessed power != specified, throw an exception.
		if(guessing)
		{
			// getPowerFromLocation() should throw an exception if no unit present.
			Power tempPower = getPowerFromLocation(false, position, turnState, src);
			if(power != null)
			{
				if( !tempPower.equals(power) )
				{
					Log.println("OrderException: order: ", ord);
					throw new OrderException(Utils.getLocalString(OF_BAD_FOR_POWER, power));
				}
			}
			
			power = tempPower;
		}
		
		assert(power != null);
		
		
		// create order based on order type
		if(orderType.equals("h"))
		{
			// HOLD order
			// <power>: <type> <s-prov> h 
			return orderFactory.createHold(power, src, srcUnitType);
		}
		else if(orderType.equals("m"))
		{
			return parseMoveOrder(map, turnState, position, orderFactory, st, 
						power, src, srcUnitType, false);
		
		}
		else if(orderType.equals("s"))
		{
			// SUPPORT order
			// <power>: <type> <s-prov> s <type> <s-prov>
			// <power>: <type> <s-prov> s <type> <s-prov> [h]
			// <power>: <type> <s-prov> s <type> <s-prov> m <d-prov>
			//
			// get type and/or support source names
			TypeAndSource tas = getTypeAndSource(st);
			
			// parse supSrc / supUnit
			Unit.Type supUnitType = parseUnitType(tas.type);
			Location supSrc = parseLocation(map, tas.src);
			
			assert (supUnitType != null);
			assert (supSrc != null);
			
			// get power from unit, if possible
			Power supPower = null;
			if(position.hasUnit(supSrc.getProvince()))
			{
				supPower = position.getUnit(supSrc.getProvince()).getPower();
			}
			
			// support a MOVE [if specified]
			if(st.hasMoreTokens())
			{
				token = st.nextToken();

				if(token.equals("m"))
				{
					String supDestName = getToken(st, Utils.getLocalString(OF_SUPPORT_NO_DEST));
					Location supDest = parseLocation(map, supDestName);
					assert(supDest != null);
					return orderFactory.createSupport(power, src, srcUnitType, 
						supSrc, supPower, supUnitType, supDest);
				}
				else if(!token.equals("h"))
				{
					// anything BUT a hold is ok. 
					Log.println("OrderException: order: ", ord);
					throw new OrderException(Utils.getLocalString(OF_SUPPORT_NO_MOVE));
				}
			}

			// support a HOLD
			return orderFactory.createSupport(power, src, srcUnitType, supSrc, 
				supPower, supUnitType);
		}
		else if(orderType.equals("c"))
		{
			// CONVOY order
			// <power>: <type> <s-prov> c <type> <s-prov> m <d-prov>
			// get type and/or support source
			TypeAndSource tas = getTypeAndSource(st);
			String conSrcName = tas.src;
			String conUnitName = tas.type;
			
			// verify that there is an "m" 
			token = getToken(st, Utils.getLocalString(OF_CONVOY_NO_MOVE_OR_DEST));
			if(!token.equalsIgnoreCase("m"))
			{
				Log.println("OrderException: order: ", ord);
				throw new OrderException(Utils.getLocalString(OF_CONVOY_NO_MOVE_SPEC));
			}
			
			// get the destination
			String conDestName = getToken(st, Utils.getLocalString(OF_CONVOY_NO_DEST));
			
			// parse convoy src/dest/type
			Location conSrc = parseLocation(map, conSrcName);
			Location conDest = parseLocation(map, conDestName);
			Unit.Type conUnitType = parseUnitType(conUnitName);
			
			// get power, from unit 
			Power conPower = null;
			if(position.hasUnit(conSrc.getProvince()))
			{
				conPower = position.getUnit(conSrc.getProvince()).getPower();
			}
			
			// create order.
			return orderFactory.createConvoy(power, src, srcUnitType,
				conSrc, conPower, conUnitType, conDest);
		}
		else if(orderType.equals("d"))
		{
			// DISBAND order
			return createDisbandOrRemove(orderFactory, turnState, true, power, src, srcUnitType);
		}
		else if(orderType.equals("definestate"))
		{
			return orderFactory.createDefineState(power, src, srcUnitType);
		}
		
		Log.println("OrderException: order: ", ord);
		throw new OrderException(Utils.getLocalString(OF_UNKNOWN_ORDER, orderType));
	}// parse
	
	
	
	/**
	*	Parses the "rest" of a move/retreat order; this finds 
	*	the destination location, and the location-list if it is
	*	a convoyed move. It also checks for the "by convoy" or "via convoy"
	*	phrase that signfies a convoyed move.
	*	<p>
	*	ignoreFirstM will ignore a token called "m" if set to true.
	*	<p>
	*	This will return a Move or Retreat order, or throw an OrderException.
	*/
	private Order parseMoveOrder(Map map, TurnState turnState, Position position,
						OrderFactory orderFactory, StringTokenizer st, 
						Power srcPower, Location srcLoc, Unit.Type srcUnitType,
						boolean ignoreFirstM)
	throws OrderException
	{	
		// MOVE order, or RETREAT order, if we are in RETREAT phase. If so, we can ignore the convoy stuff.			
		// <power>: <type> <s-prov> m <d-prov>
		//
		boolean isExplicitConvoy = false;	// "by convoy" or "via convoy" present
		boolean isConvoyedMove = false;		// multiple 'move' locations
		String destName = getToken(st, Utils.getLocalString(OF_MISSING_DEST));
		
		// eat possible first "M" if allowed and repeat dest-getting attempt
		if(ignoreFirstM && destName.equals("m"))
		{
			destName = getToken(st, Utils.getLocalString(OF_MISSING_DEST));
		}
		
		// 
		// We do 2 things in this loop:
		// (1) JUDGE compatibility:
		// 		check and see if we have 'multiple' destinations; e.g.,
		// 		"A STP-BAR-NRG-NTH-YOR" we BAR is not the dest, YOR is.
		// 		so we will keep parsing until we come across the last valid
		// 		province. A move specifier ('m') *MUST* occur before each province.
		// (2) "via convoy" or "by convoy" checking
		// 		
		ArrayList al = null;
		if(st.hasMoreTokens())
		{
			al = new ArrayList();
			al.add(srcLoc.getProvince());
			// parse first destination (and add to array list)
			al.add( parseLocation(map, destName).getProvince() );
		}
		
		while(st.hasMoreTokens())
		{
			String token = st.nextToken();
			if(token.equals("m"))
			{
				destName = getToken(st, Utils.getLocalString(OF_MISSING_DEST));
				Location pathLoc = parseLocation(map, destName);
				assert(pathLoc != null);
				al.add(pathLoc.getProvince());
				isConvoyedMove = true;
			}
			else if((token.equals("via") || token.equals("by")) && st.hasMoreTokens())
			{
				token = st.nextToken();
				if(token.equals("c"))
				{
					isExplicitConvoy = true;  // we are convoying!
				}
			}
		}	
		
		// final destination
		Location dest = parseLocation(map, destName);
		assert(dest != null);
		
		if(turnState.getPhase().getPhaseType() == Phase.PhaseType.RETREAT)
		{
			return orderFactory.createRetreat(srcPower, srcLoc, srcUnitType, dest);
		}
		else
		{
			if(isConvoyedMove)	// MUST test this first -- it overrides isExplicitConvoy
			{
				assert(al != null);
				Province[] convoyRoute = (Province[]) al.toArray(new Province[al.size()]);
				return orderFactory.createMove(srcPower, srcLoc, srcUnitType, dest, convoyRoute);
			}
			else if(isExplicitConvoy)
			{
				return orderFactory.createMove(srcPower, srcLoc, srcUnitType, dest, isExplicitConvoy);
			}
			else
			{
				// implicit convoy [determiend by Move.validate()] or nonconvoyed move order
				return orderFactory.createMove(srcPower, srcLoc, srcUnitType, dest);
			}
		}
	}// parseMoveOrder()
	
	
	
	/**
	*	Parse command-prefixed orders (e.g., "Build army paris"). This typically
	*	applies to adjustment orders, however, we also allow Move orders to be
	*	specified this way.
	*
	*/
	private Order parseCommandPrefixedOrders(OrderFactory orderFactory, Position position, 
		Map map, Power power, String orderType, StringTokenizer st, 
		boolean guessing, TurnState turnState) 
	throws OrderException
	{
		// these orders have a command-specifier BEFORE unit/src information
		if(orderType.equals("waive"))
		{
			// WAIVE order
			// <power>: <waive> <province>
			TypeAndSource tas = getTypeAndSource(st);	// we ignore 'type', but let it be specified
			Location src = parseLocation(map, tas.src);
			if(guessing)
			{
				power = getPowerFromLocation(true, position, turnState, src);
			}
			return orderFactory.createWaive(power, src);
		}
		else if(orderType.equals("b"))
		{
			// BUILD order
			// <power>: BUILD <type> <s-prov> 
			TypeAndSource tas = getTypeAndSource(st);
			Location src = parseLocation(map, tas.src);
			Unit.Type unitType = parseUnitType(tas.type);
			if(guessing)
			{
				power = getPowerFromLocation(true, position, turnState, src);
			}
			
			return orderFactory.createBuild(power, src, unitType);
		}
		else if(orderType.equals("r"))
		{
			// REMOVE order
			// <power>: REMOVE <type> <s-prov>
			TypeAndSource tas = getTypeAndSource(st);
			Location src = parseLocation(map, tas.src);
			Unit.Type unitType = parseUnitType(tas.type);
			if(guessing)
			{
				power = getPowerFromLocation(true, position, turnState, src);
			}
			
			return createDisbandOrRemove(orderFactory, turnState, false, power, src, unitType);
		}
		else if(orderType.equals("m"))
		{
			// MOVE order: command-first version
			// <power>: m <unit> <location> m <location>
			// example: "france: move army paris to gascony"
			// or: "move army paris-gascony"
			TypeAndSource srcTas = getTypeAndSource(st);
			Location src = parseLocation(map, srcTas.src);
			Unit.Type srcUnitType = parseUnitType(srcTas.type);
			if(guessing)
			{
				power = getPowerFromLocation(true, position, turnState, src);
			}
			
			return parseMoveOrder(map, turnState, position, orderFactory, st,
					power, src, srcUnitType, true);
		}
		else if(orderType.equals("d"))
		{
			// DISBAND: command-first version
			// <power>: DISBAND <type> <s-prov> 
			TypeAndSource tas = getTypeAndSource(st);
			Location src = parseLocation(map, tas.src);
			Unit.Type unitType = parseUnitType(tas.type);
			if(guessing)
			{
				power = getPowerFromLocation(true, position, turnState, src);
			}
			
			return createDisbandOrRemove(orderFactory, turnState, true, power, src, unitType);
		}
		throw new IllegalArgumentException(Utils.getLocalString(OF_INTERNAL_ERROR, orderType));
	}// parseCommandPrefixedOrders()
	
	
	private String getToken(StringTokenizer st, String error) throws OrderException
	{
		if(st.hasMoreTokens())
		{
			return st.nextToken();
		}
		else
		{
			throw new OrderException(error);
		}
	}// getToken()
	
	
	private String getToken(StringTokenizer st) throws OrderException
	{
		return getToken(st, Utils.getLocalString(OF_TOO_SHORT));
	}// getToken()}
	
	
	/**
	*	Derives the power based upon the location of the source unit. We have a  
	*	special flag (isAdjToken) which should be set to TRUE if we are parsing
	*	an adjustment-phase order, and false otherwise.
	*
	*/
	private Power getPowerFromLocation(boolean isAdjToken, Position position, TurnState turnState, Location source)
	throws OrderException
	{
		Province province = source.getProvince();
		Phase phase = turnState.getPhase();
		
		if(phase.getPhaseType() == Phase.PhaseType.ADJUSTMENT && isAdjToken)
		{
			// adjustment phase
			// we are supposed to 'guess', so we guess by getting the owner of the supply center chosen.
			// NOTE: this is loose (supply center, rather than home supply center); validation will take
			// care of details.
			//
			// if a unit exists, assume remove, and use that power; otherwise, assume a build.
			// 
			if(position.hasUnit(province))
			{
				return position.getUnit(province).getPower();
			}
			else
			{
				assert(position.getSupplyCenterOwner(province) != null);
				return position.getSupplyCenterOwner(province);
			}
		}
		else
		{
			// retreat / movement phases:
			Unit unit = (phase.getPhaseType() == Phase.PhaseType.RETREAT) ? position.getDislodgedUnit(province) : position.getUnit(province); 
			if(unit != null)
			{
				return unit.getPower();
			}
		}
		
		throw new OrderException(Utils.getLocalString(OF_NO_UNIT_IN_PROVINCE, province));
	}// getPowerFromLocation()
	
	
	/** Determine if a Token is a Unit.Type token */
	private boolean isTypeToken(String s)
	{
		if(s.equals("f") || s.equals("a") || s.equals("w"))
		{
			return true;
		}
		
		return false;
	}// isTypeToken
	
	// deletes any strings in the stringBuffer that match
	// strings specified in toDelete
	private void delChars(StringBuffer sb, String[] toDelete)
	{
		for(int i=0; i<toDelete.length; i++)
		{
			int idx = sb.indexOf(toDelete[i]);
			while(idx != -1)
			{
				sb.delete(idx, idx + toDelete[i].length());
				idx = sb.indexOf(toDelete[i], idx);
			}
		}
	}// delChars()
	
	/**
	*	Filters out any ISO control characters; improves the
	*	robustness of pasted text parsing. Also replaces any
	*	whitespace with a true space character. Returns a new
	*	StringBuffer. 
	*	<p>
	*	Also trims and lowercases the input, too
	*/
	private StringBuffer filterInput(String input)
	{
		input = input.trim();
		
		StringBuffer sb = new StringBuffer(input.length());
		
		// delete control chars and whitespace conversion
		for(int i=0; i<input.length(); i++)
		{
			final char c = input.charAt(i);
			
			if( Character.isWhitespace(c) )
			{
				sb.append(' ');
			}
			else if( !Character.isIdentifierIgnorable(c) )
			{
				sb.append( Character.toLowerCase(c) );
			}
		}
		
		return sb;
	}// delChars()
	
	/**
	*	Some orders have the verb (command) at the beginning; e.g.:
	*	"Build army france". We also allow move orders
	*	to be specified this way, but most commonly 
	*	adjustment orders are specified this way.
	*
	*/
	private boolean isCommandPrefixed(String s)
	{
		// b,r,w = build, remove, waive
		// m = move
		if( s.equalsIgnoreCase("b") 
		   	|| s.equalsIgnoreCase("r") 
			|| s.equalsIgnoreCase("d")
			|| s.equalsIgnoreCase("m")
			|| s.equalsIgnoreCase("waive") )
		{
			return true;
		}
		
		return false;
	}// isCommandPrefixed

	private TypeAndSource getTypeAndSource(StringTokenizer st) throws OrderException
	{
		// given a StringTokenize, parse the next token
		// to determine if it is a type (Army or Fleet).
		// if it is missing, sets token to null, and sets
		// source token.
		TypeAndSource tas = new TypeAndSource();
		String token = getToken(st);
		
		if(isTypeToken(token))
		{
			tas.type = token;
			tas.src = getToken(st);
		}
		else
		{
			tas.src = token;
		}	
		
		return tas;
	}// getTypeAndSource()
	
	private class TypeAndSource
	{
		public String type = null;
		public String src = null;
	}// inner class TypeAndSource
	
	
	/**
	*	Parses a Location; never returns a null Location;
	*	will throw an exception if the Location is unclear
	*	or not recognized. This is similar to Map.parseLocation()
	*	except that more detailed error information is returned.
	*	<p>
	*	<b>THIS ASSUMES COASTS HAVE ALREADY BEEN NORMALIZED WITH
	*	Coast.normalize()</b>
	*/
	private Location parseLocation(Map map, String locName)
	throws OrderException
	{
		// parse the coast
		Coast coast = Coast.parse(locName);	// will return Coast.UNDEFINED at worst
		
		// parse the province. if there are 'ties', we return the result.
		final Collection col = map.getProvincesMatchingClosest(locName);
		final Province[] provinces = (Province[]) col.toArray(new Province[col.size()]);
		
		
		if(provinces.length == 0)
		{
			// nothing matched! we didn't recognize.
			throw new OrderException(Utils.getLocalString(OF_PROVINCE_NOT_RECOGNIZED, locName));
		}
		else if(provinces.length == 1)
		{
			return new Location(provinces[0], coast);
		}
		else if(provinces.length == 2)
		{
			// 2 matches... means it's unclear!
			throw new OrderException(Utils.getLocalString(OF_PROVINCE_UNCLEAR, 
				locName, provinces[0], provinces[1]));
		}
		else
		{
			// multiple matches! unclear. give a more detailed error message.
			// create a comma-separated list of all but the last.
			StringBuffer sb = new StringBuffer(128);
			for(int i=0; i<(provinces.length-1); i++)
			{
				sb.append(provinces[i]);
				sb.append(", ");
			}
			
			throw new OrderException(Utils.getLocalString(OF_PROVINCE_UNCLEAR, 
				locName, 
				sb.toString(), 
				provinces[provinces.length - 1]));
		}
	}// parseLocation()
	
	
	

	
	//
	// uses unit.Type.parse()
	//	f/fleet -> FLEET
	//	a/army -> ARMY
	//  w/wing -> WING
	//	null -> UNDEFINED
	//	any other	-> null
	//
	private Unit.Type parseUnitType(String unitName) throws OrderException
	{
		Unit.Type unitType = Unit.Type.parse(unitName);
		if(unitType == null)
		{
			throw new OrderException(Utils.getLocalString(OF_UNIT_NOT_RECOGNIZED, unitName));
		}
		
		return unitType;
	}// parseUnitType()
	
	private Power parsePower(Map map, String powerName) throws OrderException
	{
		Power power = map.getPowerMatching(powerName);
		if(power == null)
		{
			throw new OrderException(Utils.getLocalString(OF_POWER_NOT_RECOGNIZED, powerName));
		}
		
		return power;
	}// parsePower()


	/** 
	*	Creates a Disband or Remove order, depending upon the phase.
	*	Since some people use "Disband" to mean "Remove" and vice-versa, 
	*	but jDip interprets them differently (Disband is for retreat 
	*	phase, Remove is for adjustment phase). If the phase is not
	*	adjustment or retreat, we create the 'desired' order, so that 
	*	the error message is correct.
	*/
	private Order createDisbandOrRemove(OrderFactory orderFactory, TurnState ts, 
		boolean disbandPreferred, Power power, Location src, Unit.Type unitType)
	throws OrderException
	{
		if(ts.getPhase().getPhaseType() == Phase.PhaseType.RETREAT)		
		{
			return orderFactory.createDisband(power, src, unitType);
		}
		else if(ts.getPhase().getPhaseType() == Phase.PhaseType.ADJUSTMENT)		
		{
			return orderFactory.createRemove(power, src, unitType);
		}
		
		if(disbandPreferred)
		{
			return orderFactory.createDisband(power, src, unitType);
		}
		else
		{
			return orderFactory.createRemove(power, src, unitType);
		}
	}// createDisbandOrRemove()
	
	
}// class OrderParser
