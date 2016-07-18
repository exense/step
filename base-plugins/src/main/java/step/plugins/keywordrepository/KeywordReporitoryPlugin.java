package step.plugins.keywordrepository;

import step.core.GlobalContext;
import step.core.plugins.AbstractPlugin;
import step.core.plugins.Plugin;

@Plugin
public class KeywordReporitoryPlugin extends AbstractPlugin {
	
	public static final String KEYWORD_REPOSITORY_KEY = "KeywordRepository_Instance";
	
	@Override
	public void executionControllerStart(GlobalContext context) {
		KeywordRepository kwRepo = new KeywordRepository();
		context.put(KEYWORD_REPOSITORY_KEY, kwRepo);
		context.getServiceRegistrationCallback().registerService(KeywordRepositoryServices.class);
	}

	@Override
	public void executionControllerDestroy(GlobalContext context) {
		Object repo = context.get(KEYWORD_REPOSITORY_KEY);
		if(repo!=null) {
			((KeywordRepository)repo).destroy();
		}
	}
}
