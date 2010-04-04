//
//  @(#)MapPanel.java		4/2002
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


public class MapException extends java.lang.Exception
{
	/** Constructs a new exception with null as its detail message. */
	public MapException()
	{
		super();
	}// MapException()
	
	/** Constructs a new exception with the specified detail message. */
	public MapException(String message)
	{
		super(message);
	}// MapException()
	
	/**  Constructs a new exception with the specified detail message and cause. */
	public MapException(String message, Throwable cause)
	{
		super(message, cause);
	}// MapException()
	
	/** 
	*	Constructs a new exception with the specified cause and a detail message of 
	*	(cause==null ? null : cause.toString()) (which typically contains the class 
	*	and detail message of cause).
	*/
	public MapException(Throwable cause)
	{
		super(cause);
	}// MapException()
	
}// class MapException()
