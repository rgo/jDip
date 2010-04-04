//
//  @(#)PressMessage.java	9/2003
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

/**
*	A Press Message
*	<p>
*	
*
*
*
*/
public interface PressMessage
{
		
	/** Message sender. May never be null. */
	public MID getFrom();
	
	/** Message recipients. Never null. May be zero-length if broadcast. */
	public MID[] getTo();
	
	/** Message subject. May be null if there is no subject. */
	public String getSubject();
	
	/** Message body. Never null; may be empty (""). */
	public String getMessage();
	
	/** 
	*	Phase during which message was sent. 
	*	May be null if sent before a game has started, or after
	*	a game has ended.
	*/
	public Phase getPhase();
	
	/** Time when message arrived. 0 if unknown. */
	public long getTimeReceived();
	
	/** Time when message was sent. 0 if unknown. */
	public long getTimeSent();
	
	/** True if this message has been read */
	public boolean isRead();
	
	/** True if this message has been replied to */
	public boolean isRepliedTo();
	
	/** Set whether this message has been read */
	public void setRead(boolean value);
	
	/** Set whether this message has been replied to */
	public void setRepliedTo(boolean value);
	
}// interface PressMessage

