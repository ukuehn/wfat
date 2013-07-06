/* -*- java -*-
 *
 * This is WebForrestAnalysisToolkit, a structural and security analysis tool
 * for http server configurations.
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

package net.ukuehn.wfat;


import java.net.*;
import java.io.*;

import java.util.StringTokenizer;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.MalformedURLException;

import java.util.HashSet;
import java.util.Iterator;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.X509TrustManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import java.security.NoSuchAlgorithmException;
import java.security.KeyManagementException;
import java.security.SecureRandom;


import net.ukuehn.xml.SimpleXMLWriter;
import net.ukuehn.security.NullTrustManager;
import net.ukuehn.security.NullHostnameVerifier;
import net.ukuehn.html.HtmlBuffer;
import net.ukuehn.html.HtmlStructureExtractor;
import net.ukuehn.html.HttpEquivExtractor;
import net.ukuehn.html.HttpEquivResult;
import net.ukuehn.html.ParserException;
import net.ukuehn.util.Debug;



public class HTTPFingerprint {


	boolean noRedirect;
	boolean followAppRedirect;
	boolean verbose;
	boolean doStructHash;
	ConnectionHandler hc;
	Publisher pub;
	int delay;
	boolean initialRequest;


	// Be sure not to include headers here that my occur multiple
	// times, like set-cookie!
	private String[] hdrKeyConcise = {
		"Server",
		"Location",
		"X-Powered-By",
		"Refresh",     // for http-equiv "headers" from html
	};


	private final String DELIM = "\"";


	public HTTPFingerprint(Publisher publisher, ConnectionHandler h)
		throws ToolkitError {
		pub = publisher;
		hc = h;
		noRedirect = false;
		followAppRedirect = false;
		verbose = false;
		doStructHash = false;
		delay = 0;
		initialRequest = true;
	}


	public void setNoRedirect(boolean parm) {
		noRedirect = parm;
	}


	public void setFollowAppRedirect(boolean parm) {
		followAppRedirect = parm;
	}


	public void setVerbose(boolean beVerbose) {
		verbose = beVerbose;
	}


	public void setStructHash(boolean doit) {
		doStructHash = doit;
	}


	public void setDelay(int parmDelay) {
		delay = parmDelay;
	}


	protected void handleHTML(HtmlBuffer html, URL u)
		throws ParserException, ToolkitError {
		if (Debug.get(Debug.HTML)) {
			System.err.println("handleHTML()");
		}
		Digester d = new Digester();
		HtmlStructureExtractor hse = new 
			HtmlStructureExtractor(html, u, hc, d);
		hse.extract();
	}


	protected boolean isRedirectCode(int code) {
		if ( (code == HttpURLConnection.HTTP_SEE_OTHER) ||
		     (code == HttpURLConnection.HTTP_MOVED_PERM) ||
		     (code == HttpURLConnection.HTTP_MOVED_TEMP) ) {
			return true;
		}
		return false;
	}


	public void fingerprint(String urlStr)
		throws IOException, ToolkitError {
		HttpURLConnection conn;
		URL u, currUrl, targetUrl;
		int maxRedirects;
		int resp;
		int redirState;
		boolean initial;
		boolean obtainContent;
		HttpEquivResult er;
		HtmlBuffer htmlBuf;
		String alr;  // Application layer redirect from http-equiv
		String errorMsg;

		try {
			targetUrl = hc.getURL(urlStr);
			if (targetUrl == null) {
				throw new MalformedURLException("Not an URL: "
								+urlStr);
			}
		} catch (Exception e) {
			// Output just the urlstr and nothing more,
			// as we do not have a result...
			pub.publishStart(urlStr);
			pub.publishEnd();
			return;
		}

		pub.publishStart(targetUrl.toString());

		obtainContent = (verbose ||
				 followAppRedirect ||
				 doStructHash);

		u = targetUrl;
		initial = true;
		maxRedirects = (noRedirect) ? 1 : 31;
		for (int i = 0;  i < maxRedirects;  i++) {
			// if delay is set, wait for some time before doing
			// the request, however, not for the first of all.
			if (!initialRequest && (delay > 0)) {
				try {
					Thread.sleep(delay);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
			}
			initialRequest = false;
			resp = 0;

			if (hc.isForbidden(u.getHost())) {
				// Forbidden host reached, do not
				// continue.
				errorMsg = "Forbidden host"+u.getHost();
				pub.publishException(errorMsg);
				break;
			}
			try {
				conn = hc.prepareConnection(u, false);
				if (conn != null) {
					resp = conn.getResponseCode();
				} else {
					break;
				}
			} catch (IOException e) {
				errorMsg = e.getMessage();
				pub.publishException(errorMsg);
				break;
			}

			htmlBuf = null;
			er = null;
			alr = null;
			if (!isRedirectCode(resp) && obtainContent) {
				// Try to read document 
				try {
					htmlBuf = new HtmlBuffer(conn);
				} catch (ParserException e) {
					// Cannot read html doc, then
					// we do not have one. That's fine...
					if (Debug.get(
						Debug.AppLayerRedirect) ||
					    Debug.get(Debug.HTML)) {
						System.err.println("Cannot "
						      +"read html document: "
							 +e.toString());
					}
				}

				// Determine if we have an application-level
				// redirect. Here only html-redirects
				// using <meta http-equiv="refresh"...>
				// are handled.
				if (htmlBuf != null) {
					er = getAppLayerEquiv(htmlBuf);
				}
				if (er != null) {
					alr = getAppLayerRedirect(er);
				}
			}

			if (isRedirectCode(resp)) {
				redirState = Publisher.REDIRHTTP;
			} else if (alr != null) {
				redirState = Publisher.REDIREQUIV;
			} else {
				redirState = Publisher.REDIRNONE;
			}

			// Output protocol stuff
			currUrl = conn.getURL();
			String hostName = currUrl.getHost();
			InetAddress hostIP = null;
			try {
				hostIP = InetAddress.getByName(hostName);
			} catch (UnknownHostException e) {
				// if IP address cannot be determined,
				// continue with null. The publisher
				// methods are prepared to handle this.
				// Can happen if behind an outgoing proxy..
				hostIP = null;
			}

			pub.publishStartResult(targetUrl, currUrl, 
					       hostName, hostIP,
					       resp, redirState);
			publishHttpResults(conn);
			if ((er != null) && verbose) {
				    publishEquivResults(er);
			}
			pub.publishEndResult();

			// Output redirect information
			if (!isRedirectCode(resp) && (alr == null)) {
				// Not being redirected, so terminate here
				if (targetUrl.equals(currUrl)) {
					pub.publishDestNoRedir(currUrl,
							       hostName,
							       hostIP);
				} else {
					pub.publishDestWithRedir(u,
								 currUrl,
								 hostName,
								 hostIP,
								 initial);
				}
				if ((htmlBuf != null) && doStructHash) {
					try {
						handleHTML(htmlBuf, u);
					} catch (ParserException e) {
						throw new ToolkitError(e);
					}
					break;
				}
				//if (alr == null) {
					// no application layer redirect
					//if (doStructHash &&
					//    (htmlBuf != null)) {
					//	try {
					//		handleHTML(htmlBuf);
					//	} catch (ParserException e) {
					//		// Ignore
					//	}
					//}
					//break;
				//}
			}

			// Get redirect location and start over
			try {
				String loc = null;
				URL base = u;
				if ((alr != null) && followAppRedirect) {
					loc = alr;
				} else {
					loc = conn.getHeaderField("Location");
					// Location field must be
					// absolute
					base = null;
				}
				if (loc == null) {
					break;
				}
				if (base != null) {
					u = hc.getURL(base, loc);
				} else {
					u = hc.getURL(loc);
				}
				if (u == null) {
					throw new MalformedURLException(
								  "No URL");
				}
				pub.publishRedirect(currUrl,
						    hostName, hostIP,
						    u, initial, resp);
			} catch (Exception e) {
				break;
			}
			initial = false;
		}
		pub.publishEnd();
	}


	protected void publishHttpResults(HttpURLConnection conn)
		throws IOException, ToolkitError {

		String hdrName;
		String hdrVal;
		if (verbose) {
			URL ru = conn.getURL();
			int code = conn.getResponseCode();

			for (int i = 0;  ;  i++) {
				hdrName = conn.getHeaderFieldKey(i);
				hdrVal = conn.getHeaderField(i);

				if ((hdrName == null) && (hdrVal == null)) {
					// no more entries
					break;
				}
				pub.publishElement(hdrName, hdrVal,
						   Publisher.HTTP);
			}
		} else {
			/**
			 * First, output status line:
			 * Some implementations report a null key for
			 * header field #0 and the status line in the
			 * value field.
			 * Otherwise, construct a result from the output of
			 * the respective methods...
			 */
			hdrName = conn.getHeaderFieldKey(0);
			hdrVal = conn.getHeaderField(0);
			if (! ((hdrName == null) && (hdrVal == null))) {
				pub.publishElement(hdrName, hdrVal,
						   Publisher.HTTP);
			} else {
				StringBuilder sb = new StringBuilder();
				sb.append("HTTP/ ");
				sb.append(conn.getResponseCode());
				sb.append(" ");
				sb.append(conn.getResponseMessage());
				pub.publishElement(null, sb.toString(),
						   Publisher.HTTP);
			}
			for (int i = 0;  i < hdrKeyConcise.length;  i++) {
				hdrName = hdrKeyConcise[i];
				hdrVal = conn.getHeaderField(hdrName);
				if (hdrVal != null) {
					pub.publishElement(hdrName, hdrVal,
							   Publisher.HTTP);
				}
			}
		}
	}


	protected HttpEquivResult getAppLayerEquiv(HtmlBuffer hb) {
		HttpEquivResult er = null;
		HttpEquivExtractor ex;

		if (hb == null) {
			return null;
		}
		try {
			ex = new HttpEquivExtractor(hb);
			er = ex.extract();
		} catch (ParserException e) {
			// ok, cannot parse input, so we are done...
		}
		return er;
	}


	protected String getAppLayerRedirect(HttpEquivResult er) {
		int i = er.getKey("Refresh");
		if (i < 0) {
			return null;
		}
		String val = er.getVal(i);
		if (val == null) {
			return null;
		}
		String s = val.toLowerCase();
		i = s.indexOf("url=");
		if (i < 0) {
			return null;
		}
		String res = val.substring(i + 4);
		if (Debug.get(Debug.HToken)) {
			System.err.println("getAppLayerRedirect: got '"
					   +res+"'");
		}

		return res;
	}


	protected void publishEquivResults(HttpEquivResult er)
		throws IOException, ToolkitError {
		String key;
		String val;
		for (int i = 0;  i < er.getCount();  i++) {
			key = er.getKey(i);
			val = er.getVal(i);
			pub.publishElement(key, val, Publisher.EQUIV);
		}
	}
}
