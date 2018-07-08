package com.testlims.zeroMQcore;

import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;

/**
 * Implementation of zeroMQ/jeroMQ SUB log service a where the constructor 
 * creates the message logger that listens to for the topic on a TCP socket 
 * from any service sending to its socket, and logs the message to System.out. 
 *
 * @author Marc Whitlow, Colabrativ, Inc. 
 */
public class MessageLogger extends Thread {
//	private String 		socketURL 		= null; 
//	private String 		topic 			= null;
	private String 		topicDelimitated = null;
	private Context 	context 		= null;
	private ZMQ.Socket	logger  		= null;
	
	/**
	 * MessageLogger Constructor 
	 * 
	 * @param socketURL The URL that the logger will be bound to. 
	 * @param topic The topic that logger monitors.  
	 * @param logFileURL The URL of the log file. 
	 */
	public MessageLogger(String socketURL, String topic, String logFileURL) {
		topicDelimitated = topic + " ";
		
		setDaemon(true);
		context = ZMQ.context(1);
		logger = context.socket( ZMQ.SUB);
		logger.subscribe( topic.getBytes());
		logger.bind( socketURL);
	}
	
	/** 
	 * Run the message logger. 
	 * If the message logger receives a TERMINATE_LOGGER message, 
	 * then the logger is closed and the context terminated. 
	 */
	public void run()
	{
		while (!Thread.currentThread().isInterrupted()) {
			System.out.println( "MessageLogging waiting in run()");
			String topicAndMessage = logger.recvStr(); 
			String message = topicAndMessage.replace( topicDelimitated, "");
			
			if (message.equals( "TERMINATE_LOGGER")) {
			//	run = false;
				System.out.println( "MessageLogging received TERMINATE_LOGGER");
				break;
			}
			else {
				System.out.println( "MessageLogging received " + message);
			}
		}	
		System.out.println( "MessageLogging after while loop -> Close logger socket and terminate context. ");
		
		logger.close();
		context.close();
	}
	
	/**
	 * Main for MessageLogger that creates a PUB instance and starts the message logger. 
	 *
	 * @param args The following arguments are required to start message logger:  
	 * args[0]:  The topic that logger monitors, e.g. Project_Log 
	 * args[1]:  The URL that the logger will be bound to, e.g. tcp://127.0.0.1:5555 
	 * args[2]:  The URL of the log file, e.g. /var/log/zeroMQcore/project.log  
	 */
	public static void main( String[] args) throws Exception {
		
		MessageLogger messageLogger = new MessageLogger( args[0], args[1], args[2]);
		messageLogger.start();
		System.out.println( "MessageLogger started");
	}
	
}
