package com.testlims.zeroMQcore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TreeMap;

import org.json.JSONObject;
import org.junit.*;

import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;

import com.testlims.utilities.StackTrace;

/**
 * Unit tests for Hello Service(s). 
 */
public class ServiceTests 
{
	static final String		dateFormat		= "yyyy-MM-dd'T'HH:mm:ss.SSS";
	static final DateFormat dateFormatter 	= new SimpleDateFormat( dateFormat);
	
	/**
	 * Create the test case
	 *
	 * @param testName name of the test case
	 *
	public LoggerTests( String testName) {
		super( testName);
	} */

	/**  @return the suite of tests being tested. 
	public static junit.framework.Test suite() {
		return new TestSuite( LoggerTests.class);
    } */
	
	/**
     * Test of HelloService1
     * <p>
     * Unlike most unit tests, this test only checks that no exception was thrown. 
     * The actual logging is going to System.out, and should look like: 
<pre>
helloService1Test: 2018-07-10T19:50:17.367 helloService started
HelloService waiting in run()
helloService1Test: sending requestId 0 requestString Tess
HelloService received request: Tess
HelloService response sent Hello Tess
HelloService waiting in run()
helloService1Test: sending requestId 1 requestString TERMINATE_HELLO_SERVICE
HelloService received request: TERMINATE_HELLO_SERVICE
HelloService received TERMINATE_HELLO_SERVICE
HelloService after while loop -&gt; Close service socket and terminate context. 
</pre>
     * Note that it takes about 20 milliseconds before the HelloService is ready to receive requests. 
	 */
	@Test
    public void helloService1Test() {
		final String SOCKET_URL	= "tcp://localhost:5557"; 
		
		try {
			// Start HelloService1 in a tread. 
			HelloService1 helloService = new HelloService1();
			helloService.start();
			System.out.println( "helloService1Test: " + dateFormatter.format( new Date()) + " helloService started");

			Context clientContext = ZMQ.context(1);
			ZMQ.Socket requestClient = clientContext.socket( ZMQ.REQ); 
			requestClient.connect( SOCKET_URL); 
			
			Thread.sleep(25);
			int requestId = 0;
			String requestString = "Tess";
			System.out.println( "helloService1Test: sending requestId " + requestId + " requestString " + requestString);
			requestClient.send( requestString.getBytes(), 0);
			String reply = requestClient.recvStr();
			assertEquals( "Hello " + requestString, reply);
			
			requestId++;
			System.out.println( "helloService1Test: sending requestId " + requestId + " requestString TERMINATE_HELLO_SERVICE");
			requestClient.send( ("TERMINATE_HELLO_SERVICE").getBytes(), 0);
			reply = requestClient.recvStr();
			assertEquals( "HelloService being terminated", reply);
			
	        Thread.sleep(10);
			requestClient.close();
			clientContext.close();
		}
		catch (Exception e) {
			fail( StackTrace.asString(e));
		}
    }
	
	/**
     * Test of HelloService
     * <p>
     * Note that it takes about 20 milliseconds before the HelloService is ready to receive requests. 
	 */
	@Test
    public void helloServiceTest() {
		final String LOG_FILE_URL	= "/var/log/zeroMQcore/project.log"; 
		final String LOGGER_TOPIC	= "Project_Log"; 
		final String LOGGER_URL		= "tcp://localhost:5556"; 
		final String SOCKET_URL		= "tcp://localhost:5557"; 
		
		Date startTime	= new Date();
		Date endTime	= null;
		
		try {
			// Start MessageLogger 
			MessageLogger messageLogger = new MessageLogger( LOGGER_URL, LOGGER_TOPIC, LOG_FILE_URL);
			messageLogger.start();
			Thread.sleep(25);
			
			// Start HelloService 
			HelloService helloService = new HelloService( SOCKET_URL, LOGGER_URL, LOGGER_TOPIC);
			helloService.start();

			Context clientContext = ZMQ.context(1);
			ZMQ.Socket requestClient = clientContext.socket( ZMQ.REQ); 
			requestClient.connect( SOCKET_URL); 

			Thread.sleep(25);
			String requestId 	= "helloServiceTest.1";
			String requestType	= "sendHTML";
			JSONObject requestJSON = new JSONObject();
			requestJSON.put( "requestId",	requestId);
			requestJSON.put( "requestType", requestType);
			requestClient.send( requestJSON.toString().getBytes(), 0);
			String reply = requestClient.recvStr();
			JSONObject responseJSON = new JSONObject( reply);
			assertEquals( requestId,		responseJSON.getString( "requestId"));
			assertEquals( requestType,		responseJSON.getString( "requestType"));
			StringBuilder html = new StringBuilder();
			html.append( "<form class=\"helloForm\">\n");
			html.append( "  Name: <input type=\"text\" name=\"name\" />\n");
			html.append( "  <br />\n");
			html.append( "  <input type=\"submit\" value=\"Submit\" />\n");
			html.append( "</form>");
			assertEquals( html.toString(),	responseJSON.getString( "html"));
			
			Thread.sleep(25);
			String requestId2 	= "helloServiceTest.2";
			String requestType2	= "sayHello";
			String name 		= "Tess";
			JSONObject request2JSON = new JSONObject();
			request2JSON.put( "requestId",	requestId2);
			request2JSON.put( "requestType", requestType2);
			request2JSON.put( "name", 		name);
			requestClient.send( request2JSON.toString().getBytes(), 0);
			String reply2 = requestClient.recvStr();
			JSONObject response2JSON = new JSONObject( reply2);
			assertEquals( requestId2,		response2JSON.getString( "requestId"));
			assertEquals( requestType2,		response2JSON.getString( "requestType"));
			assertEquals( "Hello " + name,	response2JSON.getString( "response"));
			
			requestClient.send( ("TERMINATE_HELLO_SERVICE").getBytes(), 0);
			reply = requestClient.recvStr();
			assertEquals( "HelloService being terminated", reply);
			
	        Thread.sleep(25);
			requestClient.close();
			clientContext.close();
			
			MessageLogger.terminate( LOGGER_URL, LOGGER_TOPIC);
		}
		catch (Exception e) {
			fail( StackTrace.asString(e));
		}
		endTime	= new Date();
		
		// ____________________ Check Log File ____________________ 
		TreeMap<Integer,String> logFileLines = LoggerTests.readLogFile( LOG_FILE_URL);
				
		boolean foundClosingLogger		= false; 
		boolean foundTerminateLogger	= false;
		boolean foundresponse2			= false;
		boolean foundreceived2			= false;
		boolean foundresponse1			= false;
		boolean foundreceived1			= false;
		boolean foundresponse0			= false;
		boolean foundreceived0			= false;
		boolean foundLogFileOpenned		= false;
		
		// Read the log file backwards. 
		for (Integer lineNumber : logFileLines.descendingKeySet()) {
			String line = logFileLines.get( lineNumber);
		
			try { 
				String timestampString = line.substring( 0, 23);
				Date timestamp = dateFormatter.parse( timestampString);
				
				if (timestamp.before( startTime)) { 
					break; 
				}
				else if (timestamp.before( endTime)) {
					
					if (line.contains( "MessageLogging closing logger socket and terminating context.")) {
						foundClosingLogger = true;
					}
					else if (line.contains( "MessageLogging received TERMINATE_LOGGER")) {
						foundTerminateLogger = true;
					}
					else if (line.contains( "HelloService closing service and logger sockets and terminate context.")) {
						foundresponse2 = true;
					}
					else if (line.contains( "HelloService request: TERMINATE_HELLO_SERVICE")) {
						foundreceived2 = true;
					}
					else if (line.contains( "HelloService:helloServiceTest.2:sayHello.request:Tess")) {
						foundresponse1 = true;
					}
					else if (line.contains( "HelloService:helloServiceTest.2:sayHello.response:Hello Tess")) {
						foundreceived1 = true;
					}
					else if (line.contains( "HelloService:helloServiceTest.1:sendHTML.request")) {
						foundresponse0 = true;
					}
					else if (line.contains( "HelloService:helloServiceTest.1:sendHTML.response")) {
						foundreceived0 = true;
					}
					else if (line.contains( "MessageLogging Log file /var/log/zeroMQcore/project.log opened.")) {
						foundLogFileOpenned = true;
					}
					else {	
						fail( "unexpected line: " + line);
					}
				}
			}
			catch (Exception e) {
			fail( "Unable to parse timestamp on line " + lineNumber + " line: " + line + "/n" + StackTrace.asString(e));
			}
		}
		
		assertTrue( foundClosingLogger);
		assertTrue( foundTerminateLogger);
		assertTrue( foundresponse2);
		assertTrue( foundreceived2);
		assertTrue( foundresponse1);
		assertTrue( foundreceived1);
		assertTrue( foundresponse0);
		assertTrue( foundreceived0);
		assertTrue( foundLogFileOpenned);
    }
	
	/**
     * Test of HelloService using Mock HTTP request
     * <p>
     * Note that it takes about 20 milliseconds before the HelloService is ready to receive requests. 
	 * 
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	@Test
    public void mockHelloServiceTest() throws InterruptedException, IOException {
		final String LOG_FILE_URL	= "/var/log/zeroMQcore/project.log"; 
		final String LOGGER_TOPIC	= "Project_Log"; 
		final String LOGGER_URL		= "tcp://localhost:5556"; 
		final String SOCKET_URL		= "tcp://localhost:5557"; 
		
		Date startTime	= new Date();
		Date endTime	= null;
		
		// Start MessageLogger 
		MessageLogger messageLogger = new MessageLogger( LOGGER_URL, LOGGER_TOPIC, LOG_FILE_URL);
		messageLogger.start();
		Thread.sleep(25);
		
		// Start HelloService 
		HelloService helloService = new HelloService( SOCKET_URL, LOGGER_URL, LOGGER_TOPIC);
		helloService.start();
		Thread.sleep(25);
		
		// Start MockHTTPzeroMQ
		MockHTTPzeroMQ mockHTTPzeroMQ = new MockHTTPzeroMQ( SOCKET_URL, LOGGER_URL, LOGGER_TOPIC);
		Thread.sleep(25);
		
		// Send a HTTP request and response to the MockHTTPzeroMQ 
		String requestType	= "sendHTML";
		JSONObject requestJSON = new JSONObject();
		requestJSON.put( "requestType", requestType);
		MockHttpServletRequest mockRequest = new MockHttpServletRequest( requestJSON.toString());
		MockHttpServletResponse mockResponse = new MockHttpServletResponse();
		mockHTTPzeroMQ.doPost( mockRequest, mockResponse);
		
		// Send a second HTTP request and response to the MockHTTPzeroMQ 
		String requestType1	= "sayHello";
		String name 		= "Tess";
		JSONObject request1JSON = new JSONObject();
		request1JSON.put( "requestType", requestType1);
		request1JSON.put( "name", 		name);
		MockHttpServletRequest mockRequest1 = new MockHttpServletRequest( request1JSON.toString());
		MockHttpServletResponse mockResponse1 = new MockHttpServletResponse();
		mockHTTPzeroMQ.doPost( mockRequest1, mockResponse1);
		Thread.sleep(2);
		
		// Send a HTTP request and response to terminate Hello Service
		String request = "TERMINATE_HELLO_SERVICE";
		MockHttpServletRequest mockRequest2 = new MockHttpServletRequest( request);
		MockHttpServletResponse mockResponse2 = new MockHttpServletResponse();
		mockHTTPzeroMQ.doPost( mockRequest2, mockResponse2);
		
		Thread.sleep(10);
		mockHTTPzeroMQ.closeAndTerminate();

		Thread.sleep(10);
		MessageLogger.terminate( LOGGER_URL, LOGGER_TOPIC);
		
		// ____________________ Check Results _____________________ 
		Thread.sleep(10);
		endTime = new Date();
		mockResponse.assertEqualsContentType( "application/json; charset=utf-8");
		mockResponse.assertEqualsStatus( 200);
		mockResponse.assertEqualsResponse( "{\"requestType\":\"sendHTML\",\"requestId\":\"1\"," +
				"\"html\":\"<form class=\\\"helloForm\\\">\\n  Name: <input type=\\\"text\\\" name=\\\"name\\\" />\\n  <br />\\n  <input type=\\\"submit\\\" value=\\\"Submit\\\" />\\n<\\/form>\"}");

		mockResponse1.assertEqualsContentType( "application/json; charset=utf-8");
		mockResponse1.assertEqualsStatus( 200);
		mockResponse1.assertEqualsResponse( "{\"requestType\":\"sayHello\",\"requestId\":\"2\",\"response\":\"Hello Tess\"}");
		
		mockResponse2.assertEqualsContentType( "application/text; charset=utf-8");
		mockResponse2.assertEqualsStatus( 200);
		mockResponse2.assertEqualsResponse( "HelloService being terminated");
		
		// ____________________ Check Log File ____________________ 
		TreeMap<Integer,String> logFileLines = LoggerTests.readLogFile( LOG_FILE_URL);
		
		boolean foundClosingLogger		= false; 
		boolean foundTerminateLogger	= false;
		boolean foundClosingMock		= false;
		boolean foundMockResp2			= false;
		boolean foundresponse2			= false;
		boolean foundreceived2			= false;
		boolean foundMock2				= false;
		boolean foundMockResp1			= false;
		boolean foundresponse1			= false;
		boolean foundreceived1			= false;
		boolean foundMock1				= false;
		boolean foundMockResp0			= false;
		boolean foundresponse0			= false;
		boolean foundreceived0			= false;
		boolean foundMockRequest0		= false;
		boolean foundLogFileOpenned		= false;
		
		// Read the log file backwards. 
		for (Integer lineNumber : logFileLines.descendingKeySet()) {
			String line = logFileLines.get( lineNumber);
		
			try { 
				String timestampString = line.substring( 0, 23);
				Date timestamp = dateFormatter.parse( timestampString);
				
				if (timestamp.before( startTime)) { 
					break; 
				}
				else if (timestamp.before( endTime)) {
					
					if (line.contains( "MessageLogging closing logger socket and terminating context.")) {
						foundClosingLogger = true;
					}
					else if (line.contains( "MessageLogging received TERMINATE_LOGGER")) {
						foundTerminateLogger = true;
					}
					else if (line.contains( "MockHTTPzeroMQ request: close and terminate")) {
						foundClosingMock = true;
					}
					else if (line.contains( "MockHTTPzeroMQ doPost:3:TERMINATE_HELLO_SERVICE.response")) {
						foundMockResp2 = true;
					}
					else if (line.contains( "HelloService closing service and logger sockets and terminate context.")) {
						foundresponse2 = true;
					}
					else if (line.contains( "HelloService request: TERMINATE_HELLO_SERVICE")) {
						foundreceived2 = true;
					}
					else if (line.contains( "MockHTTPzeroMQ doPost:3:TERMINATE_HELLO_SERVICE.request")) {
						foundMock2 = true;
					}
					else if (line.contains( "MockHTTPzeroMQ doPost:2:sayHello.response")) {
						foundMockResp1 = true;
					}
					else if (line.contains( "HelloService:2:sayHello.request:Tess")) {
						foundresponse1 = true;
					}
					else if (line.contains( "HelloService:2:sayHello.response:Hello Tess")) {
						foundreceived1 = true;
					}
					else if (line.contains( "MockHTTPzeroMQ doPost:2:sayHello.request")) {
						foundMock1 = true;
					}
					else if (line.contains( "MockHTTPzeroMQ doPost:1:sendHTML.response")) {
						foundMockResp0 = true;
					}
					else if (line.contains( "HelloService:1:sendHTML.request")) {
						foundresponse0 = true;
					}
					else if (line.contains( "HelloService:1:sendHTML.response")) {
						foundreceived0 = true;
					}
					else if (line.contains( "MockHTTPzeroMQ doPost:1:sendHTML.request")) {
						foundMockRequest0 = true;
					}
					else if (line.contains( "MessageLogging Log file /var/log/zeroMQcore/project.log opened.")) {
						foundLogFileOpenned = true;
					}
					else {	
						fail( "unexpected line: " + line);
					}
				}
			}
			catch (Exception e) {
				fail( "Unable to parse timestamp on line " + lineNumber + " line: " + line + "/n" + StackTrace.asString(e));
			}
		}	
		
		assertTrue( foundClosingLogger);
		assertTrue( foundTerminateLogger);
		assertTrue( foundClosingMock);
		assertTrue( foundMockResp2);
		assertTrue( foundresponse2);
		assertTrue( foundreceived2);
		assertTrue( foundMock2);
		assertTrue( foundMockResp1);
		assertTrue( foundresponse1);
		assertTrue( foundreceived1);
		assertTrue( foundMock1);
		assertTrue( foundMockResp0);
		assertTrue( foundresponse0);
		assertTrue( foundreceived0);
		assertTrue( foundMockRequest0);
		assertTrue( foundLogFileOpenned);
	}	

}
