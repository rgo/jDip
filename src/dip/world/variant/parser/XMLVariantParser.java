//t
//  @(#)XMLVariantParser.java		7/2002
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
package dip.world.variant.parser;

import dip.world.variant.VariantManager;

import dip.world.variant.data.Variant;
import dip.world.variant.data.SupplyCenter;
import dip.world.variant.data.InitialState;
import dip.world.variant.data.MapGraphic;
import dip.world.variant.data.ProvinceData;
import dip.world.variant.data.BorderData;

import dip.world.Phase;
import dip.world.Power;
import dip.world.Unit;
import dip.world.Coast;

import dip.misc.LRUCache;
import dip.misc.Utils;
import dip.misc.Log;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.w3c.dom.*;


/**
*	Parses an XML Variant description.
*
*/
public class XMLVariantParser implements VariantParser
{
	// XML Element constants
	public static final String EL_VARIANTS = "VARIANTS";
	public static final String EL_VARIANT = "VARIANT";
	public static final String EL_DESCRIPTION = "DESCRIPTION";
	public static final String EL_MAP = "MAP";
	public static final String EL_STARTINGTIME = "STARTINGTIME";
	public static final String EL_INITIALSTATE = "INITIALSTATE";
	public static final String EL_SUPPLYCENTER = "SUPPLYCENTER";
	public static final String EL_POWER = "POWER";
	public static final String EL_MAP_DEFINITION = "MAP_DEFINITION";
	public static final String EL_MAP_GRAPHIC = "MAP_GRAPHIC";
	public static final String EL_VICTORYCONDITIONS = "VICTORYCONDITIONS";
	public static final String EL_GAME_LENGTH = "GAME_LENGTH";
	public static final String EL_YEARS_WITHOUT_SC_CAPTURE = "YEARS_WITHOUT_SC_CAPTURE";
	public static final String EL_WINNING_SUPPLY_CENTERS = "WINNING_SUPPLY_CENTERS";
	public static final String EL_RULEOPTIONS = "RULEOPTIONS";
	public static final String EL_RULEOPTION = "RULEOPTION";
	
	
	// XML Attribute constants
	public static final String ATT_ALIASES = "aliases";
	public static final String ATT_VERSION = "version";
	public static final String ATT_URI = "URI";
	public static final String ATT_DEFAULT = "default";
	public static final String ATT_TITLE = "title";
	public static final String ATT_DESCRIPTION = "description";
	public static final String ATT_THUMBURI = "thumbURI";
	public static final String ATT_ADJACENCYURI = "adjacencyURI";
	public static final String ATT_NAME = "name";
	public static final String ATT_ACTIVE = "active";
	public static final String ATT_ADJECTIVE = "adjective";
	public static final String ATT_ALTNAMES = "altnames";
	public static final String ATT_TURN = "turn";
	public static final String ATT_VALUE = "value";
	public static final String ATT_PROVINCE = "province";
	public static final String ATT_HOMEPOWER = "homepower";
	public static final String ATT_OWNER = "owner";
	public static final String ATT_POWER = "power";
	public static final String ATT_UNIT = "unit";
	public static final String ATT_UNITCOAST = "unitcoast";
	public static final String ATT_ALLOW_BC_YEARS = "allowBCYears";
	public static final String ATT_PREFERRED_UNIT_STYLE = "preferredUnitStyle";
	public static final String ATT_ID	= "id";
	public static final String ATT_REF	= "ref";
	
	
	// il8n error message constants
	private static final String ERR_NO_ELEMENT = "XMLVariantParser.noelement";
	
	// instance variables
	private Document doc = null;
	private DocumentBuilder docBuilder = null;
	private List variantList = null;
	private XMLProvinceParser provinceParser = null;
	
	
	/** Create an XMLVariantParser */
	/*
	public XMLVariantParser(boolean isValidating)
	throws ParserConfigurationException
	{
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setValidating(isValidating);
		dbf.setCoalescing(false);
		dbf.setIgnoringComments(true);
		
		docBuilder = dbf.newDocumentBuilder();
		docBuilder.setErrorHandler(new XMLErrorHandler());
		
		provinceParser = new XMLProvinceParser(dbf);
		
		variantList = new LinkedList();
		AdjCache.init(provinceParser);
	}// XMLVariantParser()
	*/
	
	/** Create an XMLVariantParser */
	public XMLVariantParser(final DocumentBuilderFactory dbf)
	throws ParserConfigurationException
	{
		docBuilder = dbf.newDocumentBuilder();
		docBuilder.setErrorHandler(new XMLErrorHandler());
		FastEntityResolver.attach(docBuilder);
		provinceParser = new XMLProvinceParser(dbf);
		
		variantList = new LinkedList();
		AdjCache.init(provinceParser);
	}// XMLVariantParser()
	
	
	
	/** Parse the given input stream; parsed data available via <code>getVariants()</code>
		<p>
		Note that when this method is called, any previous Variants (if any exist) are
		cleared.
	*/
	public void parse(InputStream is, URL variantPackageURL)
	throws IOException, SAXException
	{
		Log.println("XMLVariantParser: Parsing: ", variantPackageURL);
		long time = System.currentTimeMillis();
		
		// cleanup cache (very important to remove references!)
		AdjCache.clear();
		variantList.clear();
		
		if(variantPackageURL == null)
		{
			throw new IllegalArgumentException();
		}
		
		AdjCache.setVariantPackageURL(variantPackageURL);
		doc = docBuilder.parse(is);
		procVariants();
		Log.printTimed(time, "   time: ");
	}// parse()
	
	
	
	
	/** Cleanup, clearing any references/resources */
	public void close()
	{
		AdjCache.clear();
		variantList.clear();
	}// close()
	
	
	
	/** Returns an array of Variant objects.
		<p>
		Will never return null. Note that parse() must be called before
		this will return any information.
	*/
	public Variant[] getVariants()
	{
		return (Variant[]) variantList.toArray(new Variant[variantList.size()]); 			
	}// getVariants()
	
	
	
	
	
	
	/** Process the Variant list description file */
	private void procVariants()
	throws IOException, SAXException
	{
		// setup map definition ID hashmap
		HashMap mapDefTable = new HashMap(7);	// maps String ID -> MapDef
		
		
		// find the root element (VARIANTS), and all VARIANT elements underneath.
		Element root = doc.getDocumentElement();
		
		// get map definitions (at least one, under VARIANT)
		NodeList mapDefEls = root.getElementsByTagName(EL_MAP_DEFINITION);
		for(int i=0; i<mapDefEls.getLength(); i++)
		{
			Element elMapDef = (Element) mapDefEls.item(i);
			
			// get description
			String description = null;
			Element element = getSingleElementByName(elMapDef, EL_DESCRIPTION);
			if(element != null)
			{
				Node text = element.getFirstChild();
				description = text.getNodeValue();
			}
			
			// create MapDef
			MapDef md = new MapDef( 
				elMapDef.getAttribute(ATT_ID),
				elMapDef.getAttribute(ATT_TITLE),
				elMapDef.getAttribute(ATT_URI),
				elMapDef.getAttribute(ATT_THUMBURI),
				elMapDef.getAttribute(ATT_PREFERRED_UNIT_STYLE),
				description );
			
			// if no title, error!
			if("".equals(md.getTitle()))
			{
				throw new IOException("map id="+md.getID()+" missing a title (name)");
			}
			
			// map it.
			mapDefTable.put(md.getID(), md);
		}
		
		// search for variant data
		NodeList variantElements = root.getElementsByTagName(EL_VARIANT);
		for(int i=0; i<variantElements.getLength(); i++)
		{
			Variant variant = new Variant();
			Element elVariant = (Element) variantElements.item(i);
			
			// VARIANT attributes
			variant.setName( elVariant.getAttribute(ATT_NAME) );
			variant.setDefault( Boolean.valueOf(elVariant.getAttribute(ATT_DEFAULT)).booleanValue() );
			variant.setVersion( parseFloat(elVariant.getAttribute(ATT_VERSION)) ); 
			variant.setAliases( Utils.parseCSV(elVariant.getAttribute(ATT_ALIASES)) );
			
			// description
			Element element = getSingleElementByName(elVariant, EL_DESCRIPTION);
			checkElement(element, EL_DESCRIPTION);
			Node text = element.getFirstChild();
			variant.setDescription( text.getNodeValue() );
			
			// starting time
			element = getSingleElementByName(elVariant, EL_STARTINGTIME);
			checkElement(element, EL_STARTINGTIME);
			variant.setStartingPhase( Phase.parse(element.getAttribute(ATT_TURN)) );
			variant.setBCYearsAllowed( Boolean.valueOf(element.getAttribute(ATT_ALLOW_BC_YEARS)).booleanValue() );
			
			// if start is BC, and BC years are not allowed, then BC years ARE allowed.
			if(variant.getStartingPhase().getYear() < 0)
			{
				variant.setBCYearsAllowed(true);
			}
			
			// victory conditions (single, with single subitems)
			element = getSingleElementByName(elVariant, EL_VICTORYCONDITIONS);
			checkElement(element, EL_VICTORYCONDITIONS);
			Element vcSubElement = getSingleElementByName(element, EL_WINNING_SUPPLY_CENTERS);
			if(vcSubElement != null)
			{
				variant.setNumSCForVictory( parseInt(vcSubElement.getAttribute(ATT_VALUE)) );
			}
			
			vcSubElement = getSingleElementByName(element, EL_YEARS_WITHOUT_SC_CAPTURE);
			if(vcSubElement != null)
			{
				variant.setMaxYearsNoSCChange( parseInt(vcSubElement.getAttribute(ATT_VALUE)) );
			}
			
			vcSubElement = getSingleElementByName(element, EL_GAME_LENGTH);
			if(vcSubElement != null)
			{
				variant.setMaxGameTimeYears( parseInt(vcSubElement.getAttribute(ATT_VALUE)) );
			}
			
			
			// powers (multiple)
			NodeList nodes = elVariant.getElementsByTagName(EL_POWER);
			final int nodeListLen = nodes.getLength();
			List powerList = new ArrayList(nodeListLen);
			for(int j=0; j<nodeListLen; j++)
			{
				element = (Element) nodes.item(j);
				String name = element.getAttribute(ATT_NAME);
				final boolean isActive =  Boolean.valueOf( element.getAttribute(ATT_ACTIVE) ).booleanValue();
				String adjective = element.getAttribute(ATT_ADJECTIVE);
				String[] altNames = Utils.parseCSVXE( element.getAttribute(ATT_ALTNAMES) );
				
				String[] names = new String[altNames.length + 1];
				names[0] = name;
				System.arraycopy(altNames, 0, names, 1, altNames.length);
				
				Power power = new Power(names, adjective, isActive);
				powerList.add(power);
			}
			variant.setPowers(powerList);
			
			
			// supply centers (multiple)
			nodes = elVariant.getElementsByTagName(EL_SUPPLYCENTER);
			List supplyCenterList = new ArrayList(nodes.getLength());
			for(int j=0; j<nodes.getLength(); j++)
			{
				element = (Element) nodes.item(j);
				SupplyCenter supplyCenter = new SupplyCenter();
				supplyCenter.setProvinceName( element.getAttribute(ATT_PROVINCE) );
				supplyCenter.setHomePowerName( element.getAttribute(ATT_HOMEPOWER) );
				supplyCenter.setOwnerName( element.getAttribute(ATT_OWNER) );
				supplyCenterList.add(supplyCenter);
			}
			variant.setSupplyCenters(supplyCenterList);
			
			// initial state (multiple)
			nodes = elVariant.getElementsByTagName(EL_INITIALSTATE);
			List stateList = new ArrayList(nodes.getLength());
			for(int j=0; j<nodes.getLength(); j++)
			{
				element = (Element) nodes.item(j);
				InitialState initialState = new InitialState();
				initialState.setProvinceName( element.getAttribute(ATT_PROVINCE) );
				initialState.setPowerName( element.getAttribute(ATT_POWER) );
				initialState.setUnitType( Unit.Type.parse(element.getAttribute(ATT_UNIT)) );
				initialState.setCoast( Coast.parse(element.getAttribute(ATT_UNITCOAST)) );
				stateList.add(initialState);
			}
			variant.setInitialStates(stateList);
			
			// MAP element and children
			element = getSingleElementByName(elVariant, EL_MAP);
			
			// MAP adjacency URI; process it using ProvinceData parser
			try
			{
				URI adjacencyURI = new URI(element.getAttribute(ATT_ADJACENCYURI));
				variant.setProvinceData( AdjCache.getProvinceData(adjacencyURI) );
				variant.setBorderData( AdjCache.getBorderData(adjacencyURI) );
			}
			catch(URISyntaxException e)
			{
				throw new IOException(e.getMessage());
			}
			
			
			// MAP_GRAPHIC element (multiple)
			nodes = element.getElementsByTagName(EL_MAP_GRAPHIC);
			List graphicList = new ArrayList(nodes.getLength());
			for(int j=0; j<nodes.getLength(); j++)
			{
				Element mgElement = (Element) nodes.item(j);
				final String refID = mgElement.getAttribute(ATT_REF);
				final boolean isDefault = Boolean.valueOf(mgElement.getAttribute(ATT_DEFAULT)).booleanValue();
				final String preferredUnitStyle = mgElement.getAttribute(ATT_PREFERRED_UNIT_STYLE);
				
				// lookup; if we didn't find it, throw an exception
				MapDef md = (MapDef) mapDefTable.get(refID);
				if(md == null)
				{
					throw new IOException("MAP_GRAPHIC refers to unknown ID: \""+refID+"\"");						
				}
				
				// create the MapGraphic object
				MapGraphic mapGraphic = new MapGraphic(
					md.getMapURI(),
					isDefault,
					md.getTitle(),
					md.getDescription(),
					md.getThumbURI(),
					("".equals(preferredUnitStyle)) ? md.getPrefUnitStyle() : preferredUnitStyle );
				
				graphicList.add(mapGraphic);
			}
			variant.setMapGraphics( graphicList );
			
			// rule options (if any have been set)
			// this element is optional.
			element = getSingleElementByName(elVariant, EL_RULEOPTIONS);
			if(element != null)
			{
				nodes = element.getElementsByTagName(EL_RULEOPTION);
				List ruleNVPList = new ArrayList(nodes.getLength());
				for(int j=0; j<nodes.getLength(); j++)
				{
					Element rElement = (Element) nodes.item(j);
					Variant.NameValuePair nvp = new Variant.NameValuePair(
													rElement.getAttribute(ATT_NAME),
													rElement.getAttribute(ATT_VALUE)
												);
					ruleNVPList.add(nvp);
				}
				variant.setRuleOptionNVPs( ruleNVPList );
			}
			else
			{
				variant.setRuleOptionNVPs( new ArrayList(0) );
			}
			
			// add variant to list of variants
			variantList.add(variant);
		}// for(i)
	}// procVariants()
	
	
	/** Checks that an element is present */
	private void checkElement(Element element, String name)
	throws SAXException
	{
		if(element == null)
		{
			throw new SAXException(Utils.getLocalString(ERR_NO_ELEMENT, name));
		}
	}// checkElement()
	
	/** Get an Element by name; only returns a single element. */
	private Element getSingleElementByName(Element parent, String name)
	{
		NodeList nodes = parent.getElementsByTagName(name);
		return (Element) nodes.item(0);
	}// getSingleElementByName()
	
	
	/** Integer parser; throws an exception if number cannot be parsed. */
	private int parseInt(String value)
	throws IOException
	{
		String message = "";
		
		try
		{
			return Integer.parseInt(value);
		}
		catch(NumberFormatException e)
		{
			message = e.toString();
		}
		
		throw new IOException(message);
	}// parseInt()
	
	
	/** Float parser; throws an exception if number cannot be parsed. Value must be >= 0.0 */
	private float parseFloat(String value)
	throws IOException
	{
		String message = "";
		
		try
		{
			final float floatValue = Float.parseFloat(value);
			if(floatValue < 0.0f)
			{
				throw new NumberFormatException("Value must be >= 0");
			}
			
			return floatValue;
		}
		catch(NumberFormatException e)
		{
			message = e.toString();
		}
		
		throw new IOException(message);
	}// parseInt()
	
	
	/** 
	*	Inner class which caches XML adjacency data (ProvinceData and BorderData), 
	*	which may be shared between different variants (if the variants use the
	*	same adjacency data).
	*	<p>
	*	NOTE: this depends on the XMLVariantParser variable "adjCache", since inner classes
	*	cannot have statics (unless they inner class is static, which just creates more problems;
	*	this is a simpler solution)
	*
	*/
	private static class AdjCache
	{
		private static URL vpURL = null;
		private static XMLProvinceParser pp = null;
		private static LRUCache adjCache = null;	// URI -> AdjCache objects
		
		// instance variables
		private ProvinceData[] 	provinceData;
		private BorderData[]	borderData;
		
		
		public AdjCache()
		{
		}// AdjCache()
		
		/** initialization */
		public static void init(XMLProvinceParser provinceParser)
		{
			pp = provinceParser;
			adjCache = new LRUCache(6);
		}// AdjCache()
		
		/** Sets the variant package URL */
		public static void setVariantPackageURL(URL variantPackageURL)
		{
			vpURL = variantPackageURL;
		}// setVariantPackageURL()
		
		
		/** Clears the cache. */
		public static void clear()
		{
			adjCache.clear();
		}// clear()
		
		
		/** Gets the ProvinceData for a given adjacency URI */
		public static ProvinceData[] getProvinceData(URI adjacencyURI)
		throws IOException, SAXException
		{
			AdjCache ac = get(adjacencyURI);
			return ac.provinceData;
		}// getProvinceData()
		
		
		/** Gets the BorderData for a given adjacency URI */
		public static BorderData[] getBorderData(URI adjacencyURI)
		throws IOException, SAXException
		{
			AdjCache ac = get(adjacencyURI);
			return ac.borderData;
		}// getBorderData()
		
		
		/** Gets the AdjCache object from the cache, or parses from the URI, as appropriate */
		private static AdjCache get(URI adjacencyURI)
		throws IOException, SAXException
		{
			// see if we already have the URI data cached.
			if(adjCache.get(adjacencyURI) != null)
			{
				//Log.println("  AdjCache: using cached adjacency data: ", adjacencyURI);
				return (AdjCache) adjCache.get(adjacencyURI);
			}
			
			// it's not cached. resolve URI.
			URL url = VariantManager.getResource(vpURL, adjacencyURI);
			if(url == null)
			{
				throw new IOException("Could not convert URI: "+adjacencyURI+" from variant package: "+vpURL);
			}
			
			// parse resolved URI
			//Log.println("  AdjCache: not in cache: ", adjacencyURI);
			InputStream is = null;
			try
			{
				is = new BufferedInputStream(url.openStream());
				pp.parse(is);
			}
			finally
			{
				if(is != null)
				{
					try { is.close(); } catch (IOException e) {}
				}
			}
			
			// cache and return parsed data.
			AdjCache ac = new AdjCache();
			ac.provinceData	= pp.getProvinceData();
			ac.borderData 	= pp.getBorderData();
			adjCache.put(adjacencyURI, ac);
			return ac;
		}// get()
		
	}// inner class AdjCache
	
	
	/** 
	*	Class that holds MAP_DEFINITION data, which is 
	*	inserted into a hashtable for later recall. 
	*/
	private class MapDef
	{
		private final String id;
		private final String title;
		private final String mapURI;
		private final String thumbURI;
		private final String preferredUnitStyle;
		private final String description;
		
		public MapDef(String id, String title, String mapURI, String thumbURI, 
			String preferredUnitStyle, String description)
		{
			this.id = id;
			this.title = title;
			this.mapURI = mapURI;
			this.thumbURI = thumbURI;
			this.preferredUnitStyle = preferredUnitStyle;
			this.description = description;
		}// MapDef()
		
		public String getID() 	{ return id; }
		public String getTitle() 	{ return title; }
		public String getMapURI() 	{ return mapURI; }
		public String getThumbURI() 	{ return thumbURI; }
		public String getPrefUnitStyle() 	{ return preferredUnitStyle; }
		public String getDescription() 	{ return description; }
	}// inner class MapDef
	
}// class XMLVariantParser



