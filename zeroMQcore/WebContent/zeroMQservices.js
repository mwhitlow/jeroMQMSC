/** Determine the zeroMQcore URL.  */
var zeroMQcoreURL;
if (window.location.hostname == "localhost")
	zeroMQcoreURL = window.location.protocol + "//" + window.location.hostname + ":" + 
					 window.location.port + "/zeroMQcore";
else
	zeroMQcoreURL = window.location.protocol + "//" + window.location.hostname + "/zeroMQcore";


function displayHelloService() { 
	var xhttp = new XMLHttpRequest();
	xhttp.open( 'POST', zeroMQcoreURL + "/services", true);
	xhttp.setRequestHeader( 'Content-type', 'application/json');
	xhttp.onload = function() {
		if (this.readyState == 4 && this.status == 200) {
			var responseJSON = JSON.parse( xhttp.responseText);
			document.getElementById( "hello__service").innerHTML = responseJSON.html; 
			
			if (responseJSON.script) {
				var script = document.createElement( "script");
				script.type = "text/javascript";
				script.appendChild( document.createTextNode( responseJSON.script));
				document.body.appendChild( script);
			} 
		}
	};
	xhttp.send( '{\"requestType\":\"sendHTML\"}');
}
