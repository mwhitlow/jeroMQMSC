package com.testlims.zeroMQcore;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TreeMap;

import org.junit.*;

import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;

import com.testlims.utilities.StackTrace;

/**
 * Unit tests for Message Loggers. 
 */
public class LoggerTests 
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
     * Test of MessageLogger0 
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
	
	/**
     * Test of MessageLogger 
     * 
     * Unlike most unit tests, this test only check that no exception was thrown. 
     * The actual logging is going to System.out, and should look like: 
     * 
	 * @throws InterruptedException if there is an issue with Thread.sleep in test. 
     */ 
	@Test
    public void loggerTest1() throws InterruptedException {
		final String LOG_FILE_URL	= "/var/log/zeroMQcore/project.log"; 
		final String SOCKET_URL 	= "tcp://localhost:5556"; 
		final String TOPIC 			= "Project_Log";
		final String topicDelimitated = TOPIC + " ";
		
		Date startTime	= new Date();
		Date endTime	= null;
		
		// _______________________ Run Test _______________________ 
		// Start MessageLogger in a tread. 
		MessageLogger messageLogger = new MessageLogger( SOCKET_URL, TOPIC, LOG_FILE_URL);
		messageLogger.start();
			
		// Build a publisher that send out TOPIC on the socket using socketURL.
		Context context = ZMQ.context(1);
		ZMQ.Socket pub2Logger = context.socket( ZMQ.PUB); 
		pub2Logger.connect( SOCKET_URL); 
			
		int sleepIncrement = 5;
		int elapsedMilliSeconds = 0;
		while (elapsedMilliSeconds <= 40)
		{	
			pub2Logger.send( topicDelimitated + "test message after " + elapsedMilliSeconds + " milliseconds", 0);
			Thread.sleep( sleepIncrement);
			elapsedMilliSeconds += sleepIncrement;
		}
		
		pub2Logger.send( topicDelimitated + "TERMINATE_LOGGER", 0);	
		pub2Logger.close();
		context.close();
		endTime = new Date();
		
		// ____________________ Check Log File ____________________ 
		TreeMap<Integer,String> logFileLines = readLogFile( LOG_FILE_URL);
		
		boolean foundClosingLogger	= false; 
		boolean foundTerminateLoger	= false;
		int		nfoundTestMessages	= 0;
		boolean foundLofFileOpenned	= false;
		
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
						foundTerminateLoger = true;
					}
					else if (line.contains( "test message after") && line.contains( "milliseconds")) {
						nfoundTestMessages++;
					}
					else if (line.contains( "MessageLogging Log file /var/log/zeroMQcore/project.log opened.")) {
						foundLofFileOpenned = true;
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
		assertTrue( foundTerminateLoger);
		assertTrue( nfoundTestMessages > 0);
		assertTrue( foundLofFileOpenned);
	}
	
	@Test
    public void loggerTest1main() {
		final String LOG_FILE_URL	= "/var/log/zeroMQcore/project.log";
		final String SOCKET_URL 	= "tcp://localhost:5556"; 
		final String TOPIC 			= "Project_Log";
		final String topicDelimitated = TOPIC + " ";
		
		try {
			// Start MessageLogger. 
			String[] args = {SOCKET_URL, TOPIC, LOG_FILE_URL};
			MessageLogger.main( args);
			
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
	
	/** 
	 * Read the log file.
	 * Asserts that the file exist and can be read. 
	 * 
	 * @param logFileURL URL of the log file. 
	 * 
	 * @return map of the log file line using the line number as the key. 
	 */
	private TreeMap<Integer,String> readLogFile(String logFileURL) {
		
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
