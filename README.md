WFAT README
===

WFAT is the Web Forrest Analysis Toolkit designed to help analyse a
large number of web sites for redirection structure, configured
headers etc.

Installation
---

Type "ant".


Normal usage
---

To analyse responses from a single URL run

	java -jar wfat.jar <url>

or, for verbose output, run

	java -jar wfat.jar -v <url>

To analyse responses for a list of URL, place them in a file and run

	java -jar wfat.jar -f <filename>

possibly with the -v option.





Options
---

  -N         Do not follow redirects. Wins over -A.

  -A         Follow application-level redirects, i.e. meta tags with
             http-equiv="refresh".

  -v         Show all header fields and values, including application-level
             headers (http-equiv). Use also to include host IP address
             when creating a graph.

  -c         Format output as CSV.

  -g         Format output as graph structure (edges) suitable for import
             into Gephi. Use -v to include host IP address as
             an extra node.

  -r <file>  Read back XML output from <file> and replays the contents as
             responses to the requests that were made. Use to transform the
             output without doing the actual requests.
             Ignores the command line urls, -f, -F, -p, and -P options.

  -i         in conjunction with -r, query the IP addresses for each host.
             Useful for inserting the IP addresses if the original queries
             happened behind a proxy server.

  -f <file>  Read urls from file <file> instead of
             using command line args. Use - for stdin.

  -d <n>     Wait for <n> milliseconds between requests.

  -u <agent> Use <agent> as user agent string in each request.

  -p <proto> Use <proto> as default for urls w/o explicitly given protocol,
             defaults to 'http://'.

  -X <file>  Consider any hosts give in <file> as forbidden. Do never query
             any of these hosts, even when redirected to.

  -?         Print version, help and exit.

  -P <proxyspec>  Specifiy HTTP proxy server. Formats for <proxyspec> are
             <proxy>\[\:<port>\[\:<uid>\[\:<pw>\]\]\] or \[<uid>\[\:<pw>\]@]<proxy>\[\:<port>\]
             with optional basic authentication using user id <uid> and
             password <pw>.




Credits
---

WFAT is developed by Ulrich Kuehn (ukuehn AT acm.org) and is released under
the GPL v2 or later.

