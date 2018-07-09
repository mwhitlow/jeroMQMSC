package com.testlims.zeroMQcore;

import com.testlims.utilities.StackTrace;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

/**
 * MessageLogger read in message using a SUB socket from any 
 * service sending to it socket, and logs the message in to 
 * the log file using SLF4J/LOG4J. 
 *
 * @author Marc Whitlow, Colabrativ, Inc. 
 */
public class MessageLoggerX {
	private static File 			logFile 	= null;
	private static BufferedWriter	logWriter	= null;
//	private ZContext				context 	= null;
	
	/**
	 * MessageLogger Constructor 
	 * 
	 * @param context zeroMQ context 
	 * @param logFileURL URL of the log file, e.g. /var/log/zeroMQcore/messaging.log 
	 */
	public MessageLoggerX( ZContext context, String logFileURL) {
//		this.context = context;
		
		try {
			logFile = new File( logFileURL);
			logWriter = new BufferedWriter( new FileWriter( logFile, true));
			log( "MessageLogging Log file " + logFileURL + " opened.");
		} 
		catch (IOException ioe) {
			System.out.print( StackTrace.asString( "MessageLogging ERROR: Failed to open log file " + logFile, ioe));
			try { 
				if (logWriter != null) 
				{	logWriter.close(); 
					logWriter = null;
				}
			}
			catch (IOException e1) { /** Do nothing */ }
		}
		
		if (logWriter != null) 
		{
		//	Context context = new ZContext();  // ZMQ.context(1);
			ZMQ.Socket logger  = context.createSocket( ZMQ.SUB);
			String socketURL = "tcp://127.0.0.1:5555"; 
		//	logger.connect( socketURL);
			logger.bind( socketURL);
			log( "MessageLogging service bound to socket " + socketURL);
			
			//  Process messages from logger socket
		//	Boolean run = true;
		//	while (run) {
			while (!Thread.currentThread().isInterrupted()) {
//				byte[] byteMessage = logger.recv(0);
//				System.out.println( format("MessageLogging recv(0) message: %s", new String( byteMessage)));
//				log( new String( byteMessage));
				//  Use trim to remove the tailing '0' character
				String message = logger.recvStr().trim(); 
				System.out.println( "MessageLogging received message: " + message);
				
				if (message.equals( "TERMINATE_LOGGER")) {
				//	run = false;
					log( "MessageLogging recieved TERMINATE_LOGGER");
					break;
				}
				else {
					log( message);
				}
	        }
			System.out.println( "MessageLogging after while loop -> Close logger socket and terminate context. ");
			
			//  Close logger socket and terminate context. 
			logger.close();
			log( "MessageLogging socket closed.");
		//	context.term();
		//	log( "MessageLogging terminated.");
			
			// Close logWriter 
			try { 
				if (logWriter != null) 
				{	logWriter.close(); 
					logWriter = null;
				}
			}
			catch (IOException e1) { /** Do nothing */ }
		}
	}
	
	
	/**
	 * Main for MessageLogger
	 * @param args args[] is the URL of the log file, e.g. 
	 */
	public static void main( String[] args) {
		ZContext context = new ZContext();
		MessageLoggerX logger = new MessageLoggerX( context, args[0]);
		if (logger != null)
			System.out.println( "MessageLogger started that output to " + args[0]);
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
			String requestId	= messageJSON.getString( "requestId");
			String requestType	= messageJSON.getString( "requestType");
			String message		= messageJSON.getString( "message");
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
			System.out.print( StackTrace.asString( "ERROR: Failed to log massage: " + loggedMessage, ioe));
		}
	}
}
