//
//  @(#)GUIExceptionHandler.java	2/2003
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
package dip.gui;

import dip.gui.dialog.ErrorDialog;

/**
*	Handles uncaught exceptions from an AWT event thread. Note that this class does NOT handle
*	uncaught exceptions from non-AWT event threads!
*	<p>
*	This is an effective but nonportable (it will not work on non-Sun JVMs, 
*	and may not work in future Java versions) of handling uncaught exceptions.
*	<p>
*	This code was derived from <a href="http://forum.java.sun.com/thread.jsp?forum=52&thread=92316">
*	a Java Developer Forum</a> post.
*	<h3>Usage</h3>
*	At the beginning of your application set the System property
*	to the name of your exception handler class:<br>
	<pre>
	System.setProperty("sun.awt.exception.handler" ,"dip.gui.GUIExceptionHandler");
	</pre>
*	Alternatively, this can be done transparently with the registerHandler() method.
*	
*	<p>
*	When an uncaught exception occurs in the EventDispatchThread, it
*	will check the value of "sun.awt.exception.handler", create an
*	instance of the class and call its <code>handle</code> method.
*	
*	
*/
public class GUIExceptionHandler
{
	
	/** Default Constructor */
	public GUIExceptionHandler()
	{
	}// GUIExceptionHandler()
	
	/**
	*	Registers this GUIExceptionHandler for uncaught Exceptions.
	*	This will return <code>false</code> if the handler could 
	*	not be registered.
	*/
	public static boolean registerHandler()
	{
		try
		{
			System.setProperty("sun.awt.exception.handler", GUIExceptionHandler.class.getName());
			return true;
		}
		catch(Exception e)
		{
			return false;
		}
	}// registerHandler()
	
	
	/**
	*	Handles the thrown Exception, from an AWT event thread
	*
	*/
	public void handle(Throwable thrown)
	{
		ErrorDialog.displaySerious(null, thrown);
	}// handle()
	
	
}// class GUIExceptionHandler

