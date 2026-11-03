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
package step.cli;

import step.core.accessors.AbstractOrganizableObject;
import step.core.artefacts.reports.ParentSource;
import step.core.artefacts.reports.ReportNode;
import step.core.execution.ExecutionContext;
import step.core.plans.Plan;

/**
 * The path of a report node from the root of its plan, as in {@code Load test > Thread 1 > [BEFORE] > Echo}, which
 * identifies a node in the outputs of a local execution.
 */
final class ReportNodePaths {

    static final String PATH_SEPARATOR = " > ";

    private ReportNodePaths() {
    }

    /**
     * @param parentPath the path of the parent of the node, null for the root of the plan
     * @return what precedes the name of the node in its path: the path of its parent and the source of the node
     * within it, such as {@code [BEFORE]}, each followed by a separator
     */
    static String prefixOf(String parentPath, ReportNode node) {
        StringBuilder prefix = new StringBuilder();
        if (parentPath != null) {
            prefix.append(parentPath).append(PATH_SEPARATOR);
        }
        ParentSource parentSource = node.getParentSource();
        if (parentSource != null && parentSource.printSource) {
            prefix.append("[").append(parentSource.name()).append("]").append(PATH_SEPARATOR);
        }
        return prefix.toString();
    }

    /**
     * @return the name of the plan the paths of an execution start from, or the id of the execution for a plan without
     * name
     */
    static String planNameOf(ExecutionContext context) {
        Plan plan = context.getPlan();
        String name = plan != null ? plan.getAttribute(AbstractOrganizableObject.NAME) : null;
        return name != null ? name : context.getExecutionId();
    }
}
