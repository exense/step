package step.plugins.keywordrepository;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.script.ScriptException;

import step.commons.activation.Activator;
import step.commons.conf.FileRepository;
import step.commons.conf.FileRepository.FileRepositoryCallback;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionContextBindings;

public class KeywordRepository {

	FileRepository<KeywordRepositoryConfiguration> repo;
	
	List<Keyword> keywords;
		
	public KeywordRepository() {
		super();
		
		repo = new FileRepository<KeywordRepositoryConfiguration>("Keywords.js", KeywordRepositoryConfiguration.class, new FileRepositoryCallback<KeywordRepositoryConfiguration>() {
			@Override
			public void onLoad(KeywordRepositoryConfiguration object) throws ScriptException {
				keywords = Activator.compileActivationExpressions(object.getKeywords());
			}} );
	}
	

	public Keyword getConfigurationForKeyword(ExecutionContext context, String keywordID) {
		Map<String, Object> bindings = ExecutionContextBindings.get(context);
		return Activator.findBestMatch(bindings, keywords.stream().filter(item -> item.getName().equals(keywordID)).collect(Collectors.toList()));
	}
	
	public List<String> getKeywordList() {
		return keywords.stream().map(item -> item.getName()).distinct().collect(Collectors.toList());
	}

	
	public void destroy() {
		repo.destroy();
	}
}
