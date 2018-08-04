package com.testlims.zeroMQcore;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

import org.json.JSONObject;

import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;

/**
 * MockHTTPzeroMQ is a mock of HTTP POST request being sent out as a zeroMQ message.  
 * Normally the javax.servlet.http.HTTPServlet would assign the next requestId, and 
 * forward the request JSON object.  For testing both of these are supplied in the 
 * constructor.  The mock run the following step: 
 <ol>
   <li>Logs the request Id and service being requested.</li>
   <li>Send out the request to the zeroMQ broker.</li>
   <li>Logs the request Id and returning response from the zeroMQ broker.</li>
 </ol>
 *
 * In order to run a MockHTTPzeroMQ the MessageLogger need to be running. 
 *
 * @author Marc Whitlow, Colabrativ, Inc. 
 */
public class MockHTTPzeroMQ extends Thread {

	private Context 	context					= null; 
	private ZMQ.Socket 	pub2Logger				= null; 
	private ZMQ.Socket 	reqHelloService			= null; 
	private	String		loggerTopicDelimitated	= null;
	private int 		requestId 				= 0; 
	
	/**
	 * MockHTTPzeroMQ Constructor 
	 * 
	 * @param socketURL The URL that the zeroMQ socket will be bound to. 
	 * @param loggerURL The URL of the logger.
	 * @param loggerTopic the logger topic, e.g. Project_Log. 
	 */
	public MockHTTPzeroMQ(String socketURL, String loggerURL, String loggerTopic) {
		setDaemon(true);
		context = ZMQ.context(1); 
		
		pub2Logger = context.socket( ZMQ.PUB);
		pub2Logger.connect( loggerURL); 
		loggerTopicDelimitated = loggerTopic + " ";
		pub2Logger.send( ( "MockHTTPzeroMQ:PUB socket to MessageLogger connected to " + loggerURL).getBytes());
			
		reqHelloService = context.socket( ZMQ.REQ);
		reqHelloService.connect( socketURL);
		pub2Logger.send( ( "MockHTTPzeroMQ:REQ socket to HelloService connected to " + socketURL).getBytes());
	}
	
	/**
	 * Process POST request. 
	 * 
	 * @param requestId the request Id of the mock HTTP request. 
	 * @param requestJSON the request JSON object, containing the request type 
	 * and other information associated with the request. 
	 * 
	 * @throws IOException if there is an issue reading the request. 
	 */
	protected void doPost(MockHttpServletRequest request, MockHttpServletResponse response) throws IOException {
		Integer httpStatusCode = 200;

		requestId++;
		String requestType = null;
		// ___________________ Readout Request ___________________ 
		String line = null;
		StringBuffer jsonBuffer = new StringBuffer();
		BufferedReader reader = request.getReader();
		while ((line = reader.readLine()) != null) {
			jsonBuffer.append( line);
		} 
		
		try {	
			JSONObject requestJSON = new JSONObject( jsonBuffer.toString());
			
			// ___________________ Log the Request ___________________ 
			requestType = requestJSON.getString( "requestType");
			pub2Logger.send( (loggerTopicDelimitated + "MockHTTPzeroMQ doPost:" + requestId + ":" + requestType + ".request").getBytes());		
			
			//                 
			// ___________ Send Request to HelloServices _____________ 
			requestJSON.put( "requestId", String.valueOf( requestId));
			reqHelloService.send( requestJSON.toString().getBytes(), 0);
		}
		catch(Exception e) {
			//             Failed to process as JSON Object
			// ______________ Log the Request as String ______________ 
			requestType = jsonBuffer.toString();
			pub2Logger.send( (loggerTopicDelimitated + "MockHTTPzeroMQ doPost:" + requestId + ":" + requestType + ".request").getBytes());		
			
			// _______________ Send Request to Broker ________________ 
			reqHelloService.send( requestType.getBytes(), 0);
		}
		
		// __________________ Log the Response ___________________ 
		response.setStatus( httpStatusCode);
		PrintWriter writer = response.getWriter();
		String reply = reqHelloService.recvStr();
		try {
			JSONObject responseJSON = new JSONObject( reply);
			response.setContentType( "application/json; charset=utf-8");
			writer.println( responseJSON.toString());
		}
		catch(Exception e) {
			//             Failed to process as JSON Object
			// ______________ Log the Request as String ______________ 
			response.setContentType( "application/text; charset=utf-8");
			writer.println( reply);
		}
		pub2Logger.send( (loggerTopicDelimitated + "MockHTTPzeroMQ doPost:" + requestId + ":" + requestType + ".response").getBytes());
	}
	
	/** Close sockets and context  */
	public void closeAndTerminate() { 
		pub2Logger.send( loggerTopicDelimitated + "MockHTTPzeroMQ request: close and terminate", 0);
		pub2Logger.close();
		reqHelloService.close();
		context.term();
	}
}
