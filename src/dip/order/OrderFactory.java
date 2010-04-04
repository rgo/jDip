//
//  @(#)OrderFactory.java	12/2002
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
package dip.order;

import dip.world.Power;
import dip.world.Location;
import dip.world.Province;
import dip.world.Unit;

import java.util.List;

/**
*	
*	Creates Orders..
*	<p>
*
*/
public abstract class OrderFactory
{
	private static final OrderFactory defaultFactory;
	
	// static initializers
	static
	{
		defaultFactory = new DefaultOrderFactory();
	}
	
	
	/** Creates an OrderFactory */
	protected OrderFactory()
	{
	}// OrderFactory()
	
	
	/** 
	*	Get the default OrderFactory, which is 
	*	dip.order.OrderFactory.DefaultOrderFactory.
	*	<p>
	*	<b>WARNING:</b> this should NOT be used if there is any likelyhood
	*	of every wanting to use GUI orders (e.g., via GUIOrderFactory). 
	*	GUI orders are completely compatible with regular orders, via the
	*	Orderable interface.
	*	<p>
	*	This method is mainly provided for convenience (e.g., command-line 
	*	utilities).
	*/
	public static OrderFactory getDefault()
	{
		return defaultFactory;
	}// getDefault()
	
	/** Creates a Hold order */
	public abstract Hold createHold(Power power, Location source, 
		Unit.Type sourceUnitType);
	
	/** Creates a Move order */
	public abstract Move createMove(Power power, Location source, 
		Unit.Type srcUnitType, Location dest);
	
	/** Creates a Move order */
	public abstract Move createMove(Power power, Location source, 
		Unit.Type srcUnitType, Location dest, boolean isConvoying);
	
	/** Creates a Move order */
	public abstract Move createMove(Power power, Location src, 
		Unit.Type srcUnitType, Location dest, Province[] convoyRoute);
	
	/** Creates a Move order */
	public abstract Move createMove(Power power, Location src, 
		Unit.Type srcUnitType, Location dest, List routes);
	
	/** Creates a Support order, to Support a unit staying in place. */
	public abstract Support createSupport(Power power, Location src, 
		Unit.Type srcUnitType, Location supSrc, Power supPower, 
		Unit.Type supUnitType);
	
	/** 
	*	Creates a Support order, to Support a unit moving 
	*	(or staying in place, if supDest == null) 
	*/
	public abstract Support createSupport(Power power, Location src, 
		Unit.Type srcUnitType, Location supSrc, Power supPower, 
		Unit.Type supUnitType, Location supDest);
	
	/** Creates a Convoy order */
	public abstract Convoy createConvoy(Power power, Location src, 
		Unit.Type srcUnitType, Location convoySrc, Power convoyPower,
		Unit.Type convoySrcUnitType, Location convoyDest);
	
	/** Creates a Retreat order */
	public abstract Retreat createRetreat(Power power, Location source, 
		Unit.Type srcUnitType, Location dest);
	
	/** Creates a Disband order */
	public abstract Disband createDisband(Power power, Location source, 
		Unit.Type sourceUnitType);
	
	/** Creates a Build order */
	public abstract Build createBuild(Power power, Location source, 
		Unit.Type sourceUnitType);
	
	/** Creates a Remove order */
	public abstract Remove createRemove(Power power, Location source, 
		Unit.Type sourceUnitType);
	
	/** Creates a Waive order */
	public abstract Waive createWaive(Power power, Location source);
	
	/** Creates a DefineState order */
	public abstract DefineState createDefineState(Power power, 
		Location source, Unit.Type sourceUnitType)
	throws OrderException;
	
	
	private static final class DefaultOrderFactory extends OrderFactory
	{
		/** Create a DefaultOrderFactory */
		protected DefaultOrderFactory()
		{
			super();
		}// DefaultOrderFactory()
		
		/** Creates a Hold order */
		public Hold createHold(Power power, Location source, 
			Unit.Type sourceUnitType)
		{
			return new Hold(power, source, sourceUnitType);
		}// createHold()
		
		
		/** Creates a Move order */
		public Move createMove(Power power, Location source, 
			Unit.Type srcUnitType, Location dest)
		{
			return new Move(power, source, srcUnitType, dest);
		}// createMove()
		
		/** Creates a Move order */
		public Move createMove(Power power, Location source, 
			Unit.Type srcUnitType, Location dest, boolean isConvoying)
		{
			return new Move(power, source, srcUnitType, dest, isConvoying);
		}// createMove()
		
		/** Creates a Move order */
		public Move createMove(Power power, Location src, 
			Unit.Type srcUnitType, Location dest, Province[] convoyRoute)
		{
			return new Move(power, src, srcUnitType, dest, convoyRoute);
		}// createMove()
		
		/** Creates a Move order */
		public Move createMove(Power power, Location src, 
			Unit.Type srcUnitType, Location dest, List routes)
		{
			return new Move(power, src, srcUnitType, dest, routes);
		}// createMove()
		
		/** Creates a Support order, to Support a unit staying in place. */
		public Support createSupport(Power power, Location src, 
			Unit.Type srcUnitType, Location supSrc, Power supPower, 
			Unit.Type supUnitType)
		{
			return new Support(power, src, srcUnitType, 
				supSrc, supPower, supUnitType);
		}// createSupport()
		
		
		/** 
		*	Creates a Support order, to Support a unit moving 
		*	(or staying in place, if supDest == null) 
		*/
		public Support createSupport(Power power, Location src, 
			Unit.Type srcUnitType, Location supSrc, Power supPower, 
			Unit.Type supUnitType, Location supDest)
		{
			return new Support(power, src, srcUnitType, supSrc, 
				supPower, supUnitType, supDest);
		}// createSupport()
		
		
		/** Creates a Convoy order */
		public Convoy createConvoy(Power power, Location src, 
			Unit.Type srcUnitType, Location convoySrc, Power convoyPower,
			Unit.Type convoySrcUnitType, Location convoyDest)
		{
			return new Convoy(power, src, srcUnitType, convoySrc, 
				convoyPower, convoySrcUnitType, convoyDest);
		}// createConvoy()
		
		
		/** Creates a Retreat order */
		public Retreat createRetreat(Power power, Location source, 
			Unit.Type srcUnitType, Location dest)
		{
			return new Retreat(power, source, srcUnitType, dest);
		}// createRetreat()
		
		
		/** Creates a Disband order */
		public Disband createDisband(Power power, Location source, 
			Unit.Type sourceUnitType)
		{
			return new Disband(power, source, sourceUnitType);
		}// createDisband()
		
		
		/** Creates a Build order */
		public Build createBuild(Power power, Location source, 
			Unit.Type sourceUnitType)
		{
			return new Build(power, source, sourceUnitType);
		}// createBuild()
		
		
		/** Creates a Remove order */
		public Remove createRemove(Power power, Location source, 
			Unit.Type sourceUnitType)
		{
			return new Remove(power, source, sourceUnitType);
		}// createRemove()
		
		/** Creates a Waive order */
		public Waive createWaive(Power power, Location source)
		{
			return new Waive(power, source);
		}// createWaive()
		
		/** Creates a DefineState order */
		public DefineState createDefineState(Power power, Location source, 
			Unit.Type sourceUnitType)
		throws OrderException
		{
			return new DefineState(power, source, sourceUnitType);
		}// createDefineState()
			
	}// inner class DefaultOrderFactory
	
}// class OrderFactory
