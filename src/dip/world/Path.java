//
//  @(#)Path.java		4/2002
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

import dip.order.Orderable;
import dip.order.Convoy;
import dip.order.Move;
import dip.process.Adjudicator;
import dip.process.OrderState;
import dip.process.Tristate;

import java.util.*;

/**
*	Determines Convoy paths between points on a Map, and also minimum distances
*	between two map points.
*	<p>
*	This class is undergoing a transition, and most of the current method will probably
*	be replaced by the static methods based on findAllSeaPaths().
*
*/
public class Path extends Object
{
	
	private final Position position;
	private final Adjudicator adjudicator;
	
	
	/** Create a new Path object */
	public Path(Position position)
	{
		this.adjudicator = null;
		this.position = position;
	}// Path()
	
	
	/** 
	*	Create a new Path object.
	*	<p>
	*	Note: this constructor is required for SuperConvoyPathEvaluator / 
	*	LegalConvoyPathEvaluator which are used by ANY non-theoretical
	*	convoy route evaluator.
	*/
	public Path(Adjudicator adjudicator)
	{
		this.adjudicator = adjudicator;
		this.position = adjudicator.getTurnState().getPosition();
	}// Path()
	
	
	
	/**
	*	Convenience Method.
	*	<p>
	*	This is analagous to other getConvoyRouteEvaluation() methods, however,
	*	it will work for both Implicit (jDip finds a path) and Explicit (paths
	*	were specified in the Move order, judge-style) paths in a Move order.
	*	<p>
	*	This decides whether to use implicit or explicit paths based on if
	*	Move.getConvoyRoutes() returns null or not.
	*	<p>
	*	The actual path taken will be returned in the actualPath argument.
	*	<p>
	*	Note that the invalidLoc (2000 rule support) and actualPath arguments
	*	may be null
	*	<p>.
	*	<h3>Returns:</h3>
	*	<ul>	
	*		<li>Tristate.FAILURE<br>
	*				no path exists from src-dest, or a convoy was dislodged / failed on this route.</li>
	*		
	*		<li>Tristate.UNCERTAIN<br>
	*				a path exists, but, the status of the convoy units is not yet determined.</li>
	*				
	*		<li>Tristate.SUCCESS<br>
	*				a path exists, and all Convoy orders along that path are SUCCESS.
	*				note that if multiple paths exist, only one path has to be successful 
	*				for this to work.</li>
	*	</ul>				
	*/
	public Tristate getConvoyRouteEvaluation(Move move, Location invalidLoc, List actualPath)
	{
		if(move == null)
		{
			throw new IllegalArgumentException();
		}
		
		final List explicitRoutes = move.getConvoyRoutes();
		
		if(explicitRoutes == null)
		{
			// implicit paths.
			return getConvoyRouteEvaluation(move.getSource(), move.getDest(),
					invalidLoc, actualPath);
		}
		else
		{
			// explicit path(s). Evaluate them all. Return the successful path
			// if one is successful.
			//
			final Province invalidProvince = (invalidLoc == null) ? null : invalidLoc.getProvince();
			final Location src = move.getSource();
			final Location dest = move.getDest();
			
			boolean hasUncertainRoute = false;		// true if >= 1 route is uncertain, but not failed.
			
			Iterator iter = explicitRoutes.iterator();
			while(iter.hasNext())
			{
				final Province[] route = (Province[]) iter.next();
				boolean isFailed = true;
				boolean isUncertain = false;
				
				for(int i=1; i<(route.length - 1); i++)
				{
					final Province province = route[i];
					OrderState os = adjudicator.findOrderStateBySrc( province );
					Orderable order = os.getOrder();
					if(order instanceof Convoy)
					{
						Convoy convoy = (Convoy) order;
						
						if(	convoy.getConvoySrc().isProvinceEqual(src) 
							&& convoy.getConvoyDest().isProvinceEqual(dest) )
						{
							if(province.equals(invalidProvince))
							{
								isFailed = true;
								break;
							}
							
							final Tristate evalState = os.getEvalState();
							
							// if 'invalidLoc' (invalidProvince) is on the path,
							// it is not successfull.
							if(!province.equals(invalidLoc))
							{
								if(evalState == Tristate.FAILURE || os.getDislodgedState() == Tristate.YES)
								{
									isFailed = true;
									break;
								}
								else if(evalState == Tristate.UNCERTAIN)
								{
									isUncertain = true;
									break;
								}
								else if(evalState == Tristate.SUCCESS)
								{
									isUncertain = false;
									isFailed = false;
								}
								else
								{
									throw new IllegalStateException();
								}
							}
						}
						else
						{
							isFailed = true;
							break;
						}
					}
					else
					{
						isFailed = true;
						break;
					}
				}
				
				// if we found a successful route, we don't need to check other routes
				// return success. Return path, too.
				if(!isFailed && !isUncertain)
				{
					if(actualPath != null)
					{
						actualPath.addAll( Arrays.asList(route) );
					}
					return Tristate.SUCCESS;
				}
				
				// if uncertain, and not yet set, set uncertain flag.
				hasUncertainRoute = (hasUncertainRoute || isUncertain);
			}
			
			// we would have returned SUCCESS by now, if successful.
			// so, if we have not uncertain routes, we fail. 
			//
			return (hasUncertainRoute) ? Tristate.UNCERTAIN : Tristate.FAILURE;
		}
	}// getConvoyRouteEvaluation()
	
	
	
	/**
	*	Checks a convoy route to see if it is 'theoretically' valid; this is true iff:
	*	<ol>
	*		<li>route is non-null and length of 3 or more
	*		<li>first and last route provinces match src and dest Location provinces </li>
	*		<li>first and last provinces are land</li>
	*		<li>all other provinces are convoyable (sea or convoyable coast)</li>
	*		<li>all convoyable locations have a fleet present</li>
	*		<li>route is composed of adjacent spaces</li>
	*	</ol>
	*
	*/
	public static boolean isRouteValid(final Position pos, final Location src, 
		final Location dest, final Province[] route)
	{
		if(route == null || route.length < 3)
		{
			return false;
		}
		
		if( !src.isProvinceEqual(route[0])
			|| !dest.isProvinceEqual(route[route.length-1]) )
		{
			return false;
		}
		
		if( !route[0].isLand() 
			|| !route[route.length-1].isLand() )
		{
			return false;
		}
		
		Coast lastCoast = src.getCoast();
		Province p = route[0];
		for(int i=1; i<(route.length-1); i++)
		{
			p = route[i];
			
			if(!p.isConvoyable())
			{
				return false;
			}
			
			Unit unit = pos.getUnit(p);
			if(unit == null || unit.getType() != Unit.Type.FLEET)
			{
				return false;
			}
			
			// for the second provice, check only if we can reach the source province (not location!),
			// as we do not know the source coast!
			if(((i != 1) && (!p.isAdjacent(unit.getCoast(), new Location(route[i-1], lastCoast)))) ||
			   ((i == 1) && (!p.isAdjacent(unit.getCoast(), route[i-1]))))
			{
				return false;
			}
			
			lastCoast = unit.getCoast();
		}
		
		// endpoints adjacency check
		// check only, if we can reach the destination _province_ from the last location,
		// as we do not know the destination coast!
		if(!p.isAdjacent(lastCoast, dest.getProvince()))
		{
			return false;
		}
		
		return true;
	}// isRouteValid()
	
	/**
	*	Verifies a route during adjudication stage;
	*	Given a valid route, all fleets must have:
	*	<ol>
	*		<li>a Convoy order, with convoy src/dest matching the src/dest given</li>
	*		<li>the evaluation state of the Convoy order must not be Tristate.FAILURE</li>
	*	</ol>
	*/
	public static boolean isRouteLegal(final Adjudicator adj, final Province[] route)
	{
		final Province src = route[0];
		final Province dest = route[route.length-1];
		
		for(int i=1; i<(route.length-1); i++)
		{
			OrderState os = adj.findOrderStateBySrc( route[i] );
			final Orderable order = os.getOrder();
			
			if(order instanceof Convoy)
			{
				final Convoy convoy = (Convoy) order;
				
				if(	convoy.getConvoySrc().isProvinceEqual(src) 
					&& convoy.getConvoyDest().isProvinceEqual(dest) )
				{
					if( os.getEvalState() == Tristate.FAILURE 
						|| os.getDislodgedState() == Tristate.YES)
					{
						return false;
					}
				}
			}
		}
		
		return true;
	}// isRouteLegal()
	
	/**
	*	Determines the adjudication status of static convoy route(s), 
	*	that are both valid and legal (verified). 
	*	Analagous to getConvoyRouteEvaluation()
	*	<p>
	*	See getConvoyRouteEvaluation() for return values.
	*/
	public static Tristate evaluateRoutes(final Adjudicator adj, List routes, final Location invalid)
	{
		final Province invalidProvince = (invalid == null) ? null : invalid.getProvince();
		
		Tristate overallResult = Tristate.FAILURE;
		
		for(int routeIdx=0; routeIdx<routes.size(); routeIdx++)
		{
			final Province[] route = (Province[]) routes.get(routeIdx);
			final Province src = route[0];
			final Province dest = route[route.length-1];
			
			Tristate result = null;
			for(int i=1; i<(route.length-1); i++)
			{
				Province province = route[i];
				OrderState os = adj.findOrderStateBySrc( province );
				Orderable order = os.getOrder();
				
				if(order instanceof Convoy)
				{
					Convoy convoy = (Convoy) order;
					
					// only consider matching orders,
					// and only consider orders that don't involve the invalidProvince
					//
					if(	convoy.getConvoySrc().isProvinceEqual(src) 
						&& convoy.getConvoyDest().isProvinceEqual(dest) 
						&& (invalidProvince != province) )
					{
						Tristate evalState = os.getEvalState();
						if(evalState == Tristate.FAILURE || os.getDislodgedState() == Tristate.YES)
						{
							result = Tristate.FAILURE;
							break;
						}
						else if(evalState == Tristate.UNCERTAIN)
						{
							result = Tristate.UNCERTAIN;
							break;
						}
						else if(evalState == Tristate.SUCCESS)
						{
							result = Tristate.SUCCESS;
						}
						else
						{
							// this is not strictly needed.
							throw new IllegalStateException("evaluateRoute(): internal error");
						}
					}
				}
			}// for(route)
			
			assert(result != null);
			
			// if we have one successful route, return success; no others need be checked
			// if we have an uncertain result, return uncertain
			// if all have failed, return failure.
			if(result == Tristate.SUCCESS)
			{
				return Tristate.SUCCESS;
			}
			else
			{
				// result at this point cannot be SUCCESS; only UNCERTAIN or FAILURE
				// we cannot return with an 'uncertain' result, because we may have a
				// subsequent 'success' result later.
				assert(result != Tristate.SUCCESS);
				overallResult = (result == Tristate.UNCERTAIN) ? Tristate.UNCERTAIN : Tristate.FAILURE;
			}
		}
		
		return overallResult;
	}// evaluateRoute
	
	
	/**
	*	Returns if there is a "theoretical" convoy 
	*	route between src and dest. 
	*	<p>
	*	A theoretical convoy route is a string of adjacent fleets that 
	*	could convoy the desired army from src to dest, but may not
	*	have convoy orders to do so.
	*/
	public boolean isPossibleConvoyRoute(Location src, Location dest)
	{
		if(src.getProvince().isCoastal() && dest.getProvince().isCoastal())
		{
			List path = new ArrayList(12);
			PathEvaluator pe = new AnyConvoyPathEvaluator();
			
			return findPathBreadthFirst(src, dest, src, path, pe);
		}
		
		return false;
	}// isPossibleConvoyRoute()
	
	
	
	
	
	
	/**
	*	Returns a path (the first valid path) of a theoretical convoy 
	*	route between src and dest. 
	*	<p>
	*	A theoretical convoy route is a string of adjacent fleets that 
	*	could convoy the desired army from src to dest, but may not
	*	have convoy orders to do so.
	*/
	public List getConvoyRoute(Location src, Location dest)
	{
		List path = new ArrayList();
		
		PathEvaluator pe = new AnyConvoyPathEvaluator();
		findPathBreadthFirst(src, dest, src, path, pe);
		
		return path;
	}// getConvoyRoute()
	
	
	
	/**
	*	Find if a true convoy route exists between src & dest; all fleets must
	*	have:
	*	<ol>
	*		<li>a Convoy order, with convoy src/dest matching the src/dest given</li>
	*		<li>the evaluation state of the Convoy order must not be Tristate.FAILURE</li>
	*	</ol>
	*	
	*/
	public boolean isLegalConvoyRoute(Location src, Location dest)
	{
		List path = new ArrayList(12);
		PathEvaluator pe = new LegalConvoyPathEvaluator(src, dest);
		return findPathBreadthFirst(src, dest, src, path, pe);
	}// isLegalConvoyRoute()
	
	
	
	
	/**
	*	Returns the true convoy route between src & dest; all fleets must
	*	have:
	*	<ol>
	*		<li>a Convoy order, with convoy src/dest matching the src/dest given</li>
	*		<li>the evaluation state of the Convoy order must not be Tristate.FAILURE</li>
	*	</ol>
	*	
	*/
	public List getLegalConvoyRoute(Location src, Location dest)
	{
		List path = new ArrayList(12);
		PathEvaluator pe = new LegalConvoyPathEvaluator(src, dest);
		findPathBreadthFirst(src, dest, src, path, pe);
		return path;
	}// isLegalConvoyRoute()
	
	
	/**
	*	Convenience version of getConvoyRouteEvaluation() where the 
	*	'invalid' Location is set to null.
	*			
	*/
	public Tristate getConvoyRouteEvaluation(Location src, Location dest, List validPath)
	{
		return getConvoyRouteEvaluation(src, dest, null, validPath);
	}// getConvoyRouteEvaluation()
	
	
	/**
	*	The convoying fleet specified in 'invalid' is considered
	*	to fail. (this is for implementing the 2000 rule / multi-route-convoys)
	*	<p>
	*	Thus, Say we have the following:<br>
	*		<code>path: a-b-c</code><br>
	*	if a, b, OR c is marked as 'invalid', we do not have a complete path, thus false results.
	*	<p>
	*	But, if we have 2 paths:<br>
	*		<code>path 1: a-b-c</code><br>
	*		<code>path 2: a-d-c</code><br>
	*	if a or c is invalid, path will fail (false) returned.
	*	if b OR d is invalid (but a & c are good), path will succeed, since an alternate path exists. 
	*		This will return true unless both b & d are invalid
	*	<p>
	*	
	*	<h3>Returns:</h3>
	*	<ul>	
	*		<li>Tristate.FAILURE<br>
	*				a) no path exists from src-dest, or a convoy was dislodged / failed on this route.</li>
	*		
	*		<li>Tristate.UNCERTAIN<br>
	*				a) a path exists, but, the status of the convoy units is not yet determined.</li>
	*				
	*		<li>Tristate.SUCCESS<br>
	*				a) a path exists, and all Convoy orders along that path are SUCCESS.
	*					note that if multiple paths exist, only one path has to be successful 
	*					for this to work.</li>
	*	</ul>				
	*	<p>
	*	There is an optional argument, validPath. If a List is supplied, and the convoy route is
	*	successful, the path taken will be returned in this List. If this argument is null,
	*	it will be ignored. The returned List will contain only Province objects.
	*	<p>
	*	<h3>Algorithm:</h3>
	*		we must call SuperConvoyPath evaluator twice. The first time, we check for
	*		successes, by not counting "uncertain" convoys. This is because we cannot
	*		easily distinguish a successfull path from an uncertain path.
	*		<p>
	*		The second time through (if we were not succesful), we check to see if we
	*		fail (no path), or if we are uncertain.
	*	
	*/
	public Tristate getConvoyRouteEvaluation(Location src, Location dest, Location invalid, List validPath)
	{
		List path = new ArrayList(12);
		SuperConvoyPathEvaluator spe = null;
		boolean isPathFound = false;
		
		// 1st pass: look for a successful route only.
		spe = new SuperConvoyPathEvaluator(src, dest, invalid, false);
		isPathFound = findPathBreadthFirst(src, dest, src, path, spe);
		if(isPathFound)
		{
			// note: our path, if found, may be longer than required (due to
			// breadth-first search. So iterate until we find the dest.
			if(validPath != null)
			{
				for(int i=0; i<path.size(); i++)
				{
					Location loc = (Location) path.get(i);
					validPath.add(loc.getProvince());
					if(dest.isProvinceEqual(loc))
					{
						break;
					}
				}
			}
			
			return Tristate.SUCCESS;
		}
		
		
		// 2nd pass: determine unsuccessful vs. uncertain
		path.clear();
		spe = new SuperConvoyPathEvaluator(src, dest, invalid, true);
		isPathFound = findPathBreadthFirst(src, dest, src, path, spe);
		if(isPathFound)
		{
			// TODO: assert that isUncertain() is true here. It should be; 
			// otherwise, we would have returned above.
			return Tristate.UNCERTAIN;
		}
		
		return Tristate.FAILURE;
	}// getConvoyRouteEvaluation()
	
	
	
	
	
	
	
	
	
	/**
	* 	Generalized recursive Path-Finder, Breadth-First search.
	*	<p>
	*	The path evaluator decides WHICH locations to add to the list of
	*	<p>
	*	The first path found that meets criteria will be in 'path'
	*
	*
	*
	*
	*/
	protected boolean findPathBreadthFirst(Location src, Location dest, 
		Location current, List path, PathEvaluator pathEvaluator)
	{
		// Step 1: add current location to path
		path.add(current);
		
		// Step 2: check if current location is adjacent to dest
		// if so, add it to the path, and return 'true', since we are done.
		if(pathEvaluator.isAdjacentToDest(current, dest))
		{
			path.add(dest);
			return true;
		}
		
		// Step 3: find all adjacent locations to the current location.
		// note that we ONLY add a location if it is ok'd by the PathEvaluator.
		List adjLocs = new LinkedList();
		for(int i=0; i<Coast.ALL_COASTS.length; i++)
		{
			Location[] locations = current.getProvince().getAdjacentLocations(Coast.ALL_COASTS[i]);
			
			for(int j=0; j<locations.length; j++)
			{
				Location testLoc = locations[j];
				if(pathEvaluator.evaluate(testLoc))
				{
					adjLocs.add(testLoc);
				}
			}
		}
		
		// Step 4: If there are no locations in our adjacency list,
		// then, we do not have a path
		if(adjLocs.isEmpty())
		{
			return false;
		}
		
		
		// Step 5: We have one or more possible routes to check.
		// If we find that a route is invalid, we will remove it
		// from adjacency list.
		Iterator iter = adjLocs.iterator();
		while(iter.hasNext())
		{
			Location location = (Location) iter.next();
			
			if(path.contains(location))
			{
				// if adjacent province already in the path, we are going
				// in circles (or at least backwards)! remove it.
				iter.remove();
			}
			else
			{
				// we haven't yet visited this Location. We will recusively
				// evaluate this position, and remove this location from the
				// list iff we return 'false'.
				if( !findPathBreadthFirst(src, dest, location, path, pathEvaluator) )
				{
					iter.remove();
				}
			}
		}
		
		// Step 6: If there are ANY paths left in the adjacency list, we have
		// at least one path that may be valid.
		return !(adjLocs.isEmpty());
	}// findPathBreadthFirst()
	
	
	
	protected static interface PathEvaluator
	{
		// see if current location has nesc. requirments
		// to add it to the path.
		public boolean evaluate(Location location);
		
		// only called to check if we are at the end.
		public boolean isAdjacentToDest(Location current, Location dest);
	}// inner interface PathEvaluator
	
	
	
	protected class AnyConvoyPathEvaluator implements PathEvaluator
	{
		// if this is false, then we have not even found a single convoy
		// and we cannot possibly have a valid path. This is to prevent
		// us from checking if src.isAdjacent(dest) when src,dest are immediately
		// adjacent to each other (by land) but we may not even *have* convoy fleets
		// near enough in the water!
		private boolean foundConvoy = false;
		
		// must have a fleet in the desired area
		public boolean evaluate(Location location)
		{
			Province province = location.getProvince();
			Unit unit = position.getUnit(province);
			
			if(unit != null && (province.isSea() || province.isConvoyableCoast()))
			{
				if(unit.getType() == Unit.Type.FLEET)
				{
					final boolean result = evalFleet(province, unit);
					
					if(result)
					{
						foundConvoy = true;
					}
					
					return result;
				}
			}
			return false;
		}// evaluate()
		
		// must be adjacent by PROVINCE (not coastal) to destination.
		public boolean isAdjacentToDest(Location current, Location dest)
		{
			Province province = current.getProvince();
			if(province.isTouching(dest.getProvince()) && foundConvoy)
			{
				return true;
			}
			return false;
		}// isAdjacentToDest()
		
		// can subclass to do further fleet evaluation
		// we do no further evaluation with this class.
		// evaluates the fleet present in this province
		protected boolean evalFleet(Province province, Unit unit)
		{
			return true;
		}// evalFleet()
	}// inner class AnyConvoyPathEvaluator
	
	
	
	
	private class LegalConvoyPathEvaluator extends AnyConvoyPathEvaluator
	{
		private Location src = null;
		private Location dest = null;
		
		
		// set Src and Dest of path, so we can evaluate fleet orders
		public LegalConvoyPathEvaluator(Location src, Location dest)
		{
			if(adjudicator == null)
			{
				throw new IllegalStateException("null adjudicator in path");
			}
			
			this.src = src;
			this.dest = dest;
		}// LegalConvoyPathEvaluator()
		
		// override: check fleet orders
		protected boolean evalFleet(Province province, Unit unit)
		{
			OrderState os = adjudicator.findOrderStateBySrc( province );
			Orderable order = os.getOrder();
			
			if(order instanceof Convoy)
			{
				Convoy convoy = (Convoy) order;
				
				if(	convoy.getConvoySrc().isProvinceEqual(src) 
					&& convoy.getConvoyDest().isProvinceEqual(dest) )
				{
					if(os.getEvalState() != Tristate.FAILURE && os.getDislodgedState() != Tristate.YES)
					{
						return true;
					}
				}
			}
			
			return false;
		}// evalFleet()
		
	}// inner class LegalConvoyPathEvaluator
	
	
	
	
	
	private class SuperConvoyPathEvaluator extends AnyConvoyPathEvaluator
	{
		private Location src = null;
		private Location dest = null;
		private Location invalid = null;
		
		private boolean isUncertain = false;	// if we found one or more uncertains.
		private boolean isFailure = false;		// if we found one or more failures/dislodged
		private boolean noteUncertains = false;
		
		// set Src and Dest of path, so we can evaluate fleet orders
		public SuperConvoyPathEvaluator(Location src, Location dest, Location invalid, boolean noteUncertains)
		{
			if(adjudicator == null)
			{
				throw new IllegalStateException("null adjudicator in path");
			}
			
			this.src = src;
			this.dest = dest;
			this.invalid = invalid;
			this.noteUncertains = noteUncertains;
		}// SuperConvoyPathEvaluator()
		
		// success / failure depends upon if a route can be found. However, if a route
		// is found, it may be "uncertain". 
		public boolean isUncertain()
		{
			return isUncertain;
		}// getPathStatus()
		
		
		public boolean isFailure()
		{
			return isFailure;
		}// isFailure()
		
		// override: check fleet orders
		protected boolean evalFleet(Province province, Unit unit)
		{
			OrderState os = adjudicator.findOrderStateBySrc( province );
			Orderable order = os.getOrder();
			
			if(order instanceof Convoy)
			{
				Convoy convoy = (Convoy) order;
				
				if(	convoy.getConvoySrc().isProvinceEqual(src) 
					&& convoy.getConvoyDest().isProvinceEqual(dest) )
				{
					// we found a correctly matching Convoy order.
					//
					// but, if this order should be ignored ('invalid'), we won't consider it.
					if(invalid != null)
					{
						if(invalid.getProvince() == province)
						{
							return false;
						}
					}
					
					Tristate evalState = os.getEvalState();
					
					if(evalState == Tristate.FAILURE || os.getDislodgedState() == Tristate.YES)
					{
						isFailure = true;
						return false;	// not valid for the path
					}
					else if(evalState == Tristate.UNCERTAIN)
					{
						if(noteUncertains)
						{
							isUncertain = true;
							return true;	
						}
						else
						{
							return false;
						}
					}
					else if(evalState == Tristate.SUCCESS)
					{
						return true;	// definately valid!
					}
					else
					{
						// this is not strictly needed.
						throw new IllegalStateException("path exception: else case reached");
					}
				}
			}
			
			return false;
		}// evalFleet()
		
	}// inner class SuperConvoyPathEvaluator	
	
	
	/**
	*	Find shortest distance between src & dest.
	*	Note that this uses 'touching' adjacency, and that
	*	the cost of movement between any adjacent province
	*	is the same.
	*	<p>
	*	This will return -1 in the event that src and dest are
	*	not connected. 
	*	<p>
	*	Null src/dest Provinces are not allowed
	*	
	*/
	public int getMinDistance(Province src, Province dest)
	{
		// simple case: src adjacent to dest
		if(src == dest)
		{
			return 0;
		}
		
		int dist = 0;
		
		HashMap visited = new HashMap(119);
		visited.put(src, Boolean.TRUE);	
		
		ArrayList toCheck = new ArrayList(32);
		ArrayList nextToCheck = new ArrayList(32);
		ArrayList swapTmp = null;
		toCheck.add(src);
		
		while(true)
		{
			// inc dist
			dist++;
			
			// iterate toCheck, create nextToCheck list
			for(int z=0; z<toCheck.size(); z++)
			{
				Province p = (Province) toCheck.get(z);
				if(p == dest)
				{
					return dist;
				}
				
				/* OLD CODE: before Coast.WING (TOUCHING) available
				for(int i=0; i<Coast.ALL_COASTS.length; i++)
				{
					Location[] locs = p.getAdjacentLocations(Coast.ALL_COASTS[i]);
					for(int j=0; j<locs.length; j++)
					{
						Province ckp = locs[j].getProvince();
						
						if(visited.get(ckp) == null)
						{
							nextToCheck.add(ckp);
							visited.put(ckp, Boolean.TRUE);
						}
					}
				}
				*/
				
				// NEW CODE: using Coast.TOUCHING
				Location[] locs = p.getAdjacentLocations(Coast.TOUCHING);
				for(int i=0; i<locs.length; i++)
				{
					Province ckp = locs[i].getProvince();
					
					if(visited.get(ckp) == null)
					{
						nextToCheck.add(ckp);
						visited.put(ckp, Boolean.TRUE);
					}
				}
				// END NEW CODE
			}
			
			// swap lists
			toCheck.clear();
			swapTmp = toCheck;
			toCheck = nextToCheck;
			nextToCheck = swapTmp;
			
			// test for unconnectedness (remember, we are swapped)
			if(toCheck.isEmpty())
			{
				return -1;
			}
		}
	}// getMinDistance()
	
	
	//// NEW path finding stuff below here....
	
	/**
	*	Finds all sea paths from src to dest.. 
	*	<p>
	*	This will return a zero-length array iff src or dest is not adjacent to 
	*	a sea or a convoyable coastal ("sea equivalent") province.
	*	<p>
	*	Otherwise, all possible unique paths are returned, subject to 
	*	the evaluation constraints of the FAPEvaluator.
	*	<p>
	*	This is typically very fast. For standard map, gas->lvp takes about
	*	0.155 ms on a P4/3.0ghz; 10 unique paths are found. More specific 
	*	FAPEvaluator methods (e.g., that look for a Fleet) will be faster.
	*/
	public static Province[][] findAllSeaPaths(FAPEvaluator evaluator, Province src, Province dest)
	{
		// check: src/dest
		if(!src.isLand() || !dest.isLand())
		{
			return new Province[0][];
		}
		
		// quick check: dest: next to at least 1 sea/conv coastal province
		final Location[] dLocs = dest.getAdjacentLocations(Coast.TOUCHING);
		boolean isOk = false;
		for(int i=0; i<dLocs.length; i++)
		{
			final Province p = dLocs[i].getProvince();
			if(p.isConvoyableCoast() || p.isSea())
			{
				isOk = true;
				break;
			}
		}
		
		if(!isOk)
		{
			return new Province[0][];
		}
		
		// create the tree.
		final TreeNode root = new TreeNode(null, src);
		
		// breadth-first add of Sea or Convoayble Coastal provinces to the tree,
		// that are adjacent to the current node. A path cannot use the same
		// TreeNode more than once, so we use addUniqueChild() to ensure this.
		//
        LinkedList queue = new LinkedList();
        queue.addLast(root);
        while( queue.size() > 0 ) 
		{
			TreeNode node = (TreeNode) queue.removeFirst();
			Province prov = node.getProvince();
			final Location[] locs = prov.getAdjacentLocations(Coast.TOUCHING);
			for(int i=0; i<locs.length; i++)
			{
				Province p = locs[i].getProvince();
				if(p.equals(dest))
				{
					// special case. dest is NOT nescessarily a convoyable coast,
					// and is not Sea, and should not be evaluated with TreeBuilder.
					TreeNode newNode = new TreeNode(node, p);
					node.addUniqueChild(newNode);
				}
				else if( (p.isConvoyableCoast() || p.isSea())
						  && evaluator.evaluate(p) )
				{
					TreeNode newNode = new TreeNode(node, p);
					if(node.addUniqueChild(newNode))
					{
						queue.addLast(newNode);
					}
				}
			}
        }
		
		// return all paths from root, now that tree is built.
		return root.getAllBranchesTo(dest);	
	}// findAllSeaPaths()
	
	
	
	/**
	*	FAPEvaluator class. Determines if a Province should be added
	*	to the tree of possible moves.
	*	<p>
	*	By default, this just returns true. When used with 
	*	<code>findAllSeaPaths</code>, this will return <b>all</b> possible 
	*	unique convoy paths from the source to the destination.
	*	<p>
	*	For example, if we wanted to know if a unit was present in 
	*	the province, we could test for it here in the evaluate()
	*	method.
	*/
	public static class FAPEvaluator	// FAP == "Find All Paths"
	{
		/** Evaluate if a Province should be added. */
		public boolean evaluate(Province province)
		{
			return true;
		}// evaluate()
		
	}// inner class FAPEvaluator
	
	
	/**
	*	FAPEvaluator that checks to see if there is a Fleet present
	*	in the given Province. This is useful to find theoretical, or
	*	"possible" convoy paths between two locations.
	*/
	public static class FleetFAPEvaluator extends FAPEvaluator
	{
		private final Position fapPos;
		
		/** Create a FleetFAPEvaluator */
		public FleetFAPEvaluator(Position pos)
		{
			fapPos = pos;
		}// FleetFAPEvaluator()
		
		/** Evaluate if a Province should be added. */
		public boolean evaluate(Province province)
		{
			return fapPos.hasUnit(province, Unit.Type.FLEET);
		}// evaluate()
	}// inner class FleetFAPEvaluator
	
	
	/**
	*	FAPEvaluator that checks to see if there is a Fleet present
	*	in the given Province that has orders to Convoy, and that
	*	convoy order has the given convoy-source and convoy-destination 
	*	provinces.
	*	<p>
	*	This additionally checks to make sure that the Convoying Fleet in the
	*	province being evaluated is not dislodged and has not 
	*	failed.
	*/
	public static class ConvoyFAPEvaluator extends FAPEvaluator
	{
		private final Adjudicator adj;
		private final Province src;
		private final Province dest;
		
		/** Create a ConvoyFAPEvaluator */
		public ConvoyFAPEvaluator(Adjudicator adj, Province src, Province dest)
		{
			this.adj = adj;
			this.src = src;
			this.dest = dest;
		}// ConvoyFAPEvaluator()
		
		/** Evaluate if a Province should be added. */
		public boolean evaluate(Province province)
		{
			final Position pos = adj.getTurnState().getPosition();
			if(pos.hasUnit(province, Unit.Type.FLEET))
			{
				OrderState os = adj.findOrderStateBySrc( province );
				final Orderable order = os.getOrder();
				
				if(order instanceof Convoy)
				{
					Convoy convoy = (Convoy) order;
					
					if(	convoy.getConvoySrc().isProvinceEqual(src) 
						&& convoy.getConvoyDest().isProvinceEqual(dest) )
					{
						if( os.getEvalState() != Tristate.FAILURE 
							&& os.getDislodgedState() != Tristate.YES )
						{
							return true;
						}
					}
				}
			}
			return false;
		}// evaluate()
	}// inner class ConvoyFAPEvaluator
	
	
	/** Node of a Tree that holds a Location */
	private static class TreeNode
	{
		private final TreeNode parent;
		private final Province prov;
		private final int depth;
		private List kids;
		
		/** Create a TreeNode. Null parent is the root. Null Location not ok. */
		public TreeNode(TreeNode parent, Province prov)
		{
			if(prov == null) { throw new IllegalArgumentException(); }
			this.parent = parent;
			this.prov = prov;
			this.kids = new ArrayList(4);	// ?? vs. linkedlist
			this.depth = (parent == null) ? 0 : (parent.getDepth() + 1);
		}// TreeNode()
		
		/** Get Location */
		public final Province getProvince() { return prov; }
		
		/** Get Parent */
		public final TreeNode getParent() { return parent; }
		
		/** Get # of children */
		public final int getChildCount() { return kids.size(); }
		
		/** Depth. 0 == root. */
		public final int getDepth() { return depth; }
		
		/** Leaf Node? (no kids) */
		public final boolean isLeaf() { return kids.isEmpty(); }
		
		/** Add a child */
		public void addChild(TreeNode child)
		{
			if(child == null) { throw new IllegalArgumentException(); }
			kids.add(child);
		}// addChild()
		
		/** 
		*	Add a child, but only if it Location is unique, as compared
		*	to its lineage (all parents).. 
		*/
		public boolean addUniqueChild(TreeNode child)
		{
			if(child == null) { throw new IllegalArgumentException(); }
			TreeNode mommy = getParent();
			while(mommy != null)
			{
				if(child.getProvince().equals(mommy.getProvince()))
				{
					return false;
				}
				mommy = mommy.getParent();
			}
			
			kids.add(child);
			return true;
		}// addUniqueChild()
		
		/** 
		*	Get all branches of the Tree, starting with this node.
		*	If used on the root node, this would get ALL nodes.
		*	This is an IRREGULAR 2D list of Locations.
		*	Location[a][b] where a == the branch, and b == the
		*	location on the branch (0 to length).
		*
		*/
		public Province[][] getAllBranches()
		{
			// first: do a BFS to find all leaf nodes (no kids).
			// we'll store these in a list, and then iterate back
			// up using getParent().
			LinkedList leafNodeList = new LinkedList();
			LinkedList queue = new LinkedList();
			
			queue.addLast(this);
			while(queue.size() > 0)
			{
				TreeNode n = (TreeNode) queue.removeFirst();
				
				if(n.isLeaf())
				{
					leafNodeList.addLast(n);
				}
				
				for(int i=0; i<n.kids.size(); i++)
				{
					queue.addLast(n.kids.get(i));
				}
            }
			
			return createProvinceArray(leafNodeList);
		}// getAllBranches()
		
		/**
		*	Get all branches of the Tree, starting with this node,
		*	and ending with a TreeNode that contains the given Province.
		*	<p>
		*	Otherwise similar to getAllBranches()
		*
		*/
		public Province[][] getAllBranchesTo(Province end)
		{
			LinkedList leafNodeList = new LinkedList();
			LinkedList queue = new LinkedList();
			
			queue.addLast(this);
			while(queue.size() > 0)
			{
				TreeNode n = (TreeNode) queue.removeFirst();
				
				if(n.isLeaf() && n.getProvince().equals(end))
				{
					leafNodeList.addLast(n);
				}
				
				for(int i=0; i<n.kids.size(); i++)
				{
					queue.addLast(n.kids.get(i));
				}
            }
			
			return createProvinceArray(leafNodeList);
		}// getAllBranchesTo()
		
		
		
		/** Creates a Province array from a List of endpoints. */
		private Province[][] createProvinceArray(List list)
		{
			Province[][] pathArray = new Province[list.size()][];
			int idx = 0;
			Iterator iter = list.iterator();
			while(iter.hasNext())
			{
				
				TreeNode n = (TreeNode) iter.next();
				Province[] path = new Province[n.getDepth()+1];	// root is depth 0
				for(int i=(path.length - 1); i>=0; i--)
				{
					path[i] = n.getProvince();
					n = n.getParent();
				}
				
				pathArray[idx] = path;
				idx++;
			}
			
			return pathArray;
		}// createProvinceArray()
		
		
		/** Print the Tree from this node to System.out */
		public void print()
		{
			Province[][] px = getAllBranches();
			
			for(int i=0; i<px.length; i++)
			{
				StringBuffer sb = new StringBuffer(128);
				sb.append(px[i][0].getShortName());
				for(int j=1; j<px[i].length; j++)
				{
					sb.append("-");
					sb.append(px[i][j].getShortName());
				}
				System.out.println(sb);
			}
		}// print()
		
	}// inner class TreeNode
	
}// class Path
