//
//  @(#)World.java		4/2002
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
package dip.world;

import dip.world.metadata.PlayerMetadata;
import dip.world.metadata.GameMetadata;
import dip.gui.undo.UndoRedoManager;

import dip.net.message.PressStore;
import dip.net.message.DefaultPressStore;

import java.io.*;
import java.util.*;
import java.util.Map;

import java.net.*;
import java.util.zip.*;

import JSX.*;


/**
*	The entire game World. This contains the state of an entire game.
*	<p>
*	A World contains:
*	<ol>
*		<li>Map (dip.world.Map) object [constant]
*		<li>TurnState objects [in a linked hash map]
*		<li>HashMap of per-power and global state information (used to set various data)
*	</ol>
*
*
*/
public class World implements Serializable
{
	// constants for non-turn-data lookup
	private static final String KEY_GLOBAL_DATA = "_global_data_";
	private static final String KEY_VICTORY_CONDITIONS = "_victory_conditions_";
	
	private static final String KEY_WORLD_METADATA = "_world_metadata_";
	private static final String KEY_UNDOREDOMANAGER = "_undo_redo_manager_";
	private static final String KEY_GAME_SETUP = "_game_setup_";
	private static final String KEY_PRESS_STORE = "_press_store_";
	private static final String KEY_VARIANT_INFO = "_variant_info_";
	
	// instance variables
	private SortedMap 				turnStates = null;			// turn data
	private Map 					nonTurnData = null;			// non-turn data (misc data & per-player data)
	private final dip.world.Map		map;						// the actual map (constant)
	
	
	/**
	*	Reads a World object from a file.
	*/
	public static World open(File file)
	throws IOException
	{
		JSX.ObjectReader in = null;
		
		try
		{
			GZIPInputStream gzi = new GZIPInputStream(new BufferedInputStream(new FileInputStream(file), 4096));
			in =  new JSX.ObjectReader(gzi);
			World w = (World) in.readObject();
			return (World) w;
		}
		catch(IOException ioe)
		{
			throw ioe;
		}
		catch(Exception e)
		{
			// rethrow all non-IOExceptions as IOExceptions
			IOException ioe = new IOException(e.getMessage());
			ioe.initCause(e);
			throw ioe;
		}
		finally
		{
			if(in != null)
			{
				in.close();
			}	
		}
	}// open()
	
	
	/**
	*	Saves a World object to a file.
	*/
	public static void save(File file, World world)
	throws IOException
	{
		GZIPOutputStream gzos = null;
		
		try
		{
			gzos = new GZIPOutputStream(new FileOutputStream(file), 2048);
			JSX.ObjectWriter out = new JSX.ObjectWriter(gzos);
			out.setPrettyPrint(false);
			out.writeObject(world);
			out.close();
			gzos.finish(); // this is key. otherwise data is not written.
		}
		catch(IOException ioe)
		{
			throw ioe;
		}
		catch(Exception e)
		{
			// rethrow all non-IOExceptions as IOExceptions
			IOException ioe = new IOException(e.getMessage());
			ioe.initCause(e);
			throw ioe;
		}
		finally
		{
			if(gzos != null)
			{
				gzos.close();
			}	
		}
	}// save()
	
	
	
	
	
	
	/**
	*	Constructs a World object.
	*/
	protected World(dip.world.Map map)
	{
		this.map = map;
		turnStates = Collections.synchronizedSortedMap(new TreeMap());	// synchronize on TreeMap
		nonTurnData = new HashMap(17);
	}// World()
	
	
	/** Returns the Map (dip.world.Map) associated with this World. */
	public dip.world.Map getMap()
	{
		return map;
	}// getMap()
	
	
	/** 
	*	Sets any special per-power state information that is not associated with
	*	a particular TurnState. This may be set to null.
	*/
	public void setPowerState(Power power, Object state)
	{
		nonTurnData.put(power, state);
	}// setPowerState()
	
	/** 
	*	Gets any special per-power state information that is not associated with
	*	a particular TurnState. This may return null.
	*/
	public Object getPowerState(Power power)
	{
		return nonTurnData.get(power);
	}// getPowerState()
	
	
	/** Set the Global state object. This may be set to null. */
	public void setGlobalState(Object state)
	{
		nonTurnData.put(KEY_GLOBAL_DATA, state);
	}// setGlobalState()
	
	/** Get the Global state object. This may return null. */
	public Object getGlobalState()
	{
		return nonTurnData.get(KEY_GLOBAL_DATA);
	}// getGlobalState()	
	
	
	/** Set the Victory Conditions */
	public void setVictoryConditions(VictoryConditions value)
	{
		nonTurnData.put(KEY_VICTORY_CONDITIONS, value);
	}// setVictoryConditions()
	
	/** Get the Victory Conditions */
	public VictoryConditions getVictoryConditions()
	{
		return (VictoryConditions) nonTurnData.get(KEY_VICTORY_CONDITIONS);
	}// getVictoryConditions()	
	
	
	/** Gets the first TurnState object */
	public TurnState getInitialTurnState()
	{
		TurnState ts = (TurnState) turnStates.get(turnStates.firstKey());
		if(ts != null)
		{
			ts.setWorld(this);
		}
		return ts;
	}// getInitialTurnState()
	
	
	/** Gets the most current (last in the list) TurnState. */
	public TurnState getLastTurnState()
	{
		TurnState ts = (TurnState) turnStates.get(turnStates.lastKey());
		if(ts != null)
		{
			ts.setWorld(this);
		}
		return ts;
	}// getLastTurnState()
	
	
	/** Gets the TurnState associated with the specified Phase */
	public TurnState getTurnState(Phase phase)
	{
		TurnState ts = (TurnState) turnStates.get(phase);
		if(ts != null)
		{
			ts.setWorld(this);
		}
		return ts;
	}// getTurnState()
	
	
	/** Gets the TurnState that comes after this phase (if it exists). 
	*	<p>
	*	Note that the next phase may not be (due to phase skipping) the
	*	same phase generated by phase.getNext(). This will return null
	*	iff we are at the last Phase.
	*/
	public TurnState getNextTurnState(TurnState state)
	{
		Phase current = state.getPhase();
		if(current == null)
		{
			return null;
		}
		
		Phase next = null;
		Iterator iter = turnStates.keySet().iterator();
		while(iter.hasNext())
		{
			Phase phase = (Phase) iter.next();
			if(current.compareTo(phase) == 0)
			{
				if(iter.hasNext())
				{
					next = (Phase) iter.next();
				}
				
				break;
			}
		}
		
		if(next == null)
		{
			return null;
		}
		
		TurnState ts = (TurnState) turnStates.get(next);
		ts.setWorld(this);
		return ts;
	}// getNextTurnState()
	
	
	/**
	*	Get all TurnStates. Note that the returned List
	*	may be modified, without modifications being reflected
	*	in the World object. However, modifications to individual
	*	TurnState objects will be reflected in the World object
	*	(TurnStates are not cloned here).
	*/
	public List getAllTurnStates()
	{
		Collection values = turnStates.values();
		ArrayList al = new ArrayList(values.size());
		al.addAll(values);
		return al;
	}// getAllTurnStates()
	
	
	/** Gets the TurnState that comes before the specified phase. 
	*	<p>
	*	Note that the previous phase may not be (due to phase skipping) the
	*	same phase generated by phase.getPrevious(). This will return null
	*	iff we are at the first (initial) Phase.
	*/
	public TurnState getPreviousTurnState(TurnState state)
	{
		Phase current = state.getPhase();
		if(current == null)
		{
			return null;
		}
		
		
		Phase previous = null;
		Iterator iter = turnStates.keySet().iterator();
		while(iter.hasNext())
		{
			Phase phase = (Phase) iter.next();
			if(phase.compareTo(current) != 0)
			{
				previous = phase;
			}
			else
			{
				break;
			}
		}
		
		if(previous == null)
		{
			return null;
		}
		
		TurnState ts = (TurnState) turnStates.get(previous);
		ts.setWorld(this);
		return ts;
	}// getPreviousTurnState()
	
	
	/** If a TurnState with the given phase already exists, it is replaced. */
	public void setTurnState(TurnState turnState)
	{
		turnStates.put(turnState.getPhase(), turnState);
	}// setTurnState()
	
	
	/** 
	*	Removes a turnstate from the world. This should 
	*	be used with caution!
	*/
	public void removeTurnState(TurnState turnState)
	{
		turnStates.remove(turnState.getPhase());
	}// removeTurnState()
	
	
	/** Removes <b>all</b> TurnStates from the World. */
	public void removeAllTurnStates()
	{
		turnStates.clear();
	}// removeAllTurnStates()
	
	
	/** returns sorted (ascending) set of all Phases */
	public Set getPhaseSet()
	{
		return turnStates.keySet();
	}// getPhaseSet()
	
	
	/** Sets the Game metadata */
	public void setGameMetadata(GameMetadata gmd)
	{
		if(gmd == null)
		{
			throw new IllegalArgumentException("null metadata");
		}
		
		nonTurnData.put(KEY_WORLD_METADATA, gmd);
	}// setGameMetadata()
	
	/** Gets the Game metadata. Never returns null. Does not return a copy. */
	public GameMetadata getGameMetadata()
	{
		GameMetadata gmd = (GameMetadata) nonTurnData.get(KEY_WORLD_METADATA);
		if(gmd == null)
		{
			gmd = new GameMetadata();
			setGameMetadata(gmd);
		}
		return gmd;
	}// setGameMetadata()
	
	
	/** Sets the metadata for a player, referenced by Power */
	public void setPlayerMetadata(Power power, PlayerMetadata pmd)
	{
		if(power == null || pmd == null)
		{
			throw new IllegalArgumentException("null power or metadata");
		}
		nonTurnData.put(power, pmd);
	}// setPlayerMetadata()
	
	/** Gets the metadata for a power. Never returns null. Does not return a copy. */
	public PlayerMetadata getPlayerMetadata(Power power)
	{
		if(power == null)
		{
			throw new IllegalArgumentException("null power");
		}
		
		PlayerMetadata pmd = (PlayerMetadata) nonTurnData.get(power);
		if(pmd == null)
		{
			pmd = new PlayerMetadata();
			setPlayerMetadata(power, pmd);
		}
		return pmd;
	}// getPlayerMetadata()
	
	
	/** Sets the UndoRedo manager to be saved. This may be set to null. */
	public void setUndoRedoManager(UndoRedoManager urm)
	{
		nonTurnData.put(KEY_UNDOREDOMANAGER, urm);
	}// setGlobalState()
	
	
	/** Gets the UndoRedo manager that was saved. Null if none was saved. */
	public UndoRedoManager getUndoRedoManager()
	{
		return (UndoRedoManager) nonTurnData.get(KEY_UNDOREDOMANAGER);
	}// getUndoRedoManager()
	
	
	/** Sets the GameSetup object */
	public void setGameSetup(GameSetup gs)
	{
		if(gs == null) { throw new IllegalArgumentException(); }
		nonTurnData.put(KEY_GAME_SETUP, gs);
	}// setGameSetup()
	
	
	/** Returns the GameSetup object */
	public GameSetup getGameSetup()
	{
		return (GameSetup) nonTurnData.get(KEY_GAME_SETUP);
	}// getGameSetup()
	
	
	/** Get the PressStore object, which stores and retreives Press messages. */
	public synchronized PressStore getPressStore()
	{
		// create on demand
		if(nonTurnData.get(KEY_PRESS_STORE) == null)
		{
			nonTurnData.put(KEY_PRESS_STORE, new DefaultPressStore());
		}
		
		return (PressStore) nonTurnData.get(KEY_PRESS_STORE);
	}// getPressStore()
	
	
	
	/** Get the Variant Info object. This returns a Reference to the Variant information. */
	public synchronized VariantInfo getVariantInfo()
	{
		VariantInfo vi = (VariantInfo) nonTurnData.get(KEY_VARIANT_INFO);
		
		if(vi == null)
		{
			vi = new VariantInfo();
			nonTurnData.put(KEY_VARIANT_INFO, vi);
		}
		
		return vi; 
	}// getVariantInfo()
	
	/** Set the Variant Info object. */
	public synchronized void setVariantInfo(VariantInfo vi)
	{
		nonTurnData.put(KEY_VARIANT_INFO, vi);
	}// getVariantInfo()
	
	
	/** Convenience method: gets RuleOptions from VariantInfo object. */
	public RuleOptions getRuleOptions()
	{
		return getVariantInfo().getRuleOptions();
	}// getRuleOptions()
	
	/** Convenience method: sets RuleOptions in VariantInfo object. */
	public void setRuleOptions(RuleOptions ruleOpts)
	{
		if(ruleOpts == null) { throw new IllegalArgumentException(); }
		getVariantInfo().setRuleOptions(ruleOpts);
	}// getRuleOptions()
	
	
	/** 
	*	Variant Info is a class which holds information about 
	*	the variant, map, symbols, and symbol options.
	*/
	public static class VariantInfo
	{
		private String variantName;
		private String mapName;
		private String symbolsName;
		private float variantVersion;
		private float symbolsVersion;
		private RuleOptions ruleOptions;
		
		/** Create a VariantInfo object */
		public VariantInfo() {}
		
		/** Set the Variant name. */
		public void setVariantName(String value) 	{ this.variantName = value; }
		/** Set the Map name. */
		public void setMapName(String value) 		{ this.mapName = value; }
		/** Set the Symbol pack name. */
		public void setSymbolPackName(String value) 	{ this.symbolsName = value; }
		/** Set the Variant version. */
		public void setVariantVersion(float value) 	
		{ 
			checkVersion(value); 
			this.variantVersion = value; 
		}
		/** Set the Symbol pack version. */
		public void setSymbolPackVersion(float value)
		{ 
			checkVersion(value); 
			this.symbolsVersion = value; 
		}
		/** <b>Replaces</b> the current RuleOptions with the given RuleOptions */
		public void setRuleOptions(RuleOptions value)	{ this.ruleOptions = value; }
		
		
		/** Get the Variant name. */
		public String getVariantName() 		{ return this.variantName; }
		/** Get the Map name. */
		public String getMapName() 			{ return this.mapName; }
		/** Get the Symbol pack name. */
		public String getSymbolPackName() 		{ return this.symbolsName; }
		/** Get the Variant version. */
		public float getVariantVersion() 	{ return this.variantVersion; }
		/** Get the Symbol pack version. */
		public float getSymbolPackVersion() 	{ return this.symbolsVersion; }
		
		/** Gets the RuleOptions */
		public RuleOptions getRuleOptions()
		{
			if(ruleOptions == null)
			{
				ruleOptions = new RuleOptions();
			}
			
			return ruleOptions;
		}// getRuleOptions()
		
		
		/** ensures Version is a value &gt;0.0f */
		private void checkVersion(float v)
		{
			if(v <= 0.0f)
			{
				throw new IllegalArgumentException("version: "+v);
			}
		}// checkVersion();
	}// nested class VariantInfo
	
	
}// class World
