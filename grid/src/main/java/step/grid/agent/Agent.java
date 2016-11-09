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
package step.grid.agent;

import java.io.File;
import java.net.Inet4Address;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.regex.Pattern;

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

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import step.grid.Token;
import step.grid.agent.conf.AgentConf;
import step.grid.agent.conf.AgentConfParser;
import step.grid.agent.conf.TokenConf;
import step.grid.agent.conf.TokenGroupConf;
import step.grid.agent.handler.TokenHandlerPool;
import step.grid.agent.tokenpool.AgentTokenPool;
import step.grid.agent.tokenpool.AgentTokenWrapper;
import step.grid.io.ObjectMapperResolver;
import step.grid.tokenpool.Interest;

public class Agent {
	
	private static final Logger logger = LoggerFactory.getLogger(Agent.class);
	
	private String id;
	
	private AgentConf agentConf;
	
	private Server server;
	
	private AgentTokenPool tokenPool;
	
	private TokenHandlerPool handlerPool;
	
	private Timer timer;
	
	private long registrationIntervalMs = 10000;
	
	private RegistrationTask registrationTask;
	
	public static void main(String[] args) throws Exception {
		ArgumentParser arguments = new ArgumentParser(args);

		String agentConfStr = arguments.getOption("config");
		
		if(agentConfStr!=null) {
			AgentConfParser parser = new AgentConfParser();
			AgentConf agentConf = parser.parser(new File(agentConfStr));

			if(arguments.hasOption("gridHost")) {
				agentConf.setGridHost(arguments.getOption("gridHost"));
			}
			
			if(arguments.hasOption("agentPort")) {
				agentConf.setAgentPort(Integer.decode(arguments.getOption("agentPort")));
			} else if (agentConf.getAgentPort() == null) {
					agentConf.setAgentPort(12131);
			}
			
			if(arguments.hasOption("agentUrl")) {
				agentConf.setAgentUrl(arguments.getOption("agentUrl"));
			}

			(new Agent(agentConf)).start();
		} else {
			throw new RuntimeException("Argument '-config' is missing.");
		}
	}
	
	public Agent(AgentConf agentConf) throws Exception {
		super();

		this.agentConf = agentConf;
		
		if(agentConf.getAgentUrl()==null) {
			agentConf.setAgentUrl("http://" + Inet4Address.getLocalHost().getCanonicalHostName() + ":" + agentConf.getAgentPort());
		}
		
		id = UUID.randomUUID().toString();
		tokenPool = new AgentTokenPool(10000);
		handlerPool = new TokenHandlerPool();
	}
	
	public String getId() {
		return id;
	}
	
	public void addTokens(int count, Map<String, String> attributes, Map<String, String> selectionPatterns, Map<String, String> properties) {
		for(int i=0;i<count;i++) {
			AgentTokenWrapper token = new AgentTokenWrapper();
			token.getToken().setAgentid(id);
			token.setAttributes(attributes);
			token.setSelectionPatterns(createInterestMap(selectionPatterns));
			token.setProperties(properties);
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
		
		if(agentConf.getTokenGroups()!=null) {
			for(TokenGroupConf group:agentConf.getTokenGroups()) {
				TokenConf tokenConf = group.getTokenConf();
				addTokens(group.getCapacity(), tokenConf.getAttributes(), tokenConf.getSelectionPatterns(), 
						tokenConf.getProperties());
			}
		}
		
		ResourceConfig resourceConfig = new ResourceConfig();
		resourceConfig.packages(AgentServices.class.getPackage().getName());
		resourceConfig.register(JacksonJsonProvider.class);
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
		
		server = new Server(agentConf.getAgentPort());

		ContextHandlerCollection contexts = new ContextHandlerCollection();
        contexts.setHandlers(new Handler[] { context });
		server.setHandler(contexts);
		
		timer = new Timer();
		
		registrationTask = new RegistrationTask(this);

		server.start();
		
		timer.schedule(registrationTask, 0, registrationIntervalMs);
		timer.schedule(new TimerTask() {
			@Override
			public void run() {
				tokenPool.evictSessions();
			}
		}, 15000, 10000);
	}
	
	public long getRegistrationIntervalMs() {
		return registrationIntervalMs;
	}

	public void setRegistrationIntervalMs(long registrationIntervalMs) {
		this.registrationIntervalMs = registrationIntervalMs;
	}

	protected String getAgentUrl() {
		return agentConf.getAgentUrl();
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
		List<Token> tokens = new ArrayList<>();
		for(AgentTokenWrapper wrapper:tokenPool.getTokens()) {
			tokens.add(wrapper.getToken());
		}
		return tokens;
	}
	
	protected List<Token> getAvailableTokens() {
		List<Token> tokens = new ArrayList<>();
		for(AgentTokenWrapper wrapper:tokenPool.getTokens()) {
			if(!wrapper.isInUse()) {
				tokens.add(wrapper.getToken());				
			}
		}
		return tokens;
	}
	
	protected String getGridHost() {
		return agentConf.getGridHost();
	}

}
