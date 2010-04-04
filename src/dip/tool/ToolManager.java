//
//  @(#)ToolManager.java	1.00	9/2002
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
package dip.tool;

import dip.misc.Log;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.*;
import java.util.*;
import java.util.jar.*;

/**
*
*	Manages Tool plugins.
*
*
*/
public class ToolManager
{
	// constants
	private static final String TOOL_EXT_JAR = "Tool.jar";
	
	// class variables
	private static ToolManager tm = null;
	
	// instance variables
	private URLClassLoader toolClassLoader = null;
	private Tool[] tools = new Tool[0];
	
	/** 
	*	Initialize the ToolManager. No other methods are guaranteed to work
	*	until the ToolManager singleton has been initialized.
	*/
	public static synchronized void init(File[] searchPaths)
	{
		tm = new ToolManager();
		
		// search for Tools
		final File[] foundToolFiles = tm.searchForFiles(searchPaths);	// no null entries
		URL[] foundToolURLs = new URL[foundToolFiles.length];			// entries will be null if invalid
		String[] mainClassNames = new String[foundToolURLs.length];		// entries will be null if invalid
		
		for(int i=0; i<foundToolFiles.length; i++)
		{
			// defaults
			foundToolURLs[i] = null;
			mainClassNames[i] = null;
			
			// attempt file-to-URL conversion
			try
			{
                                /*
                                 * NOTE-RGO: "toURL" is deprecated I change it to ".toURI().toURL()" instead
                                 */
				foundToolURLs[i] = foundToolFiles[i].toURI().toURL();
			}
			catch(java.net.MalformedURLException e)
			{
				Log.println("ERROR: ToolManager: could not convert to URL: ", foundToolFiles[i]);
			}
			
			// do not attempt if URL is null.
			if(foundToolURLs[i] != null)
			{
				try
				{
					JarFile jarFile = new JarFile(foundToolFiles[i], true, JarFile.OPEN_READ);
					Manifest manifest = jarFile.getManifest();
					Attributes attr = manifest.getMainAttributes();
					mainClassNames[i] = attr.getValue(Attributes.Name.MAIN_CLASS);
					jarFile.close();
				}
				catch(IOException e)
				{
					mainClassNames[i] = null;
					Log.println("ERROR: ToolManager: could not find main-class attribute in manifest for tool: ", 
						foundToolFiles[i], "; reason: ", e.getMessage());
				}
			}
		}
		
		tm.toolClassLoader = new URLClassLoader(foundToolURLs);
		
		
		// for each Tool, attempt to load its main class (same as the file name, without the 
		// file extension) and add it to the Tool array
		ArrayList list = new ArrayList();
		for(int i=0; i<foundToolURLs.length; i++)
		{
			if(mainClassNames[i] != null && foundToolURLs[i] != null)
			{
				try
				{
					Tool tool = (Tool) tm.toolClassLoader.loadClass(mainClassNames[i]).newInstance();
					list.add(tool);
				}
				catch(Throwable e)
				{
					Log.println("ERROR: loading Tool: "+e);
				}
			}
		}
		
		tm.tools = (Tool[]) list.toArray(new Tool[list.size()]);
	}// init()
	
	
	/** Returns all Tool objects loaded. Never returns null. */
	public static synchronized Tool[] getTools()
	{
		checkTM();
		return tm.tools;
	}// getTools()
	
	
	/** Ensures that we have initialized the ToolManager */
	private static void checkTM()
	{
		if(tm == null)
		{
			throw new IllegalArgumentException("not initialized");
		}
	}// checkTM()
	
	
	/** Searches the paths for plugins, and returns the URL to each. */
	private File[] searchForFiles(File[] searchPaths)
	{
		List fileList = new ArrayList();
		
		for(int spIdx=0; spIdx<searchPaths.length; spIdx++)
		{
			Log.println("Searching for tools on: ", searchPaths[spIdx]);
			File[] list = searchPaths[spIdx].listFiles();
			if(list != null)
			{
				for(int i=0; i<list.length; i++)
				{
					if(list[i].isFile())
					{
						String fileName = list[i].getPath();
						if(fileName.endsWith(TOOL_EXT_JAR))
						{
							Log.println("found tool: ", list[i]);				
							fileList.add(list[i]);
						}
					}
				}
			}
		}
		
		return (File[]) fileList.toArray(new File[fileList.size()]);
	}// searchForFiles()
	
	/** (Singleton) Constructor */
	private ToolManager()
	{
	}// ToolManager()
}// class ToolManager






