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
		
		for(step.grid.tokenpool.Token<TokenWrapper> token:grid.getTokenPool().getTokens()) {
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
		for(step.grid.tokenpool.Token<TokenWrapper> token:grid.getTokenPool().getTokens()) {
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
			AgentRef ref = grid.getAgentRefs().get(aToken.getToken().getAgentid());
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
		for(step.grid.tokenpool.Token<TokenWrapper> token:grid.getTokenPool().getTokens()) {
			Object currentOwner = token.getObject().getCurrentOwner();
			if(currentOwner!=null||(currentOwner==null&&!onlyWithOwner)) {
				tokens.add(new TokenAssociation(token.getObject().getToken(), currentOwner));
			}
		}
		return tokens;
	}
	
}
