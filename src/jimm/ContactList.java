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
 File: src/jimm/ContactList.java
 Version: ###VERSION###  Date: ###DATE###
 Author(s): Manuel Linsmayer, Andreas Rossbacher, Artyomov Denis
 *******************************************************************************/

package jimm;

import DrawControls.*;
import jimm.comm.*;
import jimm.util.*;

import java.io.*;
import java.util.*;
import javax.microedition.lcdui.*;

import javax.microedition.rms.*;

//#sijapp cond.if target is "SIEMENS1"#
import com.siemens.mp.game.*;
import com.siemens.mp.media.*;
import com.siemens.mp.media.control.*;
import java.io.InputStream;
//#sijapp cond.end#

//#sijapp cond.if target is "MIDP2" | target is "MOTOROLA" | target is "SIEMENS2"#
import javax.microedition.lcdui.game.Sprite;
import javax.microedition.media.*;
import javax.microedition.media.control.*;
import java.io.InputStream;
//#sijapp cond.end#

//#sijapp cond.if target is "RIM"#
//import net.rim.device.api.system.Alert;
import net.rim.device.api.system.LED;
//#sijapp cond.end#

//////////////////////////////////////////////////////////////////////////////////
public class ContactList implements CommandListener, VirtualTreeCommands, VirtualListCommands
									//#sijapp cond.if target is "MIDP2" | target is "MOTOROLA" | target is "SIEMENS2"#
									, PlayerListener
									//#sijapp cond.end#
{
	/* Status (all are mutual exclusive) TODO: move status to ContactItem */
	public static final int STATUS_AWAY       = 0x00000001;
	public static final int STATUS_DND        = 0x00000002;
	public static final int STATUS_NA         = 0x00000004;
	public static final int STATUS_OCCUPIED   = 0x00000010;
	public static final int STATUS_CHAT       = 0x00000020;
	public static final int STATUS_INVISIBLE  = 0x00000100;
	public static final int STATUS_INVIS_ALL  = 0x00000200;
	public static final int STATUS_EVIL       = 0x00003000;
	public static final int STATUS_DEPRESSION = 0x00004000;
	public static final int STATUS_HOME       = 0x00005000;
	public static final int STATUS_WORK       = 0x00006000;
	public static final int STATUS_LUNCH      = 0x00002001;
	public static final int STATUS_OFFLINE    = 0xFFFFFFFF;
	public static final int STATUS_NONE       = 0x10000000;
	public static final int STATUS_ONLINE     = 0x00000000;

	/* Sound notification typs */
	public static final int SOUND_TYPE_MESSAGE = 1;
	public static final int SOUND_TYPE_ONLINE  = 2;
	public static final int SOUND_TYPE_TYPING  = 3;
	public static final int SOUND_TYPE_OFFLINE = 4;

//	private static boolean needPlayOnlineNotif = false;
//	private static boolean needPlayOfflineNotif = false;
	private static boolean needPlayMessNotif = false;
	private static ContactList _this;

	/* Main menu command */
	private static Command cmdMainMenu;

	//#sijapp cond.if modules_DEBUGLOG is "true" #
	private static Command cmdDebugList = new Command("*Debug list*", Command.ITEM, 2);
	//#sijapp cond.end#
	
	/** ************************************************************************* */

	/* Version id numbers */
	static private int ssiListLastChangeTime = -1;
	static private int ssiNumberOfItems = 0;

	/* Update help variable */
	private static boolean haveToBeCleared;

	/* Contact items */
	public static Vector cItems;

	/* Group items */
	private static Vector gItems;

	private static boolean treeBuilt = false; //treeSorted = false;

	private static boolean justConnected;

	/* Contains tree nodes by groip ids */
	private static Hashtable gNodes = new Hashtable();

	/* Tree object */
	public static VirtualTree tree;

	/* Images for icons */
	final public static ImageList imageList    = ImageList.load("/icons.png");
	final public static ImageList menuIcons    = ImageList.load("/micons.png");
	final public static ImageList clientIcons  = ImageList.load("/clicons.png");
	final public static ImageList privateIcons = ImageList.load("/prlists.png");
	final public static ImageList birthDayIcon = ImageList.load("/bday.png");
	final public static ImageList happyIcon    = ImageList.load("/happy.png");
	final public static ImageList authIcon     = ImageList.load("/auth.png");
	final public static ImageList psIcons      = ImageList.load("/pstatus.png");

	private static int onlineCounter;

	/* Initializer */
	static
	{
		//#sijapp cond.if target is "MIDP2"#
		cmdMainMenu = new Command(ResourceBundle.getString("menu"), Jimm.is_smart_SE() ? Command.CANCEL : Command.BACK, 3);
		//#sijapp cond.else#
		cmdMainMenu = new Command(ResourceBundle.getString("menu"), Command.BACK, 3);
		//#sijapp cond.end#
	}

	/* Constructor */
	public ContactList()
	{
		_this = this;

		try
		{
			load();
		}
		catch (Exception e)
		{
			haveToBeCleared = false;
			cItems = new Vector();
			gItems = new Vector();
		}

		tree = new VirtualTree(null, false);
		tree.setVTCommands(this);
		tree.setVLCommands(this);
		tree.setImageList(imageList);
		tree.setStepSize(0);

		//#sijapp cond.if modules_TRAFFIC is "true" #
		updateTitle(Traffic.getSessionTraffic());
		//#sijapp cond.else #
		updateTitle(0);
		//#sijapp cond.end#

		tree.addCommandEx(cmdMainMenu, VirtualList.MENU_RIGHT_BAR);
		tree.addCommandEx(JimmUI.cmdSelect, VirtualList.MENU_LEFT_BAR);

		//#sijapp cond.if modules_DEBUGLOG is "true" #
		tree.addCommandEx(debugListCommand, VirtualList.MENU_LEFT);
		//#sijapp cond.end#

		tree.setCommandListener(this);
	}

	/* *********************************************************** */
	final static public int SORT_BY_NAME            = 0;
	final static public int SORT_BY_STATUS          = 1;
	final static public int SORT_BY_STATUS_AND_NAME = 2;

	static private int sortType;

	public int vtCompareNodes(TreeNode node1, TreeNode node2)
	{
		Object obj1 = node1.getData();
		Object obj2 = node2.getData();
		ContactListItem item1 = (ContactListItem)obj1;
		ContactListItem item2 = (ContactListItem)obj2;
		int result = 0;

		switch (sortType)
		{
		case SORT_BY_NAME:
			result = item1.getSortText().compareTo(item2.getSortText());
			break;
		case SORT_BY_STATUS:
		case SORT_BY_STATUS_AND_NAME:
			int weight1 = item1.getSortWeight(sortType);
			int weight2 = item2.getSortWeight(sortType);
			if (weight1 == weight2) result = item1.getSortText().compareTo(item2.getSortText());
			else result = (weight1 < weight2) ? -1 : 1;
			break;
		}
		return result;
	}

	/* *********************************************************** */
	
	/* Returns reference to tree */
	static public VirtualList getVisibleContactListRef()
	{
		return tree;
	}

	/* Returns image list with status icons and status icons with red letter "C" */
	static public ImageList getImageList()
	{
		return imageList;
	}

	/* Returns the id number #1 which identifies (together with id number #2)
	 the saved contact list version */
	static public int getSsiListLastChangeTime()
	{
		return ssiListLastChangeTime;
	}

	/* Returns the id number #2 which identifies (together with id number #1)
	 the saved contact list version */
	static public int getSsiNumberOfItems()
	{
		return (ssiNumberOfItems);
	}

	// Returns number of contact items
	public static int getSize()
	{
		return cItems.size();
	}

	public static ContactItem getCItem(int index)
	{
		return (ContactItem)cItems.elementAt(index);
	}

	// Returns all contact items as array
	static public synchronized ContactItem[] getContactItems()
	{
		ContactItem[] cItems_ = new ContactItem[cItems.size()];
		ContactList.cItems.copyInto(cItems_);
		return (cItems_);
	}

	// Returns all group items as array
	static public synchronized GroupItem[] getGroupItems()
	{
		GroupItem[] gItems_ = new GroupItem[gItems.size()];
		ContactList.gItems.copyInto(gItems_);
		return (gItems_);
	}

	// Request display of the given alert and the main menu afterwards
	static public void activate(Alert alert)
	{
		ContactList.tree.activate(Jimm.display, alert);

		//#sijapp cond.if target is "MOTOROLA"#
		LightControl.flash(false);
		//#sijapp cond.end#
	}

	// Request display of the main menu
	static public void activate()
	{
		if (Options.getBoolean(Options.OPTION_CLEAR_HEAP)) System.gc(); // Очистка кучи

		tree.setCapImage(imageList.elementAt(JimmUI.getStatusImageIndex(Icq.getCurrentStatus())));
		tree.setCapXstImage(Icq.getCurrentXStatus());
		tree.setCapPrivateImage(MainMenu.getPrivateStatusImage());
		tree.setCapHappyImage(happyIcon.elementAt(Options.getBoolean(Options.OPTION_FLAG_HAPPY) ? 0 : -1));
		tree.setCapSoundImage(MainMenu.getSoundImage(Options.getBoolean(Options.OPTION_SILENT_MODE)));
		tree.setFontSize(Options.getInt(Options.OPTION_CL_FONT_SIZE) * 8);

		//#sijapp cond.if modules_TRAFFIC is "true" #
		updateTitle(Traffic.getSessionTraffic());
		//#sijapp cond.else #
		updateTitle(0);
		//#sijapp cond.end#

		// show contact list
		tree.lock();
		buildTree();
		sortAll();
		tree.unlock();

//		VirtualList.moveInCircle = true;
		tree.setCyclingCursor(true);

		JimmUI.setLastScreen(ContactList.tree);
		tree.activate(Jimm.display);

		//#sijapp cond.if target is "MOTOROLA"#
		LightControl.flash(false);
		//#sijapp cond.end#

		//#sijapp cond.if target isnot "DEFAULT" #
		// play sound notifications after connecting 
//		if (needPlayOnlineNotif)
//		{
//			needPlayOnlineNotif = false;
//			playSoundNotification(SOUND_TYPE_ONLINE);
//		}

//		if (needPlayOfflineNotif)
//		{
//			needPlayOfflineNotif = false;
//			playSoundNotification(SOUND_TYPE_OFFLINE);
//		}

		if (needPlayMessNotif)
		{
			needPlayMessNotif = false;
			playSoundNotification(SOUND_TYPE_MESSAGE);
		}
		//#sijapp cond.end#
	}

	// is called by options form when options changed
	static public void optionsChanged(boolean needToRebuildTree, boolean needToSortContacts)
	{
		if (needToRebuildTree) treeBuilt = false;
		// if (needToSortContacts) treeSorted = false;
	}

	// Tries to load contact list from record store
	static private void load() throws Exception, IOException, RecordStoreException
	{
		// Initialize vectors
		ContactList.cItems = new Vector();
		ContactList.gItems = new Vector();

		// Check whether record store exists
		String[] recordStores = RecordStore.listRecordStores();
		boolean exist = false;
		for (int i = 0; i < recordStores.length; i++)
		{
			if (recordStores[i].equals("contactlist"))
			{
				exist = true;
				break;
			}
		}
		if (!exist) throw (new Exception());

		// Open record store
		RecordStore cl = RecordStore.openRecordStore("contactlist", false);

		try
		{
			// Temporary variables
			byte[] buf;
			ByteArrayInputStream bais;
			DataInputStream dis;

			// Get version info from record store
			buf = cl.getRecord(1);
			bais = new ByteArrayInputStream(buf);
			dis = new DataInputStream(bais);
			if (!(dis.readUTF().equals(Jimm.VERSION))) throw (new IOException());

			// Get version ids from the record store
			buf = cl.getRecord(2);
			bais = new ByteArrayInputStream(buf);
			dis = new DataInputStream(bais);
			ssiListLastChangeTime = dis.readInt();
			ssiNumberOfItems = dis.readUnsignedShort();

			// Read all remaining items from the record store
			int marker = 3;

			//#sijapp cond.if modules_DEBUGLOG is "true"#
			System.gc();
			long mem = Runtime.getRuntime().freeMemory();
			//#sijapp cond.end#

			while (marker <= cl.getNumRecords())
			{
				// Get type of the next item
				buf = cl.getRecord(marker++);
				bais = new ByteArrayInputStream(buf);
				dis = new DataInputStream(bais);

				// Loop until no more items are available
				//int load = 0;
				while (dis.available() > 0)
				{
					// Get item type
					byte type = dis.readByte();

					// Normal contact
					if (type == 0)
					{
						// Instantiate ContactItem object and add to vector
						ContactItem ci = new ContactItem();
						ci.loadFromStream(dis);
						ContactList.cItems.addElement(ci);
					}
					// Group of contacts
					else if (type == 1)
					{
						// Instantiate GroupItem object and add to vector
						GroupItem gi = new GroupItem();
						gi.loadFromStream(dis);
						ContactList.gItems.addElement(gi);
					}
				}
			}

			//#sijapp cond.if modules_DEBUGLOG is "true"#
			buf = null;
			dis = null;
			bais = null;
			System.gc();
			//#sijapp cond.end#
		}
		finally
		{
			// Close record store
			cl.closeRecordStore();
		}
	}

	// Save contact list to record store
	static protected void save() throws IOException, RecordStoreException
	{
		// Try to delete the record store
		try
		{
			RecordStore.deleteRecordStore("contactlist");
		} catch (RecordStoreNotFoundException e) {}

		// Create new record store
		RecordStore cl = RecordStore.openRecordStore("contactlist", true);

		// Temporary variables
		ByteArrayOutputStream baos;
		DataOutputStream dos;
		byte[] buf;

		// Add version info to record store
		baos = new ByteArrayOutputStream();
		dos = new DataOutputStream(baos);
		dos.writeUTF(Jimm.VERSION);
		buf = baos.toByteArray();
		cl.addRecord(buf, 0, buf.length);

		// Add version ids to the record store
		baos.reset();
		dos.writeInt(ssiListLastChangeTime);
		dos.writeShort((short)ssiNumberOfItems);
		buf = baos.toByteArray();
		cl.addRecord(buf, 0, buf.length);

		// Initialize buffer
		baos.reset();

		// Iterate through all contact items
		int cItemsCount = cItems.size();
		int totalCount = cItemsCount + gItems.size();
		for (int i = 0; i < totalCount; i++)
		{
			if (i < cItemsCount) getCItem(i).saveToStream(dos);
			else
			{
				GroupItem gItem = (GroupItem)gItems.elementAt(i - cItemsCount);
				gItem.saveToStream(dos);
			}

			// Start new record if it exceeds 4000 bytes
			if ((baos.size() >= 4000) || (i == totalCount - 1))
			{
				// Save record
				buf = baos.toByteArray();
				cl.addRecord(buf, 0, buf.length);

				// Initialize buffer
				baos.reset();
			}
		}

		// Close record store
		cl.closeRecordStore();
	}
	
	// called before jimm start to connect to server
	public static void beforeConnect()
	{
		treeBuilt = false; //treeSorted = false;
		haveToBeCleared = true;
		justConnected = true;
		tree.clear();
		setStatusesOffline();
	}

	static public void setStatusesOffline()
	{
		onlineCounter = 0;
		for (int i = cItems.size() - 1; i >= 0; i--)
		{
			ContactItem item = getCItem(i);
			item.setOfflineStatus();
		}

		for (int i = gItems.size() - 1; i >= 0; i--)
		{
			((GroupItem)gItems.elementAt(i)).setCounters(0, 0);
		}

		treeBuilt = false;
	}

	// reset autoAnswered flag for all contacts
	static public void resetAutoAnsweredFlag()
	{
	   	long currStatus = Options.getLong(Options.OPTION_ONLINE_STATUS);

		for (int i = cItems.size() - 1; i >= 0; i--)
		{
			ContactItem item = getCItem(i); 

			if ((currStatus == ContactList.STATUS_NA) || (currStatus == ContactList.STATUS_OCCUPIED)
				|| (currStatus == ContactList.STATUS_DND) || (currStatus == ContactList.STATUS_AWAY))
			{
				item.autoAnswered = false;
			}
			else
			{
				item.autoAnswered = true;
			}
		}
	}

	// Returns array of uins of unuthorized and temporary contacts
	public static String[] getUnauthAndTempContacts()
	{
		Vector data = new Vector();
		for (int i = cItems.size() - 1; i >= 0; i--)
		{
			if (getCItem(i).getBooleanValue(ContactItem.CONTACTITEM_NO_AUTH) || getCItem(i).getBooleanValue(ContactItem.CONTACTITEM_IS_TEMP))
				data.addElement(getCItem(i).getStringValue(ContactItem.CONTACTITEM_UIN));
		}
		String result[] = new String[data.size()];
		data.copyInto(result);
		return result;
	}

	// Updates the client-side conact list (called when a new roster has been received)
	static public synchronized void update(int flags, int versionId1_, int versionId2_, ContactListItem[] items)
	{
		// Remove all Elements from the old ContactList
		if (haveToBeCleared)
		{
			cItems.removeAllElements();
			gItems.removeAllElements();
			haveToBeCleared = false;
			ssiNumberOfItems = 0;
		}

		// Add new contact items and group items
		for (int i = 0; i < items.length; i++)
		{
			if (items[i] instanceof ContactItem)
			{
				cItems.addElement(items[i]);
			}
			else if (items[i] instanceof GroupItem)
			{
				gItems.addElement(items[i]);
			}
		}
		ssiNumberOfItems += versionId2_;

		// Save new contact list
		ssiListLastChangeTime = versionId1_;
		safeSave();
		treeBuilt = false;

		// Which contacts already have chats?
		for (int i = getSize() - 1; i >= 0; i--)
		{
			ContactItem cItem = getCItem(i);
			cItem.setBooleanValue
			(
				ContactItem.CONTACTITEM_HAS_CHAT,
				ChatHistory.chatHistoryExists(cItem.getStringValue(ContactItem.CONTACTITEM_UIN))
			);
		}
	}

	public static void safeSave()
	{
		try
		{
			save();
		}
		catch (Exception e) {}
	}

	//==================================//
	//								    //
	//	WORKING WITH CONTACTS TREE      //
	//								    //
	//==================================//

	// Sorts the contacts and calc online counters
	static private synchronized void sortAll()
	{
		// if (treeSorted) return;
		sortType = Options.getInt(Options.OPTION_CL_SORT_BY);
		if (Options.getBoolean(Options.OPTION_USER_GROUPS))
		{
			// Sort groups
			tree.sortNode(null);

			// Sort contacts
			for (int i = 0; i < gItems.size(); i++)
			{
				GroupItem gItem = (GroupItem)gItems.elementAt(i);
				TreeNode groupNode = (TreeNode)gNodes.get(new Integer(gItem.getId()));
				tree.sortNode(groupNode);
				calcGroupData(groupNode, gItem);
			}
		}
		else tree.sortNode(null);
		// treeSorted = true;
	}

	// Builds contacts tree (without sorting) 
	static private synchronized void buildTree()
	{
		int i, gCount, cCount;
		boolean use_groups  = Options.getBoolean(Options.OPTION_USER_GROUPS);
		boolean only_online = Options.getBoolean(Options.OPTION_CL_HIDE_OFFLINE);

		cCount = cItems.size();
		gCount = gItems.size();

		if (treeBuilt || ((cCount == 0) && (gCount == 0))) return;

		tree.clear();
		tree.setShowButtons(use_groups);

		// add group nodes
		gNodes.clear();

		if (use_groups)
		{
			for (i = 0; i < gCount; i++)
			{
				GroupItem item = (GroupItem)gItems.elementAt(i);
				TreeNode groupNode = tree.addNode(null, item);
				gNodes.put(new Integer(item.getId()), groupNode);
			}
		}

		// add contacts
		for (i = 0; i < cCount; i++)
		{
			ContactItem cItem = getCItem(i);

			if (only_online && (cItem.getIntValue(ContactItem.CONTACTITEM_STATUS) == STATUS_OFFLINE)
					&& !cItem.mustBeShownAnyWay()) continue;

			if (use_groups)
			{
				TreeNode groupNode = (TreeNode)gNodes.get(new Integer(cItem.getIntValue(ContactItem.CONTACTITEM_GROUP)));
				tree.addNode(groupNode, cItem);
			}
			else
			{
				tree.addNode(null, cItem);
			}
		}

		// treeSorted = false;
		treeBuilt = true;
	}

	// Returns reference to group with id or null if group not found
	public static GroupItem getGroupById(int id)
	{
		for (int i = gItems.size() - 1; i >= 0; i--)
		{
			GroupItem group = (GroupItem) gItems.elementAt(i);
			if (group.getId() == id) return group;
		}
		return null;
	}

	// Returns reference to contact item with uin or null if not found  
	static public ContactItem getItembyUIN(String uin)
	{
		int uinInt = Integer.parseInt(uin);
		for (int i = cItems.size() - 1; i >= 0; i--)
		{
			ContactItem citem = getCItem(i);
			if (citem.getUIN() == uinInt) return citem;
		}
		return null;
	}

	static public ContactItem[] getGroupItems(int groupId)
	{
		Vector vect = new Vector();
		for (int i = 0; i < cItems.size(); i++)
		{
			ContactItem cItem = getCItem(i);
			if (cItem.getIntValue(ContactItem.CONTACTITEM_GROUP) == groupId) vect.addElement(cItem);
		}

		ContactItem[] result = new ContactItem[vect.size()];
		vect.copyInto(result);

		return result;
	}

	// Calculates online/total values for group
	static private void calcGroupData(TreeNode groupNode, GroupItem group)
	{
		if ((group == null) || (groupNode == null)) return;

		ContactItem cItem;
		int onlineCount = 0;

		int count = groupNode.size();
		for (int i = 0; i < count; i++)
		{
			if (!(groupNode.elementAt(i).getData() instanceof ContactItem)) continue; // TODO: must be removed
			cItem = (ContactItem)groupNode.elementAt(i).getData();
			if (cItem.getIntValue(ContactItem.CONTACTITEM_STATUS) != STATUS_OFFLINE) onlineCount++;
		}
		group.setCounters(onlineCount, count);
	}

	// Must be called after any changes in contacts
	public static void contactChanged
	(
		ContactItem item,
		boolean setCurrent,
		boolean needSorting
	)
	{
		if (!treeBuilt) return;

		boolean contactExistInTree = false,
				contactExistsInList,
				wasDeleted = false,
				haveToAdd = false,
				haveToDelete = false;
		TreeNode cItemNode = null;
		int i, count, groupId;

		int status = item.getIntValue(ContactItem.CONTACTITEM_STATUS);

		String uin = item.getStringValue(ContactItem.CONTACTITEM_UIN);

		// which group id ?
		groupId = item.getIntValue(ContactItem.CONTACTITEM_GROUP);

		boolean only_online = Options.getBoolean(Options.OPTION_CL_HIDE_OFFLINE);

		// Whitch group node?
		TreeNode groupNode = (TreeNode)gNodes.get(new Integer(groupId));
		if (groupNode == null) groupNode = tree.getRoot();

		// Does contact exists in tree?
		count = groupNode.size();
		for (i = 0; i < count; i++)
		{
			cItemNode = groupNode.elementAt(i);
			Object data = cItemNode.getData();
			if (!(data instanceof ContactItem)) continue;
			if (!((ContactItem)data).getStringValue(ContactItem.CONTACTITEM_UIN).equals(uin)) continue;
			contactExistInTree = true;
			break;
		}

		// Does contact exists in internal list?
		contactExistsInList = (cItems.indexOf(item) != -1);

		// Lock tree repainting
		tree.lock();

		haveToAdd = contactExistsInList && !contactExistInTree;
		if (only_online && !contactExistInTree)
			haveToAdd |= ((status != STATUS_OFFLINE) | item.mustBeShownAnyWay());

		haveToDelete = !contactExistsInList && contactExistInTree;
		if (only_online && contactExistInTree)
			haveToDelete |= ((status == STATUS_OFFLINE) && !item.mustBeShownAnyWay());

		// if have to add new contact
		if (haveToAdd && !contactExistInTree)
		{
			cItemNode = tree.addNode(groupNode, item);
		}

		// if have to delete contact
		else if (haveToDelete)
		{
			tree.removeNode(cItemNode);
			wasDeleted = true;
		}

		// sort group
		if (needSorting && !wasDeleted)
		{
			boolean isCurrent = (tree.getCurrentItem() == cItemNode), inserted = false;

			tree.deleteChild(groupNode, tree.getIndexOfChild(groupNode, cItemNode));

			int contCount = groupNode.size();
			sortType = Options.getInt(Options.OPTION_CL_SORT_BY);

			// TODO: Make binary search instead of linear before child insertion!!!
			for (int j = 0; j < contCount; j++)
			{
				TreeNode testNode = groupNode.elementAt(j);
				if (!(testNode.getData() instanceof ContactItem)) continue;
				if (_this.vtCompareNodes(cItemNode, testNode) < 0)
				{
					tree.insertChild(groupNode, cItemNode, j);
					inserted = true;
					break;
				}
			}
			if (!inserted) tree.insertChild(groupNode, cItemNode, contCount);
			if (isCurrent) tree.setCurrentItem(cItemNode);
		}

		// if set current
		if (setCurrent) tree.setCurrentItem(cItemNode);

		// unlock tree and repaint
		tree.unlock();

		// change status for chat (if exists)
		item.setStatusImage();
	}

	/* lastUnknownStatus is used for adding contact item as sometimes online messages
	 is received before contact is added to internal list */
	private static int lastUnknownStatus = STATUS_NONE;

	// Updates the client-side contact list (called when a contact changes status)
	// DO NOT CALL THIS DIRECTLY FROM OTHER THREAND THAN MAIN!
	// USE RunnableImpl.updateContactList INSTEAD!
	static public synchronized void update
	(
		String uin,
		int status,
		int xStatus,
		byte[] internalIP,
		byte[] externalIP,
		int dcPort,
		int dcType,
		int icqProt,
		int authCookie,
		int signon,
		int online,
		int idle,
		int regdata
	)
	{
		ContactItem cItem = getItembyUIN(uin);

		int trueStatus = Util.translateStatusReceived(status);

		if (cItem == null)
		{
			lastUnknownStatus = trueStatus;
			return;
		}

		long oldStatus = cItem.getIntValue(ContactItem.CONTACTITEM_STATUS);
		int newXstatus = cItem.getXStatus().getStatusIndex();

		boolean statusChanged  = (oldStatus != trueStatus);
		boolean xstatusChanged = (xStatus != newXstatus);
		boolean wasOnline = (oldStatus != STATUS_OFFLINE);
		boolean nowOnline = (trueStatus != STATUS_OFFLINE);

		if (!nowOnline)
		{
			if (wasOnline)
			{
				cItem.setStringValue(ContactItem.CONTACTITEM_OFFLINETIME, Util.getDateString(false, false));
				cItem.setIntValue(ContactItem.CONTACTITEM_CAPABILITIES, 0);
				cItem.setXStatus(new byte[0]);

				playSoundNotification(SOUND_TYPE_OFFLINE);
				cItem.prepareToBlink();
				cItem.BlinkOffline();
			}
			else if (!justConnected)
			{
				if ((System.currentTimeMillis() - cItem.lastOfflineActivity) < 60000)
				{
					MagicEye.addAction(uin, "maybe_hiding_from_you");
				}
				else
				{
					cItem.lastOfflineActivity = System.currentTimeMillis();
				}
			}

			cItem.happyFlag = 0;
			//#sijapp cond.if target isnot "DEFAULT"#
			cItem.BeginTyping(false);
			//#sijapp cond.end#
		}
		//#sijapp cond.if target isnot "DEFAULT" #
		else
		{
			checkAndPlayOnlineSound(cItem, trueStatus);
		}
		//#sijapp cond.end #

		// Online counters
		statusChanged(cItem, wasOnline, nowOnline, 0);

		// Set Status
		cItem.setIntValue(ContactItem.CONTACTITEM_STATUS, trueStatus);
		
		if (treeBuilt && statusChanged) JimmUI.statusChanged(JimmUI.getCurrentScreen(), cItem, trueStatus);
		if (xstatusChanged && (newXstatus != -1)) cItem.readXtraz = true;

		// Update DC values
		//#sijapp cond.if (target="MIDP2" | target="MOTOROLA" | target="SIEMENS2") & modules_FILES="true"#
		if ((dcType != -1) && nowOnline)
		{
			cItem.setIPValue (ContactItem.CONTACTITEM_INTERNAL_IP, internalIP);
			cItem.setIPValue (ContactItem.CONTACTITEM_EXTERNAL_IP, externalIP);
			cItem.setIntValue(ContactItem.CONTACTITEM_DC_PORT, (int)dcPort);
			cItem.setIntValue(ContactItem.CONTACTITEM_DC_TYPE, dcType);
			cItem.setIntValue(ContactItem.CONTACTITEM_ICQ_PROT, icqProt);
			cItem.setIntValue(ContactItem.CONTACTITEM_AUTH_COOKIE, authCookie);
		}
		//#sijapp cond.end#

		// Update time values
		cItem.setIntValue(ContactItem.CONTACTITEM_SIGNON, signon);
		if (nowOnline) cItem.setIntValue(ContactItem.CONTACTITEM_REGDATA, regdata);
		cItem.setIntValue(ContactItem.CONTACTITEM_ONLINE, online);
		cItem.setIntValue(ContactItem.CONTACTITEM_IDLE, idle);

//		// Play sound notice if selected
//		if ((trueStatus != STATUS_OFFLINE) && statusChanged && !treeBuilt && !wasOnline) needPlayOnlineNotif |= true;

		// Update visual list
		if (statusChanged || xstatusChanged) contactChanged(cItem, false, true);

		if (tree.isActive())
		{
			String text = null;
			if (statusChanged) text = JimmUI.getStatusString(trueStatus);
			if (text != null) JimmUI.showCapText(JimmUI.getCurrentScreen(), cItem.name + ": " + text, TimerTasks.TYPE_FLASH);
		}
	}

	//#sijapp cond.if target isnot "DEFAULT" #
	static private void checkAndPlayOnlineSound(ContactItem cItem, int trueStatus)
	{
		if (cItem == null) return;
		if ((!justConnected) && (trueStatus != STATUS_OFFLINE) && (cItem.getIntValue(ContactItem.CONTACTITEM_STATUS) == STATUS_OFFLINE))
		{
			playSoundNotification(SOUND_TYPE_ONLINE);
			cItem.prepareToBlink();
			cItem.BlinkOnline();
		}
	}
	//#sijapp cond.end#
	

	// Updates the client-side contact list (called when a contact changes status)
	static public synchronized void update(String uin, int status)
	{
		update(uin, status, -1, null, null, 0, 0, -1, 0, -1, -1, -1, -1);
	}

	static private void statusChanged(ContactItem cItem, boolean wasOnline, boolean nowOnline, int tolalChanges)
	{
		boolean changed = false;

		// which group id ?
		int groupId = cItem.getIntValue(ContactItem.CONTACTITEM_GROUP);

		// which group ?
		GroupItem group = getGroupById(groupId);

		// Calc online counters
		if (wasOnline && !nowOnline)
		{
			onlineCounter--;
			if (group != null) group.updateCounters(-1, 0);
			changed = true;
		}

		if (!wasOnline && nowOnline)
		{
			onlineCounter++;
			if (group != null) group.updateCounters(1, 0);
			changed = true;
		}

		if (group != null)
		{
			group.updateCounters(0, tolalChanges);
			changed |= (tolalChanges != 0);
		}

		if (changed) RunnableImpl.updateContactListCaption();
	}

	//Updates the title of the list
	static public void updateTitle(int traffic)
	{
		String text = onlineCounter + "/" + cItems.size();
		if (!Options.getBoolean(Options.OPTION_SHOW_TIME))
		{
			text += "-" + Util.getDateString(true, false);
		}
		//#sijapp cond.if target is "MIDP2" | target is "MOTOROLA" | target is "SIEMENS2"#
		if (traffic != 0)
		{
			text += "-" + traffic / 1024 + "K";
		}
		//if (SplashCanvas._this.getWidth() > 175)
		if (Options.getBoolean(Options.OPTION_SHOW_FREE_HEAP))
		{
			text += "-" + Runtime.getRuntime().freeMemory() / 1024 + "K";
		}
		//#sijapp cond.else#
		if (traffic != 0)
		{
			text += "-" + traffic / 1024 + "K";
		}
		//#sijapp cond.end#
		tree.setCaption(text);
	}

	// Removes a contact list item
	static public synchronized void removeContactItem(ContactItem cItem)
	{
		// Remove given contact item
		ContactList.cItems.removeElement(cItem);

		// Update visual list
		contactChanged(cItem, false, false);

		// Update online counters
		statusChanged(cItem, cItem.getIntValue(ContactItem.CONTACTITEM_STATUS) != STATUS_OFFLINE, false, -1);

		// Save list
		safeSave();
	}

	// Adds a contact list item
	static public synchronized void addContactItem(ContactItem cItem)
	{
		if (!cItem.getBooleanValue(ContactItem.CONTACTITEM_ADDED))
		{
			// does contact already exists or temporary ?
			ContactItem oldItem = getItembyUIN(cItem.getStringValue(ContactItem.CONTACTITEM_UIN));
			if (oldItem != null)
			{
				removeContactItem(oldItem);
				lastUnknownStatus = oldItem.getIntValue(ContactItem.CONTACTITEM_STATUS);
			}

			// Add given contact item
			cItems.addElement(cItem);
			cItem.setBooleanValue(ContactItem.CONTACTITEM_ADDED, true);

			// Check is chat availible 
			cItem.setBooleanValue
			(
				ContactItem.CONTACTITEM_HAS_CHAT,
				ChatHistory.chatHistoryExists(cItem.getStringValue(ContactItem.CONTACTITEM_UIN))
			);

			// Set contact status (if already received)
			if (lastUnknownStatus != STATUS_NONE)
			{
				cItem.setIntValue(ContactItem.CONTACTITEM_STATUS, lastUnknownStatus);
				lastUnknownStatus = STATUS_NONE;
			}

			// Update visual list
			contactChanged(cItem, true, true);

			// Update online counters
			statusChanged(cItem, false, cItem.getIntValue(ContactItem.CONTACTITEM_STATUS) != STATUS_OFFLINE, 1);

			// Save list
			safeSave();
		}
	}

	// Adds new group
	static public synchronized void addGroup(GroupItem gItem)
	{
		gItems.addElement(gItem);
		if (!Options.getBoolean(Options.OPTION_USER_GROUPS)) return;
		TreeNode groupNode = tree.addNode(null, gItem);
		gNodes.put(new Integer(gItem.getId()), groupNode);
		safeSave();
	}

	// removes existing group 
	static public synchronized void removeGroup(GroupItem gItem)
	{
		for (int i = cItems.size()-1; i >= 0; i--)
		{
			ContactItem cItem = getCItem(i);
			if (cItem.getIntValue(ContactItem.CONTACTITEM_GROUP) == gItem.getId())
			{
				if (cItem.getIntValue(ContactItem.CONTACTITEM_STATUS) != STATUS_OFFLINE) onlineCounter--;
				cItems.removeElementAt(i);
			}
		}
		Integer groupId = new Integer(gItem.getId());

		if (Options.getBoolean(Options.OPTION_USER_GROUPS))
		{
			TreeNode node = (TreeNode) gNodes.get(groupId);
			tree.deleteChild(tree.getRoot(), tree.getIndexOfChild(tree.getRoot(), node));
			gNodes.remove(groupId);
		}
		gItems.removeElement(gItem);
		safeSave();
	}

	static public synchronized ContactItem createTempContact(String uin)
	{
		ContactItem cItem = getItembyUIN(uin);

		if (cItem != null) return cItem;

		try
		{
			cItem = new ContactItem(0, 0, uin, uin, false, true);
		}
		catch (Exception e)
		{
			return null; // Message from non-icq contact
		}
		cItems.addElement(cItem);

		ConnectAction.setPrivacyMarks(cItem);

		cItem.setBooleanValue(ContactItem.CONTACTITEM_IS_TEMP, true);
		return cItem;
	}

	/* Adds the given message to the message queue of the contact item identified by the given UIN */
	static public synchronized void addMessage(Message message, boolean haveToBeep)
	{
		String uin = message.getSndrUin();

		ContactItem cItem = getItembyUIN(uin);

		/* Create a temporary contact entry if no contact entry could be found do we have a new temp contact */
		if (cItem == null) cItem = createTempContact(uin);

		/* Add message to chat */
		ChatHistory.addMessage(cItem, message);

		/* Notify splash canvas */
		SplashCanvas.messageAvailable();

		/* Notify user */
		if (!treeBuilt) needPlayMessNotif |= true;
		//#sijapp cond.if target isnot "DEFAULT" #
		else if (haveToBeep) playSoundNotification(SOUND_TYPE_MESSAGE);
		//#sijapp cond.end #

		/* Flag contact as having chat */
		cItem.setBooleanValue(ContactItem.CONTACTITEM_HAS_CHAT, true);

		/* Update tree */
		contactChanged(cItem, true, true);
	}

	//#sijapp cond.if target isnot "DEFAULT" & target isnot "RIM"#
	private static Player player;

	public static boolean testSoundFile(String source)
	{
		createPlayer(source);
		boolean ok = (player != null);
		closePlayer();
		return ok;
	}
	//#sijapp cond.end#

	//#sijapp cond.if target is "MIDP2" | target is "MOTOROLA" | target is "SIEMENS2"#
	// Reaction to player events. (Thanks to Alexander Barannik for idea!)
	public void playerUpdate(final Player player, final String event, Object eventData)
	{
		if (event.equals(PlayerListener.END_OF_MEDIA))
		{
			player.close();
		}
	}

	/* Creates player for file 'source' */
	static private Player createPlayer(String source)
	{
		closePlayer();

		try
		{
			/* What is file extention? */
			String ext = "wav";
			int point = source.lastIndexOf('.');
			if (point != -1) ext = source.substring(point + 1).toLowerCase();

			/* What is media type? */
			String mediaType;
			if (ext.equals("mp3")) 								mediaType = "audio/mpeg";
			else if (ext.equals("mid") || ext.equals("midi")) 	mediaType = "audio/midi";
			else if (ext.equals("amr")) 						mediaType = "audio/amr";
			else 												mediaType = "audio/X-wav";
	
			Class cls = new Object().getClass();
			InputStream is = cls.getResourceAsStream(source);
			if (is == null) is = cls.getResourceAsStream("/" + source);
			if (is != null)
			{
				player = Manager.createPlayer(is, mediaType);
				player.addPlayerListener(_this);
			}
		}
		catch (Exception e)
		{
			player = null;
		}
		return player;
	}
	
	static private void closePlayer() 
	{
		if (player != null) 
		{
			try 
			{
				if (player.getState() == Player.STARTED) 
				{
					player.stop();
				}
				player.close();
			} catch (Exception e) {}
			player = null;
		}
	}
	//#sijapp cond.end#

	//#sijapp cond.if target is"SIEMENS1"#
	static private Player createPlayer(String source)
	{
		closePlayer();
		try
		{
			player = Manager.createPlayer(source);
		}
		catch (Exception e)	{}
	}

	static private void closePlayer() 
	{
		if (player != null) 
		{
			player.close();
			player = null;
		}
	}
	//#sijapp cond.end#


	//#sijapp cond.if target is "MIDP2" | target is "MOTOROLA" | target is "SIEMENS2"#
	// sets volume for player
	static private void setVolume(int value)
	{
		try
		{
			player.realize();
			VolumeControl c = (VolumeControl) player.getControl("VolumeControl");
			if (c != null)
			{
				c.setLevel(value);
			}
			player.prefetch();
		}
		catch (Exception e) {}
	}

	//#sijapp cond.end#

	//#sijapp cond.if target isnot "DEFAULT"#
	static private synchronized void TypingHelper(String uin, boolean type)
	{
		if (type) playSoundNotification(ContactList.SOUND_TYPE_TYPING);
		if (ChatHistory.chatHistoryShown(uin))
		{
			ChatHistory.getChatHistoryAt(uin).BeginTyping(type);
		}
		else
			tree.repaint();
	}

	static public synchronized void repaintTree()
	{
		tree.repaint();
	}
	
	public static void afterConnect()
	{
		TimerTask AfterConnect = new TimerTask()
		{
			public void run()
			{
				justConnected = false;
			}
		};
		Jimm.getTimerRef().schedule(AfterConnect, 7000);

		RequestInfoAction.StartMainRequestInfo = true;
		RequestInfoAction act = new RequestInfoAction(Options.getString(Options.OPTION_UIN), "");
		try
		{
			Icq.requestAction(act);
		}
		catch (JimmException e) {}

		//#sijapp cond.if target isnot "MOTOROLA"#
		try
		{
			//#sijapp cond.if target is "MIDP2"#
			Jimm.supportsCamCapture = (System.getProperty("video.snapshot.encodings") != null);
			// Jimm.supportsCamCapture = System.getProperty("supports.video.capture").equals("true");
			//#sijapp cond.else#
			Jimm.supportsCamCapture = true;
			//#sijapp cond.end#
		}
		catch (Exception e) {}
		//#sijapp cond.end#
	}

	static public synchronized void BeginTyping(String uin, boolean type)
	{
		ContactItem item = getItembyUIN(uin);

		if (item == null)
		{
			if (!Options.getBoolean(Options.OPTION_ANTISPAM_ENABLE))
			{
				item = createTempContact(uin);
			}
		}
		
		if (item == null)
		{
			return;
		}

		// If the user does not have it add the typing capability
		if (!item.hasCapability(Util.CAPF_TYPING)) item.addCapability(Util.CAPF_TYPING);
		item.BeginTyping(type);
		TypingHelper(uin, type);
	}
	//#sijapp cond.end#

	//#sijapp cond.if target isnot "DEFAULT"#
	// Play a sound notification
	static public void playSoundNotification(int notType)
	{
		synchronized (_this)
		{
			if (!treeBuilt) return;

			int vibraKind = Options.getInt(Options.OPTION_VIBRATOR);
			if (vibraKind == 2) vibraKind = SplashCanvas.locked() ? 1 : 0;
			if ((vibraKind > 0) && (notType == SOUND_TYPE_MESSAGE))
			{
				Jimm.display.vibrate(500);
			}

			if (Options.getBoolean(Options.OPTION_SILENT_MODE) == true) return;

			//#sijapp cond.if target is "MIDP2" | target is "MOTOROLA" | target is "SIEMENS2"#
			int not_mode = 0;

			switch (notType)
			{
			case SOUND_TYPE_MESSAGE:
				not_mode = Options.getInt(Options.OPTION_MESS_NOTIF_MODE);
				break;

			case SOUND_TYPE_ONLINE:
				not_mode = Options.getInt(Options.OPTION_ONLINE_NOTIF_MODE);
				break;

			case SOUND_TYPE_OFFLINE:
				not_mode = Options.getInt(Options.OPTION_OFFLINE_NOTIF_MODE);
				break;

			case SOUND_TYPE_TYPING:
				not_mode = Options.getInt(Options.OPTION_TYPING_NOTIF_MODE) - 1;
				break;
			}

			switch (not_mode)
			{
			case 1:
				try
				{
					switch (notType)
					{
					case SOUND_TYPE_MESSAGE:
						Manager.playTone(ToneControl.C4, 500, Options.getInt(Options.OPTION_NOTIF_VOL));
						break;

					case SOUND_TYPE_ONLINE:
						Manager.playTone(ToneControl.C4 + 5, 500, Options.getInt(Options.OPTION_NOTIF_VOL));
						break;

					case SOUND_TYPE_OFFLINE:
						Manager.playTone(ToneControl.C4 + 6, 500, Options.getInt(Options.OPTION_NOTIF_VOL));
						break;

					case SOUND_TYPE_TYPING:
						Manager.playTone(ToneControl.C4 + 7, 500, Options.getInt(Options.OPTION_NOTIF_VOL));
						break;
					}

				}
				catch (Exception e) {}

				break;

			case 2:
				try
				{
					//Siemens 65-75 bugfix
					// #sijapp cond.if target is "SIEMENS2"#
					Player p1 = createPlayer("silence.wav");
					setVolume(100);
					p1.start();
					p1.close();
					//#sijapp cond.end#
					if (notType == SOUND_TYPE_MESSAGE)
					{
						player = createPlayer(Options.getString(Options.OPTION_MESS_NOTIF_FILE));
						if (player == null) return;
						setVolume(Options.getInt(Options.OPTION_NOTIF_VOL));
					}
					else if (notType == SOUND_TYPE_ONLINE)
					{
						player = createPlayer(Options.getString(Options.OPTION_ONLINE_NOTIF_FILE));
						if (player == null) return;
						setVolume(Options.getInt(Options.OPTION_NOTIF_VOL)); 
					}
					else if (notType == SOUND_TYPE_OFFLINE)
					{
						player = createPlayer(Options.getString(Options.OPTION_OFFLINE_NOTIF_FILE));
						if (player == null) return;
						setVolume(Options.getInt(Options.OPTION_NOTIF_VOL)); 
					}
					else
					{
						player = createPlayer(Options.getString(Options.OPTION_TYPING_NOTIF_FILE));
						if (player == null) return;
						setVolume(Options.getInt(Options.OPTION_NOTIF_VOL)); 
					}
					player.start();
				}
				catch (Exception me) {}

				break;
			}
			//#sijapp cond.end#

			//#sijapp cond.if target is "RIM"#
			if (Options.getBoolean(Options.OPTION_VIBRATOR))
			{
				// had to use full path since import already contains another Alert object
				net.rim.device.api.system.Alert.startVibrate(500);
			}
			int mode_rim;
			if (notType == SOUND_TYPE_MESSAGE)
				mode_rim = Options.getInt(Options.OPTION_MESS_NOTIF_MODE);
			else
				mode_rim = Options.getInt(Options.OPTION_ONLINE_NOTIF_MODE);
			switch (mode_rim)
			{
			case 1:
				// array is note in Hz, duration in ms.
				final short[] tune = new short[] { 349, 250, 0, 10, 523, 250 };
				net.rim.device.api.system.Alert.startAudio(tune, 50);
				net.rim.device.api.system.Alert.startBuzzer(tune, 50);
				break;
			}
			//#sijapp cond.end#
		}
	}
	//#sijapp cond.end#

	//#sijapp cond.if target isnot "DEFAULT"#
	static public boolean changeSoundMode(boolean needToAlert)
	{
		boolean newValue = !Options.getBoolean(Options.OPTION_SILENT_MODE);
		Options.setBoolean(Options.OPTION_SILENT_MODE, newValue);
		if (Options.getBoolean(Options.OPTION_SOUND_VIBRA)) Options.setInt(Options.OPTION_VIBRATOR, newValue ? 1 : 0);
		Options.safe_save();

		if (needToAlert)
		{
//			// #sijapp cond.if target is "MIDP2"#
//			if (Jimm.is_phone_SE())
//			{
//				Alert alert = new Alert(null, ResourceBundle.getString(newValue ? "#sound_is_off" : "#sound_is_on"), null, null);
//				alert.setTimeout(1500);
//				activate();
//				Jimm.display.setCurrent(alert);
//			}
//			else
//			// #sijapp cond.end#
			activate();
		}
		return newValue;
	}
	//#sijapp cond.end#

	static ContactItem lastChatItem = null;

	public void vtGetItemDrawData(TreeNode src, ListItem dst)
	{
		ContactListItem item = (ContactListItem)src.getData();

		dst.image         = imageList.elementAt(item.getImageIndex());
		dst.xStatusImg    = item.getXStatus().getStatusImage();
		dst.happyImg      = happyIcon.elementAt(item.getHappyImageIndex());
		dst.bDayImg       = birthDayIcon.elementAt(item.getBirthDayImageIndex());
		dst.text          = item.getText();
		dst.authImg       = authIcon.elementAt(item.getAuthImageIndex());
		dst.ignoreImg     = privateIcons.elementAt(item.getIgnoreImageIndex());
		dst.visibilityImg = privateIcons.elementAt(item.getVisibilityImageIndex());
		dst.clientImg     = clientIcons.elementAt(item.getClientImageIndex());
		dst.color         = item.getTextColor();
		dst.fontStyle     = item.getFontStyle();
	}

	public void vlCursorMoved(VirtualList sender) {}

	public void vlItemClicked(VirtualList sender) {}

	public void vlKeyPress(VirtualList sender, int keyCode, int type)
	{
		TreeNode node = tree.getCurrentItem();
		ContactItem item = ((node != null) && (node.getData() instanceof ContactItem)) ? (ContactItem)node.getData() : null;

		if (JimmUI.execDoubleHotKey(item, keyCode, type))
		{
			return;
		}

		//#sijapp cond.if target is "MIDP2" | target is "MOTOROLA"#
		switch (keyCode)
		{
			case -8:
				if (item != null)
				{
					JimmUI.removeContactMessageBox = JimmUI.showMessageBox
					(
						item,
						ResourceBundle.getString("remove"), 
						ResourceBundle.getString("remove") + " " + item.name + "?", 
						JimmUI.MESBOX_OKCANCEL
					);
				}
				return;
			//#sijapp cond.if target is "MIDP2"#
			case -26:
				if (Options.getBoolean(Options.OPTION_MAGIC_EYE))
				{
					MagicEye.activate();
				}
				return;
			//#sijapp cond.end#
		}
		//#sijapp cond.end#

		JimmUI.execHotKey(item, keyCode, type);
	}

	// shows next or previos chat 
	static synchronized protected String showNextPrevChat(boolean next)
	{
		int index = cItems.indexOf(lastChatItem);
		if (index == -1) return null;
		int di = next ? 1 : -1;
		int maxSize = cItems.size();

		for (int i = index + di;; i += di)
		{
			if (i < 0) i = maxSize - 1;
			if (i >= maxSize) i = 0;
			if (i == index) break;

			ContactItem cItem = getCItem(i);
			if (cItem.getBooleanValue(ContactItem.CONTACTITEM_HAS_CHAT))
			{
//				lastChatItem.resetUnreadMessages(); // сброс флага наличия непрочитанных сообщений...
				JimmUI.textMessReceiver = null; // на всякий случай...
				enterContactMenu = false; // сброс флага на всякий случай...
				lastChatItem = cItem;
				cItem.activate();
				return cItem.getStringValue(ContactItem.CONTACTITEM_UIN);
			}
		}
		return null;
	}

	// Returns number of unread messages 
	static protected int getUnreadMessCount()
	{
		int count = cItems.size();
		int result = 0;
		for (int i = 0; i < count; i++) result += getCItem(i).getUnreadMessCount();
		return result;
	}

	static public ContactItem[] getItems(GroupItem group)
	{
		Vector data = new Vector();
		int gid = group.getId();
		int size = getSize();
		for (int i = 0; i < size; i++)
		{
			ContactItem item = getCItem(i);
			if (item.getIntValue(ContactItem.CONTACTITEM_GROUP) == gid)
				data.addElement(item);
		}
		ContactItem[] result = new ContactItem[data.size()];
		data.copyInto(result);
		return result;
	}

	public static boolean enterContactMenu = false;

	// Command listener
	public void commandAction(Command c, Displayable d)
	{
		// Activate main menu
		if (c == cmdMainMenu)
		{
			MainMenu.activate();
		}

		// Contact item has been selected
		else if (c == JimmUI.cmdSelect)
		{
			activateListOrChat();
		}

		//#sijapp cond.if modules_DEBUGLOG is "true" #
		else if (c == cmdDebugList)
		{
			DebugLog.activate();
		}
		//#sijapp cond.end#
	}

	private void activateListOrChat()
	{
		TreeNode node = tree.getCurrentItem();

		if (node == null) return;
		ContactListItem item = (ContactListItem)node.getData();

		if (item instanceof ContactItem)
		{
			// Activate the contact item menu
			//#sijapp cond.if target is "RIM"#
			LED.setState(LED.STATE_OFF);
			//#sijapp cond.end#

			lastChatItem = (ContactItem)item;
			lastChatItem.activate();
		}

		if (item instanceof GroupItem)
		{
			tree.setExpandFlag(node, !node.getExpanded());
		}
	}
}