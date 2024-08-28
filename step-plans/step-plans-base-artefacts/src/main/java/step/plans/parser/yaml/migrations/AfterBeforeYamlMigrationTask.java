package step.plans.parser.yaml.migrations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.Version;
import step.core.collections.CollectionFactory;
import step.core.collections.Document;
import step.core.collections.DocumentObject;
import step.core.collections.Filters;
import step.migration.MigrationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * This migration task will migrate all plans to be compatible with the new artefact properties (before and after for
 * all artefacts, beforeThread and afterThread for thread group). Previously specifc artefact (i.e. BeforeSequence) were
 * used directly as children "property" and need to be moved over to the new class property.
 * The implementation is similar to the JSON migration taks for the "DB" syntax in MigrateBeforeAfterArtefactInPlans
 */
@YamlPlanMigration
public class AfterBeforeYamlMigrationTask extends AbstractYamlPlanMigrationTask {

    private static final Logger logger = LoggerFactory.getLogger(AfterBeforeYamlMigrationTask.class);

    public static final String ARTEFACT_CHILDREN = "children";
    public static final String ARTEFACT_BEFORE_PROPERTY = "before";
    public static final String ARTEFACT_AFTER_PROPERTY = "after";
    private static final String ARTEFACT_BEFORE_SEQUENCE = "beforeSequence";
    private static final String ARTEFACT_AFTER_SEQUENCE = "afterSequence";
    private static final String ARTEFACT_BEFORE_THREAD = "beforeThread";
    private static final String ARTEFACT_AFTER_THREAD = "afterThread";
    public static final String ARTEFACT_AFTER_THREAD_PROPERTY = "afterThread";
    public static final String ARTEFACT_BEFORE_THREAD_PROPERTY = "beforeThread";
    private static final String ARTEFACT_PERFORMANCE_ASSERT = "performanceAssert";
    public static final String STEPS = "steps";

    public AfterBeforeYamlMigrationTask(CollectionFactory collectionFactory, MigrationContext migrationContext) {
        super(new Version(1,1,0), collectionFactory, migrationContext);
    }

    @Override
    public void runUpgradeScript() {
        AtomicInteger errorCount = new AtomicInteger(0);
        AtomicInteger successCount = new AtomicInteger(0);
        Stream<Document> allYamlPlans = yamlPlansCollection.find(Filters.empty(), null, null, null, 0);
        allYamlPlans.forEach(new Consumer<>() {
            @Override
            public void accept(Document document) {
                try {
                    DocumentObject root = document.getObject("root");
                    migrateBeforeAfterSequnce(root);
                    yamlPlansCollection.save(document);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    logger.error("Unable to migrate Yaml plan: {}", document, e);
                    errorCount.incrementAndGet();
                }
            }
        });

        logger.info("Migrated {} Yaml plans successfully", successCount.get() );
        if (errorCount.get() > 0) {
            logger.error("Failed to migrate {} plans. Check previous error for details.", errorCount.get());
        }
    }

    /**
     * Yaml and JSON document are deserialized with different structure. YAML syntax for artefact always start with
     * artefact name followed by the object which is different from Json (DataBase) structure
     *          YAML structure example as ref
     *           testCase:
     *             nodeName: "MainPlan"
     *             ...
     *             children:
     *              - beforeSequence:
     *                  children:
     *                    - ...
     *
     * @param artifact: the deserilaization of the YAML artefact as DocumentObject to be modified
     */
    private void migrateBeforeAfterSequnce(DocumentObject artifact) {
        //should only return one entry based on our Yaml syntax but keep it generic
        Set<String> artefactNames = artifact.keySet();
        for (String artefactName : artefactNames) {
            DocumentObject artifactProperties = artifact.getObject(artefactName);
            List<DocumentObject> children = artifactProperties.getArray(ARTEFACT_CHILDREN);
            if (children != null && !children.isEmpty()) {
                //Attach the children of the beforeSequence to the parent before property
                moveToParentSource(artifactProperties, children, ARTEFACT_BEFORE_SEQUENCE, ARTEFACT_BEFORE_PROPERTY);
                //Attach the children of the afterSequence to the parent after property
                moveToParentSource(artifactProperties, children, ARTEFACT_AFTER_SEQUENCE, ARTEFACT_AFTER_PROPERTY);
                moveToParentSource(artifactProperties, children, ARTEFACT_BEFORE_THREAD, ARTEFACT_BEFORE_THREAD_PROPERTY);
                moveToParentSource(artifactProperties, children, ARTEFACT_AFTER_THREAD, ARTEFACT_AFTER_THREAD_PROPERTY);
                moveToParentSource(artifactProperties, children, ARTEFACT_PERFORMANCE_ASSERT, ARTEFACT_AFTER_PROPERTY, false);
                //getArray return a copy, we must explicitly set it back
                artifactProperties.put(ARTEFACT_CHILDREN, children);
                //Process children recursively
                for (DocumentObject child : children) {
                    migrateBeforeAfterSequnce(child);
                }
            }
        }

    }

    private void processBeforeSequence(DocumentObject sourceArtifactProperties, List<DocumentObject> sourceChildren) {
        moveToParentSource(sourceArtifactProperties, sourceChildren, ARTEFACT_BEFORE_SEQUENCE, ARTEFACT_BEFORE_PROPERTY);
    }

    private void processAfterSequence(DocumentObject sourceArtifactProperties, List<DocumentObject> sourceChildren) {
        moveToParentSource(sourceArtifactProperties, sourceChildren, ARTEFACT_AFTER_SEQUENCE, ARTEFACT_AFTER_PROPERTY);
    }

    private void processBeforeThread(DocumentObject sourceArtifactProperties, List<DocumentObject> sourceChildren) {
        moveToParentSource(sourceArtifactProperties, sourceChildren, ARTEFACT_BEFORE_THREAD, ARTEFACT_BEFORE_THREAD_PROPERTY);
    }

    private void processAfterThread(DocumentObject sourceArtifactProperties, List<DocumentObject> sourceChildren) {
        moveToParentSource(sourceArtifactProperties, sourceChildren, ARTEFACT_AFTER_THREAD, ARTEFACT_AFTER_THREAD_PROPERTY);
    }

    private void moveToParentSource(DocumentObject sourceArtifactProperties, List<DocumentObject> sourceChildren, String fromArtefactKey, String toPropertyKey) {
        moveToParentSource(sourceArtifactProperties, sourceChildren, fromArtefactKey, toPropertyKey, true);
    }

    private void moveToParentSource(DocumentObject sourceArtifactProperties, List<DocumentObject> sourceChildren, String fromArtefactKey, String toPropertyKey, boolean moveChildrenOnly) {
        List<DocumentObject> fromArray = sourceChildren.stream().filter(child -> child.containsKey(fromArtefactKey)).collect(Collectors.toList());
        for (DocumentObject artifact: fromArray) {
            if (moveChildrenOnly) {
                //This case is used for legacy beforeSequence... artefacts, we move their children to the corresponding source property (i.e. new before property)
                Set<String> artefactNames = artifact.keySet();
                for (String artefactName : artefactNames) {
                    DocumentObject artifactProperties = artifact.getObject(artefactName);
                    List<DocumentObject> innerChildren = artifactProperties.getArray(ARTEFACT_CHILDREN);
                    if (innerChildren != null && !innerChildren.isEmpty()) {
                        List<DocumentObject> toSteps = getOrInitPropertySteps(sourceArtifactProperties, artifactProperties.getBoolean("continueOnError"), toPropertyKey);
                        toSteps.addAll(innerChildren);

                        //Also process children of legacy before,after Sequence,thread recursively
                        moveToParentSource(artifactProperties, innerChildren, fromArtefactKey, toPropertyKey);
                    }
                }
            } else {
                //This case is used for instance for PerformanceAssert artefact moved from children to the after children block property
                //Add this children artefact directly to the source property 'toPropertyKey', if the target property is not yet defined use false as default for continueOnError
                List<DocumentObject> toSteps = getOrInitPropertySteps(sourceArtifactProperties, false, toPropertyKey);
                toSteps.add(artifact);
            }
        }
        sourceChildren.removeAll(fromArray);
    }

    public static List<DocumentObject> getOrInitPropertySteps(DocumentObject sourceArtifactProperties, Object continueOnError, String toPropertyKey){
        DocumentObject toProperty = sourceArtifactProperties.getObject(toPropertyKey);
        if (toProperty == null) {
            toProperty = new DocumentObject();
            toProperty.put("continueOnError", continueOnError);
            sourceArtifactProperties.put(toPropertyKey, toProperty);
        }
        List<DocumentObject> toSteps = toProperty.getArray("steps");
        if (toSteps == null) {
            toSteps = new ArrayList<>();
            toProperty.put("steps", toSteps);
        }
        toProperty.put(STEPS, toSteps);
        return toSteps;
    }
}
