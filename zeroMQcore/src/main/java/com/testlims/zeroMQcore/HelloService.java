package com.testlims.zeroMQcore;

import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;

/**
 * Implementation of a hello service using zeroMQ/jeroMQ REP (Response), where the constructor 
 * creates the HelloService with both a response (REP) and logger publisher (PUB) sockets. 
 *
 * @author Marc Whitlow, Colabrativ, Inc. 
 */
public class HelloService extends Thread {
	private Context 	context					= null; 
	private ZMQ.Socket 	service					= null; 
	private ZMQ.Socket 	pub2Logger				= null; 
	private String		loggerTopicDelimitated	= null;
	
	/**
	 * MessageLogger Constructor 
	 * 
	 * @param socketURL The URL that the service will be bound to. 
	 * @param loggerURL The URL of the logger.
	 * @param loggerTopic the logger topic, e.g. Project_Log. 
	 */
	public HelloService(String socketURL, String loggerURL, String loggerTopic) {
		setDaemon(true);
		context = ZMQ.context(1);
		
		pub2Logger = context.socket( ZMQ.PUB);
		pub2Logger.connect( loggerURL); 
		this.loggerTopicDelimitated = loggerTopic + " ";
		
		service = context.socket( ZMQ.REP);
		service.bind( socketURL);
	}
	
	/** 
	 * Run the Hello Service. 
	 * <p>
	 * If the service receives a TERMINATE_HELLO_SERVICE request, 
	 * then the service and published to logger are closed and the context terminated. 
	 */
	public void run()
	{
		while (!Thread.currentThread().isInterrupted()) {
			String request = service.recvStr(); 
			String logMessage = "HelloService received request: " + request;
			pub2Logger.send( loggerTopicDelimitated + logMessage, 0);
            
            // Create a responses
			if (request.contains( "TERMINATE_HELLO_SERVICE")) {
				service.send( "HelloService being terminated".getBytes(), 0);
				break;
			}
			else {
				String response = "Hello " + request;
				pub2Logger.send( (loggerTopicDelimitated + "HelloService response sent " + response), 0);
				service.send( response.getBytes(), 0);
			}
		}
		
		pub2Logger.send( (loggerTopicDelimitated + "HelloService closing service and logger sockets and terminate context."), 0);
		service.close();
        pub2Logger.close();
		context.close();
	}
	
	/**
	 * Main for HelloService1 that creates a REP instance and starts the Hello service. 
	 * 
	 * @param args The following arguments are required to start message logger:  
	 * args[0]:  The URL that the service will be bound to, e.g. tcp://127.0.0.1:5557 
	 * args[1]:  The URL that the logger will be bound to, e.g. tcp://127.0.0.1:5556 
	 * args[2]:  The topic used by the logger, e.g. Project_Log. 
	 * 
	 * @throws Exception if there is an issue start, running or shutting down the HelloService1. 
	 */
	public static void main( String[] args) throws Exception {
		
		HelloService helloService = new HelloService( args[0], args[1], args[2]);
		helloService.start();
	}
	
}
