//
//  @(#)GameSetup.java		6/2003
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
package dip.world;

/**
*	A GameSetup is an object set in the World object that 
*	contains the required functionality to restore a saved
*	game. For example, if playing a networked game, it should
*	contain the nescessary data to restore the game.
*	<p>
*	This is a marker interface to maintain better gui/non-gui
*	separation.
*/
public interface GameSetup
{
	
	// no methods : marker interface
	
}// interface GameSetup
