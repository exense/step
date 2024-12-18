package step.migration.tasks;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;
import step.core.accessors.DefaultJacksonMapperProvider;
import step.core.collections.Collection;
import step.core.collections.Document;
import step.core.collections.Filters;
import step.core.collections.inmemory.InMemoryCollectionFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

public class MigrateBeforeAfterAndPropertiesArtefactInPlansTest {

    @Test
    public void testMigrateBeforeAfterArtefactInPlan() throws IOException {
        try (InputStream is = this.getClass().getResourceAsStream("oldDBPlanWithBeforeAfter.json");
                InputStream expectedIS = this.getClass().getResourceAsStream("newDBPlanWithBeforeAfter.json")) {
            ObjectMapper mapper = DefaultJacksonMapperProvider.getObjectMapper();
            Document oldPlan = mapper.readValue(is, Document.class);
            InMemoryCollectionFactory collectionFactory = new InMemoryCollectionFactory(new Properties());
            Collection<step.core.collections.Document> plans = collectionFactory.getCollection("plans", step.core.collections.Document.class);
            plans.save(oldPlan);
            MigrateBeforeAfterAndPropertiesArtefactInPlans migrateBeforeAfterArtefactInPlans = new MigrateBeforeAfterAndPropertiesArtefactInPlans(collectionFactory, null);
            migrateBeforeAfterArtefactInPlans.runUpgradeScript();
            Document newPlan = plans.find(Filters.empty(), null, null, null, 0).findFirst().orElseThrow(() -> new RuntimeException("No plans found in collection"));
            String actual = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(newPlan);
            String expected = new String(expectedIS.readAllBytes(), StandardCharsets.UTF_8);
            Assert.assertEquals(expected, actual.replace("\r", ""));
        }
    }
}
