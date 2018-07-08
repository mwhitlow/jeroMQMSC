package com.testlims.zeroMQcore;

import org.json.JSONObject;

import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;

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
public class MockHTTPzeroMQ {

	private Context 	context 	= null; 
	private Socket 		pub2Logger	= null; 
	
	private static final MockHTTPzeroMQ instance = new MockHTTPzeroMQ();
	
	// Private constructor to avoid client applications to use constructor
    private MockHTTPzeroMQ(){}

    public static MockHTTPzeroMQ getInstance() {
        return instance;
    }
	
	/**
	 * MockHTTPzeroMQ Constructor 
	 * 
	 * @param requestId the request Id of the mock HTTP request. 
	 * @param requestJSON the request JSON object, containing the request type 
	 * and other information associated with the request. 
	 * 
	 * @throws InterruptedException 
	 */
	public void createPublisher() {
		
		// ___________ Prepare our context and sockets ___________ 
		context = ZMQ.context(1);
		pub2Logger = context.socket( ZMQ.PUB);  // .socket( ZMQ.PUB);
		String socketURL = "tcp://127.0.0.1:5555";
	//	pub2Logger.bind( socketURL);
		pub2Logger.connect( socketURL); 
	/*	try {
			Thread.sleep( 1);
		} catch (InterruptedException e) {
			System.out.println( StackTrace.asString( "MockHTTPzeroMQ:Socket Sleep Exception", e));
		}
	 */
		pub2Logger.send( ( "MockHTTPzeroMQ:Socket pub2Logger connected to socket " + socketURL).getBytes());
		System.out.println( "MockHTTPzeroMQ:Socket pub2Logger connected to socket " + socketURL);
	}

	/**
	 * Process request. 
	 * 
	 * @param requestId the request Id of the mock HTTP request. 
	 * @param requestJSON the request JSON object, containing the request type 
	 * and other information associated with the request. 
	 */
	public void requestRecieved( String requestId, JSONObject requestJSON) {
		
		// ___________________ Log the Request ___________________ 
		String receivedMessage = null;
		if (requestJSON == null) {
			receivedMessage = requestId;
		}
		else {
			receivedMessage = "RID:" + requestId + " requestType: " + requestJSON.get( "requestType") + " received.";
		}
		pub2Logger.send( receivedMessage.getBytes());
		System.out.println( "MockHTTPzeroMQ:Message sent to pub2Logger: " + receivedMessage);
		
		// _______________ Send Request to Broker ________________ 
		//	TODO: Create Broker & Send Request to Broker
		//	Socket req2Broker = context.socket( ZMQ.PUB);
		
		// __________________ Log the Response ___________________ 
		//	TODO: Log response to logger.
		//	String responseMessage = "RID:" + requestId + " Request type " + requestJSON.get( "rquestType") + " response: ";
		//	pub2Logger.send( responseMessage, 0);
		
	}
	
	/** Close Socket  */
	public void closePublisher() { 
		pub2Logger.close();
		context.term();
	}
}
