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
 File: src/jimm/comm/OtherAction.java
 Version: ###VERSION###  Date: ###DATE###
 Author(s): aspro
 *******************************************************************************/

package jimm.comm;

import java.util.Vector;
import jimm.*;
import jimm.util.*;
import jimm.comm.*;

public class OtherAction 
{
    /** Creates a new instance of OtherAction */
    public OtherAction() {}

    private static GUID CAP_JIMM;

    private static final GUID CAP_UNKNOWN       = new GUID(Util.explodeToBytes("09,46,00,00,4c,7f,11,d1,82,22,44,45,53,54,00,00", ',', 16));
    private static final GUID CAP_QIP_PRTMESS   = new GUID(Util.explodeToBytes("d3,d4,53,19,8b,32,40,3b,ac,c7,d1,a9,e2,b5,81,3e", ',', 16));
    private static final GUID CAP_VOICE_CHAT    = new GUID(Util.explodeToBytes("b9,97,08,b5,3a,92,42,02,b0,69,f1,e7,57,bb,2e,17", ',', 16));
    private static final GUID CAP_XTRAZ_CHAT    = new GUID(Util.explodeToBytes("67,36,15,15,61,2d,4c,07,8f,3d,bd,e6,40,8e,a0,41", ',', 16));
    private static final GUID CAP_TZERS         = new GUID(Util.explodeToBytes("b2,ec,8f,16,7c,6f,45,1b,bd,79,dc,58,49,78,88,b9", ',', 16));
    private static final GUID CAP_ICQLITE       = new GUID(Util.explodeToBytes("17,8c,2d,9b,da,a5,45,bb,8d,db,f3,bd,bd,53,a1,0a", ',', 16));
    private static final GUID CAP_ICQLIVE_AUDIO = new GUID(Util.explodeToBytes("09,46,01,04,4c,7f,11,d1,82,22,44,45,53,54,00,00", ',', 16));
    private static final GUID CAP_ICQLIVE_VIDEO = new GUID(Util.explodeToBytes("09,46,01,01,4c,7f,11,d1,82,22,44,45,53,54,00,00", ',', 16));
//    private static final GUID CAP_CORE_PAGER    = new GUID(Util.explodeToBytes("43,4f,52,45,20,50,61,67,65,72,00,00,00,00,00,00", ',', 16));
//    private static final GUID CAP_PUSH2TALK     = new GUID(Util.explodeToBytes("e3,62,c1,e9,12,1a,4b,94,a6,26,7a,74,de,24,27,0d", ',', 16));

    public static void setUserInfo(GUID[] guids) throws JimmException
    {
        // Send a CLI_SETUSERINFO packet
        // Set version information to this packet in our capability
        byte[] packet = new byte[guids.length * 16 + 4];
        packet[0] = 0x00;
        packet[1] = 0x05;
        packet[2] = (byte)((guids.length * 16) / 0x100);
        packet[3] = (byte)((guids.length * 16) % 0x100);
        for (int i = 0; i < guids.length; i++) 
        {
            System.arraycopy(guids[i].toByteArray(), 0, packet, i * 16 + 4, 16);
        }
        Icq.c.sendPacket(new SnacPacket(SnacPacket.LOCATION_FAMILY, SnacPacket.CLI_SETUSERINFO_COMMAND, 0, new byte[0], packet));
    }

   /*********************************************************************************************************/
    public static void setClientGUID(Vector guids) 
    {
        // init CAP_JIMM
        byte[] guid = Util.CAP_JIMM;
        byte[] ver = Util.stringToByteArray(Options.getString(Options.OPTION_STRING_VERSION));

        System.arraycopy(ver, 0, guid, 5, ver.length <= 11 ? ver.length : 11);
        CAP_JIMM = new GUID(guid);

        int client = Options.getInt(Options.OPTION_CLIENT_ID);
        switch (client) 
        {
            case  7: guids.addElement(CAP_JIMM);
                      break; // Jimm
            case  1: guids.addElement(new GUID(Util.CAP_MIRANDAIM));
                      guids.addElement(new GUID(Util.CAP_AVATAR));
                      break; // Miranda
            case  0: guids.addElement(new GUID(Util.CAP_QIP));
                      guids.addElement(CAP_QIP_PRTMESS);
                      guids.addElement(new GUID(Util.CAP_AVATAR));
                      break; // QIP 2005a
            case 12: guids.addElement(new GUID(Util.CAP_QIPPDASYM));
                      break; // QIP PDA (Symbian)
            case 13: guids.addElement(new GUID(Util.CAP_QIPPDAWIN));
                      break; // QIP PDA (Windows)
            case 14: guids.addElement(new GUID(Util.CAP_QIPINFIUM));
                      guids.addElement(new GUID(Util.CAP_AVATAR));
                      break; // QIP Infium
            case 18: guids.addElement(new GUID(Util.CAP_RICHTEXT));
                      guids.addElement(new GUID(Util.CAP_AVATAR));
                      guids.addElement(CAP_TZERS);
                      guids.addElement(CAP_VOICE_CHAT);
                      guids.addElement(new GUID(Util.CAP_XTRAZ));
                      guids.addElement(CAP_XTRAZ_CHAT);
                      break; //ICQ 5.1
            case 15: guids.addElement(CAP_XTRAZ_CHAT);
                      guids.addElement(new GUID(Util.CAP_RICHTEXT));
                      guids.addElement(new GUID(Util.CAP_AVATAR));
                      guids.addElement(CAP_TZERS);
                      guids.addElement(new GUID(Util.CAP_ICQ6));
                      guids.addElement(CAP_ICQLITE);
                      guids.addElement(CAP_ICQLIVE_AUDIO);
                      guids.addElement(CAP_ICQLIVE_VIDEO);
                      break; // ICQ 6
            case  8: break; // stICQ
            case 11: guids.addElement(new GUID(Util.CAP_VMICQ));
                      break; // VmICQ
            case 21: guids.addElement(new GUID(Util.CAP_MCHAT));
                      break; // mChat
            case  2: guids.addElement(new GUID(Util.CAP_ANDRQ));
                      guids.addElement(new GUID(Util.CAP_AVATAR));
                      break; // &RQ
            case  3: guids.addElement(new GUID(Util.CAP_XTRAZ));
                      break; // XTraz for R&Q
            case  6: guids.addElement(new GUID(Util.CAP_KOPETE));
                      break; // Kopete
            case 22: guids.addElement(new GUID(Util.CAP_MACICQ));
                      break; // ICQ for MAC
//          case 15: break; // Unknown
        }
    }

    public static void setClientId() 
    {
        int client = Options.getInt(Options.OPTION_CLIENT_ID);
        long fp1 = 0xFFFFFFFE;
        long fp2 = 0x00010000;
        long fp3 = 0xFFFFFFFE;
        int prot = 8;
        
        switch (client) 
        {
            case  7: fp1 = 0xFFFFFFFE;  fp2 = 0x21062008; fp3 = 0xFFFFFFFE; prot =  Options.getInt(Options.OPTION_PROT_VERSION); break; // Jimm
            case  1: fp1 = 0xFFFFFFFF;  fp2 = 0x00030807; fp3 = 0xFFFFFFFF; prot =  8; break; // Miranda
            case  0: fp1 = 0x08000600;  fp2 = 0x0000000E; fp3 = 0x0000000F; prot = 11; break; // QIP 2005a
            case 12: fp1 = 0x00000000;  fp2 = 0x00000000; fp3 = 0x00000000; prot = 11; break; // QIP PDA (Symbian)
            case 13: fp1 = 0x00000000;  fp2 = 0x00000000; fp3 = 0x00000000; prot = 11; break; // QIP PDA (Windows)
            case 14: fp1 = 0x00002330;  fp2 = 0x00000000; fp3 = 0x00000000; prot = 11; break; // QIP Infium
            case 18: fp1 = 0x00000000;  fp2 = 0x00000000; fp3 = 0x00000000; prot =  9; break; // ICQ 5.1
            case 15: fp1 = 0x00000000;  fp2 = 0x00000000; fp3 = 0x00000000; prot =  9; break; // ICQ 6   
            case  8: fp1 = 0x3BA8DBAF;  fp2 = 0x3BEB5373; fp3 = 0x3BEB5262; prot =  2; break; // stICQ
            case 11: fp1 = 0x00000000;  fp2 = 0x00000000; fp3 = 0x00000000; prot =  0; break; // VmICQ
            case 21: fp1 = 0x00000000;  fp2 = 0x00000000; fp3 = 0x00000000; prot =  0; break; // mChat
            case  2: fp1 = 0xFFFFFF7F;  fp2 = 0x00090808; fp3 = 0x00000000; prot =  7; break; // &RQ
            case  3: fp1 = 0xFFFFF666;  fp2 = 0x0000044F; fp3 = 0x00000000; prot =  9; break; // R&Q
            case  6: fp1 = 0x00000000;  fp2 = 0x00000000; fp3 = 0x00000000; prot =  8; break; // Kopete
            case 22: fp1 = 0x00000000;  fp2 = 0x00000000; fp3 = 0x00000000; prot =  7; break; // Mac ICQ
//          case 15: fp1 = 0xFF7777FF;  fp2 = 0x0000142C; fp3 = 0x00000000; prot =  9; break; // Unknown
        }
        Util.putWord (CLI_SETSTATUS_DATA, 21, prot, true);
        Util.putDWord(CLI_SETSTATUS_DATA, 35,  fp1, true);
        Util.putDWord(CLI_SETSTATUS_DATA, 39,  fp2, true);
        Util.putDWord(CLI_SETSTATUS_DATA, 43,  fp3, true);
    }
   /*********************************************************************************************************/

    public static void setStandartUserInfo() throws JimmException 
    {
        GUID[] standart = new GUID[]
        {
            new GUID(Util.CAP_AIM_SERVERRELAY),
            new GUID(Util.CAP_DIRECT),
            CAP_UNKNOWN
        };

        Vector guids = new Vector();
        setClientGUID(guids);
        /*if (!Options.getBoolean(Options.OPTION_CP1251_HACK))*/ guids.addElement(new GUID(Util.CAP_UTF8));
        //#sijapp cond.if modules_FILES="true" #
        guids.addElement(new GUID(Util.CAP_AIMFILE));
        //#sijapp cond.end #
        //#sijapp cond.if target isnot "DEFAULT"#
        if (Options.getInt(Options.OPTION_TYPING_NOTIF_MODE) > 0) guids.addElement(new GUID(Util.CAP_TYPING));
        //#sijapp cond.end#
        GUID xstatus = XStatus.getStatusGUID(Options.getInt(Options.OPTION_XSTATUS));
        if (xstatus != null && MainMenu.getXStatusImage() != null)
        {
            guids.addElement(xstatus);
            if (Options.getBoolean(Options.OPTION_XTRAZ_ENABLE)) guids.addElement(new GUID(Util.CAP_XTRAZ));
        }
        GUID[] result = new GUID[guids.size() + standart.length];
        guids.copyInto(result);
        System.arraycopy(standart, 0, result, guids.size(), standart.length);
        setUserInfo(result);
    }

    // CLI_SETSTATUS packet data
    public static byte[] CLI_SETSTATUS_DATA =
    	Util.explodeToBytes
    	(
    		"00,06,00,04,"+
    		"11,00,00,00,"+ // Online status
    		"00,0C,00,25,"+ // TLV(C)
    		"C0,A8,00,01,"+ // 192.168.0.1, cannot get own IP address
    		"00,00,AB,CD,"+ // Port 43981
    		"00,"+          // Firewall
    		"00,08,"+       // Supports protocol version 8
    		"00,00,00,00,"+
    		"00,00,00,50,"+
    		"00,00,00,03,"+
    		"FF,FF,FF,FE,"+ // Timestamp 1
    		"00,01,00,00,"+ // Timestamp 2
    		"FF,FF,FF,FE,"+ // Timestamp 3
    		"00,00", 
    		',', 16
    	);

    public static void setStatus(int status) throws JimmException 
    {
        // Send a CLI_SETSTATUS packet
        setClientId();
        Util.putDWord(CLI_SETSTATUS_DATA, 4, status);
        Icq.c.sendPacket(new SnacPacket(SnacPacket.SERVICE_FAMILY, SnacPacket.CLI_SETSTATUS_COMMAND, 0, new byte[0], CLI_SETSTATUS_DATA));
    }

	public static final byte PSTATUS_ALL           = 0x01;
	public static final byte PSTATUS_NONE          = 0x02;
	public static final byte PSTATUS_VISIBLE_ONLY  = 0x03;
	public static final byte PSTATUS_NOT_INVISIBLE = 0x04;
	public static final byte PSTATUS_CL_ONLY       = 0x05;

	static public void setPrivateStatus(byte status) throws jimm.JimmException
	{
		int id = Icq.getIcq().getPrivateStatusId();
        int cmd = SnacPacket.CLI_ROSTERUPDATE_COMMAND;

        if (id == 0)
        {
            id = Util.createRandomId();
            cmd = SnacPacket.CLI_ROSTERADD_COMMAND;
            Icq.getIcq().setPrivateStatusId(id);
        }

		byte[] buf = new byte[15];
		int marker = 0;

		Util.putWord(buf, marker,      0); marker += 2; // name (null)
		Util.putWord(buf, marker,      0); marker += 2; // GroupID
		Util.putWord(buf, marker,     id); marker += 2; // EntryID
		Util.putWord(buf, marker,      4); marker += 2; // EntryType
		Util.putWord(buf, marker,      5); marker += 2; // Length in bytes of following TLV
		Util.putWord(buf, marker,   0xCA); marker += 2; // TLV Type
		Util.putWord(buf, marker,	   1); marker += 2; // TLV Length
		Util.putByte(buf, marker, status);              // TLV Value
/*
		byte[] buf = new byte[43];
		int marker = 0;

		Util.putWord(buf, marker,      0); marker += 2; // name (null)
		Util.putWord(buf, marker,      0); marker += 2; // GroupID
		Util.putWord(buf, marker,     id); marker += 2; // EntryID
		Util.putWord(buf, marker,      4); marker += 2; // EntryType
		Util.putWord(buf, marker,     33); marker += 2; // Length in bytes of following TLV
		Util.putWord(buf, marker,   0xCA); marker += 2; // TLV Type
		Util.putWord(buf, marker,      1); marker += 2; // TLV Length
		Util.putByte(buf, marker, status); marker += 1; // TLV Value
		Util.putWord(buf, marker,   0xD0); marker += 3;
		Util.putWord(buf, marker, 0x0101); marker += 2;
		Util.putWord(buf, marker,   0xD1); marker += 3;
		Util.putWord(buf, marker, 0x0101); marker += 2;
		Util.putWord(buf, marker,   0xD2); marker += 3;
		Util.putWord(buf, marker, 0x0101); marker += 2;
		Util.putWord(buf, marker,   0xD3); marker += 3;
		Util.putWord(buf, marker, 0x0101); marker += 2;
		Util.putWord(buf, marker,   0xCB); marker += 2;
		Util.putWord(buf, marker,      4); marker += 2;
		Util.putWord(buf, marker, 0xFFFF); marker += 2;
		Util.putWord(buf, marker, 0xFFFF);
*/
		Icq.c.sendPacket(new SnacPacket(SnacPacket.ROSTER_FAMILY, cmd, 0, new byte[0], buf));
	}
}
