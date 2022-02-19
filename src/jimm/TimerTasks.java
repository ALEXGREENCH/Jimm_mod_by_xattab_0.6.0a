package jimm;

import javax.microedition.lcdui.*;
import java.util.TimerTask;
import java.util.Timer;

import DrawControls.*;
import jimm.comm.*;
import jimm.util.*;

public class TimerTasks extends TimerTask implements CommandListener
{
	public static final int SC_AUTO_REPAINT = 1;

	public static final int SC_HIDE_KEYLOCK = 2;
	public static final int SC_RESET_TEXT_AND_IMG = 3;
	final static public int TYPE_FLASH = 4;
	final static public int TYPE_CREEPING = 5;

	//#sijapp cond.if target="MOTOROLA"#
	public static final int VL_SWITCHOFF_BKLT = 10;
	//#sijapp cond.end#
	public static final int ICQ_KEEPALIVE = 100;
	private static final int ICQ_AUTOSTATUS = 200;
	private static int oldStatus = (int)Options.getLong(Options.OPTION_ONLINE_STATUS);
	public static int delay = Options.getInt(Options.OPTION_STATUS_DELAY) * 60000;
	private static java.util.Timer timer;

	private int type = -1;

	private Action action;

	boolean wasError = false;
	boolean canceled = false;

	private Object flashDispl;
	private String flashText, flashOldText;
	private int flashCounter;

	public static long currData = Util.createCurrentDate(false, true);

	public TimerTasks(Action action)
	{
		this.action = action;
	}

	public TimerTasks(int type)
	{
		this.type = type;
	}

	public TimerTasks(Object displ, String text, int counter, int type)
	{
		this.flashDispl = displ;
		this.flashText = text;
		this.flashOldText = JimmUI.getCaption(displ);
		this.flashCounter = (type == TYPE_FLASH) ? counter : 0;
		this.type = type;
	}

	public boolean cancel()
	{
		canceled = true;
		return super.cancel();
	}

	public boolean isCanceled()
	{
		return canceled;
	}

	public int getType()
	{
		return type;
	}

	public void run()
	{
		if (wasError) return;
		if (type != -1)
		{
			switch (type)
			{
			case SC_AUTO_REPAINT:
				SplashCanvas.Repaint();
				break;

			case SC_HIDE_KEYLOCK:
				SplashCanvas.showKeylock = false;
				SplashCanvas.Repaint();
				break;

			case SC_RESET_TEXT_AND_IMG:
				SplashCanvas.setMessage(ResourceBundle.getString("keylock_enabled"));
				SplashCanvas.setStatusToDraw(JimmUI.getStatusImageIndex(Icq.getCurrentStatus()));
				SplashCanvas.setXStatusToDraw(Icq.getCurrentXStatus());
				SplashCanvas.Repaint();
				break;

			case ICQ_KEEPALIVE:
				if (Icq.isConnected() && Options.getBoolean(Options.OPTION_KEEP_CONN_ALIVE))
				{
					// Instantiate and send an alive packet
					try
					{
						Icq.c.sendPacket(new jimm.comm.Packet(5, new byte[0]));
					}
					catch (JimmException e)
					{
						JimmException.handleException(e);
						if (e.isCritical()) cancel();
					}
				}
				System.gc();
				//#sijapp cond.if target="MIDP2" | target is "SIEMENS2"#
				if (currData != Util.createCurrentDate(false, true)) NoticeOnBirthDay.refreshBday();
				//#sijapp cond.end#
				break;

			case TYPE_FLASH:
				if (flashCounter == 0)
				{		
					JimmUI.setCaption(flashDispl, flashOldText);
					cancel();
					flashDispl = null;
					return;
				}
				if (checkFlashControlIsActive()) return;
				JimmUI.setCaption(flashDispl, ((flashCounter & 1) == 0) ? flashText : " ");
				flashCounter--;
				break;

			case TYPE_CREEPING:
				if (checkFlashControlIsActive()) return;
				JimmUI.setCaption(flashDispl, flashText.substring(flashCounter));
				flashCounter++;
				if (flashCounter > flashText.length() - 5) flashCounter = 0;
				break;

			case ICQ_AUTOSTATUS:
				if (Icq.isConnected() && Options.getBoolean(Options.OPTION_STATUS_AUTO))
				{
					switch(Icq.getCurrentStatus())
					{
						case ContactList.STATUS_ONLINE:
						case ContactList.STATUS_CHAT:
						case ContactList.STATUS_DND:
						case ContactList.STATUS_OCCUPIED:
						case ContactList.STATUS_EVIL:
						case ContactList.STATUS_DEPRESSION:
						case ContactList.STATUS_WORK:
						case ContactList.STATUS_HOME:
						case ContactList.STATUS_LUNCH:
							oldStatus = (int)Options.getLong(Options.OPTION_ONLINE_STATUS);
							statusChange(ContactList.STATUS_AWAY);
							MainMenu.haveToRestoreStatus = true;
							break;
						case ContactList.STATUS_AWAY:
							if (MainMenu.haveToRestoreStatus)
							{
								statusChange(ContactList.STATUS_NA);
								MainMenu.haveToRestoreStatus = true;
							}
							break;
					}
				}
				else
				{
					timer.cancel();
				}
				break;
			}
			return;
		}
		// отрисовка надписей при подключении. значения берутся из ConnectAction.java
		String screenMsg = action.getProgressMsg();
		if (screenMsg != null)
		SplashCanvas.setMessage(screenMsg);
		// отрисовка програесс-бара при подключении. значения берутся из ConnectAction.java
		SplashCanvas.setProgress(action.getProgress());
		if (action.isCompleted())
		{
			cancel();
			action.onEvent(Action.ON_COMPLETE);
		}
		else if (action.isError())
		{
			wasError = true;
			cancel();
			action.onEvent(Action.ON_ERROR);
		}
	}

	//////////////////////////////////////////////////////////////////////////////////

	public static void setStatusTimer()
	{
		if (timer != null)
		{
			timer.cancel();
			timer = null;
		}    
		timer = new Timer();
		timer.schedule(new TimerTasks(ICQ_AUTOSTATUS), delay, delay);
		if (MainMenu.haveToRestoreStatus && Options.getBoolean(Options.OPTION_STATUS_RESTORE))
		{
			int status = (int)Options.getLong(Options.OPTION_ONLINE_STATUS);
			if (status == ContactList.STATUS_AWAY || status == ContactList.STATUS_NA) statusChange(oldStatus);
		}
	}

	private static void statusChange(int status)
	{
		try
		{
			Options.setLong(Options.OPTION_ONLINE_STATUS, status);
			Options.safe_save();
			Icq.setOnlineStatus(status);
			// if (ContactList.getVisibleContactListRef().isActive()) // проверка открыт ли список...
			ContactList.tree.setCapImage(ContactList.imageList.elementAt(JimmUI.getStatusImageIndex(Icq.getCurrentStatus())));
		}
		catch (JimmException e)
		{
			JimmException.handleException(e);
		}
	}

	//////////////////////////////////////////////////////////////////////////////////

	private boolean checkFlashControlIsActive()
	{
		boolean isVisible = false;
		if (flashDispl instanceof VirtualList)
		{
			isVisible = JimmUI.isControlActive((VirtualList)flashDispl);
		}
		else if (flashDispl instanceof Displayable)
		{
			isVisible = ((Displayable)flashDispl).isShown();
		}
		
		if (!isVisible)
		{
			JimmUI.setCaption(flashDispl, flashOldText);
			cancel();
			flashDispl = null;
		}
		
		return !isVisible;
	}
	
	public void flashRestoreOldCaption()
	{
		JimmUI.setCaption(flashDispl, flashOldText);
	}

	public void commandAction(Command c, Displayable d)
	{
		if (c == SplashCanvas.cancelCommnad)
		{
			action.onEvent(Action.ON_CANCEL);
			cancel();
		}
	}
}
