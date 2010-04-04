//
//  @(#)TestParser.java		3/2003
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
package dip.misc;

import dip.order.*;
import dip.order.result.*;

import dip.world.*;
import dip.world.Phase.*;
import dip.misc.*;
import dip.world.variant.VariantManager;
import dip.world.variant.data.*;
import dip.process.*;

import java.util.*;
import java.io.*;
import java.lang.reflect.*;
import jcmdline.*;	

/**
*	A test harness that allows testing of the Order Parser (OrderParser.java). 
*	<p>
*	<pre>
*		VARIANT: xxxxxxx
*		SETUP
*		ENDSETUP
*		ORD:
*		RES (or RESULT):
*
*	Remove(Power power, Location src, Unit.Type srcUnit)
*	Build(Power power, Location src, Unit.Type srcUnit)
*	
*	Disband(Power power, Location src, Unit.Type srcUnit)
*	Retreat(Power power, Location src, Unit.Type srcUnitType, Location dest)
*	
*	Hold(Power power, Location src, Unit.Type srcUnit)
*	
*	Move(Power power, Location src, Unit.Type srcUnitType, Location dest, boolean isConvoying)
*	
*	Convoy(Power power, Location src, Unit.Type srcUnit, Location convoySrc, Unit.Type convoyUnitType, Location convoyDest)
*	
*	Support(Power power, Location src, Unit.Type srcUnit, Location supSrc, Unit.Type supUnit, Location supDest)
*
*	Waive(Power power, Location src)
*
*	</pre>
*/
public class TestParser
{
	// instance fields
	String	variantName;
	World world = null;
	TurnState turnState = null;	// the first & only TurnState in the World object
	dip.world.Map map = null;
	List cases = null;			// a List of ORPairs
	OrderParser op = null;
	boolean isLogging = false;	// OrderParser internal logging enabled
	ValidationOptions valOpts = null;	// validation options
	
	// constants
	private static final String VARIANT_DIR				= "variants";
	
	private static final String KEY_VARIANT 	= "variant";
	private static final String KEY_SETUP 		= "setup";
	private static final String KEY_SETUPDISLODGED	= "setupdislodged";
	private static final String KEY_END 		= "end";
	private static final String KEY_ORDER	 	= "order";
	private static final String KEY_RESULT	 	= "result";
	private static final String KEY_COMMENT	 	= "comment";
	
	// note: order is important; e.g., 'setupdislodged' before 'setup'
	private static final String KEYWORDS[][] = 
	{
		{"variant", KEY_VARIANT},
		{"setupdislodged", KEY_SETUPDISLODGED},
		{"setup", KEY_SETUP},
		{"end", KEY_END},
		{"order", KEY_ORDER},
		{"ord", KEY_ORDER},
		{"result", KEY_RESULT},
		{"res", KEY_RESULT},
		{"#", KEY_COMMENT}
	};
	
	// argument type specifiers
	private static final String T_POWER = "power";
	private static final String T_LOCATION = "location";
	private static final String T_UTYPE = "unittype";
	private static final String T_BOOLEAN = "boolean";
	
	// order types and their acceptable arguments.
	private static final String[][] ORDER_ARGS = 
	{
		// really simple types (2 args)
		{"waive", T_POWER, T_LOCATION},
		// simple types (3 args)
		{"hold", T_POWER, T_LOCATION, T_UTYPE},
		{"disband", T_POWER, T_LOCATION, T_UTYPE},
		{"build", T_POWER, T_LOCATION, T_UTYPE},
		{"remove", T_POWER, T_LOCATION, T_UTYPE},
		// complex types
		{"move", T_POWER, T_LOCATION, T_UTYPE, T_LOCATION, T_BOOLEAN},
		{"retreat", T_POWER, T_LOCATION, T_UTYPE, T_LOCATION},
		{"convoy", T_POWER, T_LOCATION, T_UTYPE, T_LOCATION, T_UTYPE, T_LOCATION},
		{"support", T_POWER, T_LOCATION, T_UTYPE, T_LOCATION, T_UTYPE, T_LOCATION},
	};
	
	// 3 main phase types for proper validation
	private static final Phase PHASE_MOVE		= new Phase(SeasonType.FALL, 1900, PhaseType.MOVEMENT);
	private static final Phase PHASE_ADJUSTMENT	= new Phase(SeasonType.FALL, 1900, PhaseType.ADJUSTMENT);
	private static final Phase PHASE_RETREAT	= new Phase(SeasonType.FALL, 1900, PhaseType.RETREAT);
	
	
	/** Start the parser. */
	public static void main(String args[])
	{
		// input file specifier
		FileParam argInputFile = 
			new FileParam("input", "the input file of test-case definitions", 
							FileParam.IS_FILE & FileParam.IS_READABLE & FileParam.EXISTS,
							FileParam.REQUIRED,
							FileParam.SINGLE_VALUED);
		
		// are we logging? or not.
		BooleanParam logOpt =
			new BooleanParam("log", "log OrderParser internal processing");
		
		// verbose help text
		String helpText	=	" ";	
		
		// main command line handler
		CmdLineHandler cl = new VersionCmdLineHandler(
			"TestParser 1.0",
			new HelpCmdLineHandler(helpText,
				"TestParser",
				"Test harness for testing order parsing",
				// options
				new Parameter[] {argInputFile, logOpt},
				// arguments [left on command line]
				new Parameter[] {}
			)
		);
		
		// parse command line
		cl.parse(args);
		
		// Start the parser
		new TestParser(argInputFile.getFile(), logOpt.isTrue());
	}// main()
	
	
	/** Creates a TestParser using the given input file. */
	private TestParser(File input, boolean isLogging)
	{
		this.isLogging = isLogging;
		System.out.println("TestParser started on: "+new Date());
		//Log.setLogging(isLogging);
		parseCaseFile(input);
		Log.setLogging(isLogging);
		runTest();
	}// TestParser()
	
	
	/** Test each case and keep stats */
	private void runTest()
	{
		// list of failed case descriptions.
		int numprocessed = 0;
		List failedCases = new LinkedList();
		
		
		// set the validation options; lenient -- we only care about syntax!
		valOpts = new ValidationOptions();
		valOpts.setOption(ValidationOptions.KEY_GLOBAL_PARSING, ValidationOptions.VALUE_GLOBAL_PARSING_LOOSE);
		
		Iterator iter = cases.iterator();
		while(iter.hasNext())
		{
			ORPair orp = (ORPair) iter.next();
			
			// determine if case is marked with a "FAIL" result line.
			boolean isMarkedFail = false;
			String res = orp.getResult().trim().toLowerCase();
			if(res.startsWith("fail"))
			{
				isMarkedFail = true;
			}
			
			
			// First, attempt to parse the order. Guessing is allowed.
			try
			{
				numprocessed++;
				Order o = op.parse(OrderFactory.getDefault(), orp.getOrder(), null, turnState, false, true);
				
				// validate order: note that we need to set the phase appropriately
				// first
				if(o instanceof Build || o instanceof Remove)
				{
					turnState.setPhase(PHASE_ADJUSTMENT);
				}
				else if(o instanceof Retreat || o instanceof Disband)
				{
					turnState.setPhase(PHASE_RETREAT);
				}
				else
				{
					turnState.setPhase(PHASE_MOVE);
				}
				
				// now, do the validation
				o.validate(turnState, valOpts, world.getRuleOptions());
				
				// if marked as fail, and we succeed, it's a failure!
				if(isMarkedFail)
				{
					StringBuffer sb = new StringBuffer(128);
					sb.append("Order line ");
					sb.append(String.valueOf(orp.getLineNumber()));
					sb.append("\"");
					sb.append(orp.getOrder());
					sb.append("\"");
					sb.append(" succeeded, but should have failed.");
					failedCases.add(sb.toString());
				}
				else
				{
					// check order normally
					checkORP(orp, o, failedCases);
				}
			}
			catch(OrderException e)
			{
				// only count as a failure if RESULT line does NOT have a "FAIL" result.
				if(!isMarkedFail)
				{
					StringBuffer sb = new StringBuffer(128);
					sb.append("Order line ");
					sb.append(String.valueOf(orp.getLineNumber()));
					sb.append(" \"");
					sb.append(orp.getOrder());
					sb.append("\"");
					sb.append(" failed: ");
					sb.append(e);
					failedCases.add(sb.toString());
				}
			}
		}
		
		// print stats
		System.out.println(numprocessed+" order(s) parsed");
		System.out.println(failedCases.size()+" order(s) failed");
		
		if(failedCases.size() > 0)
		{
			System.out.println("\nFailed orders, and failure reasons follow:");
			
			iter = failedCases.iterator();
			while(iter.hasNext())
			{
				System.out.println(iter.next());
				System.out.println("");
			}
		}
		
		System.out.println("TestParser completed on "+new Date());
		
		// clean or error exit
		if(failedCases.size() > 0)
		{
			System.exit(1);
		}
		else
		{
			System.exit(0);
		}
	}// runTest()
	
	
	/** 
	*	
	*	Check the order against what actually should be there. 
	*	Originally, reflection was to be used but this is much easier, 
	*	though less flexible.
	*/
	private void checkORP(ORPair orp, Order o, List failedCases)
	{
		String[] toks = getORPTokens(orp.getResult());
		if(toks.length == 0)
		{
			System.out.println("ERROR: in result of order pair starting at line: "+orp.getLineNumber());
			System.out.println("No result type was given.");
			System.exit(1);
		}
		
		// find order.
		String[] params = null;
		for(int i=0; i<ORDER_ARGS.length; i++)
		{
			if(toks[0].equalsIgnoreCase(ORDER_ARGS[i][0]))
			{
				if(toks.length != ORDER_ARGS[i].length)
				{
					System.out.println("ERROR: in result of order pair starting at line: "+orp.getLineNumber());
					System.out.println("Invalid number of arguments; "+(ORDER_ARGS[i].length-1)+" are required.");
					System.exit(1);
				}
				else
				{
					params = ORDER_ARGS[i];
				}
			}
		}
		
		// order not found.
		if(params == null)
		{
			System.out.println("ERROR: in result of order pair starting at line: "+orp.getLineNumber());
			System.out.println("Order type \""+toks[0]+"\" not found.");
			System.exit(1);
		}
		
		// name
		String name = toks[0].toLowerCase();
		
		// validate name
		if(!name.equalsIgnoreCase(o.getFullName()))
		{
			StringBuffer sb = new StringBuffer(128);
			sb.append("Order line ");
			sb.append(String.valueOf(orp.getLineNumber()));
			sb.append(" \"");
			sb.append(orp.getOrder());
			sb.append("\"");
			sb.append(" failed; a ");
			sb.append(o.getFullName());
			sb.append(" was parsed but a ");
			sb.append(name);
			sb.append(" order was expected.");
			failedCases.add(sb.toString());
			return;
		}
		
		// exception: Waive has 2 parameters; valBasicParam expects 3. 
		if(name.equals("waive"))
		{
			// validate power, and validate location.
			if(!valPower(orp, o.getPower(), toks[1], failedCases))
			{
				return;
			}
			
			if(!valLocation(orp, o.getSource(), toks[2], failedCases))
			{
			}
			
			// we are OK
			return;
		}
		
		// validate basic params -- same for all orders
		if(!valBasicParams(orp, o, toks, failedCases))
		{
			return;
		}
		
		// validate extended parameters
		if(name.equals("move"))
		{
			if(!valLocation(orp, (((Move) o).getDest()), toks[4], failedCases))
			{
				return;
			}
			
			if(!valBoolean(orp, (((Move) o).isConvoying()), toks[5], failedCases))
			{
				return;
			}
		}
		else if(name.equals("retreat"))
		{
			if(!valLocation(orp, (((Retreat) o).getDest()), toks[4], failedCases))
			{
				return;
			}
		}
		else if(name.equals("support"))
		{
			if(!valLocation(orp, (((Support) o).getSupportedSrc()), toks[4], failedCases))
			{
				return;
			}
			
			if(!valUnitType(orp, (((Support) o).getSupportedUnitType()), toks[5], failedCases))
			{
				return;
			}
			
			if(!valLocation(orp, (((Support) o).getSupportedDest()), toks[6], failedCases))
			{
				return;
			}
		}
		else if(name.equals("convoy"))
		{
			if(!valLocation(orp, (((Convoy) o).getConvoySrc()), toks[4], failedCases))
			{
				return;
			}
			
			if(!valUnitType(orp, (((Convoy) o).getConvoyUnitType()), toks[5], failedCases))
			{
				return;
			}
			
			if(!valLocation(orp, (((Convoy) o).getConvoyDest()), toks[6], failedCases))
			{
				return;
			}
		}
	}// checkORP()
	
	
	/** Validate basic params -- for all orders; always 3 params */
	private boolean valBasicParams(ORPair orp, Order o, String[] toks, List failedCases)
	{
		boolean isOK = valPower(orp, o.getPower(), toks[1], failedCases);
		
		if(isOK)
		{
			isOK = valLocation(orp, o.getSource(), toks[2], failedCases);
		}
		
		if(isOK)
		{
			isOK = valUnitType(orp, o.getSourceUnitType(), toks[3], failedCases);
		}
		
		return isOK;
	}// valBasicParams()
	
	
	
	
	/** Validate a Power */
	private boolean valPower(ORPair orp, Power thePower, String tok, List failedCases)
	{
		// is tok a valid Power name? if not, error-exit
		Power power = map.getPower(tok);
		if(power == null)
		{
			System.out.println("ERROR: in result of order pair starting at line: "+orp.getLineNumber());
			System.out.println("Power \""+tok+"\" not found.");
			System.exit(1);
		}
		
		// does tok match? if not, add to failed cases, return false
		if(power != thePower)
		{
			StringBuffer sb = new StringBuffer(128);
			sb.append("Order line ");
			sb.append(String.valueOf(orp.getLineNumber()));
			sb.append(" \"");
			sb.append(orp.getOrder());
			sb.append("\"");
			sb.append(" failed; the powers do not match. ");
			failedCases.add(sb.toString());
			return false;
		}
		
		return true;
	}// valPower()
	
	
	/** Validate a Location */
	private boolean valLocation(ORPair orp, Location theLoc, String tok, List failedCases)
	{
		// is tok a valid Power name? if not, error-exit
		Location loc = map.parseLocation(tok);
		if(loc == null)
		{
			System.out.println("ERROR: in result of order pair starting at line: "+orp.getLineNumber());
			System.out.println("Location \""+tok+"\" not found.");
			System.exit(1);
		}
		
		// does tok match? if not, add to failed cases, return false
		// cannot use identity-equals here
		if(!loc.equals(theLoc))
		{
			StringBuffer sb = new StringBuffer(128);
			sb.append("Order line ");
			sb.append(String.valueOf(orp.getLineNumber()));
			sb.append(" \"");
			sb.append(orp.getOrder());
			sb.append("\"");
			sb.append(" failed; the location ");
			sb.append("\"");
			sb.append(loc);
			sb.append("\"");
			sb.append(" does not match ");
			sb.append("\"");
			sb.append(theLoc);
			sb.append("\"");
			failedCases.add(sb.toString());
			return false;
		}
		
		return true;
	}// valPower()
	
	/** Validate a Unit Type */
	private boolean valUnitType(ORPair orp, Unit.Type theUnitType, String tok, List failedCases)
	{
		// is tok a valid Power name? if not, error-exit
		Unit.Type ut = Unit.Type.parse(tok);
		if(ut == null || ut == Unit.Type.UNDEFINED)
		{
			System.out.println("ERROR: in result of order pair starting at line: "+orp.getLineNumber());
			System.out.println("Unit Type \""+tok+"\" was not found.");
			System.exit(1);
		}
		
		// does tok match? if not, add to failed cases, return false
		if(ut != theUnitType)
		{
			StringBuffer sb = new StringBuffer(128);
			sb.append("Order line ");
			sb.append(String.valueOf(orp.getLineNumber()));
			sb.append(" \"");
			sb.append(orp.getOrder());
			sb.append("\"");
			sb.append(" failed; the Unit Type ");
			sb.append("\"");
			sb.append(ut);
			sb.append("\"");
			sb.append(" does not match ");
			sb.append("\"");
			sb.append(theUnitType);
			sb.append("\"");
			failedCases.add(sb.toString());
			return false;
		}
		
		return true;
	}// valPower()
	
	/** Validate a Boolean */
	private boolean valBoolean(ORPair orp, boolean theBoolean, String tok, List failedCases)
	{
		// is tok a valid Power name? if not, error-exit
		boolean bool = false;
		if(tok.equalsIgnoreCase("true"))
		{
			bool = true;
		}
		else if(tok.equalsIgnoreCase("false"))
		{
			bool = false;
		}
		else
		{
			System.out.println("ERROR: in result of order pair starting at line: "+orp.getLineNumber());
			System.out.println("Boolean value \""+tok+"\" must be \"true\" or \"false\".");
			System.exit(1);
		}
		
		// does tok match? if not, add to failed cases, return false
		if(bool != theBoolean)
		{
			StringBuffer sb = new StringBuffer(128);
			sb.append("Order line ");
			sb.append(String.valueOf(orp.getLineNumber()));
			sb.append(" \"");
			sb.append(orp.getOrder());
			sb.append("\"");
			sb.append(" failed; the value ");
			sb.append("\"");
			sb.append(bool);
			sb.append("\"");
			sb.append(" does not match ");
			sb.append("\"");
			sb.append(theBoolean);
			sb.append("\"");
			failedCases.add(sb.toString());
			return false;
		}
		
		return true;
	}// valPower()	
	
	
	
	
	
	/** Get tokens from an order result (non-failure) as a string array */
	private String[] getORPTokens(String in)
	{
		ArrayList al = new ArrayList(10);
		
		// parse result.
		// format is like xxxx(a, b, c, d)
		StringTokenizer st = new StringTokenizer(in, "(),;");
		while(st.hasMoreTokens())
		{
			al.add(st.nextToken().trim());
		}
		
		return (String[]) al.toArray(new String[al.size()]);
	}// getORPTokens()
	

	
	
	
	
	
	/** Setup the variant, using variantName, which shouldn't be null */
	private void setupVariant()
	{
		assert(variantName != null);
		
		// get default variant directory. 
		File defaultVariantSearchDir = null;
		if(System.getProperty("user.dir") == null)
		{
			defaultVariantSearchDir = new File(".", VARIANT_DIR);
		}
		else
		{
			defaultVariantSearchDir = new File(System.getProperty("user.dir"), VARIANT_DIR );
		}
		
		try
		{
			// parse variants
			VariantManager.init(new File[]{defaultVariantSearchDir}, false);
			
			// load the default variant (Standard)
			// error if it cannot be found!!
			Variant variant = VariantManager.getVariant(variantName, VariantManager.VERSION_NEWEST);
			if(variant == null)
			{
				System.out.println("ERROR: cannot find variant: "+variantName);
				System.exit(1);
			}
			
			// create the world
			world = WorldFactory.getInstance().createWorld(variant);
			turnState = world.getLastTurnState();
			map = world.getMap();
			
			// set the RuleOptions in the World (this is normally done
			// by the GUI)
			world.setRuleOptions(RuleOptions.createFromVariant(variant));
		}
		catch(Exception e)
		{
			System.out.println("ERROR: could not create variant.");
			System.out.println(e);
			e.printStackTrace();
			System.exit(1);
		}
		
		// clear positions in this world
		Position pos = turnState.getPosition();
		Province[] provs = pos.getProvinces();
		for(int i=0; i<provs.length; i++)
		{
			pos.setUnit(provs[i], null);
			pos.setDislodgedUnit(provs[i], null);
		}
		
		System.out.println("Variant \""+variantName+"\" loaded successfully.");
		
		// setup the order parser
		op = OrderParser.getInstance();
		System.out.println("OrderParser created.");
	}// setupVariant()
	
	
	/**
	*	A bunch of DefineState orders used to set unit positions
	*	for subsequent order processing.
	*/
	private void setupPositions(List nonDislodged, List dislodged)
	{
		assert(nonDislodged != null);
		assert(dislodged != null);
		assert(turnState != null);
		
		Position pos = turnState.getPosition();
		
		int count = 0;
		Iterator iter = nonDislodged.iterator();
		while(iter.hasNext())
		{
			String line = (String) iter.next();
			DefineState ds = parseDSOrder(line.trim());
			Unit unit = new Unit(ds.getPower(), ds.getSourceUnitType());
			unit.setCoast(ds.getSource().getCoast());
			pos.setUnit(ds.getSource().getProvince(), unit);
			count++;
		}
		System.out.println(count+" (non-dislodged) unit positions set.");
		
		
		count = 0;
		iter = dislodged.iterator();
		while(iter.hasNext())
		{
			String line = (String) iter.next();
			DefineState ds = parseDSOrder(line.trim());
			Unit unit = new Unit(ds.getPower(), ds.getSourceUnitType());
			unit.setCoast(ds.getSource().getCoast());
			pos.setDislodgedUnit(ds.getSource().getProvince(), unit);
			count++;
		}
		System.out.println(count+" (dislodged) unit positions set.");
	}// setupPositions()
	
	
	/** Parse the Case file. */
	private void parseCaseFile(File caseFile)
	{
		LineNumberReader lnr = null;
		
		try
		{
			boolean setupDone = false;
			List accum = null;
			cases = new ArrayList(200);
			ORPair currentCase = null;
			List posList = null;
			List dislodgedPosList = null;
			
			lnr = new LineNumberReader(new BufferedReader(new FileReader(caseFile)));
			String line = lnr.readLine();
			while(line != null)
			{
				// trim line
				line = line.trim();
				
				// cutoff any text after first '#'
				final int cidx = line.indexOf('#');
				if(cidx != -1)
				{
					line = line.substring(0, cidx).trim();
				}
				
				// allow blank/empty/only-whitespace lines (but don't parse them)
				if(line.length() > 0)
				{
					// cut off any text after the first '#'
					
					
					String key = getKeyword(line);
					if(key == KEY_VARIANT)
					{
						if(variantName != null)
						{
							lnrErrorExit(lnr, "VARIANT already defined.");
						}
						
						variantName = getPostKeywordText(line).trim();
						setupVariant();
					}
					else if(key == KEY_SETUP)
					{
						if(posList != null)
						{
							lnrErrorExit(lnr, "SETUP block already defined.");
						}
						
						accum = new ArrayList(50);
					}
					else if(key == KEY_END)
					{
						if(accum == null)
						{
							lnrErrorExit(lnr, "END line must be after a SETUP line or SETUPDISLODGED line.");
						}
						
						// send accumulated line to setup parser
						if(posList == null)
						{
							posList = accum;
						}
						else
						{
							dislodgedPosList = accum;
						}
						
						// if we are done, mark the setupDone flag
						// and setup the positions.
						if(posList != null && dislodgedPosList != null)
						{
							setupPositions(posList, dislodgedPosList);
							setupDone = true;
						}
					}
					else if(key == KEY_SETUPDISLODGED)
					{
						if(posList == null)
						{
							lnrErrorExit(lnr, "SETUPDISLODGED must be after a SETUP block.");
						}
						
						if(dislodgedPosList != null)
						{
							lnrErrorExit(lnr, "SETUPDISLODGED block already defined.");
						}
						
						accum = new ArrayList(50);
					}
					else if(key == KEY_ORDER)
					{
						if(!setupDone)
						{
							lnrErrorExit(lnr, "ORDER (or ORD) keyword must be after SETUP block complete.");
						}
						
						if(currentCase != null)
						{
							lnrErrorExit(lnr, "ORDER (or ORD) keyword must precede and be paired with a RESULT (or RES) line.");
						}
						
						currentCase = new ORPair();
						currentCase.setOrder( getPostKeywordText(line) );
						currentCase.setLineNumber( lnr.getLineNumber() );
					}
					else if(key == KEY_RESULT)
					{
						if(!setupDone)
						{
							lnrErrorExit(lnr, "RESULT (or RES) keyword must be after SETUP block complete.");
						}
						
						if(currentCase == null)
						{
							lnrErrorExit(lnr, "RESULT (or RES) line must follow and be paired with an ORDER (or ORD) line.");
						}
						
						currentCase.setResult( getPostKeywordText(line) );
						cases.add(currentCase);
						currentCase = null;
					}
					else if(key == KEY_COMMENT)
					{
						// do nothing
					}
					else
					{
						// add lines to accumulator
						if(accum != null)
						{
							accum.add(line);
						}
						else
						{
							// no accumulator? we don't want no non-blank lines with werdz
							System.out.println("ERROR: line: "+lnr.getLineNumber());
							System.out.println("Unknown action or non-comment line.");
							System.exit(1);
						}
					}
				}
				
				line = lnr.readLine();
			}
		}
		catch(IOException e)
		{
			System.out.println("ERROR reading the case file \""+caseFile+"\".");
			System.out.println(e);
			System.exit(1);
		}
		finally
		{
			if(lnr != null)
			{
				try { lnr.close(); } catch(Exception e) {}
			}
		}
		
		System.out.println("Case file succesfully parsed.");
		System.out.println(cases.size()+" order/result pairs read.");
	}// parseCaseFile()
	
	
	
	/** 
	*	Gets if lien starts with keyword (case-insensitive); if not, returns	
	*	null; if so, returns normalized keyword
	*
	*/
	private String getKeyword(String line)
	{
		String lcLine = line.trim().toLowerCase();
		for(int i=0; i<KEYWORDS.length; i++)
		{
			if(lcLine.startsWith(KEYWORDS[i][0]))
			{
				return KEYWORDS[i][1];
			}
		}
		
		return null;
	}// getKeyword()
	
	
	/** Gets text after a keyword, ignoring comments, "" if no text found */
	private static String getPostKeywordText(String line)
	{
		line = stripComment(line);
		int idx = getWSIndex(line);
		if(idx == -1 || idx+1 > line.length())
		{
			return "";
		}
		
		return line.substring(idx+1);
	}// getPostKeywordText()
	
	
	/** Strips the comment after a line (starts with #) if present. */
	private static String stripComment(String line)
	{
		int idx = line.indexOf('#');
		if(idx >= 0)
		{
			return line.substring(0, idx);
		}
		
		return line;
	}// stripComment()
	
	
	/** 
	*	Gets first index of a whitespace character, and returns it's 
	* 	index of -1 if not found 
	*/
	private static int getWSIndex(String in)
	{
		for(int i=0; i<in.length(); i++)
		{
			if( Character.isWhitespace(in.charAt(i)) )
			{
				return i;
			}
		}
		
		return -1;
	}// getWSIndex()
	
	
	private void lnrErrorExit(LineNumberReader lnr, String msg)
	{
		System.out.println("ERROR: line: "+lnr.getLineNumber());
		System.out.println("SETUP block already defined.");
		System.exit(1);
	}// lnrErrorExit()
	
	
	/** 
	*	Parse order as a DefineState order, and if it fails, 
	*	error exit.
	*/
	private DefineState parseDSOrder(String line)
	{
		// parse each line as a DefineState order.
		// add each unit to the turnState position.
		try
		{
			// no guessing (but not locked); we must ALWAYS specify the power.
			Order o = op.parse(OrderFactory.getDefault(), line, null, turnState, false, false);
			
			if(o instanceof DefineState)
			{
				// we just want to check if the DefineState order does not have
				// an undefined coast for a fleet unit.
				Location newLoc = o.getSource().getValidatedSetup(o.getSourceUnitType());
				
				// create a new DefineState with a validated loc
				return OrderFactory.getDefault().createDefineState(o.getPower(), 
					newLoc, o.getSourceUnitType());
			}
			else
			{
				throw new OrderException("A DefineState order (e.g., \"England: A Lon\" is required.");
			}
		}
		catch(OrderException e)
		{
			System.out.println("ERROR in SETUP or SETUPDISLODGED position.");
			System.out.println("      line: "+line);
			System.out.println("parseOrder() OrderException: "+e);
			System.exit(1);
		}	
		
		// shouldn't occur.
		assert(false);
		return null;
	}// parseDSOrder()
	
	
	/** 
	*	Order-Result pair class
	*	Pairs an Order with its Result
	*	
	*/
	private class ORPair
	{
		private String order;
		private String result;
		private int line;
		
		public ORPair() {}
		
		public String getOrder()	{ return order; }
		public String getResult()	{ return result; }
		public int getLineNumber()	{ return line; }
		public void setOrder(String s) 	{ order = s; }
		public void setResult(String s) { result = s; }
		public void setLineNumber(int i) { line = i; }
	}
	
	
	
}// class TestParser




