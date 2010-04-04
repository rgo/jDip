/*
*  @(#)Remove.java	1.00	4/1/2002
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

import dip.misc.Log;
import dip.misc.Utils;

import dip.process.Adjudicator;
import dip.process.OrderState;
import dip.process.Tristate;
import dip.process.Adjustment;
import dip.process.Adjustment.AdjustmentInfo;


/**
*	Implementation of the Remove order.
*
*
*
*/
public class Remove extends Order
{
	// il8n constants
	private static final String REMOVE_FORMAT = "REMOVE_FORMAT";
	
	// constants: names
	private static final String orderNameBrief 	= "R";
	private static final String orderNameFull 	= "Remove";
	private static final transient String orderFormatString = Utils.getLocalString(REMOVE_FORMAT);
	
	
	/** Creates a Remove order */
	protected Remove(Power power, Location src, Unit.Type srcUnit)
	{
		super(power, src, srcUnit);
	}// Remove()
	
	/** Creates a Remove order */
	protected Remove()
	{
		super();
	}// Remove()
	
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
		
		sb.append(power);
		sb.append(": ");
		sb.append(orderNameBrief);
		sb.append(' ');
		sb.append(srcUnitType.getShortName());
		sb.append(' ');
		src.appendBrief(sb);
		
		return sb.toString();
	}// toBriefString()
	
	
	public String toFullString()
	{
		StringBuffer sb = new StringBuffer(128);
		
		sb.append(power);
		sb.append(": ");
		sb.append(orderNameFull);
		sb.append(' ');
		sb.append(srcUnitType.getFullName());
		sb.append(' ');
		src.appendFull(sb);
		
		return sb.toString();
	}// toFullString()	
	
	
	
	public boolean equals(Object obj)
	{
		if(obj instanceof Remove)
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
		checkSeasonAdjustment(state, orderNameFull);
		super.validate(state, valOpts, ruleOpts);
		checkPower(power, state, false);
		
		// not much else to validate; adjudiator must take care of tricky situations.
	}// validate()
	
	
	/** Empty method: Remove orders do not require verification. */
	public void verify(Adjudicator adjudicator)
	{
		OrderState thisOS = adjudicator.findOrderStateBySrc(getSource());
		thisOS.setVerified(true);
	}// verify()

	/** Empty method: Remove orders do not require dependency determination. */
	public void determineDependencies(Adjudicator adjudicator)	{}
	
	
	/**
	*	Remove orders are always successful.
	*	<p>
	*	Note that too many (or two few) remove orders may be given; this
	*	must be handled by the adjustment adjudicator.
	*	
	*/
	public void evaluate(Adjudicator adjudicator)
	{
		Log.println("--- evaluate() dip.order.Disband ---");
		Log.println("   order: ", this);
		
		OrderState thisOS = adjudicator.findOrderStateBySrc(getSource());
		if(thisOS.getEvalState() == Tristate.UNCERTAIN)
		{
			thisOS.setEvalState(Tristate.SUCCESS);
		}
	}// evaluate()
	
	
}// class Remove
