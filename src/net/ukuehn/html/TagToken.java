/* -*- java -*-
 *
 * (C) 2012 Ulrich Kuehn <ukuehn@acm.org>
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


public class TagToken extends Token {

	protected String[] scanAttr;
	protected String[] attr;
	protected String[] value;
	protected int nAttr;


	public TagToken(String parm, String[] parmAttr)
		throws ParserException {
		htmlVal = parm;
		scanAttr = parmAttr;
		if ((scanAttr == null) || (scanAttr.length == 0)) {
			scanAttr = new String[1];
			scanAttr[0] = new String();
		}
		scanAttrToAttrib();
	}


	public String getName() {
		return scanAttr[0];
	}


	public int getAttrCount() {
		return nAttr;
	}


	public String getAttr(int i) {
		if ((i >= 0) && (i < nAttr)) {
			return attr[i];
		} else {
			return null;
		}
	}


	public int attrIndex(String a) {
		if (a != null) {
			if (Debug.get(Debug.HToken)) {
				System.err.println("TagToken.getAttr("
						   +a+"):");
			}			
			for (int i = 0;  i < nAttr;  i++) {
				if (Debug.get(Debug.HToken)) {
					System.err.println("  Compare to "
							   +"'"+attr[i]
							   +"'");
				}
				if (a.equalsIgnoreCase(attr[i])) {
					return i;
				}
			}
		}
		return -1;
	}


	public String getValue(int i) {
		if ((i >= 0) && (i < nAttr)) {
			return value[i];
		} else {
			return null;
		}
	}


	protected String ensureUnquoted(String s) {
		int len = s.length();
		if ( ((s.charAt(0) == '"') && (s.charAt(len-1) == '"')) ||
		     ((s.charAt(0) == '\'') && (s.charAt(len-1) == '\'')) ) {
			return s.substring(1, len-1);
		}
		return s;
	}


	protected void scanAttrToAttrib()
		throws ParserException {

		int n, i;

		if (scanAttr[0].startsWith("!")) {
			// Markup tag, like <!DOCTPYE ...>
			nAttr = scanAttr.length-1;
			attr = new String[nAttr];
			value = new String[nAttr];
			for (i = 0;  i < nAttr;  i += 1) {
				attr[i] = scanAttr[i+1];
				value[i] = null;
			}
			return;
		}
		if ( ((scanAttr.length-1) % 3) != 0 ) {
			throw new ParserException("Illegal attribute "
						  +"structure in tag "
						  +scanAttr[0]+".");
		}
		if (Debug.get(Debug.HToken)) {
			System.err.println("TagToken: Attributes for tag "
					   +scanAttr[0]);
		}

		nAttr = (scanAttr.length-1) / 3;
		attr = new String[nAttr];
		value = new String[nAttr];

		for (n = 0, i = 1;  n < nAttr;  n += 1, i += 3) {
			if (!scanAttr[1+(3*n)+1].equals("=")) {
				throw new ParserException(
					      "Illegal attribute structure "
					      +"in tag "+scanAttr[0]
					      +", must be key = value.");
			}
			attr[n] = scanAttr[1+(3*n)];
			value[n] = ensureUnquoted(scanAttr[1+(3*n)+2]);
			if (Debug.get(Debug.HToken)) {
				System.err.println("  "+attr[n]
						   +" = "+value[n]);
			}
		}
	}

}
