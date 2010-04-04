//
//  @(#)MID.java	9/2003
//
//  Copyright 2003 Zachary DelProposto. All rights reserved.
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
package dip.net.message;

import dip.world.Power;

/**
*	An Message Identifier, which identifies a sender or recipient of a message.
*	An optional nickname (defaults to name if not specified) may also be set.
*	<p>
*	A player is identified by having the Power attribute set. A non-player does
*	not have the Power set.
*	<p>
*	No security is attempted here. It is up to the underlying Channel to ensure 
*	(by extending this class, if required) that MIDs are valid. Specifically, 
*	ensuring that Player MIDs cannot be faked by non-players or other players.
*
*/
public class MID
{
	/** Anonymous sender (TODO: needs i18n) */
	public static final MID ANONYMOUS = new MID("", "Anonymous");
	
	// instance variables
	private final Power power;		// may be null
	private final String name;		// may be null
	private final String nick;		// never may be null
	
	
	
	/** Create an MID */
	public MID(String nick, String name)
	{
		if(nick == null)
		{
			throw new IllegalArgumentException();
		}
		
		this.power = null;
		this.nick = nick;
		this.name = name;
	}// MID()
	
	
	/** Create an MID */
	public MID(Power power, String name)
	{
		this.power = power;
		this.nick = power.getName();
		this.name = name;
	}// MID()
	
	
	/** Get the Power (or null, if unknown or not applicable) associated with the ID */
	public final Power getPower() 
	{
		return power;
	}// getPower()
	
	
	/** Get the name (which may be null) associated with the ID */
	public String getName()
	{
		return name;
	}// getName()
	
	
	/** Get the nickname. (never null). */
	public String getNick()
	{
		return nick;
	}// getNick()
	
	
	/** Determine if this MID belongs to a player or not. */
	public boolean isPlayer()
	{
		return (power != null);
	}// isPlayer()
	
	/** Prints nick, and if set, name.	*/
	public String getNickAndName()
	{
		StringBuffer sb = new StringBuffer(64);
		sb.append(getNick());
		if(getName() != null)
		{
			sb.append(" (");
			sb.append(getName());
			sb.append(")");
		}
		
		return sb.toString();
	}// getNickAndName()
	
	/** Equivalent to getNickAndName() */
	public String toString()
	{
		return getNickAndName();
	}// toString()
	
	
	/** Compare two MIDs for equality (of all fields) */
	public boolean equals(Object obj)
	{
		if(obj == this)
		{
			return true;
		}
		else if(obj instanceof MID)
		{
			MID mid = (MID) obj;
			if( mid.power == this.power && 
				mid.nick.equals(this.nick) )
			{
				// names may be null.
				if(mid.name == this.name)
				{
					return true;
				}
				else if(mid.name != null)
				{
					return mid.name.equals(this.name);
				}
			}
		}
		
		return false;
	}// equals()
	
	/** Hashcode implementation */
	public int hashCode()
	{
		if(name != null)
		{
			return nick.hashCode() + 37*name.hashCode();
		}
		
		return nick.hashCode();
	}// hashCode()
	
}// class MID

