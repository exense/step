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

import org.bson.types.ObjectId;
import step.cli.AttachmentExporter.ExportedAttachment;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.execution.ExecutionContext;
import step.core.plugins.IgnoreDuringAutoDiscovery;
import step.core.plugins.Plugin;
import step.engine.plugins.AbstractExecutionEnginePlugin;

import java.io.PrintStream;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Prints the report tree of an execution as it unfolds, each line holding the path of its report node from the root
 * of the plan, so that it can be read on its own even when the branches of a parallel artefact are interleaved:
 * <pre>
 * ▶ MyPlan
 * ✔ MyPlan > [BEFORE] > Echo(Setting up) : PASSED (12 ms)
 * ▶ MyPlan > ThreadGroup
 * ▶ MyPlan > ThreadGroup > Thread 1
 * ▶ MyPlan > ThreadGroup > Thread 1 > Session
 * ▶ MyPlan > ThreadGroup > Thread 1 > Session > Iteration 1
 * ▶ MyPlan > ThreadGroup > Thread 1 > Session > Iteration 1 > MyKeyword
 * ✖ MyPlan > ThreadGroup > Thread 1 > Session > Iteration 1 > MyKeyword(Input={}, Output={}) : FAILED (2.1 s) Expected 'ok' but was 'ko'
 * ...
 * ✖ MyPlan : FAILED (2.4 s)
 * </pre>
 * Every node is printed once it ends, with its outcome, its details, status, duration and error. A node containing
 * other nodes is additionally printed with a ▶ before the first of them, and so is a node running for longer than
 * the hold delay, for a long running keyword not to go unnoticed.
 * <p>
 * The markers fall back to ASCII characters when the charset of the output cannot encode them, as a Windows console
 * using a legacy code page.
 * <p>
 * Given an {@link AttachmentExporter}, it writes the attachments of each node as soon as it ends, and prints where
 * below the node:
 * <pre>
 * ✖ MyPlan > MyKeyword(Input={}, Output={}) : TECHNICAL_ERROR (12 ms) Connection refused
 *     attachment: file:///C:/work/reports/20261102-143012-123456-local/MyPlan/0001-MyKeyword/exception.log
 * </pre>
 */
@Plugin
@IgnoreDuringAutoDiscovery
public class LiveReportTreePrinterPlugin extends AbstractExecutionEnginePlugin {

    public static final Duration DEFAULT_HOLD_DELAY = Duration.ofSeconds(1);

    private static final String ATTACHMENT_INDENT = "    ";

    private final PrintStream out;
    private final Duration holdDelay;
    private final Markers markers;
    private final AttachmentExporter attachmentExporter;

    // All the fields below are guarded by this instance
    private final Map<ObjectId, RunningNode> runningNodes = new HashMap<>();
    private ScheduledThreadPoolExecutor holdTimer;

    public LiveReportTreePrinterPlugin() {
        this(System.out, DEFAULT_HOLD_DELAY);
    }

    public LiveReportTreePrinterPlugin(PrintStream out, Duration holdDelay) {
        this(out, holdDelay, null);
    }

    /**
     * @param attachmentExporter writes the attachments of the nodes, null for them not to be written
     */
    public LiveReportTreePrinterPlugin(PrintStream out, Duration holdDelay, AttachmentExporter attachmentExporter) {
        this.out = Objects.requireNonNull(out);
        this.holdDelay = Objects.requireNonNull(holdDelay);
        this.attachmentExporter = attachmentExporter;
        this.markers = Markers.UNICODE.canBeEncodedIn(out.charset()) ? Markers.UNICODE : Markers.ASCII;
    }

    @Override
    public synchronized void executionStart(ExecutionContext context) {
        runningNodes.clear();
    }

    @Override
    public synchronized void beforeReportNodeExecution(ExecutionContext context, ReportNode node) {
        // The root of the plan is a child of the root node of the execution, which is not executed by an artefact handler
        RunningNode parent = node.getParentID() != null ? runningNodes.get(node.getParentID()) : null;
        if (parent != null) {
            printStart(parent);
        }

        RunningNode runningNode = new RunningNode(ReportNodePaths.prefixOf(parent != null ? parent.path() : null, node), node.getName());
        runningNode.holdTimeout = getHoldTimer().schedule(() -> printStartOfLongRunningNode(runningNode),
            holdDelay.toMillis(), TimeUnit.MILLISECONDS);
        // Registered once its timeout is scheduled, for no node to be found without one
        runningNodes.put(node.getId(), runningNode);
    }

    @Override
    public void afterReportNodeExecution(ExecutionContext context, ReportNode node) {
        String parentPath;
        synchronized (this) {
            RunningNode runningNode = runningNodes.remove(node.getId());
            if (runningNode != null) {
                runningNode.ended = true;
                runningNode.holdTimeout.cancel(false);
                parentPath = runningNode.parentPath;
            } else {
                parentPath = "";
            }
        }
        // Written without holding the lock, for the other threads not to wait for the copy of the files
        List<ExportedAttachment> attachments = attachmentExporter != null
            ? attachmentExporter.export(context, node, parentPath + node.getName()) : List.of();
        synchronized (this) {
            print(markers.of(node.getStatus()) + " " + parentPath + describeNode(node) + " : " + describeResult(node));
            // Right below their node, before any line of another thread
            for (ExportedAttachment attachment : attachments) {
                print(ATTACHMENT_INDENT + attachment.describe());
            }
        }
    }

    @Override
    public synchronized void executionFinally(ExecutionContext context) {
        runningNodes.clear();
        if (holdTimer != null) {
            holdTimer.shutdownNow();
            holdTimer = null;
        }
    }

    private synchronized void printStartOfLongRunningNode(RunningNode runningNode) {
        // The node might have ended in the meantime, the cancellation of the timeout racing with its expiry
        if (!runningNode.ended) {
            printStart(runningNode);
        }
    }

    private void printStart(RunningNode runningNode) {
        if (!runningNode.startPrinted) {
            print(markers.start + " " + runningNode.path());
            runningNode.startPrinted = true;
            runningNode.holdTimeout.cancel(false);
        }
    }

    private ScheduledThreadPoolExecutor getHoldTimer() {
        if (holdTimer == null) {
            holdTimer = new ScheduledThreadPoolExecutor(1, runnable -> {
                Thread thread = new Thread(runnable, "live-report-tree-printer");
                thread.setDaemon(true);
                return thread;
            });
            // A timeout is scheduled for every node, and almost all of them are cancelled
            holdTimer.setRemoveOnCancelPolicy(true);
        }
        return holdTimer;
    }

    private void print(String line) {
        out.println(line);
        out.flush();
    }

    /**
     * The name of the node followed by its details, e.g. the message of an echo or the input and output of a
     * keyword, as in the report tree. The details are only complete once the node has ended.
     */
    private static String describeNode(ReportNode node) {
        String details = node.getReportAsString();
        return details != null ? node.getName() + "(" + details + ")" : node.getName();
    }

    private static String describeResult(ReportNode node) {
        StringBuilder result = new StringBuilder().append(node.getStatus());
        Integer duration = node.getDuration();
        if (duration != null) {
            result.append(" (").append(formatDuration(duration)).append(")");
        }
        if (node.getError() != null && node.getError().getMsg() != null) {
            result.append(" ").append(node.getError().getMsg());
        }
        return result.toString();
    }

    static String formatDuration(int durationMs) {
        if (durationMs < 1000) {
            return durationMs + " ms";
        } else {
            return String.format(Locale.ROOT, "%.1f s", durationMs / 1000.0);
        }
    }

    private static class RunningNode {

        private final String parentPath;
        private final String name;
        private boolean startPrinted;
        private boolean ended;
        private ScheduledFuture<?> holdTimeout;

        private RunningNode(String parentPath, String name) {
            this.parentPath = parentPath;
            this.name = name;
        }

        private String path() {
            return parentPath + name;
        }
    }

    private record Markers(String start, String passed, String failed, String other) {

        private static final Markers UNICODE = new Markers("▶", "✔", "✖", "○");
        private static final Markers ASCII = new Markers(">", "+", "x", "-");

        private boolean canBeEncodedIn(Charset charset) {
            return charset.newEncoder().canEncode(start + passed + failed + other);
        }

        private String of(ReportNodeStatus status) {
            if (status == null) {
                return other;
            }
            switch (status) {
                case PASSED:
                    return passed;
                case FAILED:
                case TECHNICAL_ERROR:
                case IMPORT_ERROR:
                    return failed;
                default:
                    return other;
            }
        }
    }
}
