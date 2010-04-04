//
//  @(#)MetadataDialog.java	1.00	8/2002
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


import dip.world.Power;
import dip.world.World;
import dip.gui.ClientFrame;
import dip.gui.AbstractCFPListener;
import dip.gui.swing.XJTextPane;
import dip.gui.swing.XJScrollPane;
import dip.gui.swing.ColorRectIcon;
import dip.gui.map.SVGColorParser;
import dip.gui.map.MapMetadata;
import dip.world.metadata.GameMetadata;
import dip.world.metadata.PlayerMetadata;
import dip.misc.Utils;

// HIGLayout
import cz.autel.dmi.HIGConstraints;
import cz.autel.dmi.HIGLayout;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.border.EtchedBorder;
import java.net.URI;
import java.net.URISyntaxException;

import java.awt.*;

/**
*	Metadata Editing/Viewing dialog
*	<p>
*	Although the PlayerMetadata object supports multiple email addresses per player, the
*	editor currently only supports upto 4.
	<p>
	TODO: make non-modal; but if game is destroyed, dialog must too be destroyed; if new dialog
		selected, and one already exists, it must pop to the top.
*
*/
public class MetadataDialog extends HeaderDialog
{
	// il8n constants
	private static final String TITLE = "MetadataDialog.title";
	private static final String HEADER_LOCATION = "MetadataDialog.location.header";
	private static final String TAB_GAME_PANEL = "MetadataDialog.tabname.game";
	
	// il8n constants: game data field names (notes: GDF = Game Data Field)
	private static final String GDF_NOTES = "MetadataDialog.field.game.notes";
	private static final String GDF_COMMENT = "MetadataDialog.field.game.comment";
	private static final String GDF_GAME_NAME = "MetadataDialog.field.game.gamename";
	private static final String GDF_MOD_NAME = "MetadataDialog.field.game.modname";
	private static final String GDF_MOD_EMAIL = "MetadataDialog.field.game.modemail";
	private static final String GDF_MOD_URI = "MetadataDialog.field.game.moduri";
	private static final String GDF_JUDGE_NAME = "MetadataDialog.field.game.judgename";
	private static final String GDF_GAME_URI = "MetadataDialog.field.game.gameuri";
	private static final String GDF_GAME_ID = "MetadataDialog.field.game.gameid";
	
	// il8n constants: player data field names
	private static final String PDF_NOTES = "MetadataDialog.field.player.notes";
	private static final String PDF_NAME = "MetadataDialog.field.player.name";
	private static final String PDF_EMAIL = "MetadataDialog.field.player.email";
	private static final String PDF_URI = "MetadataDialog.field.player.uri";
	
	
	// misc constants
	private static final int BORDER = 10;
	private static final int COLUMNS = 20;
	private static final int COLUMNS_LONG = 40;
	
	
	// instance variables
	private JTabbedPane tabPane = null;
	private ClientFrame clientFrame = null;
	private IconColorListener propertyListener = null;	// for tab icons
	private MapMetadata mmd = null;		// for tab icons
	
	
	
	/** 
	*	Display the dialog. <br>
	*	Editable by default. Null metadata arguments are not permissable 
	*/
	public static void displayDialog(ClientFrame parent)
	{
		MetadataDialog md = new MetadataDialog(parent);
		md.pack();
		md.setSize(Utils.getScreenSize(0.65f, 0.85f));
		Utils.centerInScreen(md);
		md.tabPane.setSelectedIndex(0);	// always reset to first tab
		md.setVisible(true);
	}// displayDialog()
	
	
	/** Creates the dialog */
	private MetadataDialog(ClientFrame parent)
	{
		super(parent, Utils.getLocalString(TITLE), true);
		this.clientFrame = parent;
		
		mmd = clientFrame.getMapMetadata();
		if(mmd == null)
		{
			propertyListener = new IconColorListener();
			clientFrame.addPropertyChangeListener(propertyListener);
		}
		
		makeTabPanel();	
		setTabIcons();
		
		setHeaderText( Utils.getText(Utils.getLocalString(HEADER_LOCATION)) );
		createDefaultContentBorder(tabPane);
		setContentPane(tabPane);
		addTwoButtons( makeCancelButton(), makeOKButton(), false, true );
		setHelpID(dip.misc.Help.HelpID.Dialog_Metadata);
	}// MetadataDialog()
	
	
	/** Handle OK/Cancel selections */
	public void close(String actionCommand)
	{
		super.close(actionCommand);
		
		if(isOKorAccept(actionCommand))
		{
			World world = clientFrame.getWorld();
			
			// this isn't really the way to do it
			GamePanel gp = (GamePanel) tabPane.getComponentAt(0);
			GameMetadata gmd = new GameMetadata();
			gp.write(gmd);
			world.setGameMetadata(gmd);
			
			// assume panels 1-n are all player panels
			for(int i=1; i<tabPane.getTabCount(); i++)
			{
				PlayerPanel pp = (PlayerPanel) tabPane.getComponentAt(i);
				PlayerMetadata pmd = new PlayerMetadata();
				pp.write(pmd);
				world.setPlayerMetadata(pp.getPower(), pmd);
			}
			
			// set data-changed flag
			clientFrame.fireStateModified();
		}
		
		if(propertyListener != null)
		{
			clientFrame.removePropertyChangeListener(propertyListener);
		}
	}// close()
	
	
	/** Make the tab panel */
	private void makeTabPanel()
	{
		// create tabbed pane
		tabPane = new JTabbedPane();
		
		// first tab is Game info
		World world = clientFrame.getWorld();
		tabPane.add( Utils.getLocalString(TAB_GAME_PANEL), makeGamePanel(world.getGameMetadata()) );
		
		// all other tabs are by Power name
		Power[] powers = world.getMap().getPowers();
		for(int i=0; i<powers.length; i++)
		{
			tabPane.add( powers[i].getName(),
						 makePlayerPanel(powers[i], world.getPlayerMetadata(powers[i])) );
		}
	}// makeTabPanel()
	
	/** Make a GameMetadata display */
	private GamePanel makeGamePanel(GameMetadata gmd)
	{
		GamePanel panel = new GamePanel();
		panel.read(gmd);
		panel.revalidate();
		return panel;
	}// makeGamePanel()
	
	
	/** Make a PlayerMetadata display */
	private JPanel makePlayerPanel(Power power, PlayerMetadata pmd)
	{
		PlayerPanel panel = new PlayerPanel(power);
		panel.read(pmd);
		panel.revalidate();
		return panel;
	}// makePlayerPanel()
	
	
	/** n2e = null-to-empty; If field may be null, allow field to be empty. 
		Converts all objects to strings. 
	*/
	private String n2e(Object in)
	{
		if(in == null)
		{
			return "";
		}
		
		return in.toString();
	}// n2e()
	
	
	private class GamePanel extends JPanel
	{
		private JTextPane notes = new JTextPane();
		private JTextField comment = new JTextField(COLUMNS_LONG);
		private JTextField gameName = new JTextField(COLUMNS_LONG);
		private JTextField modName = new JTextField(COLUMNS_LONG);
		private JTextField modEmail = Utils.createEmailTextField(COLUMNS_LONG);
		private JTextField modURI = Utils.createURITextField(COLUMNS_LONG);
		private JTextField judgeName = new JTextField(COLUMNS);
		private JTextField gameURI = Utils.createURITextField(COLUMNS_LONG);
		private JTextField gameID = new JTextField(COLUMNS);
		
		
		public GamePanel()
		{
			notes.setBorder(new EtchedBorder());
			
			// layout
			int w1[] = { BORDER, 10, 0, 5, 0, 10, 5, 0, BORDER };
			int h1[] = { BORDER, 0, 20, 0, 5, 0,10, 0,10, 0,10, 0,30, 0,10, 0,10, 0, BORDER };
			
			HIGLayout layout = new HIGLayout(w1, h1);
			layout.setColumnWeight(8,1);
			this.setLayout(layout);
			
			HIGConstraints c = new HIGConstraints();
			
			this.add(new JLabel(Utils.getLocalString(GDF_GAME_NAME)), c.rcwh(2,3,1,1,"r"));
			this.add(gameName, c.rcwh(2,5,1,1,"lr"));
			
			
			this.add(new JLabel(Utils.getLocalString(GDF_NOTES)), c.rcwh(4,8,1,1,"l"));
			this.add(makeScrollPane(notes), c.rcwh(6,8,1,14,"lrtb"));
						
			this.add(new JLabel(Utils.getLocalString(GDF_COMMENT)), c.rcwh(6,3,1,1,"r"));
			this.add(comment, c.rcwh(6,5,1,1,"lr"));
			
			this.add(new JLabel(Utils.getLocalString(GDF_GAME_URI)), c.rcwh(8,3,1,1,"r"));
			this.add(gameURI, c.rcwh(8,5,1,1,"lr"));
			
			this.add(new JLabel(Utils.getLocalString(GDF_GAME_ID)), c.rcwh(10,3,1,1,"r"));
			this.add(gameID, c.rcwh(10,5,1,1,"lr"));
			
			this.add(new JLabel(Utils.getLocalString(GDF_JUDGE_NAME)), c.rcwh(12,3,1,1,"r"));
			this.add(judgeName, c.rcwh(12,5,1,1,"lr"));
			
			
			this.add(new JLabel(Utils.getLocalString(GDF_MOD_NAME)), c.rcwh(14,3,1,1,"r"));
			this.add(modName, c.rcwh(14,5,1,1,"lr"));
			
			this.add(new JLabel(Utils.getLocalString(GDF_MOD_EMAIL)), c.rcwh(16,3,1,1,"r"));
			this.add(modEmail, c.rcwh(16,5,1,1,"lr"));
			
			this.add(new JLabel(Utils.getLocalString(GDF_MOD_URI)), c.rcwh(18,3,1,1,"r"));
			this.add(modURI, c.rcwh(18,5,1,1,"lr"));
		}// GamePanel()
		
		/** Get the panel values from the given GameMetadata object. */
		public void read(GameMetadata gmd)
		{
			notes.setText(gmd.getNotes());
			comment.setText(gmd.getComment());
			gameName.setText(gmd.getGameName());
			modName.setText(n2e(gmd.getModeratorName()));
			modEmail.setText(n2e(gmd.getModeratorEmail()));
			modURI.setText(n2e(gmd.getModeratorURI()));
			judgeName.setText(n2e(gmd.getJudgeName()));
			gameURI.setText(n2e(gmd.getGameURI()));
			gameID.setText(gmd.getGameID());
		}// read()
		
		
		/** Set GameMetadata object from the entered panel values. */
		public void write(GameMetadata gmd)
		{
			gmd.setNotes(notes.getText());
			gmd.setComment(comment.getText());
			gmd.setGameName(gameName.getText());
			gmd.setGameID(gameID.getText());
			gmd.setGameURI( convertURI(gameURI.getText()) );
			
			gmd.setJudgeName( (judgeName.getText().length() == 0) ? null : judgeName.getText() );
			
			gmd.setModeratorName( (modName.getText().length() == 0) ? null : modName.getText() );
			gmd.setModeratorEmail( (modEmail.getText().length() == 0) ? null : modEmail.getText() );
			gmd.setModeratorURI( convertURI(modURI.getText()) );
		}// write()
	}// inner class GamePanel
	
	
	private class PlayerPanel extends JPanel
	{
		private JTextPane notes = new JTextPane();
		private JTextField name = new JTextField(COLUMNS);
		private JTextField uri = Utils.createURITextField(COLUMNS);
		private JTextField[] email = new JTextField[4];
		private Power power = null;
		
		public PlayerPanel(Power power)
		{
			this.power = power;	
			
			notes.setBorder(new EtchedBorder());
			
			for(int i=0; i<email.length; i++)
			{
				email[i] = Utils.createEmailTextField(COLUMNS);
			}
			
			// layout
			int w1[] = { BORDER, 0, 5, 0, 15, 0, 5, 0, BORDER };
			int h1[] = { BORDER, 0,5, 0,5, 0,5, 0,5, 0,5, 10, 0,5, 0, BORDER };
			
			HIGLayout layout = new HIGLayout(w1, h1);
			layout.setColumnWeight(5,1);
			layout.setRowWeight(12, 1);
			this.setLayout(layout);
			
			HIGConstraints c = new HIGConstraints();
			
			this.add(new JLabel(Utils.getLocalString(PDF_NAME)), c.rcwh(2,2,1,1,"r"));
			this.add(name, c.rcwh(2,4,1,1,"l"));
			
			this.add(new JLabel(Utils.getLocalString(PDF_URI)), c.rcwh(4,2,1,1,"r"));
			this.add(uri, c.rcwh(4,4,1,1,"l"));
			
			this.add(new JLabel(Utils.getLocalString(PDF_NOTES)), c.rcwh(10,2,1,1,"l"));
			this.add(makeScrollPane(notes), c.rcwh(12,2,7,1,"lrtb"));
			
			for(int i=0; i<email.length; i++)
			{
				int row = 2 + (i*2);
				this.add(new JLabel(Utils.getLocalString(PDF_EMAIL)+" "+String.valueOf(i+1)), c.rcwh(row,6,1,1,"r"));
				this.add(email[i], c.rcwh(row,8,1,1,"l"));
			}
		}// PlayerPanel()
		
		/** Returns the Power associated with this PlayerPanel */
		public Power getPower()
		{
			return power;
		}// getPower()
		
		
		/** Get the panel values from the given PlayerMetadata object. */
		public void read(PlayerMetadata pmd)
		{
			notes.setText(pmd.getNotes());
			name.setText(pmd.getName());
			uri.setText(n2e(pmd.getURI()));
			
			String[] tmpEmail = pmd.getEmailAddresses();
			for(int i=0; i<email.length; i++)
			{
				if(i < tmpEmail.length)
				{
					email[i].setText( n2e(tmpEmail[i]) );
				}
				else
				{
					email[i].setText("");
				}
			}
		}// read()
		
		/** Write the panel values to the given PlayerMetadata object. */
		public void write(PlayerMetadata pmd)
		{
			pmd.setName( name.getText() );
			pmd.setURI( convertURI(uri.getText()) );
			pmd.setNotes( notes.getText() );
			
			String[] tmpEmail = new String[email.length];
			for(int i=0; i<email.length; i++)
			{
				tmpEmail[i] = email[i].getText().trim();
			}
			
			pmd.setEmailAddresses(tmpEmail);
		}// write()
	}// inner class PlayerPanel
	
	
	/** If can convert to URI, do it; otherwise returns null */
	private static URI convertURI(String in)
	{
		try
		{
			return new URI(in);
		}
		catch(URISyntaxException e)
		{
		}
		
		return null;
	}// convertURI()
	
	
	/** Creates a JScrollPane that can only scroll vertically. */
	private JScrollPane makeScrollPane(JComponent c)
	{
		JScrollPane jsp = new XJScrollPane(c);
		jsp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		return jsp;
	}// makeScrollPane()
	
	
	/** Listener to get Tab Icon colors */
	private class IconColorListener extends AbstractCFPListener
	{
		public void actionMMDReady(MapMetadata mmd)
		{
			MetadataDialog.this.mmd = mmd;
			setTabIcons();
		}// actionMMDReady()
	}// nested class IconColorListener
	
	
	/** Sets the tab icons for each power. */
	private void setTabIcons()
	{
		if(mmd != null)
		{
			final World world = clientFrame.getWorld();
			final int tabCount = tabPane.getTabCount();
		   	for(int i=1; i<tabCount; i++)	// no icon for 'game' info
            {
				Power power = world.getMap().getPower( tabPane.getTitleAt(i) );
				assert(power != null);
				String colorName = mmd.getPowerColor(power);
				Color color = SVGColorParser.parseColor(colorName);
				tabPane.setIconAt( i, new ColorRectIcon(12,12, color) );
           }
		}
	}// setTabIcons()
	
}// class MetadataDialog




