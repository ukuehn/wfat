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


// Strings for XML File Structure
public class XFS {

	public static final String ECOLLECTION = "CollectionResult";
	public static final String ACOLLDEFAULT = "DefaultProto";

	public static final String ETARGET = "Target";
	public static final String ATARGETURL = "TargetURL";

	public static final String ERESPONSE = "ServerResponse";
	public static final String ARESPURL = "ReqURL";
	public static final String ARESPHOST = "Host";
	public static final String ARESPIP   = "IP";
	public static final String ARESPCODE = "RespCode";
	public static final String AREDIR = "Redir";
	public static final String VRNONE = "none";
	public static final String VRHTTP = "http";
	public static final String VREQUIV = "html";

	public static final String EEXCEPTION = "Exception";
	public static final String AMSG = "msg";

	public static final String EHEADER = "Header";
	public static final String ASOURCE = "Source";
	public static final String AKEY = "Key";
	public static final String AVALUE = "Value";

	public static final String VSRCHTTP = "http";
	public static final String VSRCEQUIV = "html";
}
