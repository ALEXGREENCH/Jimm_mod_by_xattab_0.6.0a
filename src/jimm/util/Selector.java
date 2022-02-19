/*******************************************************************************
 Jimm - Mobile Messaging - J2ME ICQ clone
 Copyright (C) 2003-05  Jimm Project

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
 File: src/jimm/util/Selector.java
 Version: ###VERSION###  Date: ###DATE###
 Author(s): Artyomov Denis, Denis K., Arvin
 *******************************************************************************/

package jimm.util;

import javax.microedition.lcdui.*;

import DrawControls.*;
import jimm.*;
import jimm.comm.XStatus;

public class Selector extends VirtualList implements VirtualListCommands
{
	static private int cols, rows, imgHeight, itemHeight, increment, curCol, imagesCount, selectorType;
	static private Selector _this;

	static public final int[] colorTable = new int[216];

	static
	{
		int idx = 0;
		for (int a0 = 0; a0 < 0x100; a0 += 0x33)
		{
			for (int a1 = 0; a1 < 0x100; a1 += 0x33)
			{
				for (int a2 = 0; a2 < 0x100; a2 += 0x33)
				{
					colorTable[idx] = (a0 * 0x10000 + a1 * 0x100 + a2);
					idx++;
				}
			}
		}
	}

	public Selector(int flag, int xstIndex)
	{
		super(null);
		_this = this;
		setVLCommands(this);
		selectorType = flag;

		int drawWidth = getWidth() - scrollerWidth;

		setMode(MODE_TEXT);

		switch (selectorType)
		{
		case 0:
			imgHeight = Emotions.images.getHeight();
			itemHeight = imgHeight + 2;
			break;
		case 1:
			imgHeight = XStatus.getXStatusImageList().getHeight();
			itemHeight = imgHeight + ((drawWidth > 176) ? 6 : 4);
			break;
		case 2:
			imgHeight = drawWidth / ((drawWidth < 150) ? 8 : 12);
			itemHeight = imgHeight;
			break;
		}

		cols = drawWidth / itemHeight;
		int index = getLength();

		rows = (index + cols - 1 ) / cols;
		_this.setCurrSelectedIdx(xstIndex);

		itemHeight = itemHeight + ((drawWidth - (itemHeight * cols)) / cols);
		increment = itemHeight - imgHeight;
		showCurrName();
	}
		
	//#sijapp cond.if target is "MIDP2"#
	protected boolean pointerPressedOnUtem(int index, int x, int y, int mode)
	{
		int lastCol = curCol; 
		curCol = x / itemHeight;
		if (curCol < 0) curCol = 0;
		if (curCol >= cols) curCol = cols - 1;
		if (lastCol != curCol)
		{
			showCurrName();
			invalidate();
		}
		return false;
	}
	//#sijapp cond.end#

	protected void drawItemData(Graphics g, int index, int x1, int y1, int x2, int y2, int fontHeight)
	{
		int xa, xb;
		int startIdx = cols * index;

		switch (selectorType)
		{
		case 0:
			imagesCount = Emotions.images.size();
			break;
		case 1:
			imagesCount = XStatus.getXStatusImageList().size();
			break;
		case 2:
			imagesCount = colorTable.length;
			break;
		}

		boolean isSelected = (index == getCurrIndex());
		int idx = getLength();
		xa = x1;
		for (int i = 0; i < cols; i++, startIdx++)
		{
			if (startIdx >= idx) break;
			
			xb = xa + itemHeight;

			if (isSelected && (i == curCol))
			{
				if (!Options.getBoolean(Options.OPTION_TRANS_CURSOR))
				{
					VirtualList.drawGradient(g, xa + 1, y1 + 1, itemHeight - 2, y2 - y1 - 2, jimm.Options.cursorColor, 16, -32, 0);
				}
				g.setColor(transformColorLight(jimm.Options.cursorColor, -48));
				g.drawRect(xa, y1, itemHeight - 1, y2 - y1 - 1);
			}

			if (startIdx < imagesCount)
			{
				switch (selectorType)
				{
				case 0:
					Emotions.images.elementAt(startIdx).drawInCenter(g, xa + itemHeight / 2, (y1 + y2) / 2);
					break;
				case 1:
					int offset = (increment / 2) + (increment % 2);
					int xstIndex = (startIdx == 0) ? XStatus.XSTATUS_NONE - 1 : startIdx - 1;
					XStatus.getStatusImage(xstIndex).drawImage(g, xa + offset, y1 + offset);
					break;
				case 2:
					g.setColor(colorTable[startIdx]);
					g.fillRect(xa + 2, y1 + 2, itemHeight - 4, itemHeight - 4);
					break;
				}
			}
			xa = xb;
		}
	}
		
	static private void showCurrName()
	{
		int selIdx = _this.getCurrIndex() * cols + curCol;

		switch (selectorType)
		{
		case 0:
			if (selIdx >= Emotions.selEmotionsSmileNames.length) return;
			Emotions.emotionText = Emotions.selEmotionsWord[selIdx];
			_this.setCaption(Emotions.selEmotionsSmileNames[selIdx]);
			break;
		case 1:
			if (selIdx > XStatus.getXStatusCount() + 1) return;
			int xstIndex = (selIdx == 0) ? XStatus.XSTATUS_NONE - 1 : selIdx - 1;
			_this.setCaption(XStatus.getStatusAsString(xstIndex));
			break;
		case 2:
			String tmpst = "00000" + (Integer.toHexString(colorTable[selIdx])).toUpperCase();
			_this.setCaption(tmpst.substring(tmpst.length() - 6));
			break;
		}
	}

	public int getItemHeight(int itemIndex)
	{
		return itemHeight;
	}

	protected int getSize()
	{
		return rows;
	}

	private int getLength()
	{
		switch (selectorType)
		{
		case 0:
			return Emotions.selEmotionsIndexes.length;
		case 1:
			return XStatus.getXStatusCount() + 1;
		case 2:
			return colorTable.length;
		}
		return -1;
	}

	public int getCurrSelectedIdx()
	{
		return _this.getCurrIndex() * cols + curCol;
	}

	public void setCurrSelectedIdx(int index)
	{
		_this.setCurrentItem(index / cols);
		curCol = index % cols;
		invalidate();
	}

	static public int getColorIndex(int color)
	{
		for (int i = 0; i < colorTable.length; i++)
		{
			if (color == colorTable[i]) return i;
		}
		return 0;
	}

	protected void get(int index, ListItem item) {}

	public void vlKeyPress(VirtualList sender, int keyCode, int type) 
	{
		int lastCol = curCol;
		int curRow = getCurrIndex();
		int rowCount = getSize();
		switch (getGameAction(keyCode))
		{
		case Canvas.LEFT:
			if (curCol != 0)
			{
				curCol--;
			}
			else if (curRow != 0)
			{
				curCol = cols - 1;
				curRow--;
			}
			else
			{
				curCol = (getLength() - 1) % cols;
				curRow = rowCount - 1;
			}
			break;

		case Canvas.RIGHT:
			if (curCol < (cols - 1))
			{
				curCol++;
			}
			else if (curRow <= rowCount)
			{
				curCol = 0;
				curRow++;
			}
			if ((curCol + curRow * cols) > (getLength() - 1))
			{
				curCol = 0;
				curRow = 0;
			}
			break;
		}

		setCurrentItem(curRow);

		int index = curCol + getCurrIndex() * cols;
		int idx = getLength();
		if (index >= idx) curCol = (idx - 1) % cols; 

		if (lastCol != curCol)
		{
			invalidate();
			showCurrName();
		}
	}

	public void vlCursorMoved(VirtualList sender) 
	{
		showCurrName();
	}

	public void vlItemClicked(VirtualList sender) {}
}