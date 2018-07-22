package com.testlims.zeroMQcore;

import static org.junit.Assert.assertEquals;

import java.io.PrintWriter;
import java.io.StringWriter;

public class MockHttpServletResponse {
	private PrintWriter		printWriter		= null;
	private StringWriter	stringWriter	= null;
	private String 			contentType		= null;
	private Integer 		httpStatusCode	= null;

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}
	
	public void assertEqualsContentType(String string2Check) {
		assertEquals( string2Check, contentType);
	}

	public void setStatus(Integer httpStatusCode) {
		this.httpStatusCode = httpStatusCode;
	}

	public void assertEqualsStatus(Integer code2Check) {
		assertEquals( code2Check, httpStatusCode);
	}
	
	public PrintWriter getWriter() {
		stringWriter = new StringWriter();
		printWriter	 = new PrintWriter( stringWriter);
		return printWriter;
	}
	
	public void assertEqualsResponse(String string2Check) {
		assertEquals( string2Check, stringWriter.toString().trim());
	}
}
