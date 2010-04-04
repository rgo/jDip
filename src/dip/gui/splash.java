//
//  @(#)splash.java		12/1/2002
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
package dip.gui;

import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.*;
/**
*
*	A very fast statup Splash screen, using AWT.
*
*
*
*
*/
public final class splash
{
	private static final String SPLASH_GRAPHIC = "resource/common/splash/splash.jpg";
	private static splashRunner sprunner = null;
	
	
	/** Starts the Splash screen, then starts loading jDip (ClientFrame) */
	public static void main(String args[])
	{
		//long time = System.currentTimeMillis();
		//System.out.println("splash.main() START: "+time); 
		checkRequirements();
		
		// check for a 'nosplash' argument
		boolean noSplash = false;
		for(int i=0; i<args.length; i++)
		{
			if(args[i].equalsIgnoreCase("-nosplash"))
			{
				noSplash = true;
				break;
			}
		}
		
		// start splash
		if(!noSplash)
		{
			new splash();
		}
		
		// start main program
		//
		// we use Class.forName() because otherwise we will not be able to 
		// successfully compile this class with 1.1 and the rest with 1.4
		// hence the loose coupling.
		try
		{
			Class cf = Class.forName("dip.gui.ClientFrame");
			Method m = cf.getMethod("main", new Class[] {String[].class});
			Object[] params = new Object[] { args };
			m.invoke(null, params);
			//time = (System.currentTimeMillis() - time);
			//System.out.println("splash.main() INVOKED: "+time); 
		}
		catch(Exception e)
		{
			System.err.println(e);
			e.printStackTrace();
		}
		
		// time = (System.currentTimeMillis() - time);
		// System.out.println("splash.main() END: "+time); 
	}// main()
	
	
	/** Destroy the Splash Screen */
	public synchronized static void destroy()
	{
		if(sprunner != null)
		{
			sprunner.destroy();
			sprunner = null;
		}
	}// destroy()
	
	
	/** */
	private splash()
	{
		 sprunner = new splashRunner();
		 Thread t = new Thread(sprunner);
		 t.setPriority(Thread.MIN_PRIORITY);
		 t.start();
	}// splsah()
	
	/** Create display components */
	private class splashRunner implements Runnable
	{
		private Frame frame = null;
		private Window win = null;
		private Image img = null;
		
		public void destroy()
		{
                    /*
                     * NOTE-RGO: "show/hide" is deprecated I change it to "setVisible(boolean b) instead
                     */
			win.setVisible(false);
			win.dispose();
			frame.dispose();
			img.flush();
			img = null;
			win = null;
			frame = null;
		}		
		
		public void run()
		{
			//long time = System.currentTimeMillis();
			//System.out.println("splash.run() START: "+time); 
			// if we are headless, OR, if X is not loaded (on unix systems), 
			// this will throw an internal error. Fix.
			try
			{
				frame = new Frame("test");
			}
			catch(Throwable t)
			{
				// be a bit facetious
				System.err.println("jDip requires a computer with a mouse, keyboard, monitor,"); 
				System.err.println("and graphical environment (e.g., XWindows) to run.");
				System.exit(1);
			}
			
			
			win = new Window(frame);
			
			// get image
			ClassLoader classLoader = this.getClass().getClassLoader();
			img = win.getToolkit().createImage(classLoader.getResource(SPLASH_GRAPHIC));
			
			// load entire image
			MediaTracker tracker = new MediaTracker(win);
			synchronized(tracker)
			{
				tracker.addImage(img, 0);
				
				try
				{
					tracker.waitForID(0);
					tracker.removeImage(img, 0);
				}
				catch(InterruptedException e)
				{
				}
			}
			
			tracker = null;
			
			Canvas canvas = new Canvas()
			{
				public void paint(Graphics g)
				{
					super.paint(g);
					if(img != null)
					{
						g.drawImage(img, 0, 0, null);
					}
				}
			};
			
			if(img != null)
			{
				int w = img.getWidth(null);
				int h = img.getHeight(null);
				Dimension screenSize = win.getToolkit().getScreenSize();
			
				canvas.setBounds(0,0,w,h);
				win.setLayout(null);
				win.add(canvas);
				win.setBounds(0,0,w,h);
				win.pack();
				win.setLocation((screenSize.width - w)/2, (screenSize.height - h)/2);
                                /*
                                 * NOTE-RGO: "show/hide" is deprecated I change it to "setVisible(boolean b) instead
                                 */
				win.setVisible(true);
				win.repaint();
				win.toFront();
			}
			//time = (System.currentTimeMillis() - time);
			//System.out.println("splash.run() END: "+time+"; "+System.currentTimeMillis()); 
		}
	}// splash()
	
	
	
	
	/** 
	*	Check the Java version; if not > 1.4, show an error message. 
	*	Note that for this to work, this class must be compiled with a 
	*	JDK 1.1 (preferably) class file target; otherwise the old-version
	*	JDK will not load dip.gui.splash.
	*	<p>
	*	This method will also check to see that the graphical environment
	*	is running (if the version check succeeds).
	*	<p>
	*	This method will return if successful; otherwise, it will exit
	*	with an error condition.
	*/
	private static void checkRequirements()
	{
		// 48 = 1.4; 49 = 1.5; 44 = 1.0   : class file version -> java version
		String cv = System.getProperty("java.class.version","44.0");
		if("48.0".compareTo(cv) > 0)
		{
			String version = System.getProperty("java.version", "(unknown)");
			String vendor = System.getProperty("java.vendor", "(unknown)");
			
			
			// print an error to stderr
			// 
			System.err.println("jDip requires Java version 1.4 or greater to be installed.");
			System.err.println("The detected Java version is: "+version+", by "+vendor);
			System.err.println("An updated version of Java may be obtained from:");
			System.err.println("   http://www.java.com");
			System.err.println("");
			
			
			// use an AWT to prevent loading of swing classes just to display a 
			// graphical error
			// 
			// wrap everything in a try/catch to catch possible headless/AWT exceptions,
			// e.g., graphical environment is not loaded.
			//
			try
			{
				final Frame dlgFrame = new Frame("jDip");
				final Dialog dlg = new Dialog(dlgFrame, "jDip Error", true);
				
				dlg.addWindowListener(new WindowAdapter()
				{
					public void windowClosing(WindowEvent e)
					{
                                                /*
                                                 * NOTE-RGO: "show/hide" is deprecated I change it to "setVisible(boolean b) instead
                                                 */
						dlg.setVisible(false);
						dlg.dispose();
						dlgFrame.dispose();
					}
				});
				
				
				Button button = new Button("  OK  ");
				button.addActionListener(new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
                                                /*
                                                 * NOTE-RGO: "show/hide" is deprecated I change it to "setVisible(boolean b) instead
                                                 */
						dlg.setVisible(false);
						dlg.dispose();
						dlgFrame.dispose();
					}
				});
				
				Panel panel = new Panel(new FlowLayout(FlowLayout.CENTER));
				panel.add(button);
				
				dlg.setLayout(new GridLayout(6,1,0,0));
				dlg.add(new Label("jDip requires a Java 1.4 or later to be installed."));
				dlg.add(new Label("The Java version detected was: "+version+" from "+vendor));
				dlg.add(new Label("Please obtain a newer version from a site below:"));
				dlg.add(new Label("  * http://www.java.com"));
				dlg.add(new Label("  * http://www.apple.com/java/     (for OS X users)"));
				dlg.add(panel);
				dlg.pack();
				
				// dialog-close-listener
				
				// center and show dialog
				Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
				Dimension dlgSize = dlg.getSize();		
				dlg.setLocation((screenSize.width - dlgSize.width)/2, (screenSize.height - dlgSize.height)/2);
                                /*
                                 * NOTE-RGO: "show/hide" is deprecated I change it to "setVisible(boolean b) instead
                                 */
				dlg.setVisible(true);
			}
			catch(Throwable t)
			{
				// do nothing. we've done enough already :-)
			}
			
			// error exit
			System.exit(1);
		}
	}// checkVersion()
	
}// class splash
