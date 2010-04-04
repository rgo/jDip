//
//  @(#)PhaseSelector.java		4/2002
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

package dip.gui;

import dip.world.World;
import dip.world.Phase;
import dip.world.TurnState;

import java.util.Set;
import java.util.Arrays;

/**
*
*	Manages all (except for select) items in the History menu.
*	<p>
*	This object is created by ClientFrame, and can be used by 
*	other classes via the getPhaseSelector() method in ClientFrame.
*
*/
public class PhaseSelector
{
	private World currentWorld = null;
	private TurnState currentTS = null;
	private int currentPos = 0;
	private int maxPos = 0;
	private Phase[] phases = null;
	private ClientFrame parent = null;
	private ClientMenu menu = null;
	private PhasePCL pcl = null;
	
	/** Create a PhaseSelector object. */
	public PhaseSelector(ClientFrame parent)
	{
		this.parent = parent;
		this.menu = parent.getClientMenu();
		this.pcl = new PhasePCL();
		parent.addPropertyChangeListener(pcl);
	}// PhaseSelector()
	
	/** Cleanup the PhaseSelector object */
	public void close()
	{
		parent.removePropertyChangeListener(pcl);
	}// close()
	
	/** Go to the Previous phase, if possible. */
	public void previous()
	{
		if(currentPos > 0)
		{
			parent.fireTurnstateChanged( currentWorld.getTurnState(phases[currentPos-1]) );
		}
	}// previous()
	
	/** Go to the next phase, if possible. */
	public void next()
	{
		if(currentPos < maxPos)
		{
			parent.fireTurnstateChanged( currentWorld.getTurnState(phases[currentPos+1]) );
		}
	}// previous()
	
	/** Go to the first (initial) phase. */
	public void first()
	{
		this.currentTS = currentWorld.getInitialTurnState();
		parent.fireTurnstateChanged(currentTS);
	}// previous()
	
	/** Go to the last phase. */
	public void last()
	{
		this.currentTS = currentWorld.getLastTurnState();
		parent.fireTurnstateChanged(currentTS);
	}// previous()
	
	/** Get the total number of phases. */
	public int getPhaseCount()
	{
		return maxPos + 1;
	}// getPhaseCount()
	
	/** Get the current phase position. */
	public int getCurrentPhasePosition()
	{
		return currentPos;
	}// getCurrentPhasePosition()
	
	
	private void setWorld(World newWorld)
	{
		if(newWorld == null)
		{
			// disable menu options
			menu.setEnabled(ClientMenu.HISTORY_PREVIOUS, false);
			menu.setEnabled(ClientMenu.HISTORY_NEXT, false);
			menu.setEnabled(ClientMenu.HISTORY_INITIAL, false);
			menu.setEnabled(ClientMenu.HISTORY_LAST, false);
			menu.setEnabled(ClientMenu.HISTORY_SELECT, false);
			
			// reset our data
			currentWorld = null;
			currentTS = null;
			phases = null;
			currentPos = 0;
			maxPos = 0;
		}
		else
		{
			// enable menu options
			//
			// always enable select
			menu.setEnabled(ClientMenu.HISTORY_SELECT, true);
			
			// always enable first and last
			menu.setEnabled(ClientMenu.HISTORY_INITIAL, true);
			menu.setEnabled(ClientMenu.HISTORY_LAST, true);
			
			// set our data
			currentWorld = newWorld;
		}
	}// setWorld()
	
	
	private void setTurnState(TurnState ts)
	{
		if(ts == null)
		{
			throw new IllegalArgumentException("null turnstate");
		}
		
		if(currentWorld == null)
		{
			throw new IllegalStateException("null world");
		}
		
		currentTS = ts;
		setCurrentPosition();
	}// setTurnState()
	
	
	
	private void setCurrentPosition()
	{
		// get set, convert to array
		Set set = currentWorld.getPhaseSet();
		phases = (Phase[]) set.toArray(new Phase[set.size()]);
		
		// set max size
		maxPos = phases.length - 1;	
		
		// find current position in array
		Phase currentPhase = currentTS.getPhase();
		currentPos = Arrays.binarySearch(phases, currentPhase);
		if(currentPos < 0)
		{
			throw new IllegalStateException("bad position!");
		}
		
		// update
		updateNextPrevious();
		updateResults();
	}// setCurrentPosition()
	
	
	/** Update Next/Previous menu items */
	private void updateNextPrevious()
	{
		menu.setEnabled(ClientMenu.HISTORY_PREVIOUS, (currentPos > 0));
		menu.setEnabled(ClientMenu.HISTORY_NEXT, (currentPos < maxPos));
	}// updateNextPrevious()
	
	
	/** Update the Reports | Result (current, previous) menu items */
	private void updateResults()
	{
		if(currentTS == null)
		{
			// if currentTS is null, previous turn state must also be null
			menu.setEnabled(ClientMenu.REPORTS_RESULTS, false);
			menu.setEnabled(ClientMenu.REPORTS_PREVIOUS_RESULTS, false);
		}
		else
		{
			// set current results, if available
			menu.setEnabled(ClientMenu.REPORTS_RESULTS, currentTS.isResolved());
			
			// last results available? (if a turnstate is available, and we are not at initial position)
			if(currentPos <= 0)
			{
				menu.setEnabled(ClientMenu.REPORTS_PREVIOUS_RESULTS, false);
			}
			else
			{
				TurnState previousTS = currentWorld.getTurnState(phases[currentPos-1]);
				if(previousTS != null)
				{
					menu.setEnabled(ClientMenu.REPORTS_PREVIOUS_RESULTS, previousTS.isResolved());
				}
				else
				{
					menu.setEnabled(ClientMenu.REPORTS_PREVIOUS_RESULTS, false);
				}
			}
		}
	}// updateResults()
	
	
	
	/**
	* Property Change Listener
	*
	*/
	private class PhasePCL extends AbstractCFPListener 
	{
		
		public void actionWorldCreated(World w)			
		{
			setWorld(w);
		}// actionWorldCreated()
		
		public void actionWorldDestroyed(World w)		
		{
			setWorld(null);
		}// actionWorldDestroyed()
		
		public void actionTurnstateChanged(TurnState ts)	
		{
			setTurnState(ts);
		}// actionTurnstateChanged()
		
		public void actionTurnstateAdded(TurnState ts)		
		{
			setCurrentPosition();
		}// actionTurnstateAdded()
		
		public void actionTurnstateRemoved()				
		{
			setCurrentPosition();
		}// actionTurnstateRemoved()
		
		public void actionModeChanged(String newMode)
		{	
			if( newMode == ClientFrame.MODE_NONE 
				|| newMode == ClientFrame.MODE_EDIT )
			{
				// disable history menu, completely
				menu.setEnabled(ClientMenu.HISTORY_PREVIOUS, false);
				menu.setEnabled(ClientMenu.HISTORY_NEXT, false);
				menu.setEnabled(ClientMenu.HISTORY_INITIAL, false);
				menu.setEnabled(ClientMenu.HISTORY_LAST, false);
				menu.setEnabled(ClientMenu.HISTORY_SELECT, false);
			}
			else 
			{
				// refresh the history menu
				menu.setEnabled(ClientMenu.HISTORY_INITIAL, true);
				menu.setEnabled(ClientMenu.HISTORY_LAST, true);
				menu.setEnabled(ClientMenu.HISTORY_SELECT, true);
				updateNextPrevious();
			}
			
			updateResults();
		}// actionModeChanged()
	}// inner class PCListener
	
	
}// class PhaseSelector
