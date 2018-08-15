package com.testlims.messageLogger;

import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;

/**
 * The second implementation of zeroMQ/jeroMQ SUB log service a where the constructor 
 * creates the message logger that listens to for the TOPIC on a TCP connection 
 * from any service sending to its socket, and logs the message to System.out. 
 *
 * @author Marc Whitlow, Colabrativ, Inc. 
 */
public class MessageLogger1 extends Thread {
	static final String TOPIC = "Project_Log";
	static final String TOPIC_DELIMITATED = TOPIC + " ";
	static final String socketURL = "tcp://localhost:5555"; 
	ZMQ.Socket logger  = null;
	Context context = null;
	
	/**
	 * MessageLogger Constructor  */
	public MessageLogger1() {
		setDaemon(true);
		context = ZMQ.context(1);
		logger = context.socket( ZMQ.SUB);
		logger.subscribe( TOPIC.getBytes());
		logger.bind( socketURL);
	}
	
	/** 
	 * Run the message logger. 
	 * <p>
	 * If the message logger receives a TERMINATE_LOGGER message, 
	 * then the logger is closed and the context terminated. 
	 */
	public void run()
	{
		while (!Thread.currentThread().isInterrupted()) {
			System.out.println( "MessageLogging waiting in run()");
			String topicAndMessage = logger.recvStr(); 
			String message = topicAndMessage.replace( TOPIC_DELIMITATED, "");
			
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
	 * @param args No arguments are required. 
	 * 
	 * @throws Exception if there is an issue start, running or shutting down the MessageLogger1. 
	 */
	public static void main( String[] args) throws Exception {
		
		MessageLogger1 messageLogger = new MessageLogger1();
		messageLogger.start();
		System.out.println( "MessageLogger started");
	}
	
}
