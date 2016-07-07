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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.grid.Token;
import step.grid.agent.tokenpool.AgentTokenPool;
import step.grid.agent.tokenpool.AgentTokenWrapper;
import step.grid.tokenpool.Interest;

public class Agent {
	
	private static final Logger logger = LoggerFactory.getLogger(Agent.class);
	
	private String id;
	
	private String gridHost;
	
	private Integer agentPort;

	private String agentUrl;
	
	private AgentServlet agentServlet;
	
	private AgentTokenPool tokenPool;
	
	private Timer timer;
	
	private RegistrationTask registrationTask;
	
	public Agent(String gridHost, String agentUrl, Integer agentPort) {
		super();
		this.gridHost = gridHost;
		this.agentUrl = agentUrl;
		this.agentPort = agentPort;
		
		id = UUID.randomUUID().toString();
		tokenPool = new AgentTokenPool(10000);
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

	public void run() {
		if(agentUrl==null) {
			try {
				agentUrl = "http://" + Inet4Address.getLocalHost().getHostName() + ":" + agentPort;
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		Server server = new Server(agentPort);
		ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
		context.setContextPath("/grid");
		
		agentServlet = new AgentServlet(this);
		
		context.addServlet(new ServletHolder(agentServlet), "/*");		
		
		
		ContextHandlerCollection contexts = new ContextHandlerCollection();
        contexts.setHandlers(new Handler[] { context });
		server.setHandler(contexts);
		
		timer = new Timer();
		
		registrationTask = new RegistrationTask(this);
		timer.schedule(registrationTask, 10000,10000);
		
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				tokenPool.evictSessions();
			}
		}, 15000, 10000);

		try {
			server.start();
			server.join();
		} catch (Exception e) {
			logger.error("Error while starting server",e);
		}
		
	}
	
	protected String getAgentUrl() {
		return agentUrl;
	}

	public void destroy() {
		if(timer!=null) {
			timer.cancel();
		}
		
		if(registrationTask!=null) {
			registrationTask.cancel();
			registrationTask.unregister();
			registrationTask.destroy();
		}
	}

	protected List<Token> getTokens() {
		return ((List<AgentTokenWrapper>)tokenPool.getTokens()).stream().map(t->t.getToken()).collect(Collectors.toList());
	}
	
	protected String getGridHost() {
		return gridHost;
	}

}
