/*
 * ******************************************************************************
 *  * Copyright (C) 2020, exense GmbH
 *  *
 *  * This file is part of STEP
 *  *
 *  * STEP is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU Affero General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * STEP is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU Affero General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU Affero General Public License
 *  * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 *  *****************************************************************************
 */
package step.automation.packages;

import step.core.objectenricher.ObjectEnricher;
import step.core.objectenricher.ObjectPredicate;
import step.core.objectenricher.WriteAccessValidator;

public class AutomationPackageAccessParameters {
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
    public final WriteAccessValidator writeAccessValidator;
    /**
     * the user triggering the operation, this is used to udpate tracking information for the package and its resources
     */
    public final String actorUser;

    public AutomationPackageAccessParameters(ObjectEnricher enricher, ObjectPredicate objectPredicate, WriteAccessValidator writeAccessValidator, String actorUser) {
        this.enricher = enricher;
        this.objectPredicate = objectPredicate;
        this.writeAccessValidator = writeAccessValidator;
        this.actorUser = actorUser;

        if (objectPredicate == null) {
            throw new AutomationPackageManagerException("The objectPredicate cannot be null");
        }
        if (writeAccessValidator == null) {
            throw new AutomationPackageManagerException("The writeAccessValidator cannot be null");
        }
    }
}
