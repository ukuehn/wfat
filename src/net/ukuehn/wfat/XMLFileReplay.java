/* -*- java -*-
 *
 * This is WebForrestAnalysisToolkit, a structural and security analysis tool
 * for http server configurations.
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

package net.ukuehn.wfat;


import java.net.*;
import java.io.*;

import java.util.StringTokenizer;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.MalformedURLException;

import java.util.Map;
import java.util.Iterator;

import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;

import net.ukuehn.util.Debug;


public class XMLFileReplay extends DefaultHandler {

	protected static final String LOCATIONHDR = "Location";
	protected static final String REFRESHHDR = "Refresh";

	int verbLevel;
	Publisher pub;

	URL targetUrl;
	URL responseUrl;
	String responseHostName;
	InetAddress responseIA;
	int responseCode;
	int redirState;
	boolean initial;
	String redirectUrlStr;
	URL redirectUrl;
	String appLayerRedirect;
	boolean evalIP;


	// Be sure not to include headers here that my occur multiple
	// times, like set-cookie!
	private String[] hdrKeyConcise = {
		"Location",
		"Server",
		"X-Powered-By",
		"Refresh",     // for http-equiv "headers" from html
	};


	private final String DELIM = "\"";
	private String defaultProto = "http://";


	public XMLFileReplay(Publisher publisher, int parmVerbLevel)
		throws ToolkitError {
		verbLevel = parmVerbLevel;
		pub = publisher;
		evalIP = false;
	}


	public XMLFileReplay(Publisher publisher, boolean verb)
		throws ToolkitError {
		this(publisher, (verb)?1:0);
	}


	public XMLFileReplay(Publisher publisher)
		throws ToolkitError {
		this(publisher, 0);
	}


	public void setVerbose(boolean verb) {
		verbLevel = (verb)?1:0;
	}


	public void setEvalIP(boolean doEval) {
		evalIP = doEval;
	}


	protected URL getURL(String urlStr)
		throws MalformedURLException {
		URL u;

		try {
			u = new URL(urlStr);
		} catch (MalformedURLException e) {
			try {
				u = new URL(defaultProto+urlStr);
			} catch (MalformedURLException ex) {
				throw new MalformedURLException(
					"Neither "
					+urlStr+" nor "
					+defaultProto+urlStr+" is a valid "
					+"URL.");
			}
		}
		return u;
	}


	protected boolean isRedirectCode(int code) {
		if ( (code == HttpURLConnection.HTTP_SEE_OTHER) ||
		     (code == HttpURLConnection.HTTP_MOVED_PERM) ||
		     (code == HttpURLConnection.HTTP_MOVED_TEMP) ) {
			return true;
		}
		return false;
	}


	protected void resultsConcise(HttpURLConnection conn) 
		throws IOException, ToolkitError {

		String hdrName;
		String hdrVal;

		/**
		 * First, output status line:
		 * Some implementations report a null key for header field #0
		 * and the status line in the value field.
		 * Otherwise, construct a result from the output of
		 * the respective methods...
		 */
		hdrName = conn.getHeaderFieldKey(0);
		hdrVal = conn.getHeaderField(0);
		if (! ((hdrName == null) && (hdrVal == null))) {
			pub.publishElement(hdrName, hdrVal);
		} else {
			StringBuilder sb = new StringBuilder();
			sb.append("HTTP/ ");
			sb.append(conn.getResponseCode());
			sb.append(" ");
			sb.append(conn.getResponseMessage());
			pub.publishElement(null, sb.toString());
		}
		for (int i = 0;  i < hdrKeyConcise.length;  i++) {
			hdrName = hdrKeyConcise[i];
			hdrVal = conn.getHeaderField(hdrName);
			if (hdrVal != null) {
				pub.publishElement(hdrName, hdrVal);
			}
		}
	}


	protected void resultsVerbose(HttpURLConnection conn)
		throws IOException, ToolkitError {
		URL ru = conn.getURL();
		int code = conn.getResponseCode();

		for (int i = 0;  ;  i++) {
			String hdrName = conn.getHeaderFieldKey(i);
			String hdrVal = conn.getHeaderField(i);

			if ((hdrName == null) && (hdrVal == null)) {
				// no more entries
				break;
			}
			pub.publishElement(hdrName, hdrVal);
		}
	}



	protected void handleTargetElemStart(Attributes attr)
		throws SAXException, IOException, ToolkitError {
		String targetUrlStr = attr.getValue(XFS.ATARGETURL);
		if (Debug.get(Debug.Replay)) {
			System.err.println("   "
					   +XFS.ATARGETURL
					   +"="+targetUrlStr);
		}
		try {
			targetUrl = new URL(targetUrlStr);
		} catch (MalformedURLException e) {
			throw new SAXException("Cannot parse URL "
					       +targetUrlStr, e);
		}

		pub.publishStart(targetUrlStr);

		responseUrl = null;
		responseIA = null;
		responseHostName = null;
		responseCode = 0;
		initial = true;
		redirectUrlStr = null;
		redirectUrl = null;
		appLayerRedirect = null;
	}


	protected void handleTargetElemEnd()
		throws SAXException, IOException, ToolkitError {

		pub.publishEnd();
	}


	protected void handleResponseElemStart(Attributes attr)
		throws SAXException, IOException, ToolkitError {
		String respUrlStr = attr.getValue(XFS.ARESPURL);
		String respIpStr = attr.getValue(XFS.ARESPIP);
		String respCodeStr = attr.getValue(XFS.ARESPCODE);
		String redirStateStr = attr.getValue(XFS.AREDIR);

		responseHostName = attr.getValue(XFS.ARESPHOST);
		responseIA = null;
		redirectUrlStr = null;
		redirectUrl = null;
		appLayerRedirect = null;

		if (Debug.get(Debug.Replay)) {
			System.err.println("   "+XFS.ARESPURL+"="+respUrlStr);
			System.err.println("   "+XFS.ARESPHOST
					   +"="+responseHostName);
			System.err.println("   "+XFS.ARESPIP+"="+respIpStr);
			System.err.println("   "+XFS.ARESPCODE
					   +"="+respCodeStr);
		}
		try {
			responseUrl = new URL(respUrlStr);
		} catch (MalformedURLException e) {
			throw new SAXException("Cannot parse URL "
					       +respUrlStr, e);
		}
		responseIA = null;
		try {
			if (evalIP) {
				responseIA = InetAddress.getByName(
						      responseUrl.getHost());
			} else {
				responseIA = InetAddress.getByName(respIpStr);
			}
		} catch (UnknownHostException e) {
			// Ignore, publisher can work with responseIA == null
		}
		try {
			responseCode = Integer.parseInt(respCodeStr);
		} catch (NumberFormatException e) {
			throw new SAXException("Response code must be a "
					       +"number: "+respCodeStr, e);
		}

		if (redirStateStr.equals(XFS.VRNONE)) {
			redirState = Publisher.REDIRNONE;
		} else if (redirStateStr.equals(XFS.VRHTTP)) {
			redirState = Publisher.REDIRHTTP;
		} else if (redirStateStr.equals(XFS.VREQUIV)) {
			redirState = Publisher.REDIREQUIV;
		} else {
			throw new SAXException("Redirect state must be either "
					       +XFS.VRNONE+", "
					       +XFS.VRHTTP+", or "
					       +XFS.VREQUIV+".");
		}
		pub.publishStartResult(targetUrl,
				       responseUrl,
				       responseHostName,
				       responseIA,
				       responseCode,
				       redirState     );
	}


	protected void handleResponseElemEnd()
		throws SAXException, IOException, ToolkitError {

		pub.publishEndResult();

		if (!isRedirectCode(responseCode) && (redirectUrl == null)) {
			if (targetUrl.equals(responseUrl)) {
				pub.publishDestNoRedir(responseUrl,
						       responseHostName,
						       responseIA);
			} else {
				URL reqUrl =
					(initial) ? targetUrl: responseUrl;
				pub.publishDestWithRedir(reqUrl,
							 responseUrl,
							 responseHostName,
							 responseIA,
							 initial);
			}
		} else {
			if (redirectUrl != null) {
				pub.publishRedirect(responseUrl,
						    responseHostName,
						    responseIA,
						    redirectUrl,
						    initial,
						    responseCode);
			} else {
				throw new SAXException(
				     "No redirect location found despite "
				     +"redirect response code, target URL "
				     +targetUrl.toString()
				     +", response for URL "
				     +responseUrl.toString());
			}
		}
		initial = false;
	}


	protected void handleHeaderElemStart(Attributes attr)
		throws SAXException, IOException, ToolkitError {
		String srcStr = attr.getValue(XFS.ASOURCE);
		String keyStr = attr.getValue(XFS.AKEY);
		String valStr = attr.getValue(XFS.AVALUE);
		int src;

		if (XFS.VSRCHTTP.equals(srcStr)) {
			src = Publisher.HTTP;
		} else if (XFS.VSRCEQUIV.equals(srcStr)) {
			src = Publisher.EQUIV;
		} else {
			throw new SAXException("Unknown source attribute '"
					       +srcStr+"'");
		}
		if (XFS.VSRCHTTP.equals(srcStr) &&
		    LOCATIONHDR.equalsIgnoreCase(keyStr)) {
			redirectUrlStr = valStr;
			try {
				// allow relative URL in redirection. This
				// is against the HTTP 1.1 spec, but
				// some sites do it anyway.
				redirectUrl = new URL(responseUrl,
						      redirectUrlStr);
				//redirectUrl = new URL(redirectUrlStr);
			} catch (MalformedURLException e) {
				throw new SAXException("No URL: "
						       +redirectUrlStr, e);
			}
		} else if (XFS.VSRCEQUIV.equals(srcStr) &&
			   REFRESHHDR.equalsIgnoreCase(keyStr)) {
			if (valStr == null) {
				valStr = "";
			}
			String s = valStr.toLowerCase();
			int i = s.indexOf("url=");
			if (i < 0) {
				throw new SAXException("Invalid format of "
						       +"http-equiv refresh "
						       +"content: '"
						       +valStr+"'.");
			}
			String appLayerRedirect = valStr.substring(i+4);
			try {
				// Allow relative URL in http equiv
				// redirection. The HTTP 1.1 and HTML 4.01
				// specs do not specify if it must be
				// absolute or may be relative. So mimic
				// typical browsers' behavior.
				redirectUrl = new URL(responseUrl,
						      appLayerRedirect);
			} catch (MalformedURLException e) {
				throw new SAXException("No URL: "
						       +appLayerRedirect, e);
			}			
		}
		if (Debug.get(Debug.Replay)) {
			System.err.println("   "+XFS.AKEY+"="+keyStr);
			System.err.println("   "+XFS.AVALUE
					   +"="+valStr);
		}

		if (verbLevel > 0) {
			pub.publishElement(keyStr, valStr, src);
		} else {
			// filter the keys...
			if ( ((keyStr == null) ||
			      ((keyStr != null) && keyStr.equals("")))
			     && (valStr != null) && !valStr.equals("") ) {
				pub.publishElement(keyStr, valStr, src);
				return;
			}
			for (int i = 0;  i < hdrKeyConcise.length;  i++) {
				if (hdrKeyConcise[i].
				    equalsIgnoreCase(keyStr)) {
					pub.publishElement(keyStr,
							   valStr, src);
					break;
				}
			}
		}
	}


	protected void handleHeaderElemEnd()
		throws SAXException, IOException, ToolkitError {
		// do nothing
	}


	protected void handleExceptionElemStart(Attributes attr)
		throws SAXException, IOException, ToolkitError {
		String msg = attr.getValue(XFS.AMSG);

		if (Debug.get(Debug.Replay)) {
			System.err.println("   "+XFS.AMSG+"="+msg);
		}
		pub.publishException(msg);
	}


	protected void handleExceptionElemEnd()
		throws SAXException, IOException, ToolkitError {
		// do nothing
	}


	public void startDocument() throws SAXException {
		// so far nothing
	}

	public void endDocument() throws SAXException {
		// nothing ?
	}

	public void startElement(String nsURI,
				 String locName,
				 String qualName,
				 Attributes attr)
		throws SAXException {

		if (Debug.get(Debug.Replay)) {
			System.err.println("startElement("
					   +nsURI+", "+locName+", "
					   +qualName
					   +")");
		}
		try {
			if (XFS.ECOLLECTION.equalsIgnoreCase(qualName)) {
				// nothing to do here
			} else if (XFS.ETARGET.equalsIgnoreCase(qualName)) {
				handleTargetElemStart(attr);
			} else if (XFS.ERESPONSE.equalsIgnoreCase(qualName)) {
				handleResponseElemStart(attr);
			} else if (XFS.EHEADER.equalsIgnoreCase(qualName)) {
				handleHeaderElemStart(attr);
			} else if (XFS.EEXCEPTION.equalsIgnoreCase(qualName)) {
				handleExceptionElemStart(attr);
			} else {
				throw new SAXException("Unsupported tag '"
						       +qualName
						       +"' found.");
			}
		} catch (Exception e) {
			throw new SAXException(e);
		}
	}


	public void endElement(String nsURI,
			       String locName,
			       String qualName)
		throws SAXException {
		
		if (Debug.get(Debug.Replay)) {
			System.err.println("endElement("
					   +nsURI+", "+locName+", "
					   +qualName+")");
		}
		try {
			if (qualName.equals(XFS.ECOLLECTION)) {
				// nothing to do here
			} else if (qualName.equals(XFS.ETARGET)) {
				handleTargetElemEnd();
			} else if (qualName.equals(XFS.ERESPONSE)) {
				handleResponseElemEnd();
			} else if (qualName.equals(XFS.EHEADER)) {
				handleHeaderElemEnd();
			} else if (qualName.equals(XFS.EEXCEPTION)) {
				handleExceptionElemEnd();
			}
		} catch (Exception e) {
			throw new SAXException(e);
		}
	}


	public void characters(char buf[], int offset, int len)
		throws SAXException {
		// nothing to do
	}


	

}
