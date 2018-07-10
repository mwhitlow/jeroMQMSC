package com.testlims.zeroMQcore;

import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;

/**
 * Implementation of a hello service using zeroMQ/jeroMQ REP (Response), where the constructor 
 * creates the HelloService. 
 *
 * @author Marc Whitlow, Colabrativ, Inc. 
 */
public class HelloService extends Thread {
	private Context 	context	= null; 
	private ZMQ.Socket 	service	= null; 
	
	/**
	 * MessageLogger Constructor 
	 * 
	 * @param socketURL The URL that the service will be bound to. 
	 */
	public HelloService(String socketURL) {
		setDaemon(true);
		context = ZMQ.context(1);
		service = context.socket( ZMQ.REP);
		service.bind( socketURL);
	}
	
	/** 
	 * Run the Hello Service. 
	 * <p>
	 * If the service receives a TERMINATE_HELLO_SERVICE request, 
	 * then the service is closed and the context terminated. 
	 */
	public void run()
	{
		while (!Thread.currentThread().isInterrupted()) {
			System.out.println( "HelloService waiting in run()");
			String request = service.recvStr(); 
			System.out.println( "HelloService received request: " + request);
            
            // Create a responses
			
			if (request.contains( "TERMINATE_HELLO_SERVICE")) {
			//	run = false;
				System.out.println( "HelloService received TERMINATE_HELLO_SERVICE");
				service.send( "HelloService being terminated".getBytes(), 0);
				break;
			}
			else {
				String response = "Hello " + request;
				System.out.println( "HelloService response sent " + response);
				service.send( response.getBytes(), 0);
			}
		}	
		System.out.println( "HelloService after while loop -> Close logger socket and terminate context. ");
		
		service.close();
		context.close();
	}
	
	/**
	 * Main for HelloService1 that creates a REP instance and starts the Hello service. 
	 * 
	 * @param args The following arguments are required to start message logger:  
	 * args[0]:  The URL that the service will be bound to, e.g. tcp://127.0.0.1:5557 
	 * 
	 * @throws Exception if there is an issue start, running or shutting down the HelloService1. 
	 */
	public static void main( String[] args) throws Exception {
		
		HelloService helloService = new HelloService(args[0]);
		helloService.start();
		System.out.println( "HelloService started");
	}
	
}
