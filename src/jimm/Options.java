
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
*******************************************************************************
File: src/jimm/Options.java
Version: ###VERSION###  Date: ###DATE###
Author(s): Manuel Linsmayer, Andreas Rossbacher, Artyomov Denis, Igor Palkin
******************************************************************************/


/*******************************************************************************
Current record store format:

Record #1: VERSION               (UTF8)
Record #2: OPTION KEY            (BYTE)
           OPTION VALUE          (Type depends on key)
           OPTION KEY            (BYTE)
           OPTION VALUE          (Type depends on key)
           OPTION KEY            (BYTE)
           OPTION VALUE          (Type depends on key)
           ...

Option key            Option value
  0 -  63 (00XXXXXX)  UTF8
 64 - 127 (01XXXXXX)  INTEGER
128 - 191 (10XXXXXX)  BOOLEAN
192 - 224 (110XXXXX)  LONG
225 - 255 (111XXXXX)  SHORT, BYTE-ARRAY (scrambled String)
******************************************************************************/

package jimm;

import java.io.*;
import java.util.*;
import DrawControls.*;

import javax.microedition.lcdui.*;
import javax.microedition.rms.*;

import jimm.*;
import jimm.comm.*;
import jimm.util.*;

public class Options
{
	/* Option keys */
	static final int OPTION_UIN1                              =   0; /* String  */
	static final int OPTION_PASSWORD1                         = 228; /* String  */
//	static final int OPTION_MY_NICK1                          =  28; /* String  */
	static final int OPTION_UIN2                              =  14; /* String  */
	static final int OPTION_PASSWORD2                         = 229; /* String  */
//	static final int OPTION_MY_NICK2                          =  29; /* String  */
	static final int OPTION_UIN3                              =  15; /* String  */
	static final int OPTION_PASSWORD3                         = 230; /* String  */
//	static final int OPTION_MY_NICK3                          =  30; /* String  */
	static final int OPTIONS_CURR_ACCOUNT                     =  86; /* int     */
	//static final int OPTION_SHOW_PASSWORD                     = 163; /* boolean */
	// Theese two options are not stored in RMS 
	public static final int OPTION_UIN                       = 254; /* String  */
	public static final int OPTION_PASSWORD                  = 255; /* String  */
//	public static final int OPTION_MY_NICK                   =  27; /* String  */
	public static final int OPTION_SRV_HOST                  =   1; /* String  */
	public static final int OPTION_SRV_PORT                  =   2; /* String  */
	public static final int OPTION_KEEP_CONN_ALIVE           = 128; /* boolean */
	public static final int OPTION_CONN_ALIVE_INVTERV        =  13; /* String  */
	public static final int OPTION_CONN_PROP                 =  64; /* int     */
	public static final int OPTION_CONN_TYPE                 =  83; /* int     */
	public static final int OPTION_AUTO_CONNECT              = 138; /* boolean */
	//#sijapp cond.if target isnot  "MOTOROLA"#
	public static final int OPTION_SHADOW_CON                = 139; /* boolean */
	//#sijapp cond.end#
	public static final int OPTION_RECONNECT                 = 149; /* boolean */
	public static final int OPTION_RECONNECT_NUMBER          =  91; /* int     */
	public static final int OPTION_HTTP_USER_AGENT           =  17; /* String  */
	public static final int OPTION_HTTP_WAP_PROFILE          =  18; /* String  */
	public static final int OPTION_UI_LANGUAGE               =   3; /* String  */
	public static final int OPTION_TRANS_CURSOR              = 129; /* boolean */
	public static final int OPTION_CL_SORT_BY                =  65; /* int     */
//	//#sijapp cond.if target is "MIDP2" | target is "SIEMENS2"#
//	public static final int OPTION_CL_SMALL_FONT             = 154; /* boolean */
//	//#sijapp cond.end#
	//#sijapp cond.if target is "SIEMENS2"#
	public static final int OPTION_BACKLIGHT                 = 158; /* boolean */
	//#sijapp cond.end#
	public static final int OPTION_CL_HIDE_OFFLINE           = 130; /* boolean */
	public static final int OPTION_CL_HIDE_OFFLINE_ALL       = 134; /* boolean */
	public static final int OPTION_SHOW_TIME                 = 173; /* boolean */
	public static final int OPTION_MESS_NOTIF_TYPE           = 146; /* boolean */
	//#sijapp cond.if target isnot  "DEFAULT"#
	public static final int OPTION_MESS_NOTIF_MODE           =  66; /* int     */
	public static final int OPTION_MESS_NOTIF_FILE           =   4; /* String  */
	public static final int OPTION_ONLINE_NOTIF_MODE         =  68; /* int     */
	public static final int OPTION_ONLINE_NOTIF_FILE         =   5; /* String  */
	public static final int OPTION_OFFLINE_NOTIF_MODE        =  99; /* int     */
	public static final int OPTION_OFFLINE_NOTIF_FILE        =  41; /* String  */
	public static final int OPTION_TYPING_NOTIF_MODE         =  88; /* integer */
	public static final int OPTION_TYPING_NOTIF_FILE         =  16; /* String  */
	public static final int OPTION_NOTIF_VOL                 =  67; /* int     */
	public static final int OPTION_VIBRATOR                  =  75; /* integer */
//	public static final int OPTION_ONLINE_NOTIF_VOL          =  69; /* int     */
	//#sijapp cond.end #
	public static final int OPTION_CLIENT_ID                 =  94; /* int     */
	public static final int OPTION_ONLINE_BLINK_ICON         = 156; /* boolean */
	public static final int OPTION_ONLINE_BLINK_NICK         = 157; /* boolean */
	public static final int OPTION_ONLINE_BLINK_TIME         =  93; /* int     */
	public static final int OPTION_OFFLINE_BLINK_NICK        = 184; /* boolean */
	public static final int OPTION_OFFLINE_BLINK_TIME        =  89; /* int     */
	public static final int OPTION_CP1251_HACK               = 133; /* boolean */
	//#sijapp cond.if modules_TRAFFIC is "true" #
	public static final int OPTION_COST_PER_PACKET           =  70; /* int     */
	public static final int OPTION_COST_PER_DAY              =  71; /* int     */
	public static final int OPTION_COST_PACKET_LENGTH        =  72; /* int     */
	public static final int OPTION_CURRENCY                  =   6; /* String  */
	//#sijapp cond.end #
	public static final int OPTION_ONLINE_STATUS             = 192; /* long    */
	public static final int OPTION_XSTATUS                   =  92; /* int     */
	public static final int OPTION_CHAT_SMALL_FONT           = 135; /* boolean */
	public static final int OPTION_USER_GROUPS               = 136; /* boolean */
	public static final int OPTION_HISTORY                   = 137; /* boolean */
	public static final int OPTION_SHOW_LAST_MESS            = 142; /* boolean */
	public static final int OPTION_CHAT_IMAGE                = 164; /* boolean */
	public static final int OPTION_DELIVERY_REPORT           = 170; /* boolean */
	public static final int OPTION_CHAT_SECONDS              = 165; /* boolean */
	public static final int OPTION_CLEAR_HEAP                = 155; /* boolean */
	public static final int OPTION_CACHE_CONTACTS            = 169; /* boolean */
	public static final int OPTION_AUTO_ANSWER               = 166; /* boolean */
	public static final int OPTION_AUTO_XTRAZ                = 167; /* boolean */
	public static final int OPTION_SWAP_SOFT_KEY             = 143; /* boolean */
	//#sijapp cond.if target is "MIDP2" | target is "MOTOROLA"#
	public static final int OPTION_SWAP_SEND_AND_BACK        = 174; /* boolean */
	//#sijapp cond.end#
	public static final int OPTION_STATUS_MESSAGE_AWAY       =   7; /* String  */
	public static final int OPTION_STATUS_MESSAGE_DND        =  19; /* String  */
	public static final int OPTION_STATUS_MESSAGE_NA         =  20; /* String  */
	public static final int OPTION_STATUS_MESSAGE_OCCUPIED   =  21; /* String  */
	public static final int OPTION_STATUS_MESSAGE_EVIL       =  22; /* String  */
	public static final int OPTION_STATUS_MESSAGE_DEPRESSION =  23; /* String  */
	public static final int OPTION_STATUS_MESSAGE_HOME       =  24; /* String  */
	public static final int OPTION_STATUS_MESSAGE_WORK       =  25; /* String  */
	public static final int OPTION_STATUS_MESSAGE_LUNCH      =  26; /* String  */
	public static final int OPTION_STATUS_RESTORE            = 147; /* boolean */
	public static final int OPTION_STATUS_AUTO               = 153; /* boolean */
	public static final int OPTION_XTRAZ_TITLE               =  32; /* String  */
	public static final int OPTION_XTRAZ_MESSAGE             =  33; /* String  */
	public static final int OPTION_XTRAZ_ENABLE              = 159; /* boolean */
	public static final int OPTION_WEB_AWARE                 = 160; /* boolean */
	public static final int OPTION_MY_AUTH                   = 183; /* boolean */
	public static final int OPTION_FLAG_HAPPY                = 171; /* boolean */
	public static final int OPTION_MAGIC_EYE                 = 172; /* boolean */
	public static final int OPTION_STRING_VERSION            =  31; /* String  */
	public static final int OPTION_PROT_VERSION              =  98; /* int     */
	public static final int OPTION_ENTER_PASSWORD            =  38; /* String  */
	public static final int OPTION_BACK_IMAGE                = 161; /* boolean */
	public static final int OPTION_IMG_PATH                  =  34; /* String  */
	public static final int OPTION_ANTISPAM_MSG              =  35; /* String  */
	public static final int OPTION_ANTISPAM_HELLO            =  36; /* String  */
	public static final int OPTION_ANTISPAM_ANSWER           =  37; /* String  */
	public static final int OPTION_ANTISPAM_ENABLE           = 162; /* boolean */
	//#sijapp cond.if target is "MOTOROLA"#
	public static final int OPTION_LIGHT_TIMEOUT             =  74; /* int     */
	public static final int OPTION_LIGHT_MANUAL              = 140; /* boolean */
	//#sijapp cond.end#
	public static final int OPTION_USE_SMILES                = 141; /* boolean */
	public static final int OPTION_WORD_WRAP                 = 132; /* boolean */
	public static final int OPTION_TEXT_ABC                  = 131; /* boolean */
	public static final int OPTION_MD5_LOGIN                 = 144; /* boolean */
	//#sijapp cond.if modules_PROXY is "true" #
	public static final int OPTION_PRX_TYPE                  =  76; /* int     */
	public static final int OPTION_PRX_SERV                  =   8; /* String  */
	public static final int OPTION_PRX_PORT                  =   9; /* String  */
	public static final int OPTION_AUTORETRY_COUNT           =  10; /* String  */
	public static final int OPTION_PRX_NAME                  =  11; /* String  */
	public static final int OPTION_PRX_PASS                  =  12; /* String  */
	//#sijapp cond.end#
	public static final int OPTIONS_GMT_OFFSET               =  87; /* int     */
	public static final int OPTIONS_LOCAL_OFFSET             =  90; /* int     */
	public static final int OPTION_FULL_SCREEN               = 145; /* boolean */
	public static final int OPTION_SILENT_MODE               = 150; /* boolean */
	public static final int OPTION_BRING_UP                  = 151; /* boolean */
	public static final int OPTION_CREEPING_LINE             = 152; /* boolean */
	public static final int OPTION_SOUND_VIBRA               = 168; /* boolean */
	public static final int OPTION_POPUP_WIN                 =  84; /* int     */
	protected static final int OPTIONS_LANG_CHANGED          = 148; /* boolean */
	public static final int OPTION_SHOW_XST_ICON             = 176; /* boolean */
	public static final int OPTION_SHOW_PRST_ICON            = 177; /* boolean */
	public static final int OPTION_SHOW_SND_ICON             = 178; /* boolean */	
	public static final int OPTION_SHOW_FREE_HEAP            = 179; /* boolean */	
	public static final int OPTION_SHOW_HAPPY_ICON           = 180; /* boolean */	
//	public static final int OPTION_CLIENT_CAPS               = 181; /* boolean */
	public static final int OPTION_ASK_FOR_WEB_FT            = 182; /* boolean */
	//Hotkeys
	public static final int OPTION_EXT_CLKEY0                =  77; /* int     */
	//#sijapp cond.if target is "MIDP2"#
	public static final int OPTION_LEFT_OFFSET               =  95; /* int     */
	public static final int OPTION_RIGHT_OFFSET              =  96; /* int     */
	//#sijapp cond.end#
	public static final int OPTION_FT_MODE                   =  97; /* int     */
	public static final int OPTION_EXT_CLKEY4                =  79; /* int     */
	public static final int OPTION_EXT_CLKEY6                =  80; /* int     */
//	public static final int OPTION_EXT_CLKEY7                =  89; /* int     */
//	public static final int OPTION_EXT_CLKEY8                = 100; /* int     */
//	public static final int OPTION_EXT_CLKEY9                = 101; /* int     */
	public static final int OPTION_EXT_CLKEYSTAR             =  78; /* int     */
	public static final int OPTION_EXT_CLKEYCALL             =  81; /* int     */
	public static final int OPTION_EXT_CLKEYPOUND            =  82; /* int     */

	//Colors
//	public static final int OPTION_COLOR_SCHEME              =  73; /* int     */
	public static final int OPTION_COLOR_BACK                = 102; /* int     */
	public static final int OPTION_COLOR_TEXT                = 103; /* int     */
	public static final int OPTION_COLOR_BLUE                = 104; /* int     */
	public static final int OPTION_COLOR_TEMP                = 105; /* int     */
	public static final int OPTION_COLOR_CAP                 = 106; /* int     */
	public static final int OPTION_COLOR_BLINK               = 107; /* int     */
	public static final int OPTION_COLOR_NICK                = 108; /* int     */
	public static final int OPTION_STATUS_DELAY              = 109; /* int     */
	public static final int OPTION_PRIVATE_STATUS            = 110; /* int     */
	public static final int OPTION_CL_FONT_SIZE              = 111; /* int     */
	public static final int OPTION_CL_FONT_STYLE             = 112; /* int     */
	public static final int OPTION_COLOR_MY_NICK             = 113; /* int     */
	public static final int OPTION_COLOR_CURSOR              = 114; /* int     */
	public static final int OPTION_COLOR_SBACK               =  85; /* int     */

	//Hotkey Actions
	public static final int HOTKEY_NONE      =  0;
	public static final int HOTKEY_INFO      =  2;
	public static final int HOTKEY_NEWMSG    =  3;
	public static final int HOTKEY_ONOFF     =  4;
	public static final int HOTKEY_OPTIONS   =  5;
	public static final int HOTKEY_MENU      =  6;
	public static final int HOTKEY_LOCK      =  7;
	public static final int HOTKEY_HISTORY   =  8;
	public static final int HOTKEY_MINIMIZE  =  9;
	public static final int HOTKEY_CLI_INFO  = 10;
	public static final int HOTKEY_FULLSCR   = 11;
	public static final int HOTKEY_SOUNDOFF  = 12;
	public static final int HOTKEY_ADJLIGHT  = 13;
	public static final int HOTKEY_MAGIC_EYE = 14;
	public static final int HOTKEY_DEL_CHATS = 15;
	public static final int HOTKEY_XTRAZ_MSG = 16;
	public static final int HOTKEY_CALL_SMS  = 17;

	// OPTION_EXT_DOUBLE_KEYS
	public static final int OPTION_EXT_KEY_ACTIONS           =  40; /* String  */
	// OPTION_EXTENDED_KEYS
	public static final int OPTION_ENABLE_EXT_KEYS           = 175; /* boolean */

	public static final int EXT_KEY_SHIFT = 1024;
	public static final String EXT_KEY_NAMES[] = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "#"};
	public static final int EXT_KEY_CODES[] = {Canvas.KEY_NUM0, Canvas.KEY_NUM1, Canvas.KEY_NUM2, Canvas.KEY_NUM3, Canvas.KEY_NUM4, Canvas.KEY_NUM5, Canvas.KEY_NUM6, Canvas.KEY_NUM7, Canvas.KEY_NUM8, Canvas.KEY_NUM9, Canvas.KEY_STAR, Canvas.KEY_POUND};
	public static final int EXT_KEY_COUNT = EXT_KEY_NAMES.length;

	//#sijapp cond.if modules_DEBUGLOG is "true" #
	private static boolean checkKeys = false;
	//#sijapp cond.end #

	static int accountKeys[] = 
	{
		Options.OPTION_UIN1, Options.OPTION_PASSWORD1,
		Options.OPTION_UIN2, Options.OPTION_PASSWORD2,
		Options.OPTION_UIN3, Options.OPTION_PASSWORD3,
	};
	
	// Filetransfer modes
	public static final int FS_MODE_WEB = 0;
	public static final int FS_MODE_NET = 1;

	/**************************************************************************/
	
	final public static String emptyString = new String();  

	static private Object[] options = new Object[256];

	// Options form
	static public OptionsForm optionsForm;
	
	static public int blinkColor, cursorColor;

	/* Private constructor prevent to create instances of Options class */ 
	public Options()
	{
		// Try to load option values from record store and construct options form
		try
		{
			//#sijapp cond.if modules_DEBUGLOG is "true"#
			checkKeys = true;
			setDefaults();
			checkKeys = false;
			//#sijapp cond.else#
			setDefaults();
			resetLangDependedOpts();
			//#sijapp cond.end #

			load();

			if (getBoolean(OPTIONS_LANG_CHANGED))
			{
				setBoolean(OPTIONS_LANG_CHANGED, false);
				resetLangDependedOpts();
			}
		}
		// Use default values if loading option values from record store failed
		catch (Exception e)
		{
			setDefaults();
			resetLangDependedOpts();
		}

		ResourceBundle.setCurrUiLanguage(getString(Options.OPTION_UI_LANGUAGE));
		
		// Initialize colors
		blinkColor = getInt(OPTION_COLOR_BLINK); // Initialize blinkig color
		cursorColor = getInt(OPTION_COLOR_CURSOR); // Initialize cursor color
	}

	/* Set default values. This is done before loading because older saves may not contain all new values */
	static private void setDefaults()
	{
	    setString (Options.OPTION_UIN1,               emptyString);
		setString (Options.OPTION_PASSWORD1,          emptyString);
		//#sijapp cond.if target is "MIDP2" | target is "MOTOROLA" | target is "SIEMENS2"# ===>
		setString (Options.OPTION_SRV_HOST,           "195.66.114.37");
		//#sijapp cond.else# ===
		//#sijapp cond.if modules_PROXY is "true" #
		setString (Options.OPTION_SRV_HOST,           "195.66.114.37"); //Cannot resolve host IP on MIDP1 devices
		//#sijapp cond.else#
		setString (Options.OPTION_SRV_HOST,           "195.66.114.37");
		//#sijapp cond.end#
		//#sijapp cond.end# <===
		setString (Options.OPTION_SRV_PORT,           "5190");
		setBoolean(Options.OPTION_KEEP_CONN_ALIVE,    true);
		setBoolean(Options.OPTION_RECONNECT,          true);
		setInt    (Options.OPTION_RECONNECT_NUMBER,   3);
		//#sijapp cond.if target is "SIEMENS2"#
		setString (Options.OPTION_CONN_ALIVE_INVTERV, Jimm.is_SGold() ? "120" : "27");
		//#sijapp cond.else#
		setString (Options.OPTION_CONN_ALIVE_INVTERV, "120");
		//#sijapp cond.end#
		setInt    (Options.OPTION_CONN_PROP,          1);
		setInt    (Options.OPTION_CONN_TYPE,          0);
		//#sijapp cond.if target isnot "MOTOROLA"#
		//#sijapp cond.if target is "MIDP2"#
		setBoolean(Options.OPTION_SHADOW_CON,         Jimm.is_phone_NOKIA());
		//#sijapp cond.else#
		setBoolean(Options.OPTION_SHADOW_CON,         false);
		//#sijapp cond.end#
		//#sijapp cond.end#
		setBoolean(Options.OPTION_MD5_LOGIN,          false);
		setBoolean(Options.OPTION_AUTO_CONNECT,       false);
		setString (Options.OPTION_HTTP_USER_AGENT,    "unknown");
		setString (Options.OPTION_HTTP_WAP_PROFILE,   "unknown");
		setString (Options.OPTION_UI_LANGUAGE,        ResourceBundle.langAvailable[0]);
		setBoolean(Options.OPTION_TRANS_CURSOR,       false);
		setInt    (Options.OPTION_CL_SORT_BY,         2);
		setInt    (Options.OPTION_CL_FONT_SIZE,       1);
		setInt    (Options.OPTION_CL_FONT_STYLE,      0);
		setBoolean(Options.OPTION_CLEAR_HEAP,         true);
		setBoolean(Options.OPTION_CACHE_CONTACTS,     false);
		setBoolean(Options.OPTION_AUTO_ANSWER,        false);
		setBoolean(Options.OPTION_AUTO_XTRAZ,         false);
		setBoolean(Options.OPTION_SWAP_SOFT_KEY,      false);
		//#sijapp cond.if target is "MIDP2" | target is "MOTOROLA"#
		setBoolean(Options.OPTION_SWAP_SEND_AND_BACK, false);
		//#sijapp cond.end#
		setBoolean(Options.OPTION_XTRAZ_ENABLE,       false);
		setBoolean(Options.OPTION_WEB_AWARE,          false);
		setBoolean(Options.OPTION_MY_AUTH,            true);
		setBoolean(Options.OPTION_FLAG_HAPPY,         false);
		setBoolean(Options.OPTION_MAGIC_EYE,          true);
//		setBoolean(Options.OPTION_CLIENT_CAPS,        false);
		setString (Options.OPTION_STRING_VERSION,     "###VERSION###");
		setInt    (Options.OPTION_PROT_VERSION,       9);
		setString (Options.OPTION_ENTER_PASSWORD,     emptyString);
		setBoolean(Options.OPTION_BACK_IMAGE,         false);
		setString (Options.OPTION_IMG_PATH,           "back.png");
		setString (Options.OPTION_ANTISPAM_MSG,       emptyString);
		setString (Options.OPTION_ANTISPAM_HELLO,     emptyString);
		setString (Options.OPTION_ANTISPAM_ANSWER,    emptyString);
		setBoolean(Options.OPTION_ANTISPAM_ENABLE,    false);
		//#sijapp cond.if target is "SIEMENS2"#
		if (Jimm.is_SGold())
			setBoolean(Options.OPTION_BACKLIGHT,      false);
		else
			setBoolean(Options.OPTION_BACKLIGHT,      true);
		//#sijapp cond.end#
		setBoolean(Options.OPTION_CL_HIDE_OFFLINE,    false);
		setBoolean(Options.OPTION_CL_HIDE_OFFLINE_ALL, false);
		setBoolean(Options.OPTION_SHOW_TIME,          true);
		//#sijapp cond.if target is "SIEMENS1"#
		setInt    (Options.OPTION_MESS_NOTIF_MODE,    0);
		setString (Options.OPTION_MESS_NOTIF_FILE,    "message.mmf");
		setInt    (Options.OPTION_ONLINE_NOTIF_MODE,  0);
		setString (Options.OPTION_ONLINE_NOTIF_FILE,  "online.mmf");
		setInt    (Options.OPTION_OFFLINE_NOTIF_MODE, 0);
		setString (Options.OPTION_OFFLINE_NOTIF_FILE, "offline.mmf");
		setString (Options.OPTION_TYPING_NOTIF_FILE,  "typing.mmf");
		setInt    (Options.OPTION_TYPING_NOTIF_MODE,  1);
		setInt    (Options.OPTION_NOTIF_VOL,          100);
		//#sijapp cond.elseif target is "MIDP2" | target is "SIEMENS2"#
		setInt    (Options.OPTION_MESS_NOTIF_MODE,    0);
		setString (Options.OPTION_MESS_NOTIF_FILE,    "message.wav");
		setInt    (Options.OPTION_ONLINE_NOTIF_MODE,  0);
		setString (Options.OPTION_ONLINE_NOTIF_FILE,  "online.wav");
		setInt    (Options.OPTION_OFFLINE_NOTIF_MODE, 0);
		setString (Options.OPTION_OFFLINE_NOTIF_FILE, "offline.wav");
		setString (Options.OPTION_TYPING_NOTIF_FILE,  "typing.wav");
		setInt    (Options.OPTION_TYPING_NOTIF_MODE,  1);
		setInt    (Options.OPTION_NOTIF_VOL,          100);
		//#sijapp cond.elseif target is "MOTOROLA"#
		setInt    (Options.OPTION_MESS_NOTIF_MODE,    0);
		setString (Options.OPTION_MESS_NOTIF_FILE,    "message.mp3");
		setInt    (Options.OPTION_ONLINE_NOTIF_MODE,  0);
		setString (Options.OPTION_ONLINE_NOTIF_FILE,  "online.mp3");
		setInt    (Options.OPTION_OFFLINE_NOTIF_MODE, 0);
		setString (Options.OPTION_OFFLINE_NOTIF_FILE, "offline.mp3");
		setString (Options.OPTION_TYPING_NOTIF_FILE,  "typing.mp3");
		setInt    (Options.OPTION_TYPING_NOTIF_MODE,  1);
		setInt    (Options.OPTION_NOTIF_VOL,          100);
		setInt    (Options.OPTION_LIGHT_TIMEOUT,      5);
		setBoolean(Options.OPTION_LIGHT_MANUAL,       false);
		//#sijapp cond.end#
		setInt    (Options.OPTION_CLIENT_ID,          7);
		setBoolean(Options.OPTION_MESS_NOTIF_TYPE,    false);
		setBoolean(Options.OPTION_ONLINE_BLINK_ICON,  true);
		setBoolean(Options.OPTION_ONLINE_BLINK_NICK,  true);
		setInt    (Options.OPTION_ONLINE_BLINK_TIME,  25);
		setBoolean(Options.OPTION_OFFLINE_BLINK_NICK, false);
		setInt    (Options.OPTION_OFFLINE_BLINK_TIME, 25);
		setBoolean(Options.OPTION_CP1251_HACK,        (ResourceBundle.langAvailable[0].equals("RU") || ResourceBundle.langAvailable[0].equals("CZ")));
		//#sijapp cond.if target isnot "DEFAULT"#
		setInt    (Options.OPTION_VIBRATOR,           1);
		setInt    (OPTION_FT_MODE,                    FS_MODE_WEB);
		setBoolean(OPTION_ASK_FOR_WEB_FT,             true);
		//#sijapp cond.end#
		//#sijapp cond.if modules_TRAFFIC is "true" #
		setInt    (Options.OPTION_COST_PER_PACKET,    0);
		setInt    (Options.OPTION_COST_PER_DAY,       0);
		setInt    (Options.OPTION_COST_PACKET_LENGTH, 1024);
		setString (Options.OPTION_CURRENCY,           "$");
		//#sijapp cond.end #
		setLong   (Options.OPTION_ONLINE_STATUS,      ContactList.STATUS_ONLINE);
		setBoolean(OPTION_STATUS_RESTORE,             false);
		setBoolean(OPTION_STATUS_AUTO,                false);
		setInt    (OPTION_STATUS_DELAY,               15);
		setInt    (Options.OPTION_XSTATUS,            XStatus.XSTATUS_NONE);
		setInt    (Options.OPTION_PRIVATE_STATUS,     (int)OtherAction.PSTATUS_NOT_INVISIBLE);
		//#sijapp cond.if target is "MOTOROLA"#
		setBoolean(Options.OPTION_CHAT_SMALL_FONT,    false);
		//#sijapp cond.else#
		setBoolean(Options.OPTION_CHAT_SMALL_FONT,    true);
		//#sijapp cond.end#
		setBoolean(Options.OPTION_USER_GROUPS,        false);
		setBoolean(Options.OPTION_HISTORY,            false);
		setBoolean(Options.OPTION_USE_SMILES,         true);
		setBoolean(Options.OPTION_WORD_WRAP,          false);
		setBoolean(Options.OPTION_TEXT_ABC,           true);
		setBoolean(Options.OPTION_SHOW_LAST_MESS,     false);
		//#sijapp cond.if modules_PROXY is "true" #
		setInt    (Options.OPTION_PRX_TYPE,           0);
		setString (Options.OPTION_PRX_SERV,           emptyString);
		setString (Options.OPTION_PRX_PORT,           "1080");
		setString (Options.OPTION_AUTORETRY_COUNT,    "1");
		setString (Options.OPTION_PRX_NAME,           emptyString);
		setString (Options.OPTION_PRX_PASS,           emptyString);
		//#sijapp cond.end #
		setInt    (Options.OPTION_EXT_CLKEY0,         HOTKEY_MAGIC_EYE);
		setInt    (Options.OPTION_EXT_CLKEY4,         HOTKEY_CLI_INFO);
		setInt    (Options.OPTION_EXT_CLKEY6,         HOTKEY_INFO);
		setInt    (Options.OPTION_EXT_CLKEYCALL,      HOTKEY_NEWMSG);
		setInt    (Options.OPTION_EXT_CLKEYPOUND,     HOTKEY_LOCK);
		//#sijapp cond.if target is "MIDP2" | target is "SIEMENS2"#
		setInt    (Options.OPTION_EXT_CLKEYSTAR,      HOTKEY_FULLSCR);
		//#sijapp cond.else#
		setInt    (Options.OPTION_EXT_CLKEYSTAR,      0);
		//#sijapp cond.end #
		setInt    (Options.OPTION_COLOR_BACK,         0xcccccc);
		setInt    (Options.OPTION_COLOR_TEXT,         0x000000);
		setInt    (Options.OPTION_COLOR_BLUE,         0x0000ff);
		setInt    (Options.OPTION_COLOR_TEMP,         0x909090);
		setInt    (Options.OPTION_COLOR_CAP,          0x990000);
		setInt    (Options.OPTION_COLOR_BLINK,        0xcc0000);
		setInt    (Options.OPTION_COLOR_NICK,         0xff0000);
		setInt    (Options.OPTION_COLOR_MY_NICK,      0x0000ff);
		setInt    (Options.OPTION_COLOR_CURSOR,       0xb8d8f8);
		setInt    (Options.OPTION_COLOR_SBACK,        0x000000);
		//#sijapp cond.if target isnot "DEFAULT" #
		setBoolean(Options.OPTION_SILENT_MODE,        false);
		//#sijapp cond.end #
		setInt    (Options.OPTION_POPUP_WIN,          0);
		setBoolean(Options.OPTION_CHAT_IMAGE,         true);
		setBoolean(Options.OPTION_DELIVERY_REPORT,    false);
		setBoolean(Options.OPTION_CHAT_SECONDS,       false);
		setString (Options.OPTION_UIN2,               emptyString);
		setString (Options.OPTION_PASSWORD2,          emptyString);
		setString (Options.OPTION_UIN3,               emptyString);     
		setString (Options.OPTION_PASSWORD3,          emptyString);
		setInt    (Options.OPTIONS_CURR_ACCOUNT,      0);
		//setBoolean(Options.OPTION_SHOW_PASSWORD,      false);
		setBoolean(Options.OPTION_FULL_SCREEN,        false);
		//#sijapp cond.if target is "MIDP2" | target is "MOTOROLA" | target is "SIEMENS2"#
		setBoolean(Options.OPTION_BRING_UP,           true);
		//#sijapp cond.end#
		/* Offset (in hours) between GMT time and local zone time GMT_time + GMT_offset = Local_time */
		setInt    (Options.OPTIONS_GMT_OFFSET,        0);
		/* Offset (in hours) between GMT time and phone clock Phone_clock + Local_offset = GMT_time */
		setInt    (Options.OPTIONS_LOCAL_OFFSET,      0);
		setBoolean(OPTIONS_LANG_CHANGED,              false);
		//#sijapp cond.if target="MIDP2"#
		setBoolean(OPTION_CREEPING_LINE,              false);
		setInt    (Options.OPTION_LEFT_OFFSET,        Jimm.is_phone_NOKIA() ? 20 : 0);
		setInt    (Options.OPTION_RIGHT_OFFSET,       Jimm.is_phone_SE() ? 12 : 0);
		//#sijapp cond.else#
		setBoolean(OPTION_CREEPING_LINE,              true);
		//#sijapp cond.end#
		setBoolean(OPTION_SOUND_VIBRA,                false);
		setBoolean(OPTION_SHOW_XST_ICON,              true);
		setBoolean(OPTION_SHOW_PRST_ICON,             false);
		setBoolean(OPTION_SHOW_HAPPY_ICON,            false);
		setBoolean(OPTION_SHOW_SND_ICON,              true);
		setBoolean(OPTION_SHOW_FREE_HEAP,             false);
		//#sijapp cond.if target isnot "DEFAULT" & target isnot "RIM"#
		selectSoundType("online.",  OPTION_ONLINE_NOTIF_FILE);
		selectSoundType("offline.",  OPTION_OFFLINE_NOTIF_FILE);
		selectSoundType("message.", OPTION_MESS_NOTIF_FILE);
		selectSoundType("typing.",  OPTION_TYPING_NOTIF_FILE);
		//#sijapp cond.end#
		setBoolean(OPTION_ENABLE_EXT_KEYS,            false);
		setString (Options.OPTION_EXT_KEY_ACTIONS,    emptyString);
	}
	
	static public void resetLangDependedOpts()
	{
		setString (Options.OPTION_STATUS_MESSAGE_AWAY,           ResourceBundle.getString("status_message_text_away"));
		setString (Options.OPTION_STATUS_MESSAGE_DND,            ResourceBundle.getString("status_message_text_dnd"));
		setString (Options.OPTION_STATUS_MESSAGE_NA,             ResourceBundle.getString("status_message_text_na"));
		setString (Options.OPTION_STATUS_MESSAGE_OCCUPIED,       ResourceBundle.getString("status_message_text_occ"));
		setString (Options.OPTION_STATUS_MESSAGE_EVIL,           ResourceBundle.getString("status_message_text_evil"));
		setString (Options.OPTION_STATUS_MESSAGE_DEPRESSION,     ResourceBundle.getString("status_message_text_depress"));
		setString (Options.OPTION_STATUS_MESSAGE_HOME,           ResourceBundle.getString("status_message_text_home"));
		setString (Options.OPTION_STATUS_MESSAGE_WORK,           ResourceBundle.getString("status_message_text_work"));
		setString (Options.OPTION_STATUS_MESSAGE_LUNCH,          ResourceBundle.getString("status_message_text_lunch"));
	}

	/* Load option values from record store */
	static public void load() throws IOException, RecordStoreException
	{
		/* Open record store */
		RecordStore account = RecordStore.openRecordStore("options", false);
		
		/* Temporary variables */
		byte[] buf;
		ByteArrayInputStream bais;
		DataInputStream dis;
		
		/* Get version info from record store */
		buf = account.getRecord(1);
		bais = new ByteArrayInputStream(buf);
		dis = new DataInputStream(bais);
		Options.setDefaults();
		
		/* Read all option key-value pairs */
		buf = account.getRecord(2);
		bais = new ByteArrayInputStream(buf);
		dis = new DataInputStream(bais);
		while (dis.available() > 0)
		{
		    int optionKey = dis.readUnsignedByte();
		    if (optionKey < 64)   /* 0-63 = String */
		    {
		        setString(optionKey, dis.readUTF());
			}
			else if (optionKey < 128)   /* 64-127 = int */
			{
			    setInt(optionKey, dis.readInt());
			}
			else if (optionKey < 192)   /* 128-191 = boolean */
			{
			    setBoolean(optionKey, dis.readBoolean());
			}
			else if (optionKey < 224)   /* 192-223 = long */
			{
			    setLong(optionKey, dis.readLong());
			}
			else   /* 226-255 = Scrambled String */
			{
			    byte[] optionValue = new byte[dis.readUnsignedShort()];
			    dis.readFully(optionValue);
			    optionValue = Util.decipherPassword(optionValue);
			    setString(optionKey, Util.byteArrayToString(optionValue, 0, optionValue.length, true));
			}
		}
		/* Close record store */
		account.closeRecordStore();
	}

	/* Save option values to record store */
	static public void save() throws IOException, RecordStoreException
	{
		/* Open record store */
		RecordStore account = RecordStore.openRecordStore("options", true);

		/* Add empty records if necessary */
		while (account.getNumRecords() < 3)
		{
			account.addRecord(null, 0, 0);
		}

		/* Temporary variables */
		byte[] buf;
		ByteArrayOutputStream baos;
		DataOutputStream dos;

		/* Add version info to record store */
		baos = new ByteArrayOutputStream();
		dos = new DataOutputStream(baos);
		dos.writeUTF(Jimm.VERSION);
		buf = baos.toByteArray();
		account.setRecord(1, buf, 0, buf.length);

		/* Save all option key-value pairs */
		baos = new ByteArrayOutputStream();
		dos = new DataOutputStream(baos);

		for (int key = 0; key < options.length; key++)
		{
			if (options[key] == null)
			{
				continue;
			}
			dos.writeByte(key);
			if (key < 64) /* 0-63 = String */
			{
				dos.writeUTF((String)options[key]);
			}
			else if (key < 128) /* 64-127 = int */
			{
				dos.writeInt(((Integer)options[key]).intValue());
			}
			else if (key < 192) /* 128-191 = boolean */
			{
				dos.writeBoolean(((Boolean)options[key]).booleanValue());
			}
			else if (key < 224) /* 192-223 = long */
			{
				dos.writeLong(((Long)options[key]).longValue());
			}
			else if (key < 256) /* 226-255 = Scrambled String */
			{
				byte[] optionValue = Util.stringToByteArray((String)options[key], true);
				optionValue = Util.decipherPassword(optionValue);
				dos.writeShort(optionValue.length);
				dos.write(optionValue);
			}
		}

		buf = baos.toByteArray();
		account.setRecord(2, buf, 0, buf.length);

		/* Close record store */
		account.closeRecordStore();
	}

	static public void safe_save()
	{
		try
		{
			save();
		}
		catch (Exception e)
		{
			JimmException.handleException(new JimmException(172, 0, true));
		}
	}

	/* Option retrieval methods (no type checking!) */
	static public synchronized String getString(int key)
	{
		switch (key)
		{
		case OPTION_UIN:
		case OPTION_PASSWORD:
			int index = getInt(Options.OPTIONS_CURR_ACCOUNT) * 2;
			return getString(accountKeys[key == OPTION_UIN ? index : index + 1]);
		}
		return (String)options[key];
	}

	static public synchronized int getInt(int key)
	{
		return ((Integer)options[key]).intValue();
	}

	static public synchronized boolean getBoolean(int key)
	{
		return ((Boolean)options[key]).booleanValue();
	}

	static public synchronized long getLong(int key)
	{
		return ((Long)options[key]).longValue();
	}

	/* Option setting methods (no type checking!) */
	static public synchronized void setString(int key, String value)
	{
	    options[key] = new String(value);
	}
	static public synchronized void setInt(int key, int value)
	{
	    options[key] = new Integer(value);
	}

	static public synchronized void setBoolean(int key, boolean value)
	{
	    options[key] = new Boolean(value);
	}

	static public synchronized void setLong(int key, long value)
	{
		options[key] = new Long(value);
	}

	/**************************************************************************/
	
	/* Constants for connection type */
	public static final int  CONN_TYPE_SOCKET  = 0;
	public static final int  CONN_TYPE_HTTP    = 1; 
	public static final int  CONN_TYPE_PROXY   = 2; 

	static public void editOptions()
	{
		// Construct option form
		optionsForm = new OptionsForm();
		optionsForm.activate();
	}

	static public void setCaptchaImage(Image img)
	{
		img = Util.createThumbnail(img, (SplashCanvas.getAreaWidth() - 6), 0);
		optionsForm.addCaptchaToForm(img);
		img = null;
	}

	static public void submitNewUinPassword(String uin, String password)
	{
		optionsForm.addAccount(uin, password);
	}

	//#sijapp cond.if target isnot "DEFAULT" & target isnot "RIM"#
	private static void selectSoundType(String name, int option)
	{
		boolean ok;
		
		/* Test existsing option */
		ok = ContactList.testSoundFile( getString(option) );
		if (ok) return;

		/* Test other extensions */
		String[] exts = Util.explode("wav|mp3", '|');
		for (int i = 0; i < exts.length; i++)
		{
			String testFile = name+exts[i];
			ok = ContactList.testSoundFile(testFile);
			if (ok)
			{
				setString(option, testFile);
				break;
			}
		}
	}
	//#sijapp cond.end#	
}

/**************************************************************************/
/**************************************************************************/


/* Form for editing option values */

class OptionsForm implements CommandListener, ItemStateListener
{
	private boolean lastGroupsUsed, lastHideOffline;
	private int lastSortMethod;
	private int currentHour;
	private String lastUILang;

	/* Commands */
	private Command saveCommand;
	private Command enableExtKeyCommand;
	private Command disableExtKeyCommand;

	/* Options menu */
	private TextList optionsMenu;

	/* Options form */
	private Form optionsForm;

    // Static constants for menu actios
    private static final int OPTIONS_ACCOUNT      =  0;
    private static final int OPTIONS_NETWORK      =  1;
    //#sijapp cond.if modules_PROXY is "true"#       
    private static final int OPTIONS_PROXY        =  2;
    //#sijapp cond.end#
    private static final int OPTIONS_INTERFACE    =  3;
    private static final int OPTIONS_COLOR_SCHEME =  4;
    private static final int OPTIONS_HOTKEYS      =  5;
    private static final int OPTIONS_SIGNALING    =  6;
    //#sijapp cond.if modules_TRAFFIC is "true"#
    private static final int OPTIONS_TRAFFIC      =  7;
    //#sijapp cond.end#
    private static final int OPTIONS_TIMEZONE     =  8;
    private static final int OPTIONS_MISC         =  9;
    private static final int OPTIONS_ANTISPAM     = 10;
    private static final int OPTIONS_CLIENT_ID    = 11;
    private static final int OPTIONS_ICQ_SETTINGS = 12;

    // Options
    private TextField[] uinTextField;
    private TextField[] passwordTextField;
    //private ChoiceGroup accountOtherGroup;
    private TextField srvHostTextField;
    private TextField srvPortTextField;
    private TextField httpUserAgendTextField;
    private TextField httpWAPProfileTextField;
    private ChoiceGroup keepConnAliveChoiceGroup;
    private TextField connAliveIntervTextField;
    private ChoiceGroup connPropChoiceGroup;
    private ChoiceGroup connTypeChoiceGroup;
    private TextField reconnectNumberTextField;
    private ChoiceGroup uiLanguageChoiceGroup;
    private ChoiceGroup choiceInterfaceMisc;
    //#sijapp cond.if target is "SIEMENS2"#
    private ChoiceGroup backLightChoiceGroup;
    //#sijapp cond.end#
    private ChoiceGroup clSortByChoiceGroup;
    private ChoiceGroup captionChoiceGroup;
    private ChoiceGroup clFontSizeChoiceGroup;
    private ChoiceGroup clFontStyleChoiceGroup;
    private ChoiceGroup chrgChat;
    private ChoiceGroup autoStatusChoiceGroup;
    private TextField autoStatusDelayTimeTextField;
    private ChoiceGroup chrgPopupWin;
    private ChoiceGroup vibratorChoiceGroup;
    private ChoiceGroup chsBringUp;
    private ChoiceGroup choiceCurAccount;
    private ChoiceGroup chsFSMode;
    private ChoiceGroup chsTimeZone;
    private ChoiceGroup chsCurrTime;
  
    //#sijapp cond.if target isnot "DEFAULT"#
    private ChoiceGroup messageNotificationModeChoiceGroup;
    private ChoiceGroup onlineNotificationModeChoiceGroup;
    private ChoiceGroup offlineNotificationModeChoiceGroup;
    private ChoiceGroup typingNotificationModeChoiceGroup;
    //#sijapp cond.if target isnot "RIM"#
    private Gauge notificationSoundVolume;
    private TextField messageNotificationSoundfileTextField;
    private TextField onlineNotificationSoundfileTextField;
    private TextField offlineNotificationSoundfileTextField;
    private TextField typingNotificationSoundfileTextField;
    private ChoiceGroup disableOutgoingNotification;
    private ChoiceGroup miscChoiceGroup;
    private ChoiceGroup icqChoiceGroup;
    private ChoiceGroup blinkOnlineChoiceGroup;
    private TextField blinkOnlineTimeTextField;
    private ChoiceGroup blinkOfflineChoiceGroup;
    private TextField blinkOfflineTimeTextField;
    private TextField stringVersionTextField;
    private TextField stringProtTextField;
    private TextField enterPasswordTextField;
    private TextField leftOffsetTextField;
    private TextField rightOffsetTextField;
    private ChoiceGroup bgImgChoiceGroup;
    private TextField bgImgPathTextField;
    //#sijapp cond.end#
    //#sijapp cond.end#

    private TextField antispamMsgTextField;
    private TextField antispamAnswerTextField;
    private TextField antispamHelloTextField;
    private ChoiceGroup antispamEnableChoiceGroup;
    
    //#sijapp cond.if modules_TRAFFIC is "true" #
    private TextField costPerPacketTextField;
    private TextField costPerDayTextField;
    private TextField costPacketLengthTextField;
    private TextField currencyTextField;
    //#sijapp cond.end#
    private ChoiceGroup choiceContactList;
    private ChoiceGroup colorScheme;

    //#sijapp cond.if target is "MOTOROLA"#
    private TextField lightTimeout;
    private ChoiceGroup lightManual;
    //#sijapp cond.end#       
    //#sijapp cond.if modules_PROXY is "true"#
    private ChoiceGroup srvProxyType;
    private TextField srvProxyHostTextField;
    private TextField srvProxyPortTextField;
    private TextField srvProxyLoginTextField;
    private TextField srvProxyPassTextField;
    private TextField connAutoRetryTextField;
    //#sijapp cond.end#

	private TextList keysMenu, actionMenu, colorsMenu, clientIdMenu;

	final private String[] hotkeyActionNames = Util.explode
	(
		"ext_hotkey_action_none"
		+ "|" + "info"
		+ "|" + "send_message"
		//#sijapp cond.if modules_HISTORY is "true"#
		+ "|" + "history"
		//#sijapp cond.end#
		+ "|" + "ext_hotkey_action_onoff"
		+ "|" + "options_lng"
		+ "|" + "menu"
		+ "|" + "keylock"
		//#sijapp cond.if target is "MIDP2" | target is "SIEMENS2"#
		+ "|" + "minimize"
		//#sijapp cond.end#
		+ "|" + "dc_info"
		//#sijapp cond.if target is "MIDP2" | target is "MOTOROLA" | target is "SIEMENS2"#
		+ "|" + "full_screen"
		//#sijapp cond.end#
		//#sijapp cond.if target isnot "DEFAULT" #
		+ "|" + "#sound_off"
		//#sijapp cond.end#
		//#sijapp cond.if target is "SIEMENS2"#
		+ "|" + "light"
		//#sijapp cond.end#
		+ "|" + "magic_eye"
		+ "|" + "delete_chats"
		//#sijapp cond.if target is "MIDP2"#
		+ "|" + "calls"
		//#sijapp cond.end#
		+ "|" + "xtraz_msg"
		, '|'
	);

	final private int [] hotkeyActions = 
	{
		Options.HOTKEY_NONE,
		Options.HOTKEY_INFO,
		Options.HOTKEY_NEWMSG,
		//#sijapp cond.if modules_HISTORY is "true"#
		Options.HOTKEY_HISTORY,
		//#sijapp cond.end#
		Options.HOTKEY_ONOFF,
		Options.HOTKEY_OPTIONS,
		Options.HOTKEY_MENU,
		Options.HOTKEY_LOCK,
		//#sijapp cond.if target is "MIDP2" | target is "SIEMENS2"#
		Options.HOTKEY_MINIMIZE,
		//#sijapp cond.end#
		Options.HOTKEY_CLI_INFO,
		//#sijapp cond.if target is "MIDP2" | target is "MOTOROLA" | target is "SIEMENS2"#
		Options.HOTKEY_FULLSCR,
		//#sijapp cond.end#
		//#sijapp cond.if target isnot "DEFAULT" #
		Options.HOTKEY_SOUNDOFF,
		//#sijapp cond.end#
		//#sijapp cond.if target is "SIEMENS2"#
		Options.HOTKEY_ADJLIGHT,
		//#sijapp cond.end#
		Options.HOTKEY_MAGIC_EYE,
		Options.HOTKEY_DEL_CHATS,
		//#sijapp cond.if target is "MIDP2"#
		Options.HOTKEY_CALL_SMS,
		//#sijapp cond.end#
		Options.HOTKEY_XTRAZ_MSG
	};

	// Constructor
	public OptionsForm() throws NullPointerException
	{
		// Initialize hotkeys
		keysMenu = new TextList(ResourceBundle.getString("ext_listhotkeys"));
		JimmUI.setColorScheme(keysMenu, false);
		keysMenu.setCommandListener(this);
		actionMenu = new TextList(ResourceBundle.getString("ext_actionhotkeys"));
		JimmUI.setColorScheme(actionMenu, false);
		actionMenu.setCommandListener(this);

		/*************************************************************************/
		// Initialize commands
		//#sijapp cond.if target is "MIDP2"#
		saveCommand = new Command(ResourceBundle.getString("save"), Jimm.is_phone_FLY() ? Command.BACK : Command.SCREEN, 1);
		//#sijapp cond.else#
		saveCommand = new Command(ResourceBundle.getString("save"), Command.SCREEN, 1);
		//#sijapp cond.end#
		enableExtKeyCommand = new Command(ResourceBundle.getString("enable_ext_keys"), Command.ITEM, 2);
		disableExtKeyCommand = new Command(ResourceBundle.getString("disable_ext_keys"), Command.ITEM, 2);

        optionsMenu = new TextList(ResourceBundle.getString("options_lng"));
		JimmUI.setColorScheme(optionsMenu, false);

		colorsMenu = new TextList(ResourceBundle.getString("color_scheme"));
		clientIdMenu = new TextList(ResourceBundle.getString("client_id"));

		optionsMenu.addCommandEx(JimmUI.cmdBack, VirtualList.MENU_RIGHT_BAR);
		optionsMenu.addCommandEx(JimmUI.cmdSelect, VirtualList.MENU_LEFT_BAR);
        optionsMenu.setCommandListener(this);            

		// Initialize options form
		optionsForm = new Form(ResourceBundle.getString("options_lng"));
		optionsForm.addCommand(saveCommand);
		optionsForm.addCommand(JimmUI.cmdBack);
		optionsForm.setCommandListener(this);
		optionsForm.setItemStateListener(this);
	}

	/* calls ColorScheme Menu */
	public void callColorSchemeOptions()
	{
		optionsMenu.selectTextByIndex(OPTIONS_MISC);
		commandAction(JimmUI.cmdSelect, null); // ��� ��� ����������... ���� commandAction(JimmUI.cmdSelect, optionsMenu)
	}

	// Initialize the kist for the Options menu
	public void initOptionsList()
	{
		optionsMenu.clear();
		optionsMenu.setMode(TextList.MODE_TEXT);
		optionsMenu.setCyclingCursor(true);

		if (Icq.isNotConnected())
			JimmUI.addTextListItem(optionsMenu, "options_account", ContactList.menuIcons.elementAt(12), OPTIONS_ACCOUNT, true);
		JimmUI.addTextListItem(optionsMenu, "options_network", ContactList.menuIcons.elementAt(13), OPTIONS_NETWORK, true);
		//#sijapp cond.if modules_PROXY is "true"#
		if (Options.getInt(Options.OPTION_CONN_TYPE) == Options.CONN_TYPE_PROXY)
			JimmUI.addTextListItem(optionsMenu, "proxy", ContactList.menuIcons.elementAt(21), OPTIONS_PROXY, true); 
		//#sijapp cond.end#
		JimmUI.addTextListItem(optionsMenu, "options_interface", ContactList.menuIcons.elementAt(14), OPTIONS_INTERFACE, true);
		JimmUI.addTextListItem(optionsMenu, "color_scheme", ContactList.menuIcons.elementAt(32), OPTIONS_COLOR_SCHEME, true);
		JimmUI.addTextListItem(optionsMenu, "options_hotkeys", ContactList.menuIcons.elementAt(22), OPTIONS_HOTKEYS, true);
		JimmUI.addTextListItem(optionsMenu, "options_icq", ContactList.imageList.elementAt(7), OPTIONS_ICQ_SETTINGS, true);
		JimmUI.addTextListItem(optionsMenu, "options_signaling", ContactList.menuIcons.elementAt(15), OPTIONS_SIGNALING, true);
		//#sijapp cond.if modules_TRAFFIC is "true"#
		JimmUI.addTextListItem(optionsMenu, "options_cost", ContactList.menuIcons.elementAt(25), OPTIONS_TRAFFIC, true); 
		//#sijapp cond.end#
		JimmUI.addTextListItem(optionsMenu, "time_zone", ContactList.menuIcons.elementAt(26), OPTIONS_TIMEZONE, true); 
		JimmUI.addTextListItem(optionsMenu, "antispam", ContactList.menuIcons.elementAt(0), OPTIONS_ANTISPAM, true); 
		JimmUI.addTextListItem(optionsMenu, "client_id", ContactList.clientIcons.elementAt(Options.getInt(Options.OPTION_CLIENT_ID)), OPTIONS_CLIENT_ID, true); 
		JimmUI.addTextListItem(optionsMenu, "misc", ContactList.menuIcons.elementAt(10), OPTIONS_MISC, true); 
	}

	private String getHotKeyActName(String langStr, int option)
	{
		return getHotKeyActName(langStr, option, true);
	}

	private String getHotKeyActName(String str, int option, boolean translate)
	{
		String keyName = (translate ? ResourceBundle.getString(str) : str);
		int optionValue = translate ? Options.getInt(option) : option;
		for (int i = 0; i < hotkeyActionNames.length; i++)
		{
			if (hotkeyActions[i] == optionValue) 
				return keyName + ": " + ResourceBundle.getString(hotkeyActionNames[i]);
		}
		return keyName + ": <???>";
	}

	private void InitHotkeyMenuUI()
	{
		int lastItemIndex = keysMenu.getCurrTextIndex();
		boolean enableExtendedKeys = Options.getBoolean(Options.OPTION_ENABLE_EXT_KEYS);
		keysMenu.clear();

		if (!enableExtendedKeys)
		{
			JimmUI.addTextListItem(keysMenu, getHotKeyActName("ext_clhotkey0", Options.OPTION_EXT_CLKEY0), null, Options.OPTION_EXT_CLKEY0, true);
		}
		JimmUI.addTextListItem(keysMenu, getHotKeyActName("ext_clhotkey4", Options.OPTION_EXT_CLKEY4), null, Options.OPTION_EXT_CLKEY4, true);
		JimmUI.addTextListItem(keysMenu, getHotKeyActName("ext_clhotkey6", Options.OPTION_EXT_CLKEY6), null, Options.OPTION_EXT_CLKEY6, true);
		//#sijapp cond.if target isnot "MOTOROLA"#
		JimmUI.addTextListItem(keysMenu, getHotKeyActName("ext_clhotkeystar", Options.OPTION_EXT_CLKEYSTAR), null, Options.OPTION_EXT_CLKEYSTAR, true);
		//#sijapp cond.end#
		JimmUI.addTextListItem(keysMenu, getHotKeyActName("ext_clhotkeypound", Options.OPTION_EXT_CLKEYPOUND), null, Options.OPTION_EXT_CLKEYPOUND, true);
		//#sijapp cond.if target is "MIDP2" | target is "SIEMENS2"#
		JimmUI.addTextListItem(keysMenu, getHotKeyActName("ext_clhotkeycall", Options.OPTION_EXT_CLKEYCALL), null, Options.OPTION_EXT_CLKEYCALL, true);
		//#sijapp cond.end#

		if (enableExtendedKeys)
		{
			String extActions = Options.getString(Options.OPTION_EXT_KEY_ACTIONS);
			if (extActions.length() < Options.EXT_KEY_COUNT)
			{
				extActions = "";
				for (int i = 0; i < Options.EXT_KEY_COUNT; ++i)
				{
					extActions += (char)Options.HOTKEY_NONE;
				}
				Options.setString(Options.OPTION_EXT_KEY_ACTIONS, extActions);
			}

			for (int i = 0; i < Options.EXT_KEY_COUNT; ++i)
			{
				int action = (int)extActions.charAt(i);
				JimmUI.addTextListItem(keysMenu, getHotKeyActName("0+" + Options.EXT_KEY_NAMES[i], action, false), null, Options.EXT_KEY_SHIFT + i, true);
			}
		}

		keysMenu.selectTextByIndex(lastItemIndex);
		keysMenu.setCyclingCursor(true);

		keysMenu.addCommandEx(JimmUI.cmdMenu, VirtualList.MENU_RIGHT_BAR);
		keysMenu.addCommandEx(saveCommand, VirtualList.MENU_RIGHT);
		if (enableExtendedKeys)
		{
			keysMenu.removeCommandEx(enableExtKeyCommand);
			keysMenu.addCommandEx(disableExtKeyCommand, VirtualList.MENU_RIGHT);
		}
		else
		{
			keysMenu.removeCommandEx(disableExtKeyCommand);
			keysMenu.addCommandEx(enableExtKeyCommand, VirtualList.MENU_RIGHT);
		}
		keysMenu.addCommandEx(JimmUI.cmdSelect, VirtualList.MENU_LEFT_BAR);
		keysMenu.activate(Jimm.display);
	}

	public void InitColorMenuUI()
	{
		int lastIndex = colorsMenu.getCurrTextIndex();
		colorsMenu.clear();
		JimmUI.setColorScheme(colorsMenu, false);
		colorsMenu.setMode(TextList.MODE_TEXT);
		colorsMenu.setCyclingCursor(true);
		Icon image = ContactList.menuIcons.elementAt(32);

		colorsMenu.lock();
		JimmUI.addTextListItem(colorsMenu, "color_cap",     image, Options.OPTION_COLOR_CAP,     true);
		JimmUI.addTextListItem(colorsMenu, "color_back",    image, Options.OPTION_COLOR_BACK,    true);
		JimmUI.addTextListItem(colorsMenu, "color_sback",   image, Options.OPTION_COLOR_SBACK,   true);
		JimmUI.addTextListItem(colorsMenu, "color_cursor",  image, Options.OPTION_COLOR_CURSOR,  true);
		JimmUI.addTextListItem(colorsMenu, "color_text",    image, Options.OPTION_COLOR_TEXT,    true);
		JimmUI.addTextListItem(colorsMenu, "color_blue",    image, Options.OPTION_COLOR_BLUE,    true);
		JimmUI.addTextListItem(colorsMenu, "color_temp",    image, Options.OPTION_COLOR_TEMP,    true);
		JimmUI.addTextListItem(colorsMenu, "color_nick",    image, Options.OPTION_COLOR_NICK,    true);
		JimmUI.addTextListItem(colorsMenu, "color_my_nick", image, Options.OPTION_COLOR_MY_NICK, true);
		JimmUI.addTextListItem(colorsMenu, "blink_color",   image, Options.OPTION_COLOR_BLINK,   true);
		colorsMenu.unlock();

		colorsMenu.selectTextByIndex(lastIndex);
		colorsMenu.addCommandEx(saveCommand, VirtualList.MENU_RIGHT_BAR);
		colorsMenu.addCommandEx(JimmUI.cmdSelect, VirtualList.MENU_LEFT_BAR);
		colorsMenu.setCommandListener(this);
		colorsMenu.activate(Jimm.display);
	}

	public void InitClientIdMenuUI()
	{
		clientIdMenu.clear();
		JimmUI.setColorScheme(clientIdMenu, false);
		clientIdMenu.setMode(TextList.MODE_TEXT);
		clientIdMenu.setCyclingCursor(true);

		clientIdMenu.lock();
		JimmUI.addTextListItem(clientIdMenu, "Jimm",              ContactList.clientIcons.elementAt(7),   7, false);
		JimmUI.addTextListItem(clientIdMenu, "Miranda",           ContactList.clientIcons.elementAt(1),   1, false);
		JimmUI.addTextListItem(clientIdMenu, "QIP 2005a",         ContactList.clientIcons.elementAt(0),   0, false);
		JimmUI.addTextListItem(clientIdMenu, "QIP PDA (Symbian)", ContactList.clientIcons.elementAt(12), 12, false);
		JimmUI.addTextListItem(clientIdMenu, "QIP PDA (Windows)", ContactList.clientIcons.elementAt(13), 13, false);
		JimmUI.addTextListItem(clientIdMenu, "QIP Infium",        ContactList.clientIcons.elementAt(14), 14, false);
		JimmUI.addTextListItem(clientIdMenu, "ICQ 5.1",           ContactList.clientIcons.elementAt(18), 18, false);
		JimmUI.addTextListItem(clientIdMenu, "ICQ 6",             ContactList.clientIcons.elementAt(15), 15, false);
		JimmUI.addTextListItem(clientIdMenu, "StICQ",             ContactList.clientIcons.elementAt(8),   8, false);
		JimmUI.addTextListItem(clientIdMenu, "VmICQ",             ContactList.clientIcons.elementAt(11), 11, false);
		JimmUI.addTextListItem(clientIdMenu, "mChat",             ContactList.clientIcons.elementAt(21), 21, false);
		JimmUI.addTextListItem(clientIdMenu, "&RQ",               ContactList.clientIcons.elementAt(2),   2, false);
		JimmUI.addTextListItem(clientIdMenu, "R&Q",               ContactList.clientIcons.elementAt(3),   3, false);
		JimmUI.addTextListItem(clientIdMenu, "Kopete",            ContactList.clientIcons.elementAt(6),   6, false);
		JimmUI.addTextListItem(clientIdMenu, "Mac ICQ",           ContactList.clientIcons.elementAt(22), 22, false);
//		JimmUI.addTextListItem(clientIdMenu, "Unknown",           ContactList.menuIcons.elementAt(10),   15, false);
		clientIdMenu.unlock();

		clientIdMenu.selectTextByIndex(Options.getInt(Options.OPTION_CLIENT_ID));
		clientIdMenu.addCommandEx(JimmUI.cmdBack, VirtualList.MENU_RIGHT_BAR);
		clientIdMenu.addCommandEx(JimmUI.cmdSelect, VirtualList.MENU_LEFT_BAR);
		clientIdMenu.setCommandListener(this);
		clientIdMenu.activate(Jimm.display);
	}

	///////////////////////////////////////////////////////////////////////////
	
	// Accounts
	private Command cmdAddNewAccount = new Command(ResourceBundle.getString("add_new"), Command.ITEM, 3);
	private Command cmdDeleteAccount = new Command(ResourceBundle.getString("delete", ResourceBundle.FLAG_ELLIPSIS), Command.ITEM, 3);
	private Command cmdRegisterAccount = new Command(ResourceBundle.getString("register_new"), Command.ITEM, 3);
	private Command cmdRequestCaptchaImage = new Command(ResourceBundle.getString("register_request_image"), Command.ITEM, 3);
	private Command cmdRequestRegistration = new Command(ResourceBundle.getString("register_request_send"), Command.ITEM, 3);

	private int currAccount;
	private Vector uins = new Vector();
	private Vector passwords = new Vector();
	private int maxAccountsCount = Options.accountKeys.length / 2;

	private TextField captchaCode;
	private TextField newPassword;
	private boolean registration_connected = false;

	private static Selector selector;

	private void readAccontsData()
	{
		uins.removeAllElements();
		passwords.removeAllElements();
		for (int i = 0; i < maxAccountsCount; i++)
		{
			int index = i * 2;
			String uin = Options.getString(Options.accountKeys[index]);
			if ((i != 0) && (uin.length() == 0)) continue;
			uins.addElement(uin);
			passwords.addElement(Options.getString(Options.accountKeys[index + 1]));
		}
		currAccount = Options.getInt(Options.OPTIONS_CURR_ACCOUNT);
	}
	
	private String checkUin(String value)
	{
		if ((value == null) || (value.length() == 0)) return "---";
		return value;
	}

	private void showRegisterControls()
	{
		newPassword = new TextField(ResourceBundle.getString("password"), "", 8, TextField.PASSWORD);
		captchaCode = new TextField(ResourceBundle.getString("captcha"), "", 8, TextField.ANY);
		optionsForm.removeCommand(saveCommand);
		optionsForm.append(newPassword);
		if (!Icq.isConnected())
		{
			registration_connected = false;
			optionsForm.addCommand(cmdRequestCaptchaImage);
		}
	}

	public void addCaptchaToForm(Image img)
	{
		clearForm();
		optionsForm.append(img);
		optionsForm.append(captchaCode);
		optionsForm.append(ResourceBundle.getString("register_notice"));
		optionsForm.addCommand(cmdRequestRegistration);
		registration_connected = true;
	}

	public void addAccount(String uin, String password)
	{
		readAccontsControls();
		if (checkUin((String) uins.elementAt(currAccount)) == "---")
		{
			uins.setElementAt(uin, currAccount);
			passwords.setElementAt(password, currAccount);
		}
		else
		{
			uins.addElement(uin);
			passwords.addElement(password);
		}
		optionsForm.addCommand(saveCommand);
		clearForm();
		showAccountControls();
	}

	private void showAccountControls()
	{
		int size = uins.size();
		
		if (size != 1)
		{
			if (choiceCurAccount == null)
				choiceCurAccount = new ChoiceGroup(ResourceBundle.getString("options_account"), Choice.POPUP);
			//#sijapp cond.if target is "MIDP2" | target is "MOTOROLA" | target is "SIEMENS2"#
			choiceCurAccount.deleteAll();
			//#sijapp cond.else#
			while (choiceCurAccount.size() > 0) { choiceCurAccount.delete(0); }
			//#sijapp cond.end#
			
			for (int i = 0; i < size; i++)
				choiceCurAccount.append(checkUin((String)uins.elementAt(i)), null);
			optionsForm.append(choiceCurAccount);
			if (currAccount >= size) currAccount = size - 1;
			choiceCurAccount.setSelectedIndex(currAccount, true);
		}
		
		uinTextField = new TextField[size];
		passwordTextField = new TextField[size];
		for (int i = 0; i < size; i++)
		{
			if (size > 1) optionsForm.append("---");
			
			String add = (size == 1) ? "" : "-" + (i + 1);
			
			TextField uinFld = new TextField(ResourceBundle.getString("uin")+add, (String)uins.elementAt(i), 12,TextField.NUMERIC);
			// int textFieldType = (Options.getBoolean(Options.OPTION_SHOW_PASSWORD)) ? TextField.ANY : TextField.PASSWORD;
			TextField passFld = new TextField(ResourceBundle.getString("password")+add, (String)passwords.elementAt(i), 32, TextField.PASSWORD);

			optionsForm.append(uinFld);
			optionsForm.append(passFld);

			uinTextField[i] = uinFld;
			passwordTextField[i] = passFld; 
		}
		
		if (size != maxAccountsCount)
		{
			optionsForm.addCommand(cmdAddNewAccount);
			if (!Icq.isConnected())
			{
				optionsForm.addCommand(cmdRegisterAccount);
			}
		}
		if (size != 1)
		{
			optionsForm.addCommand(cmdDeleteAccount);
		}
	}

	private void setAccountOptions()
	{
		int size = uins.size();
		String uin, pass;
		
		for (int i = 0; i < maxAccountsCount; i++)
		{
			if (i < size)
			{
				uin  = (String)uins.elementAt(i);
				pass = (String)passwords.elementAt(i);
			}
			else uin = pass = Options.emptyString;
			
			Options.setString(Options.accountKeys[2 * i], uin);
			Options.setString(Options.accountKeys[2 * i + 1], pass);
		}

		if (currAccount >= size) currAccount = size - 1;
		Options.setInt(Options.OPTIONS_CURR_ACCOUNT, currAccount);
	}
	
	private void readAccontsControls()
	{
		uins.removeAllElements();
		passwords.removeAllElements();
		for (int i = 0; i < uinTextField.length; i++)
		{
			uins.addElement(uinTextField[i].getString());
			passwords.addElement(passwordTextField[i].getString());
		}
		
		currAccount = (choiceCurAccount == null) ? 0 : choiceCurAccount.getSelectedIndex();
	}

	public void itemStateChanged(Item item)
	{
		if (uinTextField != null)
		{
			int accCount = uinTextField.length;
			if (accCount != 1)
			{
				for (int i = 0; i < accCount; i++)
				{
					if (uinTextField[i] != item) continue;
					choiceCurAccount.set(i, checkUin(uinTextField[i].getString()), null);
					return;
				}
			}
		}
		//select file for background
		if (item == bgImgChoiceGroup && bgImgChoiceGroup.isSelected(1))
		{
			Options.setBoolean(Options.OPTION_BACK_IMAGE, true);
			//#sijapp cond.if modules_FILES is "true"#
			FileTransfer.askForWebFileTransfer = false;
			FileTransfer ft = new FileTransfer(FileTransfer.FT_TYPE_FILE_BY_NAME, null);
			ft.startFT();
			//#sijapp cond.end#
		}
	}

	///////////////////////////////////////////////////////////////////////////
	
	/* Activate options menu */
	protected void activate()
	{
		// Store some last values
		lastUILang = Options.getString(Options.OPTION_UI_LANGUAGE);
		lastHideOffline = Options.getBoolean(Options.OPTION_CL_HIDE_OFFLINE);
		lastGroupsUsed = Options.getBoolean(Options.OPTION_USER_GROUPS);
		lastSortMethod = Options.getInt(Options.OPTION_CL_SORT_BY);

		initOptionsList();
		optionsMenu.activate(Jimm.display);
	}

	final private static int TAG_DELETE_ACCOUNT = 1;

	/* Helpers for options UI: */
	static private void addStr(ChoiceGroup chs, String lngStr)
	{
		String[] strings = Util.explode(lngStr, '|');
		for (int i = 0; i < strings.length; i++) chs.append(ResourceBundle.getString(strings[i]), null);
	}
	
	static private ChoiceGroup createSelector(String cap, String items, int optValue)
	{
		ChoiceGroup chs = new ChoiceGroup(ResourceBundle.getString(cap), Choice.EXCLUSIVE);
		addStr(chs, items);
		chs.setSelectedIndex(Options.getInt(optValue), true);
		return chs;
	}
	
	static private void setChecked(ChoiceGroup chs, String lngStr, int optValue)
	{
		addStr(chs, lngStr);
		chs.setSelectedIndex(chs.size() - 1, Options.getBoolean(optValue));
	}

	/* Command listener */
	public void commandAction(Command c, Displayable d)
	{
		boolean needToUpdate = false;

		/* Command handler for hotkeys list in Options */
		if (JimmUI.isControlActive(keysMenu))
		{
			if (c == JimmUI.cmdSelect)
			{
				if (actionMenu.getSize() == 0)
				{
					for (int i=0; i < hotkeyActionNames.length; i++)
					{
						JimmUI.addTextListItem(actionMenu, hotkeyActionNames[i], null, hotkeyActions[i], true);
					}
				}

				actionMenu.addCommandEx(JimmUI.cmdSelect, VirtualList.MENU_LEFT_BAR);
				actionMenu.addCommandEx(JimmUI.cmdBack, VirtualList.MENU_RIGHT_BAR);

				int keyIndex = keysMenu.getCurrTextIndex();
				int optValue = (keyIndex < Options.EXT_KEY_SHIFT)
									? Options.getInt(keyIndex)
									: Options.getString(Options.OPTION_EXT_KEY_ACTIONS).charAt(keyIndex - Options.EXT_KEY_SHIFT);
				actionMenu.selectTextByIndex(optValue);
				actionMenu.setCyclingCursor(true);
				
				actionMenu.activate(Jimm.display);
				return;
			}
		}

		/* Command handler for actions list in Hotkeys */
		if (JimmUI.isControlActive(actionMenu))
		{
			if (c == JimmUI.cmdSelect)
			{ 
				int keyIndex = keysMenu.getCurrTextIndex();
				if (keyIndex < Options.EXT_KEY_SHIFT)
				{
					Options.setInt(keyIndex, actionMenu.getCurrTextIndex());
				}
				else
				{
					StringBuffer keyActions = new StringBuffer(Options.getString(Options.OPTION_EXT_KEY_ACTIONS));
					keyActions.setCharAt(keyIndex - Options.EXT_KEY_SHIFT, (char)actionMenu.getCurrTextIndex());
					Options.setString(Options.OPTION_EXT_KEY_ACTIONS, new String(keyActions));
				}
			}
			InitHotkeyMenuUI();
			return;
		}

		if (JimmUI.isControlActive(colorsMenu))
		{
			if (c == JimmUI.cmdSelect)
			{
				JimmUI.setLastScreen(colorsMenu);
				selector = new Selector(2, Selector.getColorIndex(Options.getInt(colorsMenu.getCurrTextIndex())));
				JimmUI.setColorScheme(selector, false);
				selector.setCyclingCursor(true);
				selector.addCommandEx(JimmUI.cmdSelect, VirtualList.MENU_LEFT_BAR);
				selector.addCommandEx(JimmUI.cmdBack, VirtualList.MENU_RIGHT_BAR);
				selector.setCommandListener(this);
				selector.activate(Jimm.display);
				return;
			}
		}

		if (JimmUI.isControlActive(selector))
		{
			if (c == JimmUI.cmdSelect)
			{
				JimmUI.setLastScreen(selector);
				ColorChooser colorChooser = new ColorChooser(colorsMenu.getCurrTextIndex(), selector.getCurrSelectedIdx());
				Jimm.display.setCurrent(colorChooser);
				return;
			}
			else if (c == JimmUI.cmdBack)
			{
				Options.optionsForm.InitColorMenuUI();
				return;
			}
		}

		if (JimmUI.isControlActive(clientIdMenu))
		{
			if (c == JimmUI.cmdSelect)
			{
				Options.setInt(Options.OPTION_CLIENT_ID, clientIdMenu.getCurrTextIndex());
				/* Save options */
				Options.safe_save();
				activate();
				setStatusAfterChanges();
				return;
			}
		}

		// Look for select command
		if (c == JimmUI.cmdSelect)
		{
			// Delete all items
			clearForm();
			optionsForm.addCommand(saveCommand);

			//#sijapp cond.if target is "MOTOROLA"#
			LightControl.flash(true);
			//#sijapp cond.end#

			// Add elements, depending on selected option menu item
			switch (optionsMenu.getCurrTextIndex())
			{
				case OPTIONS_ACCOUNT:
					readAccontsData();
					showAccountControls();

					//accountOtherGroup = new ChoiceGroup(ResourceBundle.getString("misc"), Choice.MULTIPLE);
					//setChecked(accountOtherGroup, "show_password", Options.OPTION_SHOW_PASSWORD);

					//optionsForm.append(accountOtherGroup);
					break;
					
				case OPTIONS_NETWORK:
					// Initialize elements (network section)
					srvHostTextField = new TextField(ResourceBundle.getString("server_host"), Options.getString(Options.OPTION_SRV_HOST), 255, TextField.ANY);
					srvPortTextField = new TextField(ResourceBundle.getString("server_port"), Options.getString(Options.OPTION_SRV_PORT), 5, TextField.NUMERIC);

					connTypeChoiceGroup = new ChoiceGroup(ResourceBundle.getString("conn_type"), Choice.POPUP);
					addStr(connTypeChoiceGroup, "socket" + "|" + "http");
					//#sijapp cond.if modules_PROXY is "true"#
					addStr(connTypeChoiceGroup, "proxy");
					connTypeChoiceGroup.setSelectedIndex(Options.getInt(Options.OPTION_CONN_TYPE), true);
					//#sijapp cond.else#
					connTypeChoiceGroup.setSelectedIndex(Options.getInt(Options.OPTION_CONN_TYPE) % 2, true);
					//#sijapp cond.end#

					keepConnAliveChoiceGroup = new ChoiceGroup(ResourceBundle.getString("keep_conn_alive"), Choice.MULTIPLE);
					setChecked(keepConnAliveChoiceGroup, "yes", Options.OPTION_KEEP_CONN_ALIVE);

					connAliveIntervTextField = new TextField(ResourceBundle.getString("timeout_interv"), Options.getString(Options.OPTION_CONN_ALIVE_INVTERV), 4, TextField.NUMERIC);

					connPropChoiceGroup = new ChoiceGroup(ResourceBundle.getString("conn_prop"), Choice.MULTIPLE);
					addStr(connPropChoiceGroup, "md5_login" + "|" + "async" + "|" + "auto_connect" + "|" + "reconnect");
					//#sijapp cond.if target isnot "MOTOROLA"#
					addStr(connPropChoiceGroup, "shadow_con");
					//#sijapp cond.end#
					connPropChoiceGroup.setSelectedIndex(0, Options.getBoolean(Options.OPTION_MD5_LOGIN));
					connPropChoiceGroup.setSelectedIndex(1, Options.getInt(Options.OPTION_CONN_PROP) != 0);
					connPropChoiceGroup.setSelectedIndex(2, Options.getBoolean(Options.OPTION_AUTO_CONNECT));
					connPropChoiceGroup.setSelectedIndex(3, Options.getBoolean(Options.OPTION_RECONNECT));
					//#sijapp cond.if target isnot "MOTOROLA"#
					connPropChoiceGroup.setSelectedIndex(4, Options.getBoolean(Options.OPTION_SHADOW_CON));
					//#sijapp cond.end#

					httpUserAgendTextField = new TextField(ResourceBundle.getString("http_user_agent"), Options.getString(Options.OPTION_HTTP_USER_AGENT), 256, TextField.ANY);
					httpWAPProfileTextField = new TextField(ResourceBundle.getString("http_wap_profile"), Options.getString(Options.OPTION_HTTP_WAP_PROFILE), 256, TextField.ANY);
					reconnectNumberTextField = new TextField(ResourceBundle.getString("reconnect_number"), String.valueOf(Options.getInt(Options.OPTION_RECONNECT_NUMBER)), 2, TextField.NUMERIC);

					optionsForm.append(srvHostTextField);
					optionsForm.append(srvPortTextField);
					optionsForm.append(connTypeChoiceGroup);
					optionsForm.append(keepConnAliveChoiceGroup);
					optionsForm.append(connAliveIntervTextField);
					optionsForm.append(connPropChoiceGroup);
					optionsForm.append(reconnectNumberTextField);
					optionsForm.append(httpUserAgendTextField);
					optionsForm.append(httpWAPProfileTextField);
					break;

				//#sijapp cond.if modules_PROXY is "true"#
				case OPTIONS_PROXY:
					srvProxyType = new ChoiceGroup(ResourceBundle.getString("proxy_type"), Choice.POPUP);
					srvProxyType.append(ResourceBundle.getString("proxy_socks4"), null);
					srvProxyType.append(ResourceBundle.getString("proxy_socks5"), null);
					srvProxyType.append(ResourceBundle.getString("proxy_guess"), null);
					srvProxyType.setSelectedIndex(Options.getInt(Options.OPTION_PRX_TYPE), true);

					srvProxyHostTextField = new TextField(ResourceBundle.getString("proxy_server_host"), Options.getString(Options.OPTION_PRX_SERV), 32, TextField.ANY);
					srvProxyPortTextField = new TextField(ResourceBundle.getString("proxy_server_port"), Options.getString(Options.OPTION_PRX_PORT), 5, TextField.NUMERIC);

					srvProxyLoginTextField = new TextField(ResourceBundle.getString("proxy_server_login"), Options.getString(Options.OPTION_PRX_NAME), 32, TextField.ANY);
					srvProxyPassTextField  = new TextField(ResourceBundle.getString("proxy_server_pass"), Options.getString(Options.OPTION_PRX_PASS), 32, TextField.PASSWORD);

					connAutoRetryTextField = new TextField(ResourceBundle.getString("auto_retry_count"), Options.getString(Options.OPTION_AUTORETRY_COUNT), 5, TextField.NUMERIC);

					optionsForm.append(srvProxyType);
					optionsForm.append(srvProxyHostTextField);
					optionsForm.append(srvProxyPortTextField);
					optionsForm.append(srvProxyLoginTextField);
					optionsForm.append(srvProxyPassTextField);
					optionsForm.append(connAutoRetryTextField);
					break;
				//#sijapp cond.end# 

				case OPTIONS_INTERFACE:
					// Initialize elements (interface section)
					if (ResourceBundle.langAvailable.length > 1)
					{
						uiLanguageChoiceGroup = new ChoiceGroup(ResourceBundle.getString("language"), Choice.POPUP);
						for (int j = 0; j < ResourceBundle.langAvailable.length; j++)
						{
							uiLanguageChoiceGroup.append(ResourceBundle.getString("lang_" + ResourceBundle.langAvailable[j]), null);
							if (ResourceBundle.langAvailable[j].equals(Options.getString(Options.OPTION_UI_LANGUAGE)))
							{
								uiLanguageChoiceGroup.setSelectedIndex(j, true);
							}
						}
					}

					//#sijapp cond.if target is "SIEMENS2"#
					backLightChoiceGroup = new ChoiceGroup(ResourceBundle.getString("backlight_opt"), Choice.POPUP);
					backLightChoiceGroup.append(ResourceBundle.getString("backlight_on"), null);
					backLightChoiceGroup.append(ResourceBundle.getString("backlight_off"), null);
					if(Options.getBoolean(Options.OPTION_BACKLIGHT)) backLightChoiceGroup.setSelectedIndex(0, true);
					else backLightChoiceGroup.setSelectedIndex(1, true);
					//#sijapp cond.end#

					choiceInterfaceMisc = new ChoiceGroup(ResourceBundle.getString("misc"), Choice.MULTIPLE);
					setChecked(choiceInterfaceMisc, "trans_cursor", Options.OPTION_TRANS_CURSOR);
					//#sijapp cond.if target is "MIDP2" | target is "MOTOROLA" | target is "SIEMENS2"#
					setChecked(choiceInterfaceMisc, "full_screen", Options.OPTION_FULL_SCREEN);
					//#sijapp cond.end#
					setChecked(choiceInterfaceMisc, "swap_softs", Options.OPTION_SWAP_SOFT_KEY);

					choiceContactList = new ChoiceGroup(ResourceBundle.getString("contact_list"), Choice.MULTIPLE);
					setChecked(choiceContactList, "show_user_groups", Options.OPTION_USER_GROUPS);
					setChecked(choiceContactList, "hide_offline", Options.OPTION_CL_HIDE_OFFLINE);
					setChecked(choiceContactList, "hide_offline_anyway", Options.OPTION_CL_HIDE_OFFLINE_ALL);
					setChecked(choiceContactList, "show_time", Options.OPTION_SHOW_TIME);

					clFontSizeChoiceGroup = new ChoiceGroup(ResourceBundle.getString("font_size"), Choice.POPUP);
					clFontSizeChoiceGroup.append(ResourceBundle.getString("font_medium"), null);
					clFontSizeChoiceGroup.append(ResourceBundle.getString("font_small"), null);
					clFontSizeChoiceGroup.append(ResourceBundle.getString("font_large"), null);
					clFontSizeChoiceGroup.setSelectedIndex(Options.getInt(Options.OPTION_CL_FONT_SIZE), true);

					clFontStyleChoiceGroup = new ChoiceGroup(ResourceBundle.getString("font_style"), Choice.POPUP);
					clFontStyleChoiceGroup.append(ResourceBundle.getString("font_plain"), null);
					clFontStyleChoiceGroup.append(ResourceBundle.getString("font_bold"), null);
//					clFontStyleChoiceGroup.append(ResourceBundle.getString("font_italic"), null);
					clFontStyleChoiceGroup.setSelectedIndex(Options.getInt(Options.OPTION_CL_FONT_STYLE), true);

					clSortByChoiceGroup = new ChoiceGroup(ResourceBundle.getString("sort_by"), Choice.POPUP);
					clSortByChoiceGroup.append(ResourceBundle.getString("sort_by_name"), null);
					clSortByChoiceGroup.append(ResourceBundle.getString("sort_by_status"), null);
					clSortByChoiceGroup.append(ResourceBundle.getString("sort_by_status_and_name"), null);
					clSortByChoiceGroup.setSelectedIndex(Options.getInt(Options.OPTION_CL_SORT_BY), true);

					captionChoiceGroup = new ChoiceGroup(ResourceBundle.getString("show_in_caption"), Choice.MULTIPLE);
					setChecked(captionChoiceGroup, "set_xstatus", Options.OPTION_SHOW_XST_ICON);
					setChecked(captionChoiceGroup, "private_status", Options.OPTION_SHOW_PRST_ICON);
					setChecked(captionChoiceGroup, "happy_balloon", Options.OPTION_SHOW_HAPPY_ICON);
					setChecked(captionChoiceGroup, "show_sound_icon", Options.OPTION_SHOW_SND_ICON);
					setChecked(captionChoiceGroup, "show_free_heap", Options.OPTION_SHOW_FREE_HEAP);
					//#sijapp cond.if target is "MIDP2"#
					leftOffsetTextField = new TextField(ResourceBundle.getString("offset_left"), String.valueOf(Options.getInt(Options.OPTION_LEFT_OFFSET)), 2, TextField.NUMERIC);
					rightOffsetTextField = new TextField(ResourceBundle.getString("offset_right"), String.valueOf(Options.getInt(Options.OPTION_RIGHT_OFFSET)), 2, TextField.NUMERIC);
					//#sijapp cond.end#
					chrgChat = new ChoiceGroup(ResourceBundle.getString("chat"), Choice.MULTIPLE);
					setChecked(chrgChat, "chat_small_font", Options.OPTION_CHAT_SMALL_FONT);

					setChecked(chrgChat, "chat_show_image", Options.OPTION_CHAT_IMAGE);
					setChecked(chrgChat, "chat_show_seconds", Options.OPTION_CHAT_SECONDS);

					//#sijapp cond.if modules_SMILES is "true"#
					setChecked(chrgChat, "use_smiles", Options.OPTION_USE_SMILES);
					//#sijapp cond.end#
					setChecked(chrgChat, "word_wrapping", Options.OPTION_WORD_WRAP);
					setChecked(chrgChat, "text_abc", Options.OPTION_TEXT_ABC);
					//#sijapp cond.if modules_HISTORY is "true"#
					setChecked(chrgChat, "use_history", Options.OPTION_HISTORY);
					setChecked(chrgChat, "show_prev_mess", Options.OPTION_SHOW_LAST_MESS);
					//#sijapp cond.end#
					setChecked(chrgChat, "cp1251", Options.OPTION_CP1251_HACK);
					//#sijapp cond.if target is "MIDP2"#
					if (!Jimm.is_phone_SE() && !Jimm.is_smart_SE())
					{
						setChecked(chrgChat, "swap_send_and_back", Options.OPTION_SWAP_SEND_AND_BACK);
					}
					//#sijapp cond.end#
					//#sijapp cond.if target is "MOTOROLA"#
					setChecked(chrgChat, "swap_send_and_back", Options.OPTION_SWAP_SEND_AND_BACK);
					//#sijapp cond.end#

					//#sijapp cond.if target is "MOTOROLA"#
					lightTimeout = new TextField(ResourceBundle.getString("backlight_timeout"), String.valueOf(Options.getInt(Options.OPTION_LIGHT_TIMEOUT)), 2, TextField.NUMERIC);
					lightManual = new ChoiceGroup(ResourceBundle.getString("backlight_manual"), Choice.MULTIPLE);
					setChecked(lightManual, "yes", Options.OPTION_LIGHT_MANUAL);
					//#sijapp cond.end#

					if (uiLanguageChoiceGroup != null)
					{
						optionsForm.append(uiLanguageChoiceGroup);
					}
					//#sijapp cond.if target is "SIEMENS2"#
					if (!Jimm.is_SGold())
					{
						optionsForm.append(backLightChoiceGroup);
					}
					//#sijapp cond.end#
					optionsForm.append(choiceContactList);
					optionsForm.append(clFontSizeChoiceGroup);
					optionsForm.append(clFontStyleChoiceGroup);
					optionsForm.append(clSortByChoiceGroup);
					optionsForm.append(captionChoiceGroup);
					//#sijapp cond.if target is "MIDP2"#
					if (Jimm.is_phone_NOKIA())
					{
						optionsForm.append(leftOffsetTextField);
					}
					if (Jimm.is_phone_SE())
					{
						optionsForm.append(rightOffsetTextField);
					}
					//#sijapp cond.end#
					optionsForm.append(chrgChat);
					//#sijapp cond.if target is "MOTOROLA"#
					optionsForm.append(lightTimeout);
					optionsForm.append(lightManual);
					//#sijapp cond.end #
					optionsForm.append(choiceInterfaceMisc);
					break;

				case OPTIONS_COLOR_SCHEME:
					InitColorMenuUI();
					return;

				case OPTIONS_HOTKEYS:
					InitHotkeyMenuUI();
					return;

				case OPTIONS_SIGNALING:
					/* Initialize elements (Signaling section) */
					//#sijapp cond.if target isnot "DEFAULT"#
					onlineNotificationModeChoiceGroup = new ChoiceGroup(ResourceBundle.getString("onl_notification"), Choice.POPUP);
					onlineNotificationModeChoiceGroup.append(ResourceBundle.getString("no"), null);
					onlineNotificationModeChoiceGroup.append(ResourceBundle.getString("beep"), null);
					//#sijapp cond.if target isnot "RIM"#
					onlineNotificationModeChoiceGroup.append(ResourceBundle.getString("sound"), null);
					//#sijapp cond.end#
					//#sijapp cond.if target isnot "RIM"#
					onlineNotificationSoundfileTextField = new TextField(null, Options.getString(Options.OPTION_ONLINE_NOTIF_FILE), 32, TextField.ANY);
					//#sijapp cond.end#
					onlineNotificationModeChoiceGroup.setSelectedIndex(Options.getInt(Options.OPTION_ONLINE_NOTIF_MODE), true);

					blinkOnlineChoiceGroup = new ChoiceGroup(null, Choice.MULTIPLE);
					setChecked(blinkOnlineChoiceGroup, "blink_icon", Options.OPTION_ONLINE_BLINK_ICON);
					setChecked(blinkOnlineChoiceGroup, "blink_nick", Options.OPTION_ONLINE_BLINK_NICK);
					blinkOnlineTimeTextField = new TextField(ResourceBundle.getString("blink_time"), String.valueOf(Options.getInt(Options.OPTION_ONLINE_BLINK_TIME)), 3, TextField.NUMERIC);

					//#sijapp cond.if target isnot "DEFAULT"#
					offlineNotificationModeChoiceGroup = new ChoiceGroup(ResourceBundle.getString("offl_notification"), Choice.POPUP);
					offlineNotificationModeChoiceGroup.append(ResourceBundle.getString("no"), null);
					offlineNotificationModeChoiceGroup.append(ResourceBundle.getString("beep"), null);
					//#sijapp cond.if target isnot "RIM"#
					offlineNotificationModeChoiceGroup.append(ResourceBundle.getString("sound"), null);
					//#sijapp cond.end#
					//#sijapp cond.if target isnot "RIM"#
					offlineNotificationSoundfileTextField = new TextField(null, Options.getString(Options.OPTION_OFFLINE_NOTIF_FILE), 32, TextField.ANY);
					//#sijapp cond.end#
					offlineNotificationModeChoiceGroup.setSelectedIndex(Options.getInt(Options.OPTION_OFFLINE_NOTIF_MODE), true);

					blinkOfflineChoiceGroup = new ChoiceGroup(null, Choice.MULTIPLE);
					setChecked(blinkOfflineChoiceGroup, "blink_nick", Options.OPTION_OFFLINE_BLINK_NICK);
					blinkOfflineTimeTextField = new TextField(ResourceBundle.getString("blink_time"), String.valueOf(Options.getInt(Options.OPTION_OFFLINE_BLINK_TIME)), 3, TextField.NUMERIC);

					messageNotificationModeChoiceGroup = new ChoiceGroup(ResourceBundle.getString("message_notification"), Choice.POPUP);
					messageNotificationModeChoiceGroup.append(ResourceBundle.getString("no"), null);
					messageNotificationModeChoiceGroup.append(ResourceBundle.getString("beep"), null);
					//#sijapp cond.if target isnot "RIM"#
					messageNotificationModeChoiceGroup.append(ResourceBundle.getString("sound"), null);
					//#sijapp cond.end#
					messageNotificationModeChoiceGroup.setSelectedIndex(Options.getInt(Options.OPTION_MESS_NOTIF_MODE), true);

					//#sijapp cond.if target isnot "RIM"#
					notificationSoundVolume = new Gauge(ResourceBundle.getString("volume"), true, 10, Options.getInt(Options.OPTION_NOTIF_VOL) / 10);
					messageNotificationSoundfileTextField = new TextField(null, Options.getString(Options.OPTION_MESS_NOTIF_FILE), 32, TextField.ANY);
					typingNotificationSoundfileTextField = new TextField(null, Options.getString(Options.OPTION_TYPING_NOTIF_FILE), 32, TextField.ANY);

					typingNotificationModeChoiceGroup = new ChoiceGroup(ResourceBundle.getString("typing_notify"), Choice.POPUP);
					typingNotificationModeChoiceGroup.append(ResourceBundle.getString("no"), null);
					typingNotificationModeChoiceGroup.append(ResourceBundle.getString("typing_display_only"), null);
					typingNotificationModeChoiceGroup.append(ResourceBundle.getString("beep"), null);
					//#sijapp cond.if target isnot "RIM"#
					typingNotificationModeChoiceGroup.append(ResourceBundle.getString("sound"), null);
					//#sijapp cond.end#
					typingNotificationModeChoiceGroup.setSelectedIndex(Options.getInt(Options.OPTION_TYPING_NOTIF_MODE), true);

					disableOutgoingNotification = new ChoiceGroup(null, Choice.MULTIPLE);
					setChecked(disableOutgoingNotification, "dis_out_notif", Options.OPTION_MESS_NOTIF_TYPE);
					//#sijapp cond.end#

					vibratorChoiceGroup = new ChoiceGroup(ResourceBundle.getString("vibration"), Choice.POPUP);
					vibratorChoiceGroup.append(ResourceBundle.getString("no"), null);
					vibratorChoiceGroup.append(ResourceBundle.getString("yes"), null);
					vibratorChoiceGroup.append(ResourceBundle.getString("when_locked"), null);
					vibratorChoiceGroup.setSelectedIndex(Options.getInt(Options.OPTION_VIBRATOR), true);
					//#sijapp cond.end#

					chrgPopupWin = new ChoiceGroup(ResourceBundle.getString("popup_win"), Choice.POPUP);
					chrgPopupWin.append(ResourceBundle.getString("no"), null);
					chrgPopupWin.append(ResourceBundle.getString("pw_forme"), null);
					chrgPopupWin.append(ResourceBundle.getString("pw_all"), null);
					chrgPopupWin.setSelectedIndex(Options.getInt(Options.OPTION_POPUP_WIN), true);

					//#sijapp cond.if target isnot "DEFAULT"#
					optionsForm.append(notificationSoundVolume);
					optionsForm.append(messageNotificationModeChoiceGroup);
					//#sijapp cond.if target isnot "RIM"#
					optionsForm.append(messageNotificationSoundfileTextField);
					//#sijapp cond.end#
					optionsForm.append(vibratorChoiceGroup);
					optionsForm.append(onlineNotificationModeChoiceGroup);
					//#sijapp cond.if target isnot "RIM"#
					optionsForm.append(onlineNotificationSoundfileTextField);
					optionsForm.append(blinkOnlineChoiceGroup);
					optionsForm.append(blinkOnlineTimeTextField);
					optionsForm.append(offlineNotificationModeChoiceGroup);
					optionsForm.append(offlineNotificationSoundfileTextField);
					optionsForm.append(blinkOfflineChoiceGroup);
					optionsForm.append(blinkOfflineTimeTextField);
					optionsForm.append(typingNotificationModeChoiceGroup);
					optionsForm.append(typingNotificationSoundfileTextField);
					optionsForm.append(disableOutgoingNotification);
					//#sijapp cond.end#
					//#sijapp cond.end#
					optionsForm.append(chrgPopupWin);

					//#sijapp cond.if target is "MIDP2" | target is "MOTOROLA" | target is "SIEMENS2"#
					chsBringUp = new ChoiceGroup(ResourceBundle.getString("misc"), Choice.MULTIPLE);
					setChecked(chsBringUp, "bring_up", Options.OPTION_BRING_UP);
					setChecked(chsBringUp, "creeping_line", Options.OPTION_CREEPING_LINE);
					setChecked(chsBringUp, "sound_vibra", Options.OPTION_SOUND_VIBRA);
					optionsForm.append(chsBringUp);
					//#sijapp cond.end#
					break;

				//#sijapp cond.if modules_TRAFFIC is "true"#
				case OPTIONS_TRAFFIC:
					/* Initialize elements (cost section) */
					costPerPacketTextField = new TextField(ResourceBundle.getString("cpp"), Util.intToDecimal(Options.getInt(Options.OPTION_COST_PER_PACKET)), 6, TextField.ANY);
					costPerDayTextField = new TextField(ResourceBundle.getString("cpd"), Util.intToDecimal(Options.getInt(Options.OPTION_COST_PER_DAY)), 6, TextField.ANY);
					costPacketLengthTextField = new TextField(ResourceBundle.getString("plength"), String.valueOf(Options.getInt(Options.OPTION_COST_PACKET_LENGTH) / 1024), 4, TextField.NUMERIC);
					currencyTextField = new TextField(ResourceBundle.getString("currency"), Options.getString(Options.OPTION_CURRENCY), 4, TextField.ANY);

					optionsForm.append(costPerPacketTextField);
					optionsForm.append(costPerDayTextField);
					optionsForm.append(costPacketLengthTextField);
					optionsForm.append(currencyTextField);
					break;
				//#sijapp cond.end#

				case OPTIONS_TIMEZONE:
				{
					int choiceType;

					//#sijapp cond.if target is "MIDP2" | target is "MOTOROLA" | target is "SIEMENS2"#
					choiceType = Choice.POPUP;
					//#sijapp cond.else#
					choiceType = Choice.EXCLUSIVE;
					//#sijapp cond.end#

					chsTimeZone = new ChoiceGroup(ResourceBundle.getString("time_zone"), choiceType);
					for (int i = -12; i <= 13; i++)
						chsTimeZone.append("GMT" + (i < 0 ? "" : "+") + i + ":00", null);
					chsTimeZone.setSelectedIndex(Options.getInt(Options.OPTIONS_GMT_OFFSET) + 12, true);

					int[] currDateTime = Util.createDate(Util.createCurrentDate(false));
					chsCurrTime = new ChoiceGroup(ResourceBundle.getString("local_time"), choiceType);
					int minutes = currDateTime[Util.TIME_MINUTE];
					int hour = currDateTime[Util.TIME_HOUR];
					for (int i = 0; i < 24; i++) chsCurrTime.append(i+":"+minutes, null);
					chsCurrTime.setSelectedIndex(hour, true);
					Calendar calendar = Calendar.getInstance();
					calendar.setTime( new Date() );
					currentHour = calendar.get(Calendar.HOUR_OF_DAY);

					optionsForm.append(chsTimeZone);
					optionsForm.append(chsCurrTime);
					break;
				}

				case OPTIONS_ANTISPAM:
					antispamMsgTextField    = new TextField(ResourceBundle.getString("antispam_msg"), Options.getString(Options.OPTION_ANTISPAM_MSG), 255, TextField.ANY);
					antispamAnswerTextField = new TextField(ResourceBundle.getString("antispam_answer"), Options.getString(Options.OPTION_ANTISPAM_ANSWER), 255, TextField.ANY);
					antispamHelloTextField  = new TextField(ResourceBundle.getString("antispam_hello"), Options.getString(Options.OPTION_ANTISPAM_HELLO), 255, TextField.ANY);

					antispamEnableChoiceGroup = new ChoiceGroup(null, Choice.MULTIPLE);
					setChecked(antispamEnableChoiceGroup, "antispam_enable", Options.OPTION_ANTISPAM_ENABLE);

					optionsForm.append(antispamMsgTextField);
					optionsForm.append(antispamAnswerTextField);
					optionsForm.append(antispamHelloTextField);
					optionsForm.append(antispamEnableChoiceGroup);
					break;

				case OPTIONS_CLIENT_ID:
					InitClientIdMenuUI();
					return;

				case OPTIONS_MISC:
					miscChoiceGroup = new ChoiceGroup(null, Choice.MULTIPLE);
					setChecked(miscChoiceGroup, "clear_heap",		Options.OPTION_CLEAR_HEAP);
					setChecked(miscChoiceGroup, "magic_eye",		Options.OPTION_MAGIC_EYE);

					//Image selector
					//#sijapp cond.if target is "MIDP2"#
					bgImgChoiceGroup = new ChoiceGroup(ResourceBundle.getString("bg_image"), Jimm.is_phone_SE() ? Choice.EXCLUSIVE : Choice.POPUP);
					//#sijapp cond.else#
					bgImgChoiceGroup = new ChoiceGroup(ResourceBundle.getString("bg_image"), Choice.POPUP);
					//#sijapp cond.end#
					bgImgChoiceGroup.append(ResourceBundle.getString("no"), null);
					bgImgChoiceGroup.append(ResourceBundle.getString("yes"), null);
					if (Options.getBoolean(Options.OPTION_BACK_IMAGE)) bgImgChoiceGroup.setSelectedIndex(1, true);
					else bgImgChoiceGroup.setSelectedIndex(0, true);
					bgImgPathTextField = new TextField(null, Options.getString(Options.OPTION_IMG_PATH), 255, TextField.ANY);

					enterPasswordTextField = new TextField(ResourceBundle.getString("startup_pass"), Options.getString(Options.OPTION_ENTER_PASSWORD), 20, TextField.PASSWORD);

					optionsForm.append(miscChoiceGroup);
					optionsForm.append(bgImgChoiceGroup);
					optionsForm.append(bgImgPathTextField);
					optionsForm.append(enterPasswordTextField);
					break;

				case OPTIONS_ICQ_SETTINGS:
					icqChoiceGroup = new ChoiceGroup(null, Choice.MULTIPLE);
					setChecked(icqChoiceGroup, "delivery_report",	Options.OPTION_DELIVERY_REPORT);
					setChecked(icqChoiceGroup, "cache_contacts",	Options.OPTION_CACHE_CONTACTS);
					setChecked(icqChoiceGroup, "auto_answer",		Options.OPTION_AUTO_ANSWER);
					setChecked(icqChoiceGroup, "auto_xtraz",		Options.OPTION_AUTO_XTRAZ);
//					setChecked(icqChoiceGroup, "client_caps",		Options.OPTION_CLIENT_CAPS);
					if (Icq.setPoint && Icq.isConnected())
					{
						setChecked(icqChoiceGroup, "Web Aware",		Options.OPTION_WEB_AWARE);
						setChecked(icqChoiceGroup, "auth",			Options.OPTION_MY_AUTH);
					}

					stringVersionTextField = new TextField(ResourceBundle.getString("jimm_version"), Options.getString(Options.OPTION_STRING_VERSION), 11, TextField.ANY);
					stringProtTextField = new TextField(ResourceBundle.getString("jimm_prot"), String.valueOf(Options.getInt(Options.OPTION_PROT_VERSION)), 5, TextField.NUMERIC);

					autoStatusChoiceGroup = new ChoiceGroup(ResourceBundle.getString("auto_status"), Choice.MULTIPLE);
					setChecked(autoStatusChoiceGroup, "auto_status_restore", Options.OPTION_STATUS_RESTORE);
					setChecked(autoStatusChoiceGroup, "auto_status_enable", Options.OPTION_STATUS_AUTO);
					autoStatusDelayTimeTextField = new TextField(ResourceBundle.getString("auto_status_delay"), String.valueOf(Options.getInt(Options.OPTION_STATUS_DELAY)), 3, TextField.NUMERIC);

					//#sijapp cond.if modules_FILES is "true"#
					chsFSMode = new ChoiceGroup(ResourceBundle.getString("ft_type"), Choice.POPUP);
					chsFSMode.append(ResourceBundle.getString("ft_type_web"), null);
					chsFSMode.append(ResourceBundle.getString("ft_type_net"), null);
					chsFSMode.setSelectedIndex(Options.getInt(Options.OPTION_FT_MODE), true);
					//#sijapp cond.end#

					optionsForm.append(icqChoiceGroup);
					optionsForm.append(stringVersionTextField);
					optionsForm.append(stringProtTextField);
					optionsForm.append(autoStatusChoiceGroup);
					optionsForm.append(autoStatusDelayTimeTextField);
					//#sijapp cond.if modules_FILES is "true"#
					optionsForm.append(chsFSMode);
					//#sijapp cond.end#
					break;
			}
			/* Activate options form */
			Jimm.display.setCurrent(optionsForm);
		}

		/* Look for back command */
		else if (c == JimmUI.cmdBack)
		{
			if (d == optionsForm || clientIdMenu.isActive())
			{
				/* Active Options List */
				optionsMenu.activate(Jimm.display);
			}
			else
			{
				if (registration_connected)
				{
					 Icq.disconnect();
				}
				Options.optionsForm = null;
				/* Active Main Menu */
				MainMenu.activate();
				return;
			}
		}

		// Look for save command
		else if (c == saveCommand)
		{
			// Save values, depending on selected option menu item
			switch (optionsMenu.getCurrTextIndex())
			{
				case OPTIONS_ACCOUNT:
					readAccontsControls();
					setAccountOptions();
					//Options.setBoolean(Options.OPTION_SHOW_PASSWORD,accountOtherGroup.isSelected(0));
					break;

				case OPTIONS_NETWORK:
					Options.setString(Options.OPTION_SRV_HOST,srvHostTextField.getString());
					Options.setString(Options.OPTION_SRV_PORT,srvPortTextField.getString());
					Options.setInt(Options.OPTION_CONN_TYPE,connTypeChoiceGroup.getSelectedIndex());
					Options.setBoolean(Options.OPTION_KEEP_CONN_ALIVE,keepConnAliveChoiceGroup.isSelected(0));
					Options.setString(Options.OPTION_CONN_ALIVE_INVTERV,connAliveIntervTextField.getString());
					Options.setBoolean(Options.OPTION_MD5_LOGIN,connPropChoiceGroup.isSelected(0));
					if (connPropChoiceGroup.isSelected(1))
						Options.setInt(Options.OPTION_CONN_PROP,1);
					else
						Options.setInt(Options.OPTION_CONN_PROP,0);
					Options.setBoolean(Options.OPTION_AUTO_CONNECT,connPropChoiceGroup.isSelected(2));
					Options.setBoolean(Options.OPTION_RECONNECT,connPropChoiceGroup.isSelected(3));
					//#sijapp cond.if target isnot "MOTOROLA"#
					Options.setBoolean(Options.OPTION_SHADOW_CON,connPropChoiceGroup.isSelected(4));
					//#sijapp cond.end#
					Options.setString(Options.OPTION_HTTP_USER_AGENT,httpUserAgendTextField.getString());
					Options.setString(Options.OPTION_HTTP_WAP_PROFILE,httpWAPProfileTextField.getString());
					Options.setInt(Options.OPTION_RECONNECT_NUMBER, Integer.parseInt(reconnectNumberTextField.getString()));
					break;

				//#sijapp cond.if modules_PROXY is "true"#
				case OPTIONS_PROXY:
					Options.setInt(Options.OPTION_PRX_TYPE, srvProxyType.getSelectedIndex());
					Options.setString(Options.OPTION_PRX_SERV, srvProxyHostTextField.getString());
					Options.setString(Options.OPTION_PRX_PORT, srvProxyPortTextField.getString());

					Options.setString(Options.OPTION_PRX_NAME, srvProxyLoginTextField.getString());
					Options.setString(Options.OPTION_PRX_PASS, srvProxyPassTextField.getString());

					Options.setString(Options.OPTION_AUTORETRY_COUNT, connAutoRetryTextField.getString());
					break;
				//#sijapp cond.end#	  

				case OPTIONS_INTERFACE:
					if (ResourceBundle.langAvailable.length > 1)
						Options.setString(Options.OPTION_UI_LANGUAGE,ResourceBundle.langAvailable[uiLanguageChoiceGroup.getSelectedIndex()]);

					int idx = 0;
					Options.setBoolean(Options.OPTION_TRANS_CURSOR, choiceInterfaceMisc.isSelected(idx++));
					//#sijapp cond.if target is "MIDP2" | target is "MOTOROLA" | target is "SIEMENS2"#
					Options.setBoolean(Options.OPTION_FULL_SCREEN, choiceInterfaceMisc.isSelected(idx++));
					/* Set fullscreen mode */
					JimmUI.setColorScheme(false);
					//#sijapp cond.end#
					Options.setBoolean(Options.OPTION_SWAP_SOFT_KEY,  choiceInterfaceMisc.isSelected(idx++));
					VirtualList.assignSoftKeys();

					//#sijapp cond.if target is "SIEMENS2"#
					Options.setBoolean(Options.OPTION_BACKLIGHT, backLightChoiceGroup.isSelected(0));
					if(backLightChoiceGroup.isSelected(0)) com.siemens.mp.game.Light.setLightOn();
					else com.siemens.mp.game.Light.setLightOff();
					//#sijapp cond.end#

					int newSortMethod = clSortByChoiceGroup.getSelectedIndex();

					idx = 0;
					boolean newUseGroups = choiceContactList.isSelected(idx++);
					boolean newHideOffline = choiceContactList.isSelected(idx++);
					Options.setInt(Options.OPTION_CL_FONT_SIZE, clFontSizeChoiceGroup.getSelectedIndex());
					Options.setInt(Options.OPTION_CL_FONT_STYLE, clFontStyleChoiceGroup.getSelectedIndex());
					Options.setInt(Options.OPTION_CL_SORT_BY, newSortMethod);
					Options.setBoolean(Options.OPTION_CL_HIDE_OFFLINE, newHideOffline);
					Options.setBoolean(Options.OPTION_CL_HIDE_OFFLINE_ALL, choiceContactList.isSelected(idx++));
					Options.setBoolean(Options.OPTION_SHOW_TIME, choiceContactList.isSelected(idx++));

					idx = 0;
					Options.setBoolean(Options.OPTION_SHOW_XST_ICON,   captionChoiceGroup.isSelected(idx++));
					Options.setBoolean(Options.OPTION_SHOW_PRST_ICON,  captionChoiceGroup.isSelected(idx++));
					Options.setBoolean(Options.OPTION_SHOW_HAPPY_ICON, captionChoiceGroup.isSelected(idx++));
					Options.setBoolean(Options.OPTION_SHOW_SND_ICON,   captionChoiceGroup.isSelected(idx++));
					Options.setBoolean(Options.OPTION_SHOW_FREE_HEAP,  captionChoiceGroup.isSelected(idx++));

					idx = 0;
					Options.setBoolean(Options.OPTION_CHAT_SMALL_FONT, chrgChat.isSelected(idx++));
					Options.setBoolean(Options.OPTION_CHAT_IMAGE,      chrgChat.isSelected(idx++));
					Options.setBoolean(Options.OPTION_CHAT_SECONDS,    chrgChat.isSelected(idx++));
					//#sijapp cond.if modules_SMILES is "true"#
					Options.setBoolean(Options.OPTION_USE_SMILES,      chrgChat.isSelected(idx++));
					//#sijapp cond.end#
					Options.setBoolean(Options.OPTION_WORD_WRAP,       chrgChat.isSelected(idx++));
					Options.setBoolean(Options.OPTION_TEXT_ABC,        chrgChat.isSelected(idx++));
					//#sijapp cond.if modules_HISTORY is "true"#
					Options.setBoolean(Options.OPTION_HISTORY,         chrgChat.isSelected(idx++));
					Options.setBoolean(Options.OPTION_SHOW_LAST_MESS,  chrgChat.isSelected(idx++));
					//#sijapp cond.end#
					Options.setBoolean(Options.OPTION_CP1251_HACK,     chrgChat.isSelected(idx++));
					//#sijapp cond.if target is "MIDP2"#
					if (!Jimm.is_phone_SE() && !Jimm.is_smart_SE())
					{
						Options.setBoolean(Options.OPTION_SWAP_SEND_AND_BACK, chrgChat.isSelected(idx++));
					}
					//#sijapp cond.end#
					//#sijapp cond.if target is "MOTOROLA"#
					Options.setBoolean(Options.OPTION_SWAP_SEND_AND_BACK, chrgChat.isSelected(idx++));
					//#sijapp cond.end#

					//#sijapp cond.if target is "MIDP2"#
					if (Jimm.is_phone_NOKIA())
					{
						Options.setInt(Options.OPTION_LEFT_OFFSET, Integer.parseInt(leftOffsetTextField.getString()));
					}
					if (Jimm.is_phone_SE())
					{
						Options.setInt(Options.OPTION_RIGHT_OFFSET, Integer.parseInt(rightOffsetTextField.getString()));
					}
					VirtualList.setCaptionOffsets();
					//#sijapp cond.end#

					Options.setBoolean(Options.OPTION_USER_GROUPS,     newUseGroups);

					// Set UI options for real controls
					ContactList.optionsChanged
					(
						(newUseGroups != lastGroupsUsed) || (newHideOffline != lastHideOffline),
						(newSortMethod != lastSortMethod)
					);

					//#sijapp cond.if target is "MOTOROLA"#
					Options.setInt(Options.OPTION_LIGHT_TIMEOUT, Integer.parseInt(lightTimeout.getString()));
					Options.setBoolean(Options.OPTION_LIGHT_MANUAL, lightManual.isSelected(0));
					//#sijapp cond.end#

					if (!lastUILang.equals(Options.getString(Options.OPTION_UI_LANGUAGE)))
					{
						Options.setBoolean(Options.OPTIONS_LANG_CHANGED, true);
					}
					break;

				case OPTIONS_SIGNALING:
					//#sijapp cond.if target isnot "DEFAULT"# ===>
					Options.setInt(Options.OPTION_MESS_NOTIF_MODE,messageNotificationModeChoiceGroup.getSelectedIndex());
					Options.setInt(Options.OPTION_VIBRATOR, vibratorChoiceGroup.getSelectedIndex());
					Options.setInt(Options.OPTION_ONLINE_NOTIF_MODE,onlineNotificationModeChoiceGroup.getSelectedIndex());
					Options.setInt(Options.OPTION_OFFLINE_NOTIF_MODE,offlineNotificationModeChoiceGroup.getSelectedIndex());
					Options.setInt(Options.OPTION_TYPING_NOTIF_MODE,typingNotificationModeChoiceGroup.getSelectedIndex());

					//#sijapp cond.if target isnot "RIM"#       
					Options.setInt(Options.OPTION_NOTIF_VOL, notificationSoundVolume.getValue() * 10);
					Options.setString(Options.OPTION_MESS_NOTIF_FILE,messageNotificationSoundfileTextField.getString());
					Options.setString(Options.OPTION_ONLINE_NOTIF_FILE,onlineNotificationSoundfileTextField.getString());
					Options.setString(Options.OPTION_OFFLINE_NOTIF_FILE,offlineNotificationSoundfileTextField.getString());
					Options.setString(Options.OPTION_TYPING_NOTIF_FILE,typingNotificationSoundfileTextField.getString());
					Options.setBoolean(Options.OPTION_ONLINE_BLINK_ICON, blinkOnlineChoiceGroup.isSelected(0));
					Options.setBoolean(Options.OPTION_ONLINE_BLINK_NICK, blinkOnlineChoiceGroup.isSelected(1));
					Options.setInt(Options.OPTION_ONLINE_BLINK_TIME, Integer.parseInt(blinkOnlineTimeTextField.getString()));
					Options.setBoolean(Options.OPTION_OFFLINE_BLINK_NICK, blinkOfflineChoiceGroup.isSelected(0));
					Options.setInt(Options.OPTION_OFFLINE_BLINK_TIME, Integer.parseInt(blinkOfflineTimeTextField.getString()));
					Options.setBoolean(Options.OPTION_MESS_NOTIF_TYPE, disableOutgoingNotification.isSelected(0));
					//#sijapp cond.end#
					//#sijapp cond.end# <===
					Options.setInt(Options.OPTION_POPUP_WIN, chrgPopupWin.getSelectedIndex()); 

					//#sijapp cond.if target is "MIDP2" | target is "MOTOROLA" | target is "SIEMENS2"#
					Options.setBoolean(Options.OPTION_BRING_UP, chsBringUp.isSelected(0));
					Options.setBoolean(Options.OPTION_CREEPING_LINE, chsBringUp.isSelected(1));
					Options.setBoolean(Options.OPTION_SOUND_VIBRA, chsBringUp.isSelected(2));
					//#sijapp cond.end#
					break;

				//#sijapp cond.if modules_TRAFFIC is "true"#
				case OPTIONS_TRAFFIC:
					Options.setInt(Options.OPTION_COST_PER_PACKET,Util.decimalToInt(costPerPacketTextField.getString()));
					costPerPacketTextField.setString(Util.intToDecimal(Options.getInt(Options.OPTION_COST_PER_PACKET)));
					Options.setInt(Options.OPTION_COST_PER_DAY,Util.decimalToInt(costPerDayTextField.getString()));
					costPerDayTextField.setString(Util.intToDecimal(Options.getInt(Options.OPTION_COST_PER_DAY)));
					Options.setInt(Options.OPTION_COST_PACKET_LENGTH,Integer.parseInt(costPacketLengthTextField.getString()) * 1024);
					Options.setString(Options.OPTION_CURRENCY,currencyTextField.getString());
					break;
				//#sijapp cond.end#

				case OPTIONS_TIMEZONE:
					/* Set up time zone*/
					int timeZone = chsTimeZone.getSelectedIndex()-12;
					Options.setInt(Options.OPTIONS_GMT_OFFSET, timeZone);

					/* Translate selected time to GMT */
					int selHour = chsCurrTime.getSelectedIndex()-timeZone;
					if (selHour < 0) selHour += 24;
					if (selHour >= 24) selHour -= 24;

					/* Calculate diff. between selected GMT time and phone time */ 
					int localOffset = selHour-currentHour;
					while (localOffset >= 12) localOffset -= 24;
					while (localOffset < -12) localOffset += 24;
					Options.setInt(Options.OPTIONS_LOCAL_OFFSET, localOffset);
					break;

				case OPTIONS_ANTISPAM:
					Options.setString(Options.OPTION_ANTISPAM_MSG, antispamMsgTextField.getString());
					Options.setString(Options.OPTION_ANTISPAM_ANSWER, antispamAnswerTextField.getString());
					Options.setString(Options.OPTION_ANTISPAM_HELLO, antispamHelloTextField.getString());
					Options.setBoolean(Options.OPTION_ANTISPAM_ENABLE, antispamEnableChoiceGroup.isSelected(0));
					break;

				case OPTIONS_MISC:
					idx = 0;
					Options.setBoolean(Options.OPTION_CLEAR_HEAP,  miscChoiceGroup.isSelected(idx++));
					Options.setBoolean(Options.OPTION_MAGIC_EYE, miscChoiceGroup.isSelected(idx++));

					//#sijapp cond.if target is "MIDP2" | target is "MOTOROLA" | target is "SIEMENS2"#
					Options.setBoolean(Options.OPTION_BACK_IMAGE, bgImgChoiceGroup.isSelected(1));
					if (bgImgChoiceGroup.isSelected(0))
					{
						Options.setString(Options.OPTION_IMG_PATH, "back.png");
						bgImgPathTextField.setString("back.png");
						VirtualList.setBackGroundImage(null);
					}
					Options.setString(Options.OPTION_IMG_PATH, bgImgPathTextField.getString());
					//#sijapp cond.end#

					Options.setString(Options.OPTION_ENTER_PASSWORD, enterPasswordTextField.getString());
					break;

				case OPTIONS_ICQ_SETTINGS:
					idx = 0;
					Options.setBoolean(Options.OPTION_DELIVERY_REPORT, icqChoiceGroup.isSelected(idx++));
					Options.setBoolean(Options.OPTION_CACHE_CONTACTS, icqChoiceGroup.isSelected(idx++));
					Options.setBoolean(Options.OPTION_AUTO_ANSWER, icqChoiceGroup.isSelected(idx++));
					Options.setBoolean(Options.OPTION_AUTO_XTRAZ, icqChoiceGroup.isSelected(idx++));
//					Options.setBoolean(Options.OPTION_CLIENT_CAPS, icqChoiceGroup.isSelected(idx++));
					if (Icq.setPoint && Icq.isConnected())
					{
						Options.setBoolean(Options.OPTION_WEB_AWARE, icqChoiceGroup.isSelected(idx++));
						Options.setBoolean(Options.OPTION_MY_AUTH, icqChoiceGroup.isSelected(idx++));
					}

					Options.setString(Options.OPTION_STRING_VERSION, stringVersionTextField.getString());
					Options.setInt(Options.OPTION_PROT_VERSION, Integer.parseInt(stringProtTextField.getString()));

					Options.setBoolean(Options.OPTION_STATUS_RESTORE, autoStatusChoiceGroup.isSelected(0));
					Options.setBoolean(Options.OPTION_STATUS_AUTO, autoStatusChoiceGroup.isSelected(1));
					Options.setInt(Options.OPTION_STATUS_DELAY, Integer.parseInt(autoStatusDelayTimeTextField.getString()));
					TimerTasks.delay = Options.getInt(Options.OPTION_STATUS_DELAY) * 60000;
					//#sijapp cond.if modules_FILES is "true"#
					Options.setInt(Options.OPTION_FT_MODE, chsFSMode.getSelectedIndex());
					//#sijapp cond.end#

					needToUpdate = true;
					break;
			}

			if (colorsMenu.isActive()) JimmUI.setColorScheme(true);

			/* Save options */
			Options.safe_save();
			JimmUI.setColorScheme(optionsMenu, false);

			if (needToUpdate)
			{
				Icq.setPoint();
				setStatusAfterChanges();
				needToUpdate = false;
			}
			/* Activate MM */
			activate();
		}

		// enable/disable extended keys
		else if (c == enableExtKeyCommand)
		{
			Options.setBoolean(Options.OPTION_ENABLE_EXT_KEYS, true);
			InitHotkeyMenuUI();
		}
		else if (c == disableExtKeyCommand)
		{
			Options.setBoolean(Options.OPTION_ENABLE_EXT_KEYS, false);
			InitHotkeyMenuUI();
		}

		/* Accounts */
		else if (c == cmdAddNewAccount)
		{
			readAccontsControls();
			uins.addElement(Options.emptyString);
			passwords.addElement(Options.emptyString);
			clearForm();
			showAccountControls();
			return;
		}
		else if (c == cmdDeleteAccount)
		{
			readAccontsControls();
			int size = uins.size();
			String items[] = new String[size];
			for (int i = 0; i < size; i++) items[i] = checkUin((String)uins.elementAt(i));
			JimmUI.showSelector("delete", items, this, TAG_DELETE_ACCOUNT, false);
			return;
		}

		else if (c == cmdRegisterAccount)
		{
			clearForm();
			showRegisterControls();
			return;
		} 
		else if (c == cmdRequestCaptchaImage)
		{
			optionsForm.append(ResourceBundle.getString("wait"));
			Icq.connect(newPassword.getString());
			return;
		} 
		else if (c == cmdRequestRegistration)
		{
			try
			{
				optionsForm.append(ResourceBundle.getString("wait"));
				RegisterNewUinAction.requestRegistration(newPassword.getString(), captchaCode.getString().toUpperCase());
			}
			catch (Exception e) {}
			return;
		}
		else if (JimmUI.getCommandType(c, TAG_DELETE_ACCOUNT) == JimmUI.CMD_OK)
		{
			readAccontsControls();
			int index = JimmUI.getLastSelIndex();
			uins.removeElementAt(index);
			passwords.removeElementAt(index);
			clearForm();
			showAccountControls();
			Jimm.display.setCurrent(optionsForm);
		}
	}

	private static void setStatusAfterChanges()
	{
		if (Icq.isConnected())
		{
			try
			{
				OtherAction.setStandartUserInfo();
				OtherAction.setStatus(Icq.setWebAware() | (int)Options.getLong(Options.OPTION_ONLINE_STATUS));
			}
			catch (Exception e) {}
		}
	}

	private void clearForm()
	{
		optionsForm.removeCommand(cmdAddNewAccount);
		optionsForm.removeCommand(cmdRegisterAccount);
		optionsForm.removeCommand(cmdRequestCaptchaImage);
		optionsForm.removeCommand(cmdRequestRegistration);
		optionsForm.removeCommand(cmdDeleteAccount);
		//#sijapp cond.if target is "MIDP2" | target is "MOTOROLA" | target is "SIEMENS2"#
		optionsForm.deleteAll();
		//#sijapp cond.else#
		while (optionsForm.size() > 0) { optionsForm.delete(0); }
		//#sijapp cond.end#
	}
} // end of 'class OptionsForm'

class ColorChooser extends Canvas
{
	private final int height, width, _fldif, _flddo;
	private final Font font;
	private int origColor, _fldbyte, optionsColorIndex, a;
	private String s, s1;

	public ColorChooser(int colorIndex, int newColorIndex)
	{
		setFullScreenMode(true);
		optionsColorIndex = colorIndex;
		origColor = Options.getInt(optionsColorIndex);
		_fldbyte = Selector.colorTable[newColorIndex];
		height = getHeight();
		width = getWidth();
		font = Font.getFont(32, 1, 0); //Font.getFont(32, 1, 16);
		_fldif = font.charWidth('0') + 2;
		_flddo = height - font.getHeight() - 7;
		s1 = "00000" + Integer.toHexString(origColor).toUpperCase();
		s1 = s1.substring(s1.length() - 6);
		//#sijapp cond.if target is "MOTOROLA"#
		LightControl.flash(true);
		//#sijapp cond.end#
	}

	protected final void keyPressed(int keyCode)
	{
		int j = getGameAction(keyCode);
		if (j == 2 || j == 5)
		{
			a = (a + (j != 2 ? 1 : -1) + 6) % 6;
		}
		else if (j == 1 || j == 6)
		{
			int k = 0x100000 >> a * 4;
			int l = 0xf00000 >> a * 4;
			_fldbyte = _fldbyte & ~l | _fldbyte + (j != 1 ? -k : k) & l;
		}
		else if ((j == Canvas.FIRE) || (j == 8))
		{
			Options.setInt(optionsColorIndex, Integer.parseInt(s, 16));
			Options.optionsForm.InitColorMenuUI();
			Options.blinkColor = Options.getInt(Options.OPTION_COLOR_BLINK);
			Options.cursorColor = Options.getInt(Options.OPTION_COLOR_CURSOR);
		}
		else
		{
			JimmUI.backToLastScreen();
		}
		repaint();
	}

	protected final void paint(Graphics g)
	{
		g.setColor(0xffffff);
		g.fillRect(0, 0, width, height); // ����� ��� �� ���� �����...

		g.setColor(Options.getInt(optionsColorIndex));
		g.fillRect(2, 2, width - 4, (_flddo - 4) / 2); // ������ �������������...

		g.setColor(VirtualList.getInverseColor(origColor));
		g.setFont(font);
		g.drawString(s1, width / 2, (_flddo - 4) / 4, 17); // ��� ����������� �����...

		g.setColor(_fldbyte);
		g.fillRect(2, (_flddo) / 2, width - 4, (_flddo - 4) / 2); // ������ �������������. � ��� ����� �������� ����...

		g.setColor(0);
		g.drawRect(0, 0, width - 1, _flddo - 1);

		s = "00000" + Integer.toHexString(_fldbyte).toUpperCase();
		s = s.substring(s.length() - 6);
		int i = (width - 6 * _fldif) / 2;

		for (int j = 0; j < s.length(); j++)
		{
			g.setColor(j >= 2 ? j >= 4 ? 0x2020e0 : 0x20e020 : 0xe02020);
			g.drawSubstring(s, j, 1, i + j * _fldif, _flddo + 4, 20);
			if (j == a)
			{
				g.setColor(0);
				g.fillRect(i + j * _fldif, _flddo + 3, _fldif - 3, 2);
				g.fillRect(i + j * _fldif, _flddo + font.getHeight() + 3, _fldif - 3, 2);
			}
		}
	}
}