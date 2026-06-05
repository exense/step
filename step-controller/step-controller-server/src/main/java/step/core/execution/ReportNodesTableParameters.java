package step.core.execution;

import step.framework.server.tables.service.TableParameters;

import java.util.List;

public class ReportNodesTableParameters extends TableParameters {
    private String eid;
    private List<String> ancestorIds;
    private boolean enrichWithContributingErrors;
    private Integer enrichWithContributingErrorsLimit = 10;

    public String getEid() {
        return eid;
    }

    public void setEid(String eid) {
        this.eid = eid;
    }

    public List<String> getAncestorIds() {
        return ancestorIds;
    }

    public void setAncestorIds(List<String> ancestorIds) {
        this.ancestorIds = ancestorIds;
    }

    public boolean isEnrichWithContributingErrors() {
        return enrichWithContributingErrors;
    }

    public void setEnrichWithContributingErrors(boolean enrichWithContributingErrors) {
        this.enrichWithContributingErrors = enrichWithContributingErrors;
    }

    public Integer getEnrichWithContributingErrorsLimit() {
        return enrichWithContributingErrorsLimit;
    }

    public void setEnrichWithContributingErrorsLimit(Integer enrichWithContributingErrorsLimit) {
        this.enrichWithContributingErrorsLimit = enrichWithContributingErrorsLimit;
    }
}
