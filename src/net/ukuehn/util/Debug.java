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


package net.ukuehn.util;



import java.io.*;


public class Debug {

	public static final int Fingerprint = 0;

	public static final int Logic = 1;

	public static final int Publish = 2;

	public static final int AppLayerRedirect = 3;

	public static final int Connection = 5;

	public static final int SSL = 6;

	public static final int ProxyAuth = 7;

	public static final int Replay = 8;

	public static final int HTML = 9;

	public static final int HTMLDetail = 10;

	public static final int HToken = 11;

	public static final int HTokenDetail = 12;


	private static long debug = 0;


	public static void set(long level) {
		debug = level;

		if (debug != 0) {
			System.err.println("Setting debug level "+debug);
		}

		if (Debug.get(Debug.Fingerprint)) {
			System.err.println("Debug: Fingerprint");
		}
		if (Debug.get(Debug.Logic)) {
			System.err.println("Debug: Logic");
		}		
		if (Debug.get(Debug.Publish)) {
			System.err.println("Debug: Publish");
		}		
		if (Debug.get(Debug.AppLayerRedirect)) {
			System.err.println("Debug: AppLayerRedirect");
		}		
		if (Debug.get(Debug.Connection)) {
			System.err.println("Debug: Connection");
		}
		if (Debug.get(Debug.SSL)) {
			System.err.println("Debug: SSL");
		}
		if (Debug.get(Debug.ProxyAuth)) {
			System.err.println("Debug: ProxyAuth");
		}
		if (Debug.get(Debug.Replay)) {
			System.err.println("Debug: Replay");
		}
		if (Debug.get(Debug.HTML)) {
			System.err.println("Debug: HTML");
		}
		if (Debug.get(Debug.HTMLDetail)) {
			System.err.println("Debug: HTML Details");
		}
		if (Debug.get(Debug.HToken)) {
			System.err.println("Debug: HtmlToken");
		}
		if (Debug.get(Debug.HTokenDetail)) {
			System.err.println("Debug: HtmlToken Details");
		}
	}

	public static boolean get(int what) {
		return ((debug & (1 << what)) != 0);
	}

}