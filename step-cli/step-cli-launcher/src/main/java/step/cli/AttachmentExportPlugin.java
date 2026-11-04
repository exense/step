/*******************************************************************************
 * Copyright (C) 2026, exense GmbH
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

import org.bson.types.ObjectId;
import step.core.artefacts.reports.ReportNode;
import step.core.execution.ExecutionContext;
import step.core.plugins.IgnoreDuringAutoDiscovery;
import step.core.plugins.Plugin;
import step.engine.plugins.AbstractExecutionEnginePlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Writes the attachments of each report node as soon as it ends, with an {@link AttachmentExporter}. Used when the
 * report is not printed live: the {@link LiveReportTreePrinterPlugin} writes them itself, to print where they are.
 */
@Plugin
@IgnoreDuringAutoDiscovery
public class AttachmentExportPlugin extends AbstractExecutionEnginePlugin {

    private final AttachmentExporter exporter;

    // Guarded by this instance
    private final Map<ObjectId, String> runningNodePaths = new HashMap<>();

    public AttachmentExportPlugin(AttachmentExporter exporter) {
        this.exporter = Objects.requireNonNull(exporter);
    }

    @Override
    public synchronized void executionStart(ExecutionContext context) {
        runningNodePaths.clear();
    }

    @Override
    public synchronized void beforeReportNodeExecution(ExecutionContext context, ReportNode node) {
        String parentPath = node.getParentID() != null ? runningNodePaths.get(node.getParentID()) : null;
        runningNodePaths.put(node.getId(), ReportNodePaths.prefixOf(parentPath, node) + node.getName());
    }

    @Override
    public void afterReportNodeExecution(ExecutionContext context, ReportNode node) {
        String path;
        synchronized (this) {
            path = runningNodePaths.remove(node.getId());
        }
        exporter.export(context, node, path != null ? path : node.getName());
    }

    @Override
    public synchronized void executionFinally(ExecutionContext context) {
        runningNodePaths.clear();
    }
}
