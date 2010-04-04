//
//  @(#)PlayerMetadata.java	1.00	6/2002
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
package dip.world.metadata;

import java.io.Serializable;
import java.net.URI;
/**
*	Contains MetaData about a player. 
*	One PlayerMetadata object exists for each player.
*	
*	
*	
*	
*	
*/
public class PlayerMetadata implements Serializable
{
	// constants
	private static final String EMPTY = "";
	
	// MetaData
	private String 		name = EMPTY;
	private String[] 	email = new String[0];
	private URI 		uri = null;
	private String		notes = EMPTY;
	
	
	/** Create a PlayerMetadata object */
	public PlayerMetadata()
	{
	}// PlayerMetadata()
	
	
	/** Gets player name. Never null. May be empty. */
	public String getName()						{ return name; }
	
	/** Gets player email addresses. Never null. */
	public String[] getEmailAddresses()			{ return email; }
		
	/** Gets player contact URI. May be null. */
	public URI getURI()							{ return uri; }
	
	/** Gets textual notes. Never null, but may be empty. */
	public String getNotes()					{ return notes; }
	
	
	
	/** Sets the player name. A null value will create an empty string. */
	public void setName(String value)				{ name = (value == null) ? EMPTY : value; }
	
	/** Sets the player's email addresses. a Null value will create a zero-length array.*/
	public void setEmailAddresses(String[] value)	{ email = (value == null) ? new String[0] : value; }
	
	/** Sets the player contact URI. Null values are permissable. */
	public void setURI(URI value)					{ uri = value; }
	
	/** Sets notes. Null values will create an empty string. */
	public void setNotes(String value)				{ notes = (value == null) ? EMPTY : value; }
	
}// class PlayerMetadata
