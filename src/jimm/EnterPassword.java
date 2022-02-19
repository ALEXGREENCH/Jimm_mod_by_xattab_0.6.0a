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
 File: src/jimm/comm/EnterPassword.java
 Version: ###VERSION###  Date: ###DATE###
 Author: Denis K.
 *******************************************************************************/

package jimm;

import jimm.comm.*;
import jimm.util.*;
//#sijapp cond.if target is "MOTOROLA"#
import DrawControls.*;
//#sijapp cond.end#

import java.util.*;
import javax.microedition.lcdui.*;
import javax.microedition.midlet.MIDletStateChangeException;

public class EnterPassword extends Form implements CommandListener 
{
    /** Creates a new instance of EnterPassword */
    private EnterPassword(Displayable previousForm) 
    {
        super(null);
        _PreviousForm = previousForm;
        passwordTextField.setTitle(ResourceBundle.getString("enter_password"));
        passwordTextField.addCommand(okCommand);
        passwordTextField.addCommand(cancelCommand);
        passwordTextField.setCommandListener(this);
    }
    
    private TextBox passwordTextField = new TextBox("", "", 20, TextField.PASSWORD);
    private Command okCommand           = new Command(ResourceBundle.getString("ok"), Command.OK, 1);
    //#sijapp cond.if target is "MIDP2"#
    private Command cancelCommand       = new Command(ResourceBundle.getString("cancel"), Jimm.is_smart_SE() ? Command.CANCEL : Command.BACK, 2);
    //#sijapp cond.else#
    private Command cancelCommand       = new Command(ResourceBundle.getString("cancel"), Command.BACK, 2);
    //#sijapp cond.end#
    private Displayable _PreviousForm;

    private void showPasswordForm()
    {
        if ((Options.getString(Options.OPTION_ENTER_PASSWORD)).length() > 0 && Jimm.isPasswordProtected)
        {
			passwordTextField.setString("");
			Jimm.display.setCurrent(passwordTextField);
			//#sijapp cond.if target is "MOTOROLA"#
			LightControl.flash(true); // включение постоянной подстветки...
			//#sijapp cond.end#
		}
		else
		{
            if (Icq.isNotConnected())
            {
                autoConnect();
            }
            else
            {
                ContactList.activate();
            }
		}
    }

    private static EnterPassword instance;
    
    public static void activate(Displayable previousForm) 
    {
        if (instance == null) 
        {
            instance = new EnterPassword(previousForm);
        }
        instance.showPasswordForm();
    }
    
    private static void autoConnect()
    {
        if (Options.getBoolean(Options.OPTION_AUTO_CONNECT))
        {
            Icq.reconnect_attempts = Options.getInt(Options.OPTION_RECONNECT_NUMBER);
            ContactList.beforeConnect();
            Icq.connect();
        }
        else
        {
            MainMenu.activate();
        }
    }


    public void commandAction(Command command, Displayable displayable) 
    {
        if (command == okCommand) 
        {
            if (Options.getString(Options.OPTION_ENTER_PASSWORD).equals(passwordTextField.getString()))
            {
                if (Icq.isNotConnected())
                {
                    autoConnect();
                }
                else
                {
                    SplashCanvas.unlock(true);
                    SplashCanvas.poundPressTime = 0;

                }
       			Jimm.isPasswordProtected = false;
            }
        }
        else 
        {
            if (Icq.isNotConnected())
            {
                try
                {
                    Jimm.jimm.destroyApp(true);
                }
                catch (MIDletStateChangeException e) {}
            }
            else
            {
                Jimm.display.setCurrent(_PreviousForm);
            }
        }
    }
}