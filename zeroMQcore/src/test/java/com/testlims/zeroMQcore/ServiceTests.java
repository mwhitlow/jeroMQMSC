package com.testlims.zeroMQcore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TreeMap;

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
helloService1Test: 2018-07-10T10:48:15.454 helloService started
HelloService waiting in run()
helloService1Test: sending requestId 0 requestString Tess
HelloService received request: Tess
HelloService response sent Hello Tess
HelloService waiting in run()
helloService1Test: received requestId 0 reply: Hello Tess

helloService1Test: sending requestId 1 requestString TERMINATE_HELLO_SERVICE
HelloService received request: TERMINATE_HELLO_SERVICE
HelloService received TERMINATE_HELLO_SERVICE
HelloService after while loop -> Close logger socket and terminate context. 
helloService1Test: received requestId 1 reply: HelloService being terminated
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
	        System.out.println( "helloService1Test: received requestId " + requestId + " reply: " + reply + "\n");
			
			requestId++;
			System.out.println( "helloService1Test: sending requestId " + requestId + " requestString TERMINATE_HELLO_SERVICE");
			requestClient.send( ("TERMINATE_HELLO_SERVICE").getBytes(), 0);
			reply = requestClient.recvStr();
	        System.out.println( "helloService1Test: received requestId " + requestId + " reply: " + reply);
			
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
		//	int requestId = 0;
			String requestString = "Tess";
			requestClient.send( requestString.getBytes(), 0);
			String reply = requestClient.recvStr();
			assertEquals( "Hello " + requestString, reply);
			
		//	requestId++;
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
					else if (line.contains( "HelloService received request: TERMINATE_HELLO_SERVICE")) {
						foundreceived2 = true;
					}
					else if (line.contains( "HelloService response sent Hello Tess")) {
						foundresponse1 = true;
					}
					else if (line.contains( "HelloService received request: Tess")) {
						foundreceived1 = true;
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
		assertTrue( foundLogFileOpenned);
    }
}
