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
 File: src/jimm/SplashCanvas.java
 Version: ###VERSION###  Date: ###DATE###
 Author(s): Manuel Linsmayer, Andreas Rossbacher
 *******************************************************************************/

package jimm;

import DrawControls.*;
import jimm.comm.*;
import jimm.util.*;

import java.io.IOException;
import javax.microedition.lcdui.*;
import javax.microedition.midlet.MIDletStateChangeException;
import java.util.*;

//#sijapp cond.if target is "MOTOROLA"#
import javax.microedition.lcdui.Screen;
//#sijapp cond.end#

//#sijapp cond.if target is "RIM"#
import net.rim.device.api.system.LED;
//#sijapp cond.end#

public class SplashCanvas extends Canvas
{
	static public SplashCanvas _this;

	//#sijapp cond.if target is "MIDP2"#
	public final static Command cancelCommnad = new Command(ResourceBundle.getString("cancel"), Jimm.is_smart_SE() ? Command.CANCEL : Command.BACK, 1);
	//#sijapp cond.else#
	public final static Command cancelCommnad = new Command(ResourceBundle.getString("cancel"), Command.BACK, 1);
	//#sijapp cond.end#

	//Timer for repaint
	static private Timer t1,t2;

	// Location of the splash image (inside the JAR file)
	private static final String SPLASH_IMG = "/logo.png";

	// Image object, holds the splash image
	private static Image splash;

	// Location of the notice image (inside the JAR file)
	private static final String NOTICE_IMG = "/notice.png";

	//#sijapp cond.if target is "SIEMENS2"#
	private static final String BATT_IMG = "/batt.png";
	private static Image battImg = null;
	
	private static Image getBattImg() 
	{
		if( battImg == null )
		{
			try
			{
				battImg = Image.createImage(SplashCanvas.BATT_IMG);
			}
			catch(IOException e){}
		}
		return battImg;
	}
	//#sijapp cond.end#
/*
	// Adding logo in About Screen
	private static Image AboutImg = null;
	
	static public Image getAboutImg() 
	{
		if (AboutImg == null)
		{
			try
			{
				AboutImg = Image.createImage("/about.png");
			}
			catch(IOException e){}
		}
		return AboutImg;
	}
*/
	// Image object, holds the notice image
	private static Image notice;

	// Font used to display the logo (if image is not available)
	private static Font logoFont = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_LARGE);
	
	// Font used to display the version nr
	private static Font versionFont = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
	/* For E2 */
	//private static Font versionFont = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL);

	// Font (and font height in pixels) used to display informational messages
	private static Font font = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN, Font.SIZE_SMALL);
	/* For E2 */
	//private static Font font = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_BOLD, Font.SIZE_SMALL);

	private static int height = font.getHeight();

	// Initializer block
	static
	{
		// Construct splash image
		try
		{
			SplashCanvas.notice = Image.createImage(SplashCanvas.NOTICE_IMG);
		}
		catch (IOException e)
		{
			// Do nothing
		}
	}

	/*****************************************************************************/

	// Message to display beneath the splash image
	static private String message;

	// Progress in percent
	static private int progress; // = 0

	// True if keylock has been enabled
	static private boolean isLocked;

	// Number of available messages
	static private int availableMessages;
	
	// Time since last key # pressed 
	static public long poundPressTime;

	// Should the keylock message be drawn to the screen?
	static protected boolean showKeylock;

	static private int status_index = -1;
	static private Icon xstatus_img = null;

	// Constructor
	public SplashCanvas(String message)
	{
		_this = this;
		//#sijapp cond.if target is "MOTOROLA" | target is "SIEMENS2" | target is "MIDP2"#
		setFullScreenMode(true);
		//#sijapp cond.end#
		setMessage(message);
		showKeylock = false;
	}

	// Constructor, blank message
	public SplashCanvas()
	{
		this(null);
	}

	// Returns the informational message
	static public synchronized String getMessage()
	{
		return (message);
	}

	// Sets the informational message
	static public synchronized void setMessage(String message)
	{
		SplashCanvas.message = new String(message);
		SplashCanvas._this.repaint();
	}

	public static synchronized void setStatusToDraw(int st_index)
	{
		status_index = st_index;
	}

	public static synchronized void setXStatusToDraw(Icon st_index)
	{
		xstatus_img = st_index;
	}

	// Returns the current progress in percent
	static public synchronized int getProgress()
	{
		return (progress);
	}
	
	static public Image getSplashImage()
	{
		if (SplashCanvas.splash == null)
		{
			try
			{
				SplashCanvas.splash = Image.createImage(SplashCanvas.SPLASH_IMG);
			}
			catch (Exception e)
			{
				SplashCanvas.splash = null;
			}
		}
		return SplashCanvas.splash;
	}

	static public void show()
	{
		if (t2 != null)
		{
			t2.cancel();
			t2 = null;
		}
		Jimm.display.setCurrent(_this);
	}

	static public void addCmd(Command cmd)
	{
		_this.addCommand(cmd);
	}

	static public void removeCmd(Command cmd)
	{
		_this.removeCommand(cmd);
	}

	static public void setCmdListener(CommandListener l)
	{
		_this.setCommandListener(l);
	}

	static public void Repaint()
	{
		_this.repaint();
	}

	// Sets the current progress in percent (and request screen refresh)
	static public synchronized void setProgress(int progress)
	{
		if (SplashCanvas.progress == progress) return;
		int previousProgress = SplashCanvas.progress;
		SplashCanvas.progress = progress;
		if(progress < previousProgress) _this.repaint();
		else _this.repaint(0, _this.getHeight() - SplashCanvas.height - 2, _this.getWidth(), SplashCanvas.height + 2);
	}

	// Enable keylock
	static public synchronized void lock()
	{
		SplashCanvas._this.removeCommand(SplashCanvas.cancelCommnad);
		if (isLocked) return;
		
		isLocked = true;
		//#sijapp cond.if target is "MOTOROLA"#
		LightControl.Off();
		//#sijapp cond.end#
		setMessage(ResourceBundle.getString("keylock_enabled"));
		setStatusToDraw(JimmUI.getStatusImageIndex(Icq.getCurrentStatus()));
		setXStatusToDraw(Icq.getCurrentXStatus());
		Jimm.display.setCurrent(_this);
		(t2 = new Timer()).schedule(new TimerTasks(TimerTasks.SC_AUTO_REPAINT), 20000, 20000);
		setProgress(0);
		Jimm.isPasswordProtected = true;
	}

	// Disable keylock
	static public synchronized void unlock(boolean showContactList)
	{
		if (isLocked)
		{
			isLocked = false;
			availableMessages = 0;
			//#sijapp cond.if target is "RIM"#
			LED.setState(LED.STATE_OFF);
			//#sijapp cond.end#

			//#sijapp cond.if target is "MOTOROLA"#
			if (Options.getBoolean(Options.OPTION_LIGHT_MANUAL))
			{
				LightControl.On();
			}
			//#sijapp cond.end#
			if (t2 != null)
			{
				t2.cancel();
			}
		}

		ContactList.activate();
	}

	// Is the screen locked?
	static public boolean locked()
	{
		return (isLocked);
	}

	protected void hideNotify()
	{
		SplashCanvas.splash = null;
	}

	// Called when message has been received
	static public synchronized void messageAvailable()
	{
		if (isLocked)
		{
			++availableMessages;
			// #sijapp cond.if target is "RIM"#
			LED.setConfiguration(500, 250, LED.BRIGHTNESS_50);
			LED.setState(LED.STATE_BLINKING);
			// #sijapp cond.end#
			// #sijapp cond.if target is "MOTOROLA"#
			if (Options.getInt(Options.OPTION_MESS_NOTIF_MODE) == 0)
			Jimm.display.flashBacklight(1000);
			// #sijapp cond.end#
			_this.repaint();
		}
	}

	// Called when a key is pressed
	protected void keyPressed(int keyCode)
	{
		if (isLocked)
		{
			if ((keyCode == Canvas.KEY_POUND) || (keyCode == Canvas.KEY_STAR))
			{
				poundPressTime = System.currentTimeMillis();
			}
			else
			{
				if (t1 != null)
				{
					t1.cancel();
				}
				showKeylock = true;
				repaint();
			}
		}
		//#sijapp cond.if target is "MOTOROLA"#
		Jimm.display.flashBacklight(3000);
		//#sijapp cond.end#
	}

	private void tryToUnlock(int keyCode)
	{
		if (!isLocked) return;
		if ((keyCode != Canvas.KEY_POUND) && (keyCode != Canvas.KEY_STAR))
		{
			poundPressTime = 0;
			return;
		}

		if ((poundPressTime != 0) && ((System.currentTimeMillis() - poundPressTime) > 900))
		{
			if ((Options.getString(Options.OPTION_ENTER_PASSWORD)).length() > 0)
			{
				EnterPassword.activate(Jimm.display.getCurrent());
			}
			else
			{
				unlock(true);
				poundPressTime = 0;
			}
		}
	}

	// Called when a key is released
	protected void keyReleased(int keyCode)
	{
		tryToUnlock(keyCode);
	}
	
	protected void keyRepeated(int keyCode)
	{
		tryToUnlock(keyCode);
	}

	// Render the splash image
	protected void paint(Graphics g)
	{
		int bgColor = Options.getInt(Options.OPTION_COLOR_SBACK);
		int txtColor = VirtualList.getInverseColor(bgColor);

		// Do we need to draw the splash image?
		if (g.getClipY() < getHeight() - SplashCanvas.height - 2)
		{
			// Draw background
			g.setColor(bgColor);
			g.fillRect(0, 0, getWidth(), getHeight());

			// Display splash image (or text)
			Image image = getSplashImage();
			if (image != null)
			{
				g.drawImage(image, getWidth() / 2, getHeight() / 2, Graphics.HCENTER | Graphics.VCENTER);
			}
			else
			{
				g.setColor(txtColor);
				g.setFont(SplashCanvas.logoFont);
				g.drawString("jimm", getWidth() / 2, getHeight() / 2 + 5, Graphics.HCENTER | Graphics.BASELINE);
				g.setFont(SplashCanvas.font);
			}

			// Display notice image (or nothing)
			if (SplashCanvas.notice != null)
			{
				g.drawImage(SplashCanvas.notice, getWidth() / 2, 2, Graphics.HCENTER | Graphics.TOP);
			}

			// Display message icon, if keylock is enabled
			if (isLocked && availableMessages > 0)
			{
				ContactList.imageList.elementAt(14).drawImage(g, 1, getHeight() - (2 * SplashCanvas.height) - 9);
				g.setColor(txtColor);
				g.setFont(SplashCanvas.font);
				g.drawString("# " + availableMessages, ContactList.imageList.elementAt(14).getWidth() + 4, getHeight() - (2 * SplashCanvas.height) - 5, Graphics.LEFT | Graphics.TOP);
			}

			//#sijapp cond.if target is "SIEMENS2"#
			String accuLevel = System.getProperty("MPJC_CAP");
			if( accuLevel != null && isLocked )
			{
				accuLevel += "%";
				int fontX = getWidth() -  SplashCanvas.font.stringWidth(accuLevel) - 1;
				if (getBattImg() != null)
					g.drawImage(getBattImg(), fontX - getBattImg().getWidth() - 1, getHeight() - (2 * SplashCanvas.height) - 9, Graphics.LEFT | Graphics.TOP);
				g.setColor(txtColor);
				g.setFont(SplashCanvas.font);
				g.drawString(accuLevel, fontX, getHeight() - (2 * SplashCanvas.height) - 5, Graphics.LEFT | Graphics.TOP);
			}
			//#sijapp cond.end#

			// Draw the date bellow notice
			g.setColor(txtColor);
			g.setFont(SplashCanvas.font);
			g.drawString(Util.getDateString(false, false), getWidth() / 2, 12, Graphics.TOP | Graphics.HCENTER);
			g.drawString(Util.getCurrentDay(), getWidth() / 2, 13 + SplashCanvas.font.getHeight(), Graphics.TOP | Graphics.HCENTER);

			// Display the keylock message if someone hit the wrong key
			if (showKeylock)
			{
				// Init the dimensions
				int x, y, size_x, size_y;
				size_x = getWidth() / 10 * 8;
				size_y = Font.getFont(Font.FACE_SYSTEM, Font.STYLE_PLAIN,Font.SIZE_MEDIUM).getHeight() * TextList.getLineNumbers(ResourceBundle.getString("keylock_message"), size_x - 8, 0, 0, 0) + 8;
				x = getWidth() / 2 - (getWidth() / 10 * 4);
				y = getHeight() / 2 - (size_y / 2);

				g.setColor(txtColor);
				g.fillRect(x, y, size_x, size_y);
				g.setColor(bgColor);
				g.drawRect(x + 2, y + 2, size_x - 5, size_y - 5);
				TextList.showText(g, ResourceBundle.getString("keylock_message"), x + 4, y + 4, size_x - 8, size_y - 8, TextList.MEDIUM_FONT, 0, VirtualList.getInverseColor(txtColor));

				(t1 = new Timer()).schedule(new TimerTasks(TimerTasks.SC_HIDE_KEYLOCK), 2000);
			}
		}

		// Draw white bottom bar
		g.setColor(txtColor);
		g.setStrokeStyle(Graphics.DOTTED);
		g.drawLine(0, getHeight() - SplashCanvas.height - 3, getWidth(), getHeight() - SplashCanvas.height - 3);

		g.setColor(txtColor);
		g.setFont(SplashCanvas.font);

		Icon draw_img = null;
		int im_width = 0;
		if (status_index != -1)
		{
			draw_img = ContactList.imageList.elementAt(status_index);
			im_width = draw_img.getWidth();
		}

		int ims_width = 0;
		if (xstatus_img != null && xstatus_img != XStatus.getStatusImage(XStatus.XSTATUS_NONE) && getWidth() > 129)
		{
			ims_width = xstatus_img.getWidth();
		}
		// Draw the progressbar message
		g.drawString(message, (getWidth() / 2) + (im_width / 2), getHeight(), Graphics.BOTTOM | Graphics.HCENTER);

		if (ims_width != 0)
		{
			xstatus_img.drawByRight(g, (getWidth() / 2) - (font.stringWidth(message) / 2) + (im_width / 2), getHeight() - (height / 2));
		}

		if (draw_img != null)
		{
			draw_img.drawByRight(g, (getWidth() / 2) - (font.stringWidth(message) / 2) + (im_width / 2) - ims_width, getHeight() - (height / 2));
		}

		// Draw current progress
		int progressPx = getWidth() * progress / 100;
		if (progressPx < 1) return;

		g.setClip(0, getHeight() - SplashCanvas.height - 2, progressPx, SplashCanvas.height + 2);
		g.setColor(txtColor);
		g.fillRect(0, getHeight() - SplashCanvas.height - 2, progressPx, SplashCanvas.height + 2);

		// Draw the progressbar message
		g.setColor(bgColor);
		g.drawString(message, (getWidth() / 2) + (im_width / 2), getHeight(), Graphics.BOTTOM | Graphics.HCENTER);

		if (ims_width != 0)
		{
			xstatus_img.drawByRight(g, (getWidth() / 2) - (font.stringWidth(message) / 2) + (im_width / 2), getHeight() - (height / 2));
		}

		if (draw_img != null)
		{
			draw_img.drawByRight(g, (getWidth() / 2) - (font.stringWidth(message) / 2) + (im_width / 2) - ims_width, getHeight() - (height / 2));
		}
	}

	public static int getAreaWidth()
	{
		return _this.getWidth();
	}

	public static void startTimer()
	{
		if (status_index != 8)
		{
			new Timer().schedule(new TimerTasks(TimerTasks.SC_RESET_TEXT_AND_IMG), 15000);
		}
	}

	public static void addTimerTask(String captionLngStr, Action action, boolean canCancel)
	{
		if (t2 != null)
		{
			t2.cancel();
			t2 = null;
		}

		isLocked = false;

		TimerTasks timerTask = new TimerTasks(action); 

		SplashCanvas._this.removeCommand(cancelCommnad);
		if (canCancel)
		{
			SplashCanvas._this.addCommand(cancelCommnad);
			SplashCanvas._this.setCommandListener(timerTask);
		}

		SplashCanvas.setMessage(ResourceBundle.getString(captionLngStr));
		SplashCanvas.setProgress(0);
		Jimm.display.setCurrent(SplashCanvas._this);

		Jimm.getTimerRef().schedule(timerTask, 1000, 1000);
	}
}
