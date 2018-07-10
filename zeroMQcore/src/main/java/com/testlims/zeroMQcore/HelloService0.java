package com.testlims.zeroMQcore;

import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;

/**
 * Initial implementation of zeroMQ/jeroMQ REP (Response) hello service a where the constructor 
 * creates the HelloService and sends it a set of requests.  
 * This implementation was based on the Request-Reply example Chapter 1 of the ØMQ - The Guide, 
 * and HwClient.java and HwServer.java. 
 * <p>
 * To test the system the main creates a HelloService instance and sends it the requests. 
 *
 * @author Marc Whitlow, Colabrativ, Inc. 
 */
public class HelloService0 extends Thread {
	static final String socketURL = "tcp://localhost:5557"; 
	ZMQ.Socket service  = null;
	Context context = null;
	
	/**
	 * MessageLogger Constructor  */
	public HelloService0() {
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
	 * Main for HelloService0 that creates a PUB instance that send two messages. 
	<ol>
	  <li>request</li>
	  <li>terminate request</li>
	</ol>
	 * 
	 * The output look as follows: 
	 <pre>HelloService started
HelloService waiting in run()
RequestClient sending requestId 0 requestString Tess
HelloService received request: Tess
HelloService response sent Hello Tess
HelloService waiting in run()
RequestClient received requestId 0 reply: Hello Tess

RequestClient sending requestId 1 requestString TERMINATE_HELLO_SERVICE
HelloService received request: TERMINATE_HELLO_SERVICE
HelloService received TERMINATE_HELLO_SERVICE
HelloService after while loop -&gt; Close logger socket and terminate context. 
RequestClient received requestId 1 reply: HelloService being terminated
	 </pre>
	 *
	 * @param args No arguments are required.
	 * 
	 * @throws Exception if there is an issue start, running or shutting down the HelloService0. 
	 */
	public static void main( String[] args) throws Exception {
		
		HelloService1 helloService = new HelloService1();
		helloService.start();
		System.out.println( "HelloService started");
		sleep(100);

		Context clientContext = ZMQ.context(1);
		ZMQ.Socket requestClient = clientContext.socket( ZMQ.REQ); 
		requestClient.connect( socketURL); 
		
		sleep(100);
		int requestId = 0;
		String requestString = "Tess";
		System.out.println( "RequestClient sending requestId " + requestId + " requestString " + requestString);
		requestClient.send( requestString.getBytes(), 0);
		String reply = requestClient.recvStr();
        System.out.println( "RequestClient received requestId " + requestId + " reply: " + reply + "\n");
		
		requestId++;
		System.out.println(  "RequestClient sending requestId " + requestId + " requestString TERMINATE_HELLO_SERVICE");
		requestClient.send( ("TERMINATE_HELLO_SERVICE").getBytes(), 0);
		reply = requestClient.recvStr();
        System.out.println( "RequestClient received requestId " + requestId + " reply: " + reply);
		
		sleep(100);
		requestClient.close();
		clientContext.close();
	}
	
}
