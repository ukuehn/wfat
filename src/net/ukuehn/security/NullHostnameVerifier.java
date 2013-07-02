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
 *
 */


/*
 * Use this class to disable host name validation in SSL.
 *
 * DANGER: Use only if you are sure that you do not want a secure SSL
 *         connection.
 */


package net.ukuehn.security;


import javax.net.ssl.*;
import java.security.cert.*;
import javax.security.auth.*;

import javax.net.*;
import java.net.*;
import java.io.*;


public class NullHostnameVerifier implements HostnameVerifier {

	private boolean debug;

	public NullHostnameVerifier() {
		this(false);
	}


	public NullHostnameVerifier(boolean parm) {
		debug = parm;
		if (debug) {
			System.err.println("Instantiating "
					   +"NullHostnameVerifier");
		}
	}


	public boolean verify(String hostname, SSLSession session) {
		if (debug) {
			System.err.println("Verifying host '"
					   +hostname
					   +"' -> true");
		}
		return true;
	}

}
