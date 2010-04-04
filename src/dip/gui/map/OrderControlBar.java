//
//  @(#)OrderControlBar.java	1.00	4/1/2002
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
package dip.gui.map;

import dip.gui.order.*;
import dip.misc.Utils;
import dip.misc.Log;
import dip.order.ValidationOptions;
import dip.process.Adjustment;
import dip.world.*;
import org.apache.batik.dom.events.DOMKeyEvent;
import org.w3c.dom.events.MouseEvent;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


/*
 *  WARNING: some versions of batik (CVS version > 1.5b4b)
 *  do not change cursors appropriately.
 */
/**
 *  ControlBar that displays Order functionality <p>
 *
 *  Mnemonics not used -- they interfere with menubar functionality.
 *
 */
public class OrderControlBar extends ViewControlBar
{
	// action commands: movement phase
	private final static String MODE_HOLD = "MODE_HOLD";
	private final static String MODE_MOVE = "MODE_MOVE";
	private final static String MODE_SUPPORT = "MODE_SUPPORT";
	private final static String MODE_CONVOY = "MODE_CONVOY";

	// action commands: retreat phase
	private final static String MODE_RETREAT = "MODE_RETREAT";
	private final static String MODE_DISBAND = "MODE_DISBAND";

	// action commands: adjustment phase
	private final static String MODE_BUILD_ARMY = "MODE_BUILD_ARMY";
	private final static String MODE_BUILD_FLEET = "MODE_BUILD_FLEET";
	private final static String MODE_BUILD_WING = "MODE_BUILD_WING";
	private final static String MODE_REMOVE = "MODE_REMOVE";
	private final static String MODE_WAIVE_BUILD = "MODE_WAIVE_BUILD";
	

	// cancel key (ESC) character code (char 27)
	private final static int KEY_CANCEL = java.awt.event.KeyEvent.VK_ESCAPE;

	// il8n
	private final static String[] GROUP_MOVEMENT_TEXT = Utils.getLocalStringArray( "OrdConBar.icons.movement" );
	private final static String[] GROUP_MOVEMENT_CMD = {MODE_HOLD, MODE_MOVE, MODE_SUPPORT, MODE_CONVOY};
	//public static String[] 	GROUP_MOVEMENT_MNEMONIC = Utils.getLocalStringArray("OrdConBar.mnemonic.movement");
	private final static int[] GROUP_MOVEMENT_CHARCODES = Utils.getLocalIntArray( "OrdConBar.keys.movement" );
	private final static String GROUP_MOVEMENT_DEFAULT_BUTTON = MODE_MOVE;

	private final static String[] GROUP_RETREAT_TEXT = Utils.getLocalStringArray( "OrdConBar.icons.retreat" );
	private final static String[] GROUP_RETREAT_CMD = {MODE_RETREAT, MODE_DISBAND};
	//public static String[] 	GROUP_RETREAT_MNEMONIC = Utils.getLocalStringArray("OrdConBar.mnemonic.retreat");
	private final static int[] GROUP_RETREAT_CHARCODES = Utils.getLocalIntArray( "OrdConBar.keys.retreat" );
	private final static String GROUP_RETREAT_DEFAULT_BUTTON = MODE_RETREAT;

	private final static String[] GROUP_ADJUSTMENT_TEXT = Utils.getLocalStringArray( "OrdConBar.icons.adjustment" );
	private final static String[] GROUP_ADJUSTMENT_CMD = {MODE_BUILD_ARMY, MODE_BUILD_FLEET, MODE_BUILD_WING, MODE_REMOVE, MODE_WAIVE_BUILD};
	//public static String[] 	GROUP_ADJUSTMENT_MNEMONIC = Utils.getLocalStringArray("OrdConBar.mnemonic.adjustment");
	private final static int[] GROUP_ADJUSTMENT_CHARCODES = Utils.getLocalIntArray( "OrdConBar.keys.adjustment" );
	private final static String GROUP_ADJUSTMENT_DEFAULT_BUTTON = MODE_BUILD_ARMY;

	// buttons
	private JToggleButton[] buttons = null;

	// instance variables
	private String currentAction = null;
	private String defaultAction = null;
	private int[] charMap = null;

	private GUIOrder.StateInfo stateInfo = null;
	private GUIOrderFactory guiOrderFactory = null;
	private GUIOrder currentOrder = null;
	private StringBuffer sb = null;
	// recycled stringbuffer
	private Location lastLoc = null;
	// last location we were over.

	// For dragging
	private Location dragLoc = null;
	private boolean inDrag = false;
	private Location dragSupportLoc = null;
	private GUIOrder tempOrder = null;
	
	// use explicit GUIMove?
	private final boolean useExplicitGUIMove; 

	/**
	 *  Any time the TurnState changes, a new OrderControlBar is created.
	 *
	 * @param  mp  MapPanel object
	 * @since
	 */
	public OrderControlBar( MapPanel mp )
	{
		super( mp );

		// init
		guiOrderFactory = mapPanel.getClientFrame().getGUIOrderFactory();
		sb = new StringBuffer( 80 );

		// Create GUIOrder StateInfo object
		stateInfo = new GUIOrder.StateInfo();
		stateInfo.setTurnState( mapPanel.getTurnState() );
		stateInfo.setRuleOptions( mapPanel.getWorld().getRuleOptions() );
		stateInfo.setValidationOptions( mapPanel.getClientFrame().getValidationOptions() );
		stateInfo.setClientFrame( mapPanel.getClientFrame() );
		
		// set adjustment info, if appropriate phase
		// add to StateInfo object
		if( stateInfo.getTurnState().getPhase().getPhaseType() == Phase.PhaseType.ADJUSTMENT )
		{
			Power[] powers = stateInfo.getTurnState().getWorld().getMap().getPowers();
			Adjustment.AdjustmentInfoMap adjMap = Adjustment.getAdjustmentInfo( stateInfo.getTurnState(), stateInfo.getRuleOptions(), powers );

			stateInfo.setAdjustmenInfoMap( adjMap );
		}
		
		RuleOptions ro = mapPanel.getWorld().getRuleOptions();
		useExplicitGUIMove = RuleOptions.VALUE_PATHS_EXPLICIT.equals(ro.getOptionValue(RuleOptions.OPTION_CONVOYED_MOVES));
		
		makeLayout();
	}
	// OrderControlBar()


	/**
	 *  Update the ValidationOptions
	 *
	 * @param  valOpts  The new validationOptions value
	 * @since
	 */
	public void setValidationOptions( ValidationOptions valOpts )
	{
		stateInfo.setValidationOptions( valOpts );
	}
	// setValidationOptions()


	/**
	 *  Dispatch mouse over events
	 *
	 * @param  loc  current Location
	 * @since  me	mouseEvent (<b>may be null</b>)
	 */
	public void mouseOver(MouseEvent me, Location loc)
	{
		lastLoc = loc;

		if( currentOrder != null )
		{
			// handle null Location
			if( loc == null )
			{
				handleNullLoc();
				return;
			}

			if( currentOrder.testLocation( stateInfo, loc, sb ) )
			{
				setUnitCursor( true );
				// Painting the current order in drag mode
				// Are we dragging from a province that has a unit available to move?
				if( inDrag && !dragLoc.equals( loc ) && currentOrder.getSourceUnitType() != null )
				{
					if( MODE_MOVE == currentAction )
					{
						tempOrder = (GUIMove) guiOrderFactory.createMove(
								currentOrder.getPower(), dragLoc,
								currentOrder.getSourceUnitType(), loc, ( (GUIMove) currentOrder ).isViaConvoy()
								 );
						// The next two lines ensure tempOrder.isComplete()
						tempOrder.setLocation(stateInfo, dragLoc, sb);
						tempOrder.setLocation(stateInfo, loc, sb);

						mapPanel.getOrderDisplayPanel().addOrder( tempOrder, false );
					}
					else if( MODE_RETREAT == currentAction )
					{
						tempOrder = (GUIRetreat) guiOrderFactory.createRetreat(
								currentOrder.getPower(), dragLoc,
								currentOrder.getSourceUnitType(), loc
								 );
						mapPanel.getOrderDisplayPanel().addOrder( tempOrder, false );
					}
					else if( MODE_SUPPORT == currentAction )
					{
						if( dragSupportLoc == null )
						{
							dragSupportLoc = loc;
							doOrder( me, loc );
						}

						tempOrder = guiOrderFactory.createGUISupport();
						tempOrder.setLocation( stateInfo, dragLoc, sb );
						tempOrder.setLocation( stateInfo, dragSupportLoc, sb );

						if( tempOrder.setLocation( stateInfo, loc, sb ) )
						{
							mapPanel.getOrderDisplayPanel().addOrder( tempOrder, false );
						}
					}
				}
			}
			else
			{
				// Current order can't be completed here

				// Change cursor
				if( currentOrder.getSourceUnitType() != null )
				{
					setUnitCursor( false );
				}
				else
				{
					mapPanel.setMapCursor( MapPanel.BAD_ACTION );
				}

				// append cancel message, if the order is "in progress"
				// otherwise do not append the cancel message.
				if(currentOrder.getCurrentLocationNum() > 0)
				{
					sb.append(' ');
					sb.append( Utils.getLocalString( GUIOrder.CLICK_TO_CANCEL ) );
				}
			}

			mapPanel.getStatusBarUtils().displayProvinceInfo( loc, sb.toString() );
		}
		else
		{
			// inform that no order has been selected.
			mapPanel.getStatusBarUtils().displayProvinceInfo( loc, Utils.getLocalString( "OrdConBar.noselection" ) );
			mapPanel.setMapCursor( MapPanel.BAD_ACTION );
		}
	}
	// mouseOver()


	/**
	 *  Dispatch mouse out events
	 *
	 * @param  loc  current Location
	 * @since
	 */
	public void mouseOut(MouseEvent me, Location loc)
	{
		lastLoc = loc;

		if( currentOrder == null )
		{
			// inform that no order has been selected
			mapPanel.setMapCursor( MapPanel.BAD_ACTION );
			mapPanel.getStatusBarUtils().setText( Utils.getLocalString( "OrdConBar.noselection" ) );
		}
		else
		{
			handleNullLoc();
		}
	}
	// handleNullLoc()



	/**
	 *  Dispatch mouse click events
	 *
	 */
	public void mouseClicked(MouseEvent me, Location loc)
	{
		inDrag = false;
		dragLoc = null;
		dragSupportLoc = null;
		currentAction = defaultAction;
	}


	/**
	 *  Called when the user is attempting to start (by clicking or initiating a
	 *  drag) or complete (by clicking again or dropping) an order.
	 *
	 * @param  loc  The location where the mouse event took place.
	 * @since
	 */
	public void doOrder(MouseEvent me, Location loc)
	{
		if( currentOrder != null )
		{
			// cancel if we are in a null location
			if( loc == null )
			{
				if((tempOrder!=null && tempOrder.isComplete())) // We can still finish up
				{
					loc = tempOrder.getSource();
				}
				else
				{
					doCanceled();
					return;
				}
			}

			if( currentOrder.setLocation( stateInfo, loc, sb ) )
			{
				mapPanel.setMapCursor( MapPanel.DEFAULT_CURSOR );
				mapPanel.getClientFrame().getOrderStatusPanel().setOrderText(currentOrder.toFormattedString(mapPanel.getClientFrame().getOFO()));
				mapPanel.getStatusBarUtils().displayProvinceInfo( loc, sb.toString() );
			}
			else if(tempOrder != null && tempOrder.isComplete())
			{
				currentOrder = tempOrder;
			}
			else
			{
				// we cannot set the location.
				// cancel the order.
				//
				mapPanel.setMapCursor( MapPanel.BAD_ACTION );
				mapPanel.getStatusBarUtils().displayProvinceInfo( loc, Utils.getLocalString( GUIOrder.CANCELED, currentOrder.getFullName() ) );
				setGUIOrder( loc );
				// create a new order writer
				return;
			}

			if( currentOrder.isComplete() )
			{
				mapPanel.getOrderDisplayPanel().addOrder( currentOrder, true );
				mapPanel.getClientFrame().getOrderStatusPanel().clearOrderText();
				mapPanel.setMapCursor( MapPanel.BAD_ACTION );
				setGUIOrder( loc );
				// create a new order writer
			}
			else
			{
				// retest the location if we aren't complete; the current location may not
				// be valid for subsequent clicks.
				mouseOver( me, loc );
			}
		}
	}
	// mouseClicked()


	// Start of click or drag
	/**
	 *  Description of the Method
	 *
	 * @param  loc     	current Location
	 * @param  me  		mouse event
	 * @since
	 */
	public void mouseDown(MouseEvent me, Location loc)
	{
		if( loc != null )
		{
			inDrag = true;
			dragLoc = loc;
			
			final short button = me.getButton();
			
			if( button == DOMUIEventListener.BUTTON_RIGHT )
			{
				currentAction = MODE_SUPPORT;
				setGUIOrder( loc );
				// Start a support order
			}
			else if( button == DOMUIEventListener.BUTTON_MIDDLE )
			{
				currentAction = MODE_HOLD;
				setGUIOrder( loc );
				// Start a hold order
				currentAction = defaultAction;
				inDrag = false;
				// Ignore the rest of the drag
			}

			doOrder( me, loc );

			if( currentOrder.getSourceUnitType() != null )
			{
				setUnitCursor( true );
			}
		}

	}


	// Note: A click seems to generate a mouseDown and mouseClicked but no mouseUp.
	/**
	 *  Description of the Method
	 *
	 * @param  loc  current Location
	 * @since
	 */
	public void mouseUp(MouseEvent me, Location loc)
	{
		currentAction = defaultAction;
		if( inDrag )
		{
			doOrder( me, loc );
		}
		tempOrder = null;
		inDrag = false;
		dragLoc = null;
		dragSupportLoc = null;
	}
	// doCanceled()



	/**
	 *  Handle DOM keyPress events
	 *
	 * @param  dke  DOMKeyEvent
	 * @param  loc  current mouse Location
	 * @since
	 */
	public void keyPressed(DOMKeyEvent dke, Location loc)
	{
		super.keyPressed( dke, loc );

		// ignore modifier keys
		if( dke.getAltKey() || dke.getCtrlKey() || dke.getMetaKey() )
		{
			return;
		}

		final int charCode = dke.getCharCode();

		if( charCode == KEY_CANCEL )
		{
			doCanceled();
		}

		for( int i = 0; i < charMap.length; i++ )
		{
			if( charCode == charMap[i] )
			{
				buttons[i].doClick();

				if( loc != null )
				{
					mouseOver( null, loc );
				}

				return;
			}
		}
	}
	// inner class ToggleListener



	/**
	 *  Creates a new GUIOrder subclass based upon the currently selected order.
	 *  <p>
	 *  sets currentOrder null if nothing is selected.
	 *
	 * @param  loc  current mouse Location
	 * @since
	 */
	protected void setGUIOrder( Location loc )
	{
		mapPanel.getClientFrame().getOrderStatusPanel().clearOrderText();
		
		/*
		 *  This is amateurish but effective. Should probably be replaced by
		 *  an object-oriented approach
		 */
		if( currentAction == MODE_HOLD )
		{
			currentOrder = guiOrderFactory.createGUIHold();
		}
		else if( currentAction == MODE_MOVE )
		{
			if(useExplicitGUIMove)
			{
				currentOrder = guiOrderFactory.createGUIMoveExplicit();
			}
			else
			{
				currentOrder = guiOrderFactory.createGUIMove();
			}
		}
		else if( currentAction == MODE_SUPPORT )
		{
			currentOrder = guiOrderFactory.createGUISupport();
		}
		else if( currentAction == MODE_CONVOY )
		{
			currentOrder = guiOrderFactory.createGUIConvoy();
		}
		else if( currentAction == MODE_RETREAT )
		{
			currentOrder = guiOrderFactory.createGUIRetreat();
		}
		else if( currentAction == MODE_DISBAND )
		{
			currentOrder = guiOrderFactory.createGUIDisband();
		}
		else if( currentAction == MODE_BUILD_ARMY )
		{
			currentOrder = guiOrderFactory.createGUIBuild();
			currentOrder.setParam( GUIBuild.BUILD_UNIT, Unit.Type.ARMY );
		}
		else if( currentAction == MODE_BUILD_FLEET )
		{
			currentOrder = guiOrderFactory.createGUIBuild();
			currentOrder.setParam( GUIBuild.BUILD_UNIT, Unit.Type.FLEET );
		}
		else if( currentAction == MODE_BUILD_WING )
		{
			currentOrder = guiOrderFactory.createGUIBuild();
			currentOrder.setParam( GUIBuild.BUILD_UNIT, Unit.Type.WING );
		}
		else if( currentAction == MODE_REMOVE )
		{
			currentOrder = guiOrderFactory.createGUIRemove();
		}
		else if( currentAction == MODE_WAIVE_BUILD )
		{
			currentOrder = guiOrderFactory.createGUIWaive();
		}
		else
		{
			currentOrder = null;
		}

		if( loc != null && currentOrder != null )
		{
			mouseOver( null, loc );
		}
	}
	// setGUIOrder()


	// If there's an order in progress, set the cursor to show which type of unit you're
	// ordering and/or show that the current status is invalid for that unit.
	private void setUnitCursor( boolean ok )
	{
		if( ok )
		{
			if( Unit.Type.ARMY == currentOrder.getSourceUnitType() )
			{
				mapPanel.setMapCursor( MapPanel.CURSOR_DRAG_ARMY );
			}
			else if( Unit.Type.FLEET == currentOrder.getSourceUnitType() )
			{
				mapPanel.setMapCursor( MapPanel.CURSOR_DRAG_FLEET );
			}
			else if( Unit.Type.WING == currentOrder.getSourceUnitType() )
			{
				mapPanel.setMapCursor( MapPanel.CURSOR_DRAG_WING );
			}
			else
			{
				mapPanel.setMapCursor( MapPanel.DEFAULT_CURSOR );
			}
		}
		else
		{
			if( Unit.Type.ARMY == currentOrder.getSourceUnitType() )
			{
				mapPanel.setMapCursor( MapPanel.CURSOR_DRAG_ARMY_NO );
			}
			else if( Unit.Type.FLEET == currentOrder.getSourceUnitType() )
			{
				mapPanel.setMapCursor( MapPanel.CURSOR_DRAG_FLEET_NO );
			}
			else if( Unit.Type.WING == currentOrder.getSourceUnitType() )
			{
				mapPanel.setMapCursor( MapPanel.CURSOR_DRAG_WING_NO );
			}
			else
			{
				mapPanel.setMapCursor( MapPanel.BAD_ACTION );
			}
		}
	}
	// mouseOut()


	/**
	 *  Handle a null location. Assumes currentOrder != null. Sets pointer and
	 *  text.
	 *
	 * @since
	 */
	private void handleNullLoc()
	{
		// the order cannot be executed here -- but if it is in progress, it may be cancelled.
		if( currentOrder != null && currentOrder.getSourceUnitType() != null )
		{
			setUnitCursor( false );
		}
		else
		{
			mapPanel.setMapCursor( MapPanel.BAD_ACTION );
		}
		sb.setLength( 0 );
		sb.append( Utils.getLocalString( "OrdConBar.noprovince.nostart", currentOrder.getFullName() ) );

		if( currentOrder.getCurrentLocationNum() >= 1 )
		{
			// the order is 'in progress' and in a bad location. click to cancel
			sb.append(' ');
			sb.append( Utils.getLocalString( GUIOrder.CLICK_TO_CANCEL ) );
		}

		mapPanel.getStatusBarUtils().setText( sb.toString() );
	}


	/**
	 *  Handle 'cancel' events
	 *
	 * @since
	 */
	private void doCanceled()
	{
		if( currentOrder != null )
		{
			mapPanel.setMapCursor( MapPanel.BAD_ACTION );
			mapPanel.getStatusBarUtils().setText( Utils.getLocalString( GUIOrder.CANCELED, currentOrder.getFullName() ) );
			currentOrder = null;
			setGUIOrder( lastLoc );
			// create a new GUIOrder
		}
	}


	/**
	 *  Layout the ControlBar
	 *
	 * @since
	 */
	private void makeLayout()
	{
		// phase determines groups
		String[] text = null;
		String[] cmd = null;
		String defaultButton = null;

		Phase.PhaseType phaseType = stateInfo.getTurnState().getPhase().getPhaseType();
		Log.println("OCB:phase: ", stateInfo.getTurnState().getPhase());
		
		if( phaseType == Phase.PhaseType.MOVEMENT )
		{
			text = GROUP_MOVEMENT_TEXT;
			cmd = GROUP_MOVEMENT_CMD;
			charMap = GROUP_MOVEMENT_CHARCODES;
			defaultButton = GROUP_MOVEMENT_DEFAULT_BUTTON;
		}
		else if( phaseType == Phase.PhaseType.RETREAT )
		{
			text = GROUP_RETREAT_TEXT;
			cmd = GROUP_RETREAT_CMD;
			charMap = GROUP_RETREAT_CHARCODES;
			defaultButton = GROUP_RETREAT_DEFAULT_BUTTON;
		}
		else if( phaseType == Phase.PhaseType.ADJUSTMENT )
		{
			text = GROUP_ADJUSTMENT_TEXT;
			cmd = GROUP_ADJUSTMENT_CMD;
			charMap = GROUP_ADJUSTMENT_CHARCODES;
			defaultButton = GROUP_ADJUSTMENT_DEFAULT_BUTTON;

			// filter out 'wing' button, if wings are NOT enabled.
			RuleOptions ro = mapPanel.getWorld().getRuleOptions();

			if( ro.getOptionValue( RuleOptions.OPTION_WINGS ) != RuleOptions.VALUE_WINGS_ENABLED )
			{
				text = new String[GROUP_ADJUSTMENT_TEXT.length - 1];
				cmd = new String[GROUP_ADJUSTMENT_CMD.length - 1];
				charMap = new int[GROUP_ADJUSTMENT_CHARCODES.length - 1];

				int idx = 0;

				for( int i = 0; i < GROUP_ADJUSTMENT_CMD.length; i++ )
				{
					if( GROUP_ADJUSTMENT_CMD[i] != MODE_BUILD_WING )
					{
						text[idx] = GROUP_ADJUSTMENT_TEXT[i];
						cmd[idx] = GROUP_ADJUSTMENT_CMD[i];
						charMap[idx] = GROUP_ADJUSTMENT_CHARCODES[i];
						idx++;
					}
				}
			}
		}
		else
		{
			throw new IllegalArgumentException( "Unknown Phase: " + phaseType );
		}

		// spacer
		addSeparator();

		// listener
		ToggleListener tl = new ToggleListener();

		// create toggle buttons.
		// set the default button as selected
		buttons = new JToggleButton[text.length];

		ButtonGroup bg = new ButtonGroup();

		for( int i = 0; i < text.length; i++ )
		{
			buttons[i] = new JToggleButton( text[i], false );
			buttons[i].setActionCommand( cmd[i] );
			buttons[i].addActionListener( tl );

			// check for default button; if we are it, make it appear 'clicked'
			if( cmd[i] == defaultButton )
			{
				buttons[i].doClick();
			}

			bg.add( buttons[i] );
			this.add( buttons[i] );
		}
	}
	// makeLayout()



	/**
	 *  Listens for toggle events; sets which button is selected
	 */
	private class ToggleListener implements ActionListener
	{
		public void actionPerformed( ActionEvent e )
		{
			defaultAction = ( (JToggleButton) e.getSource() ).getActionCommand();
			currentAction = defaultAction;
			setGUIOrder( null );
		}
		// actionPerformed()
	}
	// setUnitCursor
	
	
	
}

