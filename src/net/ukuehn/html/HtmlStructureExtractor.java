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


/* Extract only those parts of an html document that are relevant
 * to compute the structural hash:
 *
 * <!DOCTYPE> 
 * <script>   contents up to closing </script>. Contains only PCDATA, no
 *            tags allowed.
 * <style>    contents up to closing </style>. Contains only PCDATA, no
 *            tags allowed.
 * <frame>    empty element.
 * <base>     empty element.
 * <link>     if attribute rel="stylesheet". empty element.
 * <iframe>   contents up to closing </iframe>. Contents is for agents
 *            not supporting iframes.
 * <object>   ?
 * <embed>    ?
 * <applet>   ?
 *
 * and additionally any tag with events containing javascript
 *
 */


package net.ukuehn.html;


import java.io.*;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.Arrays;

import org.xml.sax.SAXException;
import java.net.URL;
import java.net.MalformedURLException;
import java.net.URLConnection;
import java.net.HttpURLConnection;


import net.ukuehn.util.Debug;
import net.ukuehn.wfat.ConnectionHandler;
import net.ukuehn.wfat.Digester;


public class HtmlStructureExtractor {

	private String dbgPref = "HSE";

	private HtmlBuffer html;
	private URL htmlSrc;
	private URL baseUrl;
	private ConnectionHandler hc;
	private Digester d;
	LinkedList<StructElem> structure;


	// Make sure these names remain sorted!
	static final private String[] intrinsic = {
		"onblur",
		"onchange",
		"onclick",
		"ondblclick",
		"onfocus",
		"onkeydown",
		"onkeypress",
		"onkeyup",
		"onload",
		"onmousedown",
		"onmousemove",
		"onmouseout",
		"onmouseover",
		"onmouseup",
		"onreset",
		"onselect",
		"onsubmit",
		"onunload",
	};


	public HtmlStructureExtractor(HttpURLConnection conn,
				      ConnectionHandler h, Digester dgst)
		throws ParserException {
		html = new HtmlBuffer(conn);
		htmlSrc = conn.getURL();
		hc = h;
		baseUrl = htmlSrc;
		d = dgst;
		structure = null;
	}


	public HtmlStructureExtractor(HtmlBuffer hb, URL u,
				      ConnectionHandler h, Digester dgst) {
		html = hb;
		htmlSrc = u;
		hc = h;
		baseUrl = htmlSrc;
		d = dgst;
		structure = null;
	}


	protected void debugTagToken(TagToken tt,
				     String headline, String prefix) {
		if (headline != null) {
			System.err.println(headline);
		}
		System.err.print(prefix);
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
	}


	protected String tagName(Token t) {
		if (t == null) {
			return "";
		}
		if (!(t instanceof TagToken)) {
			return "";
		}
		TagToken tt = (TagToken)t;
		return tt.getName();
	}


	public void extract() throws ParserException {
		Token t;
		TagToken tt;
		Iterator<Token> it;

		if (Debug.get(Debug.HTML)) {
			System.err.println(dbgPref+"()");
		}

		structure = new LinkedList<StructElem>();
		it = html.iterator();
		while (it.hasNext()) {
			t = it.next();

			if (!(t instanceof TagToken)) {
				// Ignore all TextToken,
				// CDATAToken, CommenToken
				continue;
			}
			tt = (TagToken)t;

			if (Debug.get(Debug.HTMLDetail)) {
				System.err.println(dbgPref
						   +".extract: Found tag <"
						   +tt.getName()+">");
			}

			String tn = tt.getName().toLowerCase();
			if (tn.equals("!doctype")) {
				// handle doctype tag
				StructElem e
					= extractDoctypeElement(tt, it); 
				structure.add(e);
			} else if (tn.equals("base")) {
				// Handle base tag
				StructElem e = extractBaseElement(tt, it);
				structure.add(e);
			} else if (tn.equals("script")) {
				// handle script element
				StructElem e = extractScriptElement(tt, it);
				structure.add(e);
			} else if (tn.equals("style")) {
				// Handle style element
			} else if (tn.equals("iframe")) {
				// Handle iframe element
				StructElem e = extractIFrameElement(tt, it);
				structure.add(e);
			} else if (tn.equals("frame")) {
				// Handle frame element
			} else if (tn.equals("object")) {
				// Handle object element
			} else if (tn.equals("embed")) {
				// Handle embed element
			} else if (tn.equals("applet")) {
				// Handle applet element
			} else {
				// for all other tags handle intrinsic events
				LinkedList<StructElem> le 
					= extractEvents(tt, it);
				structure.addAll(le);
			}
		}


		Iterator<StructElem> ei = structure.iterator();
		while (ei.hasNext()) {
			StructElem e = ei.next();
			if (e != null) {
				System.err.println(e.digestInput(d));
			} else {
				System.err.println("(null)");
			}
		}

	}


	protected boolean isIntrinsic(String name) {
		int res = Arrays.binarySearch(intrinsic, name);
		return (res >= 0);
	}


	protected LinkedList<StructElem>
		extractEvents(TagToken t, Iterator<Token> it) {
		String attr, val;
		LinkedList<StructElem> res = new LinkedList<StructElem>();
		if (Debug.get(Debug.HTML)) {
			System.err.println("extractEvents: "
					   +t.toString());
		}

		for (int i = 0;  i < t.getAttrCount();  i++) {
			attr = t.getAttr(i);
			val = t.getValue(i);
			if (isIntrinsic(attr)) {
				if (Debug.get(Debug.HTMLDetail)) {
					System.err.println("  "+attr+"="+val);
				}
				StructAttr a = new StructAttr(t, attr, val);
				res.add(a);
			}
		}
		return res;
	}


	protected StructElem extractDoctypeElement(TagToken startToken,
						   Iterator<Token> it) {
		if (Debug.get(Debug.HTML)) {
			System.err.println("extractDoctypeElement: "
					   +startToken.toString());
		}
		StructDoctype dt = new StructDoctype(startToken);
		return dt;
	}


	protected StructElem extractBaseElement(TagToken startToken,
					  Iterator<Token> it) {
		if (Debug.get(Debug.HTML)) {
			System.err.println("extractBaseElement: "
					   +startToken.toString());
		}
		
		int hrefIdx = startToken.attrIndex("href");
		String hrefUrlStr = startToken.getValue(hrefIdx);

		if (Debug.get(Debug.HTML)) {
			System.err.println("extractBaseElement: got URL '"
					   +hrefUrlStr+"'");
		}

		URL u = null;
		try {
			u = new URL(hrefUrlStr);
		} catch (MalformedURLException e) {
			// ignore
		}
		if (u != null) {
			baseUrl = u;
		}

		StructBase b = new StructBase(startToken, u);
		return b;
	}


	protected StructElem extractScriptElement(TagToken startToken,
						  Iterator<Token> it) {
		TagToken endToken = null;
		StringBuilder sb = new StringBuilder();
		LinkedList<Token> et = new LinkedList<Token>();
		StructElem res = null;

		endToken = collectFlatElement(startToken, it, sb, et);

		int srcAttrIdx = startToken.attrIndex("src");
		String src = startToken.getValue(srcAttrIdx);
		URL srcUrl = null;
		try {
			srcUrl = new URL(htmlSrc, src);
		} catch (MalformedURLException e) {
			srcUrl = null;
		}
		if (Debug.get(Debug.HTML)) {
			System.err.println("  html url = \""
					   +((htmlSrc!=null)
					     ?htmlSrc.toString():"\""));
			System.err.println("  -> src = \""
					   +src+"\"");
			if (srcUrl != null) {
				System.err.println("  -> src url = \""
						   +srcUrl.toString()
						   +"\"");
			}
		}
		StructScript sc;
		if (srcUrl == null) {
			sc = new StructScript(startToken, endToken,
					      StructScript.ELEMENT,
					      sb.toString());
		} else {
			// retrieve the url and hash pointed to
			// document. If that doesn't work, hash
			// url.
			HttpURLConnection conn = null;
			try {
				conn = hc.prepareConnection(srcUrl, true);
			} catch (IOException e) {
				// ignore
			}
			if (conn != null) {
				conn.disconnect();
			}
			
			if (srcUrl != null) {
				sc = new StructScript(startToken, endToken,
						      StructScript.SOURCED,
						      srcUrl.toString());
			} else {
				sc = new StructScript(startToken, endToken,
						      StructScript.INLINE,
						      src);
			}
		}
		return sc;
	}


	/* Extract and handle an iframe element.
	 *
	 * An iframe's contents is rendered if iframes are not supported,
	 * so recursive iframes do not make sense. Therefor we can
	 * run a non-recursive extractor.
	 */
	protected StructElem extractIFrameElement(TagToken startToken,
						  Iterator<Token> it) {

		TagToken endToken = null;
		StringBuilder sb = new StringBuilder();
		LinkedList<Token> et = new LinkedList<Token>();

		endToken = collectFlatElement(startToken, it, sb, et);

		int srcAttrIdx = startToken.attrIndex("src");
		String src = startToken.getValue(srcAttrIdx);
		URL srcUrl = null;
		try {
			srcUrl = new URL(htmlSrc, src);
		} catch (MalformedURLException e) {
			srcUrl = null;
		}
		return null;
	}


	/* Extract an element that cannot contain itself as a sibling from
	 * the stream of tokens.
	 *
	 * @param startToken    opening tag of the element
	 * @param it            iterator for getting more tokens from the
	 *                      HTML document
	 * @param elemText      output parameter to be filled with the
	 *                      element's contents. The start and end tags
	 *                      are not included here.
	 * @param et            output parameter that collects all tokens
	 *                      belonging to the element, including start
	 *                      and end tags.
	 * @returns             the token ending the element. It is also
	 *                      the last token in @param et.
	 */
	protected TagToken collectFlatElement(TagToken startToken,
					  Iterator<Token> it,
					  StringBuilder elemText,
					  LinkedList<Token> et) {
		Token t;
		TagToken tt;
		TagToken endToken = null;
		String endName = "/"+startToken.getName();

		if (Debug.get(Debug.HTML)) {
			System.err.println("extractFlatElement: "					   +startToken.toString());
		}

		// Collect everything that belongs to this element
		et.add(startToken);
		while (it.hasNext()) {
			t = it.next();
			et.add(t);
			if (Debug.get(Debug.HTMLDetail)) {
				System.err.println(t.toString());
			}
			if (t instanceof TagToken) {
				tt = (TagToken)t;
				if (Debug.get(Debug.HTMLDetail)) {
					System.err.println(dbgPref
						+".extractFlatElement: "
						+"Found tag "
						+tt.getName()+": ");
				}
				if (tt.getName().equalsIgnoreCase(endName)) {
					endToken = tt;
					break;
				}
			}
			// Collect the contents from all tokens before
			// the closing tag </script>.
			// Do it here as we will not get here if we are
			// handling the closing tag.
			elemText.append(t.toString());
		}
		if (Debug.get(Debug.HTML)) {
			System.err.println(dbgPref+".extractFltElement "
					   +"==============");
			System.err.println(startToken.toString()
					   +elemText.toString()
					   +((endToken!=null)
					     ?endToken.toString():""));
		}
		return endToken;
	}






}
