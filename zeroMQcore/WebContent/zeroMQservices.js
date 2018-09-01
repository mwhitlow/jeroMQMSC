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
			displayHelloServiceHTML( responseJSON, "#hello__service");
		}
	};
	xhttp.send( '{\"requestType\":\"sendHTML\"}');
}

function displayHelloServiceHTML(json, helloServiceElementId) {	
//	TODO:  Remove alert	
	alert( JSON.stringify( json, null, 2));
	document.getElementById( helloServiceElementId).value = json.html;  // TODO:  Check response JSON element.
}
