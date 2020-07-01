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
public class PatientController {

	static Logger logger = Logger.getLogger(PatientController.class.getName());
	static final String TestServer = "https://open-ic.epic.com/Argonaut/api/FHIR/Argonaut"; 

	static final String SESSION_FHIR_SERVICE_URI_KEY = "fhirServiceURI";


	@GetMapping("/patient")
    public String launch(
    		HttpServletRequest clientRequest,
    		@RequestParam(name="patientID", required=true) String patientId, 
    		@RequestParam(name="authorizationToken", required=true) String authorizationToken, 
    		Model model) {

        logger.info("handling request: "+clientRequest.getRequestURL());
/*
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
*/
        return "greeting";
    }
	
}
