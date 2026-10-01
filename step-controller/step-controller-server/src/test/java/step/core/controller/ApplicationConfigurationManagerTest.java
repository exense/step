package step.core.controller;

import ch.exense.commons.app.Configuration;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Test;
import step.core.accessors.DefaultJacksonMapperProvider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ApplicationConfigurationManagerTest {

    @Test
    public void reportingFlagsDefaultToFalse() {
        JsonNode applicationConfiguration = serializeConfiguration(new Configuration());

        assertTrue(applicationConfiguration.has("disableLegacyReporting"));
        assertFalse(applicationConfiguration.get("disableLegacyReporting").asBoolean());
        assertFalse(applicationConfiguration.get("forceLegacyReporting").asBoolean());
    }

    @Test
    public void disableLegacyReportingIsExposedAlongsideForceLegacyReporting() {
        Configuration configuration = new Configuration();
        configuration.putProperty("ui.reporting.disable.legacy", "true");
        configuration.putProperty("ui.reporting.force.legacy", "true");
        configuration.putProperty("ui.title", "Custom Step");

        JsonNode applicationConfiguration = serializeConfiguration(configuration);

        assertTrue(applicationConfiguration.path("disableLegacyReporting").asBoolean());
        assertTrue(applicationConfiguration.get("forceLegacyReporting").asBoolean());
        assertEquals("Custom Step", applicationConfiguration.get("title").asText());
    }

    @Test
    public void disableLegacyReportingCanBeExplicitlyDisabled() {
        Configuration configuration = new Configuration();
        configuration.putProperty("ui.reporting.disable.legacy", "false");
        configuration.putProperty("ui.reporting.force.legacy", "true");

        JsonNode applicationConfiguration = serializeConfiguration(configuration);

        assertTrue(applicationConfiguration.has("disableLegacyReporting"));
        assertFalse(applicationConfiguration.get("disableLegacyReporting").asBoolean());
        assertTrue(applicationConfiguration.get("forceLegacyReporting").asBoolean());
    }

    private JsonNode serializeConfiguration(Configuration configuration) {
        ApplicationConfiguration applicationConfiguration = new ApplicationConfigurationManager()
            .getDefaultBuilder(configuration).build();
        return DefaultJacksonMapperProvider.getObjectMapper().valueToTree(applicationConfiguration);
    }
}
