/*******************************************************************************
 Jimm - Mobile Messaging - J2ME ICQ clone
 Copyright (C) 2003-06  Jimm Project

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
 File: src/jimm/comm/ActionListener.java
 Version: ###VERSION###  Date: ###DATE###
 Author(s): Manuel Linsmayer, Andreas Rossbacher, Spassky Alexander, Igor Palkin
 *******************************************************************************/

package jimm.comm;

import java.util.*;
import javax.microedition.lcdui.*;

import jimm.*;
import jimm.util.*;

public class ActionListener
{
    /** ************************************************************************* */
    
    // Forwards received packet
    protected void forward(Packet packet) throws JimmException
    {
        // Watch out for channel 4 (Disconnect) packets
        if (packet instanceof DisconnectPacket)
        {
            DisconnectPacket disconnectPacket = (DisconnectPacket) packet;

            // Throw exception
            if (disconnectPacket.getError() == 0x0001)
            {
                Icq.connecting = false;
                throw (new JimmException(110, 0)); // Multiple logins
            }
            else
            {
                throw (new JimmException(100, 0)); // Unknown error
            }
        }

        /** *********************************************************************** */

        // Watch out for channel 2 (SNAC) packets
        if (packet instanceof SnacPacket)
        {
            SnacPacket snacPacket = (SnacPacket) packet;

	        // Typing notify
            //#sijapp cond.if target isnot "DEFAULT"#
	    	if ((snacPacket.getFamily() == SnacPacket.ICBM_FAMILY) && (snacPacket.getCommand() == 0x0014) 
					&& (Options.getInt(Options.OPTION_TYPING_NOTIF_MODE) > 0))
	    	{
			    byte[] p = snacPacket.getData();
			    int uin_len = Util.getByte(p, 10);
			    String uin = Util.byteArrayToString(p, 11, uin_len);
			    int flag = Util.getWord(p, 11 + uin_len);
			    
			    if (flag == 0x0002) RunnableImpl.BeginTyping(uin, true); //Begin typing
			    else RunnableImpl.BeginTyping(uin, false); //End typing
			}
	    	//#sijapp cond.end#
	
        // Watch out for CLI_ACKMSG_COMMAND packets
		if ((snacPacket.getFamily() == SnacPacket.ICBM_FAMILY)
				&& (snacPacket.getCommand() == SnacPacket.CLI_ACKMSG_COMMAND))
        {
			byte abyte3[];
			abyte3 = snacPacket.getData();
			int j5 = Util.getByte(abyte3, 10); //UIN LEN
			String s6 = Util.byteArrayToString(abyte3, 11, j5); //UIN
			long msgId = (Util.getDWord(abyte3, 0) << 32) + Util.getDWord(abyte3, 4);
			ChatHistory.AckMessage(s6, msgId, true);
			int j1 = 11 + j5;
			j1 += 47; // (2+2+1+16+4+4+2+2+2+4+4+4)

			if (j1 < abyte3.length)
			{
                // Get message type
                int msgType = Util.getWord(abyte3, j1, false);
                // Util.showBytes(abyte3);

                ContactItem contact = ContactList.getItembyUIN(s6);

                if ((msgType >= Message.MESSAGE_TYPE_AWAY) && (msgType <= Message.MESSAGE_TYPE_FFC) && contact.readStatusMess)
                {
                    // Create an message entry
                    int textLen = Util.getWord(abyte3, 64 + j5,false);
					if (!contact.getBooleanValue(ContactItem.CONTACTITEM_HAS_CHAT)) ChatHistory.newChatForm(contact, contact.name);
					ChatHistory.addTextToForm(s6, "", Util.byteArrayToString(abyte3, 66 + j5, textLen, false), "", 0, true, false, ContactList.imageList.elementAt(contact.getImageIndex()), 0);
					ContactList.enterContactMenu = false; // сборос флага, шоб меню контакта случайно не вылезло...
					if (!SplashCanvas.locked()) contact.activate();
                    //reset flag
                    contact.readStatusMess = false;
                }

				int k6 = Util.getWord(abyte3, j1, false); //00 1a - unknown...msg type?
				j1 += 6; // (2+2+2)
				if (k6 == 26)
				{
					j1 += 3; // (2+1)
					int j7 = Util.getWord(abyte3, j1, false); //00 4f
					j1 += 2;
					if (j7 != 79) throw (new JimmException(509, 1, false));
					j1 += 18; // (16+2)
					long l12 = Util.getDWord(abyte3, j1, false);
					j1 += 4;
					String s13 = Util.byteArrayToString(abyte3, j1, (int)l12);
					j1 = (int)((long)j1 + l12);
					j1 += 15; // (4+4+4+2+1)
					if(s13 != null && s13.compareTo("Script Plug-in: Remote Notification Arrive") == 0)
					{
						j1 += 8; // (4+4)
						int l14 = (abyte3.length) - j1;
						String s14 = Util.byteArrayToString(abyte3, j1, l14, true);
						// System.out.println("Message is: " + s14);
						try
						{
								XtrazSM.b(s6, s14);
						}
						catch (Exception e) {}
					}
				}
			}
		}

            // Watch out for SRV_USERONLINE packets
            if (((snacPacket.getFamily() == SnacPacket.CONTACT_FAMILY) 
                    && (snacPacket.getCommand() == SnacPacket.SRV_USERONLINE_COMMAND))
                    || ((snacPacket.getFamily() == 0x0002) && (snacPacket.getCommand() == 0x0006)))
            {
                // #sijapp cond.if target is "MIDP2" | target is "MOTOROLA" | target is "SIEMENS2"#
                // #sijapp cond.if modules_FILES is "true"#
                // DC variables
                byte[] internalIP = new byte[4];
                byte[] externalIP = new byte[4];
                int dcPort = 0;
                int dcType = -1;
                int icqProt = 0;
                int authCookie = 0;
                // #sijapp cond.end#
                // #sijapp cond.end#
                
                boolean statusChange = true;
                boolean checkStatus = ((snacPacket.getFamily() == 0x0002) && (snacPacket.getCommand() == 0x0006));
                int dwFT1 = 0, dwFT2 = 0, dwFT3 = 0;
                int wVersion = 0;
                byte[] capabilities_old = null; // Buffer for old style capabilities (TLV 0x000D)
                byte[] capabilities_new = null; // Buffer for new style capabilities (TLV 0x0019)
                
                // Time variables
                int idle = -1;
                int online = -1;
                int signon = -1;
                int regdata = -1;

                // Get data
                byte[] buf = snacPacket.getData();

                // Get UIN of the contact changing status
                int uinLen = Util.getByte(buf, 0);
                String uin = Util.byteArrayToString(buf, 1, uinLen);

                // Get new status and client capabilities
                int status = checkStatus ? ContactList.STATUS_OFFLINE : ContactList.STATUS_ONLINE;
                int xstatusIndex = -1;
                int xstatusMod = -1;
                String xstatusModTitle = "";

                int marker = 1 + uinLen + 2;
                int tlvNum = Util.getWord(buf, marker);
                marker += 2;

                for (int i = 0; i < tlvNum; i++)
                {
                    int tlvType = Util.getWord(buf, marker);
                    byte[] tlvData = Util.getTlv(buf, marker);

                    if (tlvType == 0x0006) // STATUS
                    {
                        status = (int)Util.getDWord(tlvData, 0);
                    }
                    else if (tlvType == 0x000D) // Old style CAPABILITIES
                    {
                        capabilities_old = tlvData;
                    } 
                    else if (tlvType == 0x0019) // New style CAPABILITIES
                    {
                        capabilities_new = tlvData;
                    }
                    // #sijapp cond.if target is "MIDP2" | target is "MOTOROLA"  | target is "SIEMENS2"#
                    // #sijapp cond.if modules_FILES is "true"#
                    
                    else if (tlvType == 0x000A) // External IP
                    {
                        System.arraycopy(tlvData, 0, externalIP, 0, 4);
                    }
                    else if (tlvType == 0x000c) // DC Infos
                    {
                        // dcMarker
                        int dcMarker = 0;
                        
                        // Get internal IP
                        System.arraycopy(tlvData,dcMarker, internalIP, 0, 4);
                        dcMarker += 4;
                        
                        // Get tcp port
                        dcPort = (int)Util.getDWord(tlvData, dcMarker);
                        dcMarker += 4;
                        
                        // Get DC type
                        dcType = Util.getByte(tlvData, dcMarker);
                        dcMarker ++;
                        
                        // Get protocol version
                        icqProt = Util.getWord(tlvData, dcMarker);
                        dcMarker += 2;
                        
                        // Get auth cookie
                        authCookie = (int)Util.getDWord(tlvData,dcMarker);
                        dcMarker +=12;
                        
                        // Get data for client detection
                        dwFT1 = (int) Util.getDWord(tlvData,dcMarker);
                        dcMarker += 4;
                        dwFT2 = (int) Util.getDWord(tlvData,dcMarker);
                        dcMarker += 4;
                        dwFT3 = (int) Util.getDWord(tlvData,dcMarker);
                        
                        statusChange = false;
                    }
                    // #sijapp cond.end#
                    // #sijapp cond.end#
                    else if (tlvType == 0x0003) // Signon time
                    {
                    	signon = (int)Util.gmtTimeToLocalTime(Util.byteArrayToLong(tlvData));
                    }
                    else if (tlvType == 0x0004) // Idle time
                    {
                    	idle = (int)Util.byteArrayToLong(tlvData) / 256;
                    }
                    else if (tlvType == 0x000F) // Online time
                    {
                    	online = (int)Util.byteArrayToLong(tlvData);
                    }
                    else if (tlvType == 0x001D) // new style xStatusand xTraz message
                    {
                        int ps = 0;
                        int len = tlvData.length;

                        while (ps < len - 1)
                        {
                            int ExtTlv = Util.getWord(tlvData, ps);
                            ps += 3;
                            int ExtTlvLen = Util.getByte(tlvData, ps);
                            int idx = ps;
                            if (ExtTlvLen > 0)
                            {
                                switch (ExtTlv)
                                {
                                    case 0x0e:
                                        String xtrazTit = Util.byteArrayToString(tlvData, idx + 8, ExtTlvLen - 7);
                                        xstatusMod = Integer.parseInt(xtrazTit);
                                        break;
                                    case 0x02:
                                        idx++;
                                        int textLen = (int)Util.getWord(tlvData, idx);
                                        idx += 2;
                                        xstatusModTitle = Util.byteArrayToString(tlvData, idx, textLen, true);
                                        break;
                                }
                            }
                            ps += ExtTlvLen + 1;
                        }
                    }
                    else if (tlvType == 0x0005) // Reg. data time
                    {
                        regdata = (int)Util.byteArrayToLong(tlvData);
                    }
                    
                    marker += 2 + 2 + tlvData.length;
                }

                // Update contact list
                ContactItem item = ContactList.getItembyUIN(uin);
                if (item != null)
                {
                    xstatusIndex = item.getXStatus().getStatusIndex();
                    byte[] capsArray = Util.mergeCapabilities(capabilities_old, capabilities_new);
                    item.setXStatus(capsArray);
                    item.setHappyFlag((status >> 16) & 0xFFFF);

					if (xstatusMod != -1) // new style xStatuses (ICQ6)
					{
						item.getXStatus().setStatusIndex(xstatusMod);

						if (!(xstatusModTitle.length() < 1) && (item.getBooleanValue(ContactItem.CONTACTITEM_HAS_CHAT)))
						{
							ChatHistory.addTextToForm(uin, "", xstatusModTitle, "", 0, true, false, XStatus.getStatusImage(xstatusMod), 0);
						}
					}
					item.setStatusImage();
				}

				if (checkStatus)
				{
					int statusIndex = Util.translateStatusReceived(status);
					Alert statusAlert = new Alert(uin, JimmUI.getStatusString(statusIndex), null, null);
					//#sijapp cond.if target is "MIDP2"#
					statusAlert.setTimeout(Jimm.is_phone_SE() ? Alert.FOREVER : 3000);
					//#sijapp cond.else#
					statusAlert.setTimeout(3000);
					//#sijapp cond.end#
					Jimm.display.setCurrent(statusAlert);
				}

				// Update contact list
				//#sijapp cond.if (target="MIDP2" | target="MOTOROLA" | target="SIEMENS2") & modules_FILES="true" #
				Util.detectUserClient(uin, dwFT1, dwFT2, dwFT3, Util.mergeCapabilities(capabilities_old, capabilities_new), icqProt, statusChange);
				RunnableImpl.updateContactList(uin, status, xstatusIndex, internalIP, externalIP, dcPort, dcType, icqProt, authCookie, signon, online, idle, regdata);
				//#sijapp cond.else#
				RunnableImpl.updateContactList(uin, status, xstatusIndex, null, null, 0, 0, 0, 0, signon, online,idle, regdata);
				//#sijapp cond.end#
			}

            /** ********************************************************************* */

            // Watch out for SRV_USEROFFLINE packets
            if ((snacPacket.getFamily() == SnacPacket.CONTACT_FAMILY)
                    && (snacPacket.getCommand() == SnacPacket.SRV_USEROFFLINE_COMMAND))
            {
                // Get raw data
                byte[] buf = snacPacket.getData();

                // Get UIN of the contact that goes offline
                int uinLen = Util.getByte(buf, 0);
                String uin = Util.byteArrayToString(buf, 1, uinLen);

                // Update contact list
                RunnableImpl.callSerially(RunnableImpl.TYPE_USER_OFFLINE, uin);
            }

            /** ********************************************************************* */
            
			// Watch out for SRV_ACKMSG
            else if ((snacPacket.getFamily() == SnacPacket.ICBM_FAMILY)
                    && (snacPacket.getCommand() == SnacPacket.SRV_ACKMSG_COMMAND))
            {
				// Get data
                byte[] buf = snacPacket.getData();

                if (buf.length > 11)
                {
                    // Get msgId
                    long msgId = (Util.getDWord(buf, 0) << 32) + Util.getDWord(buf, 4);

                    // Get UIN of the contact changing status
                    int uinLen = Util.getByte(buf, 10);
                    String uin = Util.byteArrayToString(buf, 11, uinLen);

                    ChatHistory.AckMessage(uin, msgId, false);
                }
            }

            /* ********************************************************************** */            

            // Watch out for SRV_RECVMSG
            else if ((snacPacket.getFamily() == SnacPacket.ICBM_FAMILY)
                    && (snacPacket.getCommand() == SnacPacket.SRV_RECVMSG_COMMAND))
            {
                // Get raw data, initialize marker
                byte[] buf = snacPacket.getData();
                int marker = 0;

                // Check length
                if (buf.length < 11) { throw (new JimmException(150, 0, false)); }

             /****************************ICQ XTraz Support****************************/
                long l3 = Util.getDWord(buf, 0, false);
                long l5 = Util.getDWord(buf, 4, false);
             /*************************************************************************/

                // Get message format
                marker += 8;
                int format = Util.getWord(buf, marker);
                marker += 2;

                // Get UIN length
                int uinLen = Util.getByte(buf, marker);
                marker += 1;

                // Check length
                if (buf.length < marker + uinLen + 4) { throw (new JimmException(150, 1, false)); }

                // Get UIN
                String uin = Util.byteArrayToString(buf, marker, uinLen);
                marker += uinLen;

                // Skip WARNING
                marker += 2;

                // Skip TLVS
                int tlvCount = Util.getWord(buf, marker);
                marker += 2;
                for (int i = 0; i < tlvCount; i++)
                {
                    byte[] tlvData = Util.getTlv(buf, marker);
                    if (tlvData == null) { throw (new JimmException(150, 2, false)); }

                    /**************************************************************/
//                    int tlvType = Util.getWord(buf, marker);
//                    // user status
//                    if (tlvType == 0x0006)
//                    {
//                        ContactItem item = ContactList.getItembyUIN(uin);
//                        long status = Util.getDWord(tlvData, 0);
//                        if (item != null && !item.getBooleanValue(ContactItem.CONTACTITEM_IS_TEMP)) 
//                            item.setIntValue(ContactItem.CONTACTITEM_STATUS, Util.translateStatusReceived(status));
//                    }
                    /*************************************************************/

                    marker += 4 + tlvData.length;
                }

                // Get message data and initialize marker
                byte[] msgBuf;
                int tlvType;
                do
                {
                    msgBuf = Util.getTlv(buf, marker);
                    if (msgBuf == null) { throw (new JimmException(150, 3, false)); }
                    tlvType = Util.getWord(buf, marker);
                    marker += 4 + msgBuf.length;
                } while ((tlvType != 0x0002) && (tlvType != 0x0005));
                int msgMarker = 0;

                //////////////////////
                // Message format 1 //
                //////////////////////
                if (format == 0x0001)
                {
                    // Variables for all possible TLVs
                    // byte[] capabilities = null;
                    byte[] message = null;

                    // Read all TLVs
                    while (msgMarker < msgBuf.length)
                    {
                        // Get next TLV
                        byte[] tlvValue = Util.getTlv(msgBuf, msgMarker);
                        if (tlvValue == null) { throw (new JimmException(151, 0, false)); }

                        // Get type of next TLV
                        tlvType = Util.getWord(msgBuf, msgMarker);

                        // Update markers
                        msgMarker += 4 + tlvValue.length;

                        // Save value
                        switch (tlvType)
                        {
                        case 0x0501:
                            // capabilities
                            // capabilities = tlvValue;
                            break;
                        case 0x0101:
                            // message
                            message = tlvValue;
                            break;
                        //default: // из-за этого не приходили сообщения с QIP PDA (Windows)
                        //    throw (new JimmException(151, 1, false));
                        }
                    }

                    // Process packet if at least the message TLV was present
                    if (message != null)
                    {
                        // Check length of message
                        if (message.length < 4) { throw (new JimmException(151, 2, false)); }

                        // Get message text
                        String text;
                        if (Util.getWord(message, 0) == 0x0002)
                        {
                            text = Util.removeCr(Util.ucs2beByteArrayToString(message, 4, message.length - 4));
                        } else
                        {
                            text = Util.removeCr(Util.byteArrayToString(message, 4, message.length - 4));
                        }

                        // Construct object which encapsulates the received plain message
						PlainMessage plainMsg = new PlainMessage(uin, Options.getString(Options.OPTION_UIN), Util.createCurrentDate(false), text, false);
						RunnableImpl.addMessageSerially(plainMsg);
						
						ContactItem contact = ContactList.getItembyUIN(uin);
						if ((Options.getBoolean(Options.OPTION_AUTO_ANSWER)) && (contact != null))
						{
							sendAutoMessage(contact);
						}
					}
				}
                //////////////////////
                // Message format 2 //
                //////////////////////
                else if (format == 0x0002)
                {
        			// TLV(A): Acktype 0x0000 - normal message
        			//                 0x0001 - file request / abort request
        			//                 0x0002 - file ack
                    
                    // Check length
                    if (msgBuf.length < 10) { throw (new JimmException(152, 0, false)); }

                    // Get and validate SUB_MSG_TYPE2.COMMAND
                    int command = Util.getWord(msgBuf, msgMarker);
                    if (command != 0x0000) return; // Only normal messages are
                                                    // supported yet
                    msgMarker += 2;

                    // Skip SUB_MSG_TYPE2.TIME and SUB_MSG_TYPE2.ID
                    msgMarker += 4 + 4;

                    // Check length
                    if (msgBuf.length < msgMarker + 16) { throw (new JimmException(152, 1, false)); }

                    // Skip SUB_MSG_TYPE2.CAPABILITY
                    msgMarker += 16;

                    // Get message data and initialize marker
                    byte[] msg2Buf;
                    // #sijapp cond.if target is "MIDP2" | target is "MOTOROLA" | target is "SIEMENS2"#
                    // #sijapp cond.if modules_FILES is "true"#
                    int ackType = -1;
                    byte[] extIP = new byte[4];
                    byte[] ip = new byte[4];
                    String port = "0";
                    int status = -1;
                    // #sijapp cond.end#
                    // #sijapp cond.end#
                    
                    do
                    {
                        msg2Buf = Util.getTlv(msgBuf, msgMarker);
                        if (msg2Buf == null) { throw (new JimmException(152, 2, false)); }
                        tlvType = Util.getWord(msgBuf, msgMarker);
                        // #sijapp cond.if target is "MIDP2" | target is "MOTOROLA" | target is "SIEMENS2"#
                        // #sijapp cond.if modules_FILES is "true"#
                        switch (tlvType)
                        {
                        	case 0x0003: System.arraycopy(msg2Buf,0,extIP,0,4); break;
                            case 0x0004: System.arraycopy(msg2Buf,0,ip,0,4);break;
                            case 0x0005: port = Util.byteArrayToString(msg2Buf); break;
                            case 0x000a: ackType = Util.getWord(msg2Buf,0);break;
                        }
                        // #sijapp cond.end#
                        // #sijapp cond.end#
                        msgMarker += 4 + msg2Buf.length;
                    } while (tlvType != 0x2711);
                    
                    int msg2Marker = 0;

                    // Check length
                    if (msg2Buf.length < 2 + 2 + 16 + 3 + 4 + 2 + 2 + 2 + 12 + 2 + 2 + 2 + 2) { throw (new JimmException(
                            152, 3, false)); }

                    // Skip values up to (and including) SUB_MSG_TYPE2.UNKNOWN
                    // (before MSGTYPE)
                    msg2Marker += 2 + 2 + 16 + 3 + 4 + 2 + 2 + 2 + 12;

                    // Get and validate message type
                    int msgType = Util.getWord(msg2Buf, msg2Marker, false);
                    msg2Marker += 2;
                    if (!((msgType == 0x0001) || (msgType == 0x0004) || (msgType == 0x001A) || ((msgType >= 1000) && (msgType <= 1004)))) return;

                    // #sijapp cond.if target is "MIDP2" | target is "MOTOROLA" | target is "SIEMENS2"#
                    // #sijapp cond.if modules_FILES is "true"#
                    status = Util.getWord(msg2Buf,msg2Marker);
                    // #sijapp cond.end#
                    // #sijapp cond.end#
                    msg2Marker += 2;
                    
                    // Skip PRIORITY
                    msg2Marker += 2;

                    // Get length of text
                    int textLen = Util.getWord(msg2Buf, msg2Marker, false);
                    msg2Marker += 2;

                    // Check length
                    if (!((msgType >= 1000) && (msgType <= 1004)))
                        if (msg2Buf.length < msg2Marker + textLen + 4 + 4) { throw (new JimmException(152, 4, false)); }

                    // Get raw text
                    byte[] rawText = new byte[textLen];
                    System.arraycopy(msg2Buf, msg2Marker, rawText, 0, textLen);
                    msg2Marker += textLen;
                    // Plain message or URL message
                    if (((msgType == 0x0001) || (msgType == 0x0004)) && (rawText.length > 1))
                    {
                        // Skip FOREGROUND and BACKGROUND
                        if ((msgType == 0x0001) || (msgType == 0x0004))
                        {
                            msg2Marker += 4 + 4;
                        }

                        // Check encoding (by checking GUID)
                        boolean isUtf8 = false;
                        if (msg2Buf.length >= msg2Marker + 4)
                        {
                            int guidLen = (int) Util.getDWord(msg2Buf, msg2Marker, false);
                            if (guidLen == 38)
                            {
                                if (Util.byteArrayToString(msg2Buf, msg2Marker + 4, guidLen).equals("{0946134E-4C7F-11D1-8222-444553540000}"))
                                {
                                    isUtf8 = true;
                                }
                            }
                            msg2Marker += 4 + guidLen;
                        }

                        // Decode text and create Message object
                        Message message;
                        if (msgType == 0x0001)
                        {
                            // Decode text
                            String text = Util.removeCr(Util.byteArrayToString(rawText, isUtf8));

                          /************************ проверка на RTF сообщение ***********************/
                            if (text.indexOf("rtf1") > 0) text = Util.DecodeRTF(text);
                          /***************************************************************************/  
                            
                            // Instantiate message object
                            message = new PlainMessage(uin, Options.getString(Options.OPTION_UIN), Util.createCurrentDate(false), text, false);

                        } else
                        {
                            // Search for delimited
                            int delim = -1;
                            for (int i = 0; i < rawText.length; i++)
                            {
                                if (rawText[i] == 0xFE)
                                {
                                    delim = i;
                                    break;
                                }
                            }

                            // Decode text; split text first, if delimiter could
                            // be found
                            String urlText, url;
                            if (delim != -1)
                            {
                                urlText = Util.removeCr(Util.byteArrayToString(rawText, 0, delim, isUtf8));
                                url = Util.removeCr(Util.byteArrayToString(rawText, delim + 1, rawText.length - delim - 1, isUtf8));
                            } else
                            {
                                urlText = Util.removeCr(Util.byteArrayToString(rawText, isUtf8));
                                url = "";
                            }

                            // Instantiate UrlMessage object
                            message = new UrlMessage(uin, Options.getString(Options.OPTION_UIN), Util.createCurrentDate(false), url, urlText);
                        }

                        // Forward message object to contact list
                        RunnableImpl.addMessageSerially(message);

						ContactItem contact = ContactList.getItembyUIN(uin);
						if ((Options.getBoolean(Options.OPTION_AUTO_ANSWER)) && (contact != null))
						{
							sendAutoMessage(contact);
						}

                        // Acknowledge message
                        byte[] ackBuf = new byte[10 + 1 + uinLen + 2 + 51 + 3];
                        int ackMarker = 0;
                        System.arraycopy(buf, 0, ackBuf, ackMarker, 10);
                        ackMarker += 10;
                        Util.putByte(ackBuf, ackMarker, uinLen);
                        ackMarker += 1;
                        byte[] uinRaw = Util.stringToByteArray(uin);
                        System.arraycopy(uinRaw, 0, ackBuf, ackMarker, uinRaw.length);
                        ackMarker += uinRaw.length;
                        Util.putWord(ackBuf, ackMarker, 0x0003);
                        ackMarker += 2;
                        System.arraycopy(msg2Buf, 0, ackBuf, ackMarker, 51);
                        ackMarker += 51;
                        Util.putWord(ackBuf, ackMarker, 0x0001, false);
                        ackMarker += 2;
                        Util.putByte(ackBuf, ackMarker, 0x00);
                        ackMarker += 1;
                        SnacPacket ackPacket = new SnacPacket
												(
													SnacPacket.ICBM_FAMILY,
													SnacPacket.CLI_ACKMSG_COMMAND,
													0, new byte[0], ackBuf
												);
                        Icq.c.sendPacket(ackPacket);
                    }

                    // Extended message
                    else if (msgType == 0x001A)
                    {
                        // Check length
                        if (msg2Buf.length < msg2Marker + 2 + 18 + 4) { throw (new JimmException(152, 5, false)); }

                        // Save current marker position
                        int extDataStart = msg2Marker;

                        // Skip EXTMSG.LEN and EXTMSG.UNKNOWN
                        msg2Marker += 2 + 18;

                        // Get length of plugin string
                        int pluginLen = (int) Util.getDWord(msg2Buf, msg2Marker, false);
                        msg2Marker += 4;

                        // Check length
                        if (msg2Buf.length < msg2Marker + pluginLen + 15 + 4 + 4) { throw (new JimmException(152, 6,
                                false)); }

                        // Get plugin string
                        String plugin = Util.byteArrayToString(msg2Buf, msg2Marker, pluginLen);
                        msg2Marker += pluginLen;

                        // Skip EXTMSG.UNKNOWN and EXTMSG.LEN
                        msg2Marker += 15 + 4;

                        // Get length of text
                        textLen = (int) Util.getDWord(msg2Buf, msg2Marker, false);
                        msg2Marker += 4;

                        // Check length
                        if (msg2Buf.length < msg2Marker + textLen) { throw (new JimmException(152, 7, false)); }

                        // Get text
                        String text = Util.removeCr(Util.byteArrayToString(msg2Buf, msg2Marker, textLen));
                        msg2Marker += textLen;

                        // #sijapp cond.if target is "MIDP2" | target is "MOTOROLA" | target is "SIEMENS2"#
                        // #sijapp cond.if modules_FILES is "true"#
                        // File transfer message
                        if (plugin.equals("File") && Jimm.jimm.getSplashCanvasRef().isShown())
                        {
                         if (ackType == 2)
                         {
                             // Get the port we should connect to
                             port = Integer.toString(Util.getWord(msg2Buf,msg2Marker));
                             msg2Marker += 2;
                             
                             // Skip unknwon stuff
                             msg2Marker += 2;
                             
                             // Get filename
                             textLen = Util.getWord(msg2Buf, msg2Marker, false);
                             msg2Marker += 2;

                             // Check length
                             if (msg2Buf.length < msg2Marker + textLen) { throw (new JimmException(152, 8, false)); }

                             // Get text
                             String filename = Util.removeCr(Util.byteArrayToString(msg2Buf, msg2Marker, textLen));
                             msg2Marker += textLen;
                             
                             // Get filesize
                             long filesize = Util.getDWord(msg2Buf,msg2Marker,false);
                             msg2Marker += 4;
                             
                             // Get IP if possible
                             // Check length
                             if (msgBuf.length < + 8) {throw (new JimmException(152, 9, false));}
                             
                             msg2Buf = Util.getTlv(msgBuf, msgMarker);
                             if (msg2Buf == null) {throw (new JimmException(152, 2, false));}
                             tlvType = Util.getWord(msgBuf, msgMarker);
                             if (tlvType == 0x0004) System.arraycopy(msg2Buf, 0, ip, 0, 4);
                             msgMarker += 4 + msg2Buf.length;
                             
                             ContactItem sender = ContactList.getItembyUIN(uin);
                                                       
                     		sender.setIPValue (ContactItem.CONTACTITEM_INTERNAL_IP,ip);
                    		sender.setIPValue (ContactItem.CONTACTITEM_EXTERNAL_IP,extIP);
                    		sender.setIntValue (ContactItem.CONTACTITEM_DC_PORT, Integer.parseInt(port));

                             DirectConnectionAction dcAct = new DirectConnectionAction(sender.getFTM());
                             try
                             {
                                 Icq.requestAction(dcAct);
                             }
                             catch (JimmException e)
                             {
                                 JimmException.handleException(e);
                                 if (e.isCritical()) return;
                             }
                                 
                             // Start timer (timer will activate splash screen)
                             SplashCanvas.addTimerTask("filetransfer", dcAct, true);
                          }
                        }
                        // URL message
                        else 
                        // #sijapp cond.end#
                        // #sijapp cond.end#
                        if (plugin.equals("Send Web Page Address (URL)"))
                        {
                            // Search for delimiter
                            int delim = text.indexOf(0xFE);

                            // Split message, if delimiter could be found
                            String urlText;
                            String url;
                            if (delim != -1)
                            {
                                urlText = text.substring(0, delim);
                                url = text.substring(delim + 1);
                            } else
                            {
                                urlText = text;
                                url = "";
                            }

                            // Forward message message to contact list
                            UrlMessage message = new UrlMessage(uin, Options.getString(Options.OPTION_UIN), Util.createCurrentDate(false), url, urlText);
                            RunnableImpl.addMessageSerially(message);

                            // Acknowledge message
                            byte[] ackBuf = new byte[10 + 1 + uinLen + 2 + 51 + 3 + 20 + 4 + (int) pluginLen + 19 + 4
                                    + textLen];
                            int ackMarker = 0;
                            System.arraycopy(buf, 0, ackBuf, ackMarker, 10);
                            ackMarker += 10;
                            Util.putByte(ackBuf, ackMarker, uinLen);
                            ackMarker += 1;
                            byte[] uinRaw = Util.stringToByteArray(uin);
                            System.arraycopy(uinRaw, 0, ackBuf, ackMarker, uinRaw.length);
                            ackMarker += uinRaw.length;
                            Util.putWord(ackBuf, ackMarker, 0x0003);
                            ackMarker += 2;
                            System.arraycopy(msgBuf, 0, ackBuf, ackMarker, 51);
                            ackMarker += 51;
                            Util.putWord(ackBuf, ackMarker, 0x0001, false);
                            ackMarker += 2;
                            Util.putByte(ackBuf, ackMarker, 0x00);
                            ackMarker += 1;
                            System.arraycopy(msg2Buf, extDataStart, ackBuf, ackMarker, 20 + 4 + (int) pluginLen + 19
                                    + 4 + textLen);
                            SnacPacket ackPacket = new SnacPacket
														(
															SnacPacket.ICBM_FAMILY,
															SnacPacket.CLI_ACKMSG_COMMAND,
															0, new byte[0], ackBuf
														);
                            Icq.c.sendPacket(ackPacket);

                        }
                        // Other messages
                        else 
                        {
                            if (plugin.equals("Script Plug-in: Remote Notification Arrive"))
                            {
								try
								{
									XtrazSM.a(uin, text, msgType, l3, l5);
								}
								catch (Exception e) {}
							}
                       }
                    }                    
                    // Status message requests
                    else if (((msgType >= 1000) && (msgType <= 1004)))
                    {
						MagicEye.addAction(uin, "read_status_message");

						String statusMess = "";
						int statusMsgIdx = 0;
                    	int currStatus = (int)Options.getLong(Options.OPTION_ONLINE_STATUS);

						switch (currStatus)
						{
						case ContactList.STATUS_AWAY:
							statusMsgIdx = Options.OPTION_STATUS_MESSAGE_AWAY;
							break;

						case ContactList.STATUS_NA:
							statusMsgIdx = Options.OPTION_STATUS_MESSAGE_NA;
							break;

						case ContactList.STATUS_DND:
							statusMsgIdx = Options.OPTION_STATUS_MESSAGE_DND;
							break;

						case ContactList.STATUS_OCCUPIED:
							statusMsgIdx = Options.OPTION_STATUS_MESSAGE_OCCUPIED;
							break;

						case ContactList.STATUS_EVIL:
							statusMsgIdx = Options.OPTION_STATUS_MESSAGE_EVIL;
							break;

						case ContactList.STATUS_DEPRESSION:
							statusMsgIdx = Options.OPTION_STATUS_MESSAGE_DEPRESSION;
							break;

						case ContactList.STATUS_HOME:
							statusMsgIdx = Options.OPTION_STATUS_MESSAGE_HOME;
							break;

						case ContactList.STATUS_WORK:
							statusMsgIdx = Options.OPTION_STATUS_MESSAGE_WORK;
							break;

						case ContactList.STATUS_LUNCH:
							statusMsgIdx = Options.OPTION_STATUS_MESSAGE_LUNCH;
							break;
						}

                   	    if (statusMsgIdx != 0) statusMess = Util.replaceStr(Options.getString(statusMsgIdx), "%TIME%", Icq.getLastStatusChangeTime());

                        // Acknowledge message with away message
                    	final byte[] statusMessBytes = Util.stringToByteArray(statusMess, false);
                    	
						if (statusMessBytes.length < 1) return;
                    	
                        byte[] ackBuf = new byte[10 + 1 + uinLen + 2 + 51 + 2 + statusMessBytes.length +1];
                        int ackMarker = 0;
                        System.arraycopy(buf, 0, ackBuf, ackMarker, 10);
                        ackMarker += 10;
                        Util.putByte(ackBuf, ackMarker, uinLen);
                        ackMarker += 1;
                        byte[] uinRaw = Util.stringToByteArray(uin);
                        System.arraycopy(uinRaw, 0, ackBuf, ackMarker, uinRaw.length);
                        ackMarker += uinRaw.length;
                        Util.putWord(ackBuf, ackMarker, 0x0003);
                        ackMarker += 2;
                        System.arraycopy(msg2Buf, 0, ackBuf, ackMarker, 51);
                        Util.putWord(ackBuf, ackMarker+2, 0x0800);
                        ackMarker += 51;
                        Util.putWord(ackBuf, ackMarker, statusMessBytes.length+1, false);
                        ackMarker += 2;
                        System.arraycopy(statusMessBytes,0,ackBuf,ackMarker,statusMessBytes.length);
                        Util.putByte(ackBuf, ackMarker+statusMessBytes.length, 0x00);
                        SnacPacket ackPacket = new SnacPacket
													(
														SnacPacket.ICBM_FAMILY,
														SnacPacket.CLI_ACKMSG_COMMAND,
														0, new byte[0], ackBuf
													);
                        Icq.c.sendPacket(ackPacket);
                    }
                }
                //////////////////////
                // Message format 4 //
                //////////////////////
                else if (format == 0x0004)
                {
                    // Check length
                    if (msgBuf.length < 8) { throw (new JimmException(153, 0, false)); }

                    // Skip SUB_MSG_TYPE4.UIN
                    msgMarker += 4;

                    // Get SUB_MSG_TYPE4.MSGTYPE
                    int msgType = Util.getWord(msgBuf, msgMarker, false);
                    msgMarker += 2;

                    // Only plain messages and URL messagesa are supported
                    if ((msgType != 0x0001) && (msgType != 0x0004)) return;

                    // Get length of text
                    int textLen = Util.getWord(msgBuf, msgMarker, false);
                    msgMarker += 2;

                    // Check length (exact match required)
                    if (msgBuf.length != 8 + textLen) { throw (new JimmException(153, 1, false)); }

                    // Get text
                    String text = Util.removeCr(Util.byteArrayToString(msgBuf, msgMarker, textLen));
                    msgMarker += textLen;

                    // Plain message
                    if (msgType == 0x0001)
                    {
                        // Forward message to contact list
                        PlainMessage plainMsg = new PlainMessage(uin, Options.getString(Options.OPTION_UIN), Util.createCurrentDate(false), text, false);
                        RunnableImpl.addMessageSerially(plainMsg);
                    }
                    // URL message
                    else if (msgType == 0x0004)
                    {
                        // Search for delimiter
                        int delim = text.indexOf(0xFE);

                        // Split message, if delimiter could be found
                        String urlText;
                        String url;
                        if (delim != -1)
                        {
                            urlText = text.substring(0, delim);
                            url = text.substring(delim + 1);
                        } else
                        {
                            urlText = text;
                            url = "";
                        }

                        // Forward message message to contact list
                        UrlMessage urlMsg = new UrlMessage(uin, Options.getString(Options.OPTION_UIN), Util.createCurrentDate(false), url, urlText);
						RunnableImpl.addMessageSerially(urlMsg);
                    }
                }
            }

            //	  Watch out for SRV_ADDEDYOU
            else if ((snacPacket.getFamily() == SnacPacket.ROSTER_FAMILY)
                    && (snacPacket.getCommand() == SnacPacket.SRV_ADDEDYOU_COMMAND))
            {
                // Get data
                byte[] buf = snacPacket.getData();

                // Get UIN of the contact changing status
                int uinLen = Util.getByte(buf, 0);
                String uin = Util.byteArrayToString(buf, 1, uinLen);

                // Create a new system notice
                SystemNotice notice = new SystemNotice(SystemNotice.SYS_NOTICE_YOUWEREADDED, uin, false, null);

                // Handle the new system notice
                RunnableImpl.addMessageSerially(notice);                
            }

            /*******************************************************************************************/ 
            //	  Watch out for password change confirmation
            else if ((snacPacket.getFamily() == SnacPacket.OLD_ICQ_FAMILY)
                    && (snacPacket.getCommand() == SnacPacket.SRV_FROMICQSRV_COMMAND))
            {
				byte[] passrvc = snacPacket.getData();
				int Type = Util.getWord(passrvc, 0, false);
				int Type1 = Util.getByte(passrvc, 2);
				if ((Type == 170) && (Type1 == 10))
				{
                    Alert pass_message = new Alert("", ResourceBundle.getString("change_pass_message"), null, AlertType.INFO);
                    pass_message.setTimeout(15000);
                    Jimm.display.setCurrent(pass_message, Jimm.display.getCurrent());
                    Options.safe_save();
                    // ContactList.activate(pass_message);
				}
            }
            /******************************************************************************************/

            /******************************************************************************************/
            //	  Watch out for CLI_ROSTERDELETE
            else if ((snacPacket.getFamily() == SnacPacket.ROSTER_FAMILY)
                    && (snacPacket.getCommand() == SnacPacket.CLI_ROSTERDELETE_COMMAND))
            {
                byte[] bufdel = snacPacket.getData();
                int uinLenq = Util.getByte(bufdel, 1);
                if (Util.getByte(bufdel, 2 + uinLenq) != 0)
                {
                    String uinq = Util.byteArrayToString(bufdel, 2, uinLenq);
                    int nikLenq = Util.getByte(bufdel, 13 + uinLenq);
                    String nikq = Util.byteArrayToString(bufdel, 14 + uinLenq, nikLenq, true);
                    SystemNotice notice = new SystemNotice(SystemNotice.SYS_NOTICE_DELETE, uinq, false, nikq);
                    RunnableImpl.addMessageSerially(notice);
                }
            }
            /******************************************************************************************/

            //	  Watch out for SRV_AUTHREQ
            else if ((snacPacket.getFamily() == SnacPacket.ROSTER_FAMILY)
                    && (snacPacket.getCommand() == SnacPacket.SRV_AUTHREQ_COMMAND))
            {
                int authMarker = 0;
                
                // Get data
                byte[] buf = snacPacket.getData();

                // Get UIN of the contact changing status
                int length = Util.getByte(buf, 0);
                authMarker += 1;
                String uin = Util.byteArrayToString(buf, authMarker, length);
                authMarker += length;

                // Get reason
                length = Util.getWord(buf, authMarker);
                authMarker += 2;
                String reason = Util.byteArrayToString(buf, authMarker, length, Util.isDataUTF8(buf, authMarker, length));

                // Create a new system notice
				SystemNotice notice = new SystemNotice(SystemNotice.SYS_NOTICE_AUTHREQ, uin, false, reason);

				// Handle the new system notice
				RunnableImpl.addMessageSerially(notice);
            }

            //	  Watch out for SRV_AUTHREPLY
            else if ((snacPacket.getFamily() == SnacPacket.ROSTER_FAMILY)
                    && (snacPacket.getCommand() == SnacPacket.SRV_AUTHREPLY_COMMAND))
            {
                int authMarker = 0;
                // Get data
                byte[] buf = snacPacket.getData();

                // Get UIN of the contact changing status
                int length = Util.getByte(buf, 0);
                authMarker += 1;
                String uin = Util.byteArrayToString(buf, authMarker, length);
                authMarker += length;

                // Get granted boolean
                boolean granted = false;
                if (Util.getByte(buf, authMarker) == 0x01)
                {
                    granted = true;
                }
                authMarker += 1;

                // Get reason only of not granted
                SystemNotice notice;
                if (!granted)
                {
                    length = Util.getWord(buf, authMarker);
                    //String reason = Util.byteArrayToString(buf, authMarker, length + 2); // old
                    String reason = Util.byteArrayToString(buf, authMarker + 2, length, Util.isDataUTF8(buf, authMarker + 2, length));
                    
                    // Create a new system notice
                    if (length == 0)
                        notice = new SystemNotice(SystemNotice.SYS_NOTICE_AUTHREPLY, uin, granted, null);
                    else
                        notice = new SystemNotice(SystemNotice.SYS_NOTICE_AUTHREPLY, uin, granted, reason);
                } else
                {
                    // Create a new system notice
                    //System.out.println("Auth granted");
                    notice = new SystemNotice(SystemNotice.SYS_NOTICE_AUTHREPLY, uin, granted, "");
                }

                // Handle the new system notice
                RunnableImpl.addMessageSerially(notice);
            }
        }       
    }

	/********************************************** Auto Answer *********************************************/
	private static void sendAutoMessage(ContactItem contact) 
	{
		if ((contact.getInvisibleId() != 0) || contact.getBooleanValue(ContactItem.CONTACTITEM_IS_TEMP)
			|| (Options.getInt(Options.OPTION_PRIVATE_STATUS) == OtherAction.PSTATUS_NONE) || contact.autoAnswered
			|| ((Options.getInt(Options.OPTION_PRIVATE_STATUS) == OtherAction.PSTATUS_VISIBLE_ONLY) && (contact.getVisibleId() == 0))) return;

		long currStatus = Options.getLong(Options.OPTION_ONLINE_STATUS);

		String statusMess = new String();

		if (currStatus == ContactList.STATUS_AWAY)
			statusMess = Util.replaceStr(Options.getString(Options.OPTION_STATUS_MESSAGE_AWAY), "%TIME%", Icq.getLastStatusChangeTime());

		if (currStatus == ContactList.STATUS_DND)
			statusMess = Util.replaceStr(Options.getString(Options.OPTION_STATUS_MESSAGE_DND), "%TIME%", Icq.getLastStatusChangeTime());

		if (currStatus == ContactList.STATUS_NA)
			statusMess = Util.replaceStr(Options.getString(Options.OPTION_STATUS_MESSAGE_NA), "%TIME%", Icq.getLastStatusChangeTime());

		if (currStatus == ContactList.STATUS_OCCUPIED)
			statusMess = Util.replaceStr(Options.getString(Options.OPTION_STATUS_MESSAGE_OCCUPIED), "%TIME%", Icq.getLastStatusChangeTime());

		if (!(statusMess.length() < 1))
		{
			PlainMessage plainMsg = new PlainMessage
				(
					contact.getStringValue(ContactItem.CONTACTITEM_UIN), 
					contact, 
					Message.MESSAGE_TYPE_NORM, 
					Util.createCurrentDate(false), 
					ResourceBundle.getString("auto_message") + "\n" + statusMess
				);

			SendMessageAction sendMsgAct = new SendMessageAction(plainMsg);
			try 
			{
				Icq.requestAction(sendMsgAct);
			} 
			catch (Exception e) {}
		}
		contact.autoAnswered = true;
	}
	/******************************************************************************************************/

	/********************************************** Anti-Spam *********************************************/
    private static void sendMessage(ContactItem contact, String message) 
    {
        if ((Options.getLong(Options.OPTION_ONLINE_STATUS) == ContactList.STATUS_INVISIBLE) 
			|| (Options.getLong(Options.OPTION_ONLINE_STATUS) == ContactList.STATUS_INVIS_ALL) 
			|| (Options.getString(Options.OPTION_ANTISPAM_ANSWER).length() < 1))
        {
            return;
        }
        PlainMessage plainMsg = 
                new PlainMessage(contact.getStringValue(ContactItem.CONTACTITEM_UIN), contact, Message.MESSAGE_TYPE_NORM, Util.createCurrentDate(false), message);

        SendMessageAction sendMsgAct = new SendMessageAction(plainMsg);
        try 
        {
            Icq.requestAction(sendMsgAct);
        } 
        catch (Exception e) {}
    }

    private static Vector uins = new Vector();

    private static boolean isChecked(String uin) 
    {
        for (int i = 0; i < uins.size(); i++) 
        {
            if (uins.elementAt(i).equals(uin)) 
            {
                return true;
            }
        }
        return false;
    }

    /***************************************************/
    private static Vector uin1 = new Vector();
    private static Vector uin2 = new Vector();

    private static boolean isCheckedData(String uin) 
    {
        for (int i = 0; i < uin1.size(); i++) 
        {
            if (uin1.elementAt(i).equals(uin)) 
            {
                return true;
            }
        }
        return false;
    }
    /***************************************************/

    private static boolean stringEquals(String s1, String s2) 
    {
        if (s1.length() != s2.length()) 
        {
            return false;
        }
        if (s1 == s2) 
        {
            return true;
        }
        int size = s1.length();
        for (int i = 0; i < size; i++) 
        {
            if (StringConvertor.toLowerCase(s1.charAt(i)) != StringConvertor.toLowerCase(s2.charAt(i))) 
            {
                return false;
            }
        }
        return true;
    }

    public static boolean isSpam(Message message) 
    {
        if (!Options.getBoolean(Options.OPTION_ANTISPAM_ENABLE)) 
        {
            return false;
        }
        String uin = message.getSndrUin();
        ContactItem contact = ContactList.getItembyUIN(uin);

        if ((null != contact) || isChecked(uin)) 
        {
            return false;
        }

        if (!(message instanceof PlainMessage)) 
        {
            return true;
        }

        /************************************************/     
        int d1 = 0;
        String d2;
        int d3 = 0;

        if (!isCheckedData(uin))
        {
            uin1.addElement(uin);
            uin2.addElement(String.valueOf(0));
        }
        else
        {
            d1 = uin1.indexOf(uin);
            d2 = (String)uin2.elementAt(d1);
            d3 = Integer.parseInt(d2);
            d3++;
            uin2.setElementAt(String.valueOf(d3), d1);
        }
        /*************************************************/

        String msg = ((PlainMessage)message).getText();

        MagicEye.addAction(uin, "antispam", msg);

        if (message.getOffline()) 
        {
            return true;
        }

        if (stringEquals(msg, Options.getString(Options.OPTION_ANTISPAM_ANSWER))) 
        {
            uins.addElement(uin);
        }
        
        if ((d3 < 3) || isChecked(uin))
        {
            contact = new ContactItem(0, 0, uin, uin, true, false);
            sendMessage(contact, Options.getString(isChecked(uin) ? Options.OPTION_ANTISPAM_HELLO : Options.OPTION_ANTISPAM_MSG));
        }
        return true;
    }
    /*******************************************************************************************************/
}