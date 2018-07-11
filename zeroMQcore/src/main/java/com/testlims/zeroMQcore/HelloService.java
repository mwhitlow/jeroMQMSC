package com.testlims.zeroMQcore;

import org.json.JSONException;
import org.json.JSONObject;
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
			
            // Create a responses
			if (request.contains( "TERMINATE_HELLO_SERVICE")) {
				pub2Logger.send( loggerTopicDelimitated + "HelloService request: TERMINATE_HELLO_SERVICE", 0);
				service.send( "HelloService being terminated".getBytes(), 0);
				break;
			}
			else {
				String logRequestMessage  = "HelloService:";
				String logResponseMessage = "HelloService:";
				JSONObject responseJSON = new JSONObject();
				
				try {
					JSONObject requestJSON = new JSONObject( request);
					String requestId	= requestJSON.getString( "requestId");
					String requestType	= requestJSON.getString( "requestType");
					logRequestMessage	+= requestId + ":" + requestType + ".request:";
					logResponseMessage	+= requestId + ":" + requestType + ".response:";
					
					if (requestType.equals( "sayHello") && requestJSON.has( "name")) { 
						String name = requestJSON.getString( "name");
						logRequestMessage = logRequestMessage + name;
						
						String responseText = "Hello " + name;
						responseJSON.put( "requestId",		requestId);
						responseJSON.put( "requestType",	requestType);
						responseJSON.put( "response", 		responseText);
						
						logResponseMessage += responseText;
					}
				}
				catch (JSONException e) {
					logRequestMessage	= logRequestMessage + " JSON Issue in " + request;
					logResponseMessage	= logResponseMessage + " JSON Issue, see request above.";
				}
				
				pub2Logger.send( loggerTopicDelimitated + logRequestMessage, 0);
				service.send( responseJSON.toString().getBytes(), 0);
				pub2Logger.send( loggerTopicDelimitated + logResponseMessage, 0);
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
