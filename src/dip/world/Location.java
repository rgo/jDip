//
//  @(#)Location.java		4/2002
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

import dip.order.OrderException;
import dip.misc.Utils;


/**
*
*	A Location defines where an object on the map exists.
*	<p>
*	Locations are immutable. 
*	Convenience methods are provided for obtaining adjacency 
*	information.
*
*
*
*
*
*
*/
public class Location implements Cloneable
{
	/** Constant defining an empty array of Location() objects */
	public static final Location[] EMPTY = new Location[0];
	
	// il8n text strings
	private static final String LOC_VWM_MULTICOAST = "LOC_VWM_MULTICOAST"; 
	private static final String LOC_VAD_NO_UNIT = "LOC_VAD_NO_UNIT"; 
	private static final String LOC_VS_UNDEFCOAST = "LOC_VS_UNDEFCOAST"; 
	private static final String LOC_V_ARMY_IN_SEA = "LOC_V_ARMY_IN_SEA"; 
	private static final String LOC_V_FLEET_LANDLOCKED = "LOC_V_FLEET_LANDLOCKED"; 
	private static final String LOC_V_COAST_BAD = "LOC_V_COAST_BAD"; 
	private static final String LOC_V_COAST_SINGLE = "LOC_V_COAST_SINGLE"; 
	
	
	// instance variables
	// note: all fields should be final to ensure immutability
	private final Province province;
	private final Coast coast;
	
	
	
	/** Create a Location object */
	public Location(Province province, Coast coast)
	{
		if(province == null || coast == null)
		{
			throw new IllegalArgumentException("null parameter(s)");
		}
		
		this.province = province;
		this.coast = coast;
	}// getLocation()
	
	
	/** Get the Province for this Location */
	public final Province getProvince()
	{
		return province;
	}// getProvince()
	
	/** Get the Coast for this Location */
	public final Coast getCoast()
	{
		return coast;
	}// getCoast()
	
	
	/** Convenience method. Determines if the Location has a convoyable coastal Province. */
	public boolean isConvoyableCoast()
	{
		return province.isConvoyableCoast();
	}// isConvoyableCoast()
	
	
	/** 
	*	Returns <code>true</code> if this location is touching 
	*	the given province. Note that "Touching" does not 
	*	nescessarily imply connectedness (adjacency).
	*/
	public boolean isTouching(Province destProvince)
	{
		return province.isTouching(destProvince);
	}// isTouching()
	
	
	/**
	*	Determines if the two locations are adjacent (connected).
	*	<p>
	*	This uses the Province/Coast information of this object, but 
	*	only uses the Province information supplied; thus it does not
	*	check the 'destination' coast.
	*
	*/
	public boolean isAdjacent(Province destProvince)
	{
		return province.isAdjacent(this.coast, destProvince);
	}// isAdjacent()
	
	
	/**
	*	Determines if the two locations are adjacent (connected), 
	*	taking coasts into account as well.
	*
	*/
	public boolean isAdjacent(Location location)
	{
		return province.isAdjacent(this.coast, location);
	}// isAdjacent()
	
	
	
	/** 
	*	Returns <code>true</code> if the Province in both locations
	*	is equal, ignoring the coasts.
	*/
	public boolean isProvinceEqual(Location location)
	{
		return (this.province == location.province);
	}// isProvinceEqual()
	
	/** 
	*	Returns <code>true</code> if the Province in this location
	*	is equal to the province argument; coasts are ignored.
	*/
	public boolean isProvinceEqual(Province province)
	{
		return (this.province == province);
	}// isProvinceEqual()
	
	
	/** 
	*	Implements a clone. Note that this is not a strict 
	*	implementation, in that a constructor is invoked
	*	for performance reasons, rather than using super.clone().
	*/
	public Object clone()
	{
		return new Location(province, coast);
	}// clone()
	
	/**
	*	Determines if two Locations are equal.
	*/
	public boolean equals(final Object obj)
	{
		if(obj == this)
		{
			return true;
		}
		else if(obj instanceof Location)
		{
			// compare referential Province and Coast equality
			Location loc = (Location) obj;
			return (province == loc.province && coast == loc.coast);
		}
		
		return false;
	}// equals()
	
	
	/**
	*	Hashcode for Location.
	*/
	public int hashCode()
	{
		return (37 * province.hashCode() + coast.hashCode()); 
	}// hashCode()
	
	
	/** 
	*	Determines if two Locations are "loosely" equal. This means that 
	*	the Province must match, but, an Undefined coast will match 
	*	any coast (including another undefined coast). If coasts are defined,
	*	they must match to return true.
	*
	*/
	public boolean equalsLoosely(Location loc)
	{
		return ( (province == loc.province) 
				 && (coast == loc.coast || coast == Coast.UNDEFINED || loc.coast == Coast.UNDEFINED) );
	}// equalsLoosely()
	
	
	/** Returns the short Location name (as per appendBrief()) as a String */
	public String toString()
	{
		StringBuffer sb = new StringBuffer(8);
		appendBrief(sb);
		return sb.toString();
	}// toString()
	
	/** Returns the full (long) Locatin name (as per appendFull()) as a String */
	public String toLongString()
	{
		StringBuffer sb = new StringBuffer(64);
		appendFull(sb);
		return sb.toString();
	}// toString()
	
	
	/** Append the brief Location name to the StringBuffer (e.g., spa/sc) */
	public void appendBrief(StringBuffer sb)
	{
		sb.append(province.getShortName());
		if(coast.isDirectional())
		{
			sb.append('/');
			sb.append(coast.getAbbreviation());
		}
	}// appendBrief()
	
	
	/** Append the full Location name to the StringBuffer. (e.g., Spain/South Coast) */
	public void appendFull(StringBuffer sb)
	{
		sb.append(province);
		if(coast.isDirectional())
		{
			sb.append('(');
			sb.append(coast.getName());
			sb.append(')');
		}
	}// appendFull()
	
	
	/**
	<pre>
        This extends the simple validate method, be performing additional
        checking for move orders. 
        
        For example:
            F gas-spain
        
        Now, using the simple validate() method, the above order would fail, becuase
        spain/nc must be specified. However, a fleet in gascony can ONLY move to 
        spain/nc; thus the specification should be optional. If the fleet could move 
        to EITHER coast, then this method offers no gain, and an error will occur.
        
        Essentially, this 'loosens up' the validate method to account for orders that
        are legal and yet not ambiguous. 
        
        However, we need to know the 'from' location. 
        
        This only applies to fleets that are moving into multicoastal provinces.
        
        some examples:
            F gas-spa       ** illegal; no coast specified
            F gas-spa/nc    ** ok
            F gas-spa/sc    ** illegal; non-adjacent
            F mar-spa/sc    ** ok
            A gas-spa       ** ok (army)
            A gas-spa/nc    ** ok (coast ignored for armies)
            A gas-spa/sc    ** ok (coast ignored for armies)
            
        
        
        
        This should only be used in Move orders; it should not be used in Convoy orders,
        since only Armies can be convied, nor should it be used for Support orders, since
        the destination province may legally be a loose or 'unreachable coast' match.
        
        NOTE: 'from' MUST BE adjacent to this location for this to work, with a defined
              coast. Thus the 'from' location should be fully validated before this method is used.
        
        NOTE: This method may return a new Location object, if changes were made,
              or the same Location object, if no changes were made.
	</pre>
	*/
	public Location getValidatedWithMove(Unit.Type unitType, Location from)
	throws OrderException
	{
		// simple validate
		Location newLoc = getValidated(unitType);
		Coast newCoast = newLoc.getCoast();
		
		// DO NOT use internal 'coast' and 'province' variables past here
		//
		// extended validation, if coast is undefined.
		if(unitType == Unit.Type.FLEET && newCoast == Coast.UNDEFINED && newLoc.getProvince().isMultiCoastal())
		{
			int adjCoasts = 0;
			Coast toCoast = null;
			
			// TODO: this can be optimized. If we find a coast, we should assign it
			// and if we find another coast, 
			// then we should throw an exception
			// (this prevents iterating completely thru all coasts each time)
			// 
			Location[] locs = from.getProvince().getAdjacentLocations(from.getCoast());
			for(int i=0; i<locs.length; i++)
			{
				if(locs[i].getProvince() == newLoc.getProvince())
				{
					adjCoasts++;
					toCoast = locs[i].getCoast();
				}
			}
			
			// if we have 1 and only 1 coast, we can assign a coast; otherwise, 
			// we must report an error.
			if(adjCoasts == 1)
			{
				newCoast = toCoast;
			}
			else
			{
				throw new OrderException(Utils.getLocalString(LOC_VWM_MULTICOAST, newLoc.getProvince().getFullName()));
			}
		}
		
		return ((newCoast == newLoc.getCoast()) ? newLoc : new Location(newLoc.getProvince(), newCoast));
	}// getValidatedWithMove()
	
	
	/**
	<pre>	
        This extends the simple getValidated() method, by performing
        additional validation/checking given turnstate information.
        
        It is most appropriate for unit source orders. Given the following
        order:
            F xxx SUPPORTS yyy-zzz
        It would be most appropriate to validate the location "xxx" with this 
        method. This is because a unit exists in "xxx", and we can derive/match
        any existing information (coast type, in particular) from the unit.
        
        It would also be appropriate to validate location yyy with this, because
        we know that a unit must exist in yyy (otherwise the order is invalid).
        
        Thus coast ambiguities are removed.
        
        This should NOT be used for validation of destination locations. This is because
        no unit may be in the destination location, and even if there is it wouldn't make
        sense; for example:
            F gas-spain/nc
            A spain
        If we used getValidAndDerived() for "spain/nc", we would derive the coast from the
        army in spain, which would be illegal. Thus a simple validate or a move validate
        should be used instead.
        
        
        So: The method works as follows:
            1) calls validate()     "simple validate"
            2) if unit type is a fleet: (if army, we're done)
                a) if coast undefined, derive coast from unit
                b) if coast is defined, ensure that it matches the
                    coast of the unit.
        
        Fixed: we must give the unit (or null, which may cause a failure).
                The reason to give the unit is that a dislodged unit or
                non-dislodged unit may be req'd depending on what we are
                validating (e.g., retreat orders derive against dislodged
                units)
        
        A Coast will never be undefined after this method completes.
        
        NOTE: This method may return a new Location object, if changes were made,
              or the same Location object, if no changes were made.
	</pre>
	*/
	public Location getValidatedAndDerived(Unit.Type unitType, Unit existingUnit)
	throws OrderException
	{
		// simple validate
		Location newLoc = getValidated(unitType);
		Coast newCoast = newLoc.getCoast();
		
		// DO NOT use internal 'coast' and 'province' variables past here
		//
		// extended validation; only for Fleets
		if(unitType.equals(Unit.Type.FLEET) && newLoc.getProvince().isMultiCoastal())
		{
			String provinceName = newLoc.getProvince().getFullName();
			
			if(existingUnit == null)
			{
				throw new OrderException(Utils.getLocalString(LOC_VAD_NO_UNIT, provinceName));
			}
			else
			{
				// Derive the coast from the existing coast, irrespective of what was specified.
				newCoast = existingUnit.getCoast();
				
				// the commented-out code is more strict (too strict)				
				/*
				if(coast.equals(Coast.UNDEFINED))
				{
					// no coast specified; derive coast from unit
					coast = origCoast;
				}
				
				else if(!coast.equals(origCoast))
				{
					// a coast was specified, but it doesn't match the coast of the unit!
					throw new OrderException("The unit in "+provinceName+" occupies the "+origCoast+
											 ", not the "+coast+", as specified in the order.");
				}
				// else: we are ok
				*/
			}
		}
		
		// Postcondition: Coast should NOT be undefined at this point.
		assert(newCoast != Coast.UNDEFINED);
		return ((newCoast == newLoc.getCoast()) ? newLoc : new Location(newLoc.getProvince(), newCoast));
	}// getValidAndDerived()
	
	
	/**
	*	This is mainly used for game setup (WorldFactory). Extends validate() by 
	*	ensuring that fleets cannot have undefined coasts. 
	*	<p>
	*	For example: France: F spain is unacceptable; it should be F spain/nc or
	*	F spain/sc.
	*/
	public Location getValidatedSetup(Unit.Type unitType)
	throws OrderException
	{
		Location newLoc = getValidated(unitType);
		
		// DO NOT use internal 'coast' and 'province' variables past here
		//
		if(newLoc.getCoast() == Coast.UNDEFINED)
		{
			String provinceName = newLoc.getProvince().getFullName();
			throw new OrderException(Utils.getLocalString(LOC_VS_UNDEFCOAST, provinceName));
		}
		
		return newLoc;
	}// validateSetup()
	
	
	/**
	<pre>
        Simple Validate
        
        This is typically used for support destinations, e.g.
            F xxx SUPPORTS A yyy-zzz
        The support destination is "zzz".   
        
        Does the following:
            For Armies
            ==========
                1) army must not be in a sea space
                2) coast MUST be Coast.NONE (Coast.LAND)
                        if coast is NOT coast.land, it is assumed to be coast.land, because
                        armies are NEVER located in coasts.
                        
            For Fleets
            ==========
                1) must not be in a landlocked space (non-sea, non-coastal)
                2) Any coast that is not illegal is acceptable. In other words:
                    a) if no coast, or single coast, coast changed to Coast.SINGLE
                    b) if multiple coasts exist:
                        1) if no coast specified, coast == Coast.UNDEFINED
                        2) if a coast is specified, it must be one of the recognized
                            legal coats. Thus for spain: 
                                spain, spain/nc, and spain/sc are all legal
                                (resulting in Coast.UNDEFINED, Coast.NORTH, Coast.SOUTH)
                            however,
                                any other coast (spain/ec, spain/wc) are NOT legal, and
                                will cause an exception.
            For Wings
            =========
                1) Always in Coast.WING
        
        NOTE:   coast may be equal to Coast.UNDEFINED (Coast.ANY) with succesful validation.
                This is intentional! This allows less specific location to match a more specific
                location (e.g., SUPPORT xxx-yyy, and a MOVE zzz-yyy/nc; unspecific "yyy" will 
                still match the more specific "yyy/nc")
                
        FURTHER NOTE: This method may return a new Location object, if changes were made,
                      or the same Location object, if no changes were made.
					  
	</pre>
	*/
	public Location getValidated(Unit.Type unitType)
	throws OrderException
	{
		// pre-conditions
		if(unitType == Unit.Type.UNDEFINED)
		{
			throw new IllegalArgumentException("Cannot validate location with an undefined unit type.");
		}
		
		String provinceName = province.getFullName();
		Coast newCoast = getCoast();
		
		if(unitType == Unit.Type.ARMY)
		{
			if(province.isSea())
			{
				throw new OrderException(Utils.getLocalString(LOC_V_ARMY_IN_SEA, provinceName));				
			}
			
			/* FORCE coast to be Coast.LAND; do not perform this check; too strict
			if(coast.isDirectional())
			{
				throw new OrderException("An Army cannot be located in a specific coast.");
			}
			*/
			
			newCoast = Coast.LAND;
		}		
		else if(unitType == Unit.Type.FLEET)
		{
			if(province.isLandLocked())
			{
				throw new OrderException(Utils.getLocalString(LOC_V_FLEET_LANDLOCKED, provinceName));
			}
			else if(province.isMultiCoastal())
			{
				if(!province.isCoastValid(coast) && !(coast == Coast.UNDEFINED))
				{
					throw new OrderException(Utils.getLocalString(LOC_V_COAST_BAD, provinceName, coast));
				}				
			}
			else
			{
				if(coast.isDirectional())
				{
					throw new OrderException(Utils.getLocalString(LOC_V_COAST_SINGLE, provinceName));
				}
				
				newCoast = Coast.SINGLE;
			}
		}
		else if(unitType == Unit.Type.WING)
		{
			newCoast = Coast.WING;
		}
		
		// return *this if no change
		return ((newCoast == getCoast()) ? this : new Location(getProvince(), newCoast));
	}// getValidated()
	
	
}// class Location


