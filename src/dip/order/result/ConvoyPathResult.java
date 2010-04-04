//	
//	@(#)ConvoyPathResult.java	12/2003
//	
//	Copyright 2003 Zachary DelProposto. All rights reserved.
//	Use is subject to license terms.
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
//  Or from http://www.gnu.org/package dip.order.result;
//
package dip.order.result;

import dip.order.Orderable;
import dip.order.OrderFormat;
import dip.order.OrderFormatOptions;
import dip.world.Location;
import dip.world.Province;
import dip.misc.Utils;

import java.util.List;
/**
*	
*	An OrderResult that contains the path taken by a successfully 
*	convoyed Move. It has the result type of CONVOY_PATH_TAKEN.
*
*/
public class ConvoyPathResult extends OrderResult
{
	// i18n
	private static final String KEY_MESSAGE = "ConvoyPathResult.message";
	private static final String KEY_ARROW = "ConvoyPathResult.arrow";
	
	// instance fields
	private Province[] convoyPath = null;
	
	
	/** Create a ConvoyPathResult */
	public ConvoyPathResult(Orderable order, List path)
	{
		this(order, (Province[]) path.toArray(new Province[path.size()]));
	}// ConvoyPathResult()
	
	/** Create a ConvoyPathResult */
	public ConvoyPathResult(Orderable order, Province[] convoyPath)
	{
		super();
		if(convoyPath == null || convoyPath.length < 3)
		{
			throw new IllegalArgumentException("bad path (null or length < 3)");
		}
		
		this.power = order.getPower();
		this.message = null;
		this.order = order;
		this.resultType = OrderResult.ResultType.CONVOY_PATH_TAKEN;
		this.convoyPath = convoyPath;
	}// ConvoyPathResult()
	
	
	/** Gets the Convoy Path; path includes source and destination provinces. */
	public Province[] getConvoyPath()
	{
		return convoyPath;
	}// getConvoyPath()
	
	
	/**
	*	Creates an appropriate internationalized text message given the 
	*	convoy path.
	*/
	public String getMessage(OrderFormatOptions ofo)
	{
		/*
		arguments:
			{0}	: convoy path taken.
		*/
		
		// create path list
		StringBuffer sb = new StringBuffer(128);
		final String arrow = Utils.getLocalString(KEY_ARROW);
		
		sb.append(OrderFormat.format(ofo, convoyPath[0]));
		for(int i=1; i<convoyPath.length; i++)
		{
			sb.append(arrow);
			sb.append(OrderFormat.format(ofo, convoyPath[i]));
		}
		
		// return formatted message
		return Utils.getLocalString(KEY_MESSAGE, sb.toString());
	}// getMessage()
	
	
	/**
	*	Primarily for debugging.
	*/
	public String toString()
	{
		StringBuffer sb = new StringBuffer(256);
		sb.append(super.toString());
		
		// add convoy path
		sb.append(" convoy path: ");
		sb.append(convoyPath[0].getShortName());
		for(int i=1; i<convoyPath.length; i++)
		{
			sb.append("-");
			sb.append(convoyPath[i].getShortName());
		}
		
		return sb.toString();
	}// toString()
	
}// class ConvoyPathResult
