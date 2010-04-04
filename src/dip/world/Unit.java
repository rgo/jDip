//
//  @(#)Unit.java		4/2002
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

import dip.order.Order;
import dip.misc.Utils;


/**
*
*	A Unit is an object that has an owner (power), a coast location, and a Type
*	describing that unit.
*	<p>
*	Units are placed in Provinces.
*	<p>
*	<b>This object is not immutable!</b>
*/

public class Unit implements java.io.Serializable, Cloneable
{
	// instance variables
	protected final Unit.Type type;
	protected final Power owner;
	protected Coast coast = Coast.UNDEFINED;
	
	
	/**                                                               
	*	Creates a new Unit
	*/
	public Unit(Power power, Unit.Type unitType)
	{
		if(power == null || unitType == null)
		{
			throw new IllegalArgumentException("null arguments not permitted");
		}
		
		if(unitType == Unit.Type.UNDEFINED)
		{
			throw new IllegalArgumentException("cannot create a unit with undefined type");
		}
		
		this.owner = power;
		this.type = unitType;
	}// Unit()
	
	
	/** For Cloning: *NO* arguments are checked. */
	private Unit(Power power, Unit.Type unitType, Coast coast)
	{
		this.owner = power;
		this.type = unitType;
		this.coast = coast;
	}// Unit()
	
	
	/**
	*	Set the coast of a unit.
	*/
	public void setCoast(Coast coast)
	{
		if(coast == null)
		{
			throw new IllegalArgumentException("null coast");
		}
		
		this.coast = coast;
	}// setCoast()
	
	
	/** Get the Coast where this Unit is located */
	public Coast getCoast() 			{ return coast; }
	
	/** Get the Power who controls this Unit */
	public Power getPower() 			{ return owner; }
	
	/** Get the Type of unit (e.g., Army or Fleet) */
	public Unit.Type getType() 			{ return type; }
	
	/** Returns if two Units are equivalent. */
	public boolean equals(final Object obj)
	{
		if(obj == this)
		{
			return true;
		}
		else if(obj instanceof Unit)
		{
			Unit unit = (Unit) obj;
			return (unit.type == this.type	&& unit.owner == this.owner
				&& unit.coast == this.coast);
		}
		
		return false;
	}// equals()
	
	/** 
	*	Returns a Clone of the unit. Note that this is not a 
	*	strict implementation of clone(); a constructor is
	*	invoked for performance reasons.
	*/
	public Object clone()
	{
		return new Unit(owner, type, coast);
	}// clone()
	
	
	/** Displays internal object values. For debugging use only! */
	public String toString()
	{
		StringBuffer sb = new StringBuffer(64);
		sb.append("Unit:[type=");
		sb.append(type);
		sb.append(",power=");
		sb.append(owner);
		sb.append(",coast=");
		sb.append(coast);
		sb.append(']');
		return sb.toString();
	}// toString()
	
	
	/**
	*	A Type is the class of unit, for example, Army or Fleet.
	*	<p>
	*	Type constans should be used; new Type objects should not be created
	*	unless the game concepts are being extended.
	*
	*/
	public static class Type extends Object implements java.io.Serializable
	{
		// internal i18n key constants
		private static final String UNIT_TYPE_PREFIX			= "unit.type.";
		private static final String UNIT_TYPE_BRIEF_SUFFIX 		= ".brief";
		private static final String UNIT_TYPE_ARTICLE_SUFFIX 	= ".witharticle";
		
		// so, for an army (brief name), the key would be:
		// UNIT_TYPE_PREFIX + NAME_ARMY + UNIT_TYPE_BRIEF_SUFFIX
		
		// internal constants, also used in i18n keys
		private static final String NAME_ARMY  		= "army";
		private static final String NAME_FLEET		= "fleet";    
		private static final String NAME_WING		= "wing";
		private static final String NAME_UNDEFINED	= "undefined";
		
		/** Constant representing an Army */
		public static final Unit.Type ARMY = new Unit.Type(NAME_ARMY);
		/** Constant representing a Fleet */
		public static final Unit.Type FLEET = new Unit.Type(NAME_FLEET);
		/** Constant representing a Wing */
		public static final Unit.Type WING = new Unit.Type(NAME_WING);
		/** Constant representing an unknown type */
		public static final Unit.Type UNDEFINED = new Unit.Type(NAME_UNDEFINED);
		
		// instance variables
		private final String internalName;
		private final transient String name;
		private final transient String shortName;
		private final transient String nameWithArticle;
		
		
		/** Create a new Type */
		protected Type(String internalName)
		{
			this.internalName = internalName;
			this.name = Utils.getLocalString(UNIT_TYPE_PREFIX + internalName);
			this.shortName = Utils.getLocalString(UNIT_TYPE_PREFIX + 
				internalName + UNIT_TYPE_BRIEF_SUFFIX);
			this.nameWithArticle = Utils.getLocalString(UNIT_TYPE_PREFIX + 
				internalName + UNIT_TYPE_ARTICLE_SUFFIX);
		}// Type()
		
		/** Get the full name of this type (e.g., 'Army') */
		public String getFullName()
		{
			return name;
		}// getName()
		
		/** Get the short name of this type (e.g., 'A') */
		public String getShortName()
		{
			return shortName;
		}// getShortName();
			
		/** Get the short name */
		public String toString()
		{
			return shortName;
		}// toString()
		
		/** Get the full name, including an article*/
		public String getFullNameWithArticle()
		{
			return nameWithArticle;
		}// getFullNameWithArticle()
		
		/** Returns the hashcode */
		public int hashCode()
		{
			return name.hashCode();
		}// hashCode()
		
		/*
			equals():
			
			We use Object.equals(), which just does a test of 
			referential equality. 
			
		*/
		
		
		/**
		*	Returns a type constant corresponding to the input.
		*	Case insensitive. This will parse localized names,
		*	AS WELL AS the standard English names. So, for 
		*	English names (and all other languages):
		*	<pre>
		*		null -> Type.UNDEFINED
		*		'f' or 'fleet' -> Type.FLEET
		*		'a' or 'army' -> Type.ARMY
		*		'w' or 'wing' -> Type.WING
		*		any other -> null
		*	</pre>
		*
		*/
		public static Unit.Type parse(String text)
		{
			if(text == null)
			{
				return Unit.Type.UNDEFINED;
			}
			
			String input = text.toLowerCase().trim();
			if(Unit.Type.ARMY.getShortName().equals(input) 
				|| Unit.Type.ARMY.getFullName().equals(input))
			{
				return Unit.Type.ARMY;			
			}
			else if(Unit.Type.FLEET.getShortName().equals(input) 
					|| Unit.Type.ARMY.getFullName().equals(input))
			{
				return Unit.Type.FLEET;			
			}
			else if(Unit.Type.WING.getShortName().equals(input) 
					|| Unit.Type.ARMY.getFullName().equals(input))
			{
				return Unit.Type.WING;			
			}
			
			// test against standard English names after trying
			// localized names.
			//
			if("a".equals(input) || "army".equals(input))
			{
				return Unit.Type.ARMY;			
			}
			else if("f".equals(input) || "fleet".equals(input))
			{
				return Unit.Type.FLEET;			
			}
			else if("w".equals(input) || "wing".equals(input))
			{
				return Unit.Type.WING;			
			}
			
			return null;
		}// parse()
		
		/** Assigns serialized objects to a single constant reference */
		protected Object readResolve()
		throws java.io.ObjectStreamException
		{
			Type type = null;
	  		
			if(internalName.equals(NAME_ARMY))
			{
				type = ARMY;
			}
			else if(internalName.equals(NAME_FLEET))
			{
				type = FLEET;
			}
			else if(internalName.equals(NAME_WING))
			{
				type = WING;
			}
			else if(internalName.equals(NAME_UNDEFINED))
			{
				type = UNDEFINED;
			}
			else
			{
				throw new java.io.InvalidObjectException("Unknown Unit.Type: "+internalName);
			}
			
			return type;
		}// readResolve()
	}// inner class Type
	
	
}// class Unit
