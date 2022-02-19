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
 File: src/jimm/util/MagicEye.java
 Version: ###VERSION###  Date: ###DATE###
 Author(s): Denis K.
 *******************************************************************************/

package jimm.util;

import DrawControls.*;
import java.util.Vector;
import javax.microedition.lcdui.*;
import jimm.*;
import jimm.comm.*;

public class MagicEye implements CommandListener
{
	private static MagicEye instance = new MagicEye();
	private static TextList list;
	private static MagicEye _this;
	private Vector uins = new Vector();

	private static final Command cmdContactMenu = new Command(ResourceBundle.getString("user_menu"), Command.OK, 1);

	static
	{
		list = new TextList(ResourceBundle.getString("magic_eye"));
		list.setFontSize(TextList.SMALL_FONT);
		list.setMode(TextList.MODE_TEXT);
		list.addCommandEx(JimmUI.cmdBack, TextList.MENU_RIGHT_BAR);
	}

	public MagicEye()
	{
		_this = this;
	}

	public static void activate()
	{
		removeCommands();

		if (list.getCurrTextIndex() != -1)
		{
			list.addCommandEx(JimmUI.cmdMenu, TextList.MENU_LEFT_BAR);
			list.addCommandEx(JimmUI.cmdCopyText, TextList.MENU_LEFT);
			list.addCommandEx(JimmUI.cmdCopyAll, TextList.MENU_LEFT);
			list.addCommandEx(JimmUI.cmdClearText, TextList.MENU_LEFT);
			list.addCommandEx(cmdContactMenu, TextList.MENU_LEFT);
		}

		list.setCommandListener(_this);
		JimmUI.setColorScheme(list, false);
		list.activate(Jimm.display);
	}

	private static void removeCommands()
	{
		list.removeCommandEx(JimmUI.cmdMenu);
		list.removeCommandEx(JimmUI.cmdCopyText);
		list.removeCommandEx(JimmUI.cmdCopyAll);
		list.removeCommandEx(JimmUI.cmdClearText);
		list.removeCommandEx(cmdContactMenu);
	}

	private int counter = 1;

	private synchronized void registerAction(String uin, String action, String msg)
	{
		if (!Options.getBoolean(Options.OPTION_MAGIC_EYE)) return;

		uins.addElement(uin);
		ContactItem contact = ContactList.getItembyUIN(uin);
		action = ResourceBundle.getString(action);
		String date = Util.getDateString(true, true);
		int textColor = Options.getInt(Options.OPTION_COLOR_TEXT);
		int nickColor = Options.getInt(Options.OPTION_COLOR_MY_NICK);

		list.lock();
		list.addBigText("[" + counter + "]: ", textColor, Font.STYLE_PLAIN, counter);

		if (contact == null) list.addBigText(uin + " (" + date + ") ", 0xFF0000, Font.STYLE_BOLD, counter);
		else list.addBigText(contact.name + /*" [" + uin + "]*/" (" + date + ") ", nickColor, Font.STYLE_BOLD, counter);

		list.addBigText(action, textColor, Font.STYLE_PLAIN, counter);

		if (null != msg)
		{
			list.doCRLF(counter);
			list.addBigText(msg, textColor, Font.STYLE_PLAIN, counter);
		}

		list.doCRLF(counter);
		counter++;
		list.setTopItem(list.getSize());
		list.unlock();
	}

	public static void addAction(String uin, String action, String msg)
	{
		instance.registerAction(uin, action, msg);
	}

	public static void addAction(String uin, String action)
	{
		instance.registerAction(uin, action, null);
	}

	public void commandAction(Command c, Displayable d)
	{
		if (c == JimmUI.cmdBack)
		{
			list.uiState = list.UI_STATE_NORMAL;
			if (JimmUI.lastScreen == list)
			{
				ContactList.activate();
			}
			else
			{
				JimmUI.backToLastScreen(/*true*/);
			}
		}

		else if (c == cmdContactMenu)
		{
			JimmUI.setLastScreen(list);
			try
			{
				String uin = (String)uins.elementAt(list.getCurrTextIndex() - 1);
				ContactItem contact = ContactList.createTempContact(uin);
				JimmUI.showContactMenu(contact);
			}
			catch (Exception e) {}
		}

		else if ((c == JimmUI.cmdCopyText) || (c == JimmUI.cmdCopyAll))
		{
			JimmUI.setClipBoardText("[" + JimmUI.getCaption(list) + "]\n" + list.getCurrText(0, (c == JimmUI.cmdCopyAll)));
		}

		else if (c == JimmUI.cmdClearText)
		{
			synchronized (instance)
			{
				uins.removeAllElements();
				counter = 1;
				list.clear();
				removeCommands();
			}
		}
    }
}