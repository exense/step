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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import step.grid.reports.TokenAssociation;
import step.grid.reports.TokenGroupCapacity;
import step.grid.tokenpool.Interest;

public class GridReportBuilder {

	private Grid grid;
	
	public GridReportBuilder(Grid adapterGrid) {
		super();
		this.grid = adapterGrid;
	}

	public List<TokenGroupCapacity> getUsageByIdentity(List<String> groupbys) {		
		Map<Map<String, String>, TokenGroupCapacity> countsByIdentity = new HashMap<>();
		
		for(step.grid.tokenpool.Token<TokenWrapper> token:grid.getTokens()) {
			TokenWrapper aToken = token.getObject();
			
			Map<String, String> key = new HashMap<>(); 

			for(String groupby:groupbys) {
				key.put(groupby, getValue(groupby, aToken));
			}
			
			if(!countsByIdentity.containsKey(key)) {
				countsByIdentity.put(key, new TokenGroupCapacity(key));
			}
			TokenGroupCapacity c = countsByIdentity.get(key);
			c.incrementCapacity();
			if(!token.isFree()) {
				c.incrementUsage();					
			}
		}
		
		return new ArrayList<>(countsByIdentity.values());
	}
	
	public Set<String> getTokenAttributeKeys() {
		Set<String> result = new HashSet<>();
		for(step.grid.tokenpool.Token<TokenWrapper> token:grid.getTokens()) {
			result.addAll(token.getObject().getAttributes().keySet());
			result.addAll(token.getObject().getInterests().keySet());
		}
		return result;
	}
	
	private static final String UID_KEY = "id";
	
	private static final String URL_KEY = "url";
	
	private String getValue(String key, TokenWrapper aToken) {
		if(key.equals(UID_KEY)) {
			return aToken.getID();
		}
		if(key.equals(URL_KEY)) {
			AgentRef ref = aToken.getAgent();
			return ref!=null?ref.getAgentUrl():"-";
		}
		if(aToken.getAttributes()!=null) {
			String attribute = aToken.getAttributes().get(key);
			if(attribute!=null) {
				return attribute;						
			}
		}
		if(aToken.getInterests()!=null) {
			Interest interest = aToken.getInterests().get(key);
			if(interest!=null) {
				return interest.getSelectionPattern().toString();	
			}
		}
		return null;
	}
	
	public List<TokenAssociation> getTokenAssociations(boolean onlyWithOwner) {
		List<TokenAssociation> tokens = new ArrayList<>();
		for(step.grid.tokenpool.Token<TokenWrapper> token:grid.getTokens()) {
			Object currentOwner = token.getObject().getCurrentOwner();
			if(currentOwner!=null||(currentOwner==null&&!onlyWithOwner)) {
				tokens.add(new TokenAssociation(token.getObject().getToken(), currentOwner));
			}
		}
		return tokens;
	}
	
}
