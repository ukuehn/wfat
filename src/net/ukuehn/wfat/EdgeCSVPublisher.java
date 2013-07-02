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

import java.util.Date;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.Iterator;
import java.text.SimpleDateFormat;

import net.ukuehn.util.Debug;



public class EdgeCSVPublisher extends Publisher {

	PrintWriter out;
	boolean showHost;

	private final String DELIM = "\"";
	private final String SEP = ";";
	private final String IPPREFIX = "IP_";

	public EdgeCSVPublisher(PrintWriter output) {
		super();
		out = output;
		showHost = false;
	}


	public EdgeCSVPublisher(PrintWriter output,
				boolean longOutput, int level,
				boolean parmShowHost) {
		super(longOutput, level);
		out = output;
		showHost = parmShowHost;
	}


	public void publishGlobalStart() throws IOException, ToolkitError {
		// nothing
	}


	public void publishGlobalEnd() throws IOException, ToolkitError {
		out.flush();
	}


	public void publishStart(String urlstr)
		throws IOException, ToolkitError {
		targetUrlStr = urlstr;
	}


	public void publishStartResult(URL theTargetUrl,
				       URL theRespUrl,
				       String respHostName,
				       InetAddress respIP,
				       int respCode)
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

		if (Debug.get(Debug.Publish)) {
			System.err.println("publishElement: "+key+" "+value);
		}
	}


	public void publishRedirect(URL respUrl,
				    String respHostName,
				    InetAddress respIP,
				    URL nextUrl,
				    boolean initial,
				    int respCode)
		throws IOException, ToolkitError {

		if (Debug.get(Debug.Publish)) {
			System.err.println("publishRedirect: "
					   +respUrl.toString()+" "
					   +nextUrl.toString());
		}

		String src;
		if (initial && !showHost) {
			src = respUrl.toString();
		} else {
			src = respUrl.getHost();
		}
		String dst = nextUrl.getHost();
		out.println(src+SEP+dst);
	}


	public void publishDestNoRedir(URL reqUrl,
				       String hostName,
				       InetAddress hostIP)
		throws IOException, ToolkitError {

		if (Debug.get(Debug.Publish)) {
			System.err.println("publishDestNoRedir "
					   +reqUrl.toString());
		}

		String hostOrUrl;
		if (showHost) {
			hostOrUrl = hostName;
		} else {
			hostOrUrl = reqUrl.toString();
		}
		if (verbLevel == 0) {
			out.println(hostOrUrl);
		} else if (verbLevel > 0) {
			String ip = "";
			if (hostIP != null) {
				ip = hostIP.getHostAddress();
				out.println(hostOrUrl+SEP
					    +DELIM+IPPREFIX+ip+DELIM);
			}
		}
	}


	public void publishDestWithRedir(URL reqUrl,
					 URL respUrl,
					 String respHostName,
					 InetAddress respIP,
					 boolean initial)
		throws IOException, ToolkitError {

		if (Debug.get(Debug.Publish)) {
			System.err.println("publishDestWithRedir "
					   +reqUrl.toString()+" "
					   +respUrl.toString()+" "
					   +String.valueOf(initial));
		}
		String src;
		String dst = respUrl.getHost();
		if (initial) {
			if (showHost) {
				src = reqUrl.getHost();
			} else {
				src = reqUrl.toString();
			}
			out.println(src+SEP+dst);
		} else {
			src = reqUrl.getHost();
			// do not output here, as we got here with
			// a redirect, but that is already published
			// the previous round...
		}

		if (verbLevel > 0) {
			InetAddress ia = null;
			String ip = "";
			try {
				ia = InetAddress.getByName(dst);
				ip = ia.getHostAddress();
				out.println(dst+SEP
					    +DELIM+IPPREFIX+ip+DELIM);
			} catch (IOException e) {
				// ignore, just no output
			}
		}
	}


	public void publishEndResult() throws IOException, ToolkitError {
		// nothing
	}


	public void publishEnd() throws IOException, ToolkitError {
		out.flush();
	}




}
