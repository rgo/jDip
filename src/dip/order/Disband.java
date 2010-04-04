/*
*  @(#)Disband.java	1.00	4/1/2002
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

import dip.process.Adjudicator;
import dip.process.OrderState;
import dip.process.Tristate;

import dip.misc.Log;
import dip.misc.Utils;

import dip.world.*;

/**
*
*	Implementation of the Disband order.
*
*
*/
public class Disband extends Order
{
	// il8n
	private static final String DISBAND_FORMAT = "DISBAND_FORMAT";
	
	// constants: names
	private static final String orderNameBrief 	= "D";
	private static final String orderNameFull 	= "Disband";
	private static final transient String orderFormatString = Utils.getLocalString(DISBAND_FORMAT);
	
	
	/** Creates a Disband order */
	protected Disband(Power power, Location src, Unit.Type srcUnit)
	{
		super(power, src, srcUnit);
	}// Disband()
	
	/** Creates a Disband order */
	protected Disband()
	{
		super();
	}// Disband()
	
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
		sb.append(' ');
		sb.append(orderNameBrief);
		
		return sb.toString();
	}// toBriefString()
	
	
	public String toFullString()
	{
		StringBuffer sb = new StringBuffer(128);
		
		super.appendFull(sb);
		sb.append(' ');
		sb.append(orderNameFull);
		
		return sb.toString();
	}// toFullString()	
	
	
	
	
	public boolean equals(Object obj)
	{
		if(obj instanceof Disband)
		{
			if(super.equals(obj))
			{
				return true;
			}
		}
		return false;
	}// equals()	
	
	
	public void validate(TurnState state, ValidationOptions valOpts, RuleOptions ruleOpts)
	throws OrderException
	{	
		// step 0
		checkSeasonRetreat(state, orderNameFull);
		checkPower(power, state, false);	// inactive units can disband!
		
		// step 1
		Position position = state.getPosition();
		Unit unit = position.getDislodgedUnit( src.getProvince() );
		super.validate(valOpts, unit);
	}// validate()
	
	
	/** Disband orders do not require verification. */
	public void verify(Adjudicator adjudicator)
	{
		OrderState thisOS = adjudicator.findOrderStateBySrc(getSource());
		thisOS.setVerified(true);
	}// verify()
	
	/** Empty method: Disband orders do not require dependency determination. */
	public void determineDependencies(Adjudicator adjudicator)	{}
	
	
	/**
	*	Disband orders are always successful.
	*/
	public void evaluate(Adjudicator adjudicator)
	{
		OrderState thisOS = adjudicator.findOrderStateBySrc(getSource());
		
		Log.println("--- evaluate() dip.order.Disband ---");
		Log.println("   order: ", this);
		
		if(thisOS.getEvalState() == Tristate.UNCERTAIN)
		{
			thisOS.setEvalState(Tristate.SUCCESS);
		}
		
		Log.println("   result: ",thisOS.getEvalState());
	}// evaluate()
	
}// class Disband
