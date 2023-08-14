package com.king.platform.net.http.util;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class KURITest {
	@ParameterizedTest
	@ValueSource(strings = {
		"http://a-b",
		"http://_host",
		"http://host",
		"http://host:8080",
		"http://host/",
		"http://host:8080/",
		"http://host/foo/bar",
		"http://host_name/foo/bar",
		"http://host_name:8081/foo/bar",
		"http://www.google.com/foo/bar",
		"http://_jabber._tcp.gmail.com/foo/bar",
		"http://__www__.some.wi___erd_name.fun_ky.c_o_m__/foo/bar",
		"http://host:8081/foo/bar?foo=bar&param=value",
		"http://host:8081/foo/bar?foo=bar&param=value#fragment1",
		"http://foo@host:8081/foo/bar?foo=bar&param=value#fragment1",
		"http://foo:bar@host:8081/foo/bar?foo=bar&param=value#fragment1",
		"http://foo:bar@host_name:8081/foo/bar?foo=bar&param=value#fragment1",
		"http://foo:bar@192.168.0.1:8081/foo/bar?foo=bar&param=value#fragment1",
		"http://foo:bar@[2001:4860:4860::8888]:8081/foo/bar?foo=bar&param=value#fragment1",

	})
	public void getRelativeAbsolutUri(String uri) throws Exception {

		KURI kUri = new KURI(uri);

		StringBuilder rebuiltUri = new StringBuilder();
		rebuiltUri.append(kUri.getScheme()).append("://");

		if (kUri.getUserInfo() != null) {
			rebuiltUri.append(kUri.getUserInfo()).append("@");
		}

		rebuiltUri.append(kUri.getHost());

		if (kUri.getPort() != -1) {
			rebuiltUri.append(":").append(kUri.getPort());
		}


		rebuiltUri.append(kUri.getPath());

		if (kUri.getQuery() != null) {
			rebuiltUri.append("?").append(kUri.getQuery());
		}

		if (kUri.getFragment() != null) {
			rebuiltUri.append("#").append(kUri.getFragment());
		}


		assertEquals(uri, rebuiltUri.toString());
	}
}
