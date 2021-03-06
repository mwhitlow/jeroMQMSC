package com.testlims.helloService;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
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
 * Unit tests for Hello Service(s). 
 */
public class HelloServiceTests 
{
	static final String 	LOG_FILE_URL		= "/var/log/zeroMQcore/project.log"; 
	static final String 	LOGGER_TOPIC		= "Project_Log"; 
	static final String 	LOGGER_URL			= "tcp://localhost:5556"; 
	static final String 	SOCKET_URL			= "tcp://localhost:5558"; 
	static final String		dateFormat			= "yyyy-MM-dd'T'HH:mm:ss.SSS";
	static final String		fileDateFormat		= "yyyy-MM-dd'T'HH.mm.ss.SSS";
	static final DateFormat dateFormatter 		= new SimpleDateFormat( dateFormat);
	static final DateFormat fileDateFormatter 	= new SimpleDateFormat( fileDateFormat);
	
	Date	startTime 		= null;
		
	@BeforeClass
	public static void removeOldLogFiles() {	
		File logDirectory = new File( "C:/var/log/zeroMQcore");
		File[] logFileList = logDirectory.listFiles();
		
		for (int f=0; f<logFileList.length; f++)
		{	File logFile = logFileList[f];
			
			if (!logFile.getName().equals( "project.log")) {
				if (!logFile.delete()) {
					System.out.println( "HelloServiceTests.removeOldLogFiles: " + dateFormatter.format( new Date()) + 
							" FAILED TO DELETE " + logFile.getName());
				}
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
			// Start HelloService 
			HelloService helloService = new HelloService( SOCKET_URL, LOGGER_URL, LOGGER_TOPIC);
			helloService.start();
			Thread.sleep(25);
			
			// Start Request Client
			Context clientContext = ZMQ.context(1);
			ZMQ.Socket requestClient = clientContext.socket( ZMQ.REQ); 
			requestClient.connect( SOCKET_URL); 
			
			String requestId 	= "1";
			String serviceName	= "HelloService";
			String requestType	= "sendHTML";
			JSONObject requestJSON = new JSONObject();
			requestJSON.put( "requestId",	requestId);
			requestJSON.put( "serviceName", serviceName);
			requestJSON.put( "requestType", requestType);
			requestClient.send( requestJSON.toString().getBytes(), 0);
			String reply1 = requestClient.recvStr();
			Thread.sleep(2);
			
			requestClient.send( ("TERMINATE_HELLO_SERVICE").getBytes(), 0);
			String reply2 = requestClient.recvStr();
			Thread.sleep(2);
			
			// ____________________ Check Results _____________________ 
			assertEquals( "{\"requestType\":\"sendHTML\",\"requestId\":\"1\",\"html\":\"<form class=\\\"helloForm\\\">Name: <input id=\\\"hello__service-name\\\" type=\\\"text\\\" name=\\\"name\\\" />"
						+ "  <input type=\\\"button\\\" class=\\\"helloService__button\\\" value=\\\"Submit\\\" onclick=\\\"helloService_sayHello()\\\" /><\\/form><div>Response: <span id=\\\"hello__service-sayHello\\\"><\\/span><\\/div>\",\"serviceName\":\"HelloService\","
						+ "\"script\":\"function helloService_sayHello() { \\tvar xhttp = new XMLHttpRequest();\\txhttp.open( 'POST', zeroMQcoreURL + \\\"/services\\\", true);\\txhttp.setRequestHeader( 'Content-type', 'application/json');\\txhttp.onload = function() {\\t\\tif (this.readyState == 4 && this.status == 200) {\\t\\t\\tvar responseJSON = JSON.parse( xhttp.responseText);\\t\\t\\tvar helloName = responseJSON.response;\\t\\t\\tdocument.getElementById( \\\"hello__service-sayHello\\\").innerHTML = helloName;\\t\\t}\\t};\\tvar name = document.getElementById( \\\"hello__service-name\\\").value;\\tvar requestJSON = '{\\\"serviceName\\\":\\\"HelloService\\\", \\\"requestType\\\":\\\"sayHello\\\", \\\"name\\\":\\\"' + name + '\\\"}';\\txhttp.send( requestJSON);}\"}", 
							reply1);
			assertEquals( "HelloService being terminated", reply2);
		}
		catch (Exception e) {
			fail( StackTrace.asString(e));
		}
    }
	
	/**
     * Test the sayHello request in HelloService using Mock HTTP request
	 * 
	 * @throws InterruptedException if there is an issue putting the thread to sleep. 
	 * @throws IOException if there is an issue reading the log file. 
	 */
	@Test
    public void helloServiceShouldReturnHelloName() throws InterruptedException, IOException {
		// Start HelloService 
		HelloService helloService = new HelloService( SOCKET_URL, LOGGER_URL, LOGGER_TOPIC);
		helloService.start();
		Thread.sleep(25);
		
		// Start Request Client
		Context clientContext = ZMQ.context(1);
		ZMQ.Socket requestClient = clientContext.socket( ZMQ.REQ); 
		requestClient.connect( SOCKET_URL); 
		
		// Send a HTTP request and response to the MockHTTPzeroMQ 
		String requestId 	= "3";
		String serviceName	= "HelloService";
		String requestType	= "sayHello";
		String name 		= "Tess";
		JSONObject requestJSON = new JSONObject();
		requestJSON.put( "requestId",	requestId);
		requestJSON.put( "serviceName", serviceName);
		requestJSON.put( "requestType", requestType);
		requestJSON.put( "name", 		name);
		requestClient.send( requestJSON.toString().getBytes(), 0);
		String reply3 = requestClient.recvStr();
		Thread.sleep(2);
		
		// Send terminate hello service request. 
		requestClient.send( ("TERMINATE_HELLO_SERVICE").getBytes(), 0);
		String reply4 = requestClient.recvStr();
		Thread.sleep(2);
		
		// ____________________ Check Results _____________________ 
		assertEquals( "{\"requestType\":\"sayHello\",\"requestId\":\"3\",\"response\":\"Hello Tess\",\"serviceName\":\"HelloService\"}", reply3);
		assertEquals( "HelloService being terminated", 	reply4);
	}

	@Test
	/**
     * Check the logging of HelloService using Mock HTTP request
     * <p>
     * Note that it takes about 20 milliseconds before the HelloService is ready to receive requests. 
	 * 
	 * @throws InterruptedException 
	 * @throws IOException 
	 */
    public void helloServiceCheckLog() throws InterruptedException, IOException {
		startTime = new Date();
		Date endTime = null;
		
		// Start HelloService 
		HelloService helloService = new HelloService( SOCKET_URL, LOGGER_URL, LOGGER_TOPIC);
		helloService.start();
		Thread.sleep(25);
		
		// Start Request Client
		Context clientContext = ZMQ.context(1);
		ZMQ.Socket requestClient = clientContext.socket( ZMQ.REQ); 
		requestClient.connect( SOCKET_URL); 
		
		String requestId 	= "5";
		String serviceName	= "HelloService";
		String requestType	= "sendHTML";
		JSONObject request5JSON = new JSONObject();
		request5JSON.put( "requestId",	requestId);
		request5JSON.put( "serviceName", serviceName);
		request5JSON.put( "requestType", requestType);
		requestClient.send( request5JSON.toString().getBytes(), 0);
		String reply5 = requestClient.recvStr();
		Thread.sleep(2);
		
		// Send a second HTTP request. 
		requestId 			= "6";
		String name 		= "Tess";
		String requestType6 = "sayHello";
		JSONObject request6JSON = new JSONObject();
		request6JSON.put( "requestId",	requestId);
		request6JSON.put( "serviceName", serviceName);
		request6JSON.put( "requestType", requestType6);
		request6JSON.put( "name", 		name);
		requestClient.send( request6JSON.toString().getBytes(), 0);
		String reply6 = requestClient.recvStr();
		Thread.sleep(2);
		
		// Send terminate hello service request. 
		requestClient.send( ("TERMINATE_HELLO_SERVICE").getBytes(), 0);
		String reply7 = requestClient.recvStr();
		Thread.sleep(10);
		
		// ____________________ Check Log File ____________________ 
		assertNotNull( reply5);
		assertNotNull( reply6);
		assertNotNull( reply7);
		Thread.sleep(10);
		endTime = new Date();
		
		TreeMap<Integer,String> logFileLines = readLogFile( LOG_FILE_URL);
		if( logFileLines.size() < 6) {
			fail( "logFileLines.size() = " + logFileLines.size() + " < 6");
		}
		
		for (Integer lineNumber : logFileLines.keySet()) {
			String line = logFileLines.get( lineNumber);
		//	DONE: Remove System.out	
		//	System.out.println( "line #" + lineNumber + ": " + line);
			
			try { 
				String timestampString = line.substring( 0, 23);
				Date timestamp = dateFormatter.parse( timestampString);
			//	assertTrue( timestamp.after( startTime));
				assertTrue( timestamp.before( endTime));
			}
			catch (Exception e) {
				fail( "Unable to parse timestamp on line " + lineNumber + " line: " + line);
			}
			
			if (lineNumber == 0) { 
				assertTrue( line.contains( "0:MessageLogging:Log file /var/log/zeroMQcore/project.log opened."));
			}
			else if (lineNumber == 1) { 
				assertTrue( line.contains( "0:HelloService:Started")); 
			}
			else if (lineNumber == 2) { 
				assertTrue( line.contains( "5:HelloService:sendHTML.request")); 
			}
			else if (lineNumber == 3) { 
				assertTrue( line.contains( "5:HelloService:sendHTML.response")); 
			}
			else if (lineNumber == 4) { 
				assertTrue( line.contains( "6:HelloService:sayHello.request:Tess")); 
			}
			else if (lineNumber == 5) { 
				assertTrue( line.contains( "6:HelloService:sayHello.response:Hello Tess")); 
			}
			else if (lineNumber == 6) { 
				assertTrue( line.contains( "-1:HelloService:request: TERMINATE_HELLO_SERVICE")); 
			}
			else if (lineNumber == 7) { 
				assertTrue( line.contains( "-1:HelloService:Closing service and logger sockets and terminate context.")); 
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
