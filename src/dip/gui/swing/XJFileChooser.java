//
//  @(#)XJFileChooser.java		7/2003
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
package dip.gui.swing;

import dip.misc.Utils;
import dip.misc.SimpleFileFilter;
import dip.misc.Log;

import java.awt.Component;

import java.io.File;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

/**
*	A simplified and extended JFileChooser for single-file (only!)
*	selections. It is also cached, so that it displays faster.
*	<p>
*	SimpleFileFilter support is integrated, so that file extensions
*	are automatically appended (unless "all files" is selected). 
*	<p>
*	Futhermore, if saving a file, checking is done to determine
*	if a file will be overwritten; if so, a confirmation dialog
*	is displayed.
*	<p>
*/
public class XJFileChooser
{
	// constants
	private static final String OVERWRITE_TEXT = "XJFileChooser.dialog.overwrite.text.location";
	private static final String OVERWRITE_TITLE = "XJFileChooser.dialog.overwrite.title";
	
	public static final String BTN_DIR_SELECT = "XJFileChooser.button.dir.select";
	private static final String TITLE_SAVE_AS = "XJFileChooser.title.saveas"; 
	
	
	/* 
	//simple test...
	public static void main(String[] dsf)
	throws Exception
	{
		System.out.println("hello");
		
		XJFileChooser c = XJFileChooser.getXJFileChooser();
		c.addFileFilter(SimpleFileFilter.JPG_FILTER);
		System.out.println(c.displayOpen(null));
		c.dispose();
		
		c = XJFileChooser.getXJFileChooser();
		c.addFileFilter(SimpleFileFilter.PDF_FILTER);
		c.addFileFilter(SimpleFileFilter.TXT_FILTER);
		System.out.println(c.displaySave(null));
		c.dispose();

		c = XJFileChooser.getXJFileChooser();
		c.addFileFilter(SimpleFileFilter.PNG_FILTER);
		c.addFileFilter(SimpleFileFilter.TXT_FILTER);
		System.out.println(c.displaySaveAs(null));
		c.dispose();
		
		// this shoudl fail (no get() called)
		c.displaySaveAs(null);
		// this should fail
		c.displaySaveAs(null);
	}
	*/
	
	// class variables
	private static XJFileChooser instance = null;
	private static SwingWorker loader = null;
	private static int refcount = 0;
	
	// instance variables
	private final CheckedJFileChooser chooser;
	
	/** Constructor */
	private XJFileChooser()
	{
		chooser = new CheckedJFileChooser();
		chooser.setAcceptAllFileFilterUsed(true);
	}// XJFileChooser()
	
	
	/**
	*	Can be used to initialize the XJFileChooser when called
	*	to provide faster response later
	*/
	public static synchronized void init()
	{
		if(instance == null)
		{
			if(loader == null)
			{
				loader = new SwingWorker()
				{
					public Object construct()
					{
						long time = System.currentTimeMillis();
						XJFileChooser xjf = new XJFileChooser();
						Log.printTimed(time, "XJFileChooser construct() complete: ");
						return xjf;
					}// construct()
				};
				
				loader.start(Thread.MIN_PRIORITY);
			}
			else
			{
				instance = (XJFileChooser) loader.get();
				instance = null;
			}
		}
	}// init()
	
	/**
	*	Gets the XJFileChooser (only one exists -- this must be 
	*	enforced by you!). 
	*	<p>
	*	The file filters are reset when this is called, which 
	*	means that (usually) only the AcceptAll file filter 
	*	(which is not a SimpleFileFilter) remains.
	*/
	public static synchronized XJFileChooser getXJFileChooser()
	{
		if(instance == null)
		{
			instance = new XJFileChooser();
		}
		
		refcount++;
		if(refcount > 1)
		{
			throw new IllegalStateException("cannot re-use getXJFileChooser()");
		}
		
		instance.reset();
		return instance;
	}// getXJFileChooser()
	
	
	/**
	*	Disposes the in-use XJFileChooser. This should ALWAYS be called
	*	after a display() method has been called. The number of dispose()
	*	and getXJFileChooser() methods should be balanced.
	*/
	public static synchronized void dispose()
	{
		refcount--;
		if(refcount < 0)
		{
			throw new IllegalStateException("XJFileChooser too many dispose() calls");
		}
	}// dispose()
	
	
	/**
	*	Adds a SimpleFileFilter to the list of available
	*	file filters.
	*/
	public void addFileFilter(SimpleFileFilter filter)
	{
		chooser.addChoosableFileFilter(filter);
	}// addFileFilter()
	
	/**
	*	Sets the default file filter. If null, sets the
	*	'accept all' file filter.
	*/
	public void setFileFilter(SimpleFileFilter filter)
	{
		if(filter != null)
		{
			chooser.setFileFilter(filter);
		}
		else
		{
			chooser.setFileFilter(chooser.getAcceptAllFileFilter());
		}
	}// addFileFilter()
	
	/**
	*	Set the current directory. May be set to null ('home' directory)
	*/
	public void setCurrentDirectory(File file)
	{
		chooser.setCurrentDirectory(file);
	}// setCurrentDirectory()
	
	/**
	*	Sets the suggested file name. By default, no file name
	*	is suggested.
	*/
	public void setSelectedFile(File file)
	{
		if(file == null)
		{
			chooser.setSelectedFile(new File(""));
		}
		else
		{
			chooser.setSelectedFile(file);
		}
	}// setSuggestedFileName()
	
	/**
	*	Display the file chooser, with the given title and Accept button
	*	text. No file filters are added. The type (JFileChooser.OPEN_DIALOG or
	*	SAVE_DIALOG) must be specified. If the acceptButtonText and/or title is null, the default
	*	button text (for OPEN_DIALOG or SAVE_DIALOG) and/or title is used.
	*
	*	@return the selected File, or null
	*/
	public File display(Component parent, String title, String acceptButtonText, int type, int mode)
	{
		synchronized(XJFileChooser.class)
		{
			if(refcount != 1)
			{
				throw new IllegalStateException("dipose / get not balanced");
			}
		}
		
		if(type != JFileChooser.OPEN_DIALOG && type != JFileChooser.SAVE_DIALOG)
		{
			throw new IllegalArgumentException("invalid type");
		}
		
		chooser.setDialogType(type);
		
		if(acceptButtonText != null)
		{
			chooser.setApproveButtonText(acceptButtonText);
		}
		
		if(title != null)
		{
			chooser.setDialogTitle(title);
		}
		
		chooser.setFileSelectionMode(mode);
		
		if(chooser.showDialog(parent, null) == JFileChooser.APPROVE_OPTION)
		{
			if(chooser.getDialogType() != JFileChooser.OPEN_DIALOG)
			{
				return fixFileExtension(chooser.getFileFilter(), chooser.getSelectedFile());
			}
			else
			{
				return chooser.getSelectedFile();
			}
		}
		
		return null;
	}// display()
	
	
	/** Appends an extension, if appropriate */
	private File fixFileExtension(FileFilter ff, File file)
	{
		if(ff instanceof SimpleFileFilter)
		{
			return ((SimpleFileFilter) ff).appendExtension(file);
		}
		
		return file;
	}// fixFileExtension()
	
	
	/**
	*	The typical "Open" dialog. No filters are added.
	*
	*	@return the selected File, or null
	*/
	public File displayOpen(Component parent)
	{
		return display(parent, null, null, JFileChooser.OPEN_DIALOG, JFileChooser.FILES_ONLY);
	}// displayOpen()
	
	/**
	*	The typical "Open" dialog. No filters are added. 
	*	A title may be specified.
	*
	*	@return the selected File, or null
	*/
	public File displayOpen(Component parent, String title)
	{
		return display(parent, title, null, JFileChooser.OPEN_DIALOG, JFileChooser.FILES_ONLY);
	}// displayOpen()
	
	
	/**
	*	The typical "Save" dialog. No filters are added.
	*
	*	@return the selected File, or null
	*/
	public File displaySave(Component parent)
	{
		return display(parent, null, null, JFileChooser.SAVE_DIALOG, JFileChooser.FILES_ONLY);
	}// displaySave()
	
	/**
	*	The typical "Save" dialog. No filters are added.
	*	A different title may be specified.
	*	
	*	@return the selected File, or null
	*/
	public File displaySave(Component parent, String title)
	{
		return display(parent, title, null, JFileChooser.SAVE_DIALOG, JFileChooser.FILES_ONLY);
	}// displaySave()
	
	
	/**
	*	The typical "Save As" dialog. No filters are added.
	*
	*	@return the selected File, or null
	*/
	public File displaySaveAs(Component parent)
	{
		final String title = Utils.getLocalString(TITLE_SAVE_AS);
		return display(parent, title, null, JFileChooser.SAVE_DIALOG, JFileChooser.FILES_ONLY);
	}// displaySaveAs()
	
	/**
	*	Display a filechooser that only allows the selection
	*	of a single directory.
	*
	*	@return the selected directory, or null
	*/
	public File displaySelectDir(Component parent, String title)
	{
		final String selectText = Utils.getLocalString(BTN_DIR_SELECT);
		return display(parent, title, selectText, JFileChooser.OPEN_DIALOG, JFileChooser.DIRECTORIES_ONLY);
	}// displaySelectDir()
	
	
	
	
	
	/**
	*	resets the XJFileChooser to its default state
	*
	*/
	private void reset()
	{
		chooser.setSelectedFile(new File(""));
		chooser.resetChoosableFileFilters();
		chooser.setMultiSelectionEnabled(false);
		chooser.setCurrentDirectory(null);
	}// reset()
	
	
	
	/** 
	*	Extends JFileChooser; displays a confirmation popup
	*	if we are a SAVE dialog, and the file already exists.
	*	This prevents users from accidentally overwriting 	
	*	files.
	*
	*/
	private class CheckedJFileChooser extends JFileChooser
	{
		public CheckedJFileChooser()
		{
			super((File) null);
		}
		
		
		/** Override to check for overwrite confirmation */
		public void approveSelection() 
		{
			if(getDialogType() != JFileChooser.OPEN_DIALOG)
			{
				File selectedFile = fixFileExtension(chooser.getFileFilter(), chooser.getSelectedFile());
				if(selectedFile != null)
				{
					if(	selectedFile.exists() )
					{
						String message = Utils.getText(Utils.getLocalString(OVERWRITE_TEXT), selectedFile.getName());
						
						int result = JOptionPane.showConfirmDialog(getParent(), 
										message,
										Utils.getLocalString(OVERWRITE_TITLE),
										JOptionPane.YES_NO_OPTION );
						
						if(result != JOptionPane.YES_OPTION)
						{
							cancelSelection();
							return;
						}
						
						// fall thru
					}
				}
			}
			
			super.approveSelection();
		}// approveSelection()
		
		
	}// inner class CheckedJFileChooser
	
	
	
}// class XJFileChooser

