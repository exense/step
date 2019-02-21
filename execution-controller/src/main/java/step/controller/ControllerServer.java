/*******************************************************************************
 * (C) Copyright 2016 Jerome Comte and Dorian Cransac
 *  
 * This file is part of STEP
 *  
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *  
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package step.controller;

import java.io.File;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.commons.conf.Configuration;
import step.core.Controller;
import step.core.Controller.ServiceRegistrationCallback;
import step.core.deployment.AccessServices;
import step.core.deployment.AdminServices;
import step.core.deployment.ApplicationServices;
import step.core.deployment.AuthenticationFilter;
import step.core.deployment.ControllerServices;
import step.core.deployment.ErrorFilter;
import step.core.deployment.JacksonMapperProvider;
import step.core.export.ExportServices;
import step.core.export.ImportServices;
import step.grid.agent.ArgumentParser;
import step.plugins.interactive.InteractiveServices;


public class ControllerServer {

	private Controller controller;
	
	private Server server;
	
	private ContextHandlerCollection handlers;
	
	private Integer port;
	
	private static final Logger logger = LoggerFactory.getLogger(ControllerServer.class);
	
	public static void main(String[] args) throws Exception {
		ArgumentParser arguments = new ArgumentParser(args);
		
		Configuration configuration; 
		String configStr = arguments.getOption("config");
		if(configStr!=null) {
			configuration = new Configuration(new File(configStr), arguments.getOptions());
		} else {
			configuration = new Configuration();
		}
		
		arguments.entrySet().forEach(e->configuration.putProperty(e.getKey(),e.getValue()));
		
		Configuration.setInstance(configuration);
		
		(new ControllerServer()).start();
	}
	
	public ControllerServer(Integer port) {
		super();
		this.port = port;
	}
	
	public ControllerServer() {
		this(Configuration.getInstance().getPropertyAsInteger("port", 8080));
	}

	public void start() throws Exception {
		server = new Server();
		handlers = new ContextHandlerCollection();

		initController();
		initWebapp();
		
		setupConnectors();

		server.setHandler(handlers);
		server.start();
	}
	
	private void stop() {
		try {
			server.stop();
		} catch (Exception e) {
			logger.error("Error while stopping jetty",e);
		} finally {
			server.destroy();
		}
	}

	private void setupConnectors() {
		Configuration conf = Configuration.getInstance();

		HttpConfiguration http = new HttpConfiguration();
		http.addCustomizer(new SecureRequestCustomizer());
		http.setSecureScheme("https");

		ServerConnector connector = new ServerConnector(server);
		connector.addConnectionFactory(new HttpConnectionFactory(http));
		connector.setPort(port);
		
		if(conf.getPropertyAsBoolean("ui.ssl.enabled", false)) {
			int httpsPort = conf.getPropertyAsInteger("ui.ssl.port", 443);
			
			http.setSecurePort(httpsPort);

			HttpConfiguration https = new HttpConfiguration();
			https.addCustomizer(new SecureRequestCustomizer());
			
			SslContextFactory sslContextFactory = new SslContextFactory();
			sslContextFactory.setKeyStorePath(conf.getProperty("ui.ssl.keystore.path"));
			sslContextFactory.setKeyStorePassword(conf.getProperty("ui.ssl.keystore.password"));
			sslContextFactory.setKeyManagerPassword(conf.getProperty("ui.ssl.keymanager.password"));
			
			ServerConnector sslConnector = new ServerConnector(server, new SslConnectionFactory(sslContextFactory, "http/1.1"), new HttpConnectionFactory(https));
			sslConnector.setPort(httpsPort);
			server.addConnector(sslConnector);
		}

		server.addConnector(connector);
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

			@Override
			public void stop() {
				try {
					ControllerServer.this.stop();
				} catch (Exception e) {
					logger.error("Error while trying to stop the controller",e);
				}
			}
		});
		
		resourceConfig.register(JacksonMapperProvider.class);
		resourceConfig.register(MultiPartFeature.class);
		
		resourceConfig.registerClasses(ApplicationServices.class);
		resourceConfig.registerClasses(ControllerServices.class);
		resourceConfig.registerClasses(InteractiveServices.class);
		resourceConfig.registerClasses(AccessServices.class);
		resourceConfig.registerClasses(AuthenticationFilter.class);
		resourceConfig.registerClasses(ErrorFilter.class);
		resourceConfig.registerClasses(AdminServices.class);
		resourceConfig.registerClasses(ExportServices.class, ImportServices.class);
		
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
}
