/*
 * requires jquery
 * 
 * 		http://ajax.googleapis.com/ajax/libs/jquery/2.1.3/jquery.min.js
 */

/*
 * handleLaunch
 * 
 * 		This does the heavy lifting for OAuth authorization during SMART on FHIR app launch.
 * 
 * 		The SMART launchpad sends two parameters, which are required here: 
 * 			iss - the FHIR server URL
 * 			launch - a generated launch context ID
 * 
 * 		The SMART on FHIR app integration involves a registration step, during which a clientID 
 * 		is generated. That clientID is also a required parameter here. 
 * 
 * 		The SMART authorization service is auxilliary to the FHIR server itself. A FHIR client 
 * 		can discover the authorization service via FHIR server query of a conformance statement,
 *		where the response to this query includes a FHIR "oauth-uris" extension.
 * 
 * 		Per the SMART on FHIR OAuth procedure, successful authorization will result in a redirect
 * 		to another URL within this app for next steps. 
 */
function handleLaunch(clientId, serviceUri, launchContextId, authScope, sessionKey, redirectUri) {

    // FHIR Service Conformance Statement URL
    var conformanceUri = serviceUri + "/metadata"
    console.log("conformanceUri = "+conformanceUri)

    // Request the conformance statement from the SMART on FHIR API server and
    // find out the endpoint URLs for the authorization service
    $.get(conformanceUri, function(conformance, responseStatus, xhr) {

		console.log("conformance request status: "+xhr.statusText)

        var authUri, tokenUri;

		// <Conformance>
		//   ...
		//   <rest>
		//	   ...
		//	   <security>
		//		 ...
		//		 <extension url="http://fhir-registry.smarthealthit.org/StructureDefinition/oauth-uris">
		//         <extension url="authorize"><valueUri value="https://open-ic.epic.com/Argonaut/oauth2/authorize"/>
		//         <extension url="token"><valueUri value="https://open-ic.epic.com/Argonaut/oauth2/token"/>
		
        var smartExtension = conformance.rest[0].security.extension.filter(function (e) {
           return (e.url === "http://fhir-registry.smarthealthit.org/StructureDefinition/oauth-uris");
        });

        smartExtension[0].extension.forEach(function(arg, index, array){
          if (arg.url === "authorize") {
            authUri = arg.valueUri;
          } else if (arg.url === "token") {
            tokenUri = arg.valueUri;
          }
        });

        // retain a couple parameters in the session for later use
        sessionStorage[sessionKey] = JSON.stringify({
            clientId: clientId,
            launchContextId: launchContextId,
            serviceUri: serviceUri,
            redirectUri: redirectUri,
            tokenUri: tokenUri
        });

        //https://open-ic.epic.com/Argonaut/oauth2/authorize
        //	?response_type=code
        //	&client_id=44d7c35a-f6e7-4481-8b69-31d4d8017f03
        //	&redirect_uri=https%3A%2F%2Fdemo.careevolution.com%2FSMARTest%2FSMARTest.html
        //	&aud=https%3A%2F%2Fopen-ic.epic.com%2Fargonaut%2Fapi%2FFHIR%2FArgonaut%2F
        //	&scope=launch%2Fpatient%20patient%2F*.read
        	
        // finally, redirect the browser to the authorizatin server and pass the needed
        // parameters for the authorization request in the URL
        var nextLocation = authUri + "?" +
            "response_type=code&" +
            "client_id=" + encodeURIComponent(clientId) + "&" +
            "redirect_uri=" + encodeURIComponent(redirectUri) + "&" +
            "scope=" + encodeURIComponent(authScope) + "&" +
            "aud=" + encodeURIComponent(serviceUri) + "&" +
            "launch=" + launchContextId + "&" +
            "state=" + sessionKey;
        console.log("Redirecting to: "+nextLocation) 
        window.location.href = nextLocation;
    }, "json")
    .error(function(xhr) {
   		console.log("request failed for: "+conformanceUri+ " => "+xhr.statusText)
	});
}
    
/*
 * getAuthorizationToken
 * 
 * 		After successful authorization, the OAuth client requests an authorization code
 * 		which is then used as a "Bearer" token in subsequent requests to the FHIR server. 
 */
function getAuthorizationToken(sessionKey, authCode) {

    // load the app parameters stored in the session
    var params = JSON.parse(sessionStorage[sessionKey]);  // load app session
    var tokenServiceUri = params.tokenUri;
    var clientId = params.clientId;
    var launchContextId = params.launchContextId;
    var serviceUri = params.serviceUri;
    
    // Prep the token service request parameters
    var requestParameters = {
    	client_id: clientId,
        code: authCode,
        grant_type: 'authorization_code',
        redirect_uri: redirectUri
    };
    
    var requestOptions = {
        url: tokenServiceUri,
        type: 'POST',
        data: requestParameters
    };
    
    console.log(requestOptions);
    
    /* Obtain authorization token from the authorization service using the authorization code.
     * response will be of the form:
     *  {
     *     "access_token": ...,
     *     "token_type": "bearer",
     *     "expires_in": 3600,
     *     "state": "38789188",
     *     "patient": ...,
     *     "user": "ARGONAUT"
     *   }
     */
    $.ajax(requestOptions)
    	.done(function(res){
            var accessToken = res.access_token;
            var patientId = res.patient;
                    
            // Authorization is complete. Proceed with app, using accessToken in any FHIR
            // server requests. 
            var url = serviceUri + "/Patient/" + patientId;
            $.ajax({
                url: url,
                type: "GET",
                dataType: "json",
                headers: {
                    "Authorization": "Bearer " + accessToken
                },
            }).done(function(pt){
                var name = pt.name[0].given.join(" ") +" "+ pt.name[0].family.join(" ");
                document.body.innerHTML += "<h3>Patient: " + name + "</h3>";
            });
        })
        .error(function(xhr) {
   			console.log("request failed for: "+tokenServiceUri+ " => "+xhr.statusText)
		});
}
    
/*
 *  Convenience function for parsing of URL parameters
 *  based on http://www.jquerybyexample.net/2012/06/get-url-parameters-using-jquery.html
 */
function getUrlParameter(sParam)
{
    var sPageURL = window.location.search.substring(1);
    var sURLVariables = sPageURL.split('&');
    for (var i = 0; i < sURLVariables.length; i++)
    {
        var sParameterName = sURLVariables[i].split('=');
        if (sParameterName[0] == sParam) {
            var res = sParameterName[1].replace(/\+/g, '%20');
            return decodeURIComponent(res);
        }
    }
}
