/* -*- java -*-
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

package net.ukuehn.net;


import java.net.*;
import java.io.*;

import java.util.StringTokenizer;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.InetSocketAddress;


public class ProxyAuthenticator extends Authenticator {

	private String uid;
	private String pass;
	private Proxy proxy;

	private boolean debug;


	public ProxyAuthenticator(String userId, String passwd) {
		this(null, userId, passwd);
	}


	public ProxyAuthenticator(Proxy theProxy,
				  String userId, String passwd) {
		uid = userId;
		pass = passwd;
		proxy = theProxy;
		debug = false;
	}


	public void setDebug(boolean parm) {
		debug = parm;
	}


	public PasswordAuthentication getPasswordAuthentication() {

		if (debug) {
			System.err.println("getPasswordAuthentication():");
			System.err.println("  requestingHost "
					   +getRequestingHost());
			System.err.println("  requestingPort "
					   +getRequestingPort());
			System.err.println("  requestingProtocol "
					   +getRequestingProtocol());
			System.err.println("  requestingScheme "
					   +getRequestingScheme());
			System.err.println("  requestingPrompt "
					   +getRequestingPrompt());
			System.err.println("  requestingSite "
					   +getRequestingSite().toString());
		}
		if (proxy != null) {
			SocketAddress sockAddr = proxy.address();
			if (!(sockAddr instanceof InetSocketAddress)) {
				return null;
			}
			InetSocketAddress pisa = (InetSocketAddress)sockAddr;
			
			String proxyName = pisa.getHostName();
			int proxyPort = pisa.getPort();

			if (debug) {
				System.err.println("ProxyName "+proxyName);
				System.err.println("ProxyPort "
						   +String.valueOf(proxyPort));
			}
			if ((proxyPort == getRequestingPort()) &&
			    proxyName.equalsIgnoreCase(getRequestingHost())) {
				return new PasswordAuthentication(uid,
						    pass.toCharArray());
			}
		} else {
			return new PasswordAuthentication(uid,
							  pass.toCharArray());
		}
		return null;
	}

}
