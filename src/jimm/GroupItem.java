/*******************************************************************************
 Jimm - Mobile Messaging - J2ME ICQ clone
 Copyright (C) 2003-04  Jimm Project

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
 File: src/jimm/GroupItem.java
 Version: ###VERSION###  Date: ###DATE###
 Author(s): Manuel Linsmayer, Artyomov Denis, Andreas Rossbacher
 *******************************************************************************/

package jimm;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import jimm.comm.Util;
import javax.microedition.lcdui.Font;

public class GroupItem implements ContactListItem
{
	// Persistent variables
	private int id;
	private String name;
	
	private int
		// Counter for online users
		onlineCount,
		
		// counter for total users in group
		totalCount;

	// Constructor for an existing group item
	public GroupItem(int id, String name)
	{
		this.id = id;
		this.name = new String(name);
		onlineCount = totalCount = 0;
	}
	
	public GroupItem() {}

	// Constructor for a new group item
	public GroupItem(String name)
	{
        this.id = Util.createRandomId();
		this.name = new String(name);
		onlineCount = totalCount = 0;
	}

	public void setCounters(int online, int total)
	{
	    onlineCount = online;
	    totalCount  = total;
	}
	
	public void updateCounters(int onlineInc, int totalInc)
	{
	    onlineCount += onlineInc;
	    totalCount  += totalInc;
	}

    public int getImageIndex()
    {
        return 22; // ?????? ?????? ?????????...
    }
    
	private jimm.comm.XStatus xstatus = new jimm.comm.XStatus();

	public synchronized jimm.comm.XStatus getXStatus() 
	{
		return xstatus;
	}

	public String getText()
    {
        String result;
        
        if ((onlineCount != 0) && !Options.getBoolean(Options.OPTION_CL_HIDE_OFFLINE)) 
            result = name + " (" +Integer.toString(onlineCount) + "/" +Integer.toString(totalCount) + ")";
        else result = name;
        return result;
    }

    public int getTextColor()
    {
    	return Options.getInt(Options.OPTION_COLOR_TEXT);
    }

    public int getClientImageIndex()
    {
        return -1;
    }

    public int getBirthDayImageIndex()
    {
        return -1;
    }

    public int getHappyImageIndex()
    {
        return -1;
    }

    public int getAuthImageIndex()
    {
        return -1;
    }

    public int getVisibilityImageIndex()
    {
        return -1;
    }

    public int getIgnoreImageIndex()
    {
        return -1;
    }

	// Returns the group item id
	public int getId()
	{
		return (this.id);
	}

	// Sets the group item id
	public void setId(int id)
	{
		this.id = id;
	}

	// Returns the group item name
	public String getName()
	{
		return (new String(this.name));
	}

	// Sets the group item name
	public void setName(String name)
	{
		this.name = new String(name);
	}

	// Checks whether some other object is equal to this one
	public boolean equals(Object obj)
	{
		if (!(obj instanceof GroupItem)) return (false);
		GroupItem gi = (GroupItem) obj;
		return (this.id == gi.getId());
	}
	
	public int getFontStyle()
	{
		return Options.getInt(Options.OPTION_CL_FONT_STYLE);
	}
	
	public void saveToStream(DataOutputStream stream) throws IOException
	{
		stream.writeByte(1);
		stream.writeInt(id);
		stream.writeUTF(name);
	}
	
	public void loadFromStream(DataInputStream stream) throws IOException
	{
		id = stream.readInt();
		name = stream.readUTF();
	}

	public String getSortText()
	{
		return name;
	}
	
	public int getSortWeight(int sortType)
	{
		return 0;
	}
}
