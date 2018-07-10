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

import org.json.JSONObject;
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
     * Test of MessageLogger1
     * <p>
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
MessageLogging after while loop -&gt; Close logger socket and terminate context. 
</pre>
     * Note that it takes about 20 milliseconds before the MessageLogger is ready to receive messages. 
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
     * In this test the MessageLogger is created using its constructor. 
     * This test checks for the following messages:
     <ol>
       <li>The log file was opened message. </li>
       <li>Several "test message after X milliseconds", where X is an increment of 5 were logged.</li>
       <li>The TERMINATE_LOGGER message, and </li>
       <li>The closing logger socket and terminating context message.</li>
     </ol>
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
		boolean foundLogFileOpenned	= false;
		
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
		assertTrue( foundTerminateLoger);
		assertTrue( nfoundTestMessages > 0);
		assertTrue( foundLogFileOpenned);
	}
	
	/**
     * In this test of MessageLogger the logger is created using the MessageLogger main method. 
     * Two message are sent to the logger.  The first sends the follow JSON object: 
     * <pre>
{ 
  "requestId": "loggerTest1mainId",
  "requestType": "LoggerTests",
  "message": "loggerTest1main message" 
} </pre>
	 * The second sends the TERMINATE_LOGGER request. 
	 * The test checks that the requestId, requestType, and message are parsed 
	 * from the JSON object and placed into the log entry, 
	 * in addition to checking for the following start-up and shut-down messages:
     <ol>
       <li>The log file was opened message. </li>
       <li>The TERMINATE_LOGGER message, and </li>
       <li>The closing logger socket and terminating context message.</li>
     </ol>
     * 
	 * @throws InterruptedException if there is an issue with Thread.sleep in test. 
     */ 
	@Test
    public void loggerTest1main() throws Exception {
		final String LOG_FILE_URL	= "/var/log/zeroMQcore/project.log";
		final String SOCKET_URL 	= "tcp://localhost:5556"; 
		final String TOPIC 			= "Project_Log";
		final String topicDelimitated = TOPIC + " ";
		final String requestId		= "loggerTest1mainId";
		final String requestType	= "LoggerTests";
		final String message		= "loggerTest1main message";
		
		Date startTime	= new Date();
		Date endTime	= null;
		
		// Start MessageLogger. 
		String[] args = {SOCKET_URL, TOPIC, LOG_FILE_URL};
		MessageLogger.main( args);
		
		// Build a publisher that send out TOPIC on the socket using socketURL.
		Context context = ZMQ.context(1);
		ZMQ.Socket pub2Logger = context.socket( ZMQ.PUB); 
		pub2Logger.connect( SOCKET_URL); 
		
		Thread.sleep( 30);
		JSONObject messageJSON = new JSONObject();
		messageJSON.put( "requestId",	requestId);
		messageJSON.put( "requestType", requestType);
		messageJSON.put( "message", 	message);
		pub2Logger.send( topicDelimitated + messageJSON.toString());
			
		Thread.sleep( 5);
		pub2Logger.send( topicDelimitated + "TERMINATE_LOGGER", 0);
			
		pub2Logger.close();
		context.close();
		endTime = new Date();
		
		// ____________________ Check Log File ____________________ 
		TreeMap<Integer,String> logFileLines = readLogFile( LOG_FILE_URL);
		
		boolean foundClosingLogger	= false; 
		boolean foundTerminateLoger	= false;
		boolean foundLogFileOpenned	= false;
		
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
					else if (line.contains( requestId)) {
						assertTrue( line.contains( requestType));
						assertTrue( line.contains( message));
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
		assertTrue( foundTerminateLoger);
		assertTrue( foundLogFileOpenned);
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
