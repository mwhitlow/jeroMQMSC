package com.testlims.zeroMQcore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
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
	static final String 	LOG_FILE_URL		= "/var/log/zeroMQcore/project.log"; 
	static final String 	LOGGER_TOPIC		= "Project_Log"; 
	static final String 	LOGGER_URL			= "tcp://localhost:5556"; 
	static final String		dateFormat			= "yyyy-MM-dd'T'HH:mm:ss.SSS";
	static final String		fileDateFormat		= "yyyy-MM-dd'T'HH.mm.ss.SSS";
	static final DateFormat dateFormatter 		= new SimpleDateFormat( dateFormat);
	static final DateFormat fileDateFormatter 	= new SimpleDateFormat( fileDateFormat);
	
	Date startTime = null;
	
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
	
	@BeforeClass
	public static void removeOldLogFiles() {	
		File logDirectory = new File( "C:/var/log/zeroMQcore");
		File[] logFileList = logDirectory.listFiles();
		
		for (int f=0; f<logFileList.length; f++)
		{	File logFile = logFileList[f];
			
			if (!logFile.delete()) {
				System.out.println( "LoggerTests.removeOldLogFiles: " + dateFormatter.format( new Date()) + 
					" FAILED TO DELETE " + logFile.getName());
			}
		}
	}
	
	@Before
	public void startServices() throws InterruptedException {
		startTime = new Date();
		
		// Start MessageLogger 
		MessageLogger messageLogger = new MessageLogger( LOGGER_URL, LOGGER_TOPIC, LOG_FILE_URL);
		messageLogger.start();
		Thread.sleep(25);
	}
	
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
     * Test of HelloService's sendHTML method. 
     * <p>
     * Note that it takes about 20 milliseconds before the HelloService is ready to receive requests. 
	 */
	@Test
    public void helloServiceShouldReturnHTML() {
		final String SOCKET_URL	= "tcp://localhost:5557"; 
		
		try {
			// Start HelloService 
			HelloService helloService = new HelloService( SOCKET_URL, LOGGER_URL, LOGGER_TOPIC);
			helloService.start();
			Thread.sleep(25);

			// Start MockHTTPzeroMQ
			MockHTTPzeroMQ mockHTTPzeroMQ = new MockHTTPzeroMQ( SOCKET_URL, LOGGER_URL, LOGGER_TOPIC);
			Thread.sleep(25);
			
			String requestId 	= "1";
			String requestType	= "sendHTML";
			JSONObject requestJSON = new JSONObject();
			requestJSON.put( "requestId",	requestId);
			requestJSON.put( "requestType", requestType);
			MockHttpServletRequest mockRequest = new MockHttpServletRequest( requestJSON.toString());
			MockHttpServletResponse mockResponse = new MockHttpServletResponse();
			mockHTTPzeroMQ.doPost( mockRequest, mockResponse);
			Thread.sleep(2);

			// Send a HTTP request and response to terminate Hello Service
			String request = "TERMINATE_HELLO_SERVICE";
			MockHttpServletRequest mockRequest2 = new MockHttpServletRequest( request);
			MockHttpServletResponse mockResponse2 = new MockHttpServletResponse();
			mockHTTPzeroMQ.doPost( mockRequest2, mockResponse2);

			mockHTTPzeroMQ.closeAndTerminate();
			
			// ____________________ Check Results _____________________ 
			mockResponse.assertEqualsContentType( "application/json; charset=utf-8");
			mockResponse.assertEqualsStatus( 200);
			mockResponse.assertEqualsResponse( "{\"requestType\":\"sendHTML\",\"requestId\":\"1\",\"html\":\"<form class=\\\"helloForm\\\">\\n  Name: <input type=\\\"text\\\" name=\\\"name\\\" />\\n  <br />\\n  <input type=\\\"submit\\\" value=\\\"Submit\\\" />\\n<\\/form>\"}");
			
			mockResponse2.assertEqualsContentType( "application/text; charset=utf-8");
			mockResponse2.assertEqualsStatus( 200);
			mockResponse2.assertEqualsResponse( "HelloService being terminated");
		}
		catch (Exception e) {
			fail( StackTrace.asString(e));
		}
    }
	
	/**
     * Test the sayHello request in HelloService using Mock HTTP request
	 * 
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	@Test
    public void helloServiceShouldReturnHelloName() throws InterruptedException, IOException {
		final String SOCKET_URL	= "tcp://localhost:5557"; 
		
		// Start HelloService 
		HelloService helloService = new HelloService( SOCKET_URL, LOGGER_URL, LOGGER_TOPIC);
		helloService.start();
		Thread.sleep(25);
		
		// Start MockHTTPzeroMQ
		MockHTTPzeroMQ mockHTTPzeroMQ = new MockHTTPzeroMQ( SOCKET_URL, LOGGER_URL, LOGGER_TOPIC);
		Thread.sleep(25);
		
		// Send a HTTP request and response to the MockHTTPzeroMQ 
		String requestType	= "sayHello";
		String name 		= "Tess";
		JSONObject requestJSON = new JSONObject();
		requestJSON.put( "requestType", requestType);
		requestJSON.put( "name", 		name);
		MockHttpServletRequest mockRequest = new MockHttpServletRequest( requestJSON.toString());
		MockHttpServletResponse mockResponse = new MockHttpServletResponse();
		mockHTTPzeroMQ.doPost( mockRequest, mockResponse);
		Thread.sleep(2);
		
		// Send a HTTP request and response to terminate Hello Service
		String request = "TERMINATE_HELLO_SERVICE";
		MockHttpServletRequest mockRequest2 = new MockHttpServletRequest( request);
		MockHttpServletResponse mockResponse2 = new MockHttpServletResponse();
		mockHTTPzeroMQ.doPost( mockRequest2, mockResponse2);
		
		mockHTTPzeroMQ.closeAndTerminate();
		
		// ____________________ Check Results _____________________ 
		mockResponse.assertEqualsContentType( "application/json; charset=utf-8");
		mockResponse.assertEqualsStatus( 200);
		mockResponse.assertEqualsResponse( "{\"requestType\":\"sayHello\",\"requestId\":\"1\",\"response\":\"Hello Tess\"}");
		
		mockResponse2.assertEqualsContentType( "application/text; charset=utf-8");
		mockResponse2.assertEqualsStatus( 200);
		mockResponse2.assertEqualsResponse( "HelloService being terminated");
	}
	
	/**
     * Check the logging of HelloService using Mock HTTP request
     * <p>
     * Note that it takes about 20 milliseconds before the HelloService is ready to receive requests. 
	 * 
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
	@Test
    public void helloServiceCheckLog() throws InterruptedException, IOException {
		final String SOCKET_URL	= "tcp://localhost:5557"; 
		
		Date endTime = null;
		
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
		
		// Send a HTTP request and response to terminate Hello Service
		Thread.sleep(2);
		String request2 = "TERMINATE_HELLO_SERVICE";
		MockHttpServletRequest mockRequest2 = new MockHttpServletRequest( request2);
		MockHttpServletResponse mockResponse2 = new MockHttpServletResponse();
		mockHTTPzeroMQ.doPost( mockRequest2, mockResponse2);
		
		mockHTTPzeroMQ.closeAndTerminate();
		
		// ____________________ Check Log File ____________________ 
		Thread.sleep(10);
		endTime = new Date();
		
		TreeMap<Integer,String> logFileLines = LoggerTests.readLogFile( LOG_FILE_URL);
		
		// Read the log file backwards. 
		for (Integer lineNumber : logFileLines.keySet()) {
			String line = logFileLines.get( lineNumber);
		
			try { 
				String timestampString = line.substring( 0, 23);
				Date timestamp = dateFormatter.parse( timestampString);
				assertTrue( timestamp.equals( startTime) || timestamp.after( startTime));
				assertTrue( timestamp.before( endTime));
			}
			catch (Exception e) {
				fail( "Unable to parse timestamp on line " + lineNumber + " line: " + line);
			}
			
			if (lineNumber == 0) { 
				assertTrue( line.contains( "MessageLogging Log file /var/log/zeroMQcore/project.log opened."));
			}
			else if (lineNumber == 1) { 
				assertTrue( line.contains( "MockHTTPzeroMQ doPost:1:sendHTML.request")); 
			}
			else if (lineNumber == 2) { 
				assertTrue( line.contains( "HelloService:1:sendHTML.request")); 
			}
			else if (lineNumber == 3) { 
				assertTrue( line.contains( "HelloService:1:sendHTML.response")); 
			}
			else if (lineNumber == 4) { 
				assertTrue( line.contains( "MockHTTPzeroMQ doPost:1:sendHTML.response")); 
			}
			else if (lineNumber == 5) { 
				assertTrue( line.contains( "MockHTTPzeroMQ doPost:2:sayHello.request")); 
			}
			else if (lineNumber == 6) { 
				assertTrue( line.contains( "HelloService:2:sayHello.request:Tess")); 
			}
			else if (lineNumber == 7) { 
				assertTrue( line.contains( "HelloService:2:sayHello.response:Hello Tess")); 
			}
			else if (lineNumber == 8) { 
				assertTrue( line.contains( "MockHTTPzeroMQ doPost:2:sayHello.response")); 
			}
			else if (lineNumber == 9) { 
				assertTrue( line.contains( "MockHTTPzeroMQ doPost:3:TERMINATE_HELLO_SERVICE.request")); 
			}
			else if (lineNumber == 10) { 
				assertTrue( line.contains( "HelloService request: TERMINATE_HELLO_SERVICE")); 
			}
			else if (lineNumber == 11) { 
				assertTrue( line.contains( "HelloService closing service and logger sockets and terminate context.")); 
			}
			else if (lineNumber == 12) { 
				assertTrue( line.contains( "MockHTTPzeroMQ doPost:3:TERMINATE_HELLO_SERVICE.response")); 
			}
			else if (lineNumber == 13) { 
				assertTrue( line.contains( "MockHTTPzeroMQ request: close and terminate")); 
			}
			else if (lineNumber == 14) { 
				assertTrue( line.contains( "MessageLogging received TERMINATE_LOGGER")); 
			}
			else if (lineNumber == 15) { 
				assertTrue( line.contains( "MessageLogging closing logger socket and terminating context.")); 
			}
			else {	
				fail( "unexpected line " + lineNumber + ": " + line);
			}
		}
	}

	
	@After
	public void moveLogFile() throws InterruptedException {	
		// Terminate message logger. 
		Context context = ZMQ.context(1);
		ZMQ.Socket pub2Logger = context.socket( ZMQ.PUB); 
		pub2Logger.connect( LOGGER_URL); 
		
		Thread.sleep( 20);
		pub2Logger.send( LOGGER_TOPIC + " " + "TERMINATE_LOGGER", 0);
		pub2Logger.close();
		context.close();
		
		File logFile = new File( "C:/var/log/zeroMQcore/project.log");
		
		if (logFile.exists())
		{	String timestamp = fileDateFormatter.format( new Date( logFile.lastModified()));
			File renamedLogFile = new File( "C:/var/log/zeroMQcore/project_" + timestamp + ".log");
			
			if (!logFile.renameTo( renamedLogFile)) {
				System.out.println( "LoggerTests.moveExistingLogFile: " + dateFormatter.format( new Date()) + 
						" FAILED TO MOVE project.log to " + renamedLogFile.getName());
			}
		}
	}
}
