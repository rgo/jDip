//
//  @(#)RetreatChecker.java	1.00	4/1/2002
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
package dip.process;

import dip.world.Location;
import dip.world.TurnState;
import dip.world.Position;
import dip.world.World;

import dip.order.Orderable;
import dip.order.Move;
import dip.order.result.Result;
import dip.order.result.OrderResult;
import dip.order.result.OrderResult.ResultType;
import dip.misc.Log;

import java.util.List;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.HashMap;

/**
*	RetreatChecker analyzes the current TurnState and the results of the previous
*	TurnState to determine which (if any) retreat locations are acceptable for a 
*	retreating unit. Thus it is only dependent upon the adjudication results, and
*	the current Position.
*	<p>
*	Basic Retreat Algorithm:<br>
*	<ul>
*	<li> locations must be adjacent to dislodged unit
*	<li> locations must be unoccupied
*	<li> unoccupied location must not be that of the dislodging attacks origin, 
*			unless that attack occured was by a (successful) convoyed Move
*	<li> unoccupied locations must not be involved in a standoff (2 or more 
*			unsuccesful Moves)
*	</ul>
*
*	Should be threadsafe.
*/
public class RetreatChecker
{
	// instance variables
	private transient final Position position;
	private transient final ArrayList filteredMoveResults;
	
	/**
	*	Create a RetreatChecker.
	*	<p>
	*	There must be at least one prior TurnState in the World object for 
	*	this to work, however, if we a unit is Dislodged and it is the very
	*	first TurnState (this can happen if the game is edited), it is allowed.
	*/
	public RetreatChecker(TurnState current)
	{
		List results = null;
		
		TurnState last = current.getWorld().getPreviousTurnState(current);
		if(last == null)
		{
			// if we are the very first TurnState, last==null is permissable,
			// but we must take special action to make it work
			World w = current.getWorld();
			if(w.getInitialTurnState() == current)
			{
				//Log.println("     no previous turnstate, and we are first; creating results");
				results = new ArrayList();
			}
			else
			{
				throw new IllegalStateException("No Previous Turn State!!");
			}
		}
		else
		{
			results = last.getResultList();
			//Log.println("     last turnstate: ",last.getPhase());
		}
		
		this.position = current.getPosition();
		this.filteredMoveResults = makeFMRList(results);
	}// RetreatChecker()
	
	
	/**
	*	Create a RetreatChecker.
	*	<p>
	*	Useful for when the previous TurnState has not yet been inserted
	*	into the World object.
	*/
	public RetreatChecker(TurnState current, List previousTurnStateResults)
	{
		if(current == null || previousTurnStateResults == null)
		{
			throw new IllegalStateException("null arguments!");
		}
		
		this.position = current.getPosition();
		this.filteredMoveResults = makeFMRList(previousTurnStateResults);
	}// RetreatChecker()
	
	
	
	/**
	*	Determines if the unit located in <code>from</code> can retreat to
	*	the Location <code>to</code>
	*
	*/
	public boolean isValid(Location from, Location to)
	{
		Location[] validLocs = getValidLocations(from);
		
		// debugging
		/*
		if(Log.isLogging())
		{
			Log.print("   valid retreat locations from ");
			Log.print(from);
			Log.print(": ");
			
			for(int i=0; i<validLocs.length; i++)
			{
				Log.print(validLocs[i]);
				Log.print(" ");
			}
			Log.print("\n");
		}
		*/
		
		for(int i=0; i<validLocs.length; i++)
		{
			if(validLocs[i].equals(to))
			{
				return true;
			}
		}
		
		return false;
	}// isValid()
	
	
	/**
	*	Gets all valid locations to which this unit may retreat.
	*	<p>
	*	Returns a zero-length array if there are no acceptable retreat locations.
	*/
	public Location[] getValidLocations(Location from)
	{
		List retreatLocations = new ArrayList(8);
		
		Location[] adjacent = from.getProvince().getAdjacentLocations(from.getCoast());
		
		for(int i=0; i<adjacent.length; i++)
		{
			if( !position.hasUnit( adjacent[i].getProvince() )
				&& !isDislodgersSpace(from, adjacent[i])
				&& !isContestedSpace(adjacent[i]) )
			{
				retreatLocations.add(adjacent[i]);
			}
		}
		
		return (Location[]) retreatLocations.toArray(new Location[retreatLocations.size()]);
	}// getValidLocations()
	
	
	
	/** Returns 'true' if at least one valid retreat exists for the dislodged unit in 'from' */
	public boolean hasRetreats(Location from)
	{
		Location[] adjacent = from.getProvince().getAdjacentLocations(from.getCoast());
		
		for(int i=0; i<adjacent.length; i++)
		{
			if( !position.hasUnit( adjacent[i].getProvince() )
				&& !isDislodgersSpace(from, adjacent[i])
				&& !isContestedSpace(adjacent[i]) )
			{
				return true;
			}
		}
		
		return false;
	}// hasNoRetreats()
	
	
	/** 
	*	Returns <code>true</code> if the space is unoccupied, 
	*	and their exists a <b>successful</b> move order from 
	*	that space which dislodged the unit.
	*	<b>
	*	@param dislodgedLoc The location with the dislodged unit
	*	@param loc The unoccupied location which we are checking
	*	@return <code>true</code> if Move from <code>loc</code> 
	*			dislodged <code>dislodgedLoc</code>
	*/
	private boolean isDislodgersSpace(Location dislodgedLoc, Location loc)
	{
		Iterator iter = filteredMoveResults.iterator();
		while(iter.hasNext())
		{
			RCMoveResult rcmr = (RCMoveResult) iter.next();
			
			// note: dislodgedLoc is the potential move destination
			if(rcmr.isDislodger(loc, dislodgedLoc))
			{
				return true;
			}
		}
		
		return false;
	}// isDislodgersSpace()
	
	
	
	
	/**
	*	Returns true if a standoff has occured; 
	*	A standoff exists if:
	*	<ol>
	*		<li>no unit in space (essential!)</li>
	*		<li>2 or more <b>legal ("valid")</b> failed move orders exist 
	*			with dest of space</li>
	*	</ol>
	*/
	private boolean isContestedSpace(Location loc)
	{
		if( position.hasUnit(loc.getProvince()) )
		{
			return false;
		}
		
		int moveCount = 0;
		
		Iterator iter = filteredMoveResults.iterator();
		while(iter.hasNext())
		{
			RCMoveResult rcmr = (RCMoveResult) iter.next();
			
			if(rcmr.isPossibleStandoff(loc))
			{
				moveCount++;
			}
		}
		
		return (moveCount >= 2);
	}// isContestedSpace()
	
	
	
	/** 
	*	Generate a List of (only) Move orders; when checking multiple 
	*	retreats this is a performance gain. 
	*	<p>
	*	The filtered Move results consist of going through all OrderResults
	*	looking for those involving Move orders. For each Move order, we 
	*	generate one RCMoveResult object, which holds the pertinent information
	*	about that Move order.
	*/
	private ArrayList makeFMRList(List turnStateResults)
	{
		ArrayList mrList = new ArrayList(64);
		HashMap map = new HashMap(119);	// key: move source province; value: RCMoveResult
		
		Iterator iter = turnStateResults.iterator();
		while(iter.hasNext())
		{
			Object obj = iter.next();
			if(obj instanceof OrderResult)
			{
				OrderResult or = (OrderResult) obj;
				Orderable order = or.getOrder();
				if(order instanceof Move)
				{
					// see if we have an entry for this Move; if so, 
					// set options; if not, create an entry.
					// This avoids duplicate entries per Move.
					//
					RCMoveResult rcmr = (RCMoveResult) map.get(order.getSource().getProvince());
					if(rcmr == null)
					{
						rcmr = new RCMoveResult(or);
						map.put(order.getSource().getProvince(), rcmr);
						mrList.add(rcmr);
					}
					else
					{
						rcmr.setOptions(or);
					}
				}
			}
		}
		
		map.clear();	// no longer needed
		
		return mrList;
	}// makeFMRList()
	
	
	
	/**
	*	RCMoveResult holds information about a Move order, as generated
	*	from an OrderResult.
	*	
	*/
	private class RCMoveResult
	{
		private final Move move;
		private boolean isSuccess = false;
		private boolean isByConvoy = false;
		private boolean isValid = true;
		
		/**
		*	Create an RCMoveResult. Assumes that the passed
		*	OrderResult refers to a Move order. Sets any options
		*	for the OrderResult.
		*/
		public RCMoveResult(OrderResult or)
		{
			move = (Move) or.getOrder();
			setOptions(or);
		}// RCMoveResult()
		
		/** 
		*	Given an OrderResult, checks to see if the success
		*	or convoy flags can be set. If the passed OrderResult
		*	does NOT refer to the same Move (via referential 
		*	equality), an exception is thrown.
		*/
		public final void setOptions(OrderResult or)
		{
			if(or.getOrder() != move)
			{
				throw new IllegalArgumentException();
			}
			
			if(or.getResultType() == OrderResult.ResultType.CONVOY_PATH_TAKEN)
			{
				isByConvoy = true;
			}
			else if(or.getResultType() == OrderResult.ResultType.SUCCESS)
			{
				isSuccess = true;
			}
			else if(or.getResultType() == OrderResult.ResultType.VALIDATION_FAILURE)
			{
				isValid = false;
			}
		}// setOptions()
		
		/** Successful? */
		public boolean isSuccess() 
		{ 
			return isSuccess; 
		}
		
		/**
		*	Is this move a potentially vying for a potential standoff?
		*	<p>
		*	This will return true iff:
		*		(1) Destination *province* of Move matchest given location
		*		(2) Move is NOT successful
		*		(3) Move is NOT invalid (i.e., no VALIDATION_FAILURE result)
		*/
		public boolean isPossibleStandoff(Location loc)
		{
			return
			(
				isValid
				&& !isSuccess
				&& move.getDest().isProvinceEqual(loc)
			);
		}// isPossibleStandoff()
		
		
		/**
		*	Is this move a potentially dislodging move? 
		*	This will return true iff: 
		*		(1) src and dest match;
		*		(2) successful
		*		(3) NOT convoyed (DATC 16-dec-03 4.A.5)
		*/
		public boolean isDislodger(Location src, Location dest)
		{
			return
			(
				isSuccess
				&& !isByConvoy
				&& move.getSource().isProvinceEqual(src)
				&& move.getDest().isProvinceEqual(dest) 
			);
		}// isDislodger()
		
	}// inner class RCMoveResult
	
	
}// class RetreatChecker
