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
 File: src/jimm/main/NoticeOnBirthDay.java
 Version: ###VERSION###  Date: ###DATE###
 Author(s): Arvin
 *******************************************************************************/

//#sijapp cond.if target is "MIDP2" | target is "SIEMENS2"#
package jimm.util;

import java.io.*;
import javax.microedition.lcdui.*;
import javax.microedition.rms.*;
import java.util.*;

import jimm.comm.Util;
import jimm.*;

public class NoticeOnBirthDay implements Runnable
{
	private static NoticeOnBirthDay _this = new NoticeOnBirthDay();
	private static Vector a0 = new Vector(); //uin
	private static Vector a1 = new Vector(); //день
	private static Vector a2 = new Vector(); //месяц
	private static final int s1 = 86400; //86400 секунд  в дне
	private static long bData1;

	public static void refreshBday()
	{
		(new Thread(_this)).start();
	}

	public void run()
	{
		TimerTasks.currData = Util.createCurrentDate(false, true);
		int size = ContactList.getSize();
		for (int i = size - 1; i >= 0; i--)
		{
			ContactItem item = ContactList.getCItem(i); 
			item.setIntValue(ContactItem.CONTACTITEM_BDAY, checkDatacurrData(item.getUinString()));

			try
			{
				Thread.sleep(1);
			}
			catch (InterruptedException e) {}
		}
	}

	public static void additemB(String uin, int day, int mon)
	{
		deleteBitem(uin); // удаление на случай, если дата ДР сменилась... так, на всякий случай...
		ContactItem item = ContactList.getItembyUIN(uin);
		if (a0.size() <= 0) load(); //если база пустая то загружаем из рмс
		if ((item != null) && (checkUin(uin) == -1) && (day != 0) && (mon != 0)
			&& (!item.getBooleanValue(ContactItem.CONTACTITEM_IS_TEMP))
			&& (!item.getBooleanValue(ContactItem.CONTACTITEM_PHANTOM)))
		{
			a0.addElement(uin);
			a1.addElement(String.valueOf(day));
			a2.addElement(String.valueOf(mon));
			item.setIntValue(ContactItem.CONTACTITEM_BDAY,checkDatacurrData(uin));
			save();
		}
	}

	public static void deleteBitem(String uin)
	{
		int index = checkUin(uin);
		if (index != -1)
		{
			a0.removeElementAt(index);
			a1.removeElementAt(index);
			a2.removeElementAt(index);
			save();
		}
	}

	public static int checkDatacurrData(String uin) 
	{
		if (a0.size() <= 0) load(); //если база пустая то загружаем из рмс
		int index = a0.indexOf(uin);
		if (index == -1) return index;
		int day = Integer.parseInt((String)a1.elementAt(index));
		int mount = Integer.parseInt((String)a2.elementAt(index));
		int CurrYear = Util.createDate(TimerTasks.currData)[Util.TIME_YEAR]; //текущий год
		bData1 = Util.createLongTime(CurrYear, mount, day, 0, 0, 0);
		if (bData1 < TimerTasks.currData) bData1 = Util.createLongTime(CurrYear + 1, mount, day, 0, 0, 0);
		long t = TimerTasks.currData;
		for (int i = 0; i < 3; i++)
		{
			if (t == bData1) return i; //возвращает количество дней (до 9) оставшихся до дня рождения, если 0 то день настал :)))
			t += s1;
		}
		return -1;
	}

	private static int checkUin(String uin) //проверка на наличие uin в базе и возврат индекса
	{
		return a0.indexOf(uin);
	}

	private static void load()
	{
		try
		{
			load_safe();
		}
		catch (Exception e){}
	}

	private static void save() 
	{
		try
		{
			save_safe();
		}
		catch (Exception e){} 
	}

	static private void save_safe() throws IOException, RecordStoreException
	{
		RecordStore account = RecordStore.openRecordStore("birthday", true);
		while (account.getNumRecords() < 2)
		{
			account.addRecord(null, 0, 0);
		}
		byte[] buf;
		ByteArrayOutputStream baos;
		DataOutputStream dos;
		baos = new ByteArrayOutputStream();
		dos = new DataOutputStream(baos);
		int num = a1.size() * 3;
		int num1 = 0;
		int num2 = 0;
		while (num > num1)
		{
			dos.writeByte(num1);
			dos.writeInt(Integer.parseInt((String)a0.elementAt(num2)));
			num1++;
			dos.writeByte(num1);
			dos.writeInt(Integer.parseInt((String)a1.elementAt(num2)));
			num1++;
			dos.writeByte(num1);
			dos.writeInt(Integer.parseInt((String)a2.elementAt(num2)));
			num1++;
			num2++;
		}
		buf = baos.toByteArray();
		account.setRecord(1, buf, 0, buf.length);
		account.closeRecordStore();
	}

	static private void load_safe() throws IOException, RecordStoreException
	{
		RecordStore account = RecordStore.openRecordStore("birthday", false);
		byte[] buf;
		ByteArrayInputStream bais;
		DataInputStream dis;
		buf = account.getRecord(1);
		bais = new ByteArrayInputStream(buf);
		dis = new DataInputStream(bais);
		while (dis.available() > 0)
		{
			dis.readUnsignedByte();
			a0.addElement(String.valueOf(dis.readInt()));
			dis.readUnsignedByte();
			a1.addElement(String.valueOf(dis.readInt()));
			dis.readUnsignedByte();
			a2.addElement(String.valueOf(dis.readInt())); 
		}
		account.closeRecordStore();
	}
}
//#sijapp cond.end#