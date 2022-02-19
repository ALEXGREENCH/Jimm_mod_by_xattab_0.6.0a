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
 File: src/jimm/comm/XStatus.java
 Version: ###VERSION###  Date: ###DATE###
 Author(s): aspro
 *******************************************************************************/

package jimm.comm;

import java.io.IOException;

import DrawControls.Icon;
import DrawControls.ImageList;
import jimm.util.ResourceBundle;

public class XStatus
{
	public static final int XSTATUS_NONE = 37;

	private static final GUID[] xguids =
	{
		new GUID(Util.explodeToBytes("63,62,73,37,A0,3F,49,FF,80,E5,F7,09,CD,E0,A4,EE", ',', 16)), // SHOPPING
		new GUID(Util.explodeToBytes("5A,58,1E,A1,E5,80,43,0C,A0,6F,61,22,98,B7,E4,C7", ',', 16)), // DUCK
		new GUID(Util.explodeToBytes("83,C9,B7,8E,77,E7,43,78,B2,C5,FB,6C,FC,C3,5B,EC", ',', 16)), // TIRED
		new GUID(Util.explodeToBytes("E6,01,E4,1C,33,73,4B,D1,BC,06,81,1D,6C,32,3D,81", ',', 16)), // PARTY
		new GUID(Util.explodeToBytes("8C,50,DB,AE,81,ED,47,86,AC,CA,16,CC,32,13,C7,B7", ',', 16)), // BEER
		new GUID(Util.explodeToBytes("3F,B0,BD,36,AF,3B,4A,60,9E,EF,CF,19,0F,6A,5A,7F", ',', 16)), // THINKING
		new GUID(Util.explodeToBytes("F8,E8,D7,B2,82,C4,41,42,90,F8,10,C6,CE,0A,89,A6", ',', 16)), // EATING
		new GUID(Util.explodeToBytes("80,53,7D,E2,A4,67,4A,76,B3,54,6D,FD,07,5F,5E,C6", ',', 16)), // TV
		new GUID(Util.explodeToBytes("F1,8A,B5,2E,DC,57,49,1D,99,DC,64,44,50,24,57,AF", ',', 16)), // FRIENDS
		new GUID(Util.explodeToBytes("1B,78,AE,31,FA,0B,4D,38,93,D1,99,7E,EE,AF,B2,18", ',', 16)), // COFFEE
		new GUID(Util.explodeToBytes("61,BE,E0,DD,8B,DD,47,5D,8D,EE,5F,4B,AA,CF,19,A7", ',', 16)), // MUSIC
		new GUID(Util.explodeToBytes("48,8E,14,89,8A,CA,4A,08,82,AA,77,CE,7A,16,52,08", ',', 16)), // BUSINESS
		new GUID(Util.explodeToBytes("10,7A,9A,18,12,32,4D,A4,B6,CD,08,79,DB,78,0F,09", ',', 16)), // CAMERA
		new GUID(Util.explodeToBytes("6F,49,30,98,4F,7C,4A,FF,A2,76,34,A0,3B,CE,AE,A7", ',', 16)), // FUNNY
		new GUID(Util.explodeToBytes("12,92,E5,50,1B,64,4F,66,B2,06,B2,9A,F3,78,E4,8D", ',', 16)), // PHONE
		new GUID(Util.explodeToBytes("D4,A6,11,D0,8F,01,4E,C0,92,23,C5,B6,BE,C6,CC,F0", ',', 16)), // GAMES
		new GUID(Util.explodeToBytes("60,9D,52,F8,A2,9A,49,A6,B2,A0,25,24,C5,E9,D2,60", ',', 16)), // COLLEGE
		new GUID(Util.explodeToBytes("1F,7A,40,71,BF,3B,4E,60,BC,32,4C,57,87,B0,4C,F1", ',', 16)), // SICK
		new GUID(Util.explodeToBytes("78,5E,8C,48,40,D3,4C,65,88,6F,04,CF,3F,3F,43,DF", ',', 16)), // SLEEPING
		new GUID(Util.explodeToBytes("A6,ED,55,7E,6B,F7,44,D4,A5,D4,D2,E7,D9,5C,E8,1F", ',', 16)), // SURFING
		new GUID(Util.explodeToBytes("12,D0,7E,3E,F8,85,48,9E,8E,97,A7,2A,65,51,E5,8D", ',', 16)), // INTERNET
		new GUID(Util.explodeToBytes("BA,74,DB,3E,9E,24,43,4B,87,B6,2F,6B,8D,FE,E5,0F", ',', 16)), // ENGINEERING
		new GUID(Util.explodeToBytes("63,4F,6B,D8,AD,D2,4A,A1,AA,B9,11,5B,C2,6D,05,A1", ',', 16)), // TYPING
		new GUID(Util.explodeToBytes("01,D8,D7,EE,AC,3B,49,2A,A5,8D,D3,D8,77,E6,6B,92", ',', 16)), // ANGRY
		new GUID(Util.explodeToBytes("2C,E0,E4,E5,7C,64,43,70,9C,3A,7A,1C,E8,78,A7,DC", ',', 16)), // UNK
		new GUID(Util.explodeToBytes("10,11,17,C9,A3,B0,40,F9,81,AC,49,E1,59,FB,D5,D4", ',', 16)), // PPC
		new GUID(Util.explodeToBytes("16,0C,60,BB,DD,44,43,F3,91,40,05,0F,00,E6,C0,09", ',', 16)), // MOBILE
		new GUID(Util.explodeToBytes("64,43,C6,AF,22,60,45,17,B5,8C,D7,DF,8E,29,03,52", ',', 16)), // MAN
		new GUID(Util.explodeToBytes("16,F5,B7,6F,A9,D2,40,35,8C,C5,C0,84,70,3C,98,FA", ',', 16)), // WC
		new GUID(Util.explodeToBytes("63,14,36,FF,3F,8A,40,D0,A5,CB,7B,66,E0,51,B3,64", ',', 16)), // QUESTION
		new GUID(Util.explodeToBytes("B7,08,67,F5,38,25,43,27,A1,FF,CF,4C,C1,93,97,97", ',', 16)), // WAY
		new GUID(Util.explodeToBytes("DD,CF,0E,A9,71,95,40,48,A9,C6,41,32,06,D6,F2,80", ',', 16)), // HEART
		new GUID(Util.explodeToBytes("3F,B0,BD,36,AF,3B,4A,60,9E,EF,CF,19,0F,6A,5A,7E", ',', 16)), // CIGARETTE
		new GUID(Util.explodeToBytes("E6,01,E4,1C,33,73,4B,D1,BC,06,81,1D,6C,32,3D,82", ',', 16)), // SEX
		new GUID(Util.explodeToBytes("D4,E2,B0,BA,33,4E,4F,A5,98,D0,11,7D,BF,4D,3C,C8", ',', 16)), // SEARCH
		new GUID(Util.explodeToBytes("00,72,D9,08,4A,D1,43,DD,91,99,6F,02,69,66,02,6F", ',', 16))  // DIARY
	};

	private static final String[] xstatus =
	{
		"xstatus_shopping",
		"xstatus_duck",
		"xstatus_tired",
		"xstatus_party",
		"xstatus_beer",
		"xstatus_thinking",
		"xstatus_eating",
		"xstatus_tv",
		"xstatus_friends",
		"xstatus_coffee",
		"xstatus_music",
		"xstatus_business",
		"xstatus_camera",
		"xstatus_funny",
		"xstatus_phone",
		"xstatus_games",
		"xstatus_college",
		"xstatus_sick",
		"xstatus_sleeping",
		"xstatus_surfing",
		"xstatus_internet",
		"xstatus_engineering",
		"xstatus_typing",
		"xstatus_angry",
		"xstatus_unk",
		"xstatus_ppc",
		"xstatus_mobile",
		"xstatus_man",
		"xstatus_wc",
		"xstatus_question",
		"xstatus_way",
		"xstatus_heart",
		"xstatus_cigarette",
		"xstatus_sex",
		"xstatus_rambler_search",
		"xstatus_rambler_journal"
	};

	public static final ImageList imageList = ImageList.load("/xstatus.png");

	private int index;

	private static int getXStatus(byte[] guid)
	{
		for (int i = 0; i < xguids.length; i++)
		{
			if (xguids[i].equals(guid))
			{
				return i;
			}
		}
		return -1;
	}

	public XStatus()
	{
		index = -1;
	}

	public void setXStatus(byte[] guids)
	{
		index = -1;
		if (guids == null)
		{
			return;
		}

		byte guid[] = new byte[16];
		for (int i = 0; (i < guids.length) && (index == -1); i += 16)
		{
			System.arraycopy(guids, i, guid, 0, 16);
			index = getXStatus(guid);
		}
	}

	public void setStatusIndex(int idx)
	{
		index = idx;
	}

	public static GUID getStatusGUID(int index)
	{
		if (index >= 0)
		{
			try
			{
				return xguids[index];
			}
			catch (Exception e) {}
		}
		return null;
	}

	public static Icon getStatusImage(int index)
	{
		if (index >= 0)
		{
			return imageList.elementAt(index);
		}
		return null;
	}

	public static int getStatusIndex(int index)
	{
		if (index >= 0)
		{
			return index;
		}
		return -1;
	}

	public static String getStatusAsString(int index)
	{
		if (index >= 0)
		{
			try
			{
				return ResourceBundle.getString(xstatus[index]);
			}
			catch (Exception e) {}
		}
		return ResourceBundle.getString("xstatus_none");
	}

	public static int getXStatusCount()
	{
		return xguids.length;
	}

	public GUID getStatusGUID()
	{
		return getStatusGUID(index);
	}

	public Icon getStatusImage()
	{
		return getStatusImage(index);
	}

	public int getStatusIndex()
	{
		return getStatusIndex(index);
	}

	public String getXStatusAsString()
	{
		return getStatusAsString(index);
	}

	public static ImageList getXStatusImageList()
	{
		return imageList;
	}
}