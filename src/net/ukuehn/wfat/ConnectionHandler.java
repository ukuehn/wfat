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

package net.ukuehn.wfat;


import java.net.*;
import java.io.*;

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


import net.ukuehn.security.NullTrustManager;
import net.ukuehn.security.NullHostnameVerifier;
import net.ukuehn.util.Debug;



public class ConnectionHandler {


	String userAgent;
	Proxy proxy;
	SSLSocketFactory sf;
	HostnameVerifier hv;
	HashSet<String> forbiddenHosts;

	private String defaultProto = "http://";


	public ConnectionHandler(Proxy theProxy) throws ToolkitError {
		userAgent = null;
		proxy = theProxy;
		initDisabledSSLChecks();
		forbiddenHosts = new HashSet<String>();
	}


	public void setDefaultProto(String defProt) {
		defaultProto = defProt;
	}


	public void setUserAgent(String ua) {
		userAgent = ua;
	}


	protected void addForbidden(String host) {
		URL u;

		try {
			/* construct url, also try default proto/scheme */
			u = getURL(host); 
			String realHost = u.getHost();
			forbiddenHosts.add(realHost);
		} catch (MalformedURLException e) {
			/* ignore */
		}
	}


	public void setForbiddenHosts(String[] hosts) {
		for (int i = 0;  i < hosts.length;  i++) {
			addForbidden(hosts[i]);
		}
	}


	public void setForbiddenHosts(Iterator<String> hosts) {
		while (hosts.hasNext()) {
			addForbidden(hosts.next());
		}
	}


	/**
	 * Prepare to disable SSL certificate and hostname
	 * verification.
	 *
	 * DANGER: Never do this in an environment where security
	 * is essential.
	 */
	protected void initDisabledSSLChecks() throws ToolkitError {
		SSLContext sc = null;
		try {
			X509TrustManager[] tm = {
				new NullTrustManager(Debug.get(Debug.SSL))
			};
			sc = SSLContext.getInstance("SSL");
			sc.init(null, tm, new SecureRandom());
		} catch (KeyManagementException e) {
			// Ignore, should not occur
		} catch (NoSuchAlgorithmException e) {
			throw new InstallationError("No SSL in Java?", e);
		}
		sf = sc.getSocketFactory();
		HttpsURLConnection.setDefaultSSLSocketFactory(sf);

		hv = new NullHostnameVerifier(Debug.get(Debug.SSL));
		HttpsURLConnection.setDefaultHostnameVerifier(hv);
	}


	protected void disableSSLChecks(HttpsURLConnection sconn){
		sconn.setHostnameVerifier(hv);
		sconn.setSSLSocketFactory(sf);
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


	protected URL getURL(URL curr, String urlStr)
		throws MalformedURLException {
		URL u;

		try {
			u = new URL(curr, urlStr);
		} catch (MalformedURLException e) {
			try {
				u = new URL(curr, defaultProto+urlStr);
			} catch (MalformedURLException ex) {
				throw new MalformedURLException(
					"Neither "
					+urlStr+" nor "
					+defaultProto+urlStr+" is a valid "
					+"URL in context "
					+((curr!=null)?curr.toString():"")
					+".");
			}
		}
		return u;
	}


	public boolean isForbidden(URL u) {
		return forbiddenHosts.contains(u.getHost());
	}
	

	public boolean isForbidden(String host) {
		return forbiddenHosts.contains(host);
	}


	public HttpURLConnection prepareConnection(URL u,
						   boolean follow)
		throws IOException {

		HttpURLConnection conn;

		if (isForbidden(u)) {
			if (Debug.get(Debug.Connection)) {
				System.err.println("prepareConnection: "
						   +"host '"+u.getHost()
						   +"' is forbidden.");
			}
			return null;
		}
		if (proxy != null) {
			conn = (HttpURLConnection)u.openConnection(proxy);
		} else {
			conn = (HttpURLConnection)u.openConnection();
		}
		if (Debug.get(Debug.SSL)) {
			if (conn instanceof HttpsURLConnection) {
				System.err.println("HttpsURLConnection("
						   +conn.getURL()
						   +")");
			}
		}
		try {
			conn.setRequestMethod("GET");
		} catch (ProtocolException e) {
			// ignore, get is used by default
		}
		conn.setInstanceFollowRedirects(follow);
		if (userAgent != null) {
			conn.setRequestProperty("User-Agent", userAgent);
		}
		conn.connect();
		return conn;
	}



}
