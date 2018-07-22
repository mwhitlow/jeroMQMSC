package com.testlims.zeroMQcore;

import java.io.BufferedReader;
import java.io.StringReader;

public class MockHttpServletRequest {
	private String request = null;
	
	public MockHttpServletRequest(String request) {
		this.request = request;
	}
	
	public BufferedReader getReader() {
		BufferedReader reader = new BufferedReader( new StringReader(request));
		return reader;
	}
}
