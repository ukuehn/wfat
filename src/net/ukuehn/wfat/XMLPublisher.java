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

import org.xml.sax.SAXException;

import net.ukuehn.xml.SimpleXMLWriter;
import net.ukuehn.util.Debug;



public class XMLPublisher extends Publisher {

	SimpleXMLWriter xw;


	public XMLPublisher(SimpleXMLWriter xmlWriter) {
		super();
		xw = xmlWriter;
	}


	public XMLPublisher(SimpleXMLWriter xmlWriter,
			    boolean longOutput, int level) {
		super(longOutput, level);
		xw = xmlWriter;
	}


	public void publishGlobalStart()
		throws IOException, ToolkitError {
		try {
			xw.startDocument();
			//xw.startElement("CollectionResult");
			xw.startElement(XFS.ECOLLECTION);
		} catch (SAXException e) {
			throw new ToolkitError(e);
		}
	}


	public void publishGlobalEnd()
		throws IOException, ToolkitError {
		try {
			xw.endElement();  // XFS.ECOLLECTION
			xw.endDocumentNL();
		} catch (SAXException e) {
			throw new ToolkitError(e);
		}
	}


	public void publishStart(String urlstr)
		throws IOException, ToolkitError {

		targetUrlStr = urlstr;
		try {
			//xw.startElement("Target");
			//xw.attribute("OrigURL", urlstr);
			xw.startElement(XFS.ETARGET);
			xw.attribute(XFS.ATARGETURL, urlstr);
		} catch (SAXException e) {
			throw new ToolkitError(e);
		}
	}


	public void publishStartResult(URL theTargetUrl,
				       URL theRespUrl,
				       String respHostName,
				       InetAddress respIP,
				       int respCode,
				       int redirState)
		throws IOException, ToolkitError {
		try {
			//xw.startElement("ResultSet");
			//xw.attribute("ResultURL", theRespUrl.toString());
			xw.startElement(XFS.ERESPONSE);
			xw.attribute(XFS.ARESPURL, theRespUrl.toString());

			xw.attribute(XFS.ARESPHOST, respHostName);
			//if ((hostName != null) && !hostName.equals("")) {
			//xw.attribute("Host", hostName);
			//} else {
			//xw.attribute("Host", "");
			//}
			String ipStr = new String();
			if (respIP != null) {
				ipStr = respIP.getHostAddress();
			}
			//xw.attribute("IP", ipStr);
			//xw.attribute("RespCode", String.valueOf(respCode));
			xw.attribute(XFS.ARESPIP, ipStr);
			xw.attribute(XFS.ARESPCODE, String.valueOf(respCode));
			String redirName = "";
			if (redirState == REDIRHTTP) {
				redirName = XFS.VRHTTP;
			} else if (redirState == REDIREQUIV) {
				redirName = XFS.VREQUIV;
			} else if (redirState == REDIRNONE) {
				redirName = XFS.VRNONE;
			}
			xw.attribute(XFS.AREDIR, redirName);
		} catch (SAXException e) {
			throw new ToolkitError(e);
		}
	}

	
	public void publishElement(String key, String value, int source)
		throws IOException, ToolkitError {
		if (key == null) {
			key = "";
		}
		if (value == null) {
			value = "";
		}
		try {
			//xw.startElement("Header");
			//xw.attribute("Key", key);
			//xw.attribute("Value", value);
			//xw.endElement();  // Header
			xw.startElement(XFS.EHEADER);
			String srcName;
			if (source == HTTP) {
				srcName = XFS.VSRCHTTP;
			} else {
				srcName = XFS.VSRCEQUIV;
			}
			xw.attribute(XFS.ASOURCE, srcName);
			xw.attribute(XFS.AKEY, key);
			xw.attribute(XFS.AVALUE, value);
			xw.endElement();  // XFS.EHEADER
		} catch (SAXException e) {
			throw new ToolkitError(e);
		}
	}


	public void publishException(String msg)
		throws IOException, ToolkitError {
		try {
			xw.startElement(XFS.EEXCEPTION);
			if (msg != null) {
				xw.attribute(XFS.AMSG, msg);
			}
			xw.endElement();
		} catch (SAXException e) {
			throw new ToolkitError(e);
		}
	}


	public void publishEndResult() throws IOException, ToolkitError {
		try {
			xw.endElement();  // XFS.ERESPONSE
		} catch (SAXException e) {
			throw new ToolkitError(e);
		}
	}


	public void publishEnd() throws IOException, ToolkitError {
		try {
			xw.endElement();  // XFS.ETARGET
		} catch (SAXException e) {
			throw new ToolkitError(e);
		}
		xw.flush();
	}


}
