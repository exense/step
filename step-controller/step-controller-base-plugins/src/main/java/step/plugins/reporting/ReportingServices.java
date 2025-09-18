package step.plugins.reporting;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import step.core.GlobalContext;
import step.core.deployment.AbstractStepServices;
import step.core.deployment.Unfiltered;
import step.core.reports.Measure;

import java.util.List;

@Path("/reporting")
@Tag(name = "Reporting")
public class ReportingServices extends AbstractStepServices {

    private ReportingManager manager;

    @PostConstruct
    public void init() throws Exception {
        super.init();
        GlobalContext globalContext = getContext();
        manager = globalContext.get(ReportingManager.class);
    }

    @POST
    @Path("/live-measure")
    @Consumes(MediaType.APPLICATION_JSON)
    @Unfiltered
    public void injectMeasures(List<Measure> measures, @QueryParam("contextId") String contextId) {
        manager.onMeasuresReceived(contextId, measures);
    }
}
