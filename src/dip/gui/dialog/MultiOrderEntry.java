//
//  @(#)MultiOrderEntry.java	1.00	4/1/2002
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
package dip.gui.dialog;

import dip.order.OrderException;
import dip.misc.Utils;
import dip.misc.Log;
import dip.gui.ClientFrame;
import dip.gui.OrderDisplayPanel;
import dip.world.Map;
import dip.world.World;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import javax.swing.JScrollPane;

/**
*
*	Modal dialog for entering multiple orders
*	Returns exceptions as a group, HTML-formatted.
*	<p>
*	This has some special feature to make parsing even easier and
*	more reliable.
*	
*/
public class MultiOrderEntry
{
	// i18n constants
	private static final String TITLE = "MOED.title";
	private static final String HEADER_TEXT_LOCATION = "MOED.header.text.location";
	
	private static final String RESULT_DIALOG_TITLE = "MOED.dlg.result.title"; 
	private static final String RESULT_DIALOG_HEADER = "MOED.dlg.result.text.result"; 
	private static final String COMMENT_PREFIX = "(*";
	
	// constants
	/**
	*	Eliminates list prefixes, such as:
	*	(where X = alphanumeric +/- surrounded by whitespace)
	*	X:	<br>
	*	X.	<br>
	*	X&gt;	<br>
	*	X)	<br>
	*	X]   <br>
	*	(X)	<br>
	*	&lt;X&gt;	<br>
	*	[X]	<br>
	*	and all of above may have a "." or ":" after them as well
	*	e.g.: X): or X). or 1).
	*
	*/
	private static final String LIST_REGEX = "^\\s*[<\\(\\[]?\\s*\\p{Alnum}*\\s*[>\\)\\]\\.\\:][\\.\\:]?\\s*";
	
	
	// NOTE: this is sort of a hack. 
	private static final String[] BAD_TOKS = 
	{
		"army","fleet","wing","a","f","w",
		"s", "support", "supports", "sup", "suppor", "supp", "sprt", "supprt", "spprt", "supporting", "supportng",
		"convoy", "con", "conv", "convy", "c", "convoying", "convying"
	};
	
	// instance variables
	private ClientFrame parent;
	private TextViewer tv;
	private OrderDisplayPanel orderDisplayPanel;
	private World world;
	private Pattern listPattern = null;
	
	
	/** Display the MultiOrderEntry dialog */
	public static void displayDialog(ClientFrame parent, World world)
	{
		MultiOrderEntry moe = new MultiOrderEntry(parent, world);
		moe.tv.displayDialog();
	}// displayDialog()
	
	
	
	private MultiOrderEntry(ClientFrame parent, World world)
	{
		this.parent = parent;
		this.world = world;
		this.orderDisplayPanel = parent.getOrderDisplayPanel();
		
		tv = new TextViewer(parent, true);
		tv.setEditable(true);
		tv.setHeaderText( Utils.getText(Utils.getLocalString(HEADER_TEXT_LOCATION)) );
		tv.setTitle(Utils.getLocalString(TITLE));
		tv.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		tv.addTwoButtons( tv.makeCancelButton(), tv.makeAcceptButton(), false, true );
		tv.setAcceptListener(new Acceptor());
		tv.setText("");
		
		tv.setHelpID(dip.misc.Help.HelpID.Dialog_MultiOrder);
	}// MultiOrderEntry()
	
	
	
	private class Acceptor implements TextViewer.AcceptListener
	{
		public boolean isAcceptable(TextViewer t)
		{
			String text = t.getText();
			text = text.trim();
			if(!text.equals(""))
			{
				return process(t.getText());
			}
			return true;
		}// isAcceptable()
		
		public boolean getCloseDialogAfterUnacceptable()
		{
			return true;
		}// getCloseDialogAfterUnacceptable()
	}// nested class Acceptor
	
	
	
	// process line-by-line
	// keep exceptions
	// keep tally of total orders / invalid / valid
	private boolean process(String text)
	{
		int nOrders = 0;
		List exList = new ArrayList();
		List failList = new ArrayList();
		
		try
		{
			BufferedReader br = new BufferedReader(new StringReader(text));
			String line = br.readLine();
			while(line != null)
			{
				try
				{
					// first trim
					line = line.trim();
					
					// now check length, after trimming
					// (otherwise, lines with just whitespace will be interpreted as an order)
					if(line.length() > 0)
					{
						nOrders++;
						
						// trim anything after (and including) a "(*" for cut-and-pastes
						// from judge output
						int idx = line.indexOf(COMMENT_PREFIX);
						if(idx > COMMENT_PREFIX.length())
						{
							line = line.substring(0, idx);
						}
						
						parseOrder(line);
					}
				}
				catch(OrderException oe)
				{
					exList.add(oe.getMessage());
					failList.add(line);
				}
				
				line = br.readLine();
			}
			br.close();
		}
		catch(IOException ioe)
		{
			throw new IllegalStateException("BufferedReader: internal error?");
		}
		
		int nFailed = exList.size();
		if(nFailed > 0)
		{
			String headerText = Utils.getLocalString(RESULT_DIALOG_HEADER,
											new Integer(nOrders),
											new Integer(nFailed),
											new Integer(nOrders - nFailed));
			
			// order text, formatted with CSS
			StringBuffer sb = new StringBuffer(4096);
			
			for(int i=0; i<exList.size(); i++)
			{
				// failed order
				sb.append("<div class=\"ind1top05big\"><u>");
				sb.append( failList.get(i) );
				sb.append("</u></div>");
				
				// description
				sb.append("<div class=\"indent2cmbig\">");
				sb.append( exList.get(i) );
				sb.append("</div>");
				
			}
			
			// the above text, added to the template
			String templateText = Utils.getText(Utils.getLocalString("MOED.dlg.template"));
			String dialogText = Utils.format(templateText,
								new Object[] {headerText, sb.toString()} );
			
			TextViewer rv = new TextViewer(parent, true);
			rv.setEditable(false);
			rv.setTitle(Utils.getLocalString(RESULT_DIALOG_TITLE));
			rv.addSingleButton( rv.makeOKButton() );
			rv.setContentType("text/html");
			rv.setText(dialogText);
			rv.setHeaderVisible(false);
			rv.displayDialog();
			
			return false;
		}
		
		return true;
	}// process()
	
	
	/**
	*	First, clean up the order by applying the List Eliminator
	*	regex pattern. If parsing fails after this, attempt parsing
	*	with the Reducing Recursive Token Elimination Parser.
	*
	*
	*/
	private void parseOrder(String input)
	throws OrderException
	{
		Log.println("MOE::parseOrder() applying list prefix eliminator on: ", input);
		
		if(listPattern == null)
		{
			listPattern = Pattern.compile(LIST_REGEX);
		}
		
		Matcher m = listPattern.matcher(input);
		
		// we only want ONE match
		if(m.lookingAt())	// find FIRST match
		{
			input = input.substring( m.end() );
		}
		
		Log.println("MOE::parseOrder(): after list prefix eliminator: ", input);
		Log.println("MOE::parseOrder(): now applying recursive elimination parser...");
		
		recursiveParse(input);
	}// parseOrder()
	
	/**
	*	Reducing Recursive Token Elimination Parser
	*	<p>
	*	If an order fails parsing, tokenize the order
	*	and successively remove (from the beginning) 
	*	tokens and attempt parsing, until it parses.
	*	<p>
	*	If the order parses, it is added automatically
	*	to the order list. If it does not, the first 
	*	exception generated is thrown.
	*	<p>
	*	Rational: we can cut/paste from email and the
	*	beginning content (e.g.: "1> gas-par" the "1>"
	*	would be the first token eliminated) will be
	*	ignored.
	*	<p>
	*	We 'limit' until the first recognized unit
	*	type name or province name is detected in a token.
	*	This helps prevent bad support or convoy orders
	*	from being over-parsed into move orders.
	*
	*/
	private void recursiveParse(String input)
	throws OrderException
	{
		Log.println("MOE::recursiveParse(): ", input);
		OrderException firstException = null;
		
		// first pass
		try
		{
			orderDisplayPanel.addOrderRaw(input, true);
			Log.println("  MOE::recursiveParse(): success on first pass.");
			return;
		}
		catch(OrderException oe)
		{
			Log.println("  MOE::recursiveParse(): first pass failed.");
			firstException = oe;
		}
		
		// tokenize
		final String[] tokens = toTokens(input.toLowerCase());
		
		// test
		for(int i=0; i<tokens.length; i++)
		{
			if(isRecognized(tokens[i]))
			{
				Log.println("  MOE::recursiveParse(): abort; token is recognized: ", tokens[i]);
				break;
			}
			
			String text = fromTokens(tokens, i, tokens.length);
			Log.println("  MOE::recursiveParse(): now trying: \"", text, "\"");
			
			try
			{
				orderDisplayPanel.addOrderRaw(text, true);
				return;
			}
			catch(OrderException oe)
			{
				Log.println("  MOE::recursiveParse(): try failed.");
				// do nothing. 
			}
			
			// if we failed, and this current token already is a 
			// definate known province, we shouldn't process further.
			if(world.getMap().getProvince(tokens[i]) != null)
			{
				Log.println("  MOE::recursiveParse(): abort; known province recognized: ", tokens[i]);
				break;
			}
		}
		
		throw firstException;
	}// recursiveParse()
	
	
	/** Converts input to token array */
	private String[] toTokens(String input)
	{
		ArrayList list = new ArrayList(10);
		StringTokenizer st = new StringTokenizer(input);
		while(st.hasMoreTokens())
		{
			list.add( st.nextToken() );
		}
		
		return (String[]) list.toArray(new String[list.size()]);
	}// toTokens()
	
	
	/** 
	*	Converts input array to space-separated String,
	*	using the given starting and ending indices
	*	Start is inclusive; End is exclusive
	*/
	private String fromTokens(String[] tokens, int start, int end)
	{
		if(start > end || start < 0 || end > tokens.length)
		{
			throw new IllegalArgumentException();
		}
		
		StringBuffer sb = new StringBuffer(256);
		
		for(int i=start; i<end; i++)
		{
			sb.append(tokens[i]);
			sb.append(' ');
		}
		
		return sb.toString();
	}// fromTokens()
	
	/**
	*	See if we recognize a Token
	*	as a:
	*		a) Power
	*		b) Unit
	*/
	private boolean isRecognized(String tok)
	{
		Map map = world.getMap();
		
		// check against known powers
		if(map.getPower(tok) != null)
		{
			return true;
		}
		
		// check against against other bad tokens
		for(int i=0; i<BAD_TOKS.length; i++)
		{
			if(tok.equals(BAD_TOKS[i]))
			{
				return true;
			}
		}
		
		return false;
	}// isRecognized()
	
	
	
}// class MultiOrderEntry()

