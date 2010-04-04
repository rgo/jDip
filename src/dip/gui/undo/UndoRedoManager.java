//
//  @(#)UndoRedoManager.java	1.00	4/2002
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

import dip.gui.ClientMenu;
import dip.gui.OrderDisplayPanel;
import dip.gui.ClientFrame;
import dip.misc.Log;

import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;

import java.io.*;
import java.util.*;

/**
*
*	Manages Undo/Redo events, and associated Edit menu items involving
*	Undo and Redo.
*	<p>
*	This is not a singleton.
*
*
*/
public class UndoRedoManager extends UndoManager
{
	// the max number of undo/redo events we can hold
	private static final int 	MAX_UNDOS			= 1000;
	
	
	// instance variables
	private transient ClientFrame clientFrame;
	private transient OrderDisplayPanel orderDisplayPanel;
	
	
	
	/** Constructor */
	public UndoRedoManager(ClientFrame clientFrame, OrderDisplayPanel orderDisplayPanel)
	{
		super();
		if(clientFrame == null || orderDisplayPanel == null)
		{
			throw new IllegalArgumentException("null argument(s)");
		}
		
		this.clientFrame = clientFrame;
		this.orderDisplayPanel = orderDisplayPanel;
		
		super.setLimit(MAX_UNDOS);
	}// UndoRedoManager()
	
	
	/** Can be used post-deserialization */
	public synchronized void setClientFrame(ClientFrame clientFrame)
	{
		if(clientFrame == null)
		{
			throw new IllegalArgumentException("null clientFrame");
		}
		this.clientFrame = clientFrame;
	}// setClientFrame()
	
	
	/** Can be used post-deserialization */
	public synchronized void setOrderDisplayPanel(OrderDisplayPanel orderDisplayPanel)
	{
		if(orderDisplayPanel == null)
		{
			throw new IllegalArgumentException("null clientFrame");
		}
		this.orderDisplayPanel = orderDisplayPanel;
	}// setOrderDisplayPanel()
	
	
	/** Add an Edit (UndoableEdit) */
	public synchronized boolean addEdit(UndoableEdit anEdit)
	{
		//System.out.println("URM: addEdit(): edits (before): "+edits.size());
		checkState();
		//System.out.println("     added: "+anEdit);
		final boolean retVal = super.addEdit(anEdit);
		//System.out.println("     addEdit(): edits (after): "+edits.size());
		refreshMenu();
		return retVal;
	}// addEdit()
	
	/** Redo last undo */
	public synchronized void redo()
	{
		//System.out.println("URM: redo(): edits (before): "+edits.size());
		checkState();
		super.redo();
		//System.out.println("     redo(): edits (after): "+edits.size());
		refreshMenu();
	}// redo()
	
	/** Undo an UndoableEdit */
	public synchronized void undo()
	{
		//System.out.println("URM: undo(): edits (before): "+edits.size());
		checkState();
		super.undo();
		//System.out.println("     undo(): edits (after): "+edits.size());
		refreshMenu();
	}// undo()
	
	/** Throw away all stored edits */
	public synchronized void discardAllEdits()
	{
		checkState();
		super.discardAllEdits();
		refreshMenu();
	}// discardAllEdits()
	
	/** Undo or Redo */
	public synchronized void undoOrRedo()
	{
		checkState();
		super.undoOrRedo();
		refreshMenu();
	}// undoOrRedo()
	
	
	/** For debugging: lists edits. */
	public void dumpEdits()
	{
		if(Log.isLogging())
		{
			Log.println("UndoRedoManager: "+edits.size()+" edits");
			for(int i=0; i<edits.size(); i++)
			{
				Log.println("  "+i+": "+edits.get(i));
			}
		}
	}// dumpEdits()
	
	/** Returns the OrderDisplayPanel associated with this UndoRedo manager. */
	public synchronized OrderDisplayPanel getOrderDisplayPanel()
	{
		return orderDisplayPanel;
	}// getOrderDisplayPanel()
	
	/** Returns the ClientFrame object */
	public synchronized ClientFrame getClientFrame()
	{
		return clientFrame;
	}// getWorld()
	
	
	/** 
	*	Refreshes the menu items, enabling & adding action names as appropriate.
	*	This is mode-aware; undo/redo is only available when in MODE_EDIT and
	*	MODE_ORDER.
	*/
	public void refreshMenu()
	{
		ClientMenu menu = clientFrame.getClientMenu();
		menu.setText(ClientMenu.EDIT_UNDO, getUndoPresentationName());
		menu.setText(ClientMenu.EDIT_REDO, getRedoPresentationName());
		
		if( clientFrame.getMode() == ClientFrame.MODE_ORDER ||
			clientFrame.getMode() == ClientFrame.MODE_EDIT )
		{
			menu.setEnabled(ClientMenu.EDIT_UNDO, canUndo());
			menu.setEnabled(ClientMenu.EDIT_REDO, canRedo());
		}
		else
		{
			menu.setEnabled(ClientMenu.EDIT_UNDO, false);
			menu.setEnabled(ClientMenu.EDIT_REDO, false);
		}
	}// refreshMenu()
	
	
	/** 
	*	Filters the Undo list, in reverse order, removing all actions
	*	until the first UndoResolve action is detected. This is used 
	*	when in F2F and switching powers, so that a power can 'undo'
	*	but cannot undo or see another power's moves.
	*/
	public synchronized void filterF2F()
	{
		Log.println("UndoRedoManager::filterF2F()");
		
		ListIterator listIter = edits.listIterator(edits.size());
		
		int from = Integer.MAX_VALUE;
		
		while(listIter.hasPrevious())
		{
			final int idx = listIter.previousIndex();
			UndoableEdit ue = (UndoableEdit) listIter.previous();
			
			//Log.println("  checking: ", String.valueOf(idx), ": ", ue.getClass().getName());
			
			if(ue instanceof UndoResolve)
			{
				break;
			}
			else
			{
				from = idx;
			}
		}
		
		// trim the edits (trimEdits() does nothing if from > to)
		Log.println("  trimEdits(): ", String.valueOf(from), "->", String.valueOf(edits.size()-1));
		trimEdits(from, edits.size()-1);
		refreshMenu();
	}// filterF2F()
	
	
	/**
	*	Simplifies the undo list. We save all undoable actions for the current
	*	TurnState. However, after the TurnState has been resolved, all old 
	*	actions (order adds/deletes, edit actions, etc) <b>except</b> for 
	*	UndoResolve are eliminated.
	*	<p>
	*	The rationale for this is this enables quickly undoing a number of
	*	resolves, and simplifies the task of going 'back in time' to change
	*	the game.
	*
	*/
	public synchronized void simplify()
	{
		Log.println("UndoRedoManager::simplify()");
		
		//dumpEdits();
		
		// search backwards. After we find the first UndoResolve, keep going
		// backwards, telling those edits to die() and then dequeuing 
		// unless they are UndoResolves.
		//
		// We wait until after we find the first UndoResolve to avoid destroying
		// any edits (if any) in the current unresolved turnstate.
		//
		ListIterator listIter = edits.listIterator(edits.size());
		boolean foundResolved = false;
		
		int from = Integer.MAX_VALUE;
		int to = -1;
		
		while(listIter.hasPrevious())
		{
			final int idx = listIter.previousIndex();
			UndoableEdit ue = (UndoableEdit) listIter.previous();
			
			//Log.println("  checking: ", String.valueOf(idx), ": ", ue.getClass().getName());
			
			if(ue instanceof UndoResolve)
			{
				if(!foundResolved)
				{
					foundResolved = true;
				}
				else
				{
					break;
				}
			}
			else
			{
				if(foundResolved && to < 0)
				{
					to = idx;	// set but one time
				}
				else
				{
					from = idx;	// this gets updated only when we found a 'to'
				}
			}
		}
		
		// trim the edits (trimEdits() does nothing if from > to)
		Log.println("  trimEdits(): ", String.valueOf(from), "->", String.valueOf(to));
		trimEdits(from, to);
		refreshMenu();
	}// simplify()
	
	
	/** Internal consistency check */
	private void checkState()
	{
		if(clientFrame == null || orderDisplayPanel == null)
		{
			throw new IllegalArgumentException("error: clientframe/orderpanel not set");
		}
	}// checkState()
	
}// class UndoRedoManager////
