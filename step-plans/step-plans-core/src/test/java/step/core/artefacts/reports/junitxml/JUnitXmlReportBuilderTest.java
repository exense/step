package step.core.artefacts.reports.junitxml;

import jakarta.xml.bind.JAXBException;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlunit.assertj.XmlAssert;
import step.core.artefacts.reports.junitxml.model.TestSuite;

public class JUnitXmlReportBuilderTest {

    private static final Logger log = LoggerFactory.getLogger(JUnitXmlReportBuilderTest.class);

    private static final String EXPECTED_1 = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
            "<testSuite errors=\"0\" failures=\"0\" name=\"step.plans.parser.yaml.YamlPlanReaderTest\" skipped=\"0\" tests=\"0\" time=\"2.551\"/>";
    @Test
    public void testMarshall() throws JAXBException {
        TestSuite testSuite = new TestSuite();
        testSuite.setName("testName");
        testSuite.setTime("2.551");
        testSuite.setName("step.plans.parser.yaml.YamlPlanReaderTest");

        String res = JUnitXmlReportBuilder.buildXmlForSuite(testSuite);
        log.info("Output: {}", res);

        XmlAssert.assertThat(res).and(EXPECTED_1).areSimilar();
    }


}