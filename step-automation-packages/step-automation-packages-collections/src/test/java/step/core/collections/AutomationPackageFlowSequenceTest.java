package step.core.collections;

import org.junit.Before;
import org.junit.Test;
import step.automation.packages.AutomationPackageReadingException;
import step.core.plans.Plan;
import step.plans.parser.yaml.YamlPlan;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

/**
 * The descriptor references its fragments as a flow sequence ("fragments: [...]"), whose items share their line with
 * the sequence: saving a plan of the descriptor writes it with the sequence as a block sequence
 */
public class AutomationPackageFlowSequenceTest extends AutomationPackageCollectionTestBase {

    private Collection<Plan> planCollection;

    public AutomationPackageFlowSequenceTest() {
        super(new File("src/test/resources/testdata/ap-with-flow-sequence"));
    }

    @Before
    public void setUp() throws IOException, AutomationPackageReadingException {
        super.setUp();
        AutomationPackageCollectionFactory collectionFactory = new AutomationPackageCollectionFactory(new Properties(), fragmentManager);
        planCollection = collectionFactory.getCollection(YamlPlan.PLANS_ENTITY_NAME, Plan.class);
    }

    @Test
    public void planOfADescriptorWithAFlowSequenceIsSaved() throws IOException {
        Plan plan = planCollection.find(Filters.equals("attributes.name", "Exense"), null, null, null, 0).findFirst().orElseThrow();
        plan.getRoot().getChildren().get(0).getAttributes().put("name", "Go to the exense website");
        planCollection.save(plan);

        assertFilesEqual(expectedFilesPath.resolve("descriptorWithFlowSequenceAfterPlanSave.yaml"),
            destinationDirectory.toPath().resolve("automation-package.yaml"));
    }
}
