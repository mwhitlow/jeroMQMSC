package com.testlims.zeroMQcore;

import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;

/**
 * MessageLogger read in message using a SUB socket from any 
 * service sending to it socket, and logs the message in to 
 * the log file using SLF4J/LOG4J. 
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
	 * MessageLogger Constructor 
	 * 
	 * @param context zeroMQ context 
	 * @param logFileURL URL of the log file, e.g. /var/log/zeroMQcore/messaging.log 
	 */
	public MessageLogger() {
		setDaemon(true);
		context = ZMQ.context(1);
		logger = context.socket( ZMQ.SUB);
		logger.subscribe( TOPIC.getBytes());
		logger.bind( socketURL);
	}
	
	public void run()
	{
		while (!Thread.currentThread().isInterrupted()) {
			System.out.println( "MessageLogging waiting in run()");
			String topicAndMessage = logger.recvStr(); 
			System.out.println( "MessageLogging received topic and message: " + topicAndMessage);
			String message = topicAndMessage.replace( TOPIC_DELIMITATED, "");
			
			if (message.equals( "TERMINATE_LOGGER")) {
			//	run = false;
				System.out.println( "MessageLogging recieved TERMINATE_LOGGER");
				break;
			}
			else {
				System.out.println( "MessageLogging recieved " + message);
			}
		}	
		System.out.println( "MessageLogging after while loop -> Close logger socket and terminate context. ");
		
		logger.close();
		context.close();
	}
	
	/**
	 * Main for MessageLogger
	 * @param args args[] is the URL of the log file, e.g. 
	 */
	public static void main( String[] args) throws Exception {
		
		MessageLogger logger2 = new MessageLogger();
		logger2.start();
		sleep(2000);
		System.out.println( "MessageLogger started");

		Context context2 = ZMQ.context(1);
		ZMQ.Socket pub2Logger = context2.socket( ZMQ.PUB); 
		pub2Logger.connect( "tcp://localhost:5555"); 
		sleep(2000);
		System.out.println( "main after sleep and before sent");
		pub2Logger.send( TOPIC_DELIMITATED + "test message", 0);
		System.out.println( "main between sents");
		pub2Logger.send( TOPIC_DELIMITATED + "TERMINATE_LOGGER", 0);
		
		sleep(500);
		pub2Logger.close();
		context2.close();
	}
	
}
