package com.testlims.messageLogger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;

import com.testlims.utilities.StackTrace;

/**
 * Implementation of zeroMQ/jeroMQ SUB log service a where the constructor 
 * creates the message logger that listens to for the topic on a TCP socket 
 * from any service sending to that socket, and logs the message to log file. 
 *
 * @author Marc Whitlow, Colabrativ, Inc. 
 */
public class MessageLogger extends Thread {
	private String 		logFileURL			= null;
	private String 		topicDelimitated 	= null;
	private Context 	context 			= null;
	private ZMQ.Socket	logger  			= null;
	private File 		logFile 			= null;
	private static BufferedWriter logWriter	= null;
	
	static final String		dateFormat			= "yyyy-MM-dd'T'HH:mm:ss.SSS";
	static final String		fileDateFormat		= "yyyy-MM-dd'T'HH.mm.ss.SSS";
	static final DateFormat dateFormatter 		= new SimpleDateFormat( dateFormat);
	static final DateFormat fileDateFormatter 	= new SimpleDateFormat( fileDateFormat);	
	/**
	 * MessageLogger Constructor 
	 * 
	 * @param socketURL The URL that the logger will be bound to. 
	 * @param topic The topic that logger monitors.  
	 * @param logFileURL The URL of the log file. 
	 */
	public MessageLogger(String socketURL, String topic, String logFileURL) {
		this.logFileURL = logFileURL;
		topicDelimitated = topic + " ";
		startLogWritter();
		
		if (logWriter != null) {
			setDaemon(true);
			context = ZMQ.context(1);
			logger = context.socket( ZMQ.SUB);
			logger.subscribe( topic.getBytes());
			logger.bind( socketURL);
		}
	}
	
	/** 
	 * Run the message logger. 
	 * If the message logger receives a TERMINATE_LOGGER message, 
	 * then the logger is closed and the context terminated. 
	 */
	public void run()
	{
		while (!Thread.currentThread().isInterrupted()) {
			String topicAndMessage = logger.recvStr(); 
			String message = topicAndMessage.replace( topicDelimitated, "");
			
			if (message.equals( "ARCIVE_LOG_FILE")) {
				log( "MessageLogging received ARCIVE_LOG_FILE");
				archiveLogFile();
			}
			else if (message.equals( "TERMINATE_LOGGER")) {
				log( "MessageLogging received TERMINATE_LOGGER");
				break;
			}
			else {
				log( message);
			}
		}	
		
		log( "MessageLogging closing logger socket and terminating context.");
		if (logWriter != null) {
			try {
				logWriter.flush();
				logWriter.close();
			} catch (IOException e) {
				System.err.print( StackTrace.asString( "ERROR: Failed to flush and close logWriter: ", e));
			}
		}
		logger.close();
		context.close();
	}
	
	private void startLogWritter() {
		try {
			logFile = new File( logFileURL);
			logWriter = new BufferedWriter( new FileWriter( logFile, true));
			log( "MessageLogging Log file " + logFileURL + " opened.");
		} 
		catch (IOException ioe) {
			System.err.print( StackTrace.asString( "MessageLogging ERROR: Failed to open log file " + logFile, ioe));
			try { 
				if (logWriter != null) 
				{	logWriter.close(); 
					logWriter = null;
				}
			}
			catch (IOException e1) { /** Do nothing */ }
		}
	}
	
	private void archiveLogFile() {
		try {
			logWriter.flush();
			logWriter.close();
			
			String timestamp = fileDateFormatter.format( new Date( logFile.lastModified()));
			File renamedLogFile = new File( "C:/var/log/zeroMQcore/project_" + timestamp + ".log");
				
			if (!logFile.renameTo( renamedLogFile)) {
				System.err.println( "LoggerTests.moveExistingLogFile: " + dateFormatter.format( new Date()) + 
							" FAILED TO MOVE project.log to " + renamedLogFile.getName());
			}
			
			startLogWritter();
		} catch (IOException e) {
			System.err.print( StackTrace.asString( "ERROR: Unable to Archive Log File: ", e));
		}
		
	}

	/**
	 * Log the supplied messageString to the log file. 
	 * 
	 * @param messageString The message string contains the message to be logged. 
	 * messageString can either be a JSON Object for the form: 
	 * <pre>
{    "requestId": "Id1234", 
     "requestType": "createPersonel", 
     "message": "createPersonel request received."
}</pre> 
	 * or a text message.  All log messages start with a date, e.g. 2018-06-29:08:38:03. 
	 * If message is a JSON object string, then the requestId, requestType, and message are 
	 * appended to the date and written to the log file.  Otherwise the messageString is 
	 * appended to the date and written to the log file. 
	 */
	private static void log(String messageString) 
	{	
		DateFormat 	dateFormatter = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss.SSS");
		String 		loggedMessage = dateFormatter.format( new Date()) + ":";
		
		try {
			JSONObject messageJSON = new JSONObject( messageString);
			String requestId	= messageJSON.has( "requestId")		? messageJSON.getString( "requestId")	: "0";
			String requestType	= messageJSON.has( "requestType")	? messageJSON.getString( "requestType")	: "REQUEST_TYPE_MISSING";
			String message		= messageJSON.has( "message")		? messageJSON.getString( "message")		: "MESSAGE_MISSING";
			loggedMessage = loggedMessage + requestId + ":" + requestType+ ":" + message;
		}
		catch (JSONException e) {
			loggedMessage = loggedMessage + messageString;
		}
		
		try {
			logWriter.write( loggedMessage);
			logWriter.newLine();
			logWriter.flush();
		} 
		catch (IOException ioe) {
			System.err.print( StackTrace.asString( "ERROR: Failed to log massage: " + loggedMessage, ioe));
		}
	}
	
	/**
	 * Main for MessageLogger that creates a PUB instance and starts the message logger. 
	 *
	 * @param args The following arguments are required to start message logger:  
	 * args[0]:  The URL that the logger will be bound to, e.g. tcp://127.0.0.1:5555 
	 * args[1]:  The topic that logger monitors, e.g. Project_Log 
	 * args[2]:  The URL of the log file, e.g. /var/log/zeroMQcore/project.log  
	 */
	public static void main( String[] args) {
		
		MessageLogger messageLogger = new MessageLogger( args[0], args[1], args[2]);
		messageLogger.run();
	}
	
}
