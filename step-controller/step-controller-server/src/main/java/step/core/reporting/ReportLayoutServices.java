package step.core.reporting;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Singleton;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.apache.poi.ss.formula.functions.T;
import step.controller.services.entities.AbstractEntityServices;
import step.core.accessors.AbstractOrganizableObject;
import step.core.deployment.AuthorizationException;
import step.core.entities.EntityConstants;
import step.core.reporting.model.ReportLayout;
import step.core.reporting.model.ReportLayoutJson;
import step.framework.server.security.Secured;
import step.framework.server.security.SecuredContext;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;


@Singleton
@Path("/report-layout")
@Tag(name = "ReportLayout")
@Tag(name = "Entity=ReportLayout")
@SecuredContext(key = "entity", value = ReportLayoutServices.REPORT_LAYOUT_RIGHT)
public class ReportLayoutServices extends AbstractEntityServices<ReportLayout> {

    private static final String SHARED_RIGHT_SUFFIX = "shared";
    public static final String READ_RIGHT = "read";
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

    @Override
    public ReportLayout get(String id) {
        ReportLayout reportLayout = super.get(id);
        checkLayoutRight(reportLayout, READ_RIGHT);
        return reportLayout;
    }

    @Operation(operationId = "exportLayout", description = "Export the report layout to its Json representation")
    @GET
    @Path("/{id}/json")
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(right = "{entity}-read")
    public ReportLayoutJson exportLayout(@PathParam("id") String id) {
        ReportLayout reportLayout = getEntity(id);
        return new ReportLayoutJson(reportLayout);
    }

    @Override
    public List<ReportLayout> findByIds(List<String> ids) {
        List<ReportLayout> byIds = super.findByIds(ids);
        byIds.forEach(r -> checkLayoutRight(r, READ_RIGHT));
        return byIds;
    }

    @Override
    public Map<String, String> findNamesByIds(List<String> ids) {
        return reportLayoutAccessor.findByIds(ids).peek(l -> checkLayoutRight(l, READ_RIGHT)).collect(Collectors.toMap(a -> a.getId().toHexString(), a ->
                a.getAttribute(AbstractOrganizableObject.NAME)
        ));
    }

    @Override
    public List<ReportLayout> findManyByAttributes(Map<String, String> attributes) {
        return super.findManyByAttributes(attributes).stream().filter(this::canReadLayout).collect(Collectors.toList());
    }

    private boolean canReadLayout(ReportLayout r) {
        try {
            checkLayoutRight(r, READ_RIGHT);
            return true;
        } catch (AuthorizationException e) {
            return false;
        }
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
        Optional.ofNullable(super.get(reportLayout.getId().toHexString())).ifPresent(entity -> checkLayoutRight(entity, WRITE_RIGHT));
        return super.save(reportLayout);
    }

    private void checkLayoutRight(ReportLayout reportLayout, String right) {
        if (ReportLayout.ReportLayoutVisibility.Preset.equals(reportLayout.visibility)) {
            //Preset layouts can be read by any user with the base right, modifications are never allowed
            if (!READ_RIGHT.equals(right)) {
                throw new AuthorizationException("Modifying a preset layout is not allowed.");
            }
        } else {
            //If the current user is the owner, he is always allowed (if he has the base access right role)
            if (!reportLayout.getCreationUser().equals(this.getSession().getUser().getUsername())) {
                if (ReportLayout.ReportLayoutVisibility.Shared.equals(reportLayout.visibility)) {
                    //Base read right automatically grant access to reading shared dashboard, write and delete require specific rights
                    if (!READ_RIGHT.equals(right)) {
                        //Check specific access right for shared layouts
                        checkRights(REPORT_LAYOUT_RIGHT + "-" + SHARED_RIGHT_SUFFIX + "-" + right);
                    }
                } else {
                    //The layout isn't shared, the current user is not the owner, and he doesn't have the "all" right
                    throw new AuthorizationException("This is a private layout owned by " + reportLayout.getCreationUser() + ", you have no permission to " + right + " it.");
                }
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
        clone.visibility = ReportLayout.ReportLayoutVisibility.Private;
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
        changeLayoutVisibility(id, ReportLayout.ReportLayoutVisibility.Shared);
    }

    private void changeLayoutVisibility(String id, ReportLayout.ReportLayoutVisibility visibility) {
        ReportLayout reportLayout = getEntity(id);
        reportLayout.visibility = visibility;
        save(reportLayout);
    }

    @Operation(operationId = "unshareReportLayout", description = "Unshare this report layout")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Secured(right = "{entity}-write")
    @Path("{id}/unshare")
    public void unshareReportLayout(@PathParam("id") String id) {
        changeLayoutVisibility(id, ReportLayout.ReportLayoutVisibility.Private);
    }
}
