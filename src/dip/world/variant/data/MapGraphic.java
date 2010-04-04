//
//  @(#)MapGraphic.java	1.00	7/2002
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
package dip.world.variant.data;

import java.net.URI;
import java.net.URISyntaxException;

/**


*/	
public class MapGraphic
{
	private final String name;
	private final URI uri;
	private final boolean isDefault;
	private final String desc;
	private final URI thumbURI;
	private final String prefSPName;
	
	
	/** 
	*	Constructs a MapGraphic object. 
	*	<p>
	*	If the preferred Symbol Pack Name (prefSPName) is an empty string, it will
	*	be converted to a null String.
	*/
	public MapGraphic(String uri, boolean isDefault, String name, String description, 
		String thumbURI, String prefSPName)
	{
		if(name == null) { throw new IllegalArgumentException(); }
		
		this.name = name;
		this.isDefault = isDefault;
		this.desc = description;
		this.prefSPName = ("".equals(prefSPName)) ? null : prefSPName;
		
		// set URI
		URI tmpURI = null;
		try
		{
			tmpURI = new URI(uri);
		}
		catch(URISyntaxException e)
		{
			tmpURI = null;
		}
		this.uri = tmpURI;
		
		tmpURI = null;
		try
		{
			tmpURI = new URI(thumbURI);
		}
		catch(URISyntaxException e)
		{
			tmpURI = null;
		}
		this.thumbURI = tmpURI;
	}// MapGraphic()
	
	/** The URI for a map SVG file. */
	public URI getURI()				{ return uri; }
	
	/** Whether this is the default graphic to use. */
	public boolean isDefault()		{ return isDefault; }
	
	/** The name of this map. <p> Should not be null. */
	public String getName()		{ return name; }
	
	/** The description of this map. <p> May return null. */
	public String getDescription()	{ return desc; }
	
	/** The URI for the thumbnail graphic of this map. */
	public URI getThumbnailURI()	{ return thumbURI; }
	
	/** The Preferred SymbolPack name, or null if none. */
	public String getPreferredSymbolPackName()	{ return prefSPName; }
	
	/** For debugging only! */
	public String toString()
	{
		StringBuffer sb = new StringBuffer(256);
		sb.append(this.getClass().getName());
		sb.append('[');
		sb.append("uri=");
		sb.append(uri);
		sb.append(",isDefault=");
		sb.append(isDefault);
		sb.append(",name=");
		sb.append(name);
		sb.append(",desc=");
		sb.append(desc);
		sb.append(",thumbURI=");
		sb.append(thumbURI);
		sb.append(']');
		return sb.toString();
	}// toString()
}// nested class MapGraphic


