package com.testlims.zeroMQcore;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONObject;

import org.junit.*;
//import junit.framework.TestCase;
//import junit.framework.TestSuite;
import org.zeromq.ZContext;

import com.testlims.utilities.StackTrace;

/**
 * Unit test for Logger. 
 */
public class LoggerTests 
{
	ZContext 	context 		= null;
	DateFormat 	dateFormatter 	= new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss.SSS");
	
	/**
	 * Create the test case
	 *
	 * @param testName name of the test case
	 
	public LoggerTests( String testName) {
		super( testName);
	} */

	/**  @return the suite of tests being tested. 
	public static junit.framework.Test suite() {
		return new TestSuite( LoggerTests.class);
    } */
	
	class MessageLoggerThread extends Thread {
		public void run() {
			try {
				new MessageLogger( context, "/var/log/zeroMQcore/messaging.log");
			}
			catch (Exception e) {
				System.out.print( dateFormatter.format( new Date()) + 
					StackTrace.asString( " MessageLogging Execption: ", e));
			}
		}
	}
	
	@Before
	public void createContext() {
		context = new ZContext();
	}
	
	/**
     * Test the MessageLogger 
	 * @throws InterruptedException when sleeping. 
	 */
	@Test
    public void loggerTest() throws InterruptedException 
    {	
		// Start MessageLogger in a tread. 
		MessageLoggerThread loggerThread = new MessageLoggerThread();
		loggerThread.start();
        System.out.println( dateFormatter.format( new Date()) + " loggerThread.start()");
        
        Thread.sleep( 200);
		
        MockHTTPzeroMQ mockHTTPInsatance = MockHTTPzeroMQ.getInstance();
        mockHTTPInsatance.createPublisher( context);
        System.out.println( dateFormatter.format( new Date()) + " mockHTTPInsatance.createPublisher( context)");
        
        Thread.sleep( 200);
    	
    	// Create a message to log. 
        String		reguestId	= "loggerTestId";
        String		reguestType	= "loggerTestType";
        String		message		= "loggerTest message";
    	JSONObject	requestJSON = new JSONObject();
        requestJSON.put( "requestType", reguestType);
        requestJSON.put( "message", 	message);
        mockHTTPInsatance.requestRecieved( reguestId, requestJSON);
        System.out.println( dateFormatter.format( new Date()) + " mockHTTPInsatance.requestRecieved( reguestId, requestJSON)");
        
        Thread.sleep( 200);
        
        // Terminate logger. 
        mockHTTPInsatance.requestRecieved( "TERMINATE_LOGGER", null);
        System.out.println( dateFormatter.format( new Date()) + " mockHTTPInsatance.requestRecieved( \"TERMINATE_LOGGER\", null)");
        
        Thread.sleep( 200);
        
        // Terminate Publisher
        mockHTTPInsatance.closePublisher();
        System.out.println( dateFormatter.format( new Date()) + " mockHTTPInsatance.closePublisher()");
    }
	
	
	@After
	public void terminateContext() {
	//	context.term();
	}
}
