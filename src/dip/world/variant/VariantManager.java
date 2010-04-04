//
//  @(#)VariantManager.java	1.00	7/2002
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
package dip.world.variant;

import dip.world.variant.data.*;
import dip.world.variant.parser.*;
import dip.gui.dialog.ErrorDialog;
import dip.misc.Log;

import java.util.*;
import java.net.URLClassLoader;
import java.net.URL;
import java.net.URI;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.io.*;
import org.xml.sax.*;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import javax.jnlp.*; 
import java.util.*;


/**
*	Finds Variant-packs, which are of the format:
*	<ol>
*		<li>*Variant.zip</li>
*		<li>*Variants.zip</li>
*		<li>*Variant.jar</li>
*		<li>*Variants.jar</li>
*	</ol>
*	Faciliates loading of variant resources. Within the above file, the variants.xml
*	file is parsed to determine the required information.
*	<p>
*	Also finds all SymbolPacks, which end in:
*	<ol>
*		<li>*Symbols.zip</li>
*		<li>*Symbols.jar</li>
*	</ol>
*	<p>
*	
*	TODO: deconflict code may work better if we include the preceding "/" before
*	the .jar name.<br> 
*	<br>
*/
public class VariantManager 
{
	/** Version Constant representing the most recent version of a Variant or SymbolPack */
	public static final float VERSION_NEWEST	= -1000.0f;
	/** Version Constant representing the most oldest version of a Variant or SymbolPack */
	public static final float VERSION_OLDEST	= -2000.0f;
	
	
	// variant constants
	private static final String VARIANT_EXTENSIONS[] = { "Variant.zip", "Variants.zip", "Variant.jar", "Variants.jar" }; 
	private static final String VARIANT_FILE_NAME = "variants.xml";
	
	// symbol constants
	private static final String SYMBOL_EXTENSIONS[] = { "Symbols.zip", "Symbols.jar" }; 
	private static final String SYMBOL_FILE_NAME = "symbols.xml";
	
	// class variables
	private static VariantManager vm = null;
	
	// instance variables
	private final boolean isInWebstart;
	private HashMap variantMap = null;	// map of lowercased Variant names to MapRec objects (which contain VRecs)
	private HashMap symbolMap = null;	// lowercase symbol names to MapRec objects (which contain SPRecs)
	
	// cached variables to enhance performance of getResource() methods
	private transient Variant[] variants = new Variant[0];			// The sorted Variant list
	private transient SymbolPack[] symbolPacks = new SymbolPack[0];	// The sorted SymbolPack list
	private transient URLClassLoader currentUCL = null;				// The current class loader 
	private transient URL currentPackageURL = null;					// The current class loader URL
	
	
	
	/** 
	*	Initiaize the VariantManager. 
	*	<p>
	*	An exception is thrown if no File paths are specified. A "." may be used
	*	to specify th ecurrent directory.
	*	<p>
	*	Loaded XML may be validated if the isValidating flag is set to true.
	*
	*/
	public static synchronized void init(File[] searchPaths, boolean isValidating)
	throws javax.xml.parsers.ParserConfigurationException, NoVariantsException
	{
		long ttime = System.currentTimeMillis();
		long vptime = ttime;
		Log.println("VariantManager.init()");
		
		if(searchPaths == null || searchPaths.length == 0)
		{
			throw new IllegalArgumentException();
		}
		
		if(vm != null)
		{
			// perform cleanup
			vm.variantMap.clear();
			vm.variants = new Variant[0];	
			vm.currentUCL = null;
			vm.currentPackageURL = null;
			
			vm.symbolPacks = new SymbolPack[0];
			vm.symbolMap.clear();
		}
		
		vm = new VariantManager();
		
		// find plugins, create plugin loader
		URL[] pluginURLs = vm.searchForFiles(searchPaths, VARIANT_EXTENSIONS);
		
		// setup document builder
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		
		try
		{
			// this may improve performance, and really only apply to Xerces
			dbf.setAttribute("http://apache.org/xml/features/dom/defer-node-expansion", Boolean.FALSE);
			dbf.setAttribute("http://apache.org/xml/properties/input-buffer-size", new Integer(4096));
			dbf.setAttribute("http://apache.org/xml/features/nonvalidating/load-external-dtd", Boolean.FALSE);
		}
		catch(Exception e)
		{
			Log.println("VM: Could not set XML feature.", e);
		}
		
		dbf.setValidating(isValidating);
		dbf.setCoalescing(false);
		dbf.setIgnoringComments(true);
		
		// setup variant parser
		XMLVariantParser variantParser = new XMLVariantParser(dbf);
		
		// for each plugin, attempt to find the "variants.xml" file inside. 
		// if it does not exist, we will not load the file. If it does, we will parse it,
		// and associate the variant with the URL in a hashtable.
		for(int i=0; i<pluginURLs.length; i++)
		{
			URLClassLoader urlCL = new URLClassLoader(new URL[]{ pluginURLs[i] });
			URL variantXMLURL = urlCL.findResource(VARIANT_FILE_NAME);
			if(variantXMLURL != null)
			{
				String pluginName = getFile( pluginURLs[i] );
				
				// parse variant description file, and create hash entry of variant object -> URL
				InputStream is = null;
				try
				{
					is = new BufferedInputStream(variantXMLURL.openStream());
					variantParser.parse(is, pluginURLs[i]);
					Variant[] variants = variantParser.getVariants();
					
					// add variants; variants with same name (but older versions) are
					// replaced with same-name newer versioned variants
					for(int vi=0; vi<variants.length; vi++)
					{
						addVariant(variants[vi], pluginName, pluginURLs[i]);
					}
				}
				catch(IOException e)
				{
					// display error dialog
					ErrorDialog.displayFileIO(null, e, pluginURLs[i].toString());
				}
				catch(org.xml.sax.SAXException e)
				{
					// display error dialog
					ErrorDialog.displayGeneral(null, e);
				}
				finally
				{
					if(is != null)
					{
						try { is.close(); } catch (IOException e) {}
					}
				}
			}
		}
		
		
		// if we are in webstart, search for variants within webstart jars
		Enumeration enumeration = null;
		ClassLoader cl = null;
		
		if(vm.isInWebstart)
		{
			cl = vm.getClass().getClassLoader();
			
			try
			{
				enumeration = cl.getResources(VARIANT_FILE_NAME);
			}
			catch(IOException e)
			{
				enumeration = null;
			}
			
			if(enumeration != null)
			{
				while(enumeration.hasMoreElements())
				{
					URL variantURL = (URL) enumeration.nextElement();
					
					// parse variant description file, and create hash entry of variant object -> URL
					InputStream is = null;
					String pluginName = getWSPluginName( variantURL );
					
					try
					{
						is = new BufferedInputStream(variantURL.openStream());
						
						variantParser.parse(is, variantURL);
						Variant[] variants = variantParser.getVariants();
						
						// add variants; variants with same name (but older versions) are
						// replaced with same-name newer versioned variants
						for(int vi=0; vi<variants.length; vi++)
						{
							addVariant(variants[vi], pluginName, variantURL);
						}
					}
					catch(IOException e)
					{
						// display error dialog
						ErrorDialog.displayFileIO(null, e, variantURL.toString());
					}
					catch(org.xml.sax.SAXException e)
					{
						// display error dialog
						ErrorDialog.displayGeneral(null, e);
					}
					finally
					{
						if(is != null)
						{
							try { is.close(); } catch (IOException e) {}
						}
					}
				}
			}// if(enum != null)
		}
		
		// check: did we find *any* variants? Throw an exception.
		if(vm.variantMap.isEmpty())
		{
			StringBuffer msg = new StringBuffer(256);
			msg.append("No variants found on path: ");
			for(int i=0; i<searchPaths.length; i++)
			{
				msg.append(searchPaths[i]);
				msg.append("; ");
			}
			
			throw new NoVariantsException(msg.toString());
		}
		
		Log.printTimed(vptime, "VariantManager: variant parsing time: ");
		
		///////////////// SYMBOLS /////////////////////////
		
		
		// now, parse symbol packs
		XMLSymbolParser symbolParser = new XMLSymbolParser(dbf);
		
		// find plugins, create plugin loader
		pluginURLs = vm.searchForFiles(searchPaths, SYMBOL_EXTENSIONS);
		
		// for each plugin, attempt to find the "variants.xml" file inside. 
		// if it does not exist, we will not load the file. If it does, we will parse it,
		// and associate the variant with the URL in a hashtable.
		for(int i=0; i<pluginURLs.length; i++)
		{
			URLClassLoader urlCL = new URLClassLoader(new URL[]{ pluginURLs[i] });
			URL symbolXMLURL = urlCL.findResource(SYMBOL_FILE_NAME);
			if(symbolXMLURL != null)
			{
				String pluginName = getFile( pluginURLs[i] );
				
				// parse variant description file, and create hash entry of variant object -> URL
				InputStream is = null;
				try
				{
					is = new BufferedInputStream(symbolXMLURL.openStream());
					symbolParser.parse(is, pluginURLs[i]);
					addSymbolPack(symbolParser.getSymbolPack(), pluginName, pluginURLs[i]);
				}
				catch(IOException e)
				{
					// display error dialog
					ErrorDialog.displayFileIO(null, e, pluginURLs[i].toString());
				}
				catch(org.xml.sax.SAXException e)
				{
					// display error dialog
					ErrorDialog.displayGeneral(null, e);
				}
				finally
				{
					if(is != null)
					{
						try { is.close(); } catch (IOException e) {}
					}
				}
			}
		}
		
		// if we are in webstart, search for variants within webstart jars		
		enumeration = null;
		cl = null;
		
		if(vm.isInWebstart)
		{
			cl = vm.getClass().getClassLoader();
			
			try
			{
				enumeration = cl.getResources(SYMBOL_FILE_NAME);
			}
			catch(IOException e)
			{
				enumeration = null;
			}
			
			if(enumeration != null)
			{
				while(enumeration.hasMoreElements())
				{
					URL symbolURL = (URL) enumeration.nextElement();
					
					// parse variant description file, and create hash entry of variant object -> URL
					InputStream is = null;
					String pluginName = getWSPluginName( symbolURL );
					
					try
					{
						is = new BufferedInputStream(symbolURL.openStream());
						symbolParser.parse(is, symbolURL);
						addSymbolPack(symbolParser.getSymbolPack(), pluginName, symbolURL);
					}
					catch(IOException e)
					{
						// display error dialog
						ErrorDialog.displayFileIO(null, e, symbolURL.toString());
					}
					catch(org.xml.sax.SAXException e)
					{
						// display error dialog
						ErrorDialog.displayGeneral(null, e);
					}
					finally
					{
						if(is != null)
						{
							try { is.close(); } catch (IOException e) {}
						}
					}
				}
			}// if(enum != null)
		}// if(isInWebStart)		
		
		
		// check: did we find *any* symbol packs? Throw an exception.
		if(vm.symbolMap.isEmpty())
		{
			StringBuffer msg = new StringBuffer(256);
			msg.append("No SymbolPacks found on path: ");
			for(int i=0; i<searchPaths.length; i++)
			{
				msg.append(searchPaths[i]);
				msg.append("; ");
			}
			
			throw new NoVariantsException(msg.toString());
		}
		Log.printTimed(ttime, "VariantManager: total parsing time: ");
	}// init()
	
	
	/** 
	*	Returns the known Variants. If multiple versions of a Variant
	*	exist, only the latest version is returned. The list is
	*	sorted in alphabetic order.
	*/
	public static synchronized Variant[] getVariants()
	{
		checkVM();
		
		if(vm.variants.length != vm.variantMap.size())
		{
			// note that we need to avoid putting duplicates
			// into the array.
			//
			ArrayList list = new ArrayList();		// list of variants
			Set set = new HashSet();				// a set of MapRecs
			set.addAll(vm.variantMap.values());
			
			// fill variant list with variants.
			Iterator iter = set.iterator();
			while(iter.hasNext())
			{
				MapRec mr = (MapRec) iter.next();
				MapRecObj mro = mr.get(VERSION_NEWEST);
				assert(mro != null);
				list.add( ((VRec) mro).getVariant() );
			}
			
			Collections.sort(list);
			vm.variants = (Variant[]) list.toArray(new Variant[list.size()]);
		}
		
		return vm.variants;
	}// getVariants()
	
	/** 
	*	Returns the known SymbolPacks. If multiple versions of a SymbolPack
	*	exist, only the latest version is returned. The list is
	*	sorted in alphabetic order.
	*/
	public static synchronized SymbolPack[] getSymbolPacks()
	{
		checkVM();
		
		if(vm.symbolPacks.length != vm.symbolMap.size())
		{
			// avoid putting duplicates into the array.
			ArrayList list = new ArrayList();		// list of SymbolPacks
			Set set = new HashSet();				// a set of MapRecs
			set.addAll(vm.symbolMap.values());
			
			// fill variant list with variants.
			Iterator iter = set.iterator();
			while(iter.hasNext())
			{
				MapRec mr = (MapRec) iter.next();
				MapRecObj mro = mr.get(VERSION_NEWEST);
				assert(mro != null);
				list.add( ((SPRec) mro).getSymbolPack() );
			}
			
			Collections.sort(list);
			vm.symbolPacks = (SymbolPack[]) list.toArray(new SymbolPack[list.size()]);
		}
		
		return vm.symbolPacks;
	}// getSymbolPacks()
	
	
	/**
	*	Finds Variant with the given name, or null if no Variant is found. 
	*	Attempts to find the version specified. Versions must be > 0.0f or
	*	the version constants VERSION_NEWEST or VERSION_OLDEST.
	*	<p>
	*	Note: Name is <b>not</b> case-sensitive.
	*
	*/
	public static synchronized Variant getVariant(String name, float version)
	{
		checkVM();
		MapRec mr = (MapRec) vm.variantMap.get(name.toLowerCase());
		if(mr != null)
		{
			return (Variant) ((VRec) mr.get(version)).getVariant();
		}
		
		return null;
	}// getVariant()
	
	
	/**
	*	Finds SymbolPack with the given name, or null if no SymbolPack is found. 
	*	Attempts to find the version specified. Versions must be > 0.0f or
	*	the version constants VERSION_NEWEST or VERSION_OLDEST.
	*	<p>
	*	Note: Name is <b>not</b> case-sensitive.
	*
	*/
	public static synchronized SymbolPack getSymbolPack(String name, float version)
	{
		checkVM();
		if(name == null)
		{
			return null;
		}
		
		MapRec mr = (MapRec) vm.symbolMap.get(name.toLowerCase());
		if(mr != null)
		{
			return (SymbolPack) ((SPRec) mr.get(version)).getSymbolPack();
		}
		
		return null;
	}// getSymbolPack()
	
	/**
	*	Obtains a SymbolPack via the following criteria:
	*	<ol>
	*		<li>If matching SymbolPack name and Version found, that is returned; otherwise</li>
	*		<li>Returns SymbolPack of same name but of the newest available version; otherwise</li>
	*		<li>Returns the newest available SymbolPack preferred by the MapGraphic (if set); otherwise</li>
	*		<li>Returns the first SymbolPack in the list of SymbolPacks.</li>
	*	</ol>
	*	<p>
	*	Thus it is assured that a SymbolPack will always be obtained.
	*/
	public static synchronized SymbolPack getSymbolPack(MapGraphic mg, 
		String symbolPackName, final float symbolPackVersion)
	{
		if(mg == null) { throw new IllegalArgumentException(); }
		
		// safety:
		// if version is invalid (< 0.0f), convert to VERSION_NEWEST 
		// automatically. Log this method, though
		float spVersion = symbolPackVersion;
		if(spVersion <= 0.0f)
		{
			Log.println("WARNING: VariantManager.getSymbolPack() called with symbolPackVersion of <= 0.0f. Check parameters.");
			spVersion = VERSION_NEWEST;
		}
		
		SymbolPack sp = getSymbolPack(symbolPackName, spVersion);
		if(sp == null)
		{
			sp = getSymbolPack(symbolPackName, VERSION_NEWEST);
			if(sp == null && mg.getPreferredSymbolPackName() != null)
			{
				sp = getSymbolPack(mg.getPreferredSymbolPackName(), VERSION_NEWEST);
			}
			
			if(sp == null)
			{
				sp = getSymbolPacks()[0];
			}
		}
		
		return sp;
	}// getSymbolPack()
	
	
	/** 
	*	Returns true if the desired version was found. Version must 
	*	be a positive floating point value, or, a defined constant.
	*	Returns false if the version is not available or the variant
	*	is not found.
	*/
	public static boolean hasVariantVersion(final String name, final float version)
	{
		return (getVariant(name, version) != null);
	}// hasVariantVersion()
	
	/** 
	*	Returns true if the desired version was found. Version must 
	*	be a positive floating point value, or, a defined constant.
	*	Returns false if the version is not available or the SymbolPack
	*	is not found.
	*/
	public static boolean hasSymbolPackVersion(final String name, final float version)
	{
		return (getSymbolPack(name, version) != null);
	}// hasVariantVersion()
	
	
	/** 
	*	Returns the versions of a variant that are available. 
	*	If the variant is not found, a zero-length array is returned.
	*/
	public synchronized static float[] getVariantVersions(final String name)
	{
		checkVM();
		MapRec mr = (MapRec) vm.variantMap.get(name.toLowerCase());
		if(mr != null)
		{
			return ( mr.getVersions() );
		}
		
		return new float[0];
	}// getVariantVersions()
	
	/** 
	*	Returns the versions of a SymbolPack that are available. 
	*	If the SymbolPack is not found, a zero-length array is returned.
	*/
	public synchronized static float[] getSymbolPackVersions(String name)
	{
		checkVM();
		MapRec mr = (MapRec) vm.symbolMap.get(name.toLowerCase());
		if(mr != null)
		{
			return ( mr.getVersions() );
		}
		
		return new float[0];
	}// getSymbolPackVersions()
	
	
	/** Ensures version is positive OR VERSION_NEWEST or VERSION_OLDEST */
	private static void checkVersionConstant(float version)
	{
		if(version <= 0.0f && (version != VERSION_NEWEST && version != VERSION_OLDEST))
		{
			throw new IllegalArgumentException("invalid version or version constant: "+version);
		}
	}// checkVersionConstant()
	
	/** 
	*	Gets a specific resource for a Variant or a SymbolPack, given a URL to
	*	the package and a reference URI. Threadsafe. 
	*	<p>
	*	Typically, getResource(Variant, URI) or getResource(SymbolPack, URI) is
	*	preferred to this method.
	*
	*/
	public static synchronized URL getResource(URL packURL, URI uri)
	{
		// ensure we have been initialized...
		checkVM();
		
		// if we are in webstart, assume that this is a webstart jar. 
		if(vm.isInWebstart)
		{
			URL url = getWSResource(packURL, uri);
			
			// if cannot get it, fall through.
			if(url != null)
			{
				return url;
			}
		}
		
		// if URI has a defined scheme, convert to a URL (if possible) and return it.
		if(uri.getScheme() != null)
		{
			try
			{
				return uri.toURL();
			}
			catch(MalformedURLException e)
			{
				return null;
			}
		}
		
		// resolve & load.
		URLClassLoader classLoader = getClassLoader(packURL);
		return classLoader.findResource(uri.toString());
	}// getResource()
	
	
	/**
	*	Gets a specific resource by properly resolving the URI
	*	to this Variant. Null arguments are illegal. Returns 
	*	null if the resource cannot be resolved. Threadsafe.
	*/
	public static URL getResource(Variant variant, URI uri)
	{
		if(variant == null) { throw new IllegalArgumentException(); }
		return getResource(getVRec(variant), uri);
	}// getResource()
	
	
	/**
	*	Gets a specific resource by properly resolving the URI
	*	to this SymbolPack. Null arguments are illegal. Returns 
	*	null if the resource cannot be resolved. Threadsafe.
	*/
	public static URL getResource(SymbolPack symbolPack, URI uri)
	{
		if(symbolPack == null) { throw new IllegalArgumentException(); }
		return getResource(getSPRec(symbolPack), uri);
	}// getResource()
	
	
	/** 
	*	Gets the URL to the Variant package (plugin). This is typically
	*	only needed in special circumstances. Returns null if null variant
	*	input OR variant not found.
	*	<p>
	*	Note that this will always return a URL with a JAR prefix.
	*	e.g.: <code>jar:http:/the.location/ajar.zip!/</code> 
	*	or <code>jar:file:/c:/plugins/ajar.zip!/</code>
	*/
	public static URL getVariantPackageJarURL(Variant variant)
	{
		if(variant != null)
		{
			VRec vr = getVRec(variant);
			if(vr != null)
			{
				assert(vr.getURL() != null);
				
				URL url = vr.getURL();
				String txtUrl = url.toString();
				
				if(txtUrl.startsWith("jar:"))
				{
					return url;
				}
				else
				{
					StringBuffer sb = new StringBuffer(txtUrl.length()+8);
					sb.append("jar:");
					sb.append(txtUrl);
					sb.append("!/");
					
					try
					{
						return new URL(sb.toString());
					}
					catch(MalformedURLException e)
					{
						Log.println("Could not convert ", url, " to a JAR url.");
						Log.println("Exception: ", e);
					}
				}
			}
		}
		
		return null;
	}// getVariantPackageURL()
	
	/** 
	*	Internal getResource() implementation
	*/
	private static synchronized URL getResource(MapRecObj mro, URI uri)
	{
		// ensure we have been initialized...
		checkVM();
		assert(mro != null);
		
		if(uri == null)
		{
			throw new IllegalArgumentException("null URI");
		}
		
		// if we are in webstart, assume that this is a webstart jar. 
		if(vm.isInWebstart)
		{
			URL url = getWSResource(mro, uri);
			
			// if cannot get it, fall through.
			if(url != null)
			{
				return url;
			}
		}
		
		
		// if URI has a defined scheme, convert to a URL (if possible) and return it.
		if(uri.getScheme() != null)
		{
			try
			{
				return uri.toURL();
			}
			catch(MalformedURLException e)
			{
				return null;
			}
		}
		
		// find the URL
		if(mro.getURL() != null)
		{
			return getClassLoader(mro.getURL()).findResource(uri.toString());
		}
		
		return null;
	}// getResource()
	
	
	/** Ensures that we have initialized the VariantManager */
	private static void checkVM()
	{
		if(vm == null)
		{
			throw new IllegalArgumentException("not initialized");
		}
	}// checkVM()
	
	
	/** Singleton */
	private VariantManager()
	{
		variantMap = new HashMap(53);
		symbolMap = new HashMap(17);
		isInWebstart = isInWebstart();
	}// VariantManager()
	
	
	/**
	*	Searches the given paths for files ending with the given extension(s).
	*	Returns URLs.
	*
	*/
	private URL[] searchForFiles(final File[] searchPaths, final String[] extensions)
	{
		List urlList = new LinkedList();
		
		for(int spIdx=0; spIdx<searchPaths.length; spIdx++)
		{
			File[] list = searchPaths[spIdx].listFiles();
			
			// internal error if list == null; means that 
			// searchPaths[] is not a directory!
			if(list != null)
			{
				for(int i=0; i<list.length; i++)
				{
					if(list[i].isFile())
					{
						String fileName = list[i].getPath();
						
						if(checkFileName(fileName, extensions))
						{
							try
							{
                                                            /*
                                                             * NOTE-RGO: "toURL" is deprecated I change it to ".toURI().toURL()" instead
                                                             */
                                                            urlList.add(list[i].toURI().toURL());
							}
							catch(java.net.MalformedURLException e)
							{
								// do nothing; we just won't add it
							}
						}
					}
				}
			}
		}
		
		return (URL[]) urlList.toArray(new URL[urlList.size()]);
	}// searchForFiles()
	
	
	/** Returns the URLClassLoader for a given URL, or creates a new one.... */
	private static URLClassLoader getClassLoader(URL packageURL)
	{
		// WARNING: this method is not (itself) threadsafe
		if(packageURL == null)
		{
			throw new IllegalArgumentException();
		}
		
		// see if a classloader for this url already exists (cache of 1)
		if(packageURL.equals(vm.currentPackageURL))
		{
			return vm.currentUCL;
		}
		
		vm.currentUCL = new URLClassLoader( new URL[]{packageURL} );
		vm.currentPackageURL = packageURL;
		return vm.currentUCL;
	}// getClassLoader()
	
	
	/** Returns the "file" part of the URL; e.g.: x/y/z.jar, returns z.jar */
	private static String getFile(URL url)
	{
		String s = url.toString();
		return s.substring(s.lastIndexOf("/")+1, s.length());
	}// getFile()
	
	/** Get the webstart plugin name */
	private static String getWSPluginName(URL url)
	{
		final String s = url.toString();
		final int idxExclam = s.indexOf('!');
		if(idxExclam >= 0)
		{
			return s.substring(s.lastIndexOf("/", idxExclam) + 1, idxExclam);
		}
		else
		{
			return s;
		}
	}// getWSPluginName()
	
	/** Checks if the fileName ends with an allowed extension; if so, returns true. */
	private boolean checkFileName(String fileName, String[] extensions)
	{
		for(int i=0; i<extensions.length; i++)
		{
			if(fileName.endsWith(extensions[i]))
			{
				return true;
			}
		}
		
		return false;
	}// checkFileName()
	
	
	
	
	/** See if we are running under Java Webstart */
	private boolean isInWebstart()
	{
		// this is not the most optimal code.
		try
		{
			ServiceManager.lookup("javax.jnlp.BasicService");
			return true;
		}
		catch(Throwable e)
		{
			return false;
		}
	}// isInWebstart()
	
	
	/** 
	*	Get a resource for a variant. This uses the variantName to 
	*	deconflict, if multiple resources exist with the same name.
	*	<p>
	*	Conflict occur when plugins are loaded under the same ClassLoader,
	*	because variant plugin namespace is not unique.
	*	<p>
	*	This primarily applies to Webstart resources
	*/
	private static URL getWSResource(MapRecObj mro, URI uri)
	{
		assert(vm.isInWebstart);
		
		if(uri == null)
		{
			return null;
		}
		
		ClassLoader cl = vm.getClass().getClassLoader();
		
		if(mro != null)
		{
			Enumeration enumeration = null;
			
			try
			{
				enumeration = cl.getResources(uri.toString());
			}
			catch(IOException e)
			{
				return null;
			}
			
			while(enumeration.hasMoreElements())
			{
				URL url = (URL) enumeration.nextElement();
				
				// deconflict. Note that this is not, and cannot be, foolproof;
				// due to name-mangling by webstart. For example, if two plugins
				// called "test" and "Supertest" exist, test may find the data
				// file within Supertest because indexOf(test, SuperTest) >= 0
				// 
				// however, if we can get the mangled name and set it as the
				// 'pluginName', we can be foolproof.
				//
				String lcPath = url.getPath();
				String search = mro.getPluginName() + "!";
				
				if(lcPath.indexOf(search) >= 0)
				{
					return url;
				}
			}
		}
		
		return null;
	}// getWSResource()
	
	
	/** 
	*	Get a resource for a variant. This uses the variantName to 
	*	deconflict, if multiple resources exist with the same name.
	*	<p>
	*	Conflict occur when plugins are loaded under the same ClassLoader,
	*	because variant plugin namespace is not unique.
	*	<p>
	*	This primarily applies to Webstart resources
	*/
	private static URL getWSResource(URL packURL, URI uri)
	{
		/*
			NOTE: this method is used by getResource(URL, URI), which is
			chiefly used by VariantManager and associated parsers; a VariantRecord
			has not yet been created. So we cannot use that; the internal
			logic here is slightly different.
		*/
		assert(vm.isInWebstart);
		
		ClassLoader cl = vm.getClass().getClassLoader();
		String deconflictName = getWSPluginName(packURL);
		
		Enumeration enumeration = null;
		
		try
		{
			enumeration = cl.getResources(uri.toString());
		}
		catch(IOException e)
		{
			return null;
		}
		
		while(enumeration.hasMoreElements())
		{
			URL url = (URL) enumeration.nextElement();
			
			// deconflict. Note that this is not, and cannot be, foolproof;
			// due to name-mangling by webstart. For example, if two plugins
			// called "test" and "Supertest" exist, test may find the data
			// file within Supertest because indexOf(test, SuperTest) >= 0
			// 
			// however, if we can get the mangled name and set it as the
			// 'pluginName', we can be foolproof.
			//
			String lcPath = url.getPath();
			if(lcPath.indexOf(deconflictName) >= 0)
			{
				return url;
			}
		}
		
		return null;
	}// getWSResource()
	
	
	
	/** 
	*	Adds a Variant. If the variant already exists with the same
	*	name, checks the version. If the same version already exists,
	*	an exception is thrown. If not, the new version is also added.
	*	If we are in Web Start, however, no exception is thrown.
	*	<p>
	*	All names and aliases are mapped to the MapRec, not the VRec.
	*	When mapping an alias, if it corresponds to a DIFFERENT
	*	MapRec, an exception is thrown (this represents a non-unique
	*	alias).
	*	<p>
	*	NOTE: names and aliases are always mapped in all lower case.
	*/
	private static void addVariant(Variant v, String pluginName, URL pluginURL)
	throws IOException
	{
		if(v == null || pluginName == null || pluginURL == null)
		{
			throw new IllegalArgumentException();
		}
		
		VRec vr = new VRec();
		vr.setPluginName(pluginName);
		vr.setURL(pluginURL);
		vr.setVariant(v);
		
		final String vName = v.getName().toLowerCase();
		
		// see if we are mapped to a MapRec already.
		//
		MapRec mapRec = (MapRec) vm.variantMap.get(vName);
		if(mapRec == null)
		{
			// not yet mapped! let's map it.
			mapRec = new MapRec(vr);
			vm.variantMap.put(vName, mapRec);
		}
		else
		{
			// we are mapped. See if this version has been added. 
			// If not, we'll add it.
			if(!mapRec.add(vr) && !vm.isInWebstart)
			{
				final VRec vrec2 = (VRec) mapRec.get(v.getVersion());
				final Variant v2 = vrec2.getVariant();
				
				// 2 variants with identical versions! we are confused!
				// try to provide as much helpful info as possible.
				throw new IOException(
					"Two variants with identical version numbers have been found.\n"+
					"Conflicting version: "+v.getVersion()+"\n"+
					"Variant 1: name="+v.getName()+"; pluginName = "+vr.getPluginName()+"; pluginURL = "+vr.getURL()+"\n"+
					"Variant 2: name="+v2.getName()+"; pluginName = "+vrec2.getPluginName()+"; pluginURL = "+vrec2.getURL()+"\n"
				);
			}
		}
		
		// map the aliases and/or check that aliases refer to the
		// same MapRec (this prevents two different Variants with the same
		// alias from causing a subtle error)
		//
		final String[] aliases =  v.getAliases();
		for(int idx=0; idx<aliases.length; idx++)
		{
			// not if it's "" though...
			if( !"".equals(aliases[idx]) )
			{
				final String alias = aliases[idx].toLowerCase();
				MapRec testMapRec = (MapRec) vm.variantMap.get(alias);
				if(testMapRec == null)
				{
					// add alias
					vm.variantMap.put(alias, mapRec);
				}
				else if(testMapRec != mapRec)
				{
					// ERROR! incorrect alias map
					final Variant v2 = ((VRec) testMapRec.get(VERSION_OLDEST)).getVariant();
					throw new IOException(
						"Two variants have a conflicting (non-unique) alias.\n"+
						"Variant 1: name="+v.getName()+"; version="+v.getVersion()+
								"; pluginName = "+vr.getPluginName()+"; pluginURL = "+vr.getURL()+"\n"+
						"Variant 2: name="+v2.getName()+"; (must check all variants with this name)\n"
					);
				}
				// else {} : we are already mapped correctly. Nothing to change.
			}
		}
	}// addVariant()
	
	/** 
	*	Adds a SymbolPack. If the SymbolPack already exists with the same
	*	name, checks the version. If the same version already exists,
	*	an exception is thrown. If not, the new version is also added.
	*	<p>
	*	SymbolPacks do not support aliases.
	*	<p>
	*	Names are always mapped in all lower case.
	*/
	private static void addSymbolPack(SymbolPack sp, String pluginName, URL pluginURL)
	throws IOException
	{
		if(sp == null || pluginName == null || pluginURL == null)
		{
			throw new IllegalArgumentException();
		}
		
		SPRec spRec = new SPRec();
		spRec.setPluginName(pluginName);
		spRec.setURL(pluginURL);
		spRec.setSymbolPack(sp);
		
		final String spName = sp.getName().toLowerCase();
		
		// see if we are mapped to a MapRec already.
		//
		MapRec mapRec = (MapRec) vm.symbolMap.get(spName);
		if(mapRec == null)
		{
			// not yet mapped! let's map it.
			mapRec = new MapRec(spRec);
			vm.symbolMap.put(spName, mapRec);
		}
		else
		{
			// we are mapped. See if this version has been added. 
			if(!mapRec.add(spRec) && !vm.isInWebstart)
			{
				SPRec spRec2 = (SPRec) mapRec.get(sp.getVersion());
				final SymbolPack sp2 = spRec2.getSymbolPack();
				if(sp2.getVersion() == sp.getVersion())
				{
					// 2 SymbolPacks with identical versions! we are confused!
					// try to provide as much helpful info as possible.
					throw new IOException(
						"Two SymbolPcaks with identical version numbers have been found.\n"+
						"Conflicting version: "+sp.getVersion()+"\n"+
						"SymbolPack 1: name="+sp.getName()+"; pluginName = "+spRec.getPluginName()+"; pluginURL = "+spRec.getURL()+"\n"+
						"SymbolPack 2: name="+sp2.getName()+"; pluginName = "+spRec2.getPluginName()+"; pluginURL = "+spRec2.getURL()+"\n"
					);
				}
			}
			
			// we haven't been added (not a dupe); add
			mapRec.add(spRec);
		}
	}// addSymbolPack()
	
	
	
	/** Gets the VRec associated with a Variant (via name and version) */
	private static VRec getVRec(Variant v)
	{
		MapRec mapRec = (MapRec) vm.variantMap.get(v.getName().toLowerCase());
		return (VRec) mapRec.get(v.getVersion());
	}// getVRec()
	
	
	/** Gets the SPRec associated with a SymbolPack (via name and version) */
	private static SPRec getSPRec(SymbolPack sp)
	{
		MapRec mapRec = (MapRec) vm.symbolMap.get(sp.getName().toLowerCase());
		return (SPRec) mapRec.get(sp.getVersion());
	}// getSPRec()
	
	/** The value which is stored within the name mapping */
	private static class MapRec
	{
		private ArrayList list = new ArrayList(2);
		
		// this constructor prevents us from having an empty list.
		public MapRec(MapRecObj obj)
		{ 
			if(obj == null) { throw new IllegalArgumentException(); }
			list.add(obj); 
		}// MapRec()
		
		public int size()				{ return list.size(); }
		
		/**
		*	Adds the MapRecObj to this MapRec, but only if it is of
		*	a unique version. If it is not, returns false. Otherwise,
		*	the MapRecObj is added and returns true.
		*/
		public boolean add(MapRecObj obj)	
		{ 
			for(int i=0; i<list.size(); i++)
			{
				MapRecObj temp = (MapRecObj) list.get(i);
				if(temp.getVersion() == obj.getVersion())
				{
					return false;
				}
			}
			
			list.add(obj); 
			return true;
		}// add()
		
		/** Get all available versions */
		public float[] getVersions()	
		{
			final float[] versions = new float[list.size()];
			for(int i=0; i<list.size(); i++)
			{
				MapRecObj mro = (MapRecObj) list.get(i);
				versions[i] = mro.getVersion();
			}
			
			return versions;
		}// getVersions()
		
		/** 
		*	Get the desired version. Supports version constants. 
		*	Returns null if version not found (shouldn't occur if
		*	version constants used, and at least one element exists)
		*/
		public MapRecObj get(final float version)
		{
			checkVersionConstant(version);
			
			final int size = list.size();
			
			// typical-case
			if(size == 1 && (version == VERSION_OLDEST || version == VERSION_NEWEST))
			{
				return (MapRecObj) list.get(0);
			}
			
			MapRecObj selected = null;
			for(int i=0; i<size; i++)
			{
				MapRecObj mro = (MapRecObj) list.get(i);
				selected = (selected == null) ? mro : selected;
				
				if(	(version == VERSION_OLDEST && mro.getVersion() < selected.getVersion()) ||
					(version == VERSION_NEWEST && mro.getVersion() > selected.getVersion()) )
				{
					selected = mro;
				}
				else if(mro.getVersion() == version)
				{
					return mro;
				}
			}
			
			return selected;
		}// get()
		
	}// inner class VMRec
	
	
	/** MapRec stores a list of ObjRecs */ 
	private static abstract class MapRecObj
	{
		private URL fileURL;
		private String pluginName;
		
		public String getPluginName()			{ return pluginName; }
		public void setPluginName(String value) { pluginName = value; }
		
		public URL getURL()				{ return fileURL; }
		public void setURL(URL value)	{ fileURL = value; }
		
		public abstract float getVersion();
		
	}// inner class ObjRec
	
	
	/** An ObjRec for Variant objects */
	private static class VRec extends MapRecObj
	{
		private Variant variant;
		
		public Variant getVariant()				{ return variant; }
		public void setVariant(Variant value)	{ variant = value; }
		public float getVersion()				{ return variant.getVersion(); }
	}// inner class VRec
	
	/** An ObjRec for SymbolPack objects */
	private static class SPRec extends MapRecObj
	{
		private SymbolPack symbolPack;
		
		public SymbolPack getSymbolPack()				{ return symbolPack; }
		public void setSymbolPack(SymbolPack value)		{ symbolPack = value; }
		public float getVersion()						{ return symbolPack.getVersion(); }
	}// inner class SPRec
	
}// class VariantManager
