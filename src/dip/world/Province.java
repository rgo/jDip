//
//  @(#)Province.java		4/2002
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
package dip.world;

import dip.order.Order;

import java.util.*;


/**
*
*	A Province represents a region on the map.
*	<p>
*	Provinces may be sea, coastal, or landlocked. Their connectivity and type (sea, coastal,
*	or landlocked) is determined by the Adjacency data.
*	<p>
*	The types of provinces are:
*	<ul>
*		<li>Landlocked; adjacent only by land (e.g., warsaw)
*		<li>Sea; adjacent only by water (e.g., black sea)
*		<li>Single-Coast; (e.g., portugal)
*		<li>Multi-Coastal;	(e.g., spain)
*	</ul>
*	<p>
*	The adjacency information is similar to that used by the Ken Lowe Judge software.
*	<p>
*	It is illegal to have a Province without any Coasts, to combine single-coast and
*	multi-coast, or to have only multiple coasts without a land coast.
*
*	<pre>
		L S N S W E 	(land / single / north / south / west / east)
		===========
		x - - - - - 	landlocked
		- x - - - - 	seaspace ("sealocked")
		x x - - - - 	coastal land space (only 1 coast)
		- - ? ? ? ? 	INVALID
		- x ? ? ? ? 	INVALID
		x x ? ? ? ? 	INVALID
		x - ? ? ? ? 	see below
		
		other valid:
			land + (!single) + (any combination of north/south/west/east)
	</pre>
*	<p>
*	If Supply Centers become more complex in the future, they may
*	be handled as a separate object within the Province.
*
*/
public class Province implements java.io.Serializable, Comparable
{
	
	// immutable persistent fields
	private final String fullName; 				// fullName MUST BE UNIQUE
	private final String shortNames[]; 			// always has AT LEAST one, and all are globally unique
	private final int index;					// contiguous index
	private final boolean isConvoyableCoast;	// 'true' if coast is convoyable
	private final Adjacency adjacency; 			// adjacency data
	
	// publicly immutable non-final persistent fields
	// (because of difficulties with creation)
	private boolean supplyCenter = false;		// true if supply center exists here.
	private Border[] borders = null;			// non-zero-length if any Borders exist
	
	
	//  transient fields
	private transient int hashCode = 0;
	
	
	/**
	* Adjacency maintains the connectivity graph between provinces.
	*/
	protected static class Adjacency implements java.io.Serializable
	{
		private final HashMap adjLoc;
		
		/**
		* Creates a new Adjacency object.
		*/
		private Adjacency()
		{
			adjLoc = new HashMap(7);
		}// Adjacency()
		
		/** 
		* Sets which locations are adjacent to the specified coast.
		*
		*/
		protected void setLocations(Coast coast, Location[] locations)
		{
			adjLoc.put(coast, locations);
		}// setLocations()
		
		
		/** 
		* Gets the locations which are adjacent to the coast.
		* <p>
		* If no locations are adjacent, a zero-length array is returned.
		*
		*/
		protected Location[] getLocations(Coast coast)
		{
			Location[] locations = (Location[]) adjLoc.get(coast);
			
			if(locations == null)
			{
				locations = Location.EMPTY;
			}
			
			return locations;
		}// getLocations()
		
		
		/**
		*	Creates a WING coast from Province coastal data. All Coasts must
		*	be set for this Province already. Note that a Wing coast is equiavalent
		*	to 'touching' adjacency.
		*/
		protected void createWingCoasts()
		{
			HashSet 	provSet = new HashSet(11);
			ArrayList 	locList = new ArrayList(11);
			for(int i=0; i<Coast.ALL_COASTS.length; i++)
			{
				Location[] locs = getLocations(Coast.ALL_COASTS[i]);
				for(int j=0; j<locs.length; j++)
				{
					Province prov = locs[j].getProvince();
					if(provSet.add(prov))
					{
						locList.add( new Location(prov, Coast.WING) );
					}
				}
			}
			
			provSet.clear();
			setLocations(Coast.WING, (Location[]) locList.toArray(new Location[locList.size()]));
		}// createWingCoasts()
		
		
		/** 
		*	Ensure that adjacency data is consistent, and that there are
		*	no illegal coast combinations.
		*	<p>
		*	The Province argument is required to correctly validate 
		*	convoyable coast regions. Convoyable coasts require a single
		*	or multi-coast region with a defined land coast.
		*	<code>
		*	
		*	land / single / north / south / west / east
		*	
		*	x = true; - = false; ? = true or false
		*
		*	L S N S W E
		* 	===========================================
		*	- - ? ? ? ? 	INVALID		(a)		where at least one ? is true
		*	- x ? ? ? ? 	INVALID		(b)		where at least one ? is true
		*	x x ? ? ? ? 	INVALID		(c) 	where at least one ? is true
		*	- - - - - - 	INVALID		(d)
		*	</code>
		*/
		protected boolean validate(Province p)
		{
			boolean isDirectional = false;
			for(int i=0; i<Coast.ANY_DIRECTIONAL.length; i++)
			{
				if(adjLoc.get(Coast.ANY_DIRECTIONAL[i]) != null)
				{
					isDirectional = true;
				}
			}
			
			final boolean isLand = (adjLoc.get(Coast.LAND) != null);
			final boolean isSingle = (adjLoc.get(Coast.SINGLE) != null);
			
			// covers cases (b) and (c)
			if(isDirectional && isSingle)
			{
				return false;
			}
			
			// covers case (d)
			if(!isDirectional && !isLand && !isSingle)
			{
				return false;
			}
			
			// covers case (a)
			if(isDirectional && !isLand && !isSingle)
			{
				return false;
			}
			
			// check convoyable coasts
			if(p.isConvoyableCoast() && (!isLand || (!isSingle && !isDirectional)) )
			{
				return false;
			}
			
			return true;
		}// validate()		
	}// inner class Adjacency()
	
	
	/**
	*	Creates a new Province object. 
	*	<b>Unless you are a WorldFactory (or subclass), it should (almost) never be nescessary 
	*	to create a Province using new. In fact, to do so will break referential equality.
	*	to get a Province, use the Map.getProvince() and similar methods.</b>
	*	<p>
	*	These are created by a WorldFactory, or through de-serialization.
	*	Null names are not allowed. At least one shortName is required.
	*	
	*/
	public Province(String fullName, String[] shortNames, int index, boolean isConvoyableCoast) 
	{
		if(fullName == null || shortNames == null)
		{	
			throw new IllegalArgumentException("null full or short name(s)");
		}
		
		if(shortNames.length < 1)
		{
			throw new IllegalArgumentException("at least one shortName required");
		}
		
		if(index < 0)
		{
			throw new IllegalArgumentException("index cannot be negative");
		}
		
		this.fullName = fullName;
		this.shortNames = shortNames;
		this.index = index;
		this.isConvoyableCoast = isConvoyableCoast;
		this.adjacency = new Adjacency();
	}// Province()
	
	
	
	
	/**
	*	Sets the Border data for this province.
	*/
	protected void setBorders(Border[] value)
	{
		borders = value;
	}// setBorders()
	
	
	/**
	*	Sets if this province has a supply center.
	*/
	protected void setSupplyCenter(boolean value)
	{
		supplyCenter = value;
	}// setSupplyCenter()
	
	
	/** 
	*	Returns the Province index; this is an int between 0 (inclusive) and
	*	the total number of provinces (exclusive). It is never negative.
	*/
	public final int getIndex()
	{
		return index;
	}// getIndex()
	
	
	/**
	*	Gets the Adjacency data for this Province
	*/
	protected final Adjacency getAdjacency()
	{
		return adjacency;
	}// getAdjacency()
	
	
	/**
	*	Get all the Locations that are adjacent to this province.
	*	Note that if you are only interested in adjacent provinces, 
	*	getAdjacentLocations(Coast.WING or Coast.TOUCHING) is more appropriate 
	*	and faster; however, all Locations returned will be of Coast.WING. 
	*	This method will return all truly adjacent locations, which have their
	*	correct coasts. No duplicate Locations will be present in the returned
	*	array. All arrays will be zero-length or higher; a null array is never
	*	returned.
	*/
	public Location[] getAllAdjacent()
	{
		HashSet 	locSet = new HashSet(13);
		ArrayList 	locList = new ArrayList(13);
		for(int i=0; i<Coast.ALL_COASTS.length; i++)
		{
			Location[] locs = adjacency.getLocations(Coast.ALL_COASTS[i]);
			for(int j=0; j<locs.length; j++)
			{
				Location aLoc = locs[j];
				if(locSet.add(aLoc))
				{
					locList.add( aLoc );
				}
			}
		}
		
		return (Location[]) locList.toArray(new Location[locList.size()]);
	}// getAllAdjacent()
	
	
	
	/**
	*	Gets the Locations adjacent to this province, given the
	*	specified coast.
	*/
	public Location[] getAdjacentLocations(Coast coast)
	{
		return adjacency.getLocations(coast);
	}// getAdjacency()
	
	
	
	/** Gets the full name (long name) of the Province */
	public final String getFullName()			{ return fullName; }
	
	/** 
	*	Gets the short name of the Province 
	*	<p> This returns the first short name if there are more than one.
	*/
	public final String getShortName()			{ return shortNames[0]; }
	
	/** Gets all short names of the Province */
	public final String[] getShortNames() 		{ return shortNames; }
	
	/** Determine if this Province contains a supply center */
	public boolean hasSupplyCenter() 			{ return supplyCenter; }
	
	
	
	
	/**
	*	Determines if two provinces are in any way adjacent (connected).
	* 	<p>
	*	If two provinces are adjacent, by any coast, this will return true. This
	*	implies connectivity in the broadest sense. No coast information is required
	*	or needed in this or the Province that is compared. <b>because Coasts are 
	*	ignored, this method should generally not be used to determine adjacency for the movement
	*	of units.</b>
	*	<p>
	*	This now uses the "Wing" ("Touching") Coast which is equivalent.
	*/
	public boolean isTouching(Province province) 
	{
		/* old code: prior to createWingCoasts()
		for(int i=0; i<Coast.ALL_COASTS.length; i++)
		{
			Location[] locations = adjacency.getLocations(Coast.ALL_COASTS[i]);
			for(int locIdx=0; locIdx<locations.length; locIdx++)
			{
				if(locations[locIdx].getProvince().equals(province))
				{
					return true;
				}
			}
		}
		*/
		Location[] locations = adjacency.getLocations(Coast.TOUCHING);
		for(int locIdx=0; locIdx<locations.length; locIdx++)
		{		
			if(locations[locIdx].isProvinceEqual(province))
			{
				return true;
			}
		}
		
		return false;
	}// isTouching()
	
	
	
	/**
	*	Checks connectivity between this and another province
	*	<p>
	*	This method only determines if the current Province with the specified
	*	coast is connected to the destination Province.
	*	
	*/
	public boolean isAdjacent(Coast sourceCoast, Province dest)
	{
		Location[] locations = adjacency.getLocations(sourceCoast);
		for(int locIdx=0; locIdx<locations.length; locIdx++)
		{		
			if(locations[locIdx].getProvince().equals(dest))
			{
				return true;
			}
		}
		
		return false;
	}// isAdjacent()
	
	
	/**
	*	Checks connectivity between this and another province
	*	<p>
	*	This method only determines if the current Province with the specified
	*	coast is connected to the destination Province and Coast. 
	*	<p>	
	*	This is a stricter version of isAdjacent(Coast, Province)
	*	
	*/
	public boolean isAdjacent(Coast sourceCoast, Location dest)
	{
		Location[] locations = adjacency.getLocations(sourceCoast);
		for(int locIdx=0; locIdx<locations.length; locIdx++)
		{		
			if(locations[locIdx].equals(dest))
			{
				return true;
			}
		}
		
		return false;
	}// isAdjacent()
	
	
	/** Determines if this Province is landlocked. */
	public boolean isLandLocked()
	{
		for(int i=0; i<Coast.ANY_SEA.length; i++)
		{
			if(adjacency.getLocations(Coast.ANY_SEA[i]) != Location.EMPTY) 
			{
				return false;
			}
		}
		
		return true;
	}// isLandLocked()
	
	// NOTE: this could be made more efficient
	/** Determines if this Province is coastal (including multi-coastal). */
	public boolean isCoastal()
	{
		if(adjacency.getLocations(Coast.LAND) != Location.EMPTY)
		{
			for(int i=0; i<Coast.ANY_SEA.length; i++)
			{
				Location[] locations = adjacency.getLocations(Coast.ANY_SEA[i]);
				if(locations.length > 0)
				{
					return true;
				}
			}
		}	
		
		return false;
	}// isCoastal()
	
	
	/** Determines if this Province is a Land province (landlocked OR coastal) */
	public boolean isLand()
	{
		return (adjacency.getLocations(Coast.LAND) != Location.EMPTY);
	}// isLand()
	
	
	/** Determines if this Province is a Sea province (no land, not coastal). */
	public boolean isSea()
	{
		if(adjacency.getLocations(Coast.LAND) != Location.EMPTY)
		{
			return false;
		}
		
		
		for(int i=0; i<Coast.ANY_DIRECTIONAL.length; i++)
		{
			if(adjacency.getLocations(Coast.ANY_DIRECTIONAL[i]) != Location.EMPTY)
			{
				return false;
			}
		}
		
		return true;
	}// isSea()
	
	
	/** Determines if this Province has multiple coasts (e.g., Spain). */
	public boolean isMultiCoastal()
	{
		if(adjacency.getLocations(Coast.SEA) == Location.EMPTY)
		{
			for(int i=0; i<Coast.ANY_DIRECTIONAL.length; i++)
			{
				if(adjacency.getLocations(Coast.ANY_DIRECTIONAL[i]) != Location.EMPTY)
				{
					return true;
				}
			}
		}
		
		return false;
	}// isMultiCoastal()
	
	
	/** 
	*	Return the coasts supported by this province.
	*	If not multicoastal, returns an empty Coast array.
	*/
	public Coast[] getValidDirectionalCoasts()
	{
		if(adjacency.getLocations(Coast.SEA) == Location.EMPTY)
		{
			ArrayList dir = new ArrayList(4);
			
			for(int i=0; i<Coast.ANY_DIRECTIONAL.length; i++)
			{
				if(adjacency.getLocations(Coast.ANY_DIRECTIONAL[i]) != Location.EMPTY)
				{
					dir.add(Coast.ANY_DIRECTIONAL[i]);
				}
			}
			
			return (Coast[]) dir.toArray(new Coast[dir.size()]);
		}
		
		return new Coast[0];
	}// getValidCoasts()
	
	
	/** Determines if specified coast is allowed for this Province */
	public boolean isCoastValid(Coast coast)
	{
		if(adjacency.getLocations(coast) == Location.EMPTY)
		{
			return false;
		};
		
		return true;
	}// isCoastValid()
	
	
	/** Implementation of Object.hashCode() */
	public int hashCode()
	{
		if(hashCode == 0)
		{
			hashCode = fullName.hashCode();
		}
		return hashCode;
	}// hashCode()
	
	
	
	/** Checks if unit can transit from a Location to this Province. */
	public boolean canTransit(Location fromLoc, Unit.Type unit, Phase phase, Class orderClass)
	{
		return (getTransit(fromLoc, unit, phase, orderClass) == null);
	}// canTransit()
	
	/** Convenient version of canTransit() */
	public boolean canTransit(Phase phase, Order order)
	{
		return canTransit(order.getSource(), order.getSourceUnitType(), phase, order.getClass());
	}// canTransit()
	
	
	/** 
	*	Checks if unit can transit from a Location to this Province. Returns the first 
	*	failing Border order; returns null if Transit is successfull.
	*/
	public Border getTransit(Location fromLoc, Unit.Type unit, Phase phase, Class orderClass)
	{
		if(borders != null)
		{
			for(int i=0; i<borders.length; i++)
			{
				if(!borders[i].canTransit(fromLoc, unit, phase, orderClass))
				{
					return borders[i];
				}
			}
		}
		return null;
	}// getTransit()
	
	/** Convenient version of getTransit() */
	public Border getTransit(Phase phase, Order order)
	{
		return getTransit(order.getSource(), order.getSourceUnitType(), phase, order.getClass());
	}// getTransit()
	
	
	/** 
	*	Looks through borders to determine if there is a baseMoveModifier. 
	*	that fits. Note that the first matching non-zero baseMoveModifier is returned 
	*	if there are more than one, which is not recommended.
	*/
	public int getBaseMoveModifier(Location fromLoc)
	{
		if(borders != null)
		{
			for(int i=0; i<borders.length; i++)
			{
				final int baseMoveMod = borders[i].getBaseMoveModifier(fromLoc);
				if(baseMoveMod != 0)
				{
					return baseMoveMod;
				}
			}
		}
		
		return 0;
	}// getBaseMoveModifier()
	
	
	/** If this province is a convoyable coastal Province, this will return <code>true</code>. */
	public boolean isConvoyableCoast()
	{
		return isConvoyableCoast;
	}// isConvoyableCoast()
	
	
	/** 
	*	Indicates if this province is convoyable, either because it is 
	*	a Sea province or a convoyable coast.
	*	<p>
	*	No transit checking is performed.
	*/
	public boolean isConvoyable()
	{
		return (isConvoyableCoast() || isSea());
	}// isConvoyable()
	
	
	/*
	 NOTE: we just use default referential equality, since these objects are immutable!
	*/
	
	
	/** Returns the full name of the province */
	public String toString()
	{
		return fullName;
	}// toString();
	
	
	
	/** Compares this province to another, by the full name, ignoring case */
	public int compareTo(Object obj)
	{
		return fullName.compareToIgnoreCase( ((Province) obj).fullName );
	}// compareTo()
	
}// class Province
