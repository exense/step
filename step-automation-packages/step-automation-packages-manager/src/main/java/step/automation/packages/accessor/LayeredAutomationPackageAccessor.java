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
package step.automation.packages.accessor;

import step.automation.packages.AutomationPackage;
import step.core.accessors.Accessor;
import step.core.accessors.LayeredAccessor;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Layered {@link AutomationPackageAccessor}, mirroring {@link step.functions.accessor.LayeredFunctionAccessor}
 * and {@link step.resources.LayeredResourceAccessor}. It is used during isolated executions so that
 * automation packages can be resolved both from the isolated (in-memory) layer and the global one.
 * <p>
 * This matters for the {@code apResource:} resolver: an isolated execution may end up executing a
 * globally-deployed keyword (surfaced through the layered function accessor) whose {@code apResource:}
 * reference points to the global automation package. Without the global layer here, that package
 * would be unresolvable.
 */
public class LayeredAutomationPackageAccessor extends LayeredAccessor<AutomationPackage> implements AutomationPackageAccessor {

    public LayeredAutomationPackageAccessor() {
        super();
    }

    public LayeredAutomationPackageAccessor(List<? extends Accessor<AutomationPackage>> accessors) {
        super(accessors);
    }

    @Override
    public List<AutomationPackage> findByAutomationPackageResource(String resourceString) {
        return layeredStreamMerge(accessor -> ((AutomationPackageAccessor) accessor).findByAutomationPackageResource(resourceString).stream())
            .collect(Collectors.toList());
    }

    @Override
    public List<AutomationPackage> findByLibraryResource(String resourceString) {
        return layeredStreamMerge(accessor -> ((AutomationPackageAccessor) accessor).findByLibraryResource(resourceString).stream())
            .collect(Collectors.toList());
    }
}
