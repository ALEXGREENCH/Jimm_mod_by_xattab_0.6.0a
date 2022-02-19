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
File: src/jimm/Templates.java
Version: ###VERSION###  Date: ###DATE###
Author(s): Igor Palkin, Arvin
*******************************************************************************/

package jimm;

import java.io.*;
import javax.microedition.lcdui.*;
import javax.microedition.rms.*;
import DrawControls.*;
import java.util.Vector;
import jimm.util.ResourceBundle;
import jimm.comm.Util;

public class Templates implements VirtualListCommands, CommandListener
{
	private static Command selectTemplateCommand = new Command(ResourceBundle.getString("select"), Command.OK, 1);
	//#sijapp cond.if target is "MIDP2"#
	private static Command backCommand = new Command(ResourceBundle.getString("back"), Jimm.is_smart_SE() ? Command.CANCEL : Command.BACK, 6);
	//#sijapp cond.else#
	private static Command backCommand = new Command(ResourceBundle.getString("back"), Command.BACK, 6);
	//#sijapp cond.end#
	private static Command newTemplateCommand = new Command(ResourceBundle.getString("add_new"), Command.ITEM, 3);
	private static Command editTemplateCommand = new Command(ResourceBundle.getString("edit"), Command.ITEM, 3);
	private static Command deleteCurrentTemplateCommand = new Command(ResourceBundle.getString("delete"), Command.ITEM, 4);
	private static Command clearCommand = new Command(ResourceBundle.getString("clear"), Command.ITEM, 5);

	private static Command addCommand = new Command(ResourceBundle.getString("save"), Command.OK, 1);
	private static Command editCommand = new Command(ResourceBundle.getString("save"), Command.OK, 1);
	//#sijapp cond.if target is "MIDP2"#
	private static Command cancelCommand = new Command(ResourceBundle.getString("back"), Jimm.is_smart_SE() ? Command.CANCEL : Command.BACK, 2);
	//#sijapp cond.else#
	private static Command cancelCommand = new Command(ResourceBundle.getString("back"), Command.BACK, 2);
	//#sijapp cond.end#

	private static TextList templateList;
	private static Templates _this;
	private static RecordStore rms = null;
	private static TextBox templateTextbox;
	
	private static Vector templates = new Vector();

	private static final int TMPL_DEL = 1;
	private static final int TMPL_CLALL = 2;

	private static Object lastScreen;
	private static TextBox textBox;
	private static int caretPos;

	public Templates()
	{
		load();
		_this = this;
	}

	public static void selectTemplate(TextBox textBox, Object lastScreen)
	{
		Templates.lastScreen = lastScreen;
		Templates.textBox = textBox;
		caretPos = textBox.getCaretPosition();

		templateList = new TextList(null);
		JimmUI.setColorScheme(templateList, false);
		templateList.setCaption(ResourceBundle.getString("templates"));
		templateList.addCommandEx(JimmUI.cmdMenu, VirtualList.MENU_LEFT_BAR);
		templateList.addCommandEx(backCommand, VirtualList.MENU_RIGHT_BAR);
		templateList.addCommandEx(newTemplateCommand, VirtualList.MENU_LEFT);
		if (templates.size() > 0) addContextCommand();
		templateList.setFontSize(Font.SIZE_SMALL);
		refreshList();

		templateList.setCyclingCursor(true);

		templateList.setCommandListener(_this);
		templateList.setVLCommands(_this);
		templateList.activate(Jimm.display);
	}

	public static boolean isMyOkCommand(Command c)
	{
		return ( c == selectTemplateCommand);
	}

	public void vlKeyPress(VirtualList sender, int keyCode,int type) {}
	public void vlCursorMoved(VirtualList sender) {}
	public void vlItemClicked(VirtualList sender) 
	{
		select();
	}

	public void commandAction(Command c, Displayable d)
	{
		if (c == backCommand)
		{
			JimmUI.selectScreen(lastScreen);
			templateList = null;
		}

		if (c == selectTemplateCommand)
		{
			select();
		}

		if (c == newTemplateCommand)
		{
			templateTextbox = new TextBox(ResourceBundle.getString("new_template"), null, 1000, TextField.ANY);
			templateTextbox.addCommand(addCommand);
			templateTextbox.addCommand(cancelCommand);
			templateTextbox.setCommandListener(_this);
			Jimm.display.setCurrent(templateTextbox);
		}

		if (c == editTemplateCommand)
		{
			templateTextbox = new TextBox(ResourceBundle.getString("edit"), getTemlate(), 1000, TextField.ANY);
			templateTextbox.addCommand(editCommand);
			templateTextbox.addCommand(cancelCommand);
			templateTextbox.setCommandListener(_this);
			Jimm.display.setCurrent(templateTextbox);
		}

		if (c == addCommand)
		{
			String text = templateTextbox.getString();
			if (text.length() >0) templates.addElement(text);
			refresh();
			addContextCommand();
		}

		if (c == deleteCurrentTemplateCommand)
		{
			templates.removeElementAt(templateList.getCurrTextIndex());
			refresh();
			if (templates.size() <= 0) removeContextCommand();
		}

		if (c == editCommand)
		{
			String text = templateTextbox.getString();
			if (text.length() > 0) templates.setElementAt(text, templateList.getCurrTextIndex());
			else templates.removeElementAt(templateList.getCurrTextIndex());
			refresh();
		}

		if (c == cancelCommand)
		{
			templateList.activate(Jimm.display);
			templateTextbox = null;
		}
		
		if (c == clearCommand)
		{
			JimmUI.messageBox(ResourceBundle.getString("attention"), ResourceBundle.getString("clear") + "?", JimmUI.MESBOX_YESNO, _this, TMPL_CLALL);
		}
		
		if (JimmUI.getCommandType(c, TMPL_CLALL) == JimmUI.CMD_YES)
		{
			templates.removeAllElements();
			save();
			JimmUI.selectScreen(lastScreen);
		}

		if (JimmUI.getCommandType(c, TMPL_CLALL) == JimmUI.CMD_NO)
		{
			templateList.activate(Jimm.display);
		}
	}

	private void select()
	{
		String selectedTemplate = null;
		if (templateList.getSize() != 0) selectedTemplate = getTemlate();
		sort();
		templateList = null;
		JimmUI.selectScreen(lastScreen);
		if (selectedTemplate != null) textBox.insert(selectedTemplate, caretPos);
	}

	private void sort()
	{
		String text = (String)templates.elementAt(templateList.getCurrTextIndex());
		for (int j = templateList.getCurrTextIndex(); j >= 1; j--)
		{
			String oldtext = (String)templates.elementAt(j - 1);
			templates.setElementAt(oldtext, j);
		}
		templates.setElementAt(text, 0);
		save();
	}

	private static void refreshList()
	{
		templateList.lock();
		templateList.clear();
		int count = templates.size();
		for ( int i = 0; i < count; i++)
			templateList.addBigText((String)templates.elementAt(i), templateList.getTextColor(), Font.STYLE_PLAIN,i).doCRLF(i);
		templateList.unlock();
	}

	private static void load()
	{
		RecordStore rms = null;
		templates.removeAllElements();
		try
		{
			rms = RecordStore.openRecordStore("tmpl", false);
			int size = rms.getNumRecords();
			for (int i = 1; i <= size; i++)
			{
				byte[] data = rms.getRecord(i);
				String str = Util.byteArrayToString(data, 0, data.length,true);
				templates.addElement(str);
			}
		}
		catch (Exception e) {}
		try
		{
			rms.closeRecordStore();
		}
		catch (Exception e) {}
	}

	private static void save()
	{
		try
		{
			RecordStore.deleteRecordStore("tmpl");
		}
		catch (Exception e) {}
		if (templates.size() == 0) return;
		RecordStore rms = null;
		try
		{
			rms = RecordStore.openRecordStore("tmpl", true);
			int size = templates.size();
			for (int i = 0; i < size; i++)
			{
				String str = (String)templates.elementAt(i);
				byte[] buffer = Util.stringToByteArray(str, true);
				rms.addRecord(buffer, 0, buffer.length);
			}
		}
		catch (Exception e) {}
		try
		{
			rms.closeRecordStore();
		}
		catch (Exception e) {}
	}

	public String getTemlate()
	{
		return (String)templates.elementAt(templateList.getCurrTextIndex());
	}

	private static void addContextCommand()
	{
		removeContextCommand();

		templateList.addCommandEx(selectTemplateCommand, VirtualList.MENU_LEFT);
		templateList.addCommandEx(editTemplateCommand, VirtualList.MENU_LEFT);
		templateList.addCommandEx(deleteCurrentTemplateCommand, VirtualList.MENU_LEFT);
		templateList.addCommandEx(clearCommand, VirtualList.MENU_LEFT);
	}

	private static void removeContextCommand()
	{
		templateList.removeCommandEx(selectTemplateCommand);
		templateList.removeCommandEx(editTemplateCommand);
		templateList.removeCommandEx(deleteCurrentTemplateCommand);
		templateList.removeCommandEx(clearCommand);
	}

	private void refresh()
	{
		save();
		refreshList();
		templateList.activate(Jimm.display);
		templateTextbox = null;
	}
}