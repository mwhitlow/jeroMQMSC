package com.testlims.zeroMQcore;

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
	private String 		topicDelimitated 	= null;
	private Context 	context 			= null;
	private ZMQ.Socket	logger  			= null;
	private File 		logFile 			= null;
	private static BufferedWriter logWriter	= null;
	
	/**
	 * MessageLogger Constructor 
	 * 
	 * @param socketURL The URL that the logger will be bound to. 
	 * @param topic The topic that logger monitors.  
	 * @param logFileURL The URL of the log file. 
	 */
	public MessageLogger(String socketURL, String topic, String logFileURL) {
		topicDelimitated = topic + " ";
		
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
			
			if (message.equals( "TERMINATE_LOGGER")) {
				log( "MessageLogging received TERMINATE_LOGGER");
				break;
			}
			else {
				log( message);
			}
		}	
		
		log( "MessageLogging closing logger socket and terminating context.");
		logger.close();
		context.close();
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
			String requestId	= messageJSON.has( "requestId")	  ? messageJSON.getString( "requestId") : "0";
			String requestType	= messageJSON.has( "requestType") ? messageJSON.getString( "requestType") : "REQUEST_TYPE_MISSING";
			String message		= messageJSON.has( "message")	  ? messageJSON.getString( "message") : "NO_MESSAGE";
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
	 * args[0]:  The topic that logger monitors, e.g. Project_Log 
	 * args[1]:  The URL that the logger will be bound to, e.g. tcp://127.0.0.1:5555 
	 * args[2]:  The URL of the log file, e.g. /var/log/zeroMQcore/project.log  
	 * 
	 * @throws Exception if there is an issue start, running or shutting down the MessageLogger. 
	 */
	public static void main( String[] args) throws Exception {
		
		MessageLogger messageLogger = new MessageLogger( args[0], args[1], args[2]);
		messageLogger.start();
	}
	
	/**
	 * Terminate the message logger that is running on the supplied loggerURL and 
	 * listen to the supplied topic. 
	 * 
	 * @param loggerURL URL of the running message logger. 
	 * @param topic The topic that logger monitors. 
	 * 
	 * @throws InterruptedException if there is an issue sleep while waiting for the "publisher to logger" to startup. 
	 *
	public static void terminate(String loggerURL, String topic) throws InterruptedException {
		Context context = ZMQ.context(1);
		ZMQ.Socket pub2Logger = context.socket( ZMQ.PUB); 
		pub2Logger.connect( loggerURL); 
		
		Thread.sleep( 25);
		pub2Logger.send( topic + " " + "TERMINATE_LOGGER", 0);
		pub2Logger.close();
		context.close();
	}
	*/
	
}
