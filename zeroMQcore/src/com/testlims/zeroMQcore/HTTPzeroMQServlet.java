package com.testlims.zeroMQcore;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONObject;

import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;


/**
 * HTTPzeroMQServlet handles HTTP POST requests, and sends them to zeroMQ request (REQ)
 * socket.  The servlet runs the following step: 
 <ol>
   <li>Assigns the requestId each request, and adds it to the request JSON object.</li>
   <li>Logs the request Id and service being requested.</li>
   <li>Send out the request to the zeroMQ broker.</li>
   <li>Logs the request Id and returning response from the zeroMQ broker.</li>
 </ol>
 *
 * In order to run the servlet both the MessageLogger and the Request/Response zeroMQ modes 
 * need to be running. 
 *
 * @author Marc Whitlow, Colabrativ, Inc. 
 */
@WebServlet(
		description = "HTTP Servlet that connects to zeroMQ MessageLogger and HelloService services", 
		urlPatterns = { "/services" })
public class HTTPzeroMQServlet extends HttpServlet {

	private static final long serialVersionUID = 8363249898405266358L;
	
	private Context 	context					= null; 
	private ZMQ.Socket 	pub2Logger				= null; 
	private ZMQ.Socket 	reqHelloService			= null; 
	private	String		loggerTopicDelimitated	= null;
	private int 		requestId 				= 0; 
	
	/**
	 * HTTPzeroMQServlet Constructor  */
	public HTTPzeroMQServlet() {
		super();
		
	//	TODO: The loggerURL, loggerTopic, and helloServiceURL should be read in from a properties file. 
		String loggerURL		= "tcp://localhost:5556"; 
		String loggerTopic		= "Project_Log"; 
		String helloServiceURL	= "tcp://localhost:5557";  
		context = ZMQ.context(1); 
		
		pub2Logger = context.socket( ZMQ.PUB);
		pub2Logger.connect( loggerURL); 
		loggerTopicDelimitated = loggerTopic + " ";
			
		reqHelloService = context.socket( ZMQ.REQ);
		reqHelloService.connect( helloServiceURL);
		
		try {
			Thread.sleep( 20);
			pub2Logger.send( ( "HTTPzeroMQServlet:PUB socket to MessageLogger connected to " + loggerURL).getBytes());
			pub2Logger.send( ( "HTTPzeroMQServlet:REQ socket to HelloService connected to " + helloServiceURL).getBytes());
			
		} catch (InterruptedException e) {
			System.err.println( "HTTPzeroMQServlet InterruptedException Thread.sleep( 50)");
		}
	}
	
	/**
	 * Process POST request. 
	 * 
	 * @param request the request should contain a JSON object containing the details of the request. 
	 * @param response 
	 * 
	 * @throws IOException if there is an issue reading the request. 
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
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
			pub2Logger.send( (loggerTopicDelimitated + "HTTPzeroMQServlet doPost:" + requestId + ":" + requestType + ".request").getBytes());		
			
			//                 Send Request to HelloServices
			// _______________ Send Request to Broker ________________ 
			requestJSON.put( "requestId", String.valueOf( requestId));
			reqHelloService.send( requestJSON.toString().getBytes(), 0);
		}
		catch(Exception e) { 
			//             Failed to process as JSON Object
			// ______________ Log the Request as String ______________ 
			requestType = jsonBuffer.toString();
			pub2Logger.send( (loggerTopicDelimitated + "HTTPzeroMQServlet doPost:" + requestId + ":" + requestType + ".request").getBytes());		
			
			// _______________ Send Request to Broker ________________ 
			reqHelloService.send( requestType.getBytes(), 0);
		}
		
		// __________________ Log the Response ___________________ 
		String reply = reqHelloService.recvStr();
		response.setStatus( httpStatusCode);
		PrintWriter writer = response.getWriter();
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
		pub2Logger.send( (loggerTopicDelimitated + "HTTPzeroMQServlet doPost:" + requestId + ":" + requestType + ".response").getBytes());
	}
	
	/** Close publisher to the logger and the request/response and terminate the zero MQ context.  */
	public void closeAndTerminate() { 
		pub2Logger.send( loggerTopicDelimitated + "HTTPzeroMQServlet request: close publisher to the logger and the request/response sockets," + 
						" and terminate the zero MQ context.", 0);
		pub2Logger.close();
		reqHelloService.close();
		context.term();
	}
}
