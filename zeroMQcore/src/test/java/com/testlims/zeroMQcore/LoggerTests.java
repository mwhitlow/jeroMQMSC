package com.testlims.zeroMQcore;

import static org.junit.Assert.fail;

import com.testlims.utilities.StackTrace;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.*;

import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;

/**
 * Unit tests for Message Loggers. 
 */
public class LoggerTests 
{
	DateFormat 	dateFormatter = new SimpleDateFormat( "yyyy-MM-dd HH:mm:ss.SSS");
	
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
     * First test of MessageLogger 
     * 
     * Unlike most unit tests, this test only check that no exception was thrown. 
     * The actual logging is going to System.out, and should look like: 
<pre>
MessageLogging waiting in run()
loggerTest0: 2018-07-08 09:11:03.617 messageLogger.start()
loggerTest0: test message after 0 milliseconds
loggerTest0: test message after 5 milliseconds
loggerTest0: test message after 10 milliseconds
loggerTest0: test message after 15 milliseconds
loggerTest0: test message after 20 milliseconds
MessageLogging received test message after 20 milliseconds
MessageLogging waiting in run()
loggerTest0: test message after 25 milliseconds
MessageLogging received test message after 25 milliseconds
MessageLogging waiting in run()
loggerTest0: test message after 30 milliseconds
MessageLogging received test message after 30 milliseconds
MessageLogging waiting in run()
loggerTest0: test message after 35 milliseconds
MessageLogging received test message after 35 milliseconds
MessageLogging waiting in run()
loggerTest0: test message after 40 milliseconds
MessageLogging received test message after 40 milliseconds
MessageLogging waiting in run()
MessageLogging received TERMINATE_LOGGER
MessageLogging after while loop -> Close logger socket and terminate context. 
</pre>
     * Note that it takes about 20 milliseconds before the MessageLogger is ready to receive messages. 
     * 
	 * @throws InterruptedException when sleeping. 
	 */
	@Test
    public void loggerTest0() {
		final String SOCKET_URL	= "tcp://localhost:5555"; 
		final String TOPIC 		= "Project_Log";
		final String TOPIC_DELIMITATED = TOPIC + " ";
		
		try {
			// Start MessageLogger in a tread. 
			MessageLogger1 messageLogger = new MessageLogger1();
			messageLogger.start();
			System.out.println( "loggerTest0: " + dateFormatter.format( new Date()) + " messageLogger.start()");
			
			// Build a publisher that send out TOPIC on the socket using socketURL.
			Context context = ZMQ.context(1);
			ZMQ.Socket pub2Logger = context.socket( ZMQ.PUB); 
			pub2Logger.connect( SOCKET_URL); 
			
			int sleepIncrement = 5;
			int elapsedMilliSeconds = 0;
			while (elapsedMilliSeconds <= 40)
			{	
				pub2Logger.send( TOPIC_DELIMITATED + "test message after " + elapsedMilliSeconds + " milliseconds", 0);
				System.out.println( "loggerTest0: test message after " + elapsedMilliSeconds + " milliseconds");
				Thread.sleep( sleepIncrement);
				elapsedMilliSeconds += sleepIncrement;
			}
			pub2Logger.send( TOPIC_DELIMITATED + "TERMINATE_LOGGER", 0);
			
			Thread.sleep( sleepIncrement);
			pub2Logger.close();
			context.close();
		}
		catch (Exception e) {
			fail( StackTrace.asString(e));
		}
    }
	

	@Test
    public void loggerTest1() {
		final String SOCKET_URL = "tcp://localhost:5556"; 
		final String TOPIC 		= "Project_Log";
		final String topicDelimitated = TOPIC + " ";
		
		try {
			// Start MessageLogger in a tread. 
			MessageLogger messageLogger = new MessageLogger( SOCKET_URL, TOPIC);
			messageLogger.start();
			System.out.println( "loggerTest: " + dateFormatter.format( new Date()) + " messageLogger.start()");
			
			// Build a publisher that send out TOPIC on the socket using socketURL.
			Context context = ZMQ.context(1);
			ZMQ.Socket pub2Logger = context.socket( ZMQ.PUB); 
			pub2Logger.connect( SOCKET_URL); 
			
			int sleepIncrement = 5;
			int elapsedMilliSeconds = 0;
			while (elapsedMilliSeconds <= 40)
			{	
				pub2Logger.send( topicDelimitated + "test message after " + elapsedMilliSeconds + " milliseconds", 0);
				System.out.println( "loggerTest: test message after " + elapsedMilliSeconds + " milliseconds");
				Thread.sleep( sleepIncrement);
				elapsedMilliSeconds += sleepIncrement;
			}
			pub2Logger.send( topicDelimitated + "TERMINATE_LOGGER", 0);
			
			Thread.sleep( sleepIncrement);
			pub2Logger.close();
			context.close();
		}
		catch (Exception e) {
			fail( StackTrace.asString(e));
		}
	}
	
	@Test
    public void loggerTest1main() {
		final String SOCKET_URL = "tcp://localhost:5556"; 
		final String TOPIC 		= "Project_Log";
		final String topicDelimitated = TOPIC + " ";
		
		try {
			// Start MessageLogger. 
			String[] args = {SOCKET_URL, TOPIC};
			MessageLogger.main( args);
			System.out.println( "loggerTest: " + dateFormatter.format( new Date()) + " messageLogger.start()");
			
			// Build a publisher that send out TOPIC on the socket using socketURL.
			Context context = ZMQ.context(1);
			ZMQ.Socket pub2Logger = context.socket( ZMQ.PUB); 
			pub2Logger.connect( SOCKET_URL); 
			
			int sleepIncrement = 5;
			int elapsedMilliSeconds = 0;
			while (elapsedMilliSeconds <= 40)
			{	
				pub2Logger.send( topicDelimitated + "test message after " + elapsedMilliSeconds + " milliseconds", 0);
				System.out.println( "loggerTest: test message after " + elapsedMilliSeconds + " milliseconds");
				Thread.sleep( sleepIncrement);
				elapsedMilliSeconds += sleepIncrement;
			}
			pub2Logger.send( topicDelimitated + "TERMINATE_LOGGER", 0);
			
			Thread.sleep( sleepIncrement);
			pub2Logger.close();
			context.close();
		}
		catch (Exception e) {
			fail( StackTrace.asString(e));
		}
	}	

}
