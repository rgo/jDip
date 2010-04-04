//
//  @(#)DefaultPressStore.java	9/2003
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

import dip.world.Phase;

import java.util.List;
import java.util.ListIterator;
import java.util.ArrayList;

/**
*	A PressStore object is returned by the World object, and
*	facilitates the storage and retrieval of Press messages.
*	<p>
*	Currently, messages may be saved or all messages may be retrieved.
*	Support for the retrieval of select messages is not yet specified.
*	
*/
public class DefaultPressStore implements PressStore
{
	// serialized fields
	protected List messages;
	
	
	// transient fields
	
	
	/** Create a DefaultPressStore */
	public DefaultPressStore()
	{
		messages = new ArrayList(100);
	}// DefaultPressStore()
	
	
	/** Get all Messages. */
	public synchronized PressMessage[] getAllMessages()
	{
		return (PressMessage[]) messages.toArray(new PressMessage[messages.size()]);
	}// getAllMessages()
	
	
	/** 
	*	Store a Message. 
	*	<p>
	*	This will replace a message already stored if the 
	*	message fields from/to/subject/body/phase
	*	match. The read/reply to flags are ignored (and thus
	*	will be updated), as are sent/receive timestamps
	*	
	*	<p>
	*	Note that PressMessages are not
	*	serialized until the World object is serialized.
	*/
	public synchronized void storeMessage(PressMessage pm)
	{
		if(pm == null)
		{
			throw new IllegalArgumentException();
		}
		
		ListIterator iter = messages.listIterator();
		while(iter.hasNext())
		{
			PressMessage msg = (PressMessage) iter.next();
			if( compare(pm, msg) )
			{
				// found! replace and return.
				iter.set(pm);
				return;
			}
		}
		
		// not found. add.
		iter.add(pm);
	}// storeMessage()
	
	
	/** Compare two messages. Null PressMessages not supported. */
	private boolean compare(PressMessage pm1, PressMessage pm2)
	{
		if(!pm1.getFrom().equals(pm2.getFrom()))
		{
			return false;
		}
		
		if(!pm1.getMessage().equals(pm2.getMessage()))
		{
			return false;
		}
		
		final MID[] to1 = pm1.getTo();
		final MID[] to2 = pm2.getTo();
		if(to1.length == to2.length)
		{
			for(int i=0; i<to1.length; i++)
			{
				if( !to1[i].equals(to2[i]) )
				{
					return false;
				}
			}
		}
		else
		{
			return false;
		}
		
		// subjects, phase may be null
		final String s1 = pm1.getSubject();
		final String s2 = pm2.getSubject();
		if(s1 != s2)	// only true if both non-null (or non-identity)
		{
			if( (s1 != null) && !s1.equals(s2) )
			{
				return false;
			}
			
			if( (s2 != null) && !s2.equals(s1) )
			{
				return false;
			}
		}
		
		
		final Phase p1 = pm1.getPhase();
		final Phase p2 = pm2.getPhase();
		if(p1 != p2)
		{
			if( (p1 != null) && !p1.equals(p2))
			{
				return false;
			}
			
			if( (p2 != null) && !p2.equals(p1))
			{
				return false;
			}
		}
		
		// we checked all we want to.
		// 
		return true;
	}// compare()
	

	
	
}// class DefaultPressStore

