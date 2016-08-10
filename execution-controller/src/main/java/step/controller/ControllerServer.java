package step.controller;

import java.io.File;
import java.util.Map.Entry;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;

import step.attachments.DownloadFileServlet;
import step.commons.conf.Configuration;
import step.core.Controller;
import step.core.Controller.ServiceRegistrationCallback;
import step.core.deployment.ControllerServices;
import step.core.deployment.JacksonMapperProvider;
import step.grid.agent.ArgumentParser;
import step.grid.io.ObjectMapperResolver;

public class ControllerServer {

	private Controller controller;
	
	private Server server;
	
	private ContextHandlerCollection handlers;
	
	private Integer port;
	
	public static void main(String[] args) throws Exception {
		ArgumentParser arguments = new ArgumentParser(args);
		
		String portStr = arguments.getOption("port");
		Integer port = portStr!=null?Integer.decode(portStr):8080;
		
		Configuration configuration; 
		String configStr = arguments.getOption("config");
		if(configStr!=null) {
			configuration = new Configuration(new File(configStr));
		} else {
			configuration = new Configuration();
		}
		
		arguments.entrySet().forEach(e->configuration.putProperty(e.getKey(),e.getValue()));
		
		Configuration.setInstance(configuration);
		
		(new ControllerServer(port)).start();
	}
	
	public ControllerServer(Integer port) {
		super();
		this.port = port;
	}

	public void start() throws Exception {
		server = new Server(port);
		handlers = new ContextHandlerCollection();

		initController();
		initWebapp();
		initDownloadServlet();
		
		server.setHandler(handlers);
		server.start();
	}

	private void initWebapp() throws Exception {
		ResourceHandler bb = new ResourceHandler();
		bb.setResourceBase(Resource.newClassPathResource("webapp").getURI().toString());
		
		ContextHandler ctx = new ContextHandler("/"); /* the server uri path */
		ctx.setHandler(bb);
		
		addHandler(ctx);
	}

	private void initController() throws Exception {
		ResourceConfig resourceConfig = new ResourceConfig();
		resourceConfig.packages(ControllerServices.class.getPackage().getName());

		controller = new Controller();
		
		controller.init(new ServiceRegistrationCallback() {
			public void registerService(Class<?> serviceClass) {
				resourceConfig.registerClasses(serviceClass);
			}

			@Override
			public void registerHandler(Handler handler) {
				addHandler(handler);
			}
		});
		
		resourceConfig.register(JacksonJaxbJsonProvider.class);
		resourceConfig.register(JacksonMapperProvider.class);
		
		resourceConfig.registerClasses(ControllerServices.class);
		
		resourceConfig.register(new AbstractBinder() {	
			@Override
			protected void configure() {
				bind(controller).to(Controller.class);
			}
		});
		ServletContainer servletContainer = new ServletContainer(resourceConfig);

		ServletHolder sh = new ServletHolder(servletContainer);
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setContextPath("/rest");
		context.addServlet(sh, "/*");

		addHandler(context);
	}
	
	private synchronized void addHandler(Handler handler) {
		handlers.addHandler(handler);
	}
	
	private void initDownloadServlet() {
		ServletHolder downloadServlet = new ServletHolder(new DownloadFileServlet());
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setContextPath("/files");
		context.addServlet(downloadServlet, "/*");
		addHandler(context);
	}
}
