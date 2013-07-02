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



public class CSVPublisher extends Publisher {

	PrintWriter out;
	int groupLineCounter;

	private final String DELIM = "\"";
	private final String SEP = ";";
	private final String SRCHTTP = "http";
	private final String SRCEQUIV = "html";

	public CSVPublisher(PrintWriter output) {
		super();
		out = output;
		groupLineCounter = 0;
	}


	public CSVPublisher(PrintWriter output,
			    boolean longOutput, int level) {
		super(longOutput, level);
		out = output;
		groupLineCounter = 0;
	}


	public void publishGlobalStart() throws IOException, ToolkitError {
		groupLineCounter = 0;
		if (longMode) {
			out.println(DELIM+"TargetURL"+DELIM+SEP
				    +DELIM+"RespURL"+DELIM+SEP
				    +DELIM+"RespCode"+DELIM+SEP
				    +DELIM+"Src"+DELIM+SEP
				    +DELIM+"HeaderKey"+DELIM+SEP
				    +DELIM+"Value"+DELIM);
		} else {
			out.println(DELIM+"TargetURL"+DELIM+SEP
				    +DELIM+"HeaderKey"+DELIM+SEP
				    +DELIM+"Value"+DELIM);
		}
	}


	public void publishGlobalEnd() throws IOException, ToolkitError {
		out.flush();
	}


	public void publishStart(String urlstr)
		throws IOException, ToolkitError {
		targetUrlStr = urlstr;
		groupLineCounter = 0;
	}


	public void publishStartResult(URL theTargetUrl,
				       URL theRespUrl,
				       String hostName,
				       InetAddress hostIP,
				       int respCode,
				       int redirState)
		throws IOException, ToolkitError {

		groupLineCounter = 0;
		responseCode = respCode;
		targetUrl = theTargetUrl;
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


	public void publishElement(String key, String value, int source)
		throws IOException, ToolkitError {

		groupLineCounter += 1;

		StringBuilder sb = new StringBuilder();
		sb.append(DELIM).append(targetUrlStr).append(DELIM);
		sb.append(SEP);
		if (longMode) {
			sb.append(DELIM).append(respUrlStr).append(DELIM);
			sb.append(SEP);
		}
		sb.append(String.valueOf(responseCode)).append(SEP);
		if (longMode) {
			sb.append(DELIM);
			if (source == HTTP) {
				sb.append(SRCHTTP);
			} else if (source == EQUIV) {
				sb.append(SRCEQUIV);
			} else {
				throw new ToolkitError(
					    "Internal Error: Unknown source "
					    +"indicated for header: "
					    +String.valueOf(source));
			}
			sb.append(DELIM).append(SEP);
		}
		sb.append(DELIM).append(key).append(DELIM).append(SEP);
		sb.append(DELIM).append(value).append(DELIM);

		out.println(sb.toString());
	}


	public void publishEndResult() throws IOException, ToolkitError {
		if (groupLineCounter == 0) {
			// no output so far, so make sure to
			// output a summary that indicates this
			// fact
			StringBuilder sb = new StringBuilder();
			sb.append(DELIM).append(targetUrlStr).append(DELIM);
			sb.append(SEP);
			if (longMode) {
				sb.append(DELIM).append(respUrlStr);
				sb.append(DELIM).append(SEP);
			}
			sb.append(SEP).append(SEP);

			out.println(sb.toString());
			return;
		}
		// nothing else
	}


	public void publishEnd() throws IOException, ToolkitError {
		out.flush();
	}


}
