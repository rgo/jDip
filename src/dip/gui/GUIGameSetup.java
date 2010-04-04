//
//  @(#)GUIGameSetup.java		6/2003
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
package dip.gui;

import dip.world.GameSetup;
import dip.world.TurnState;
import dip.world.World;
import dip.world.Power;

/**
*	The base class for all GUI GameSetup operations.
*	
*/
public interface GUIGameSetup extends GameSetup
{
	
	/** 
	*	Setup the game. This method is always called
	*	before any other method.
	*	
	*/
	public void setup(ClientFrame cf, World world);
	
	
	/**
	*	Called by the persistance manager just prior to 
	*	a save. This allows serialization of any needed
	*	data to permit proper restoration.
	*/
	public void save(ClientFrame cf);
	
	
}// interface GUIGameSetup
