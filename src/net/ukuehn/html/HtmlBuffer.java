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


import java.io.*;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Iterator;

import org.xml.sax.SAXException;
import java.net.URLConnection;
import java.net.HttpURLConnection;


import net.ukuehn.util.Debug;



public class HtmlBuffer {

	HtmlTokenizer ht;
	ArrayList<Token> docTokens;
	boolean haveAllTokens;

	public HtmlBuffer() {
		ht = null;
		haveAllTokens = true;
		docTokens = new ArrayList<Token>();
	}


	public HtmlBuffer(HttpURLConnection conn)
		throws ParserException {
		ht = new HtmlTokenizer(conn);
		haveAllTokens = false;
		//docTokens = new ArrayList<Token>();
		docTokens = getDocumentTokens();
		close();
	}


	public void close() {
		haveAllTokens = true;
		try {
			ht.close();
		} catch (Exception e) {
			// ignore
		}
	}


	public Iterator<Token> iterator() {
		return docTokens.iterator();
	}


	public Token[] toArray() {
		return docTokens.toArray(new Token[0]);
	}


	public int numberOfToken() {
		if (docTokens != null) {
			return docTokens.size();
		} else {
			return 0;
		}
	}


	public Token getToken(int i) {
		if (docTokens == null) {
			return null;
		}
		return docTokens.get(i);
	}


	protected boolean needsNonHTMLParsingNext(Token t) {
		if (t instanceof TagToken) {
			TagToken tt = (TagToken)t;
			if (tt.getName().equalsIgnoreCase("script")) {
				return true;
			}
			if (tt.getName().equalsIgnoreCase("style")) {
				return true;
			}
		}
		return false;
	}


	protected ArrayList<Token> getDocumentTokens()
		throws ParserException {

		ArrayList<Token> tokens = new ArrayList<Token>();

		if (Debug.get(Debug.HToken)) {
			System.err.println("getDocumentTokens()");
		}

		Token t = ht.nextToken();
		while (t != null) {
			tokens.add(t);
			if (Debug.get(Debug.HToken)) {
				debugToken(t);
			}
			if (needsNonHTMLParsingNext(t)) {
				t = ht.nextTokenNonHTML();
			} else {
				t = ht.nextToken();
			}
		}

		return tokens;
	}


	protected void debugToken(Token t) {

		if (t instanceof TagToken) {
			TagToken tt = (TagToken)t;
			System.err.print("HtmlBuffer: Found tag "
					 +tt.getName()+": '");
			for (int i = 0;  i < tt.getAttrCount();  i++) {
				if (i > 0) {
					System.err.print(" ");
				}
				System.err.print(tt.getAttr(i)
						 +"=\""
						 +tt.getValue(i)
						 +"\"");
			}
			System.err.println("'");
		} else {
			System.err.println("HtmlBuffer: " + t.toString());
		}
		System.err.println();
	}


}
