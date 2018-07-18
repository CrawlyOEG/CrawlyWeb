package crawly;

import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertiesPropertySource;

@SpringBootApplication
public class Application {

	public static void main(String[] args) {
		SpringApplication application = new SpringApplication(Application.class);
        Properties properties = new Properties();
        properties.put("crawly.paths.uploadedFiles", args[0]);
        properties.put("crawly.paths.pdfFiles", args[1]);
        properties.put("crawly.paths.loLibrairy", args[2]);
        application.setDefaultProperties(properties);
        application.run(args);
        //SpringApplication.run(Application.class, args);
	}

}
