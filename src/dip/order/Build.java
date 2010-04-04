//	
//	 @(#)Build.java		4/1/2002
//	
//	 Copyright 2002 Zachary DelProposto. All rights reserved.
//	 Use is subject to license terms.
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
*
*	Implementation of the Build order.
*
*
*/

public class Build extends Order
{
	// il8n constants
	private static final String BUILD_MULTICOAST = "BUILD_MULTICOAST";
	private static final String BUILD_HOME_SUPPLY = "BUILD_HOME_SUPPLY";
	private static final String BUILD_OWNED_SUPPLY = "BUILD_OWNED_SUPPLY";
	private static final String BUILD_EXISTINGUNIT = "BUILD_EXISTINGUNIT";
	private static final String BUILD_FORMAT = "BUILD_FORMAT";
	private static final String BUILD_WING_PROHIBITED = "BUILD_WING_PROHIBITED";
	
	
	// constants: names
	private static final String orderNameBrief 	= "B";
	private static final String orderNameFull 	= "Build";
	private static final transient String orderFormatString = Utils.getLocalString(BUILD_FORMAT);
	
	
	/** Creates a Build order */
	protected Build(Power power, Location src, Unit.Type srcUnit)
	{
		super(power, src, srcUnit);
	}// Build()
	
	/** Creates a Build order */
	protected Build()
	{
		super();
	}// Build()
	
	public String getFullName()
	{
		return orderNameFull;
	}// getName()
	
	public String getBriefName()
	{
		return orderNameBrief;
	}// getBriefName()
	
	
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
		if(obj instanceof Build)
		{
			if(super.equals(obj))
			{
				return true;
			}
		}
		return false;
	}// equals()
	
	
	/**
		Builds:
		<p>
		Builds are valid if:
		<ol>		
			<li>supply center is owned by this power
			<li>supply centers is home to this power
			<li>province contains no unit
			<li>can build unit of that type in said province
			<li>if fleet specified, coast must be valid
			<li>default to army if no type specified AND supply center is inland
				otherwise build fails.
		</ol>
		<p>
		Validation is different than that of most other orders, since there
		is no unit present.
		<p>
		If builds not in home supply centers are allowed, restriction #2 above is waived.
		<p>
		WARNING: if the "build in any owned sc if one (or more) home sc is owned" rule is
		in place, this method does NOT currently check and reject the order if no home 
		supply centers are owned. However, no units are allowed to be built in that case
		anyway, so the adjustments should be 0.
		<p>
		NOTE: validate() cannot take care of state-dependent order problems (since validate()
		cannot know what other orders have been submitted), such as if too many
		build orders are submitted. verify() could, but currently does not.
		
	*/
	public void validate(TurnState state, ValidationOptions valOpts, RuleOptions ruleOpts)
	throws OrderException
	{
		checkSeasonAdjustment(state, orderNameFull);
		checkPower(power, state, true);
		
		Province province = src.getProvince();
		Position position = state.getPosition();
		
		// basic
		if( position.hasUnit(province) )
		{
			throw new OrderException(Utils.getLocalString(BUILD_EXISTINGUNIT));
		}
		else if( power != position.getSupplyCenterOwner(province) )
		{
			throw new OrderException(Utils.getLocalString(BUILD_OWNED_SUPPLY, power));
		}
		else if( power != position.getSupplyCenterHomePower(province) 
				 && ruleOpts.getOptionValue(RuleOptions.OPTION_BUILDS) == RuleOptions.VALUE_BUILDS_HOME_ONLY )
		{
			throw new OrderException(Utils.getLocalString(BUILD_HOME_SUPPLY));
		}
		
		// undefined types assumed to be Army
		if(srcUnitType.equals(Unit.Type.UNDEFINED))
		{
			srcUnitType = Unit.Type.ARMY;
		}
		
		// disallow wing units, if wing unit option prohibited
		if(	srcUnitType == Unit.Type.WING
			&& ruleOpts.getOptionValue(RuleOptions.OPTION_WINGS) == RuleOptions.VALUE_WINGS_DISABLED )
		{
			throw new OrderException(Utils.getLocalString(BUILD_WING_PROHIBITED));
		}
		
		src = src.getValidated(srcUnitType);
		
		// validate Borders
		Border border = src.getProvince().getTransit(src, srcUnitType, state.getPhase(), this.getClass());
		if(border != null)
		{
			throw new OrderException( Utils.getLocalString(ORD_VAL_BORDER, src.getProvince(), border.getDescription()) );
		}
		
		// if coast is still undefined after validation, error!
		if( src.getCoast().equals(Coast.UNDEFINED) )
		{
			throw new OrderException(Utils.getLocalString(BUILD_MULTICOAST));
		}
		
		// not much else to validate; adjudiator must take care of tricky situations.
		// such as too many or too few build orders
	}// validate()
	
	
	/** Empty method: Build orders do not require verification. */
	public void verify(Adjudicator adjudicator)	
	{
		OrderState thisOS = adjudicator.findOrderStateBySrc(getSource());
		thisOS.setVerified(true);
	}// verify()
	
	/** Empty method: Build orders do not require dependency determination. */
	public void determineDependencies(Adjudicator adjudicator)	{}
	
	
	/**
		Build orders are always successful.
		<p>
		If there are too few build orders, that is ok.
		If there are too many build orders, extra build orders
		must be discarded by the Adjustment adjudicator as appropriate.
		<p>
		Extra build orders are NOT considered in the evaluate() method here.
	*/
	public void evaluate(Adjudicator adjudicator)
	{
		Log.println("--- evaluate() dip.order.Build ---");
		Log.println("   order: ", this);
		
		OrderState thisOS = adjudicator.findOrderStateBySrc(getSource());
		
		if(thisOS.getEvalState() == Tristate.UNCERTAIN)
		{
			thisOS.setEvalState(Tristate.SUCCESS);
		}
	}// evaluate()
	
	
}// class Build


