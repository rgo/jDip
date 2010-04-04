/*
*  @(#)Hold.java	1.00	4/1/2002
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

import dip.process.Adjudicator;
import dip.process.OrderState;
import dip.process.Tristate;

import dip.misc.Log;
import dip.misc.Utils;

import java.util.List;
import java.util.Iterator;


/**
*
*	Implementation of the Hold order.
*
*
*/
public class Hold extends Order
{
	// il8n
	private static final String HOLD_FORMAT = "HOLD_FORMAT";
	
	// constants: names
	private static final String orderNameBrief 	= "H";
	private static final String orderNameFull 	= "Hold";
	private static final transient String orderFormatString = Utils.getLocalString(HOLD_FORMAT);
	
	
	/** Creates a Hold order */
	protected Hold(Power power, Location src, Unit.Type srcUnit)
	{
		super(power, src, srcUnit);
	}// Hold()
	
	/** Creates a Hold order */
	protected Hold()
	{
		super();
	}// Hold()
	
	
	public String getFullName()
	{
		return orderNameFull;
	}// getName()
	
	public String getBriefName()
	{
		return orderNameBrief;
	}// getBriefName()
	
	
	// format-strings for orders
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
		if(obj instanceof Hold && super.equals(obj))
		{
			return true;
		}
		return false;
	}// equals()	
	
	
	public void validate(TurnState state, ValidationOptions valOpts, RuleOptions ruleOpts)
	throws OrderException
	{
		checkSeasonMovement(state, orderNameFull);
		checkPower(power, state, false);	// inactive powers can issue Hold orders
		super.validate(state, valOpts, ruleOpts);
		
		// validate Borders
		Border border = src.getProvince().getTransit(src, srcUnitType, state.getPhase(), this.getClass());
		if(border != null)
		{
			throw new OrderException( Utils.getLocalString(ORD_VAL_BORDER, src.getProvince(), border.getDescription()) );
		}
	}// validate();
	
	
	/** No verification is required for Hold orders. */
	public void verify(Adjudicator adjudicator)
	{
		OrderState thisOS = adjudicator.findOrderStateBySrc(getSource());
		thisOS.setVerified(true);
	}// verify()
	
	
	/**
	*	Dependencies for a Hold order:
	*	<ol>
	*		<li>Supports to this space
	*		<li>Moves to this space
	*	</ol>
	*/
	public void determineDependencies(Adjudicator adjudicator)
	{
		addSupportsOfAndMovesToSource(adjudicator);
	}// determineDependencies()
	
	/** Hold order evaluation logic. */
	public void evaluate(Adjudicator adjudicator)
	{
		Log.println("--- evaluate() dip.order.Hold ---");
		
		OrderState thisOS = adjudicator.findOrderStateBySrc(getSource());
		
		// calculate support
		thisOS.setDefMax( thisOS.getSupport(false) );
		thisOS.setDefCertain( thisOS.getSupport(true) );
		
		if(Log.isLogging())
		{
			Log.println("   order: ",this);
			Log.println("   initial evalstate: ",thisOS.getEvalState());
			Log.println("     def-max: ",thisOS.getDefMax());
			Log.println("    def-cert: ",thisOS.getDefCertain());		
			Log.println("  # supports: ",thisOS.getDependentSupports().length);
			Log.println("  dislodged?: ",thisOS.getDislodgedState());
		}
		
		if(thisOS.getEvalState() == Tristate.UNCERTAIN)
		{
			// if no moves against this order, we must succeed.
			// Otherwise, MOVE orders will determine if we are dislodged and thus fail.
			if( thisOS.getDependentMovesToSource().length == 0)
			{
				thisOS.setEvalState(Tristate.SUCCESS);
				thisOS.setDislodgedState(Tristate.NO);
			}
		}
		
		Log.println("  final evalState: ", thisOS.getEvalState());
	}// evaluate()
	
}// class Hold
