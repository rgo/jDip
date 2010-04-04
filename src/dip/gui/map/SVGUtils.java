//
//  @(#)SVGUtils.java	1.00	4/1/2002
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

import dip.world.Province;

import com.dautelle.util.TypeFormat;

import org.apache.batik.swing.svg.JSVGComponent;
import org.apache.batik.swing.JSVGCanvas;
import org.apache.batik.util.SVGConstants;
import org.apache.batik.util.RunnableQueue;
import org.apache.batik.bridge.UpdateManager;

import org.apache.batik.dom.util.XLinkSupport;
import org.apache.batik.dom.svg.SVGDOMImplementation;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.svg.SVGDocument;
import org.w3c.dom.svg.SVGElement;
import org.w3c.dom.svg.SVGUseElement;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.awt.geom.AffineTransform;
import java.awt.*;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.swing.*;



/**
*	Assorted utilities for altering the Batik SVG DOM.
*	<p>
*	The add/remove/replace methods are not only thread safe but also 
*	follow Batik guidelines for altering the DOM. If the SVGComponent 
*	has been set to dynamic prior to loading of the SVG document, then 
*	any DOM change will be rendered automatically.
*	<p>
*	Also note that for the title/descriptions setting methods, it would be 
*	faster to create a Title or Description SVG element and use add / remove / 
*	replace as appropriate. This could be an issue if there are frequent title 
*	or description changes.
*
*/
public class SVGUtils
{
	
	/** Default floating-point format precision */
	private final static float FLOAT_PRECISION = 0.1f;

	/**
	*	Sets the title of an SVG element.
	*	<p>
	*	a null title is not valid.
	*/
	public static void setTitle(SVGDocument document, SVGElement element, String title)
	{
		Node titleNode = null;
		Node textNode = null;
		
		NodeList nl = element.getChildNodes();
		for(int i=0; i<nl.getLength(); i++)
		{
			Node node = nl.item(i);
			if(node.getNodeName() == SVGConstants.SVG_TITLE_TAG)
			{
				titleNode = node;
				textNode = titleNode.getFirstChild();
				break;
			}
		}
		
		if(titleNode == null)
		{
			titleNode = document.createElementNS(SVGDOMImplementation.SVG_NAMESPACE_URI, SVGConstants.SVG_TITLE_TAG);
			element.appendChild(titleNode);
			
			textNode = document.createTextNode(title); 
			titleNode.appendChild(textNode);
		}
		else
		{
			textNode.setNodeValue(title);
		}
	}// setTitle()
	
	
	
	/**
	*	Sets the description of an SVG element.
	*	<p>
	*	A null description is not valid.
	*/
	public static void setDescription(SVGDocument document, SVGElement element, String description)
	{
		Node descNode = null;
		Node textNode = null;
		
		NodeList nl = element.getChildNodes();
		for(int i=0; i<nl.getLength(); i++)
		{
			Node node = nl.item(i);
			if(node.getNodeName() == SVGConstants.SVG_TITLE_TAG)
			{
				descNode = node;
				textNode = descNode.getFirstChild();
				break;
			}
		}
		
		if(descNode == null)
		{
			descNode = document.createElementNS(SVGDOMImplementation.SVG_NAMESPACE_URI, SVGConstants.SVG_DESC_TAG);
			element.appendChild(descNode);
			
			textNode = document.createTextNode(description); 
			descNode.appendChild(textNode);
		}
		else
		{
			textNode.setNodeValue(description);
		}
	}// setDescription()
	
	
	
	
	/**
	*	Creates an SVG &lt;use&gt; element.
	*	<p>
	*	Allows insertion of a previously-defined &lt;symbol&gt; element.
	*	If id is null, no ID is used. If style is null, no style is used.
	*	<p>
	*	x,y are required.<br>
	*	SymbolSize will specify the width/height; if null, no width/height will be specified.
	*	<p>
	*	
	*/
	public static SVGUseElement createUseElement(SVGDocument document, String symbolName, String id, String attClass, 
											  float x, float y, MapMetadata.SymbolSize symbolSize)
	{
		// prepend '#' to name, if required
		if(symbolName.charAt(0) != '#')
		{
			StringBuffer sb = new StringBuffer(symbolName.length() + 1);
			sb.append('#');
			sb.append(symbolName);
			symbolName = sb.toString();
		}
		
		SVGUseElement useElement = (SVGUseElement) document.createElementNS(SVGDOMImplementation.SVG_NAMESPACE_URI, 
																		SVGConstants.SVG_USE_TAG);
		useElement.setAttributeNS(null, SVGConstants.SVG_X_ATTRIBUTE, floatToString(x));
		useElement.setAttributeNS(null, SVGConstants.SVG_Y_ATTRIBUTE, floatToString(y));
		useElement.setAttributeNS(XLinkSupport.XLINK_NAMESPACE_URI, SVGConstants.SVG_HREF_ATTRIBUTE, symbolName);	
		
		if(symbolSize != null)
		{
			useElement.setAttributeNS(null, SVGConstants.SVG_WIDTH_ATTRIBUTE, symbolSize.getWidth());
			useElement.setAttributeNS(null, SVGConstants.SVG_HEIGHT_ATTRIBUTE, symbolSize.getHeight());
		}
		
		if(id != null)
		{
			useElement.setAttributeNS(null, SVGConstants.SVG_ID_ATTRIBUTE, id);
		}
		
		if(attClass != null)
		{
			useElement.setAttributeNS(null, SVGConstants.SVG_CLASS_ATTRIBUTE, attClass);
		}
		
		return useElement;
	}// createUseElement()
	
	
	/*
	*	Adds an SVG element underneath parent element.
	*	<p>
	*	This does NOT check to see if the same element has already been added.
	*	<p>
	*	This method is threadsafe.
	public static void addSVGElement(JSVGComponent svgComponent, final SVGElement parent, final SVGElement child)
	{
		RunnableQueue rq = svgComponent.getUpdateManager().getUpdateRunnableQueue();
		rq.invokeLater(new Runnable() { 
			public void run() 
			{
				parent.appendChild(child);
			}// run()
		});
	}// addSVGElement()
	*/
	
	
	
	/*
	*	Removes the specific SVG element.
	*	<p>
	*	This method is threadsafe.
	public static void removeSVGElement(JSVGComponent svgComponent, final SVGElement element)
	{
		RunnableQueue rq = svgComponent.getUpdateManager().getUpdateRunnableQueue();
		rq.invokeLater(new Runnable() { 
			public void run() 
			{
				element.getParentNode().removeChild(element);
			}// run()
		});
	}// removeSVGElement()
	*/
	
	
	/*
	*	Replaces a specific SVG element with a new element
	*	<p>
	*	This method is threadsafe.
	public static void replaceSVGElement(JSVGComponent svgComponent, final SVGElement oldElement, final SVGElement newElement)
	{
		RunnableQueue rq = svgComponent.getUpdateManager().getUpdateRunnableQueue();
		rq.invokeLater(new Runnable() { 
			public void run() 
			{
				oldElement.getParentNode().replaceChild(newElement, oldElement);
			}// run()
		});
	}// replaceSVGElement()
	*/
	
	
	/**
	*	Given a list of objects, finds the first SVG tag that 
	*	has an ID that matches the object. That element is stored in the 
	*	returned HashMap, and can be retreived via the Object key
	*	<p>
	*	Null objects are ignored.
	*	<p>
	*	Supported Objects in the looklist are:
	*	<ul>
	*		<li>String
	*		<li>Object (via toString() method)
	*		<li>Province (checks all short names via getShortNames())
	*	</ul>
	*
	*/
	public static Map tagFinderSVG(List lookList, Node root)
	{
		return tagFinderSVG(lookList, root, false);
	}// tagFinderSVG	
	
	/** As above, but allows any SVG element to be returned */
	public static Map tagFinderSVG(List lookList, Node root, boolean anySVGElement)
	{
		List list = new ArrayList(lookList);
		Map map = new HashMap( (4 * lookList.size())/3 );
		
		// recursively walk tree from root
		nodeWalker(root, list, map, anySVGElement);
		
		return map;
	}// tagFinderSVG	
	
	/**
	*	Given a list of objects, finds the first SVG tag that 
	*	has an ID that matches the object. That element is stored in the 
	*	returned HashMap, and can be retreived via the Object key
	*	<p>
	*	The SVG elements found are put into the supplied java.util.Map object.
	*	<p>
	*	Null objects are ignored.
	*	<p>
	*	Supported Objects in the looklist are:
	*	<ul>
	*		<li>String
	*		<li>Object (via toString() method)
	*		<li>Province (checks all short names via getShortNames())
	*	</ul>
	*
	*/
	public static void tagFinderSVG(Map map, List lookList, Node root)
	{
		tagFinderSVG(map, lookList, root, false);
	}// tagFinderSVG
	
	/** As above but allows any SVG element to be returned */
	public static void tagFinderSVG(Map map, List lookList, Node root, boolean anySVGElement)
	{
		List list = new ArrayList(lookList);
		
		// recursively walk tree from root
		nodeWalker(root, list, map, anySVGElement);
	}// tagFinderSVG
	
	
	/** 
	*	Returns all elements with IDs under the given root, as an array of SVGElement objects.
	*	Objects w/o IDs are ignored.
	*
	*/
	public static SVGElement[] idFinderSVG(Node root)
	{
		List list = new ArrayList(150);
		idNodeWalker(root, list, true);
		return (SVGElement[]) list.toArray(new SVGElement[list.size()]);
	}// idFinderSVG()
	
	
	/**
	*	Walks the nodes of the SVG DOM, recursively.
	*	All non-G or non-SYMBOL elements are ignored, if anySVGElement flag is false
	*
	*/
	private static void nodeWalker(Node node, List list, Map map, boolean anySVGElement)
	{
		if( node.getNodeType() == Node.ELEMENT_NODE 
			&& ((anySVGElement && node instanceof org.w3c.dom.svg.SVGElement)
				|| (node.getNodeName() == SVGConstants.SVG_G_TAG || node.getNodeName() == SVGConstants.SVG_SYMBOL_TAG)) )
		{
            // check if the element has an ID attribute
			if(node.hasAttributes()) 
			{
				NamedNodeMap attributes = node.getAttributes();
				Node attrNode = attributes.getNamedItem(SVGConstants.SVG_ID_ATTRIBUTE);	// was ATTR_ID
				if(attrNode != null)
				{
					nodeChecker(attrNode, node, list, map);
				}
			}
	 	}
		
		// check if current node has any children
		// if so, iterate through & recursively call this method
		NodeList children = node.getChildNodes();
		if(children != null)
		{
			for(int i=0; i<children.getLength(); i++)
			{
				nodeWalker(children.item(i), list, map, anySVGElement);
			}
		}
	}// nodeWalker()
	
	
	/**
	*	Walks the nodes of the SVG DOM, recursively.
	*	Looks for any ELEMENT with an ID value.
	*
	*/
	private static void idNodeWalker(Node node, List list, boolean isRoot)
	{
		if(node.getNodeType() == Node.ELEMENT_NODE && !isRoot)
		{
 			String id = ((Element) node).getAttribute(SVGConstants.SVG_ID_ATTRIBUTE);
			if( !"".equals(id) )
			{
				list.add( (SVGElement) node );
			}
	 	}
		
		// check if current node has any children
		// if so, iterate through & recursively call this method
		NodeList children = node.getChildNodes();
		if(children != null)
		{
			for(int i=0; i<children.getLength(); i++)
			{
				idNodeWalker(children.item(i), list, false);
			}
		}
	}// nodeWalker()
	
	
	/**
	*	Checks if the current node ID matches the ID of any elements in the 
	*	list. If it does, the element is added to map, and removed from the list.
	*
	*/
	private static void nodeChecker(Node attributeNode, Node parentNode, List list, Map map)
	{
		String nodeValue = attributeNode.getNodeValue();
		Iterator iter = list.iterator();
		while(iter.hasNext())
		{
			Object obj = iter.next();
			if(obj == null)
			{
				iter.remove();
			}
			else
			{
				if(obj instanceof Province)
				{
					// use getShortNames() to check against all short names
					String[] provShortNames = ((Province) obj).getShortNames();
					for(int i=0; i<provShortNames.length; i++)
					{
						if(nodeValue.equalsIgnoreCase(provShortNames[i]))
						{
							map.put(obj, parentNode);
							iter.remove();
							return;
						}
					}
				}
				else
				{
					// for String and Objects, just use toString()
					if(nodeValue.equalsIgnoreCase(obj.toString()))
					{
						map.put(obj, parentNode);
						iter.remove();
						return;
					}
				}
			}
		}
	}// nodeChecker()	
	
	
	/** 
	*	Walks the DOM tree from root, until the first element with the same ID is found. 
	*	Case Insensitive. 
	*/
	public static Node findNodeWithID(Node node, String id)
	{
		if(node.getNodeType() == Node.ELEMENT_NODE)
		{
            // check if the current element has an ID attribute
			if(node.hasAttributes()) 
			{
				NamedNodeMap attributes = node.getAttributes();
				Node attrNode = attributes.getNamedItem(SVGConstants.SVG_ID_ATTRIBUTE);	// was ATTR_ID
				if(attrNode != null)
				{
					String nodeValue = attrNode.getNodeValue();
					if(nodeValue.equalsIgnoreCase(id))
					{
						return node;
					}
				}
			}
	 	}
		
		// check if current node has any children
		// if so, iterate through & recursively call this method
		NodeList children = node.getChildNodes();
		if(children != null)
		{
			for(int i=0; i<children.getLength(); i++)
			{
				Node aNode = findNodeWithID(children.item(i), id);
				if(aNode != null)
				{
					return aNode;
				}
			}
		}
		
		return null;
	}// findNodeWithID()

    /**
     * Calculates the maximum possible stretching of a canvas, remaining
     * inside the bounds of a given scrollPane.
     *
     * @param canvas         the canvas to be fitted
     */
     public static void getBestFit(JSVGCanvas canvas) {
         // NOTE: we use the initial transform...
         AffineTransform iat = canvas.getInitialTransform();
         if(iat != null)
         {
             Dimension dim = canvas.getSize();
             java.awt.geom.Dimension2D docSize = canvas.getSVGDocumentSize();
			 
			 //System.out.println("viewport extents: (getBestFit()): "+dim);
			 
			 // find out if width or height is larger; we use that to scale.
             double scaleFactor = 0.0;
             if(docSize.getWidth() >= docSize.getHeight())
             {
                 scaleFactor = dim.getWidth() / docSize.getWidth();
             }
             else
             {
                 scaleFactor = dim.getHeight() / docSize.getHeight();
             }

             AffineTransform t = AffineTransform.getTranslateInstance(0,0);
             t.scale(scaleFactor, scaleFactor);
             t.concatenate(iat);
             canvas.setRenderingTransform(t);
         }
     }

	/** 
	*	Formats a Floating-Point value into a String,
	*	using the jDip default precision.
	*/
	public static String floatToString(float v)
	{
		return floatToSB(v).toString();
	}// toString()
	
	
	/** 
	*	Formats a Floating-Point value into a StringBuffer,
	*	using the jDip default precision.
	*/
	public static void appendFloat(StringBuffer sb, float v)
	{
		sb.append(floatToSB(v));
	}// appendFloat()
	
	
	/** 
	*	Internal append; assumes floats <= 8 digits.
	*	If ends in ".0", the ".0" is truncated.
	*/
	private static StringBuffer floatToSB(float v)
	{
		StringBuffer sb = new StringBuffer(8);
		TypeFormat.format(v, FLOAT_PRECISION, sb);
		final int s = sb.length();
		if(sb.charAt(s-1) == '0' && sb.charAt(s-2) == '.')
		{
			sb.delete(s-2,s);
		}
		
		return sb;
	}// floatToSB()
	
}// class SVGUtils
