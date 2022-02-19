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
 File: src/jimm/ChatHistory.java
 Version: ###VERSION###  Date: ###DATE###
 Author(s): Andreas Rossbacher, Artyomov Denis, Dmitry Tunin
 *******************************************************************************/

package jimm;

import java.util.*;
import javax.microedition.lcdui.*;

import jimm.comm.*;
import jimm.util.*;
//#sijapp cond.if modules_HISTORY is "true" #
import jimm.HistoryStorage;
//#sijapp cond.end#
import DrawControls.*;

class MessData
{
	private long time;
	private int rowData;
	
	public MessData(boolean incoming, long time, int textOffset, boolean contains_url)
	{
		this.time    = time;
		this.rowData = (textOffset & 0xFFFFFF) | (contains_url ? 0x8000000 : 0) | (incoming ? 0x4000000 : 0);
	}
		
	public boolean getIncoming()
	{
		return (rowData & 0x4000000) != 0;
	}

	public long getTime()
	{
		return time;
	}

	public int getOffset()
	{
		return (rowData&0xFFFFFF);
	}

	//#sijapp cond.if target is "MIDP2" | target is "SIEMENS2" | target is "MOTOROLA"#
	public boolean isURL()
	{
		return (rowData & 0x8000000) != 0;
	}
	//#sijapp cond.end#
}

class ChatTextList implements VirtualListCommands, CommandListener
{
	// UI modes
	final public static int UI_MODE_NONE = 0;
	final public static int UI_MODE_DEL_CHAT = 1;
	
	// Chat
	TextList textList;

	/* Show the message menu */
	private static final Command cmdContactMenu = new Command(ResourceBundle.getString("user_menu"), Command.ITEM, 1);

	/* Message reply command */
	private static final Command cmdMsgReply = new Command(ResourceBundle.getString("reply"), Command.OK, 8); // было 2...

	/* Message close command */
	//#sijapp cond.if target is "MIDP2"#
	private static final Command cmdCloseChat = new Command(ResourceBundle.getString("back"), Jimm.is_smart_SE() ? Command.CANCEL : Command.BACK, 2);
	//#sijapp cond.else#
	private static final Command cmdCloseChat = new Command(ResourceBundle.getString("back"), Command.BACK, 2);
	//#sijapp cond.end#

	/* Deny authorisation a for authorisation asking contact */
	private static final Command cmdDenyAuth = new Command(ResourceBundle.getString("deny"), Command.CANCEL, 3);
	
	/* Request authorisation from a contact */
	private static final Command cmdReqAuth = new Command(ResourceBundle.getString("requauth"), Command.ITEM, 3);	
	
	/* Grand authorisation a for authorisation asking contact */
	private static final  Command cmdGrantAuth = new Command(ResourceBundle.getString("grant"), Command.ITEM, 3);

	/* Message copy command */
	private static final Command cmdCopyText = new Command(ResourceBundle.getString("copy_text"), Command.ITEM, 4);

	/* Add temporary or phantom contact to contact list */
	private static final Command cmdAddUrs = new Command(ResourceBundle.getString("add_user"), Command.ITEM, 5);

	/* Add selected message to history */
	//#sijapp cond.if modules_HISTORY is "true" #
	private static final  Command cmdAddToHistory = new Command(ResourceBundle.getString("add_to_history"), Command.ITEM, 6);
	//#sijapp cond.end#

	/* Delete Chat History */
	private static final  Command cmdDelChat = new Command(ResourceBundle.getString("delete_chat", ResourceBundle.FLAG_ELLIPSIS), Command.ITEM, 9);
	
	public String ChatName;
	ContactItem contact;

	private Vector messData = new Vector();
	private int messTotalCounter = 0;
	private static int currentUiMode; 
	
	ChatTextList(String name, ContactItem contact)
	{
		textList = new TextList(null);
		
		textList.setMode(TextList.MODE_TEXT);
		//#sijapp cond.if target is "MOTOROLA"#
		textList.setFontSize(Options.getBoolean(Options.OPTION_CHAT_SMALL_FONT) ? TextList.LARGE_FONT : TextList.MEDIUM_FONT);
		//#sijapp cond.else#
		textList.setFontSize(Options.getBoolean(Options.OPTION_CHAT_SMALL_FONT) ? TextList.SMALL_FONT : TextList.MEDIUM_FONT);
		//#sijapp cond.end#
		
		this.contact = contact;
		ChatName = name;
		JimmUI.setColorScheme(textList, Options.getBoolean(Options.OPTION_FULL_SCREEN));
		
		textList.setVLCommands(this);
	}
	
	public ContactItem getContact()
	{
		return contact;
	}

	public Object getUIControl()
	{
		return textList;
	}

	void buildMenu(ContactItem item)
	{
		contact = item;
		textList.removeAllCommands();
		textList.addCommandEx(JimmUI.cmdMenu, VirtualList.MENU_LEFT_BAR);
		textList.addCommandEx(cmdCloseChat, VirtualList.MENU_RIGHT_BAR);
		textList.addCommandEx(cmdContactMenu, VirtualList.MENU_LEFT);
		textList.addCommandEx(cmdMsgReply, VirtualList.MENU_LEFT);
		textList.addCommandEx(cmdDelChat, VirtualList.MENU_LEFT);
		textList.addCommandEx(cmdCopyText, VirtualList.MENU_LEFT);
		if (!JimmUI.clipBoardIsEmpty())
		{
			textList.addCommandEx(JimmUI.cmdQuote, VirtualList.MENU_LEFT);
			textList.addCommandEx(JimmUI.cmdPaste, VirtualList.MENU_LEFT);
		}
		if ((contact.getBooleanValue(ContactItem.CONTACTITEM_IS_TEMP) ||
			contact.getBooleanValue(ContactItem.CONTACTITEM_PHANTOM)) &&
				!contact.getBooleanValue(ContactItem.CONTACTITEM_NO_AUTH))
			textList.addCommandEx(cmdAddUrs, VirtualList.MENU_LEFT);
		if (contact.getBooleanValue(ContactItem.CONTACTITEM_NO_AUTH))
			textList.addCommandEx(cmdReqAuth, VirtualList.MENU_LEFT);
		//#sijapp cond.if modules_HISTORY is "true" #
		if (!Options.getBoolean(Options.OPTION_HISTORY))
			textList.addCommandEx(cmdAddToHistory, VirtualList.MENU_LEFT);
		//#sijapp cond.end#
		
		checkTextForURL();
		checkForAuthReply();
		
		textList.setCommandListener(this);
	}

	public boolean isVisible()
	{
		if (textList != null) return textList.isActive();
		return false;
	}
	
	private Object getVisibleObject()
	{
		return textList;
	}

	public void commandAction(Command c, Displayable d)
	{
		/* User selected chat to delete */
		if ((currentUiMode == UI_MODE_DEL_CHAT) && (c == JimmUI.cmdOk))
		{
			int delType = -1;

			switch (JimmUI.getLastSelIndex())
			{
			case 0:
				delType = ChatHistory.DEL_TYPE_CURRENT;
				break;
			case 1:
				delType = ChatHistory.DEL_TYPE_ALL_EXCEPT_CUR;
				break;
			case 2:
				delType = ChatHistory.DEL_TYPE_ALL;
				break;
			}

			ChatHistory.chatHistoryDelete(contact.getUinString(), delType);
			ContactList.activate();
			return;
		}

		/* Write new message */
		else if (c == cmdMsgReply)
		{
			JimmUI.writeMessage(contact, null);
		}

		/* Close current chat */
		else if (c == cmdCloseChat)
		{
			contact.resetUnreadMessages();
			textList.uiState = textList.UI_STATE_NORMAL;
			JimmUI.textMessReceiver = null; // на всякий случай...
			ContactList.activate();
		}

		/* Delete current chat */
		else if (c == cmdDelChat)
		{
			currentUiMode = UI_MODE_DEL_CHAT;
			JimmUI.showSelector("delete_chat", JimmUI.stdSelector, this, UI_MODE_DEL_CHAT, true);
		}

		/* Copy selected text to clipboard */
		else if (c == cmdCopyText)
		{
			ChatHistory.copyText(contact.getUinString(), ChatName);
			textList.addCommandEx(JimmUI.cmdQuote, VirtualList.MENU_LEFT);
			textList.addCommandEx(JimmUI.cmdPaste, VirtualList.MENU_LEFT);
		}

		/* Reply with quotation */
		else if ((c == JimmUI.cmdQuote) || (c == JimmUI.cmdPaste))
		{
			JimmUI.writeMessage(contact, JimmUI.getClipBoardText(c == JimmUI.cmdQuote));
		}

		/* Open URL in web brouser */
		//#sijapp cond.if target is "MIDP2" | target is "MOTOROLA" | target is "SIEMENS2"#
		else if (c == JimmUI.cmdGotoURL)
		{
			JimmUI.gotoURL(textList.getCurrText(0, false), getVisibleObject());
		}
		//#sijapp cond.end#

		/* Add temporary or phantom contact to list */
		else if (c == cmdAddUrs)
		{
			Search search = new Search(true);
			String data[] = new String[Search.LAST_INDEX];
			data[Search.UIN] = contact.getUinString();

			SearchAction act = new SearchAction(search, data, SearchAction.CALLED_BY_ADDUSER);

			try
			{
				Icq.requestAction(act);
			}
			catch (JimmException e)
			{
				JimmException.handleException(e);
			}

			SplashCanvas.addTimerTask("wait", act, false);
		}

		/* Add selected text to history */
		//#sijapp cond.if modules_HISTORY is "true" #
		else if (c == cmdAddToHistory)
		{
			int textIndex = textList.getCurrTextIndex();

			MessData data = (MessData) getMessData().elementAt(textIndex);

			String text = textList.getCurrText(data.getOffset(), false);
			if (text == null) return;

			String uin = contact.getUinString();
			HistoryStorage.addText(uin, text, data.getIncoming() ? (byte) 0 : (byte) 1, 
						data.getIncoming() ? ChatName : Icq.myNick, data.getTime());
		}
		//#sijapp cond.end#

		/* Grant authorization */
		else if (c == cmdGrantAuth)
		{
			contact.setIntValue(ContactItem.CONTACTITEM_AUTREQUESTS, 0);
			removeAuthCommands();

			SystemNotice notice = new SystemNotice
			(
				SystemNotice.SYS_NOTICE_AUTHORISE,
				contact.getUinString(),
				true, ""
			);
			SysNoticeAction sysNotAct = new SysNoticeAction(notice);
			try
			{
				Icq.requestAction(sysNotAct);
			}
			catch (JimmException e)
			{
				JimmException.handleException(e);
				if (e.isCritical()) return;
			}
		}
		
		/* Deny authorization */
		else if (c == cmdDenyAuth)
		{
			JimmUI.authMessage(JimmUI.AUTH_TYPE_DENY, contact, "reason", null);
		}
		
		/* Request autorization */
		else if (c == cmdReqAuth)
		{
			JimmUI.authMessage(JimmUI.AUTH_TYPE_REQ_AUTH, contact, "requauth", "plsauthme");
		}
		
		/* Show contact menu */
		else if (c == cmdContactMenu)
		{
			JimmUI.showContactMenu(contact);
		}
	}

	public void removeAuthCommands()
	{
		textList.removeCommandEx(cmdGrantAuth);
		textList.removeCommandEx(cmdDenyAuth);
	}

	static int getInOutColor(boolean incoming)
	{
		return incoming ? Options.getInt(Options.OPTION_COLOR_NICK) : Options.getInt(Options.OPTION_COLOR_MY_NICK);
	}

	Vector getMessData()
	{
		return messData; 
	}

	public void vlCursorMoved(VirtualList sender) 
	{
		checkTextForURL();
	}
	
	void checkTextForURL()
	{
		//#sijapp cond.if target is "MIDP2" | target is "SIEMENS2" | target is "MOTOROLA"#
		textList.removeCommandEx(JimmUI.cmdGotoURL);
		int messIndex = textList.getCurrTextIndex();
		if (messIndex != -1)
		{
			MessData md = (MessData) getMessData().elementAt(messIndex);
			if (md.isURL()) textList.addCommandEx(JimmUI.cmdGotoURL, VirtualList.MENU_LEFT);
		}
		//#sijapp cond.end#
	}

	void checkForAuthReply()
	{
		if (contact.isMessageAvailable(ContactItem.MESSAGE_AUTH_REQUEST))
		{
			textList.addCommandEx(cmdGrantAuth, VirtualList.MENU_LEFT);
			textList.addCommandEx(cmdDenyAuth, VirtualList.MENU_LEFT);
		}
	}

	public void setImage(Icon img)
	{
		textList.setCapImage(img);
	}

	public void setXstImage(Icon img)
	{
		textList.setCapXstImage(img);
	}

	public void setHappyImage(Icon img)
	{
		textList.setCapHappyImage(img);
	}
/*
	public void setClientImage(Icon img)
	{
		textList.setCapClientImage(img);
	}
*/
	public void vlItemClicked(VirtualList sender) {}

	public void vlKeyPress(VirtualList sender, int keyCode, int type)
	{
		if (JimmUI.execDoubleHotKey(contact, keyCode, type))
		{
			return;
		}

		switch (keyCode)
		{
			// #sijapp cond.if target is "MIDP2" | target is "MOTOROLA"#
			case -8:
				currentUiMode = UI_MODE_DEL_CHAT;
				//#sijapp cond.if target is "MOTOROLA"#
				if (type == VirtualList.KEY_RELEASED)
				//#sijapp cond.else#
				if (type == VirtualList.KEY_PRESSED)
				//#sijapp cond.end#
					JimmUI.showSelector("delete_chat", JimmUI.stdSelector, this, UI_MODE_DEL_CHAT, true);
				return;
			// #sijapp cond.end#

			case Canvas.KEY_STAR:
				ChatHistory.copyText(contact.getUinString(), ChatName);
				textList.addCommandEx(JimmUI.cmdQuote, VirtualList.MENU_LEFT);
				textList.addCommandEx(JimmUI.cmdPaste, VirtualList.MENU_LEFT);
				return;

			case Canvas.KEY_POUND:
				if (!JimmUI.clipBoardIsEmpty())
				{
					try
					{
						JimmUI.writeMessage(contact, JimmUI.getClipBoardText(true));
					}
					catch (Exception e)
					{
						Alert alert = new Alert(ResourceBundle.getString("text_too_long"));
						Jimm.display.setCurrent(alert, Jimm.display.getCurrent());
					}
				}
				return;
		}

		try // getGameAction can raise exception
		{
			//#sijapp cond.if target is "MOTOROLA"#
			if (type == VirtualList.KEY_RELEASED)
			//#sijapp cond.else#
			if (type == VirtualList.KEY_PRESSED)
			//#sijapp cond.end#
			{
				String currUin;

				switch (sender.getGameAction(keyCode))
				{
					case Canvas.LEFT:
						currUin = ContactList.showNextPrevChat(false);
						ChatHistory.calcCounter(currUin);
						return;
				
					case Canvas.RIGHT:
						currUin = ContactList.showNextPrevChat(true);
						ChatHistory.calcCounter(currUin);
						return;
				}
			}
			JimmUI.execHotKey(contact, keyCode, type);
		}
		catch(Exception e) {}
	}
	
	//#sijapp cond.if target isnot "DEFAULT"#
	public void BeginTyping(boolean type)
	{
		textList.repaint();
	}
	//#sijapp cond.end#

	public void AckMessage(long msgId, Icon image, boolean removeRecord)
	{
		textList.AckMessage(msgId, image, removeRecord);
	}	

	void addTextToForm(String from, String message, String url, long time, boolean red, boolean offline, Icon image, long msgId)
	{
		int texOffset = 0;
		boolean xTraz = (time == 0);
		
		textList.lock();
		int lastSize = textList.getSize();
		
		StringBuffer messHeader = new StringBuffer();

		if (image != null) 
		{
			textList.addMsgHeaderImage(image, messTotalCounter, msgId);
			//messHeader.append(" ");
		}

		messHeader.append(!xTraz ? from + " (" + Util.getDateString(!offline, Options.getBoolean(Options.OPTION_CHAT_SECONDS), time) + "): " : from + " ");

		if (messHeader.length() != 0)
		{
			textList.addBigText(messHeader.toString(), getInOutColor(red), Font.STYLE_BOLD, messTotalCounter);

			// new line after MessHeader
			if (!offline && !xTraz) textList.doCRLF(messTotalCounter);

			texOffset = !xTraz ? (textList.getTextByIndex(0, false, messTotalCounter)).length() : 0;
		}
		else texOffset = 0;
		
		if (url.length() > 0)
		{
			textList.addBigText(ResourceBundle.getString("url") + ": " + url, 0x00FF00, Font.STYLE_PLAIN, messTotalCounter);
		}

		JimmUI.addMessageText(textList, message, !xTraz ? textList.getTextColor() : getInOutColor(red), messTotalCounter);
		
		boolean contains_url = false;
		//#sijapp cond.if target is "MIDP2" | target is "SIEMENS2" | target is "MOTOROLA"#
		if (Util.parseMessageForURL(message) != null) 
		{
			contains_url = true;
			if (texOffset == 1) textList.addCommandEx(JimmUI.cmdGotoURL, VirtualList.MENU_LEFT);
		}
		//#sijapp cond.end#
		getMessData().addElement(new MessData(red, time, texOffset, contains_url));
		messTotalCounter++;
		
		if (!red || (textList.getCurrTextIndex() >= (messData.size() - 2))) textList.setTopItem(lastSize);

		textList.unlock();
	}
	
	public void activate(boolean initChat, boolean resetText)
	{
		currentUiMode = UI_MODE_NONE;
		textList.activate(Jimm.display);
		JimmUI.setLastScreen(textList);
		ChatHistory.currentChat = this;
		contact.resetUnreadMessages();
	}
}

public class ChatHistory
{
	static private ChatHistory _this;
	static private Hashtable historyTable;
	static private int counter;

	public ChatHistory()
	{
		_this = this;
	}

	static
	{
		historyTable = new Hashtable();
		counter = 1;
		//#sijapp cond.if target is "MOTOROLA"#
		if ((Options.getBoolean(Options.OPTION_CHAT_SMALL_FONT)) || (Options.getInt(Options.OPTION_CL_FONT_SIZE) == 2))
		{
			TPropFont.font = new TPropFont("/font.prs");
			TPropFont.font.setImage("/font.png");
		}
		//#sijapp cond.end#
	}

//	//#sijapp cond.if target is "MOTOROLA"#
//	public static void loadGraphicFont()
//	{
//		if (Options.getBoolean(Options.OPTION_CHAT_SMALL_FONT))
//		{
//			TPropFont.font = new TPropFont("/font.prs");
//			TPropFont.font.setImage("/font.png");
//		}
//	}
//	//#sijapp cond.end#


	private static Icon image = null;

	/* Adds a message to the message display */
	static protected synchronized void addMessage(ContactItem contact, Message message)
	{
		String uin = contact.getUinString();
		if (!historyTable.containsKey(uin)) newChatForm(contact, contact.name);

		ChatTextList chat = (ChatTextList)historyTable.get(uin);

		boolean offline = message.getOffline();
		boolean showImage = Options.getBoolean(Options.OPTION_CHAT_IMAGE);
		boolean visible = chat.isVisible(); 

		if (message instanceof PlainMessage)
		{
			PlainMessage plainMsg = (PlainMessage) message;
			image = showImage ? ContactList.imageList.elementAt(14) : null;
			if (!visible) contact.increaseMessageCount(ContactItem.MESSAGE_PLAIN);

			addTextToForm(uin, contact.name, plainMsg.getText(), "", plainMsg.getNewDate(), true, offline, image, 0);
			//#sijapp cond.if modules_HISTORY is "true" #
			if (Options.getBoolean(Options.OPTION_HISTORY))
				HistoryStorage.addText(uin, plainMsg.getText(), (byte)0, contact.name, plainMsg.getNewDate());
			//#sijapp cond.end#
			if (!offline)
			{
				/* Popup window */
				JimmUI.showPopupWindow(uin, contact.name, plainMsg.getText());
				/* Show creeping line */
				JimmUI.showCreepingLine(JimmUI.getCurrentScreen(), contact, plainMsg.getText()); //old variant
			}
		}
		else if (message instanceof UrlMessage)
		{
			UrlMessage urlMsg = (UrlMessage) message;
			image = showImage ? ContactList.imageList.elementAt(14) : null;
			if (!chat.isVisible()) contact.increaseMessageCount(ContactItem.MESSAGE_URL);

			addTextToForm(uin, contact.name, urlMsg.getText(), urlMsg.getUrl(), urlMsg.getNewDate(), false, offline, image, 0);
		}
		else if (message instanceof SystemNotice)
		{
			String text = "";
			SystemNotice notice = (SystemNotice) message;
			if (!visible) contact.increaseMessageCount(ContactItem.MESSAGE_SYS_NOTICE);

			if (notice.getSysnotetype() == SystemNotice.SYS_NOTICE_YOUWEREADDED)
			{
				image = showImage ? ContactList.imageList.elementAt(17) : null;
				text = ResourceBundle.getString("youwereadded") + notice.getSndrUin();
			}
			else if (notice.getSysnotetype() == SystemNotice.SYS_NOTICE_DELETE)
			{
				image = showImage ? ContactList.imageList.elementAt(17) : null;
				text = "User " + notice.getReason() + ResourceBundle.getString(" has removed himself from your contact list");
			}
			else if (notice.getSysnotetype() == SystemNotice.SYS_NOTICE_AUTHREQ)
			{
				contact.increaseMessageCount(ContactItem.MESSAGE_AUTH_REQUEST);
				image = showImage ? ContactList.imageList.elementAt(16) : null;
				text = notice.getSndrUin() + ResourceBundle.getString("wantsyourauth") + notice.getReason();
			}
			else if (notice.getSysnotetype() == SystemNotice.SYS_NOTICE_AUTHREPLY)
			{
				if (notice.isAUTH_granted())
				{
					contact.setBooleanValue(ContactItem.CONTACTITEM_NO_AUTH,false);
					image = showImage ? ContactList.imageList.elementAt(17) : null;
					text = ResourceBundle.getString("grantedby") + notice.getSndrUin();
				}
				else if (notice.getReason() != null)
				{
					image = showImage ? ContactList.imageList.elementAt(16) : null;
					text = ResourceBundle.getString("denyedby") + notice.getSndrUin() + ". " + ResourceBundle.getString("reason") + ": " + notice.getReason();
				}
				else
				{
					image = showImage ? ContactList.imageList.elementAt(16) : null;
					text = ResourceBundle.getString("denyedby") + notice.getSndrUin() + ". " + ResourceBundle.getString("noreason");
				}
			}
			addTextToForm(uin, ResourceBundle.getString("sysnotice"), text, "", notice.getNewDate(), false, offline, image, 0);
			MagicEye.addAction(uin, text);
		}
		chat.checkTextForURL();
		chat.checkForAuthReply();
	}

	static public synchronized void AckMessage(String uin, long msgId, boolean isUserAck)
	{
		if (historyTable.containsKey(uin))
		{
			int imageIndex = isUserAck ? 21 : 20;
			if (imageIndex >= ContactList.imageList.size())
			{
				return;
			}
			Icon img = ContactList.imageList.elementAt(imageIndex);

			// remove record from undelivered messages table 
			boolean removeRecord = isUserAck || !Options.getBoolean(Options.OPTION_DELIVERY_REPORT); 

			ChatTextList msgDisplay = (ChatTextList) historyTable.get(uin);
			msgDisplay.AckMessage(msgId, img, removeRecord);
		}
	}

	static protected synchronized void addMyMessage(ContactItem contact, String message, long time, String ChatName, long msgId)
	{
		String uin = contact.getUinString();
		if (!historyTable.containsKey(uin)) newChatForm(contact, ChatName);
		image = Options.getBoolean(Options.OPTION_CHAT_IMAGE) ? ContactList.imageList.elementAt(14) : null;
		addTextToForm(uin, Icq.myNick, message, "", time, false, false, image, msgId);
	}
	
	// Add text to message form
	static synchronized public void addTextToForm(String uin,String from, String message, String url, long time, boolean red, boolean offline, Icon image, long msgId)
	{
		ChatTextList msgDisplay = (ChatTextList) historyTable.get(uin);
		msgDisplay.addTextToForm(from, message, url, time, red, offline, image, msgId);
	}
	
	static private MessData getCurrentMessData(String uin)
	{
		ChatTextList list = getChatHistoryAt(uin);
		int messIndex = list.textList.getCurrTextIndex();
		if (messIndex == -1) return null;
		MessData md = (MessData)list.getMessData().elementAt(messIndex);
		return md;
	}
	
	static public String getCurrentMessage(String uin)
	{
		return getChatHistoryAt(uin).textList.getCurrText(getCurrentMessData(uin).getOffset(), false);
	}
	
	static public void copyText(String uin, String from)
	{
		ChatTextList list = getChatHistoryAt(uin);
		int messIndex = list.textList.getCurrTextIndex();
		if (messIndex == -1) return;
		MessData md = (MessData)list.getMessData().elementAt(messIndex);
		
		JimmUI.setClipBoardText
		(
			md.getIncoming(), Util.getDateString(true, true, md.getTime()),
			md.getIncoming() ? from : Icq.myNick, getCurrentMessage(uin)
		);
	}

	// Returns the chat history form at the given uin
	static public ChatTextList getChatHistoryAt(String uin)
	{
		if (historyTable.containsKey(uin)) return (ChatTextList) historyTable.get(uin);
		else return null;
	}
	
	final static public int DEL_TYPE_CURRENT        = 1;
	final static public int DEL_TYPE_ALL_EXCEPT_CUR = 2;
	final static public int DEL_TYPE_ALL            = 3;
	
	static public void chatHistoryDelete(String uin)
	{
		ContactItem cItem = ContactList.getItembyUIN(uin);
		historyTable.remove(uin);
		
		cItem.setBooleanValue(ContactItem.CONTACTITEM_HAS_CHAT, false);
		cItem.setIntValue(ContactItem.CONTACTITEM_PLAINMESSAGES, 0);
		cItem.setIntValue(ContactItem.CONTACTITEM_URLMESSAGES, 0);
		cItem.setIntValue(ContactItem.CONTACTITEM_SYSNOTICES, 0);
	}

	// Delete the chat history for uin
	static public void chatHistoryDelete(String uin, int delType)
	{
		switch (delType)
		{
		case DEL_TYPE_CURRENT:
			chatHistoryDelete(uin);
			break;
			
		case DEL_TYPE_ALL_EXCEPT_CUR:
		case DEL_TYPE_ALL:
			Enumeration AllChats = historyTable.keys();
			while (AllChats.hasMoreElements())
			{
				String key = (String)AllChats.nextElement();
				if ((delType == DEL_TYPE_ALL_EXCEPT_CUR) && (key.equals(uin))) continue;
				chatHistoryDelete(key);
			}
			break;
		}
	}

	// Returns if the chat history at the given number is shown
	static public boolean chatHistoryShown(String uin)
	{
		if (historyTable.containsKey(uin))
		{
			ChatTextList temp = (ChatTextList)historyTable.get(uin);
			return temp.isVisible();
		}
		else return false;
	}

	// Returns true if chat history exists for this uin
	static public boolean chatHistoryExists(String uin)
	{
		return historyTable.containsKey(uin);
	}

	
	// Creates a new chat form
	static public void newChatForm(ContactItem contact, String name)
	{
		ChatTextList chatForm = new ChatTextList(name, contact);
		String uin = contact.getUinString();
		historyTable.put(uin, chatForm);
		UpdateCaption(uin);
		contact.setBooleanValue(ContactItem.CONTACTITEM_HAS_CHAT, true);
		//#sijapp cond.if modules_HISTORY is "true" #
		fillFormHistory(contact);
		//#sijapp cond.end#
		contact.setStatusImage();
	}
	
	// fill chat with last history lines
	//#sijapp cond.if modules_HISTORY is "true" #
	final static private int MAX_HIST_LAST_MESS = 5;

	static public void fillFormHistory(ContactItem contact)
	{
		String name = contact.name;
		String uin = contact.getUinString();
		if (Options.getBoolean(Options.OPTION_SHOW_LAST_MESS))
		{
			int recCount = HistoryStorage.getRecordCount(uin);
			if (recCount == 0) return;
			 
			if (!chatHistoryExists(uin)) newChatForm(contact, name);
			ChatTextList chatForm = (ChatTextList) historyTable.get(uin);
			if (chatForm.textList.getSize() != 0) return;
			
			int insSize = (recCount > MAX_HIST_LAST_MESS) ? MAX_HIST_LAST_MESS : recCount;  
			for (int i = recCount - insSize; i < recCount; i++)
			{
				CachedRecord rec = HistoryStorage.getRecord(uin, i);
				chatForm.textList.addBigText
				(
					"[" + rec.from + " " + rec.date + "]", 
					ChatTextList.getInOutColor(rec.type == 0),
					Font.STYLE_PLAIN,
					-1
				);
				chatForm.textList.doCRLF(-1);
				
				//#sijapp cond.if modules_SMILES is "true" #
				Emotions.addTextWithEmotions(chatForm.textList, rec.text, Font.STYLE_PLAIN, 0x808080, -1);
				//#sijapp cond.else#
				chatForm.textList.addBigText(rec.text, 0x808080, Font.STYLE_PLAIN, -1);
				//#sijapp cond.end#
				chatForm.textList.doCRLF(-1);
			}
		}
	}
	//#sijapp cond.end#
	
	static public void contactRenamed(String uin, String newName)
	{
		ChatTextList temp = (ChatTextList)historyTable.get(uin);
		if (temp == null) return;
		temp.ChatName = newName;
		UpdateCaption(uin);
	}

	static public void UpdateCaption(String uin)
	{
		calcCounter(uin);
		ChatTextList temp = (ChatTextList)historyTable.get(uin);

		// Calculate the title for the chatdisplay.
		String Title = temp.ChatName + " (" + counter + "/" + historyTable.size() + ")";
		temp.textList.setCaption(Title);
	}

	static public void setColorScheme()
	{
		Enumeration AllChats = historyTable.elements();
		while (AllChats.hasMoreElements())
			JimmUI.setColorScheme(((ChatTextList)AllChats.nextElement()).textList, Options.getBoolean(Options.OPTION_FULL_SCREEN));
	}
	
	// Sets the counter for the ChatHistory
	static public void calcCounter(String curUin)
    {
		if (curUin == null) return;
		Enumeration AllChats = historyTable.elements();
		Object chat = historyTable.get(curUin);
		counter = 1;
		while (AllChats.hasMoreElements())
		{
			if (AllChats.nextElement() == chat) break;
			counter++;
		}
    }

	public static boolean activateIfExists(ContactItem item)
	{
		if (item == null) return false;
		
		ChatTextList chat = getChatHistoryAt(item.getUinString());
		if (chat != null)
		{
			chat.buildMenu(item);
			chat.activate(false, false);
		}
		return (chat != null);
	}
	
	static ChatTextList currentChat;
	
	public static ChatTextList getCurrent()
	{
		return currentChat;
	}
	
	public static void removeAuthCommands(ContactItem item)
	{
		if (item == null) return;
		ChatTextList chat = getChatHistoryAt(item.getUinString());
		if (chat != null) chat.removeAuthCommands();
	}
}
