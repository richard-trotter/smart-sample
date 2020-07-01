package rht.samples.smart.one;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.Properties;
import java.util.jar.Manifest;
import java.util.logging.Logger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.io.ClassPathResource;

/*
 * The @SpringBootApplication annotation is equivalent to using:
 *  @Configuration 
 *  @EnableAutoConfiguration 
 *  @ComponentScan
 */
@SpringBootApplication
@EnableConfigurationProperties
@ConfigurationProperties
@PropertySource("classpath:application.properties")
public class Application {
	
	private static Logger logger = Logger.getLogger(Application.class.getName());
    
    public static void main(String[] args) {
    	
    	try {
			logBuildInfo();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	/*
    	 * The constructor arguments passed to SpringApplication are configuration sources 
    	 * for Spring beans.
    	 */
        SpringApplication.run(Application.class, args);
    }

	static ApplicationContext context = new AnnotationConfigApplicationContext(Application.class);

	/*
	 * Show jar manifest attributes (to include build timestamp) and application config properties
	 * in the log. 
	 */
    static void logBuildInfo() throws IOException {

    	ClassPathResource propertiesResource = new ClassPathResource("application.properties");
    	
    	URL url = propertiesResource.getURL();
    	logger.info("config url: "+url);

    	// "Build-Timestamp"
    	if( url.toString().startsWith("jar:") ) {
			Manifest manifest = ((JarURLConnection)url.openConnection()).getManifest();
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			outputStream.write("#jar manifest \n".getBytes());
			manifest.write(outputStream);
			logger.info(outputStream.toString());
	    	
			try(InputStream propertiesInputStream = propertiesResource.getInputStream()) {
				Properties props = new Properties();
				props.load(propertiesInputStream);
				ByteArrayOutputStream propertiesOutputStream = new ByteArrayOutputStream();
				props.store(propertiesOutputStream, "application.properties");
				logger.info(propertiesOutputStream.toString());
			}
    	}
    }
    
}
