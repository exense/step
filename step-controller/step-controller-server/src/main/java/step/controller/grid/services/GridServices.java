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
package step.controller.grid.services;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.annotation.PostConstruct;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

import io.swagger.v3.oas.annotations.tags.Tag;
import step.core.deployment.AbstractStepServices;
import step.framework.server.security.Secured;
import step.grid.TokenWrapper;
import step.grid.TokenWrapperState;
import step.grid.client.GridClient;
import step.grid.client.reports.GridReportBuilder;
import step.grid.client.reports.TokenGroupCapacity;

@Path("/grid")
@Tag(name = "Grid")
public class GridServices extends AbstractStepServices {

	protected GridClient gridClient;
	
	@PostConstruct
	public void init() throws Exception {
		super.init();
		gridClient = getContext().get(GridClient.class);
	}
	
	private GridReportBuilder getReportBuilder() {
		return new GridReportBuilder(gridClient);
	}
	

	@GET
	@Path("/agent")
	@Produces(MediaType.APPLICATION_JSON)
	public Collection<AgentListEntry> getAgents(@QueryParam("notokens") String notokens) {
		boolean skipTokens = Boolean.parseBoolean(notokens);
		List<AgentListEntry> agents = new ArrayList<>();
		
		gridClient.getAgents().forEach(agent->{
			AgentListEntry agentState = new AgentListEntry();
			agentState.setAgentRef(agent);
			List<TokenWrapper> agentTokens = getAgentTokens(agent.getAgentId());
			if (!skipTokens) {
				agentState.setTokens(agentTokens);
			}
			agentState.setTokensCapacity(getTokensCapacity(agentTokens));
			
			agents.add(agentState);
		});
		return agents;
	}

	protected List<TokenWrapper> getAgentTokens(String agentId) {
		List<TokenWrapper> agentTokens = new ArrayList<>();
		
		gridClient.getTokens().forEach(token->{
			if(agentId.equals(token.getAgent().getAgentId())) {
				agentTokens.add(token);
			}
		});
		return agentTokens;
	}
	
	
	protected TokenGroupCapacity getTokensCapacity(List<TokenWrapper> tokens) {
		TokenGroupCapacity tokenGroup = new TokenGroupCapacity(new HashMap<>());
		Map<TokenWrapperState, AtomicInteger> stateDistribution = new HashMap<>();
		Arrays.asList(TokenWrapperState.values()).forEach(s->stateDistribution.put(s, new AtomicInteger(0)));
		tokens.forEach(token->{
			tokenGroup.incrementCapacity();
			stateDistribution.get(token.getState()).incrementAndGet();
		});
		
		Map<TokenWrapperState, Integer> result = new HashMap<>();
		stateDistribution.entrySet().forEach(e->result.put(e.getKey(),e.getValue().get()));
		
		tokenGroup.setCountByState(result);
		return tokenGroup;
	}
	
	@PUT
	@Secured(right="token-manage")
	@Path("/agent/{id}/interrupt")
	@Produces(MediaType.APPLICATION_JSON)
	public void interruptAgent(@PathParam("id") String agentId) {
		gridClient.getTokens().forEach(token->{
			if(agentId.equals(token.getAgent().getAgentId())) {
				gridClient.startTokenMaintenance(token.getID());
			}
		});
	}
	
	@PUT
	@Secured(right="token-manage")
	@Path("/agent/{id}/resume")
	@Produces(MediaType.APPLICATION_JSON)
	public void resumeAgent(@PathParam("id") String agentId) {
		gridClient.getTokens().forEach(token->{
			if(agentId.equals(token.getAgent().getAgentId())) {
				gridClient.stopTokenMaintenance(token.getID());
			}
		});
	}
	
	@DELETE
	@Secured(right="token-manage")
	@Path("/agent/{id}/tokens/errors")
	@Produces(MediaType.APPLICATION_JSON)
	public void removeAgentTokenErrors(@PathParam("id") String agentId) {
		gridClient.getTokens().forEach(token->{
			if(token.getState().equals(TokenWrapperState.ERROR)) {
				gridClient.removeTokenError(token.getID());
			}
		});
	}

	@GET
	@Path("/token")
	@Produces(MediaType.APPLICATION_JSON)
	public List<TokenWrapper> getTokenAssociations() {
		return getReportBuilder().getTokenAssociations(false);
	}
	
	@DELETE
	@Secured(right="token-manage")
	@Path("/token/{id}/error")
	@Consumes(MediaType.APPLICATION_JSON)
	public void removeTokenError(@PathParam("id") String tokenId) {
		gridClient.removeTokenError(tokenId);
	}
	
	@POST
	@Secured(right="token-manage")
	@Path("/token/{id}/maintenance")
	@Consumes(MediaType.APPLICATION_JSON)
	public void startTokenMaintenance(@PathParam("id") String tokenId) {
		gridClient.startTokenMaintenance(tokenId);
	}
	
	@DELETE
	@Secured(right="token-manage")
	@Path("/token/{id}/maintenance")
	@Consumes(MediaType.APPLICATION_JSON)
	public void stopTokenMaintenance(@PathParam("id") String tokenId) {
		gridClient.stopTokenMaintenance(tokenId);
	}
	
	@GET
	@Path("/token/usage")
	@Produces(MediaType.APPLICATION_JSON)
	public List<TokenGroupCapacity> getUsageByIdentity(@QueryParam("groupby") List<String> groupbys) {
		return getReportBuilder().getUsageByIdentity(groupbys);
	}
	
	@GET
	@Path("/keys")
	@Produces(MediaType.APPLICATION_JSON)
	public Set<String> getTokenAttributeKeys() {
		return getReportBuilder().getTokenAttributeKeys();
	}
}
