/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
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
 ******************************************************************************/
package step.controller;

import ch.exense.commons.app.ArgumentParser;
import ch.exense.commons.app.Configuration;
import ch.exense.viz.persistence.accessors.GenericVizAccessor;
import org.eclipse.jetty.http.HttpCookie;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.session.SessionHandler;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.process.internal.RequestScoped;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;
import step.controller.swagger.Swagger;
import step.core.Controller;
import step.core.Controller.ServiceRegistrationCallback;
import step.core.authentication.AuthenticationFilter;
import step.core.controller.errorhandling.ErrorFilter;
import step.core.deployment.*;
import step.core.scheduler.SchedulerServices;
import step.plugins.interactive.InteractiveServices;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import java.io.File;
import java.io.IOException;
import java.util.EnumSet;
import java.util.logging.LogManager;


public class ControllerServer {

	private Configuration configuration;
	
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
		
		setupLogging();
		
		(new ControllerServer(configuration)).start();
	}

	protected static void setupLogging() {
		LogManager.getLogManager().reset();
		SLF4JBridgeHandler.install();
	}
	
	public ControllerServer(Configuration configuration) {
		super();
		this.configuration = configuration;
		this.port = configuration.getPropertyAsInteger("port", 8080);
	}

	public void start() throws Exception {
		server = new Server();
		handlers = new ContextHandlerCollection();

		initController();
		
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
		if(configuration != null) {
			try {
				configuration.close();
			} catch (IOException e) {
				logger.error("Error while closing configuration",e);
			}
		}
	}

	private void setupConnectors() {
		HttpConfiguration http = new HttpConfiguration();
		http.addCustomizer(new SecureRequestCustomizer());
		http.setSecureScheme("https");

		ServerConnector connector = new ServerConnector(server);
		connector.addConnectionFactory(new HttpConnectionFactory(http));
		connector.setPort(port);
		
		if(configuration.getPropertyAsBoolean("ui.ssl.enabled", false)) {
			int httpsPort = configuration.getPropertyAsInteger("ui.ssl.port", 443);
			
			http.setSecurePort(httpsPort);

			HttpConfiguration https = new HttpConfiguration();
			https.addCustomizer(new SecureRequestCustomizer());
			
			SslContextFactory sslContextFactory = new SslContextFactory.Server();
			sslContextFactory.setKeyStorePath(configuration.getProperty("ui.ssl.keystore.path"));
			sslContextFactory.setKeyStorePassword(configuration.getProperty("ui.ssl.keystore.password"));
			sslContextFactory.setKeyManagerPassword(configuration.getProperty("ui.ssl.keymanager.password"));
			
			ServerConnector sslConnector = new ServerConnector(server, new SslConnectionFactory(sslContextFactory, "http/1.1"), new HttpConnectionFactory(https));
			sslConnector.setPort(httpsPort);
			server.addConnector(sslConnector);
		}

		server.addConnector(connector);
	}

	private void initController() throws Exception {
		ResourceConfig resourceConfig = new ResourceConfig();
		resourceConfig.packages(ControllerServices.class.getPackage().getName());

		controller = new Controller(configuration);

		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setBaseResource(Resource.newSystemResource("/dist/step-frontend"));
		context.setContextPath("/");
		addHandler(context);

		
		controller.init(new ServiceRegistrationCallback() {
			@Override
			public void register(Object component) {
				resourceConfig.register(component);
			}

			public void registerService(Class<?> serviceClass) {
				resourceConfig.registerClasses(serviceClass);
			}

			@Override
			public void registerHandler(Handler handler) {
				addHandler(handler);
			}

			@Override
			public void registerServlet(ServletHolder servletHolder, String subPath) {
				context.addServlet(servletHolder, subPath);
			}

			@Override
			public FilterHolder registerServletFilter(Class<? extends Filter> filterClass, String pathSpec, EnumSet<DispatcherType> dispatches) {
				return context.addFilter(filterClass, pathSpec, dispatches);
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
		resourceConfig.registerClasses(SchedulerServices.class);
		resourceConfig.registerClasses(InteractiveServices.class);
		resourceConfig.registerClasses(AccessServices.class);
		resourceConfig.registerClasses(AuthenticationFilter.class);
		resourceConfig.registerClasses(SecurityFilter.class);
		resourceConfig.registerClasses(ErrorFilter.class);
		resourceConfig.registerClasses(CORSRequestResponseFilter.class);
		resourceConfig.registerClasses(AdminServices.class);
		
		resourceConfig.register(new AbstractBinder() {	
			@Override
			protected void configure() {
				bind(controller).to(Controller.class);
				bindFactory(HttpSessionFactory.class).to(HttpSession.class)
                .proxy(true).proxyForSameScope(false).in(RequestScoped.class);
			}
		});
		GenericVizAccessor accessor = new GenericVizAccessor(controller.getContext().getCollectionFactory());
		resourceConfig.register(new AbstractBinder() {
			@Override
			protected void configure() {
				bind(accessor).to(GenericVizAccessor.class);
			}
		});
		
		ServletContainer servletContainer = new ServletContainer(resourceConfig);

		ServletHolder sh = new ServletHolder(servletContainer);
		context.addServlet(sh, "/rest/*");

		//Http session management
		SessionHandler s = new SessionHandler();
		Integer timeout = configuration.getPropertyAsInteger("ui.sessiontimeout.minutes", 180)*60;
		s.setMaxInactiveInterval(timeout);
		s.setUsingCookies(true);
		s.setSessionCookie("sessionid");
		s.setSameSite(HttpCookie.SameSite.LAX);
		//s.getSessionCookieConfig().setSecure(true);
		s.setHttpOnly(true);
		context.setSessionHandler(s);
		context.addEventListener(new HttpSessionListener() {
			@Override
			public void sessionCreated(HttpSessionEvent httpSessionEvent) {}
			@Override
			public void sessionDestroyed(HttpSessionEvent httpSessionEvent) {
				AuditLogger.logSessionInvalidation(httpSessionEvent.getSession());
			}
		});

		// Lastly, the default servlet for root content (always needed, to satisfy servlet spec)
		// It is important that this is last.
		ServletHolder holderPwd = new ServletHolder("default", DefaultServlet.class);
		holderPwd.setInitParameter("dirAllowed","true");
		context.addServlet(holderPwd,"/");
		
		Swagger.setup(resourceConfig);
	}

	private synchronized void addHandler(Handler handler) {
		handlers.addHandler(handler);
	}
}
