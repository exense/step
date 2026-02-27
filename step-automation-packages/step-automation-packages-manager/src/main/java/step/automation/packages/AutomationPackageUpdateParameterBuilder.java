package step.automation.packages;

import org.bson.types.ObjectId;
import step.attachments.FileResolver;
import step.core.AbstractContext;
import step.core.objectenricher.*;

import java.util.Map;
import static step.core.objectenricher.WriteAccessValidator.NO_CHECKS_VALIDATOR;

public class AutomationPackageUpdateParameterBuilder {
    private boolean allowUpdate = true;
    private boolean allowCreate = true;
    private boolean isClasspathBased = false;
    private ObjectId explicitOldId = null;
    private AutomationPackageFileSource apSource;
    private AutomationPackageFileSource apLibrarySource;
    private String versionName = null;
    private String activationExpression = null;
    private ObjectEnricher enricher;
    private ObjectPredicate objectPredicate;
    private WriteAccessValidator writeAccessValidator;
    private boolean async = false;
    private String actorUser;
    private boolean forceRefreshOfSnapshots = false;
    private boolean checkForSameOrigin = true;
    private Map<String, String> functionsAttributes;
    private Map<String, String> plansAttributes;
    private Map<String, String> tokenSelectionCriteria;
    private boolean executeFunctionsLocally;
    private boolean reloading = false;

    public AutomationPackageUpdateParameterBuilder withAllowUpdate(boolean allowUpdate) {
        this.allowUpdate = allowUpdate;
        return this;
    }

    public AutomationPackageUpdateParameterBuilder withAllowCreate(boolean allowCreate) {
        this.allowCreate = allowCreate;
        return this;
    }

    public AutomationPackageUpdateParameterBuilder withClasspathBased(boolean isClasspathBased) {
        this.isClasspathBased = isClasspathBased;
        return this;
    }


    public AutomationPackageUpdateParameterBuilder withExplicitOldId(ObjectId explicitOldId) {
        this.explicitOldId = explicitOldId;
        return this;
    }

    public AutomationPackageUpdateParameterBuilder withApSource(AutomationPackageFileSource apSource) {
        this.apSource = apSource;
        return this;
    }

    public AutomationPackageUpdateParameterBuilder withApLibrarySource(AutomationPackageFileSource apLibrarySource) {
        this.apLibrarySource = apLibrarySource;
        return this;
    }

    public AutomationPackageUpdateParameterBuilder withVersionName(String automationPackageVersion) {
        this.versionName = automationPackageVersion;
        return this;
    }

    public AutomationPackageUpdateParameterBuilder withActivationExpression(String activationExpression) {
        this.activationExpression = activationExpression;
        return this;
    }

    public AutomationPackageUpdateParameterBuilder withEnricher(ObjectEnricher enricher) {
        this.enricher = enricher;
        return this;
    }

    public AutomationPackageUpdateParameterBuilder withObjectPredicate(ObjectPredicate objectPredicate) {
        this.objectPredicate = objectPredicate;
        return this;
    }

    public AutomationPackageUpdateParameterBuilder withWriteAccessValidator(WriteAccessValidator writeAccessValidator) {
        this.writeAccessValidator = writeAccessValidator;
        return this;
    }

    public AutomationPackageUpdateParameterBuilder withAsync(boolean async) {
        this.async = async;
        return this;
    }

    public AutomationPackageUpdateParameterBuilder withActorUser(String actorUser) {
        this.actorUser = actorUser;
        return this;
    }

    public AutomationPackageUpdateParameterBuilder withForceRefreshOfSnapshots(boolean forceRefreshOfSnapshots) {
        this.forceRefreshOfSnapshots = forceRefreshOfSnapshots;
        return this;
    }

    public AutomationPackageUpdateParameterBuilder withCheckForSameOrigin(boolean checkForSameOrigin) {
        this.checkForSameOrigin = checkForSameOrigin;
        return this;
    }

    public AutomationPackageUpdateParameterBuilder withFunctionsAttributes(Map<String, String> functionsAttributes) {
        this.functionsAttributes = functionsAttributes;
        return this;
    }

    public AutomationPackageUpdateParameterBuilder withPlansAttributes(Map<String, String> plansAttributes) {
        this.plansAttributes = plansAttributes;
        return this;
    }

    public AutomationPackageUpdateParameterBuilder withTokenSelectionCriteria(Map<String, String> tokenSelectionCriteria) {
        this.tokenSelectionCriteria = tokenSelectionCriteria;
        return this;
    }

    public AutomationPackageUpdateParameterBuilder withExecuteFunctionsLocally(boolean executeFunctionsLocally) {
        this.executeFunctionsLocally =  executeFunctionsLocally;
        return this;
    }

    public AutomationPackageUpdateParameterBuilder forRedeployPackage(ObjectHookRegistry objectHookRegistry, AutomationPackage oldPackage, AutomationPackageUpdateParameter parentParameters) {
        this.allowUpdate = true;
        this.allowCreate = false;
        this.explicitOldId = oldPackage.getId();
        this.isClasspathBased = parentParameters.isClasspathBased;
        String automationPackageResource = oldPackage.getAutomationPackageResource();
        if (FileResolver.isResource(automationPackageResource)) {
            this.apSource = AutomationPackageFileSource.withResourceId(FileResolver.resolveResourceId(automationPackageResource));
        }
        String automationPackageLibraryResource = oldPackage.getAutomationPackageLibraryResource();
        if (FileResolver.isResource(automationPackageLibraryResource)) {
            this.apLibrarySource = AutomationPackageFileSource.withResourceId(FileResolver.resolveResourceId(automationPackageLibraryResource));
        }
        this.versionName = oldPackage.getVersionName();
        this.activationExpression = oldPackage.getActivationExpression() != null ? oldPackage.getActivationExpression().getScript() : null;

        //We need to rebuild the context to get the proper object enricher of the provided package
        AbstractContext context = new AbstractContext() { };
        try {
            objectHookRegistry.rebuildContext(context, oldPackage);
        } catch (Exception e) {
            String errorMessage = "Error while rebuilding context for origin automation package " + AutomationPackageManager.getLogRepresentation(oldPackage);
            throw new RuntimeException(errorMessage, e);
        }
        this.enricher = objectHookRegistry.getObjectEnricher(context);
        //The access checks ensure that we can modify the resources used by automation packages. Reloading automation package that chose to use these resources is always allowed
        this.objectPredicate =  o -> true;
        this.writeAccessValidator = NO_CHECKS_VALIDATOR;
        this.async = false;
        this.actorUser = parentParameters.actorUser;
        this.forceRefreshOfSnapshots = false;
        this.checkForSameOrigin = false;
        this.functionsAttributes = oldPackage.getFunctionsAttributes();
        this.plansAttributes = oldPackage.getPlansAttributes();
        this.tokenSelectionCriteria = oldPackage.getTokenSelectionCriteria();
        this.executeFunctionsLocally = oldPackage.getExecuteFunctionsLocally();
        this.reloading = true;
        return this;
    }

    /**
     * helper setting automatically the properties allowCreate, AllowUpdate and explicitOldId to match create only use cases
     */
    public AutomationPackageUpdateParameterBuilder withCreateOnly() {
        this.allowCreate = true;
        this.allowUpdate = false;
        this.explicitOldId = null;
        return this;
    }

    /**
     * helper method for junit test, use dummy predicate and enricher
     * @return the builder
     */
    public AutomationPackageUpdateParameterBuilder forJunit() {
        this.objectPredicate = o -> true;
        this.writeAccessValidator = NO_CHECKS_VALIDATOR;
        this.actorUser = "testUser";
        return this;
    }

    /**
     * helper setting automatically the properties to match CLI local executions
     */
    public AutomationPackageUpdateParameterBuilder forLocalExecution() {
        this.objectPredicate = o -> true;
        this.writeAccessValidator = NO_CHECKS_VALIDATOR;
        this.explicitOldId = null;
        this.async = false;
        this.actorUser = null;
        this.versionName = null;
        this.activationExpression = null;
        this.forceRefreshOfSnapshots = false;
        this.checkForSameOrigin = true;
        return this;
    }

    public AutomationPackageUpdateParameter build() {
        return new AutomationPackageUpdateParameter(allowUpdate, allowCreate, isClasspathBased, explicitOldId, apSource,
                apLibrarySource, versionName, activationExpression, enricher, objectPredicate, writeAccessValidator,
                async, actorUser, forceRefreshOfSnapshots, checkForSameOrigin, functionsAttributes, plansAttributes,
                tokenSelectionCriteria, executeFunctionsLocally, reloading);
    }



}