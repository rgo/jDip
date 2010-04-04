//
//  @(#)RuleOptions.java		10/2002
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
import dip.world.variant.data.Variant;

import java.io.Serializable;
import java.io.ObjectStreamException;
import java.io.InvalidObjectException;
import java.util.HashMap;
import java.util.Set;
import java.util.Iterator;
import java.lang.reflect.*;

/**
*	RuleOptions is an object for storing Options and OptionValues that 
*	describe rule variants.
*	<p>
*	Internationalization notes:
*	<p>
<pre>
	OptionValue:
		getNameI18N()				gets internationalized name (key is getName())
		getDescriptionI18N()		gets internationalized description (name + "_description")
	
	Option:
		getNameI18N()				gets internationalized name
		getDescriptionI18N()		gets internationalized description (name + "_description")
		

</pre>
*	
*	
*
*
*
*/
public class RuleOptions implements Serializable
{
	// internal constnats
	private static final String DESCRIPTION = "_description";
	
	// il8n constants
	private static final String RO_BAD_OPTIONVALUE 	= "RuleOpts.parser.badoptionvalue";
	private static final String RO_BAD_NVP 			= "RuleOpts.parser.badnvp";
	
	// DO NOT change the names of these!!
	// pre-defined option values that are shared between multiple options
	/** TRUE (Boolean) OptionValue */
	public static final OptionValue VALUE_TRUE = 	new OptionValue("OptionValue.true");
	/** FALSE (Boolean) OptionValue */
	public static final OptionValue VALUE_FALSE = 	new OptionValue("OptionValue.false");
	
	// DO NOT change the names of these!!
	// defined options, and their values
	// BUILD options
	public static final OptionValue VALUE_BUILDS_HOME_ONLY = new OptionValue("OptionValue.home-only");
	public static final OptionValue VALUE_BUILDS_ANY_OWNED = new OptionValue("OptionValue.any-owned");
	public static final OptionValue VALUE_BUILDS_ANY_IF_HOME_OWNED = new OptionValue("OptionValue.any-if-home-owned");
	public static final Option OPTION_BUILDS = new Option("Option.builds", 
													VALUE_BUILDS_HOME_ONLY,
													new OptionValue[] {VALUE_BUILDS_HOME_ONLY, 
														VALUE_BUILDS_ANY_OWNED,
														VALUE_BUILDS_ANY_IF_HOME_OWNED });
	
	
	
	public static final OptionValue VALUE_WINGS_ENABLED = new OptionValue("OptionValue.wings-enabled");
	public static final OptionValue VALUE_WINGS_DISABLED = new OptionValue("OptionValue.wings-disabled");
	public static final Option OPTION_WINGS = new Option("Option.wings", 
													VALUE_WINGS_DISABLED,
													new OptionValue[] {
														VALUE_WINGS_ENABLED, 
														VALUE_WINGS_DISABLED });
	
	
	public static final OptionValue VALUE_PATHS_EXPLICIT = new OptionValue("OptionValue.explicit-paths");
	public static final OptionValue VALUE_PATHS_IMPLICIT = new OptionValue("OptionValue.implicit-paths");
	public static final OptionValue VALUE_PATHS_EITHER = new OptionValue("OptionValue.either-path");
	public static final Option OPTION_CONVOYED_MOVES = new Option("Option.move.convoyed", 
													VALUE_PATHS_EITHER,
													new OptionValue[] {
														VALUE_PATHS_EITHER, 
														VALUE_PATHS_EXPLICIT, 
														VALUE_PATHS_IMPLICIT });
	
	
	
	// array of default options, that are always set for every variant.
	private static final Option[] DEFAULT_RULE_OPTIONS = { OPTION_BUILDS, OPTION_WINGS, OPTION_CONVOYED_MOVES };
	
	// NOTE: we must include all options / optionvalues in these arrays
	// OptionList -- for serialization/deserialization
	private static final Option[] ALL_OPTIONS = {OPTION_BUILDS, OPTION_WINGS, OPTION_CONVOYED_MOVES}; 
	
	// OptionValue -- for serialization/deserialization
	private static final OptionValue[] ALL_OPTIONVALUES = {
		VALUE_BUILDS_HOME_ONLY, VALUE_BUILDS_ANY_OWNED, VALUE_BUILDS_ANY_IF_HOME_OWNED,
		VALUE_WINGS_ENABLED, VALUE_WINGS_DISABLED,
		VALUE_PATHS_EXPLICIT, VALUE_PATHS_IMPLICIT, VALUE_PATHS_EITHER
	}; 
	
	
	
	// instance variables
	protected HashMap optionMap = null;
	
	
	/**
	*	An Option is created for each type of Rule that may have more than 
	*	one allowable option. The name of each Option must be unique. 
	*
	*
	*/
	public static class Option implements Serializable
	{
		// instance variables
		protected final String name;
		protected final OptionValue[] allowed;
		protected final OptionValue defaultValue;
		
		/** Create an Option. */
		public Option(String name, OptionValue defaultValue, OptionValue[] allowed)
		{
			if(defaultValue == null || name == null || allowed == null)
			{
				throw new IllegalArgumentException();
			}
			
			this.name = name;
			this.defaultValue = defaultValue;
			this.allowed = allowed;
		}// Option()
		
		/** Returns the Option name. */
		public String getName()					{ return name; }
		/** Returns the default Option value. */
		public OptionValue getDefault()			{ return defaultValue; }
		/** Returns the allowed Option values. */
		public OptionValue[] getAllowed()		{ return allowed; }
		
		/** Checks if the given optionValue is allowed. */
		public boolean isAllowed(OptionValue optionValue)
		{
			for(int i=0; i<allowed.length; i++)
			{
				if(optionValue == allowed[i])
				{
					return true;
				}
			}
			
			return false;
		}// isAllowed()
		
		/** Gets the internationalized ("display") version of the name. */
		public String getNameI18N()				{ return Utils.getLocalString(getName()); }
		
		/** Gets the internationalized ("display") version of the description. */
		public String getDescriptionI18N()		{ return Utils.getLocalString(getName()+DESCRIPTION); }
		
		/** Checks if the given OptionValue is permitted; if so, returns true. */
		public boolean checkValue(OptionValue value)
		{ 	
			for(int i=0; i<allowed.length; i++)
			{
				if(allowed[i].equals(value))
				{
					return true;
				}
			}
			
			return false;
		}// checkValue() 
		
		
		public boolean equals(Object obj)
		{
			return name.equals( ((Option) obj).name );
		}// equals()
		
		
		public int hashCode()
		{
			return name.hashCode();
		}// hashCode()
		
		
		protected Object readResolve()
		throws java.io.ObjectStreamException
		{
			// slow but easy
			for(int i=0; i<ALL_OPTIONS.length; i++)
			{
				if( name.equals(ALL_OPTIONS[i].name) )
				{
					return ALL_OPTIONS[i];
				}
			}
			
			throw new InvalidObjectException("RuleOptions: ALL_OPTIONS internal error");
		}// readResolve()
		
		/** For debugging only */
		public String toString()
		{
			return name;
		}
	}// nested class Option
	
	
	/**
	*	OptionValues are the pre-defined values that an Option may have.
	*	<p>
	*	OptionValue names need not be unique, and may be shared between 
	*	options.
	*/
	public static class OptionValue implements Serializable
	{
		// instance variables
		final String name;
		
		/** Create an OptionValue. */
		public OptionValue(String name)
		{
			if(name == null)
			{
				throw new IllegalArgumentException();
			}
			
			this.name = name;
		}// OptionValue()
		
		/** Returns the OptionValue name. */
		public String getName()
		{
			return name;
		}// getName()
		
		/** Gets the internationalized ("display") version of the name. */
		public String getNameI18N()				{ return Utils.getLocalString(getName()); }
		
		/** Gets the internationalized ("display") version of the description. */
		public String getDescriptionI18N()		{ return Utils.getLocalString(getName()+DESCRIPTION); }
		
		public boolean equals(Object obj)
		{
			return name.equals( ((OptionValue) obj).name );
		}// equals()
		
		public int hashCode()
		{
			return name.hashCode();
		}// hashCode()
		
		
		protected Object readResolve()
		throws java.io.ObjectStreamException
		{
			// slow but easy
			for(int i=0; i<ALL_OPTIONVALUES.length; i++)
			{
				if( name.equals(ALL_OPTIONVALUES[i].name) )
				{
					return ALL_OPTIONVALUES[i];
				}
			}
			
			throw new InvalidObjectException("RuleOptions: ALL_OPTIONVALUES internal error");
		}// readResolve()
		
		/** For debugging only */
		public String toString()
		{
			return name;
		}
	}// nested class OptionValue()
	
	
	
	
	
	/** Creates a new RuleOptions object, which stores various Rule options. */
	public RuleOptions()
	{
		optionMap = new HashMap(31);
	}// RuleOptions()
	
	
	/**
	*	Sets the OptionValue for an Option.
	*	<p>
	*	Null Options or OptionValues are not permitted. If an invalid OptionValue
	*	is given, an IllegalArgumentException is thrown.
	*
	*/
	public void setOption(Option option, OptionValue value)
	{
		if(option == null || value == null)
		{
			throw new IllegalArgumentException("null Option or OptionValue");
		}
		
		if(!option.checkValue(value))
		{
			throw new IllegalArgumentException("invalid OptionValue for Option");
		}
		
		optionMap.put(option, value);
	}// setOption()
	
	
	/** 
	*	Obtains the value for an Option. If the Option is not found, or its OptionValue
	*	not set, the default OptionValue is returned.
	*	<p>
	*	A null Option is not permitted.
	*/
	public OptionValue getOptionValue(Option option)
	{
		if(option == null)
		{
			throw new IllegalArgumentException("null Option");
		}
		
		OptionValue value = (OptionValue) optionMap.get(option);
		if(value == null)
		{
			return option.getDefault();
		}
		
		return value;
	}// getOption()
	
	
	/** Returns a Set of all Options. */
	public Set getAllOptions()
	{
		return optionMap.keySet();
	}// getAllOptions()
	
	
	/** For debugging only; print the rule options */
	public String toString()
	{
		StringBuffer sb = new StringBuffer(256);
		sb.append(this.getClass().getName());
		sb.append('\n');
		
		Set set = getAllOptions();
		Iterator iter = set.iterator();
		while(iter.hasNext())
		{
			Option opt = (Option) iter.next();
			OptionValue ov = getOptionValue(opt);
			sb.append("  ");
			sb.append(opt);
			sb.append(" : ");
			sb.append(ov);
			sb.append('\n');
		}
		
		return sb.toString();
	}// toString()
	
	
	/**
	*
	*	Create a RuleOptions from a Variant.
	*	<p>
	*	An InvalidWorldException is thrown if the passed data
	*	is invalid.
	*
	*/
	public static RuleOptions createFromVariant(Variant variant)
	throws InvalidWorldException
	{
		// create ruleoptions
		// set rule options
		RuleOptions ruleOpts = new RuleOptions();
		
		// set default rule options
		for(int i=0; i<DEFAULT_RULE_OPTIONS.length; i++)
		{
			ruleOpts.setOption(DEFAULT_RULE_OPTIONS[i], DEFAULT_RULE_OPTIONS[i].getDefault());
		}
		
		// this class
		Class clazz = ruleOpts.getClass();
		
		// look up all name-value pairs via reflection.
		Variant.NameValuePair[] nvps = variant.getRuleOptionNVPs();
		for(int i=0; i<nvps.length; i++)
		{
			Option option = null;
			OptionValue optionValue = null;
			
			// first, check the name
			try
			{
				Field field = clazz.getField(nvps[i].getName());
				option = (Option) field.get(null);
				
				field = clazz.getField(nvps[i].getValue());
				optionValue = (OptionValue) field.get(null);
			}
			catch(Exception e)
			{
				throw new InvalidWorldException(
					Utils.getLocalString(RO_BAD_NVP, nvps[i].getName(), nvps[i].getValue(), e.getMessage()) );
			}
			
			
			// ensure that optionValue is valid for option
			if( !option.isAllowed(optionValue) )
			{
				throw new InvalidWorldException(
					Utils.getLocalString(RO_BAD_OPTIONVALUE, nvps[i].getValue(), nvps[i].getName()) );
			}
			
			// set option
			ruleOpts.setOption(option, optionValue);
		}
		
		// done.
		return ruleOpts;
	}// createFromVariant()
	
	
	
}// class RuleOptions
