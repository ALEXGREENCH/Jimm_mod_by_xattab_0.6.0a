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
Author(s): Igor Palkin
*******************************************************************************/
package jimm;

import java.io.ByteArrayOutputStream;
import javax.microedition.lcdui.*;

//#sijapp cond.if target is "MOTOROLA"#
import DrawControls.LightControl;
//#sijapp cond.end#
import jimm.comm.*;
import jimm.util.*;

public class EditInfo extends Form implements CommandListener, Runnable
{
	private TextField _NickNameItem  = new TextField(ResourceBundle.getString("nick"), null, 20, TextField.ANY);
	private TextField _FirstNameItem = new TextField(ResourceBundle.getString("firstname"), null, 20, TextField.ANY);
	private TextField _LastNameItem  = new TextField(ResourceBundle.getString("lastname"), null, 20, TextField.ANY);
	private TextField _EmailItem     = new TextField(ResourceBundle.getString("email"), null, 50, TextField.EMAILADDR);
	private TextField _BdayItem      = new TextField(ResourceBundle.getString("birth_day"), null, 15, TextField.ANY);
	private TextField _HomePageItem  = new TextField(ResourceBundle.getString("home_page"), null, 70, TextField.ANY);
	private TextField _AboutItem     = new TextField(ResourceBundle.getString("notes"), null, 600, TextField.ANY);
	private TextField _CityItem      = new TextField(ResourceBundle.getString("city"), null, 50, TextField.ANY);
	private ChoiceGroup _SexItem     = new ChoiceGroup(ResourceBundle.getString("gender"), ChoiceGroup.POPUP);
	private ChoiceGroup _InterestsItem = new ChoiceGroup(ResourceBundle.getString("interests"), ChoiceGroup.MULTIPLE);

	private static ChoiceGroup intterest1;
	private static ChoiceGroup intterest2;
	private static ChoiceGroup intterest3;
	private static ChoiceGroup intterest4;

	private TextField intterestText1;
	private TextField intterestText2;
	private TextField intterestText3;
	private TextField intterestText4;

	private TextField _CurrentPass   = new TextField(ResourceBundle.getString("current_pass"), null, 16, TextField.PASSWORD);
	private TextField _NewPass       = new TextField(ResourceBundle.getString("new_pass"), null, 8, TextField.PASSWORD);
	private TextField _NewPassAgain  = new TextField(ResourceBundle.getString("new_pass_again"), null, 8, TextField.PASSWORD);

	private Command _CmdCancel       = new Command(ResourceBundle.getString("cancel"), Command.BACK, 0);
	private Command _CmdSave         = new Command(ResourceBundle.getString("save"),   Command.OK, 1);
	private Command _CmdChange       = new Command(ResourceBundle.getString("change"), Command.OK, 1);

	private Displayable _PreviousForm;
	private static String[] userInfo;
	private static EditInfo editInfoForm;

	private EditInfo(Displayable currentForm) 
	{
		super(ResourceBundle.getString("editform"));
		_PreviousForm = currentForm;

		intterest1 = new ChoiceGroup(null, Choice.POPUP);
		intterest2 = new ChoiceGroup(null, Choice.POPUP);
		intterest3 = new ChoiceGroup(null, Choice.POPUP);
		intterest4 = new ChoiceGroup(null, Choice.POPUP);
		intterestText1 = new TextField(null, null, 60, TextField.ANY);
		intterestText2 = new TextField(null, null, 60, TextField.ANY);
		intterestText3 = new TextField(null, null, 60, TextField.ANY);
		intterestText4 = new TextField(null, null, 60, TextField.ANY);

		_SexItem.append("---", null);
		_SexItem.append(ResourceBundle.getString("female"), null);
		_SexItem.append(ResourceBundle.getString("male"), null);
		append(_NickNameItem);
		append(_FirstNameItem);
		append(_LastNameItem);
		append(_SexItem);
		append(_EmailItem);
		append(_BdayItem);
		append(_HomePageItem);
		append(_AboutItem);
		append(_CityItem);

		addCommand(_CmdSave);
		addCommand(_CmdCancel);
		setCommandListener(this);
	}

	private EditInfo(boolean fake, Displayable currentForm) 
	{
		super(ResourceBundle.getString("change_pass"));
		_PreviousForm = currentForm;
		append(_CurrentPass);
		append(_NewPass);
		append(_NewPassAgain);
		addCommand(_CmdChange);
		addCommand(_CmdCancel);
		setCommandListener(this);
	}

	public static void showEditForm(String[] userInfo, Displayable previousForm)
	{
		EditInfo.userInfo = userInfo;
		editInfoForm = new EditInfo(previousForm);
		editInfoForm._SexItem.setSelectedIndex(Util.stringToGender(userInfo[JimmUI.UI_GENDER]), true);
		editInfoForm._NickNameItem.setString(userInfo[JimmUI.UI_NICK]);
		editInfoForm._EmailItem.setString(userInfo[JimmUI.UI_EMAIL]);
		editInfoForm._BdayItem.setString(userInfo[JimmUI.UI_BDAY]);
		editInfoForm._FirstNameItem.setString(userInfo[JimmUI.UI_FIRST_NAME]);
		editInfoForm._LastNameItem.setString(userInfo[JimmUI.UI_LAST_NAME]);
		editInfoForm._HomePageItem.setString(userInfo[JimmUI.UI_HOME_PAGE]);
		editInfoForm._AboutItem.setString(userInfo[JimmUI.UI_ABOUT]);
		editInfoForm._CityItem.setString(userInfo[JimmUI.UI_CITY]);

		(new Thread(editInfoForm)).start();

		Jimm.display.setCurrent(editInfoForm);
		//#sijapp cond.if target is "MOTOROLA"#
		LightControl.flash(true);
		//#sijapp cond.end#
	}

	public void run()
	{
		for (int z = 0; z < 51; z++) 
		{
			EditInfo.intterest1.append(ResourceBundle.getString(RequestInfoAction.getCategoriesName(z)), null);
			EditInfo.intterest2.append(ResourceBundle.getString(RequestInfoAction.getCategoriesName(z)), null);
			EditInfo.intterest3.append(ResourceBundle.getString(RequestInfoAction.getCategoriesName(z)), null);
			EditInfo.intterest4.append(ResourceBundle.getString(RequestInfoAction.getCategoriesName(z)), null);
			try
			{
				Thread.sleep(1);
			}
			catch (InterruptedException e) {}
		}
		EditInfo.editInfoForm.intterest1.setSelectedIndex(RequestInfoAction.getSelectIndex(RequestInfoAction.indexCategories[0]), true);
		EditInfo.editInfoForm.intterest2.setSelectedIndex(RequestInfoAction.getSelectIndex(RequestInfoAction.indexCategories[1]), true);
		EditInfo.editInfoForm.intterest3.setSelectedIndex(RequestInfoAction.getSelectIndex(RequestInfoAction.indexCategories[2]), true);
		EditInfo.editInfoForm.intterest4.setSelectedIndex(RequestInfoAction.getSelectIndex(RequestInfoAction.indexCategories[3]), true);
		EditInfo.editInfoForm.intterestText1.setString((EditInfo.userInfo[JimmUI.UI_INETRESTS_1] != null) ? EditInfo.userInfo[JimmUI.UI_INETRESTS_1].substring(1) : null);
		EditInfo.editInfoForm.intterestText2.setString((EditInfo.userInfo[JimmUI.UI_INETRESTS_2] != null) ? EditInfo.userInfo[JimmUI.UI_INETRESTS_2].substring(1) : null);
		EditInfo.editInfoForm.intterestText3.setString((EditInfo.userInfo[JimmUI.UI_INETRESTS_3] != null) ? EditInfo.userInfo[JimmUI.UI_INETRESTS_3].substring(1) : null);
		EditInfo.editInfoForm.intterestText4.setString((EditInfo.userInfo[JimmUI.UI_INETRESTS_4] != null) ? EditInfo.userInfo[JimmUI.UI_INETRESTS_4].substring(1) : null);

		append(_InterestsItem);
		append(intterest1);
		append(intterestText1);
		append(intterest2);
		append(intterestText2);
		append(intterest3);
		append(intterestText3);
		append(intterest4);
		append(intterestText4);
	}

	public static void showChangePassForm(Displayable previousForm) 
	{
		EditInfo changePassForm = new EditInfo(true, previousForm);
		changePassForm._CurrentPass.setString(null);
		changePassForm._NewPass.setString(null);
		changePassForm._NewPassAgain.setString(null);
		Jimm.display.setCurrent(changePassForm);
	}

	public void commandAction(Command c, Displayable d) 
	{
		if (c == _CmdCancel)
		{
			Jimm.display.setCurrent(_PreviousForm);
		}

		if (c == _CmdSave)
		{
			userInfo[JimmUI.UI_NICK]       = _NickNameItem.getString();
			userInfo[JimmUI.UI_EMAIL]      = _EmailItem.getString();
			userInfo[JimmUI.UI_BDAY]       = _BdayItem.getString();
			userInfo[JimmUI.UI_FIRST_NAME] = _FirstNameItem.getString();
			userInfo[JimmUI.UI_LAST_NAME]  = _LastNameItem.getString();
			userInfo[JimmUI.UI_HOME_PAGE]  = _HomePageItem.getString();
			userInfo[JimmUI.UI_ABOUT]      = _AboutItem.getString();
			userInfo[JimmUI.UI_CITY]       = _CityItem.getString();
			userInfo[JimmUI.UI_GENDER]     = Util.genderToString(_SexItem.getSelectedIndex());

			userInfo[JimmUI.UI_INETRESTS_1] = intterestText1.getString();
			userInfo[JimmUI.UI_INETRESTS_2] = intterestText2.getString();
			userInfo[JimmUI.UI_INETRESTS_3] = intterestText3.getString();
			userInfo[JimmUI.UI_INETRESTS_4] = intterestText4.getString();

			RequestInfoAction.indexCategories[0] = RequestInfoAction.getCategoriesCode(intterest1.getSelectedIndex());
			RequestInfoAction.indexCategories[1] = RequestInfoAction.getCategoriesCode(intterest2.getSelectedIndex());
			RequestInfoAction.indexCategories[2] = RequestInfoAction.getCategoriesCode(intterest3.getSelectedIndex());
			RequestInfoAction.indexCategories[3] = RequestInfoAction.getCategoriesCode(intterest4.getSelectedIndex());

			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			Util.writeWord(stream, ToIcqSrvPacket.CLI_SET_FULLINFO, false);

			Util.writeAsciizTLV(SaveInfoAction.FIRSTNAME_TLV_ID, stream, userInfo[JimmUI.UI_FIRST_NAME], false);
		
			SaveInfoAction action = new SaveInfoAction(userInfo);
			try
			{
				Icq.requestAction(action);
			}
			catch (JimmException e)
			{
				JimmException.handleException(e);
				if (e.isCritical()) return;
			}

			Icq.myNick = (_NickNameItem.getString().length() > 0) ? _NickNameItem.getString() : ResourceBundle.getString("me");
			SplashCanvas.addTimerTask("saveinfo", action, false);
		}

		if (c == _CmdChange)
		{
			String currPass = _CurrentPass.getString();
			String newPass = _NewPass.getString();
			String newPassAgain = _NewPassAgain.getString();
			int length = newPass.length();
			byte[] passRaw = Util.stringToByteArray(newPass);
			byte[] buf = new byte[length + 4 + 1];
			Util.putWord(buf, 0, 0x2e04);
			Util.putWord(buf, 2, length, false);
			System.arraycopy(passRaw, 0, buf, 4, passRaw.length);
			Util.putByte(buf, 4 + length, 0x00);
			
			if ((length != 0) && (newPass.equals(newPassAgain)) && (currPass.equals(Options.getString(Options.OPTION_PASSWORD))))
			{
				ToIcqSrvPacket reply3 = new ToIcqSrvPacket(SnacPacket.CLI_TOICQSRV_COMMAND, 0x00000000, Options.getString(Options.OPTION_UIN), 0x07d0, new byte[0], buf);
				try
				{
					Icq.c.sendPacket(reply3);
				}
				catch (JimmException e)
				{
					JimmException.handleException(e);
					if (e.isCritical()) return;
				}
				int i = Options.getInt(Options.OPTIONS_CURR_ACCOUNT);
				Options.setString(Options.accountKeys[2 * i + 1], newPass);
				Jimm.display.setCurrent(_PreviousForm);
			}
			else
			{
				Alert wrong_entry = new Alert("", ResourceBundle.getString("wrong_pass_entry"), null, AlertType.ERROR);
				wrong_entry.setTimeout(15000);
				Jimm.display.setCurrent(wrong_entry, Jimm.display.getCurrent());
			}
		}
	}
}