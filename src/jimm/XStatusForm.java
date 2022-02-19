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
 File: src/jimm/comm/XStatusForm.java
 Version: ###VERSION###  Date: ###DATE###
 Author: aspro
 *******************************************************************************/

package jimm;

import jimm.comm.*;
import jimm.util.*;
//#sijapp cond.if target is "MOTOROLA"#
import DrawControls.*;
//#sijapp cond.end#
import java.util.*;
import javax.microedition.rms.*;
import javax.microedition.lcdui.*;

public class XStatusForm extends Form implements CommandListener 
{
    private TextField titleTextField = new TextField(ResourceBundle.getString("xtraz_title"), "", 20, TextField.ANY);
    private TextField descTextField  = new TextField(ResourceBundle.getString("xtraz_desc"), "", 1000, TextField.ANY);
    private ChoiceGroup choiceGroup  = new ChoiceGroup(null, Choice.MULTIPLE);

    private Command saveCommand      = new Command(ResourceBundle.getString("save"), Command.SCREEN, 1);
    //#sijapp cond.if target is "MIDP2"#
    private Command backCommand      = new Command(ResourceBundle.getString("back"), Jimm.is_smart_SE() ? Command.CANCEL : Command.BACK, 2);
    //#sijapp cond.else#
    private Command backCommand      = new Command(ResourceBundle.getString("back"), Command.BACK,   2);
    //#sijapp cond.end#

    private static Vector xstatusform = new Vector();
    private static XStatusForm instance;

    private int xstIndex = -1;
    private boolean happyFlag;

    /** Creates a new instance of XtrazForm */
    private XStatusForm() 
    {
        super(ResourceBundle.getString("xtraz_msg"));
        choiceGroup.append(ResourceBundle.getString("xtraz_enable"), null);
        choiceGroup.append(ResourceBundle.getString("happy_balloon"), null);
        append(titleTextField);
        append(descTextField);
        append(choiceGroup);
        addCommand(saveCommand);
        addCommand(backCommand);
        setCommandListener(this);
    }

    private void showXtrazForm(int index) 
    {
        xstIndex = index - 1;
        happyFlag = Options.getBoolean(Options.OPTION_FLAG_HAPPY);
        String TitleAndDesc = getRecordDesc(xstIndex);
        titleTextField.setString(TitleAndDesc.substring(0, TitleAndDesc.indexOf("\t")));
        descTextField.setString(TitleAndDesc.substring(TitleAndDesc.indexOf("\t") + 1));
        choiceGroup.setSelectedIndex(0, Options.getBoolean(Options.OPTION_XTRAZ_ENABLE));
        choiceGroup.setSelectedIndex(1, happyFlag);

		Jimm.display.setCurrent(this);
		//#sijapp cond.if target is "MOTOROLA"#
		LightControl.flash(true);
		//#sijapp cond.end#
    }

    public static void activate(int index) 
    {
        if (instance == null) 
        {
            instance = new XStatusForm();
        }
        instance.showXtrazForm(index);
    }
    
    public void commandAction(Command command, Displayable displayable) 
    {
        if (command == saveCommand) 
        {
            Options.setString(Options.OPTION_XTRAZ_TITLE, titleTextField.getString());
            Options.setString(Options.OPTION_XTRAZ_MESSAGE, descTextField.getString());
            Options.setBoolean(Options.OPTION_XTRAZ_ENABLE, choiceGroup.isSelected(0));
            Options.setBoolean(Options.OPTION_FLAG_HAPPY, choiceGroup.isSelected(1));
            Options.setInt(Options.OPTION_XSTATUS, xstIndex);
            Options.safe_save();
            
            String xStatus = titleTextField.getString() + "\t" + descTextField.getString();
            if (!getRecordDesc(xstIndex).equals(xStatus))
            {
                xstatusform.setElementAt(xStatus, xstIndex);
                save();
            }
            
            MainMenu.build();

			if (Icq.isConnected())
			{
				ContactList.activate();
                try 
                {
                    OtherAction.setStandartUserInfo();

                    if (happyFlag != Options.getBoolean(Options.OPTION_FLAG_HAPPY))
						OtherAction.setStatus(Icq.setWebAware() | (int)Options.getLong(Options.OPTION_ONLINE_STATUS));
                } 
                catch (JimmException e) 
                {
                    JimmException.handleException(e);
                }
			}
			else
			{
				MainMenu.activate();
			}
        } 
        else 
        {
            MainMenu.activate();
        }
    }

    private void load()
    {
        RecordStore rms = null;
        xstatusform.removeAllElements();
        try
        {
            rms = RecordStore.openRecordStore("xtraz", true); 
            if (rms.getNumRecords() <= 0)
            {
                for (int i = 0; i <= XStatus.getXStatusCount(); i++)
                {
                    String str = XStatus.getStatusAsString(i) + "\t" + "";
                    xstatusform.addElement(str);
                }
            }
            else
            {
                byte[] data = rms.getRecord(1);
                LoadLineInTable(data);
            }
        }
        catch (Exception e) {}
        try 
        {
            rms.closeRecordStore();
        }
        catch (Exception e) {}
    }
    
    private void save()
    {
        try
        {
            RecordStore.deleteRecordStore("xtraz");
        } 
        catch (Exception e) {}  
        RecordStore rms = null;
        try
        {
            rms = RecordStore.openRecordStore("xtraz", true);
            byte[] buffer = Util.stringToByteArray(saveInLine(), true);
            rms.addRecord(buffer, 0, buffer.length);
        }
        catch (Exception e) {}
        try
        {
            rms.closeRecordStore();
        }
        catch (Exception e) {}
    }

    private String getRecordDesc(int num)
    {
        load();
        return (String)xstatusform.elementAt(num);
    }

    private String saveInLine()
    {
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < XStatus.getXStatusCount(); i++)
        {
            result.append((String)xstatusform.elementAt(i)).append("\t\r");
        }
        return result.toString();
    }

    private void LoadLineInTable(byte[] data)
    {
        String str = Util.byteArrayToString(data, 0, data.length, true);
        int l = 0; //начало
        int l1 = 0; //конец
        do
        {
            l1 = str.indexOf("\t\r", l);
            String str1 = str.substring(l, l1);
            xstatusform.addElement(str1);
            l = l1 + 2;
        }
        while (str.indexOf("\t\r", l) != -1);
    }
}