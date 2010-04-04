//
//  @(#)StatusBarUtils.java	1.00	4/1/2002
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
package dip.gui.map;

import dip.gui.StatusBar;

import dip.world.Province;
import dip.world.Location;
import dip.world.Position;
import dip.world.Unit;
import dip.world.Power;
import dip.misc.Utils;

import java.awt.GridLayout;
import java.awt.BorderLayout;
import java.awt.Dimension;

import javax.swing.JTextField;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.border.EmptyBorder;

/**
*	Convenience methods for displaying information on the StatusBar.
*	
*
*/
public class StatusBarUtils
{
	private final MapPanel mp;
	private final StatusBar statusbar;
	
	//	i18n constants
	private static final String SB_NO_UNIT_PRESENT = "StatusBar.nounitpresent";
	private static final String SB_DISLOGED        = "StatusBar.disloged";
	private static final String SB_UNOWNED         = "StatusBar.unowned";
	private static final String SB_SUPPLY_CENTER   = "StatusBar.supplycenter";
		 
	/** Create the StatusBarUtils object */
	protected StatusBarUtils(MapPanel mp, StatusBar statusbar)
	{
		this.mp = mp;
		this.statusbar = statusbar;
	}// StatusBarUtils()
	
	/** Set the StatusBar text */
	public void setText(String text)
	{
		statusbar.setText(text);
	}// setText()
	
	
	/** Clear the StatusBar text */
	public void clearText()
	{
		statusbar.clearText();
	}// clearText()
	
	
	/** Convenience version of displayProvinceInfo(Location, null) */
	public void displayProvinceInfo(Location loc)
	{
		 displayProvinceInfo(loc, null);
	}// displayProvinceInfo()
	
	/**
	*	Suitable for mouseOver events
	*	<p>
	*	Displays Province information. 
	*	<p>
	*	Clears status bar if location & appendText is null
	*
	*/
	public void displayProvinceInfo(Location loc, String appendText)
	{
		if(loc == null)
		{
			if(appendText == null)
			{
				clearText();
			}
			else
			{
				setText(appendText);
			}
			
			return;
		}
		
		StringBuffer sb = new StringBuffer(128);
		Province p = loc.getProvince();
		Position position = mp.getPosition();
		
		if(position == null)
		{
			// 1) No unit present
			sb.append(p.getFullName());
			sb.append(" (");
			sb.append(p.getShortName());
			sb.append(") "+Utils.getLocalString(SB_NO_UNIT_PRESENT));
		}
		else
		{
			// Province Name + Abbreviation
			//
			// province name
			sb.append(p.getFullName());
			sb.append(" (");
			sb.append(p.getShortName());
			sb.append(')');
			
			// province information
			//
			// Unit Information
			Unit unit = position.getUnit(p);
			if(unit != null)
			{
				sb.append(": ");
				sb.append(unit.getPower().getAdjective());
				sb.append(' ');
				sb.append(unit.getType().getFullName());
				getCoast(sb, unit, p);
			}
			
			// Dislodged Unit information
			Unit disUnit = position.getDislodgedUnit(p);
			if(disUnit != null)
			{
				if(unit != null)
				{
					sb.append(", ");
				}
				else
				{
					sb.append(": ");
				}
				
				sb.append(Utils.getLocalString(SB_DISLOGED)+" ");
				sb.append(disUnit.getPower().getAdjective());
				sb.append(' ');
				sb.append(disUnit.getType().getFullName());
				getCoast(sb, disUnit, p);
			}
			
			// Supply Center information
			if(p.hasSupplyCenter())
			{
				if(disUnit != null || unit != null)
				{
					sb.append(", ");
				}
				else
				{
					sb.append(": ");
				}
				
				Power owner = position.getSupplyCenterOwner(p);
				if(owner == null)
				{
					sb.append(Utils.getLocalString(SB_UNOWNED));
				}
				else
				{
					sb.append(owner.getAdjective());
				}
				
				sb.append(" "+Utils.getLocalString(SB_SUPPLY_CENTER)+".");
			}
			else
			{
				if(unit != null || disUnit != null)
				{
					sb.append('.');
				}
			}
			
			if(unit == null && disUnit == null && !p.hasSupplyCenter())
			{
				if(appendText == null)
				{
					sb.append('.');
				}
				else
				{
					sb.append(':');
				}
			}
			
		}
		
		// Appended Order Text
		if(appendText != null)
		{
			sb.append(' ');
			sb.append(appendText);
		}
		
		statusbar.setText(sb.toString());
	}// displayProvinceInfo
	
	
	/** Get coast information, if appropriate */
	private void getCoast(StringBuffer sb, Unit unit, Province p)
	{
		if(unit.getType().equals(Unit.Type.FLEET) && p.isMultiCoastal())
		{
			sb.append(" (");
			sb.append(unit.getCoast().getName());
			sb.append(")");
		}
	}// getCoast()
	
}// class StatusBarUtils
