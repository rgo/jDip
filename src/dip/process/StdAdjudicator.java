//
//  @(#)StdAdjudicator.java	1.00	4/1/2002
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
import dip.world.Province;
import dip.world.Unit;
import dip.world.Path;
import dip.world.Phase;
import dip.world.Power;
import dip.world.TurnState;
import dip.world.World;
import dip.world.Position;
import dip.world.VictoryConditions;
import dip.world.RuleOptions;

import dip.order.*;
import dip.order.result.Result;
import dip.order.result.TimeResult;
import dip.order.result.OrderResult;
import dip.order.result.DislodgedResult;
import dip.order.result.BouncedResult;
import dip.order.result.SubstitutedResult;
import dip.order.result.OrderResult.ResultType;

import dip.misc.Log;
import dip.misc.Utils;

import java.util.*;
import java.text.MessageFormat;

/**
*
*	StdAjudicator is adjudicates all phases of a typical game, using
*	the standard rule set, as well as enhancements indicated in 
*	the DPTG and DATC.
*	<p>
*	This is a pretty large class, and could be split into 3 separate 
*	classes (1 for each phase).
*		
*/
public class StdAdjudicator implements Adjudicator
{
	// il8n messages
	private static final String STDADJ_DUP_ORDER = "STDADJ_DUP_ORDER";
	private static final String STDADJ_MV_NO_UNIT = "STDADJ_MV_NO_UNIT";
	private static final String STDADJ_MV_NO_ORDER = "STDADJ_MV_NO_ORDER";
	private static final String STDADJ_MV_BAD = "STDADJ_MV_BAD";
	private static final String STDADJ_MV_UNRESOLVED_PARADOX = "STDADJ_MV_UNRESOLVED_PARADOX";
	private static final String STDADJ_MV_SZYKMAN_NOTICE = "STDADJ_MV_SZYKMAN_NOTICE";
	private static final String STDADJ_MV_SZYKMAN_MOVE_FAILED = "STDADJ_MV_SZYKMAN_MOVE_FAILED";
	private static final String STDADJ_MV_UNIT_DESTROYED = "STDADJ_MV_UNIT_DESTROYED";
	private static final String STDADJ_MV_PHASE_ADV_ALL_DESTROYED = "STDADJ_MV_PHASE_ADV_ALL_DESTROYED";
	private static final String STDADJ_RET_BAD_UNIT = "STDADJ_RET_BAD_UNIT";
	private static final String STDADJ_RET_NO_ORDER = "STDADJ_RET_NO_ORDER";
	private static final String STDADJ_RET_VAL_FAIL = "STDADJ_RET_VAL_FAIL";
	private static final String STDADJ_ADJ_IGNORED_MUST_BUILD = "STDADJ_ADJ_IGNORED_MUST_BUILD";
	private static final String STDADJ_ADJ_IGNORED_MUST_REMOVE = "STDADJ_ADJ_IGNORED_MUST_REMOVE";
	private static final String STDADJ_ADJ_IGNORED_NO_CHANGE = "STDADJ_ADJ_IGNORED_NO_CHANGE";
	private static final String STDADJ_ADJ_IGNORED_TOO_MANY = "STDADJ_ADJ_IGNORED_TOO_MANY";
	private static final String STDADJ_ADJ_IGNORED_INVALID = "STDADJ_ADJ_IGNORED_INVALID";
	private static final String STDADJ_ADJ_TOO_FEW_DISBANDS = "STDADJ_ADJ_TOO_FEW_DISBANDS";
	private static final String STDADJ_ADJ_ELIMINATED = "STDADJ_ADJ_ELIMINATED";
	private static final String STDADJ_ADJ_NO_MORE_DISBANDS = "STDADJ_ADJ_NO_MORE_DISBANDS";
	private static final String STDADJ_ADJ_DISBAND_ORDER = "STDADJ_ADJ_DISBAND_ORDER";
	private static final String STDADJ_ADJ_BUILDS_UNUSED = "STDADJ_ADJ_BUILDS_UNUSED";
	private static final String STDADJ_ADJ_IGNORED_DUPLICATE = "STDADJ_ADJ_IGNORED_DUPLICATE";
	
	
	
	private static final String STDADJ_PREADJ_TOBUILD = "STDADJ_PREADJ_TOBUILD";
	private static final String STDADJ_PREADJ_TOREMOVE = "STDADJ_PREADJ_TOREMOVE";
	private static final String STDADJ_PREADJ_TONEITHER = "STDADJ_PREADJ_TONEITHER";
	private static final String STDADJ_SKIP_RETREAT = "STDADJ_SKIP_RETREAT";
	private static final String STDADJ_SKIP_ADJUSTMENT = "STDADJ_SKIP_ADJUSTMENT";
	private static final String STDADJ_COMPLETED = "TimeResult.adjudication.complete";
	private static final String STDADJ_POWER_ORDER_LIST_CORRUPT = "STDADJ_POWER_ORDER_LIST_CORRUPT";
	private static final String STDADJ_INACTIVE_POWER_DISLODGED = "STDADJ_INACTIVE_POWER_DISLODGED";
	
	// messageformat statics [for performance enhancement]
	// these are complex Choice formats
	// ?? will this be threadsafe ?? 
	private static final MessageFormat MFRemove = new MessageFormat(Utils.getLocalString(STDADJ_PREADJ_TOREMOVE));
	private static final MessageFormat MFBuild = new MessageFormat(Utils.getLocalString(STDADJ_PREADJ_TOBUILD));
	private static final OrderFormatOptions DEFAULT_OFO = OrderFormatOptions.createDefault();
	
	// instance variables
	private final OrderFactory orderFactory;
	private OrderFormatOptions orderFormat = DEFAULT_OFO;
	private final TurnState turnState;
	private final Position position;
	private final World world;
	private final RuleOptions ruleOpts;
	private final List resultList;
	private final Map osMap;
	private final ArrayList substOrders;
	
	private OrderState[] orderStates = null;
	private boolean isUnRezParadox = false;
	private int paradoxBreakAttempt = 0;
	private int syzkmanAppliedCount = 0;
	private boolean statReporting = false;
	private boolean isPOCEnabled = false;
	private TurnState nextTurnState = null;
	
	/** 
	*	Create a Adjudicator for the Standard rules, that will evaluate all Orders 
	*	for the current TurnState.
	*/
	public StdAdjudicator(OrderFactory orderFactory, TurnState ts)
	{
		// initialization
		this.orderFactory = orderFactory;
		this.turnState = ts;
		this.position = ts.getPosition();
		this.world = ts.getWorld();
		this.ruleOpts = world.getRuleOptions();
		this.resultList = ts.getResultList();
		this.osMap = new HashMap(119);
		this.substOrders = new ArrayList(16);
	}// StdAdjudicator()
	
	
	/** Process the orders. */
	public void process()
	{
		Phase.PhaseType pt = turnState.getPhase().getPhaseType();
		
		if(isPOCEnabled)
		{
			checkOrders();
		}
		
		if(pt == Phase.PhaseType.MOVEMENT)
		{
			adjudicateMoves();
		}
		else if(pt == Phase.PhaseType.RETREAT)
		{
			adjudicateRetreats();
		}
		else if(pt == Phase.PhaseType.ADJUSTMENT)
		{
			adjudicateAdjustment();
		}
		else
		{
			// we could use an assertion here...
			throw new IllegalStateException("cannot adjudicate phase: "+pt);
		}
	}// process()
	
	
	/** Sets the order formatting options */
	public void setOrderFormat(OrderFormatOptions ofo)
	{
		if(ofo == null)
		{
			throw new IllegalArgumentException();
		}
		
		orderFormat = ofo;
	}// setOrderFormat()
	
	
	/** Enable or disable reporting of failure statistics. */
	public void setStatReporting(boolean value)
	{
		statReporting = value;
	}// setStatReporting()
	
	/**
	*	If enabled, checks to make sure that each Power's 
	*	list of orders only contains orders from that Power.
	*	This is important for networked games, to prevent
	*	illicit order injection.
	*/
	public void setPowerOrderChecking(boolean value)
	{
		isPOCEnabled = value;
	}// setPowerOrderChecking()
	
	/** Get all OrderStates */
	public final OrderState[] getOrderStates()
	{
		return orderStates;
	}// getOrderStates()
	
	/** Get the TurnState */
	public final TurnState getTurnState()
	{
		return turnState;
	}// getTurnState()
	
	/** 
	*	Find the OrderState with the given source Province. Returns null if
	*	no corresponding order was found. <b>Note:</b> Coast is not relevent
	*	here; only the Province in the given Location is used.
	*/
	public final OrderState findOrderStateBySrc(Location location)
	{
		return (OrderState) osMap.get(location.getProvince());
	}// findOrderStateBySrc()
	
	
	/** 
	*	Find the OrderState with the given source Province. Returns null if
	*	no corresponding order was found.
	*/
	public final OrderState findOrderStateBySrc(Province src)
	{
		return (OrderState) osMap.get(src);
	}// findOrderStateBySrc()
	
	/**
	*	Returns 'true' if The Orderstate in question is a support order
	*	that is supporting a move against itself.		
	*	<ol>
	*		<li>Support order is supporting a Move
	*		<li>unit in supportedDest must be present
	*		<li>power of unit in supported dest == power of support order
	*	</ol>
	*
	*/
	public final boolean isSelfSupportedMove(OrderState os)
	{
		if(os.getOrder() instanceof Support)
		{
			Support support = (Support) os.getOrder();
			OrderState destOS = findOrderStateBySrc(support.getSupportedDest());
			
			if(	!support.isSupportingHold()
				&& destOS != null 
				&& destOS.getPower() == os.getPower() )
			{
				return true;
			}
		}
		
		return false;
	}// isSelfSupportedMove()
	
	/**
	*	Returns a list of substituted orders. This is a list of OrderStates.
	*	Note that all OrderStates in this list will be marked "illegal". Also
	*	note that this will <b>not</b> contain 'null' substitutions (e.g., 
	*	no order was specified, and a Hold order was automatically generated).
	*/
	public List getSubstitutedOrderStates()
	{
		return substOrders;
	}// getSubstitutedOrderStates()
	
	/** Add a Result to the result list */
	public final void addResult(Result result)
	{
		resultList.add(result);
	}// addResult()
	
	/** Add a BouncedResult to the result list */
	public final  void addBouncedResult(OrderState os, OrderState bouncer)
	{
		Log.println("Bounce Result added: ", os.getOrder(), "; by ", bouncer.getSourceProvince());
		BouncedResult br = new BouncedResult(os.getOrder());
		br.setBouncer(bouncer.getSourceProvince());
		resultList.add(br);
	}// addBouncedResult()
	
	/** Add a DislodgedResult to the result list */
	public final  void addDislodgedResult(OrderState os)
	{
		Log.println("Bounce Result added: ", os.getOrder(), "; from: ", os.getDislodger().getSourceProvince());
		DislodgedResult dr = new DislodgedResult(os.getOrder(), null);
		dr.setDislodger(os.getDislodger().getSourceProvince());
		resultList.add(dr);
	}// addBouncedResult()
	
	/** Add a Result to the result list */
	public final  void addResult(OrderState os, String message)
	{
		OrderResult ordResult = new OrderResult(os.getOrder(), message);
		resultList.add(ordResult);
	}// addResult()
	
	
	/** Add a Result to the result list */
	public final  void addResult(OrderState os, ResultType type, String message)
	{
		OrderResult ordResult = new OrderResult(os.getOrder(), type, message);
		resultList.add(ordResult);
	}// addResult()
	
	
	/**
	*	Checks that each Power's List of orders contains
	*	orders from that Power. If it does not, the 
	*	order is removed and a result is created.
	*	<p>
	*	If PowerOrderChecking is enabled, this should be
	*	performed before any adjudication phase.
	*/
	private void checkOrders()
	{
		Power[] powers = world.getMap().getPowers();
		
		for(int i=0; i<powers.length; i++)
		{
			Power power = powers[i];
			Iterator iter = turnState.getOrders(power).iterator();
			while(iter.hasNext())
			{
				Orderable order = (Orderable) iter.next();
				if(order.getPower() != power)
				{
					// remove order: it is invalid (and 
					// likely a bug or a cheat attempt)
					iter.remove();
					
					// create an informative result
					// {0} power, {1} order (formatted)
					String orderText = order.toFormattedString(orderFormat);
					addResult( new Result(Utils.getLocalString(
						STDADJ_POWER_ORDER_LIST_CORRUPT, power, orderText)) );
				}
			}
		}
	}// checkOrders()
	
	
	/** Adjudicates the Movement phase */
	private void adjudicateMoves()
	{
		// step 1: 
		// create orderstate mapping (province==>OrderState); REQUIRED by Adjudicator.java
		// also, this ensures that each location has only 1 order. If multiple orders for a location
		// exist, the last order is used.
		//
		// make sure that each location in the OrderState mapping has a corresponding unit,
		// If an order exists for a province without a unit, the order is deleted.
		List orderList = turnState.getAllOrders();
		// temporary list for holding orders; ASSUME that we won't be adding too many orders.
		ArrayList osList = new ArrayList(orderList.size());
		
		Iterator iter = orderList.iterator();
		while(iter.hasNext())
		{
			Order order = (Order) iter.next();
			OrderState os = new OrderState(order);
			Province province = os.getSourceProvince();
			
			// check that a unit exists for this order
			if( position.hasUnit(province) )
			{
				OrderState oldOS = (OrderState) osMap.get(province);
				if(oldOS != null)
				{
					addResult( new OrderResult(oldOS.getOrder(), 
									Utils.getLocalString(STDADJ_DUP_ORDER, os.getOrder())) );
					osList.remove(oldOS);	// we don't want duplicates in osList
				}
				
				osMap.put(province, os);
				osList.add(os);
			}
			else
			{
				addResult(new OrderResult(order, Utils.getLocalString(STDADJ_MV_NO_UNIT)));
			}
		}
		
		
		// step 2:
		// ensure that each unit has a corresponding OrderState. If a unit has no corresponding
		// OrderState, an OrderState with a Hold order is used. 
		Province[] unitList = position.getUnitProvinces();
		for(int i=0; i<unitList.length; i++)
		{
			Province province = unitList[i];
			if( !osMap.containsKey(province) )
			{
				Unit unit = position.getUnit(province);
				Hold hold = orderFactory.createHold(unit.getPower(), new Location(province, unit.getCoast()), unit.getType());
				OrderState os = new OrderState(hold);
				osList.add(os);
				osMap.put(os.getSourceProvince(), os);
				
				// create a result detailing our creation of a new order.
				addResult(new SubstitutedResult(
					null,
					hold,
					Utils.getLocalString(STDADJ_MV_NO_ORDER, province) ));
			}
		}
		
		// set OrderStates from our temporary list
		orderStates = (OrderState[]) osList.toArray(new OrderState[osList.size()]);
		
		// null out unitList & orderList -- we don't need them (and shouldn't use them)
		// (we'll get an NPE if we accidentaly use them later)
		unitList = null;
		orderList = null;
		osList = null;
		
		// integrity check: osList && osMap should have the same number of entries.
		assert (orderStates.length == osMap.size());
		
		// step 3: perform a complete validation of all orders
		ValidationOptions valOpts = new ValidationOptions();
		valOpts.setOption(ValidationOptions.KEY_GLOBAL_PARSING, ValidationOptions.VALUE_GLOBAL_PARSING_STRICT);
		
		for(int osIdx=0; osIdx<orderStates.length; osIdx++)
		{
			OrderState os = orderStates[osIdx];
			Order order = os.getOrder();
			
			// validate order (Strict, No warnings)
			try
			{
				order.validate(turnState, valOpts, ruleOpts);
			}
			catch(OrderWarning ow)
			{
				// just in case we didn't turn off all warnings; do nothing
			}
			catch(OrderException oe)
			{
				// If the order failed validation, create a VALIDATION_FAILURE result.
				// Then, replace the OrderState order with a Hold order. This prevents
				// the adjudicator from using (or even knowing about) the invalid order
				// 
				addResult(os, ResultType.VALIDATION_FAILURE, 
						Utils.getLocalString(STDADJ_MV_BAD, 
						oe.getMessage()));
						
				Hold hold = orderFactory.createHold(order.getPower(), order.getSource(), 
							order.getSourceUnitType());
				
				addResult(new SubstitutedResult(order, hold, null));
				os.setOrder(hold);
				
				// add old (subtituted) order to substituted order list 
				OrderState substOS = new OrderState(order);
				substOS.setLegal(false);
				substOrders.add(substOS);
			}
		}
		
		
		// step 4: calculate dependencies
		for(int osIdx=0; osIdx<orderStates.length; osIdx++)
		{
			OrderState os = orderStates[osIdx];
			Order order = os.getOrder();
			order.determineDependencies(this);
		}
		
		
		// step 5: Order verification / automatic failures
		/*
			What is an Automatic Failure?
			============================
				1) convoyed move with a partial or no convoy between src & dest
				2) mismatched supports
				3) mismatched convoy orders (no corresponding move order)
			
			Order Verification
			==================
				Orders that need to be matched against other orders, to determine
				validity, when all orders have been submitted.
				
				This is done via the Order.verify() method. By default, Order.verify()
				does nothing.
				
				Support.verify() checks for matched support
				Convoy.verify() checks for matched convoys
				Move.verify() checks for partial or no convoys between src & dest.
				
				
				
			Note that failure does NOT imply dislodged, in this case.
			
			This is, in fact, a type of validation, however, it cannot be performed
			until all players have submitted orders.				
			
			
		*/
		verifyOrders();
		
		
		// step 6: 
		// count the total number of moves and total number of non-move orders. 
		// NEVER count invalid moves.
		int totalMoves = 0;
		int totalNonMoves = 0;
		
		for(int osIdx=0; osIdx<orderStates.length; osIdx++)
		{
			OrderState os = orderStates[osIdx];
			
			if(os.getOrder() instanceof Move)
			{
				totalMoves++;
			}
			else
			{
				totalNonMoves++;
			}
		}
		
		if(Log.isLogging())
		{
			Log.println("order counts:");
			Log.println("      moves to evaluate: ",totalMoves);
			Log.println("  non-moves to evaluate: ",totalNonMoves);
		}
		
		
		// if we have no move orders, we have nothing to evaluate
		// and we cannot have any dislodged units.
		boolean areAnyUnitsDislodged = false;
		if(totalMoves > 0)
		{
			// Step 7: 
			// evaluate all orders, until evaluation is complete OR 
			// until we cannot break paradoxes any more (!)
			boolean evaluationComplete = false;
			while(!evaluationComplete)
			{
				evaluationComplete = evaluateOrders(totalMoves, totalNonMoves);
				if(!evaluationComplete)
				{
					evaluationComplete = !canBreakParadox();
				}
			}
			
			
			// Step 8:
			// a) convert 'maybe' dislodged to 'yes' disloged
			// b) create 'dislodged' result
			// c) any 'dislodged' with 'uncertain' evaluation ==> failure / dislodged
			// d) if power isn't active (e.g., Italy in a 6-player game), and 
			//    its unit is dislodged, it will be automatically disbanded (later)
			// e) set flag indicating if any units are dislodged
			for(int osIdx=0; osIdx<orderStates.length; osIdx++)
			{
				OrderState os = orderStates[osIdx];
				
				// convert maybe->certain
				if(os.getDislodgedState() == Tristate.MAYBE)
				{
					Log.println("dislodged: maybe -> yes: ", os.getOrder()); 
					
					os.setDislodgedState(Tristate.YES);
					
					if(os.getEvalState() == Tristate.UNCERTAIN)
					{
						os.setEvalState(Tristate.FAILURE);
						//addResult(os, ResultType.FAILURE, null);
						addDislodgedResult(os);
					}
					else if(os.getEvalState() == Tristate.FAILURE)
					{
						// we were dislodged, probably by another unit, not the
						// unit that caused the failure (see bug #952038)
						// So, we need to create a dislodged result, in addition
						// to setting the DislodgedState flag.
						// 
						addDislodgedResult(os);
					}
				}
				
				// ensure all dislodged units have a dislodged result (needed to process 
				// Order.evalute() methods can create dislodged results if they are 
				//    certain a unit was dislodged.
				if(os.getDislodgedState() == Tristate.YES)
				{
					// inactive powers will have their units disbanded, during step 12
					// (creating the next turnstate).
					if(os.getPower().isActive())
					{
						areAnyUnitsDislodged = true;
					}
				}
			}
		}
		
		// Step 9: 
		// ensure that any 'uncertain' orders are converted to success, and 
		// have a success result. Also any other order that was evaluated 
		// successfully gets a success result.
		// 
		// CHANGE [5/03]: dislodged units cannot be successful
		for(int osIdx=0; osIdx<orderStates.length; osIdx++)
		{
			OrderState os = orderStates[osIdx];
			
			if(os.getEvalState() == Tristate.UNCERTAIN)
			{
				//Log.println("uncertain: uncertain -> yes: ", os.getOrder()+"; dislodged: ",os.getDislodgedState()); 
				if(os.getDislodgedState() == Tristate.YES)
				{
					os.setEvalState(Tristate.FAILURE);
					// we have already created a DISLODGED result, which is equivalent to failure.
				}
				else
				{
					os.setEvalState(Tristate.SUCCESS);
				}
			}
			
			if(os.getEvalState() == Tristate.SUCCESS)
			{
				addResult(os, ResultType.SUCCESS, null);
			}
		}
		
		// report statistics, if enabled
		if(statReporting)
		{
			iter = resultList.iterator();
			while(iter.hasNext())
			{
				Result r = (Result) iter.next();
				if(r instanceof BouncedResult)
				{
					Log.println("-- setting stats for: BouncedResult: ", r);
					BouncedResult br = (BouncedResult) r;
					
					OrderState defOS = findOrderStateBySrc(br.getBouncer());
					OrderState atkOS = findOrderStateBySrc(br.getOrder().getSource());
					
					Log.println("   bouncer: ", br.getBouncer());
					Log.println("   attacker: ", br.getOrder().getSource());
					
					br.setAttackStrength(atkOS.getAtkCertain());
					br.setDefenseStrength(defOS.getDefCertain());
				}
				else if(r instanceof DislodgedResult)
				{
					Log.println("-- setting stats for: DislodgedResult: ", r);
					DislodgedResult dr = (DislodgedResult) r;
					
					OrderState atkOS = findOrderStateBySrc(dr.getDislodger());
					OrderState defOS = findOrderStateBySrc(dr.getOrder().getSource());
					
					Log.println("   dislodger: ", dr.getDislodger());
					Log.println("   defender: ", dr.getOrder().getSource());
					
					dr.setAttackStrength(atkOS.getAtkCertain());
					dr.setDefenseStrength(defOS.getDefCertain());
				}
			}
		}
		
		
		// Step 11:
		// Determine the next phase. If there are no dislodged units, we can
		// skip past the retreat phase. We will do adjustment-phase checking later.
		Phase oldPhase = turnState.getPhase();
		Phase nextPhase = oldPhase.getNext();
		if(!areAnyUnitsDislodged && nextPhase.getPhaseType() == Phase.PhaseType.RETREAT)
		{
			addResult( new Result(Utils.getLocalString(STDADJ_SKIP_RETREAT)) );
			nextPhase = nextPhase.getNext();
		}
		
		
		// Step 12:
		// Create the next TurnState. This is derived from the current turnstate,
		// Position is cloned in such a way that all data except unit positions are cloned.
		//
		// all units that have moved, are moved. All units that have been dislodged, are dislodged.
		// any other units stay in the same place.
		Position nextPosition = null;
		nextPosition = position.cloneExceptUnits();
		
		nextTurnState = new TurnState(nextPhase);
		nextTurnState.setPosition(nextPosition);
		nextTurnState.setWorld(turnState.getWorld());
		
		// create units in the appropriate place.
		for(int osIdx=0; osIdx<orderStates.length; osIdx++)
		{
			OrderState os = orderStates[osIdx];
			Order order = os.getOrder();
			Province sourceProvince = os.getSourceProvince();
			
			// clone the old unit (from the old position)
			Unit newUnit = (Unit) position.getUnit(sourceProvince).clone();
			
			if(os.getDislodgedState() == Tristate.YES)
			{
				// unit remains in same location / coast, but in dislodged area
				// unless the unit is of an inactive power; then, it is deleted.
				//
				if(os.getPower().isActive())
				{
					nextPosition.setDislodgedUnit(sourceProvince, newUnit);
				}
				else 
				{
					// notify the power of what happened.
					// 
					addResult(new OrderResult(os.getOrder(),
						Utils.getLocalString(STDADJ_INACTIVE_POWER_DISLODGED)));
				}
			}
			else if( order instanceof Move 
					 && os.getEvalState() == Tristate.SUCCESS)
			{
				Location dest = ((Move)order).getDest();
				newUnit.setCoast( dest.getCoast() );
				nextPosition.setUnit(dest.getProvince(), newUnit);
				nextPosition.setLastOccupier(dest.getProvince(), newUnit.getPower());
			}
			else
			{
				// unit is in same place
				nextPosition.setUnit(sourceProvince, newUnit);
				nextPosition.setLastOccupier(sourceProvince, newUnit.getPower());
			}
		}
		
		// Step 12a:
		// Set supply center ownership, if we *were* in the FALL season.
		// this must occur before we can see if adjustment phase is to be skipped
		if(oldPhase.getSeasonType() == Phase.SeasonType.FALL)
		{
			setSCOwners();
		}
		
		
		// Step 12b: 
		// See if victory conditions have been met; if they have, there is no need
		// for any additional phases, nor do we care about dislodged units.
		Adjustment.AdjustmentInfoMap adjustmentMap = Adjustment.getAdjustmentInfo(nextTurnState, ruleOpts, world.getMap().getPowers());
		VictoryConditions vc = world.getVictoryConditions();
		if( vc.evaluate(this, adjustmentMap) )
		{
			// finish current turnstate
			turnState.setResolved(true);
			resultList.addAll(vc.getEvaluationResults());
			TimeResult completed = new TimeResult(STDADJ_COMPLETED);
			addResult(completed);
			
			// nextTurnState:
			nextTurnState.setEnded(true);
			nextTurnState.setResolved(true);
			
			List nextResults = nextTurnState.getResultList();
			nextResults.addAll(vc.getEvaluationResults());
			nextResults.add(completed);
			
			return;
		}
		
		// Step 12c:
		// If the next phase is an adjustment phase, and there are no adjustments to make,
		// then skip it.
		checkAdjustmentPhase();
		
		// Step 13:
		// in cases where there are dislodged units, but the dislodged units have
		// no valid retreats, they are destroyed, instead of just dislodged.
		//
		// if all dislodged units are destroyed, then the phase is advanced by one
		// to eliminate retreat phase (since all units were destroyed.)
		//
		if(areAnyUnitsDislodged)
		{
			RetreatChecker rc = new RetreatChecker(nextTurnState, resultList);
			boolean areAllDestroyed = true;
			
			final Province[] provinces = nextPosition.getProvinces();
			for(int i=0; i<provinces.length; i++)
			{
				Province prov = provinces[i];
				Unit unit = nextPosition.getDislodgedUnit(prov);
				if(unit != null)
				{
					if( rc.hasRetreats(new Location(prov, unit.getCoast())) )
					{
						areAllDestroyed = false;
					}
					else
					{
						// destroy the unit
						nextPosition.setDislodgedUnit(prov, null);
						
						// create unit destroyed message
						addResult(new Result(unit.getPower(), 
							Utils.getLocalString(STDADJ_MV_UNIT_DESTROYED, unit.getType().getFullName(), prov)));
					}
				}
			}
			
			if(areAllDestroyed)
			{
				// advance phase by 1. Inform players why.
				Phase p = nextTurnState.getPhase().getNext();
				nextTurnState.setPhase(p);
				addResult( new Result(null, Utils.getLocalString(STDADJ_MV_PHASE_ADV_ALL_DESTROYED)) );
			}
		}
		
		// Timestamp: Adjudication completed.
		turnState.setResolved(true);
		addResult(new TimeResult(STDADJ_COMPLETED));
	}// adjudicateMoves()
	
	
	/** 
	*	This may return null, if the game has ended and victory conditions have been met
	*	during the Adjustment phase. During the movement phase, if victory conditions 
	*	have been met, this will return the next TurnState.
	*/
	public TurnState getNextTurnState()
	{
		return nextTurnState;
	}// getNextTurnState()
	
	
	/**
	*	Calls order.evaluate() for each order
	*	performs multiple iterations, until all *move* orders are evaluated.
	*	when all move orders have been evaluated, returns 'true'.
	*	<p>
	*	HOWEVER, if a paradox is detected, 'false' is returned.
	*	<p>
	*	paradox detection:
	*	<ol>
	*		<li>must supply INITIAL # of move orders / non-move orders to this method</li>
	*		<li>each iteration:
	*			<ol>
	*				<li># of move orders evaluated is determined</li>
	*				<li># of non-move orders evaluated is determined</li>
	*				<li>if # of move orders AND # of non-move orders are 
	*					both NOT increasing, we have a paradox.</li>
	*			</ol>
	*		</li>
	*	</ol>
	*	'invalid' move orders are never ever counted!!
	*/
	private boolean evaluateOrders(final int totalMoveOrderCount, final int totalNonMoveOrderCount)
	{
		int lastNumMovesEvaluated = 0;
		int lastNumNonMovesEvaluated = 0;
		int iterations = 0;
		
		do {
			int nMovesEvaluated = 0;
			int nNonMovesEvaluated = 0;
			
			// for logging statistics only:
			iterations++;
			
			for(int osIdx=0; osIdx<orderStates.length; osIdx++)
			{
				OrderState os = orderStates[osIdx];

				// evaluate each order
				os.getOrder().evaluate(this);
				
				// determine how many orders are evaluated
				if( os.getEvalState() != Tristate.UNCERTAIN )
				{
					if( os.getOrder() instanceof Move )
					{
						nMovesEvaluated++;
					}
					else
					{
						nNonMovesEvaluated++;
					}
				}
			}
			
			// check for paradox
			// NOTE: if totalMoveOrderCount == 0, we cannot have a paradox.
			// 
			if( totalMoveOrderCount > 0
				&& nMovesEvaluated <= lastNumMovesEvaluated
				&& nNonMovesEvaluated <= lastNumNonMovesEvaluated )
			{
				if(Log.isLogging())
				{
					Log.println("**** PARADOX ****");
					Log.println(" 	nMovesEvaluated = ",nMovesEvaluated);
					Log.println(" 	lastNumMovesEvaluated = ",lastNumMovesEvaluated);
					Log.println(" 	nNonMovesEvaluated = ",nNonMovesEvaluated);
					Log.println(" 	lastNumNonMovesEvaluated = ",lastNumNonMovesEvaluated);
				}
				
				return false;
			}
			
			// print iteration statistics
			if(Log.isLogging())
			{
				Log.println("-------- iteration statistics --------");
				Log.println("    iteration: ",iterations);
				Log.println("       orders: "+nNonMovesEvaluated+" of "+totalNonMoveOrderCount+" (non-move) evaluated");
				Log.println("  move orders: "+nMovesEvaluated+" of "+totalMoveOrderCount+" evaluated");
				Log.println("--------------------------------------");
			}
			
			// set last evaluated, so next iteration can be compared.
			lastNumMovesEvaluated = nMovesEvaluated;
			lastNumNonMovesEvaluated = nNonMovesEvaluated;
			
		} while(lastNumMovesEvaluated < totalMoveOrderCount);
		
		return true;
	}// evaluateOrders()
	
	
	/**
	*	If an unresolved paradox was detected, this returns true. This is 
	*	mostly intended for debugging.
	*/
	public boolean isUnresolvedParadox()
	{
		return isUnRezParadox;
	}// isUnresolvedParadox()
	
	
	/**
	*	Returns true if we have a method to attempt to break a paradox.
	*	returns false if we have no further paradox-breaking ideas.
	*	<p>
	*	This method must keep track of (through the class) which 
	*	methods to break paradoxes have already been tried.
	*	<p>
	*	The algorithm, currently, is simple. First, we look for 
	*	circular movement paradoxes, and attempt to re-resolve.
	*	<p>
	*	If paradox(es) remain, we attempt to apply the Szykman rule
	*	to break the paradox, until we succeed or still have a paradox.
	*	<p>
	*	If a paradox remain even after multiple applications of the
	*	Syzkman rule, we return <code>false</code> and add a result 
	*	indicating we have an unresolved paradox.
	*
	*
	*/
	private boolean canBreakParadox()
	{
		paradoxBreakAttempt++;
		assert (paradoxBreakAttempt >= 1);
		
		// prevent infinite loop.....
		if(syzkmanAppliedCount > 10)
		{
			addResult( new Result(null, 
					Utils.getLocalString(STDADJ_MV_UNRESOLVED_PARADOX, new Integer(paradoxBreakAttempt))) );
			
			if(Log.isLogging())
			{
				Log.println("paradox: order status:");
				Log.println("======================");
				
				for(int osIdx=0; osIdx<orderStates.length; osIdx++)
				{
					OrderState os = orderStates[osIdx];
					Log.println("  > ", os.getOrder(), " ", os.getEvalState());
				}
				
				Log.println("======================");
			}// if(logging)
			
			isUnRezParadox = true;
			return false;
		}
		
		
		if(paradoxBreakAttempt == 1)
		{
			breakCircularParadox();
			return true;
		}
		else
		{
			// try szykman
			syzkmanAppliedCount++;
			breakParadoxSzykman();
			return true;
		}
	}// canBreakParadox()
	
	
	/**
	*	Attempts to break a Circular Movement (this includes swaps)
	*	paradoxes. Note that this will break any chain of circular 
	*	movements, where n >= 2.
	*/
	private void breakCircularParadox()
	{
		final int nCircular = markCircularMoves();
		Log.println(":: circular chains found: ",nCircular);
		
		if(nCircular > 0)
		{
			for(int osIdx=0; osIdx<orderStates.length; osIdx++)
			{
				OrderState os = orderStates[osIdx];
				if(	os.getEvalState() == Tristate.UNCERTAIN 
					&& os.isCircular() )
				{
					os.setEvalState(Tristate.SUCCESS);
				}
			}
		}
	}// breakCircularParadox()
	
	
	/**
	*	Implements the Szykman Rule to break paradoxes.
	*	<p>
	*	"If a situation arises in which an army's convoy order results in a paradoxical adjudication, 
	*	the moves of all involved convoying armies fail and have no effect on the place where they 
	*	were ordered to convoy." This rule was proposed by Simon Szykman in a discussion with 
	*	Manus Hand in the Diplomatic Pouch Zine (1999, Fall Retreat). (DATC, section III)
	*	<p>
	*	Algorithm:
	*	<ol>
	*		<li>find all move orders, that are convoyed, and have an Uncertain resolve state.
	*		<li>if this move order has any unresolved convoy orders, move fails.
	*	</ol>
	*	<p>
	*	<pre>
	*	BUG WARNING: perhaps we should just cause moves to fail for those moves which have the	
	*				 fewest # of unresolved convoy orders; then re-evaluate, and attempt to
	*				 break paradox again if it occurs again (using next fewest # of unresolved
	*				 convoy orders).
	*	</pre>
	*/
	private void breakParadoxSzykman()
	{
		addResult(new Result(null, Utils.getLocalString(STDADJ_MV_SZYKMAN_NOTICE)));
		Log.println("breakParadoxSzykman(): entered");
		
		for(int osIdx=0; osIdx<orderStates.length; osIdx++)
		{
			OrderState os = orderStates[osIdx];
			if(	os.getEvalState() == Tristate.UNCERTAIN 
				&& os.getOrder() instanceof Move )
			{
				Move move = (Move) os.getOrder();
				
				if(move.isConvoying())
				{
					Log.println("  checking move: ", move);
					
					Iterator it = getConvoyList(move).iterator();
					while(it.hasNext())
					{
						OrderState itos = (OrderState) it.next();
						
						Log.println("    convoy: ",itos.getOrder(),"  evalstate:",itos.getEvalState());  
						if(itos.getEvalState() == Tristate.UNCERTAIN)
						{
							Log.println("    *** Syzkman rule applied to this move!!!");
							os.setEvalState(Tristate.FAILURE);
							addResult(os, ResultType.FAILURE, Utils.getLocalString(STDADJ_MV_SZYKMAN_MOVE_FAILED));
							break;
						}
					}
				}
			}
		}
		Log.println("breakParadoxSzykman(): exit");
	}// breakParadoxSzykman()
	
	
	/** 
	*	Verifies orders in a loop. Order verification can have dependencies,
	*	but extreme caution should be taken when implementing Order.verify()
	*	so that dependencies are minimized.
	*
	*/
	protected void verifyOrders()
	{
		Log.println("verifying orders...");
		int nRemainingToVerify = orderStates.length;
		int nLastVerified = 1;	// reset in while() loop
		
		while(nRemainingToVerify > 0 && nLastVerified > 0)
		{
			nLastVerified = 0;
			nRemainingToVerify = orderStates.length;
			
			for(int osIdx=0; osIdx<orderStates.length; osIdx++)
			{
				final OrderState os = orderStates[osIdx];
				if(!os.isVerified())
				{
					os.getOrder().verify(this);
					if(os.isVerified())
					{
						nRemainingToVerify--;
						nLastVerified++;
					}
				}
				else
				{
					// because the verify() method could call the verify() method
					// of other orders
					nRemainingToVerify--;
				}
			}
		}
		
		// detect condition where all orders did not verify.
		// this is an error.
		if(nRemainingToVerify > 0)
		{
			Log.println("ERROR: StdAdjudicator: incomplete verification.");
			Log.println("   Orders remaining to verify: ", nRemainingToVerify);
			Log.println("   Orders last verified: ", nLastVerified);
			
			throw new IllegalStateException("Verification Error");
		}
	}// verifyOrders()
	
	
	
	
	/** Adjudicates the Retreat phase */
	private void adjudicateRetreats()
	{
		// step 1: 
		// create orderstate mapping (province==>OrderState); REQUIRED by Adjudicator.java
		// ensure that each dislodged unit has one, and only one, order.
		// 
		// during the retreat phase, we are only concerned with dislodged units.
		List orderList = turnState.getAllOrders();
		ArrayList osList = new ArrayList(orderList.size());
		
		Iterator iter = orderList.iterator();
		while(iter.hasNext())
		{
			Order order = (Order) iter.next();
			OrderState os = new OrderState(order);
			Province province = os.getSourceProvince();
			
			// check that a unit exists for this order
			if( position.hasDislodgedUnit(province) )
			{
				OrderState oldOS = (OrderState) osMap.get(province);
				if(oldOS != null)
				{
					addResult(new OrderResult(oldOS.getOrder(),
									Utils.getLocalString(STDADJ_DUP_ORDER, os.getOrder())));
					osList.remove(oldOS);	// we don't want duplicates in osList
				}
				
				osMap.put(province, os);
				osList.add(os);
			}
			else
			{
				addResult(new OrderResult(order, Utils.getLocalString(STDADJ_RET_BAD_UNIT)));
			}
		}		
		
		
		// step 2: 
		// ensure that each unit now has a corresponding OrderState. If a unit has no corresponding
		// OrderState, an OrderState with a Disband order is used. 
		Province[] dislodgedUnitProvs = position.getDislodgedUnitProvinces();
		for(int i=0; i<dislodgedUnitProvs.length; i++)
		{
			Province province = dislodgedUnitProvs[i];
			if( !osMap.containsKey(province) )
			{
				Unit unit = position.getDislodgedUnit(province);
				Disband disband = orderFactory.createDisband(unit.getPower(), new Location(province, unit.getCoast()), unit.getType());
				OrderState os = new OrderState(disband);
				addResult(new Result(unit.getPower(), 
						Utils.getLocalString(STDADJ_RET_NO_ORDER, province)));
				
				osList.add(os);
				osMap.put(os.getSourceProvince(), os);
			}
		}
		
		// set OrderStates from our temporary list
		orderStates = (OrderState[]) osList.toArray(new OrderState[osList.size()]);
		
		// null out unitList & orderList -- we don't need them (and shouldn't use them)
		// (we'll get an NPE if we use them later)
		dislodgedUnitProvs = null;
		orderList = null;
		osList = null;
		
		// integrity check: osList && osMap should have the same number of entries.
		assert (orderStates.length == osMap.size());
		
		// step 3: perform a complete validation of all orders
		// use the most strict validation options
		ValidationOptions valOpts = new ValidationOptions();
		valOpts.setOption(ValidationOptions.KEY_GLOBAL_PARSING, ValidationOptions.VALUE_GLOBAL_PARSING_STRICT);
		
		for(int osIdx=0; osIdx<orderStates.length; osIdx++)
		{
			OrderState os = orderStates[osIdx];
			Order order = os.getOrder();
			
			try
			{
				order.validate(turnState, valOpts, ruleOpts);
			}
			catch(OrderWarning ow)
			{
				// just in case we didn't turn off all warnings; do nothing
			}
			catch(OrderException oe)
			{
				// all illegal orders are changed to Disband orders
				addResult(os, ResultType.VALIDATION_FAILURE, 
							Utils.getLocalString(STDADJ_RET_VAL_FAIL, oe.getMessage()));
				os.setOrder(orderFactory.createDisband(order.getPower(), order.getSource(), order.getSourceUnitType()));
			}
		}
		
		
		// step 4: calculate dependencies
		for(int osIdx=0; osIdx<orderStates.length; osIdx++)
		{
			OrderState os = orderStates[osIdx];
			Order order = os.getOrder();
			order.determineDependencies(this);
		}
		
		
		// step 5: Order verification / automatic failures
		verifyOrders();
		
		// step 6: 
		// count the total number of moves (retreats) and total number of non-move orders. 
		// NEVER count invalid moves.
		int totalMoves = 0;
		int totalNonMoves = 0;
		
		for(int osIdx=0; osIdx<orderStates.length; osIdx++)
		{
			OrderState os = orderStates[osIdx];
			
			if(os.getOrder() instanceof Move)
			{
				totalMoves++;
			}
			else
			{
				totalNonMoves++;
			}
		}
		
		if(Log.isLogging())
		{
			Log.println("order counts:");
			Log.println("      moves to evaluate: ",totalMoves);
			Log.println("  non-moves to evaluate: ",totalNonMoves);
		}
		
		// evaluate the orders in a loop. There should NOT be a paradox here.
		// 
		if( !evaluateOrders(totalMoves, totalNonMoves) )
		{
			throw new IllegalStateException("ERROR: retreat paradox detected");
		}
		
		
		
		// Step 7:
		// a) Create SUCCESS and FAILURE results
		for(int osIdx=0; osIdx<orderStates.length; osIdx++)
		{
			OrderState os = orderStates[osIdx];
			
			if(os.getEvalState() == Tristate.SUCCESS)
			{
				addResult(os, ResultType.SUCCESS, null);
			}
			else
			{
				addResult(os, ResultType.FAILURE, null);
			}
		}
		
		
		// Step 9:
		// Create the next TurnState. This is derived from the current turnstate,
		// with the Map derived from the current Map.
		// Dislodged units in the current position are not cloned into nextPosition
		// 
		// All non-dislodged units remain in the same place.
		// All dislodged units with failed orders or successful disband orders are eliminated.
		// All dislodged units that retreat successfully, are moved, and the supply-center
		// 		ownership changes if appropriate.
		//
		Position nextPosition = null;
		nextPosition = position.cloneExceptDislodged();
		nextTurnState = new TurnState( turnState.getPhase().getNext() );
		nextTurnState.setPosition(nextPosition);
		nextTurnState.setWorld(turnState.getWorld());
		
		// create units in the appropriate places
		for(int osIdx=0; osIdx<orderStates.length; osIdx++)
		{
			OrderState os = orderStates[osIdx];
			Order order = os.getOrder();
			Province sourceProvince = os.getSourceProvince();
			
			// clone the old unit (from the old position)
			Unit newUnit = (Unit) position.getDislodgedUnit(sourceProvince).clone();
			
			if( order instanceof Retreat
				&& os.getEvalState() == Tristate.SUCCESS )
			{
				Location dest = ((Move)order).getDest();
				Province destProvince = dest.getProvince();
				newUnit.setCoast( dest.getCoast() );
				nextPosition.setUnit(destProvince, newUnit);
				nextPosition.setLastOccupier(destProvince, newUnit.getPower());
				
				Log.println("  moved: unit from ", os.getSourceProvince(), " to ", destProvince);
			}
		}
		
		// Step 10:
		// Set supply center ownership, if we are in the FALL season.
		if(turnState.getPhase().getSeasonType() == Phase.SeasonType.FALL)
		{
			setSCOwners();
		}
		
		
		// Step 11:
		// Determine if next phase is an Adjustmeent phase. If so, determine
		// if we can skip it.
		checkAdjustmentPhase();
		
		// Timestamp: Adjudication completed.
		turnState.setResolved(true);
		addResult(new TimeResult(STDADJ_COMPLETED));
	}// adjudicateRetreats()
	
	
	/**
	*
	*	Determine adjustments.
	*	<p>
	*	This is done differently than adjudicating retreat or movement phases.
	*	<p>
	*	We assume the following:
	*	<ol>
	*		<li>Build/Remove orders, if valid, are always successful
	*		<li>Build/Remove orders are never verified() or have dependencies 
	*	</ol>
	*
	*
	*/
	private void adjudicateAdjustment()
	{
		// Step 1: get adjustment information
		Power[] powers = world.getMap().getPowers();
		Adjustment.AdjustmentInfoMap adjustmentMap = Adjustment.getAdjustmentInfo(turnState, ruleOpts, powers);
		
		// Step 2:
		// determine if any victory conditions have been met. If so, the adjustment phase
		// is aborted, and the game will end.
		VictoryConditions vc = turnState.getWorld().getVictoryConditions();
		if( vc.evaluate(this, adjustmentMap) )
		{
			// getNextTurnState() will return null; thus game has ended.
			turnState.setEnded(true);
			turnState.setResolved(true);
			resultList.addAll(vc.getEvaluationResults());
			addResult(new TimeResult(STDADJ_COMPLETED));
			return;
		}
		
		
		// step 3
		// match orders to actions. If there are extra valid build or remove orders, 
		// THEY ARE IGNORED. Thus, if a power can only build 2 units, only the first
		// 2 build orders (valid) are accepted. If a power must remove 2 units, 
		// only the first 2 remove orders are accepted.
		//
		// if more orders are detected, a 'failure' message will be created for that order.
		// we do not create an orderstate for such an order--it is just ignored.
		// step 1: 
		// create orderstate mapping (province==>OrderState); REQUIRED by Adjudicator.java
		// ensure that each dislodged unit has one, and only one, order.
		// 
		// We also validate here. We do not add invalid orders. This makes handling 'too many'
		// builds easier.
		//
		// use the most strict validation options
		ValidationOptions valOpts = new ValidationOptions();
		valOpts.setOption(ValidationOptions.KEY_GLOBAL_PARSING, ValidationOptions.VALUE_GLOBAL_PARSING_STRICT);
		
		ArrayList osList = new ArrayList(32);
		
		for(int i=0; i<powers.length; i++)
		{
			Power power = powers[i];
			Adjustment.AdjustmentInfo ai = (Adjustment.AdjustmentInfo) adjustmentMap.get(power);
			int orderCount = 0;
			final int adjAmount = ai.getAdjustmentAmount();
			
			List orders = turnState.getOrders(power);
			Iterator iter = orders.iterator();
			while(iter.hasNext())
			{
				Order order = (Order) iter.next();
				
				if(order instanceof Remove && adjAmount > 0)
				{
					addResult(new OrderResult(order, Utils.getLocalString(STDADJ_ADJ_IGNORED_MUST_BUILD)));
				}
				else if(order instanceof Build && adjAmount < 0)
				{
					addResult(new OrderResult(order, Utils.getLocalString(STDADJ_ADJ_IGNORED_MUST_REMOVE)));
				}
				else if(adjAmount == 0)
				{
					addResult(new OrderResult(order, Utils.getLocalString(STDADJ_ADJ_IGNORED_NO_CHANGE)));
				}
				else if( orderCount >= Math.abs(adjAmount) )
				{
					addResult(new OrderResult(order, Utils.getLocalString(STDADJ_ADJ_IGNORED_TOO_MANY)));
				}
				else
				{
					try
					{
						order.validate(turnState, valOpts, ruleOpts);
						
						// we only add legal orders, that haven't *already* been added
						if(osMap.get(order.getSource().getProvince()) == null)
						{
							OrderState os = new OrderState(order);
							osMap.put(os.getSourceProvince(), os);
							osList.add(os);
							orderCount++;
						}
						else
						{
							// duplicate or duplicate for space; we already have 
							// a valid order.
							addResult(new OrderResult(order, ResultType.FAILURE, 
									Utils.getLocalString(STDADJ_ADJ_IGNORED_DUPLICATE, 
									order.getSource().getProvince())));
						}
					}
					catch(OrderWarning ow)
					{
						// just in case we didn't turn off all warnings; do nothing
					}
					catch(OrderException oe)
					{
						addResult(new OrderResult(order, ResultType.VALIDATION_FAILURE, 
								Utils.getLocalString(STDADJ_ADJ_IGNORED_INVALID, oe.getMessage())));
					}
				}
			}// while(orders-for-power)
			
			// it is legal for a power to not use all the build orders, but if that occurs,
			// a result indicating that some builds were unused is created
			if(ai.getAdjustmentAmount() > 0 && orderCount < ai.getAdjustmentAmount())
			{
				addResult( 	new Result(powers[i],
							Utils.getLocalString(STDADJ_ADJ_BUILDS_UNUSED, new Integer(adjAmount - orderCount))) );
			}
			
			// While builds are optional (they may be waived), removes are not.
			// If a power has not issued enough remove orders, or remove orders were invalid,
			// correct remove orders must be created.
			// 
			// if a unit is already set to be removed, we will look for the next unit to remove.
			// clear the list when done
			int ordersToMake = adjAmount + orderCount;
			if(ordersToMake < 0)
			{
				addResult(new Result(powers[i], Utils.getLocalString(STDADJ_ADJ_TOO_FEW_DISBANDS)));
				createRemoveOrders(osList, powers[i], Math.abs(ordersToMake));
			}
		}// for(power)
		
		// set OrderStates from our temporary list
		orderStates = (OrderState[]) osList.toArray(new OrderState[osList.size()]);
		osList = null;	// prevent accidental re-use
		
		assert(osMap.size() == orderStates.length);
		
		// step 4: calculate dependencies
		// NOTE: while no orders currently use this, it's here for future use (thus a variant
		// could subclass Build or Remove but not have to subclass StdAdjudicator)
		for(int osIdx=0; osIdx<orderStates.length; osIdx++)
		{
			OrderState os = orderStates[osIdx];
			Order order = os.getOrder();
			order.determineDependencies(this);
		}
		
		
		// step 5: Order verification / automatic failures
		// NOTE: while no orders currently use this, it's here for future use (thus a variant
		// could subclass Build or Remove but not have to subclass StdAdjudicator)
		//
		// WARNING: this doesn't handle dependent-verifications, as verifyOrders() does.
		//
		for(int osIdx=0; osIdx<orderStates.length; osIdx++)
		{
			OrderState os = orderStates[osIdx];
			Order order = os.getOrder();
			order.verify(this);
			
			// remove orders that failed verification.
			// this is unique to Adjustment adjudication
			// WARNING: this is a low-performance process [array resized]; if this happens with 
			// any frequency, reconsider approach
			if(os.getEvalState() == Tristate.FAILURE)
			{
				osMap.remove(os);
				
				// safe... can't use an index...
				List list = Arrays.asList(orderStates);
				list.remove(os);
				orderStates = (OrderState[]) list.toArray(new OrderState[list.size()]);
			}
		}
		
		// step 6
		// single-pass adjudication (order evaluation)
		for(int osIdx=0; osIdx<orderStates.length; osIdx++)
		{
			OrderState os = orderStates[osIdx];
			os.getOrder().evaluate(this);
			
			// internal check against error; failure = failed single-pass decision making.
			assert (os.getEvalState() != Tristate.UNCERTAIN);
		}
		
		
		// step 8
		// create success results for all successful orders
		// only invalid orders will have failed.
		for(int osIdx=0; osIdx<orderStates.length; osIdx++)
		{
			OrderState os = orderStates[osIdx];
			
			if(os.getEvalState() == Tristate.SUCCESS)
			{
				addResult(os, ResultType.SUCCESS, null);
			}
			else
			{
				addResult(os, ResultType.FAILURE, null);
			}
		}
		
		
		// step 9
		// determine the next phase [always a move phase; do not skip]
		Phase oldPhase = turnState.getPhase();
		Phase nextPhase = oldPhase.getNext();
		
		// Step 10:
		// Create the next TurnState. This is derived from the current turnstate,
		// with the Map derived from the current Map. Dislodged units are not 
		// cloned (although, there should not be any dislodged units)
		//
		Position nextPosition = null;
		nextPosition = position.cloneExceptDislodged();
		
		nextTurnState = new TurnState(nextPhase);
		nextTurnState.setPosition(nextPosition);
		nextTurnState.setWorld(turnState.getWorld());
		
		// remove units that are disbanded, and build units that are to be built.
		for(int osIdx=0; osIdx<orderStates.length; osIdx++)
		{
			OrderState os = orderStates[osIdx];
			Order order = os.getOrder();
			Province sourceProvince = os.getSourceProvince();
			
			if(os.getEvalState() == Tristate.SUCCESS)
			{
				if(order instanceof Build)
				{
					Unit unit = new Unit(order.getPower(), order.getSourceUnitType());
					unit.setCoast(order.getSource().getCoast());
					nextPosition.setUnit(sourceProvince, unit);
					nextPosition.setLastOccupier(sourceProvince, unit.getPower());
				}
				else if(order instanceof Remove)
				{
					nextPosition.setUnit(sourceProvince, null);
				}
			}
		}
		
		
		// step 11: check if any powers have been eliminated; if so, set
		// the flags on the new map
		// NOTE: if a power is eliminated, supplyCentersOwned == 0. We can use
		// the AdjustmentInfo obtained from the beginning, since that will not
		// have changed since step 1.
		powers = world.getMap().getPowers();
		for(int i=0; i<powers.length; i++)
		{
			// get adjustment information
			Adjustment.AdjustmentInfo ai = (Adjustment.AdjustmentInfo) adjustmentMap.get(powers[i]);
			
			// check for player elimination
			if(ai.getSupplyCenterCount() == 0)
			{
				nextPosition.setEliminated(powers[i], true);
				addResult(new Result(powers[i], Utils.getLocalString(STDADJ_ADJ_ELIMINATED, powers[i].getName())));
			}
		}
		
		// Timestamp: Adjudication completed.
		turnState.setResolved(true);
		addResult(new TimeResult(STDADJ_COMPLETED));
	}// adjudicateAdjustment()
	
	
	/**
		Creates ordersToMake remove orders, using DPTG algorithm.
		If some units are set to already be removed (they will have
		OrderStates in the powerOrders List), another unit will be
		assigned.
		<p>
		Algorithm for determining which unit to Remove:
		<ul>	
			<li>for each unit without an existing Remove order, find shortest distance to a home supply center
				counted as per DPTG (all areas counted as one move). For build-in-any supply center 
				variants, all owned supply centers are counted as home supply centers.
			<li>reverse-sort from longest->shortest
			<li>iterate through sorted length list:
				<ul>
					<li>if no ties, order unit with longest distance to disband.
					<li>if ties, break tie as follows:
				</ul>	
					<ul>
						<li>Fleets before Armies
						<li>provinces in alphabetical order, by full name, after case conversion.
					</ul>	
		</ul>
		These orders are added directly to osList and osMap
		<p>
		This is not a high-performance method.....
	*/
	private void createRemoveOrders(List osList, Power power, int ordersToMake)
	{
		Path path = new Path(position);
		
		// find home supply centers for power
		// this depends upon the rule settings: if it's not VALUE_BUILDS_HOME_ONLY (typical)
		// then *all* owned supply centers are considered.
		Province[] homeSupplyCenters = null;
		RuleOptions.OptionValue buildOpt = ruleOpts.getOptionValue(RuleOptions.OPTION_BUILDS);
		if(buildOpt == RuleOptions.VALUE_BUILDS_HOME_ONLY)
		{
			homeSupplyCenters = position.getHomeSupplyCenters(power);
		}
		else
		{
			homeSupplyCenters = position.getOwnedSupplyCenters(power);
		}
		
		assert (homeSupplyCenters != null);
		
		for(int i=0; i<ordersToMake; i++)
		{
			LinkedList ties = new LinkedList();
			int maxDist = 0;
			
			Province[] provinces = position.getProvinces();
			for(int provIdx=0; provIdx<provinces.length; provIdx++)
			{
				Province province = provinces[provIdx];
				Unit unit = position.getUnit(province);
				
				if(unit != null)
				{
					OrderState os = findOrderStateBySrc(province);
					if(os == null)
					{
						if( unit.getPower().equals(power) )
						{
							int hsDist = 9999;
							for(int hsIdx=0; hsIdx<homeSupplyCenters.length; hsIdx++)
							{
								final int d = path.getMinDistance(province, homeSupplyCenters[hsIdx]);
								hsDist = (d < hsDist) ? d : hsDist;
							}
							
							if(hsDist > maxDist)
							{
								ties.clear();
								ties.add(province);
								maxDist = hsDist;
							}
							else if(hsDist == maxDist)
							{
								ties.add(province);
							}
						}
					}
				}
			}
			
			if(ties.size() == 0)
			{
				addResult(new Result(power, Utils.getLocalString(STDADJ_ADJ_NO_MORE_DISBANDS)));
				return;	// exit if no more units!!
			}
			else 
			{
				// complex case, DPTG compliant.
				//
				// first, sort the list alphabetically by full name
				// (that's why Province implements Comparable)
				Collections.sort( ties );
				
				// now, extract the first Fleet we find; if none found, extract the first
				// unit in the list. The first unit extracted will be alphabetically first;
				// the first fleet extracted will similarly be first alphabetically.
				// if there are only 2 units, it doesn't matter.
				boolean foundFleet = false;
				Iterator tieIter = ties.iterator();
				while(tieIter.hasNext() && !foundFleet)
				{
					Province province = (Province) tieIter.next();
					Unit unit = position.getUnit(province);
					if(unit.getType().equals(Unit.Type.FLEET))
					{
						foundFleet = true;
						createDisbandOrder(osList, province);
					}
				}
				
				if(!foundFleet)
				{
					createDisbandOrder(osList, (Province) ties.getFirst() );
				}
			}
		}
	}// createRemoveOrders()
	
	
	/** Creates a valid Disband order; adds to internal hashmap and given order list. */
	private void createDisbandOrder(List osList, Province province)
	{
		Unit unit = position.getUnit(province);
		Remove remove = orderFactory.createRemove(unit.getPower(), new Location(province, unit.getCoast()), unit.getType());
		OrderState os = new OrderState(remove);
		osMap.put(province, os);
		osList.add(os);
		addResult(new Result(unit.getPower(), 
					Utils.getLocalString(STDADJ_ADJ_DISBAND_ORDER, unit.getType().getFullName(), province)));
	}// createDisbandOrder()
	
	

	/**
	*	Supply Center Ownership Scanner
	*	<p>
	*	NOTE: this does NOT check SEASON.
	*	<p>
	*	This should be run at the end of the FALL season (movement and retreat) typically.
	*	Any unit (except a Wing unit) in a supply center effectively controls that supply center. If the supply
	*	center has changed hands, (from any power, including none, to another), the SCOwnerChanged
	*	flag of TurnState is also set. 
	*	<p>
	*	This should be run AFTER all orders have been adjudicated; it uses nextTurnState <b>not</b>
	*	TurnState.
	*/
	private void setSCOwners()
	{
		// remember, we're using the adjudicated position!
		Position nextPosition = nextTurnState.getPosition();
		Province[] provinces = nextPosition.getProvinces();
		for(int i=0; i<provinces.length; i++)
		{
			Province province = provinces[i];
			
			if(province != null && province.hasSupplyCenter())
			{
				Unit unit = nextPosition.getUnit(province);
				if(unit != null)
				{
					// nextPosition still contains old ownership information
					Power oldOwner = nextPosition.getSupplyCenterOwner(province);	
					Power newOwner = unit.getPower();
					
					// change if ownership change, and not a wing unit
					if(oldOwner != newOwner && unit.getType() != Unit.Type.WING)
					{
						// now we set the new ownership information!
						nextPosition.setSupplyCenterOwner(province, unit.getPower());
						// set owner-changed flag in TurnState [req'd for certain victory conditions]
						nextTurnState.setSCOwnerChanged(true);
					}
				}
			}
			
		}
	}// setSCOwners()
	
	
	/**
	*	Checks 'nextTurnState' to determine if it is an Adjustment phase.
	*	If it is, see if there are adjustments to make. If yes, add results for 
	*	each power telling what adjustments to make. If no, add result indicating
	*	adjustment phase was skipped, and advance the phase by one.
	*
	*/
	private void checkAdjustmentPhase()
	{
		if(nextTurnState.getPhase().getPhaseType() == Phase.PhaseType.ADJUSTMENT)
		{
			boolean canSkipAdjustment = true;
			Object[] args = new Object[1];
			
			Power[] powers = world.getMap().getPowers();
			for(int i=0; i<powers.length; i++)
			{
				Adjustment.AdjustmentInfo ai = Adjustment.getAdjustmentInfo(nextTurnState, ruleOpts, powers[i]);
				final int adjAmount = ai.getAdjustmentAmount();
				if( adjAmount != 0 )
				{
					// can't skip adjustment phase
					canSkipAdjustment = false;
					
					// write adjustment results
					// NOTE: it's difficult to believe, but 1/3 of the time (more, before
					// the patterns we cached as statics) of checkAdjustmentPhase() was spent
					// in MessageFormat. (MFRemove/MFBuild). 
					// 
					// using String.valueOf() instead of new Integer() results in a MASSIVE
					// speed improvment.
					if( adjAmount < 0 )
					{
						args[0] = String.valueOf( -adjAmount );	// 'abs'
						addResult(	new Result(powers[i], MFRemove.format(args)) );
					}
					else if( adjAmount > 0 )
					{
						args[0] = String.valueOf(adjAmount);
						addResult(	new Result(powers[i], MFBuild.format(args)) );
					}
					else
					{
						addResult( new Result(powers[i], Utils.getLocalString(STDADJ_PREADJ_TONEITHER)) );
					}
				}
			}
			
			if(canSkipAdjustment)
			{
				addResult( new Result(Utils.getLocalString(STDADJ_SKIP_ADJUSTMENT)) );
				
				// we RE-set the phase in nextTurnState.
				Phase p = nextTurnState.getPhase().getNext();
				nextTurnState.setPhase(p);
			}
		}
	}// checkAdjustmentPhase()
	
	
	/**
	*	Find all OrderStates with Move orders to the given destination.
	*	<p>
	*	The returned List is guaranteed to be filled with Move orders only
	*
	*/
	private List findMovesTo(Province moveDest)
	{
		ArrayList list = new ArrayList(6);
		
		for(int i=0; i<orderStates.length; i++)
		{
			OrderState os = orderStates[i];
			Order order = os.getOrder();
			if( order instanceof Move
				&& moveDest == ((Move) order).getDest().getProvince() )
			{
				list.add(order);
			}
		}
		
		return null;
	}// findMovesTo()
	
	
	/** Find the OrderState corresponding to the given Move order */
	private OrderState findMove(Province src, Province dest)
	{
		for(int i=0; i<orderStates.length; i++)
		{
			OrderState os = orderStates[i];
			Order order = os.getOrder();
			if( order instanceof Move
				&& order.getSource().isProvinceEqual(src)
				&& ((Move) order).getDest().isProvinceEqual(dest) )
			{
				return os;
			}
		}
		
		return null;
	}// findMove()
	
	/** Find the OrderState for a Move originating from the given Province */
	private OrderState findMoveFrom(Province src)
	{
		for(int i=0; i<orderStates.length; i++)
		{
			OrderState os = orderStates[i];
			Order order = os.getOrder();
			if(	order instanceof Move
				&& order.getSource().isProvinceEqual(src) )
			{
				return os;
			}
		}
		
		return null;
	}// findMoveFrom()	
	
	/**
	*	Returns a list of all orderStates for this power.
	*
	*/
	private List findOrderStatesForPower(Power power)
	{
		ArrayList list = new ArrayList(32);
		
		for(int i=0; i<orderStates.length; i++)
		{
			OrderState os = orderStates[i];
			if(os.getPower() == power)
			{
				list.add(os);
			}
		}
		
		return list;
	}// findOrderStatesForPower()
	
	
	
	/** 
	*	Returns a List of all OrderStates representing Convoy 
	*	orders for the given Move order
	*
	*/
	private List getConvoyList(Move move)
	{
		ArrayList list = new ArrayList(8);
		
		for(int i=0; i<orderStates.length; i++)
		{
			OrderState os = orderStates[i];
			if(os.getOrder() instanceof Convoy)
			{
				Convoy convoy = (Convoy) os.getOrder();
				if( convoy.getConvoySrc().isProvinceEqual(move.getSource())
					&& convoy.getConvoyDest().isProvinceEqual(move.getDest()) )
				{
					list.add(os);
				}
			}
		}
		
		return list;
	}// getConvoyList()
	
	/**
	*	Given an unresolved move A-B, check if it is in a 'string' of 
	*	unresolved moves. If so, set the isCircular() flag on them.
	*	<p>
	*	if we have found ANY circular moves, return true.
	*	note: a circular move is defined as:
	*	<ul>
	*		<li>a chain of moves >= 3
	*		<li>a chain of moves >= 2, where at least one
	*			of the moves is convoyed (isByConvoy())
	*	</ul>
	*	<p>
	*	returns # of circular *chains* that have been marked
	*		>=0
	*		
	*	<p>	
	*	TODO: 	this code is sortof hackish; should be simplified; we should 
	*			do a findMoveFrom() to match only within movesToCheck, and if
	*			a circle is found, move should be removed from movesToCheck list.
	*/
	private int markCircularMoves()
	{
		List movesToCheck = new LinkedList();
		LinkedList chain = new LinkedList();	// we'll be using this as a Stack; it will be cleared if chain is not complete.
		boolean isChainCircular = false;		// 'true' if chain contains a list of circular moves.
		int chainCount = 0;
		
		// step 1: get list of moves to check. Note that circular moves can only include
		// valid, UNCERTAIN moves, that haven't already been marked as circular
		for(int i=0; i<orderStates.length; i++)
		{
			OrderState os = orderStates[i];
			if(	os.getEvalState() == Tristate.UNCERTAIN 
				&& !os.isCircular() 
				&& os.getOrder() instanceof Move )
			{
				movesToCheck.add(os);
			}
		}
		
		
		// step 2: check each move in list for cicularity chains. If multiple chains
		// exist, we will find them in a single pass
		Iterator iter = movesToCheck.iterator();
		while(iter.hasNext())
		{
			chain.clear();
			isChainCircular = false;
			
			OrderState moveOS = (OrderState) iter.next();
			if( !moveOS.isCircular() )
			{
				Move firstMove = (Move) moveOS.getOrder();
				
				chain.addLast(moveOS);
				OrderState nextMoveOS = findMoveFrom(firstMove.getDest().getProvince());
				
				while(nextMoveOS != null && !isChainCircular)
				{
					if( !nextMoveOS.isCircular() )
					{
						chain.addLast(nextMoveOS);
						
						Move nextMove = (Move) nextMoveOS.getOrder();
						
						if( nextMove.getDest().isProvinceEqual(firstMove.getSource()) )
						{
							// we've found the move that completes the chain.
							isChainCircular = true;
						}
						
						nextMoveOS = findMoveFrom(nextMove.getDest().getProvince());
					}
					else
					{
						break;	// can't have intersecting circles!
					}
				}
				
				
				if(isChainCircular)
				{
					if(chain.size() > 2)
					{
						// double-check: only chains of >= 3 moves; prevents head-to-head moves from being
						// flagged as circular, in the event of some sort of ajudicator error.
						chainCount++;
						
						// all moves in chain are part of a circle (last move -> first move)
						// set 'isCircular()' flags
						Iterator chainIter = chain.iterator();
						while(chainIter.hasNext())
						{
							OrderState os = (OrderState) chainIter.next();
							os.setCircular(true);
						}
					}
					else if(chain.size() == 2)
					{
						// head-to-head moves (swaps) where one or both units are convoyed
						// are legitimate, however. 
						boolean isSwap = false;
						
						Iterator chainIter = chain.iterator();
						while(chainIter.hasNext())
						{
							OrderState os = (OrderState) chainIter.next();
							Move move = (Move) os.getOrder();
							if(move.isConvoying())
							{
								isSwap = true;
							}
						}
						
						if(isSwap)
						{
							chainCount++;
							
							chainIter = chain.iterator();
							while(chainIter.hasNext())
							{
								OrderState os = (OrderState) chainIter.next();
								os.setCircular(true);
							}
						}
					}
				}
			}
			
		}// while(iter)
		
		return chainCount;
	}// markCircularMoves()
	
	
}// class StandardAdjudicator

