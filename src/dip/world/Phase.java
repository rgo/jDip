//
//  @(#)Phase.java		4/2002
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

import dip.misc.Utils;

import java.io.Serializable;
import java.io.Externalizable;
import java.io.IOException;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
/**
*
*	A Phase object represents when a turn takes place, and contains the
*	year, game phase (PhaseType), and Season information.
*	<p>
*	Phase objects are mutable and comparable.
*	<p>
*	PhaseType and SeasonType objects may be compared with referential equality.
*	(For example, "Phase.getSeasonType() == SeasonType.SPRING")
*
*/
public class Phase implements java.io.Serializable, Comparable
{
	// internal constants: describes ordering of phases
	// Setup is independent of this ordering.
	// ordering: (for a given year)
	//		spring movement, spring retreat, fall movement, fall retreat, fall adjustment
	// both these constants correspond, and must have equal sizes
	private static final SeasonType[] ORDER_SEASON = {SeasonType.SPRING, SeasonType.SPRING, 
													SeasonType.FALL, SeasonType.FALL, SeasonType.FALL};
													
	private static final PhaseType[] ORDER_PHASE = {PhaseType.MOVEMENT, PhaseType.RETREAT, 
													PhaseType.MOVEMENT, PhaseType.RETREAT, PhaseType.ADJUSTMENT};
	
	// formatter to always 4-digit format a year
	private static final DecimalFormat YEAR_FORMAT = new DecimalFormat("0000");
	
	
	// instance variables
	protected final SeasonType seasonType;
	protected final YearType yearType;
	protected final PhaseType phaseType;
	private transient int orderIdx;				// set by readResolve() when object de-serialized
	
	
	/**
	*	Create a new Phase.
	*/
	public Phase(SeasonType seasonType, int year, PhaseType phaseType)
	{
		this(seasonType, new YearType(year), phaseType);
	}// Phase()
	
	
	/**
	*	Create a new Phase.
	*/
	public Phase(SeasonType seasonType, YearType yearType, PhaseType phaseType)
	{
		if(seasonType == null || yearType == null || phaseType == null)
		{
			throw new IllegalArgumentException("invalid args");
		}
		
		this.orderIdx = deriveOrderIdx(seasonType, phaseType);
		if(orderIdx == -1)
		{
			throw new IllegalArgumentException("invalid seasontype/phasetype combination");
		}
		
		this.seasonType = seasonType;
		this.yearType = yearType;
		this.phaseType = phaseType;
	}// Phase()
	
	/** Create a new Phase, given a known index */
	protected Phase(YearType yt, int idx)
	{
		this.orderIdx = idx;
		this.yearType = yt;
		this.phaseType = ORDER_PHASE[idx];
		this.seasonType = ORDER_SEASON[idx];
	}// Phase()
	
	
	/** Returns the year */
	public int getYear()					{ return yearType.getYear(); }
	
	/** Returns the YearType */
	public YearType getYearType()			{ return yearType; }
	
	/** Returns the PhaseType */
	public PhaseType getPhaseType()			{ return phaseType; }
	
	/** Returns the SeasonType */
	public SeasonType getSeasonType() 		{ return seasonType; }
	
	/** Displays as a short String (e.g., F1902R) */
	public String getBriefName()
	{
		StringBuffer sb = new StringBuffer(6);
		sb.append(seasonType.getBriefName());
		sb.append(YEAR_FORMAT.format(yearType.getYear()));
		sb.append(phaseType.getBriefName());
		return sb.toString();
	}// getBriefName()
		
	/** Displays the phase as a String */
	public String toString()
	{
		StringBuffer sb = new StringBuffer(64);
		sb.append(seasonType);
		sb.append(", ");
		sb.append(yearType);
		sb.append(" (");
		sb.append(phaseType);
		sb.append(')');
		return sb.toString();
	}// toString()
	
	/** Returns true if the two phases are equivalent. */
	public boolean equals(Object obj)
	{
		Phase phase = (Phase) obj;
		if( yearType.equals(phase.yearType)
			&& seasonType.equals(phase.seasonType)
			&& phaseType.equals(phase.phaseType) )
		{
			return true;
		}
		return false;
	}// equals()
	
	/** 
	*	Compares the Phase to the given Phase object. Returns a negative, zero, or 
	*	positive integer depending if the given Phase is less than, equal, or 
	*	greater than (temporally) to this Phase.
	*/
	public int compareTo(Object obj)
	{
		Phase phase = (Phase) obj;
		int result = 0;
		
		// year is dominant
		result = yearType.compareTo(phase.yearType);
		if(result != 0)
		{
			return result;
		}
		
		// then season
		result = seasonType.compareTo(phase.seasonType);
		if(result != 0)
		{
			return result;
		}
		
		// finally, phase type.
		return phaseType.compareTo(phase.phaseType);
	}// compareTo()
	
	
	/** Get the phase that would be after to the current phase */
	public Phase getNext()
	{
		// advance the phase index by one, UNLESS we are over; then
		// advance the year and reset.
		int idx = orderIdx + 1;
		idx = (idx > ORDER_SEASON.length - 1) ? 0 : idx;
		YearType yt = (idx == 0) ? yearType.getNext() : yearType;
		
		return new Phase(yt, idx);
	}// getNext()
	
	
	/** Get the phase that would come before the current phase */
	public Phase getPrevious()
	{
		int idx = orderIdx - 1;
		YearType yt = (idx < 0) ? yearType.getPrevious() : yearType;
		idx = (idx < 0) ? (ORDER_SEASON.length - 1) : idx;
		
		return new Phase(yt, idx);
	}// getPrevious()
	
	
	/** given season/phase, derive the order index. If we cannot, our index is -1. */
	private int deriveOrderIdx(SeasonType st, PhaseType pt)
	{
		for(int i=0; i<ORDER_SEASON.length; i++)
		{
			if( ORDER_SEASON[i] == st
				&& ORDER_PHASE[i] == pt )
			{
				return i;
			}
		}
		
		return -1;
	}// deriveOrderIdx()
	
	
	/** 
	*	Determines if this phase is valid. Not all PhaseType and
	*	SeasonType combinations are valid.
	*/
	public static boolean isValid(SeasonType st, PhaseType pt)
	{
		for(int i=0; i<ORDER_SEASON.length; i++)
		{
			if( ORDER_SEASON[i] == st
				&& ORDER_PHASE[i] == pt )
			{
				return true;
			}
		}
		
		return false;
	}// isValid()
	
	
	
	
	/**
	*	Determines the Phase from a String.
	*	<p>
	*	Expects input in the following form(s):
	*	<p>
	*	Season, Year (Phase)<br>
	*	Season, Year [Phase]<br>
	*	SYYYYP  as a single 6-character token, e.g., F1900M = Fall 1900, Movement<br>
	*	<p>
	*	Whitespace: space, comma, colon, semicolon, [], (), tab, newline, return, quotes
	*	<p>
	*	The order is not important. If the combination is not valid (via isValid()), or if 
	*	any Phase component cannot be parsed, a null value is returned. Note that this is very 
	*	forgiving, but it does not allow any non-word tokens between what we look for.
	*/
	public static Phase parse(final String in)
	{
		SeasonType seasonType = null;
		YearType yearType = null;
		PhaseType phaseType = null;
		
		// special case: 6 char token (commonly seen in Judge input)
		// 'bc' years aren't allowed in 6 char tokens.
		if(in.length() == 6)
		{
			// parse season & phase
			seasonType = SeasonType.parse(in.substring(0,1));
			yearType = YearType.parse(in.substring(1,5));
			phaseType = PhaseType.parse(in.substring(5,6));
			
			if(seasonType == null || yearType == null || phaseType == null)
			{
				return null;
			}
		}
		else
		{
			// case conversion
			String lcIn = in.toLowerCase();
			
			// our token list (should be 3 or 4; whitespace/punctuation is ignored)
			ArrayList tokList = new ArrayList(10);
			
			// get all tokens, ignoring ANY whitespace or punctuation; StringTokenizer is ideal for this
			StringTokenizer st = new StringTokenizer(lcIn, " ,:;[](){}-_|/\\\"\'\t\n\r", false);
			while(st.hasMoreTokens())
			{
				tokList.add( st.nextToken() );
			}
			
			// not enough tokens (we need at least 3)
			if(tokList.size() < 3)
			{
				return null;
			}
			
			// parse until we run out of things to parse
			Iterator iter = tokList.iterator();
			while(iter.hasNext())
			{
				String tok = (String) iter.next();
				
				SeasonType tmpSeason = SeasonType.parse(tok);
				seasonType = (tmpSeason == null) ? seasonType : tmpSeason;
				
				PhaseType tmpPhase = PhaseType.parse(tok);
				phaseType = (tmpPhase == null) ? phaseType : tmpPhase;
				
				YearType tmpYear = YearType.parse(tok);
				yearType = (tmpYear == null) ? yearType : tmpYear;
			}
			
			if(yearType == null || seasonType == null || phaseType == null)
			{
				return null;
			}
			
			// 'bc' token may be 'loose'. If so, we need to find it, as the
			// YearType parser was fed only a single token (no whitespace)
			// e.g., "1083 BC" won't be parsed right, but "1083bc" will be.
			if(lcIn.indexOf("bc") >= 0 || lcIn.indexOf("b.c.") >= 0)
			{
				if(yearType.getYear() > 0)
				{
					yearType = new YearType(-yearType.getYear());
				}
			}
			
			// check season-phase validity
			if(!isValid(seasonType, phaseType))
			{
				return null;
			}
		}
		
		return new Phase(seasonType, yearType, phaseType);
	}// parse()
	
	
	
	/**
	*	Returns a String array, in order, of valid season/phase combinations.
	*	<p>
	*	E.g.: Spring Move, or Spring Adjustment, etc.
	*/
	public static String[] getAllSeasonPhaseCombos()
	{
		String[] spCombos = new String[ORDER_SEASON.length];
		for(int i=0; i<ORDER_SEASON.length; i++)
		{
			StringBuffer sb = new StringBuffer(64);
			sb.append(ORDER_SEASON[i].toString());
			sb.append(' ');
			sb.append(ORDER_PHASE[i].toString());
			spCombos[i] = sb.toString();
		}
		
		return spCombos;
	}// getAllSeasonPhaseCombos()
	
	
	
	
	/** Reconstitute a Phase object */
	protected Object readResolve()
	throws java.io.ObjectStreamException
	{
		this.orderIdx = deriveOrderIdx(this.seasonType, this.phaseType);
		return this;
	}// readResolve()
	
	
	
	
	
	
	
	
	
	
	
	
	/**
	*	Represents seasons
	*	<p>
	*	SeasonType constants should be used, rather than creating new SeasonType objects.
	*	
	*/
	public static class SeasonType implements Serializable, Comparable
	{
		// always-accepted english constants for SeasonTypes
		protected static final String CONST_SPRING 	= "SPRING";
		protected static final String CONST_FALL 	= "FALL";
		protected static final String CONST_SUMMER 	= "SUMMER";
		protected static final String CONST_WINTER 	= "WINTER";
		
		// internal constants
		protected static final String IL8N_SPRING = "SEASONTYPE_SPRING";
		protected static final String IL8N_FALL = "SEASONTYPE_FALL";
		
		// positions are spaced such that other seasons can be inserted easily
		protected static final int POS_SPRING = 1000;
		protected static final int POS_FALL = 2000;
		
		// Season Type Constants
		/** Spring season */
		public static final SeasonType SPRING = new SeasonType(IL8N_SPRING, POS_SPRING);
		/** Fall season */
		public static final SeasonType FALL = new SeasonType(IL8N_FALL, POS_FALL);
		/** SeasonType array
		*	<b>Warning: this should not be mutated.</b>
		*/
		public static final SeasonType[] ALL = { SPRING, FALL };
		
		// instance variables
		protected final int position;
		protected transient String displayName = null;
		
		
		/**
		*	Creates a new SeasonType
		*/
		protected SeasonType(String il8nKey, int pos)
		{
			this.position = pos;
			setDisplayName(il8nKey);
		}// SeasonType()
		
		
		/** Return the name of this season */
		public String toString()
		{
			return displayName;
		}// toString()
		
		
		/** Brief name of a Season (e.g., [F]all, [S]pring) */
		public String getBriefName()
		{
			if(this == SPRING)
			{
				return "S";
			}
			else if(this == FALL)
			{
				return "F";
			}
			
			return "?";
		}// getBriefName()
		
		/** Returns the hashCode */
		public int hashCode()
		{
			return position;
		}// hashCode()
		
		/** Returns <code>true</code> if SeasonType objects are equivalent */
		public boolean equals(Object obj)
		{
			if(obj == this)
			{
				return true;
			}
			if(obj instanceof SeasonType)
			{
				return ( this.position == ((SeasonType)obj).position );
			}
			
			return false;
		}// equals()
		
		
		/**
		*	Compares the order of two seasons.
		*	<p>
		*	Fall always follows Spring.
		*/
		public int compareTo(Object obj)
		{
			SeasonType st = (SeasonType) obj;
			return (position - st.position);
		}// compareTo()
		
		
		/** Get the next season */
		public SeasonType getNext()
		{
			if(this == SPRING)
			{
				return FALL;
			}
			else if(this == FALL)
			{
				return SPRING;
			}
			
			return null;
		}// getNext()
		
		
		/** Get the previous season */
		public SeasonType getPrevious()
		{
			if(this == SPRING)
			{
				return FALL;
			}
			else if(this == FALL)
			{
				return SPRING;
			}
			
			return null;
		}// getPrevious()
		
		
		/** 
		* 	Parse input to determine season; return null if input cannot be parsed
		* 	into a known SeasonType constant.
		*	<p>
		*	Note: SUMMER and WINTER are converted to Spring and Fall, respectively.
		*/
		public static SeasonType parse(final String in)
		{
			// short cases (1 letter); not i18n'd
			if(in.length() == 1)
			{
				String lcIn = in.toLowerCase();
				if("s".equals(lcIn))
				{
					return SPRING;
				}
				else if("f".equals(lcIn) || ("w".equals(lcIn)))
				{
					return FALL;
				}
				
				return null;
			}
			
			// typical cases
			if(in.equalsIgnoreCase(CONST_SPRING))
			{
				return SPRING;
			}
			else if(in.equalsIgnoreCase(CONST_FALL))
			{
				return FALL;
			}
			else if(in.equalsIgnoreCase(CONST_SUMMER))
			{
				return SPRING;
			}
			else if(in.equalsIgnoreCase(CONST_WINTER))
			{
				return FALL;
			}
			
			// il8n cases
			if(in.equalsIgnoreCase(Utils.getLocalString(IL8N_SPRING)))
			{
				return SPRING;
			}
			else if(in.equalsIgnoreCase(Utils.getLocalString(IL8N_FALL)))
			{
				return FALL;
			}
			
			return null;
		}// parse()
		
		
		
		
		/** Resolves the serialized reference into a constant */
		protected Object readResolve()
		throws java.io.ObjectStreamException
		{
			SeasonType st = null;
			
			if(position == POS_SPRING)
			{
				st = SPRING;
				st.setDisplayName(IL8N_SPRING);
			}
			else if(position == POS_FALL)
			{
				st = FALL;
				st.setDisplayName(IL8N_FALL);
			}
			
			return st;
		}// readResolve()
		
		/** Sets the internationalized name */
		private void setDisplayName(String key)
		{
			this.displayName = Utils.getLocalString(key);
		}// resetDisplayName()
		
	}// nested class SeasonType
	
	
	/**
	*	PhaseTypes represent game phases. For example, MOVEMENT or RETREAT phases.
	*	<p>
	*	PhaseType constants should be used instead of creating new PhaseType objects.
	*
	*/
	public static class PhaseType implements Serializable, Comparable
	{
		// always-accepted english constants for phase types
		// these MUST be in lower case
		protected static final String CONST_ADJUSTMENT = "adjustment";
		protected static final String CONST_MOVEMENT = "movement";
		protected static final String CONST_RETREAT = "retreat";
		
		// internal constants
		protected static final String IL8N_ADJUSTMENT = "PHASETYPE_ADJUSTMENT";
		protected static final String IL8N_MOVEMENT = "PHASETYPE_MOVEMENT";
		protected static final String IL8N_RETREAT = "PHASETYPE_RETREAT";
		
		// position constants
		protected static final int POS_MOVEMENT 	= 100;
		protected static final int POS_RETREAT 		= 200;
		protected static final int POS_ADJUSTMENT 	= 300;
		
		
		// PhaseType Constants
		/** Adjustment PhaseType */
		public static final PhaseType ADJUSTMENT = new PhaseType(CONST_ADJUSTMENT, POS_ADJUSTMENT, IL8N_ADJUSTMENT);
		/** Movement PhaseType */
		public static final PhaseType MOVEMENT = new PhaseType(CONST_MOVEMENT, POS_MOVEMENT, IL8N_MOVEMENT);
		/** Retreat PhaseType */
		public static final PhaseType RETREAT = new PhaseType(CONST_RETREAT, POS_RETREAT, IL8N_RETREAT);
		/**
		*	PhaseType array
		*	<b>Warning: this should not be mutated.</b>
		*/
		public static final PhaseType[] ALL = { MOVEMENT, RETREAT, ADJUSTMENT };
		
		// instance variables
		protected transient String displayName = null;
		protected final String constName;
		protected final int position;
		
		/** Create a new PhaseType */
		protected PhaseType(String cName, int pos, String il8nKey)
		{
			this.constName = cName;
			this.position = pos;
			this.displayName = Utils.getLocalString(il8nKey);
		}// PhaseType()
		
		/** Get the name of a phase */
		public String toString()
		{
			return displayName;
		}// toString()
		
		/** Brief name of a Phase (e.g., [B]uild, [R]etreat [M]ove */
		public String getBriefName()
		{
			if(this == ADJUSTMENT)
			{
				return "B";
			}
			else if(this == RETREAT)
			{
				return "R";
			}
			else if(this == MOVEMENT)
			{
				return "M";
			}
			
			// unknown!
			return "?";
		}// getBriefName()
		
		
		/** Returns the hashCode */
		public int hashCode()
		{
			return constName.hashCode();
		}// hashCode()
		
		/** Returns <code>true</code> if PhaseType objects are equivalent */
		public boolean equals(Object obj)
		{
			if(obj == this)
			{
				return true;
			}
			else if(obj instanceof PhaseType)
			{
				return ( this.position == ((PhaseType)obj).position );
			}
			return false;
		}// equals()
		
		
		/** Temporally compares PhaseType objects */
		public int compareTo(Object obj)
		{
			PhaseType pt = (PhaseType) obj;
			return (position - pt.position);
		}// compareTo()
		
		
		/**
		*	Get the next PhaseType, in sequence. 
		*/
		public PhaseType getNext()
		{
			if(this == ADJUSTMENT)
			{
				return MOVEMENT;
			}
			else if(this == RETREAT)
			{
				return ADJUSTMENT;
			}
			else if(this == MOVEMENT)
			{
				return RETREAT;
			}
			
			return null;
		}// getNext()
		
		
		/**
		* 	Get the previous PhaseType, in sequence.
		*/
		public PhaseType getPrevious()
		{
			if(this == ADJUSTMENT)
			{
				return RETREAT;
			}
			else if(this == RETREAT)
			{
				return MOVEMENT;
			}
			else if(this == MOVEMENT)
			{
				return ADJUSTMENT;
			}
			
			return null;
		}// getPrevious()
		
		
		/**
		*	Returns the appropriate PhaseType constant representing
		*	the input, or null.
		*	<p>
		*	Plurals are allowable on constants, but not in il8n versions.
		*/
		public static PhaseType parse(final String in)
		{
			// short cases (1 letter); not i18n'd
			if(in.length() == 1)
			{
				String lcIn = in.toLowerCase();
				if("m".equals(lcIn))
				{
					return MOVEMENT;
				}
				else if("a".equals(lcIn) || "b".equals(lcIn))
				{
					return ADJUSTMENT;
				}
				else if("r".equals(lcIn))
				{
					return RETREAT;
				}
				
				return null;
			}
			
			// typical cases; use 'startsWith' 
			if(in.startsWith(CONST_ADJUSTMENT))
			{
				return ADJUSTMENT;
			}
			else if(in.startsWith(CONST_MOVEMENT))
			{
				return MOVEMENT;
			}
			else if(in.startsWith(CONST_RETREAT))
			{
				return RETREAT;
			}
			
			// il8n cases
			if(in.equalsIgnoreCase(Utils.getLocalString(IL8N_ADJUSTMENT)))
			{
				return ADJUSTMENT;
			}
			else if(in.equalsIgnoreCase(Utils.getLocalString(IL8N_MOVEMENT)))
			{
				return MOVEMENT;
			}
			else if(in.equalsIgnoreCase(Utils.getLocalString(IL8N_RETREAT)))
			{
				return RETREAT;
			}
				
			return null;
		}// parse()
		
		
		/** Resolves a serialized Phase object into a constant reference */
		protected Object readResolve()
		throws java.io.ObjectStreamException
		{
			PhaseType pt = null;
			
			if(constName.equalsIgnoreCase(CONST_ADJUSTMENT))
			{
				pt = ADJUSTMENT;
				pt.displayName = Utils.getLocalString(IL8N_ADJUSTMENT);
			}
			else if(constName.equalsIgnoreCase(CONST_MOVEMENT))
			{
				pt = MOVEMENT;
				pt.displayName = Utils.getLocalString(IL8N_MOVEMENT);
			}
			else if(constName.equalsIgnoreCase(CONST_RETREAT))
			{
				pt = RETREAT;
				pt.displayName = Utils.getLocalString(IL8N_RETREAT);
			}
			
			return pt;
		}// readResolve()
		
	}// nested class PhaseType
	
	
	/**
	*	YearType is used to represent the Year
	*	<p>
	*	A YearType is used because we now support negative years ("BC")
	*	and need to appropriately advance, parse, and format these years.
	*	<p>
	*	A YearType is an immutable object.
	*/
	public static class YearType implements Serializable, Comparable
	{
		// instance fields
		protected final int year;
		
		
		/** Create a new YearType */
		public YearType(int value)
		{
			if(value == 0)
			{
				throw new IllegalArgumentException("Year 0 not valid");
			}
			
			year = value;
		}// YearType()
		
		
		/** Get the name of a year. */
		public String toString()
		{
			if(year >= 1000)
			{
				return String.valueOf(year);
			}
			else if(year > 0)
			{
				// explicitly add "AD"
				StringBuffer sb = new StringBuffer(8);
				sb.append(year);
				sb.append(" AD");
				return sb.toString();
			}
			else
			{
				StringBuffer sb = new StringBuffer(8);
				sb.append(-year);
				sb.append(" BC");
				return sb.toString();
			}
		}// toString()
		
		
		/** Gets the year. This will return a negative number if it is a BC year. */
		public int getYear()
		{
			return year;
		}// getYear()
		
		/** Returns the hashcode */
		public int hashCode()
		{
			return year;
		}// hashCode()
		
		/** Returns <code>true</code> if YearTYpe objects are equivalent */
		public boolean equals(Object obj)
		{
			if(obj == this)
			{
				return true;
			}
			else if(obj instanceof YearType)
			{
				return (year == ((YearType) obj).year);
			}
			
			return false;
		}// equals()
		
		/** Temporally compares YearType objects */
		public int compareTo(Object obj)
		{
			return (year - ((YearType) obj).year);
		}// compareTo()
		
		
		/**
		*	Get the next YearType, in sequence
		*/
		public YearType getNext()
		{
			// 1BC -> 1AD, otherwise, just add 1
			return ((year == -1) ? new YearType(1) : new YearType(year + 1));
		}// getNext()
		
		
		/**
		* 	Get the previous YearType, in sequence.
		*/
		public YearType getPrevious()
		{
			// 1 AD -> 1 BC, otherwise, just subtract 1
			return ((year == 1) ? new YearType(-1) : new YearType(year - 1));
		}// getPrevious()
		
		
		/**
		*	Returns the appropriate YearType constant representing
		*	the input, or null.
		*	<p>
		*	0 is not a valid year<br>
		*	Negative years are interpreted as BC<br>
		*	The modifier "BC" following a year is valid<br>
		*	A negative year with the BC modifier is still a BC year<br>
		*	Periods are NOT allowed in "BC"<br>
		*	The modifier BC must be in lower case<br>
		*/
		public static YearType parse(final String input)
		{
			String in = input;
			final int idx = in.indexOf("bc");
			boolean isBC = false;
			if(idx >= 1)
			{
				isBC = true;
				in = in.substring(0, idx);
			}
			
			int y = 0;
			try
			{
				y = Integer.parseInt(in.trim());
			}
			catch(NumberFormatException e)
			{
				return null;
			}
			
			if(y == 0)
			{
				return null;
			}
			else if(y > 0 && isBC)
			{
				y = -y;
			}
			
			return new YearType(y);
		}// parse()
		
	}// nested class YearType
	
}// class Phase
