//
//  @(#)PressChannel.java	9/2003
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
*	Defines a Channel for sending Press messages.
*	<p>
*	Before opening a channel, a PressListener should be added, 
*	because PressListeners receive error events (e.g., if an 
*	error occured while opening a channel).
*
*/
public interface PressChannel
{
	
	/**
	* 	Open a PressChannel, configured with the given
	*	PressConfiguration object.
	*/
	public void open(PressConfiguration pressConfig);
	
	/**	Close a PressChannel. */
	public void close();
	
	/** Get the current configuration */
	public PressConfiguration getPressConfiguration();
	
	/**	Explicitly check for new Press */
	public void checkNew();
	
	/**	Send Press */
	public void sendPress(PressMessage msg);
	
	/**	Add a Press Listener */
	public void addPressListener(PressListener pressListener);
	
	/**	Remove a Press Listener */
	public void removePressListener(PressListener pressListener);
	
	/**
	*	Returns <code>true</code> if sent press messages are
	*	acknowledged.
	*/
	public boolean areSendsAcknowledged();
	
	/** Compose a broadcast message */
	public PressMessage composeBroadcast(MID from, String subject, String body);
	
	/** Compose a message (to a single recipient) */
	public PressMessage compose(MID from, MID to, String subject, String body);
	
	/** Compose a message (to multiple recipients) */
	public PressMessage compose(MID from, MID[] to, String subject, String body);
	
}// interface PressChannel


