//
//  @(#)UndoEditAddUnit.java	1.00	8/2002
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
package dip.gui.undo;

import dip.world.Province;
import dip.world.Position;
import dip.world.Unit;
import dip.misc.Utils;

import javax.swing.undo.*;


/**
*
*	UndoEditAddUnit is created any time a unit is added in Edit mode.
*	
*/	
public class UndoEditAddUnit extends XAbstractUndoableEdit
{
	private static final String PRESENTATION_NAME = "Undo.edit.addunit";
	
	// instance variables
	private Position position;
	private Unit unit;
	private boolean isDislodged;
	private Province province;
	
	public UndoEditAddUnit(UndoRedoManager urm, Position position, Province province, Unit unit, boolean isDislodged)
	{
		super(urm);
		this.position = position;
		this.unit = unit;
		this.province = province;
		this.isDislodged = isDislodged;
	}// UndoEditAddUnit()
	
	public String getPresentationName()
	{
		return Utils.getLocalString(PRESENTATION_NAME, unit.getType().getFullName(), province.getShortName());
	}// getPresentationName()
	
	public void redo()
	throws CannotRedoException
	{
		super.redo();
		UndoEditAddUnit.addUnit(undoRedoManager, position, province, unit, isDislodged);
	}// redo()
	
	public void undo()
	throws CannotUndoException
	{
		super.undo();
		UndoEditRemoveUnit.removeUnit(undoRedoManager, position, province, isDislodged);
	}// undo()
 	
	
	/** helper method: add unit to position */
	static void addUnit(UndoRedoManager urm, Position position, Province province, Unit unit, boolean isDislodged)
	{
		if(isDislodged)
		{
			position.setDislodgedUnit(province, unit);
		}
		else
		{
			position.setUnit(province, unit);
		}
		
		urm.getClientFrame().fireStateModified();
		urm.getClientFrame().getMapPanel().updateProvince(province);
		urm.getOrderDisplayPanel().revalidateAllOrders();
	}// addUnit()
	
	
	
}// class UndoEditAddUnit

