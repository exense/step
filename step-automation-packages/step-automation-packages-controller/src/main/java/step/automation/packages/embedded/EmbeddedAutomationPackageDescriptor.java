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
package step.automation.packages.embedded;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import step.core.accessors.AbstractOrganizableObject;
import step.core.objectenricher.EnricheableObject;

/**
 * The optional {@code <archive-file-name>.json} sidecar of an embedded automation package, carrying
 * the attributes the package is deployed with. Never persisted.
 * <p>
 * Being an {@link EnricheableObject} is what makes the standard enrichment machinery usable: once
 * the attributes are resolved, {@code objectHookRegistry.rebuildContext} yields the enricher and the
 * predicate of the project named in the sidecar.
 * <p>
 * Unknown properties are ignored: the sidecars were written against the keyword package model, of
 * which only the attributes ever applied.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class EmbeddedAutomationPackageDescriptor extends AbstractOrganizableObject implements EnricheableObject {
}
