package step.reporting;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmlunit.assertj.XmlAssert;

public class JunitReportEntryCollectorTest {

    private static final Logger log = LoggerFactory.getLogger(JunitReportEntryCollectorTest.class);

    /**
     * Generate several failures and ignore 'skipped'
     */
    @Test
    public void testAggregateSeveralFailures(){
        JUnit4ReportWriter.JUnitReportEntryCollector collector = new JUnit4ReportWriter.JUnitReportEntryCollector();
        collector.add(new JUnit4ReportWriter.JUnitReportEntry(JUnit4ReportWriter.JUnitReportEntry.Type.FAILURE, null, "Failure message 1"));
        collector.add(new JUnit4ReportWriter.JUnitReportEntry(JUnit4ReportWriter.JUnitReportEntry.Type.FAILURE, null, "Failure message 2"));
        collector.add(new JUnit4ReportWriter.JUnitReportEntry(JUnit4ReportWriter.JUnitReportEntry.Type.SKIPPED, null, "Skipped message 1"));

        String result = getMergedEntriesAsXml(collector);
        String expected = "<testcase>\n" +
                "<failure message=\"Failure message 1\"/>\n" +
                "<failure message=\"Failure message 2\"/>\n" +
                "</testcase>";

        XmlAssert.assertThat(result)
                .and(expected)
                .areSimilar()
                .ignoreWhitespace();
    }

    /**
     * If 'error' exists, we aggregate all messages in single 'error' node
     */
    @Test
    public void testAggregateFailuresAndErrors(){
        JUnit4ReportWriter.JUnitReportEntryCollector collector = new JUnit4ReportWriter.JUnitReportEntryCollector();
        collector.add(new JUnit4ReportWriter.JUnitReportEntry(JUnit4ReportWriter.JUnitReportEntry.Type.ERROR, null, "Error message 1"));
        collector.add(new JUnit4ReportWriter.JUnitReportEntry(JUnit4ReportWriter.JUnitReportEntry.Type.ERROR, null, "Error message 2"));
        collector.add(new JUnit4ReportWriter.JUnitReportEntry(JUnit4ReportWriter.JUnitReportEntry.Type.FAILURE, null, "Failure message 1"));
        collector.add(new JUnit4ReportWriter.JUnitReportEntry(JUnit4ReportWriter.JUnitReportEntry.Type.FAILURE, null, "Failure message 2"));
        collector.add(new JUnit4ReportWriter.JUnitReportEntry(JUnit4ReportWriter.JUnitReportEntry.Type.SKIPPED, null, "Skipped message 1"));

        String result = getMergedEntriesAsXml(collector);
        String expected = "<testcase>\n" +
                "<error message=\"Error message 1; Error message 2; Failure message 1; Failure message 2\"/>\n" +
                "</testcase>";

        XmlAssert.assertThat(result)
                .and(expected)
                .areSimilar()
                .ignoreWhitespace();
    }

    /**
     * If there are 'skipped' elements ONLY, we generate the single `skipped` tag with aggregated messages
     */
    @Test
    public void testAggregateSkippedWithMessages(){
        JUnit4ReportWriter.JUnitReportEntryCollector collector = new JUnit4ReportWriter.JUnitReportEntryCollector();
        collector.add(new JUnit4ReportWriter.JUnitReportEntry(JUnit4ReportWriter.JUnitReportEntry.Type.SKIPPED, null, "Skipped message 1"));
        collector.add(new JUnit4ReportWriter.JUnitReportEntry(JUnit4ReportWriter.JUnitReportEntry.Type.SKIPPED, null, "Skipped message 2"));

        String result = getMergedEntriesAsXml(collector);
        String expected = "<testcase>\n" +
                "<skipped message=\"Skipped message 1; Skipped message 2\"/>\n" +
                "</testcase>";

        XmlAssert.assertThat(result)
                .and(expected)
                .areSimilar()
                .ignoreWhitespace();
    }

    /**
     * If there are 'skipped' elements ONLY, we generate the single `skipped` tag. The generated tag contains NO message text,
     * if there is no details provided in step report nodes
     */
    @Test
    public void testAggregateSkippedWithoutMessages(){
        // empty messages
        JUnit4ReportWriter.JUnitReportEntryCollector collector = new JUnit4ReportWriter.JUnitReportEntryCollector();
        collector.add(new JUnit4ReportWriter.JUnitReportEntry(JUnit4ReportWriter.JUnitReportEntry.Type.SKIPPED, null, null));
        collector.add(new JUnit4ReportWriter.JUnitReportEntry(JUnit4ReportWriter.JUnitReportEntry.Type.SKIPPED, null, ""));

        String result = getMergedEntriesAsXml(collector);
        String expected = "<testcase>\n" +
                "<skipped/>\n" +
                "</testcase>";

        XmlAssert.assertThat(result)
                .and(expected)
                .areSimilar()
                .ignoreWhitespace();
    }


    protected String getMergedEntriesAsXml(JUnit4ReportWriter.JUnitReportEntryCollector collector){
        StringBuilder builder = new StringBuilder().append("<testcase>").append("\n");
        for (JUnit4ReportWriter.JUnitReportEntry mergedEntry : collector.getMergedEntries()) {
            builder.append(mergedEntry.toXml());
            builder.append("\n");
        }
        builder.append("</testcase>");

        log.info(builder.toString());
        return builder.toString();
    }

}