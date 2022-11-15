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
package step.artefacts.handlers;

import step.artefacts.Failure;
import step.artefacts.reports.FailureReportNode;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.execution.ExecutionContext;
import step.core.miscellaneous.ReportNodeAttachmentManager;

import java.nio.charset.StandardCharsets;

public class FailureHandler extends ArtefactHandler<Failure, FailureReportNode> {

    private ReportNodeAttachmentManager reportNodeAttachmentManager;

    @Override
    public void init(ExecutionContext context) {
        super.init(context);
        reportNodeAttachmentManager = new ReportNodeAttachmentManager(context);
    }

    @Override
    protected void createReportSkeleton_(FailureReportNode parentNode, Failure testArtefact) {

    }

    @Override
    protected void execute_(FailureReportNode reportNode, Failure testArtefact) {
        reportNode.setStatus(ReportNodeStatus.TECHNICAL_ERROR);
        reportNode.setError(testArtefact.getMessage(), 0, true);
        String stackTrace = testArtefact.getStackTrace();
        if (stackTrace != null) {
            reportNodeAttachmentManager.attach(stackTrace.getBytes(StandardCharsets.UTF_8), "exception.log", reportNode);
        }
    }

    @Override
    public FailureReportNode createReportNode_(ReportNode parentNode, Failure testArtefact) {
        return new FailureReportNode();
    }
}
