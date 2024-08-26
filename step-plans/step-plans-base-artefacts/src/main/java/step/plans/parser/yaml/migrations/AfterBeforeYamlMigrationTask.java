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

@YamlPlanMigration
public class AfterBeforeYamlMigrationTask extends AbstractYamlPlanMigrationTask {

    private static final Logger logger = LoggerFactory.getLogger(AfterBeforeYamlMigrationTask.class);

    private static final String ARTEFACT_CHILDREN = "children";
    private static final String ARTEFACT_BEFORE_PROPERTY = "before";
    private static final String ARTEFACT_AFTER_PROPERTY = "after";
    private static final String ARTEFACT_BEFORE_SEQUENCE = "beforeSequence";
    private static final String ARTEFACT_AFTER_SEQUENCE = "afterSequence";
    private static final String ARTEFACT_BEFORE_THREAD = "beforeThread";
    private static final String ARTEFACT_AFTER_THREAD = "afterThread";
    private static final String ARTEFACT_AFTER_THREAD_PROPERTY = "afterThread";
    private static final String ARTEFACT_BEFORE_THREAD_PROPERTY = "beforeThread";
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

    /* yaml structure example as ref
      testCase:
        nodeName: "MainPlan"
        ...
        children:
         - beforeSequence:
             children:
               - ...
     */

    private void migrateBeforeAfterSequnce(DocumentObject artifact) {
        //should only return one entry based on our Yaml syntax but keep it generic
        Set<String> artefactNames = artifact.keySet();
        for (String artefactName : artefactNames) {
            DocumentObject artifactProperties = artifact.getObject(artefactName);
            List<DocumentObject> children = artifactProperties.getArray(ARTEFACT_CHILDREN);
            if (children != null && !children.isEmpty()) {
                //Attach the children of the beforeSequence to the parent before property
                processBeforeSequence(artifactProperties, children);
                //Attach the children of the afterSequence to the parent after property
                processAfterSequence(artifactProperties, children);
                processBeforeThread(artifactProperties, children);
                processAfterThread(artifactProperties, children);
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
        List<DocumentObject> fromArray = sourceChildren.stream().filter(child -> child.containsKey(fromArtefactKey)).collect(Collectors.toList());
        for (DocumentObject artifact: fromArray) {
            Set<String> artefactNames = artifact.keySet();
            for (String artefactName : artefactNames) {
                DocumentObject artifactProperties = artifact.getObject(artefactName);
                List<DocumentObject> innerChildren = artifactProperties.getArray(ARTEFACT_CHILDREN);
                if (innerChildren != null && !innerChildren.isEmpty()) {
                    DocumentObject toProperty = sourceArtifactProperties.getObject(toPropertyKey);
                    if (toProperty == null) {
                        toProperty = new DocumentObject();
                        toProperty.put("continueOnError", artifactProperties.getBoolean("continueOnError"));
                        sourceArtifactProperties.put(toPropertyKey, toProperty);
                    }
                    List<DocumentObject> toSteps = toProperty.getArray("steps");
                    if (toSteps == null) {
                        toSteps = new ArrayList<>();
                        toProperty.put("steps", toSteps);
                    }
                    toSteps.addAll(innerChildren);
                    toProperty.put(STEPS, toSteps);
                    //Also process children of legacy before,after Sequence,thread recursively
                    moveToParentSource(artifactProperties, innerChildren, fromArtefactKey, toPropertyKey);
                }
            }
        }
        sourceChildren.removeAll(fromArray);
    }
}
