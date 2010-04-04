
package dip.misc;

import java.util.*;
import java.io.*;

/**
*	Given 2 or more .properties files, it compares them, and writes
*	(to stdout) which property file is missing a key (compared to all
*	other files).
*	
*	
*/
public class PropertyCheck
{
	final Properties[] props;
	final String[] names;
	
	public static void main(String args[])
	throws IOException
	{
		if(args.length < 2)
		{
			System.err.println("PropertyCheck: 2 or more properties files must be specified.");
			System.exit(1);
		}
		
		PropertyCheck pc = new PropertyCheck(args);
		pc.check();
	}// main()
	
	
	public PropertyCheck(String[] args)
	throws IOException
	{
		this.names = args;
		this.props = new Properties[names.length];
		for(int i=0; i<props.length; i++)
		{
			props[i] = new Properties();
			props[i].load(new BufferedInputStream(new FileInputStream(names[i])));
		}
	}// PropertyCheck()
	
	
	public void check()
	{
		for(int i=0; i<props.length; i++)
		{
			Properties p = props[i];
			
			System.out.println("\n\nCHECKING: "+names[i]);
			System.out.println("Missing Keys:");
			System.out.println("---------------------------------------------");
			boolean noneMissing = true;
			
			for(int j=0; j<props.length; j++)
			{
				if(i != j)
				{
					// go through all entries in 'j' and compare to 'i'.
					// if 'i' doesn't contain an entry, print that, and 
					// print which file it was from.
					//
					final String name = names[j];
					Enumeration e = props[j].propertyNames(); 
					while(e.hasMoreElements())
					{
						final String key = (String) e.nextElement();
						if(p.getProperty(key) == null)
						{
							System.out.println(" "+key+"  (from "+name+")");
							noneMissing = false;
						}
					}
				}
			}
			
			if(noneMissing)
			{
				System.out.println("no keys missing.");
			}
		}
	}// check()
	
	
	
}// class PropertyCheck
