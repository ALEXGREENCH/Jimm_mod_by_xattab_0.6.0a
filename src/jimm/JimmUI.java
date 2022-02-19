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
 File: src/jimm/JimmUI.java
 Version: ###VERSION###  Date: ###DATE###
 Author(s): Artyomov Denis, Igor Palkin, Andreas Rossbacher
 *******************************************************************************/

package jimm;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import javax.microedition.io.Connector;
import javax.microedition.io.HttpConnection;
import javax.microedition.lcdui.*;

import jimm.comm.*;
import jimm.util.*;
import DrawControls.*;

public class JimmUI implements CommandListener
{
	// Last screen constants
	public static Object lastScreen;
	
	static private TextList msgBoxList;

	public static void setLastScreen(Object screen)
	{
		lastScreen = screen;
	}
	
	public static void backToLastScreen()
	{
		selectScreen(lastScreen);
		//#sijapp cond.if target is "MOTOROLA"#
		LightControl.flash(false);
		//#sijapp cond.end#
	}
	
	public static void selectScreen(Object screen)
	{
		if (screen instanceof VirtualList)
		{
			VirtualList vl = (VirtualList)screen;
			vl.activate(Jimm.display);
		}
		else if (screen instanceof Displayable)
		{
			Jimm.display.setCurrent((Displayable)screen);
		}
		else
		{
			MainMenu.activate();
		}
	}

	// Commands codes
	final public static int CMD_OK     = 1;
	final public static int CMD_CANCEL = 2;
	final public static int CMD_YES    = 3;
	final public static int CMD_NO     = 4;
	final public static int CMD_FIND   = 5;
	final public static int CMD_BACK   = 6;
	
	// Commands
	public final static Command cmdOk       = new Command(ResourceBundle.getString("ok"),            Command.OK,     1);
	//#sijapp cond.if target is "MIDP2"#
	public static Command cmdCancel   = new Command(ResourceBundle.getString("cancel"),              Jimm.is_smart_SE() ? Command.CANCEL : Command.BACK, 14);
	public final static Command cmdBack     = new Command(ResourceBundle.getString("back"),          Jimm.is_smart_SE() ? Command.CANCEL : Command.BACK, 2);
	//#sijapp cond.else#
	//#sijapp cond.if target is "MOTOROLA"#
	public static Command cmdCancel   = new Command(ResourceBundle.getString("cancel"),              Command.BACK,   14);
	//#sijapp cond.else#
	public final static Command cmdCancel   = new Command(ResourceBundle.getString("cancel"),        Command.BACK,   14);
	//#sijapp cond.end#
	public final static Command cmdBack     = new Command(ResourceBundle.getString("back"),          Command.BACK,   2);
	//#sijapp cond.end#
	public final static Command cmdYes      = new Command(ResourceBundle.getString("yes"),           Command.OK,     1);
	public final static Command cmdNo       = new Command(ResourceBundle.getString("no"),            Command.CANCEL, 2);
	public final static Command cmdFind     = new Command(ResourceBundle.getString("find"),          Command.ITEM,   1);
	public final static Command cmdCopyText = new Command(ResourceBundle.getString("copy_text"),     Command.ITEM,   4);
	public final static Command cmdCopyAll  = new Command(ResourceBundle.getString("copy_all_text"), Command.ITEM,   5);
	public final static Command cmdEdit     = new Command(ResourceBundle.getString("edit"),          Command.ITEM,   1);
	public final static Command cmdNewPass  = new Command(ResourceBundle.getString("change_pass"),   Command.ITEM,   2);
	public final static Command cmdMenu     = new Command(ResourceBundle.getString("option"),        Command.ITEM,   1);
	public final static Command cmdSelect   = new Command(ResourceBundle.getString("select"),        Command.OK,     1);
	//#sijapp cond.if target is "MIDP2" | target is "MOTOROLA"#
	public static Command cmdSend     = new Command(ResourceBundle.getString("send"),                 Command.OK,     1);
	//#sijapp cond.else#
	public final static Command cmdSend     = new Command(ResourceBundle.getString("send"),          Command.OK, Jimm.is_SGold() ? 1 : 15);
	//#sijapp cond.end#

	/* Quote and Paste command */
	public static final Command cmdQuote    = new Command(ResourceBundle.getString("quote"),         Command.ITEM,   4);
	public static final Command cmdPaste    = new Command(ResourceBundle.getString("paste"),         Command.ITEM,   4);

	//#sijapp cond.if modules_SMILES is "true" #
	public final static Command cmdInsertEmo = new Command(ResourceBundle.getString("insert_emotion"), Command.ITEM, 3);
	//#sijapp cond.end#

	public final static Command cmdInsTemplate = new Command(ResourceBundle.getString("templates"), Command.ITEM,    4);

	//#sijapp cond.if target is "MIDP2" | target is "SIEMENS2" | target is "MOTOROLA"#
	public final static Command cmdGotoURL = new Command(ResourceBundle.getString("goto_url"),      Command.ITEM,    7);
	//#sijapp cond.end#

    public final static Command transCmd   = new Command(ResourceBundle.getString("transliterate"),   Command.ITEM,  8);
    public final static Command detransCmd = new Command(ResourceBundle.getString("detransliterate"), Command.ITEM,  9);

	public final static Command nextCmd = new Command(ResourceBundle.getString("next"),             Command.ITEM,   10);
	public final static Command prevCmd = new Command(ResourceBundle.getString("prev"),             Command.ITEM,   11);

	public final static Command cmdClearText = new Command(ResourceBundle.getString("clear"),       Command.ITEM,   12);

	public final static Command cmdVisitCite = new Command(ResourceBundle.getString("visit_site"),   Command.ITEM,   5);
	public final static Command cmdVisitForum = new Command(ResourceBundle.getString("visit_forum"), Command.ITEM,   6);


	static private CommandListener listener;
	static private Hashtable commands = new Hashtable();
	static private JimmUI _this;

	private static int[] groupList;

	// Misc constants
	final private static int GROUP_SELECTOR_MOVE_TAG = 5;

	// Associate commands and commands codes
	static 
	{
		commands.put(cmdOk,     new Integer(CMD_OK)    );
		commands.put(cmdCancel, new Integer(CMD_CANCEL));
		commands.put(cmdYes,    new Integer(CMD_YES)   );
		commands.put(cmdNo,     new Integer(CMD_NO)    );
		commands.put(cmdFind,   new Integer(CMD_FIND)  );
		commands.put(cmdBack,   new Integer(CMD_BACK)  );

		//#sijapp cond.if target is "MIDP2" | target is "MOTOROLA"#
		if (Options.getBoolean(Options.OPTION_SWAP_SEND_AND_BACK))
		{
			cmdSend   = new Command(ResourceBundle.getString("send"),   Command.CANCEL, 1);
			cmdCancel = new Command(ResourceBundle.getString("cancel"), Command.ITEM, 14);
		}
		//#sijapp cond.end#
	}
	
	JimmUI()
	{
		_this = this;
	}
	
	// Returns commands index of command
	public static int getCommandIdx(Command command)
	{
		Object result = commands.get(command);
		return (result == null) ? -1 : ((Integer)result).intValue();  
	}
	
	// Place "object = null;" code here:
	private static void clearAll()
	{
		msgForm = null;
		aboutTextList = null;
		System.gc();
	}
	
	private boolean chatExists(ContactItem item)
	{
		return ChatHistory.activateIfExists(item);
	}

	private static void setPrivateListsID(int index, ContactItem item)
	{
		Action act = new ServerListsAction(index, item);
		try
		{
			Icq.requestAction(act);
		}
		catch (JimmException e)
		{
			JimmException.handleException(e);
		}
	}

	private static boolean showTyping()
	{
		return ((Options.getInt(Options.OPTION_TYPING_NOTIF_MODE) > 0) && ((textMessReceiver.caps & Util.CAPF_TYPING) != 0)
				&& (Options.getInt(Options.OPTION_PRIVATE_STATUS) != OtherAction.PSTATUS_NONE)
				&& ((Options.getInt(Options.OPTION_PRIVATE_STATUS) != OtherAction.PSTATUS_VISIBLE_ONLY) || (textMessReceiver.getVisibleId() != 0)));

	}

	public void commandAction(Command c, Displayable d)
	{
		if (isControlActive(serverLists))
		{
			if (c == cmdSelect)
			{
				int visId = clciContactMenu.getVisibleId();
				int invisId = clciContactMenu.getInvisibleId();
				int index = serverLists.getCurrTextIndex();

				setPrivateListsID(index, clciContactMenu);
/*
				if (((visId == 0) && (invisId != 0) && (index == ServerListsAction.VISIBLE_LIST)))
				{
					setPrivateListsID(ServerListsAction.INVISIBLE_LIST, clciContactMenu);
				}

				if (((visId != 0) && (invisId == 0) && (index == ServerListsAction.INVISIBLE_LIST)))
				{
					setPrivateListsID(ServerListsAction.VISIBLE_LIST, clciContactMenu);
				}
*/
				serverLists = null;
				ContactList.activate();
			}
			else
			{
				serverLists = null;
				ContactList.activate();
			}
		}

		else if (isControlActive(removeContactMessageBox))
		{
			if (c == cmdOk) menuRemoveContactSelected();
			else
			{
				backToLastScreen();
				removeContactMessageBox = null;
			}
		}
		
		else if ((renameTextbox != null) && (d == renameTextbox))
		{
			if (c == cmdOk) menuRenameSelected();
			else
			{
				backToLastScreen();
				renameTextbox = null;
			}
		}
		
		else if (isControlActive(removeMeMessageBox))
		{
			if (c == cmdOk) menuRemoveMeSelected();
			else
			{
				backToLastScreen();
				removeMeMessageBox = null;
			}
		}
		
		else if (isControlActive(tlContactMenu))
		{
			if (c == cmdSelect) contactMenuSelected(tlContactMenu.getCurrTextIndex());
			else
			{
				backToLastScreen();
				tlContactMenu = null;
				clciContactMenu = null;
			}
		}
		
		else if ((authTextbox != null) && (d == authTextbox))
		{
			if (c == cmdSend)
			{
				SystemNotice notice = null;

				/* If or if not a reason was entered
				 Though this box is used twice (reason for auth request and auth reply)
				 we have to distinguish what we wanna do requReason is used for that */
				String textBoxText = authTextbox.getString();
				String reasonText = (textBoxText == null || textBoxText.length() < 1) ? "" : textBoxText;
				
				switch (authType)
				{
				case AUTH_TYPE_DENY:
					notice = new SystemNotice(SystemNotice.SYS_NOTICE_AUTHORISE, authContactItem.getUinString(), false, reasonText);
					break;
				case AUTH_TYPE_REQ_AUTH:
					notice = new SystemNotice(SystemNotice.SYS_NOTICE_REQUAUTH, authContactItem.getUinString(), false, reasonText);
					break;
				}
				
				/* Assemble the sysNotAction and request it */
				SysNoticeAction sysNotAct = new SysNoticeAction(notice);
				UpdateContactListAction updateAct = new UpdateContactListAction(authContactItem, UpdateContactListAction.ACTION_REQ_AUTH);

				try
				{
					Icq.requestAction(sysNotAct);
					if (authContactItem.getBooleanValue(ContactItem.CONTACTITEM_IS_TEMP))
						Icq.requestAction(updateAct);
				}
				catch (JimmException e)
				{
					JimmException.handleException(e);
					if (e.isCritical()) return;
				}
				
				switch (authType)
				{
				case AUTH_TYPE_DENY:
					authContactItem.setIntValue(ContactItem.CONTACTITEM_AUTREQUESTS, 0);
					ChatHistory.removeAuthCommands(authContactItem);
					break;
				case AUTH_TYPE_REQ_AUTH:
					authContactItem.setBooleanValue(ContactItem.CONTACTITEM_IS_TEMP, false);
					break;
				}
			}

			if (!chatExists(authContactItem)) ContactList.activate();

			authTextbox = null;
			authContactItem = null;
			
			return;
		}
		
		//#sijapp cond.if target is "MIDP2" | target is "MOTOROLA" | target is "SIEMENS2"#
		if (JimmUI.isControlActive(URLList))
		{
			if (c == cmdSelect)
			{
				try
				{
					Jimm.jimm.platformRequest(URLList.getCurrText(0, false));
				}
				catch (Exception e) {}
			}
			selectScreen(lastScreenBeforeUrlSelect);
			URLList = null;
			lastScreenBeforeUrlSelect = null;
			return;
		}
		//#sijapp cond.end#
		
		else if ((messageTextbox != null) && (d == messageTextbox))
		{
			boolean activated;

			//#sijapp cond.if target isnot "DEFAULT"#
			if (!Options.getBoolean(Options.OPTION_MESS_NOTIF_TYPE))
			{
				if (((c == cmdCancel) || (c == cmdSend)) && showTyping())
				{
					try
					{
						Jimm.jimm.getIcqRef().beginTyping(textMessReceiver.getUinString(), false);
					}
					catch (JimmException e) {}
				}
			}
			//#sijapp cond.end#

			if (c == cmdCancel)
			{
				backToLastScreen();
			}

			else if (c == cmdSend)
			{
				switch (textMessCurMode)
				{
				case EDITOR_MODE_MESSAGE:
					String messText = getString();

					if (messText.length() != 0)
					{
						sendMessage(messText, textMessReceiver);
						//#sijapp cond.if target is "MIDP2"#
						setString(null);
						if (Jimm.is_phone_SE()) messageTextbox = null;
						//#sijapp cond.else#
						setString(null);
						//#sijapp cond.end#
						if (!chatExists(textMessReceiver)) backToLastScreen();
					}
					else
					{
						backToLastScreen();
					}
					break;
				}
			}

			//#sijapp cond.if modules_SMILES is "true" #
			else if (c == cmdInsertEmo)
			{
				Emotions.selectEmotion(messageTextbox, messageTextbox);
			}
			//#sijapp cond.end#
			
			else if (c == cmdInsTemplate)
			{
				Templates.selectTemplate(messageTextbox, messageTextbox);
			}

			else if (c == cmdClearText)
			{
				setString(null);
			}

			/* Reply with quotation */
			else if ((c == cmdQuote) || (c == cmdPaste))
			{
				//#sijapp cond.if target is "MOTOROLA"#
				int caretPos = messageTextbox.getString().length();
				//#sijapp cond.else#
				int caretPos = messageTextbox.getCaretPosition();
				//#sijapp cond.end#
				insert(getClipBoardText(c == cmdQuote), caretPos);
			}

			else if (c == detransCmd)
			{
				messageTextbox.setString(StringConvertor.detransliterate(messageTextbox.getString()));
			}

			else if (c == transCmd)
			{
				messageTextbox.setString(StringConvertor.transliterate(messageTextbox.getString()));
			}

			else if (c == nextCmd)
			{
				switchText(current + 1);
			}

			else if (c == prevCmd)
			{
				switchText(current - 1);
			}
		}

		// "About" -> "Back"
		else if (JimmUI.isControlActive(aboutTextList))
		{
			if (c == cmdBack)
			{
				MainMenu.activate();
				aboutTextList = null;
			}

			else if (c == cmdVisitCite)
			{
				try
				{
					Jimm.jimm.platformRequest("http://jimm.im/wap");
				}
				catch (Exception e) {}
			}

			else if (c == cmdVisitForum)
			{
				try
				{
					Jimm.jimm.platformRequest("http://wapland.org/forum");
				}
				catch (Exception e) {}
			}
		}

		// "User info"
		else if (JimmUI.isControlActive(infoTextList))
		{
			// "User info" -> "Cancel, Back"
			if ((c == cmdCancel) || (c == cmdBack))
			{
				backToLastScreen();
			}

			if (c == cmdEdit)
			{
				EditInfo.showEditForm(last_user_info, Jimm.display.getCurrent());
			}

			if (c == cmdNewPass)
			{
				EditInfo.showChangePassForm(Jimm.display.getCurrent());
			}

			// "User info" -> "Copy text, Copy all"
			else if ((c == cmdCopyText) || (c == cmdCopyAll))
			{
				JimmUI.setClipBoardText
				(
					"[" + getCaption(infoTextList) + "]\n" + infoTextList.getCurrText(0, (c == cmdCopyAll))
				);
			}
		}

		// "Selector"
		else if (isControlActive(lstSelector))
		{
			lastSelectedItemIndex = lstSelector.getCurrTextIndex();

			// "Selector" -> "Cancel"
			if (c == cmdCancel)
			{
				backToLastScreen();
				resetLstSelector();
			}

			// "Selector" -> "Ok"
			else if (c == cmdOk)
			{
				// User have selected new group for contact
				if (curScreenTag == GROUP_SELECTOR_MOVE_TAG)
				{
					int currGroupId = clciContactMenu.getIntValue(ContactItem.CONTACTITEM_GROUP);
					int newGroupId = groupList[JimmUI.getLastSelIndex()];
					GroupItem oldGroup = ContactList.getGroupById(currGroupId);
					GroupItem newGroup = ContactList.getGroupById(newGroupId);
					UpdateContactListAction act = new UpdateContactListAction(clciContactMenu, oldGroup, newGroup);

					try
					{
						Icq.requestAction(act);
						SplashCanvas.addTimerTask("wait", act, false);
					}
					catch (JimmException e)
					{
						JimmException.handleException(e);
					}
				}
				else
				{
					listener.commandAction(c, d);
				}

				resetLstSelector();
			}
		}

		// Message box
		else if ((msgForm != null) && (d == msgForm))
		{
			listener.commandAction(c, d);
			msgForm = null;
			curScreenTag = -1;
		}
	}

	private static void resetLstSelector()
	{
		lstSelector = null;
		listener = null;
		curScreenTag = -1;
	}

	public static void setCaption(Object ctrl, String caption)
	{
		if (ctrl instanceof VirtualList)
		{
			VirtualList vl = (VirtualList)ctrl;
			vl.setCaption(caption);
		}
		//#sijapp cond.if target is "MIDP2" | target is "MOTOROLA" | target is "SIEMENS2"#
		else if (ctrl instanceof Displayable)
		{
			((Displayable)ctrl).setTitle(caption);
		}
		//#sijapp cond.end#
	}

	public static String getCaption(Object ctrl)
	{
		if (ctrl == null) return null;
		String result = null;
		if (ctrl instanceof VirtualList)
		{
			VirtualList vl = (VirtualList) ctrl;
			result = vl.getCaption();
		}
		//#sijapp cond.if target is "MIDP2" | target is "MOTOROLA" | target is "SIEMENS2"#
		else if (ctrl instanceof Displayable)
		{
			result = ((Displayable)ctrl).getTitle();
		}
		//#sijapp cond.end#

		return result;
	}
	
	public static int getCurScreenTag()
	{
		if ((msgForm != null) && (msgForm.isShown())) return curScreenTag;
		if (isControlActive(msgBoxList)) return curScreenTag;
		if (isControlActive(lstSelector)) return curScreenTag;
		return -1;
	}

	/////////////////////////
	//                     // 
	//     Message Box     //
	//                     //
	/////////////////////////
	static private Form msgForm;
	static private int curScreenTag = -1;

	public static int getCommandType(Command testCommand, int testTag)
	{
		return (curScreenTag == testTag) ? getCommandIdx(testCommand) : -1;
	}

	final public static int MESBOX_YESNO    = 1;
	final public static int MESBOX_OKCANCEL = 2;

	static public void messageBox(String cap, String text, int type, CommandListener listener, int tag)
	{
		clearAll();
		
		curScreenTag = tag;
		msgForm = new Form(cap);
		msgForm.append(text);
		
		switch (type)
		{
		case MESBOX_YESNO:
			msgForm.addCommand(cmdYes);
			msgForm.addCommand(cmdNo);
			break;
			
		case MESBOX_OKCANCEL:
			msgForm.addCommand(cmdOk);
			msgForm.addCommand(cmdCancel);
			break;
		}

		JimmUI.listener = listener;
		msgForm.setCommandListener(_this);
		Jimm.display.setCurrent(msgForm);
	}
	
	public static TextList showMessageBox(ContactItem contact, String cap, String text, int type)
	{
		msgBoxList = new TextList(cap);
		msgBoxList.setMode(TextList.MODE_TEXT);
		setColorScheme(msgBoxList, false);
		msgBoxList.setFontSize(Options.getInt(Options.OPTION_CL_FONT_SIZE) * 8);
		msgBoxList.addBigText(text, msgBoxList.getTextColor(), Options.getInt(Options.OPTION_CL_FONT_STYLE), -1);
		
		switch (type)
		{
		case MESBOX_YESNO:
			msgBoxList.addCommandEx(cmdYes, TextList.MENU_LEFT_BAR);
			msgBoxList.addCommandEx(cmdNo, TextList.MENU_RIGHT_BAR);
			break;

		case MESBOX_OKCANCEL:
			msgBoxList.addCommandEx(cmdOk, TextList.MENU_LEFT_BAR);
			msgBoxList.addCommandEx(cmdCancel, TextList.MENU_RIGHT_BAR);
			break;
		}
		clciContactMenu = contact;
		msgBoxList.setCommandListener(_this);
		msgBoxList.activate(Jimm.display);
		return msgBoxList; 
	}

	//////////////////////////////////////////////////////////////////////////////
	private static TextList aboutTextList;
    
    // String for recent version
    static private String version;
    
	static public void about(Displayable lastDisplayable_)
	{
		System.gc();
		long freeMem = Runtime.getRuntime().freeMemory() / 1024;

		if (aboutTextList == null) aboutTextList = new TextList(null);
		
		aboutTextList.lock();
		aboutTextList.clear();
		aboutTextList.setMode(TextList.MODE_TEXT);
		setColorScheme(aboutTextList, false);
/*
		aboutTextList.setColors
		(
			Options.getInt(Options.OPTION_COLOR_TEXT),
			Options.getInt(Options.OPTION_COLOR_CAP),
			Options.getInt(Options.OPTION_COLOR_BACK),
			Options.getInt(Options.OPTION_COLOR_BLUE)
		);
*/
		// #sijapp cond.if target is "MOTOROLA"#
		aboutTextList.setFontSize(Font.SIZE_MEDIUM);
		//#sijapp cond.else#
		aboutTextList.setFontSize(Font.SIZE_SMALL);
		//#sijapp cond.end#

		aboutTextList.setCaption(ResourceBundle.getString("about"));
		
		String commaAndSpace = ", "; 
	    
		StringBuffer str = new StringBuffer();
		str.append(" ").append(ResourceBundle.getString("about_info")).append("\n")
		
		   .append(ResourceBundle.getString("midp_info")).append(": \n")
		   .append(Jimm.microeditionPlatform);
		
		if (Jimm.microeditionProfiles != null) str.append(commaAndSpace).append(Jimm.microeditionProfiles);
		
		String locale = System.getProperty("microedition.locale");
		if (locale != null) str.append(commaAndSpace).append(locale);
		
		str.append("\n\n")
		   .append(ResourceBundle.getString("free_heap")).append(": ")
		   .append(freeMem).append("kb\n")
		   .append(ResourceBundle.getString("total_mem")).append(": ")
		   .append(Runtime.getRuntime().totalMemory() / 1024)
		   .append("kb\n\n")
		   .append(ResourceBundle.getString("latest_ver")).append(": ");
		
		if (version != null) str.append(version);
		else str.append("...");
		
		try
		{
			aboutTextList.doCRLF(-1).addBigText(str.toString(), Options.getInt(Options.OPTION_COLOR_TEXT), Font.STYLE_PLAIN, -1);
			aboutTextList.addCommandEx(cmdMenu, VirtualList.MENU_LEFT_BAR);
			aboutTextList.addCommandEx(cmdBack, VirtualList.MENU_RIGHT_BAR);
			aboutTextList.addCommandEx(cmdVisitCite, VirtualList.MENU_LEFT);
			aboutTextList.addCommandEx(cmdVisitForum, VirtualList.MENU_LEFT);
			aboutTextList.setCommandListener(_this);
		}
		catch (Exception e) {}

		aboutTextList.unlock();
		aboutTextList.activate(Jimm.display);

		if (version == null) Jimm.getTimerRef().schedule(new GetVersionInfoTimerTask(), 2000);
	}

	//////////////////////
	//                  //
	//    Clipboard     //
	//                  //
	//////////////////////

    static private String clipBoardText;
    static private String clipBoardHeader;
    static private boolean clipBoardIncoming;
	
	static private String insertQuotingChars(String text, String qChars)
	{
		StringBuffer result = new StringBuffer();
		int size = text.length();
		boolean wasNewLine = true;
		for (int i = 0; i < size; i++)
		{
			char chr = text.charAt(i);
			if (wasNewLine) result.append(qChars);
			result.append(chr);
			wasNewLine = (chr == '\n');
		}
		return result.toString();
	}
	
    static public boolean clipBoardIsEmpty()
    {
        return clipBoardText == null;
    }

    static public String getClipBoardText(boolean quote)
    {
        if (!quote)
        {
            return clipBoardText;
        }

        StringBuffer sb = new StringBuffer();

        if (clipBoardHeader != null)
        {
            sb.append('[').append(clipBoardHeader).append(']').append('\n');
        }

        if (clipBoardText != null)
        {
            sb.append(insertQuotingChars(clipBoardText, clipBoardIncoming ? ">> " : "<< "));
        }
        return sb.toString();
    }
    
    static public void setClipBoardText(String text)
    {
        clipBoardText     = text;
        clipBoardHeader   = null;
        clipBoardIncoming = true;
    }
    
    static public void setClipBoardText(boolean incoming, String date, String from, String text)
    {
        clipBoardText     = text;
        if (date != "***error***")
        {
            clipBoardHeader = from + ' ' + date;
        }
        else
        {
            clipBoardHeader = from;
        }
        clipBoardIncoming = incoming;
    }

/*
	static public void clearClipBoardText()
	{
		clipBoardText = null;
	}
*/	

	////////////////////////
	//                    //
	//    Color scheme    //
	//                    //
	////////////////////////
	
	static public void setColorScheme(VirtualList vl, boolean setFullScreen)
	{
		if (vl == null) return;

		vl.setColors
		(
			Options.getInt(Options.OPTION_COLOR_TEXT),
			Options.getInt(Options.OPTION_COLOR_CAP),
			Options.getInt(Options.OPTION_COLOR_BACK),
			Options.getInt(Options.OPTION_COLOR_TEXT)
		);

		if (setFullScreen) vl.setFullScreen(true);
		else vl.setFullScreen(false);
	}
	
	static public void setColorScheme(boolean changeColors)
	{
		if (changeColors)
		{
			// #sijapp cond.if modules_HISTORY is "true" #
			HistoryStorage.setColorScheme();
			// #sijapp cond.end#
			ChatHistory.setColorScheme();
		}
		setColorScheme((VirtualList)ContactList.getVisibleContactListRef(), Options.getBoolean(Options.OPTION_FULL_SCREEN));
	}
    
    /*****************************************************************************/
    /*****************************************************************************/
    /*****************************************************************************/


    // Waits until contact listupdate is completed
    public static class GetVersionInfoTimerTask extends TimerTask
    {
        // Try to get current Jimm version from Jimm server
        HttpConnection httemp;
        InputStream istemp;
        
        // Timer routine
        public void run()
        {
            try
            {
                httemp = (HttpConnection) Connector.open("http://jimm.im/last");
                if (httemp.getResponseCode() != HttpConnection.HTTP_OK) throw new IOException();
                istemp = httemp.openInputStream();
                byte[] version_ = new byte[(int)httemp.getLength()];
                istemp.read(version_,0,version_.length);
                version = new String(version_);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            	version = "Error: " + e.getMessage();
            }
            
            synchronized(_this)
            {
            	if ((aboutTextList != null) && aboutTextList.isActive())
            	{
            		aboutTextList.addBigText(version, aboutTextList.getTextColor(), Font.STYLE_PLAIN, -1);
            	}
            }
        }
    }
    
    /************************************************************************/
    /************************************************************************/
    /************************************************************************/
    
    ///////////////////
    //               //
    //    Hotkeys    //
    //               //
    ///////////////////

	static public boolean execDoubleHotKey(ContactItem cItem, int keyCode, int type)
	{
		//#sijapp cond.if target is "MOTOROLA"#
		if (VirtualList.zeroWasPressed && (type == VirtualList.KEY_RELEASED))
		//#sijapp cond.else#
		if (VirtualList.zeroWasPressed && (type == VirtualList.KEY_PRESSED))
		//#sijapp cond.end#
		{
			VirtualList.zeroWasPressed = false;

			for (int i = 0; i < Options.EXT_KEY_COUNT; ++i)
			{
				if (keyCode == Options.EXT_KEY_CODES[i])
				{
					int action = (int)Options.getString(Options.OPTION_EXT_KEY_ACTIONS).charAt(i);
					execHotKeyAction(action, cItem, type);
					return true;
				}
			}
		}
		return false;
	}

	static public void execHotKey(ContactItem cItem, int keyCode, int type)
	{
		switch (keyCode)
		{
		case Canvas.KEY_NUM0:
			if (Options.getBoolean(Options.OPTION_ENABLE_EXT_KEYS))
			{
				//#sijapp cond.if target is "MOTOROLA"#
				if (type == VirtualList.KEY_RELEASED)
				//#sijapp cond.else#
				if (type == VirtualList.KEY_PRESSED)
				//#sijapp cond.end#
				{
					VirtualList.zeroWasPressed = true;
				}
			}
			else
			{
				execHotKeyAction(Options.getInt(Options.OPTION_EXT_CLKEY0), cItem, type);
			}
			break;
		/* For E2 */
		//case -30:
		case Canvas.KEY_NUM4:
			execHotKeyAction(Options.getInt(Options.OPTION_EXT_CLKEY4), cItem, type);
			break;
		/* For E2 */
		//case -31:
		case Canvas.KEY_NUM6:
			execHotKeyAction(Options.getInt(Options.OPTION_EXT_CLKEY6), cItem, type);
			break;
		case Canvas.KEY_STAR:
			execHotKeyAction(Options.getInt(Options.OPTION_EXT_CLKEYSTAR), cItem, type);
			break;
			
		case Canvas.KEY_POUND:
			execHotKeyAction(Options.getInt(Options.OPTION_EXT_CLKEYPOUND), cItem, type);
			break;
		// #sijapp cond.if target is "SIEMENS2"#
		case -11:
			// This means the CALL button was pressed...
			execHotKeyAction(Options.getInt(Options.OPTION_EXT_CLKEYCALL), cItem, type);
			break;
		// #sijapp cond.end#
		// #sijapp cond.if target is "MIDP2"#
		case -10: // Nokia phones
			// This means the CALL button was pressed...
			execHotKeyAction(Options.getInt(Options.OPTION_EXT_CLKEYCALL), cItem, type);
			break;
		// #sijapp cond.end#
		}
	}
	
	private static long lockPressedTime = -1;

	static private void execHotKeyAction(int actionNum, ContactItem item, int keyType)
	{
		//#sijapp cond.if target is "MOTOROLA"#
		if (keyType == VirtualList.KEY_RELEASED)
		//#sijapp cond.else#
		if (keyType == VirtualList.KEY_PRESSED)
		//#sijapp cond.end#
		{
			lockPressedTime = System.currentTimeMillis();
			
			switch (actionNum)
			{
			// #sijapp cond.if modules_HISTORY is "true" #
			case Options.HOTKEY_HISTORY:
				if (item != null) item.showHistory();
				break;
			// #sijapp cond.end#

			case Options.HOTKEY_INFO:
				if (item != null)
					requiestUserInfo(item.getUinString(), item.name);
				break;

			case Options.HOTKEY_NEWMSG:
				if (item != null) writeMessage(item, null);
				break;

			case Options.HOTKEY_ONOFF:
				if (Options.getBoolean(Options.OPTION_CL_HIDE_OFFLINE)) 
					Options.setBoolean(Options.OPTION_CL_HIDE_OFFLINE, false);
				else 
					Options.setBoolean(Options.OPTION_CL_HIDE_OFFLINE, true);
				Options.safe_save();
				ContactList.optionsChanged(true, false);
				ContactList.activate();
				break;

			case Options.HOTKEY_OPTIONS:
				Options.editOptions();
				break;

			case Options.HOTKEY_MENU:
				MainMenu.activate();
				break;

			// #sijapp cond.if target is "MIDP2"#
			case Options.HOTKEY_MINIMIZE:
				Jimm.setMinimized(true);
				break;
			// #sijapp cond.end#

			// #sijapp cond.if target is "SIEMENS2"#
			case Options.HOTKEY_MINIMIZE:
					try
					{
						Jimm.jimm.platformRequest(Jimm.strMenuCall);
					}
					catch(Exception exc1) {}
				break;
			// #sijapp cond.end#

			case Options.HOTKEY_CLI_INFO:
				if (item != null) showClientInfo(item);
				break;
			//#sijapp cond.if target is "MIDP2" | target is "MOTOROLA" | target is "SIEMENS2"#

			case Options.HOTKEY_FULLSCR:
				boolean fsValue = !Options.getBoolean(Options.OPTION_FULL_SCREEN);
				VirtualList.setFullScreenForCurrent(fsValue);
				Options.setBoolean(Options.OPTION_FULL_SCREEN, fsValue);
				Options.safe_save();
				ContactList.activate();
				break;
			//#sijapp cond.end#

			//#sijapp cond.if target isnot "DEFAULT"#
			case Options.HOTKEY_SOUNDOFF:
				ContactList.changeSoundMode(true);
				break;
			//#sijapp cond.end#

			//#sijapp cond.if target is "SIEMENS2"#
			case Options.HOTKEY_ADJLIGHT:
				try
				{
					Jimm.jimm.platformRequest(Jimm.strLightCall);
				}
				catch(Exception exc) {}
				break;
			//#sijapp cond.end#

			case Options.HOTKEY_MAGIC_EYE:
				if (Options.getBoolean(Options.OPTION_MAGIC_EYE)) MagicEye.activate();
				break;

			case Options.HOTKEY_DEL_CHATS:
				ChatHistory.chatHistoryDelete(null, ChatHistory.DEL_TYPE_ALL);
				ContactList.activate();
				break;

			case Options.HOTKEY_XTRAZ_MSG:
				if (item instanceof ContactItem)
				{
					if (item.getXStatus().getStatusIndex() != -1)
					{
						try
						{
							XtrazSM.a(item.getStringValue(0), 0);
						}
						catch (Exception e) {}
						item.readXtraz = false;
						item.openChat = true;
					}
				}
				break;

			//#sijapp cond.if target is "MIDP2"#
			case Options.HOTKEY_CALL_SMS:
				PhoneBook.activate();
				break;
			//#sijapp cond.end#
			}
		}

		else if ((keyType == VirtualList.KEY_REPEATED) || (keyType == VirtualList.KEY_RELEASED))
		{
			if (lockPressedTime == -1) return;
			long diff = System.currentTimeMillis() - lockPressedTime;
			if ((actionNum == Options.HOTKEY_LOCK) && (diff > 900))
			{
				lockPressedTime = -1;
				SplashCanvas.lock();
			}
		}
	}
	
	///////////////////////////////////////////////////////////////////////////
	//                                                                       //
	//               U S E R   A N D   C L I E N T   I N F O                 //
	//                                                                       //
	///////////////////////////////////////////////////////////////////////////
	
	// Information about the user
	final public static int UI_UIN               =  0;
	final public static int UI_NICK              =  1;
	final public static int UI_NAME              =  2;
	final public static int UI_EMAIL             =  3;
	final public static int UI_CITY              =  4;
	final public static int UI_STATE             =  5;
	final public static int UI_PHONE             =  6;
	final public static int UI_FAX               =  7;
	final public static int UI_ADDR              =  8;
	final public static int UI_CPHONE            =  9;
	final public static int UI_AGE               = 10;
	final public static int UI_GENDER            = 11;
	final public static int UI_HOME_PAGE         = 12;
	final public static int UI_BDAY              = 13;
	final public static int UI_W_CITY            = 14;
	final public static int UI_W_STATE           = 15;
	final public static int UI_W_PHONE           = 16;
	final public static int UI_W_FAX             = 17;
	final public static int UI_W_ADDR            = 18;
	final public static int UI_W_NAME            = 19;
	final public static int UI_W_DEP             = 20;
	final public static int UI_W_POS             = 21;
	final public static int UI_ABOUT             = 22;
	final public static int UI_INETRESTS         = 23;
	final public static int UI_AUTH              = 24;
	final public static int UI_STATUS            = 25;
	final public static int UI_ICQ_CLIENT        = 26;
	final public static int UI_SIGNON            = 27;
	final public static int UI_ONLINETIME        = 28;
	final public static int UI_IDLE_TIME         = 29;
	final public static int UI_REGDATA           = 30;
	final public static int UI_ICQ_VERS          = 31;
	final public static int UI_INT_IP            = 32;
	final public static int UI_EXT_IP            = 33;
	final public static int UI_PORT              = 34;
	final public static int UI_CLIENT_CAP        = 35;
	final public static int UI_OFFLINE_TIME      = 36;
	final public static int UI_UIN_LIST          = 37;
	final public static int UI_FIRST_NAME        = 38;
	final public static int UI_LAST_NAME         = 39;
	final public static int UI_INETRESTS_INDEX1  = 40;
	final public static int UI_INETRESTS_INDEX2  = 41;
	final public static int UI_INETRESTS_INDEX3  = 42;
	final public static int UI_INETRESTS_INDEX4  = 43;
	final public static int UI_INETRESTS_1       = 44;
	final public static int UI_INETRESTS_2       = 45;
	final public static int UI_INETRESTS_3       = 46;
	final public static int UI_INETRESTS_4       = 47;

	final public static int UI_LAST_ID           = 48;
	
	static private int uiBigTextIndex;
	static private String uiSectName = null;
	
	static private void addToTextList(String str, String langStr, TextList list)
	{
		if (uiSectName != null)
		{
			list.addBigText
			(
				ResourceBundle.getString(uiSectName),
				list.getTextColor(),
				Font.STYLE_BOLD,
				-1
			).doCRLF(-1);
			uiSectName = null;
		}
		
		list.addBigText(ResourceBundle.getString(langStr)+": ", list.getTextColor(), Font.STYLE_PLAIN, uiBigTextIndex)
		  .addBigText(str, Options.getInt(Options.OPTION_COLOR_BLUE), Font.STYLE_PLAIN, uiBigTextIndex)
		  .doCRLF(uiBigTextIndex);
		uiBigTextIndex++;
	}
	
	static private void addToTextList(int index, String[] data, String langStr, TextList list)
	{
		String str = data[index];
		if (str == null) return;
		if (str.length() == 0) return;

		addToTextList(str, langStr, list);
	}
	
	static public void fillUserInfo(String[] data, TextList list)
	{
		uiSectName = "main_info";
		addToTextList(UI_UIN_LIST,  data, "uin",        list);
		addToTextList(UI_NICK,      data, "nick",       list);
		addToTextList(UI_NAME,      data, "name",       list);
		addToTextList(UI_GENDER,    data, "gender",     list);
		addToTextList(UI_AGE,       data, "age",        list);
		addToTextList(UI_EMAIL,     data, "email",      list);
		if (data[UI_AUTH] != null)
			addToTextList
			(
				data[UI_AUTH].equals("1") ? ResourceBundle.getString("yes") : ResourceBundle.getString("no"), "auth", list
			);
		addToTextList(UI_BDAY,      data, "birth_day",  list);
		addToTextList(UI_HOME_PAGE, data, "home_page",  list);
		addToTextList(UI_ABOUT,     data, "notes",      list);
		addToTextList(UI_INETRESTS, data, "interests",  list);

		uiSectName = "interests";
		if (data[UI_INETRESTS_INDEX1] != null)
			addToTextList(UI_INETRESTS_1, data, data[UI_INETRESTS_INDEX1],  list);
		if (data[UI_INETRESTS_INDEX2] != null)
			addToTextList(UI_INETRESTS_2, data, data[UI_INETRESTS_INDEX2],  list);
		if (data[UI_INETRESTS_INDEX3] != null)
			addToTextList(UI_INETRESTS_3, data, data[UI_INETRESTS_INDEX3],  list);
		if (data[UI_INETRESTS_INDEX4] != null)
			addToTextList(UI_INETRESTS_4, data, data[UI_INETRESTS_INDEX4],  list);

		if (data[UI_STATUS] != null)
		{
			int stat = Integer.parseInt(data[UI_STATUS]);
			int imgIndex = 0;
			if (stat == 0) imgIndex = 6;
			else if (stat == 1) imgIndex = 7;
			else if (stat == 2) imgIndex = 3;
			list.addBigText(ResourceBundle.getString("status") + ": ", list.getTextColor(), Font.STYLE_PLAIN, uiBigTextIndex)
				.addImage(ContactList.getImageList().elementAt(imgIndex), null, uiBigTextIndex).doCRLF(uiBigTextIndex);
			uiBigTextIndex++;
		}

		uiSectName = "home_info";
		addToTextList(UI_CITY,      data, "city",       list);
		addToTextList(UI_STATE,     data, "state",      list);
		addToTextList(UI_ADDR,      data, "addr",       list);
		addToTextList(UI_PHONE,     data, "phone",      list);
		addToTextList(UI_CPHONE,    data, "cell_phone", list);
		addToTextList(UI_FAX,       data, "fax",        list);

		uiSectName = "work_info";
		addToTextList(UI_W_NAME,    data, "title",    list);
		addToTextList(UI_W_DEP,     data, "depart",   list);
		addToTextList(UI_W_POS,     data, "position", list);
		addToTextList(UI_W_CITY,    data, "city",     list);
		addToTextList(UI_W_STATE,   data, "state",    list);
		addToTextList(UI_W_ADDR,    data, "addr",     list);
		addToTextList(UI_W_PHONE,   data, "phone",    list);
		addToTextList(UI_W_FAX,     data, "fax",      list);

		uiSectName = "dc_info";
		addToTextList(UI_ICQ_CLIENT,   data, "icq_client",      list);
		addToTextList(UI_SIGNON,       data, "li_signon_time",  list);
		addToTextList(UI_ONLINETIME,   data, "li_online_time",  list);
		addToTextList(UI_OFFLINE_TIME, data, "li_offline_time", list);
		addToTextList(UI_IDLE_TIME,    data, "li_idle_time",    list);
		addToTextList(UI_REGDATA,      data, "li_regdata_time", list);

		uiSectName = "DC Information";
		addToTextList(UI_ICQ_VERS,   data, "ICQ version", list);
		addToTextList(UI_INT_IP,     data, "Int IP",      list);
		addToTextList(UI_EXT_IP,     data, "Ext IP",      list);
		addToTextList(UI_PORT,       data, "Port",        list);
		addToTextList(UI_CLIENT_CAP, data, "cap_info",    list);
	}

	///////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////
	
	static private TextList infoTextList = null;
	
	static public void requiestUserInfo(String uin, String name)
	{
		infoTextList = getInfoTextList(uin, false);
		infoTextList.setCommandListener(_this);

		if (Icq.isConnected())
		{
			if (uin == Options.getString(Options.OPTION_UIN))
			{
				infoTextList.addCommandEx(cmdMenu, VirtualList.MENU_LEFT_BAR);
				infoTextList.addCommandEx(cmdEdit, VirtualList.MENU_LEFT);
				infoTextList.addCommandEx(cmdNewPass, VirtualList.MENU_LEFT);
			}
			
			RequestInfoAction act = new RequestInfoAction(uin, name);
			
			infoTextList.addCommandEx(cmdCancel, VirtualList.MENU_RIGHT_BAR);
			
			try
			{
				Icq.requestAction(act);
			}
			catch (JimmException e)
			{
				JimmException.handleException(e);
				if (e.isCritical()) return;
			}
		
			infoTextList.add(ResourceBundle.getString("wait"));

			showInfoTextList(infoTextList);
		}
		else
		{
			String[] data = new String[JimmUI.UI_LAST_ID];
			data[JimmUI.UI_NICK] = name;
			data[JimmUI.UI_UIN_LIST] = uin;
			showUserInfo(data);
			showInfoTextList(infoTextList);
		}
	}
	
	static private String[] last_user_info;
	
	static public void showUserInfo(String[] data)
	{
		last_user_info = data;
		if (infoTextList == null) return;
		infoTextList.clear();
		JimmUI.fillUserInfo(data, infoTextList);
		infoTextList.removeCommandEx(cmdCancel);
		infoTextList.addCommandEx(cmdBack, VirtualList.MENU_RIGHT_BAR);
		infoTextList.addCommandEx(cmdMenu, VirtualList.MENU_LEFT_BAR);
		infoTextList.addCommandEx(cmdCopyText, VirtualList.MENU_LEFT);
		infoTextList.addCommandEx(cmdCopyAll, VirtualList.MENU_LEFT);
	}
	
	static public TextList getInfoTextList(String caption, boolean addCommands)
	{
		infoTextList = new TextList(null);
		
		// #sijapp cond.if target is "MOTOROLA"#
		infoTextList.setFontSize(Font.SIZE_MEDIUM);
		// #sijapp cond.else#
		infoTextList.setFontSize(Font.SIZE_SMALL);
		// #sijapp cond.end#

		infoTextList.setCaption(caption);
		
		JimmUI.setColorScheme(infoTextList, false);
		infoTextList.setMode(TextList.MODE_TEXT);
		
		if (addCommands)
		{
			infoTextList.addCommandEx(cmdMenu, VirtualList.MENU_LEFT_BAR);
			infoTextList.addCommandEx(cmdBack, VirtualList.MENU_RIGHT_BAR);
			infoTextList.addCommandEx(cmdCopyText, VirtualList.MENU_LEFT);
			infoTextList.addCommandEx(cmdCopyAll, VirtualList.MENU_LEFT);

			infoTextList.setCommandListener(_this);
		}
		
		return infoTextList;
	}
	
	static public void showInfoTextList(TextList list)
	{
		list.activate(Jimm.display);
	}
	
	///////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////
	
	static public String[] stdSelector = Util.explode("currect_contact" + "|" + "all_contact_except_this" + "|" + "all_contacts", '|');
	
	static private TextList lstSelector;
	
	static private int lastSelectedItemIndex;
	
	static public void showSelector(String caption, String[] elements, CommandListener listener, int tag, boolean translateWords)
	{
		if (translateWords)
			for (int i = 0; i < elements.length; i++)
				elements[i] = ResourceBundle.getString(elements[i]);
		curScreenTag = tag;
		lstSelector = new TextList(ResourceBundle.getString(caption));
		JimmUI.setColorScheme(lstSelector, false);
		lstSelector.setMode(VirtualList.MODE_TEXT);
		lstSelector.setCyclingCursor(true);

		lstSelector.setFontSize(Options.getInt(Options.OPTION_CL_FONT_SIZE) * 8);

		for (int i = 0; i < elements.length; i++)
			JimmUI.addTextListItem(lstSelector, elements[i], ContactList.menuIcons.elementAt(10), i, translateWords);

		lstSelector.addCommandEx(cmdOk, VirtualList.MENU_LEFT_BAR);
		lstSelector.addCommandEx(cmdCancel, VirtualList.MENU_RIGHT_BAR);
		lstSelector.setCommandListener(_this);
		JimmUI.listener = listener;
		lstSelector.activate(Jimm.display);
	}
	
	static public int getLastSelIndex()
	{
		return lastSelectedItemIndex;
	}
	
	///////////////////////////////////////////////////////////////////////////
	
	static public void addMessageText(TextList textList, String text, int color, int messTotalCounter)
	{
		//#sijapp cond.if modules_SMILES is "true" #
		Emotions.addTextWithEmotions(textList, text, Font.STYLE_PLAIN, color, messTotalCounter);

		//#sijapp cond.else#
		textList.addBigText(text, textList.getTextColor(), Font.STYLE_PLAIN, messTotalCounter);
		//#sijapp cond.end#
		textList.doCRLF(messTotalCounter);
	}
	
	///////////////////////////////////////////////////////////////////////////
	
	private final static long[] statuses = 
	{
		ContactList.STATUS_AWAY,
		ContactList.STATUS_CHAT,
		ContactList.STATUS_DND,
		ContactList.STATUS_INVISIBLE,
		ContactList.STATUS_NA,
		ContactList.STATUS_OCCUPIED,
		ContactList.STATUS_OFFLINE,
		ContactList.STATUS_ONLINE,
		ContactList.STATUS_EVIL,
		ContactList.STATUS_DEPRESSION,
		ContactList.STATUS_HOME,
		ContactList.STATUS_WORK,
		ContactList.STATUS_LUNCH,
		ContactList.STATUS_INVIS_ALL,
	};
	
	public final static int[] imageIndexes = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13 };
	
	private final static String[] statusStrings = 
		Util.explode
		(
			"status_away"       +"|"+
			"status_chat"       +"|"+
			"status_dnd"        +"|"+
			"status_invisible"  +"|"+
			"status_na"         +"|"+
			"status_occupied"   +"|"+
			"status_offline"    +"|"+
			"status_online"     +"|"+
			"status_evil"       +"|"+
			"status_depression" +"|"+
			"status_home"       +"|"+
			"status_work"       +"|"+
			"status_lunch"      +"|"+
			"status_invis_all"
			, '|'
		);
	
	public static int getStatusIndex(long status)
	{
		for (int i = 0; i < statuses.length; i++) if (statuses[i] == status) return i;
		return -1;
	}
	
	public static int getStatusImageIndex(long status)
	{
		int index = getStatusIndex(status);
		return (index == -1) ? -1 : imageIndexes[index];
	}

	public static String getStatusString(long status)
	{
		int index = getStatusIndex(status);
		return (index == -1) ? null : ResourceBundle.getString(statusStrings[index]);
	}

	
	public static final int SHS_TYPE_ALL   = 1;
	public static final int SHS_TYPE_EMPTY = 2;
	
	public static int[] showGroupSelector(String caption, int tag, CommandListener listener, int type, int excludeGroupId)
	{
		GroupItem[] groups = ContactList.getGroupItems();
		String[] groupNamesTmp = new String[groups.length];
		int[] groupIdsTmp = new int[groups.length];

		int index = 0;
		for (int i = 0; i < groups.length; i++)
		{
			int groupId = groups[i].getId();
			if (groupId == excludeGroupId) continue;
			switch (type)
			{
			case SHS_TYPE_EMPTY:
				ContactItem[] cItems = ContactList.getGroupItems(groupId);
				if (cItems.length != 0) continue;
				break;
			}
			
			groupIdsTmp[index] = groupId;
			groupNamesTmp[index] = groups[i].getName();
			index++;
		}
		
		if (index == 0)
		{
			Alert alert = new Alert("", ResourceBundle.getString("no_availible_groups"), null, AlertType.INFO );
			alert.setTimeout(Alert.FOREVER);
			Jimm.display.setCurrent(alert);
			return null;
		}
		
		String[] groupNames = new String[index];
		int[] groupIds = new int[index];

		System.arraycopy(groupIdsTmp, 0, groupIds, 0, index);
		System.arraycopy(groupNamesTmp, 0, groupNames, 0, index);
			
		showSelector(ResourceBundle.getString(caption), groupNames, listener, tag, false);
		
		return groupIds;
	}

	///////
	
	public static void addTextListItem(TextList list, String text, Icon image, int value, boolean translate)
	{
		list.setFontSize(Options.getInt(Options.OPTION_CL_FONT_SIZE) * 8);
		if (image != null) list.addImage(image, null, value);
		String textToAdd = translate ? ResourceBundle.getString(text) : text;
		list.addBigText(textToAdd, list.getTextColor(), Options.getInt(Options.OPTION_CL_FONT_STYLE), value);
		list.doCRLF(value);
	}
	
	//////
	
	static public boolean isControlActive(VirtualList list)
	{
		if (list == null) return false;
		return list.isActive();
	}

	//////////////////////////////
	//                          //
	// Text editor for messages //
	//                          //
	//////////////////////////////

	/* Size of text area for entering mesages */
	private static final int MAX_EDITOR_TEXT_SIZE = 2048;

	/* Textbox for entering messages */
	private static TextBox messageTextbox;

	private Vector strings = new Vector();
	private int current = 0;
	private static int textLimit;
	private String caption;

	/* receiver for text message */
	public static ContactItem textMessReceiver;

	/* Modes constant for text editor */
	final private static int EDITOR_MODE_MESSAGE = 200001;
	
	/* Current text editor mode */
	private static int textMessCurMode; 

	private void switchText(int cur)
	{
		saveCurPage();
		current = Math.max(Math.min(cur, strings.size()), 0);
		setCurrentScreen();
	}

	private void saveCurPage()
	{
		String text = messageTextbox.getString();
		text = (text == null) ? "" : text;
		if (current >= strings.size())
		{
			if (text.length() > 0)
			{
				strings.addElement(text);
			}
		}
		else
		{
			strings.setElementAt(text, current);
		}
	}

	private void restore()
	{
		//#sijapp cond.if target is "MIDP2" | target is "MOTOROLA" | target is "SIEMENS2"#
		try
		{
			int mode = TextField.ANY;
			if (Options.getBoolean(Options.OPTION_TEXT_ABC))
			{
				mode |= TextField.INITIAL_CAPS_SENTENCE;
			}
			messageTextbox.setConstraints(mode);
		}
		catch (Exception e) {}
		//#sijapp cond.end#
	}

	private void setCurrentScreen()
	{
		try
		{
			messageTextbox.setString((String)strings.elementAt(current));
		}
		catch (Exception e)
		{
			messageTextbox.setString(null);
		}
		setCaption(caption);
	}

	private void setCaption(String title)
	{
		caption = (title == null) ? "" : title;

		if (strings.size() > 1)
		{
			title = "[" + (current + 1) + "/" + (strings.size() + 1) + "] " + caption;
		}
		else
		{
			title = caption;
		}
		messageTextbox.setTitle(title);
	}

	private String getString()
	{
		saveCurPage();
		StringBuffer buf = new StringBuffer();

		for (int i = 0; i < strings.size(); i++)
		{
			String str = (String)strings.elementAt(i);
			if (str != null)
			{
				buf.append(str);
			}
		}
		String messText = buf.toString();
		return messText;
	}

	private void insert(String str, int pos)
	{
		try
		{
			messageTextbox.insert(str, pos);
			return;
		}
		catch (Exception e) {}

		switchText(current);

		if (current < strings.size())
		{
			strings.removeElementAt(current);
			String curStr = messageTextbox.getString();

			if (curStr == null || curStr.length() == 0)
			{
				curStr = "";
				pos = 0;
			}
			str = curStr.substring(0, pos) + str + curStr.substring(pos);
		}

		int size = str.length();
		for (int i = 0; size > 0; i += textLimit, size -= textLimit, current++)
		{
			strings.insertElementAt(str.substring(i, i + Math.min(size, textLimit)), current);
		}

		current--;
		setCurrentScreen();
	}

	private void setString(String initText)
	{
		current = 0;
		strings.removeAllElements();

		if (initText == null)
		{
			messageTextbox.setString(null);
			return;
		}

		setCurrentScreen();
	}

	/* Write a message */
	public static void writeMessage(ContactItem receiver, String initText)
	{
		if (messageTextbox == null)
		{
			messageTextbox = new TextBox(null, null, MAX_EDITOR_TEXT_SIZE, TextField.ANY);
			textLimit = messageTextbox.getMaxSize();

			messageTextbox.addCommand(cmdSend);
			messageTextbox.addCommand(cmdCancel);
			messageTextbox.addCommand(cmdClearText);
			messageTextbox.addCommand(transCmd);
			messageTextbox.addCommand(detransCmd);
			messageTextbox.addCommand(nextCmd);
			messageTextbox.addCommand(prevCmd);
			//#sijapp cond.if modules_SMILES is "true" #
			messageTextbox.addCommand(cmdInsertEmo);
			//#sijapp cond.end#
			messageTextbox.addCommand(cmdInsTemplate);
		}

		textMessReceiver = receiver;
		textMessCurMode = EDITOR_MODE_MESSAGE;
		_this.restore();
		_this.setCaption(textMessReceiver.name);

		if (!JimmUI.clipBoardIsEmpty())
		{
			messageTextbox.addCommand(cmdQuote);
			messageTextbox.addCommand(cmdPaste);
		}

		if (initText != null)
		{
			//#sijapp cond.if target is "MOTOROLA"#
			int caretPos = messageTextbox.getString().length();
			//#sijapp cond.else#
			int caretPos = messageTextbox.getCaretPosition();
			//#sijapp cond.end#
			_this.insert(initText, caretPos);
		}

		messageTextbox.setCommandListener(_this);
		Jimm.display.setCurrent(messageTextbox);

		//#sijapp cond.if target isnot "DEFAULT"#
		if (showTyping())
		{
			try
			{
				Jimm.jimm.getIcqRef().beginTyping(textMessReceiver.getUinString(), true);
			}
			catch (JimmException e){}
		}
		//#sijapp cond.end#

		// #sijapp cond.if target is "MOTOROLA"#
		LightControl.flash(true);
		// #sijapp cond.end#
	}

	/* Construct plain message object, request new SendMessageAction and Add the new message to the chat history */
	public static void sendMessage(String text, ContactItem textMessReceiver)
	{
		if ((text == null) || (text.length() == 0))
		{
			return;
		}

		PlainMessage plainMsg;
		for (int pos = 0; pos < text.length(); pos += 2048) // 2048 - лимит на одно сообщение. на всяк случай разбивка сделана...
		{
			String messageText = text.substring(pos, Math.min(pos + 2048, text.length()));
			plainMsg = new PlainMessage(Options.getString(Options.OPTION_UIN), textMessReceiver, Message.MESSAGE_TYPE_NORM, Util.createCurrentDate(false), messageText);

			SendMessageAction sendMsgAct = new SendMessageAction(plainMsg);
			long msgId = sendMsgAct.getMsgId();

			try
			{
				Icq.requestAction(sendMsgAct);
			}
			catch (JimmException e)
			{
				JimmException.handleException(e);
				if (e.isCritical()) return;
			}
			ChatHistory.addMyMessage(textMessReceiver, messageText, plainMsg.getNewDate(), textMessReceiver.name, msgId);

			//#sijapp cond.if modules_HISTORY is "true" #
			if (Options.getBoolean(Options.OPTION_HISTORY))
				HistoryStorage.addText(textMessReceiver.getUinString(), text, (byte) 1, Icq.myNick, plainMsg.getNewDate());
			//#sijapp cond.end#

			if ((pos + 2048) < text.length())
			{
				try
				{
					Thread.sleep(100);
				}
				catch (Exception e) {}
			}
		}

		//#sijapp cond.if target is "MOTOROLA"#
		LightControl.flash(false);
		//#sijapp cond.end#
	}

	/////////////////////////////////////////////////////

	//#sijapp cond.if target is "MIDP2" | target is "MOTOROLA" | target is "SIEMENS2"#
	private static TextList URLList;
	private static Object lastScreenBeforeUrlSelect;
	
	public static void gotoURL(String msg, Object lastScreen)
	{
		lastScreenBeforeUrlSelect = lastScreen;
		Vector v = Util.parseMessageForURL(msg);
		if (v.size() == 1)
		{
			try
			{
				Jimm.jimm.platformRequest((String) v.elementAt(0));
			}
			catch (Exception e) {}
		}
		else
		{
			URLList = JimmUI.getInfoTextList(ResourceBundle.getString("goto_url"), false);
			URLList.addCommandEx(cmdSelect, VirtualList.MENU_LEFT_BAR);
			URLList.addCommandEx(cmdBack, VirtualList.MENU_RIGHT_BAR);
			URLList.setCommandListener(_this);
			for (int i = 0; i < v.size(); i++)
			{
				URLList.addBigText((String) v.elementAt(i), URLList.getTextColor(), Font.STYLE_PLAIN, i).doCRLF(i);
			}
			JimmUI.showInfoTextList(URLList);
		}
	}
	//#sijapp cond.end#
	
	///////////////////////////////////////////////////////////

	private static int authType;
	private static TextBox authTextbox;
	private static ContactItem authContactItem;

	public static final int AUTH_TYPE_DENY = 10001;
	public static final int AUTH_TYPE_REQ_AUTH = 10002;
	
	public static void authMessage(int authType, ContactItem contactItem, String caption, String text)
	{
		JimmUI.authType = authType;
		authContactItem = contactItem;
		authTextbox = new TextBox(ResourceBundle.getString(caption), ResourceBundle.getString(text), 500, TextField.ANY);

		authTextbox.addCommand(cmdSend);
		authTextbox.addCommand(cmdCancel);
		authTextbox.setCommandListener(_this);
		Jimm.display.setCurrent(authTextbox);
	}
	
	/////////////////////////////////////////////////////////////

	/////////////////////////////////////////////////////////////

	// Static constants for menu actios
	private static final int USER_MENU_MESSAGE          = 1;
	private static final int USER_MENU_STATUS_MESSAGE   = 3;
	private static final int USER_MENU_REQU_AUTH        = 4;
	// #sijapp cond.if target is "MIDP2" | target is "MOTOROLA" | target is "SIEMENS2"#
	// #sijapp cond.if modules_FILES is "true"#
	private static final int USER_MENU_FILE_TRANS       = 5;
	// #sijapp cond.if target isnot "MOTOROLA" #
	private static final int USER_MENU_CAM_TRANS        = 6;
	// #sijapp cond.end#
	// #sijapp cond.end#
	// #sijapp cond.end#        
	private static final int USER_MENU_USER_REMOVE      = 7;
	private static final int USER_MENU_REMOVE_ME        = 8;
	private static final int USER_MENU_RENAME           = 9;
	// #sijapp cond.if modules_HISTORY is "true"#         
	private static final int USER_MENU_HISTORY          = 10;
	// #sijapp cond.end#
	private static final int USER_MENU_LOCAL_INFO       = 11;
	private static final int USER_MENU_USER_INFO        = 12;
	private static final int USER_MENU_COPY_UIN         = 13;
	//private static final int USER_MENU_QUOTA            = 14;
	private static final int USER_MENU_MOVE             = 15;
	private static final int USER_MENU_LIST_OPERATION   = 16;
	private static final int USER_MENU_XTRAZ_MESSAGE    = 17;
	private static final int USER_MENU_CHECK_STATUS     = 18;

	private static TextList tlContactMenu;
	public  static ContactItem clciContactMenu;
	public  static TextList removeContactMessageBox;
	private static TextList removeMeMessageBox;
	private static TextBox renameTextbox;
	private static TextList serverLists;

	public static void showContactMenu(ContactItem contact)
	{
		clciContactMenu = contact;
		long status = contact.getIntValue(ContactItem.CONTACTITEM_STATUS);

		tlContactMenu = new TextList(clciContactMenu.name);
		setColorScheme(tlContactMenu, false);
		tlContactMenu.setMode(VirtualList.MODE_TEXT);
		tlContactMenu.setCyclingCursor(true);
		
		tlContactMenu.lock();
		addTextListItem(tlContactMenu, "send_message", ContactList.imageList.elementAt(14), USER_MENU_MESSAGE, true);

		if (contact.getBooleanValue(ContactItem.CONTACTITEM_NO_AUTH))
			addTextListItem(tlContactMenu, "requauth", ContactList.imageList.elementAt(16), USER_MENU_REQU_AUTH, true);

		if (((contact.getBooleanValue(ContactItem.CONTACTITEM_NO_AUTH)) || (contact.getBooleanValue(ContactItem.CONTACTITEM_IS_TEMP))
			||(contact.getBooleanValue(ContactItem.CONTACTITEM_PHANTOM))) && (Icq.isConnected()))
		{
			addTextListItem(tlContactMenu, "check_status", XStatus.imageList.elementAt(XStatus.XSTATUS_NONE), USER_MENU_CHECK_STATUS, true);
		}

		//#sijapp cond.if target is "MIDP2" | target is "MOTOROLA" | target is "SIEMENS2"#
		//#sijapp cond.if modules_FILES is "true"#
		if ((status != ContactList.STATUS_OFFLINE) && ((Options.getInt(Options.OPTION_FT_MODE) == Options.FS_MODE_WEB)
			|| ((contact.getIntValue(ContactItem.CONTACTITEM_ICQ_PROT) >= 8) && (contact.getIntValue(ContactItem.CONTACTITEM_CLIENT) != Util.CLI_JIMM))))
		{
			addTextListItem(tlContactMenu, "ft_name", ContactList.menuIcons.elementAt(8), USER_MENU_FILE_TRANS, true);
			//#sijapp cond.if target isnot "MOTOROLA"#
			if (Jimm.supportsCamCapture)
			{
				addTextListItem(tlContactMenu, "ft_cam", ContactList.menuIcons.elementAt(23), USER_MENU_CAM_TRANS, true);
			}
			//#sijapp cond.end#
		}
		//#sijapp cond.end#
		//#sijapp cond.end#

		if ((status != ContactList.STATUS_ONLINE) && (status != ContactList.STATUS_OFFLINE) && (status != ContactList.STATUS_INVISIBLE))
			addTextListItem(tlContactMenu, "reqstatmsg", ContactList.imageList.elementAt(contact.getImageIndex()), USER_MENU_STATUS_MESSAGE, true);		

		if ((contact.getXStatus().getStatusIndex() != -1))
			addTextListItem(tlContactMenu, "xtraz_msg", contact.getXStatus().getStatusImage(), USER_MENU_XTRAZ_MESSAGE, true);

		addTextListItem(tlContactMenu, "info", ContactList.menuIcons.elementAt(9), USER_MENU_USER_INFO, true);
		
		addTextListItem(tlContactMenu, "copy_uin", ContactList.menuIcons.elementAt(19), USER_MENU_COPY_UIN, true);

		addTextListItem(tlContactMenu, "dc_info", ContactList.menuIcons.elementAt(10), USER_MENU_LOCAL_INFO, true);

		//#sijapp cond.if modules_HISTORY is "true" #
		addTextListItem(tlContactMenu, "history", ContactList.menuIcons.elementAt(7), USER_MENU_HISTORY, true);
		//#sijapp cond.end#

		if ((ContactList.getGroupItems().length > 1) && Options.getBoolean(Options.OPTION_USER_GROUPS)
			&& !clciContactMenu.getBooleanValue(ContactItem.CONTACTITEM_NO_AUTH)
			&& !clciContactMenu.getBooleanValue(ContactItem.CONTACTITEM_IS_TEMP)
			&& !clciContactMenu.getBooleanValue(ContactItem.CONTACTITEM_PHANTOM))
			addTextListItem(tlContactMenu, "move_to_group", ContactList.menuIcons.elementAt(20), USER_MENU_MOVE, true);

		addTextListItem(tlContactMenu, "server_lists", ContactList.menuIcons.elementAt(31), USER_MENU_LIST_OPERATION, true);

		addTextListItem(tlContactMenu, "remove_me", ContactList.menuIcons.elementAt(30), USER_MENU_REMOVE_ME, true);
		addTextListItem(tlContactMenu, "rename", ContactList.menuIcons.elementAt(11), USER_MENU_RENAME, true);
		addTextListItem(tlContactMenu, "remove", ContactList.menuIcons.elementAt(6), USER_MENU_USER_REMOVE, true);
		tlContactMenu.unlock();

		tlContactMenu.addCommandEx(cmdSelect, VirtualList.MENU_LEFT_BAR);
		tlContactMenu.addCommandEx(cmdBack, VirtualList.MENU_RIGHT_BAR);
		tlContactMenu.setCommandListener(_this);
		tlContactMenu.activate(Jimm.display);
	}

	private static void showListsOperation()
	{
		serverLists = new TextList(ResourceBundle.getString("server_lists"));
		setColorScheme(serverLists, false);

		String visibleList = (clciContactMenu.getVisibleId() == 0) ? "add_visible_list" : "rem_visible_list";
		String invisibleList = (clciContactMenu.getInvisibleId() == 0) ? "add_invisible_list" : "rem_invisible_list";
		String ignoreList = (clciContactMenu.getIgnoreId() == 0) ? "add_ignore_list" : "rem_ignore_list";
		Icon visibleImage = (clciContactMenu.getVisibleId() == 0) ? ContactList.menuIcons.elementAt(5) : ContactList.menuIcons.elementAt(6);
		Icon invisibleImage = (clciContactMenu.getInvisibleId() == 0) ? ContactList.menuIcons.elementAt(5) : ContactList.menuIcons.elementAt(6);
		Icon ignoreImage = (clciContactMenu.getIgnoreId() == 0) ? ContactList.menuIcons.elementAt(5) : ContactList.menuIcons.elementAt(6);

		serverLists.lock();
		addTextListItem(serverLists, visibleList, visibleImage, ServerListsAction.VISIBLE_LIST, true);
		addTextListItem(serverLists, invisibleList, invisibleImage, ServerListsAction.INVISIBLE_LIST, true);
		addTextListItem(serverLists, ignoreList, ignoreImage, ServerListsAction.IGNORE_LIST, true);
		serverLists.unlock();

		serverLists.setMode(TextList.MODE_TEXT);
		serverLists.setCyclingCursor(true);
		serverLists.addCommandEx(cmdSelect, VirtualList.MENU_LEFT_BAR);
		serverLists.addCommandEx(cmdBack, VirtualList.MENU_RIGHT_BAR);
		serverLists.setCommandListener(_this);
		serverLists.activate(Jimm.display);
	}

	private static void contactMenuSelected(int index)
	{
		switch (index)
		{
		case USER_MENU_MESSAGE:
			writeMessage(clciContactMenu, null);
			break;

		case USER_MENU_REQU_AUTH:
			JimmUI.authMessage(JimmUI.AUTH_TYPE_REQ_AUTH, clciContactMenu, "requauth", "plsauthme");
			break;
/*
		case USER_MENU_QUOTA:
			writeMessage(clciContactMenu, JimmUI.getClipBoardText(true));
			break;
*/
		case USER_MENU_STATUS_MESSAGE:
			long status = clciContactMenu.getIntValue(ContactItem.CONTACTITEM_STATUS);
			if (!((status == ContactList.STATUS_ONLINE) || (status == ContactList.STATUS_OFFLINE) ||
					(status == ContactList.STATUS_INVISIBLE)))
			{
				int msgType;
				/* Send a status message request message */
				if (status == ContactList.STATUS_AWAY) msgType = Message.MESSAGE_TYPE_AWAY;
				else if (status == ContactList.STATUS_OCCUPIED) msgType = Message.MESSAGE_TYPE_OCC;
				else if (status == ContactList.STATUS_DND) msgType = Message.MESSAGE_TYPE_DND;
				else if (status == ContactList.STATUS_CHAT) msgType = Message.MESSAGE_TYPE_FFC;
				else if (status == ContactList.STATUS_NA) msgType = Message.MESSAGE_TYPE_NA;
				else if (status == ContactList.STATUS_EVIL) msgType = Message.MESSAGE_TYPE_EVIL;
				else if (status == ContactList.STATUS_DEPRESSION) msgType = Message.MESSAGE_TYPE_DEPRESSION;
				else if (status == ContactList.STATUS_HOME) msgType = Message.MESSAGE_TYPE_HOME;
				else if (status == ContactList.STATUS_WORK) msgType = Message.MESSAGE_TYPE_WORK;
				else if (status == ContactList.STATUS_LUNCH) msgType = Message.MESSAGE_TYPE_LUNCH;
				else msgType = Message.MESSAGE_TYPE_AWAY;

				clciContactMenu.readStatusMess = true;

				PlainMessage awayReq = new PlainMessage(Options.getString(Options.OPTION_UIN), clciContactMenu, msgType, Util.createCurrentDate(false), "");

				SendMessageAction act = new SendMessageAction(awayReq);

				try
				{
					Icq.requestAction(act);

				}
				catch (JimmException e)
				{
					JimmException.handleException(e);
					if (e.isCritical()) return;
				}
				ContactList.activate();
			}
			break;

			case USER_MENU_XTRAZ_MESSAGE: /* Send a XTraz message request */
				try
				{
					XtrazSM.a(clciContactMenu.getUinString(), 0);
				}
				catch (Exception e) {}

				clciContactMenu.readXtraz = false;
				clciContactMenu.openChat = true;
				ContactList.activate();
				break;

			//#sijapp cond.if target is "MIDP2" | target is "MOTOROLA" | target is "SIEMENS2"#
			//#sijapp cond.if modules_FILES is "true"#                    
			case USER_MENU_FILE_TRANS:
				/* Send a filetransfer with a file given by path */
				{
					FileTransfer.askForWebFileTransfer = true;
					FileTransfer ft = new FileTransfer(FileTransfer.FT_TYPE_FILE_BY_NAME, clciContactMenu);
					ft.startFT();
				}
				break;

			//#sijapp cond.if target isnot "MOTOROLA" #
			case USER_MENU_CAM_TRANS:
				/* Send a filetransfer with a camera image */
				{
					FileTransfer.askForWebFileTransfer = true;
					FileTransfer ft = new FileTransfer(FileTransfer.FT_TYPE_CAMERA_SNAPSHOT, clciContactMenu);
					ft.startFT();
				}
				break;
			//#sijapp cond.end#
			//#sijapp cond.end#
			//#sijapp cond.end#

			case USER_MENU_MOVE:
				groupList = showGroupSelector
				(
					"group_name",
					GROUP_SELECTOR_MOVE_TAG,
					_this,
					SHS_TYPE_ALL,
					clciContactMenu.getIntValue(ContactItem.CONTACTITEM_GROUP)
				);
				break;

			case USER_MENU_USER_REMOVE:
				removeContactMessageBox = showMessageBox
				(
					clciContactMenu,
					ResourceBundle.getString("remove"), 
					ResourceBundle.getString("remove") + " " + clciContactMenu.name + "?", 
					JimmUI.MESBOX_OKCANCEL
				);
				break;

			case USER_MENU_REMOVE_ME: /* Remove me from other users contact list */
				removeMeMessageBox = showMessageBox
				(
					clciContactMenu,
					ResourceBundle.getString("remove_me"),
					ResourceBundle.getString("remove_me_from") + clciContactMenu.name + "?", 
					JimmUI.MESBOX_OKCANCEL
				); 
				break;

			case USER_MENU_RENAME:
				renameTextbox = new TextBox
				(
					ResourceBundle.getString("rename"),
					clciContactMenu.name,
					64,
					TextField.ANY
				);
				renameTextbox.addCommand(cmdOk);
				renameTextbox.addCommand(cmdCancel);
				renameTextbox.setCommandListener(_this);
				Jimm.display.setCurrent(renameTextbox);
				break;

			case USER_MENU_USER_INFO:
				requiestUserInfo(clciContactMenu.getUinString(), clciContactMenu.name);
				break;

			case USER_MENU_LOCAL_INFO:
				showClientInfo(clciContactMenu);
				break;

			//#sijapp cond.if modules_HISTORY is "true" #
			case USER_MENU_HISTORY:
				HistoryStorage.showHistoryList(clciContactMenu.getUinString(), clciContactMenu.name);
				break;
			//#sijapp cond.end#

			case USER_MENU_COPY_UIN:
				JimmUI.setClipBoardText(clciContactMenu.getUinString());
				ContactList.activate();
				break;

			case USER_MENU_LIST_OPERATION: /* Show privacy options */
				showListsOperation();
				break;

			case USER_MENU_CHECK_STATUS:
				checkStatus(clciContactMenu.getUinString(), true);
				break;
		}
	}

	public static void checkStatus(String uin, boolean act)
	{
		int length = uin.length();
		byte[] uinRaw = Util.stringToByteArray(uin);
		byte[] buf = new byte[length + 4 + 1];
		Util.putWord(buf, 0, 0x0000);
		Util.putWord(buf, 2, 0x0005);
		Util.putByte(buf, 4, length);
		System.arraycopy(uinRaw, 0, buf, 5, uinRaw.length);
		SnacPacket packet = new SnacPacket(0x0002, 0x0015, 0, new byte[0], buf);

		try
		{
			Icq.c.sendPacket(packet);
		}
		catch (JimmException e) {}

		if (act)
		{
			ContactList.activate();
		}
	}

	private static void showClientInfo(ContactItem cItem)
	{
		TextList tlist = JimmUI.getInfoTextList(cItem.getUinString(), true);
		String[] clInfoData = new String[JimmUI.UI_LAST_ID];

		/* regdata on time */
		long regdataTime = cItem.getIntValue(ContactItem.CONTACTITEM_REGDATA); 
		if (regdataTime > 0) clInfoData[JimmUI.UI_REGDATA] = Util.getDateString(false, false, regdataTime);

		/* sign on time */
		long signonTime = cItem.getIntValue(ContactItem.CONTACTITEM_SIGNON);
		if (signonTime > 0) clInfoData[JimmUI.UI_SIGNON] = Util.getDateString(false, false, signonTime);

		/* online time */
		long onlineTime = cItem.getIntValue(ContactItem.CONTACTITEM_ONLINE);
		if (onlineTime > 0) clInfoData[JimmUI.UI_ONLINETIME] = Util.longitudeToString(onlineTime);

		/* Offline since */
		if ((cItem.getStringValue(ContactItem.CONTACTITEM_OFFLINETIME) != null) &&
				(cItem.getIntValue(ContactItem.CONTACTITEM_STATUS) == ContactList.STATUS_OFFLINE))
			clInfoData[JimmUI.UI_OFFLINE_TIME] = cItem.getStringValue(ContactItem.CONTACTITEM_OFFLINETIME);

		/* idle time */
		int idleTime = cItem.getIntValue(ContactItem.CONTACTITEM_IDLE);
		if (idleTime > 0) clInfoData[JimmUI.UI_IDLE_TIME] = Util.longitudeToString(idleTime);
		
		//#sijapp cond.if (target="MIDP2" | target="MOTOROLA" | target="SIEMENS2") & modules_FILES="true"#
		/* Client version */
		int clientVers = cItem.getIntValue(ContactItem.CONTACTITEM_CLIENT);
		if (clientVers != Util.CLI_NONE) clInfoData[JimmUI.UI_ICQ_CLIENT] = Util.getClientString((byte) clientVers)
									+ " " + cItem.getStringValue(ContactItem.CONTACTITEM_CLIVERSION);
		/* ICQ protocol version */
		clInfoData[JimmUI.UI_ICQ_VERS] = Integer.toString(cItem.getIntValue(ContactItem.CONTACTITEM_ICQ_PROT));

		/* Internal IP */
		clInfoData[JimmUI.UI_INT_IP] = Util.ipToString(cItem.getIPValue(ContactItem.CONTACTITEM_INTERNAL_IP));

		/* External IP */
		clInfoData[JimmUI.UI_EXT_IP] = Util.ipToString(cItem.getIPValue(ContactItem.CONTACTITEM_EXTERNAL_IP));

		/* Port */
		int port = cItem.getIntValue(ContactItem.CONTACTITEM_DC_PORT);
		if (port != 0) clInfoData[JimmUI.UI_PORT] = Integer.toString(port);
		//#sijapp cond.end#

//		/* характеристики клиента */
//		if (((cItem.getStringValue(ContactItem.CONTACTITEM_CLIENTCAP)) != ""))
//			clInfoData[JimmUI.UI_CLIENT_CAP] = cItem.getStringValue(ContactItem.CONTACTITEM_CLIENTCAP);

		JimmUI.fillUserInfo(clInfoData, tlist);
		JimmUI.showInfoTextList(tlist);
	}

	private static void menuRemoveContactSelected()
	{
		String uin = clciContactMenu.getUinString();
		ChatHistory.chatHistoryDelete(uin);
		boolean wasDeleted = Icq.delFromContactList(clciContactMenu);
		//#sijapp cond.if modules_HISTORY is "true" #
		if (wasDeleted) HistoryStorage.clearHistory(uin);
		//#sijapp cond.end#
		//#sijapp cond.if target is "MIDP2" | target is "SIEMENS2"#
		NoticeOnBirthDay.deleteBitem(uin);
		//#sijapp cond.end#
	}

	private static void menuRemoveMeSelected()
	{
		RemoveMeAction remAct = new RemoveMeAction(clciContactMenu.getUinString());

		try
		{
			Icq.requestAction(remAct);
		} 
		catch (JimmException e)
		{
			JimmException.handleException(e);
			if (e.isCritical()) return;
		}
		ContactList.activate();
	}

	private static void menuRenameSelected()
	{
		String newName = renameTextbox.getString();
		if ((newName == null) || (newName.length() == 0)) return;
		clciContactMenu.rename(newName);
		renameTextbox.setString(null);
		ContactList.activate();
	}
	////////////////////////////////

	public static Object getCurrentScreen()
	{
		if (VirtualList.getCurrent() != null) return VirtualList.getCurrent();
		Displayable disp = Jimm.display.getCurrent();
		if ((disp == null) || (disp instanceof Canvas)) return null;
		return disp;
	}
	
	private static TimerTasks flashTimerTask;
	
	public static void showCapText(Object control, String text, int type)
	{
		if ((text == null) || (control == null) || (control instanceof Canvas)) return;
		if (!Options.getBoolean(Options.OPTION_CREEPING_LINE)) return;
		if ((type == TimerTasks.TYPE_FLASH) && (flashTimerTask != null) &&
			!flashTimerTask.isCanceled() && (flashTimerTask.getType() == TimerTasks.TYPE_CREEPING)) return;
		
		if (flashTimerTask != null)
		{
			flashTimerTask.cancel(); // отмена задания...
			flashTimerTask.flashRestoreOldCaption(); // восстановление преждего заголовка...
		}
		flashTimerTask = new TimerTasks(control, text, (type == TimerTasks.TYPE_FLASH) ? 14 : 0, type);
		int interval = (type == TimerTasks.TYPE_FLASH) ? 500 : 300;
		Jimm.getTimerRef().schedule(flashTimerTask, interval, interval);
	}

	/* Shows creeping line */ 
	static synchronized public void showCreepingLine(Object control, ContactItem item, String text)
	{
		if (Options.getBoolean(Options.OPTION_CREEPING_LINE) && (textMessReceiver == item) && messageTextbox.isShown())
		{
			showCapText(control, text, TimerTasks.TYPE_CREEPING);
		}
	}

	/* Flashs form caption */
	static synchronized public void statusChanged(Object control, ContactItem item, long status)
	{
		if (Options.getBoolean(Options.OPTION_CREEPING_LINE) && (textMessReceiver == item) && messageTextbox.isShown())
		{
			showCapText(control, JimmUI.getStatusString(status), TimerTasks.TYPE_FLASH);
		}
	}

	/* Shows popup window with text of received message */
	static public void showPopupWindow(String uin, String name, String text)
	{
		if (SplashCanvas.locked()) return;
		
		boolean haveToShow = false;
		boolean chatVisible = ChatHistory.chatHistoryShown(uin);
		boolean uinEquals = uin.equals(ContactItem.currentUin);
		boolean textBoxExists = (messageTextbox != null);

		switch (Options.getInt(Options.OPTION_POPUP_WIN))
		{
			case 0: return;
			case 1:
				if (textBoxExists)
				{
					haveToShow = !chatVisible && uinEquals && messageTextbox.isShown();
				}
				break;
			case 2:
				haveToShow = !chatVisible || (chatVisible && !uinEquals);
				break;
		}

		if (!haveToShow) return;

		//#sijapp cond.if target is "MIDP2"#
		String oldText = (Jimm.is_phone_SE() && textBoxExists && messageTextbox.isShown()) ? messageTextbox.getString() : null;
		//#sijapp cond.end#

		String textToAdd = "[" + name + "]\n" + text;
		
		if (Jimm.display.getCurrent() instanceof Alert)
		{
			Alert currAlert = (Alert)Jimm.display.getCurrent();
			if (currAlert.getImage() != null) currAlert.setImage(null);
			currAlert.setString(currAlert.getString() + "\n\n" + textToAdd);
			return;
		}

		Alert alert = new Alert(ResourceBundle.getString("message"), textToAdd, null, null);
		alert.setTimeout(Alert.FOREVER);
		
		Jimm.display.setCurrent(alert);
		
		//#sijapp cond.if target is "MIDP2"#
		if (oldText != null) messageTextbox.setString(oldText);
		//#sijapp cond.end#
	}
}
