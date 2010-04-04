//
//  @(#)PressStore.java	9/2003
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

/**
*	A PressStore object is returned by the World object, and
*	facilitates the storage and retrieval of Press messages.
*	<p>
*	Currently, messages may be saved or all messages may be retrieved.
*	Support for the retrieval of select messages is not yet specified.
*	
*/
public interface PressStore
{
		
	/** Get all Messages. */
	public PressMessage[] getAllMessages();
	
	
	/** 
	*	Store a Message. Note that PressMessages are not
	*	serialized until the World object is serialized.
	*/
	public void storeMessage(PressMessage pm);
	
	
	
}// interface PressStore

