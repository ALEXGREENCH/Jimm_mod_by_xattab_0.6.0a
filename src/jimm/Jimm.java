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
 File: src/jimm/Jimm.java
 Version: ###VERSION###  Date: ###DATE###
 Author(s): Manuel Linsmayer, Andreas Rossbacher
 *******************************************************************************/

package jimm;

import jimm.*;
import jimm.comm.*;
import jimm.util.*;
//#sijapp cond.if modules_FILES is "true" | modules_HISTORY="true"#
import jimm.FileBrowser.*;
//#sijapp cond.end#

import java.util.Timer;

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Display;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;
import javax.microedition.lcdui.Displayable;
import java.io.*;

public class Jimm extends MIDlet
{
	// Version
	public static String VERSION;

	// Application main object
	public static Jimm jimm;

	// Display object
	public static Display display;

	/****************************************************************************/

	// ICQ object
	private Icq icq;

	// Options container
	private Options o;

	// Main menu object
	private MainMenu mm;

	// Contact list object
	private ContactList cl;

	// Chat history object
	private ChatHistory ch;
	
	//#sijapp cond.if target is "MIDP2"#
	static private boolean is_phone_FLY;
	static private boolean is_phone_SE;
	static private boolean is_smart_SE;
	static private boolean is_phone_NOKIA;
	//#sijapp cond.end#

	//#sijapp cond.if target is "SIEMENS2"#
	static private boolean is_SGold;
	//#sijapp cond.end#

	//#sijapp cond.if target is "MOTOROLA" & (modules_FILES is "true" | modules_HISTORY is "true")#
	static public final boolean supports_JSR75;
	//#sijapp cond.end#

	// Timer object
	private static Timer timer = new Timer();

	// Traffic counter
	//#sijapp cond.if modules_TRAFFIC is "true" #
	private Traffic traffic;
	//#sijapp cond.end#

	// Splash canvas object
	private SplashCanvas sc;
	
	// Storage for messages
	//#sijapp cond.if modules_HISTORY is "true" #
	private HistoryStorage history;
	//#sijapp cond.end#

	//#sijapp cond.if target is "SIEMENS2" #
	// Main menu or My menu address
	public static String strMenuCall;
	// Illuminatin address
	public static String strLightCall;
	//#sijapp cond.end#

	// Load back image
	//#sijapp cond.if modules_FILES is "true" | modules_HISTORY="true"#
	FileSystem fs;
	public InputStream InStr;
	//#sijapp cond.end#

	private JimmUI ui;

	public static final String microeditionPlatform = System.getProperty("microedition.platform");
	public static final String microeditionProfiles = System.getProperty("microedition.profiles");
	//#sijapp cond.if target isnot "MOTOROLA"#
	public static boolean supportsCamCapture = false;
	//#sijapp cond.end#
	public static boolean isPasswordProtected = true;

	//#sijapp cond.if target="MOTOROLA" | target="MIDP2" | target is "SIEMENS2"#
	static
	{
		//#sijapp cond.if target is "MIDP2"#
		if (microeditionPlatform != null)
		{
			String platform = microeditionPlatform.toLowerCase();

			is_phone_FLY = (platform.toLowerCase().indexOf("fly") != -1);
			is_phone_SE = (platform.toLowerCase().indexOf("ericsson") != -1);
			is_smart_SE = ((platform.indexOf("m600") != -1) || (platform.indexOf("p800") != -1)
						|| (platform.indexOf("p900") != -1) || (platform.indexOf("p910") != -1)
						|| (platform.indexOf("w950") != -1) || (platform.indexOf("p990") != -1)
						|| (platform.indexOf("p1i")  != -1));
			is_phone_NOKIA = (platform.toLowerCase().indexOf("nokia") != -1);
		}
		//#sijapp cond.end#

		//#sijapp cond.if target is "SIEMENS2"#
		if (microeditionPlatform != null)
		{
			String platform = microeditionPlatform.toLowerCase();

			is_SGold = ((platform.indexOf("65") != -1) || (platform.indexOf("66") != -1)
				|| (platform.indexOf("70") != -1) || (platform.indexOf("72") != -1)
				|| (platform.indexOf("75") != -1 && platform.indexOf("s") < 0));
		}
		//#sijapp cond.end#

		//#sijapp cond.if target is "MOTOROLA" & (modules_FILES="true" | modules_HISTORY="true")#
		boolean jsr75 = false;
		try
		{
			jsr75 = Class.forName("javax.microedition.io.file.FileConnection") != null;
		}
		catch (ClassNotFoundException cnfe) {}
		finally
		{
			supports_JSR75 = jsr75;
		}
		//#sijapp cond.end#
	}
	//#sijapp cond.end#

	// Start Jimm
	public void startApp() throws MIDletStateChangeException
	{
		RunnableImpl.setMidlet(this);
		
		// Return if MIDlet has already been initialized
		if (Jimm.jimm != null)
		{
			//#sijapp cond.if target is "MIDP2"#
			if (is_phone_SE()) showWorkScreen();
			//#sijapp cond.end#
			return;
		}
		
		// Save MIDlet reference
		Jimm.jimm = this;
		
		// Get Jimm version
		Jimm.VERSION = this.getAppProperty("Jimm-Version");
		if (Jimm.VERSION == null) Jimm.VERSION = "###VERSION###";
		
		// Create options container
		this.o = new Options();

		// Create splash canvas object
		this.sc = new SplashCanvas(ResourceBundle.getString("loading"));

		Display.getDisplay(this).setCurrent(this.sc);

		// Get display object
		Jimm.display = Display.getDisplay(this);
		SplashCanvas.setMessage("Get display object");
		SplashCanvas.setProgress(10);

		//#sijapp cond.if modules_FILES is "true"#
		if (Options.getBoolean(Options.OPTION_BACK_IMAGE))
		{
			try
			{
				fs = FileSystem.getInstance();
				fs.openFile(Options.getString(Options.OPTION_IMG_PATH));
				InStr = fs.openInputStream();
			}
			catch(java.io.IOException rr) {}
			catch(java.lang.Exception rr) {}
			DrawControls.VirtualList.setBackGroundImage(InStr);
		}
		//#sijapp cond.end#

		// Create and load emotion icons
		//#sijapp cond.if modules_SMILES is "true" #
		new Emotions();
		SplashCanvas.setMessage("Load emotion icons");
		SplashCanvas.setProgress(20);
		//#sijapp cond.end#

		// Create ICQ object
		this.icq = new Icq();
		SplashCanvas.setStatusToDraw(JimmUI.getStatusImageIndex(Icq.getCurrentStatus()));
		SplashCanvas.setXStatusToDraw(Icq.getCurrentXStatus());
		SplashCanvas.setMessage("Create ICQ object");
		SplashCanvas.setProgress(30);
		
		// Create object for text storage
		//#sijapp cond.if modules_HISTORY is "true" #
		history = new HistoryStorage();
		SplashCanvas.setMessage("Create text storage");
		SplashCanvas.setProgress(40);
		//#sijapp cond.end#

		// Initialize main menu object
		this.mm = new MainMenu();
		SplashCanvas.setMessage("Create main menu");
		SplashCanvas.setProgress(50);

		// Create traffic Object
		//#sijapp cond.if modules_TRAFFIC is "true" #
		this.traffic = new Traffic();
		SplashCanvas.setMessage("Create Traffic object");
		SplashCanvas.setProgress(60);
		//#sijapp cond.end#
		
		// Create contact list object
		this.cl = new ContactList();
		ContactList.beforeConnect();
		SplashCanvas.setMessage("Create list object");
		SplashCanvas.setProgress(70);
		
		// Create chat hisotry object
		this.ch = new ChatHistory();
		SplashCanvas.setMessage("Create chat object");
		SplashCanvas.setProgress(80);

		new Templates();
		SplashCanvas.setMessage("Load templates");
		SplashCanvas.setProgress(90);

		ui = new JimmUI();
		SplashCanvas.setMessage("Load user interface");
		SplashCanvas.setProgress(100);

		//#sijapp cond.if target="MIDP2" | target is "SIEMENS2"#
		NoticeOnBirthDay.refreshBday();
		//#sijapp cond.end#

		JimmUI.setColorScheme(true);

		DrawControls.VirtualList.setDisplay(Jimm.display);

		EnterPassword.activate(Jimm.display.getCurrent());

		//#sijapp cond.if target is "SIEMENS2"#
		if (is_SGold())
		{
			strMenuCall = new String("native://ELSE_STR_MYMENU");
			strLightCall = new String("native://STUP_ILLUMINATI");
		}
		else
		{
			if (Options.getBoolean(Options.OPTION_BACKLIGHT))
			{
				com.siemens.mp.game.Light.setLightOn();
			}

			strMenuCall = new String("native://NAT_MAIN_MENU");
			strLightCall = new String("native://NAT_ILLUMINATION");
		}
		//#sijapp cond.end#
	}

	// Pause
	public void pauseApp() {}

	// Destroy Jimm
	public void destroyApp(boolean unconditional) throws MIDletStateChangeException
	{
        // Disconnect
        Icq.disconnect();
        //#sijapp cond.if target is "SIEMENS2"#
        com.siemens.mp.game.Light.setLightOff();
        //#sijapp cond.end#
        
        // Save traffic
        //#sijapp cond.if modules_TRAFFIC is "true" #
		try 
		{
			Traffic.save();
		} 
		catch (Exception e) {}
		//#sijapp cond.end#
		
		Jimm.display.setCurrent(null);
		this.notifyDestroyed();
	}

	// Returns a reference to ICQ object
	public Icq getIcqRef()
	{
		return (this.icq);
	}
	
	// Returns a reference to options container
	public Options getOptionsRef()
	{ 	 
		return (this.o);
	}

	// Returns a reference to the main menu object
	public MainMenu getMainMenuRef()
	{
		return (this.mm);
	}

	// Returns a reference to the contact list object
	public ContactList getContactListRef()
	{
		return (this.cl);
	}
	
	// Returns a reference to the chat history list object
	public ChatHistory getChatHistoryRef()
	{
		return (this.ch);
	}

	//#sijapp cond.if modules_HISTORY is "true" #
	// Returns a reference to the stored history object
	public HistoryStorage getHistory()
	{
		return (this.history);
	}
	// #sijapp cond.end#

	// Returns a reference to the timer object
	static public Timer getTimerRef()
	{
		return timer;
	}
	
	// Cancels the timer and makes a new one
	public void cancelTimer()
	{
		try
		{
			timer.cancel();
		}
		catch(IllegalStateException e){}
		timer = new Timer();
	}

	// Returns a reference to splash canvas object
	public SplashCanvas getSplashCanvasRef()
	{
		return (this.sc);
	}

	//#sijapp cond.if modules_TRAFFIC is "true" #
	// Return a reference to traffic object
	public Traffic getTrafficRef()
	{
		return (this.traffic);
	}
	//#sijapp cond.end#
	
	public JimmUI getUIRef()
	{
		return ui;
	}
	
	static public void showWorkScreen()
	{
		if (SplashCanvas.locked())
		{
			SplashCanvas.show();
		}
		else
		{
			EnterPassword.activate(Jimm.display.getCurrent());
		}
	}
	
	//#sijapp cond.if target is "MIDP2" #
	// Set the minimize state of midlet
	static public void setMinimized(boolean mini)
	{
		if (mini)
		{
			Jimm.display.setCurrent(null);
		}
		else
		{
			Displayable disp = Jimm.display.getCurrent();
			if ((disp == null) || !disp.isShown())
			{
				showWorkScreen();
			}
		}
	}
	
	public static boolean is_phone_FLY()
	{
		return is_phone_FLY;
	}	

	public static boolean is_phone_SE()
	{
		return is_phone_SE;
	}	

	public static boolean is_smart_SE()
	{
		return is_smart_SE;
	}

	public static boolean is_phone_NOKIA()
	{
		return is_phone_NOKIA;
	}	
	//#sijapp cond.end #

	//#sijapp cond.if target is "SIEMENS2"#
	public static boolean is_SGold()
	{
		return is_SGold;
	}
	//#sijapp cond.end#
}
