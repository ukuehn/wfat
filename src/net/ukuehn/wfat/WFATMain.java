/* -*- java -*-
 *
 * This is WebForrestAnalysisToolkit, a structural and security analysis tool
 * for http server configurations.
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

import java.util.StringTokenizer;

import java.net.HttpURLConnection;
import java.net.URL;
import java.net.MalformedURLException;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.X509TrustManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import java.security.NoSuchAlgorithmException;
import java.security.KeyManagementException;
import java.security.SecureRandom;

import java.util.Iterator;
import java.util.LinkedList;

import java.net.Proxy;
import java.net.InetSocketAddress;
import java.net.Authenticator;

import net.ukuehn.util.LineReaderIterator;
import net.ukuehn.xml.SimpleXMLWriter;

import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.SAXParserFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;

import net.ukuehn.net.ProxyAuthenticator;
import net.ukuehn.security.NullTrustManager;
import net.ukuehn.security.NullHostnameVerifier;
import net.ukuehn.util.Debug;


public class WFATMain {

	static final String progname="WebForrestAnalysisToolkit";
	static final String version = "0.6";
	static final String prompt = progname+" v"+version
		+" by Ulrich Kuehn <ukuehn@acm.org>";

	static Proxy proxy = null;
	static ProxyAuthenticator proxyAuth = null;	

	static final String usage =
		"WebForrestAnalysisToolkit version "+version+" by "
		+"Ulrich Kuehn <ukuehn@acm.org>\n\n"
		+"Usage:\n"
		+"java -jar wfat-"+version+".jar [options] "
		+"[<url>] ...\n\n"
		+"  where options are:\n\n"
		+"  -N         Do not follow redirects. Wins over -A.\n\n"
		+"  -A         Follow application-level redirects, i.e. meta "
		+"tags with\n"
		+"             http-equiv=\"refresh\".\n\n"
		+"  -v         Show all header fields and values, including "
		+"application-level\n"
		+"             headers (http-equiv). Use also to include "
		+"host IP address\n"
		+"             when creating a graph.\n\n"
		//+"  -H         Run structural hash over retrieved HTML.\n\n"
		+"  -c         Format output as CSV.\n\n"
		+"  -g         Format output as graph structure (edges) "
		+"suitable for import\n"
		+"             into Gephi. Use -v to include host IP "
		+"address as\n"
		+"             an extra node.\n\n"
		+"  -r <file>  Read back XML output from <file> and replays "
		+"the contents as\n"
		+"             responses to the requests that were made. Use "
		+"to transform the\n"
		+"             output without doing the actual requests.\n"
		+"             Ignores the command line urls, -f, -F, -p, and "
		+"-P options.\n\n"
		+"  -i         in conjunction with -r, query the IP addresses "
		+"for each host.\n"
		+"             Useful for inserting the IP addresses if the "
		+"original queries\n"
		+"             happened behind a proxy server.\n\n"
		+"  -f <file>  Read urls from file <file> instead of\n"
		+"             using command line args. Use - for stdin.\n\n"
		+"  -d <n>     Wait for <n> milliseconds between requests.\n\n"
		+"  -u <agent> Use <agent> as user agent string in "
		+"each request.\n\n"
		+"  -p <proto> Use <proto> as default for urls w/o "
		+"explicitly given protocol,\n"
		+"             defaults to 'http://'.\n\n"
		+"  -X <file>  Consider any hosts give in <file> as "
		+"forbidden. Do never query\n"
		+"             any of these hosts, even when "
		+"redirected to.\n\n"
		+"  -?         Print version, help and exit.\n\n"
		+"  -P <proxyspec>  Specifiy HTTP proxy server. Formats for "
		+"<proxyspec> are\n"
		+"             <proxy>:<port>[:<uid>:<pw>] or "
		+"[<uid>[:<pw>]@]<proxy>[:<port>]\n"
		+"             with optional basic authentication using user "
		+"id <uid> and\n"
		+"             password <pw>.\n\n";

	String warnGraphNoRedirect =
		"Warning: using graph mode without following or allowing "
		+"redirects does not \n"
		+"make sense, proceeding anyway...";



	String optArgProxy = null;
	String optArgDebug = null;
	String optArgDelay = null;
	String optArgListFile = null;
	String optArgForbidden = null;
	String optArgUserAgent = null;
	String optArgGraphFile = null;
	String optArgDefaultProto = null;
	//int optRedirectCount = 0;
	//boolean optRedirects = false;
	boolean optNoRedirect = false;
	boolean optFollowAppRedir = false;
	boolean optStructHash = false;
	//boolean optVerbRedirects = false;
	boolean optCSV = false;
	String optArgReplayFile = null;
	boolean optEvalIP = false;
	boolean optGraph = false;
	boolean optGraphHost = false;
	Iterator<String> forbiddenHosts = null;
	Iterator<String> urlArgs = null;
	int verbLevel = 0;
	int delay = 0;

	Publisher pub = null;
	Hub hub = null;


	/**
	 * See also http://openbook.galileocomputing.de/javainsel9/
	 *            javainsel_21_003.htm#mj5e7ad4990f3f7ccbf976c3eab61c5887
	 *
	 */
	protected void setProxy(String proxyArg)
		throws NumberFormatException {

		String proxyHost = null;
		String sProxyPort = null;
		int proxyPort = 8080;   // default value
		String uid = null;
		String pw = null;
		StringTokenizer st;

		if (proxyArg == null) {
			return;
		}

		/* Try to parse [uid[:pw]@]host[:port], but if the
                 * user part uid[:pw] is empty, accept also
                 * host[:port[:uid[:pw]]]
                 */
		st = new StringTokenizer(proxyArg, "@");
		String hostPart = null;
		String userPart = null;
		if (st.hasMoreTokens()) {
			hostPart = st.nextToken();
		}
		if (st.hasMoreTokens()) {
			// hostPart is actually userPart
			// so shift it
			userPart = hostPart;
			hostPart = st.nextToken();
		}

		// we know that hostPart is non-null, as proxyArg is != null
		st = new StringTokenizer(hostPart, ":");

		if (st.hasMoreTokens()) {
			proxyHost = st.nextToken();
		}
		if (st.hasMoreTokens()) {
			sProxyPort = st.nextToken();
		}

		if (userPart != null) {
			// if we have a userPart, ignore rest of
			// hostPart after the port
			st = new StringTokenizer(userPart, ":");
		}

		// Common part to parse uid[:pw] part, either from
		// part left of @, or right of host:port
		if (st.hasMoreTokens()) {
			uid = st.nextToken();
		}
		if (st.hasMoreTokens()) {
			pw = st.nextToken();
		}
		proxyPort = 8080;  // default port
		if (sProxyPort != null) {
			try {
				proxyPort = Integer.parseInt(sProxyPort);
			} catch (NumberFormatException e) {
				throw new NumberFormatException(
					   "Port number must be a number: "
					   +sProxyPort);
			}
		}
		if (proxyHost != null) {
			//System.err.println("Setting proxy "
			//		   +proxyHost+":"+sProxyPort);
			proxy = new Proxy(Proxy.Type.HTTP,
					  new InetSocketAddress(proxyHost,
								proxyPort));
			//System.err.println("proxy.toString(): "
			//		   +proxy.toString());
			System.setProperty("http.proxyHost", proxyHost);
			System.setProperty("http.proxyPort", sProxyPort);
		}
		if (uid != null) {
			//System.err.println("new ProxyAuthenticator("
			//		   +uid+", "+pw+")");
			proxyAuth = new ProxyAuthenticator(proxy, uid, pw);
			proxyAuth.setDebug(Debug.get(Debug.ProxyAuth));
			Authenticator.setDefault(proxyAuth);
		}
	}


	Iterator<String> getStringUrlsFromCmdLine(String args[],
						  int startArg) {
		LinkedList<String> theList = new LinkedList<String>();
		for (int nextarg = startArg;
		     nextarg < args.length;  nextarg++) {
			theList.add(args[nextarg]);
		}
		return theList.listIterator();
	}


	/**
	 * Prepare to disable SSL certificate and hostname
	 * verification.
	 *
	 * DANGER: Never do this in an environment where security
	 * is essential.
	 */
	protected void disableSSLChecks()
		throws ToolkitError {
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
		SSLSocketFactory f = sc.getSocketFactory();
		HttpsURLConnection.setDefaultSSLSocketFactory(f);

		HostnameVerifier nullVerifier
			= new NullHostnameVerifier(Debug.get(Debug.SSL));
		HttpsURLConnection.setDefaultHostnameVerifier(nullVerifier);
	}


	protected void prepareUrlList(String[] args, int start)
		throws IOException {

		if (optArgListFile != null) {
			if (Debug.get(Debug.Logic)) {
				System.err.println("Getting urls from file "
						   +optArgListFile);
			}
			urlArgs = LineReaderIterator.
				getInstanceForFile(optArgListFile,
						   true);
		} else {
			if (start == args.length) {
				// no urls given
				usage();
			}
			// Handle non-option args as urls to
			// fingerprint
			if (Debug.get(Debug.Logic)) {
				System.err.println("Getting urls cmd line");
			}
			urlArgs = getStringUrlsFromCmdLine(args, start);
		}
	}


	protected void preparePublisher()
		throws IOException, ToolkitError {

		//boolean longOut = (optRedirects || optVerbRedirects);
		boolean longOut = !optNoRedirect;
		if (optArgGraphFile != null) {
			BufferedWriter out;
			if (optArgGraphFile.equals("-")) {
				out = new BufferedWriter(
					new OutputStreamWriter(System.out));
			} else {
				out = new BufferedWriter(
					 new FileWriter(optArgGraphFile));
			}
			PrintWriter pw = new PrintWriter(out);
			Publisher ep = new EdgeCSVPublisher(pw, longOut,
							    verbLevel,
							    optGraphHost);
			hub = new Hub(2);
			hub.register(ep);
			optGraph = false;
		}
		if (optGraph) {
			PrintWriter out = new PrintWriter(System.out);
			//   new OutputStreamWriter(System.out));
			pub = new EdgeCSVPublisher(out, longOut,
						   verbLevel, optGraphHost);
		} else if (optCSV) {
			PrintWriter out = new PrintWriter(System.out);
			pub = new CSVPublisher(out,
					       longOut, verbLevel);
		} else {
			BufferedWriter out
				= new BufferedWriter(
					    new PrintWriter(System.out));
			SimpleXMLWriter xw =
				new SimpleXMLWriter(out);
			pub = new XMLPublisher(xw, longOut, verbLevel);
		}
		if (hub != null) {
			hub.register(pub);
			pub = hub;
		}
	}


	protected void doit(String args[])
		throws IOException, ToolkitError {

		int nextopt;
		int debug = 0;


		nextopt = 0;
		for (nextopt = 0;  nextopt < args.length;  nextopt++) {

			/* handle options here */
			if (!args[nextopt].startsWith("-")) {
				break;
			}
			if (args[nextopt].equals("-?")) {
				usage();
			} else if (args[nextopt].equals("-v")) {
				verbLevel += 1;
			} else if (args[nextopt].equals("-D")) {
				nextopt++;
				optArgDebug = args[nextopt];
			} else if (args[nextopt].equals("-d")) {
				nextopt++;
				optArgDelay = args[nextopt];
			} else if (args[nextopt].equals("-N")) {
				optNoRedirect = true;
			} else if (args[nextopt].equals("-A")) {
				optFollowAppRedir = true;
			} else if (args[nextopt].equals("-H")) {
				optStructHash = true;
			//} else if (args[nextopt].equals("-F")) {
			//	optRedirectCount += 1;
			} else if (args[nextopt].equals("-f")) {
				nextopt++;
				optArgListFile = args[nextopt];
			} else if (args[nextopt].equals("-u")) {
				nextopt++;
				optArgUserAgent = args[nextopt];
			} else if (args[nextopt].equals("-P")) {
				nextopt++;
				optArgProxy = args[nextopt];
			} else if (args[nextopt].equals("-c")) {
				optCSV = true;
			} else if (args[nextopt].equals("-r")) {
				nextopt++;
				optArgReplayFile = args[nextopt];
			} else if (args[nextopt].equals("-i")) {
				optEvalIP = true;
			} else if (args[nextopt].equals("-g")) {
				optGraph = true;
			} else if (args[nextopt].equals("-G")) {
				nextopt++;
				optArgGraphFile = args[nextopt];
			} else if (args[nextopt].equals("-h")) {
				optGraphHost = true;
			} else if (args[nextopt].equals("-p")) {
				nextopt++;
				optArgDefaultProto = args[nextopt];
			} else if (args[nextopt].equals("-X")) {
				nextopt++;
				optArgForbidden = args[nextopt];
			} else {
				/* Wrong option given */
				usage();
			}
		}
		if ( (nextopt == args.length) &&
		     (optArgListFile == null) &&
		     (optArgReplayFile == null) ) {
			// no urls given
			usage();
		}

		if (optArgDebug != null) {
			try {
				debug = Integer.parseInt(optArgDebug);
			} catch (NumberFormatException e) {
				System.err.println("Debug setting "
						   +optArgDebug
						   +" must be a number.");
				System.exit(-1);
			}
		}
		Debug.set(debug);

		if (optArgDelay != null) {
			try {
				delay = Integer.parseInt(optArgDelay);
			} catch (NumberFormatException e) {
				System.err.println("Warning: Delay setting "
						   +optArgDelay
						   +" must be a number, "
						   +"using 0 instead.");
				delay = 0;
			}
		}

		//if (optRedirectCount > 0) {
		//	optRedirects = true;
		//	if (optRedirectCount > 1) {
		//		optVerbRedirects = true;
		//	}
		//}

		preparePublisher();

		if (optArgReplayFile != null) {

			pub.publishGlobalStart();

			XMLFileReplay xr = new XMLFileReplay(pub, verbLevel);
			xr.setEvalIP(optEvalIP);
			DefaultHandler handler = xr;
			SAXParserFactory factory
				= SAXParserFactory.newInstance();
			try {
				SAXParser sp = factory.newSAXParser();
				File f = new File(optArgReplayFile);
				sp.parse(f, handler);
			} catch (Exception e) {
				throw new ToolkitError(e);
			}

			pub.publishGlobalEnd();
			
		} else {
			if (optGraph || (optArgGraphFile != null)) {
				//	if (optRedirectCount == 0) {
				if (optNoRedirect) {
					System.err.println(
						   warnGraphNoRedirect);
				}
			}

			if (optArgProxy != null) {
				setProxy(optArgProxy);
			}
			prepareUrlList(args, nextopt);
			if (urlArgs == null) {
				usage();
			}
			if (optArgForbidden != null) {
				forbiddenHosts = LineReaderIterator.
					getInstanceForFile(optArgForbidden,
							   true);
			}

			disableSSLChecks();

			ConnectionHandler hc = new ConnectionHandler(proxy);
			if (optArgUserAgent != null) {
				hc.setUserAgent(optArgUserAgent);
			}
			if (forbiddenHosts != null) {
				hc.setForbiddenHosts(forbiddenHosts);
			}
			if (optArgDefaultProto != null) {
				hc.setDefaultProto(optArgDefaultProto);
			}

			HTTPFingerprint hfp = new HTTPFingerprint(pub, hc);
			hfp.setNoRedirect(optNoRedirect);
			hfp.setFollowAppRedirect(optFollowAppRedir);
			hfp.setVerbose((verbLevel > 0));
			hfp.setStructHash(optStructHash);
			hfp.setDelay(delay);
			pub.publishGlobalStart();

			while (urlArgs.hasNext()) {
				String urlarg = urlArgs.next();
				hfp.fingerprint(urlarg);
			}
			pub.publishGlobalEnd();
		}

	}


	protected static void usage() {
		System.err.println(usage);
		System.exit(1);
	}


	public static void main(String args[])
		throws IOException, ToolkitError {

		WFATMain app = new WFATMain();

		app.doit(args);
	}

}
