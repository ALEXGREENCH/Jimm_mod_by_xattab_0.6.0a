/*******************************************************************************
 Jimm - Mobile Messaging - J2ME ICQ clone
 Copyright (C) 2003-04  Jimm Project

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
 File: src/jimm/comm/RequestInfoAction.java
 Version: ###VERSION###  Date: ###DATE###
 Author(s): Manuel Linsmayer
 *******************************************************************************/

package jimm.comm;

import java.util.Date;
import java.io.DataInputStream;

import jimm.*;
import jimm.util.*;

public class RequestInfoAction extends Action
{
	// Receive timeout
	private static final int TIMEOUT = 10 * 1000; // milliseconds

	private boolean infoShown;

	public static boolean StartMainRequestInfo = false;

	private boolean showInfoText = true;

	public static int indexCategories[] = {0,0,0,0};

	private static int[] codeIndexes = new int[51];
	private static String[] interestNames = new String[51];

	static
	{
		initInterestsData();
	}

	/****************************************************************************/

	private String[] strData = new String[JimmUI.UI_LAST_ID];

	// Date of init
	private Date init;

	private int packetCounter;

	private boolean notFound = false;

	private String existingNick;

	private String uin_bDay;
	private int day_bDay, month_bDay;

	// Constructor
	public RequestInfoAction(String uin, String nick)
	{
		super(false, true);
		existingNick = nick;
		infoShown = false;
		packetCounter = 0;
		strData[JimmUI.UI_UIN] = uin;
	}

	// Init action
	protected void init() throws JimmException
	{
		// Send a CLI_METAREQINFO packet
		byte[] buf = new byte[6];
		Util.putWord(buf, 0, ToIcqSrvPacket.CLI_META_REQMOREINFO_TYPE, false);
		Util.putDWord(buf, 2, Long.parseLong(strData[JimmUI.UI_UIN]), false);
		ToIcqSrvPacket packet = new ToIcqSrvPacket(0, Options.getString(Options.OPTION_UIN), ToIcqSrvPacket.CLI_META_SUBCMD, new byte[0], buf);
		Icq.c.sendPacket(packet);

		// Save date
		this.init = new Date();
	}

	// Forwards received packet, returns true if packet was consumed
	protected synchronized boolean forward(Packet packet) throws JimmException
	{
		boolean consumed = false;

		// Watch out for SRV_FROMICQSRV packet
		if (packet instanceof FromIcqSrvPacket)
		{
			FromIcqSrvPacket fromIcqSrvPacket = (FromIcqSrvPacket) packet;

			// Watch out for SRV_META packet
			if (fromIcqSrvPacket.getSubcommand() != FromIcqSrvPacket.SRV_META_SUBCMD) return false;

			// Get packet data
			DataInputStream stream = Util.getDataInputStream(fromIcqSrvPacket.getData(), 0);

			// Watch out for SRV_METAGENERAL packet
			try
			{
				int type = Util.getWord(stream, false);
				int successByte = stream.readByte(); // Success byte

				switch (type)
				{
					case FromIcqSrvPacket.SRV_META_GENERAL_TYPE: // basic user information
					{
						strData[JimmUI.UI_NICK] = Util.readAsciiz(stream); // nickname
						// first name + last name
						String fistName = Util.readAsciiz(stream);
						String lastName = Util.readAsciiz(stream);
						strData[JimmUI.UI_FIRST_NAME] = fistName;
						strData[JimmUI.UI_LAST_NAME] = lastName;
						if ((fistName.length() != 0) || (lastName.length() != 0)) strData[JimmUI.UI_NAME] = fistName + " " + lastName;
						strData[JimmUI.UI_EMAIL] = Util.readAsciiz(stream); // email
						strData[JimmUI.UI_CITY] = Util.readAsciiz(stream); // home city
						strData[JimmUI.UI_STATE] = Util.readAsciiz(stream); // home state
						strData[JimmUI.UI_PHONE] = Util.readAsciiz(stream); // home phone
						strData[JimmUI.UI_FAX] = Util.readAsciiz(stream); // home fax
						strData[JimmUI.UI_ADDR] = Util.readAsciiz(stream); // home address
						strData[JimmUI.UI_CPHONE] = Util.readAsciiz(stream); // cell phone
						uin_bDay = strData[JimmUI.UI_UIN];

						if (StartMainRequestInfo && uin_bDay.equals(Options.getString(Options.OPTION_UIN)))
						{
							Util.readAsciiz(stream); // home zip code
							Util.getWord(stream, false);
							byte[] buffer = new byte[3];
							stream.readFully(buffer);
							Options.setBoolean(Options.OPTION_WEB_AWARE, Util.getByte(buffer, 2) == 0x00 ? false : true);
							Options.setBoolean(Options.OPTION_MY_AUTH, Util.getByte(buffer, 1) == 0x00 ? true : false);
							Icq.setPoint = true;
							infoShown = true;
							if (strData[JimmUI.UI_NICK].length() > 0)
							{
								Icq.myNick = strData[JimmUI.UI_NICK];
							}
							StartMainRequestInfo = false;
						}

						packetCounter++;
						consumed = true;
						break;
					}

					case 0x00DC: // more user information
					{
						int age = Util.getWord(stream, false);
						strData[JimmUI.UI_AGE] = (age != 0) ? Integer.toString(age) : new String();
						strData[JimmUI.UI_GENDER] = Util.genderToString(stream.readByte());
						strData[JimmUI.UI_HOME_PAGE] = Util.readAsciiz(stream);
						int year = Util.getWord(stream, false);
						int mon = stream.readByte();
						int day = stream.readByte();
						month_bDay = mon;
						day_bDay = day;
						strData[JimmUI.UI_BDAY] = (year != 0) ? day + "." + mon + "." + year : new String();
						packetCounter++;
						consumed = true;
						break;
					}

					case 0x00D2: // work user information
					{
						for (int i = JimmUI.UI_W_CITY; i <= JimmUI.UI_W_ADDR; i++) strData[i] = Util.readAsciiz(stream); // city - address
						Util.readAsciiz(stream); // work zip code
						Util.getWord(stream, false); // work country code
						strData[JimmUI.UI_W_NAME] = Util.readAsciiz(stream); // work company
						strData[JimmUI.UI_W_DEP] = Util.readAsciiz(stream); // work department
						strData[JimmUI.UI_W_POS] = Util.readAsciiz(stream); // work position
						packetCounter++;
						consumed = true;
						break;
					}

					case 0x00E6: // user about information
					{
						strData[JimmUI.UI_ABOUT] = Util.readAsciiz(stream); // notes string
						packetCounter++;
						consumed = true;
						break;
					}

					case 0x00F0: // user interests information
					{
						for( int i = 0; i < 4; i++)
						{
							indexCategories[i] = 0;
						}
						int counter = stream.readByte();
						int categories;
						for( int i = 0; i < counter; i++)
						{
							int idx = 40+i;
							int idx1 = 44+i;

							categories = Util.getWord(stream, false);
							if (uin_bDay.equals(Options.getString(Options.OPTION_UIN)))
							{
								indexCategories[i] = categories;
							}
							strData[idx] = getCategoriesString(categories);
							strData[idx1] = "\n" + Util.readAsciiz(stream);
						}

						packetCounter++;
						consumed = true;
						break;
					}

					case 0x00FA: // end snac
					{
						if (successByte != 0x0A)
						{
							notFound = true;
						}
						packetCounter++;
						consumed = true;
						break;
					}
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}

			if (isCompleted())
			{
				if (!infoShown && showInfoText)
				{
					RunnableImpl.callSerially(RunnableImpl.TYPE_SHOW_USER_INFO, (Object)strData);
					tryToChangeName();
					infoShown = true;
					//#sijapp cond.if target is "MIDP2" | target is "SIEMENS2"#
					if (!notFound)
					{
						NoticeOnBirthDay.additemB(uin_bDay, day_bDay, month_bDay);
					}
					//#sijapp cond.end#
				}
			}
		} // end 'if (packet instanceof FromIcqSrvPacket)'

		return (consumed);
	}

	private static void initInterestsDataItem(String name, int index, int code)
	{
		interestNames[index] = name;
		codeIndexes[index] = code;
	}

	private static void initInterestsData()
	{
		//                        name      index  code
		initInterestsDataItem("---"       ,   0,  0x0000 );
		initInterestsDataItem("interest_1",   1,  0x0089 );
		initInterestsDataItem("interest_2",   2,  0x0086 );
		initInterestsDataItem("interest_3",   3,  0x0087 );
		initInterestsDataItem("interest_4",   4,  0x0088 );
		initInterestsDataItem("interest_5",   5,  0x006d );
		initInterestsDataItem("interest_6",   6,  0x0090 );
		initInterestsDataItem("interest_7",   7,  0x0065 );
		initInterestsDataItem("interest_8",   8,  0x0080 );
		initInterestsDataItem("interest_9",   9,  0x0093 );
		initInterestsDataItem("interest_10", 10,  0x007d );
		initInterestsDataItem("interest_11", 11,  0x0092 );
		initInterestsDataItem("interest_12", 12,  0x0079 );
		initInterestsDataItem("interest_13", 13,  0x0083 );
		initInterestsDataItem("interest_14", 14,  0x0078 );
		initInterestsDataItem("interest_15", 15,  0x0073 );
		initInterestsDataItem("interest_16", 16,  0x007c );
		initInterestsDataItem("interest_17", 17,  0x0074 );
		initInterestsDataItem("interest_18", 18,  0x0084 );
		initInterestsDataItem("interest_19", 19,  0x0072 );
		initInterestsDataItem("interest_20", 20,  0x006b );
		initInterestsDataItem("interest_21", 21,  0x0095 );
		initInterestsDataItem("interest_22", 22,  0x006e );
		initInterestsDataItem("interest_23", 23,  0x0064 );
		initInterestsDataItem("interest_24", 24,  0x0091 );
		initInterestsDataItem("interest_25", 25,  0x0070 );
		initInterestsDataItem("interest_26", 26,  0x0067 );
		initInterestsDataItem("interest_27", 27,  0x0068 );
		initInterestsDataItem("interest_28", 28,  0x0081 );
		initInterestsDataItem("interest_29", 29,  0x008e );
		initInterestsDataItem("interest_30", 30,  0x0069 );
		initInterestsDataItem("interest_31", 31,  0x006c );
		initInterestsDataItem("interest_32", 32,  0x008d );
		initInterestsDataItem("interest_33", 33,  0x008f );
		initInterestsDataItem("interest_34", 34,  0x007e );
		initInterestsDataItem("interest_35", 35,  0x0071 );
		initInterestsDataItem("interest_36", 36,  0x0076 );
		initInterestsDataItem("interest_37", 37,  0x007b );
		initInterestsDataItem("interest_38", 38,  0x0085 );
		initInterestsDataItem("interest_39", 39,  0x0082 );
		initInterestsDataItem("interest_40", 40,  0x0077 );
		initInterestsDataItem("interest_41", 41,  0x007f );
		initInterestsDataItem("interest_42", 42,  0x008b );
		initInterestsDataItem("interest_43", 43,  0x0075 );
		initInterestsDataItem("interest_44", 44,  0x006f );
		initInterestsDataItem("interest_45", 45,  0x0096 );
		initInterestsDataItem("interest_46", 46,  0x0066 );
		initInterestsDataItem("interest_47", 47,  0x0094 );
		initInterestsDataItem("interest_48", 48,  0x008a );
		initInterestsDataItem("interest_49", 49,  0x006a );
		initInterestsDataItem("interest_50", 50,  0x007a );
	}

	public String getCategoriesString(int index)
	{
		for (int i = 0; i < 51; i++)
		{
			if (codeIndexes[i] == index)
			{
				return (interestNames[i]);
			}
		}
		return null;
	}

	public static int getSelectIndex(int index)
	{
		for (int i = 0; i < 51; i++)
		{
			if (codeIndexes[i] == index)
			{
				return i;
			}
		}
		return 0;
	}

	public static int  getCategoriesCode(int index)
	{
		return (codeIndexes[index]);
	}

	public static String getCategoriesName(int index)
	{
		return (interestNames[index]);
	}

	// Rename contact if its name consists of digits
	private void tryToChangeName()
	{
		if (strData[JimmUI.UI_UIN].equals(existingNick))
		{
			ContactItem item = ContactList.getItembyUIN(strData[JimmUI.UI_UIN]);
			item.rename(strData[JimmUI.UI_NICK]);
		}
	}

	// Returns true if the action is completed
	public synchronized boolean isCompleted()
	{
		return ((packetCounter >= 5) || notFound);
	}

	// Returns true if an error has occured
	public synchronized boolean isError()
	{
		return (this.init.getTime() + RequestInfoAction.TIMEOUT < System.currentTimeMillis());
	}
}