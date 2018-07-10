package com.testlims.zeroMQcore;

import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;

/**
 * The second implementation of zeroMQ/jeroMQ REP (Response) hello service a where the constructor 
 * creates the HelloService and sends it a set of requests.  
 * This implementation was based on the Request-Reply example Chapter 1 of the ØMQ - The Guide, 
 * and HwClient.java and HwServer.java. 
 *
 * @author Marc Whitlow, Colabrativ, Inc. 
 */
public class HelloService1 extends Thread {
	static final String socketURL = "tcp://localhost:5557"; 
	ZMQ.Socket service  = null;
	Context context = null;
	
	/**
	 * MessageLogger Constructor  */
	public HelloService1() {
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
	 * @param args No arguments are required.
	 * 
	 * @throws Exception if there is an issue start, running or shutting down the HelloService1. 
	 */
	public static void main( String[] args) throws Exception {
		
		HelloService1 helloService = new HelloService1();
		helloService.start();
		System.out.println( "HelloService started");
	}
	
}
