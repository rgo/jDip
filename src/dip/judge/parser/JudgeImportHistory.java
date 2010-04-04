//
//	@(#)JudgeImport.java	1.00	6/2002
//
//	Copyright 2002 Zachary DelProposto. All rights reserved.
//	Use is subject to license terms.
//
//
//	This program is free software; you can redistribute it and/or modify
//	it under the terms of the GNU General Public License as published by
//	the Free Software Foundation; either version 2 of the License, or
//	(at your option) any later version.
//
//	This program is distributed in the hope that it will be useful,
//	but WITHOUT ANY WARRANTY; without even the implied warranty of
//	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//	GNU General Public License for more details.
//
//	You should have received a copy of the GNU General Public License
//	along with this program; if not, write to the Free Software
//	Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
//	Or from http://www.gnu.org/
//
package dip.judge.parser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import dip.judge.parser.TurnParser.Turn;
import dip.misc.Log;
import dip.misc.Utils;
import dip.order.Build;
import dip.order.Disband;
import dip.order.Move;
import dip.order.NJudgeOrderParser;
import dip.order.NJudgeOrderParser.NJudgeOrder;
import dip.order.Order;
import dip.order.OrderException;
import dip.order.OrderFactory;
import dip.order.Orderable;
import dip.order.Remove;
import dip.order.ValidationOptions;
import dip.order.result.DislodgedResult;
import dip.order.result.SubstitutedResult;
import dip.order.result.OrderResult;
import dip.order.result.Result;
import dip.process.Adjustment;
import dip.world.Location;
import dip.world.Phase;
import dip.world.Position;
import dip.world.Power;
import dip.world.Province;
import dip.world.RuleOptions;
import dip.world.TurnState;
import dip.world.Unit;
import dip.world.VictoryConditions;
import dip.world.World;
/**
*
*	Processes an entire game history to create a world. 
*	<p>
*	TODO:
*		<br>positioning units with orders that failed parsing (e.g., a move to Switzerland (swi))
*
*
*/
final class JudgeImportHistory
{
	// constants
	private static final String STDADJ_MV_UNIT_DESTROYED = "STDADJ_MV_UNIT_DESTROYED";
	
	private static final String JIH_BAD_POSITION  		= "JP.import.badposition";
	private static final String JIH_NO_MOVEMENT_PHASE  	= "JP.history.nomovement";
	private static final String JIH_ORDER_PARSE_FAILURE = "JP.history.badorder";
	private static final String JIH_UNKNOWN_RESULT  	= "JP.history.unknownresult";
	private static final String JIH_NO_DISLODGED_MATCH  = "JP.history.dislodgedmatchfail";
	private static final String JIH_INVALID_RETREAT  	= "JP.history.badretreat";
	private static final String JIH_BAD_LAST_PHASE  	= "JP.history.badlastphase";
	
	// parsing parameters
	/**
	*	Regular expression for parsing what the Next phase is. This is only used to create the last
	*	(final) phase.<p>
	*	Capture groups: (1)Phase (2)Season (3)Year
	*/
	public static final String PARSE_REGEX = "(?i)the\\snext\\sphase\\s.*will\\sbe\\s(\\p{Alpha}+)\\sfor\\s(\\p{Alpha}+)\\sof\\s((\\p{Digit}+))";
	public static final String END_FOF_GAME = "(?i)the game is over";
	public static final String START_POSITIONS = "(?i)Subject:\\s\\p{Alpha}+:\\p{Alnum}+\\s-\\s(\\p{Alpha})(\\p{Digit}+)(\\p{Alpha})";
	
	
	// instance variables
	private final dip.world.Map map;
	private final OrderFactory orderFactory;
	private final World world;
	private JudgeParser jp = null;
	private Position oldPosition = null;
	private final ValidationOptions valOpts;
	private HSCInfo[] homeSCInfo = null;
	private boolean finalTurn = false;
	
	/** Create a JudgeImportHistory */
	protected JudgeImportHistory(OrderFactory orderFactory, World world, JudgeParser jp, Position oldPosition)
	throws IOException, PatternSyntaxException
	{
		this.orderFactory = orderFactory;
		this.world = world;
		this.jp = jp;
		this.oldPosition = oldPosition;
		this.map = world.getMap();
		
		// create a very strict validation object, loose seems to have some weird problems when importing.
		valOpts = new ValidationOptions();
		valOpts.setOption(ValidationOptions.KEY_GLOBAL_PARSING, ValidationOptions.VALUE_GLOBAL_PARSING_STRICT);
		
		processTurns();
	}// JudgeImportHistory()
	
	/** Create a JudgeImportHistory and process a single turn */
	protected JudgeImportHistory(OrderFactory orderFactory, World world, JudgeParser jp, Turn turn)
	throws IOException, PatternSyntaxException
	{
		this.orderFactory = orderFactory;
		this.world = world;
		this.jp = jp;
		this.oldPosition = world.getLastTurnState().getPosition();
		this.map = world.getMap();
		
		// create a very strict validation object, loose seems to have some weird problems when importing.
		valOpts = new ValidationOptions();
		valOpts.setOption(ValidationOptions.KEY_GLOBAL_PARSING, ValidationOptions.VALUE_GLOBAL_PARSING_STRICT);
		
		processSingleTurn(turn);
	}// JudgeImportHistory()
	
	/** Returns the World, with TurnStates & Positions added as appropriate. */
	protected World getWorld()
	{
		return world;
	}// getWorld()
	
	
	/** 
	*	Processes the Turn data, starting with the first Movement phase. An exception is
	*	thrown if no Movement phase exists. 
	*
	*/
	private void processTurns()
	throws IOException, PatternSyntaxException
	{
		// break data up into turns
		Turn[] turns = new TurnParser(jp.getText()).getTurns();
		//System.out.println("# of turns: "+turns.length);
		
		// find first movement phase, if any
		int firstMovePhase = -1;
		for(int i=0; i<turns.length; i++)
		{
			if(turns[i].getPhase() != null)
			{
				if(turns[i].getPhase().getPhaseType() == Phase.PhaseType.MOVEMENT)
				{
					firstMovePhase = i;
					break;
				}
			}
		}
		
		// If we couldn't find the first movement phase... perhaps the game is just starting
		if(firstMovePhase == -1)
		{
			// Try to use the text info to create the game at its starting positions
			try {
				createStartingPositions();
				// Don't do the rest of this method, it will all fail.
				return;
			} catch (IOException e){
				throw new IOException(Utils.getLocalString(JIH_NO_MOVEMENT_PHASE));
			}
		}
		
		//System.out.println("First move phase: "+firstMovePhase);	
		
		// get home supply center information from the oldPosition object
		// and store it in HSCInfo object array, so that it can be set during each successive 
		// turn.
		ArrayList hscList = new ArrayList(50);
		Province[] provinces = map.getProvinces();
		for(int i=0; i<provinces.length; i++)
		{
			Power power = oldPosition.getSupplyCenterHomePower(provinces[i]);
			if(power != null)
			{
				hscList.add(new HSCInfo(provinces[i], power));
			}
		}
		homeSCInfo = (HSCInfo[]) hscList.toArray(new HSCInfo[hscList.size()]);

		// process all but the final phase
		for(int i=firstMovePhase; i<turns.length-1; i++)
		{
			//System.out.println("processing turn: "+i+"; phase = "+turns[i].getPhase());
			if(i==0) {	procTurn(turns[i], null, null, false);	}
			else if(i==1) {	procTurn(turns[i], turns[i-1], null, false);	}
			else {	procTurn(turns[i], turns[i-1], turns[i-2], false);	}
		}
		// process the last turn once more, but as the final turn, to allow proper positioning.
		finalTurn = true;
		if(turns.length == 1){
			procTurn(turns[turns.length-1],null,null,true);
		} else if (turns.length == 2){
			procTurn(turns[turns.length-1],turns[turns.length-2],null,true);
		} else if (turns.length >= 3){
			procTurn(turns[turns.length-1], turns[turns.length-2], turns[turns.length-3], true);
		}
		
		Pattern endofgame = Pattern.compile(END_FOF_GAME);
		
		Matcher e = endofgame.matcher(turns[turns.length-1].getText());
		
		if(!e.find()){

			// create last (un-resolved) turnstate
			makeLastTurnState(turns[turns.length-1]);
			
			// reprocess the last turn, again, not as final, so it looks right for viewing.
			finalTurn = false;
			if(turns.length == 1){
				procTurn(turns[turns.length-1],null,null,false);
			} else if (turns.length == 2){
				procTurn(turns[turns.length-1],turns[turns.length-2],null,false);
			} else if (turns.length >= 3){
				procTurn(turns[turns.length-1], turns[turns.length-2], turns[turns.length-3], false);
			}
		} else {
			// The imported game has ended
			// Reprocess the last turn, again, not as final, so it looks right for viewing.
			finalTurn = false;
			procTurn(turns[turns.length-1], turns[turns.length-2], turns[turns.length-3], false);
			// Set the game as ended.
			TurnState ts = world.getTurnState(turns[turns.length-1].getPhase());
			VictoryConditions vc = world.getVictoryConditions();
			RuleOptions ruleOpts = world.getRuleOptions();
			Adjustment.AdjustmentInfoMap adjMap = Adjustment.getAdjustmentInfo(ts, ruleOpts, world.getMap().getPowers());
			vc.evaluate(ts,adjMap);
			List evalResults = ts.getResultList();
			evalResults.addAll(vc.getEvaluationResults());
			ts.setResultList(evalResults);
			ts.setEnded(true);
			ts.setResolved(true);
			world.setTurnState(ts);
		}
		
		// all phases have been processed; perform post-processing here.
	}// processTurns()
	
	/** 
	*	Processes a single turn. 
	*
	*/
	private void processSingleTurn(Turn turn)
	throws IOException, PatternSyntaxException
	{		
		// get home supply center information from the oldPosition object
		// and store it in HSCInfo object array, so that it can be set during each successive 
		// turn.
		ArrayList hscList = new ArrayList(50);
		Province[] provinces = map.getProvinces();
		for(int i=0; i<provinces.length; i++)
		{
			Power power = oldPosition.getSupplyCenterHomePower(provinces[i]);
			if(power != null)
			{
				hscList.add(new HSCInfo(provinces[i], power));
			}
		}
		homeSCInfo = (HSCInfo[]) hscList.toArray(new HSCInfo[hscList.size()]);

		// process the turn
		procTurn(turn, null, null, false);
		// save new turn state
		TurnState savedTS = world.getLastTurnState();
		
		// process the last turn once more, but as the final turn, to allow proper positioning.
		finalTurn = true;
		procTurn(turn, null, null, true);
		// create last (un-resolved) turnstate
		makeLastTurnState(turn);
			
		// inject the saved turnstate.
		world.setTurnState(savedTS);
		
		// TODO: do we have to check for victory conditions ?
		
	}// processSingleTurn()
	
	
	/** 
	 * Decides how to process the turn, based upon the phase information and past turns.
	 * This is not the best way to process the turns, especially the adjustment phase, 
	 * but it works.
	 */
	private void procTurn(Turn turn, Turn prevTurn, Turn thirdTurn, boolean positionPlacement)
	throws IOException
	{
		Phase phase = turn.getPhase();
		if(phase != null)
		{
			Phase.PhaseType phaseType = phase.getPhaseType();
			if(phaseType == Phase.PhaseType.MOVEMENT)
			{
				procMove(turn, positionPlacement);
			}
			else if(phaseType == Phase.PhaseType.RETREAT)
			{
					/*
					 * Set the proper positionPlacement value depending on if the turn being
					 * processed is the final turn. Set it back again when done. 
					 */
					if(!finalTurn)	{	procMove(prevTurn, !positionPlacement);	}
					else			{ 	procMove(prevTurn, positionPlacement);	}
					
					procRetreat(turn, positionPlacement);
					
					if(!finalTurn)	{	procMove(prevTurn, positionPlacement);	}
					else			{ 	procMove(prevTurn, !positionPlacement);	}
			}
			else if(phaseType == Phase.PhaseType.ADJUSTMENT)
			{
				Phase.PhaseType prevPhaseType = Phase.PhaseType.MOVEMENT; // dummy
				if (prevTurn != null) {
					Phase phase_p = prevTurn.getPhase();
					prevPhaseType = phase_p.getPhaseType();
				}
				/*
				 * Much the same as above, set the proper positionPlacement value depending
				 * on the PhaseType and if the turn being processed is the final turn.
				 * Set it back again when done. 
				 */
				if(prevPhaseType == Phase.PhaseType.MOVEMENT) {
					if(!finalTurn)	{	procMove(prevTurn, !positionPlacement);	}
					else			{ 	procMove(prevTurn, positionPlacement);	}
					
					procAdjust(turn, positionPlacement);
					
					if(!finalTurn)	{	procMove(prevTurn, positionPlacement);	}
					else			{ 	procMove(prevTurn, !positionPlacement);	}
						
				} else {
					
					if(!finalTurn) {	
						procMove(thirdTurn, !positionPlacement);	
						procRetreat(prevTurn, !positionPlacement);
					}
					else { 
						procMove(thirdTurn, positionPlacement);
						procRetreat(prevTurn, positionPlacement);
					}
					
					procAdjust(turn, positionPlacement);
					
					if(!finalTurn) {
						procRetreat(prevTurn, positionPlacement);	
						procMove(thirdTurn, positionPlacement);							
					}
					else { 
						procRetreat(prevTurn, !positionPlacement);
						procMove(thirdTurn, !positionPlacement);
					}
				}
			}
			else
			{
				throw new IllegalStateException("unknown phase type");
			}
		}
	}// procTurn()
	
	
	/** Creates a TurnState object with the correct Phase, Position, and World information, 
	*	including setting things such as home supply centers and what not. 
	*	<p>
	*	This method ensures that TurnState objects are properly (and consistently) initialized. 
	*/
	private TurnState makeTurnState(Turn turn)
	{
		// does the turnstate already exist?
		// it could, if we are importing orders into an already-existing game.
		// 
		Log.println("JIH::makeTurnState() ", turn.getPhase());
		
		TurnState ts = world.getTurnState(turn.getPhase());
		
		if(ts == null)
		{
			Log.println("  creating new turnstate.");
			// make new TurnState
			ts = new TurnState(turn.getPhase());
			ts.setWorld(world);
			ts.setPosition(new Position(world.getMap()));
			
			// note: we don't add the turnstate to the World object at this point (although we could), because
			// if a processing error occurs, we don't want a partial turnstate object in the World.
			
			// set Home Supply centers in position
			Position pos = ts.getPosition();
			for(int i=0; i<homeSCInfo.length; i++)
			{
				pos.setSupplyCenterHomePower(homeSCInfo[i].getProvince(), homeSCInfo[i].getPower());
			}
		}
		else
		{
			// likely importing orders to an existing game.
			// no changes to turnstate.
			Log.println("  using existing turnstate");
		}
		
		return ts;
	}// makeTurnState()
	
	
	
	/** Process a Movement phase turn */
	private void procMove(Turn turn, boolean positionPlacement)
	throws IOException
	{
		if (turn == null) return;
		// create TurnState
		TurnState ts = makeTurnState(turn);
		List results = ts.getResultList();
		
		Log.println("JIH::procMove(): ", ts.getPhase(), "; positionPlacement: ", String.valueOf(positionPlacement));
		
		// copy previous lastOccupier information into current turnstate.
		copyPreviousLastOccupierInfo(ts);
		
		// parse orders, and create orders for each unit
		final JudgeOrderParser jop = new JudgeOrderParser(map, orderFactory, turn.getText());
		final NJudgeOrder[] nJudgeOrders = jop.getNJudgeOrders();
		
		// get Position
		Position position = ts.getPosition();
		
		// create units from start position
		for(int i=0; i<nJudgeOrders.length; i++)
		{
			final NJudgeOrder njo = nJudgeOrders[i];
			
			final Orderable order = njo.getOrder();
			if(order == null)
			{
				Log.println("JIH::procMove(): Null order; njo: ", njo);
				throw new IOException("Internal error: null order in JudgeImportHistory::procMove()");
			}
			
			Location loc = order.getSource();
			final Unit.Type unitType = order.getSourceUnitType();
			final Power power = order.getPower();
			
			// validate location
			try
			{
				loc = loc.getValidated(unitType);
			}
			catch(OrderException e)
			{
				Log.println("ERROR: ", njo);
				Log.println("TURN: \n", turn.getText());
				throw new IOException(e.getMessage());
			}
			
			// create unit, and add to Position
			Unit unit = new Unit(order.getPower(), unitType);
			unit.setCoast(loc.getCoast());
			position.setUnit(loc.getProvince(), unit);
			position.setLastOccupier(loc.getProvince(), power);
			
			// if we found a Wing unit, make sure Wing units are enabled.
			checkAndEnableWings(unitType);
			
			// debug
			//System.out.println("  "+location+"; "+unit);
		}
		
		
		// now, validate all order objects from the parsed order
		// also create result objects
		// create positions from successful orders...
		// note that we only need to set the last occupier for changing (moving)
		// units, but we will do it for all units for consistency
		//
		{	
			// create orderMap, which maps powers to their respective order list
			Power[] powers = map.getPowers();
			
			Log.println("JIH::procMove():CREATING POWER->ORDER MAP");
			
			HashMap orderMap = new HashMap(powers.length);
			for(int i=0; i<powers.length; i++)
			{
				orderMap.put(powers[i], new LinkedList());
			}
			
			// process all orders
			final RuleOptions ruleOpts = world.getRuleOptions();
			
			for(int i=0; i<nJudgeOrders.length; i++)
			{
				final NJudgeOrder njo = nJudgeOrders[i];
				final Orderable order = njo.getOrder();
				
				// first try to validate under strict settings; if fail, try
				// to validate under loose settings.
				try
				{
					order.validate(ts, valOpts, ruleOpts);
					
					List list = (LinkedList) orderMap.get(order.getPower());
					list.add(order);
					
					results.addAll(njo.getResults());
					
					Log.println("  order ok: ", order);
				}
				catch(OrderException e)
				{
					Log.println("JIH::procMove():OrderException! using loose validation....");
					
					//Try loosening the validation object
					valOpts.setOption(ValidationOptions.KEY_GLOBAL_PARSING, ValidationOptions.VALUE_GLOBAL_PARSING_LOOSE);
					
					/* Try the order once more.
					 * nJudge accepts illegal moves as valid as long as the syntax is valid.
					 * (Perhaps a few other things as well). jDip should accept these as well,
					 * even if the move is illegal.
					 */
					try 
					{
						order.validate(ts, valOpts, ruleOpts);
						
						List list = (LinkedList) orderMap.get(order.getPower());
						list.add(order);
						
						results.addAll(njo.getResults());
						
						Log.println("  order ok: ", order);
					} 
					catch(OrderException e1)
					{
						// create a general result indicating failure if an order could not be validated.
						results.add( new Result(Utils.getLocalString(JIH_ORDER_PARSE_FAILURE, order, e.getMessage())));
						Log.println("JIH::procMove():OrderException (during validation): ", order, "; ", e1.getMessage());
						throw new IOException("Cannot validate order on second pass.\n"+e1.getMessage());
					}
					
					// Back to strict!
					valOpts.setOption(ValidationOptions.KEY_GLOBAL_PARSING, ValidationOptions.VALUE_GLOBAL_PARSING_STRICT);					 
				}
			}
			
			Log.println("JIH::procMove():ORDER PARSING COMPLETE");
			
			// clear all units (dislodged or not) from the board
			Province[] unitProv = position.getUnitProvinces();
			Province[] dislProv = position.getDislodgedUnitProvinces();
			for(int i=0;i<unitProv.length;i++)	{	position.setUnit(unitProv[i],null);	}
			for(int i=0;i<dislProv.length;i++)	{	position.setUnit(dislProv[i],null);	}

			// now that all orders are parsed, and all units are cleared, put
			// unit in the proper place.
			Iterator iter = results.iterator();
			while(iter.hasNext())
			{
				Result result = (Result) iter.next();
				if(result instanceof OrderResult)
				{
					OrderResult ordResult = (OrderResult) result;
					Orderable order = ordResult.getOrder();
										
					if(ordResult.getResultType() == OrderResult.ResultType.DISLODGED)
					{
						// dislodged orders create a unit in the source province, marked as dislodged, 
						// unless it was destroyed; if so, it will be destroyed later. Mark as dislodged for now.
						Unit unit = new Unit(order.getPower(), order.getSourceUnitType());
						/*
						 * Check for the positionPlacement flag, if not, we need to position the units
						 * in their source places for VIEWING. Otherwise the units need to be
						 * in their destination place for copying.
						 */
						if(!positionPlacement) {
							unit.setCoast(order.getSource().getCoast());
							position.setUnit(order.getSource().getProvince(), unit);
						} else {
							unit.setCoast(order.getSource().getCoast());
							position.setDislodgedUnit(order.getSource().getProvince(), unit);
						}					
					}
					else if( ordResult.getResultType() == OrderResult.ResultType.SUCCESS
							 && order instanceof Move )
					{
						// successful moves create a unit in the destination province
						Move move = (Move) order;
						Unit unit = new Unit(move.getPower(), move.getSourceUnitType());
						/*
						 * Check for the positionPlacement flag, if not, we need to position the units
						 * in their source places for VIEWING. Otherwise the units need to be
						 * in their destination place for copying.
						 */
						if(!positionPlacement) {
							unit.setCoast(move.getSource().getCoast());
							position.setUnit(move.getSource().getProvince(), unit);
							position.setLastOccupier(move.getSource().getProvince(), move.getPower());
						} else {
							unit.setCoast(move.getDest().getCoast());
							position.setUnit(move.getDest().getProvince(), unit);
							position.setLastOccupier(move.getDest().getProvince(), move.getPower());
						}
					}
					else
					{
						// all other orders create a non-dislodged unit in the source province
						Unit unit = new Unit(order.getPower(), order.getSourceUnitType());
						/*
						 * Only add a unit if there is not a unit currently there, this stops
						 * powers further down in alpha. order from overriding powers before
						 * them. Eg. England dislodged Germany will be overriding if this isn't here.
						 */
						if(!position.hasUnit(order.getSource().getProvince()))
						{
							unit.setCoast(order.getSource().getCoast());
							position.setUnit(order.getSource().getProvince(), unit);
							position.setLastOccupier(order.getSource().getProvince(), order.getPower());
						}
					}
				}
			}
			
			// set orders in turnstate
			for(int i=0; i<powers.length; i++)
			{
				ts.setOrders(powers[i], (LinkedList) orderMap.get(powers[i]));
			}
		}
		
		// process dislodged unit info, to determine retreat paths
		// correct dislodged results are created here, and the old dislodged 
		// results are removed
		if(!positionPlacement)
		{
			DislodgedParser dislodgedParser = new DislodgedParser(ts.getPhase(), turn.getText());
			makeDislodgedResults(ts.getPhase(), results, position, dislodgedParser.getDislodgedInfo(), false);
		}
		else
		{
			DislodgedParser dislodgedParser = new DislodgedParser(ts.getPhase(), turn.getText());
			makeDislodgedResults(ts.getPhase(), results, position, dislodgedParser.getDislodgedInfo(), true);
		}
		
		// process adjustment info ownership info (if any)
		//
		AdjustmentParser adjParser = new AdjustmentParser(map, turn.getText());
		procAdjustmentBlock(adjParser.getOwnership(), ts, position);
		
		// check for elimination
		position.setEliminationStatus(map.getPowers());
		
		// set adjudicated flag
		ts.setResolved(true);
		
		// add to world
		world.setTurnState(ts);
	}// procMove()
	
	
	
	/** Process a Retreat phase turn */
	private void procRetreat(Turn turn, boolean positionPlacement)
	throws IOException
	{
		if (turn == null) return;
		// create TurnState
		final TurnState ts = makeTurnState(turn);
		final Position position = ts.getPosition();
		final List results = ts.getResultList();
		final RuleOptions ruleOpts = world.getRuleOptions();
		
		Log.println("JIH::procRetreat(): ", ts.getPhase(), "; positionPlacement: ", String.valueOf(positionPlacement));
		
		// parse orders, and create orders for each unit
		JudgeOrderParser jop = new JudgeOrderParser(map, orderFactory, turn.getText());
		NJudgeOrder[] nJudgeOrders = jop.getNJudgeOrders();
		
		// Copy previous phase positions
		copyPreviousPositions(ts);
		
		// process retreat orders (either moves or disbands)
		// if order failed, it counts as a disband
		// generate results
		// create units for all successfull move (retreat) orders in destination province
		{	
			// create orderMap, which maps powers to their respective order list
			Power[] powers = map.getPowers();
			HashMap orderMap = new HashMap(powers.length);
			for(int i=0; i<powers.length; i++)
			{
				orderMap.put(powers[i], new LinkedList());
			}
			
			// validate all parsed orders
			for(int i=0; i<nJudgeOrders.length; i++)
			{
				final NJudgeOrder njo = nJudgeOrders[i];
				final Orderable order = njo.getOrder();
				if(order == null)
				{
					Log.println("JIH::procRetreat(): Null order; njo: ", njo);
					throw new IOException("Internal error: null order in JudgeImportHistory::procRetreat()");
				}
				
				// if we found a Wing unit, make sure Wing units are enabled.
				checkAndEnableWings(order.getSourceUnitType());
				
				try
				{
					order.validate(ts, valOpts, ruleOpts);
					
					List list = (LinkedList) orderMap.get(order.getPower());
					list.add(order);
					
					results.addAll(njo.getResults());
					
					Log.println("  order ok: ", order);
				}	
				catch(OrderException e)
				{
					results.add( new Result(Utils.getLocalString(JIH_ORDER_PARSE_FAILURE, order, e.getMessage())));
					Log.println("JIH::procMove():OrderException (during validation): ", order, "; ", e.getMessage());
					throw new IOException("Cannot validate retreat order.\n"+e.getMessage());
				}
			}
			
			// clear all dislodged units from board
			if(positionPlacement){
				Province[] dislProv = position.getDislodgedUnitProvinces();
				for(int i=0;i<dislProv.length;i++)	{	position.setDislodgedUnit(dislProv[i],null);	}
			}
			
			// now that all orders are parsed, and all units are cleared, put
			// unit in the proper place.
			//
			Iterator iter = results.iterator();
			while(iter.hasNext())
			{
				Result result = (Result) iter.next();
				if(result instanceof OrderResult)
				{
					OrderResult ordResult = (OrderResult) result;
					Orderable order = ordResult.getOrder();
					
					// successful moves create a unit in the destination province
					// unsuccessful moves OR disbands create no unit
					if(order instanceof Move)// && ordResult.getResultType() == OrderResult.ResultType.SUCCESS)
					{
						// success: unit retreat to destination
						Move move = (Move) order;
						
						Unit unit = new Unit(move.getPower(), move.getSourceUnitType());
						/*
						 * Check for the positionPlacement flag, if not, we need to position the units
						 * in their source places for VIEWING. Otherwise the units need to be
						 * in their destination place for copying.
						 */
						if(!positionPlacement) {
							unit.setCoast(move.getSource().getCoast());
							position.setDislodgedUnit(move.getSource().getProvince(), unit);
							position.setLastOccupier(move.getSource().getProvince(), move.getPower());
						} else {
							unit.setCoast(move.getDest().getCoast());
							position.setUnit(move.getDest().getProvince(), unit);
							position.setLastOccupier(move.getSource().getProvince(), move.getPower());
						}
					} else if (order instanceof Disband){
						/*
						 * Check for the positionPlacement flag, if not, we need to position the units
						 * in their source places for VIEWING. Otherwise the units should not be drawn.
						 */
						Unit unit = new Unit(order.getPower(), order.getSourceUnitType());
						if(!positionPlacement) {
							unit.setCoast(order.getSource().getCoast());
							position.setDislodgedUnit(order.getSource().getProvince(), unit);
						}
					}
				}
			}
			
			// set orders in turnstate
			for(int i=0; i<powers.length; i++)
			{
				ts.setOrders(powers[i], (LinkedList) orderMap.get(powers[i]));
			}
		}
		
		// process adjustment info ownership info (if any)
		AdjustmentParser adjParser = new AdjustmentParser(map, turn.getText());
		procAdjustmentBlock(adjParser.getOwnership(), ts, position);
		
		// check for elimination
		ts.getPosition().setEliminationStatus(map.getPowers());
		
		// set adjudicated flag
		ts.setResolved(true);
		
		// add to world
		world.setTurnState(ts);
	}// procRetreat()
	
	
	
	/** Process an Adjustment phase turn */
	private void procAdjust(Turn turn, boolean positionPlacement)
	throws IOException
	{
		if(turn == null) 
		{
			return;
		}
		
		// create TurnState
		final TurnState ts = makeTurnState(turn);
		final List results = ts.getResultList();
		final RuleOptions ruleOpts = world.getRuleOptions();
		
		Log.println("JIH::procAdjust(): ", ts.getPhase());
		
		// parse orders, and create orders for each unit
		final JudgeOrderParser jop = new JudgeOrderParser(map, orderFactory, turn.getText());
		final NJudgeOrder[] nJudgeOrders = jop.getNJudgeOrders();
		
		// Copy previous phase positions
		copyPreviousPositions(ts);
		
		// Copy previous SC info (we need proper ownership info before parsing orders)
		copyPreviousSCInfo(ts);
		
		// DEBUG: use Adjustment to check out WTF is going on
		/*
		System.out.println("dip.process.Adjustment.getAdjustmentInfo()");
		for(int i=0; i<map.getPowers().length; i++)
		{
			Power power = map.getPowers()[i];
			System.out.println("   for power: "+power+"; "+dip.process.Adjustment.getAdjustmentInfo(ts, power));
		}
		*/
		
		// process adjustment orders (either builds or removes)
		// create a unit, unless order failed
		{	
			// get Position
			final Position position = ts.getPosition();
			
			// create orderMap, which maps powers to their respective order list
			Power[] powers = map.getPowers();
			HashMap orderMap = new HashMap(powers.length);
			for(int i=0; i<powers.length; i++)
			{
				orderMap.put(powers[i], new LinkedList());
			}
			
			// parse all orders
			for(int i=0; i<nJudgeOrders.length; i++)
			{
				final NJudgeOrder njo = nJudgeOrders[i];
				final Orderable order = njo.getOrder();
				
				// all adjustment orders produced by NJudgeOrderParser should
				// have only 1 result
				// 
				if(njo.getResults().size() != 1)
				{
					throw new IOException("Internal error: JIH:procAdjustments(): "+
						"getResults() != 1");
				}
				
				final Result result = (Result) njo.getResults().get(0);
				
				// if result is a substituted result, the player defaulted,
				// and the Judge inserted a Disband order
				//
				final boolean isDefaulted = (result instanceof SubstitutedResult);
				
				if(order == null && !isDefaulted)
				{
					// orders may be null; if they are, that is because
					// it's a waive or unusable/pending order. These have 
					// results, but no associated order.
					results.addAll( njo.getResults() );
				}
				else
				{
					// NOTE: everything in this block should use newOrder,
					// not order, from here on!!
					Orderable newOrder = order;
					
					if(isDefaulted)
					{
						newOrder = ((SubstitutedResult) result).getSubstitutedOrder();
						assert (newOrder != null);
					}
					
					// if we found a Wing unit, make sure Wing units are enabled.
					checkAndEnableWings(newOrder.getSourceUnitType());
					
					try
					{
						newOrder.validate(ts, valOpts, ruleOpts);
						
						if(!isDefaulted)
						{
							List list = (LinkedList) orderMap.get(newOrder.getPower());
							list.add(newOrder);
						}
						
						results.addAll(njo.getResults());
						
						Log.println("  order ok: ", newOrder);
						
						// create or remove units
						// as far as I know, orders are always successful.
						//
						if(newOrder instanceof Build)
						{
							if(positionPlacement)
							{
								final Unit unit = new Unit(newOrder.getPower(), newOrder.getSourceUnitType());
								unit.setCoast(newOrder.getSource().getCoast());
								position.setUnit(newOrder.getSource().getProvince(), unit);
								position.setLastOccupier(newOrder.getSource().getProvince(), newOrder.getPower());
							}
						}
						else if(newOrder instanceof Remove)
						{
							if(positionPlacement)
							{
								position.setUnit(newOrder.getSource().getProvince(), null);
							}
						}
						else
						{
							throw new IllegalStateException("JIH::procAdjust(): type :"+newOrder+" not handled!");
						}
					}
					catch(OrderException e)
					{
						results.add( new Result(Utils.getLocalString(JIH_ORDER_PARSE_FAILURE, newOrder, e.getMessage())));
						
						Log.println("JIH::procAdjust():OrderException (during validation): ");
						Log.println("     phase: ", ts.getPhase());
						Log.println("     order: ", newOrder);
						Log.println("     error: ", e.getMessage());
						
						throw new IOException("Cannot validate adjustment order.\n"+e.getMessage());
					}
				}
			}
			
			// set orders in turnstate
			for(int i=0; i<powers.length; i++)
			{
				ts.setOrders(powers[i], (LinkedList) orderMap.get(powers[i]));
			}
		}
		
		
		// check for elimination
		ts.getPosition().setEliminationStatus(map.getPowers());
		
		// set adjudicated flag
		ts.setResolved(true);
		
		// Since this is the adjustment phase, check for supply center change. Required for VictoryConditions
		// Otherwise, problems can arise and the game will end after importing due to no SC change.
		if(!positionPlacement){
			TurnState previousTS = world.getPreviousTurnState(ts);
			while(previousTS.getPhase().getPhaseType() != Phase.PhaseType.MOVEMENT){
				previousTS = world.getPreviousTurnState(previousTS);
			}
			//System.out.println(previousTS.getPhase());
			Position oldPosition = previousTS.getPosition();
			Position position = ts.getPosition();
			Province[] provinces = position.getProvinces();
			for(int i=0; i<provinces.length; i++)
			{
			 Province province = provinces[i];
						 if(province != null && province.hasSupplyCenter())
			 {
				 Unit unit = position.getUnit(province);
				 if(unit != null)
				 {
					 // nextPosition still contains old ownership information
					 Power oldOwner = oldPosition.getSupplyCenterOwner(province);	
					 Power newOwner = unit.getPower();
					 //System.out.println(oldOwner + " VS " + newOwner);
		
					 // change if ownership change, and not a wing unit
					 if(oldOwner != newOwner && unit.getType() != Unit.Type.WING)
					 {
						 // set owner-changed flag in TurnState [req'd for certain victory conditions]
						 ts.setSCOwnerChanged(true);
					 }
				 }
			 }
			}
		}
		
		// add to world
		world.setTurnState(ts);
	}// procAdjust()
	
	
	/** 
	*	Clones all non-dislodged units from previous phase TurnState 
	*	and inserts them into the current turnstate.
	*	<p>
	*	We also copy non-dislodged units, unless the CURRENT turnstate is 
	* 	an Adjustment phase
	*/
	private void copyPreviousPositions(TurnState current)
	{
		// get previous turnstate
		TurnState previousTS = current.getWorld().getPreviousTurnState(current);
		final boolean isCopyDislodged = (current.getPhase().getPhaseType() != Phase.PhaseType.ADJUSTMENT);
		
		// get position info
		Position newPos = current.getPosition();
		Position oldPos = null;
		if(previousTS == null)
		{
			oldPos = oldPosition;
		} 
		else 
		{
			oldPos = previousTS.getPosition();
		}
		
		Log.println("copyPreviousPositions() from: ", oldPos);
		
		// clone!
		final Province[] provinces = map.getProvinces();
		for(int i=0; i<provinces.length; i++)
		{
			final Province p = provinces[i];
			Unit unit = oldPos.getUnit(p);
			if(unit != null)
			{
				Unit newUnit = (Unit) unit.clone();
				newPos.setUnit(p, newUnit);
				Log.println("  cloned unit from/into: ", p);
			}
			
			unit = oldPos.getDislodgedUnit(p);
			if(isCopyDislodged && unit != null)
			{
				Unit newUnit = (Unit) unit.clone();
				newPos.setDislodgedUnit(p, newUnit);
				Log.println("  cloned dislodged unit from/into: ", p);
			}
			
			// clone any lastOccupied info as well.
			newPos.setLastOccupier(p, oldPos.getLastOccupier(p));
		}
	}// copyPreviousPositions()
	
	
	/**
	*	Copies the previous TurnState (Position, really) home SC and SC info.
	*	<p>
	*	If no previous home supply center information is available (e.g., 
	*	initial turn), the information from the initial board setup is 
	*	used.
	*	<p>
	*	This method should only be used if no AdjustmentInfo block has
	*	been detected.
	*/
	private void copyPreviousSCInfo(TurnState current)
	{
		Log.println("copyPreviousSCInfo(): ", current.getPhase());
		
		// get previous position information (or initial, if previous not available)
		final TurnState previousTS = current.getWorld().getPreviousTurnState(current);
		Position prevPos = (previousTS == null) ? oldPosition : previousTS.getPosition();
		
		/*
		if(previousTS != null)
		{
			Log.println("  Copying *previous* SC ownership info from: ", previousTS.getPhase());
		}
		else
		{
			Log.println("  !! Copying *previous* SC ownership info from: oldPosition");
			Log.println("  world has the following Turnstates: ");
			Log.println("  ", current.getWorld().getPhaseSet());
		}
		*/
		
		// current position
		Position currentPos = current.getPosition();
		
		// copy!
		Province[] provinces = map.getProvinces();
		for(int i=0; i<provinces.length; i++)
		{
			Power power = prevPos.getSupplyCenterOwner(provinces[i]);
			if(power != null)
			{
				//System.out.println("  SC @ "+provinces[i]+", owned by "+power);
				currentPos.setSupplyCenterOwner(provinces[i], power);
				Log.println("  set SC: ", provinces[i], " owned by ", power);
			}
			power = prevPos.getSupplyCenterHomePower(provinces[i]);
			if(power != null)
			{
				currentPos.setSupplyCenterHomePower(provinces[i], power);
				Log.println("  set HSC: ", provinces[i], " owned by ", power);
			}
		}
	}// copyPreviousSCInfo()
	
	
	
	
	/** Copies the Previous turnstate's lastOccupier information only */
	private void copyPreviousLastOccupierInfo(TurnState current)
	{
		TurnState previousTS = current.getWorld().getPreviousTurnState(current);
		Position newPos = current.getPosition();
		Position oldPos = (previousTS == null) ? oldPosition : previousTS.getPosition();
		
		final Province[] provinces = map.getProvinces();
		for(int i=0; i<provinces.length; i++)
		{
			final Province p = provinces[i];
			
			// clone any lastOccupied info as well.
			newPos.setLastOccupier(p, oldPos.getLastOccupier(p));
		}
	}// copyPreviousLastOccupierInfo()
	
	
	/**
	*	Processes a block of adjustment info; this can occur during a 
	*	Move or Retreat phase. Only the Supply Center ownership is used;
	*	the adjustment values are ignored, since they can be computed
	*	based upon ownership information.
	*	<p>
	*	If no SC owner info exists, copyPreviousSCInfo() is used to 
	*	supply the appropriate information.
	*/
	private void procAdjustmentBlock(AdjustmentParser.OwnerInfo[] ownerInfo, TurnState ts, Position position)
	throws IOException
	{
		Log.println("procAdjustmentBlock(): ", ts.getPhase());
		if(ownerInfo.length == 0)
		{
			Log.println("   No adjustment block. Copying previous SC ownership info.");
			copyPreviousSCInfo(ts);
		}
		else
		{
			for(int i=0; i<ownerInfo.length; i++)
			{
				Power power = map.getPowerMatching(ownerInfo[i].getPowerName());
				if(power != null)
				{
					Log.println("   SC Owned by Power: ", power);
					String[] provNames = ownerInfo[i].getProvinces();
					for(int pi=0; pi<provNames.length; pi++)
					{
						Province province = map.getProvinceMatching(provNames[pi]);
						if(province == null)
						{
							throw new IOException("Unknown Province in SC Ownership block: "+provNames[pi]);
						}
						
						Log.println("       ", province);
						position.setSupplyCenterOwner(province, power);
					}
				}
				else
				{
					Log.println("  *** Unrecognized power: ", ownerInfo[i].getPowerName());
					throw new IOException("Unregognized power \""+ownerInfo[i].getPowerName()+"\" in Ownership block.");
				}
			}
		}
	}// procAdjustmentBlock()
	
	
	/** 
	*	Creates correct dislodged results (with retreat information) by matching	
	*	DislodgedInfo with the previously generated Dislodged result.
	*	<p>	
	*	Units with no retreat results are destroyed, and a message generated indicating so.
	*	<p>
	*	old Dislodged results are discarded.
	*/
	private void makeDislodgedResults(Phase phase, List results, Position position, 
		DislodgedParser.DislodgedInfo[] dislodgedInfo, boolean positionPlacement)
	throws IOException
	{
		ListIterator iter = results.listIterator();
		while(iter.hasNext())
		{
			Result result = (Result) iter.next();
			if(result instanceof OrderResult)
			{
				OrderResult orderResult = (OrderResult) result;
				if(OrderResult.ResultType.DISLODGED.equals(orderResult.getResultType()))
				{
					String[] retreatLocNames = null;
					for(int i=0; i<dislodgedInfo.length; i++)
					{
						// find the province for this dislodgedInfo source
						// remember, we use map.parseLocation() to auto-normalize coasts (see Coast.normalize())
						Location location = map.parseLocation(dislodgedInfo[i].getSourceName());
						if(orderResult.getOrder().getSource().isProvinceEqual(location))
						{
							retreatLocNames = dislodgedInfo[i].getRetreatLocationNames();
							break;
						}
					}
					
					// we didn't find a match!! note that, and don't delete old dislodged order
					if(retreatLocNames == null)
					{
						iter.add( new Result(Utils.getLocalString(JIH_NO_DISLODGED_MATCH, orderResult.getOrder())) );
						
						String message = "Could not match dislodged order: "+orderResult.getOrder()+"; phase: "+phase;
						Log.println(message);
						
						// we are more strict with our errors
						throw new IOException(message);
					}
					else
					{
						try
						{
							// create objects from retreat location names
							Location[] retreatLocations = new Location[retreatLocNames.length];
							for(int i=0; i<retreatLocNames.length; i++)
							{
								retreatLocations[i] = map.parseLocation(retreatLocNames[i]);
								retreatLocations[i] = retreatLocations[i].getValidated(orderResult.getOrder().getSourceUnitType());
							}
							
							// remove old dislodged result, replacing with the new dislodged result
							iter.set(new DislodgedResult(orderResult.getOrder(), retreatLocations));
							
							// if no retreat results, destroy unit
							if(retreatLocations.length == 0)
							{
								// destroy
								Province province = orderResult.getOrder().getSource().getProvince();
								Unit unit;
								
								/*
								 * Check for the positionPlacement flag. If so, go ahead and set the unit to the
								 * dislodged one. If not, the unit that is dislodged is not SHOWN as dislodged
								 * therefore get that one.
								 */
								if(positionPlacement) {	unit = position.getDislodgedUnit(province);	}
								else {	unit = position.getUnit(province);	}
								
								position.setDislodgedUnit(province, null);
								
								// System.out.println(" ** DESTROYING unit: "+unit+" at "+province);
								
								// send result
								iter.add(new Result(unit.getPower(), Utils.getLocalString(STDADJ_MV_UNIT_DESTROYED, unit.getType().getFullName(), province)));
							}
						}
						catch(OrderException e)
						{
							// couldn't validate!! 
							iter.add( new Result(Utils.getLocalString(JIH_INVALID_RETREAT, orderResult.getOrder())) );
							Log.println("JIH::makeDislodgedResults(): exception: ", orderResult.getOrder());
							throw new IOException(e.getMessage());
						}
					}
				}
			}
		}
	}// makeDislodgedResults()
	
	private void createStartingPositions() throws IOException{
		Phase phase = null;
		
		// determine the next phase by reading through the turn text.
		Pattern pattern = Pattern.compile(START_POSITIONS);
		Matcher m = pattern.matcher(jp.getText());
		
		if(m.find())
		{
			StringBuffer sb = new StringBuffer(64);
			sb.append(m.group(1));
			sb.append(' ');
			sb.append(m.group(2));
			sb.append(' ');
			sb.append(m.group(3));
			phase = Phase.parse(sb.toString());
		}
		
		if(phase == null)
		{
			throw new IOException(Utils.getLocalString(JIH_BAD_LAST_PHASE));
		}
		
		// Create the new turnstate
		TurnState ts = new TurnState(phase);
		ts.setWorld(world);
		ts.setPosition(new Position(world.getMap()));
		
		// set Home Supply centers in position
		Position pos = oldPosition;
		for(int i=0; i<oldPosition.getHomeSupplyCenters().length; i++)
		{
			pos.setSupplyCenterHomePower(oldPosition.getHomeSupplyCenters()[i], oldPosition.getSupplyCenterHomePower(oldPosition.getHomeSupplyCenters()[i]));
		}
		
		// Copy previous phase positions
		copyPreviousPositions(ts);
		
		// Copy previous SC info (we need proper ownership info before parsing orders)
		copyPreviousSCInfo(ts);
		
		// check for elimination
		ts.getPosition().setEliminationStatus(map.getPowers());
			
		// add to World
		world.setTurnState(ts);
	}// createStartingPositions()
	
	/** 
	*	Creates the last TurnState, which is always ready for adjudication. 
	*	<p>
	*	If parsing fails, no last turnstate will be created.
	*/	
	private void makeLastTurnState(Turn lastTurn)
	throws IOException
	{
		Phase phase = null;
		
		// determine the next phase by reading through the turn text.
		Pattern pattern = Pattern.compile(PARSE_REGEX);
		
		Matcher m = pattern.matcher(lastTurn.getText());
		
		if(m.find())
		{
			StringBuffer sb = new StringBuffer(64);
			sb.append(m.group(1));
			sb.append(' ');
			sb.append(m.group(2));
			sb.append(' ');
			sb.append(m.group(3));
			phase = Phase.parse(sb.toString());
		}
		
		if(phase == null)
		{
			throw new IOException(Utils.getLocalString(JIH_BAD_LAST_PHASE));
		}
		
		// Create the new turnstate
		TurnState ts = new TurnState(phase);
		ts.setWorld(world);
		ts.setPosition(new Position(world.getMap()));
		
		// set Home Supply centers in position
		Position pos = ts.getPosition();
		for(int i=0; i<homeSCInfo.length; i++)
		{
			pos.setSupplyCenterHomePower(homeSCInfo[i].getProvince(), homeSCInfo[i].getPower());
		}
		
		// Copy previous phase positions
		copyPreviousPositions(ts);
		
		// Copy previous SC info (we need proper ownership info before parsing orders)
		copyPreviousSCInfo(ts);
		
		// check for elimination
		ts.getPosition().setEliminationStatus(map.getPowers());
			
		// add to World
		world.setTurnState(ts);
	}// makeLastTurnState()
	
	
	/**
	*	If a WING unit is detected, make sure we have the WING option
	*	enabled; if it already is, do nothing.
	*/
	private void checkAndEnableWings(Unit.Type unitType)
	{
		if(Unit.Type.WING.equals(unitType))
		{
			RuleOptions ruleOpts = world.getRuleOptions();
			if(RuleOptions.VALUE_WINGS_DISABLED.equals(ruleOpts.getOptionValue(RuleOptions.OPTION_WINGS)))
			{
				ruleOpts.setOption(RuleOptions.OPTION_WINGS, RuleOptions.VALUE_WINGS_ENABLED);
				world.setRuleOptions(ruleOpts);
			}
		}
	}// enableWings()

	
	
	/** Home Supply Center information */
	private class HSCInfo
	{
		private Province province;
		private Power power;
		
		public HSCInfo(Province province, Power power) 
		{
			this.province = province;
			this.power = power;
		}// HSCInfo()
		
		public Province getProvince() 		{ return province; }
		public Power getPower() 			{ return power; }
	}// inner class HSCInfo
	
}// class JudgeImportHistory
