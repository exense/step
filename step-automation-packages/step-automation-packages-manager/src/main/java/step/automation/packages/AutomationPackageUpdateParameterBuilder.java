package step.automation.packages;

import org.bson.types.ObjectId;
import step.attachments.FileResolver;
import step.core.objectenricher.*;

import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class AutomationPackageUpdateParameterBuilder {
    private boolean allowUpdate = true;
    private boolean allowCreate = true;
    private boolean isLocalPackage = false;
    private ObjectId explicitOldId = null;
    private AutomationPackageFileSource apSource;
    private AutomationPackageFileSource apLibrarySource;
    private String automationPackageVersion = null;
    private String activationExpression = null;
    private ObjectEnricher enricher;
    private ObjectPredicate objectPredicate;
    private WriteAccessValidator writeAccessValidator;
    private boolean async = false;
    private String actorUser;
    private boolean allowUpdateOfOtherPackages = false;
    private boolean checkForSameOrigin = true;
    private Map<String, String> functionsAttributes;
    private Map<String, String> plansAttributes;
    private Map<String, String> tokenSelectionCriteria;
    private boolean executeFunctionLocally;

    public AutomationPackageUpdateParameterBuilder withAllowUpdate(boolean allowUpdate) {
        this.allowUpdate = allowUpdate;
        return this;
    }

    public AutomationPackageUpdateParameterBuilder withAllowCreate(boolean allowCreate) {
        this.allowCreate = allowCreate;
        return this;
    }

    public AutomationPackageUpdateParameterBuilder withIsLocalPackage(boolean isLocalPackage) {
        this.isLocalPackage = isLocalPackage;
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

    public AutomationPackageUpdateParameterBuilder withAutomationPackageVersion(String automationPackageVersion) {
        this.automationPackageVersion = automationPackageVersion;
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

    public AutomationPackageUpdateParameterBuilder withAllowUpdateOfOtherPackages(boolean allowUpdateOfOtherPackages) {
        this.allowUpdateOfOtherPackages = allowUpdateOfOtherPackages;
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

    public AutomationPackageUpdateParameterBuilder withExecuteFunctionLocally(boolean executeFunctionLocally) {
        this.executeFunctionLocally =  executeFunctionLocally;
        return this;
    }

    public AutomationPackageUpdateParameterBuilder forRedeployPackage(AutomationPackage oldPackage, AutomationPackageUpdateParameter parentParameters) {
        this.allowUpdate = true;
        this.allowCreate = false;
        this.explicitOldId = oldPackage.getId();
        this.isLocalPackage = parentParameters.isLocalPackage;
        String automationPackageResource = oldPackage.getAutomationPackageResource();
        if (FileResolver.isResource(automationPackageResource)) {
            this.apSource = AutomationPackageFileSource.withResourceId(FileResolver.resolveResourceId(automationPackageResource));
        }
        String automationPackageLibraryResource = oldPackage.getAutomationPackageLibraryResource();
        if (FileResolver.isResource(automationPackageLibraryResource)) {
            this.apLibrarySource = AutomationPackageFileSource.withResourceId(FileResolver.resolveResourceId(automationPackageLibraryResource));
        }
        this.automationPackageVersion = oldPackage.getVersion();
        this.activationExpression = oldPackage.getActivationExpression() != null ? oldPackage.getActivationExpression().getScript() : null;
        this.enricher = parentParameters.enricher;
        this.objectPredicate = parentParameters.objectPredicate;
        this.writeAccessValidator = parentParameters.writeAccessValidator;
        this.async = false;
        this.actorUser = parentParameters.actorUser;
        this.allowUpdateOfOtherPackages = false;
        this.checkForSameOrigin = false;
        this.functionsAttributes = oldPackage.getFunctionsAttributes();
        this.plansAttributes = oldPackage.getPlansAttributes();
        this.tokenSelectionCriteria = oldPackage.getTokenSelectionCriteria();
        this.executeFunctionLocally = oldPackage.getExecuteFunctionLocally();
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
        this.writeAccessValidator = getNoWriteAccessValidator();
        this.actorUser = "testUser";
        return this;
    }

    public static WriteAccessValidator getNoWriteAccessValidator() {
        return new WriteAccessValidator(null, null) {

            @Override
            public Optional<ObjectAccessException> validate(EnricheableObject entity) {
                return Optional.empty();
            }
        };
    }

    /**
     * helper setting automatically the properties to match local use cases
     */
    public AutomationPackageUpdateParameterBuilder forLocalExecution() {
        this.isLocalPackage = true;
        this.objectPredicate = o -> true;
        this.writeAccessValidator = getNoWriteAccessValidator();
        this.explicitOldId = null;
        this.async = false;
        this.actorUser = null;
        this.automationPackageVersion = null;
        this.activationExpression = null;
        this.allowUpdateOfOtherPackages = false;
        this.checkForSameOrigin = true;
        return this;
    }

    public AutomationPackageUpdateParameter build() {
        return new AutomationPackageUpdateParameter(allowUpdate, allowCreate, isLocalPackage, explicitOldId, apSource, apLibrarySource, automationPackageVersion, activationExpression, enricher, objectPredicate, writeAccessValidator, async, actorUser, allowUpdateOfOtherPackages, checkForSameOrigin, functionsAttributes, plansAttributes, tokenSelectionCriteria, executeFunctionLocally);
    }



}