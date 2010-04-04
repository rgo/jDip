//
//  @(#)PressConfiguration.java	9/2003
//
//  Copyright 2003 Zachary DelProposto. All rights reserved.
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
package dip.net.message;

import dip.world.Phase;
import dip.world.Phase.PhaseType;
import dip.world.Phase.YearType;
import dip.world.Phase.SeasonType;

import dip.misc.Utils;

/**
*	Press Configuration
*	<p>
*	This object holds the general press options. This object 
*	does not and is not meant to hold any PressChannel-specific
*	data (i.e., network info, etc.).
*
*/
public class PressConfiguration implements Cloneable
{
	// i18n constants: press types, descriptions, and misc.
	private static final String NO_MASTER 		= "pc.dialog.master.none";
	// types                                         
	private static final String I18N_TYPE_NONE 		= "pc.press.type.none";
	private static final String I18N_TYPE_WHITE 	= "pc.press.type.white";
	private static final String I18N_TYPE_GRAY 		= "pc.press.type.gray";
	private static final String I18N_TYPE_GRAYWHITE = "pc.press.type.graywhite";
	private static final String I18N_TYPE_BROADCAST = "pc.press.type.broadcast";
	// descriptions                                  
	private static final String DESC_NONE 		= "pc.press.desc.none";
	private static final String DESC_WHITE 		= "pc.press.desc.white";
	private static final String DESC_GRAY 		= "pc.press.desc.gray";
	private static final String DESC_GRAYWHITE 	= "pc.press.desc.graywhite";
	private static final String DESC_BROADCAST 	= "pc.press.desc.broadcast";
	// 
	private static final String DESC_ALWAYS		= "pc.press.desc.alwaysallowed";
	
	/** Disallow press of any type, except to the Master */
	public static final int TYPE_NONE = 0;
	/** Press that is never anonymous. */
	public static final int TYPE_WHITE = 1;
	/** Press that is always anonymous */
	public static final int TYPE_GRAY = 2;
	/** Press that may be anonymous or not. */
	public static final int TYPE_WHITE_AND_GRAY = 3;
	
	
	/** Observers: same restrictions as players */
	public static final int OBSERVER_ANY = 0;
	/** Observers: Only White press from observers is allowed */
	public static final int OBSERVER_WHITE = 1;
	/** Observers: No press (except to master) is allowed */
	public static final int OBSERVER_NONE = 2;
	
	
	
	// instance fields
	private int playerPress = TYPE_WHITE;
	private int observerPress = OBSERVER_ANY;
	private boolean isBroadcast = false;
	private MID master = null;
	private SeasonType[] prohibitedSeasons = new SeasonType[0];
	private PhaseType[] prohibitedPhases = { PhaseType.ADJUSTMENT, PhaseType.RETREAT };
	private YearType[] prohibitedYears = new YearType[0];
	
	/** Create a PressConfiguration object. */
	public PressConfiguration()
	{
	}// PressConfiguration
	
	
	/** Set the Master (null if none) */
	public void setMaster(MID mid)				{ master = mid; }
	/** Get the Master (null if none) */
	public MID getMaster()						{ return master; }
	
	
	/** Set the type of press allowed between players */
	public void setPlayerPressType(int type)		{ playerPress = type; }
	/** Get the type of press allowed between players */
	public int getPlayerPressType()					{ return playerPress; }
	
	
	/** 
	*	Set force-broadcast flag; if broadcast, Press must be sent 
	*	to all players (except master); recipients cannot be chosen. 
	*/
	public void setBroadcast(boolean value)			{ isBroadcast = value; }
	/** Get the forced broadcast flag. */
	public boolean getBroadcast()					{ return isBroadcast; }
	
	
	/** Set the type of press allowed between observers */
	public void setObserverPressType(int type)		{ observerPress = type; }
	/** Get the type of press allowed between observers */
	public int getObserverPressType()				{ return observerPress; }
	
	
	/** Get which seasons (if any) during which Press is prohibited */
	public SeasonType[] getProhibitedSeasons()					{ return prohibitedSeasons; }
	/** Set which seasons (if any) during which Press is prohibited */
	public void setProhibitedSeasons(SeasonType[] types)
	{ prohibitedSeasons = (types == null) ? new SeasonType[0] : types; }
	
	
	/** Get which years (if any) during which Press is prohibited */
	public YearType[] getProhibitedYearTypes()				{ return prohibitedYears; }
	/** Set which years (if any) during which Press is prohibited */
	public void setProhibitedYearTypes(YearType[] types)
	{ prohibitedYears = (types == null) ? new YearType[0] : types; }
	
	
	/** Get which phases (if any) during which Press is prohibited */
	public PhaseType[] getProhibitedPhases()				{ return prohibitedPhases; }
	/** Set which phases (if any) during which Press is prohibited */
	public void setProhibitedPhases(PhaseType[] types)		
	{ prohibitedPhases = (types == null) ? new PhaseType[0] : types; }
	
	
	/** Get player press type (including if broadcast). This is localized. */
	public String getPlayerPTName()
	{
		StringBuffer sb = new StringBuffer(64);
		switch(getPlayerPressType())
		{
			case TYPE_NONE:
				sb.append( Utils.getLocalString(I18N_TYPE_NONE) );
				break;
			case TYPE_WHITE:
				sb.append( Utils.getLocalString(I18N_TYPE_WHITE) );
				break;
			case TYPE_GRAY:
				sb.append( Utils.getLocalString(I18N_TYPE_GRAY) );
				break;
			case TYPE_WHITE_AND_GRAY:
				sb.append( Utils.getLocalString(I18N_TYPE_GRAYWHITE) );
				break;
			default:
				throw new IllegalArgumentException("invalid player press type");
		}
		
		if(getBroadcast())
		{
			sb.append(", ");
			sb.append( Utils.getLocalString(I18N_TYPE_BROADCAST) );
		}
		
		return sb.toString();
	}// getPlayerPTName()
	
	/** Get player press type description (including if broadcast). This is localized. */
	public String getPlayerPTDesc()
	{
		StringBuffer sb = new StringBuffer(128);
		switch(getPlayerPressType())
		{
			case TYPE_NONE:
				sb.append( Utils.getLocalString(DESC_NONE) );
				break;
			case TYPE_WHITE:
				sb.append( Utils.getLocalString(DESC_WHITE) );
				break;
			case TYPE_GRAY:
				sb.append( Utils.getLocalString(DESC_GRAY) );
				break;
			case TYPE_WHITE_AND_GRAY:
				sb.append( Utils.getLocalString(DESC_GRAYWHITE) );
				break;
			default:
				throw new IllegalArgumentException("invalid player press type");
		}
		
		if(getBroadcast())
		{
			sb.append(", ");
			sb.append( Utils.getLocalString(DESC_BROADCAST) );
		}
		
		return sb.toString();
	}// getPlayerPTDesc()
	
	/** Get observer press type (including if broadcast). This is localized. */
	public String getObserverPTName()
	{
		StringBuffer sb = new StringBuffer(64);
		switch(getObserverPressType())
		{
			case OBSERVER_ANY:
				return getPlayerPTName();	// same as player settings
				
			case OBSERVER_WHITE:
				sb.append( Utils.getLocalString(I18N_TYPE_WHITE) );
				break;
			
			case OBSERVER_NONE:
				sb.append( Utils.getLocalString(I18N_TYPE_NONE) );
				break;
				
			default:
				throw new IllegalArgumentException("invalid observer press type");
		}
		
		if(getBroadcast())
		{
			sb.append(", ");
			sb.append( Utils.getLocalString(I18N_TYPE_BROADCAST) );
		}
		
		return sb.toString();
	}// getObserverPTName()
	
	/** Get observer press type description (including if broadcast). This is localized. */
	public String getOBserverPTDesc()
	{
		StringBuffer sb = new StringBuffer(128);
		switch(getObserverPressType())
		{
			case OBSERVER_ANY:
				return getPlayerPTDesc();	// same as player settings
				
			case OBSERVER_WHITE:
				sb.append( Utils.getLocalString(DESC_WHITE) );
				break;
			
			case OBSERVER_NONE:
				sb.append( Utils.getLocalString(DESC_NONE) );
				break;
				
			default:
				throw new IllegalArgumentException("invalid observer press type");
		}
		
		if(getBroadcast())
		{
			sb.append(", ");
			sb.append( Utils.getLocalString(DESC_BROADCAST) );
		}
		
		return sb.toString();
	}// getOBserverPTDesc()
	
	/** Get master nick/name. This is localized, and handles <code>null</code> Master setting. */
	public String getMasterName()
	{
		if(getMaster() == null)
		{
			return Utils.getLocalString(NO_MASTER);
		}
		
		return getMaster().getNickAndName();
	}// getMasterName()
	
	/** Get list of times when press is not allowed, as verbose list. This is localized. */
	public String getProhibitedTimes()
	{
		boolean hasSome = false;	// do we have *any* prohibitions? 
		boolean isFirst = true;	// first printed?
		
		StringBuffer sb = new StringBuffer(256);
		
		for(int i=0; i<prohibitedPhases.length; i++)
		{
			if(!isFirst) 
			{
				sb.append(", ");
			}
			
			sb.append( prohibitedPhases[i] );
			
			hasSome = true;
			isFirst = false;
		}
		
		for(int i=0; i<prohibitedSeasons.length; i++)
		{
			if(!isFirst) 
			{
				sb.append(", ");
			}
			
			sb.append( prohibitedSeasons[i] );
			
			hasSome = true;
			isFirst = false;
		}
		
		for(int i=0; i<prohibitedYears.length; i++)
		{
			if(!isFirst) 
			{
				sb.append(", ");
			}
			
			sb.append( prohibitedYears[i] );
			
			hasSome = true;
			isFirst = false;
		}
		
		if(!hasSome)
		{
			sb.append( Utils.getLocalString(DESC_ALWAYS) );
		}
		
		return sb.toString();
	}// getProhibitedTimes()
	
	
	/** 
	*	Determines, based on settings, if a message can be sent. 
	*	Note that if the message or currentPhase parameter is null, it
	*	is ignored. (This allows checking of Phase settings, for example,
	*	before a message is composed).
	*/
	public boolean canSend(PressMessage message, Phase currentPhase)
	{
		if(message == null && currentPhase == null)
		{
			return false;
		}
		
		
		if(message != null)
		{
			// if FROM the master, it is always allowed.
			if(message.getFrom().equals(master))
			{
				return true;
			}
			
			// if TO the master, and noone else, it is always allowed.
			if(message.getTo().length == 1)
			{
				if(message.getTo()[0].equals(master))
				{
					return true;
				}
			}
			
			// if player (MID Power != null)
			if(message.getFrom().getPower() != null)
			{
				if( !checkPlayerSettings(message) )
				{
					return false;
				}
			}
			else
			{
				// we are an observer
				//
				if(observerPress == OBSERVER_NONE)
				{
					return false;
				}
				else if(observerPress == OBSERVER_WHITE)
				{
					if(message.getFrom().equals(MID.ANONYMOUS))
					{
						return false;
					}
				}
				else
				{
					// otherwise, same rules as players
					if( !checkPlayerSettings(message) )
					{
						return false;
					}
				}
			}
		}
		
		
		if(currentPhase != null)
		{
			for(int i=0; i<prohibitedPhases.length; i++)
			{
				if(prohibitedPhases[i].equals(currentPhase.getPhaseType()))
				{
					return false;
				}
			}
			
			for(int i=0; i<prohibitedSeasons.length; i++)
			{
				if(prohibitedSeasons[i].equals(currentPhase.getSeasonType()))
				{
					return false;
				}
			}
			
			for(int i=0; i<prohibitedYears.length; i++)
			{
				if(prohibitedYears[i].equals(currentPhase.getYearType()))
				{
					return false;
				}
			}
		}
		
		return true;
	}// canSend()
	
	
	/**
	*	Checks against press settings. Assumes we already checked
	*	to see if press was being sent to or from the Master.
	*
	*/
	private boolean checkPlayerSettings(PressMessage message)
	{
		// if broadcasts are forced, we must not specify 
		// a player
		if(getBroadcast())
		{
			if(message.getTo().length != 0 )
			{
				return false;
			}
		}
		
		// check settings
		if(playerPress == TYPE_NONE)
		{
			return false;
		}
		else if(playerPress == TYPE_WHITE)
		{
			if(message.getFrom().equals(MID.ANONYMOUS))
			{
				return false;
			}
		}
		else if(playerPress == TYPE_GRAY)
		{
			if(!message.getFrom().equals(MID.ANONYMOUS))
			{
				return false;
			}
		}
		else if(playerPress == TYPE_WHITE_AND_GRAY)
		{
			return true;
		}
		else
		{
			// should not occur...
			return false;
		}
		
		return true;
	}// checkPlayerSettings()
	
	
	/** Clones the object. */
	public Object clone()
	throws CloneNotSupportedException
	{
		return super.clone();
	}// clone()
	
}// class PressConfiguration

