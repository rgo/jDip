//
//  @(#)UndoDeleteOrder.java	1.00	4/1/2002
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

import dip.order.Orderable;
import dip.gui.OrderDisplayPanel;
import dip.misc.Utils;

import javax.swing.undo.*;



/**
*
*	UndoDeleteMultipleOrders is created any time multiple orders are
*	deleted. This can be used instead of a CompoundEdit of UndoDeleteOrders.
*	
*	
*/	
public class UndoDeleteMultipleOrders extends XAbstractUndoableEdit
{
	// instance variables
	private final static String PRESENTATION_NAME_PREFIX = "Undo.order.delete.multiple";
	private Orderable[] orders;
	
	
	public UndoDeleteMultipleOrders(UndoRedoManager urm, Orderable[] orders)
	{
		super(urm);
		this.orders = orders;
	}// UndoDeleteMultipleOrders()
	
	public String getPresentationName()
	{
		return Utils.getLocalString(PRESENTATION_NAME_PREFIX);
	}// getPresentationName()
	
	public void redo()
	throws CannotRedoException
	{
		super.redo();
		undoRedoManager.getOrderDisplayPanel().removeOrders(orders, false);
	}// redo()
	
	public void undo()
	throws CannotUndoException
	{
		super.undo();
		undoRedoManager.getOrderDisplayPanel().addOrdersRaw(orders, false);
	}// undo()
 	
}// class UndoDeleteMultipleOrders
