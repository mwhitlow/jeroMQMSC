package com.testlims.zeroMQcore;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
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
 * Unit tests of HellowServices using MockHTTPzeroMQ 
 */
public class MockHTTPzeroMQTests 
{
	static final String 	LOG_FILE_URL		= "/var/log/zeroMQcore/project.log"; 
	static final String 	LOGGER_TOPIC		= "Project_Log"; 
	static final String 	LOGGER_URL			= "tcp://localhost:5556"; 
	static final String 	SOCKET_URL			= "tcp://localhost:5557"; 
	static final String		dateFormat			= "yyyy-MM-dd'T'HH:mm:ss.SSS";
	static final String		fileDateFormat		= "yyyy-MM-dd'T'HH.mm.ss.SSS";
	static final DateFormat dateFormatter 		= new SimpleDateFormat( dateFormat);
	static final DateFormat fileDateFormatter 	= new SimpleDateFormat( fileDateFormat);
	
	Date startTime = null;
		
	@BeforeClass
	public static void removeOldLogFiles() {	
		File logDirectory = new File( "C:/var/log/zeroMQcore");
		File[] logFileList = logDirectory.listFiles();
		
		for (int f=0; f<logFileList.length; f++)
		{	File logFile = logFileList[f];
			
			if (!logFile.delete()) {
				System.out.println( "MockHTTPzeroMQTests.removeOldLogFiles: " + dateFormatter.format( new Date()) + 
						" FAILED TO DELETE " + logFile.getName());
			}
		}
	}
	
	@Before
	public void archiveLogFile() throws InterruptedException {
		// Archive Log file
		Context context = ZMQ.context(1);
		ZMQ.Socket pub2Logger = context.socket( ZMQ.PUB); 
		pub2Logger.connect( LOGGER_URL); 
		
		Thread.sleep( 20);
		pub2Logger.send( LOGGER_TOPIC + " " + "ARCIVE_LOG_FILE", 0);
		pub2Logger.close();
		context.close();

		startTime = new Date();
	}
	
	
	/**
     * Test of HelloService's sendHTML method. 
     * <p>
     * Note that it takes about 20 milliseconds before the HelloService is ready to receive requests. 
	 */
	@Test
    public void helloServiceShouldReturnHTML() {
		
		try {
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
			
			// ____________________ Check Results _____________________ 
			mockResponse.assertEqualsContentType( "application/json; charset=utf-8");
			mockResponse.assertEqualsStatus( 200);
			mockResponse.assertEqualsResponse( "{"
					+ "\"requestType\":\"sendHTML\","
					+ "\"requestId\":\"1\","
					+ "\"html\":\"<form class=\\\"helloForm\\\">Name: <input id=\\\"hello__service-name\\\" type=\\\"text\\\" name=\\\"name\\\" />"
					+ "  <input type=\\\"button\\\" value=\\\"Submit\\\" onclick=\\\"helloService_sayHello()\\\" />"
					+ "<\\/form><div>Response: <span id=\\\"hello__service-sayHello\\\"><\\/span><\\/div>\","
					+ "\"script\":\"function helloService_sayHello() { \\tvar xhttp = new XMLHttpRequest();"
					+ "\\txhttp.open( 'POST', zeroMQcoreURL + \\\"/services\\\", true);\\txhttp.setRequestHeader( 'Content-type', 'application/json');\\txhttp.onload = function() {\\t\\tif (this.readyState == 4 && this.status == 200)"
					+ " {\\t\\t\\tvar responseJSON = JSON.parse( xhttp.responseText);\\t\\t\\tvar helloName = responseJSON.response;\\t\\t\\tdocument.getElementById( \\\"hello__service-sayHello\\\").innerHTML = helloName;\\t\\t}\\t};"
					+ "\\tvar name = document.getElementById( \\\"hello__service-name\\\").value;"
					+ "\\tvar requestJSON = '{\\\"requestType\\\":\\\"sayHello\\\",\\\"name\\\":\\\"' + name + '\\\"}';"
					+ "\\txhttp.send( requestJSON);}\"}");	
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
		
		// ____________________ Check Results _____________________ 
		mockResponse.assertEqualsContentType( "application/json; charset=utf-8");
		mockResponse.assertEqualsStatus( 200);
		mockResponse.assertEqualsResponse( "{\"requestType\":\"sayHello\",\"requestId\":\"1\",\"response\":\"Hello Tess\"}");
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
		Date endTime = null;
		
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
		
		// ____________________ Check Log File ____________________ 
		Thread.sleep(10);
		endTime = new Date();
		
		TreeMap<Integer,String> logFileLines = readLogFile( LOG_FILE_URL);
		
		// Read the log file backwards. 
		for (Integer lineNumber : logFileLines.keySet()) {
			String line = logFileLines.get( lineNumber);
		//	DONE: Remove System.out
		//	System.out.println( "log line " + lineNumber + ": " + line);
		
			try { 
				String timestampString = line.substring( 0, 23);
				Date timestamp = dateFormatter.parse( timestampString);
			//	assertTrue( timestamp.equals( startTime) || timestamp.after( startTime));
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
			else {	
				fail( "unexpected line " + lineNumber + ": " + line);
			}
		}
	}
	
	
	/** 
	 * Read the log file.
	 * Asserts that the file exist and can be read. 
	 * 
	 * @param logFileURL URL of the log file. 
	 * 
	 * @return map of the log file line using the line number as the key. 
	 */
	private TreeMap<Integer, String> readLogFile(String logFileURL) {

		File logFile = new File( logFileURL);
		assertTrue( logFile.exists());
		assertTrue( logFile.canRead());
		
		TreeMap<Integer,String>	lines	= new TreeMap<Integer,String>();
		FileReader 		fileReader 		= null;
		BufferedReader 	bufferedReader	= null;
		
		try {
			fileReader = new FileReader( logFile);
			bufferedReader = new BufferedReader( fileReader);
			
			int		lineNumber = 0;
			String 	line;
			while ((line = bufferedReader.readLine()) != null) 
			{	lines.put( lineNumber, line);
				lineNumber++;
			}
		}
		catch (Exception e) {
			fail( StackTrace.asString(e));
		}
		finally {
			try {
				if (bufferedReader	!= null) bufferedReader.close();
				if (fileReader		!= null) fileReader.close();
			}
			catch (Exception ee) { /** Do nothing */ }	
		}
		
		return lines;
	}
}
