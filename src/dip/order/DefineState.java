/*
*  @(#)DefineState.java	1.00	4/1/2002
*
*  Copyright 2002 Zachary DelProposto. All rights reserved.
*  Use is subject to license terms.
*/
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
//  Or from http://www.gnu.org/package dip.order.result;
//
package dip.order;

import dip.world.*;

import dip.misc.Utils;
import dip.process.Adjudicator;
import dip.process.OrderState;

/**
*
*	Implementation of the Setup (DefineState) order.
*	<p>
*	This order is used to 'build' units, but typically
*	cannot be issued.
*
*
*/
public class DefineState extends Order
{
	// il8n constants
	private static final String DEFSTATE_NO_UNIT_TYPE = "DEFSTATE_NO_UNIT_TYPE";
	private static final String DEFSTATE_FORMAT = "DEFSTATE_FORMAT";
	private static final String DEFSTATE_VAL_DEFAULT = "DEFSTATE_VAL_DEFAULT";
	
	
	// constants: names
	private static final String orderNameBrief 	= "";
	private static final String orderNameFull 	= "Setup";
	private static final transient String orderFormatString = Utils.getLocalString(DEFSTATE_FORMAT);
	
	
	
	protected DefineState(Power power, Location src, Unit.Type srcUnit)
	throws OrderException 
	{
		super(power, src, srcUnit);
		
		if(srcUnit.equals(Unit.Type.UNDEFINED))
		{
			throw new OrderException(Utils.getLocalString(DEFSTATE_NO_UNIT_TYPE));
		}
	}// InitialState()
	
	
	protected DefineState()
	{
		super();
	}// DefineState()
	
	public String getFullName()
	{
		return orderNameFull;
	}// getName()
	
	public String getBriefName()
	{
		return orderNameBrief;
	}// getBriefName()
	
	
	
	// order formatting
	public String getDefaultFormat()
	{
		return orderFormatString;
	}// getFormatBrief()
	
	
	public String toBriefString()
	{
		StringBuffer sb = new StringBuffer(64);
		super.appendBrief(sb);
		return sb.toString();
	}// toBriefString()
	
	
	public String toFullString()
	{
		StringBuffer sb = new StringBuffer(128);
		super.appendFull(sb);
		return sb.toString();
	}// toFullString()	
	
	
	
	
	public boolean equals(Object obj)
	{
		if(obj instanceof DefineState)
		{
			DefineState ds = (DefineState) obj;
			if(super.equals(ds))
			{
				return true;
			}
		}
		return false;
	}// equals()
	
	/**
	*	DefineState orders will <b>always fail</b> validation. 
	*	<p>
	*	Their use is mainly for certain types of order parsing (like setting up 
	*	a game state). For example, dip.misc.TestSuite uses DefineState orders 
	*	to define the units and their positions for a test scenario. 
	*
	*/
	public void validate(TurnState state, ValidationOptions valOpts, RuleOptions ruleOpts)
	throws OrderException
	{	
		// DefineState orders always fail validation. 
		throw new OrderException( Utils.getLocalString(DEFSTATE_VAL_DEFAULT) );
	}// validate()
	
	/** DefineState orders do not require verification. */
	public void verify(Adjudicator adjudicator)
	{
		OrderState thisOS = adjudicator.findOrderStateBySrc(getSource());
		thisOS.setVerified(true);
	}// verify()
	
	/** Empty method: DefineState orders do not require dependency determination. */
	public void determineDependencies(Adjudicator adjudicator)	{}
	
	/** Empty method: DefineState orders do not require evaluation logic. */
	public void evaluate(Adjudicator adjudicator)
	{
		// do nothing
	}// evaluate()
	
}// class DefineState

