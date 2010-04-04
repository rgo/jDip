/*
*  @(#)OrderWarning.java	1.00	4/1/2002
*
*  Copyright 2002 Zachary DelProposto. All rights reserved.
*  Use is subject to license terms.
*/
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
//  Or from http://www.gnu.org/package dip.order.result;
//
package dip.order;


/**
*
*	This is a special type of OrderException that is given
*	for possibly ambiguous orders. It must be used judiciously,
*	because it does *not* have the same meaning as an OrderException.
*	<p>
*	Rules:<br>
*	<ul>
*	<li>OrderWarnings can ONLY be thrown during a validate()
*	<li>OrderWarnings, if thrown, must be the LAST exception (and only)
*		thrown by validate. In other words, the order must validate
*		without throwing an OrderException; only then can an orderWarning
*		be thrown
*	<li>OrderWarnings do <b>not</b> indicate invalid orders, but merely ambiguous
*		orders. therefore, the order can still be valid, but the user
*		should be informed. If they are not handled specially, then
*		they are treated as OrderExceptions.
*	</ul>
*
*
*/
public class OrderWarning extends OrderException
{
	
	/** Create an OrderWarning. */
	public OrderWarning(String text)
	{
		super(text);
	}// OrderWarning()
	
	/** Create an OrderWarning. */
	public OrderWarning(Order order, String text)
	{
		super(order, text);
	}// OrderWarning()
	
}// class OrderWarning
