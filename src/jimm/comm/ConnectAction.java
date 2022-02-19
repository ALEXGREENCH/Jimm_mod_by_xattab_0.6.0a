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
 File: src/jimm/comm/ConnectAction.java
 Version: ###VERSION###  Date: ###DATE###
 Author(s): Manuel Linsmayer, Andreas Rossbacher
 *******************************************************************************/

package jimm.comm;

import java.util.*;
import java.io.*;

import jimm.*;
import jimm.comm.Icq.HTTPConnection;

public class ConnectAction extends Action
{
	// Action states
	public static final int STATE_ERROR = -1;
	public static final int STATE_INIT = 0;
	public static final int STATE_INIT_DONE = 1;
	public static final int STATE_AUTHKEY_REQUESTED = 2;
	public static final int STATE_CLI_IDENT_SENT = 3;
	public static final int STATE_CLI_DISCONNECT_SENT = 4;
	public static final int STATE_CLI_COOKIE_SENT = 5;
	public static final int STATE_CLI_WANT_CAPS_SENT = 6;
	public static final int STATE_CLI_CHECKROSTER_SENT = 7;
	public static final int STATE_CLI_STATUS_INFO_SENT = 8;
	public static final int STATE_CLI_REQOFFLINEMSGS_SENT = 9;
	public static final int STATE_CLI_ACKOFFLINEMSGS_SENT = 10;

	// Privacy Lists begining
	private static Hashtable ignoreList;
	private static Hashtable invisibleList;
	private static Hashtable visibleList;
	// Privacy Lists ending

	private boolean cancel = false;

	// CLI_SETICBM packet data
	public static final byte[] CLI_SETICBM_DATA = Util.explodeToBytes("0,0,0,0,0,0B,1F,40,3,E7,3,E7,0,0,0,0", ',', 16);

	// CLI_READY packet data
	public static final byte[] CLI_READY_DATA = Util.explodeToBytes
		(
			"00,22,00,01,01,10,16,4f,"+
			"00,01,00,04,01,10,16,4f,"+ 
			"00,13,00,04,01,10,16,4f,"+
			"00,02,00,01,01,10,16,4f,"+
			"00,03,00,01,01,10,16,4f,"+
			"00,15,00,01,01,10,16,4f,"+
			"00,04,00,01,01,10,16,4f,"+
			"00,06,00,01,01,10,16,4f,"+
			"00,09,00,01,01,10,16,4f,"+
			"00,0a,00,01,01,10,16,4f,"+
			"00,0b,00,01,01,10,16,4f",
			',', 16
		);

	public static final short[] FAMILIES_AND_VER_LIST =
	{
		0x0022, 0x0001,
		0x0001, 0x0004,
		0x0013, 0x0004,
		0x0002, 0x0001,
		0x0003, 0x0001,
		0x0015, 0x0001,
		0x0004, 0x0001,
		0x0006, 0x0001,
		0x0009, 0x0001,
		0x000a, 0x0001,
		0x000b, 0x0001,
	};

	// Timeout
	//#sijapp cond.if modules_PROXY is "true"#
	public static final int TIME_OUT = 30 * 1000;
	//#sijapp cond.end#
	public int TIMEOUT = 30 * 1000; // milliseconds

	/** *********************************************************************** */

	// UIN
	private String uin;

	// Password
	private String password;

	// Server host
	private String srvHost;

	// Server port
	private String srvPort;

	// Action state
	private int state;

	// Last activity
	private long lastActivity = 0;
	private boolean active;

	// Temporary variables
	private String server;
	private byte[] cookie;
	private boolean srvReplyRosterRcvd;

    jimm.comm.Icq.Connection con;

	// Constructor
	public ConnectAction(String uin, String password, String srvHost, String srvPort)
	{
		super(true, false);
		this.uin = uin;
		this.password = password;
		this.srvHost = srvHost;
		this.srvPort = srvPort;
	}

	private void connect(String server) throws JimmException
	{
		int retry = 1;

		// #sijapp cond.if modules_PROXY is "true" #
		try
		{
			retry = Integer.parseInt(Options.getString(Options.OPTION_AUTORETRY_COUNT));
			retry = (retry > 0) ? retry : 1;
		}
		catch (NumberFormatException e)
		{
			retry = 1;
		}
		// #sijapp cond.end#

		for (int i = 0; i < retry; i++)
		{
			lastActivity = System.currentTimeMillis();

            if (cancel || (con != Icq.c)) return;

			try
			{
				con.connect(server);
				return;
			}
			catch (JimmException e)
			{
				con.close();

				if (i >= (retry - 1) || ((lastActivity + TIMEOUT) < System.currentTimeMillis()))
				{
					state = STATE_ERROR;
					throw (e);
				}
			}
		}
	}

	// Init action
	protected void init() throws JimmException
	{
		state = STATE_INIT;

		// Privacy Lists begining
		ignoreList    = new Hashtable();
		invisibleList = new Hashtable();
		visibleList   = new Hashtable();
		// Privacy Lists ending

		// Init activity timestamp
		lastActivity = System.currentTimeMillis();

		// Check parameters
		if ((uin.length() == 0) || (password.length() == 0))
		{
			state = STATE_ERROR;
			throw (new JimmException(117, 0));
		}

		con = Icq.c;

		// Open connection
		connect(srvHost + ":" + srvPort);
		
		// Set STATE_INIT_DONE
		state = STATE_INIT_DONE;

		// Update activity timestamp
		lastActivity = System.currentTimeMillis();
	}

	// Forwards received packet, returns true if packet has been consumed
	protected boolean forward(Packet packet) throws JimmException
	{
        if (cancel || (con != Icq.c)) return false;

		// Set activity flag
		active = true;

		// Catch JimmExceptions
		try
		{
			// Flag indicates whether packet has been consumed or not
			boolean consumed = false;

			// Watch out for STATE_INIT_DONE
			if (state == STATE_INIT_DONE)
			{
				// Watch out for SRV_CLI_HELLO packet
				if (packet instanceof ConnectPacket)
				{
					ConnectPacket connectPacket = (ConnectPacket) packet;
					if (connectPacket.getType() == ConnectPacket.SRV_CLI_HELLO)
					{
						if (Options.getBoolean(Options.OPTION_MD5_LOGIN))
						{
							con.sendPacket(new ConnectPacket());
							byte[] buf = new byte[4 + uin.length()];
							Util.putWord(buf, 0, 0x0001);
							Util.putWord(buf, 2, uin.length());
							byte[] uinRaw = Util.stringToByteArray(uin);
							System.arraycopy(uinRaw, 0, buf, 4, uinRaw.length);
							con.sendPacket(new SnacPacket(0x0017, 0x0006, 0, new byte[0], buf));
						}
						else
						{
							// Send a CLI_IDENT packet as reply
							ConnectPacket reply = new ConnectPacket(uin, password);
							con.sendPacket(reply);
						}

						// Move to next state
						state = !Options.getBoolean(Options.OPTION_MD5_LOGIN) ? STATE_CLI_IDENT_SENT : STATE_AUTHKEY_REQUESTED;

						// Packet has been consumed
						consumed = true;
					}
				}
			}
			else if (state == STATE_AUTHKEY_REQUESTED) {
				if (packet instanceof SnacPacket) {
					SnacPacket snacPacket = (SnacPacket)packet;
					if ((snacPacket.getFamily() == 0x0017) && (snacPacket.getCommand() == 0x0007))
					{
						byte[] rbuf = snacPacket.getData();
						int len = Util.getWord(rbuf, 0);
						byte[] authkey = new byte[len];
						System.arraycopy(rbuf, 2, authkey, 0, len);
						rbuf = null;
						byte[] buf = new byte[2 + 2 + uin.length() + 2 + 2 + 16];
						int marker = 0;
						Util.putWord(buf, marker, 0x0001);
						marker += 2;
						Util.putWord(buf, marker, uin.length());
						marker += 2;
						byte[] uinRaw = Util.stringToByteArray(uin);
						System.arraycopy(uinRaw, 0, buf, marker, uinRaw.length);
						marker += uinRaw.length;
						Util.putWord(buf, marker, 0x0025);
						marker += 2;
						Util.putWord(buf, marker, 0x0010);
						marker += 2;
						byte[] md5buf = new byte[authkey.length + password.length() + Util.AIM_MD5_STRING.length];
						int md5marker = 0;
						System.arraycopy(authkey, 0, md5buf, md5marker, authkey.length);
						md5marker += authkey.length;
						byte[] passwordRaw = Util.stringToByteArray(password);
						System.arraycopy(passwordRaw, 0, md5buf, md5marker, passwordRaw.length);
						md5marker += passwordRaw.length;
						System.arraycopy(Util.AIM_MD5_STRING, 0, md5buf, md5marker, Util.AIM_MD5_STRING.length);
						byte[] hash = Util.calculateMD5(md5buf);
						System.arraycopy(hash, 0, buf, marker, 16);
						con.sendPacket(new SnacPacket(0x0017, 0x0002, 0, new byte[0], buf));
						state = STATE_CLI_IDENT_SENT;
					}
					else
					{
						throw new JimmException(100,0);
					}
				}
				consumed = true;
			}
			// Watch out for STATE_CLI_IDENT_SENT
			else if (state == STATE_CLI_IDENT_SENT)
			{
				int errcode = -1;
				if (Options.getBoolean(Options.OPTION_MD5_LOGIN))
				{
					if (packet instanceof SnacPacket)
					{
						SnacPacket snacPacket = (SnacPacket)packet;
						if ((snacPacket.getFamily() == 0x0017) && (snacPacket.getCommand() == 0x0003))
						{
							byte[] buf = snacPacket.getData();
							int marker = 0;
							while (marker < buf.length)
							{
								byte[] tlvData = Util.getTlv(buf, marker);
								int tlvType = Util.getWord(buf, marker);
								marker += 4 + tlvData.length;
								switch (tlvType)
								{
									case 0x0008: errcode = Util.getWord(tlvData, 0); break;
									case 0x0005: server = Util.byteArrayToString(tlvData); break;
									case 0x0006: cookie = tlvData; break;
								}
							}
						}
					}
					else if (packet instanceof DisconnectPacket)
					{
						consumed = true;
					}
				}
				else
				{
					// watch out for channel 4 packet
					if (packet instanceof DisconnectPacket)
					{
						DisconnectPacket disconnectPacket = (DisconnectPacket) packet;
						// Watch out for SRV_COOKIE packet
						if (disconnectPacket.getType() == DisconnectPacket.TYPE_SRV_COOKIE)
						{
							// Save cookie
							cookie = disconnectPacket.getCookie();
							server = disconnectPacket.getServer();
						}
						// Watch out for SRV_GOODBYE packet
						else if (disconnectPacket.getType() == DisconnectPacket.TYPE_SRV_GOODBYE)
							errcode = disconnectPacket.getError();
						consumed = true;
					}
				}

				if (errcode != -1)
				{
					int toThrow = 100;
					switch (errcode)
					{
						// Multiple logins
						case 0x0001:              toThrow = 110; break;
						// Bad password
						case 0x0004: case 0x0005: toThrow = 111; break;
						// Non-existant UIN
						case 0x0007: case 0x0008: toThrow = 112; break;
						// Too many clients from same IP
						case 0x0015: case 0x0016: toThrow = 113; break;
						// Rate exceeded
						case 0x0018: case 0x001d: toThrow = 114; break;
					}
					throw new JimmException(toThrow, errcode);
				}

				if (consumed & (server != null) & (cookie != null))
				{
					// Close connection (only if not HTTP Connection)
					if (!(con instanceof HTTPConnection)) con.close();

					// Open connection
					connect(server);

					// Move to next state
					state = STATE_CLI_DISCONNECT_SENT;
				}
			}

			// Watch out for STATE_CLI_DISCONNECT_SENT
			else if (state == STATE_CLI_DISCONNECT_SENT)
			{
				// Watch out for SRV_HELLO packet
				if (packet instanceof ConnectPacket)
				{
					ConnectPacket connectPacket = (ConnectPacket) packet;
					if (connectPacket.getType() == ConnectPacket.SRV_CLI_HELLO)
					{
						// Send a CLI_COOKIE packet as reply
						ConnectPacket reply = new ConnectPacket(cookie);
						con.sendPacket(reply);

						// Move to next state
						state = STATE_CLI_COOKIE_SENT;

						// Packet has been consumed
						consumed = true;
					}
				}
			}
			// Watch out for STATE_CLI_COOKIE_SENT
			else if (state == STATE_CLI_COOKIE_SENT)
			{
				ByteArrayOutputStream stream = new ByteArrayOutputStream();
				
				for (int i = 0; i < FAMILIES_AND_VER_LIST.length; i++)
				{
					Util.writeWord(stream, FAMILIES_AND_VER_LIST[i], true);
				}

				con.sendPacket
				(
					new SnacPacket
					(
						SnacPacket.SERVICE_FAMILY,
						SnacPacket.CLI_FAMILIES_COMMAND,
						0x00000000,
						new byte[0],
						stream.toByteArray()
					)
				);

				// Move to next state
				state = ConnectAction.STATE_CLI_WANT_CAPS_SENT;
			}
			// Watch out for STATE_CLI_WANT_CAPS_SENT
			else if (state == STATE_CLI_WANT_CAPS_SENT)
			{
				SnacPacket reqp = new SnacPacket
									(
										SnacPacket.SERVICE_FAMILY,
										SnacPacket.CLI_REQINFO_COMMAND,
										SnacPacket.CLI_REQINFO_COMMAND,
										new byte[0], new byte[0]
									);
				con.sendPacket(reqp);

				byte[] rdata = new byte[6];
				Util.putDWord(rdata, 0, 0x000B0002);
				Util.putWord(rdata, 4, 0x000F);
				reqp = new SnacPacket
						(
							SnacPacket.ROSTER_FAMILY,
							SnacPacket.CLI_REQLISTS_COMMAND,
							SnacPacket.CLI_REQLISTS_COMMAND,
							new byte[0], rdata
						);
				con.sendPacket(reqp);

				reqp = new SnacPacket
						(
							SnacPacket.LOCATION_FAMILY,
							SnacPacket.CLI_REQLOCATION_COMMAND,
							SnacPacket.CLI_REQLOCATION_COMMAND,
							new byte[0], new byte[0]
						);
				con.sendPacket(reqp);

				rdata = new byte[6];
				Util.putDWord(rdata, 0, 0x00050002);
				Util.putWord(rdata, 4, 0x0003);
				reqp = new SnacPacket
						(
							SnacPacket.CONTACT_FAMILY,
							SnacPacket.CLI_REQBUDDY_COMMAND,
							SnacPacket.CLI_REQBUDDY_COMMAND,
							new byte[0], rdata
						);
				con.sendPacket(reqp);

				reqp = new SnacPacket
						(
							SnacPacket.ICBM_FAMILY,
							SnacPacket.CLI_REQICBM_COMMAND,
							SnacPacket.CLI_REQICBM_COMMAND,
							new byte[0], new byte[0]
						);
				con.sendPacket(reqp);

				reqp = new SnacPacket
						(
							SnacPacket.BOS_FAMILY,
							SnacPacket.CLI_REQBOS_COMMAND,
							SnacPacket.CLI_REQBOS_COMMAND,
							new byte[0], new byte[0]
						);
				con.sendPacket(reqp);

				// Send a CLI_REQROSTER or CLI_CHECKROSTER packet
				long versionId1 = ContactList.getSsiListLastChangeTime();
				int versionId2 = ContactList.getSsiNumberOfItems();
				if (((versionId1 == -1) && (versionId2 == -1)) || (ContactList.getSize() == 0))
				{
					SnacPacket reply2 = new SnacPacket
											(
												SnacPacket.ROSTER_FAMILY,
												SnacPacket.CLI_REQROSTER_COMMAND,
												0x00000000, new byte[0], new byte[0]
											);
					con.sendPacket(reply2);
				}
				else
				{
					byte[] data = new byte[6];
					Util.putDWord(data, 0, versionId1);
					Util.putWord(data, 4, versionId2);
					SnacPacket reply2 = new SnacPacket
											(
												SnacPacket.ROSTER_FAMILY,
												SnacPacket.CLI_CHECKROSTER_COMMAND,
												0x00000000, new byte[0], data
											);
					con.sendPacket(reply2);
				}

				// Move to next state
				state = STATE_CLI_CHECKROSTER_SENT;
			}

			// Watch out for STATE_CLI_CHECKROSTER_SENT
			else if (state == STATE_CLI_CHECKROSTER_SENT)
			{
				// Watch out for SNAC packet
				if (packet instanceof SnacPacket)
				{
					SnacPacket snacPacket = (SnacPacket) packet;

					// Watch out for SRV_REPLYROSTEROK
					if ((snacPacket.getFamily() == SnacPacket.ROSTER_FAMILY)
							&& (snacPacket.getCommand() == SnacPacket.SRV_REPLYROSTEROK_COMMAND))
					{
						srvReplyRosterRcvd = true;

						// Packet has been consumed
						consumed = true;
					}
					// watch out for SRV_REPLYROSTER
					else if ((snacPacket.getFamily() == SnacPacket.ROSTER_FAMILY)
								&& (snacPacket.getCommand() == SnacPacket.SRV_REPLYROSTER_COMMAND))
					{
						if (snacPacket.getFlags() != 1) srvReplyRosterRcvd = true;

						// Initialize vector for items
						Vector items = new Vector();

						// Get data
						byte[] buf = snacPacket.getData();
						int marker = 0;

						// Check length
						if (buf.length < 3) { throw (new JimmException(115, 0)); }

						// Skip SRV_REPLYROSTER.UNKNOWN
						marker += 1;

						// Iterate through all items
						int count = Util.getWord(buf, marker);
						marker += 2;
				
						for (int i = 0; i < count; i++)
						{
							// Check length
							if (buf.length < marker + 2) { throw (new JimmException(115, 1)); }

							// Get name length
							int nameLen = Util.getWord(buf, marker);
							marker += 2;

							// Check length
							if (buf.length < marker + nameLen + 2 + 2 + 2 + 2) { throw (new JimmException(115, 2)); }

							// Get name
							String name = Util.byteArrayToString(buf, marker, nameLen, Util.isDataUTF8(buf, marker, nameLen));
							marker += nameLen;

							// Get group, id and type
							int group = Util.getWord(buf, marker);
							int id = Util.getWord(buf, marker + 2);
							int type = Util.getWord(buf, marker + 4);
							marker += 6;

							// Get length of the following TLVs
							int len = Util.getWord(buf, marker);
							marker += 2;

							// Check length
							if (buf.length < marker + len) { throw (new JimmException(115, 3)); }

							// Normal contact
							if ((type == 0x0000) || ((type == 0x0019 || type == 0x001B) && Options.getBoolean(Options.OPTION_CACHE_CONTACTS)))
							{
								// Get nick
								String nick = new String(name);
								
								boolean noAuth = false;
								while (len > 0)
								{
									byte[] tlvData = Util.getTlv(buf, marker);
									if (tlvData == null) { throw (new JimmException(115, 4)); }
									int tlvType = Util.getWord(buf, marker);
									if (tlvType == 0x0131)
									{
										nick = Util.byteArrayToString(tlvData, true);
									}
									else if (tlvType == 0x0066)
									{
										noAuth = true;
									}
									
									len -= 4;
									len -= tlvData.length;
									marker += 4 + tlvData.length;
								}
								if (len != 0) { throw (new JimmException(115, 5)); }

								// Add this contact item to the vector
								try
								{
									ContactItem item = new ContactItem(id, group, name, nick, noAuth, true);
									items.addElement(item);

									if ((type == 0x0019 || type == 0x001B))
									{
										item.setBooleanValue(ContactItem.CONTACTITEM_PHANTOM, true);
									}
								}
								catch (NumberFormatException ne)
								{
									// Contact with wrong uin was received  
								}
								catch (Exception e)
								{
									// Contact with wrong uin was received  
								}
							}
							// Group of contacts
							else if (type == 0x0001)
							{
								// Skip TLVs
								marker += len;

								// Add this group item to the vector
								if (group != 0x0000)
								{
									items.addElement(new GroupItem(group, name));
								}
//								else if (Options.getBoolean(Options.OPTION_CACHE_CONTACTS))
//								{
//									items.addElement(new GroupItem(group, "Phantoms"));
//								}
							}

							// Privacy Lists begining
							// Permit record ("Allow" list in AIM, and "Visible" list in ICQ)
							else if (type == 0x0002) 
							{
								marker += len;
								visibleList.put(name, new Integer(id));
							}
							// Deny record ("Block" list in AIM, and "Invisible" list in ICQ)
							else if (type == 0x0003) 
							{
								marker += len;
								invisibleList.put(name, new Integer(id));
							}
							// Ignore list record.
							else if (type == 0x000E) 
							{
								marker += len;
								ignoreList.put(name, new Integer(id));
							}
							// Privacy Lists ending

							// Permit/deny settings or/and bitmask of the AIM classes
							else if (type == 0x0004)
							{
								while (len > 0)
								{
									byte[] tlvData = Util.getTlv(buf, marker);
									if (tlvData == null) { throw (new JimmException(115, 110)); }
									int tlvType = Util.getWord(buf, marker);

									if (tlvType == 0x00CA) Icq.getIcq().setPrivateStatusId(id);

									len -= 4;
									len -= tlvData.length;
									marker += 4 + tlvData.length;
								}
								if (len != 0) { throw (new JimmException(115, 111)); }
							}
							// All other item types
							else
							{
								// Skip TLVs
								marker += len;
							}
						}

						// Check length
						if (buf.length != marker + 4) { throw (new JimmException(115, 6)); }

						// Get timestamp
						int timestamp = (int)Util.getDWord(buf, marker);

						// Update contact list
						ContactListItem[] itemsAsArray = new ContactListItem[items.size()];
						items.copyInto(itemsAsArray);
						ContactList.update(snacPacket.getFlags(), timestamp, count, itemsAsArray);

						// Packet has been consumed
						consumed = true;
					}

					// Check if all required packets have been received
					if (srvReplyRosterRcvd)
					{
						// Privacy Lists begining
						for (int i = 0; i < ContactList.cItems.size(); i++)
						{
							ContactItem contact = (ContactItem)ContactList.cItems.elementAt(i);
							ConnectAction.setPrivacyMarks(contact);
						}
						// Privacy Lists ending

						// Send a CLI_ROSTERACK packet
						SnacPacket reply1 = new SnacPacket
												(
													SnacPacket.ROSTER_FAMILY,
													SnacPacket.CLI_ROSTERACK_COMMAND,
													0x00000007, new byte[0], new byte[0]
												);
						con.sendPacket(reply1);

						// Send a CLI_SETUSERINFO packet
						// Set version information to this packet in our capability
						OtherAction.setStandartUserInfo();

						byte[] tmp_packet;

						// Send a CLI_SETICBM packet
						SnacPacket reply;

						//#sijapp cond.if target isnot "DEFAULT"#
						if (Options.getInt(Options.OPTION_TYPING_NOTIF_MODE) > 0)
						{
							reply = new SnacPacket
										(
											SnacPacket.ICBM_FAMILY,
											SnacPacket.CLI_SETICBM_COMMAND,
											0x00000000, new byte[0], CLI_SETICBM_DATA
										);
						}
						else
						{
						//#sijapp cond.end#
							tmp_packet = CLI_SETICBM_DATA;
							tmp_packet[5] = 0x03;
							reply = new SnacPacket
										(
											SnacPacket.ICBM_FAMILY,
											SnacPacket.CLI_SETICBM_COMMAND,
											0x00000000, new byte[0], tmp_packet
										);
						//#sijapp cond.if target isnot "DEFAULT"#
						}
						//#sijapp cond.end#
						con.sendPacket(reply);

						// Set STATE_CONNECTED
						Icq.setConnected();

						int onlineStatus = (int)Options.getLong(Options.OPTION_ONLINE_STATUS);

						// Set privacy status according to online status
						if (MainMenu.getPrivateStatusImage() == null)
						{
							byte bCode = OtherAction.PSTATUS_NOT_INVISIBLE;
							if (onlineStatus == ContactList.STATUS_INVIS_ALL) bCode = OtherAction.PSTATUS_NONE;
							if (onlineStatus == ContactList.STATUS_INVISIBLE) bCode = OtherAction.PSTATUS_VISIBLE_ONLY;
							Icq.getIcq().setPrivateStatus(bCode);
						}
						else Icq.getIcq().setPrivateStatus((byte)Options.getInt(Options.OPTION_PRIVATE_STATUS));

						// Set online status
						OtherAction.setStatus(Icq.setWebAware() | onlineStatus);

						// Move to next state
						state = STATE_CLI_STATUS_INFO_SENT;
					}
				}
			}
			// Watch out for STATE_CLI_STATUS_INFO_SENT
			else if (state == STATE_CLI_STATUS_INFO_SENT)
			{
				// Send a CLI_READY packet
				SnacPacket reply2 = new SnacPacket
									(
										SnacPacket.SERVICE_FAMILY,
										SnacPacket.CLI_READY_COMMAND,
										0x00000000, new byte[0], CLI_READY_DATA
									);
				con.sendPacket(reply2);

				// Send a CLI_TOICQSRV/CLI_REQOFFLINEMSGS packet
				ToIcqSrvPacket reply3 = new ToIcqSrvPacket
										(
											0x00000000, uin,
											ToIcqSrvPacket.CLI_REQOFFLINEMSGS_SUBCMD,
											new byte[0], new byte[0]
										);
				con.sendPacket(reply3);

				// Move to next state
				state = STATE_CLI_REQOFFLINEMSGS_SENT;
			}
			// Watch out for STATE_CLI_REQOFFLINEMSGS_SENT
			else if (state == STATE_CLI_REQOFFLINEMSGS_SENT)
			{
				if (packet instanceof SnacPacket)
				{
					SnacPacket snPacket = (SnacPacket)packet;

					// Error after requesting offline messages?
					if ((snPacket.getFamily() == 0x0015) && (snPacket.getCommand() == 0x0001))
					{
						// Move to next state
						state = ConnectAction.STATE_CLI_ACKOFFLINEMSGS_SENT;

						// Packet has been consumed
						consumed = true;
					}
				}

				if ((packet instanceof FromIcqSrvPacket) && !consumed)
				{
					FromIcqSrvPacket fromIcqSrvPacket = (FromIcqSrvPacket) packet;

					if (fromIcqSrvPacket.getFamily() == 0x0015)
					{
						// Watch out for SRV_OFFLINEMSG
						if (fromIcqSrvPacket.getSubcommand() == FromIcqSrvPacket.SRV_OFFLINEMSG_SUBCMD)
						{
							// Get raw data
							byte[] buf = fromIcqSrvPacket.getData();

							// Check length
							if (buf.length < 14) return false;

							// Extract UIN
							long uinRaw = Util.getDWord(buf, 0, false);

							String uin = String.valueOf(uinRaw);

							// Extract date of dispatch
							long date = Util.createLongTime
										(
											Util.getWord(buf, 4, false),
											Util.getByte(buf, 6),
											Util.getByte(buf, 7),
											Util.getByte(buf, 8),
											Util.getByte(buf, 9),
											0
										);

							// Get type
							int type = Util.getWord(buf, 10, false);

							// Get text length
							int textLen = Util.getWord(buf, 12, false);
							
							String text = null;

							// Check length
							if (buf.length >= 14 + textLen)
							{
								text = Util.removeCr(Util.byteArrayToString(buf, 14, textLen, Util.isDataUTF8(buf, 14, textLen)));
							}

							if (text == null) {/* Do nothing */}

							// Normal message
							else if (type == 0x0001)
							{
								// Forward message to contact list
								PlainMessage message = new PlainMessage(uin, uin, Util.gmtTimeToLocalTime(date), text, true);
								RunnableImpl.addMessageSerially(message);
							}
							// URL message
							else if (type == 0x0004)
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
								}
								else
								{
									urlText = text;
									url = "";
								}

								// Forward message message to contact list
								UrlMessage message = new UrlMessage(uin, uin, Util.gmtTimeToLocalTime(date), url, urlText);
								RunnableImpl.addMessageSerially(message);
							}
							// Packet has been consumed
							consumed = true;
						}
						// Watch out for SRV_DONEOFFLINEMSGS
						else if (fromIcqSrvPacket.getSubcommand() == FromIcqSrvPacket.SRV_DONEOFFLINEMSGS_SUBCMD)
						{
							// Send a CLI_TOICQSRV/CLI_ACKOFFLINEMSGS packet
							ToIcqSrvPacket reply = new ToIcqSrvPacket(0x00000000, uin, ToIcqSrvPacket.CLI_ACKOFFLINEMSGS_SUBCMD, new byte[0], new byte[0]);
							con.sendPacket(reply);

							// Move to next state
							state = STATE_CLI_ACKOFFLINEMSGS_SENT;

							// Packet has been consumed
							consumed = true;
						}
					}
				}
			}

			// Update activity timestamp and reset activity flag
			lastActivity = System.currentTimeMillis();
			active = false;

			// Return consumption flag
			return (consumed);
		}
		// Catch JimmExceptions
		catch (JimmException e)
		{
			// Update activity timestamp and reset activity flag
			lastActivity = System.currentTimeMillis();
			active = false;

			// Set error state if exception is critical
			if (e.isCritical()) state = STATE_ERROR;

			// Forward exception
			throw (e);
		}
	}

	// Privacy Lists begining
	public static void setPrivacyMarks(ContactItem cItem)
	{
		String uin = cItem.getUinString();
		Integer iid = (Integer)invisibleList.get(uin);
		cItem.setInvisibleId((iid != null) ? iid.intValue() : 0);
		Integer vid = (Integer)visibleList.get(uin);
		cItem.setVisibleId((vid != null) ? vid.intValue() : 0);
		Integer xid = (Integer)ignoreList.get(uin);
		cItem.setIgnoreId((xid != null) ? xid.intValue() : 0);
	}
	// Privacy Lists ending

	// Returns true if the action is completed
	public boolean isCompleted()
	{
		return cancel || (state == STATE_CLI_ACKOFFLINEMSGS_SENT);
	}

	// Returns true if an error has occured
	public boolean isError()
	{
		if (cancel || con != Icq.c) return true;

		if ((state != STATE_ERROR) && !active && (lastActivity + TIMEOUT < System.currentTimeMillis()))
		{
			JimmException e = new JimmException(118, 0);
			if(!Icq.reconnect(e)) JimmException.handleException(e);
			state = STATE_ERROR;
		}
		return (state == STATE_ERROR);
	}

	// Returns a number between 0 and 100 (inclusive) which indicates the current progress
	public int getProgress()
	{
		switch (state)
		{
			case STATE_INIT:                    return   3;
			case STATE_INIT_DONE:               return  12;
			case STATE_AUTHKEY_REQUESTED:       return  20;
			case STATE_CLI_IDENT_SENT:          return  32;
			case STATE_CLI_DISCONNECT_SENT:     return  45;
			case STATE_CLI_COOKIE_SENT:         return  57;
			case STATE_CLI_WANT_CAPS_SENT:      return  64;
			case STATE_CLI_CHECKROSTER_SENT:    return  71;
			case STATE_CLI_STATUS_INFO_SENT:    return  79;
			case STATE_CLI_REQOFFLINEMSGS_SENT: return  87;
			case STATE_CLI_ACKOFFLINEMSGS_SENT: return 100;
			default: return 0;
		}
	}

	public String getProgressMsg()
	{
		switch (state)
		{
			case STATE_AUTHKEY_REQUESTED:       return "Sending auth key";
			case STATE_CLI_IDENT_SENT:          return "Sending client ID";
			case STATE_CLI_DISCONNECT_SENT:     return "Changing server";
			case STATE_CLI_COOKIE_SENT:         return "Sending cookies";
			case STATE_CLI_WANT_CAPS_SENT:
			case STATE_CLI_CHECKROSTER_SENT:    return "Checking roster";
			case STATE_CLI_STATUS_INFO_SENT:    return "Sending statuses";
			case STATE_CLI_REQOFFLINEMSGS_SENT: return "Reading messages";
			case STATE_CLI_ACKOFFLINEMSGS_SENT: return "Ready to chat!";
			default: return srvHost;
		}
	}

	public void onEvent(int eventType)
	{
		switch (eventType)
		{
		case ON_COMPLETE:
			ContactList.activate();
			ContactList.afterConnect();
			if (Options.getBoolean(Options.OPTION_STATUS_AUTO)) TimerTasks.setStatusTimer();
			break;

		case ON_CANCEL:
			Icq.connecting = false;
			cancel = true;
			Icq.disconnect();
			RunnableImpl.backToLastScreen();
			break;
		}
	}
}