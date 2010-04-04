//
//  @(#)Tool.java	1.00	9/2002
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

import javax.swing.JMenuItem;
import java.net.URI;

public interface Tool
{
	
	// Versioning Methods
	/** Get the current Tool version */
	public float getVersion();
	/** Get the Tool Copyright Information (authors, etc.). Never should return null. */
	public String getCopyrightInfo();
	/** Get the Tool Web URI (web address, ftp address, etc.). Never should return null. */
	public URI	getWebURI();
	/** Get the Email addresses. Never should return null. */
	public URI[] getEmailURIs();
	/** Get the Tool comment. Never should return null. */
	public String getComment();
	/** Get the Tool Description. Never should return null. */
	public String getDescription();
	/** Get the Tool name. Never should return null. */
	public String getName();
	
	// registration methods
	/** Creates a JMenuItem (or JMenu for sub-items) */
	public JMenuItem registerJMenuItem();
					
	/** Gets the ToolProxy object which allows a Tool access to internal data structures */
	public void setToolProxy(ToolProxy toolProxy);
	
	
}// interface Tool
