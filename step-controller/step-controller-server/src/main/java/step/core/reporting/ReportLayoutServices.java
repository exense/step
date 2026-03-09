package step.core.reporting;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import step.controller.services.entities.AbstractEntityServices;
import step.core.access.User;
import step.core.deployment.AuthorizationException;
import step.core.entities.EntityConstants;
import step.framework.server.security.Secured;
import step.framework.server.security.SecuredContext;

import java.util.List;
import java.util.Optional;


@Singleton
@Path("/report-layout")
@Tag(name = "ReportLayout")
@Tag(name = "Entity=ReportLayout")
@SecuredContext(key = "entity", value = ReportLayoutServices.REPORT_LAYOUT_RIGHT)
public class ReportLayoutServices extends AbstractEntityServices<ReportLayout> {

    private static final String SHARED_RIGHT_SUFFIX = "shared";
    public static final String DELETE_RIGHT = "delete";
    public static final String WRITE_RIGHT = "write";
    public static final String REPORT_LAYOUT_RIGHT = "reportLayout";

    private ReportLayoutAccessor reportLayoutAccessor;

    public ReportLayoutServices() {
        super(EntityConstants.reportLayouts);
    }

    @PostConstruct
    public void init() throws Exception {
        super.init();
        reportLayoutAccessor = getContext().require(ReportLayoutAccessor.class);
    }

    @Operation(description = "Returns all accessible report layouts.")
    @GET
    @Path("/list")
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(right="{entity}-read")
    public List<ReportLayout> getAllReportLayouts() {
        return reportLayoutAccessor.getAccessibleReportLayoutsDefinitions(getSession().getUser().getUsername());
    }

    @Override
    public ReportLayout save(ReportLayout reportLayout) {
        //Only check additional specific rights when updating layout
        Optional.ofNullable(get(reportLayout.getId().toHexString())).ifPresent(entity -> checkLayoutRight(entity, WRITE_RIGHT));
        return super.save(reportLayout);
    }

    private void checkLayoutRight(ReportLayout reportLayout, String right) {
        //If user is the owner, he is always allowed (if he has the base access right role)
        User currentUser = this.getSession().getUser();
        if (!reportLayout.getCreationUserId().equals(currentUser.getId())) {
            if (reportLayout.shared) {
                //Check specific access right for shared layouts
                checkRights(REPORT_LAYOUT_RIGHT + "-" + right + "-" + SHARED_RIGHT_SUFFIX);
            } else {
                //The layout isn't shared and the current user is not the owner, forbid access
                throw new AuthorizationException("This is a private layout owned by " + reportLayout.getCreationUser() + ", you have no permission to modify it.");
            }
        }
    }

    @Override
    public void delete(String id) {
        ReportLayout reportLayout = getEntity(id);
        checkLayoutRight(reportLayout, DELETE_RIGHT);
        super.delete(id);
    }

    @Override
    protected ReportLayout cloneEntity(ReportLayout entity) {
        ReportLayout clone = super.cloneEntity(entity);
        clone.setShared(false);
        return clone;
    }

    @Override
    public ReportLayout restoreVersion(String id, String versionId) {
        ReportLayout reportLayout = getEntity(id);
        checkLayoutRight(reportLayout, WRITE_RIGHT);
        return super.restoreVersion(id, versionId);
    }

    @Override
    public void setLocked(String id, Boolean locked) {
        ReportLayout reportLayout = getEntity(id);
        checkLayoutRight(reportLayout, WRITE_RIGHT);
        super.setLocked(id, locked);
    }

    @Operation(operationId = "shareReportLayout", description = "Share this report layout with other users")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Secured(right = "{entity}-write")
    @Path("{id}/share")
    public void shareReportLayout(@PathParam("id") String id) {
        changeSharedState(id, true);
    }

    private void changeSharedState(String id, boolean share) {
        ReportLayout reportLayout = getEntity(id);
        reportLayout.setShared(share);
        save(reportLayout);
    }

    @Operation(operationId = "unshareReportLayout", description = "Unshare this report layout")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Secured(right = "{entity}-write")
    @Path("{id}/unshare")
    public void unshareReportLayout(@PathParam("id") String id) {
        changeSharedState(id, false);
    }
}
