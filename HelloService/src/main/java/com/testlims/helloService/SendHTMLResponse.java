package com.testlims.helloService;

import org.json.JSONException;
import org.json.JSONObject;
import org.zeromq.ZMQ.Socket;

/** 
 * HelloService sendHTML repsonse.  Handles a JSON request  
 <pre>{
  "requestId":   "anId", 
  "requestType": "sendHTML"
} </pre>
 * <p>the response is returned a JSON Object containing the HTML and a javascript. 
 <pre>{
  "requestId":   "anId", 
  "requestType": "sendHTML", 
  "html": "<form class=\"helloForm\"> . . . </form>",
  "script": "function helloService_sayHello() { . . . "
} </pre> 
 *  
 * @author Marc Whitlow
 */
public class SendHTMLResponse {

	private Socket helloService				= null;
	private Socket pub2Logger				= null;
	private String loggerTopicDelimitated	= null;
	
	/**
	 * SendHTMLService Constructor 
	 * 
	 * @param helloService seroMQ response socket. 
	 * @param pub2Logger zeroMQ pub socket to the message logger. 
	 * @param loggerTopicDelimitated logger topic with delimiter, required for all publications to the logger. 
	 */
	public SendHTMLResponse(Socket helloService, Socket pub2Logger, String loggerTopicDelimitated) {
		this.helloService 			= helloService;
		this.pub2Logger				= pub2Logger;
		this.loggerTopicDelimitated	= loggerTopicDelimitated;
	}

	public void send(JSONObject requestJSON, String logRequestMessage, String logResponseMessage) throws JSONException {
		String requestId	= requestJSON.getString( "requestId");
		String requestType	= requestJSON.getString( "requestType");
		
		pub2Logger.send( loggerTopicDelimitated + logRequestMessage, 0);
		
		StringBuilder html = new StringBuilder();
		html.append("<form class=\"helloForm\">Name: <input id=\"hello__service-name\" type=\"text\" name=\"name\" />" + 
					"  <input type=\"button\" value=\"Submit\" onclick=\"helloService_sayHello()\" />" +
					"</form>" + 
					"<div>Response: <span id=\"hello__service-sayHello\"></span></div>");
		StringBuilder sayHelloScript = new StringBuilder();
		sayHelloScript.append(
					"function helloService_sayHello() { " + 
					"	var xhttp = new XMLHttpRequest();" + 
					"	xhttp.open( 'POST', zeroMQcoreURL + \"/services\", true);" + 
					"	xhttp.setRequestHeader( 'Content-type', 'application/json');" + 
					"	xhttp.onload = function() {" + 
					"		if (this.readyState == 4 && this.status == 200) {" + 
					"			var responseJSON = JSON.parse( xhttp.responseText);" + 
					"			var helloName = responseJSON.response;" + 
					"			document.getElementById( \"hello__service-sayHello\").innerHTML = helloName;" + 
					"		}" + 
					"	};" + 
					"	var name = document.getElementById( \"hello__service-name\").value;" + 
					"	var requestJSON = '{\"requestType\":\"sayHello\",\"name\":\"' + name + '\"}';" + 
					"	xhttp.send( requestJSON);" +
					"}" );
		JSONObject responseJSON = new JSONObject();
		responseJSON.put( "requestId",		requestId);
		responseJSON.put( "requestType",	requestType);
		responseJSON.put( "html", 			html.toString());
		responseJSON.put( "script", 		sayHelloScript.toString());
		helloService.send( responseJSON.toString().getBytes(), 0);

		pub2Logger.send( loggerTopicDelimitated + logResponseMessage, 0);
	}

}
