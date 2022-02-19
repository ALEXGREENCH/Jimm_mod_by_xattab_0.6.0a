/*******************************************************************************
 Jimm - Mobile Messaging - J2ME ICQ clone
 Copyright (C) 2003-06  Jimm Project

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
 File: src/jimm/ContactItem.java
 Version: ###VERSION###  Date: ###DATE###
 Author(s): Manuel Linsmayer, Andreas Rossbacher, Artyomov Denis
 *******************************************************************************/

package jimm;

import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.TimerTask;
import javax.microedition.lcdui.*;

import DrawControls.*;
import jimm.JimmUI;
import jimm.SplashCanvas;
import jimm.comm.*;
import jimm.util.*;

/* TODO: remove UI code to ChatHistory */ 
public class ContactItem implements ContactListItem
{
	/* No capability */
	public static final int CAP_NO_INTERNAL = 0x00000000;

	/* Message types */
	public static final int MESSAGE_PLAIN        = 1;
	public static final int MESSAGE_URL          = 2;
	public static final int MESSAGE_SYS_NOTICE   = 3;
	public static final int MESSAGE_AUTH_REQUEST = 4;

	private int idAndGropup,
	             idle,
	             booleanValues,
	             messCounters;

	public int caps;

	//#sijapp cond.if modules_FILES is "true"#
	private int typeAndClientId,
	             portAndProt,
	             intIP,
	             extIP,
	             authCookie;
	// #sijapp cond.end #

	private int uinLong,
	             online,
	             regdata,
	             signOn,
	             status,
	             birthDay;
	               
	private String clientVersion,
	                offlineTime,
	                clientCap,
	                lowerText;

	public String name;

	public boolean readXtraz,
	                autoAnswered,
	                openChat,
	                readStatusMess;

	public long lastOfflineActivity = 0;

	///////////////////////////////////////////////////////////////////////////
	synchronized public void setStringValue(int key, String value)
	{
		switch(key)
		{
			case CONTACTITEM_UIN:         uinLong = Integer.parseInt(value); return;
			case CONTACTITEM_NAME:        name = value; lowerText = null; return;
			case CONTACTITEM_CLIVERSION:  clientVersion = value; return;
//			case CONTACTITEM_CLIENTCAP:   clientCap = value; return;
			case CONTACTITEM_OFFLINETIME: offlineTime = value; return;
		}
	}

	synchronized public String getStringValue(int key)
	{
		switch(key)
		{
			case CONTACTITEM_UIN:         return Integer.toString(uinLong);
			case CONTACTITEM_NAME:        return name;
			case CONTACTITEM_CLIVERSION:  return clientVersion;
//			case CONTACTITEM_CLIENTCAP:   return clientCap;
			case CONTACTITEM_OFFLINETIME: return offlineTime;
		}
		return null;
	}
	//////////////////////////////////////////////////////////////////////////

	public String getSortText()
	{
		return getLowerText(); 
	}

	public int getSortWeight(int sortType)
	{
		int status = getIntValue(CONTACTITEM_STATUS); 

		if (isMessageAvailable(MESSAGE_PLAIN)) return 1;

		switch (sortType)
		{
		case ContactList.SORT_BY_STATUS:
			if (status == ContactList.STATUS_CHAT) return 2;
			if ((status == ContactList.STATUS_ONLINE) || (status == ContactList.STATUS_HOME)
				|| (status == ContactList.STATUS_WORK) || (status == ContactList.STATUS_INVISIBLE)) return 3;
			if (status == ContactList.STATUS_EVIL) return 4;
			if (status == ContactList.STATUS_DEPRESSION) return 5;
			if (status == ContactList.STATUS_OCCUPIED) return 6;
			if (status == ContactList.STATUS_DND) return 7;
			if (status == ContactList.STATUS_LUNCH) return 8;
			if (status == ContactList.STATUS_AWAY) return 9;
			if (status == ContactList.STATUS_NA) return 10;
			break;
		case ContactList.SORT_BY_STATUS_AND_NAME:
			if (status != ContactList.STATUS_OFFLINE) return 5;
			break;
		}

		if ((getBooleanValue(CONTACTITEM_IS_TEMP) || getBooleanValue(CONTACTITEM_PHANTOM)) &&
			(status == ContactList.STATUS_OFFLINE)) return 20;

		return 15;
	}

	///////////////////////////////////////////////////////////////////////////

	synchronized public void setIntValue(int key, int value)
	{
		switch (key)
		{
		case CONTACTITEM_ID:
			idAndGropup = (idAndGropup & 0x0000FFFF) | (value << 16);
			return;
			
		case CONTACTITEM_GROUP:
			idAndGropup = (idAndGropup & 0xFFFF0000) | value;
			return;
			
		case CONTACTITEM_PLAINMESSAGES:
			messCounters = (messCounters & 0x00FFFFFF) | (value << 24);
			return;
			
		case CONTACTITEM_URLMESSAGES:
			messCounters = (messCounters & 0xFF00FFFF) | (value << 16);
			return;
			
		case CONTACTITEM_SYSNOTICES:
			messCounters = (messCounters & 0xFFFF00FF) | (value << 8);
			return;
			
		case CONTACTITEM_AUTREQUESTS:
			messCounters = (messCounters & 0xFFFFFF00) | value;
			return;
		
		case CONTACTITEM_IDLE: idle = value; return;
		case CONTACTITEM_CAPABILITIES: caps = value; return;
		case CONTACTITEM_STATUS: status = value; return;
		//#sijapp cond.if target is "MIDP2" | target is "MOTOROLA" | target is "SIEMENS2"#
		//#sijapp cond.if modules_FILES is "true"#
		case CONTACTITEM_DC_TYPE: typeAndClientId = (typeAndClientId & 0xff) | ((value & 0xff) << 8); return;
		case CONTACTITEM_ICQ_PROT: portAndProt = (portAndProt & 0xffff0000) | (value & 0xffff); return;
		case CONTACTITEM_DC_PORT: portAndProt = (portAndProt & 0xffff) | ((value & 0xffff) << 16); return;
		case CONTACTITEM_CLIENT: typeAndClientId = (typeAndClientId & 0xff00) | (value & 0xff); return;
		case CONTACTITEM_AUTH_COOKIE: authCookie = value; return;
		// #sijapp cond.end #
		// #sijapp cond.end #
		
		case CONTACTITEM_ONLINE: online = value; return;
		case CONTACTITEM_SIGNON: signOn = value; return;
		case CONTACTITEM_REGDATA: regdata = value; return;
		case CONTACTITEM_BDAY: birthDay = value; return;
		}
	}
	
	synchronized public int getIntValue(int key)
	{
		switch (key)
		{
		case CONTACTITEM_ID:            return ((idAndGropup & 0xFFFF0000) >> 16) & 0xFFFF;
		case CONTACTITEM_GROUP:         return (idAndGropup & 0x0000FFFF);
		case CONTACTITEM_PLAINMESSAGES: return ((messCounters & 0xFF000000) >> 24) & 0xFF;
		case CONTACTITEM_URLMESSAGES:   return ((messCounters & 0x00FF0000) >> 16) & 0xFF;
		case CONTACTITEM_SYSNOTICES:    return ((messCounters & 0x0000FF00) >> 8) & 0xFF;
		case CONTACTITEM_AUTREQUESTS:   return (messCounters & 0x000000FF);
		case CONTACTITEM_IDLE:          return idle;
		case CONTACTITEM_CAPABILITIES:  return caps;
		case CONTACTITEM_STATUS:        return status;
		// #sijapp cond.if target is "MIDP2" | target is "MOTOROLA" | target is "SIEMENS2"#
		//#sijapp cond.if modules_FILES is "true"#
		case CONTACTITEM_DC_TYPE:       return ((typeAndClientId & 0xff00) >> 8) & 0xFF;
		case CONTACTITEM_ICQ_PROT:      return portAndProt & 0xffff;
		case CONTACTITEM_DC_PORT:       return ((portAndProt & 0xffff0000) >> 16) & 0xFFFF;
		case CONTACTITEM_CLIENT:        return typeAndClientId & 0xff;
		case CONTACTITEM_AUTH_COOKIE:   return authCookie;
		// #sijapp cond.end #
		// #sijapp cond.end #
		case CONTACTITEM_ONLINE:        return online;
		case CONTACTITEM_SIGNON:        return signOn;
		case CONTACTITEM_REGDATA:       return regdata;
		case CONTACTITEM_BDAY:          return birthDay;
		}
		return 0;
	}
	
///////////////////////////////////////////////////////////////////////////
	
	synchronized public void setBooleanValue(int key, boolean value)
	{
		booleanValues = (booleanValues & (~key)) | (value ? key : 0x00000000);
	}
	
	synchronized public boolean getBooleanValue(int key)
	{
		return (booleanValues&key) != 0;
	}
	
	///////////////////////////////////////////////////////////////////////////
	
	//#sijapp cond.if target is "MIDP2" | target is "MOTOROLA" | target is "SIEMENS2"#
	//#sijapp cond.if modules_FILES is "true"#
	public static byte[] longIPToByteAray(int value)
	{
		if (value == 0) return null;
		return new byte[]
		                {
							(byte)(value & 0x000000FF),
							(byte)((value & 0x0000FF00) >> 8),
							(byte)((value & 0x00FF0000) >> 16),
							(byte)((value & 0xFF000000) >> 24)
						}; 
	}
	
	public static int arrayToLongIP(byte[] array)
	{
		if ((array == null) || (array.length < 4)) return 0;
		return   (int)array[0] & 0xFF       | 
		       (((int)array[1] & 0xFF) << 8) | 
		       (((int)array[2] & 0xFF) <<16) |
		       (((int)array[3] & 0xFF) <<24);
	}

	synchronized public void setIPValue(int key, byte[] value)
	{
		switch (key)
		{
		case CONTACTITEM_INTERNAL_IP: intIP = arrayToLongIP(value); break;
		case CONTACTITEM_EXTERNAL_IP: extIP = arrayToLongIP(value); break;
		}
	}
	
	synchronized public byte[] getIPValue(int key)
	{
		switch (key)
		{
		case CONTACTITEM_INTERNAL_IP: return longIPToByteAray(intIP);
		case CONTACTITEM_EXTERNAL_IP: return longIPToByteAray(extIP);
		}
		return null;
	}
	//#sijapp cond.end #
	//#sijapp cond.end #
	
	public void saveToStream(DataOutputStream stream) throws IOException
	{
		stream.writeByte(0);
		stream.writeInt(idAndGropup);
		stream.writeByte(booleanValues & (CONTACTITEM_IS_TEMP | CONTACTITEM_NO_AUTH));
		stream.writeInt(uinLong);
		stream.writeUTF(name);
        // Privacy lists begining
        stream.writeInt(getVisibleId());
        stream.writeInt(getInvisibleId());
        stream.writeInt(getIgnoreId());
        // Privacy lists ending
	}
	
	public void loadFromStream(DataInputStream stream) throws IOException
	{
		idAndGropup = stream.readInt();
		booleanValues = stream.readByte();
		uinLong = stream.readInt();
		name = stream.readUTF();
        // Privacy lists begining
        setVisibleId(stream.readInt());
        setInvisibleId(stream.readInt());
        setIgnoreId(stream.readInt());
        // Privacy lists ending
	}

	/* Variable keys */
	public static final int CONTACTITEM_UIN           = 0;      /* String */
	public static final int CONTACTITEM_NAME          = 1;      /* String */
	public static final int CONTACTITEM_ID            = 64;     /* Integer */
	public static final int CONTACTITEM_GROUP         = 65;     /* Integer */
	public static final int CONTACTITEM_PLAINMESSAGES = 67;     /* Integer */
	public static final int CONTACTITEM_URLMESSAGES   = 68;     /* Integer */
	public static final int CONTACTITEM_SYSNOTICES    = 69;     /* Integer */
	public static final int CONTACTITEM_AUTREQUESTS   = 70;     /* Integer */
	public static final int CONTACTITEM_IDLE          = 71;     /* Integer */
	public static final int CONTACTITEM_ADDED         = 1 << 0; /* Boolean */
	public static final int CONTACTITEM_NO_AUTH       = 1 << 1; /* Boolean */
	public static final int CONTACTITEM_CHAT_SHOWN    = 1 << 2; /* Boolean */
	public static final int CONTACTITEM_IS_TEMP       = 1 << 3; /* Boolean */
	public static final int CONTACTITEM_HAS_CHAT      = 1 << 4; /* Boolean */
	public static final int CONTACTITEM_PHANTOM       = 1 << 5; /* Boolean */
	public static final int CONTACTITEM_STATUS        = 192;    /* Integer */
	public static final int CONTACTITEM_SIGNON        = 194;    /* Integer */
	public static final int CONTACTITEM_REGDATA       = 191;    /* Integer */
	public static final int CONTACTITEM_ONLINE        = 195;    /* Integer */
	public static final int CONTACTITEM_BDAY          = 196;    /* Integer */	
	// #sijapp cond.if target is "MIDP2" | target is "MOTOROLA" | target is "SIEMENS2"#
	// #sijapp cond.if modules_FILES is "true"#
	public static final int CONTACTITEM_INTERNAL_IP   = 225;    /* IP address */
	public static final int CONTACTITEM_EXTERNAL_IP   = 226;    /* IP address */
	public static final int CONTACTITEM_AUTH_COOKIE   = 193;    /* Integer */
	public static final int CONTACTITEM_DC_TYPE       = 72;     /* Integer */
	public static final int CONTACTITEM_ICQ_PROT      = 73;     /* Integer */
	public static final int CONTACTITEM_DC_PORT       = 74;     /* Integer */
	// #sijapp cond.end#
	// #sijapp cond.end#
	public static final int CONTACTITEM_CAPABILITIES  = 75;     /* Integer */
	public static final int CONTACTITEM_CLIENT        = 76;     /* Integer */
	public static final int CONTACTITEM_CLIVERSION    = 2;      /* String  */
	public static final int CONTACTITEM_OFFLINETIME   = 3;      /* String  */
//	public static final int CONTACTITEM_CLIENTCAP     = 4;      /* String  */

	// #sijapp cond.if target is "MIDP2" | target is "MOTOROLA" | target is "SIEMENS2"#
	// #sijapp cond.if modules_FILES is "true"#
	/* DC values */
	private FileTransferMessage ftm;
	private FileTransfer ft;
	//  #sijapp cond.end#
	//  #sijapp cond.end#
	
	public static String currentUin = new String();

	public void init(int id, int group, String uin, String name, boolean noAuth, boolean added)
	{
		if (id == -1) setIntValue(CONTACTITEM_ID, Util.createRandomId());
		else setIntValue(CONTACTITEM_ID, id);

		setIntValue(CONTACTITEM_GROUP, group);
		setStringValue(CONTACTITEM_UIN, uin);
		setStringValue(CONTACTITEM_NAME, name);
		setBooleanValue(CONTACTITEM_NO_AUTH, noAuth);
		setBooleanValue(CONTACTITEM_IS_TEMP, false);
		setBooleanValue(CONTACTITEM_HAS_CHAT, false);
		setBooleanValue(CONTACTITEM_PHANTOM, false);
		setBooleanValue(CONTACTITEM_ADDED, added);
		setIntValue(CONTACTITEM_STATUS, ContactList.STATUS_OFFLINE);
		setIntValue(CONTACTITEM_CAPABILITIES, Util.CAPF_NO_INTERNAL);
		setIntValue(CONTACTITEM_PLAINMESSAGES, 0);
		setIntValue(CONTACTITEM_URLMESSAGES, 0);
		setIntValue(CONTACTITEM_SYSNOTICES, 0);
		setIntValue(CONTACTITEM_AUTREQUESTS, 0);
		// #sijapp cond.if target is "MIDP2" | target is "MOTOROLA" | target is "SIEMENS2"#
		// #sijapp cond.if modules_FILES is "true"#
		setIPValue(CONTACTITEM_INTERNAL_IP, new byte[4]);
		setIPValue(CONTACTITEM_EXTERNAL_IP, new byte[4]);
		setIntValue(CONTACTITEM_DC_PORT, 0);
		setIntValue(CONTACTITEM_DC_TYPE, 0);
		setIntValue(CONTACTITEM_ICQ_PROT, 0);
		setIntValue(CONTACTITEM_AUTH_COOKIE, 0);
		this.ft = null;
		// #sijapp cond.end#
		// #sijapp cond.end#
		setIntValue(CONTACTITEM_SIGNON, -1);
		setIntValue(CONTACTITEM_REGDATA, -1);
		online = -1;
		setIntValue(CONTACTITEM_IDLE, -1);
		setIntValue(CONTACTITEM_CLIENT, Util.CLI_NONE);
		setStringValue(CONTACTITEM_CLIVERSION, "");
		setStringValue(CONTACTITEM_OFFLINETIME, "");
//		setStringValue(CONTACTITEM_CLIENTCAP, "");
		//#sijapp cond.if target is "MIDP2" | target is "SIEMENS2"#
		setIntValue(CONTACTITEM_BDAY, NoticeOnBirthDay.checkDatacurrData(uin));
		//#sijapp cond.else#
		setIntValue(CONTACTITEM_BDAY, -1);
		//#sijapp cond.end#

	}
	
	/* Constructor for an existing contact item */
	public ContactItem(int id, int group, String uin, String name, boolean noAuth, boolean added)
	{
		this.init(id,group,uin,name,noAuth,added);
	}
	
	public ContactItem() {}
	
	/* Returns true if client supports given capability */
	public boolean hasCapability(int capability)
	{
		return ((capability & this.caps) != 0x00000000);
	}
	
	/* Adds a capability by its CAPF value */
	public void addCapability(int capability)
	{
		this.caps |= capability;
	}
	
	public String getLowerText()
	{
		if (lowerText == null)
		{
			lowerText = name.toLowerCase();
			if (lowerText.equals(name)) lowerText = name; // to decrease memory usage 
		}
		return lowerText;
	}
	
	/* Returns font color for contact name */
	public int getTextColor()
	{
		if ((Options.getBoolean(Options.OPTION_ONLINE_BLINK_NICK) && blinkingOnline) || blinkingOffline) return Options.blinkColor;
		if (getBooleanValue(CONTACTITEM_PHANTOM))  return 0xFF0000;
		if (getBooleanValue(CONTACTITEM_IS_TEMP))  return Options.getInt(Options.OPTION_COLOR_TEMP);
		if (getBooleanValue(CONTACTITEM_HAS_CHAT)) return Options.getInt(Options.OPTION_COLOR_BLUE);

		return Options.getInt(Options.OPTION_COLOR_TEXT);
	}
	
	/* Returns font style for contact name */ 
	public int getFontStyle()
	{
		if ((getBooleanValue(CONTACTITEM_HAS_CHAT)) || blinkingOffline
			|| (Options.getBoolean(Options.OPTION_ONLINE_BLINK_NICK) && blinkingOnline))
		{
			return Font.STYLE_BOLD;
		}

		return Options.getInt(Options.OPTION_CL_FONT_STYLE);
	}
	
	public int getUIN()
	{
		return uinLong;
	}

	public String getUinString()
	{
		return Integer.toString(uinLong);
	}

	/* Returns image index for contact */
	public int getImageIndex()
	{
		int tempIndex = -1;
		//#sijapp cond.if target isnot "DEFAULT"#
		if (typing) return 18;
		if (blinkingOnline && Options.getBoolean(Options.OPTION_ONLINE_BLINK_ICON)) return 19;
		//#sijapp cond.end#
		if (isMessageAvailable(MESSAGE_PLAIN)) tempIndex = 14;
		else if (isMessageAvailable(MESSAGE_URL)) tempIndex = 15;
		else if (isMessageAvailable(MESSAGE_AUTH_REQUEST)) tempIndex = 16;
		else if (isMessageAvailable(MESSAGE_SYS_NOTICE)) tempIndex = 17;
		else tempIndex = JimmUI.getStatusImageIndex(getIntValue(CONTACTITEM_STATUS));
		return tempIndex;
	}
	
	public String getText()
	{
		/* обрезка ника. додумать...
		if ((Options.getBoolean(Options.OPTION_CUT_NICK)) && (name.length() > 11))
		{
			return name.substring(0, 11) + "...";
		}
		*/
		return name;
	}

	
    // #sijapp cond.if target is "MIDP2" | target is "MOTOROLA" | target is "SIEMENS2"#
    // #sijapp cond.if modules_FILES is "true"#
	
	/* Returns the fileTransfer Object of this contact */
	public FileTransfer getFT()
	{
		return this.ft;
	}
	
	/* Set the FileTransferMessage of this contact */
	public void setFTM(FileTransferMessage _ftm)
	{
		this.ftm = _ftm;
	}

	/* Returns the FileTransferMessage of this contact */
	public FileTransferMessage getFTM()
	{
		return this.ftm;
	}
	//  #sijapp cond.end#
	//  #sijapp cond.end#
	
	/* Returns true if contact must be shown even user offline and "hide offline" is on */
	protected boolean mustBeShownAnyWay()
	{
		if (Options.getBoolean(Options.OPTION_CL_HIDE_OFFLINE_ALL))
		{
			return ((getIntValue(CONTACTITEM_PLAINMESSAGES) > 0) || (getIntValue(CONTACTITEM_URLMESSAGES) > 0)
					|| (getIntValue(CONTACTITEM_SYSNOTICES) > 0) || (getIntValue(CONTACTITEM_AUTREQUESTS) > 0)
					|| (getBooleanValue(CONTACTITEM_IS_TEMP)) || (getBooleanValue(CONTACTITEM_PHANTOM)) || mustStayVisible);
		}
		else
		{
			return ((getBooleanValue(CONTACTITEM_HAS_CHAT)) || (getBooleanValue(CONTACTITEM_IS_TEMP))
					|| (getBooleanValue(CONTACTITEM_PHANTOM)) || mustStayVisible);
		}
	}

	/* Returns total count of all unread messages (messages, sys notices, urls, auths) */
	protected int getUnreadMessCount()
	{
		return getIntValue(CONTACTITEM_PLAINMESSAGES) + getIntValue(CONTACTITEM_URLMESSAGES)
				+ getIntValue(CONTACTITEM_SYSNOTICES) + getIntValue(CONTACTITEM_AUTREQUESTS);
	}

	/* Returns true if the next available message is a message of given type
	   Returns false if no message at all is available, or if the next available
	   message is of another type */
	protected synchronized boolean isMessageAvailable(int type)
	{
		switch (type)
		{
			case MESSAGE_PLAIN:        return (getIntValue(CONTACTITEM_PLAINMESSAGES) > 0);
			case MESSAGE_URL:          return (getIntValue(CONTACTITEM_URLMESSAGES) > 0); 
			case MESSAGE_SYS_NOTICE:   return (getIntValue(CONTACTITEM_SYSNOTICES) > 0);
			case MESSAGE_AUTH_REQUEST: return (getIntValue(CONTACTITEM_AUTREQUESTS) > 0); 
		}
		return (this.getIntValue(CONTACTITEM_PLAINMESSAGES) > 0);
	}
	
	/* Increases the mesage count */
	protected synchronized void increaseMessageCount(int type)
	{ 
		switch (type)
		{
			case MESSAGE_PLAIN:        setIntValue(CONTACTITEM_PLAINMESSAGES, getIntValue(CONTACTITEM_PLAINMESSAGES) + 1); break;
			case MESSAGE_URL:          setIntValue(CONTACTITEM_URLMESSAGES, getIntValue(CONTACTITEM_URLMESSAGES) + 1); break;
			case MESSAGE_SYS_NOTICE:   setIntValue(CONTACTITEM_SYSNOTICES, getIntValue(CONTACTITEM_SYSNOTICES) + 1); break;
			case MESSAGE_AUTH_REQUEST: setIntValue(CONTACTITEM_AUTREQUESTS, getIntValue(CONTACTITEM_AUTREQUESTS) + 1);
		}
	}

	public synchronized void resetUnreadMessages()
	{
		setIntValue(CONTACTITEM_PLAINMESSAGES,0);
		setIntValue(CONTACTITEM_URLMESSAGES,0);
		setIntValue(CONTACTITEM_SYSNOTICES,0);
	}

	/* Checks whether some other object is equal to this one */
	public boolean equals(Object obj)
	{
		if (!(obj instanceof ContactItem)) return (false);
		ContactItem ci = (ContactItem) obj;
		return (getStringValue(CONTACTITEM_UIN).equals(ci.getStringValue(CONTACTITEM_UIN))
				&& (getBooleanValue(CONTACTITEM_IS_TEMP) == ci.getBooleanValue(CONTACTITEM_IS_TEMP)));
	}

	//#sijapp cond.if modules_HISTORY is "true" #
	public void showHistory()
	{
		HistoryStorage.showHistoryList(getStringValue(CONTACTITEM_UIN), name);
	}
	//#sijapp cond.end#

	//#sijapp cond.if target isnot "DEFAULT"#
	private boolean typing = false;

	public void BeginTyping(boolean type)
	{
		typing = type;
		setStatusImage();
	}

	private boolean blinkingOnline;
	private boolean blinkingOffline;
	private boolean mustStayVisible;
	private int blinkingNumber;
	private TimerTask BlinkTimer;

	public void prepareToBlink()
	{
		if (BlinkTimer != null)
		{
			BlinkTimer.cancel();
		}
		blinkingNumber = 0;
		blinkingOnline = false;
		blinkingOffline = false;
		mustStayVisible = true;
	}

	private void updateAfterBlinking()
	{
		mustStayVisible = false;
		ContactList.contactChanged(this, false, true);
	}

	public void BlinkOnline()
	{
		if (!Options.getBoolean(Options.OPTION_ONLINE_BLINK_NICK) && !Options.getBoolean(Options.OPTION_ONLINE_BLINK_ICON))
		{
			return;
		}

		blinkingOffline = false;

		BlinkTimer = new TimerTask()
		{
			public void run()
			{
				blinkingOnline = (!blinkingOnline) ? true : false;
				blinkingNumber++;

				if (blinkingNumber > (Options.getInt(Options.OPTION_ONLINE_BLINK_TIME) * 2 + 1))
				{
					BlinkTimer.cancel();
					blinkingOnline = false;
				}

				ContactList.repaintTree();
			}
		};

		Jimm.getTimerRef().schedule(BlinkTimer, 500, 500);
	}

	public void BlinkOffline()
	{
		if (!Options.getBoolean(Options.OPTION_OFFLINE_BLINK_NICK))
		{
			mustStayVisible = false;
			return;
		}

		blinkingOnline = false;

		BlinkTimer = new TimerTask()
		{
			public void run()
			{
				blinkingOffline = (!blinkingOffline) ? true : false;
				blinkingNumber++;

				if (blinkingNumber > (Options.getInt(Options.OPTION_OFFLINE_BLINK_TIME) * 2 + 1))
				{
					if (mustStayVisible)
					{
						updateAfterBlinking();
					}
					BlinkTimer.cancel();
					blinkingOffline = false;
				}

				ContactList.repaintTree();
			}
		};

		Jimm.getTimerRef().schedule(BlinkTimer, 500, 500);
	}
	//#sijapp cond.end#
	/** ************************************************************************* */
	/** ************************************************************************* */
	/** ************************************************************************* */

	public void setOfflineStatus()
	{
		//#sijapp cond.if target isnot "DEFAULT"#
		typing = false;
		//#sijapp cond.end#
		setIntValue(CONTACTITEM_STATUS, ContactList.STATUS_OFFLINE);
	}

	// ѕолучение картинки шарика дн€ рождени€
	public synchronized int getBirthDayImageIndex()
	{
		return (getIntValue(CONTACTITEM_BDAY) != -1) ? 0 : -1;
	}

	// ѕолучение картинки шарика радости
	public synchronized int getHappyImageIndex()
	{
		return ((happyFlag & 0x0008) != 0) ? 0 : -1;
	}

	// ѕолучение картинки значка авторизации
	public synchronized int getAuthImageIndex()
	{
		return (getBooleanValue(CONTACTITEM_NO_AUTH)) ? 0 : -1;
	}

	// ѕолучение картинки ICQ-клиента
	public synchronized int getClientImageIndex()
	{
		return getIntValue(CONTACTITEM_CLIENT) - 1;
	}

	// ѕолучение картинки приватного списка дл€ контакта
	public synchronized int getVisibilityImageIndex()
	{
/*
        int status = Icq.getCurrentStatus();
        //int invisibleIndex = (getInvisibleId() == 0) ? -1 : 0;
        int visibleIndex = (getVisibleId() == 0) ? -1 : 1;
        
		if ((visibleIndex != -1) && (status == ContactList.STATUS_INVISIBLE))
		{
			return visibleIndex;
		}
		else if ((status != ContactList.STATUS_INVISIBLE) && (status != ContactList.STATUS_INVIS_ALL))
		{
			return (getInvisibleId() == 0) ? -1 : 0;
		}
*/
		if (getVisibleId()   != 0) return 1;
		if (getInvisibleId() != 0) return 0;
		return -1;
	}

	// ѕолучение картинки игнора
	public synchronized int getIgnoreImageIndex()
	{
        return (getIgnoreId() == 0) ? -1 : 2;
	}

	/* Sets new contact name */ 
	public void rename(String newName)
	{
		if ((newName == null) || (newName.length() == 0)) return;
		name = newName;
		lowerText = null;
		try
		{
			/* Save ContactList */
			ContactList.save();

			/* Try to save ContactList to server */
			if (!getBooleanValue(CONTACTITEM_IS_TEMP))
			{
				UpdateContactListAction action = new UpdateContactListAction(this, UpdateContactListAction.ACTION_RENAME);
				Icq.requestAction(action);
			}
		}
		catch (JimmException je)
		{
			if (je.isCritical()) return;
		}
		catch (Exception e) { /* Do nothing */ }

		ContactList.contactChanged(this, true, true);
		ChatHistory.contactRenamed(getStringValue(CONTACTITEM_UIN), name);
	}
		
	/* Activates the contact item menu */
	public void activate()
	{
		currentUin = getUinString();
		//#sijapp cond.if modules_HISTORY is "true" #
		if (!ContactList.enterContactMenu) ChatHistory.fillFormHistory(this);
		//#sijapp cond.end#

		/* Display contact chat history */
		if (getBooleanValue(CONTACTITEM_HAS_CHAT) && !ContactList.enterContactMenu)
		{
			ChatTextList chat = ChatHistory.getChatHistoryAt(currentUin);
			ChatHistory.UpdateCaption(currentUin);
			
			chat.buildMenu(this);
			chat.activate(true, false);

			if (Options.getBoolean(Options.OPTION_AUTO_XTRAZ) && readXtraz && (getIntValue(CONTACTITEM_CLIENT) != Util.CLI_ICQ6))
			{
				try
				{
					XtrazSM.a(currentUin, 0);
				}
				catch (Exception e) {}
				readXtraz = false;
			}

			setStatusImage();
		}
		/* Display contact menu */
		else
		{
			JimmUI.showContactMenu(this);
		}
	}

	/****************************************************************************/
	/****************************************************************************/
	/****************************************************************************/

	public void setStatusImage()
	{
		int imgIndex;
		
		//#sijapp cond.if target isnot "DEFAULT"#
		imgIndex = typing ? 18 : JimmUI.getStatusImageIndex(getIntValue(CONTACTITEM_STATUS));
		//#sijapp cond.else#
		imgIndex = JimmUI.getStatusImageIndex(getIntValue(CONTACTITEM_STATUS));
		//#sijapp cond.end#
		
		if (SplashCanvas.locked())
		{
			SplashCanvas.setStatusToDraw(imgIndex);
			SplashCanvas.setXStatusToDraw(getXStatus().getStatusImage());
			SplashCanvas.setMessage(getStringValue(CONTACTITEM_NAME));
			SplashCanvas.Repaint();
			SplashCanvas.startTimer();
			return;
		}

    	ChatTextList chat = ChatHistory.getChatHistoryAt(getStringValue(CONTACTITEM_UIN));

    	if (chat != null)
    	{
			chat.setImage(ContactList.imageList.elementAt(imgIndex));
			chat.setXstImage(getXStatus().getStatusImage());
			chat.setHappyImage(ContactList.happyIcon.elementAt(getHappyImageIndex()));
		}	 
	}
	
    private jimm.comm.XStatus xstatus = new jimm.comm.XStatus();

    public synchronized void setXStatus(byte[] capa) 
    {
        getXStatus().setXStatus(capa);
    }
    
    public synchronized jimm.comm.XStatus getXStatus() 
    {
        return xstatus;
    }

    public int happyFlag;

    public synchronized void setHappyFlag(int flag)
    {
        happyFlag = flag;
        // MagicEye.addAction("DebugLog", "flags is " + Integer.toHexString(happyFlag));
    }

    // Privacy Lists begining
    private volatile int ignoreId;
    private volatile int visAndInvisId;

    public int getIgnoreId()
    {
        return ignoreId;
    }

    public void setIgnoreId(int id)
    {
        ignoreId = id;
    }

    public int getVisibleId()
    {
        return ((visAndInvisId & 0xFFFF0000) >> 16) & 0xFFFF;
    }

    public void setVisibleId(int id)
    {
        visAndInvisId = (visAndInvisId & 0x0000FFFF) | (id << 16);
    }
    
    public int getInvisibleId()
    {
        return (visAndInvisId & 0x0000FFFF);
    }

    public void setInvisibleId(int id)
    {
        visAndInvisId = (visAndInvisId & 0xFFFF0000) | id;
    }
    // Privacy Lists ending
}