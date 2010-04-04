//
//  @(#)PersistenceManager.java		4/2002
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
package dip.gui;

import dip.gui.dialog.*;
import dip.gui.dialog.newgame.*;
import dip.gui.dialog.prefs.*;
import dip.judge.gui.FlocImportDialog;

import dip.gui.report.ResultWriter;

import dip.world.World;
import dip.world.Phase;
import dip.world.TurnState;
import dip.world.variant.VariantManager;
import dip.world.variant.data.Variant;

import dip.misc.Help;
import dip.misc.Utils;
import dip.gui.swing.XJFileChooser;
import dip.misc.SimpleFileFilter;
import dip.misc.Log;

import dip.judge.parser.JudgeImport;

import java.awt.Dimension;
import java.io.*;
import javax.swing.*;
import java.beans.*;
import java.util.*;


/**
*	Manages saving / opening of game files, and creation of new games, and exiting of the program.
*	<p>
*	Ensures user can save any changes (if changes were made) before committing to an action 
*	that cannot be undone.
*	<p>
*	Also sets the main frame title.
*/
public class PersistenceManager
{
	// i18n constants
	private static final String CONFIRM_TEXT = "PM.dialog.confirm.location.text";
	private static final String CONFIRM_TITLE = "PM.dialog.confirm.title";
	private static final String CONFIRM_BUTTON_SAVE = "PM.dialog.confirm.save";
	private static final String CONFIRM_BUTTON_DONTSAVE = "PM.dialog.confirm.dontsave";
	private static final String CONFIRM_BUTTON_CANCEL = "PM.dialog.confirm.cancel";
	private static final String CONFIRM_BUTTON_REWIND = "PM.dialog.confirm.rewind";
	private static final String CONFIRM_BUTTON_LOAD = "PM.dialog.confirm.load";
	private static final String CONFIRM_REWIND_TEXT = "PM.dialog.confirm.rewind.text";
	private static final String CONFIRM_REWIND_TITLE = "PM.dialog.confirm.rewind.title";
	private static final String CONFIRM_LOAD_TEXT = "PM.dialog.confirm.load.text";
	private static final String CONFIRM_LOAD_TITLE = "PM.dialog.confirm.load.title";
	
	private static final String UNSAVED_NAME = "PM.noname";
	//private static final String OVERWRITE_TEXT = "PM.dialog.overwrite.text.location";
	//private static final String OVERWRITE_TITLE = "PM.dialog.overwrite.title";
	private static final String IMPORT_CHOOSER_TITLE = "PM.dialog.import.title";
	private static final String MODIFIED_INDICATOR = "PM.indicator.modified";
	private static final String SAVE_TO_TITLE = "PM.chooser.title.saveto";
	private static final String EMPTY = "";
	
	// internal constants
	private final static String WINDOW_MODIFIED = "windowModified";
	private final static long   THREAD_WAIT = 7500L;
	
	// instance variables
	private ClientFrame clientFrame = null;
	private boolean isChanged = false;
	private File fileName = null;
	private PropertyChangeListener modListener = null;
	private final ThreadGroup persistTG;
	
	/** Creates a new PersistenceManager object. */
	public PersistenceManager(ClientFrame clientFrame)
	{
		this.clientFrame = clientFrame;
		
		// create the persistance-manager threadgroup
		persistTG = new ThreadGroup(Thread.currentThread().getThreadGroup(), "jdipPMGroup");
		
		// by default, disable Save/Save As until we open/new something.
		setSaveEnabled(false);
		setTitle();
		
		// enable modification event listener
		modListener = new PropertyChangeListener()
		{
			public void propertyChange(PropertyChangeEvent evt)
			{
				if(!isChanged())
				{
					setChanged(true);
				}
			}// propertyChange()
		};
		clientFrame.addPropertyChangeListener(ClientFrame.EVT_MODIFIED_STATE, modListener);
	}// PersistenceManager()
	
	/** Cleanup	*/
	public void close()
	{
		clientFrame.removePropertyChangeListener(modListener);
	}// close()
	
	/** 
	*	Threads added to this ThreadGroup will be joined() at exit
	* 	(with a pre-defined timeout) such that they will complete
	*	before jDip exits. 
	*	<p>
	*	This ensures that certain IO operations (e.g., Exports)
	*	will not be aborted at exit.
	*/
	public ThreadGroup getPMThreadGroup()
	{
		return persistTG;
	}// getPMThreadGroup()
	
	/** If any change has occured singe the last time we saved. */
	public boolean isChanged()
	{
		return isChanged;
	}// isChanged()
	
	/** Force an update of the game name / title bar */
	public void updateTitle()
	{
		setTitle();
	}// updateTitle()
	
	/** Exit from the program, after confirmation */
	public void exit()
	{
		if(confirmDialog())
		{
			GeneralPreferencePanel.saveWindowSettings(clientFrame);
			
			// shutdown Batik renderer. This should stop the occasional
			// IllegalComponentState exceptions when exiting during a 
			// render.
			if(clientFrame.getMapPanel() != null)
			{
				clientFrame.getMapPanel().close();
			}
			
			clientFrame.setVisible(false);
			
			// wait for any active threads in persistTG; if there are none,
			final int activeCount = persistTG.activeCount();
			Log.println("PM::exit(): threads pending: ", activeCount);
			
			final Thread[] pendingThreads = new Thread[activeCount];
			final int actualCount = persistTG.enumerate(pendingThreads);
			Log.println("PM::exit(): actual threads pending: ", actualCount);
			
			for(int i=0; i<pendingThreads.length; i++)
			{
				if(pendingThreads[i].isAlive())
				{
					try
					{
						Log.println("PM::exit(): waiting on ", pendingThreads[i].getName());
						pendingThreads[i].join(THREAD_WAIT);
						Log.println("    done.");
					}
					catch(Throwable t)
					{
						Log.println("PM::exit(): uncaught exception:");
						Log.println(t);
					}
				}
			}
			
			clientFrame.dispose();
			System.exit(0);
		}
	}// exit()
	
	
	/** Opens a World from the given File, after confirmation */
	public World open(File file)
	{
		World world = null;
		if(confirmDialog())
		{
			try
			{
				world = readGameFile(file);
			}
			catch(Exception e)
			{
				ErrorDialog.displayFileIO(clientFrame, e, file.toString());
			}
			
			openWorld(world, file);
			return world;
		}
		
		return world;
	}// open()
	
	
	/** Opens a world, displaying a FileChooser dialog. 
	* <br> Returns null if no file is chosen, or an error occurs.
	*/
	public World open()
	{
		if(confirmDialog())
		{
			// JFileChooser setup 
			XJFileChooser chooser = XJFileChooser.getXJFileChooser();
			chooser.addFileFilter(SimpleFileFilter.SAVE_GAME_FILTER);
			chooser.setFileFilter(SimpleFileFilter.SAVE_GAME_FILTER);
			chooser.setCurrentDirectory( GeneralPreferencePanel.getDefaultGameDir() );
			File file = chooser.displayOpen(clientFrame);
			XJFileChooser.dispose();
			
			// get file name
			if(file != null)
			{
				World world = null;
				
				try
				{
					world = readGameFile(file);
				}
				catch(Exception e)
				{
					ErrorDialog.displayFileIO(clientFrame, e, file.toString());
				}
				
				openWorld(world, file);
				return world;
			}
		}
		return null;
	}// open()
	
	
	/** 
	*	Basic operations performed whenever we read in a World. 
	*	if passed World is null, does nothing.
	*/
	private void openWorld(World world, File file)
	{
		if(world == null)
		{
			return;
		}
		
		fileName = file;
		setChanged(false);
		setSaveEnabled(true);
		setTitle(world);
		GeneralPreferencePanel.setRecentFileName(file);
		clientFrame.getClientMenu().setSelected(ClientMenu.EDIT_EDIT_MODE, false);
		clientFrame.getClientMenu().updateRecentFiles();
		
		// this is for compatibility
		// 
		if( !(world.getGameSetup() instanceof GUIGameSetup) )
		{
			// use a default game setup, if none exists
			// note in log file.
			Log.println("PM: no GuiGameSetup; creating a default.");
			world.setGameSetup(new DefaultGUIGameSetup());
		}
	}// openWorld()
	
	
	
	/** 
	*	Creates a new game (Displays New Game dialog), after 
	* 	confirmation 
	*/
	public World newGame() 	
	{
		if(confirmDialog())
		{
			World world = NewGameDialog.displayDialog(clientFrame);
			if(world != null)
			{
				fileName = null;
				setChanged(false);
				setSaveEnabled(true);
				clientFrame.getClientMenu().setSelected(ClientMenu.EDIT_EDIT_MODE, false);
				clientFrame.getClientMenu().setViewNamesNone();
				setTitle(world);
				
				// set GameSetup object
				world.setGameSetup(new DefaultGUIGameSetup());
				
				return world;
			}
		}
		
		return null;
	}// newGame()
	
	/** 
	*	Creates a new game (Displays New Game dialog), after 
	* 	confirmation 
	*/
	public World newF2FGame() 	
	{
		if(confirmDialog())
		{
			World world = NewGameDialog.displayDialog(
				clientFrame,
				Utils.getLocalString(NewGameDialog.TITLE_F2F),
				dip.misc.Help.HelpID.Dialog_NewF2f
			);
			
			if(world != null)
			{
				fileName = null;
				setChanged(false);
				setSaveEnabled(true);
				clientFrame.getClientMenu().setSelected(ClientMenu.EDIT_EDIT_MODE, false);
				clientFrame.getClientMenu().setViewNamesNone();
				setTitle(world);
				
				// set GameSetup object
				world.setGameSetup(new F2FGUIGameSetup());
				
				return world;
			}
		}
		
		return null;
	}// newGame()
	
	/** Saves the current world, if changes have occured. Performs a 
	* 'Save As' if no file has been set. Returns 'true' if saved ok. */
	public boolean save()
	{
		// only save if changes.
		if(clientFrame.getWorld() != null)
		{
			if(fileName == null)
			{
				return saveAs();
			}
			else if(isChanged())
			{
				return writeGameFile();
			}
		}
		
		return false;
	}// save()
	
	
	/** Save As: Saves the world after requesting for the filename. Returns 'true' if saved ok.*/
	public boolean saveAs()
	{
		if(clientFrame.getWorld() != null)
		{
			// JFileChooser setup 
			XJFileChooser chooser = XJFileChooser.getXJFileChooser();
			chooser.addFileFilter(SimpleFileFilter.SAVE_GAME_FILTER);
			chooser.setFileFilter(SimpleFileFilter.SAVE_GAME_FILTER);
			
			// set default save-game path
			chooser.setCurrentDirectory( GeneralPreferencePanel.getDefaultGameDir() );
			
			// set suggested save name
			chooser.setSelectedFile( new File(getSuggestedSaveName()) );
			
			// show dialog
			File file = chooser.displaySaveAs(clientFrame);
			XJFileChooser.dispose();
			
			// get file name
			if(file != null)
			{
				fileName = file;
				boolean returnValue = writeGameFile();
				setTitle(); // in case write fails; we have chosen the file name
				GeneralPreferencePanel.setRecentFileName(fileName);
				clientFrame.getClientMenu().updateRecentFiles();
				return returnValue;
			}
		}
		
		return false;
	}// saveAs()
	
	
	/** Saves the current file to a new file, without changing the currently open file or current state. */
	public void saveTo()
	{
		if(clientFrame.getWorld() != null)
		{
			// JFileChooser setup 
			XJFileChooser chooser = XJFileChooser.getXJFileChooser();
			chooser.addFileFilter(SimpleFileFilter.SAVE_GAME_FILTER);
			chooser.setFileFilter(SimpleFileFilter.SAVE_GAME_FILTER);
			chooser.setCurrentDirectory( GeneralPreferencePanel.getDefaultGameDir() );
			File file = chooser.displaySave(clientFrame, Utils.getLocalString(SAVE_TO_TITLE));
			XJFileChooser.dispose();
			
			// get file name
			if(file != null)
			{
				File saveToFile = file;
				
				try
				{
					World.save(saveToFile, clientFrame.getWorld());
					// DO NOT clear changed flag, though. 
					// Update recent file name list
					GeneralPreferencePanel.setRecentFileName(saveToFile);
					clientFrame.getClientMenu().updateRecentFiles();
				}
				catch(Exception e)
				{
					ErrorDialog.displayFileIO(clientFrame, e, saveToFile.toString());
				}
			}
		}
	}// saveTo()
	
	
	/** Lets the user choose the judge file to import */
	public World importJudge(World currentWorld)
	{
		// JFileChooser setup 
		XJFileChooser chooser = XJFileChooser.getXJFileChooser();
		chooser.addFileFilter(SimpleFileFilter.TXT_FILTER);
		chooser.setFileFilter(SimpleFileFilter.TXT_FILTER);
		chooser.setCurrentDirectory( GeneralPreferencePanel.getDefaultGameDir() );
		File file = chooser.displayOpen(clientFrame, Utils.getLocalString(IMPORT_CHOOSER_TITLE));
		XJFileChooser.dispose();
			
		// get file name
		if(file != null)
		{
			return importJudge(file, currentWorld);
		}
		return null;
	}// importJudge()
	
	
	/** 
	 *  Imports the given Judge file (no file requester dialog is displayed) 
	 *  Returns: null, if the current world has been updated, or a new world object
	 */
	public World importJudge(File file, World currentWorld)
	{
		World world = null;
			
			// TODO: operate on a backup up currentWorld, if everything is ok, update real currentWorld
			// reason for this: if we operate on the real world and something goes wrong,
			// then we have got a currupted world!
			try
			{
				JudgeImport ji = new JudgeImport(clientFrame.getGUIOrderFactory(), file, currentWorld);

				// Check if the results (if any) matched the current game,
				// otherwise diplay dialog and try again
				while ((ji.getResult() == JudgeImport.JI_RESULT_TRYREWIND) ||
					(ji.getResult() == JudgeImport.JI_RESULT_LOADOTHER)) 
				{
					String gameInfo = ji.getGameInfo();
					Phase phase = Phase.parse(gameInfo);
					
					if (ji.getResult() == JudgeImport.JI_RESULT_TRYREWIND)
					{
						// we need to rewind the current game
						if (rewindDialog(phase))
						{
							// rewind current game
							Iterator iter = currentWorld.getPhaseSet().iterator();
							LinkedList l = new LinkedList();
							while(iter.hasNext())
							{
								Phase p = (Phase) iter.next();
								if (p.compareTo(phase) > 0)
								{	
									l.add(p);
								}
							}
							while (!l.isEmpty())
							{
								Phase p = (Phase)l.getFirst();
								l.removeFirst();
								TurnState ts = currentWorld.getTurnState(p);
								currentWorld.removeTurnState( ts );
							}
							// clear orders for the last turnstate, because we have got the new orders
							currentWorld.getLastTurnState().clearAllOrders();
						}
						else
						{
							// abort.
							return world;
						}
					}
					else
					{
						// we need to load the correct game
						if (loadDialog(gameInfo))
						{
							World newWorld = open();
							if (newWorld != null)
							{
								clientFrame.createWorld(newWorld);
								currentWorld = clientFrame.getWorld();
							}
							else
							{
								// abort.
								return world;
							}
						}
						else
						{
							// abort.
							return world;
						}
					}
					
					// try again.
					ji = new JudgeImport(clientFrame.getGUIOrderFactory(), file, currentWorld);
				}
					
				if (ji.getResult() == JudgeImport.JI_RESULT_THISWORLD)
				{
					// show results (if desired)
					if(GeneralPreferencePanel.getShowResolutionResults())
					{
						final TurnState priorTS = clientFrame.getWorld().getPreviousTurnState(clientFrame.getWorld().getLastTurnState());
						ResultWriter.displayDialog(clientFrame, priorTS, clientFrame.getOFO());
					}
				}
				
				if (ji.getResult() == JudgeImport.JI_RESULT_NEWWORLD)
				{	
					if(confirmDialog())
					{
						world = ji.getWorld();
						fileName = null;
						setChanged(true);
						setSaveEnabled(true);
						clientFrame.getClientMenu().setSelected(ClientMenu.EDIT_EDIT_MODE, false);
						setTitle(world);
					}
				} else {
					// we modified the current world
					setChanged(true);
					clientFrame.getClientMenu().setSelected(ClientMenu.EDIT_EDIT_MODE, false);
					clientFrame.fireTurnstateChanged(currentWorld.getLastTurnState());
				}
				clientFrame.fireStateModified();
			}
			catch(Exception e)
			{
				ErrorDialog.displayFileIO(clientFrame, e, file.toString());
			}
			
		return world;
	}// importJudge()
	
	
	/** Imports a game from Floc.Net. User is prompted for required information. */
	public World importFloc()
	{
		World world = null;
		if(confirmDialog())
		{
			world = FlocImportDialog.displayDialog(clientFrame);
		}
		return world;
	}// importFloc()
	
	
	
	/**
	*	Given a file from a Drag operation, attempt to open it
	*	as a game file if it has an extension of SimpleFileFilter.SAVE_GAME_FILTER
	*	type. Otherwise, attempt to import it.
	*
	*/
	public World acceptDrag(File selectedFile, World currentWorld)
	{
		if(selectedFile.getPath().toLowerCase().endsWith("." + SimpleFileFilter.SAVE_GAME_FILTER.getExtension()))
		{
			return open(selectedFile);
		}
		else
		{
			return importJudge(selectedFile, currentWorld);
		}
	}// acceptDrag()
	
	
	
	// reads in a game file
	private World readGameFile(File file)
	throws Exception
	{
		World w = World.open(file);
		
		// check if variant is available; if not, inform user.
		World.VariantInfo vi = w.getVariantInfo();
		
		if(VariantManager.getVariant(vi.getVariantName(), vi.getVariantVersion()) == null)
		{
			Variant variant = VariantManager.getVariant(vi.getVariantName(), VariantManager.VERSION_NEWEST);
			if(variant == null)
			{
				// we don't have the variant AT ALL
				ErrorDialog.displayVariantNotAvailable(clientFrame, vi);
				return null;
			}
			else
			{
				// try most current version: HOWEVER, warn the user that it might not work
				ErrorDialog.displayVariantVersionMismatch(clientFrame, vi, variant.getVersion());
				vi.setVariantVersion( variant.getVersion() );
			}
		}
		
		return w;
	}// readGameFile()
	
	
	
	/** Serialize Game Data to disk. Performs synchronization. */
	private boolean writeGameFile()
	{
		try
		{
			World w = clientFrame.getWorld();
			
			Log.println("PM::writeGameFile(): saving GUIGameSetup");
			
			// notify the GameSetup object to update its
			// state
			GUIGameSetup ggs = (GUIGameSetup) w.getGameSetup();
			if(ggs != null)
			{
				ggs.save(clientFrame);
			}
			
			// save data, update saved flags
			Log.println("PM::writeGameFile(): saving world....");
			World.save(fileName, w);
			
			Log.println("PM::writeGameFile(): world saved ok.");
			setChanged(false);
			return true;
		}
		catch(Exception e)
		{
			ErrorDialog.displayFileIO(clientFrame, e, fileName.toString());
		}
		
		return false;
	}// writeGameFile()
	
	private void setTitle()
	{
		setTitle(null);
	}// setTitle()
	
	
	/** World object may not yet be available in ClientFrame; if not, can specify it here (or null) */
	private void setTitle(World localWorld)
	{
		StringBuffer title = new StringBuffer(128);
		title.append(ClientFrame.getProgramName());
		
		// if no file is open, we shouldn't display a gamename/filename
		if(localWorld != null || clientFrame.getWorld() != null)
		{
			// use local world, if not, use clientFrame world
			World world = (localWorld != null) ? localWorld : clientFrame.getWorld();
			
			// get game name
			// game name is optional; doesn't have to be the same as the file name
			String gameName = world.getGameMetadata().getGameName();
			gameName = (EMPTY.equals(gameName)) ? null : gameName;
			title.append(" - ");
			
			if(gameName != null)
			{
				title.append(gameName);
				title.append(" [");
			}
			
			// title
			if(fileName != null)
			{
				title.append(fileName.getName());
			}
			else
			{
				title.append(Utils.getLocalString(UNSAVED_NAME));
			}
			
			if(gameName != null)
			{
				title.append(']');
			}
			
			// changed flag
			if(Utils.isOSX())
			{
				// aqua-specific. Draws dot in close button.
				// http://developer.apple.com/qa/qa2001/qa1146.html
				clientFrame.getRootPane().putClientProperty(WINDOW_MODIFIED, Boolean.valueOf(isChanged));
			}
			else
			{
				if(isChanged)
				{
					title.append(' ');
					title.append( Utils.getLocalString(MODIFIED_INDICATOR) );
				}
			}
		}
		
		clientFrame.setTitle(title.toString());
	}// setTitle()
	
	
	private void setSaveEnabled(boolean value)
	{
		clientFrame.getClientMenu().setEnabled(ClientMenu.FILE_SAVE, value);
		clientFrame.getClientMenu().setEnabled(ClientMenu.FILE_SAVEAS, value);	
		clientFrame.getClientMenu().setEnabled(ClientMenu.FILE_SAVETO, value);	
	}// setSaveEnabled()
	
	
	// returns 'true' if can proceed (not cancelled, or after save)
	private boolean confirmDialog()
	{
		if(isChanged())
		{
			// per apple guidelines:
			// [don't save] ==big space=== [cancel] [save]
			// we will switch cancel/save to make it more like windows (cancel on right)
			Object[] dlgOptions = 
			{
				Utils.getLocalString(CONFIRM_BUTTON_DONTSAVE ),		// 0 
				Box.createRigidArea(new Dimension(25,5)),			// 1 
				Utils.getLocalString(CONFIRM_BUTTON_SAVE),			// 2 
				Utils.getLocalString(CONFIRM_BUTTON_CANCEL)			// 3 
			};
			
			String message = Utils.getText( Utils.getLocalString(CONFIRM_TEXT) );
			String title = Utils.getLocalString(CONFIRM_TITLE);
			
			int result = JOptionPane.showOptionDialog(clientFrame, message, title, 
							JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE,
							null, dlgOptions, dlgOptions[3]);
			
			// the result returned corresponds to 0-3, as specified in dlgOptions.
			// of course, option 1 (a spacer) cannot be returned.
			
			if(result == 2)
			{
				// save; however, if save is cancelled, cancel
				return save();
			}
			else
			{
				return !(result == 3 || result == JOptionPane.CLOSED_OPTION);
			}
		}
		
		return true;
	}// confirmDialog()
	
	private boolean rewindDialog(Phase phase)
	{
		Object[] dlgOptions = 
		{
			Utils.getLocalString(CONFIRM_BUTTON_REWIND),		// 0 
			Box.createRigidArea(new Dimension(25,5)),			// 1 
			Utils.getLocalString(CONFIRM_BUTTON_CANCEL)			// 2 
		};
		
		String message = Utils.getText( Utils.getLocalString(CONFIRM_REWIND_TEXT), phase.toString());
		String title = Utils.getLocalString(CONFIRM_REWIND_TITLE);
			
		int result = JOptionPane.showOptionDialog(clientFrame, message, title, 
						JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE,
						null, dlgOptions, dlgOptions[2]);

		// the result returned corresponds to 0-2, as specified in dlgOptions.
		// of course, option 1 (a spacer) cannot be returned.
			
		return (result == 0);
	}
	
	private boolean loadDialog(String gameInfo)
	{
		Object[] dlgOptions = 
		{
			Utils.getLocalString(CONFIRM_BUTTON_LOAD ),		// 0 
			Box.createRigidArea(new Dimension(25,5)),			// 1 
			Utils.getLocalString(CONFIRM_BUTTON_CANCEL)			// 2 
		};
		
		String message = Utils.getText( Utils.getLocalString(CONFIRM_LOAD_TEXT), gameInfo);
		String title = Utils.getLocalString(CONFIRM_LOAD_TITLE);
			
		int result = JOptionPane.showOptionDialog(clientFrame, message, title, 
						JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE,
						null, dlgOptions, dlgOptions[2]);

		// the result returned corresponds to 0-2, as specified in dlgOptions.
		// of course, option 1 (a spacer) cannot be returned.
			
		return (result == 0);
	}
	
	private void setChanged(boolean value)
	{
		isChanged = value;
		setTitle();	
	}// setChanged()	
	
	
	/**
	*	Gets a suggested filename for a saved game. This will be:<br>
	*		GameName								<br>
	*		VariantName				[no GameName]	<br>
	*		SaveFileName			[if exists]		<br>
	* 	No extension is appended.<br>
	*	Assumes current World/TurnState are not null.
	*/
	public String getSuggestedSaveName()
	{
		if(fileName == null)
		{
			// game name?
			String gameName = clientFrame.getWorld().getGameMetadata().getGameName();
			if(!EMPTY.equals(gameName) && gameName != null)
			{
				return gameName;
			}
			
			// use variant name
			//
			return clientFrame.getWorld().getVariantInfo().getVariantName();
		}
		else
		{
			return fileName.getName();
		}
	}// getSuggestedSaveName()
	
	
	/**
	*	Gets a suggested export name. This will be:
	*		SaveFileName + Phase								<br>
	*		GameName + Phase		[if no save file name]		<br>
	*		VariantName + Phase		[if no gamename]			<br>
	*	No extension is appended.<br>
	*	Assumes current World/TurnState are not null.
	*/
	public String getSuggestedExportName()
	{
		StringBuffer sb = new StringBuffer(64);
		
		// get prefix
		sb.append( getSuggestedSaveName() );
		
		// remove trailing extension from suggested name, if any
		int idx = sb.lastIndexOf("."+SimpleFileFilter.SAVE_GAME_FILTER.getExtension());
		if(idx >= 0)
		{
			sb.replace(idx, sb.length(), "");
		}
		
		// append brief phase name
		sb.append('-');
		sb.append( clientFrame.getTurnState().getPhase().getBriefName() );
		
		return sb.toString();
	}// getSuggestedExportName()
	
}// class PersistenceManager
