//
//  @(#)ToolProxyImpl.java	1.00	9/2002
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

import dip.world.World;
import dip.world.TurnState;
import dip.order.Order;
import dip.gui.ClientFrame;

import javax.swing.JFrame;
import java.beans.PropertyChangeListener;

/**
*
*	Default, standard implementation of the ToolProxy object.
*	This API has not yet been defined, and remains in flux.
*
*/
public class ToolProxyImpl implements ToolProxy
{
	private final ClientFrame clientFrame;
	
	/** Constructor */
	public ToolProxyImpl(ClientFrame clientFrame)
	{
		this.clientFrame = clientFrame;
	}// ToolProxyImpl()
	
	
	// basic methods
	//
	public World getWorld()
	{
		return clientFrame.getWorld();
	}// getWorld()
	
	public TurnState getCurrentTurnState()
	{
		return clientFrame.getTurnState();
	}// getCurrentTurnState()
	
	public ClientFrame getClient()
	{
		return clientFrame;
	}// getJFrame()
	
}// class ToolProxyImpl
