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


package net.ukuehn.util;

import java.io.*;
import java.util.Iterator;
import java.util.StringTokenizer;



public class LineReaderIterator implements Iterator<String> {

	BufferedReader bReader;
	String nextLine;
	boolean done;
	boolean suppressEmptyLines;

	protected LineReaderIterator(BufferedReader reader) {
		this(reader, false);
	}


	protected LineReaderIterator(BufferedReader reader,
				     boolean suppress) {
		bReader = reader;
		nextLine = null;
		done = false;
		suppressEmptyLines = suppress;
	}


	public static Iterator<String>
		getInstanceForFile(String fileName,
				   boolean suppress)
		throws IOException {

		BufferedReader br;
		if (fileName.equals("-")) {
			InputStreamReader ir
				= new InputStreamReader(System.in);
			br = new BufferedReader(ir);
		} else {
			FileReader fr = new FileReader(fileName);
			br = new BufferedReader(fr);
		}
		return new LineReaderIterator(br, suppress);
	}


	public static Iterator<String> getInstanceForFile(String fileName)
		throws IOException {
		return getInstanceForFile(fileName, false);
	}


	public void remove() {
		throw new UnsupportedOperationException();
	}


	public boolean hasNext()
		throws IllegalArgumentException {
		if (done) {
			return false;
		}
		if (nextLine != null) {
			return true;
		}
		while (!done && (nextLine == null)) {
			try {
				nextLine = bReader.readLine();
				while ( suppressEmptyLines &&
				        (nextLine != null) && 
				        nextLine.equals("") ) {
				       nextLine = bReader.readLine();
				}
				if (nextLine == null) {
					break;
				}
			} catch (IOException e) {
				try {
					bReader.close();
				} catch (IOException e0) {
					// ignore
				}
				done = true;
			}
		}
		return (!done && (nextLine != null));
	}


	public String next() {
		String res = nextLine;
		nextLine = null;
		return res;
	}

}
