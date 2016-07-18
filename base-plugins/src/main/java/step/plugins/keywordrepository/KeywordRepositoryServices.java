package step.plugins.keywordrepository;

import java.util.List;

import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import step.core.deployment.AbstractServices;
import step.plugins.adaptergrid.AdapterClientPlugin;

@Singleton
@Path("/keyword")
public class KeywordRepositoryServices extends AbstractServices {
	
	@GET
	@Path("/list")
	@Produces(MediaType.APPLICATION_JSON)
	public List<String> getKeywordList() {
		KeywordRepository repository = (KeywordRepository) getContext().get(AdapterClientPlugin.KEYWORD_REPOSITORY_KEY);
		
		return repository.getKeywordList();
	}

}
