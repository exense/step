package step.migration.tasks;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;
import step.core.accessors.AbstractAccessor;
import step.core.accessors.DefaultJacksonMapperProvider;
import step.core.collections.Collection;
import step.core.collections.Document;
import step.core.collections.EntityVersion;
import step.core.collections.Filters;
import step.core.collections.inmemory.InMemoryCollectionFactory;
import step.core.plans.Plan;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

import static step.core.collections.CollectionFactory.VERSION_COLLECTION_SUFFIX;

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
            Document entityVersion = new Document();
            Document oldPlanVersion = new Document(oldPlan);
            oldPlanVersion.put("_entityClass","step.core.plans.Plan");
            entityVersion.put("entity",oldPlanVersion);
            entityVersion.put("id","66cc86aa5b5628560a04dfb5");
            entityVersion.put("updateTime", System.currentTimeMillis());
            entityVersion.put("updateGroupTime", System.currentTimeMillis());
            Collection<Document> versionedPlans = collectionFactory.getCollection("plans"+VERSION_COLLECTION_SUFFIX, Document.class);
            versionedPlans.save(entityVersion);
            //Test migration
            MigrateBeforeAfterAndPropertiesArtefactInPlans migrateBeforeAfterArtefactInPlans = new MigrateBeforeAfterAndPropertiesArtefactInPlans(collectionFactory, null);
            migrateBeforeAfterArtefactInPlans.runUpgradeScript();
            Document newPlan = plans.find(Filters.empty(), null, null, null, 0).findFirst().orElseThrow(() -> new RuntimeException("No plans found in collection"));
            String actual = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(newPlan);
            String expected = new String(expectedIS.readAllBytes(), StandardCharsets.UTF_8);

            // use object mapper for a "fair" comparison (otherwise the assertion fails on some systems due to differences in line separators)
            Assert.assertEquals(mapper.readTree(expected.replace("\r", "")), mapper.readTree(actual.replace("\r", "")));

            //Make sure new model works
            Collection<Plan> plans1 = collectionFactory.getCollection("plans", Plan.class);
            Collection<EntityVersion> versionCollection = collectionFactory.getCollection("plans"+VERSION_COLLECTION_SUFFIX, EntityVersion.class);
            AbstractAccessor<Plan> planAbstractAccessor = new AbstractAccessor<>(plans1);
            planAbstractAccessor.enableVersioning(versionCollection, 1000L);
            Plan plan = plans1.find(Filters.empty(), null, null, null, 0).findFirst().orElseThrow(() -> new RuntimeException("No plans found in collection"));
            planAbstractAccessor.save(plan);


        }
    }
}
