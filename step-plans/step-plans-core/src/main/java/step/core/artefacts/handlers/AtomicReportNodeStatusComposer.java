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
package step.core.artefacts.handlers;

import org.bson.types.ObjectId;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.reports.Error;

import java.util.ArrayList;
import java.util.List;

public class AtomicReportNodeStatusComposer {

    protected ReportNodeStatus parentStatus;
    protected List<ObjectId> parentErrorSources;

    public AtomicReportNodeStatusComposer(ReportNodeStatus initialStatus) {
        super();
        this.parentStatus = initialStatus;
        this.parentErrorSources = null;
    }

    public AtomicReportNodeStatusComposer(ReportNode initialNode) {
        super();
        this.parentStatus = initialNode.getStatus();
        this.parentErrorSources = initialNode.getErrorSources();
    }

    public synchronized void addStatusAndRecompose(ReportNode reportNode) {
        ReportNodeStatus reportNodeStatus = reportNode.getStatus();
        if (parentStatus == null || reportNodeStatus.ordinal() < parentStatus.ordinal()) {
            parentStatus = reportNodeStatus;
        }
        // Propagate the error of the node if any
        Error error = reportNode.getError();
        if (error != null) {
            doIfParentErrorSourcesSizeBelowLimit(() -> parentErrorSources.add(reportNode.getId()));
        }
        // Propagate the errors of the children nodes if any
        List<ObjectId> errorSources = reportNode.getErrorSources();
        if (errorSources != null) {
            doIfParentErrorSourcesSizeBelowLimit(() -> parentErrorSources.addAll(errorSources));
        }

    }

    private void doIfParentErrorSourcesSizeBelowLimit(Runnable runnable) {
        initializeParentErrorSourcesIfNull();
        if (parentErrorSources.size() < 10) {
            runnable.run();
        }
    }

    private void initializeParentErrorSourcesIfNull() {
        if (parentErrorSources == null) {
            parentErrorSources = new ArrayList<>();
        }
    }

    public void applyComposedStatusToParentNode(ReportNode parentNode) {
        parentNode.setStatus(parentStatus);
        parentNode.setErrorSources(parentErrorSources);
    }
}
