package step.grid;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;

import step.grid.tokenpool.Identity;
import step.grid.tokenpool.SimpleAffinityEvaluator;
import step.grid.tokenpool.TokenPool;

public class Grid {

	private ExpiringMap<String, AgentRef> agentRefs;
	
	private TokenPool<Identity, TokenWrapper> tokenPool;

	private Integer port;
	
	private Server server;
	
	public Grid(Integer port) {
		super();
		
		this.port = port;
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
		agentRefs = new ExpiringMap<>();
	}
	
	private void initializeTokenPool() {
		tokenPool = new TokenPool<>(new SimpleAffinityEvaluator());
		//TODO Configuration
		Integer timeout = 60000;
		tokenPool.setKeepaliveTimeout(timeout);
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
	
	public TokenWrapper selectToken(Identity pretender, long matchTimeout, long noMatchTimeout)
			throws TimeoutException, InterruptedException {
		return tokenPool.selectToken(pretender, matchTimeout, noMatchTimeout);
	}

	public void returnToken(TokenWrapper object) {
		tokenPool.returnToken(object);
	}

	protected TokenPool<Identity, TokenWrapper> getTokenPool() {
		return tokenPool;
	}
	
	public List<TokenWrapper> getTokens() {
		List<TokenWrapper> tokens = new ArrayList<>();
		for(step.grid.tokenpool.Token<TokenWrapper> token:tokenPool.getTokens()) {
			tokens.add(token.getObject());
		}
		return tokens;
	}
	
	public ExpiringMap<String, AgentRef> getAgentRefs() {
		return agentRefs;
	}

	public boolean existsAvailableMatchingToken(Identity pretender) {
		return tokenPool.existsAvailableMatchingToken(pretender);
	}
}
