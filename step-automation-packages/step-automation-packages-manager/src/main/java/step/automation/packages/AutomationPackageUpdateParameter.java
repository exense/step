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
package step.automation.packages;

import org.bson.types.ObjectId;
import step.core.objectenricher.ObjectEnricher;
import step.core.objectenricher.ObjectPredicate;

import java.util.Map;

public class AutomationPackageUpdateParameter {
    /**
     * whether update of existing package is allowed, set to false when a creation of package is expected
     */
    public final boolean allowUpdate;
    /**
     * whether creation of a new package is allowed, set to false when an update is expected
     */
    public final boolean allowCreate;
    /**
     * Whether the package is local i.e. for the Java Step runner and CLI local executions
     */
    public final boolean isLocalPackage;
    /**
     * Id of the existing package that has to be updated, should be combined with allowUpdate true and allowCreate false
     */
    public final ObjectId explicitOldId;
    /**
     * the source of the main package file
     */
    public final AutomationPackageFileSource apSource;
    /**
     * the source of the package library file
     */
    public final AutomationPackageFileSource apLibrarySource;
    /**
     * the version to be set for the created/updated automation package
     */
    public final String automationPackageVersion;
    /**
     * the activation expression of the Automation Package, this is propagated to the depoyed functions and plans of this package and is used for their selection when multiple versions of a package ar deployed
     */
    public final String activationExpression;
    /**
     * the object enricher used to enrich the package and its entities during deployment
     */
    public final ObjectEnricher enricher;
    /**
     * the predicate used to check read access, i.e. when searching for existing packages, resources... during deployment
     */
    public final ObjectPredicate objectPredicate;
    /**
     * the write predicate used during deployment, to ensure we can update existing entities (also used to make sure we can update other packages sharing the same artefact resources)
     */
    public final ObjectPredicate writeAccessPredicate;
    /**
     * Whether the deployment of the package is synchronous, i.e. we wait to acquire the lock and deploy/update all entities before returning
     */
    public final boolean async;
    /**
     * the user triggering the operation, this is used to udpate tracking information for the package and its resources
     */
    public final String actorUser;
    /**
     * Whether we are allowed to update other automation packages sharing the same artefact resources.
     * If the deployed package uses a snapshot artefact that would trigger the update of an existing artefact resource
     * (because the resource is already used by other packages and because it has a new version in the remote repository),
     * the deployment is only allowed if we can update all "linked" packages
     */
    public final boolean allowUpdateOfOtherPackages;
    /**
     * Whether to check if resources with the same origin already exists, true in most cases except when recursively checking for linked automation packages
     */
    public final boolean checkForSameOrigin;
    /**
     * function attributes to be applied to all functions (aka keywords)
     */
    public final Map<String, String> functionsAttributes;
    /**
     * function attributes to be applied to all plans
     */
    public final Map<String, String> plansAttributes;
    /**
     * token selection criteria to be applied to all functions (aka keywords) of this package
     */
    public final Map<String, String> tokenSelectionCriteria;
    /**
     * whether the keywords from this package should all be executed locally (i.e. on controller)
     */
    public final boolean executionFunctionsLocally;

    public AutomationPackageUpdateParameter(boolean allowUpdate, boolean allowCreate, boolean isLocalPackage, ObjectId explicitOldId,
                                            AutomationPackageFileSource apSource, AutomationPackageFileSource apLibrarySource,
                                            String automationPackageVersion, String activationExpression, ObjectEnricher enricher,
                                            ObjectPredicate objectPredicate, ObjectPredicate writeAccessPredicate, boolean async,
                                            String actorUser, boolean allowUpdateOfOtherPackages, boolean checkForSameOrigin,
                                            Map<String, String> functionsAttributes, Map<String, String> plansAttributes,
                                            Map<String, String> tokenSelectionCriteria, boolean executionFunctionsLocally) {
        this.allowUpdate = allowUpdate;
        this.allowCreate = allowCreate;
        this.isLocalPackage = isLocalPackage;
        this.explicitOldId = explicitOldId;
        this.apSource = apSource;
        this.apLibrarySource = apLibrarySource;
        this.automationPackageVersion = automationPackageVersion;
        this.activationExpression = activationExpression;
        this.enricher = enricher;
        this.objectPredicate = objectPredicate;
        this.writeAccessPredicate = writeAccessPredicate;
        this.async = async;
        this.actorUser = actorUser;
        this.allowUpdateOfOtherPackages = allowUpdateOfOtherPackages;
        this.checkForSameOrigin = checkForSameOrigin;
        this.functionsAttributes = functionsAttributes;
        this.plansAttributes = plansAttributes;
        this.tokenSelectionCriteria = tokenSelectionCriteria;
        this.executionFunctionsLocally = executionFunctionsLocally;
    }


}
