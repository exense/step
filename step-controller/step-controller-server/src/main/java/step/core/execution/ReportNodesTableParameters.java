package step.core.execution;

import step.framework.server.tables.service.TableParameters;

import java.util.List;

public class ReportNodesTableParameters extends TableParameters {
    private String eid;
    private List<String> testcases;
    private boolean enrichCallKeywordWithAssertionErrors;

    public String getEid() {
        return eid;
    }

    public void setEid(String eid) {
        this.eid = eid;
    }

    public List<String> getTestcases() {
        return testcases;
    }

    public void setTestcases(List<String> testcases) {
        this.testcases = testcases;
    }

    public boolean isEnrichCallKeywordWithAssertionErrors() {
        return enrichCallKeywordWithAssertionErrors;
    }

    public void setEnrichCallKeywordWithAssertionErrors(boolean enrichCallKeywordWithAssertionErrors) {
        this.enrichCallKeywordWithAssertionErrors = enrichCallKeywordWithAssertionErrors;
    }
}
