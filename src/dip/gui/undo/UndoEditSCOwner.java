//
//  @(#)UndoEditSCOwner.java	1.00	8/2002
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

import dip.world.Power;
import dip.world.Province;
import dip.world.Position;
import dip.misc.Utils;

import javax.swing.undo.*;


/**
*
*	UndoEditSCOwner is created any time a supply center changes ownership.
*	
*/	
public class UndoEditSCOwner extends XAbstractUndoableEdit
{
	private static final String PRESENTATION_NAME = "Undo.edit.changescowner";

	// instance variables
	private Position position;
	private Province province;
	private Power oldPower;
	private Power newPower;
	
	
	public UndoEditSCOwner(UndoRedoManager urm, Position position, Province province, Power oldPower, Power newPower)
	{
		super(urm);
		this.position = position;
		this.province = province;
		this.oldPower = oldPower;
		this.newPower = newPower;
	}// UndoEditSCOwner
	
	public String getPresentationName()
	{
		return Utils.getLocalString(PRESENTATION_NAME);
	}// getPresentationName()
	
	
	public void redo()
	throws CannotRedoException
	{
		super.redo();
		UndoEditSCOwner.changeSCOwner(undoRedoManager, position, province, newPower);
	}// redo()
	
	public void undo()
	throws CannotUndoException
	{
		super.undo();
		UndoEditSCOwner.changeSCOwner(undoRedoManager, position, province, oldPower);
	}// undo()
	
	
	/** helper method: change SC owner */
	private static void changeSCOwner(UndoRedoManager urm, Position pos, Province prov, Power newPow)
	{
		pos.setSupplyCenterOwner(prov, newPow);
		
		// re-render province & set changed flag
		urm.getClientFrame().fireStateModified();
		urm.getClientFrame().getMapPanel().updateProvince(prov);
		
		urm.getOrderDisplayPanel().revalidateAllOrders();
	}// changeSCOwner()

 	
}// class UndoEditSCOwner

