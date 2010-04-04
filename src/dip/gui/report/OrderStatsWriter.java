//
//  @(#)OrderStatsWriter.java		5/2004
//
//  Copyright 2004 Zachary DelProposto. All rights reserved.
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
package dip.gui.report;

import dip.gui.ClientFrame;
import dip.gui.dialog.TextViewer;

import dip.misc.Utils;
import dip.misc.Help;

import dip.world.World;
import dip.world.Power;
import dip.world.Phase;
import dip.world.Unit;
import dip.world.TurnState;
import dip.world.Position;

import dip.gui.dialog.TextViewer;

import dip.order.*;
import dip.order.result.OrderResult;
import dip.order.OrderFormatOptions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.HashMap;
import java.util.List;

import java.text.DecimalFormat;

import javax.swing.JScrollPane;

/**
*	Order Statistics
*
*/
public class OrderStatsWriter
{
	// i18n constants
	private static final String DIALOG_TITLE				= "OrderStatsWriter.dialog.title";
	private static final String HTML_TEMPLATE 			= "OrderStatsWriter.template"; // points to HTML file
	private static final String AVERAGE_HEADER			= "OrderStatsWriter.label.average";
	private static final String NO_RESULTS_HTML_MSG		= "OrderStatsWriter.noresults";	// points to HTML file
	
	// hilite  
	private static final String TR_HIGHLIGHT			= "<tr bgcolor=\"#E6EEF0\">";
	
	// instance variables
	private final World 		world;
	private final Power[] 		allPowers;
	private final OrderFormatOptions ofo;
	private final DecimalFormat pctFmt = new DecimalFormat("###%");
	
	/**
	*	Gets the order statistics as HTML
	*/
	public static String getOrderStatsAsHTML(World w, OrderFormatOptions orderFormatOptions)
	{
		OrderStatsWriter osw = new OrderStatsWriter(w, orderFormatOptions);
		return osw.getResultsAsHTML();
	}// getOrderStatsAsHTML()
	
	
	/**
	*	Returns the HTML-encoded Order Statistics for
	*	an entire game, inside a dialog.
	*/
	public static void displayDialog(final ClientFrame clientFrame, 
		final World w, final OrderFormatOptions orderFormat)
	{
		TextViewer tv = new TextViewer(clientFrame);
		tv.setEditable(false);
		tv.addSingleButton( tv.makeOKButton() );
		tv.setTitle(Utils.getLocalString(DIALOG_TITLE));
		tv.setHeaderVisible(false);
		tv.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		
		tv.lazyLoadDisplayDialog(new TextViewer.TVRunnable()
		{
			public void run()
			{
				setText(getOrderStatsAsHTML(w, orderFormat));
			}
		});
	}// displayDialog()
	
	
	/** OrderStatsWriter constructor. */
	private OrderStatsWriter(World w, OrderFormatOptions ofo)
	{
		world = w;
		allPowers = w.getMap().getPowers();
		this.ofo = ofo;
		
		pctFmt.setMaximumFractionDigits(0); 
	}// OrderStatsWriter()
	
	
	/** Create HTML results */
	private String getResultsAsHTML()
	{
		final MovePhaseTurnData[] mptd = collectData();
		
		// if there are no movement-phase results, return a 
		// notice indicating we can't yet calculate statistics.
		if(mptd.length == 0)
		{
			return Utils.getText(Utils.getLocalString(NO_RESULTS_HTML_MSG));
		}
		
		// get template
		String templateText = Utils.getText(Utils.getLocalString(HTML_TEMPLATE));
		
		// get template objects
		Object[] templateData = new Object[]
		{
			makeTable(mptd, 0), 	// {0} : 
			makeTable(mptd, 3), 	// {1} : 
			makeTable(mptd, 1),		// {2} : 
			makeTable(mptd, 2)		// {3} : 
		};
		
		// format into template
		return Utils.format(templateText, templateData);
	}// getResultsAsHTML()
	
	
	private String makeTable(final MovePhaseTurnData[] mptds, int type)
	{
		StringBuffer sb = new StringBuffer(4096);
		
		sb.append("<table cellspacing=\"3\" cellpadding=\"1\" border=\"0\">");
		
		// header row.
		// [EMPTY] [POWER-1] [POWER-2] ... [POWER-n] [average]
		//
		sb.append("<tr>");
		sb.append("<td></td>");		// empty
		for(int i=0; i<allPowers.length; i++)
		{
			sb.append("<td><b> &nbsp;");
			sb.append(allPowers[i].getName());
			sb.append("&nbsp; </b></td>");
		}
		sb.append("<td>");
		sb.append( Utils.getLocalString(AVERAGE_HEADER) );
		sb.append("<td>");
		sb.append("</tr>");
		
		
		for(int row=0; row<mptds.length; row++)
		{
			MovePhaseTurnData mptd = mptds[row];
			
			// by-year row
			// [year] [%] [%] ... [%} [avg-%]
			// 
			final Phase phase = mptd.getPhase();
			final Stats[] stats = mptd.getStats();
			
			sb.append( (((row & 1) == 0) ? TR_HIGHLIGHT : "<tr>") );
			
			sb.append("<td><b>");
			sb.append(phase.getSeasonType());
			sb.append(", ");
			sb.append(phase.getYearType());
			sb.append("</b></td>");
			
			float sum = 0.0f;
			int nPowers = 0;			// for computing average....
			float value = 0.0f;
			for(int i=0; i<allPowers.length; i++)
			{
				switch(type)	// it's just so easy...
				{
					case 0:
						value = stats[i].getOverallSuccess();
						break;
					case 1:
						value = stats[i].getPercentSupport();
						break;
					case 2:
						value = stats[i].getPercentNonSelfSupport();
						break;
					case 3:
						value = stats[i].getPercentMoveSuccess();
						break;
					default:
						throw new IllegalStateException();
				}
				
				if(stats[i].isEliminated || value < 0.0f)
				{
					// don't add to average, don't print 0% (just empty),
					// don't increment nPowers
					sb.append("<td></td>");
				}
				else
				{
					sum += value;
					nPowers++;
					
					sb.append("<td>");
					sb.append( pctFmt.format(value) );
					sb.append("</td>");
				}
			}
			
			sb.append("<td>");
			sb.append( pctFmt.format(sum / (float) nPowers) );
			sb.append("</td>");
			
			sb.append("</tr>");
		}
		
		sb.append("</table>");
		
		return sb.toString();
	}// makeOrderSuccessRateTable()
	
	private String makeSupportRateTable(final MovePhaseTurnData[] mptds)
	{
		StringBuffer sb = new StringBuffer(4096);
		return sb.toString();
	}// makeSupportRateTable()
	
	private String makeNonSelfSupportRateTable(final MovePhaseTurnData[] mptds)
	{
		StringBuffer sb = new StringBuffer(4096);
		return sb.toString();
	}// makeNonSelfSupportRateTable()
	
	
	
	
	
	
	
	
	
	
	
	
	/**
	*	Makes an array of tabular data, for easy calculation.
	*	ONLY Movement TURNS are used to create statistical data.
	*/
	public MovePhaseTurnData[] collectData()
	{
		List turns = world.getAllTurnStates();
		ArrayList data = new ArrayList(turns.size());
		
		Iterator iter = turns.iterator();
		while(iter.hasNext())
		{
			TurnState ts = (TurnState) iter.next();
			if( ts.isResolved() && 
				Phase.PhaseType.MOVEMENT.equals(ts.getPhase().getPhaseType()) )
			{
				data.add( new MovePhaseTurnData(ts) );
			}
		}
		
		return (MovePhaseTurnData[]) data.toArray(new MovePhaseTurnData[data.size()]);
	}// collectData()
	
	
	
	/** Class to hold and gather Movement-Phase turn statistics */
	private class MovePhaseTurnData
	{
		private final Phase	phase;
		private final Stats[] stats;
		
		public MovePhaseTurnData(TurnState ts)
		{
			if(!ts.getPhase().getPhaseType().equals(Phase.PhaseType.MOVEMENT))
			{
				throw new IllegalArgumentException();
			}
			
			if(!ts.isResolved())
			{
				throw new IllegalArgumentException();
			}
			
			this.phase = ts.getPhase();
			this.stats = new Stats[allPowers.length];
			collectStats(ts);
		}// MovePhaseTurnData()
		
		public Phase getPhase()
		{
			return phase;
		}
		
		public Stats[] getStats()
		{
			return stats;
		}// getStats()
		
		public Stats getStats(Power p)
		{
			if(p == null)
			{
				throw new IllegalArgumentException();
			}
			
			for(int i=0; i<stats.length; i++)
			{
				if(p.equals(stats[i].getPower()))
				{
					return stats[i];
				}
			}
			
			throw new IllegalStateException();
		}// getStats()
		
		
		private void collectStats(TurnState ts)
		{
			// create order-result mapping
			HashMap resultMap = new HashMap(53);
			Iterator iter = ts.getResultList().iterator();
			while(iter.hasNext())
			{
				Object obj = iter.next();
				if(obj instanceof OrderResult)
				{
					OrderResult ordRes = (OrderResult) obj;
					
					// we only map SUCCESSFULL orders.
					if(ordRes.getResultType() == OrderResult.ResultType.SUCCESS)
					{
						resultMap.put(ordRes.getOrder(), Boolean.TRUE);
					}
				}
			}
			
			// create statistics
			for(int i=0; i<allPowers.length; i++)
			{
				Stats s = new Stats(allPowers[i]);
				
				s.isEliminated = ts.getPosition().isEliminated(allPowers[i]);
				
				iter = ts.getOrders(allPowers[i]).iterator();
				while(iter.hasNext())
				{
					s.nOrders++;
					
					final Orderable order = (Orderable) iter.next();
					final boolean success = (resultMap.get(order) == Boolean.TRUE);
					
					if(order instanceof Move)
					{
						s.nMoves++;
						if(success) { s.nMovesOK++; }
					}
					else if(order instanceof Hold)
					{
						s.nHolds++;
						if(success) { s.nHoldsOK++; }
					}
					else if(order instanceof Convoy)
					{
						s.nConvoys++;
						if(success) { s.nConvoysOK++; }
					}
					else if(order instanceof Support)
					{
						s.nSupports++;
						if(success) { s.nSupportsOK++; }
						
						// self support?
						final Support sup = (Support) order;
						final Unit supUnit = ts.getPosition().getUnit(sup.getSupportedSrc().getProvince());
						if(supUnit != null)
						{
							if(sup.getPower().equals( supUnit.getPower() ))
							{
								s.nSupportsSelf++;
								if(success) { s.nSupportsSelfOK++; }
							}
						}
					}
				}
				
				stats[i] = s;
			}
		}// collectStats()
		
	}// inner class MovePhaseTurnData
	
	private class Stats
	{
		private final Power power;			// power for which these stats apply
		public boolean isEliminated = false;
		public int nOrders = 0;				// total # of orders
		public int nMoves = 0;
		public int nConvoys = 0;
		public int nHolds = 0;				
		public int nSupports = 0;			// supports to any unit
		public int nSupportsSelf = 0;		// supports of own units
		public int nMovesOK = 0;			// successful Move orders
		public int nConvoysOK = 0;			// successful Convoy orders
		public int nHoldsOK = 0;			// successful Hold orders
		public int nSupportsOK = 0;			// # successful total supports
		public int nSupportsSelfOK = 0;		// # successful self-supports
		
		// other data to collect (per-power)
		// # of units total
		// # of units moving?
		// # of units dislodged?
		// 
		public Stats(Power p)
		{
			if(p == null) 
			{
				throw new IllegalArgumentException();
			}
			
			this.power = p;
		}// Stats()
		
		/** Get the Power */
		public Power getPower()
		{
			return power;
		}// getPower()
		
		/** Calculate percent successful orders (all orders) */
		public float getOverallSuccess()
		{
			if(getTotal() == 0)
			{
				return 0.0f;
			}
			
			final int success = (nMovesOK + nConvoysOK + nHoldsOK + nSupportsOK);
			return ((float) success / (float) getTotal());
		}
		
		/** 
		*	Calculate percent support orders (successfull or failed), 
		*	of all total orders. 
		*/
		public float getPercentSupport()
		{
			if(getTotal() == 0)
			{
				return 0.0f;
			}
			
			return ((float) nSupports / (float) getTotal());
		}
		
		/** 
		*	Calculate percent SELF support orders (successfull or failed), 
		*	of all total orders. 
		*/
		public float getPercentSelfSupport()
		{
			if(getTotal() == 0)
			{
				return 0.0f;
			}
			
			return ((float) nSupportsSelf / (float) getTotal());
		}
		
		/** 
		*	Calculate percent NON-SELF support orders (successfull or failed), 
		*	of all total orders. 
		*/
		public float getPercentNonSelfSupport()
		{
			assert (nSupports >= nSupportsSelf);
			if(getTotal() == 0)
			{
				return 0.0f;
			}
			
			return ((float) (nSupports-nSupportsSelf) / (float) getTotal());
		}
		
		/** 
		*	Calculate percent successful Move orders. 
		*	Returns negative # if no Move orders
		*/
		public float getPercentMoveSuccess()
		{
			if(nMoves == 0)
			{
				return -1.0f;
			}
			
			return ((float) nMovesOK / (float) nMoves);
		}// getPercentMoveSuccess()
		
		
		/** Get total orders */
		private int getTotal()
		{
			return (nMoves + nConvoys + nHolds + nSupports);
		}// getTotal()
		
	}// inner class Stats
	
	
	
}// class OrderStatsWriter
