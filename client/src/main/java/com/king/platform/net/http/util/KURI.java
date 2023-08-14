package com.king.platform.net.http.util;

import java.net.URISyntaxException;

/**
 * This is a fork of java.net.URI which has been modified to support underscore in hostnames.
 */
public class KURI {
	private String url;

	private String scheme;
	private String fragment;
	private String userInfo;
	private String host;
	private int port = -1;
	private String path;
	private String query;

	private transient String authority;

	private volatile transient String schemeSpecificPart;


	public KURI(String uri) throws URISyntaxException {
		new Parser(uri).parse(false);
	}


	public String getUri() {
		return url;
	}

	public String getScheme() {
		return scheme;
	}

	public String getFragment() {
		return fragment;
	}

	public String getUserInfo() {
		return userInfo;
	}

	public String getHost() {
		return host;
	}

	public int getPort() {
		return port;
	}

	public String getPath() {
		return path;
	}

	public String getQuery() {
		return query;
	}


	// -- Character classes for parsing --

	// RFC2396 precisely specifies which characters in the US-ASCII charset are
	// permissible in the various components of a URI reference.  We here
	// define a set of mask pairs to aid in enforcing these restrictions.  Each
	// mask pair consists of two longs, a low mask and a high mask.  Taken
	// together they represent a 128-bit mask, where bit i is set iff the
	// character with value i is permitted.
	//
	// This approach is more efficient than sequentially searching arrays of
	// permitted characters.  It could be made still more efficient by
	// precompiling the mask information so that a character's presence in a
	// given mask could be determined by a single table lookup.

	// Compute the low-order mask for the characters in the given string
	private static long lowMask(String chars) {
		int n = chars.length();
		long m = 0;
		for (int i = 0; i < n; i++) {
			char c = chars.charAt(i);
			if (c < 64)
				m |= (1L << c);
		}
		return m;
	}

	// Compute the high-order mask for the characters in the given string
	private static long highMask(String chars) {
		int n = chars.length();
		long m = 0;
		for (int i = 0; i < n; i++) {
			char c = chars.charAt(i);
			if ((c >= 64) && (c < 128))
				m |= (1L << (c - 64));
		}
		return m;
	}

	// Compute a low-order mask for the characters
	// between first and last, inclusive
	private static long lowMask(char first, char last) {
		long m = 0;
		int f = Math.max(Math.min(first, 63), 0);
		int l = Math.max(Math.min(last, 63), 0);
		for (int i = f; i <= l; i++)
			m |= 1L << i;
		return m;
	}

	// Compute a high-order mask for the characters
	// between first and last, inclusive
	private static long highMask(char first, char last) {
		long m = 0;
		int f = Math.max(Math.min(first, 127), 64) - 64;
		int l = Math.max(Math.min(last, 127), 64) - 64;
		for (int i = f; i <= l; i++)
			m |= 1L << i;
		return m;
	}

	// Tell whether the given character is permitted by the given mask pair
	private static boolean match(char c, long lowMask, long highMask) {
		if (c == 0) // 0 doesn't have a slot in the mask. So, it never matches.
			return false;
		if (c < 64)
			return ((1L << c) & lowMask) != 0;
		if (c < 128)
			return ((1L << (c - 64)) & highMask) != 0;
		return false;
	}

	// Character-class masks, in reverse order from RFC2396 because
	// initializers for static fields cannot make forward references.

	// digit    = "0" | "1" | "2" | "3" | "4" | "5" | "6" | "7" |
	//            "8" | "9"
	private static final long L_DIGIT = lowMask('0', '9');
	private static final long H_DIGIT = 0L;

	// upalpha  = "A" | "B" | "C" | "D" | "E" | "F" | "G" | "H" | "I" |
	//            "J" | "K" | "L" | "M" | "N" | "O" | "P" | "Q" | "R" |
	//            "S" | "T" | "U" | "V" | "W" | "X" | "Y" | "Z"
	private static final long L_UPALPHA = 0L;
	private static final long H_UPALPHA = highMask('A', 'Z');

	// lowalpha = "a" | "b" | "c" | "d" | "e" | "f" | "g" | "h" | "i" |
	//            "j" | "k" | "l" | "m" | "n" | "o" | "p" | "q" | "r" |
	//            "s" | "t" | "u" | "v" | "w" | "x" | "y" | "z"
	private static final long L_LOWALPHA = 0L;
	private static final long H_LOWALPHA = highMask('a', 'z');

	// alpha         = lowalpha | upalpha
	private static final long L_ALPHA = L_LOWALPHA | L_UPALPHA;
	private static final long H_ALPHA = H_LOWALPHA | H_UPALPHA;

	// alphanum      = alpha | digit
	private static final long L_ALPHANUM = L_DIGIT | L_ALPHA;
	private static final long H_ALPHANUM = H_DIGIT | H_ALPHA;

	// hex           = digit | "A" | "B" | "C" | "D" | "E" | "F" |
	//                         "a" | "b" | "c" | "d" | "e" | "f"
	private static final long L_HEX = L_DIGIT;
	private static final long H_HEX = highMask('A', 'F') | highMask('a', 'f');

	// mark          = "-" | "_" | "." | "!" | "~" | "*" | "'" |
	//                 "(" | ")"
	private static final long L_MARK = lowMask("-_.!~*'()");
	private static final long H_MARK = highMask("-_.!~*'()");

	// unreserved    = alphanum | mark
	private static final long L_UNRESERVED = L_ALPHANUM | L_MARK;
	private static final long H_UNRESERVED = H_ALPHANUM | H_MARK;

	// reserved      = ";" | "/" | "?" | ":" | "@" | "&" | "=" | "+" |
	//                 "$" | "," | "[" | "]"
	// Added per RFC2732: "[", "]"
	private static final long L_RESERVED = lowMask(";/?:@&=+$,[]");
	private static final long H_RESERVED = highMask(";/?:@&=+$,[]");

	// The zero'th bit is used to indicate that escape pairs and non-US-ASCII
	// characters are allowed; this is handled by the scanEscape method below.
	private static final long L_ESCAPED = 1L;
	private static final long H_ESCAPED = 0L;

	// uric          = reserved | unreserved | escaped
	private static final long L_URIC = L_RESERVED | L_UNRESERVED | L_ESCAPED;
	private static final long H_URIC = H_RESERVED | H_UNRESERVED | H_ESCAPED;

	// pchar         = unreserved | escaped |
	//                 ":" | "@" | "&" | "=" | "+" | "$" | ","
	private static final long L_PCHAR
		= L_UNRESERVED | L_ESCAPED | lowMask(":@&=+$,");
	private static final long H_PCHAR
		= H_UNRESERVED | H_ESCAPED | highMask(":@&=+$,");

	// All valid path characters
	private static final long L_PATH = L_PCHAR | lowMask(";/");
	private static final long H_PATH = H_PCHAR | highMask(";/");

	// Dash, for use in domainlabel and toplabel
	private static final long L_DASH = lowMask("-");
	private static final long H_DASH = highMask("-");

	// Dot, for use in hostnames
	private static final long L_DOT = lowMask(".");
	private static final long H_DOT = highMask(".");

	// userinfo      = *( unreserved | escaped |
	//                    ";" | ":" | "&" | "=" | "+" | "$" | "," )
	private static final long L_USERINFO
		= L_UNRESERVED | L_ESCAPED | lowMask(";:&=+$,");
	private static final long H_USERINFO
		= H_UNRESERVED | H_ESCAPED | highMask(";:&=+$,");

	// reg_name      = 1*( unreserved | escaped | "$" | "," |
	//                     ";" | ":" | "@" | "&" | "=" | "+" )
	private static final long L_REG_NAME
		= L_UNRESERVED | L_ESCAPED | lowMask("$,;:@&=+");
	private static final long H_REG_NAME
		= H_UNRESERVED | H_ESCAPED | highMask("$,;:@&=+");

	// All valid characters for server-based authorities
	private static final long L_SERVER
		= L_USERINFO | L_ALPHANUM | L_DASH | lowMask(".:@[]");
	private static final long H_SERVER
		= H_USERINFO | H_ALPHANUM | H_DASH | highMask(".:@[]");

	// Special case of server authority that represents an IPv6 address
	// In this case, a % does not signify an escape sequence
	private static final long L_SERVER_PERCENT
		= L_SERVER | lowMask("%");
	private static final long H_SERVER_PERCENT
		= H_SERVER | highMask("%");
	private static final long L_LEFT_BRACKET = lowMask("[");
	private static final long H_LEFT_BRACKET = highMask("[");

	// scheme        = alpha *( alpha | digit | "+" | "-" | "." )
	private static final long L_SCHEME = L_ALPHA | L_DIGIT | lowMask("+-.");
	private static final long H_SCHEME = H_ALPHA | H_DIGIT | highMask("+-.");

	// uric_no_slash = unreserved | escaped | ";" | "?" | ":" | "@" |
	//                 "&" | "=" | "+" | "$" | ","
	private static final long L_URIC_NO_SLASH
		= L_UNRESERVED | L_ESCAPED | lowMask(";?:@&=+$,");
	private static final long H_URIC_NO_SLASH
		= H_UNRESERVED | H_ESCAPED | highMask(";?:@&=+$,");


	// -- Escaping and encoding --

	private final static char[] hexDigits = {
		'0', '1', '2', '3', '4', '5', '6', '7',
		'8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
	};


	private class Parser {


		private String input;           // URI input string
		private boolean requireServerAuthority = false;

		Parser(String s) {
			input = s;
			url = s;
		}

		// -- Methods for throwing URISyntaxException in various ways --

		private void fail(String reason) throws URISyntaxException {
			throw new URISyntaxException(input, reason);
		}

		private void fail(String reason, int p) throws URISyntaxException {
			throw new URISyntaxException(input, reason, p);
		}

		private void failExpecting(String expected, int p)
			throws URISyntaxException {
			fail("Expected " + expected, p);
		}

		private void failExpecting(String expected, String prior, int p)
			throws URISyntaxException {
			fail("Expected " + expected + " following " + prior, p);
		}


		// -- Simple access to the input string --

		// Return a substring of the input string
		//
		private String substring(int start, int end) {
			return input.substring(start, end);
		}

		// Return the char at position p,
		// assuming that p < input.length()
		//
		private char charAt(int p) {
			return input.charAt(p);
		}

		// Tells whether start < end and, if so, whether charAt(start) == c
		//
		private boolean at(int start, int end, char c) {
			return (start < end) && (charAt(start) == c);
		}

		// Tells whether start + s.length() < end and, if so,
		// whether the chars at the start position match s exactly
		//
		private boolean at(int start, int end, String s) {
			int p = start;
			int sn = s.length();
			if (sn > end - p)
				return false;
			int i = 0;
			while (i < sn) {
				if (charAt(p++) != s.charAt(i)) {
					break;
				}
				i++;
			}
			return (i == sn);
		}


		// -- Scanning --

		// The various scan and parse methods that follow use a uniform
		// convention of taking the current start position and end index as
		// their first two arguments.  The start is inclusive while the end is
		// exclusive, just as in the String class, i.e., a start/end pair
		// denotes the left-open interval [start, end) of the input string.
		//
		// These methods never proceed past the end position.  They may return
		// -1 to indicate outright failure, but more often they simply return
		// the position of the first char after the last char scanned.  Thus
		// a typical idiom is
		//
		//     int p = start;
		//     int q = scan(p, end, ...);
		//     if (q > p)
		//         // We scanned something
		//         ...;
		//     else if (q == p)
		//         // We scanned nothing
		//         ...;
		//     else if (q == -1)
		//         // Something went wrong
		//         ...;


		// Scan a specific char: If the char at the given start position is
		// equal to c, return the index of the next char; otherwise, return the
		// start position.
		//
		private int scan(int start, int end, char c) {
			if ((start < end) && (charAt(start) == c))
				return start + 1;
			return start;
		}

		// Scan forward from the given start position.  Stop at the first char
		// in the err string (in which case -1 is returned), or the first char
		// in the stop string (in which case the index of the preceding char is
		// returned), or the end of the input string (in which case the length
		// of the input string is returned).  May return the start position if
		// nothing matches.
		//
		private int scan(int start, int end, String err, String stop) {
			int p = start;
			while (p < end) {
				char c = charAt(p);
				if (err.indexOf(c) >= 0)
					return -1;
				if (stop.indexOf(c) >= 0)
					break;
				p++;
			}
			return p;
		}

		// Scan a potential escape sequence, starting at the given position,
		// with the given first char (i.e., charAt(start) == c).
		//
		// This method assumes that if escapes are allowed then visible
		// non-US-ASCII chars are also allowed.
		//
		private int scanEscape(int start, int n, char first)
			throws URISyntaxException {
			int p = start;
			char c = first;
			if (c == '%') {
				// Process escape pair
				if ((p + 3 <= n)
					&& match(charAt(p + 1), L_HEX, H_HEX)
					&& match(charAt(p + 2), L_HEX, H_HEX)) {
					return p + 3;
				}
				fail("Malformed escape pair", p);
			} else if ((c > 128)
				&& !Character.isSpaceChar(c)
				&& !Character.isISOControl(c)) {
				// Allow unescaped but visible non-US-ASCII chars
				return p + 1;
			}
			return p;
		}

		// Scan chars that match the given mask pair
		//
		private int scan(int start, int n, long lowMask, long highMask)
			throws URISyntaxException {
			int p = start;
			while (p < n) {
				char c = charAt(p);
				if (match(c, lowMask, highMask)) {
					p++;
					continue;
				}
				if ((lowMask & L_ESCAPED) != 0) {
					int q = scanEscape(p, n, c);
					if (q > p) {
						p = q;
						continue;
					}
				}
				break;
			}
			return p;
		}

		// Check that each of the chars in [start, end) matches the given mask
		//
		private void checkChars(int start, int end,
								long lowMask, long highMask,
								String what)
			throws URISyntaxException {
			int p = scan(start, end, lowMask, highMask);
			if (p < end)
				fail("Illegal character in " + what, p);
		}

		// Check that the char at position p matches the given mask
		//
		private void checkChar(int p,
							   long lowMask, long highMask,
							   String what)
			throws URISyntaxException {
			checkChars(p, p + 1, lowMask, highMask, what);
		}


		// -- Parsing --

		// [<scheme>:]<scheme-specific-part>[#<fragment>]
		//
		void parse(boolean rsa) throws URISyntaxException {
			requireServerAuthority = rsa;
			int ssp;                    // Start of scheme-specific part
			int n = input.length();
			int p = scan(0, n, "/?#", ":");
			if ((p >= 0) && at(p, n, ':')) {
				if (p == 0)
					failExpecting("scheme name", 0);
				checkChar(0, L_ALPHA, H_ALPHA, "scheme name");
				checkChars(1, p, L_SCHEME, H_SCHEME, "scheme name");
				scheme = substring(0, p);
				p++;                    // Skip ':'
				ssp = p;
				if (at(p, n, '/')) {
					p = parseHierarchical(p, n);
				} else {
					int q = scan(p, n, "", "#");
					if (q <= p)
						failExpecting("scheme-specific part", p);
					checkChars(p, q, L_URIC, H_URIC, "opaque part");
					p = q;
				}
			} else {
				ssp = 0;
				p = parseHierarchical(0, n);
			}
			schemeSpecificPart = substring(ssp, p);
			if (at(p, n, '#')) {
				checkChars(p + 1, n, L_URIC, H_URIC, "fragment");
				fragment = substring(p + 1, n);
				p = n;
			}
			if (p < n)
				fail("end of URI", p);
		}

		// [//authority]<path>[?<query>]
		//
		// DEVIATION from RFC2396: We allow an empty authority component as
		// long as it's followed by a non-empty path, query component, or
		// fragment component.  This is so that URIs such as "file:///foo/bar"
		// will parse.  This seems to be the intent of RFC2396, though the
		// grammar does not permit it.  If the authority is empty then the
		// userInfo, host, and port components are undefined.
		//
		// DEVIATION from RFC2396: We allow empty relative paths.  This seems
		// to be the intent of RFC2396, but the grammar does not permit it.
		// The primary consequence of this deviation is that "#f" parses as a
		// relative URI with an empty path.
		//
		private int parseHierarchical(int start, int n)
			throws URISyntaxException {
			int p = start;
			if (at(p, n, '/') && at(p + 1, n, '/')) {
				p += 2;
				int q = scan(p, n, "", "/?#");
				if (q > p) {
					p = parseAuthority(p, q);
				} else if (q < n) {
					// DEVIATION: Allow empty authority prior to non-empty
					// path, query component or fragment identifier
				} else
					failExpecting("authority", p);
			}
			int q = scan(p, n, "", "?#"); // DEVIATION: May be empty
			checkChars(p, q, L_PATH, H_PATH, "path");
			path = substring(p, q);
			p = q;
			if (at(p, n, '?')) {
				p++;
				q = scan(p, n, "", "#");
				checkChars(p, q, L_URIC, H_URIC, "query");
				query = substring(p, q);
				p = q;
			}
			return p;
		}

		// authority     = server | reg_name
		//
		// Ambiguity: An authority that is a registry name rather than a server
		// might have a prefix that parses as a server.  We use the fact that
		// the authority component is always followed by '/' or the end of the
		// input string to resolve this: If the complete authority did not
		// parse as a server then we try to parse it as a registry name.
		//
		private int parseAuthority(int start, int n)
			throws URISyntaxException {
			int p = start;
			int q = p;
			URISyntaxException ex = null;

			boolean serverChars;
			boolean regChars;

			if (scan(p, n, "", "]") > p) {
				// contains a literal IPv6 address, therefore % is allowed
				serverChars = (scan(p, n, L_SERVER_PERCENT, H_SERVER_PERCENT) == n);
			} else {
				serverChars = (scan(p, n, L_SERVER, H_SERVER) == n);
			}
			regChars = (scan(p, n, L_REG_NAME, H_REG_NAME) == n);

			if (regChars && !serverChars) {
				// Must be a registry-based authority
				authority = substring(p, n);
				return n;
			}

			if (serverChars) {
				// Might be (probably is) a server-based authority, so attempt
				// to parse it as such.  If the attempt fails, try to treat it
				// as a registry-based authority.
				try {
					q = parseServer(p, n);
					if (q < n)
						failExpecting("end of authority", q);
					authority = substring(p, n);
				} catch (URISyntaxException x) {
					// Undo results of failed parse
					userInfo = null;
					host = null;
					port = -1;
					if (requireServerAuthority) {
						// If we're insisting upon a server-based authority,
						// then just re-throw the exception
						throw x;
					} else {
						// Save the exception in case it doesn't parse as a
						// registry either
						ex = x;
						q = p;
					}
				}
			}

			if (q < n) {
				if (regChars) {
					// Registry-based authority
					authority = substring(p, n);
				} else if (ex != null) {
					// Re-throw exception; it was probably due to
					// a malformed IPv6 address
					throw ex;
				} else {
					fail("Illegal character in authority", q);
				}
			}

			return n;
		}


		// [<userinfo>@]<host>[:<port>]
		//
		private int parseServer(int start, int n)
			throws URISyntaxException {
			int p = start;
			int q;

			// userinfo
			q = scan(p, n, "/?#", "@");
			if ((q >= p) && at(q, n, '@')) {
				checkChars(p, q, L_USERINFO, H_USERINFO, "user info");
				userInfo = substring(p, q);
				p = q + 1;              // Skip '@'
			}

			// hostname, IPv4 address, or IPv6 address
			if (at(p, n, '[')) {
				// DEVIATION from RFC2396: Support IPv6 addresses, per RFC2732
				p++;
				q = scan(p, n, "/?#", "]");
				if ((q > p) && at(q, n, ']')) {
					// look for a "%" scope id
					int r = scan(p, q, "", "%");
					if (r > p) {
						parseIPv6Reference(p, r);
						if (r + 1 == q) {
							fail("scope id expected");
						}
						checkChars(r + 1, q, L_ALPHANUM, H_ALPHANUM,
							"scope id");
					} else {
						parseIPv6Reference(p, q);
					}
					host = substring(p - 1, q + 1);
					p = q + 1;
				} else {
					failExpecting("closing bracket for IPv6 address", q);
				}
			} else {
				q = parseIPv4Address(p, n);
				if (q <= p)
					q = parseHostname(p, n);
				p = q;
			}

			// port
			if (at(p, n, ':')) {
				p++;
				q = scan(p, n, "", "/");
				if (q > p) {
					checkChars(p, q, L_DIGIT, H_DIGIT, "port number");
					try {
						port = Integer.parseInt(substring(p, q));
					} catch (NumberFormatException x) {
						fail("Malformed port number", p);
					}
					p = q;
				}
			}
			if (p < n)
				failExpecting("port number", p);

			return p;
		}

		// Scan a string of decimal digits whose value fits in a byte
		//
		private int scanByte(int start, int n)
			throws URISyntaxException {
			int p = start;
			int q = scan(p, n, L_DIGIT, H_DIGIT);
			if (q <= p) return q;
			if (Integer.parseInt(substring(p, q)) > 255) return p;
			return q;
		}

		// Scan an IPv4 address.
		//
		// If the strict argument is true then we require that the given
		// interval contain nothing besides an IPv4 address; if it is false
		// then we only require that it start with an IPv4 address.
		//
		// If the interval does not contain or start with (depending upon the
		// strict argument) a legal IPv4 address characters then we return -1
		// immediately; otherwise we insist that these characters parse as a
		// legal IPv4 address and throw an exception on failure.
		//
		// We assume that any string of decimal digits and dots must be an IPv4
		// address.  It won't parse as a hostname anyway, so making that
		// assumption here allows more meaningful exceptions to be thrown.
		//
		private int scanIPv4Address(int start, int n, boolean strict)
			throws URISyntaxException {
			int p = start;
			int q;
			int m = scan(p, n, L_DIGIT | L_DOT, H_DIGIT | H_DOT);
			if ((m <= p) || (strict && (m != n)))
				return -1;
			for (; ; ) {
				// Per RFC2732: At most three digits per byte
				// Further constraint: Each element fits in a byte
				if ((q = scanByte(p, m)) <= p) break;
				p = q;
				if ((q = scan(p, m, '.')) <= p) break;
				p = q;
				if ((q = scanByte(p, m)) <= p) break;
				p = q;
				if ((q = scan(p, m, '.')) <= p) break;
				p = q;
				if ((q = scanByte(p, m)) <= p) break;
				p = q;
				if ((q = scan(p, m, '.')) <= p) break;
				p = q;
				if ((q = scanByte(p, m)) <= p) break;
				p = q;
				if (q < m) break;
				return q;
			}
			fail("Malformed IPv4 address", q);
			return -1;
		}

		// Take an IPv4 address: Throw an exception if the given interval
		// contains anything except an IPv4 address
		//
		private int takeIPv4Address(int start, int n, String expected)
			throws URISyntaxException {
			int p = scanIPv4Address(start, n, true);
			if (p <= start)
				failExpecting(expected, start);
			return p;
		}

		// Attempt to parse an IPv4 address, returning -1 on failure but
		// allowing the given interval to contain [:<characters>] after
		// the IPv4 address.
		//
		private int parseIPv4Address(int start, int n) {
			int p;

			try {
				p = scanIPv4Address(start, n, false);
			} catch (URISyntaxException x) {
				return -1;
			} catch (NumberFormatException nfe) {
				return -1;
			}

			if (p > start && p < n) {
				// IPv4 address is followed by something - check that
				// it's a ":" as this is the only valid character to
				// follow an address.
				if (charAt(p) != ':') {
					p = -1;
				}
			}

			if (p > start)
				host = substring(start, p);

			return p;
		}

		// hostname      = domainlabel [ "." ] | 1*( domainlabel "." ) toplabel [ "." ]
		// domainlabel   = alphanum | alphanum *( alphanum | "-" ) alphanum
		// toplabel      = alpha | alpha *( alphanum | "-" ) alphanum
		//
		private int parseHostname(int start, int n)
			throws URISyntaxException {
			int p = start;
			int q;
			int l = -1;                 // Start of last parsed label

			do {
				// domainlabel = alphanum [ *( alphanum | "-" ) alphanum ]
				q = scan(p, n, L_ALPHANUM, H_ALPHANUM);
				if (q <= p)
					break;
				l = p;
				if (q > p) {
					p = q;
					q = scan(p, n, L_ALPHANUM | L_DASH, H_ALPHANUM | H_DASH);
					if (q > p) {
						if (charAt(q - 1) == '-')
							fail("Illegal character in hostname", q - 1);
						p = q;
					}
				}
				q = scan(p, n, '.');  //skip forward to next part
				q = scan(p, n, '_');  //skip forward to ignore underscore
				if (q <= p)
					break;
				p = q;
			} while (p < n);

			if ((p < n) && !at(p, n, ':'))
				fail("Illegal character in hostname", p);

			if (l < 0)
				failExpecting("hostname", start);

			// for a fully qualified hostname check that the rightmost
			// label starts with an alpha character.
			if (l > start && !match(charAt(l), L_ALPHA, H_ALPHA)) {
				fail("Illegal character in hostname", l);
			}

			host = substring(start, p);
			return p;
		}


		// IPv6 address parsing, from RFC2373: IPv6 Addressing Architecture
		//
		// Bug: The grammar in RFC2373 Appendix B does not allow addresses of
		// the form ::12.34.56.78, which are clearly shown in the examples
		// earlier in the document.  Here is the original grammar:
		//
		//   IPv6address = hexpart [ ":" IPv4address ]
		//   hexpart     = hexseq | hexseq "::" [ hexseq ] | "::" [ hexseq ]
		//   hexseq      = hex4 *( ":" hex4)
		//   hex4        = 1*4HEXDIG
		//
		// We therefore use the following revised grammar:
		//
		//   IPv6address = hexseq [ ":" IPv4address ]
		//                 | hexseq [ "::" [ hexpost ] ]
		//                 | "::" [ hexpost ]
		//   hexpost     = hexseq | hexseq ":" IPv4address | IPv4address
		//   hexseq      = hex4 *( ":" hex4)
		//   hex4        = 1*4HEXDIG
		//
		// This covers all and only the following cases:
		//
		//   hexseq
		//   hexseq : IPv4address
		//   hexseq ::
		//   hexseq :: hexseq
		//   hexseq :: hexseq : IPv4address
		//   hexseq :: IPv4address
		//   :: hexseq
		//   :: hexseq : IPv4address
		//   :: IPv4address
		//   ::
		//
		// Additionally we constrain the IPv6 address as follows :-
		//
		//  i.  IPv6 addresses without compressed zeros should contain
		//      exactly 16 bytes.
		//
		//  ii. IPv6 addresses with compressed zeros should contain
		//      less than 16 bytes.

		private int ipv6byteCount = 0;

		private int parseIPv6Reference(int start, int n)
			throws URISyntaxException {
			int p = start;
			int q;
			boolean compressedZeros = false;

			q = scanHexSeq(p, n);

			if (q > p) {
				p = q;
				if (at(p, n, "::")) {
					compressedZeros = true;
					p = scanHexPost(p + 2, n);
				} else if (at(p, n, ':')) {
					p = takeIPv4Address(p + 1, n, "IPv4 address");
					ipv6byteCount += 4;
				}
			} else if (at(p, n, "::")) {
				compressedZeros = true;
				p = scanHexPost(p + 2, n);
			}
			if (p < n)
				fail("Malformed IPv6 address", start);
			if (ipv6byteCount > 16)
				fail("IPv6 address too long", start);
			if (!compressedZeros && ipv6byteCount < 16)
				fail("IPv6 address too short", start);
			if (compressedZeros && ipv6byteCount == 16)
				fail("Malformed IPv6 address", start);

			return p;
		}

		private int scanHexPost(int start, int n)
			throws URISyntaxException {
			int p = start;
			int q;

			if (p == n)
				return p;

			q = scanHexSeq(p, n);
			if (q > p) {
				p = q;
				if (at(p, n, ':')) {
					p++;
					p = takeIPv4Address(p, n, "hex digits or IPv4 address");
					ipv6byteCount += 4;
				}
			} else {
				p = takeIPv4Address(p, n, "hex digits or IPv4 address");
				ipv6byteCount += 4;
			}
			return p;
		}

		// Scan a hex sequence; return -1 if one could not be scanned
		//
		private int scanHexSeq(int start, int n)
			throws URISyntaxException {
			int p = start;
			int q;

			q = scan(p, n, L_HEX, H_HEX);
			if (q <= p)
				return -1;
			if (at(q, n, '.'))          // Beginning of IPv4 address
				return -1;
			if (q > p + 4)
				fail("IPv6 hexadecimal digit sequence too long", p);
			ipv6byteCount += 2;
			p = q;
			while (p < n) {
				if (!at(p, n, ':'))
					break;
				if (at(p + 1, n, ':'))
					break;              // "::"
				p++;
				q = scan(p, n, L_HEX, H_HEX);
				if (q <= p)
					failExpecting("digits for an IPv6 address", p);
				if (at(q, n, '.')) {    // Beginning of IPv4 address
					p--;
					break;
				}
				if (q > p + 4)
					fail("IPv6 hexadecimal digit sequence too long", p);
				ipv6byteCount += 2;
				p = q;
			}

			return p;
		}

	}
}
