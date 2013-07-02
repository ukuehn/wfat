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

import java.util.Date;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.Iterator;
import java.text.SimpleDateFormat;



public class Publisher {


	/* To be used for publishStartResult() to indicate what kind of
	 * redirection the server response represents:
	 * REDIRNONE   no redirection
	 * REDIRHTTP   redirection via HTTP
	 * REDIREQUIV  redirection via http-equiv
	 */
	public static final int REDIRNONE = 0;
	public static final int REDIRHTTP = 1;
	public static final int REDIREQUIV = 2;


	/* To be used for publishElement() to indicate what the source
	 * of the element is:
	 * HTTP    for header elements from the http protocol
	 * EQUIV   for header elements that are provided by html meta tags
	 *         using http-equiv
	 */
	public static final int HTTP = 0;
	public static final int EQUIV = 1;


	int verbLevel;
	boolean longMode;

	URL targetUrl;
	URL responseUrl;
	int responseCode;

	String targetUrlStr;
	String respUrlStr;


	public Publisher() {
		this(false, 0);
	}


	public Publisher(boolean longOutput, int level) {
		verbLevel = level;
		longMode = longOutput;
		targetUrl = null;
		responseUrl = null;
		targetUrlStr = "";
		respUrlStr = "";
		responseCode = 0;
	}


	public void publishGlobalStart() throws IOException, ToolkitError {
		// nothing
	}


	public void publishGlobalEnd() throws IOException, ToolkitError {
		// nothing
	}


	public void publishStart(String urlstr)
		throws IOException, ToolkitError {
		targetUrlStr = urlstr;
	}


	// Must be prepared to handel hostIP == null
	public void publishStartResult(URL theTargetUrl,
				       URL theRespUrl,
				       String hostName,
				       InetAddress hostIP,
				       int respCode,
				       int redirState)
		throws IOException, ToolkitError {

		responseCode = respCode;
		responseUrl = theRespUrl;
		targetUrlStr = "";
		respUrlStr = "";
		try {
			targetUrlStr = targetUrl.toString();
			respUrlStr = responseUrl.toString();
		} catch (Exception e) {
			// ignore
		}
	}


	public void publishElement(String key, String value)
		throws IOException, ToolkitError {
		publishElement(key, value, HTTP);
	}


	public void publishElement(String key, String value, int source)
		throws IOException, ToolkitError {
		// nothing
	}


	public void publishException(String msg)
		throws IOException, ToolkitError {
		// nothing
	}


	// Must be prepared to handel respIP == null
	public void publishRedirect(URL respUrl,
				    String respHostName,
				    InetAddress respIP,
				    URL nextUrl,
				    boolean initial,
				    int respCode)
		throws IOException, ToolkitError {
		// nothing
	}


	// Must be prepared to handel hostIP == null
	public void publishDestNoRedir(URL reqUrl,
				       String hostName,
				       InetAddress hostIP)
		throws IOException, ToolkitError {
		// nothing
	}


	// Must be prepared to handel respIP == null
	public void publishDestWithRedir(URL reqUrl,
					 URL respUrl,
					 String respHostName,
					 InetAddress respIP,
					 boolean initial)
		throws IOException, ToolkitError {
		// nothing
	}


	public void publishEndResult() throws IOException, ToolkitError {
		// nothing
	}


	public void publishEnd() throws IOException, ToolkitError {
		// nothing
	}


}
