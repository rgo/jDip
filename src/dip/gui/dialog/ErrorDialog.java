//
//  @(#)ErrorDialog.java			4/2002
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

import dip.misc.Utils;
import dip.misc.Log;
import dip.world.World;
import dip.gui.*;

import java.io.InvalidClassException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.BufferedReader;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.net.URL;
import java.net.HttpURLConnection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Iterator;
import javax.swing.JFrame;
import java.awt.Dimension;
import java.awt.event.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.text.*;
import javax.swing.text.html.*;
import java.text.MessageFormat;

/**
*	Various error dialogs, which use HTML templates to display errors.
*
*/
public class ErrorDialog extends TextViewer
{
	// i18n constants
	private static final String NO_MESSAGE = Utils.getLocalString("ErrorDlg.nomessage");
	private static final String UNKNOWN = Utils.getLocalString("ErrorDlg.unknown");

	private static final String GENERAL_TEMPLATE = "ErrorDlg.general.template";
	private static final String GENERAL_TITLE = "ErrorDlg.general.title";
	private static final String GENERAL_BUTTON = "ErrorDlg.general.button";
	
	
	private static final String FNF_TEMPLATE = "ErrorDlg.filenotfound.template";
	private static final String FILE_TEMPLATE = "ErrorDlg.file.template";
	private static final String FILE_TITLE = "ErrorDlg.file.title";
	private static final String FILE_BUTTON = "ErrorDlg.file.button";
	
	private static final String FATAL_TEMPLATE = "ErrorDlg.fatal.template";
	private static final String FATAL_TITLE = "ErrorDlg.fatal.title";
	private static final String FATAL_BUTTON = "ErrorDlg.fatal.button";
	
	private static final String SERIOUS_TEMPLATE = "ErrorDlg.serious.template";
	private static final String SERIOUS_TITLE = "ErrorDlg.serious.title";
	private static final String SERIOUS_BUTTON_QUIT = "ErrorDlg.serious.button.quit";
	private static final String SERIOUS_BUTTON_CONTINUE = "ErrorDlg.serious.button.continue";
	
	private static final String NOVARIANT_TEMPLATE = "ErrorDlg.novariant.template";
	private static final String NOVARIANT_TITLE = "ErrorDlg.novariant.title";
	private static final String NOVARIANT_BUTTON = "ErrorDlg.novariant.button";
	
	private static final String NET_TEMPLATE = "ErrorDlg.net.template";
	private static final String NET_UNKNOWN_HOST_TEMPLATE = "ErrorDlg.net.unknownhost.template";
	private static final String NET_TITLE = "ErrorDlg.net.title";
	private static final String NET_BUTTON = "ErrorDlg.net.button";
	
	private static final String VERSION_MISMATCH_TEMPLATE = "ErrorDlg.file.version.template";
	private static final String VERSION_MISMATCH_TITLE = "ErrorDlg.file.version.title";
	private static final String VERSION_MISMATCH_BUTTON = "ErrorDlg.file.version.button";
	
	// bug submission parameters
	private static final String SUBMIT_BUTTON = "ErrorDlg.submit.button";
	private static final String SUBMIT_TITLE = "ErrorDlg.submit.text.title";
	private static final String SUBMIT_SUCCESS = "ErrorDlg.submit.text.success";
	private static final String SUBMIT_FAILED = "ErrorDlg.submit.text.failed";
	private final static String ACTION_SUBMIT = "ACTION_SUBMIT";	
	
	// simple testing
	
	/*
	public static void main(String args[])
	{
		Exception ex = new NegativeArraySizeException("just a test");
		java.io.IOException ioe = new java.io.FileNotFoundException("just a test");
		
		// display each dialog
		displayFileIO(null, new FileNotFoundException("file_not_exist"), "TestFile.test");
		displayFileIO(null, ioe, "TestFile.test");
		displayFileIO(null, new InvalidClassException("bad version"), "TestFile.test");
		displayNetIO(null, new UnknownHostException("Unknown host"), "127.0.0.1");
		displayNetIO(null, new IOException("Some Exception Occured"), "http://127.0.0.1/whoknows.html");
		displayGeneral(null, ex);
		displaySerious(null, ex);
		displayFatal(null, ex);
	}
	*/
	
	
	/** Convenience Method (no BugReportInfo required) */
	public static void displaySerious(JFrame parent, Throwable e) 
	{
		ErrorDialog.displaySerious(parent, e, null);
	}// displaySerious()
	
	
	/**
	*	This should be used whenever a SERIOUS error occurs.
	*	<p>
	*	A Serious error is defined as an error from which may
	*	be irrecoverable, but gives the user a chance to continue 
	*	working in the program if possible. The user may also quit
	*	the program from this dialog.
	*
	*/
	public static void displaySerious(JFrame parent, Throwable e, 
		BugReportInfo bri)
	{
		Object[] args = new Object[3];
		
		args[0] = e.getClass().getName();
		args[1] = getMsg(e);
		args[2] = getStackTrace(e);	
		
		Log.println("SeriousError: ", args[0]);
		Log.println("  message: ", args[1]);
		Log.println("  stack trace:\n", args[2]);
		
		String text = Utils.getText(Utils.getLocalString(SERIOUS_TEMPLATE));
		
		ErrorDialog ed = getForcedExitDialog(parent,
			Utils.getLocalString(SERIOUS_TITLE), Utils.format(text, args),
			(String) args[2],
			Utils.getLocalString(SERIOUS_BUTTON_QUIT),
			Utils.getLocalString(SERIOUS_BUTTON_CONTINUE),
			Utils.getScreenSize(0.4f),
			true, true, bri);
		ed.setVisible(true);
	}// displaySerious()
	
	
	/** Convenience Method (no BugReportInfo required) */
	public static void displayFatal(JFrame parent, Throwable e) 
	{
		ErrorDialog.displayFatal(parent, e, null);
	}// displayFatal()
	
	/**
	*	This should be used whenever a FATAL error occurs.
	*	<p>
	*	A Fatal error is defined as an error from which recovery
	*	is impossible. The only option is to exit the program, with
	*	error code 1.
	*
	*/
	public static void displayFatal(JFrame parent, Throwable e, BugReportInfo bri)
	{
		Object[] args = new Object[3];
		
		args[0] = e.getClass().getName();
		args[1] = getMsg(e);
		args[2] = getStackTrace(e);	
		
		Log.println("FatalError: ", args[0]);
		Log.println("  message: ", args[1]);
		Log.println("  stack trace:\n", args[2]);
		
		String text = Utils.getText(Utils.getLocalString(FATAL_TEMPLATE));
		ErrorDialog ed = ErrorDialog.getOneButtonDialog(parent, 
			Utils.getLocalString(FATAL_TITLE), 
			Utils.format(text, args), 
			(String) args[2],
			Utils.getLocalString(FATAL_BUTTON),
			Utils.getScreenSize(0.4f), true, true, bri);
		ed.setVisible(true);
		
		System.exit(1);	// ALWAYS exit
	}// displayFatal()
	
	
	/**
	*	This should be used whenever a FILE error occurs.
	*	<p>
	*	A File error is any error (not nescessarily an IOException) 
	*	that occurs when reading, writing, or processing a file.
	*	<p>
	*	Special handling exists FileNotFoundException, 
	*	which is common and not a program issue.
	*/
	public static void displayFileIO(JFrame parent, Exception e, String fileName)
	{
		Object[] args = new Object[4];
		
		args[0] = getCleanName(e.getClass().getName());
		args[1] = getMsg(e);
		args[2] = getStackTrace(e);	
		args[3] = (fileName == null) ? UNKNOWN : fileName;
		
		Log.println("FileIOError: ", args[0]);
		Log.println("  message: ", args[1]);
		Log.println("  file: ", args[3]);
		Log.println("  stack trace:\n", args[2]);
		
		String text = null;
		if(e instanceof FileNotFoundException)
		{
			text = Utils.getText(Utils.getLocalString(FNF_TEMPLATE));
		}
		else
		{
			text = Utils.getText(Utils.getLocalString(FILE_TEMPLATE));
		}
		
		
		ErrorDialog ed = getOneButtonDialog(parent, 
			Utils.getLocalString(FILE_TITLE), Utils.format(text, args), 
			(String) args[2],
			Utils.getLocalString(FILE_BUTTON), Utils.getScreenSize(0.4f), 
			true, false, null);
		
		ed.setVisible(true);		
	}// displayFileIO()
	
	
	/** Convenience Method (no BugReportInfo required) */
	public static void displayNetIO(JFrame parent, IOException e, 
		String connection)
	{
		ErrorDialog.displayNetIO(parent, e, connection, null);
	}// displayNetIO()
	
	/**
	*	This should be used whenever a NETWORK error occurs.
	*	<p>
	*	Network errors are all assumed to be IOException instances or
	*	subclasses. The <code>connection</code> parameter may be null, or, 
	*	if specified, the connection text (URL or IP address, for example).
	*	<p>
	*	Special handling exists for an UnknownHostException.
	*
	*/
	public static void displayNetIO(JFrame parent, IOException e, 
		String connection, BugReportInfo bri)
	{
		Object[] args = new Object[4];
		
		args[0] = getCleanName(e.getClass().getName());
		args[1] = getMsg(e);
		args[2] = getStackTrace(e);	
		args[3] = (connection == null) ? UNKNOWN : connection;
		
		Log.println("NetworkIOError: ", args[0]);
		Log.println("  message: ", args[1]);
		Log.println("  connection: ", args[3]);
		Log.println("  stack trace:\n", args[2]);
		
		String text = null;
		boolean submittable = false;
		if(e instanceof UnknownHostException)
		{
			text = Utils.getText(Utils.getLocalString(NET_UNKNOWN_HOST_TEMPLATE));
		}
		else
		{
			text = Utils.getText(Utils.getLocalString(NET_TEMPLATE));
			submittable = true;
		}
		
		
		ErrorDialog ed = getOneButtonDialog(parent, 
			Utils.getLocalString(NET_TITLE), Utils.format(text, args), 
			(String) args[2],
			Utils.getLocalString(NET_BUTTON), Utils.getScreenSize(0.4f), 
			true, submittable, bri);
		ed.setVisible(true);		
	}// displayNetIO()
	
	
	/** Convenience Method (no BugReportInfo required) */
	public static void displayGeneral(JFrame parent, Exception e)
	{
		ErrorDialog.displayGeneral(parent, e, null);
	}// displayGeneral()
	
	
	/**
	*	This should be used whenever a GENERAL error occurs.
	*	<p>
	*	A General error is an error that does not fit any of the
	*	other categories.
	*/
	public static void displayGeneral(JFrame parent, Exception e, 
		BugReportInfo bri)
	{
		Object[] args = new Object[3];
		
		args[0] = getCleanName(e.getClass().getName());
		args[1] = getMsg(e);
		args[2] = getStackTrace(e);	
		
		Log.println("GeneralError: ", args[0]);
		Log.println("  message: ", args[1]);
		Log.println("  stack trace:\n", args[2]);
		
		String text = Utils.getText(Utils.getLocalString(GENERAL_TEMPLATE));
		ErrorDialog ed = getOneButtonDialog(parent, 
			Utils.getLocalString(GENERAL_TITLE), Utils.format(text, args), 
			(String) args[2],
			Utils.getLocalString(GENERAL_BUTTON), Utils.getScreenSize(0.4f), 
			true, true, bri);
		ed.setVisible(true);		
	}// displayGeneral()
	
	
	/**
	*	This should be used whenever there is a Variant version mismatch.
	*/
	public static void displayVariantVersionMismatch(JFrame parent, 
		World.VariantInfo vi, float availableVersion)
	{
		Object[] args = new Object[3];
		
		args[0] = vi.getVariantName();
		args[1] = new Float( vi.getVariantVersion() );
		args[2] = new Float( availableVersion );
		
		String text = Utils.getText(Utils.getLocalString(VERSION_MISMATCH_TEMPLATE));
		ErrorDialog ed = getOneButtonDialog(parent, 
			Utils.getLocalString(VERSION_MISMATCH_TITLE), Utils.format(text, args), 
			(String) args[2],
			Utils.getLocalString(VERSION_MISMATCH_BUTTON), 
			Utils.getScreenSize(0.4f), true, false, null);
		ed.setVisible(true);
		
	}// displayVariantNotAvailable()

	
	/**
	*	This should be used whenever a Variant is not available.
	*/
	public static void displayVariantNotAvailable(JFrame parent, World.VariantInfo vi)
	{
		Object[] args = new Object[3];
		
		args[0] = vi.getVariantName();
		args[1] = new Float( vi.getVariantVersion() );
		args[2] = vi.getVariantName();
		
		String text = Utils.getText(Utils.getLocalString(NOVARIANT_TEMPLATE));
		ErrorDialog ed = getOneButtonDialog(parent, 
			Utils.getLocalString(NOVARIANT_TITLE), Utils.format(text, args), 
			(String) args[2],
			Utils.getLocalString(NOVARIANT_BUTTON), Utils.getScreenSize(0.4f), 
			true, false, null);
		ed.setVisible(true);
	}// displayVariantNotAvailable()
	
	
	/** Prepends the jDip and Java version/etc info to the stack trace */
	private static String getStackTrace(Throwable t)
	{
		StringBuffer sb = new StringBuffer(2048);
		StackTraceElement[] ste = t.getStackTrace();
		
		sb.append("jDip version: ");
		sb.append(ClientFrame.getVersion());
		
		try
		{
			sb.append("<br>\nJava version: ");
			sb.append(System.getProperty("java.version", "?"));
			sb.append("<br>\nJava vendor: ");
			sb.append(System.getProperty("java.vendor", "?"));
			sb.append("<br>\nJava runtime version: ");
			sb.append(System.getProperty("java.runtime.version", "?"));
			sb.append("<br>\nOS name: ");
			sb.append(System.getProperty("os.name", "?"));
			sb.append("<br>\nOS version: ");
			sb.append(System.getProperty("os.version", "?"));
			sb.append("<br>\nOS arch: ");
			sb.append(System.getProperty("os.arch", "?"));
			
			// some runtime info...
			Runtime rt = Runtime.getRuntime();
			sb.append("<br>\nMemory Free: ");
			sb.append(rt.freeMemory());
			sb.append("<br>\nMemory Total: ");
			sb.append(rt.totalMemory());
			sb.append("<br>\nMemory Max: ");
			sb.append(rt.maxMemory());
		}
		catch(Exception e)
		{
			sb.append("<br>\n[Exception occured while getting system/runtime info]");
		}
		
		sb.append("<br>\n");
		sb.append(t.getClass().getName());
		sb.append("<br>\n");
		sb.append(t.getMessage());
		sb.append("<br>\n");
		
		appendBatikInfo(sb, t);
		
		int len = ste.length;
		for(int i=(len - 1); i>=0; i--)
		{
			sb.append(ste[i].toString());
			sb.append('\n');
		}
		
		return sb.toString();
	}// getStackTrace()
	
	
	private static String getMsg(Throwable t)
	{
		String msg = t.getLocalizedMessage();
		if(msg != null)
		{
			if(!"".equals(msg))
			{
				return msg;
			}
		}
		
		return NO_MESSAGE;
	}// getMsg()
	
	/** given a name like xxx.x.xxx.x.x.x.aaaa returns 'aaaa' */
	private static String getCleanName(String in)
	{
		return in.substring(in.lastIndexOf(".")+1);
	}
	
	/** Create an ErrorDialog */
	private ErrorDialog(JFrame frame, String title)
	{
		super(frame, true);
		setTitle(title);
	}// ErrorDialog()
	
	/** 
	*	Create an ErrorDialog that is setup with a single button.
	*	However, if submittable is set to true, a "submit" button is also present,
	*	to send the bug report to the jDip bug report database.
	*/
	private static ErrorDialog getOneButtonDialog(final JFrame parent, 
		String title, final String text, final String rawText, 
		String buttonText, Dimension size, boolean resizable, 
		boolean submittable, final BugReportInfo bri)
	{
		ErrorDialog ed = new ErrorDialog(parent, title)
		{
			protected void close(String actionCommand)
			{
				if(ACTION_SUBMIT.equals(actionCommand))
				{
					setButtonEnabled(ACTION_SUBMIT, false);
					if(!submitBug(parent, rawText, bri))
					{
						setButtonEnabled(ACTION_SUBMIT, true);
					}
				}
				else
				{
					super.close(actionCommand);
				}
			}// close();
		};
		ed.setContentType("text/html");
		ed.setEditable(false);
		ed.setText(text);
		ed.setHeaderVisible(false);
		if(submittable)
		{
			ed.addTwoButtons( 
				ed.makeButton(buttonText, ACTION_CLOSE, true),
				ed.makeButton(Utils.getLocalString(SUBMIT_BUTTON),
					ACTION_SUBMIT, true),
				false, true);
		}
		else
		{
			ed.addSingleButton( ed.makeButton(buttonText, ACTION_CLOSE, true) );
		}
		ed.pack();
		
		
		
		if(size != null)
		{
			ed.setSize(size);
		}
		
		ed.setResizable(resizable);
		Utils.centerInScreen(ed);
		return ed;
	}// getOneButtonDialog()
	
	
	/** 
	*	Create an ErrorDialog that is setup with two buttons, 
	*	the second of which exits the program.
	*	However, if submittable is set to true, a "submit" button is also present,
	*	to send the bug report to the jDip bug report database.
	*	rawText is the error-message alone (no dialog text)
	*/
	private static ErrorDialog getForcedExitDialog(final JFrame parent, 
		String title, final String text, final String rawText, String exitText,
		String continueText, Dimension size, boolean resizable, 
		boolean submittable, final BugReportInfo bri)
	{
		ErrorDialog ed = new ErrorDialog(parent, title)
		{
			protected void close(String actionCommand)
			{
				if(ACTION_SUBMIT.equals(actionCommand))
				{
					if(submitBug(parent, rawText, bri))
					{
						setButtonEnabled(ACTION_SUBMIT, false);
					}
				}
				else if(isOKorAccept(actionCommand))
				{
					setVisible(false);
					dispose();		// attempt to continue
				}
				else
				{
					setVisible(false);	
					System.exit(1);		// exit the program, with an error condition
				}
			}// close();
		};
		
		ed.setContentType("text/html");
		ed.setEditable(false);
		ed.setText(text);
		ed.setHeaderVisible(false);
		
		JButton bR = ed.makeButton(exitText, ACTION_CANCEL, true);
		JButton bC = ed.makeButton(continueText, ACTION_OK, true);
		JButton bL = ed.makeButton(Utils.getLocalString(SUBMIT_BUTTON), ACTION_SUBMIT, true);
		ed.addThreeButtons(bL, bC, bR, bC, bR);
		ed.pack();
		
		if(size != null)
		{
			ed.setSize(size);
		}
		
		ed.setResizable(resizable);
		Utils.centerInScreen(ed);
		
		return ed;
	}// getForcedExitDialog()
	
	/** If exception is a Batik exception, with line # info, append */
	private static void appendBatikInfo(StringBuffer sb, Throwable e)
	{
		if(e instanceof org.apache.batik.bridge.BridgeException)
		{
			org.apache.batik.bridge.BridgeException be = (org.apache.batik.bridge.BridgeException) e;
			sb.append("\nBridgeException:");
			sb.append("\n  Code: ");
			sb.append(be.getCode());
			sb.append("\n  Element: ");
			sb.append(be.getElement().getTagName());
			sb.append("\n");
		}
		else if(e instanceof org.w3c.css.sac.CSSParseException)
		{
			org.w3c.css.sac.CSSParseException pe = (org.w3c.css.sac.CSSParseException) e;
			sb.append("\nCSSParseException:");
			sb.append("\n  URI: ");
			sb.append(pe.getURI());
			sb.append("\n  Line: ");
			sb.append(String.valueOf(pe.getLineNumber()));
			sb.append("\n  Column: ");
			sb.append(String.valueOf(pe.getColumnNumber()));
			sb.append("\n");
		}
		else if(e instanceof org.apache.batik.script.InterpreterException)
		{
			org.apache.batik.script.InterpreterException ie = (org.apache.batik.script.InterpreterException) e;
			sb.append("\nInterpreterException:");
			sb.append("\n  Line: ");
			sb.append(String.valueOf(ie.getLineNumber()));
			sb.append("\n  Column: ");
			sb.append(String.valueOf(ie.getColumnNumber()));
			sb.append("\n");
		}
		else if(e instanceof org.apache.batik.css.parser.ParseException)
		{
			org.apache.batik.css.parser.ParseException pe = (org.apache.batik.css.parser.ParseException) e;
			sb.append("\ncss.ParseException:");
			sb.append("\n  Line Number: ");
			sb.append(String.valueOf(pe.getLineNumber()));
			sb.append("\n  Column: ");
			sb.append(String.valueOf(pe.getColumnNumber()));
			sb.append("\n");
		}
		else if(e instanceof org.apache.batik.parser.ParseException)
		{
			org.apache.batik.parser.ParseException pe = (org.apache.batik.parser.ParseException) e;
			sb.append("\nParseException:");
			sb.append("\n  Line: ");
			sb.append(String.valueOf(pe.getLineNumber()));
			sb.append("\n  Column: ");
			sb.append(String.valueOf(pe.getColumnNumber()));
			sb.append("\n");
		}
	}// appendBatikInfo()
	
	
	
	/** 
	*	GUI version of sendBugReport. Popup error message with result.
	*	Does not enable or disable the submit button. BugReportInfo
	*	may be null.
	*/
	private static boolean submitBug(JFrame parent, String text, 
		BugReportInfo bri)
	{
		if(sendBugReport(text, bri))
		{
			Utils.popupInfo(parent, Utils.getLocalString(SUBMIT_TITLE), 
				 Utils.getLocalString(SUBMIT_SUCCESS));
			return true;
		}
		else
		{
			Utils.popupError(parent, Utils.getLocalString(SUBMIT_TITLE), 
				 Utils.getLocalString(SUBMIT_FAILED));
			return false;
		}
	}// submitBug()
	
	/**
	*	Send bug report to jDip website. Synchronous. Will not
	*	throw further exceptions (unless a null String is passed).
	*	BugReportInfo is allowed to be null.
	*/
	private static boolean sendBugReport(String text, BugReportInfo bri)
	{
		if(text == null)
		{
			throw new IllegalArgumentException();
		}
		
		OutputStreamWriter wr = null;
		BufferedReader rd = null;
		
		try
		{
			URL url = new URL("http://jdip.sourceforge.net/forms/data/bugFormProc.php");
			
			HttpURLConnection urlConn = (HttpURLConnection) url.openConnection();
			urlConn.setRequestMethod("POST");
			urlConn.setDoInput(true);
			urlConn.setDoOutput(true);
			urlConn.setUseCaches(false);
			urlConn.setRequestProperty("Content-Type","application/x-www-form-urlencoded"); 
			urlConn.setRequestProperty("Accept-Charset", "*");
			urlConn.setRequestProperty("User-Agent", "jDip");
			
			wr = new OutputStreamWriter(urlConn.getOutputStream());
			final String header 	= "brHeader="+URLEncoder.encode(
				"JDIP_REMOTE_BUG_REPORT", "UTF-8");
			final String time 	= "&brTime="+URLEncoder.encode(
				String.valueOf(new Date()), "UTF-8");
			final String message 	= "&brText="+URLEncoder.encode(text, "UTF-8");
			
			wr.write(header);
			wr.write(time);
			if(bri != null)
			{
				wr.write(bri.getInfo());
			}
			wr.write(message);
			
			wr.flush();
			wr.close();
			
			rd = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));
			String line;
			while ((line = rd.readLine()) != null)
			{
				if(line.indexOf("Thanks.") != -1)
				{
					return true;
				}
			}
		}
		catch(Exception e)
		{
			Log.println("ERROR: could not send bug report.");
			Log.println(e);
		}
		finally
		{
			try
			{
				if(wr != null)
				{
					wr.close();
				}
			}
			catch(Exception e)
			{
				Log.println("ErrorDialog: ", e);
			}
			
			try
			{
				if(rd != null)
				{
					rd.close();
				}
			}
			catch(Exception e)
			{
				Log.println("ErrorDialog: ", e);
			}
		}
		
		return false;
	}// sendBugReport()
	
	
	/**
	*	Extra debug information that can be sent, and is included in debug
	*	logs or transmitted bug reports.
	*/
	public static class BugReportInfo
	{
		private final List list;
		
		public BugReportInfo()
		{
			list = new LinkedList();
		}// BugInfo()
		
		/**
		*	Add a line; name/value. This is typically displayed as:<br>
		*	<code>name: value</code><br>
		*	on a single line.
		*/
		public void add(String name, String value)
		{
			StringBuffer sb = new StringBuffer();
			sb.append( ((name == null) ? "" : name) );
			sb.append(": ");
			sb.append( value );
			list.add(sb.toString());
		}// add()
		
		/**
		*	Returns all name-value pairs, as a String.
		*	Lines are terminated with newline charater(s).
		*/
		public String getInfo()
		{
			StringBuffer sb = new StringBuffer();
			Iterator iter = list.iterator();
			while(iter.hasNext())
			{
				String line = (String) iter.next();
				sb.append(line);
				sb.append('\n');
			}
			
			return sb.toString();
		}// getInfo()
		
		
	}// nested class BugReportInfo
	
}// class ErrorDialog
