package rht.samples.smart.one;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;


@Controller
public class AuthorizationController {

	static Logger logger = Logger.getLogger(AuthorizationController.class.getName());
	static final String TestServer = "https://open-ic.epic.com/Argonaut/api/FHIR/Argonaut"; 

	static final String SESSION_AUTH_ENDPOINTS_KEY = "authEndpoints";
	static final String SESSION_AUTH_SCOPE_KEY = "authScope";
	static final String SESSION_AUTH_CODE_KEY = "authScope";
	static final String SESSION_FHIR_SERVICE_URI_KEY = "fhirServiceURI";
	static final String SESSION_LAUNCH_CONTEXT_ID_KEY = "launchContextId";
	static final String SESSION_REDIRECT_URI_KEY = "redirect_uri";


	// Change this to the ID of the client that was registered with the SMART on FHIR authorization server.
	// Should be in a config file. 
    String clientId = "6c12dff4-24e7-4475-a742-b08972c4ea27";


	@GetMapping("/launch")
    public String launch(
    		HttpServletRequest clientRequest,
    		@RequestParam(name="launch", required=true) String launchContextId, 
    		@RequestParam(name="iss", required=true, defaultValue=TestServer) String iss, 
    		Model model) {

        logger.info("handling request: "+clientRequest.getRequestURL());
        logger.info("parameters:\n\tiss = "+ iss + "\n\tlaunch = " + launchContextId);
        
		URI serviceURI;
        try {
			serviceURI = new URI(iss);
		} catch (URISyntaxException e) {
			e.printStackTrace();
			return e.toString();
		}

        HttpSession session = initSession(clientRequest, serviceURI, launchContextId);
        
        requestAuthorization(session);
        
		//model.addAttribute("name", name);

        return "greeting";
    }
	
/*
	@GetMapping("/authorizationComplete")
    public String authorizationComplete(
    		@RequestParam(name="launch", required=true) String launchContextId, 
    		@RequestParam(name="iss", required=true, defaultValue=TestServer) String iss, 
    		Model model) {
*/
	
	private Map<String,String> getAuthorizationEndpoints(URI serviceUri) {
		
        // FHIR Service Conformance Statement URL
        URI conformanceUri;
		try {
			conformanceUri = new URI(serviceUri.toString()+"/metadata");
		} catch (URISyntaxException e) {
			e.printStackTrace();
			return null;
		}
        logger.info("FHIR server conformance uri = "+conformanceUri);

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        RequestEntity request = new RequestEntity(headers, HttpMethod.GET, conformanceUri);
        ResponseEntity<JsonNode> entity = restTemplate.exchange(request, JsonNode.class);
        
        MediaType contentType = entity.getHeaders().getContentType();
        HttpStatus requestStatus = entity.getStatusCode();
        
        logger.info("FHIR server conformance response:"
        		+" status = " +  requestStatus.getReasonPhrase()
        		+", content-type = " + contentType.toString());
        
        Map<String,String> endpoints = new HashMap<String,String>();
        JsonNode conformance = entity.getBody();
        JsonNode securityDecl = conformance.get("rest").get(0).get("security");
        JsonNode securityExtensions = securityDecl.get("extension");
        Iterator<JsonNode> iterator = securityExtensions.elements();
        while( iterator.hasNext() ) {
        	// find the FHIR extension element "oauth-uris"
        	JsonNode child = iterator.next();
        	if( child.get("url").asText().endsWith("oauth-uris") ) {
        			Iterator<JsonNode> urls = child.get("extension").elements();
        			while( urls.hasNext() ) {
        				JsonNode obj = urls.next();
        				endpoints.put(
        						obj.get("url").asText(), 
        						obj.get("valueUri").asText());
        			}
    				return endpoints;
        	}
        }
 
        return null;
	}

	private HttpSession initSession(HttpServletRequest launchRequest, URI fhirServiceURI, String launchContextId) {
		
        HttpSession session = launchRequest.getSession();
        
        Map<String,String> endpoints = getAuthorizationEndpoints(fhirServiceURI);
        session.setAttribute(SESSION_AUTH_ENDPOINTS_KEY, endpoints);
        
        // The scopes that the app will request from the authorization server
        // encoded in a space-separated string:
        //      1. permission to read all of the patient's record
        //      2. permission to launch the app in the specific context
        String scope = "patient/*.read launch";
        session.setAttribute(SESSION_AUTH_SCOPE_KEY, scope);
        
        session.setAttribute(SESSION_FHIR_SERVICE_URI_KEY, fhirServiceURI);
        
        session.setAttribute(SESSION_LAUNCH_CONTEXT_ID_KEY, launchContextId);

        try {
			URI here = new URI(launchRequest.getRequestURL().toString());
	        URI redirectURI = here.resolve("entry.html");
	        session.setAttribute(SESSION_REDIRECT_URI_KEY, redirectURI);
		} catch (URISyntaxException e) {
			e.printStackTrace();
			return null;
		}
        
		return session;
	}
		
	private void requestAuthorization(HttpSession session) {
		
        String baseURI = ((Map<String,String>)session.getAttribute(SESSION_AUTH_ENDPOINTS_KEY)).get("authorize");
        URI authRequest;
		try {
			authRequest = new URI(baseURI + "?" +
			    "response_type=code&" +
			    "client_id=" + URLEncoder.encode(clientId, "UTF-8") + "&" +
			    "redirect_uri=" + URLEncoder.encode((String)session.getAttribute(SESSION_REDIRECT_URI_KEY), "UTF-8") + "&" +
			    "scope=" + URLEncoder.encode((String)session.getAttribute(SESSION_AUTH_SCOPE_KEY), "UTF-8") + "&" +
			    "aud=" + URLEncoder.encode((String)session.getAttribute(SESSION_FHIR_SERVICE_URI_KEY), "UTF-8") + "&" +
			    "launch=" + (String)session.getAttribute(SESSION_LAUNCH_CONTEXT_ID_KEY) + "&" +
			    "state=" + session.getId());
		} catch (UnsupportedEncodingException | URISyntaxException e1) {
			e1.printStackTrace();
			return;
		}
	
        logger.info("OAuth 'authorize' request: "+authRequest.toString());
	
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> responseEntity = restTemplate.getForEntity(authRequest, String.class);
        
        MediaType contentType = responseEntity.getHeaders().getContentType();
        HttpStatus requestStatus = responseEntity.getStatusCode();
        URI redirectLocation = responseEntity.getHeaders().getLocation();
        
        logger.info("OAuth 'authorize' response:"
        		+" status = " +  requestStatus
        		+", content-type = " + contentType.toString()
        		+", location = " + redirectLocation);
        
        if( requestStatus.value() != 302 ) {
        	logger.severe("unexpected request status from authorization service (expected 302");
        	return;
        }
        
        String queryString = redirectLocation.getQuery();
        String[] queryParms = queryString.split("&");
        
		String grantType=null, authCode=null, errorCode=null, errorMessage=null;
        for( String parm : queryParms ) {
        	String[] parts = parm.split("=");
        	try {
        		if( "grantType".equals(parts[0]) ) {
        			errorCode = parts[1];
        		}
        		else if( "code".equals(parts[0]) ) {
        			authCode = parts[1];
        		}
        		else if( "error".equals(parts[0]) ) {
        			errorCode = parts[1];
        		}
        		else if( "error_description".equals(parts[0]) ) {
        			errorMessage = parts[1];
        		}
        		else
        			logger.info(parts[0] + " = " + URLDecoder.decode(parts[1], "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
        }
        
        if( errorCode != null ) {
        	logger.severe(errorCode + " (" + errorMessage + ")");
        	return;
        }
        
        session.setAttribute(SESSION_AUTH_CODE_KEY, authCode);

	}

}
