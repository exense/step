package step.plugins.reporting;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.reports.Measure;
import step.reporting.fixme.LiveMeasureContexts;

import java.util.List;

public class ReportingManager {
    private static final Logger logger = LoggerFactory.getLogger(ReportingManager.class);

    private final LiveMeasureContexts liveMeasureContexts;



    public ReportingManager(String controllerUrl) {
        // This is the full URL as exposed by controllerUrl + the inject method of the services, only missing the ID at the end.
        liveMeasureContexts = new LiveMeasureContexts(controllerUrl + "/rest/reporting/live-measure?contextId=");
    }


    public LiveMeasureContexts getLiveMeasureContexts() {
        return liveMeasureContexts;
    }

    public void onMeasuresReceived(String contextId, List<Measure> measures) {
        if (contextId == null || contextId.isEmpty()) {
            throw new IllegalArgumentException("contextId cannot be null or empty");
        }
        liveMeasureContexts.onMeasuresReceived(contextId, measures);
    }
}
