package step.plugins.livereporting;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import step.core.GlobalContext;
import step.core.deployment.AbstractStepServices;
import step.core.deployment.Unfiltered;
import step.core.reports.Measure;
import step.livereporting.LiveReportingContexts;

import java.util.List;

@Path("/live-reporting")
@Tag(name = "Live Reporting")
public class LiveReportingServices extends AbstractStepServices {

    private LiveReportingContexts liveReportingContexts;

    @PostConstruct
    public void init() throws Exception {
        super.init();
        GlobalContext globalContext = getContext();
        liveReportingContexts = globalContext.require(LiveReportingContexts.class);
    }

    @POST
    @Path("/{contextId}/measures")
    @Consumes(MediaType.APPLICATION_JSON)
    @Unfiltered
    public void injectMeasures(List<Measure> measures, @PathParam("contextId") String contextId) {
        liveReportingContexts.onMeasuresReceived(contextId, measures);
    }
}
