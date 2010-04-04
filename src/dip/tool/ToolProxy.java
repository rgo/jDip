//
//  @(#)ToolProxy.java	1.00	9/2002
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
package dip.tool;

import dip.gui.ClientFrame;
import dip.world.World;
import dip.world.TurnState;
import dip.order.Order;

import javax.swing.JFrame;
import java.beans.PropertyChangeListener;

/**
*
*	Proxy object which facilitates communication between a Tool and jDip 
*	internal data structures.
*	<p>
*	This API has not yet been defined, and remains in flux.
*
*/
public interface ToolProxy
{
	// basic methods
	//
	/** Gets the current World object, if any. May return null. */
	public World getWorld();
	
	/** Convenience method: gets the current TurnState from the current World, or null */
	public TurnState getCurrentTurnState();
	
	/** Gets the ClientFrame (JFrame) instance */
	public ClientFrame getClient();
	
	
}// interface ToolProxy
