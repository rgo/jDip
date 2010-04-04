//
//  @(#)XMLSymbolParser.java		11/2003
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
//  Or from http://www.gnu.org/19
//
package dip.world.variant.parser;

import dip.world.variant.VariantManager;
import dip.world.variant.data.SymbolPack;
import dip.world.variant.data.Symbol;

import dip.misc.Log;
import dip.misc.XMLUtils;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;
import org.xml.sax.ErrorHandler;
import org.w3c.dom.*;


/**
*	Parses a SymbolPack description.
*
*/
public class XMLSymbolParser implements SymbolParser
{
	// Element constants
	public static final String EL_SYMBOLS 		= "SYMBOLS";
	public static final String EL_DESCRIPTION 	= "DESCRIPTION";
	public static final String EL_SCALING 		= "SCALING";
	public static final String EL_SCALE 		= "SCALE";
	private static final String EL_DEFS = "defs";
	private static final String EL_STYLE = "style";
	
	
	// Attribute constants
	public static final String ATT_NAME 	= "name";
	public static final String ATT_VERSION 	= "version";
	public static final String ATT_THUMBURI = "thumbURI";
	public static final String ATT_SVGURI 	= "svgURI";
	public static final String ATT_VALUE	= "value";
	private static final String ATT_ID		= "id";
	private static final String ATT_TYPE = "type";
	
	// valid element tag names; case sensitive
	private static final String[] VALID_ELEMENTS = {"g","symbol","svg"};
	
	// misc
	private static final String CSS_TYPE_VALUE = "text/css";
	private static final String CDATA_NODE_NAME = "#cdata-section";

	
	// instance variables
	private Document doc = null;
	private DocumentBuilder docBuilder = null;
	private SymbolPack symbolPack = null;
	private URL symbolPackURL = null;
	
	
	
	/** Create an XMLSymbolParser */
	public XMLSymbolParser(final DocumentBuilderFactory dbf)
	throws ParserConfigurationException
	{
		boolean oldNSvalue = dbf.isNamespaceAware();
		
		dbf.setNamespaceAware(true);	// essential!
		docBuilder = dbf.newDocumentBuilder();
		docBuilder.setErrorHandler(new XMLErrorHandler());
		FastEntityResolver.attach(docBuilder);
		
		// cleanup
		dbf.setNamespaceAware(oldNSvalue);
	}// XMLProvinceParser()
	
	
	/** Parse the given input stream */
	public synchronized void parse(InputStream is, URL symbolPackURL)
	throws IOException, SAXException
	{
		Log.println("XMLSymbolParser: Parsing: ", symbolPackURL);
		long time = System.currentTimeMillis();
		symbolPack = null;
		this.symbolPackURL = symbolPackURL;
		doc = docBuilder.parse(is);
		procSymbolData();
		Log.printTimed(time, "    time: ");
	}// parse()
	
	
	/** Cleanup, clearing any references/resources */
	public void close()
	{
		symbolPack = null;
		doc = null;
	}// close()
	
	/** 
	*	Returns the SymbolPack, or null, if parse()
	*	has not yet been called.
	*/
	public SymbolPack getSymbolPack()
	{
		return symbolPack;
	}// getSymbolPacks()
	
	
	/** Parse the symbol data into Symbols and SymbolPacks */
	private void procSymbolData()
	throws IOException, SAXException
	{
		// create a symbolPack
		symbolPack = new SymbolPack();
		
		// find root element
		Element root = doc.getDocumentElement();	// root: EL_SYMBOLS
		symbolPack.setName( root.getAttribute(ATT_NAME).trim() );
		symbolPack.setVersion( parseFloat(root, ATT_VERSION) );
		symbolPack.setThumbnailURI( root.getAttribute(ATT_THUMBURI).trim() );
		symbolPack.setSVGURI( root.getAttribute(ATT_SVGURI).trim() );
		
		// parse description
		Element element = getSingleElementByName(root, EL_DESCRIPTION);
		if(element != null)
		{
			Node text = element.getFirstChild();
			symbolPack.setDescription( text.getNodeValue() );
		}
		
		
		// setup a hashmap: maps symbol names (case-preserved) to 
		// scale factors (Float). If hashmap is empty, we have no
		// scaling factors to worry about
		HashMap scaleMap = new HashMap();
		
		// is SCALING element present? if so, parse it.
		NodeList scalingNodes = root.getElementsByTagName(EL_SCALING);
		if(scalingNodes.getLength() == 1)
		{
			NodeList scNodes = ((Element) scalingNodes.item(0)).getElementsByTagName(EL_SCALE);
			for(int i=0; i<scNodes.getLength(); i++)
			{
				Element elScale = (Element) scNodes.item(i);
				scaleMap.put(
					elScale.getAttribute(ATT_NAME).trim(),
					parseScaleFactor(elScale, ATT_VALUE)
				);
			}
		}
		
		// extract symbol SVG into symbols
		// add symbols to SymbolPack
		procAndAddSymbolSVG(symbolPack, scaleMap);
	}// procSymbolData()
	
	
	/** Parse the symbol data into Symbols and SymbolPacks */
	private void procAndAddSymbolSVG(SymbolPack symbolPack, HashMap scaleMap)
	throws IOException, SAXException
	{
		Document svgDoc = null;
		
		// resolve SVG URI
		URL url = VariantManager.getResource(symbolPackURL, symbolPack.getSVGURI());
		if(url == null)
		{
			throw new IOException("Could not convert URI: "+
				symbolPack.getSVGURI()+" from SymbolPack: "+symbolPackURL);
		}
		
		// parse resolved URI into a Document
		InputStream is = null;
		try
		{
			is = new BufferedInputStream(url.openStream());
			svgDoc = docBuilder.parse(is);
		}
		finally
		{
			if(is != null)
			{
				try { is.close(); } catch (IOException e) {}
			}
		}
		
		// find defs section, if any, and style attribute
		// 
		Element defs = XMLUtils.findChildElementMatching(svgDoc.getDocumentElement(), EL_DEFS);
		if(defs != null)
		{
			Element style = XMLUtils.findChildElementMatching(defs, EL_STYLE);
			if(style != null)
			{
				// check CSS type (must be "text/css")
				//
				String type = style.getAttribute(ATT_TYPE).trim();
				if(!CSS_TYPE_VALUE.equals(type))
				{
					throw new IOException("Only <style type=\"text/css\"> is accepted. Cannot parse CSS otherwise.");
				}
				
				style.normalize();
				
				// get style CDATA
				CDATASection cdsNode = (CDATASection) XMLUtils.findChildNodeMatching(style,
					CDATA_NODE_NAME, Node.CDATA_SECTION_NODE);
				
				if(cdsNode == null)
				{
					throw new IOException("CDATA in <style> node is null.");
				}
				
				symbolPack.setCSSStyles( parseCSS(cdsNode.getData()) );
			}
		}
		
		// find all IDs
		HashMap map = elementMapper(svgDoc.getDocumentElement(), ATT_ID);
		
		// List of Symbols
		ArrayList list = new ArrayList(15);
		
		// iterate over hashmap finding all symbols with IDs
		Iterator iter = map.entrySet().iterator();
		while(iter.hasNext())
		{
			Map.Entry me = (Map.Entry) iter.next();
			final String name = (String) me.getKey();
			final Float scale = (Float) scaleMap.get(name);
			list.add(new Symbol(
				name,
				(scale == null) ? Symbol.IDENTITY_SCALE : scale.floatValue(),
				(Element) me.getValue() ));
		}
		
		// add symbols to symbolpack
		symbolPack.setSymbols(list);
	}// procAndAddSymbolSVG()
	
	
	/** 
	*	Returns a Float, representing the scaling factor; must be non-negative
	*	and non-zero.
	*/
	private Float parseScaleFactor(Element element, String attrName)
	throws IOException
	{
		float f = parseFloat(element, attrName);
		
		if(f <= 0.0f)
		{
			throw new IOException(element.getTagName()+" attribute "+attrName+" cannot be negative or zero.");
		}
		
		return new Float(f);
	}// parseScaleFactor()
	
	
	/** Parse a floating point value */
	private float parseFloat(Element element, String attrName)
	throws IOException
	{
		try
		{
			return Float.parseFloat(element.getAttribute(attrName).trim());
		}
		catch(NumberFormatException e)
		{
			throw new IOException(
				element.getTagName()+" attribute "+attrName+" has an invalid "+
				"floating point value \""+element.getAttribute(attrName)+"\"" ); 
		}
	}// parseScaleFactor()
	
	
	/** Get an Element by name; only returns a single element. */
	private Element getSingleElementByName(Element parent, String name)
	{
		NodeList nodes = parent.getElementsByTagName(name);
		return (Element) nodes.item(0);
	}// getSingleElementByName()
	
	
	/**
	*	Searches an XML document for Elements that have a given non-empty attribute.
	*	The Elements are then put into a HashMap, which is indexed by the attribute 
	*	value. This starts from the given Element and recurses downward. Throws an
	*	exception if an element with a duplicate attribute name is found.
	*/
	private HashMap elementMapper(Element start, String attrName)
	throws IOException
	{
		HashMap map = new HashMap(31);
		elementMapperWalker(map, start, attrName);
		return map;
	}// elementMapper()
	
	
	/** Recursive portion of elementMapper */
	private void elementMapperWalker(final HashMap map, final Node node, final String attrName)
	throws IOException
	{
		if(node.getNodeType() == Node.ELEMENT_NODE)
		{
			// node MUST be one of the following:
			// <g>, <symbol>, or <svg>
			// 
			String name = ((Element) node).getTagName();
			if( node.hasAttributes()
				&& isValidElement(name) )
			{
				NamedNodeMap attributes = node.getAttributes();
				Node attrNode = attributes.getNamedItem(attrName);
				if(attrNode != null)
				{
					final String attrValue = attrNode.getNodeValue();
					if(!"".equals(attrValue))
					{
						if(map.containsKey(attrValue))
						{
							throw new IOException("The "+attrName+" attribute has duplicate "+
								"values: "+attrValue);
						}
						
						map.put(attrValue, (Element) node);
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
				elementMapperWalker(map, children.item(i), attrName);
			}
		}
	}// elementMapperWalker()
	
	
	/** See if name is a valid element tag name */
	private boolean isValidElement(String name)
	{
		for(int i=0; i<VALID_ELEMENTS.length; i++)
		{
			if(VALID_ELEMENTS[i].equals(name))
			{
				return true;
			}
		}
		
		return false;
	}// isValidElement()
	
	
	/** 
	*	Very Simple CSS parser. Does not handle comments. 
	* 	Assumes that the beginning of a line has a CSS property, 
	*	and is followed by a braced CSS style information. 
	*	<p>
	*	<pre>
	*		.hello 		{style:lala;this:that}		// handled OK
	*		    .goodbye {fill:red;}				// handled OK
	*	.multiline {fill:red;
	*			opacity:some;}						// not handled
	*	</pre>	
	*/
	private SymbolPack.CSSStyle[] parseCSS(String input)
	throws IOException
	{
		List cssStyles = new ArrayList(20);
		
		// break input into lines
		BufferedReader br = new BufferedReader(new StringReader(input));
		String line = br.readLine();
		while(line != null)
		{
			// first non-whitespace must be a '.'
			line = line.trim();
			if(line.startsWith("."))
			{
				int idxEndName = -1;	// end of the style name
				int idxCBStart = -1;	// position of '{'
				int idxCBEnd = -1;		// position of '}'
				for(int i=0; i<line.length(); i++)
				{
					char c = line.charAt(i);
					if(idxEndName < 0 && Character.isWhitespace(c))
					{
						idxEndName = i;
					}
					
					if(idxEndName > 0 && c == '{')
					{
						if(idxCBStart < 0)
						{
							idxCBStart = i;
						}
						else
						{
							// error!
							idxCBStart = -1;
							break;
						}
					}
					
					if(idxCBStart > 0 && c == '}')
					{
						idxCBEnd = i; 
						break;
					}
				}
				
				// validate
				if(idxEndName < 0 || idxCBStart < 0 || idxCBEnd < 0)
				{
					throw new IOException(
						"Could not parse SymbolPack CSS. Note that comments are not "+
						"supported, and that there may be only one CSS style per line."+
						"Error line text: \""+line+"\""
					);
				}
				
				// parse
				String name = line.substring(0, idxEndName);
				String value = line.substring(idxCBStart, idxCBEnd+1);
				
				// create CSS Style
				cssStyles.add(new SymbolPack.CSSStyle(name, value));
			}
			
			line = br.readLine();
		}
		
		return (SymbolPack.CSSStyle[]) cssStyles.toArray(new SymbolPack.CSSStyle[cssStyles.size()]);
	}// parseCSS()
	
	
}// class XMLSymbolParser
