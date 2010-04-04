//
//  @(#)Utils.java		4/2002
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
package dip.misc;

import dip.gui.swing.XJEditorPane;
import dip.gui.dialog.ErrorDialog;

import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.JFormattedTextField;
import javax.swing.JTextField;
import javax.swing.text.*;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Insets;
import java.awt.MediaTracker;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.image.AreaAveragingScaleFilter;
import java.awt.image.FilteredImageSource;
import java.awt.image.ImageProducer;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ResourceBundle;
import java.util.MissingResourceException;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.net.URL;
import java.text.MessageFormat;
import javax.swing.text.Document;
import javax.swing.text.html.HTMLDocument;


/**
*	Various static utilities used by GUI and non-GUI classes.
*	<pre>
*	NOTE:
*		to go from a String resource to a URL, use 
*		Utils.getURL()
*			e.g., for a resource, called common/test.txt
*		to use with Utils.getText():
*			String RESOURCE = "resource/common/test.txt";
*			
*			String myText = Utils.getText(RESOURCE);
*		to get the URL:
*			URL url = Utils.getURL(RESOURCE);
*			JeditorPane editorPane.setPage(url);
*	</pre>
*/
public class Utils
{
	// public constants
	public static final Border EMPTY_BORDER_5 = new EmptyBorder(5,5,5,5);
	public static final Border EMPTY_BORDER_10 = new EmptyBorder(10,10,10,10);
	
	// private resource constants
	public static final String 	UTILS_RES_NOT_FOUND = "UTILS_RES_NOT_FOUND";
	public static final String 	UTILS_RES_ERR_DLG_TITLE = "UTILS_RES_ERR_DLG_TITLE";
	public static final String 	UTILS_RES_ERR_DLG_TEXT = "UTILS_RES_ERR_DLG_TEXT";
	
	public static final String FRAME_ICON = "resource/common/icons/frame-corner.png";
	
	// private constants
	private static final int TEXT_INSETS = 5;
	private static final String HEX[] = {"0","1","2","3","4","5","6","7","8","9","a","b","c","d","e","f"};
	private static final String RESOURCE_BASE_DIR = "resource/";
	private static final String BASE_RESOURCE_FILE = "resource/il8n/i18ntext";
	private static final String COMMON_RESOURCE_FILE = "resource/common/common";
	private static final char[] EMAIL_ALLOWED = {'@','.','-','_','+','!'};
	private static final char[] URL_ALLOWED = {' ',';','/','?',':','@','&','=','+','$',',','-','_','.','!','~','*','\'','|','%','#' };
	
	// private regex
	private static final Pattern REAL_COMMAS = Pattern.compile(",(?=([^\"]*\"[^\"]*\")*(?![^\"]*\"))");
	
	
	private static ClassLoader classLoader = null;
	private static Utils singleton = null; 
	private static ResourceBundle resourceBundle = null;
	private static ResourceBundle commonBundle = null;
	private static Toolkit toolkit = null;
	private final static Component component = new Component() {};
	private final static MediaTracker tracker = new MediaTracker(component);
	private static Locale chosenLocale = null;
	private final static boolean isOSX;
	private final static boolean isWindows;
	
	// static code
	static
	{
		singleton = new Utils();
		classLoader = singleton.getClass().getClassLoader();
		toolkit = Toolkit.getDefaultToolkit();
		
		isOSX = (System.getProperty("mrj.version", null) != null);
		isWindows = (System.getProperty("os.name","").toLowerCase().indexOf("windows") >= 0);
		
		// if a locale cannot be found, automatically defaults
		// to the closest locale, or (at worst) BASE_RESOURCE_FILE.
		chosenLocale = Locale.getDefault();
		setResourceBundle(chosenLocale);
		
		// now set the COMMON_RESOURCE_FILE
		// this is not localized
		try
		{
			commonBundle = ResourceBundle.getBundle(COMMON_RESOURCE_FILE, Locale.ENGLISH, classLoader);	
		}
		catch(MissingResourceException mre)
		{
			ErrorDialog.displayFatal(null, mre);
		}
	}
	
	/** Force a load of a locale */
	public static void loadLocale(Locale loc)
	{
		chosenLocale = loc;
		setResourceBundle(chosenLocale);
	}// loadLocale()
	
	
	/** Get which Locale has been loaded or selected. */
	public static Locale getLocale()
	{
		return chosenLocale;
	}// getLocale()
	
	/** Gets the classloader used for this class */
	public static ClassLoader getClassLoader()
	{
		return classLoader;
	}// getClassLoader()
	
	
	/** Return the screen size. */
	public static Dimension getScreenSize()
	{
		return getScreenSize(1.0f);
	}// getScreenSize()
	
	
	/**
	*	Returns the screen size multiplied by the given fraction, 
	*	preserving the aspect ratio, and returns the result.
	*	<p>
	*	For example, getScreenSize(0.5f) would return a Dimension
	*	that was 400 x 300 if the screen size was 800 x 600 pixels.
	*/
	public static Dimension getScreenSize(float fraction)
	{
		if(fraction <= 0)
		{
			throw new IllegalArgumentException("fraction <= 0");
		}
		
		Dimension size = toolkit.getScreenSize();
		size.width = (int) (size.width * fraction);
		size.height = (int) (size.height * fraction);
		return size;
	}// getScreenSize()
	
	
	/**
	*	Returns the screen size multiplied by the given fraction, 
	*	preserving the aspect ratio, and returns the result.
	*	<p>
	*	This is a hackish version, with two fractions. The second fraction
	*	is used for 'smaller' screens (defined as 800x600 or less).
	*	
	*/
	public static Dimension getScreenSize(float fraction, float smallScreenFraction)
	{
		if(fraction <= 0 || smallScreenFraction <= 0 || smallScreenFraction < fraction)
		{
			throw new IllegalArgumentException("fraction <= 0");
		}
		
		Dimension size = toolkit.getScreenSize();
		
		final float f = (size.width <= 800 || size.height <= 600) ? smallScreenFraction : fraction;
		
		size.width = (int) (size.width * f);
		size.height = (int) (size.height * f);
		return size;
	}// getScreenSize()
	
	
	/**
	*	Centers the component in the screen. If component is 
	*	larger than the screen in a particular axis, then that
	*	axis is centered to the component.
	*/
	public static void centerInScreen(Component c)
	{
		Dimension screenSize = toolkit.getScreenSize();
		Dimension componentSize = c.getSize();		
		
		componentSize.width = (componentSize.width > screenSize.width) ? screenSize.width : componentSize.width;
		componentSize.height = (componentSize.height > screenSize.height) ? screenSize.height : componentSize.height;
		
		c.setLocation((screenSize.width - componentSize.width)/2, (screenSize.height - componentSize.height)/2);
	}// centerInScreen()
	
	
	/** 
	*	Centers the inner component within the outer component.
	*	If the outer component is null, or if the inner component
	*	is larger than the outer component (in either axis), the 
	*	component is centered to the screen using centerInScreen().
	*/
	public static void centerIn(Component inner, Component outer)
	{
		if(outer == null)
		{
			centerInScreen(inner);
			return;
		}
		
		Dimension parentSize = outer.getSize();
		Point pLoc = outer.getLocationOnScreen();
		Dimension componentSize = inner.getSize();
		
		if(componentSize.width > parentSize.width || componentSize.height > parentSize.height)
		{
			centerInScreen(inner);
		}
		else
		{
			inner.setLocation(pLoc.x+(parentSize.width - componentSize.width)/2, pLoc.y+(parentSize.height - componentSize.height)/2);
		}
	}// centerIn()	
	
	
	/**
	*	Appends the extension to the file, unless it was already
	*	appended. Will also use a '.' to separate if not contained
	*	in the extension.
	*/
	public static File appendExtension(File file, String ext)
	{
		String name = file.getName();
		if(name.endsWith(ext))
		{
			return file;
		}
		
		StringBuffer sb = new StringBuffer(file.getPath());
		if(ext.charAt(0) != '.')
		{
			sb.append('.');
		}
		sb.append(ext);
		
		return new File(sb.toString());
	}// appendExtension()
	
	
	/** Create a &lt;font&gt; tag with the given color. */
	public static void setFontColor(StringBuffer sb, Color color)
	{
		setFontColor(sb, color.getRed(), color.getGreen(), color.getBlue());
	}// setFontColor()
	
	
	/** Create a &lt;font&gt; tag with the given color. */
	public static void setFontColor(StringBuffer sb, int r, int g, int b)
	{
		sb.append("<font color=\"#");
		fastToHex(sb, r);
		fastToHex(sb, g);
		fastToHex(sb, b);
		sb.append("\">");		
	}// setFontColor()	
	
	
	/** Popup an Error message dialog */
	public static void popupError(JFrame parent, String title, String text)
	{
		JOptionPane.showMessageDialog(parent, text, title, JOptionPane.ERROR_MESSAGE);
	}// popupError()
	
	/** Popup an Info message dialog */
	public static void popupInfo(JFrame parent, String title, String text)
	{
		JOptionPane.showMessageDialog(parent, text, title, JOptionPane.INFORMATION_MESSAGE);
	}// popupError()
	
	
	
	/**
	* 	Gets the Resource Base (resource directory). This is needed 
	*	for setting things like setBase() on HTML docuements.
	*/
	public static URL getResourceBase()
	{
		if(classLoader == null)
		{
			return null;
		}
		
		return classLoader.getResource(RESOURCE_BASE_DIR);		
	}// getResourceBase()
	
	
	/** Get the resource base prefix. */
	public static String getResourceBasePrefix()
	{
		return RESOURCE_BASE_DIR;
	}// getResourceBasePrefix()
	
	
	/********************************************************************
	 *
	 *	Given a resource name, extract a URL for the given resource.
	 *
	 ********************************************************************/	
	public static URL getURL(String name)
	{
		if(classLoader == null)
		{
			return null;
		}
		
		return classLoader.getResource(name);
	}// getURL()	
	
	
	
	/********************************************************************
	 *
	 * Get an ImageIcon. Return null if an ImageIcon could not be found.
	 *
	 ********************************************************************/	
	public static ImageIcon getImageIcon(String name) 
	{
		URL url = getURL(name);
		if(url != null)
		{
			return new ImageIcon( url );		
		}
		
		return null;
	}// getImageIcon()
	
	
	/********************************************************************
	 *
	 * Gets an InputStream to the named resource; throws an exception
	 * if the resource could not be found.
	 *
	 ********************************************************************/	
	public static InputStream getInputStream(String name) 
	throws java.io.IOException 
	{
		URL url = getURL(name);
		if(url != null)
		{
			return url.openStream();
		}
		
		throw new FileNotFoundException( getLocalString(UTILS_RES_NOT_FOUND, name) );
	}// getInputStream()
	
	
	/********************************************************************
	 *
	 * Gets an InputStreamReader to the named resource.
	 *
	 ********************************************************************/	
	public static InputStreamReader getInputStreamReader(String name) 
		throws java.io.IOException 
	{
		URL url = getURL(name);
		if(url != null)
		{
			return new InputStreamReader(url.openStream());		
		}
		
		throw new FileNotFoundException( getLocalString(UTILS_RES_NOT_FOUND, name) );
	}// getInputStreamReader()	
	
	
	/********************************************************************
	 *
	 * Gets a resource as a String.<p>
	 * Suitable for small text files / HTML files. May not be
	 * appropriate for larger text files.
	 * <p>
	 * A null string is returned if an error occurs.
	 *
	 ********************************************************************/	
	public static String getText(String name)
	{
		BufferedReader br = null;
		StringBuffer sb = null;
		
		try
		{
			br = new BufferedReader(getInputStreamReader(name));
			sb = new StringBuffer(4096);
			
			String line = br.readLine();
			while(line != null)
			{
				sb.append(line);
				line = br.readLine();
			}
			
			return sb.toString();
		}
		catch(IOException e)
		{
		}
		finally
		{
			if(br != null)
			{
				try
				{
					br.close();
				}
				catch(IOException e)
				{}
			}
		}			
		
		return null;
	}// getText()
	
	/********************************************************************
	 *
	 * 	Gets a resource as a String.<p>
	 *	Uses MessageFormat to replace the given argument with 
	 *	the argument provided.
	 *
	 *
	 ********************************************************************/	
	public static String getText(String name, Object arg1)
	{
		return MessageFormat.format(getText(name), new Object[] {arg1});
	}// getText()
	
	/********************************************************************
	 *
	 * 	Gets a resource as a String.<p>
	 *	Uses MessageFormat to replace the given argument with 
	 *	the arguments provided.
	 *
	 *
	 ********************************************************************/	
	public static String getText(String name, Object[] args)
	{
		return MessageFormat.format(getText(name), args);
	}// getText()
	
	
	/********************************************************************
	 *
	 * Gets a resource as an Image.<p>
	 * Synchronous; may not suitable for large images.
	 * <p>
	 * null if error
	 *
	 ********************************************************************/	
	public static Image getImage(String name)
	{
		URL url = getURL(name);
		if(url != null)
		{
			return syncCreateImage(toolkit.getImage(url));
		}
		
		return null;
	}// getImage()
	
	
	/**
	*
	* Returns an Image from the given URL.<p>
	* Synchronous; may not suitable for large images.
	* <p>
	* null if error
	*
	*/
	public static Image getImage(URL url)
	{
		if(url != null)
		{
			return syncCreateImage(toolkit.getImage(url));
		}
		
		return null;
	}// getImage()
	
	
	
	/********************************************************************
	 *
	 * Gets a resource as an Icon.<p>
	 * Synchronous; may not suitable for large images.
	 * <p>
	 * null if error
	 *
	 ********************************************************************/	
	public static Icon getIcon(String name)
	{
		URL url = getURL(name);
		if(url != null)
		{
			return new ImageIcon(url);
		}
		
		return null;
	}// getImage()

	
	/** 
	* Ensure that the entire image has been created.
	* <p>
	* This method ensures that the Image desired has been 
	* fully realized (i.e., loaded or created) before 
	* returning a reference to it. If an error occurs, the
	* returned Image will be <code>null</code>.
	*
	* @param ip ImageProducer image source.
	*
	* @return the completed Image object.
	*/
	public static Image syncCreateImage(ImageProducer ip)
	{
		Image img = toolkit.createImage(ip);
		return syncCreateImage(img);
	}// syncCreateImage()


	/** 
	* Ensure that the entire image has been created.
	* <p>
	* This method ensures that the Image desired has been 
	* fully realized (i.e., loaded or created) before 
	* returning a reference to it. If an error occurs, the
	* returned Image will be <code>null</code>.
	*
	* @param img the image source.
	*
	* @return the completed Image object.
	*/
	public static Image syncCreateImage(Image img)
	{		
		synchronized(tracker)
		{
			tracker.addImage(img, 0);
			
			try
			{
				tracker.waitForID(0);
				tracker.removeImage(img, 0);
			}
			catch(InterruptedException e)
			{
				return null;
			}
			
			if(tracker.isErrorID(0))
			{
				return null;
			}
		}		
		return img;
	}// syncCreateImage()	
	
	
	
	
	
	/********************************************************************
	 *
	 * Gets a resource-bundle String; this is for internationalization.
	 * <p>
	 * If resource is missing, a popup-error message is displayed.
	 * <p>.
	 *
	 ********************************************************************/	
	public static String getLocalString(String key)
	{
		try
		{
			return resourceBundle.getString(key);
		}
		catch(Exception e)
		{
			showNoLocalStringPopup(key, e);
		}
		
		return "[i18n:ERROR]";
	}// getLocalString()
	
	
	
	/********************************************************************
	 *
	 * Gets an array of resource-bundle Strings, for internationalization.
	 * <p>
	 * The Strings must be comma-separated. Spaces before/after are trimmed.
	 * Quotes have no special meaning.
	 * <p>.
	 *
	 ********************************************************************/	
	public static String[] getLocalStringArray(String key)
	{
		String str = null;
		
		try
		{
			str = resourceBundle.getString(key);
		}
		catch(Exception e)
		{
			showNoLocalStringPopup(key, e);
			return new String[0];
		}
		
		StringTokenizer st = new StringTokenizer(str, ",\n\r");
		String[] array = new String[st.countTokens()];
		for(int i=0; i<array.length; i++)
		{
			array[i] = st.nextToken().trim();
		}
		
		return array;
	}// getLocalStringArray()
	
	/********************************************************************
	 *
	 * Gets an array of resource-bundle ints, for internationalization.
	 * <p>
	 * The Integers must be comma-separated. Spaces before/after are trimmed.
	 * Quotes have no special meaning.
	 * <p>.
	 *
	 ********************************************************************/	
	public static int[] getLocalIntArray(String key)
	{
		String[] str = getLocalStringArray(key);
		int[] array = new int[str.length];
		
		try
		{
			for(int i=0; i<array.length; i++)
			{
				array[i] = Integer.parseInt(str[i]);
			}
		}
		catch(NumberFormatException e)
		{
			showNoLocalStringPopup(key, e);
		}
		
		return array;
	}// getLocalIntArray()
	
	/********************************************************************
	 *
	 * Gets a resource-bundle String; this is for internationalization.
	 * Objects passed in are for MessageFormat arguments.
	 * <p>
	 * If resource is missing, a popup-error message is displayed.
	 * <p>.
	 *
	 ********************************************************************/	
	public static String getLocalString(String key, Object arg1)
	{
		return MessageFormat.format(getLocalString(key), new Object[] {arg1});
	}// getLocalString()
	
	
	/********************************************************************
	 *
	 * Gets a resource-bundle String; this is for internationalization.
	 * Objects passed in are for MessageFormat arguments.
	 * <p>
	 * If resource is missing, a popup-error message is displayed.
	 * <p>.
	 *
	 ********************************************************************/	
	public static String getLocalString(String key, Object arg1, Object arg2)
	{
		return MessageFormat.format(getLocalString(key), new Object[] {arg1, arg2});
	}// getLocalString()
	

	/********************************************************************
	 *
	 * Gets a resource-bundle String; this is for internationalization.
	 * Objects passed in are for MessageFormat arguments.
	 * <p>
	 * If resource is missing, a popup-error message is displayed.
	 * <p>.
	 *
	 ********************************************************************/	
	public static String getLocalString(String key, Object arg1, Object arg2, Object arg3)
	{
		return MessageFormat.format(getLocalString(key), new Object[] {arg1, arg2, arg3});
	}// getLocalString()
	

	/********************************************************************
	 *
	 * Gets a resource-bundle String; this is for internationalization.
	 * Objects passed in are for MessageFormat arguments.
	 * <p>
	 * If resource is missing, a popup-error message is displayed.
	 * <p>.
	 *
	 ********************************************************************/	
	public static String getLocalString(String key, Object[] args)
	{
		return MessageFormat.format(getLocalString(key), args);
	}// getLocalString()
	
	
	
	/********************************************************************
	 *
	 * Gets a resource-bundle String; this is for internationalization.
	 * <p>.
	 * If there is NO resource present, returns 'null'.
	 *
	 ********************************************************************/	
	public static String getLocalStringNoEx(String key)
	{
		try
		{
			return resourceBundle.getString(key);
		}
		catch(MissingResourceException e)
		{
		}
		
		return null;
	}// getLocalStringNoEx()	
	
	
	/** What we display if we are missing a resource */
	private static void showNoLocalStringPopup(String resourceKey, Exception e)
	{
			String title = getLocalStringNoEx(UTILS_RES_ERR_DLG_TITLE);
			String text = getLocalStringNoEx(UTILS_RES_ERR_DLG_TEXT);
			
			title = (title == null) ? "Resource Error" : title;
			if(text == null)
			{
				text = "Could not find a needed resource \""+resourceKey+"\"; error:\n"+e.getMessage();
			}
			else
			{
				text = MessageFormat.format(text, new Object[] {resourceKey, e.getMessage()});
			}
			
			popupError(null, title, text);
	}// showNoLocalStringPopup()
	
	
	
	
	/********************************************************************
	 *
	 * Given a color string, converts it to a color value. This is typically
	 * used when parsing configuration files and preferences. This has been
	 * tightened to be SVG CSS (non keyword) compliant. Case insensitive.
	 * <p>
	 * Acceptable forms are:
	 * <ul>
	 *	<li><code>#RGB</code>		(3 hex values; values will be doubled (e.g., 
	 *				#FAF becomes #FFAAFF)</li>
	 *	<li><code>#RRGGBB</code> (6 hex digits)</li>
	 *	<li><code>#RRGGBBAA</code> (8 hex digits; A is alpha value)</li>
	 *	<li><code>rgb(r, g, b)</code> (where r, g, b are specified as 
	 *		integers (0-255) or percents (0-100%, 100% = 255).</li>
	 * </ul>
	 * <p>
	 * If parsing fails, defaultValue is returned.
	 * 
	 ********************************************************************/	
	public static Color parseColor(final String value, final Color defaultValue)
	{
		// Note that Color.decode() does NOT parse alpha values.
		// original lengths: (with #): 4, 7, 9
		//
		String lcColor = value.trim().toLowerCase();
		final int length = lcColor.length();	// original length
		if( lcColor.startsWith("#") ) 
		{
			lcColor = lcColor.substring(1);	// remove '#'
			
			// expand 3-digit to 6 digit
			if(length == 4)
			{
				// screw math!
				StringBuffer sb = new StringBuffer(6);
				sb.append(lcColor.charAt(0));
				sb.append(lcColor.charAt(0));
				sb.append(lcColor.charAt(1));
				sb.append(lcColor.charAt(1));
				sb.append(lcColor.charAt(2));
				sb.append(lcColor.charAt(2));
				lcColor = sb.toString();
			}
			
			if(length == 4 || length == 7 || length == 9)
			{
				try
				{
					// must parse as long, because RGBA bytes are
					// 'unsigned' as a whole, and int is signed.
					// if length == 9, we have an alpha value included.
					// 
					final int colorBits = (int) Long.parseLong(lcColor, 16);
					return new Color(colorBits, (length == 9));
				}
				catch(Exception e)
				{
				}
			}
		}
		else if(lcColor.startsWith("rgb"))
		{
			lcColor = lcColor.substring(3); // remove "rgb"
			StringTokenizer st = new StringTokenizer(lcColor, "(), ");
			String[] sRGB = new String[3];
			int idx = 0;
			while(st.hasMoreTokens() && idx < 3)
			{
				sRGB[idx] = st.nextToken();
				idx++;
			}
			
			if(idx == 3)	// not enough values, if idx is less than 2
			{
				try
				{
					int rgb[] = new int[3];
					for(int i=0; i<sRGB.length; i++)
					{
						if(sRGB[i].endsWith("%") && sRGB[i].length() > 1)
						{
							final int percent = Integer.parseInt(sRGB[i].substring(0,sRGB[i].length()-1));
							rgb[i] = (int) (255.0f * ((float) percent / 100.0f));
						}
						else
						{
							rgb[i] = Integer.parseInt(sRGB[i]);
						}
					}
					
					return new Color(rgb[0], rgb[1], rgb[2]);
				}
				catch(Exception e)
				{
				}
			}
		}
		
		// error: return default color.
		return defaultValue;
	}// parseColor()
	
	
	/********************************************************************
	 *
	 * Given a Color, convert it to a hex String. 
	 * Includes alpha value. Null input is illegal.
	 * <p>
	 * The format of the output string is:
	 * "RRGGBBAA". No '#' is prepended. The String length
	 * is always 8 characters, zero-padded as necessary.
	 *
	 * 
	 ********************************************************************/		
	public static String colorToHex(Color color)
	{
		return colorToHex(color, false);
	}// colorToHex()
	
	
	/**
	*	Converts a Color to its Hexadecimal equivalent, and
	*	prepends a '#' sign if withPound is true.
	*
	*/
	public static String colorToHex(Color color, boolean withPound)
	{
		StringBuffer sb = new StringBuffer(9);
		if(withPound)
		{
			sb.append('#');
		}
		fastToHex(sb, color.getRed());
		fastToHex(sb, color.getBlue());
		fastToHex(sb, color.getGreen());
		fastToHex(sb, color.getAlpha());
		return sb.toString();
	}// colorToHex()
	
	
	/**
	*	Converts a color to HTML hex color in the following format:
	*	<code>#RRGGBB</code>. Alpha values are not used.
	*
	*/
	public static String colorToHTMLHex(Color color)
	{
		StringBuffer sb = new StringBuffer(8);
		sb.append('#');
		fastToHex(sb, color.getRed());
		fastToHex(sb, color.getBlue());
		fastToHex(sb, color.getGreen());
		return sb.toString();
	}// colorToHTMLHex()
	
	
	
	/**
	*	Creates a JEditorPane that has flowing, rich (HTML) text, but
	*	is not selectable or editable. This will also set the document
	*	base to be the same as Utils.getResourceBase().
	*	<p>
	*	This is non-focusable by default.
	*
	* 	@param blend match background of component if true.
	*/
	public static JEditorPane createTextLabel(boolean blend)
	{
		return createTextLabel(null, blend, false);
	}// createTextLabel()
	
	/**
	*	Creates a JEditorPane that has flowing, rich (HTML) text, but
	*	is not selectable or editable. This will also set the document
	*	base to be the same as Utils.getResourceBase().
	*	<p>
	*	This is non-focusable by default.
	*
	*
	* 	@param text the initial text.
	* 	@param blend match background of component if true.
	*
	*
	*/
	public static JEditorPane createTextLabel(String text, boolean blend)
	{
		return createTextLabel(text, blend, false);
	}// createTextLabel()
	
	/**
	*	Creates a JEditorPane that has flowing, rich (HTML) text, but
	*	is not selectable or editable. This will also set the document
	*	base to be the same as Utils.getResourceBase().
	*
	* 	@param text the initial text.
	* 	@param blend match background of component if true.
	* 	@param isFocusable true if this component can receive focus events
	*/
	public static JEditorPane createTextLabel(String text, boolean blend, final boolean isFocusable)
	{
		// use antialiasing only if non-blended
		JEditorPane jep = null;
		if(blend)
		{
			jep = new JEditorPane() 
			{
				final boolean mayFocus = isFocusable;
				
				public boolean isFocusable()
				{
					return mayFocus;
				}
			};
		}
		else
		{
			jep = new XJEditorPane() 
			{
				final boolean mayFocus = isFocusable;
				
				public boolean isFocusable()
				{
					return mayFocus;
				}
			};
		}
		
		// set the content type to HTML, and the the document base
		jep.setContentType("text/html");
		Document doc = jep.getDocument();
		if(doc instanceof HTMLDocument)
		{
			((HTMLDocument)doc).setBase(Utils.getResourceBase());
		}
		
		if(!blend)
		{
			jep.setMargin(new Insets(TEXT_INSETS, TEXT_INSETS, TEXT_INSETS, TEXT_INSETS));
		}
		
		jep.setEditable(false);
		
		jep.setHighlighter(null);
		jep.setSelectedTextColor(null);	// per BugID 4532590
		jep.setForeground(UIManager.getColor("textText"));
		
		if(blend)
		{
			jep.setBackground(UIManager.getColor("Label.text"));
		}
		
		if(text != null)
		{
			jep.setText(text);
		}
		
		return jep;
	}// createTextLabel
	
	
	/** Scale an Image to the given width and height (in pixels) */
	public static Image getScaledImage(Image src, int w, int h)
	{
		AreaAveragingScaleFilter scaleFilter = new AreaAveragingScaleFilter(w, h);
		return syncCreateImage(toolkit.createImage(new FilteredImageSource(src.getSource(), scaleFilter)));
	}
	
	
	/** Scale an ImageIcon to the given width and height (in pixels) */
	public static ImageIcon rescaleImageIcon(ImageIcon src, int w, int h)
	{
		if(src.getIconWidth() != w || src.getIconHeight() != h)
		{
			AreaAveragingScaleFilter scaleFilter = new AreaAveragingScaleFilter(w, h);
			Image img =  syncCreateImage(toolkit.createImage(new FilteredImageSource(src.getImage().getSource(), scaleFilter)));
			return new ImageIcon(img);
		}
		
		return src;
	}// rescaleImageIcon()
	
	/** 
	*	Scale an ImageIcon *down* to the given width and height (in pixels), 
	*	keeping aspect ratio intact. No scaling is done if image is smaller
	*	then the desired max (specified) width/height
	*/
	public static ImageIcon scaleDown(final ImageIcon src, final int maxW, final int maxH)
	{
		if(src.getIconWidth() >= maxW || src.getIconHeight() >= maxH)
		{
			int w = maxW;
			int h = maxH;
			final float aspect = (float) src.getIconWidth() / (float) src.getIconHeight();
			
			if(src.getIconWidth() >= src.getIconHeight())
			{
				w = maxW;
				h = (int) (w / aspect);
			}
			else
			{
				h = maxH;
				w = (int) (h * aspect);
			}
			
			AreaAveragingScaleFilter scaleFilter = new AreaAveragingScaleFilter(w, h);
			Image img =  syncCreateImage(toolkit.createImage(new FilteredImageSource(src.getImage().getSource(), scaleFilter)));
			return new ImageIcon(img);
		}
		
		return src;
	}// scaleDown()
	
	
	
	/** Return a scaled image that is proportionally scaled via the given factor (e.g., 0.5f = 1/2 size).*/
	public static Image getScaledImage(Image src, float factor)
	{
		int w = (int) (src.getWidth(null) * factor);
		int h = (int) (src.getHeight(null) * factor);
		return getScaledImage(src, w, h);
	}
	
	/** 
	*	A less flexible and faster but easier-to-use message formatting.
	*	A number enclosed within braces is replaced with the argument
	*	at that index. The toString() method is used to convert the 
	*	Object argument into text.
	*/
	public static String format(String format, Object args[])
	{
		StringBuffer output = new StringBuffer(4096);
		StringBuffer accum = new StringBuffer(64);
		
		boolean inBrace = false;
		StringTokenizer st = new StringTokenizer(format, "{}", true);
		while(st.hasMoreTokens())
		{
			String tok = st.nextToken();
			if("{".equals(tok) && !inBrace)
			{
				inBrace = true;
			}
			else if("}".equals(tok) && inBrace)
			{
				inBrace = false;
				int i = -1;
				try
				{
					i = Integer.parseInt(accum.toString()); 
				}
				catch(Exception e)
				{
				}
				
				if(i >= 0 && i < args.length)
				{
					output.append(args[i]);
				}
				
				accum.setLength(0);
			}
			else
			{
				if(inBrace)
				{
					accum.append(tok);
				}
				else
				{
					output.append(tok);
				}
			}
		}
		
		return output.toString();
	}// format()	
	
	
	
	/** 
	* Converts int (0-255) to hex value, appends a '0' before if only a single digit
	* could have lookup table and index via shifting...
	* this is ONLY for values of i between [0,255]
	*/
	private static void fastToHex(StringBuffer sb, int i)
	{
		sb.append(HEX[i >> 4]);
		sb.append(HEX[i & 0x0000000F]);
	}// fastToHex()
	
	
	/** 
	*	Gets the appropriate resource bundle. Uses the default locale.
	*	If the default locale cannot be found, ResourceBundle.getBundle() 
	*	should find an acceptable substitute. If it cannot, the US English
	*	bundle is used. 
	*	<p>
	*	If the default (US English) bundle cannot be found, a fatal error 
	*	message is displayed, and the program will exit.
	*	<p>
	*	This requires the classLoader variable to be set.
	*
	*/
	private static void setResourceBundle(Locale locale)
	{
		try
		{
			resourceBundle = ResourceBundle.getBundle(BASE_RESOURCE_FILE , locale, classLoader);
		}
		catch(MissingResourceException mre)
		{
			System.err.println(mre);
			popupError(null, "ERROR: Cannot Start", "Resource File cannot be found!\n"+mre.getMessage());
			System.exit(1);
		}
	}// setResourceBundle()
	
	
	/** 
	*	Constructs a formatted text field that only allows valid email characters.
	*	These are defined as ASCII alphanumerics (a-z, A-Z, 0-9), plus the characters 
	*	<b>!,.,-,_,@</b>. This is not (by any means) RFC822 compliant, 
	*	but allows most email addresses through.
	*/
	public static JTextField createEmailTextField(int cols)
	{
		JTextField jtf = new JTextField(cols);
		AbstractDocument doc = (AbstractDocument) jtf.getDocument();
		doc.setDocumentFilter(new DocumentFilter()
		{
			public void insertString(DocumentFilter.FilterBypass fb, int offset, String text, AttributeSet attr)
			throws BadLocationException
			{
				this.replace(fb, offset, 0, text, attr);
			}// insertString()
			
			public void replace(DocumentFilter.FilterBypass fb, int offset, int length, String text, AttributeSet attr)
			throws BadLocationException
			{
				fb.replace(offset, length, getValidEmailString(text), attr);
			}// replace()
			
			private String getValidEmailString(String in)
			{
				StringBuffer buffer = new StringBuffer(in);
				
				for(int i=buffer.length()-1; i>=0; i--)
				{
					char c = buffer.charAt(i);
					
					if( !isValidEmail(c) )
					{
						buffer.deleteCharAt(i);
					}
				}
				return buffer.toString();
			}// getValidEmailString()
			
			private boolean isValidEmail(char c)
			{
				// check letters (A-Z, a-z)
				if( (c >= 0x0041 && c<= 0x005A)
					|| (c >= 0x0061 && c<= 0x007A) )
				{
					return true;
				}
				
				// check digits (0-9)
				if(c >= 0x0030 && c <= 0x0039)
				{
					return true;
				}
				
				// check misc chars
				for(int i=0; i<EMAIL_ALLOWED.length; i++)
				{
					if(c == EMAIL_ALLOWED[i])
					{
						return true;
					}
				}
				
				return false;
			}// checkValid()
		});
		
		return jtf;
	}// createEmailTextField()
	
	/** 
	*	Constructs a formatted text field that only allows valid URI characters.
	*	These are defined as ASCII alphanumerics (a-z, A-Z, 0-9), plus many
	*	additional characters, including: ;,/,?,:,@,&,=,+,$,,,-,_,.,!,~,*,',|,%
	*
	*                       
	*/
	public static JTextField createURITextField(int cols)
	{
		JTextField jtf = new JTextField(cols);
		AbstractDocument doc = (AbstractDocument) jtf.getDocument();
		doc.setDocumentFilter(new DocumentFilter()
		{
			public void insertString(DocumentFilter.FilterBypass fb, int offset, String text, AttributeSet attr)
			throws BadLocationException
			{
				replace(fb, offset, 0, text, attr);
			}// insertString()
			
			public void replace(DocumentFilter.FilterBypass fb, int offset, int length, String text, AttributeSet attr)
			throws BadLocationException
			{
				fb.replace(offset, length, getValidURLString(text), attr);
			}// replace()
			
			private String getValidURLString(String in)
			{
				StringBuffer buffer = new StringBuffer(in);
				
				for(int i=buffer.length()-1; i>=0; i--)
				{
					final char c = buffer.charAt(i);
					
					if( !isValidURL(c) )
					{
						buffer.deleteCharAt(i);
					}
				}
				
				return buffer.toString();
			}// getValidURLString()
			
			private boolean isValidURL(char c)
			{
				// check letters (A-Z, a-z)
				if( (c >= 0x0041 && c<= 0x005A)
					|| (c >= 0x0061 && c<= 0x007A) )
				{
					return true;
				}
				
				// check digits (0-9)
				if(c >= 0x0030 && c <= 0x0039)
				{
					return true;
				}
				
				// check misc chars
				for(int i=0; i<URL_ALLOWED.length; i++)
				{
					if(c == URL_ALLOWED[i])
					{
						return true;
					}
				}
				
				return false;
			}// checkValid()
		});
		
		return jtf;
	}// createURITextField()
	
	
	
   /** 
   *	Parses a line of CSV text into a String array.
   *	<p>
   *	This quoted text, unquoted text, and escaped quotes.
   *	Excel-style quote support could be added, but has not been tested
   *	adequately. Unescaped quotes are also acceptable in this parser.
   *	Commas with only whitespace between them return empty ("") Strings.
   *	Whitespace is removed around unquoted items.
   *	<p>
   *	For example:<br>
   *	<code>"hello", goodbye  , """test",, "a line, a \"quote\""</code><br>
   *	Will parse into:<br>
   *	<code>hello|goodbye|""test||a line, a "quote"|</code><br>
   *	<p>
   *	Should never return null. NOTE that a entry of "" will return "" (array length 1).
   *	An array length of 0 is never returned.
   */
   public static String[] parseCSV(String input)
   {
		Matcher m = REAL_COMMAS.matcher( input );
		ArrayList matchList = new ArrayList();
		
		// find all matches (except last)
		// we trim extra whitespace from ends (no effect if within quotes)
		int start = 0;
		while(m.find())
		{
			matchList.add( input.substring(start, m.start()).trim() );
			start = m.end();
		}
		
		// find last match
		matchList.add( input.substring(start, input.length()).trim() );
		
		// convert to array
		// final: because array length doesn't change. Contents might, though.
		final String[] matches = (String[]) matchList.toArray(new String[matchList.size()]);
		
		// cleanup
		for(int i=0; i<matches.length; i++)
		{
			if(matches[i].length() > 0)
			{
				StringBuffer sb = new StringBuffer(matches[i]);
				// step 1: remove (if present) start/end quotes
				if(sb.charAt(0) == '\"')
				{
					sb.deleteCharAt(0);
				}
				
				if(sb.charAt(sb.length()-1) == '\"')
				{
					sb.deleteCharAt(sb.length()-1);
				}
				
				
				// step 2: replace double quotes (excel-style) with single quote
				// disabled. if this is enabled, step 3 should probably be disabled.
				/*
				int idx = 0;
				while( (idx = sb.indexOf("\"\"", idx)) != -1 )
				{
					idx += 1;
					sb.deleteCharAt(idx);
				}
				*/
				
				// step 3: replace 'quoted quotes' (e.g.: \") with single quote
				int idx = 0;
				while( (idx = sb.indexOf("\\\"", idx)) != -1 )
				{
					sb.deleteCharAt(idx);
					idx += 1;
				}
				
				// replace our string
				matches[i] = sb.toString();
			}
		}
		
		return matches;
   	}// parseCSV()	
	
	/**
	*	Similar to parseCSV(), but if the input string (after
	*	trimming) is null or empty, returns an array of length 0.
	*	XE for "eXtended Edition". 
	*/
	public static String[] parseCSVXE(String input)
	{
		if(input == null || "".equals(input.trim()))
		{
			return new String[0];
		}
		
		return parseCSV(input);
	}// parseCSVXE
	
	
	
	/***
	 * 	Gets a common-bundle (non-localized) String. 
	 * 	If the key is not found, an exception is thrown.
	 */	
	public static String getCommonString(String key)
	{
		try
		{
			return commonBundle.getString(key);
		}
		catch(Exception e)
		{
			throw new IllegalStateException("Error/Missing Common Bundle Property: "+key);
		}
	}// getLocalString()
	
	/***
	 * 	Gets a common-bundle (non-localized) String array.
	 *	The string array is parsed with the parseCSV() method.
	 * 	If the key is not found, an exception is thrown.
	 */	
	public static String[] getCommonStringArray(String key)
	{
		try
		{
			return parseCSV( commonBundle.getString(key) );
			
		}
		catch(Exception e)
		{
			throw new IllegalStateException("Error/Missing Common Bundle Property: "+key);
		}
	}// getLocalString()
	
	/**
	*	Replaces all instances of "tofind" with "toReplace" 
	*	in the given input String, and returns the String
	*	after replacement has occured. If no modification
	*	to input has occured, the same reference will be 
	*	returned.
	*/
	public static String replaceAll(final String input, final String toFind, final String toReplace)
	{
		if(toFind == null || toReplace == null)
		{
			throw new IllegalArgumentException();
		}
		
		if(input == null)
		{
			return null;
		}
		
		final int toFindLen = toFind.length();
		final int toReplaceLen = toReplace.length();
		
		StringBuffer sb = new StringBuffer(input);
		
		int idx = 0;
		int start = sb.indexOf(toFind, idx);
		boolean isModified = (start != -1);
		
		while(start != -1)
		{
			int end = start + toFindLen;
			sb.replace(start, end, toReplace);
			
			// repeat search
			idx = start + toReplaceLen;
			start = sb.indexOf(toFind, idx);
		}
		
		return (isModified) ? sb.toString() : input;
	}// replaceAll()
	
	/**
	*	Replaces all instances of "tofind" with "toReplace" 
	*	in the given input String, and returns the String
	*	after replacement has occured. If no modification
	*	to input has occured, the same reference will be 
	*	returned.
	*	<p>
	*	toFind and toReplace must have the same lengths.
	*	<p>
	*	The order of replacement is the order of the toFind array.
	*/
	public static String replaceAll(final String input, final String[] toFind, final String[] toReplace)
	{
		if(toFind == null || toReplace == null || toFind.length != toReplace.length)
		{
			throw new IllegalArgumentException();
		}
		
		if(input == null)
		{
			return null;
		}
		
		StringBuffer sb = new StringBuffer(input);
		boolean isModified = false;
		
		for(int i=0; i<toFind.length; i++)
		{
			final int toFindLen = toFind[i].length();
			final int toReplaceLen = toReplace[i].length();
			
			int idx = 0;
			int start = sb.indexOf(toFind[i], idx);
			isModified = (isModified) || (start != -1);
			
			while(start != -1)
			{
				int end = start + toFindLen;
				sb.replace(start, end, toReplace[i]);
				
				// repeat search
				idx = start + toReplaceLen;
				start = sb.indexOf(toFind[i], idx);
			}
		}
		
		return (isModified) ? sb.toString() : input;
	}// replaceAll()
	
	/** Detect if we are running on Mac OS X */
	public static boolean isOSX()
	{
		return isOSX;
	}// isOSX()
	
	/** Detect if we are running on Windows */
	public static boolean isWindows()
	{
		return isWindows;
	}// isWindows()
	
	/** Constructor */
	private Utils()
	{
	}// Utils()
	
}// class Utils
