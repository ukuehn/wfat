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




public class StructDoctype extends StructElem {

	protected String htmlVal;
	protected TagToken dt;

	public StructDoctype(TagToken token) {
		dt = token;
		htmlVal = dt.toString();
	}


	public String toString() {
		return htmlVal;
	}


	public String digestInput(Digester d) {
		StringBuilder sb = new StringBuilder();

		sb.append("<DoctypeElement Content=");
		sb.append(XML.quote(htmlVal));
		sb.append(" />");
		return sb.toString();
	}
	
}
