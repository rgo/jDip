//
//  @(#)Log.java	1.00	4/1/2002
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

/**
*	
*	A very simple logging class that logs all data to stdout. Note that this
*	was implemented for speed and simplicity, rather than using the J2SDK 
*	intrinsic Logging class.
*	<p>
*	By matching methods to log callers, we eliminate String or StringBuffer
*	construction and Object.toString() invocations, which provides a significant
*	speedup. 
*	<p>
*	This class is not really MT safe
*/
public final class Log
{
	private static boolean isLogging = false; 
	private static BufferedWriter bw = null;
	
	/** Private constructor */
	private Log()
	{
	}// Log()
	
	
	/** Enable or disable logging */
	public static void setLogging(boolean value)
	{
		isLogging = value;
	}// setLogging()
	
	
	/** Check if logging is enabled or disabled */
	public static boolean isLogging()
	{
		return isLogging;
	}// isLogging()
	
	
	/**
	*	Set the File to which we will write (or append) a Log file, 
	*	if null, write output to stdout.
	*/
	public static void setFile(File out)
	{
		if(bw != null)
		{
			close();
		}
		
		if(out == null)
		{
			bw = null;
			System.out.println("*********** logging started ***********");
			System.out.println((new Date()).toString());
			System.out.println("***************************************");
		}
		else
		{
			try
			{
				bw = new BufferedWriter(new FileWriter(out, true));
				bw.newLine();
				bw.write("*********** logging started ***********");
				bw.newLine();
				bw.write((new Date()).toString());
				bw.newLine();
				bw.write("***************************************");
				bw.newLine();
				bw.flush();
			}
			catch(IOException e)
			{
				System.err.println(e);
			}
		}
	}// setFile()
	
	/**
	*	Flushes and closes the log file (if writing to stdout, this has no effect)
	*/
	public static void close()
	{
		if(bw != null)
		{
			try
			{
				bw.flush();
			}
			catch(IOException e)
			{
				System.err.println(e);
			}
			finally
			{
				try { bw.close(); } catch(IOException e2) {}
			}
		}
	}// close()
	
	/** 
	*	Print the given Object to the output file / stdout 
	*	via the Object's toString() method. 
	*/
	public static void print(Object s)
	{
		if(isLogging)
		{
			if(bw == null)
			{
				System.out.print(s);
			}
			else
			{
				try
				{
					bw.write(s.toString());
					bw.flush();
				}
				catch(IOException e)
				{
					System.err.print(e);
				}
			}
		}
	}// print()
	
	
	/** 
	*	Print the given Object to the output file / stdout 
	*	via the Object's toString() method. Follows with a 
	*	newline.
	*/
	public static void println(Object s)
	{
		if(isLogging)
		{
			if(bw == null)
			{
				System.out.println(s);
			}
			else
			{
				try
				{
					bw.write(s.toString());
					bw.newLine();
					bw.flush();
				}
				catch(IOException e)
				{
					System.err.println(e);
				}
			}
		}
	}// println()
	
	
	/** Print text followed by an int */
	public static void println(Object s0, int i0)
	{
		if(isLogging)
		{
			StringBuffer sb = new StringBuffer(256);
			sb.append(s0);
			sb.append(i0);
			println(sb);
		}
	}// println()
	
	/** Print text followed timing delta and current time. */
	public static void printTimed(long lastTime, Object s0)
	{
		if(isLogging)
		{
			long now = System.currentTimeMillis();
			StringBuffer sb = new StringBuffer(256);
			sb.append(s0);
			sb.append(' ');
			sb.append((now - lastTime));
			sb.append(" ms [delta]; current: ");
			sb.append(now);
			println(sb);
		}
	}// println()
	
	/** Print the delta from the given time. Return the new time. */
	public static long printDelta(long lastTime, Object s0)
	{
		if(isLogging)
		{
			final long now = System.currentTimeMillis();
			StringBuffer sb = new StringBuffer(128);
			sb.append(s0);
			sb.append(' ');
			sb.append((now - lastTime));
			sb.append(" ms [delta]");
			sb.append(now);
			println(sb);
			return now;
		}
		
		return 0L;
	}// printDelta()
	
	
	/** Print the given objects to the log */
	public static void println(Object s0, Object s1)
	{
		if(isLogging)
		{
			StringBuffer sb = new StringBuffer(256);
			sb.append(s0);
			sb.append(s1);
			println(sb);
		}
	}// println()
	
	/** Print the given objects to the log */
	public static void println(Object s0, Object s1, Object s2)
	{
		if(isLogging)
		{
			StringBuffer sb = new StringBuffer(256);
			sb.append(s0);
			sb.append(s1);
			sb.append(s2);
			println(sb);
		}
	}// println()
	
	
	/** Print the given objects to the log */
	public static void println(Object s0, Object s1, Object s2, Object s3)
	{
		if(isLogging)
		{
			StringBuffer sb = new StringBuffer(256);
			sb.append(s0);
			sb.append(s1);
			sb.append(s2);
			sb.append(s3);
			println(sb);
		}
	}// println()
	
}// class Log


