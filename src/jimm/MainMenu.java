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
 File: src/jimm/MainMenu.java
 Version: ###VERSION###  Date: ###DATE###
 Author(s): Manuel Linsmayer, Andreas Rossbacher
 *******************************************************************************/

package jimm;

import javax.microedition.lcdui.*;
import javax.microedition.midlet.MIDletStateChangeException;

import DrawControls.*;
import jimm.comm.*;
import jimm.util.*;

public class MainMenu implements CommandListener
{
	private static final int TAG_EXIT = 1;
	private static final int TAG_RENAME_GROUPS = 2;
	private static final int TAG_DELETE_GROUPS = 3;
	private static final int TAG_CL = 4;

	public static MainMenu _this;

	/* Static constants for menu actios */
	private static final int MENU_CONNECT        =  1;
	private static final int MENU_DISCONNECT     =  2;
	private static final int MENU_LIST           =  3;
	private static final int MENU_OPTIONS        =  4;
	private static final int MENU_TRAFFIC        =  5;
	private static final int MENU_KEYLOCK        =  6;
	private static final int MENU_STATUS         =  7;
	private static final int MENU_XSTATUS        =  8;
	private static final int MENU_PRIVATE_STATUS =  9;
	private static final int MENU_GROUPS         = 10;
	private static final int MENU_ABOUT          = 11;
	private static final int MENU_MINIMIZE       = 12;
	private static final int MENU_CALL_SMS       = 13;
	private static final int MENU_SOUND          = 14;
	private static final int MENU_MYSELF         = 15;
	private static final int MENU_MAGIC_EYE      = 16;
	private static final int MENU_EXIT           = 17; /* Exit has to be biggest element cause it also marks the size */

	/* Send command */
	private static Command sendCommand = new Command(ResourceBundle.getString("send"), Command.OK, 1);

	private static Command exitCommand = new Command(ResourceBundle.getString("exit"), Command.EXIT, 1);

	/* Lists for selecting */
	private static TextList statusList, privateStatusActList, groupActList;
	
	/////////////////////////////////////////////////////////////////
	private static final int STATUS_NONE         = 0;
	private static final int STATUS_ADD_GROUP    = 1;
	private static final int STATUS_RENAME_GROUP = 2;
	private static int status = STATUS_NONE;

	private static Selector selector;

	/** ************************************************************************* */

	/* Visual list */
	static private TextList list = new TextList(ResourceBundle.getString("menu"));

	/* Groups list */
	static private int[] groupIds;

	/* Form for the adding users dialog */
	static public Form textBoxForm;

	/* Text box for adding users to the contact list */
	static private TextField uinTextField;

	/* Textbox for  Status messages */
	static private TextBox statusMessage;

	static
	{
		list.setMode(VirtualList.MODE_TEXT);
	}
	
	static public boolean haveToRestoreStatus;

	public MainMenu()
	{
		_this = this;
	}

	static private Icon getStatusImage()
	{
		long cursStatus = Options.getLong(Options.OPTION_ONLINE_STATUS);
		int imageIndex = JimmUI.getStatusImageIndex(cursStatus);
		return ContactList.getImageList().elementAt(imageIndex);
	}

	public static Icon getXStatusImage()
	{
		return XStatus.getStatusImage(Options.getInt(Options.OPTION_XSTATUS));
	}

	public static Icon getPrivateStatusImage()
	{
		int index = 4;

		switch (Options.getInt(Options.OPTION_PRIVATE_STATUS))
		{
			case OtherAction.PSTATUS_ALL:
				index = 0;
				break;
			case OtherAction.PSTATUS_VISIBLE_ONLY:
				index = 1;
				break;
			case OtherAction.PSTATUS_NOT_INVISIBLE:
				index = 2;
				break;
			case OtherAction.PSTATUS_CL_ONLY:
				index = 3;
				break;
			case OtherAction.PSTATUS_NONE:
				index = 4;
				break;
		}

		return ContactList.psIcons.elementAt(index);
	}

	/* Builds the main menu (visual list) */
	public static void build()
	{
		JimmUI.setColorScheme(list, false);
		boolean connected = Icq.isConnected();

		list.lock();
		int lastIndex = list.getCurrTextIndex();
		
		list.removeAllCommands();
		list.clear();

		if (connected)
		{
			JimmUI.addTextListItem(list, "keylock_enable", ContactList.menuIcons.elementAt(0), MENU_KEYLOCK, true);
			JimmUI.addTextListItem(list, "disconnect", ContactList.menuIcons.elementAt(18), MENU_DISCONNECT, true);
		}
		else
		{
			JimmUI.addTextListItem(list, "connect", ContactList.menuIcons.elementAt(17), MENU_CONNECT, true);
		}

		JimmUI.addTextListItem(list, "set_status", getStatusImage(), MENU_STATUS, true);
		if (getXStatusImage() != null)
		{
			JimmUI.addTextListItem(list, "set_xstatus", getXStatusImage(), MENU_XSTATUS, true);
		}
		if (getPrivateStatusImage() != null)
		{
			JimmUI.addTextListItem(list, "private_status", getPrivateStatusImage(), MENU_PRIVATE_STATUS, true);
		}

		//#sijapp cond.if target isnot "DEFAULT"#
		boolean isSilent = Options.getBoolean(Options.OPTION_SILENT_MODE);
		JimmUI.addTextListItem(list, getSoundValue(isSilent), getSoundImage(isSilent), MENU_SOUND, true);
		//#sijapp cond.end#

		if (connected)
		{
			JimmUI.addTextListItem(list, "manage_contact_list", ContactList.menuIcons.elementAt(1), MENU_GROUPS, true);

			JimmUI.addTextListItem(list, "myself", ContactList.menuIcons.elementAt(9), MENU_MYSELF, true);
		}
		else
			JimmUI.addTextListItem(list, "contact_list", ContactList.menuIcons.elementAt(1), MENU_LIST, true);

		if (Options.getBoolean(Options.OPTION_MAGIC_EYE))
			JimmUI.addTextListItem(list, "magic_eye", ContactList.menuIcons.elementAt(33), MENU_MAGIC_EYE, true);

		//#sijapp cond.if target is "MIDP2"#
		/* For E2 */
		if (!Jimm.is_phone_SE() && !Jimm.is_smart_SE())
		{
			JimmUI.addTextListItem(list, "calls", XStatus.imageList.elementAt(14), MENU_CALL_SMS, true);
		}
		//#sijapp cond.end#
		//#sijapp cond.if target is "SIEMENS2"#
		if (!Jimm.is_SGold())
		{
			JimmUI.addTextListItem(list, "calls", XStatus.imageList.elementAt(14), MENU_CALL_SMS, true);
		}
		//#sijapp cond.end#

		JimmUI.addTextListItem(list, "options_lng",  ContactList.menuIcons.elementAt(3), MENU_OPTIONS, true);

		//#sijapp cond.if modules_TRAFFIC is "true" #
		JimmUI.addTextListItem(list, "traffic_lng", ContactList.menuIcons.elementAt(24), MENU_TRAFFIC, true);
		//#sijapp cond.end#

		JimmUI.addTextListItem(list, "about", ContactList.menuIcons.elementAt(4), MENU_ABOUT, true);

		//#sijapp cond.if target is "MIDP2" #
		if (Jimm.is_phone_SE())
		{
			JimmUI.addTextListItem(list, "minimize", ContactList.menuIcons.elementAt(27), MENU_MINIMIZE, true);
		}
		//#sijapp cond.end#

		//#sijapp cond.if target is "SIEMENS2" #
		JimmUI.addTextListItem(list, "minimize", ContactList.menuIcons.elementAt(27), MENU_MINIMIZE, true);
		//#sijapp cond.end#

		JimmUI.addTextListItem(list, "exit", ContactList.menuIcons.elementAt(28), MENU_EXIT, true);

		list.addCommandEx(JimmUI.cmdSelect, VirtualList.MENU_LEFT_BAR);
		if (connected) list.addCommandEx(JimmUI.cmdBack, VirtualList.MENU_RIGHT_BAR);
		else list.addCommandEx(exitCommand, VirtualList.MENU_RIGHT_BAR);
			
		list.selectTextByIndex(lastIndex);
		list.unlock();
		
		list.setCyclingCursor(true);

		list.setCommandListener(_this);
	}

	/* Displays the given alert and activates the main menu afterwards */
	static public void activate(Alert alert)
	{
		MainMenu.build();
		list.activate(Jimm.display, alert);
	}

	/* Activates the main menu */
	static public void activate()
	{
		if (Options.getBoolean(Options.OPTION_CLEAR_HEAP))
		{
			System.gc(); // Очистка кучи
		}
		MainMenu.build();
		list.activate(Jimm.display);
		JimmUI.setLastScreen(list);
	}

	public static TextList getUIConrol()
	{
		return list;
	}

	/* Show form for adding user */
	static public void showTextBoxForm(String caption, String label, String text, int fieldType)
	{
		textBoxForm = new Form(ResourceBundle.getString(caption));
		uinTextField = new TextField(ResourceBundle.getString(label), text, 16, fieldType);
		textBoxForm.append(uinTextField);
		
		textBoxForm.addCommand(sendCommand);
		textBoxForm.addCommand(JimmUI.cmdCancel);
		textBoxForm.setCommandListener(_this);
		Jimm.display.setCurrent(textBoxForm);
	}

	private void doExit(boolean anyway)
	{
		if (!anyway && ContactList.getUnreadMessCount() > 0)
		{
			JimmUI.messageBox(ResourceBundle.getString("attention"), ResourceBundle.getString("have_unread_mess"), JimmUI.MESBOX_YESNO, _this, TAG_EXIT);
		}
		else
		{
			Icq.disconnect();
			try
			{
				Thread.sleep(500);
			}
			catch (InterruptedException e1) {}
			/* Exit app */
			try
			{
				Jimm.jimm.destroyApp(true);
			}
			catch (MIDletStateChangeException e) {}
		}
	}
	
	// #sijapp cond.if target isnot "DEFAULT" #	
	static private String getSoundValue(boolean value)
	{
		 return ResourceBundle.getString(value ? "#sound_on" : "#sound_off");
	}
	// #sijapp cond.end#

	// #sijapp cond.if target isnot "DEFAULT" #
	public static Icon getSoundImage(boolean value)
	{
		 return (value ? ContactList.menuIcons.elementAt(16) : ContactList.menuIcons.elementAt(15));
	}
	// #sijapp cond.end#

	private static void initStatusList()
	{
		statusList = new TextList(ResourceBundle.getString("set_status"));
		JimmUI.setColorScheme(statusList, false);
		statusList.setMode(TextList.MODE_TEXT);
		statusList.setCyclingCursor(true);

		statusList.lock();
		JimmUI.addTextListItem(statusList, "status_online", ContactList.imageList.elementAt(7), ContactList.STATUS_ONLINE, true);
		JimmUI.addTextListItem(statusList, "status_chat", ContactList.imageList.elementAt(1), ContactList.STATUS_CHAT, true);
		JimmUI.addTextListItem(statusList, "status_evil", ContactList.imageList.elementAt(8), ContactList.STATUS_EVIL, true);
		JimmUI.addTextListItem(statusList, "status_depression", ContactList.imageList.elementAt(9), ContactList.STATUS_DEPRESSION, true);
		JimmUI.addTextListItem(statusList, "status_home", ContactList.imageList.elementAt(10), ContactList.STATUS_HOME, true);
		JimmUI.addTextListItem(statusList, "status_work", ContactList.imageList.elementAt(11), ContactList.STATUS_WORK, true);
		JimmUI.addTextListItem(statusList, "status_lunch", ContactList.imageList.elementAt(12), ContactList.STATUS_LUNCH, true);
		JimmUI.addTextListItem(statusList, "status_away", ContactList.imageList.elementAt(0), ContactList.STATUS_AWAY, true);
		JimmUI.addTextListItem(statusList, "status_na", ContactList.imageList.elementAt(4), ContactList.STATUS_NA, true);
		JimmUI.addTextListItem(statusList, "status_occupied", ContactList.imageList.elementAt(5), ContactList.STATUS_OCCUPIED, true);
		JimmUI.addTextListItem(statusList, "status_dnd", ContactList.imageList.elementAt(2), ContactList.STATUS_DND, true);
		JimmUI.addTextListItem(statusList, "status_invisible", ContactList.imageList.elementAt(3), ContactList.STATUS_INVISIBLE, true);
		if (getPrivateStatusImage() == null)
		{
			JimmUI.addTextListItem(statusList, "status_invis_all", ContactList.imageList.elementAt(13), ContactList.STATUS_INVIS_ALL, true);
		}
		statusList.unlock();
	}

	/* Command listener */
	public void commandAction(Command c, Displayable d)
	{
		/* Exit by soft button */
		if (c == exitCommand)
		{
			doExit(false);
			return;
		}
		
		if (JimmUI.isControlActive(privateStatusActList))
		{
			if (c == JimmUI.cmdSelect)
			{
				Icq.getIcq().setPrivateStatus((byte)privateStatusActList.getCurrTextIndex());
				actionMMCLAct();
			}
			else if (c == JimmUI.cmdBack) activate();
			return;
		}

		if (JimmUI.isControlActive(groupActList))
		{
			if (c == JimmUI.cmdSelect) CLManagementItemSelected(groupActList.getCurrTextIndex());
			else if (c == JimmUI.cmdBack) activate();
			return;
		}

		if (JimmUI.getCurScreenTag() == TAG_RENAME_GROUPS)
		{
			if (c == JimmUI.cmdOk)
			{
				String groupName = ContactList.getGroupById(groupIds[JimmUI.getLastSelIndex()]).getName(); 
				showTextBoxForm("rename_group", "group_name", groupName, TextField.ANY);
			}
			else activate();
		} 

		else if (JimmUI.getCurScreenTag() == TAG_DELETE_GROUPS)
		{
			if (c == JimmUI.cmdOk)
			{
				UpdateContactListAction deleteGroupAct = new UpdateContactListAction(ContactList.getGroupById(groupIds[JimmUI.getLastSelIndex()]), UpdateContactListAction.ACTION_DEL);
				try
				{
					Icq.requestAction(deleteGroupAct);
					SplashCanvas.addTimerTask("wait", deleteGroupAct, false);
				}
				catch (JimmException e)
				{
					JimmException.handleException(e);
				}
			}
			else activate();
		}

		// Return to works screen after canceling status selection 
		else if ((c == JimmUI.cmdBack) && (JimmUI.isControlActive(statusList) || JimmUI.isControlActive(selector)))
		{
			activate();
			statusList = null;
			selector = null;
		}

		// Activate contact list after pressing "back" menu
		else if ((c == JimmUI.cmdBack) && JimmUI.isControlActive(list))
		{
			ContactList.activate();
		}

		else if ((c == JimmUI.cmdSelect) && JimmUI.isControlActive(selector))
		{
			selectXst();
			selector = null;
		}

		else if ((c == sendCommand) && (d == textBoxForm) && (textBoxForm != null))
		{
			Action act = null;

			switch (status)
			{
			case STATUS_ADD_GROUP:
				GroupItem newGroup = new GroupItem(uinTextField.getString());
				act = new UpdateContactListAction(newGroup, UpdateContactListAction.ACTION_ADD);
				break;
				
			case STATUS_RENAME_GROUP:
				GroupItem group = ContactList.getGroupById(groupIds[JimmUI.getLastSelIndex()]);
				group.setName(uinTextField.getString());
				ContactList.safeSave();
				act = new UpdateContactListAction(group, UpdateContactListAction.ACTION_RENAME);
				break;
			}
			
			status = STATUS_NONE;
			
			try
			{
				Icq.requestAction(act);
			}
			catch (JimmException e)
			{
				JimmException.handleException(e);
				if (e.isCritical()) return;
			}
			// Start timer
			SplashCanvas.addTimerTask("wait", act, false);
		}

		else if ((c == JimmUI.cmdCancel) && (d == textBoxForm))
		{
			activate();
			textBoxForm = null;
		}


		/* User select OK in exit questiom message box */
		else if (JimmUI.getCommandType(c, TAG_EXIT) == JimmUI.CMD_YES)
		{
			doExit(true);
		}

		/* User select CANCEL in exit questiom message box */
		else if (JimmUI.getCommandType(c, TAG_EXIT) == JimmUI.CMD_NO)
		{
			ContactList.activate();
		}

		/* Menu item has been selected */
		else if ((c == JimmUI.cmdSelect) && JimmUI.isControlActive(list))
		{
			int selectedIndex = list.getCurrTextIndex();

			switch (selectedIndex)
			{
			case MENU_CONNECT: /* Connect */
				Icq.reconnect_attempts = Options.getInt(Options.OPTION_RECONNECT_NUMBER);
				ContactList.beforeConnect();
				Icq.connect();
				break;

			case MENU_DISCONNECT: /* Disconnect */
				Icq.disconnect();
				Thread.yield();
				/* Show the main menu */
				activate();
				break;

			case MENU_LIST: /* ContactList */
				ContactList.activate();
				break;

			case MENU_KEYLOCK: /* Enable keylock */
				SplashCanvas.lock();
				break;

			case MENU_STATUS: /* Set status */
				haveToRestoreStatus = false;
				initStatusList();
				statusList.selectTextByIndex((int)Options.getLong(Options.OPTION_ONLINE_STATUS));
				statusList.setCommandListener(_this);
				statusList.addCommandEx(JimmUI.cmdSelect, VirtualList.MENU_LEFT_BAR);
				statusList.addCommandEx(JimmUI.cmdBack, VirtualList.MENU_RIGHT_BAR);
				statusList.activate(Jimm.display);
				break;

			case MENU_XSTATUS: /* Set XStatus */
				selector = new Selector(1, (Options.getInt(Options.OPTION_XSTATUS) == XStatus.XSTATUS_NONE) ? 0 : Options.getInt(Options.OPTION_XSTATUS) + 1);
				JimmUI.setColorScheme(selector, false);
				selector.setCyclingCursor(true);
				selector.addCommandEx(JimmUI.cmdSelect, VirtualList.MENU_LEFT_BAR);
				selector.addCommandEx(JimmUI.cmdBack, VirtualList.MENU_RIGHT_BAR);
				selector.setCommandListener(this);
				selector.activate(Jimm.display);
				break;

			case MENU_PRIVATE_STATUS:
				privateStatusActList = new TextList(ResourceBundle.getString("private_status"));
				JimmUI.setColorScheme(privateStatusActList, false);
				privateStatusActList.setMode(TextList.MODE_TEXT);
				privateStatusActList.setCyclingCursor(true);

				privateStatusActList.lock();
				JimmUI.addTextListItem(privateStatusActList, "ps_all",               ContactList.psIcons.elementAt(0), OtherAction.PSTATUS_ALL,           true);
				JimmUI.addTextListItem(privateStatusActList, "ps_visible_list",      ContactList.psIcons.elementAt(1), OtherAction.PSTATUS_VISIBLE_ONLY,  true);
				JimmUI.addTextListItem(privateStatusActList, "ps_exclude_invisible", ContactList.psIcons.elementAt(2), OtherAction.PSTATUS_NOT_INVISIBLE, true);
				JimmUI.addTextListItem(privateStatusActList, "ps_contact_list",      ContactList.psIcons.elementAt(3), OtherAction.PSTATUS_CL_ONLY,       true);
				JimmUI.addTextListItem(privateStatusActList, "ps_none",              ContactList.psIcons.elementAt(4), OtherAction.PSTATUS_NONE,          true);
				privateStatusActList.unlock();

				privateStatusActList.setCommandListener(_this);
				privateStatusActList.addCommandEx(JimmUI.cmdSelect, VirtualList.MENU_LEFT_BAR);
				privateStatusActList.addCommandEx(JimmUI.cmdBack, VirtualList.MENU_RIGHT_BAR);
				privateStatusActList.selectTextByIndex(Options.getInt(Options.OPTION_PRIVATE_STATUS));
				privateStatusActList.activate(Jimm.display);
				break;

			case MENU_GROUPS:
				groupActList = new TextList(ResourceBundle.getString("manage_contact_list"));
				JimmUI.setColorScheme(groupActList, false);
				groupActList.setMode(TextList.MODE_TEXT);
				groupActList.setCyclingCursor(true);

				groupActList.lock();
				//JimmUI.addTextListItem(groupActList, "add_user",   ContactList.imageList.elementAt(5),  0, true);
				JimmUI.addTextListItem(groupActList, "search_user",  ContactList.menuIcons.elementAt(2),  0, true);
				JimmUI.addTextListItem(groupActList, "add_group",    ContactList.menuIcons.elementAt(29), 1, true);
				JimmUI.addTextListItem(groupActList, "rename_group", ContactList.menuIcons.elementAt(11), 2, true);
				JimmUI.addTextListItem(groupActList, "del_group",    ContactList.menuIcons.elementAt(6),  3, true);
				groupActList.unlock();

				groupActList.setCommandListener(_this);
				groupActList.addCommandEx(JimmUI.cmdSelect, VirtualList.MENU_LEFT_BAR);
				groupActList.addCommandEx(JimmUI.cmdBack, VirtualList.MENU_RIGHT_BAR);
				groupActList.activate(Jimm.display);
				break;

			case MENU_OPTIONS: /* Options */
				Options.editOptions();
				break;

			// #sijapp cond.if modules_TRAFFIC is "true" #
			case MENU_TRAFFIC: /* Traffic */
				Traffic.trafficScreen.activate();
				break;
			// #sijapp cond.end #

			case MENU_ABOUT:
				// Display an info
				JimmUI.about(null);
				break;

			//#sijapp cond.if target is "MIDP2"#
			case MENU_MINIMIZE: /* Minimize Jimm (if supported) */
				Jimm.setMinimized(true);
				break;
			//#sijapp cond.end#
				
			//#sijapp cond.if target is "SIEMENS2"#
			case MENU_MINIMIZE: /* Minimize Jimm (if supported) */
				try
				{
					if (!Jimm.is_SGold())
					{
						Jimm.jimm.platformRequest("tel://NAT_CONTACTS_LIST");
					}
					Jimm.jimm.platformRequest(Jimm.strMenuCall);
				}
				catch(Exception exc1){}
				break;
			//#sijapp cond.end#

			// #sijapp cond.if target isnot "DEFAULT" #
			case MENU_SOUND:
				ContactList.changeSoundMode(false);
				build();
				break;
				//#sijapp cond.end#

			case MENU_MAGIC_EYE:
				MagicEye.activate();
				break;

			//#sijapp cond.if target is "MIDP2" | target is "SIEMENS2"#
			case MENU_CALL_SMS:
				PhoneBook.activate();
				break;
			//#sijapp cond.end#

			case MENU_MYSELF:
				JimmUI.requiestUserInfo(Options.getString(Options.OPTION_UIN), "");
				break;
				
			case MENU_EXIT: /* Exit */
				doExit(false);
				break;
			}
		}
		
		/* Online status has been selected */
		else if ((c == JimmUI.cmdSelect) && JimmUI.isControlActive(statusList))
		{
			userSelectStatus();
		}

		else if ((d == statusMessage) && (c == JimmUI.cmdSelect))
		{
			int onlineStatus = (int)Options.getLong(Options.OPTION_ONLINE_STATUS);

			Options.setString(statusMsgIdxSelector(onlineStatus), statusMessage.getString());
			Options.safe_save();
			setOnlineStatus(onlineStatus);
		}
		
		/* Contact list management group */
		else if (JimmUI.getCommandType(c, TAG_CL) == JimmUI.CMD_OK)
		{
			CLManagementItemSelected(JimmUI.getLastSelIndex());
		}
	}
	
	private void userSelectStatus()
	{
		boolean activateMenu = false;
		int onlineStatus = statusList.getCurrTextIndex();
		Options.setLong(Options.OPTION_ONLINE_STATUS, onlineStatus);

		if ((onlineStatus != ContactList.STATUS_INVISIBLE) && (onlineStatus != ContactList.STATUS_INVIS_ALL)
				&& (onlineStatus != ContactList.STATUS_ONLINE) && (onlineStatus != ContactList.STATUS_CHAT))
		{
			statusMessage = new TextBox(ResourceBundle.getString("status_message"), Options.getString(statusMsgIdxSelector(onlineStatus)), 255, TextField.ANY);

			statusMessage.addCommand(JimmUI.cmdSelect);
			statusMessage.setCommandListener(_this);
			Jimm.display.setCurrent(statusMessage);
		}
		else activateMenu = true;

		Options.safe_save();
		statusList = null;
		
		if (activateMenu) setOnlineStatus(onlineStatus);
	}

	private void setOnlineStatus(int status)
	{
		if ((status == ContactList.STATUS_INVISIBLE))
		{
			try
			{
				Icq.getIcq().setPrivateStatus(OtherAction.PSTATUS_VISIBLE_ONLY);
			}
			catch (Exception e) {}
		}

		if (Icq.isConnected())
		{
			try
			{
				Icq.setOnlineStatus(status);
			}
			catch (JimmException e)
			{
				JimmException.handleException(e);
				if (e.isCritical()) return;
			}
		}
		/* Active MM/CL */
		actionMMCLAct();
	}

	private int statusMsgIdxSelector(int status)
	{
		int statusMsgIdx = Options.OPTION_STATUS_MESSAGE_AWAY;

		switch (status)
		{
		case ContactList.STATUS_AWAY:
			statusMsgIdx = Options.OPTION_STATUS_MESSAGE_AWAY;
			break;

		case ContactList.STATUS_NA:
			statusMsgIdx = Options.OPTION_STATUS_MESSAGE_NA;
			break;

		case ContactList.STATUS_DND:
			statusMsgIdx = Options.OPTION_STATUS_MESSAGE_DND;
			break;

		case ContactList.STATUS_OCCUPIED:
			statusMsgIdx = Options.OPTION_STATUS_MESSAGE_OCCUPIED;
			break;

		case ContactList.STATUS_EVIL:
			statusMsgIdx = Options.OPTION_STATUS_MESSAGE_EVIL;
			break;

		case ContactList.STATUS_DEPRESSION:
			statusMsgIdx = Options.OPTION_STATUS_MESSAGE_DEPRESSION;
			break;

		case ContactList.STATUS_HOME:
			statusMsgIdx = Options.OPTION_STATUS_MESSAGE_HOME;
			break;

		case ContactList.STATUS_WORK:
			statusMsgIdx = Options.OPTION_STATUS_MESSAGE_WORK;
			break;

		case ContactList.STATUS_LUNCH:
			statusMsgIdx = Options.OPTION_STATUS_MESSAGE_LUNCH;
			break;
		}
		return statusMsgIdx;
	}

	private void CLManagementItemSelected(int index)
	{
		switch (index)
		{
			case 0: /* Search for or Add User */
				Search searchf = new Search(false);
				searchf.getSearchForm().activate(Search.SearchForm.ACTIV_JUST_SHOW);
				break;

			case 1: /* Add group */
				status = STATUS_ADD_GROUP;
				showTextBoxForm("add_group", "group_name", null, TextField.ANY);
				break;

			case 2: /* Rename group */
				status = STATUS_RENAME_GROUP;
				groupIds = JimmUI.showGroupSelector("rename_group", TAG_RENAME_GROUPS, this, JimmUI.SHS_TYPE_ALL, -1); 
				break;

			case 3: /* Delete group */ 
				groupIds = JimmUI.showGroupSelector("del_group", TAG_DELETE_GROUPS, this, JimmUI.SHS_TYPE_EMPTY, -1);
				break;
		}
	}

	//XStatuses
	public static void selectXst()
	{
		int xstIndex = selector.getCurrSelectedIdx();

		if (xstIndex == 0) 
		{
			Options.setInt(Options.OPTION_XSTATUS, XStatus.XSTATUS_NONE);
			Options.safe_save();

			/* Active MM/CL */
			actionMMCLAct();

			if (Icq.isConnected()) 
			{
				try 
				{
					OtherAction.setStandartUserInfo();
				} 
				catch (JimmException e) 
				{
					JimmException.handleException(e);
				}
			}
		} 
		else 
		{
			XStatusForm.activate(xstIndex);
		}
	}

	private static void actionMMCLAct()
	{
		if (Icq.isConnected())
		{
			ContactList.activate();
		}
		else
		{
			activate();
		}
	}
}