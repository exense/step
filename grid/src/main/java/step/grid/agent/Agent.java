package step.grid.agent;

import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.grid.Token;
import step.grid.agent.handler.TokenHandlerPool;
import step.grid.agent.tokenpool.AgentTokenPool;
import step.grid.agent.tokenpool.AgentTokenWrapper;
import step.grid.io.ObjectMapperResolver;
import step.grid.tokenpool.Interest;

public class Agent {
	
	private static final Logger logger = LoggerFactory.getLogger(Agent.class);
	
	private String id;
	
	private String gridHost;
	
	private Integer agentPort;

	private String agentUrl;
	
	private Server server;
	
	private AgentTokenPool tokenPool;
	
	private TokenHandlerPool handlerPool;
	
	private Timer timer;
	
	private RegistrationTask registrationTask;
	
	public Agent(String gridHost, String agentUrl, Integer agentPort) {
		super();
		this.gridHost = gridHost;
		this.agentUrl = agentUrl;
		this.agentPort = agentPort;
		
		id = UUID.randomUUID().toString();
		tokenPool = new AgentTokenPool(10000);
		handlerPool = new TokenHandlerPool();
	}
	
	public String getId() {
		return id;
	}
	
	public void addTokens(int count, Map<String, String> attributes, Map<String, String> selectionPatterns) {
		for(int i=0;i<count;i++) {
			AgentTokenWrapper token = new AgentTokenWrapper();
			token.getToken().setAgentid(id);
			token.setAttributes(attributes);
			token.setSelectionPatterns(createInterestMap(selectionPatterns));
			token.setProperties(null);
			tokenPool.offerToken(token);
		}
	}
	
	private Map<String, Interest> createInterestMap(Map<String, String> selectionPatterns) {
		HashMap<String, Interest> result = new HashMap<String, Interest>();
		if(selectionPatterns!=null) {
			for(Entry<String, String> entry:selectionPatterns.entrySet()) {
				Interest i = new Interest(Pattern.compile(entry.getValue()), true);
				result.put(entry.getKey(), i);
			}
		}
		return result;
	}

	public void start() throws Exception {
		if(agentUrl==null) {
			try {
				agentUrl = "http://" + Inet4Address.getLocalHost().getHostName() + ":" + agentPort;
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		ResourceConfig resourceConfig = new ResourceConfig();
		resourceConfig.packages(AgentServices.class.getPackage().getName());
		//resourceConfig.register(JacksonFeature.class);
		resourceConfig.register(ObjectMapperResolver.class);
		final Agent agent = this;
		resourceConfig.register(new AbstractBinder() {	
			@Override
			protected void configure() {
				bind(agent).to(Agent.class);
			}
		});
		ServletContainer servletContainer = new ServletContainer(resourceConfig);

		ServletHolder sh = new ServletHolder(servletContainer);
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setContextPath("/");
		context.addServlet(sh, "/*");
		
		server = new Server(agentPort);

		ContextHandlerCollection contexts = new ContextHandlerCollection();
        contexts.setHandlers(new Handler[] { context });
		server.setHandler(contexts);
		
		timer = new Timer();
		
		registrationTask = new RegistrationTask(this);

		server.start();
		
		timer.schedule(registrationTask, 0, 10000);
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				tokenPool.evictSessions();
			}
		}, 15000, 10000);
	}
	
	protected String getAgentUrl() {
		return agentUrl;
	}

	public void stop() throws Exception {
		if(timer!=null) {
			timer.cancel();
		}
		
		if(registrationTask!=null) {
			registrationTask.cancel();
			registrationTask.unregister();
			registrationTask.destroy();
		}

		server.stop();
	}

	protected AgentTokenPool getTokenPool() {
		return tokenPool;
	}
	
	protected TokenHandlerPool getHandlerPool() {
		return handlerPool;
	}

	protected RegistrationTask getRegistrationTask() {
		return registrationTask;
	}

	protected List<Token> getTokens() {
		return ((List<AgentTokenWrapper>)tokenPool.getTokens()).stream().map(t->t.getToken()).collect(Collectors.toList());
	}
	
	protected String getGridHost() {
		return gridHost;
	}

}
