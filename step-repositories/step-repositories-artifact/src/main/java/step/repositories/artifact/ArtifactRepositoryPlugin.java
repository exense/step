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

    public static final String REPO_ID = "Artifact";

    @Override
    public void serverStart(GlobalContext context) throws Exception {
        PlanAccessor planAccessor = context.getPlanAccessor();
        ControllerSettingAccessor controllerSettingAccessor = context.require(ControllerSettingAccessor.class);
        Configuration configuration = context.getConfiguration();
        ArtifactRepository repository = new ArtifactRepository(planAccessor, controllerSettingAccessor, configuration);
        context.getRepositoryObjectManager().registerRepository(REPO_ID, repository);
        super.serverStart(context);
    }
}
