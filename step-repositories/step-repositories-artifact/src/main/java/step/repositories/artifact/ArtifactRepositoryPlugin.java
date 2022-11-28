package step.repositories.artifact;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.GlobalContext;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.functions.packages.FunctionPackagePlugin;

@Plugin(dependencies= {FunctionPackagePlugin.class})
public class ArtifactRepositoryPlugin extends AbstractControllerPlugin {
	
	private static final Logger logger = LoggerFactory.getLogger(ArtifactRepositoryPlugin.class);

	public static final String REPO_ID = "HTTP";
	
	@Override
	public void serverStart(GlobalContext context) throws Exception {
		context.getRepositoryObjectManager().registerRepository(REPO_ID, 
				new ArtifactRepository(context.getConfiguration(),context.getPlanAccessor(), context.getResourceManager()));
		
		super.serverStart(context);
	}
}
