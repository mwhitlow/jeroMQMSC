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
		}
	};
	xhttp.send( '{\"requestType\":\"sendHTML\"}');
}

function helloService_sayHello() { 
	var xhttp = new XMLHttpRequest();
	xhttp.open( 'POST', zeroMQcoreURL + "/services", true);
	xhttp.setRequestHeader( 'Content-type', 'application/json');
	xhttp.onload = function() {
		if (this.readyState == 4 && this.status == 200) {
			var responseJSON = JSON.parse( xhttp.responseText);
			var helloName = responseJSON.response;
			document.getElementById( "hello__service-sayHello").innerHTML = helloName;  
		}
	};
	var name = document.getElementById( "hello__service-name").value;
	var requestJSON = '{\"requestType\":\"sayHello\",\"name\":\"' + name + '\"}';
	xhttp.send( requestJSON);
}
