//
//  @(#)XMLUtils.java		12/2003
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

import org.w3c.dom.*;


/**
*	Various static utilities used to parse and
*	format XML (and SVG). None of these methods, though,
*	are SVG-specific.
*	<p>
*	The duplicate and similar XML methods in other classes should gradually
*	be added to here.
*/
public class XMLUtils 
{
	/** Private constructor. */
	private XMLUtils() {}
	
	
	/**
	*	Find the first child Element matching the given tag name. Only searches 
	*	1st-level children; not a recursive search. Null if not found.
	*/
	public static Element findChildElementMatching(Node root, String tagName)
	{
		return (Element) findChildNodeMatching(root, tagName, Node.ELEMENT_NODE);
	}// findChildMatching()
	
	
	/**
	*	Find the first child node matching the given tag name. Only searches 
	*	1st-level children; not a recursive search. Null if not found.
	*/
	public static Node findChildNodeMatching(Node root, String tagName, short nodeType)
	{
		Node childNode = root.getFirstChild();
		while(childNode != null)
		{
			if( childNode.getNodeType() == nodeType )
			{
				if(tagName.equals(childNode.getNodeName()))
				{
					return childNode;
				}
			}
			
			childNode = childNode.getNextSibling();
		}
		
		return null;
	}// findChildMatching()
	
	
	
}// class XMLUtils
