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


public class Hub extends Publisher {

	Publisher[] slot;
	int cap;
	int nPub;

	public Hub() {
		cap = 2;
		slot = new Publisher[cap];
		nPub = 0;
	}


	public Hub(int capacity) {
		cap = capacity;
		slot = new Publisher[cap];
		nPub = 0;
	}


	public void register(Publisher p) throws ToolkitError {
		if (p == null) {
			return;
		}
		if (nPub < cap) {
			// still a free slot
			slot[nPub] = p;
			nPub += 1;
		} else {
			throw new ToolkitError("No enough slots in "
						   +"publisher hub.");
		}
	}


	public void publishGlobalStart()
		throws IOException, ToolkitError {
		for (int i = 0;  i < nPub;  i++) {
			slot[i].publishGlobalStart();
		}
	}


	public void publishGlobalEnd()
		throws IOException, ToolkitError {
		for (int i = 0;  i < nPub;  i++) {
			slot[i].publishGlobalEnd();
		}
	}


	public void publishStart(String urlstr)
		throws IOException, ToolkitError {
		for (int i = 0;  i < nPub;  i++) {
			slot[i].publishStart(urlstr);
		}
	}


	public void publishStartResult(URL theTargetUrl,
				       URL theRespUrl,
				       String respHostName,
				       InetAddress respIP,
				       int respCode,
				       int redirState)
		throws IOException, ToolkitError {
		for (int i = 0;  i < nPub;  i++) {
			slot[i].publishStartResult(theTargetUrl,
						   theRespUrl,
						   respHostName, respIP,
						   respCode, redirState);
		}
	}


	public void publishElement(String key, String value)
		throws IOException, ToolkitError {
		for (int i = 0;  i < nPub;  i++) {
			slot[i].publishElement(key, value);
		}
	}


	public void publishEndResult() throws IOException, ToolkitError {
		for (int i = 0;  i < nPub;  i++) {
			slot[i].publishEndResult();
		}

	}


	public void publishEnd() throws IOException, ToolkitError {
		for (int i = 0;  i < nPub;  i++) {
			slot[i].publishEnd();;
		}

	}


	public void publishRedirect(URL respUrl,
				    String respHostName,
				    InetAddress respIP,
				    URL nextUrl,
				    boolean initial,
				    int respCode)
		throws IOException, ToolkitError {
		for (int i = 0;  i < nPub;  i++) {
			slot[i].publishRedirect(respUrl,
						respHostName, respIP,
						nextUrl,
						initial, respCode);
		}
	}


	public void publishDestNoRedir(URL reqUrl,
				       String hostName,
				       InetAddress hostIP)
		throws IOException, ToolkitError {
		for (int i = 0;  i < nPub;  i++) {
			slot[i].publishDestNoRedir(reqUrl, hostName, hostIP);
		}
	}


	public void publishDestWithRedir(URL reqUrl,
					 URL destUrl,
					 String destHostName,
					 InetAddress destIP,
					 boolean initial)
		throws IOException, ToolkitError {
		for (int i = 0;  i < nPub;  i++) {
			slot[i].publishDestWithRedir(reqUrl, destUrl,
						     destHostName, destIP,
						     initial);
		}

	}

}
