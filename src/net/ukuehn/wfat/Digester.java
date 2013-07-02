/* -*- java -*-
 *
 * (C) 2013 Ulrich Kuehn <ukuehn@acm.org>
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

package net.ukuehn.wfat;


import javax.net.ssl.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;




public class Digester {

	protected final static String alg = "SHA1";

	public Digester() throws InstallationError {
		try {
			MessageDigest md = MessageDigest.getInstance(alg);
		} catch (NoSuchAlgorithmException e) {
			throw new InstallationError("Hash algorithm "+alg
						    +" not available: ", e);
		}
	}		


	public MessageDigest getDigester() {
		try {
			MessageDigest md = MessageDigest.getInstance(alg);
			return md;
		} catch (NoSuchAlgorithmException e) {
			// ignore, constructor has checked already
			//throw new InstallationError("Hash algorithm "+alg
			//			    +" not available: ", e);
			return null;
		}
	}


}
