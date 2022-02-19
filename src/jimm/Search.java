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
 File: src/jimm/Search.java
 Version: ###VERSION###  Date: ###DATE###
 Author(s): Andreas Rossbacher
 *******************************************************************************/

package jimm;

import java.util.Vector;

import javax.microedition.lcdui.*;

import jimm.comm.SearchAction;
import jimm.comm.Icq;
import jimm.comm.Util;
import jimm.util.*;
import DrawControls.*;

public class Search
{
	SearchForm searchForm;

	private boolean liteVersion;

	final public static int UIN = 0;
	final public static int NICK = 1;
	final public static int FIRST_NAME = 2;
	final public static int LAST_NAME = 3;
	final public static int EMAIL = 4;
	final public static int CITY = 5;
	final public static int KEYWORD = 6;
	final public static int GENDER = 7;
	final public static int ONLY_ONLINE = 8;
	final public static int AGE = 9;
	final public static int LAST_INDEX = 10;

	/* Results */
	private Vector results;

	/* Constructor */
	public Search(boolean lite)
	{
		results = new Vector();
		liteVersion = lite;
	}

	/* Add a result to the results vector */
	public void addResult(String uin, String nick, String name, String email, String auth, int status, String gender, int age)
	{
		String[] resultData = new String[JimmUI.UI_LAST_ID];

		resultData[JimmUI.UI_UIN_LIST] = uin;
		resultData[JimmUI.UI_NICK] = nick;
		resultData[JimmUI.UI_NAME] = name;
		resultData[JimmUI.UI_EMAIL] = email;
		resultData[JimmUI.UI_AUTH] = auth;
		resultData[JimmUI.UI_STATUS] = Integer.toString(status);
		resultData[JimmUI.UI_GENDER] = gender;
		resultData[JimmUI.UI_AGE] = Integer.toString(age);

		results.addElement(resultData);
	}

	/* Return a result object by given Nr */
	public String[] getResult(int nr)
	{
		return (String[]) results.elementAt(nr);
	}

	/* Return size of search results */
	public int size()
	{
		return results.size();
	}

	/* Return the SearchForm object */
	public SearchForm getSearchForm()
	{
		if (searchForm == null) searchForm = new SearchForm();
		return searchForm;
	}

	/** ************************************************************************* */
	/** ************************************************************************* */
	/** ************************************************************************* */

	/* Class for the search forms */
	public class SearchForm implements CommandListener, VirtualListCommands
	{
		/* Commands */
		private Command backCommand;
		private Command searchCommand;
		private Command addCommand;
		private Command previousCommand;
		private Command nextCommand;
		private Command cmdSendMessage;
		private Command cmdShowInfo;
		private Command cmdCheckStatus;

		/* Forms for results and query */
		private Form searchForm;

		private TextList screen;

		/* List for group selection */
		private List groupList;

		/* Textboxes for search */
		private TextField uinSearchTextBox;
		private TextField nickSearchTextBox;
		private TextField firstnameSearchTextBox;
		private TextField lastnameSearchTextBox;
		private TextField emailSearchTextBox;
		private TextField citySearchTextBox;
		private TextField keywordSearchTextBox;
		private ChoiceGroup chgrAge;

		/* Choice boxes for gender and online choice */
		private ChoiceGroup gender;

		private ChoiceGroup onlyOnline;

		/* Selectet index in result screen */
		int selectedIndex;

		/* constructor for search form */
		public SearchForm()
		{
			/* Commands */
			searchCommand = new Command(ResourceBundle.getString("user_search"), Command.OK, 1);
			//#sijapp cond.if target is "MIDP2"#
			backCommand = new Command(ResourceBundle.getString("back"), Jimm.is_smart_SE() ? Command.CANCEL : Command.BACK, 2);
			//#sijapp cond.else#
			backCommand = new Command(ResourceBundle.getString("back"), Command.BACK, 2);
			//#sijapp cond.end#
			addCommand = new Command(ResourceBundle.getString("add_to_list"), Command.ITEM, 1);
			cmdCheckStatus = new Command(ResourceBundle.getString("check_status"), Command.ITEM, 2);
			nextCommand = new Command(ResourceBundle.getString("next"), Command.ITEM, 3);
			previousCommand = new Command(ResourceBundle.getString("prev"), Command.ITEM, 4);
			cmdSendMessage = new Command(ResourceBundle.getString("send_message"), Command.ITEM, 6);
			cmdShowInfo = new Command(ResourceBundle.getString("info"), Command.ITEM, 7);

			/* Form */
			searchForm = new Form(ResourceBundle.getString("search_user"));

			/* TextFields */
			uinSearchTextBox = new TextField(ResourceBundle.getString("uin"), "", 32, TextField.NUMERIC);
			nickSearchTextBox = new TextField(ResourceBundle.getString("nick"), "", 32, TextField.ANY);
			firstnameSearchTextBox = new TextField(ResourceBundle.getString("firstname"), "", 32, TextField.ANY);
			lastnameSearchTextBox = new TextField(ResourceBundle.getString("lastname"), "", 32, TextField.ANY);
			emailSearchTextBox = new TextField(ResourceBundle.getString("email"), "", 32, TextField.EMAILADDR);
			citySearchTextBox = new TextField(ResourceBundle.getString("city"), "", 32, TextField.ANY);
			keywordSearchTextBox = new TextField(ResourceBundle.getString("keyword"), "", 32, TextField.ANY);

			chgrAge = new ChoiceGroup(ResourceBundle.getString("age"), ChoiceGroup.POPUP, Util.explode("---|13-17|18-22|23-29|30-39|40-49|50-59|> 60", '|'), null);

			/* Choice Groups */
			gender = new ChoiceGroup(ResourceBundle.getString("gender"), Choice.POPUP);
			gender.append(ResourceBundle.getString("female_male"), null);
			gender.append(ResourceBundle.getString("female"), null);
			gender.append(ResourceBundle.getString("male"), null);
			onlyOnline = new ChoiceGroup("", Choice.MULTIPLE);
			onlyOnline.append(ResourceBundle.getString("only_online"), null);

			searchForm.append(onlyOnline);
			searchForm.append(uinSearchTextBox);
			searchForm.append(nickSearchTextBox);
			searchForm.append(firstnameSearchTextBox);
			searchForm.append(lastnameSearchTextBox);
			searchForm.append(citySearchTextBox);
			searchForm.append(gender);
			searchForm.append(emailSearchTextBox);
			searchForm.append(keywordSearchTextBox);
			searchForm.append(chgrAge);
			searchForm.setCommandListener(this);

			/* Result Screen */
			screen = new TextList(null);
			screen.setVLCommands(this);

			if (liteVersion)
			{
				screen.addCommandEx(addCommand, VirtualList.MENU_LEFT_BAR);
			}
			else
			{
				screen.addCommandEx(JimmUI.cmdMenu, VirtualList.MENU_LEFT_BAR);
				screen.addCommandEx(previousCommand, VirtualList.MENU_LEFT);
				screen.addCommandEx(nextCommand, VirtualList.MENU_LEFT);
				screen.addCommandEx(addCommand, VirtualList.MENU_LEFT);
				screen.addCommandEx(cmdCheckStatus, VirtualList.MENU_LEFT);
				screen.addCommandEx(cmdSendMessage, VirtualList.MENU_LEFT);
				screen.addCommandEx(cmdShowInfo, VirtualList.MENU_LEFT);
			}

			screen.setMode(TextList.MODE_TEXT);
			JimmUI.setColorScheme(screen, false);
		}
		
		static final public int ACTIV_SHOW_RESULTS   = 1;
		static final public int ACTIV_JUST_SHOW      = 2;
		static final public int ACTIV_SHOW_NORESULTS = 3;

		/* Activate search form */
		public void activate(int type)
		{
			switch (type)
			{
			case ACTIV_SHOW_RESULTS:
				drawResultScreen(selectedIndex);
				screen.activate(Jimm.display);
				break;
				
			case ACTIV_JUST_SHOW:
				searchForm.addCommand(searchCommand);
				searchForm.addCommand(backCommand);
				Jimm.display.setCurrent(searchForm);
				break;
				
			case ACTIV_SHOW_NORESULTS:
				searchForm.addCommand(searchCommand);
				searchForm.addCommand(backCommand);
            	Alert alert = new Alert(null, ResourceBundle.getString("no_results"), null, null);
            	alert.setTimeout(Alert.FOREVER);
            	Jimm.display.setCurrent(alert, searchForm);
				break;
			}
		}

		public void drawResultScreen(int n)
		{
			/* Remove the older entrys here */
			screen.clear();

			if (Search.this.size() > 0)
			{
				if (Search.this.size() == 1)
				{
					screen.removeCommandEx(nextCommand);
					screen.removeCommandEx(previousCommand);
				}

				screen.lock();

				JimmUI.fillUserInfo(getResult(n), screen);
				screen.setCaption(ResourceBundle.getString("results") + " " + Integer.toString(n + 1) + "/" + Integer.toString(Search.this.size()));
				screen.unlock();
			}
			else
			{
				/* Show a result entry */

				screen.lock();
				screen.setCaption(ResourceBundle.getString("results") + " 0/0");
				screen.addBigText(ResourceBundle.getString("no_results") + ": ", 0x0, Font.STYLE_BOLD, -1);
				screen.unlock();
			}

			screen.addCommandEx(backCommand, VirtualList.MENU_RIGHT_BAR);

			screen.setCommandListener(this);
		}

		public void nextOrPrev(boolean next)
		{
			if (next)
			{
				selectedIndex = (selectedIndex + 1) % Search.this.size();
				activate(Search.SearchForm.ACTIV_SHOW_RESULTS);
			}
			else
			{
				if (selectedIndex == 0) selectedIndex = Search.this.size() - 1;
				else
				{
					selectedIndex = (selectedIndex - 1) % Search.this.size();
				}
				activate(Search.SearchForm.ACTIV_SHOW_RESULTS);
			}

		}

		public void vlKeyPress(VirtualList sender, int keyCode, int type)
		{
			//#sijapp cond.if target is "MOTOROLA"#
			if (type == VirtualList.KEY_RELEASED)
			//#sijapp cond.else#
			if (type == VirtualList.KEY_PRESSED)
			//#sijapp cond.end#
			{
				switch (sender.getGameAction(keyCode))
				{
				case Canvas.LEFT:
					nextOrPrev(false);
					break;

				case Canvas.RIGHT:
					nextOrPrev(true);
					break;
				}
			}
		}

		public void vlCursorMoved(VirtualList sender) {}

		public void vlItemClicked(VirtualList sender) {}

		public void commandAction(Command c, Displayable d)
		{
			if (c == backCommand)
			{
				if (JimmUI.isControlActive(screen) && !liteVersion)
				{
					activate(Search.SearchForm.ACTIV_JUST_SHOW);
					//#sijapp cond.if target is "MOTOROLA"#
					LightControl.flash(true);
					//#sijapp cond.end#
				}
				else if (d == searchForm)
				{
					searchForm = null;
					MainMenu.activate();
				}
				else
				{
					if (d != groupList) searchForm = null;
					JimmUI.backToLastScreen(/*false*/);
				}
			}
			else if (c == searchCommand)
			{
				selectedIndex = 0;

				String[] data = new String[Search.LAST_INDEX];

				data[Search.UIN] = uinSearchTextBox.getString();
				data[Search.NICK] = nickSearchTextBox.getString();
				data[Search.FIRST_NAME] = firstnameSearchTextBox.getString();
				data[Search.LAST_NAME] = lastnameSearchTextBox.getString();
				data[Search.EMAIL] = emailSearchTextBox.getString();
				data[Search.CITY] = citySearchTextBox.getString();
				data[Search.KEYWORD] = keywordSearchTextBox.getString();
				data[Search.GENDER] = Integer.toString(gender.getSelectedIndex());
				data[Search.ONLY_ONLINE] = onlyOnline.isSelected(0) ? "1" : "0";
				data[Search.AGE] = Integer.toString(chgrAge.getSelectedIndex());

				SearchAction act = new SearchAction(Search.this, data, SearchAction.CALLED_BY_SEARCHUSER);
				try
				{
					Icq.requestAction(act);

				}
				catch (JimmException e)
				{
					JimmException.handleException(e);
					if (e.isCritical()) return;
				}

				/* Clear results */
				results.removeAllElements();

				/* Start timer */ 
				SplashCanvas.addTimerTask("wait", act, true);
			}
			
			/* "Next" command */
			else if (c == nextCommand) nextOrPrev(true);
			
			/* "Previous" command */
			else if (c == previousCommand) nextOrPrev(false);

			else if ((c == addCommand) && JimmUI.isControlActive(screen))
			{
				if (ContactList.getGroupItems().length == 0)
				{
					searchForm = null;
					Alert errorMsg = new Alert(ResourceBundle.getString("warning"), JimmException.getErrDesc(161, 0), null, AlertType.WARNING);
					errorMsg.setTimeout(Alert.FOREVER);
					ContactList.activate(errorMsg);
				}
				else
				{
					/* Show list of groups to select which group to add to */
					groupList = new List(ResourceBundle.getString("whichgroup"), List.EXCLUSIVE);
					for (int i = 0; i < ContactList.getGroupItems().length; i++)
					{
						groupList.append(ContactList.getGroupItems()[i].getName(), null);
					}
					groupList.addCommand(backCommand);
					groupList.addCommand(addCommand);
					groupList.setCommandListener(this);
					JimmUI.setLastScreen(screen); // задаем объект, куда возвратиться по команде Назад...
					Jimm.display.setCurrent(groupList);
				}
			}
			else if (c == addCommand && d == groupList)
			{
				searchForm = null;
				String[] resultData = getResult(selectedIndex);
				ContactItem cItem = new ContactItem(-1, ContactList.getGroupItems()[groupList.getSelectedIndex()].getId(),
						resultData[JimmUI.UI_UIN_LIST], resultData[JimmUI.UI_NICK], false, false);
				cItem.setBooleanValue(ContactItem.CONTACTITEM_NO_AUTH, resultData[JimmUI.UI_AUTH].equals("1"));
				cItem.setBooleanValue(ContactItem.CONTACTITEM_IS_TEMP, true);
				cItem.setIntValue(ContactItem.CONTACTITEM_STATUS, ContactList.STATUS_OFFLINE);
				Icq.addToContactList(cItem);
			}
			
			/* Command "Send message" */
			else if (c == cmdSendMessage)
			{
				String[] resultData = getResult(selectedIndex);
				ContactItem cItem = ContactList.createTempContact(resultData[JimmUI.UI_UIN_LIST]);
				cItem.setStringValue(ContactItem.CONTACTITEM_NAME, resultData[JimmUI.UI_NICK]);
				JimmUI.setLastScreen(screen); // задаем объект, куда возвратиться по команде Назад...
				JimmUI.writeMessage(cItem, null);
			}
			
			/* Command "Show info" */ 
			else if (c == cmdShowInfo)
			{
				String[] resultData = getResult(selectedIndex);
				JimmUI.requiestUserInfo(resultData[JimmUI.UI_UIN_LIST], resultData[JimmUI.UI_NICK]);
				JimmUI.setLastScreen(screen); // задаем объект, куда возвратиться по команде Назад...
			}

			else if (c == cmdCheckStatus)
			{
				String[] resultData = getResult(selectedIndex);
				JimmUI.checkStatus(resultData[JimmUI.UI_UIN_LIST], false);
			}
		}
	} /* end "class SearchForm" */
}
