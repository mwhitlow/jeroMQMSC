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
			displayHelloServiceHTML( responseJSON);
		}
	};
	xhttp.send( '{\"requestType\":\"sendHTML\"}');
}

function displayHelloServiceHTML(json) {	
	var helloServiceHTML = json.html;
//	TODO:  Remove alert	
	alert( "helloServiceHTML: " + helloServiceHTML);
	document.getElementById( "hello__service").innerHTML = helloServiceHTML;  
//	TODO:  Check response JSON element.
}
