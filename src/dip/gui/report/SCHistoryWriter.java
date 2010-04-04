//
//  @(#)SCHistoryWriter.java		12/2003
//
//  Copyright 2003 Zachary DelProposto. All rights reserved.
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
import dip.world.*;
import dip.gui.map.*;
import dip.misc.Utils;

import java.awt.Color;
import java.util.*;
import javax.swing.JScrollPane;


/**
*
*	Writes the Supply Center (SC) History as HTML.
*	<p>	
*	Similar to an nJudge "Status" report.
*
*/
public class SCHistoryWriter
{
	// i18n constants
	private static final String HTML_TEMPLATE 			= "SCHistoryWriter.template";
	private static final String LABEL_YEAR 				= "SCHistoryWriter.label.year";
	//private static final String LABEL_POWER 			= "SCHistoryWriter.label.power";
	//private static final String LABEL_PLAYER 			= "SCHistoryWriter.label.player";
	private static final String LABEL_INDEX 			= "SCHistoryWriter.label.index";
	private static final String DIALOG_TITLE 			= "SCHistoryWriter.dialog.title";
	private static final String TD_HEADER	 			= "<td bgcolor=\"#E6EEF0\">";
	private static final String TR_HIGHLIGHT			= "<tr bgcolor=\"#E6EEF0\">";
	private static final String LABEL_INITIAL			= "SCHistoryWriter.label.initial";
	
	
	// instance variables
	final World world;
	final Power[] allPowers;
	final Province[] scProvs;	// provinces with SCs
	final MapMetadata mmd;
	
	/**
	*	Returns the HTML-encoded Supply Center History for
	*	an entire game. If MapMetadata is ready,
	*	color will be added.
	*/
	public static String SCHistoryToHTML(ClientFrame clientFrame, World w, boolean inColor)
	{
		return new SCHistoryWriter(clientFrame, w, inColor).getAsHTML();
	}// SCHistoryToHTML()
	
	
	/**
	*	Returns the HTML-encoded Supply Center History for
	*	an entire game, inside a dialog. If MapMetadata is ready,
	*	color will be added. This uses the lazy-load dialog
	*	technique.
	*/
	public static void displayDialog(final ClientFrame clientFrame, final World w)
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
				setText(SCHistoryToHTML(clientFrame, w, true));
			}
		});
	}// displayDialog()
	
	
	/** StateWriter constructor */
	private SCHistoryWriter(ClientFrame cf, World w, boolean inColor)
	{
		this.world = w;
		this.allPowers = w.getMap().getPowers();
		
		if(inColor && cf.getMapPanel() != null)
		{
			this.mmd = cf.getMapPanel().getMapMetadata();
		}
		else
		{
			this.mmd = null;
		}
		
		// find all provinces w/supply centers
		List scList = new ArrayList();
		final Province[] provs = w.getMap().getProvinces();
		for(int i=0; i<provs.length; i++)
		{
			if(provs[i].hasSupplyCenter())
			{
				scList.add(provs[i]);
			}
		}
		
		// sort list by alphabetical order of the short name (abbreviation)
		Collections.sort(scList, new Comparator()
		{
			public int compare(Object o1, Object o2)
			{
				Province p1 = (Province) o1;
				Province p2 = (Province) o2;
				return p1.getShortName().compareTo(p2.getShortName());
			}
			
			public boolean equals(Object obj)
			{
				return false;
			}
		});
		
		this.scProvs = (Province[]) scList.toArray(new Province[scList.size()]);
	}// SCHistoryWriter()
	
	
	
	/** Write SC History as HTML */
	private String getAsHTML()
	{
		// get template
		String templateText = Utils.getText(Utils.getLocalString(HTML_TEMPLATE));
		
		// get template objects
		Object[] templateData = new Object[]
		{        
			makeSCSummary(),		// {0} : SC summary by power
			makeSCCounts()			// {1} : SC counts by power
		};
		
		// format into template
		return Utils.format(templateText, templateData);
	}// getState()
	
	
	/** Creates a table; y-axis is SC name, x-axis is year, contains power initial */
	private String makeSCSummary()
	{
		final Object[][] array = makeSummaryTable();
		final int cols = array[0].length; 	// safe: array is rectangular
		final int rows = array.length;
		
		// format array into a table.
		//
		StringBuffer sb = new StringBuffer(4096);
		sb.append("<table cellspacing=\"3\" cellpadding=\"1\" border=\"0\">");
		
		// header row
		//
		sb.append("<tr>");
		sb.append("<td></td>");	// 0,0 == empty
		for(int i=1; i<cols; i++)
		{
			sb.append(TD_HEADER);
			sb.append("<b>");
			sb.append(array[0][i]);	// usually a YearType; could be "Start", too
			sb.append("</b></td>");
		}
		sb.append("</tr>");
		
		// all other rows (by SC)
		//
		for(int r=1; r<rows; r++)
		{
			// on even rows, put a background on rows (easier to read)
			String trType = ((r & 1) == 0) ? TR_HIGHLIGHT : "<tr>";
			sb.append(trType);
			
			// col 0: special handling (province abbreviation)
			sb.append(TD_HEADER);
			sb.append("<b> ");
			sb.append( ((Province) array[r][0]).getShortName() );
			sb.append("</b></td>");
			
			// col 1..n: print 1st letter of power, or nothing if null.
			for(int c=1; c<cols; c++)
			{
				sb.append("<td>");
				Object obj = array[r][c];
				if(obj != null)
				{
					sb.append(Character.toTitleCase(obj.toString().charAt(0)));
				}
				sb.append("</td>");
			}
			
			sb.append("</tr>");
		}
		
		sb.append("</table>");
		
		return sb.toString();
	}// makeSCSummary()
	
	/** 
	*	Make the SC Summary Table.
	*	<p>
	*	Object[row][col]<br>
	*	Each Row is a SC (province)<br>
	*	Each Column is a game year<br>
	*	Col 0: SC names [province object]<br>
	*	Col 1..n: Power that owns (null if none)<br>
	*	Row 0: YearType object<br>
	*	Row 1..n: Power that owns (null if none)<br>
	*	Row,Col 0,0 is null.
	*/
	private Object[][] makeSummaryTable()
	{
		// cols: # of appropriate turns + 1 (first column is the province name)
		//
		ArrayList turnList = new ArrayList(100);	// array of TurnStates
		
		// add initial phase
		turnList.add(world.getInitialTurnState());
		
		Iterator iter = world.getAllTurnStates().iterator();
		while(iter.hasNext())
		{
			// we want the RETREAT or MOVE phase for a fall season, 
			// but not both (a unit could retreat into a SC; thus we need to check)
			//
			TurnState ts = (TurnState) iter.next();
			Phase phase = ts.getPhase();
			if(phase.getSeasonType() == Phase.SeasonType.FALL)
			{
				if(phase.getPhaseType() == Phase.PhaseType.MOVEMENT)
				{
					if(iter.hasNext())
					{
						TurnState nextTS = (TurnState) iter.next();
						if(nextTS.getPhase().getPhaseType() == Phase.PhaseType.RETREAT)
						{
							ts = nextTS;
						}
					}
					
					turnList.add(ts);
				}
			}
		}
		
		// rows: == # of SC + 1 (first row is the 'header' row)
		// make the array (rectangular)
		final int cols = turnList.size() + 1;	// easier; cols == array[0].length
		Object[][] array = new Object[scProvs.length + 1][cols];
		
		// fill the array
		// remember, (0,0) == null
		
		// col 0: provinces
		for(int i=1; i<array.length; i++)
		{
			array[i][0] = scProvs[i-1];
		}
		
		// row 0: yeartypes; HOWEVER, first 'yeartype' is really "Initial" ("Start")
		array[0][1] = Utils.getLocalString(LABEL_INITIAL);
		for(int i=2; i<cols; i++)
		{
			array[0][i] = ((TurnState) turnList.get(i-1)).getPhase().getYearType();
		}
		
		// 'the rest': fill in with power or null (un-owned)
		// we will fill by columns.
		for(int i=1; i<cols; i++)
		{
			final TurnState ts = (TurnState) turnList.get(i-1);
			final Position pos = ts.getPosition();
			
			for(int scIdx=0; scIdx<scProvs.length; scIdx++)
			{
				Power p = pos.getSupplyCenterOwner(scProvs[scIdx]);
				array[scIdx + 1][i] = p;
			}
		}
		
		return array;
	}// makeSummaryTable()
	
	
	/** 
	*	Make the SC Count table. This also has the years on the Y axis. 
	*	Power names are along the X axis.
	*/
	private String makeSCCounts()
	{
		StringBuffer sb = new StringBuffer(2048);
		sb.append("<table cellspacing=\"4\" cellpadding=\"1\" border=\"0\">");
		
		// make header
		sb.append("<tr>");
		sb.append("<td><b> ");
		sb.append(Utils.getLocalString(LABEL_YEAR));
		sb.append(" </b></td>");
		
		for(int i=0; i<allPowers.length; i++)
		{
			sb.append(TD_HEADER);
			sb.append("<b> ");
			sb.append(allPowers[i].getName());
			sb.append(" </b></td>");
		}
		
		sb.append(TD_HEADER);
		sb.append("<b>");
		sb.append(Utils.getLocalString(LABEL_INDEX));
		sb.append("</b></td>");
		
		sb.append("</tr>");
		
		// First make the Start (Initial Row).
		sb.append(makeSCCountTableRow(world.getInitialTurnState()));
		
		// make all other rows.
		Iterator iter = world.getAllTurnStates().iterator();
		while(iter.hasNext())
		{
			// we want the RETREAT or MOVE phase for a fall season, 
			// but not both.
			// (a unit could retreat into a SC; thus we need to check)
			//
			TurnState ts = (TurnState) iter.next();
			Phase phase = ts.getPhase();
			if(phase.getSeasonType() == Phase.SeasonType.FALL)
			{
				if(phase.getPhaseType() == Phase.PhaseType.MOVEMENT)
				{
					if(iter.hasNext())
					{
						TurnState nextTS = (TurnState) iter.next();
						if(nextTS.getPhase().getPhaseType() == Phase.PhaseType.RETREAT)
						{
							ts = nextTS;
						}
					}
					
					sb.append(makeSCCountTableRow(ts));
				}
			}
		}
		
		sb.append("</table>");
		return sb.toString();
	}// makeSCCounts()
	
	
	/** Make a row for the SC Summary table, including the Index. */
	private String makeSCCountTableRow(TurnState ts)
	{
		final Phase phase = ts.getPhase();
		StringBuffer sb = new StringBuffer(64);
		
		sb.append("<tr>");
		
		// year, unless initial turnstate.
		sb.append(TD_HEADER);
		sb.append("<b>");
		if(world.getInitialTurnState() == ts)
		{
			sb.append(Utils.getLocalString(LABEL_INITIAL));
		}
		else
		{
			sb.append(phase.getYearType());
		}
		sb.append("</b></td>");
		
		int sumOfSquares = 0;
		for(int i=0; i<allPowers.length; i++)
		{
			Province[] ownedSC = ts.getPosition().getOwnedSupplyCenters(allPowers[i]);
			final int count = ownedSC.length;
			
			sumOfSquares += (count * count);
			sb.append("<td>");
			
			if(count > 0)
			{
				sb.append(String.valueOf(count));
			}
			else
			{
				sb.append("&nbsp;");
			}
			
			sb.append("</td>");
		}
		
		sb.append("<td>");
		sb.append(String.valueOf(sumOfSquares / allPowers.length));
		sb.append("</td>");
		
		sb.append("</tr>");
		return sb.toString();
	}// makeSCCountTableRow()
	
	
	/** Creates a font-color */
	private String makeFontColorOpen(Power power)
	{
		if(mmd != null)
		{
			String colorName = mmd.getPowerColor(power);
			Color color = SVGColorParser.parseColor(colorName);
			StringBuffer sb = new StringBuffer(32);
			sb.append("<font color=\"");
			sb.append(Utils.colorToHTMLHex(color));
			sb.append("\">");
			return sb.toString();
		}
		return "";
	}// makeFontColorOpen()
	
/*	
		Historical Supply Center Summary
		--------------------------------
		    Ven Nap Edi Lvp Par Por Bel Mun Ber Swe Stp Mos Con Smy Rum Ser Vie
		Year  Rom Tun Lon Bre Mar Spa Hol Kie Den Nor War Sev Ank Bul Gre Bud Tri
		1900 I I I . E E E F F F . . . . G G G . . . R R R R T T T . . . . A A A
		1901 I I I I E E E F F F F F E G G G G G . E R R R R T T T T R A A A R A
		1902 I I I I E E E F F F F F F E G G G E G R R R R R T T T A R A A A A A
		1903 I I I I E E E F F F F F F F G G G G R R R R R R T T T A R I A A A A
		1904 I I I I E E F F F F F F F G G G G G R R R R R R T T T A R I A R R A
		1905 I I I I E F F F F F F F G G G G G G R R R R R R T R T T R I A R A A
		1906 I I I I E F F I F F F F G G G G R G R R R R R R T T T T R I A R A A
		1907 A I I I E F E F F I F F G R G G R G G R R R R T T T T I R T A R A A
		1908 A I I F E E E F F F F I R G R G R G R R R R R R T T T T R T A A A I
		
		
		History of Supply Center Counts
		-------------------------------
		Power    1900 '01 '02 '03 '04 '05 '06 '07 '08       Player
		Austria     3   4   6   5   3   3   3   4   4       Jacques Foury
		England     3   5   5   3   2   1   1   2   3       Chris White
		France      3   5   6   7   7   7   6   5   5       Scott Sisson
		Germany     3   5   4   4   5   6   5   5   3       jon
		Italy       3   4   4   5   5   5   6   5   4       Val�ry MENJON
		Russia      4   6   6                               John Schofield
					            7   9   9   9   8  10       Millis Miller
		Turkey      3   4   3   3   3   3   4   5   5       Greg Marrinan
		
		Index:     10  22  24  26  28  30  29  26  28
		
		
		Index is the sum of squares of the number of supply centers divided by the
		number of players.  It is a measure of how far the game has progressed.	
*/	
	
	
}// class SCHistoryWriter

