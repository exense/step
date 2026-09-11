/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
 *
 * This file is part of STEP
 *
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package step.plugins.parametermanager;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.bson.types.ObjectId;
import step.automation.packages.AutomationPackageEntity;
import step.commons.activation.Expression;
import step.controller.services.entities.AbstractEntityServices;
import step.core.GlobalContext;
import step.core.accessors.Accessor;
import step.core.deployment.ControllerServiceException;
import step.framework.server.access.AuthorizationManager;
import step.framework.server.security.Secured;
import step.framework.server.security.SecuredContext;
import step.parameter.Parameter;
import step.parameter.ParameterManager;
import step.parameter.ParameterManagerException;
import step.parameter.ParameterScope;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.StreamSupport;


@Path("/parameters")
@Tag(name = "Parameters")
@Tag(name = "Entity=Parameter")
@SecuredContext(key = "entity", value = "param")
public class ParameterServices extends AbstractEntityServices<Parameter> {

    private AuthorizationManager authorizationManager;
    private Accessor<Parameter> parameterAccessor;
    private ParameterManager parameterManager;

    public ParameterServices() {
        super(ParameterManagerControllerPlugin.ENTITY_PARAMETERS);
    }

    @PostConstruct
    @SuppressWarnings("unchecked")
    public void init() throws Exception {
        super.init();
        GlobalContext context = getContext();
        parameterAccessor = (Accessor<Parameter>) context.get("ParameterAccessor");
        parameterManager = context.require(ParameterManager.class);
        authorizationManager = context.get(AuthorizationManager.class);
    }

    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(right = "{entity}-write")
    public Parameter newParameter() {
        Parameter parameter = new Parameter(new Expression(""), "", "", "");
        parameter.setPriority(1);
        if (hasGlobalParamRight()) {
            parameter.setScope(ParameterScope.GLOBAL);
        } else {
            parameter.setScope(ParameterScope.FUNCTION);
        }
        getObjectEnricher().accept(parameter);
        return parameter;
    }

    @Override
    public Parameter save(Parameter newParameter) {
        if (newParameter.getKey() == null || newParameter.getKey().isBlank()) {
            throw new ControllerServiceException("The parameter's key is mandatory.");
        }

        Parameter oldParameter;
        if (newParameter.getId() != null) {
            oldParameter = parameterAccessor.get(newParameter.getId());
        } else {
            oldParameter = null;
        }

        return save(newParameter, oldParameter);
    }

    private Parameter save(Parameter newParameter, Parameter sourceParameter) {
        assertRights(newParameter);
        try {
            Parameter result = transformResponse(parameterManager.save(newParameter, sourceParameter, getSession().getUser().getUsername()));
            auditLog("save", result, Map.of("key", result.getKey()));
            return result;
        } catch (ParameterManagerException e) {
            throw new ControllerServiceException(e.getMessage());
        }
    }

    /**
     * Masks the value of protected parameters in every response returned by these services
     */
    @Override
    protected Parameter transformResponse(Parameter parameter) {
        return ParameterManager.maskProtectedValue(parameter);
    }

    protected void assertRights(Parameter newParameter) {
        if (newParameter.getScope() == null || newParameter.getScope() == ParameterScope.GLOBAL) {
            if (!hasGlobalParamRight()) {
                throw new RuntimeException("The user is missing the right 'param-global-write' to write global parameters.");
            }
        }
    }

    protected boolean hasGlobalParamRight() {
        return authorizationManager.checkRightInContext(getSession(), "param-global-write");
    }

    protected static boolean isProtected(Parameter oldParameter) {
        return ParameterManager.isProtected(oldParameter);
    }

    @Override
    public Parameter clone(String id) {
        Parameter sourceParameter = parameterAccessor.get(new ObjectId(id));
        assertEntityIsEditableInContext(sourceParameter);
        // Create a clone of the source parameter
        Parameter newParameter = parameterAccessor.get(new ObjectId(id));
        newParameter.setId(new ObjectId());
        //Remove link to AP
        Optional.ofNullable(newParameter.getCustomFields()).ifPresent(fields -> fields.remove(AutomationPackageEntity.AUTOMATION_PACKAGE_ID));
        auditLog("clone", newParameter, Map.of("key", newParameter.getKey()));
        return save(newParameter, sourceParameter);
    }

    @Override
    public void delete(String id) {
        Parameter parameter = get(id);
        assertEntityIsEditableInContext(parameter);
        assertRights(parameter);
        auditLog("delete", parameter, Map.of("key", parameter.getKey()));
        parameterAccessor.remove(new ObjectId(id));
    }

    @POST
    @Path("/search")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(right = "{entity}-read")
    public Parameter getParameterByAttributes(Map<String, String> attributes) {
        return transformResponse(parameterAccessor.findByAttributes(attributes));
    }

    @GET
    @Path("/all")
    @Produces(MediaType.APPLICATION_JSON)
    @Secured(right = "{entity}-read")
    public List<Parameter> getAllParameters(@QueryParam("skip") Integer skip, @QueryParam("limit") Integer limit) {
        List<Parameter> range;
        if (skip != null && limit != null) {
            range = parameterAccessor.getRange(skip, limit);
        } else {
            range = getAllParameters(0, 1000);
        }
        return transformResponse(range.stream());
    }

    @Override
    public Parameter restoreVersion(String id, String versionId) {
        Parameter parameter = parameterAccessor.get(id);
        assertEntityIsEditableInContext(parameter);
        assertRights(parameter);
        return super.restoreVersion(id, versionId);
    }
}
