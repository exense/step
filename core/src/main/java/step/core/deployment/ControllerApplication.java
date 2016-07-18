package step.core.deployment;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.glassfish.jersey.server.ResourceConfig;

import step.core.Controller;
import step.core.Controller.ServiceRegistrationCallback;

import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;

public class ControllerApplication extends ResourceConfig {

	private Controller controller;
	
	
	@PostConstruct
	public void init() throws Exception {
		controller = new Controller();
		controller.init(new ServiceRegistrationCallback() {
			public void registerService(Class<?> serviceClass) {
				registerClasses(serviceClass);
			}
		});
		
		register(JacksonMapperProvider.class);
		register(JacksonJaxbJsonProvider.class);
		
		registerClasses(ControllerServices.class);
	}

	@PreDestroy
	public void destroy() {
		controller.destroy();
	}

	public Controller getController() {
		return controller;
	}
}
