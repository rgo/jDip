//
//  @(#)GUIOrderFactory.java	12/2002
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
package dip.gui.order;

import dip.order.*;

import dip.world.Power;
import dip.world.Location;
import dip.world.Unit;
import dip.world.Province;

import java.util.List;

/**
*	
*	Creates GUIOrders. All Orders returned will be GUI subclasses of
*	the order objects. Additional no-argument create methods are supplied
*	for specific GUIOrder use.
*	<p>
*
*/
public class GUIOrderFactory extends OrderFactory
{
	
	
	/** Creates a GUIOrderFactory */
	public GUIOrderFactory()
	{
		super();
	}// GUIOrderFactory()
	
	
	
	
	/** Creates a GUIHold order */
	public GUIHold createGUIHold()
	{
		return new GUIHold();
	}// createGUIHold()
	
	/** Creates a GUIMove order */
	public GUIMove createGUIMove()
	{
		return new GUIMove();
	}// createGUIMove()
	
	/** Creates a GUIMoveExplicit order */
	public GUIMoveExplicit createGUIMoveExplicit()
	{
		return new GUIMoveExplicit();
	}// GUIMoveExplicit()
	
	
	
	/** Creates a GUISupport order */
	public GUISupport createGUISupport()
	{
		return new GUISupport();
	}// createGUISupport()
	
	/** Creates a GUIConvoy order */
	public GUIConvoy createGUIConvoy()
	{
		return new GUIConvoy();
	}// createGUIConvoy()
	
	/** Creates a GUIRetreat order */
	public GUIRetreat createGUIRetreat()
	{
		return new GUIRetreat();
	}// createGUIRetreat()
	
	/** Creates a GUIDisband order */
	public GUIDisband createGUIDisband()
	{
		return new GUIDisband();
	}// createGUIDisband()
	
	/** Creates a GUIBuild order */
	public GUIBuild createGUIBuild()
	{
		return new GUIBuild();
	}// createGUIBuild()
	
	/** Creates a GUIRemove order */
	public GUIRemove createGUIRemove()
	{
		return new GUIRemove();
	}// createGUIRemove()
	
	/** Creates a GUIWaive order */
	public GUIWaive createGUIWaive()
	{
		return new GUIWaive();
	}// createGUIWaive()
	
	/** Creates a GUIDefineState order */
	public GUIDefineState createGUIDefineState()
	{
		return new GUIDefineState();
	}// createGUIDefineState()
	
	
	// 
	// Standard OrderFactory overrides
	// 
	// 
	/** Creates a GUIHold order */
	public Hold createHold(Power power, Location source, Unit.Type sourceUnitType)
	{
		return new GUIHold(power, source, sourceUnitType);
	}// createHold()
	
	
	/** Creates a GUIMove order */
	public Move createMove(Power power, Location source, 
		Unit.Type srcUnitType, Location dest)
	{
		return new GUIMove(power, source, srcUnitType, dest, false);
	}// createMove()
	
	/** Creates a GUIMove order */
	public Move createMove(Power power, Location source, Unit.Type srcUnitType, 
		Location dest, boolean isConvoying)
	{
		return new GUIMove(power, source, srcUnitType, dest, isConvoying);
	}// createMove()
	
	/** Creates a GUIMove order */
	public Move createMove(Power power, Location src, Unit.Type srcUnitType, 
		Location dest, Province[] convoyRoute)
	{
		return new GUIMove(power, src, srcUnitType, dest, convoyRoute);
	}// createMove()
	
	/** Creates a GUIMove order */
	public Move createMove(Power power, Location src, Unit.Type srcUnitType, 
		Location dest, List routes)
	{
		return new GUIMove(power, src, srcUnitType, dest, routes);
	}// createMove()
	
	
	/** Creates a GUISupport order, to Support a unit staying in place. */
	public Support createSupport(Power power, Location src, Unit.Type srcUnitType, 
		Location supSrc, Power supPower, Unit.Type supUnitType)
	{
		return new GUISupport(power, src, srcUnitType, supSrc, supPower, supUnitType);
	}// createSupport()
	
	
	/** 
	*	Creates a GUISupport order, to Support a unit moving 
	*	(or staying in place, if supDest == null) 
	*/
	public Support createSupport(Power power, Location src, Unit.Type srcUnitType, 
		Location supSrc, Power supPower, Unit.Type supUnitType, Location supDest)
	{
		return new GUISupport(power, src, srcUnitType, supSrc, supPower, 
			supUnitType, supDest);
	}// createSupport()
	
	
	/** Creates a GUIConvoy order */
	public Convoy createConvoy(Power power, Location src, Unit.Type srcUnitType,
		Location convoySrc, Power convoyPower, 
		Unit.Type convoySrcUnitType, Location convoyDest)
	{
		return new GUIConvoy(power, src, srcUnitType, convoySrc, 
			convoyPower, convoySrcUnitType, convoyDest);
	}// createConvoy()
	
	
	/** Creates a GUIRetreat order */
	public Retreat createRetreat(Power power, Location source, 
		Unit.Type srcUnitType, Location dest)
	{
		return new GUIRetreat(power, source, srcUnitType, dest);
	}// createRetreat()
	
	
	/** Creates a GUIDisband order */
	public Disband createDisband(Power power, Location source, 
		Unit.Type sourceUnitType)
	{
		return new GUIDisband(power, source, sourceUnitType);
	}// createDisband()
	
	
	/** Creates a GUIBuild order */
	public Build createBuild(Power power, Location source, Unit.Type sourceUnitType)
	{
		return new GUIBuild(power, source, sourceUnitType);
	}// createBuild()
	
	
	/** Creates a GUIRemove order */
	public Remove createRemove(Power power, Location source, Unit.Type sourceUnitType)
	{
		return new GUIRemove(power, source, sourceUnitType);
	}// createRemove()
	
	/** Creates a GUIWaive order */
	public Waive createWaive(Power power, Location source)
	{
		return new GUIWaive(power, source);
	}// createGUIWaive()
	
	/** Creates a GUIDefineState order */
	public DefineState createDefineState(Power power, Location source, 
		Unit.Type sourceUnitType)
	throws OrderException
	{
		return new GUIDefineState(power, source, sourceUnitType);
	}// createDefineState()
	
	
	
	
	
	
}// class GUIOrderFactory
