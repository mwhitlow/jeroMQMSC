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
public class ServiceTests 
{
	static final String 	LOG_FILE_URL		= "/var/log/zeroMQcore/project.log"; 
	static final String 	LOGGER_TOPIC		= "Project_Log"; 
	static final String 	LOGGER_URL			= "tcp://localhost:5556"; 
	static final String		dateFormat			= "yyyy-MM-dd'T'HH:mm:ss.SSS";
	static final String		fileDateFormat		= "yyyy-MM-dd'T'HH.mm.ss.SSS";
	static final DateFormat dateFormatter 		= new SimpleDateFormat( dateFormat);
	static final DateFormat fileDateFormatter 	= new SimpleDateFormat( fileDateFormat);
	
	Date	startTime 		= null;
	
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
			
			if (!logFile.getName().equals( "project.log")) {
				if (!logFile.delete()) {
					System.out.println( "LoggerTests.removeOldLogFiles: " + dateFormatter.format( new Date()) + 
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
			
			// Start Request Client
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
			
			// Start Request Client
			Context clientContext = ZMQ.context(1);
			ZMQ.Socket requestClient = clientContext.socket( ZMQ.REQ); 
			requestClient.connect( SOCKET_URL); 
			
			String requestId 	= "1";
			String requestType	= "sendHTML";
			JSONObject requestJSON = new JSONObject();
			requestJSON.put( "requestId",	requestId);
			requestJSON.put( "requestType", requestType);
			requestClient.send( requestJSON.toString().getBytes(), 0);
			String reply1 = requestClient.recvStr();
			Thread.sleep(2);
			
			requestClient.send( ("TERMINATE_HELLO_SERVICE").getBytes(), 0);
			String reply2 = requestClient.recvStr();
			Thread.sleep(2);
			
			// ____________________ Check Results _____________________ 
			assertEquals( "{\"requestType\":\"sendHTML\",\"requestId\":\"1\",\"html\":\"<form class=\\\"helloForm\\\">\\n  Name: <input type=\\\"text\\\" name=\\\"name\\\" />\\n  <br />\\n  <input type=\\\"submit\\\" value=\\\"Submit\\\" />\\n<\\/form>\"}", 
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
		
		// Start Request Client
		Context clientContext = ZMQ.context(1);
		ZMQ.Socket requestClient = clientContext.socket( ZMQ.REQ); 
		requestClient.connect( SOCKET_URL); 
		
		// Send a HTTP request and response to the MockHTTPzeroMQ 
		String requestId 	= "3";
		String requestType	= "sayHello";
		String name 		= "Tess";
		JSONObject requestJSON = new JSONObject();
		requestJSON.put( "requestId",	requestId);
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
		assertEquals( "{\"requestType\":\"sayHello\",\"requestId\":\"3\",\"response\":\"Hello Tess\"}", reply3);
		assertEquals( "HelloService being terminated", 	reply4);
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
		String requestType	= "sendHTML";
		JSONObject request5JSON = new JSONObject();
		request5JSON.put( "requestId",	requestId);
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
		
		// Read the log file backwards. 
		for (Integer lineNumber : logFileLines.keySet()) {
			String line = logFileLines.get( lineNumber);
			
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
				assertTrue( line.contains( "MessageLogging Log file /var/log/zeroMQcore/project.log opened."));
			}
			else if (lineNumber == 1) { 
				assertTrue( line.contains( "HelloService:5:sendHTML.request")); 
			}
			else if (lineNumber == 2) { 
				assertTrue( line.contains( "HelloService:5:sendHTML.response")); 
			}
			else if (lineNumber == 3) { 
				assertTrue( line.contains( "HelloService:6:sayHello.request:Tess")); 
			}
			else if (lineNumber == 4) { 
				assertTrue( line.contains( "HelloService:6:sayHello.response:Hello Tess")); 
			}
			else if (lineNumber == 5) { 
				assertTrue( line.contains( "HelloService request: TERMINATE_HELLO_SERVICE")); 
			}
			else if (lineNumber == 6) { 
				assertTrue( line.contains( "HelloService closing service and logger sockets and terminate context.")); 
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
