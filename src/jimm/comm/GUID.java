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
 File: src/jimm/comm/GUID.java
 Version: ###VERSION###  Date: ###DATE###
 Author(s): aspro
 *******************************************************************************/

package jimm.comm;

public class GUID
{
	private byte[] guid;

	public GUID(byte[] guid)
	{
		this.guid = guid;
	}

	public boolean equals(byte[] data)
	{
		if (data.length != guid.length)
		{
			return false;
		}
		for (int i = 0; i < guid.length; i++) 
		{
			if (data[i] != guid[i]) 
			{
				return false;
			}
		}
		return true;
	}

	public byte[] toByteArray()
	{
		return guid;
	}
}