package com.testlims.messageLogger;

import com.testlims.utilities.StackTrace;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TreeMap;

import org.json.JSONObject;
import org.junit.*;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;

/**
 * Unit tests for Message Loggers. 
 */
public class LoggerTests 
{
	static final String		dateFormat			= "yyyy-MM-dd'T'HH:mm:ss.SSS";
	static final String		fileDateFormat		= "yyyy-MM-dd'T'HH.mm.ss.SSS";
	static final DateFormat dateFormatter 		= new SimpleDateFormat( dateFormat);
	static final DateFormat fileDateFormatter 	= new SimpleDateFormat( fileDateFormat);
	
	
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
		Thread t = new Thread( messageLogger);
		t.start();
			
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
		
		// Read the log file backwards. 
		int firstTimeLine = 0;
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
				if( line.contains( "test message after 10 milliseconds")) firstTimeLine = 1;
				if( line.contains( "test message after 15 milliseconds")) firstTimeLine = 2;
				if( line.contains( "test message after 20 milliseconds")) firstTimeLine = 3;
				if( line.contains( "test message after 25 milliseconds")) firstTimeLine = 4;
			}
			else if ((lineNumber + firstTimeLine) == 6) { 
				assertTrue( line.contains( "test message after 30 milliseconds"));
			}
			else if ((lineNumber + firstTimeLine) == 8) { 
				assertTrue( line.contains( "test message after 40 milliseconds"));
			}
			else if ((lineNumber + firstTimeLine) == 9) { 
				assertTrue( line.contains( "MessageLogging received TERMINATE_LOGGER"));
			}
			else if ((lineNumber + firstTimeLine) == 10) { 
				assertTrue( line.contains( "MessageLogging closing logger socket and terminating context."));
			}
			else if ((lineNumber + firstTimeLine) > 10) {
				fail( "Unexpected line number " + lineNumber + " line: " + line);
			}
		}
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
		
		// Start MessageLogger in a tread. 
		MessageLogger messageLogger = new MessageLogger( SOCKET_URL, TOPIC, LOG_FILE_URL);
		Thread t = new Thread( messageLogger);
		t.start();
		
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
				assertTrue( line.contains( "loggerTest1mainId:LoggerTests:loggerTest1main message"));
			}
			else if (lineNumber == 2) { 
				assertTrue( line.contains( "MessageLogging received TERMINATE_LOGGER"));
			}
			else if (lineNumber == 3) { 
				assertTrue( line.contains( "MessageLogging closing logger socket and terminating context."));
			}
			else {
				fail( "Unexpected line number " + lineNumber + " line: " + line);
			}
		}
	}
	
	/**
	 * This test that an exception is thrown if the MessageLogger.main is missing an argument.  */
	@Test
    public void loggerMainMissingArgument() {
		final String SOCKET_URL 	= "tcp://localhost:5556"; 
		final String TOPIC 			= "Project_Log";
		boolean throwsException		= false;
		
		// Start MessageLogger missing log file URL. 
		try {
			String[] args = {SOCKET_URL, TOPIC};
			MessageLogger.main( args);
		} 
		catch (Exception e) {
			throwsException = true;
		}
		
		assertTrue( throwsException);
	}
	
	/** 
	 * Read the log file.
	 * Asserts that the file exist and can be read. 
	 * 
	 * @param logFileURL URL of the log file. 
	 * 
	 * @return map of the log file line using the line number as the key. 
	 */
	static TreeMap<Integer,String> readLogFile(String logFileURL) {
		
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
	
	
	@After
	public void moveLogFile() {	
		File logFile = new File( "C:/var/log/zeroMQcore/project.log");
		
		if (logFile.exists())
		{	String timestamp = fileDateFormatter.format( new Date( logFile.lastModified()));
			File renamedLogFile = new File( "C:/var/log/zeroMQcore/project_" + timestamp + ".log");
			
			if (!logFile.renameTo( renamedLogFile)) {
				System.out.println( "LoggerTests.moveExistingLogFile: " + dateFormatter.format( new Date()) + 
						" FAILED TO MOVE project.log to " + renamedLogFile.getName());
			}
		}
		
		logFile = null;
	}
}
