package step.repositories.artifact;

import ch.exense.commons.app.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.GlobalContext;
import step.core.controller.ControllerSettingAccessor;
import step.core.controller.ControllerSettingPlugin;
import step.core.plans.PlanAccessor;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;

@Plugin(dependencies = {ControllerSettingPlugin.class})
public class ArtifactRepositoryPlugin extends AbstractControllerPlugin {

    private static final Logger logger = LoggerFactory.getLogger(ArtifactRepositoryPlugin.class);

    public static final String MAVEN_REPO_ID = "Artifact";
    public static final String RESOURCE_REPO_ID = "ResourceArtifact";

    @Override
    public void serverStart(GlobalContext context) throws Exception {
        PlanAccessor planAccessor = context.getPlanAccessor();
        ControllerSettingAccessor controllerSettingAccessor = context.require(ControllerSettingAccessor.class);
        Configuration configuration = context.getConfiguration();
        MavenArtifactRepository mavenRepository = new MavenArtifactRepository(planAccessor, controllerSettingAccessor, configuration);
        ResourceArtifactRepository resourceRepository = new ResourceArtifactRepository(planAccessor, context.getResourceManager());
        context.getRepositoryObjectManager().registerRepository(MAVEN_REPO_ID, mavenRepository);
        context.getRepositoryObjectManager().registerRepository(RESOURCE_REPO_ID, resourceRepository);
        super.serverStart(context);
    }
}
