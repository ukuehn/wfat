/* -*- java -*-
 *
 * (C) 2012, 2013 Ulrich Kuehn <ukuehn@acm.org>
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
 */

package net.ukuehn.html;

import net.ukuehn.util.Debug;


public class HttpEquivResult {

	protected String[] key;
	protected String[] val;
	protected int nEntry;


	public HttpEquivResult(String[] parmKey, String[] parmVal) {
		key = parmKey;
		val = parmVal;
		nEntry = key.length;
		if (val.length < nEntry) {
			nEntry = val.length;
		}

		if (Debug.get(Debug.HToken)) {
			System.err.println("HttpEquivResult() with "
					   +String.valueOf(nEntry)
					   +" entries:");
			for (int i = 0;  i < nEntry;  i++) {
				System.err.println("  '"+key[i]+"' '"
						   +val[i]+"'");
			}
		}
	}


	public int getCount() {
		return nEntry;
	}


	public String getKey(int i) {
		if ((i >= 0) && (i < nEntry)) {
			return key[i];
		} else {
			return null;
		}
	}


	public int getKey(String k) {
		if (k != null) {
			for (int i = 0;  i < nEntry;  i++) {
				if (k.equalsIgnoreCase(key[i])) {
					return i;
				}
			}
		}
		return -1;
	}


	public String getVal(int i) {
		if ((i >= 0) && (i < nEntry)) {
			return val[i];
		} else {
			return null;
		}
	}

}
