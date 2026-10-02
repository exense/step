package step.plugins.jmeter;

import ch.exense.commons.app.Configuration;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static step.plugins.jmeter.JMeterFunctionTypeLocalPlugin.JMETER_HOME_ENV_VAR;

public class JMeterEnvironmentConfigurationTest {

    @Test
    public void configuresTheHomeFromTheEnvironment() {
        Configuration configuration = new Configuration();
        JMeterFunctionTypeLocalPlugin.applyEnvironmentConfiguration(configuration, Map.of(JMETER_HOME_ENV_VAR, "/opt/jmeter"));

        assertEquals("/opt/jmeter", configuration.getProperty(JMeterFunctionType.JMETER_HOME_CONFIG_PROPERTY));
        assertEquals("The 'JMETER_HOME' environment variable is not set.",
            configuration.getProperty(JMeterFunctionType.MISSING_JMETER_HOME_MESSAGE_PROPERTY));
    }

    @Test
    public void reportsTheMissingEnvironmentVariable() {
        Configuration configuration = new Configuration();
        JMeterFunctionTypeLocalPlugin.applyEnvironmentConfiguration(configuration, Map.of());

        assertNull(configuration.getProperty(JMeterFunctionType.JMETER_HOME_CONFIG_PROPERTY));
        assertEquals("The 'JMETER_HOME' environment variable is not set.",
            configuration.getProperty(JMeterFunctionType.MISSING_JMETER_HOME_MESSAGE_PROPERTY));
    }
}
