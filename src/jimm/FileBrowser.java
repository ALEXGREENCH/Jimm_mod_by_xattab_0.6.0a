//#sijapp cond.if (target="MOTOROLA"|target="MIDP2"|target="SIEMENS2")&(modules_FILES="true"|modules_HISTORY="true")#
package jimm;

//#sijapp cond.if target="MIDP2"|target="MOTOROLA"#
import javax.microedition.io.file.FileSystemRegistry;
import javax.microedition.io.file.FileConnection;
//#sijapp cond.elseif target="SIEMENS2"#
import com.siemens.mp.io.file.FileConnection;
import com.siemens.mp.io.file.FileSystemRegistry;
//#sijapp cond.end#
import javax.microedition.io.Connector;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.List;

import jimm.util.ResourceBundle;
import java.util.Enumeration;
import java.util.Vector;
import java.io.*;
import DrawControls.*;

abstract class FileSystem
{
	public InputStream fileInputStream;

	public OutputStream fileOutputStream;

	public static FileSystem getInstance()
	{
		//#sijapp cond.if target="MOTOROLA"#
		if (!Jimm.supports_JSR75) return new MotorolaFileSystem();
		else
		//#sijapp cond.end#
		return new JSR75FileSystem();
	}

	public static String[] getDirectoryContents(String dir, boolean only_dirs)
			throws JimmException
	{
		//#sijapp cond.if target="MOTOROLA"#
		if (!Jimm.supports_JSR75) return MotorolaFileSystem
				.getDirectoryContents(dir, only_dirs);
		else
		//#sijapp cond.end#
		return JSR75FileSystem.getDirectoryContents(dir, only_dirs);
	}

	public static long totalSize(String root) throws Exception
	{
		//#sijapp cond.if target="MOTOROLA"#
		if (!Jimm.supports_JSR75) return MotorolaFileSystem.totalSize(root);
		else
		//#sijapp cond.end#
		return JSR75FileSystem.totalSize(root);
	}

	public abstract void openFile(String file) throws Exception;

	public abstract OutputStream openOutputStream() throws Exception;

	public abstract InputStream openInputStream() throws Exception;

	public abstract void close();

	public abstract long fileSize() throws Exception;
	
	//#sijapp cond.if target is "SIEMENS2"|target is "MIDP2"#
	public abstract String getName();
	//#sijapp cond.end#
}

//#sijapp cond.if target="MOTOROLA"#
class MotorolaFileSystem extends FileSystem
{
	private com.motorola.io.FileConnection fileConnection;

	public static String[] getDirectoryContents(
		String currDir,
		boolean only_dirs) throws JimmException
	{
		String[] items = null;
		try
		{
			if (currDir.equals(FileBrowser.ROOT_DIRECTORY))
			{
				String[] roots = com.motorola.io.FileSystemRegistry.listRoots();
				items = new String[roots.length];
				for (int i = 0; i < roots.length; i++)
					items[i] = roots[i].substring(1);
			}
			else
			{
				com.motorola.io.FileConnection fileconn = (com.motorola.io.FileConnection) Connector
						.open("file://" + currDir);
				String[] list = fileconn.list();
				fileconn.close();
				Vector list_vect = new Vector(list.length + 1);
				list_vect.addElement(FileBrowser.PARENT_DIRECTORY);
				for (int i = 0; i < list.length; i++)
				{
					if (only_dirs & !list[i].endsWith("/")) continue;
					list_vect.addElement(list[i].substring(currDir.length()));
				}
				items = new String[list_vect.size()];
				list_vect.copyInto(items);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new JimmException(191, 0, true);
		}
		return items;
	}

	public static long totalSize(String name) throws Exception
	{
		long total_size = 0;
		com.motorola.io.FileConnection fileconn = (com.motorola.io.FileConnection) Connector.open("file:///" + name);
		total_size = fileconn.totalSize();
		fileconn.close();
		return total_size;
	}

	public void openFile(String file) throws Exception
	{
		fileConnection = (com.motorola.io.FileConnection) Connector.open("file://" + file);
	}

	public OutputStream openOutputStream() throws Exception
	{
		if (!fileConnection.exists())
		{
			fileConnection.create();
		}
		else if (fileConnection.exists() & (fileOutputStream == null))
		{
			fileConnection.delete();
			fileConnection.create();
		}
		return (fileOutputStream != null) ? fileOutputStream : fileConnection
				.openOutputStream();
	}

	public InputStream openInputStream() throws Exception
	{
		return (fileInputStream != null) ? fileInputStream : fileConnection
				.openInputStream();
	}

	public void close()
	{
		try
		{
			if (fileInputStream != null) fileInputStream.close();
			if (fileOutputStream != null) fileOutputStream.close();
			if (fileConnection != null) fileConnection.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public long fileSize() throws Exception
	{
		if (fileConnection != null) return fileConnection.fileSize();
		else return -1;
	}
}

//#sijapp cond.end#
class JSR75FileSystem extends FileSystem
{
	private FileConnection fileConnection;

	public static String[] getDirectoryContents(
		String currDir,
		boolean only_dirs) throws JimmException
	{
		String[] items = null;
		try
		{
			if (currDir.equals(FileBrowser.ROOT_DIRECTORY))
			{
				Vector roots_vect = new Vector();
				Enumeration roots = FileSystemRegistry.listRoots();
				while (roots.hasMoreElements())
					roots_vect.addElement(((String) roots.nextElement()));
				items = new String[roots_vect.size()];
				roots_vect.copyInto(items);
			}
			else
			{
				FileConnection fileconn;
				//#sijapp cond.if target="SIEMENS2"#
				fileconn = (FileConnection) Connector.open("file://" + currDir);
				//#sijapp cond.else#
				fileconn = (FileConnection) Connector.open("file://localhost"
						+ currDir);
				//#sijapp cond.end#

				Enumeration list = fileconn.list();
				fileconn.close();
				Vector list_vect = new Vector();
				list_vect.addElement(FileBrowser.PARENT_DIRECTORY);
				while (list.hasMoreElements())
				{
					String filename = (String) list.nextElement();
					if (only_dirs & !filename.endsWith("/")) continue;
					list_vect.addElement(filename);
				}
				items = new String[list_vect.size()];
				list_vect.copyInto(items);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw new JimmException(191, 0, true);
		}
		return items;
	}

	public static long totalSize(String name) throws Exception
	{
		long total_size = 0;
		FileConnection fileconn;
		//#sijapp cond.if target="SIEMENS2"#
		fileconn = (FileConnection) Connector.open("file:///" + name);
		//#sijapp cond.else#
		fileconn = (FileConnection) Connector.open("file://localhost/" + name);
		//#sijapp cond.end#

		total_size = fileconn.totalSize();
		fileconn.close();
		return total_size;
	}

	public void openFile(String file) throws Exception
	{
		fileConnection = (FileConnection) Connector.open("file://" + file);
	}

	public OutputStream openOutputStream() throws Exception
	{
		if (!fileConnection.exists())
		{
			fileConnection.create();
		}
		else if (fileConnection.exists() & (fileOutputStream == null))
		{
			fileConnection.delete();
			fileConnection.create();
		}
		return (fileOutputStream != null) ? fileOutputStream : fileConnection
				.openOutputStream();
	}

	public InputStream openInputStream() throws Exception
	{
		return (fileInputStream != null) ? fileInputStream : fileConnection
				.openInputStream();
	}

	public void close()
	{
		try
		{
			if (fileInputStream != null) fileInputStream.close();
			if (fileOutputStream != null) fileOutputStream.close();
			if (fileConnection != null) fileConnection.close();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public long fileSize() throws Exception
	{
		if (fileConnection != null) return fileConnection.fileSize();
		else return -1;
	}
	
	//#sijapp cond.if target is "SIEMENS2"|target is "MIDP2"#
	public String getName()
	{
		if( fileConnection != null )
			return fileConnection.getName();
		
		return null;
	}
	//#sijapp cond.end#
}

interface FileBrowserListener
{
	public void onFileSelect(String file);

	public void onDirectorySelect(String directory);

	public ContactItem getCItem(); 
}

public class FileBrowser implements CommandListener, VirtualTreeCommands, VirtualListCommands
{
	public static final String ROOT_DIRECTORY = "/";

	public static final String PARENT_DIRECTORY = "../";

	public static final Command backCommand = new Command(ResourceBundle.getString("back"), Command.SCREEN, 0);

	public static final Command selectCommand = new Command(ResourceBundle.getString("select"), Command.OK, 1);

	public static final Command openCommand = new Command(ResourceBundle.getString("open"), Command.OK, 1);

	private static boolean needToSelectDirectory, openCommandSelected;

	private static FileBrowser _this;

	private static VirtualTree tree;

	private static FileBrowserListener listener;

	private static ImageList imageList = ImageList.load("/fs.png");

	private static String[] items;

	private static String currDir;

	public FileBrowser()
	{
		_this = this;
		tree = new VirtualTree(null, false);
		tree.setVTCommands(this);
		tree.setVLCommands(this);
		tree.setImageList(imageList);
		tree.setFontSize((imageList.getHeight() < 16) ? VirtualList.SMALL_FONT : VirtualList.MEDIUM_FONT);
		tree.setStepSize(-tree.getFontHeight() / 2);
		tree.setCapImage(imageList.elementAt(0));
		JimmUI.setColorScheme(tree, false);
		tree.setCyclingCursor(true);
		tree.setShowButtons(false);
		tree.addCommandEx(backCommand, VirtualList.MENU_RIGHT_BAR);
		tree.setCommandListener(this);
	}

	private static void reset()
	{
		tree.lock();
		items = new String[0];
		tree.clear();
		tree.unlock();
	}

	private static int getNodeWeight(String filename)
	{
		if (filename.equals(PARENT_DIRECTORY)) return 0;
		if (filename.endsWith("/")) return 10;
		return 20;
	}

	public int vtCompareNodes(TreeNode node1, TreeNode node2)
	{
		int result = 0;
		String name1 = (String) node1.getData();
		String name2 = (String) node2.getData();
		int weight1 = getNodeWeight(name1);
		int weight2 = getNodeWeight(name2);
		if (weight1 == weight2) result = name1.toLowerCase().compareTo(name2.toLowerCase());
		else result = (weight1 < weight2) ? -1 : 1;
		return result;
	}

	private static void rebuildTree()
	{
		tree.lock();
		tree.clear();
		for (int i = 0; i < items.length; i++)
			tree.addNode(null, items[i]);
		tree.sortNode(null);
		tree.unlock();
		updateTreeCaptionAndCommands((String) tree.getCurrentItem().getData());
	}

	public static void setParameters(boolean select_dir)
	{
		needToSelectDirectory = select_dir;
	}

	public static void setListener(FileBrowserListener _listener)
	{
		listener = _listener;
	}

	public void VTnodeClicked(TreeNode node)
	{
		String file = (String) node.getData();
		
		if (file.equals(PARENT_DIRECTORY))
		{
			int d = currDir.lastIndexOf('/', currDir.length() - 2);
			currDir = (d != -1) ? currDir.substring(0, d + 1) : ROOT_DIRECTORY;
			reset();
			try
			{
				items = FileSystem.getDirectoryContents(currDir, needToSelectDirectory);
			}
			catch (JimmException e)
			{
				JimmException.handleException(e);
			}
			rebuildTree();
		}
		else if (file.endsWith("/"))
		{
			currDir += file;
			reset();
			try
			{
				items = FileSystem.getDirectoryContents(currDir, needToSelectDirectory);
			}
			catch (JimmException e)
			{
				JimmException.handleException(e);
			}
		    rebuildTree();
			openCommandSelected = false;
		}
		else
		{
			listener.onFileSelect(currDir + file);
		}
	}

	private static void updateTreeCaptionAndCommands(String name)
	{
		tree.removeCommandEx(openCommand);
		tree.removeCommandEx(selectCommand);
		tree.removeCommandEx(JimmUI.cmdMenu);
		if (name.equals(PARENT_DIRECTORY))
		{
			int d = currDir.lastIndexOf('/', currDir.length() - 2);
			tree.addCommandEx(openCommand, VirtualList.MENU_LEFT_BAR);
			tree.setCaption((d != -1) ? currDir.substring(0, d + 1) : ROOT_DIRECTORY);
		}
		else if (name.endsWith("/") & currDir.equals(ROOT_DIRECTORY))
		{
			try
			{
				tree.setCaption(ResourceBundle.getString("total_mem") + ": " + (FileSystem.totalSize(name) >> 10) + "Kb");
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
//			if (needToSelectDirectory)
//			{
//				tree.addCommandEx(JimmUI.cmdMenu, VirtualList.MENU_LEFT_BAR);
//				tree.addCommandEx(selectCommand, VirtualList.MENU_LEFT);
//				tree.addCommandEx(openCommand, VirtualList.MENU_LEFT);
//			}
			tree.addCommandEx(openCommand, VirtualList.MENU_LEFT_BAR);
		}
		else if (name.endsWith("/") & !currDir.equals(ROOT_DIRECTORY))
		{
			if (needToSelectDirectory)
			{
				tree.addCommandEx(JimmUI.cmdMenu, VirtualList.MENU_LEFT_BAR);
				tree.addCommandEx(selectCommand, VirtualList.MENU_LEFT);
				tree.addCommandEx(openCommand, VirtualList.MENU_LEFT);
			}
			else tree.addCommandEx(openCommand, VirtualList.MENU_LEFT_BAR);
			tree.setCaption(currDir + name);
		}
		else
		{
			tree.addCommandEx(selectCommand, VirtualList.MENU_LEFT_BAR);
			try
			{
				int file_size = 0;
				FileSystem file = FileSystem.getInstance();
				file.openFile(currDir + name);
				file_size = (int) (file.fileSize() >> 10);
				file.close();
				int ext = name.lastIndexOf('.');
				StringBuffer str_buf = new StringBuffer();
				if (ext != -1) str_buf = str_buf.append(name.substring(ext + 1).toUpperCase()).append(", ");
				str_buf = str_buf.append(file_size).append("Kb");
				tree.setCaption(str_buf.toString());
			}
			catch (Exception e) {}
		}
	}

	public void vtGetItemDrawData(TreeNode src, ListItem dst)
	{
		String file = (String) src.getData();
		dst.text = file;
		dst.image = imageList.elementAt(file.endsWith("/") ? 0 : 1);
		dst.color = tree.getTextColor();
		dst.fontStyle = Options.getInt(Options.OPTION_CL_FONT_STYLE);
	}

	public void vlCursorMoved(VirtualList sender)
	{
		if (sender == tree) updateTreeCaptionAndCommands((String) tree.getCurrentItem().getData());
	}

	public void vlItemClicked(VirtualList sender) {}

	public void vlKeyPress(VirtualList sender, int keyCode, int type) {}

	public static void activate() throws JimmException
	{
		if (_this == null) new FileBrowser();
		reset();
		currDir = ROOT_DIRECTORY;
		items = FileSystem.getDirectoryContents(currDir, needToSelectDirectory);
		rebuildTree();
		tree.activate(Jimm.display);
	}

	public void commandAction(Command c, Displayable d)
	{
		if (JimmUI.isControlActive(tree))
		{
			if (c == openCommand)
			{
				openCommandSelected = needToSelectDirectory;
				VTnodeClicked(tree.getCurrentItem());
			}
			else if (c == selectCommand)
			{
				String filename = (String) tree.getCurrentItem().getData();
				if (filename.endsWith("/")) listener.onDirectorySelect(currDir + filename);
				else listener.onFileSelect(currDir + filename);
			}
			else if (c == backCommand)
			{
				//try to back into menu from back image choose
				if ((listener.getCItem() != null) || needToSelectDirectory) ContactList.activate();
				else Options.optionsForm.callColorSchemeOptions();
			}
		}
	}
}
//#sijapp cond.end#