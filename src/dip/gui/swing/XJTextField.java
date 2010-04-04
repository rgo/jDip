//
//  @(#)XJTextPane.java	3/2003
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
package dip.gui.swing;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Font;
import javax.swing.JTextField;
import javax.swing.text.*;

/**
*	eXtended JTextField.
*	<p>
*	Converts unicode arrows (\u2192) to "->" arrows, 
*	if the component font is not Unicode-aware.
*/
public class XJTextField extends JTextField
{
	private boolean isUnicodeArrowAware = false;
	
	public XJTextField()
	{
        this(null, 0);
	}
	
	public XJTextField(int columns)
	{
       this(null, columns);
 	}
	
	public XJTextField(String text)
	{
       this(text, 0);
 	}
	
	public XJTextField(String text, int columns) 
	{
 		super(null, columns);
		
		AbstractDocument doc = (AbstractDocument) getDocument();
		doc.setDocumentFilter(new DocumentFilter()
		{
			public void insertString(DocumentFilter.FilterBypass fb, int offset, String text, AttributeSet attr)
			throws BadLocationException
			{
				replace(fb, offset, 0, text, attr);
			}// insertString()
			
			public void replace(DocumentFilter.FilterBypass fb, int offset, int length, String text, AttributeSet attr)
			throws BadLocationException
			{
				if(!XJTextField.this.isUnicodeAware())
				{
					fb.replace(offset, length, getFixedString(text), attr);
				}
				else
				{
					super.replace(fb, offset, length, text, attr);
				}
			}// replace()
			
			private String getFixedString(String in)
			{
				StringBuffer buffer = new StringBuffer(in);
				
				for(int i=buffer.length()-1; i>=0; i--)
				{
					final char c = buffer.charAt(i);
					
					if(c == '\u2192')
					{
						buffer.deleteCharAt(i);
						buffer.insert(i, "->");
					}
				}
				
				return buffer.toString();
			}// getValidURLString()
			
			
		});
		
		setText(text);
		
		if(text != null)
		{
			setText(text);
		}
	}
	
	public void setFont(Font f)
	{
		super.setFont(f);
		detectUnicode();
	}// setFont()
	
	/** Detect if font is unicode-aware */
	private void detectUnicode()
	{
		isUnicodeArrowAware = getFont().canDisplay('\u2192');
	}// detectUnicode()
	
	/** Returns if parent is unicode-aware */
	private boolean isUnicodeAware()
	{
		return isUnicodeArrowAware;
	}// isUnicodeAware()
	
}// class XJTextField

