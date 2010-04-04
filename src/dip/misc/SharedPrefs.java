//
//  @(#)SharedPrefs.java	1.00	4/1/2002
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
package dip.misc;

import dip.gui.ClientFrame;

import java.util.prefs.Preferences;
import java.util.prefs.BackingStoreException;
import java.awt.Color;


/**
*
*	This is just a simple class to get a consistent preference node 
*	between packages; thus they will all SHARE the same preference 
*	node. This makes it easy to change, especially for properties
*	that can be altered in the PReferences dialog; no class must
*	be specified.
*	<p>
*	It defaults to a USER node.
*	<p>
*	Packages can use their own preferences node, via java.util.preferences,
*	if they desire.
*
*/
public class SharedPrefs
{
	// the class we want to be the root node.
	private static Class sharedRootNodeClass = null;
	
	static
	{
		try
		{
			sharedRootNodeClass = Class.forName("dip.gui.ClientFrame");
		}
		catch(ClassNotFoundException e)
		{
			System.err.println("ERROR: could not find class: dip.gui.ClientFrame");
		}
	}
	
	/** Per-user preferences.  */
	public static Preferences getUserNode()
	{
		return Preferences.userNodeForPackage(sharedRootNodeClass);
	}// getUserNode()
	
	/** Preferences that apply for all users ("System"). This should not generally be used. */
	public static Preferences getSystemNode()
	{
		return Preferences.systemNodeForPackage(sharedRootNodeClass);
	}// getSystemNode()
	
	
	
	// helper methods
	public static void putColor(Preferences p, String key, Color c)
	{
		if(c == null)
		{
			throw new IllegalArgumentException("null color");
		}
		
		p.put(key, Utils.colorToHex(c, true));
	}// putColor()
	
	public static Color getColor(Preferences p, String key, Color defaultColor)
	{
		String str = p.get(key, null);
		if(str != null)
		{
			return Utils.parseColor(str, defaultColor);
		}
		
		return defaultColor;
	}// getColor()
	
	
	/** Save user preferences. Save errors ignored (unless logging on) */
	public static void savePrefs(Preferences prefs)
	{
		try
		{ 
			prefs.flush();
		}
		catch(BackingStoreException bse)
		{
			Log.println(bse);
		}
	}// savePrefs()
	
	
	private SharedPrefs()
	{
	}// SharedPrefs()
	
}// class SharedPrefs
