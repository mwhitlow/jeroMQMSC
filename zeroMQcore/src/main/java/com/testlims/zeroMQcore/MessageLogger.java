package com.testlims.zeroMQcore;

import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;

/**
 * Initial implementation of zeroMQ/jeroMQ SUB log service a where the constructor 
 * creates the message logger that listens to for the TOPIC on a TCP connection 
 * from any service sending to its socket, and logs the message to System.out. 
 * 
 * To test the system the main creates a PUB instance that send two messages. 
 <ol>
   <li>a simple text message, and </li>
   <li>terminate message logger message.</li>
 </ol>
 *
 * @author Marc Whitlow, Colabrativ, Inc. 
 */
public class MessageLogger extends Thread {
	static final String TOPIC = "Project_Log";
	static final String TOPIC_DELIMITATED = TOPIC + " ";
	static final String socketURL = "tcp://localhost:5555"; 
	ZMQ.Socket logger  = null;
	Context context = null;
	
	/**
	 * MessageLogger Constructor  */
	public MessageLogger() {
		setDaemon(true);
		context = ZMQ.context(1);
		logger = context.socket( ZMQ.SUB);
		logger.subscribe( TOPIC.getBytes());
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
	 */
	public static void main( String[] args) throws Exception {
		
		MessageLogger messageLogger = new MessageLogger();
		messageLogger.start();
		System.out.println( "MessageLogger started");
	}
	
}
