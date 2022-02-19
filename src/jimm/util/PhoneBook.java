/********************************************************************************
 File: src/jimm/main/PhoneBook.java
 Version: ###VERSION###  Date: ###DATE###
 Author(s): Arvin.  ICQ305773210
 *******************************************************************************/
//#sijapp cond.if target is "MIDP2" | target is "SIEMENS2"#
package jimm.util;

import javax.microedition.lcdui.*;
import javax.wireless.messaging.*;
import javax.microedition.io.*;
import jimm.*;

public class PhoneBook implements CommandListener
{
	private TextBox SmsTextBox;
	private TextBox inputNumber;
	private static Command cmdBack = new Command(ResourceBundle.getString("back"),      Command.BACK, 1);
	private static Command cmdSms  = new Command(ResourceBundle.getString("send_sms"),  Command.ITEM, 3);
	private static Command cmdCall = new Command(ResourceBundle.getString("make_call"), Command.ITEM, 2);
	private static Command cmdSend = new Command(ResourceBundle.getString("send"),      Command.ITEM, 1);
	private static PhoneBook instance;

	public static void activate()
	{
		if (instance == null)
		{
			instance = new PhoneBook();
		}
		instance.ShowInputNumberForm();
	}

	private  void ShowInputNumberForm()
	{
		inputNumber = new TextBox(ResourceBundle.getString("phone_input"), "", 30, TextField. PHONENUMBER);
		inputNumber.addCommand(cmdSms);
		inputNumber.addCommand(cmdCall);
		inputNumber.addCommand(cmdBack);
		inputNumber.setCommandListener(this);
		Jimm.display.setCurrent(inputNumber);
	}

	private void ShowInputSmsForm()
	{
		String number = inputNumber.getString();
		SmsTextBox = new TextBox("SMS "+ number, "", 500, TextField.ANY);
		SmsTextBox.addCommand(cmdSend);
		SmsTextBox.addCommand(cmdBack);
		SmsTextBox.setCommandListener(this);
		Jimm.display.setCurrent(SmsTextBox);
	}

	public void commandAction(Command c, Displayable d)
	{
		if (c == cmdCall)
		{
			call();
		}
		else if (c == cmdSms)
		{
			if (inputNumber.getString().length() > 0)
			{
				ShowInputSmsForm();
			}
		}
		else if (c == cmdSend)
		{
			sendSms();
		}
		else if (c == cmdBack)
		{
			MainMenu.activate();
		}
	}

	private void call()
	{
		String number = inputNumber.getString();
		if (number.length() > 0)
		{
			try
			{
				Jimm.jimm.platformRequest("tel:" + number);
			}
			catch(Exception exc1) {}
		}
	}

	private void sendSms()
	{
		String smstext = SmsTextBox.getString();
		String number = inputNumber.getString();
		if ((smstext.length() > 0) && (number.length() > 0))
		{
			try
			{
				MessageConnection conn;
				TextMessage mess;
				conn = (MessageConnection)Connector.open("sms://" + number);
				mess = (TextMessage)conn.newMessage(MessageConnection.TEXT_MESSAGE);
				mess.setPayloadText(smstext);
				conn.send(mess);
				conn.close();
				mess = null;
				conn = null;
			}
			catch (Exception e) {}
		}
		MainMenu.activate();
	}
}
//#sijapp cond.end#