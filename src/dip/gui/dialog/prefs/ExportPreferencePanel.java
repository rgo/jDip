//
//  @(#)ExportPreferencePanel.java		10/2003
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

package dip.gui.dialog.prefs;

import dip.gui.ClientFrame;
import dip.gui.swing.GradientJLabel;
import dip.gui.swing.AssocJComboBox;
import dip.order.OrderFormat;
import dip.order.OrderFormatOptions;
import dip.misc.SharedPrefs;
import dip.misc.Utils;

import org.apache.batik.transcoder.Transcoder;
import org.apache.batik.transcoder.image.ImageTranscoder;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.batik.transcoder.image.JPEGTranscoder;

// HIGLayout
import cz.autel.dmi.HIGConstraints;
import cz.autel.dmi.HIGLayout;

import java.util.prefs.Preferences;
import java.util.prefs.BackingStoreException;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import java.awt.Component;

import javax.swing.*;
import java.awt.*;


/**
*	Export preferences
*	<p>
*	Static methods are included to access (in a controlled manner) preference-controlled
*	functionality.
*
*
*/
public class ExportPreferencePanel extends PreferencePanel
{
	// i18n keys
	private static final String I18N_TAB_NAME 				= "ExPP.tab.name";  
	private static final String I18N_CAT_SIZE 				= "ExPP.category.size";
	private static final String I18N_CAT_SETTINGS	 		= "ExPP.category.settings";
	private static final String I18N_CHOICE_SIZE_FULL 		= "ExPP.choice.size.full";
	private static final String I18N_CHOICE_SIZE_DEFINED 	= "ExPP.choice.size.defined";
	private static final String I18N_CHOICE_SIZE_HEIGHTONLY	= "ExPP.choice.size.heightonly";
	private static final String I18N_CHOICE_SIZE_WIDTHONLY 	= "ExPP.choice.size.widthonly";
	private static final String I18N_CHOICE_JPG_QUALITY 	= "ExPP.choice.jpg.quality";
	private static final String I18N_CHOICE_PNG_BPP 		= "ExPP.choice.png.bpp";
	private static final String I18N_CHOICE_PNG_BPP_UNLIM	= "ExPP.choice.png.bpp.unlimited";
	
	
	// Preference Node Keys
	private static final String NODE_EXPORT_WIDTH 			= "export.size.width";			// int >0
	private static final String NODE_EXPORT_HEIGHT			= "export.size.height";			// int >0
	private static final String NODE_EXPORT_JPG_QUALITY		= "export.jpg.quality";			// float 0.0 to 1.0
	private static final String NODE_PNG_BPP			 	= "export.png.bpp";				// int 0/1/2/4/8
	
	// default values
	private static final float 	DEFAULT_JPG_QUALITY			= 0.8f;
	private static final int 	MAX_IMG_SIZE				= 4096;
	
	
	// misc
	private static final int BORDER = 10;
	private static final int INDENT = 20;
	private ClientFrame	clientFrame = null;
	
	// GUI items
	private JSlider 	jpgQuality;
	private AssocJComboBox 	pngBPP;
	private JSpinner	imgWidth;
	private JSpinner	imgHeight;
	private JSpinner	imgWidthOnly;
	private JSpinner	imgHeightOnly;
	private JRadioButton[]	jrb;
	
	// JRadioButton array index constants (1 for each radiobutton)
	private static final int JRB_ARRAY_LENGTH = 4;
	
	private static final int JRB_FULLSIZE = 0;
	private static final int JRB_IMGSIZE = 1;
	private static final int JRB_WIDTH_ONLY = 2;
	private static final int JRB_HEIGHT_ONLY = 3;
	
	// allowd bpp (indexed to combobox; 0 == unlimited)
	private static final int[] ALLOWED_PNG_BPP = {0,1,2,4,8};
	
	
	
	/** Create an Export Preference Panel */
	public ExportPreferencePanel(final ClientFrame cf)
	{
		super();
		clientFrame = cf;
		
		// create GUI components
		makeAndSetGUIItems();
		
		
		// layout 
		int h1[] = { BORDER, 0,5,  0,1, 0,1, 0,1, 0,(BORDER*2),   0,5,  0,1, 0,1,   0, BORDER };
		int w1[] = { BORDER, INDENT, 0, 0, BORDER };
		
		HIGLayout l1 = new HIGLayout(w1, h1);
		l1.setColumnWeight(4, 1);
		l1.setRowWeight(18, 1);
		setLayout(l1);
		
		HIGConstraints c = new HIGConstraints();
		
		// full size
		// add text label to first JRadioButton
		jrb[0].setText(Utils.getLocalString(I18N_CHOICE_SIZE_FULL));
		
		// category + choices
		add(new GradientJLabel(Utils.getLocalString(I18N_CAT_SIZE)), c.rcwh(2,2,3,1,"lr"));
		add(jrb[0], c.rcwh(4,3,3,1));	// this one has a text label; it's wider
		add(jrb[1], c.rc(6,3));
		add(jrb[2], c.rc(8,3));
		add(jrb[3], c.rc(10,3));
		
		// full size
		//add(new JLabel(Utils.getLocalString(I18N_CHOICE_SIZE_FULL)), c.rcwh(4,4,1,1,"l"));
		
		// constrained size
		JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		panel.add(imgWidth);
		panel.add(new JLabel(" x "));
		panel.add(imgHeight);
		panel.add(new JLabel(Utils.getLocalString(I18N_CHOICE_SIZE_DEFINED)));
		add(panel, c.rcwh(6,4,1,1,"l"));
		
		// width-locked
		panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		panel.add(imgWidthOnly);
		panel.add(new JLabel(Utils.getLocalString(I18N_CHOICE_SIZE_WIDTHONLY)));
		add(panel, c.rcwh(8,4,1,1,"l"));
		
		// height-locked
		panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		panel.add(imgHeightOnly);
		panel.add(new JLabel(Utils.getLocalString(I18N_CHOICE_SIZE_HEIGHTONLY)));
		add(panel, c.rcwh(10,4,1,1,"l"));
		
		
		// category + choices
		add(new GradientJLabel(Utils.getLocalString(I18N_CAT_SETTINGS)), c.rcwh(12,2,3,1,"lr"));
		
		panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		panel.add(new JLabel(Utils.getLocalString(I18N_CHOICE_JPG_QUALITY)));
		panel.add(jpgQuality);
		add(panel, c.rcwh(14,3,2,1,"l"));
		
		panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		panel.add(new JLabel(Utils.getLocalString(I18N_CHOICE_PNG_BPP)));
		panel.add(pngBPP);
		add(panel, c.rcwh(16,3,2,1,"l"));
	}// ExportPreferencePanel()
	
	
	
	/** 
	*	Create and Initialize the GUI items 
	*/
	private void makeAndSetGUIItems()
	{
		Preferences prefs = SharedPrefs.getUserNode();
		try { prefs.sync(); } catch (BackingStoreException bse) {}
		
		// get hints
		int w = prefs.getInt(NODE_EXPORT_WIDTH, 0);
		int h = prefs.getInt(NODE_EXPORT_HEIGHT, 0);
		float q = prefs.getFloat(NODE_EXPORT_JPG_QUALITY, DEFAULT_JPG_QUALITY);
		int bpp = prefs.getInt(NODE_PNG_BPP, 0);
		
		// check hints
		w = (w < 0 || w > MAX_IMG_SIZE) ? 0 : w;
		h = (h < 0 || h > MAX_IMG_SIZE) ? 0 : h;
		q = (q < 0.0f || q > 1.0f) ? DEFAULT_JPG_QUALITY : q;
		
		// jpg quality slider (0-100 int)
		jpgQuality = new JSlider(JSlider.HORIZONTAL, 0, 100, 100);
		jpgQuality.setMajorTickSpacing(10);
		jpgQuality.setMinorTickSpacing(5);
		jpgQuality.setPaintTicks(true);
		jpgQuality.setPaintTrack(true);
		jpgQuality.setPaintLabels(true);
		jpgQuality.setValue( (int) (q*100.0) );
		
		
		// png BPP combobox
		Integer[] bppVals = new Integer[ALLOWED_PNG_BPP.length];
		String[] bppText = new String[ALLOWED_PNG_BPP.length];
		bppVals[0] = new Integer(0);
		bppText[0] = Utils.getLocalString(I18N_CHOICE_PNG_BPP_UNLIM);
		for(int i=1; i<bppVals.length; i++)
		{
			bppVals[i] = new Integer(ALLOWED_PNG_BPP[i]);
			bppText[i] = bppVals[i].toString();
		}
		
		AssocJComboBox.AssociatedObj[] assocObjs = 
			AssocJComboBox.AssociatedObj.createAssociatedObjects(bppVals, 
				bppText, bppVals[0], true);
		pngBPP = new AssocJComboBox(assocObjs);
		pngBPP.reset();
		pngBPP.setSelectedItem(new Integer(bpp));
		
		// spinners: value, min, max, step
		imgWidth = new JSpinner(new SpinnerNumberModel(0, 0, MAX_IMG_SIZE, 1));
		imgHeight = new JSpinner(new SpinnerNumberModel(0, 0, MAX_IMG_SIZE, 1));
		imgWidthOnly = new JSpinner(new SpinnerNumberModel(0, 0, MAX_IMG_SIZE, 1));
		imgHeightOnly = new JSpinner(new SpinnerNumberModel(0, 0, MAX_IMG_SIZE, 1));
		
		
		// create JRadiobutton group; and set the selected button.
		ButtonGroup bg = new ButtonGroup();
		jrb = new JRadioButton[JRB_ARRAY_LENGTH];
		for(int i=0; i<jrb.length; i++)
		{
			jrb[i] = new JRadioButton();
			bg.add(jrb[i]);
		}
		
		// set correct width/height spinners
		// and return the correct RadioButton index
		if(w == 0 && h == 0)
		{
			jrb[JRB_FULLSIZE].setSelected(true);
		}
		else if(w > 0 && h > 0)
		{
			imgWidth.setValue(new Integer(w));
			imgHeight.setValue(new Integer(h));
			jrb[JRB_IMGSIZE].setSelected(true);
		}
		else if(w > 0 && h == 0)
		{
			imgWidthOnly.setValue(new Integer(w));
			jrb[JRB_WIDTH_ONLY].setSelected(true);
		}
		else if(h > 0 && w == 0)
		{
			imgHeightOnly.setValue(new Integer(h));
			jrb[JRB_HEIGHT_ONLY].setSelected(true);
		}
		else
		{
			throw new IllegalStateException();
		}
	}// makeAndSetGUIItems()
	
	
	
	
	public String getName()
	{
		return Utils.getLocalString(I18N_TAB_NAME);
	}// getName()
	
	
	public void apply()
	{
		Preferences prefs = SharedPrefs.getUserNode();
		
		// set preference nodes
		prefs.putInt(NODE_PNG_BPP, ((Integer) pngBPP.getSelectedValue()).intValue());
		prefs.putFloat(NODE_EXPORT_JPG_QUALITY, ((float) jpgQuality.getValue() / 100.0f) );
		
		int w = 0;
		int h = 0;
		// if(FULL_SIZE) ... keep w, h both 0
		if(jrb[JRB_IMGSIZE].isSelected())
		{
			w = ((Integer) imgWidth.getValue()).intValue();
			h = ((Integer) imgHeight.getValue()).intValue();
		}
		else if(jrb[JRB_WIDTH_ONLY].isSelected())
		{
			w = ((Integer) imgWidthOnly.getValue()).intValue();
		}
		else if(jrb[JRB_HEIGHT_ONLY].isSelected())
		{
			h = ((Integer) imgHeightOnly.getValue()).intValue();
		}
		prefs.putInt(NODE_EXPORT_WIDTH, w);
		prefs.putInt(NODE_EXPORT_HEIGHT, h);
		
		try { prefs.flush(); } catch (BackingStoreException bse) {}
	}// apply()
	
	
	public void cancel()
	{
		// do nothing
	}// cancel()
	
	
	public void setDefault()
	{
		jrb[JRB_FULLSIZE].setSelected(true);
		pngBPP.setSelectedIndex(0);
		jpgQuality.setValue( (int) (DEFAULT_JPG_QUALITY*100.0f) );
	}// setDefault()
	
	
	/**
	*	Get the stored export preferences, using sensible defaults 
	*	if values are missing or invalid. This takes the passed Transcoder
	*	object and adds hints via Transcoder.addTranscodingHint().
	*/
	public static void applyTranscodingHints(Transcoder transcoder)
	{
		if(transcoder == null) { throw new IllegalArgumentException(); }
		
		Preferences prefs = SharedPrefs.getUserNode();
		try { prefs.sync(); } catch (BackingStoreException bse) {}
		
		// get hints
		int w = prefs.getInt(NODE_EXPORT_WIDTH, 0);
		int h = prefs.getInt(NODE_EXPORT_HEIGHT, 0);
		float q = prefs.getFloat(NODE_EXPORT_JPG_QUALITY, DEFAULT_JPG_QUALITY);
		int bpp = prefs.getInt(NODE_PNG_BPP, 0);
		
		// check hints
		w = (w < 0 || w > MAX_IMG_SIZE) ? 0 : w;
		h = (h < 0 || h > MAX_IMG_SIZE) ? 0 : h;
		q = (q < 0.0f || q > 1.0f) ? DEFAULT_JPG_QUALITY : q;
		
		// add hints
		if(w != 0)
		{
			transcoder.addTranscodingHint(ImageTranscoder.KEY_WIDTH, new Float(w));
		}
		
		if(h != 0)
		{
			transcoder.addTranscodingHint(ImageTranscoder.KEY_HEIGHT, new Float(h));
		}
		
		if(bpp > 0)
		{
			transcoder.addTranscodingHint(PNGTranscoder.KEY_INDEXED, new Integer(bpp));
		}
		
		transcoder.addTranscodingHint(JPEGTranscoder.KEY_QUALITY, new Float(q));
	}// applyTranscodingHints()
	
}// class ExportPreferencePanel
