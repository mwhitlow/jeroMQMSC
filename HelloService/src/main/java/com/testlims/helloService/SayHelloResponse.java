package com.testlims.helloService;

import org.json.JSONException;
import org.json.JSONObject;
import org.zeromq.ZMQ.Socket;

/** 
 * HelloService sayHello service.  Handles a JSON request  
 <pre>{
  "requestId":    "anId", 
  "requestType":  "sayHello",
  "name":         "a Name"
} </pre>
 * <p>with a JSON Response 
 <pre>{
  "requestId":    "anId", 
  "requestType":  "sayHello", 
  "response":     "Hello Tess"
} </pre> 
 *  
 * @author Marc Whitlow
 */
public class SayHelloResponse {

	private Socket helloService				= null;
	private Socket pub2Logger				= null;
	private String loggerTopicDelimitated	= null;
	
	/**
	 * SayHelloService Constructor 
	 * 
	 * @param helloService seroMQ response socket. 
	 * @param pub2Logger zeroMQ pub socket to the message logger. 
	 * @param loggerTopicDelimitated logger topic with delimiter, required for all publications to the logger. 
	 */
	public SayHelloResponse(Socket helloService, Socket pub2Logger, String loggerTopicDelimitated) {
		this.helloService 			= helloService;
		this.pub2Logger				= pub2Logger;
		this.loggerTopicDelimitated	= loggerTopicDelimitated;
	}

	public void send(JSONObject requestJSON, String logRequestMessage, String logResponseMessage) throws JSONException {
		String requestId	= requestJSON.getString( "requestId");
		String requestType	= requestJSON.getString( "requestType");
		String name 		= requestJSON.getString( "name");
		pub2Logger.send( loggerTopicDelimitated + logRequestMessage + ":" + name, 0);
		
		String responseText = "Hello " + name;
		JSONObject responseJSON = new JSONObject();
		responseJSON.put( "requestId",		requestId);
		responseJSON.put( "requestType",	requestType);
		responseJSON.put( "response", 		responseText);
		helloService.send( responseJSON.toString().getBytes(), 0);

		pub2Logger.send( loggerTopicDelimitated + logResponseMessage + ":" + responseText, 0);
	}

}
