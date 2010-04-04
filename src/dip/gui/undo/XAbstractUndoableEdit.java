//
//  @(#)XAbstractUndoableEdit.java		1/2003
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

import javax.swing.undo.*;


/**
*
*	XAbstractUndoableEdit is the base class for all jDip undoable edits.
*	it provides base functionality and ensures serialization goes smoothly.
*	
*/	
public abstract class XAbstractUndoableEdit extends AbstractUndoableEdit implements java.io.Serializable
{
	protected UndoRedoManager undoRedoManager = null;
	
	/**
	*	Constructs an XAbstractUndoableEdit object.<p>
	*	UndoRedoManager must not be null.
	*
	*/
	public XAbstractUndoableEdit(UndoRedoManager urm)
	{
		if(urm == null)
		{
			throw new IllegalArgumentException();
		}
		
		undoRedoManager = urm;
	}// XAbstractUndoableEdit()
	
	/*
	
	for debugging. uniqueNum is a incrementing number
	to track edits.
	
	public String toString()
	{
		return this.getClass().getName() + "# " + uniqueNum;
	}// toString()
	
	public void undo()
	{
		super.undo();
		System.out.println("  undo(): "+this.getClass().getName()+": -"+uniqueNum);
	}// undo()
	
	public void redo()
	{
		super.redo();
		System.out.println("  redo(): "+this.getClass().getName()+": +"+uniqueNum);
	}// redo()
	
	public void die()
	{
		super.die();
		System.out.println("  die(): "+this.getClass().getName()+": +"+uniqueNum);
	}// die()
	*/
}// class XAbstractUndoableEdit
