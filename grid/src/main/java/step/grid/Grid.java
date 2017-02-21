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
package step.grid;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;

import step.grid.filemanager.FileManagerServer;
import step.grid.filemanager.FileProvider;
import step.grid.tokenpool.Identity;
import step.grid.tokenpool.SimpleAffinityEvaluator;
import step.grid.tokenpool.Token;
import step.grid.tokenpool.TokenPool;

public class Grid {

	public static final String LOCAL_AGENT = "local";

	private ExpiringMap<String, AgentRef> agentRefs;
	
	private TokenPool<Identity, TokenWrapper> tokenPool;

	private final Integer port;
	
	private final Integer keepAliveTimeout;
	
	private Server server;
	
	private FileManagerServer fileManager = new FileManagerServer();
	
	public Grid(Integer port) {
		super();
		this.port = port;
		this.keepAliveTimeout = 60000;
	}
	
	public Grid(Integer port, Integer ttl) {
		super();
		this.port = port;
		this.keepAliveTimeout = ttl;
	}

	public void stop() throws Exception {
		server.stop();
	}

	public void start() throws Exception {
		initializeAgentRefs();
		initializeTokenPool();
		initializeServer();
		startServer();
	}

	private void initializeAgentRefs() {
		agentRefs = new ExpiringMap<>(keepAliveTimeout);
	}
	
	private void initializeTokenPool() {
		tokenPool = new TokenPool<>(new SimpleAffinityEvaluator());
		tokenPool.setKeepaliveTimeout(keepAliveTimeout);
	}
	
	private void initializeServer() {
		ResourceConfig resourceConfig = new ResourceConfig();
		resourceConfig.packages(GridServices.class.getPackage().getName());
		resourceConfig.register(JacksonJaxbJsonProvider.class);
		final Grid grid = this;
		
		resourceConfig.register(new AbstractBinder() {	
			@Override
			protected void configure() {
				bind(grid).to(Grid.class);
				bind(fileManager).to(FileProvider.class);
			}
		});
		ServletContainer servletContainer = new ServletContainer(resourceConfig);
				
		ServletHolder sh = new ServletHolder(servletContainer);
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setContextPath("/");
		context.addServlet(sh, "/*");

		server = new Server(port);
		
		ContextHandlerCollection contexts = new ContextHandlerCollection();
        contexts.setHandlers(new Handler[] { context});
		server.setHandler(contexts);
	}
	
	private void startServer() throws Exception {
		server.start();
	}
	
	protected void handleRegistrationMessage(RegistrationMessage message) {
		AgentRef agentRef = message.getAgentRef();
		agentRefs.putOrTouch(agentRef.getAgentId(), agentRef);
		for (step.grid.Token token : message.getTokens()) {
			tokenPool.offerToken(new TokenWrapper(token, agentRef));			
		}	
	}
	
	public TokenWrapper selectToken(Identity pretender, long matchTimeout, long noMatchTimeout)
			throws TimeoutException, InterruptedException {
		return tokenPool.selectToken(pretender, matchTimeout, noMatchTimeout);
	}

	public void returnToken(TokenWrapper object) {
		tokenPool.returnToken(object);
	}
	
	public String registerFile(File file) {
		return fileManager.registerFile(file);
	}

	public List<Token<TokenWrapper>> getTokens() {
		return tokenPool.getTokens();
	}
	
	public Collection<AgentRef> getAgents() {
		return agentRefs.values();
	}
	
	public int getServerPort() {
		return ((ServerConnector)server.getConnectors()[0]).getLocalPort();
	}
}
