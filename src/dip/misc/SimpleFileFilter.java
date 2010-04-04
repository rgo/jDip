//
//  @(#)SimpleFileFilter.java	3/2003
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
package dip.misc;

import java.io.File;
import javax.swing.filechooser.FileFilter;

/**
*	Simple File Filter class.
*	<p>
*	Filters a single file extension. Directories are not filtered out.
*	Extensions are case-insensitively matched.
*
*/
public class SimpleFileFilter extends FileFilter
{
	// commonly used file filters
	/** Save Game file filter (typically, ".jdip") */
	public static final SimpleFileFilter SAVE_GAME_FILTER = 
			new SimpleFileFilter(	Utils.getLocalString("SimpleFileFilter.savegame.extension"), 
									Utils.getLocalString("SimpleFileFilter.savegame.description") );
	
	/** JPEG file filter */
	public static final SimpleFileFilter JPG_FILTER = new SimpleFileFilter("jpg", 
			Utils.getLocalString("SimpleFileFilter.jpg.description") );
	
	/** PNG file filter */
	public static final SimpleFileFilter PNG_FILTER = new SimpleFileFilter("png",
			Utils.getLocalString("SimpleFileFilter.png.description") );
	
	/** SVG file filter */
	public static final SimpleFileFilter SVG_FILTER = new SimpleFileFilter("svg",
			Utils.getLocalString("SimpleFileFilter.svg.description") );
	
	/** PDF file filter */
	public static final SimpleFileFilter PDF_FILTER = new SimpleFileFilter("pdf",
			Utils.getLocalString("SimpleFileFilter.pdf.description") );
	
	/** XML file filter */
	public static final SimpleFileFilter XML_FILTER = new SimpleFileFilter("xml",
			Utils.getLocalString("SimpleFileFilter.xml.description") );
	
	/** HTML file filter */
	public static final SimpleFileFilter HTML_FILTER = new SimpleFileFilter("html",
			Utils.getLocalString("SimpleFileFilter.html.description") );
	
	/** TXT (plain text) file filter */
	public static final SimpleFileFilter TXT_FILTER = new SimpleFileFilter("txt",
			Utils.getLocalString("SimpleFileFilter.txt.description") );
	
	
	// instance variables
	private final String ext;
	private final String description;
	private final String dottedExt;
	
	/** 
	*	Create a SimpleFileFilter
	*	<p>
	*	Note: Extension should not contain a period. Thus "jdip" is valid, 
	*	but ".jdip" is an invalid extension. 
	*/
	public SimpleFileFilter(String extension, String description)
	{
		if(extension == null || description == null || extension.length() == 0)
		{
			throw new IllegalArgumentException();
		}
		
		if(extension.charAt(0) == '.')
		{
			throw new IllegalArgumentException("extension must not start with a '.'");
		}
		
		this.ext = extension;
		this.dottedExt = '.' + extension.toLowerCase();
		this.description = description;
	}// SimpleFileFilter()
	
	/** Get the Description provided */
	public String getDescription()
	{
		return description;
	}// getDescription()
	
	/** Get the Extension provided */
	public String getExtension()
	{
		return ext;
	}// getExtension()
	
	/** Implementation of FileFilter */
	public boolean accept(File f)
	{
		if(f != null) 
		{
			if(f.isDirectory())
			{
				return true;
			}
			
			return isMatch(f);
		}
		return false;
	}// accept()
	
	/** Checks if user added extension; if not, the extension is added. */
	public File appendExtension(File file)
	{
		if( !ext.equalsIgnoreCase(getExtension(file)) )
		{
			return new File(file.getAbsolutePath() + '.' + ext);
		}
		
		return file;
	}// appendExtension()
	
	/** 
	* 	Gets the extension from a given file. Does not include last ".". 
	*	Returns an empty string ("") if no extension found.
	*/
	public static String getExtension(File file)
	{
		final String filename = file.getName();
		final int idx = filename.lastIndexOf('.');
		
		if(idx > 0 && idx < filename.length()-1) 
		{
			return filename.substring(idx+1);
		}
		
		return "";
	}// getExtension()
	
	
	/** case-insensitive extension match to dotted extension */
	private boolean isMatch(File file)
	{
		final String filename = file.getName();
		final int fnLen = filename.length();
		final int dextLen = dottedExt.length();
		
		if(fnLen <= dextLen)
		{
			return false;
		}
		
		int count = fnLen - 1;
		for(int i=dextLen-1; i>=0; i--)
		{
			if(Character.toLowerCase(filename.charAt(count)) != dottedExt.charAt(i))
			{
				return false;
			}
			
			count--;
		}
		
		return true;
	}// file()
}// inner class SimpleFileFilter

