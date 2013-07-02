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
import java.nio.charset.Charset;

import java.net.URLConnection;
import java.net.HttpURLConnection;
import java.util.StringTokenizer;
import java.util.LinkedList;
import java.lang.StringBuilder;

import net.ukuehn.util.Debug;




public class HtmlTokenizer {

	public static final String DEFAULT_CONTENT_TYPE = "text/html";
	public static final String DEFAULT_CHARSET_NAME= "ISO-8859-1";
	public static final int EOF = -1;


	protected InputStream is;
	protected PushbackReader in;
	protected boolean eof;

	/* This number is a position within the scanned document.
	 * As it is used in exceptions, make it human-readable
	 * by starting at 1.
	 */
	protected int lineNo;


	public HtmlTokenizer() {
		is = null;
		in = new PushbackReader(new StringReader(""), 8);
		eof = false;
		lineNo = 1;
	}


	public HtmlTokenizer(HttpURLConnection conn)
		throws ParserException {

		eof = false;
		lineNo = 1;
		String ctypeStr = conn.getContentType();
		String contentEnc = conn.getContentEncoding();

		String contentType = getContentType(ctypeStr);
		String charsetName = getCharsetName(ctypeStr);

		if (Debug.get(Debug.HToken)) {
			System.err.println("content-type: "+contentType);
			System.err.println("charset: "+charsetName);
		}

		if (!contentType.equalsIgnoreCase("text/html")) {
			throw new ParserException("Wrong content type "
						  +contentType
						  +", text/html expected.");
		}

		try {
			is = conn.getInputStream();
			InputStreamReader isr
				= new InputStreamReader(is, charsetName);
			BufferedReader br = new BufferedReader(isr);
			in = new PushbackReader(br, 8);
		} catch (IOException e) {
			throw new ParserException("Cannot setup input.");
		}
	}


	public HtmlTokenizer(String htmlParm) {
		eof = false;
		lineNo = 1;
		is = null;
		StringReader sr = new StringReader(htmlParm);
		in = new PushbackReader(sr, 8);
	}


	public void close() throws IOException {
		in.close();
	}


	/*
	 *  Analyse content type to find out the Charset to use
	 */
	protected String getCharsetName(String contentTypeStr) {

		String charsetName = DEFAULT_CHARSET_NAME;
		if (contentTypeStr == null) {
			charsetName = DEFAULT_CHARSET_NAME;
		} else {
			String ctypeStr = contentTypeStr.toLowerCase();
			StringTokenizer st
				= new StringTokenizer(ctypeStr, " \t\n\r\f;");
			if (st.hasMoreTokens()) {
				st.nextToken(); // ignore result
			}
			if (st.hasMoreTokens()) {
				String token = st.nextToken();
				if (token.startsWith("charset=")) {
					charsetName
					    = token.substring(8,
							      token.length());
				}
			}
		}

		if (!Charset.isSupported(charsetName)) {
			charsetName = DEFAULT_CHARSET_NAME;
		}
		return charsetName;
	}


	/*
	 * Obtain the content type from the content type string of
	 * the http header. Strip any character set information.
	 * If nothing is found assume the default content type
	 * text/html.
	 *
	 * @param contentTypeStr The value of the http header content-type
	 * @return content type or default
	 */
	protected String getContentType(String contentTypeStr) {

		String contentType = null;

		if (contentTypeStr == null) {
			contentTypeStr = DEFAULT_CONTENT_TYPE;
		}
		String ctypeStr = contentTypeStr.toLowerCase();
		StringTokenizer st
			= new StringTokenizer(ctypeStr, " \t\n\r\f;");
		if (st.hasMoreTokens()) {
			contentType = st.nextToken();
		}
		if (contentType == null) {
			contentType = DEFAULT_CONTENT_TYPE;
		}
		return contentType;
	}


	public Token nextTokenNonHTML() throws ParserException {
		char ch;
		StringBuilder sb = new StringBuilder();
		Token resToken;

		if (Debug.get(Debug.HToken)) {
			System.err.println("nextTokenNonHTML()");
		}
		if (eof) {
			return null;
		}
		return scanPCDATA(sb);
	}
	

	public Token nextToken() throws ParserException {
		char ch;
		StringBuilder sb = new StringBuilder();
		Token resToken;

		if (Debug.get(Debug.HToken)) {
			System.err.println("nextToken()");
		}
		if (eof) {
			return null;
		}
		//for (ch = nextChar(); ch != (char)EOF; ch = nextChar() ) {
		ch = nextChar();
		switch (ch) {
		case (char)EOF:
			resToken = null;
			break;
		case '<':
			sb.append(ch);
			ch = nextChar();
			if (ch == (char)EOF) {
				// parse as a string, as no fully specified
				// tag is here
				//
				resToken = null;
			} else if (Character.isLetter(ch) || (ch == '/')) {
				// Opening or closing tag
				// closed by > outside of quoted value
				if (Debug.get(Debug.HTokenDetail)) {
					System.err.println("  isletter or /");
				}
				pushback(ch);
				resToken = scanTag(sb);
			} else if (ch == '!') {
				// Markup declaration, including comment
				// closed by >,
				if (Debug.get(Debug.HTokenDetail)) {
					System.err.println("  markup");
				}
				pushback(ch);
				resToken = scanMarkup(sb);
			} else {
				throw new ParserException(
					   "Cannot parse <"+
					   String.valueOf(ch));
			}
			break;
		default:
			if (Debug.get(Debug.HTokenDetail)) {
				System.err.println("  text");
			}
			pushback(ch);  // let scanText read the full thing
			resToken = scanText(sb);
			break;
		}
		return resToken;
	}


	protected char nextChar() throws ParserException {
		int i;
		char res = (char)EOF;

		try {
			i = in.read();
			if (i == EOF) {
				res = (char)EOF;
			} else {
				res = (char)i;
			}
			// inspired by htmlparser: normalise line endings:
			// replace "\r\n" and "\r" by "\n"
			if (res == '\r') {
				res = '\n'; // normalise
				i = in.read();
				if (i != EOF) {
					char nc = (char)i;
					if (nc != '\n') {
						in.unread((int)nc);
					}
				}
			}
			if (res == '\n') {
				lineNo += 1;
			}
		} catch (IOException e) {
			eof = true;
			throw new ParserException("Cannot read character");
		}
		if (Debug.get(Debug.HTokenDetail)) {
			if (res != (char)EOF) {
				System.err.println("Read: '"
						   +String.valueOf(res)
						   +"' ("
						   +String.valueOf((int)res)
						   +")");
			} else {
				System.err.println("Read: EOF");
			}
		}
		return res;
	}


	protected void pushback(char c) throws ParserException {
		if (Debug.get(Debug.HTokenDetail)) {
			System.err.println("Pushback '"
					   +String.valueOf(c)
					   +"' ("
					   +String.valueOf((int)c)
					   +")");
		}
		try {
			in.unread((int)c);
			if (c == '\n') {
				if (lineNo > 1) {
					lineNo -= 1;
				} else {
					lineNo = 1;
				}
			}
		} catch (IOException e) {
			eof = true;
			throw new ParserException("Cannot push back "
						  +"character");
		}
	}


	protected Token scanMarkup(StringBuilder sb)
		throws ParserException {

		char ch, ch1;
		Token resToken = null;

		ch = nextChar();
		if (ch != '!') {
			throw new ParserException("scanMarkup() called where "
						  +"no markup starts, "
						  +"line "
						  +String.valueOf(lineNo)
						  +".");
		}
		ch1 = nextChar();
		if (ch1 == '>') {
			// found empty comment "<!>"
			sb.append(ch);
			sb.append(ch1);
			if (Debug.get(Debug.HTokenDetail)) {
				System.err.println(
					"  constructing CommentToken");
			}
			resToken = new CommentToken(sb.toString());
		} else if (ch1 == '-') {
			pushback(ch1);
			sb.append(ch);
			resToken = scanComment(sb);
		} else if (ch1 == '[') {
			pushback(ch1);
			sb.append(ch);
			resToken = scanCDATA(sb);
		} else {
			pushback(ch1);
			pushback(ch);
			resToken = scanTag(sb);
		}
		return resToken;
	}


	/* Scan CDATA markup. Assumption is that sb contains already the
	 * starting markup indicator "<!".
	 * Scan for "[CDATA[" to start, then scan for "]]>" to close the
	 * tag.
	 * Implemented as a state machine as follows. Indicated is the
	 * state and the character expected:
	 *       0123456777789x    with x = 10
	 *     <![CDATA[???]]>
	 * where state 11 is the final accepting state. Does not read past
	 * the last character that leads to accepting state. Thus, next call
	 * to nextToken() can again start with getting the next character.
	 *
	 * @param sb  StringBuilder containing the leading "<!" of the
	 *            CDATA tag.
	 * @return    CDATA token.
	 */
	protected Token scanCDATA(StringBuilder sb)
		throws ParserException {
		int state;
		char ch;

		state = 0;
		while (state != 10) {
			ch = nextChar();
			if (ch == (char)EOF) {
				throw new ParserException(
						  "Unfinished CDATA open, "
						  +"line "
						  +String.valueOf(lineNo)
						  +".");
			}
			sb.append(ch);
			switch (state) {
			case 0:
			case 6:
				if (ch != '[') {
					throw new ParserException(
					    "scanCDATA(): no CDATA open "
					    +"starting with "+sb.toString()
					    +", line "
					    +String.valueOf(lineNo)
					    +".");
				}
				if (state == 0) {
					state = 1;
				} else {
					state = 7;
				}
				break;
			case 1:
				if (ch != 'C') {
					throw new ParserException(
					    "scanCDATA(): no CDATA open "
					    +"starting with "+sb.toString()
					    +", line "
					    +String.valueOf(lineNo)
					    +".");
				}
				state = 2;
				break;
			case 2:
				if (ch != 'D') {
					throw new ParserException(
					    "scanCDATA(): no CDATA open "
					    +"starting with "+sb.toString()
					    +", line "
					    +String.valueOf(lineNo)
					    +".");
				}
				state = 3;
				break;
			case 3:
			case 5:
				if (ch != 'A') {
					throw new ParserException(
					    "scanCDATA(): no CDATA open "
					    +"starting with "+sb.toString()
					    +", line "
					    +String.valueOf(lineNo)
					    +".");
				}
				if (state == 3) {
					state = 4;
				} else {
					state = 6;
				}
				break;
			case 4:
				if (ch != 'T') {
					throw new ParserException(
					    "scanCDATA(): no CDATA open "
					    +"starting with "+sb.toString()
					    +", line "
					    +String.valueOf(lineNo)
					    +".");
				}
				state = 5;
				break;
			case 7:
				if (ch == ']') {
					state = 8;
				} else {
					// any other character is ok
					// state = 7;
				}
				break;
			case 8: // Have seen one ']', if no second,
				// go back to state 7
				if (ch == ']') {
					state = 9;
				} else {
					state = 7;
				}
				break;
			case 9: // Have seen "]]" of CDATA closing. If
				// followed by either ']' of '>', go
				// back to state 7
				if (ch == '>') {
					state = 10;
				} else if (ch == ']') {
					// with previous ] there are two, so
					// try again with next char.
					state = 9;
				} else {
					state = 7;
				}
				break;
			default:
				throw new ParserException(
					    "Internal error in scanCDATA, "
					    +"line "
					    +String.valueOf(lineNo)+".");
				// break;
			}
		}
		CDATAToken resToken = new CDATAToken(sb.toString());
		return resToken;
	}


	/* Scan a tag. Assumption is that sb contains alread the starting "<".
	 * Names and attributes are collected into strings, where, e.g
	 * <tag attr="val"> is represented by tag, attr, =, "val" (quotes
	 * are preserved. Essentially, the tag's contents is tokenised
	 * while obeying quotes.
	 * Does not read past last character of the tag. Thus, next call
	 * to nextToken() can again start with getting the next character.
	 *
	 * @param sb  StringBuilder containing the leading "<" of the tag
	 * @return    Tag token.
	 */
	protected Token scanTag(StringBuilder sb) throws ParserException {

		char ch;
		StringBuilder tn = new StringBuilder(10);
		LinkedList<String> attr = new LinkedList<String>();

		ch = nextChar();
		for ( /* nothing */;
		     !Character.isWhitespace(ch) && (ch != '>');
		     ch = nextChar()) {
			if (ch == (char)EOF) {
				throw new ParserException(
					   "Unfinished tag in line "
					   +String.valueOf(lineNo)+".");
			}
			if (Debug.get(Debug.HTokenDetail)) {
				System.err.println("  is in tag name");
			}
			sb.append(ch);
			tn.append(ch);
		}
		if (Debug.get(Debug.HToken)) {
			System.err.println("Found tag name '"
					   +tn.toString()+"', line "
					   +String.valueOf(lineNo));
		}
		attr.add(tn.toString());

		while (ch != '>') {
			tn = new StringBuilder(10);

			// Handle preceeding whitespace
			for ( /* nothing */;
			      Character.isWhitespace(ch);
			      ch = nextChar()) {
				if (Debug.get(Debug.HTokenDetail)) {
					System.err.println("  is whitespace");
				}
				sb.append(ch);
			}
			if (ch == (char)EOF) {
				throw new ParserException(
					   "Unfinished tag in line "
					   +String.valueOf(lineNo)+".");
			}
			if (ch == '"') {
				// double quoted value
				sb.append(ch);
				tn.append(ch);
				ch = nextChar();
				for ( ; (ch != (char)EOF) && (ch != '"');
				      ch = nextChar()) {
					if (Debug.get(Debug.HTokenDetail)) {
						System.err.println(
						      "  is in double quote");
					}
					sb.append(ch);
					tn.append(ch);
				}
				if (ch == (char)EOF) {
					throw new ParserException(
						    "Unbalanced double "
						    +"quote in tag, line "
						    +String.valueOf(lineNo)
						    +".");
				}
				// include the quote
				sb.append(ch);
				tn.append(ch);
				attr.add(tn.toString());
				ch = nextChar();
			} else if (ch == '\'') {
				// single quoted value
				sb.append(ch);
				tn.append(ch);
				ch = nextChar();
				for ( ; (ch != (char)EOF) && (ch != '\'');
				      ch = nextChar()) {
					if (Debug.get(Debug.HTokenDetail)) {
						System.err.println(
						      "  is in single quote");
					}
					sb.append(ch);
					tn.append(ch);
				}
				if (ch == (char)EOF) {
					throw new ParserException(
						    "Unbalanced single "
						    +"quote in tag, line "
						    +String.valueOf(lineNo)
						    +".");
				}
				// include the quote
				sb.append(ch);
				tn.append(ch);
				attr.add(tn.toString());
				ch = nextChar();
			} else if (ch == '=') {
				// equal
				if (Debug.get(Debug.HTokenDetail)) {
					System.err.println(
					   "  is =");
				}
				sb.append(ch);
				tn.append(ch);
				attr.add(tn.toString());
				ch = nextChar();
			} else if (ch == '>') {
				// end of tag reached
				if (Debug.get(Debug.HTokenDetail)) {
					System.err.println(
					   "  is > -> done");
				}
				sb.append(ch);
				break;
			} else if (ch == '/') { // potential xml-style tag end
				char ch1 = nextChar();
				pushback(ch1);
				if (ch1 != '>') {
					throw new ParserException(
						"Incomplete tag closing "
						+"/"+String.valueOf(ch1)
						+", line"
						+String.valueOf(lineNo)
						+".");
				}
				if (Debug.get(Debug.HTokenDetail)) {
					System.err.println(
						"  is /> -> done");
				}
				sb.append(ch);
				ch = nextChar();
				//sb.append(ch1);
				//break;
			} else {
				// unquoted stuff
				for ( ; (ch != (char)EOF) &&
					      !Character.isWhitespace(ch) &&
					      (ch != '>') &&
					      (ch != '=') &&
					      (ch != '"') &&
					      (ch != '\'');
				      ch = nextChar()) {
					if (Debug.get(Debug.HTokenDetail)) {
						System.err.println(
						   "  is in unquoted");
					}
					if (ch == '/') {
						char ch1 = nextChar();
						pushback(ch1);
						if (ch1 == '>') {
							// xml-style ending
							break;
						} else {
							sb.append(ch);
							tn.append(ch);
						}
						continue;
					}
					if ( ((ch >= 'A') && (ch <= 'Z')) ||
					     ((ch >= 'a') && (ch <= 'z')) ||
					     ((ch >= '0') && (ch <= '9')) ||
					     (ch == '-') || (ch == '_') ||
					     (ch == '.') || (ch == ':') ||
					     (ch == '(') || (ch == ')') ||
					     (ch == '{') || (ch == '}') ||
					     (ch == ';') || (ch == ',') ||
					     (ch == '#') || (ch == '&')) {
						sb.append(ch);
						tn.append(ch);
					} else {
						throw new ParserException(
						    "Forbidden character '"
						    +ch+"'in "
						    +"unquoted part of "
						    +"tag, line "
						    +String.valueOf(lineNo)
						    +".");
					}
				}
				if (ch == (char)EOF) {
					throw new ParserException(
						    "Unbalanced single "
						    +"quote in tag, line "
						    +String.valueOf(lineNo)
						    +".");
				}
				// do not include the break character here
				// and do not get a next character
				attr.add(tn.toString());
			}
		}
		// record break character
		sb.append(ch);
		TagToken resToken
			= new TagToken(sb.toString(),
				       attr.toArray(new String[1]));

		return resToken;
	}


	/* Scan a comment. Assumption is that sb contains already the
	 * starting markup indicator "<!".
	 * Scan for "--" to start the comment, then scan for "--" to close
	 * the comment, and finally scan for the trailing ">".
	 * Implemented as a state machine as follows. Indicated is the
	 * state and the character expected:
	 *      0122223445
	 *    <!--???-- >
	 * where state 5 is the final accepting state. Does not read past
	 * last character that leads to accepting state. Thus, next call
	 * to nextToken() can again start with getting the next character.
	 *
	 * @param sb  StringBuilder containing the leading "<!" of the
	 *            comment
	 * @return    Commen token.
	 */
	protected Token scanComment(StringBuilder sb)
		throws ParserException {
		int state;
		char ch;

		state = 0;
		while (state != 5) {
			ch = nextChar();
			if (ch == (char)EOF) {
				throw new ParserException(
						  "Unfinished comment, "
						  +"line "
						  +String.valueOf(lineNo)
						  +".");
			}
			sb.append(ch);
			switch (state) {
			case 0:
			case 1:
				if (ch != '-') {
					throw new ParserException(
					     "scanComment(): not a comment "
					     +"starting with "+sb.toString()
					     +", line "
					     +String.valueOf(lineNo)
					     +".");
				}
				if (state == 0) {
					state = 1;
				} else {
					state = 2;
				}
				break;
			case 2:
				if (ch == '-') {
					state = 3;
				} else {
					// any other character is ok
					// state = 2;
				}
				break;
			case 3: // Have seen one '-', if no second, go back
				// to state 2
				if (ch == '-') {
					state = 4;
				} else {
					state = 2;
				}
				break;
			case 4: // have seen the closing "--", so
				// run until ">" is found.
				if (Character.isWhitespace(ch)) {
					state = 4;
				} else if (ch == '>') {
					// we are done, enter accepting state
					state = 5;
				} else {
					throw new ParserException(
						"Unexcpected character "
						+String.valueOf(ch)
						+" between closed comment "
						+" and closing '>', line "
						+String.valueOf(lineNo)
						+".");
				}
				break;
			default:
				throw new ParserException(
					    "Internal error in scanComment, "
					    +"line "
					    +String.valueOf(lineNo)+".");
				// break;
			}
		}
		CommentToken resToken = new CommentToken(sb.toString());
		return resToken;
	}


	/* Scan the input stream until input is exhausted, or a string
	 * delimiter is encountered, which is "&lt;/" (start of end tag),
	 * "&lt;!" (start of markup tag), "&lt;" followed by a letter
	 * (start of start tag).
	 *
	 * @param sb     String builder containing characters leading to
	 *               the text (the first character of the text).
	 * @return       Text token representing the text.
	 */
	protected Token scanText(StringBuilder sb)
		throws ParserException {
		boolean done;
		char ch;

		done = false;
		do {
			ch = nextChar();
			if (ch == (char)EOF) {
				done = true;
			} else if (ch == '<') {
				char ch1 = nextChar();
				if ( (ch1 == '/') ||
				     (ch1 == '!') ||
				     Character.isLetter(ch1) ) {
					// put stuff back into stream
					pushback(ch1);
					pushback(ch);
					done = true;
				} else {
					// No full end delimiter, so push
					// back the next character and
					// add the read character to the
					// buffer.
					pushback(ch1);
					sb.append(ch);
				}
			} else {
				sb.append(ch);
			}
		} while (!done);
		TextToken tt = new TextToken(sb.toString());
		return tt;
	}



	/* Scan PCDATA until finding "</" followed by tag start character.
	 * This is used to parse through script and style data without
	 * interpreting the data as HTML.
	 * See also appendix B.3.2 "Specifying non-HTML data" of the HTML
	 * 4.01 specification.
	 * http://www.w3.org/TR/html401/appendix/
	 *                   notes.html#notes-specifying-data
	 *
	 * Implemented as a state machine as follows. Indicated is the
	 * state and the character expected:
	 *     000123
	 *     ??</z
	 * where state 3 is the final accepting state and "z" stands for
	 * and name start character (a-zA-Z). Does push back the
	 * "</z" end marker, so the next call to nextToken() can again
	 * start with getting the </z.
	 *
	 * The implementation uses a StringBuilder and appends chars while
	 * scanning, and removes the trailing </ later.
	 *
	 * @param sb  StringBuilder for collecting the characters up to </
	 * @return    Text token.
	 */
	protected Token scanPCDATA(StringBuilder sb)
		throws ParserException {
		int state;
		char ch;

		state = 0;
		while (state != 3) {
			ch = nextChar();
			if (ch == (char)EOF) {
				throw new ParserException(
					  "Unfinished PCDATA, line"
					  +String.valueOf(lineNo)+".");
			}
			sb.append(ch);
			switch(state) {
			case 0:
				if (ch == '<') {
					state = 1;
				} else {
					state = 0;
				}
				break;
			case 1:
				if (ch == '/') {
					state = 2;
				} else {
					state = 0;
				}
				break;
			case 2:
				if (((ch >= 'a') && (ch <= 'z')) ||
				    ((ch >= 'A') && (ch <= 'Z'))) {
					state = 3;
				} else {
					state = 0;
				}
				break;
			default:
				throw new ParserException(
					      "Internal error in scanPCDATA,"
					      +" line "
					      +String.valueOf(lineNo)+".");
				//break;
			}
		}

		// Now push back the last 3 characters "</z" in reverse order
		int len = sb.length();
		if (len < 3) {
			throw new ParserException("Read 3-char end of PCDATA, "
						  +"but have less data, line "
						  +String.valueOf(lineNo)
						  +".");
		}
		for (int i = 0;  i < 3;  i++) {
			pushback(sb.charAt(len-1-i));
		}
		return new TextToken(sb.substring(0, len-3));
	}


	/* Test cases for the lexer class
	 *
	 */
	static protected String tc0 =
		"<!>nothing to see here<!-- blablupp --  >text\n"
		+" with newline<!--hmm-->";
	static protected String tc1 =
		"<!DOCTYPE \n";
	static protected String tc2 =
		"<!DOCTYPE html PUBLIC "+
		"\"-//W3C//DTD XHTML 1.0 Transitional//EN\"\n"
		+"\"http://www.w3.org/TR/xhtml1/DTD/xhtml1"
		+"-transitional.dtd\">\n"
		+"<body bg =\"white\"><b><!>nothing to see here"
		+"</b><!-- blablupp --  >text\n"
		+" with newline<!--hmm--></body>";
	static protected String tc3 = 
		"\"-//W3C//DTD XHTML 1.0 Transitional//EN\"\n"
		+"\"http://www.w3.org/TR/xhtml1/DTD/xhtml1"
		+"-transitional.dtd\">\n"
		+"<body bg =\"white\" id = dummy ><b><!>nothing to see here\n"
		+"newline<!--hmm--></body>";

	static protected String tc4 = 
		"<!DOCTYPE html PUBLIC "+
		"\"-//W3C//DTD XHTML 1.0 Transitional//EN\"\n"
		+"\"http://www.w3.org/TR/xhtml1/DTD/xhtml1"
		+"-transitional.dtd\">\n"
		+"<body bg =\"white\"><b><!>nothing to see here</b>"
		+"<!-- comment --   >\n"
		+"<!-- another -->\n"
		+"<script type=\"text/javascript\">\n"
		+"  //<![CDATA[\n"
		+"  window.orig_onload = window.onload;\n"
		+"  window.onload = function() {\n"
		+"  var cpost=document.location.hash;\n"
		+"  if(typeof window.orig_onload == \"function\")\n"
		+"    window.orig_onload();\n"
		+"  }\n"
		+"  //]]>\n"
		+"</script>\n"
		+"</body>";
		
	public static void main(String[] args)
		throws ParserException, IOException {

		Token tok;

		Debug.set(1 << Debug.HToken);
		HtmlTokenizer ht = new HtmlTokenizer(tc4);
		for (tok = ht.nextToken();
		     tok != null;
		     tok = ht.nextToken() ) {
			if (tok instanceof CommentToken) {
				CommentToken ct = (CommentToken)tok;
				System.err.println("Comment: "
						   +ct.getComment());
				System.err.println(tok.toString());
			} else if (tok instanceof CDATAToken) {
				CDATAToken ct = (CDATAToken)tok;
				System.err.println("CDATA  : "
						   +ct.getComment());
				System.err.println(ct.toString());
			} else if (tok instanceof TextToken) {
				TextToken tt = (TextToken)tok;
				System.err.println("Text   : "
						   +tt.getText());
				System.err.println(tok.toString());
			} else if (tok instanceof TagToken) {
				TagToken tt = (TagToken)tok;
				System.err.print("Tag      : ");
				for (int i = 0;
				     i < tt.getAttrCount();  i++) {
					System.err.print(tt.getAttr(i)
							 + "="
							 +tt.getValue(i)
							 +" ");
				}
				System.err.println();
				System.err.println(tok.toString());
			}
			
		}
	}

	
}
