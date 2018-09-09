package com.testlims.helloService;

import org.json.JSONException;
import org.json.JSONObject;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;

/**
 * Implementation of a hello service using zeroMQ/jeroMQ REP (Response), where the constructor 
 * creates the HelloService with a response (REP) and a publisher to the logger (PUB) sockets. 
 * <p>
 * The hello service has two modes of handling requests. 
 <ol>
   <li>JSON Object:  The request JSON object supplies both the request ID, and the request type.  
   The two request types that are current supported are: 
     <ol>
       <li>sendHTML
         <pre>{
  "requestId":    "anId", 
  "requestType":  "sendHTML"
} </pre>
	   </li>
       <li>sayHello
         <pre>{
  "requestId":    "anId", 
  "requestType":  "sayHello",
  "name":         "a Name"
} </pre>
	   </li> 
     </ol>
   Both JSON request return a JSON Response that, like the JSON request, contains the request ID and the request type,   
   plus the specific response for the request type.  Show below is the response JSON for sayHello request type, 
   where the supplied name was Tess.  
   <pre>{
  "requestId":    "anId", 
  "requestType":  "sayHello", 
  "response":     "Hello Tess"
} </pre> 
   </li>
   <li>Text:  The only text string that is currently supported is TERMINATE_HELLO_SERVICE, 
   which closing Hello service and logger sockets and terminate context.</li>
 </ol>
 * 
 * @author Marc Whitlow, Colabrativ, Inc. 
 */
public class HelloService extends Thread {
	private Context 	context					= null; 
	private ZMQ.Socket 	service					= null; 
	private ZMQ.Socket 	pub2Logger				= null; 
//	private String		loggerTopicDelimitated	= null;
	private String		loggerTopicAndClassName	= null;
	
	/**
	 * HelloService Constructor 
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
	//	loggerTopicDelimitated = loggerTopic + " ";
		loggerTopicAndClassName = loggerTopic + " HelloService";
		
		service = context.socket( ZMQ.REP);
		service.bind( socketURL);
		try {
			Thread.sleep( 20);
		} catch (InterruptedException e) {
			System.err.println( "HelloService InterruptedException Thread.sleep( 20)");
		}
		pub2Logger.send( loggerTopicAndClassName + " started", 0);
	}
	
	/** 
	 * Run the Hello Service. 
	 * <p>
	 * If the service receives a TERMINATE_HELLO_SERVICE request, 
	 * then the service and publisher to logger are closed and the context terminated. 
	 */
	public void run()
	{
		while (!Thread.currentThread().isInterrupted()) {
			String request = service.recvStr(); 
			
            // Create a responses
			if (request.contains( "TERMINATE_HELLO_SERVICE")) {
				pub2Logger.send( loggerTopicAndClassName + " request: TERMINATE_HELLO_SERVICE", 0);
				service.send( "HelloService being terminated".getBytes(), 0);
				break;
			}
			else {
				String logRequestMessage  = "HelloService:";
				String logResponseMessage = "HelloService:";
				
				try {
					JSONObject requestJSON = new JSONObject( request);
					String requestId	= requestJSON.getString( "requestId");
					String requestType	= requestJSON.getString( "requestType");
					logRequestMessage	+= requestId + ":" + requestType + ".request";
					logResponseMessage	+= requestId + ":" + requestType + ".response";
					
					if (requestType.equals( "sayHello")) { 
						SayHelloResponse serviceRepsonse = new SayHelloResponse( service, pub2Logger, loggerTopicAndClassName);
						serviceRepsonse.send( requestJSON, logRequestMessage, logResponseMessage);
					}
					else if (requestType.equals( "sendHTML")) {
						SendHTMLResponse serviceRepsonse = new SendHTMLResponse( service, pub2Logger, loggerTopicAndClassName);
						serviceRepsonse.send( requestJSON, logRequestMessage, logResponseMessage);
					}
				}
				catch (JSONException e) {
					logRequestMessage	= logRequestMessage + " JSON Issue in " + request;
					logResponseMessage	= logResponseMessage + " JSON Issue, see request above.";
				}
			}
		}
		
		pub2Logger.send( (loggerTopicAndClassName + " closing service and logger sockets and terminate context."), 0);
		service.close();
        pub2Logger.close();
		context.close();
	}
	
	/**
	 * Main for HelloService1 that creates a REP instance and starts the Hello service. 
	 * 
	 * @param args The following arguments are required to start message logger:  <br>
	 * args[0]:  The URL that the service will be bound to, e.g. tcp://127.0.0.1:5557 <br>
	 * args[1]:  The URL that the logger will be bound to, e.g. tcp://127.0.0.1:5555  <br>
	 * args[2]:  The topic used by the logger, e.g. Project_Log. 
	 */
	public static void main( String[] args) { 
		HelloService helloService = new HelloService( args[0], args[1], args[2]);
		helloService.run();
	}
	
}
