/*******************************************************************************
 Jimm - Mobile Messaging - J2ME ICQ clone
 Copyright (C) 2003-07  Jimm Project

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation; either version 2
 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 ********************************************************************************
 File: src/DrawControls/TextList.java
 Version: ###VERSION###  Date: ###DATE###
 Author(s): Artyomov Denis
 *******************************************************************************/

package DrawControls;

import java.util.Enumeration;
import java.util.Vector;
import java.util.Hashtable;
import javax.microedition.lcdui.*;

import DrawControls.VirtualList;
import DrawControls.ListItem;
import jimm.Options;

class TextItem
{
	public Icon image;
	public String text;
	private int fontAndColor = 0;
	private int itemHeigthAndWidth = 0;

	public int getHeight(int fontSize)
	{
		if (image != null) return image.getHeight();
		if (text == null) return 0;
		if ((itemHeigthAndWidth & 0xFFFF) == 0)
		{
			//#sijapp cond.if target is "MOTOROLA"#
			if (fontSize != TextList.LARGE_FONT) {
			//#sijapp cond.end#
			Font font = Font.getFont(Font.FACE_SYSTEM, (fontAndColor >> 24) & 0xFF, fontSize);
			itemHeigthAndWidth = (itemHeigthAndWidth & 0xFFFF0000) | font.getHeight();
			//#sijapp cond.if target is "MOTOROLA"#
			} else return TPropFont.font.getHeight();
			//#sijapp cond.end#
		}
		return itemHeigthAndWidth & 0xFFFF;
	}

	public int getWidth(int fontSize)
	{
		if (image != null) return image.getWidth() + 1;
		if (text == null) return 0;
		if ((itemHeigthAndWidth & 0xFFFF0000) == 0)
		{
			//#sijapp cond.if target is "MOTOROLA"#
			if (fontSize != TextList.LARGE_FONT) {
			//#sijapp cond.end#
			Font font = Font.getFont(Font.FACE_SYSTEM, (fontAndColor >> 24) & 0xFF, fontSize);
			itemHeigthAndWidth = (itemHeigthAndWidth & 0x0000FFFF) | (font.stringWidth(text) << 16);
			//#sijapp cond.if target is "MOTOROLA"#
			} else return TPropFont.font.getStringWidth(text);
			//#sijapp cond.end#
		}
		return (itemHeigthAndWidth & 0xFFFF0000) >> 16;
	}

	public int getColor()
	{
		return fontAndColor & 0xFFFFFF;
	}

	public void setColor(int value)
	{
		fontAndColor = (fontAndColor & 0xFF000000) | (value & 0x00FFFFFF); 
	}

	public int getFontStyle()
	{
		return (fontAndColor & 0xFF000000) >> 24;
	}

	public void setFontStyle(int value)
	{
		fontAndColor = (fontAndColor & 0x00FFFFFF) | ((value & 0xFF) << 24);
	}
}

class TextLine
{
	public Vector items = new Vector();
	int height = -1;
	int bigTextIndex = -1;
	char last_charaster;
	
	TextItem elementAt(int index)
	{
		return (TextItem)items.elementAt(index); 
	}
	
	void add(TextItem item)
	{
		items.addElement(item);
	}

	int getHeight(int fontSize)
	{
		if (height == -1)
		{
			//#sijapp cond.if target is "MOTOROLA"#
			height = (fontSize == TextList.LARGE_FONT) ? 0 : fontSize;
			//#sijapp cond.else#
			height = fontSize;
			//#sijapp cond.end#
			for (int i = items.size() - 1; i >= 0; i--)
			{
				int currHeight = elementAt(i).getHeight(fontSize);
				if (currHeight > height) height = currHeight; 
			}
		}
		return height;
	}

	int getWidth(int fontSize)
	{
		int width = 0;
		for (int i = items.size() - 1; i >= 0; i--) width += elementAt(i).getWidth(fontSize);  
		return width;
	}

	void setItemColor(int value)
	{
		for (int i = items.size() - 1; i >= 0; i--)
		{
			TextItem listItem = elementAt(i);
			listItem.setColor(value);
		}
	}

	void paint(int xpos, int ypos, Graphics g, int fontSize, VirtualList vl)
	{
		int count = items.size(); 
		int intemHeight = getHeight(fontSize);

		for (int i = 0; i < count; i++)
		{
			TextItem item = elementAt(i);
			int drawYPos = ypos + (intemHeight - item.getHeight(fontSize)) / 2;

			if (item.image != null)
			{
				xpos++; // смещение на 1 пиксель, чтобы картинка и текст не были вплотную...
				item.image.drawImage(g, xpos, drawYPos);
			}
			else if (item.text != null)
			{
				//#sijapp cond.if target is "MOTOROLA"#
				if (fontSize != TextList.LARGE_FONT) {
				//#sijapp cond.end#
				g.setColor(item.getColor());
				g.setFont(vl.getQuickFont(item.getFontStyle()));
				g.drawString(item.text, xpos, drawYPos, Graphics.TOP | Graphics.LEFT);
				//#sijapp cond.if target is "MOTOROLA"#
				} else TPropFont.font.drawString(g, xpos, drawYPos, item.text, item.getColor());
				//#sijapp cond.end#
			}
			xpos += item.getWidth(fontSize);
		}
	}

	int size()
	{
		return items.size();
	}

	void readText(StringBuffer buffer)
	{
		for (int i = 0; i < items.size(); i++) buffer.append(elementAt(i).text);
	}
}

/* This class store text and data of lines internally. You may use it to show text with colorised lines */
public class TextList extends VirtualList
{
	// Construct new text list 
	public TextList(String capt, int capTextColor, int backColor, int fontSize, int cursorMode)
	{
		super(capt, capTextColor, backColor, fontSize, cursorMode);
	}
	
	// Construct new text list with default values of colors, font size etc...
	public TextList(String capt)
	{
		super(capt);
	}

	// Vector of lines. Each line contains cols. Col can be text or image
	private Vector lines = new Vector();

	// Sent messages table (id -> index)
	private Hashtable msgTable = new Hashtable();

	public int getSize()
	{
		if (lines.isEmpty()) return 0;
		int size = lines.size();
		return (((TextLine)lines.lastElement()).size() == 0) ? size - 1 : size;
	}

	private TextLine getLine(int index)
	{
		return (TextLine)lines.elementAt(index);
	}

	protected boolean isItemSelected(int index)
	{
		int selIndex = getCurrIndex();
		int textIndex = (selIndex >= lines.size()) ? -1 : getLine(selIndex).bigTextIndex;
		if (textIndex == -1) return false;
		return (getLine(index).bigTextIndex == textIndex);
	}

	protected void get(int index, ListItem item)
	{
		TextLine listItem = getLine(index);
		item.clear();
		if (listItem.size() == 0) return;

		TextItem titem = listItem.elementAt(0);
		item.text = titem.text;
		item.color = titem.getColor();
		item.fontStyle = titem.getFontStyle();
	}

	// Remove all lines form list
	public void clear()
	{
		lines.removeAllElements();
		msgTable.clear();
		setCurrentItem(0);
		invalidate();
	}

	// Add new text item to list
	public void add (String text, int color, int imageIndex)
	{
		internAdd(text, color, imageIndex, Font.STYLE_PLAIN, -1, true, '\0');
		invalidate();
	}

	// Add new text item to list
	public void add(String text, int color, int imageIndex, int fontStyle)
	{
		internAdd(text, color, imageIndex, fontStyle, -1, true, '\0');
		invalidate();
	}

	private void internAdd(String  text, int color, int imageIndex, int fontStyle, int textIndex, boolean doCRLF, char last_charaster)
	{
		TextItem newItem = new TextItem();

		newItem.text = text;
		newItem.setColor(color);
		newItem.setFontStyle(fontStyle);

		if (lines.isEmpty()) lines.addElement(new TextLine());
		TextLine textLine = (TextLine) lines.lastElement();
		textLine.add(newItem);
		textLine.bigTextIndex = textIndex;
		if (doCRLF)
		{
			textLine.last_charaster = last_charaster;
			TextLine newLine = new TextLine();
			newLine.bigTextIndex = textIndex; 
			lines.addElement(newLine);
		}
	}

	// Add new black text item to list
	public void add(String text)
	{
		add(text, this.getTextColor(), -1);
	}

	public int getItemHeight(int itemIndex)
	{
		if (getCursorMode() != MODE_TEXT) return super.getItemHeight(itemIndex);
		if (itemIndex >= lines.size()) return 1;
		return getLine(itemIndex).getHeight(getFontSize());
	}
	
	// Overrides VirtualList.drawItemData
	protected void drawItemData(Graphics g, int index, int x1, int y1, int x2, int y2, int fontHeight)
	{
		if (getCursorMode() != MODE_TEXT)
		{
			super.drawItemData(g, index, x1, y1, x2, y2, fontHeight);
			return;
		}
		
		TextLine line = getLine(index);
		line.paint(borderWidth, y1, g, getFontSize(), this);
	}

	// Overrides VirtualList.moveCursor
	protected void moveCursor(int step, boolean moveTop)
	{
		int size, changeCounter = 0, currTextIndex, i, halfSize = getVisCount() / 2;

		switch (step)
		{
		case -1:
		case 1:
			currTextIndex = getCurrTextIndex();
			size = getSize();
			storelastItemIndexes();

			for (i = 0; i < halfSize;)
			{
				currItem += step;
				if ((currItem < 0) || (currItem >= size))
				{
					if (changeCounter != 0) currItem -= step; // это арти зачем то добавил...
					break;
				}
				TextLine item = getLine(currItem);
				if (currTextIndex != item.bigTextIndex)
				{
					currTextIndex = item.bigTextIndex;
					changeCounter++;
					if ((changeCounter == 2) || (!visibleItem(currItem) && (i > 0)))
					{
						currItem -= step;
						break;
					}
				}

				if (!visibleItem(currItem) || (changeCounter != 0)) i++;
			}
			checkCurrItem(step);
			checkTopItem();
//			checkTopItem(); // имхо, лишнее оно тут...
			repaintIfLastIndexesChanged();
			break;

		default:
			super.moveCursor(step, moveTop);
			return;
		}
	}

	public String getTextByIndex(int offset, boolean wholeText, int textIndex)
	{
		StringBuffer result = new StringBuffer();
		
		// Fills the lines
		int size = lines.size();
		for (int i = 0; i < size; i++)
		{
			TextLine line = getLine(i);
			if (wholeText || (textIndex == -1) || (line.bigTextIndex == textIndex))
			{
				line.readText(result);
				if (line.last_charaster != '\0')
				{
					if (line.last_charaster == '\n') result.append("\n");
					else result.append(line.last_charaster);
				}
			}
		}
		
		if (result.length() == 0) return null;
		String resultText = result.toString();
		int len = resultText.length();
		if (offset > len) return null;
		return resultText.substring(offset, len);
	}

	public void selectTextByIndex(int textIndex)
	{
		if (textIndex == -1) return;
		int size = lines.size();
		for (int i = 0; i < size; i++)
		{
			if (getLine(i).bigTextIndex == textIndex)
			{
				setCurrentItem(i);
				break;
			}
		}
	}

	// Returns lines of text which were added by methon addBigText in current selection
	public String getCurrText(int offset, boolean wholeText)
	{
		return getTextByIndex(offset, wholeText, getCurrTextIndex());
	}

	public int getCurrTextIndex()
	{
		int currItemIndex = getCurrIndex();
		if ((currItemIndex < 0) || (currItemIndex >= lines.size())) return -1;
		return getLine(currItemIndex).bigTextIndex;
	}

	public void setColors(int capTxt, int capbk, int bkgrnd, int text)
	{
		super.setColors(capTxt, capbk, bkgrnd, text);
	}

	public TextList doCRLF(int blockTextIndex)
	{
		if (lines.size() != 0) ((TextLine) lines.lastElement()).last_charaster = '\n';
		TextLine newLine = new TextLine();
		newLine.bigTextIndex = blockTextIndex; 
		lines.addElement(newLine);
		return this;
	}

	public void AckMessage(long msgId, Icon newImage, boolean removeRecord)
	{
		Long msgIdObject = new Long(msgId);

		if (msgTable.containsKey(msgIdObject))
		{
			int index = ((Integer)msgTable.get(msgIdObject)).intValue();
			if (index < lines.size())
			{
				TextLine textLine = (TextLine) lines.elementAt(index);
				if (textLine.size() > 0)
				{
					TextItem item = (TextItem)textLine.items.elementAt(0);

					if (item.image != null)
					{
						item.image = newImage;
						invalidate();
					}
				}
			}

			if (removeRecord)
			{
				msgTable.remove(msgIdObject);
			}
		}
	}

	public void addMsgHeaderImage(Icon image, int blockTextIndex, long msgId)
	{
		if (lines.isEmpty()) lines.addElement(new TextLine());
		TextLine textLine = (TextLine) lines.lastElement();
		textLine.bigTextIndex = blockTextIndex;

		if (textLine.size() > 0)
		{
			doCRLF(blockTextIndex);
			textLine = (TextLine) lines.lastElement();
		}

		TextItem newItem = new TextItem();
		newItem.image = image;
		newItem.text = "";
		textLine.add(newItem);

		// add msgTable record
		if (msgId != 0)
		{
			int lineIndex = lines.size() - 1;
			msgTable.put(new Long(msgId), new Integer(lineIndex));
		}
	}

	public TextList addImage(Icon image, String altarnateText, int blockTextIndex)
	{
		if (lines.isEmpty()) lines.addElement(new TextLine());
		TextLine textLine = (TextLine) lines.lastElement();
		textLine.bigTextIndex = blockTextIndex; 
		
		if ((textLine.getWidth(getFontSize()) + image.getWidth()) > getTextAreaWidth())
		{
			doCRLF(blockTextIndex);
			textLine = (TextLine)lines.lastElement();
		}
		
		TextItem newItem = new TextItem();
		newItem.image = image;
		newItem.text = altarnateText;
		textLine.add(newItem);

		return this;
	}

	private int getTextAreaWidth()
	{
		return getWidthInternal() - scrollerWidth - borderWidth * 2;
	}

	private static String replace(String text, String from, String to)
	{
		int fromSize = from.length();
		for (;;)
		{
			int pos = text.indexOf(from); 
			if (pos == -1) break;
			text = text.substring(0, pos) + to + text.substring(pos + fromSize, text.length());
		}
		return text;
	}

	private void addBigTextInternal(String text, int color, int fontStyle, int textIndex, int trueWidth)
	{
		Font font;
		int textLen, curPos, lastWordEnd, startPos, width, testStringWidth = 0;
		char curChar;
		boolean lineBreak, wordEnd, textEnd, divideLineToWords;
		String testString = null;

		if (text == null) return;

		// Replace '\r\n' charasters with '\n'
		text = replace(text, "\r\n", "\n");
		
		// Replace '\r' charasters with '\n'
		text = replace(text, "\r", "\n");

		font = getQuickFont(fontStyle);

		// Width of free space in last line 
		width = lines.isEmpty() ? trueWidth : trueWidth - ((TextLine)lines.lastElement()).getWidth(getFontSize());

		// Start pos of new line
		startPos = 0;

		// Pos of last word end
		lastWordEnd = -1;
		
		textLen = text.length();
		for (curPos = 0; curPos < textLen;)
		{
			curChar = text.charAt(curPos);
			wordEnd = (curChar == ' ');
			lineBreak = (curChar == '\n');
			textEnd = (curPos == (textLen - 1));
			divideLineToWords = false;
			if (textEnd && (!lineBreak)) curPos++;

			if (lineBreak || textEnd || wordEnd)
			{
				testString = text.substring(startPos, curPos);
				//#sijapp cond.if target is "MOTOROLA"#
				if (getFontSize() != TextList.LARGE_FONT)
				//#sijapp cond.end#
				testStringWidth = font.stringWidth(testString);
				//#sijapp cond.if target is "MOTOROLA"#
				else testStringWidth = TPropFont.font.getStringWidth(testString);
				//#sijapp cond.end#
			}

			// simply add line
			if ((lineBreak || textEnd) && (testStringWidth <= width)) 
			{
				internAdd(testString, color, -1, fontStyle, textIndex, lineBreak, lineBreak ? '\n' : ' ');
				width = trueWidth;
				curPos++;
				startPos = curPos;
				lastWordEnd = -1;
				continue;
			}

			if ((lineBreak || textEnd || wordEnd) && (testStringWidth > width))
			{
				if ((testStringWidth < trueWidth) && (lastWordEnd != -1))
				{
					divideLineToWords = true;
				}
				// Insert new line and try again
				else if ((trueWidth != width) && (lastWordEnd == -1))
				{
					doCRLF(textIndex);
					curPos = startPos;
					width = trueWidth;
					lastWordEnd = -1;
					continue;
				}
			}

			if ((lineBreak || textEnd || wordEnd) && (testStringWidth > trueWidth) && (!divideLineToWords))
			{
				// divide big word to several lines
				if (lastWordEnd == -1)
				{
					for (; curPos >= 1; curPos--)
					{
						testString = text.substring(startPos, curPos);
						//#sijapp cond.if target is "MOTOROLA"#
						int tmp = 0;
						if (getFontSize() != TextList.LARGE_FONT) tmp = font.stringWidth(testString);
						else tmp = TPropFont.font.getStringWidth(testString);
						if (tmp <= width) break;
						//#sijapp cond.else#
						if (font.stringWidth(testString) <= width) break;
						//#sijapp cond.end#
					}
					internAdd(testString, color, -1, fontStyle, textIndex, true, '\0');
					width = trueWidth;
					startPos = curPos;
					lastWordEnd = -1;
					continue;
				}
				// several words in line
				else
				{
					divideLineToWords = true;
				}
			}

			if (divideLineToWords)
			{
				if (Options.getBoolean(Options.OPTION_WORD_WRAP))
				{
					int divPos = curPos;
					for (; divPos >= 1; divPos--)
					{
						testString = text.substring(startPos, divPos);
						//#sijapp cond.if target is "MOTOROLA"#
						if (getFontSize() != TextList.LARGE_FONT)
						//#sijapp cond.else#
						if (getFontSize() != TextList.SMALL_FONT)
						//#sijapp cond.end#
						{
							testStringWidth = font.stringWidth(testString);
						}
						else
						{
							//#sijapp cond.if target is "MOTOROLA"#
							testStringWidth = TPropFont.font.getStringWidth(testString);
							//#sijapp cond.else#
							testStringWidth = font.stringWidth(testString);
							//#sijapp cond.end#
						}
						if (testStringWidth <= width) break;
					}
					if (divPos - (lastWordEnd + 1) > 1)
					{
						String insString = text.substring(startPos, divPos);
						internAdd(insString, color, -1, fontStyle, textIndex, true, '\0');
						curPos = divPos;
					}
					else
					{
						String insString = text.substring(startPos, lastWordEnd);
						internAdd(insString, color, -1, fontStyle, textIndex, true, ' ');
						curPos = lastWordEnd + 1;
					}
				}
				else
				{
					String insString = text.substring(startPos, lastWordEnd);
					internAdd(insString, color, -1, fontStyle, textIndex, true, ' ');
					curPos = lastWordEnd + 1;
				}
				startPos = curPos;
				width = trueWidth;
				lastWordEnd = -1;
				continue;
			}

			if (wordEnd) lastWordEnd = curPos;
			curPos++;
		}
	}

	/* Add big multiline text */
	/* Text visial width can be larger then screen width */
	/* Method addBigText automatically divides text to short lines and adds lines to text list */
	public TextList addBigText (String text, int color, int fontStyle, int textIndex)
	{
		addBigTextInternal(text, color, fontStyle, textIndex, getTextAreaWidth());
		invalidate();
		return this;
	}

	static public int getLineNumbers(String s, int width, int fontSize, int fontStyle, int textColor)
	{
		TextList paintList = new TextList(null);
		paintList.setFontSize(fontSize);
		paintList.addBigTextInternal(s, textColor, fontStyle, -1, width);
		return (paintList.getSize());
	}

	static public void showText(Graphics g, String s, int x, int y, int width, int height, int fontSize, int fontStyle, int textColor)
	{
		TextList paintList = new TextList(null);
		paintList.setFontSize(fontSize);
		paintList.addBigTextInternal(s, textColor, fontStyle, -1, width);
		
		int line, textHeight = 0;
		int linesCount = paintList.getSize();
		for (line = 0; line < linesCount; line++) textHeight += paintList.getLine(line).getHeight(fontSize);
		int top = y + (height - textHeight) / 2;
		for (line = 0; line < linesCount; line++)
		{
			paintList.getLine(line).paint(x, top, g, fontSize, paintList);
			top += paintList.getLine(line).getHeight(fontSize);
		}
	}
}