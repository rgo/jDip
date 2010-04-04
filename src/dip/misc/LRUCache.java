//
//  @(#)Position.java	1.00	4/1/2002
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

import java.util.LinkedHashMap;
import java.util.Map;

/**
*	Implements an LRU Cache; based on LinkedHashMap from JDK1.4
*
*
*
*
*/
public class LRUCache extends LinkedHashMap 
{
	private int maxsize;
	
	
	/**
	*	Creates an LRUCache with the given size.
	*
	*
	*/
	public LRUCache(int maxsize)
	{
		super( (maxsize*4/3) + 1, 0.75f, true);
		this.maxsize = maxsize;
	}// LRUCache()
	
	
	public Object put(Object key, Object value)
	{
		return super.put(key, value);
	}
	
	
	/**
	*	Override to enable elimination of oldest entries
	*/
	protected boolean removeEldestEntry(Map.Entry eldest)
	{
		return (size() > maxsize); 
	}// removeEldestEntry()
	
	
}// class LRUCache
