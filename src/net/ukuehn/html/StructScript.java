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
 */

package net.ukuehn.html;

import net.ukuehn.wfat.Digester;
import net.ukuehn.util.Hex;
import net.ukuehn.xml.XML;

import java.lang.StringBuilder;
import java.security.MessageDigest;


public class StructScript extends StructElem {

	protected String htmlVal;
	protected String scVal;
	protected String src;
	protected TagToken ot, ct;
	protected String lang;
	protected int type;
	protected byte[] srcDigest;
	protected byte[] contentDigest;
	protected String srcHash;
	protected String contentHash;

	public static final int ELEMENT = 0; // script given as element value
	public static final int SOURCED = 1; // script from network source
	public static final int INLINE = 2;  // script in src attibute


	public StructScript(TagToken openToken, TagToken closeToken,
			    int scriptType, String value) {
		ot = openToken;
		ct = closeToken;
		lang = ot.getValue(ot.attrIndex("type"));
		type = scriptType;
		scVal = null;
		src = null;
		switch (type) {
		case ELEMENT:
			scVal = value;
			htmlVal = ot.toString()+scVal+ct.toString();
			break;
		case SOURCED:
		case INLINE:
			src = value;
			htmlVal = ot.toString()+ct.toString();
			break;
		}
		srcDigest = null;
		srcHash = "";
		contentDigest = null;
		contentHash = "";
	}


	public String toString() {
		return htmlVal;
	}


	public String digestInput(Digester d) {
		MessageDigest md = d.getDigester();
		switch (type) {
		case ELEMENT:
			contentDigest = md.digest(scVal.getBytes());
			contentHash = Hex.toHex(contentDigest);
			break;
		case SOURCED:
			srcDigest = md.digest(src.getBytes());
			srcHash = Hex.toHex(srcDigest);
			break;
		case INLINE:
			srcDigest = md.digest(src.getBytes());
			srcHash = Hex.toHex(srcDigest);
			break;
		}

		StringBuilder sb = new StringBuilder();
		sb.append("<Script");
		sb.append(" Lang=");
		if (lang != null) {
			sb.append(XML.quote(lang));
		} else {
			sb.append(XML.quote(""));
		}
		sb.append(" SrcAttr=");
		sb.append(XML.quote(src));
		sb.append(" SrcHash=");
		sb.append(XML.quote(srcHash));
		sb.append(" ContentHash=");
		sb.append(XML.quote(contentHash));
		/*
		switch (type) {
		case ELEMENT:
			break;
		case SOURCED:
		case INLINE:
			sb.append(XML.quote(src));
			break;
		}
		*/
		sb.append(" />");
		return sb.toString();
	}

}
