/* -*- java -*-
 *
 * (C) 2013 Ulrich Kuehn <ukuehn@acm.org>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA.
 *
 */

package net.ukuehn.util;



import java.lang.StringBuilder;



public class Hex {

	protected static final String hexchar = "0123456789abcdef";

	public static String toHex(byte[] b) {
		if (b == null) {
			return null;
		}
		StringBuilder sb = new StringBuilder(b.length*2);
		for (int i = 0;  i < b.length;  i++) {
			int hi = (b[i] >> 4) & 0x0f;
			int lo = b[i] & 0x0f;
			sb.append(hexchar.charAt(hi));
			sb.append(hexchar.charAt(lo));
		}
		return sb.toString();
	}

}
