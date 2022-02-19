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
 File: src/jimm/comm/XtrazSM.java
 Version: ###VERSION###  Date: ###DATE###
 Author: Unknown
 *******************************************************************************/

package jimm.comm;

import java.io.*;
import jimm.*;
import jimm.util.*;

public final class XtrazSM
{
    public XtrazSM() {}

    public static void b(String s, String s1) throws Exception
    {
        int i;
        if ((i = s1.indexOf("<NR><RES>")) < 0) throw new Exception("cp #2");
        int j;
        if ((j = s1.indexOf("</RES></NR>")) < 0) throw new Exception("cp #3");
        String s2;
        int k;
        if ((k = (s2 = Util.DeMangleXml(s1.substring(i + 9, j))).indexOf("<val srv_id='")) < 0) throw new Exception("cp #6");
        int j2;
        if ((j2 = s2.indexOf("<title>")) < 0) throw new Exception("cp #9");
        int k2;
        if ((k2 = s2.indexOf("/title>")) < 0) throw new Exception("cp #10");
        String s3 = s2.substring(j2 + 7, k2 - 1); // заголовок х-статуса...
        int l2;
        if ((l2 = s2.indexOf("<desc>")) < 0) throw new Exception("cp #11");
        int i3;
        if ((i3 = s2.indexOf("</desc>")) < 0) throw new Exception("cp #12");
        String s4 = s2.substring(l2 + 6, i3); // подпись х-статуса...

        ContactItem cItem = ContactList.getItembyUIN(s);
        if (cItem != null)
        {
            if (!(s3.length() < 1 && s4.length() < 1))
            {
                if (!cItem.getBooleanValue(ContactItem.CONTACTITEM_HAS_CHAT)) ChatHistory.newChatForm(cItem, cItem.name);
                ChatHistory.addTextToForm(s, s3, s4, "", 0, true, false, XStatus.getStatusImage(cItem.getXStatus().getStatusIndex()), 0);
                ContactList.enterContactMenu = false; // сброс флага, шоб меню контакта случайно не вылезло...
                if (cItem.openChat && !SplashCanvas.locked()) cItem.activate();
                // reset flag
                cItem.openChat = false;
            }
        }
	    return;
    }

    public static void a(String s, String s1, int i, long l1, long l2) throws Exception
    {
        ContactItem cItem = ContactList.getItembyUIN(s);

        MagicEye.addAction(s, "read_xtraz");

        if ((cItem == null) || (cItem.getInvisibleId() != 0) || !(Options.getBoolean(Options.OPTION_XTRAZ_ENABLE))
			|| cItem.getBooleanValue(ContactItem.CONTACTITEM_IS_TEMP) || ((int)Options.getInt(Options.OPTION_XSTATUS) == XStatus.XSTATUS_NONE)
			|| (Options.getInt(Options.OPTION_PRIVATE_STATUS) == OtherAction.PSTATUS_NONE)
			|| ((Options.getInt(Options.OPTION_PRIVATE_STATUS) == OtherAction.PSTATUS_VISIBLE_ONLY) && (cItem.getVisibleId() == 0))) return;

		int j = 0;
		int k;
        if ((k = s1.indexOf("<QUERY>")) < 0)
            throw new Exception("cp #2");
        int i1;
        if ((i1 = s1.indexOf("</QUERY>")) < 0)
            throw new Exception("cp #3");
        int j1;
        if ((j1 = s1.indexOf("<NOTIFY>")) < 0)
            throw new Exception("cp #4");
        int k1;
        if ((k1 = s1.indexOf("</NOTIFY>")) < 0)
            throw new Exception("cp #5");
        String s2;
        int i2;
        if ((i2 = (s2 = Util.DeMangleXml(s1.substring(k + 7, i1))).indexOf("<PluginID>")) < 0)
            throw new Exception("cp #6");
        int j2;
        if ((j2 = s2.indexOf("</PluginID>")) < 0)
            throw new Exception("cp #7");
        if (s2.substring(i2 + 10, j2).toLowerCase().compareTo("srvmng") != 0)
            throw new Exception("cp #8");
        String s3;
        if ((s3 = Util.DeMangleXml(s1.substring(j1 + 8, k1))).indexOf("AwayStat") < 0)
            throw new Exception("cp #9");
        int k2;
        if ((k2 = s3.indexOf("<senderId>")) < 0)
            throw new Exception("");
        int i3;
        if ((i3 = s3.indexOf("</senderId>")) < 0)
            throw new Exception("");
        if (s3.substring(k2 + 10, i3).compareTo(s) != 0)
            throw new Exception("incorrect uin");
        a(s, j, Options.getString(Options.OPTION_XTRAZ_TITLE), Options.getString(Options.OPTION_XTRAZ_MESSAGE), i, l1, l2); //Xtraz SM Title & Message!!
        return;
    }

    private static void a(String s, int i, String s1, String s2, int j, long l1, long l2) throws JimmException
    {
        byte abyte0[];
        String s3 = "<NR><RES>" + Util.MangleXml("<ret event='OnRemoteNotification'><srv><id>cAwaySrv</id><val srv_id='cAwaySrv'><Root><CASXtraSetAwayMessage></CASXtraSetAwayMessage><uin>" + Options.getString(Options.OPTION_UIN) + "</uin><index>1</index><title>" + s1 + "</title><desc>" + s2 + "</desc></Root></val></srv></ret>") + "</RES></NR>";
        abyte0 = a(s, j, l1, l2, s3);
        SnacPacket SnacPacket1 = new SnacPacket(4, 11, 0L, new byte[0], abyte0);
        Icq.c.sendPacket(SnacPacket1);
        return;
    }

    public static void a(String s, int ID) throws JimmException
    {
        SnacPacket SnacPacket1;
        String s1 = "<N><QUERY>" + Util.MangleXml("<Q><PluginID>srvMng</PluginID></Q>") + "</QUERY><NOTIFY>" + Util.MangleXml("<srv><id>cAwaySrv</id><req><id>AwayStat</id><trans>") + ID + Util.MangleXml("</trans><senderId>" + Options.getString(Options.OPTION_UIN) + "</senderId></req></srv>") + "</NOTIFY></N>";
        byte abyte0[] = b(s, Util.getCounter(), System.currentTimeMillis(), 0L, s1);
        SnacPacket1 = new SnacPacket(4, 6, 0L, new byte[0], abyte0);
        Icq.c.sendPacket(SnacPacket1);
        return;
     }

    private static byte[] a(String s, int i, long l1, long l2, String s1)
    {
        byte abyte0[] = new byte[0];
        int j = 0;
        try
        {
            ByteArrayOutputStream bytearrayoutputstream = new ByteArrayOutputStream();
            (new DataOutputStream(bytearrayoutputstream)).writeUTF(s1);
            j = (abyte0 = bytearrayoutputstream.toByteArray()).length - 2;
        }
        catch(Exception _ex) {}
        int k = 0;
        byte abyte1[];
        k = a(abyte1 = new byte[s.length() + 64 + (84 + (8 + j))], 0, s, l1, l2, i, (byte)26, (byte)0);
        k = a(abyte1, k);
        k = b(abyte1, k);
        Util.putWord(abyte1, k, j + 4, false);
        k += 4;
        Util.putWord(abyte1, k, j, false);
        k += 4;
        if (j > 0)
            System.arraycopy(abyte0, 2, abyte1, k, j);
        return abyte1;
    }

    private static byte[] b(String s, int i, long l1, long l2, String s1)
    {
        int j = 0;
        int k = 92 + s1.length();
        byte abyte0[];
        Util.putDWord(abyte0 = new byte[11 + s.length() + 95 + k + 4], 0, l1, false);
        Util.putDWord(abyte0, 4, l2, false);
        Util.putWord(abyte0, 8, 2);
        Util.putByte(abyte0, 10, s.length());
        System.arraycopy(Util.stringToByteArray(s), 0, abyte0, 11, s.length());
        j = 11 + s.length();
        j = a(abyte0, j, 55 + k, l1, l2, 1);
        j = a(abyte0, j, i, 0, 256, k);
        j = a(abyte0, j);
        j = b(abyte0, j);
        Util.putWord(abyte0, j, s1.length() + 4, false);
        j += 4;
        Util.putWord(abyte0, j, s1.length(), false);
        j += 4;
        System.arraycopy(Util.stringToByteArray(s1), 0, abyte0, j, s1.length());
        j += s1.length();
        Util.putDWord(abyte0, j, 0x30000L);
        return abyte0;
    }

    private static int a(byte abyte0[], int i, int j, long l1, long l2, int k)
    {
        Util.putWord(abyte0, i, 5);
        i += 2;
        Util.putWord(abyte0, i, 36 + j);
        i += 2;
        Util.putWord(abyte0, i, 0);
        i += 2;
        Util.putDWord(abyte0, i, l1, false);
        i += 4;
        Util.putDWord(abyte0, i, l2, false);
        i += 4;
        Util.putDWord(abyte0, i, 0x9461349L); //some unknown stuff...
        Util.putDWord(abyte0, i + 4, 0x4c7f11d1L);
        Util.putDWord(abyte0, i + 8, 0xffffffff82224445L);
        Util.putDWord(abyte0, i + 12, 0x53540000L);
        i += 16;
        Util.putDWord(abyte0, i, 0xa0002L);
        i += 4;
        Util.putDWord(abyte0, i, k);
        i += 2;
        Util.putDWord(abyte0, i, 0xf0000L);
        return i += 4;
    }

    private static int a(byte abyte0[], int i, String s, long l1, long l2, int j, 
            byte byte0, byte byte1)
    {
        Util.putDWord(abyte0, i, l1, false);
        i += 4;
        Util.putDWord(abyte0, i, l2, false);
        i += 4;
        Util.putWord(abyte0, i, 2);
        i += 2;
        Util.putByte(abyte0, i, s.length());
        i++;
        System.arraycopy(Util.stringToByteArray(s), 0, abyte0, i, s.length());
        i += s.length();
        Util.putWord(abyte0, i, 3);
        i += 2;
        Util.putWord(abyte0, i, 27, false);
        i += 2;
        Util.putWord(abyte0, i, 8);
        i++;
        Util.putDWord(abyte0, i, 0L);
        Util.putDWord(abyte0, i + 4, 0L);
        Util.putDWord(abyte0, i + 8, 0L);
        Util.putDWord(abyte0, i + 12, 0L);
        i += 16;
        Util.putDWord(abyte0, i, 3L);
        i += 4;
        Util.putDWord(abyte0, i, 4L);
        i += 4;
        Util.putWord(abyte0, i, j, false);
        i += 2;
        Util.putWord(abyte0, i, 14, false);
        i += 2;
        Util.putWord(abyte0, i, j, false);
        i += 2;
        Util.putDWord(abyte0, i, 0L);
        i += 4;
        Util.putDWord(abyte0, i, 0L);
        i += 4;
        Util.putDWord(abyte0, i, 0L);
        i += 4;
        Util.putByte(abyte0, i, byte0);
        i++;
        Util.putByte(abyte0, i, byte1);
        i++;
        Util.putWord(abyte0, i, 0, false);
        i += 2;
        Util.putWord(abyte0, i, 0);
        return i += 2;
    }

    private static int a(byte abyte0[], int i, int j, int k, int i1, int j1)
    {
        Util.putWord(abyte0, i, 10001);
        i += 2;
        Util.putWord(abyte0, i, 51 + j1);
        i += 2;
        Util.putWord(abyte0, i, 27, false);
        i += 2;
        Util.putByte(abyte0, i, 8);
        i++;
        Util.putDWord(abyte0, i, 0L);
        Util.putDWord(abyte0, i + 4, 0L);
        Util.putDWord(abyte0, i + 8, 0L);
        Util.putDWord(abyte0, i + 12, 0L);
        i += 16;
        Util.putDWord(abyte0, i, 3L);
        i += 4;
        Util.putDWord(abyte0, i, 0L);
        i += 4;
        Util.putWord(abyte0, i, j, false);
        i += 2;
        Util.putWord(abyte0, i, 14, false);
        i += 2;
        Util.putWord(abyte0, i, j, false);
        i += 2;
        Util.putDWord(abyte0, i, 0L);
        i += 4;
        Util.putDWord(abyte0, i, 0L);
        i += 4;
        Util.putDWord(abyte0, i, 0L);
        i += 4;
        Util.putByte(abyte0, i, 26);
        i++;
        Util.putByte(abyte0, i, 0);
        i++;
        Util.putWord(abyte0, i, k, false);
        i += 2;
        Util.putWord(abyte0, i, i1);
        return i += 2;
    }

    private static int a(byte abyte0[], int i)
    {
        Util.putWord(abyte0, i, 1, false);
        i += 2;
        Util.putByte(abyte0, i, 0);
        return ++i;
    }

    private static int b(byte abyte0[], int i)
    {
        Util.putWord(abyte0, i, 79, false);
        i += 2;
        Util.putDWord(abyte0, i, 0x3b60b3efL); // unknown stuff..
        Util.putDWord(abyte0, i + 4, 0xffffffffd82a6c45L);
        Util.putDWord(abyte0, i + 8, 0xffffffffa4e09c5aL);
        Util.putDWord(abyte0, i + 12, 0x5e67e865L);
        i += 16;
        Util.putWord(abyte0, i, 8, false);
        i += 2;
        Util.putDWord(abyte0, i, 42L, false);
        i += 4;
        System.arraycopy(Util.stringToByteArray("Script Plug-in: Remote Notification Arrive"), 0, abyte0, i, 42);
        i += 42;
        Util.putDWord(abyte0, i, 256L);
        i += 4;
        Util.putDWord(abyte0, i, 0L);
        i += 4;
        Util.putDWord(abyte0, i, 0L);
        i += 4;
        Util.putWord(abyte0, i, 0);
        i += 2;
        Util.putByte(abyte0, i, 0);
        return ++i;
    }
}