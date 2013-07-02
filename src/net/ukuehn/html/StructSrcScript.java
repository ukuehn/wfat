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


import net.ukuehn.xml.XML;

import java.net.URL;
import java.lang.StringBuilder;



public class StructSrcScript extends StructElem {

	protected String htmlVal;
	protected String src;
	protected TagToken ot, ct;
	protected String lang;


	public StructSrcScript(TagToken openToken,
			    TagToken closeToken, String scriptSource) {
		ot = openToken;
		ct = closeToken;
		src = scriptSource;
		htmlVal = ot.toString()+ct.toString();
		lang = ot.getValue(ot.attrIndex("type"));
	}


	public String toString() {
		return htmlVal;
	}


	public String digestInput() {
		StringBuilder sb = new StringBuilder();
		sb.append("<SrcScript src=");
		sb.append(XML.quote(XML.encode(src)));
		sb.append(" lang=");
		if (lang != null) {
			sb.append(XML.quote(XML.encode(lang)));
		} else {
			sb.append(XML.quote(""));
		}
		sb.append(" />");
		return sb.toString();
	}

}
