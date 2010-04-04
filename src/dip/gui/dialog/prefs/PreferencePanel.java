//
//  @(#)PreferencePanel.java	1.00	4/1/2002
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
package dip.gui.dialog.prefs;


/**
*	This defines a Panel (tabbed Panel) in the PreferenceDialog.
*	<p>
*	Preferences can be applied or cancelled.
*	<p>
*/
public abstract class PreferencePanel extends javax.swing.JPanel
{
	/**
	*	Apply changes made by user.
	*
	*/
	public abstract void apply();
	
	
	/**
	*	Cancel; do not apply changes made.
	*
	*/
	public abstract void cancel();
	
	
	/**
	*	Return settings to their default. 
	*	Does not apply the changes.
	*
	*/
	public abstract void setDefault();
	
	
	
	/**
	*	Returns the name of the panel.
	*	<p>
	*	This is displayed in the tab.
	*/
	public abstract String getName();
	
	
}// interface PreferencePanel
