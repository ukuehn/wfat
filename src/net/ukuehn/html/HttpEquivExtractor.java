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


import java.io.*;
import java.util.LinkedList;
import java.util.Iterator;

import org.xml.sax.SAXException;
import java.net.URLConnection;
import java.net.HttpURLConnection;


import net.ukuehn.util.Debug;




public class HttpEquivExtractor {

	HtmlBuffer html;

	public HttpEquivExtractor(HttpURLConnection conn)
		throws ParserException {
		html = new HtmlBuffer(conn);
	}

	public HttpEquivExtractor(HtmlBuffer hb) {
		html = hb;
	}


	public HttpEquivResult extract() throws ParserException {
		Token t;
		TagToken tt;
		LinkedList<String> keys = new LinkedList<String>();
		LinkedList<String> vals = new LinkedList<String>();
		String equivHdr;
		String equivContent;
		Iterator<Token> it;
		int i;

		if (Debug.get(Debug.HToken)) {
			System.err.println("HttpEquivExtractor.extract()");
		}

		it = html.iterator();
		while (it.hasNext()) {
			t = it.next();
			if (!(t instanceof TagToken)) {
				continue;
			}
			tt = (TagToken)t;
			if (!tt.getName().equalsIgnoreCase("meta")) {
				continue;
			}

			if (Debug.get(Debug.HToken)) {
				System.err.print("HEE.extract: Found tag "
						 +tt.getName()+": ");
				for (i = 0;
				     i < tt.getAttrCount();  i++) {
					if (i > 0) {
						System.err.print(" ");
					}
					System.err.print(tt.getAttr(i)
							 +"=\""
							 +tt.getValue(i)
							 +"\"");
				}
				System.err.println("'");
			}

			equivHdr = null;
			equivContent = null;

			i = tt.attrIndex("http-equiv");
			if (Debug.get(Debug.HToken)) {
				System.err.println("HEE.extract: got index "
						   +String.valueOf(i)
						   +" for http-equiv");
			}
			if (i >= 0) {
				equivHdr = tt.getValue(i);
				if (Debug.get(Debug.HToken)) {
					System.err.println("  -> '"
							   +equivHdr+"'");
				}
			}
			i = tt.attrIndex("content");
			if (Debug.get(Debug.HToken)) {
				System.err.println("HEE.extract: got index "
						   +String.valueOf(i)
						   +" for content");
			}
			if (i >= 0) {
				equivContent = tt.getValue(i);
				if (Debug.get(Debug.HToken)) {
					System.err.println("  -> '"
							   +equivContent+"'");
				}
			}

			if ((equivHdr != null) && (equivContent != null)) {
				if (Debug.get(Debug.HToken)) {
					System.err.println(
						"Adding equiv header "
						+equivHdr+": "+equivContent);
				}
				keys.add(equivHdr);
				vals.add(equivContent);
			} else {
				// could throw exception here, but
				// try to continue with other meta tags,
				// so ignore ...
			}
		}

		HttpEquivResult res
			= new HttpEquivResult(keys.toArray(new String[0]),
					      vals.toArray(new String[0]));
		return res;
	}



}
