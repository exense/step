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

import java.util.HashMap;
import java.util.List;

import step.artefacts.Sequence;
import step.artefacts.While;
import step.artefacts.reports.WhileReportNode;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.dynamicbeans.DynamicValue;
import step.core.dynamicbeans.DynamicValueResolver;

public class WhileHandler extends ArtefactHandler<While, WhileReportNode> {

    @Override
    protected void createReportSkeleton_(WhileReportNode parentNode, While testArtefact) {
        evaluateExpressionAndDelegate(parentNode, testArtefact, false);
    }

    @Override
    protected void execute_(WhileReportNode node, While testArtefact) {
        evaluateExpressionAndDelegate(node, testArtefact, true);
    }

    /**
     * Evaluates the condition of the While artefact and delegates the execution or the skeleton creation of each
     * iteration to a {@link Sequence} work artefact wrapping the children of the While artefact.
     * <p>
     * During the execution phase (execution = true), the loop is repeated until one of the exit criteria of the
     * artefact is reached: the condition or the post condition evaluates to false, the timeout is exceeded, the
     * configured maximum number of iterations is reached, or the execution is interrupted.
     * <p>
     * During the skeleton creation phase (execution = false), the number of iterations is capped to 1. The number of
     * iterations of a While artefact isn't known before the execution, thus repeating the loop in this phase would
     * end up in an infinite loop. One iteration is sufficient for the token forecasting: the iterations of a While
     * artefact are executed sequentially and therefore all require the same resources (agent tokens).
     * The condition is evaluated in this phase too. If it evaluates to false or cannot be evaluated at all (which is
     * the case when it references variables that are only defined at execution time), no skeleton is created for the
     * children of this artefact and the resources they require are therefore not forecasted.
     *
     * @param node         the report node of the While artefact
     * @param testArtefact the While artefact
     * @param execution    true to execute the iterations, false to only create the report skeleton of one iteration
     */
    private void evaluateExpressionAndDelegate(WhileReportNode node, While testArtefact, boolean execution) {
        long timeout = testArtefact.getTimeout().getOrDefault(Long.class, 0l);
        long maxTime = System.currentTimeMillis() + timeout;

        Integer maxIterationsValue = testArtefact.getMaxIterations().get();
        int maxIterations = execution ? maxIterationsValue == null ? 0 : maxIterationsValue : 1;
        int currIterationsCount = 0;

        int failedLoops = 0;

        long pacing = testArtefact.getPacing().getOrDefault(Long.class, 0l);

        List<AbstractArtefact> selectedChildren = getChildren(testArtefact);

        DynamicValueResolver resolver = new DynamicValueResolver(context.getExpressionHandler());
        DynamicValue<Boolean> condition = testArtefact.getCondition();
        DynamicValue<Boolean> postCondition = testArtefact.getPostCondition();

        try {
            while (reevaluateCondition(resolver, condition) && (condition.get() == null || condition.get())                                                        // expression is true
                && (timeout == 0 || System.currentTimeMillis() < maxTime)    // infinite Timeout or timeout not reached
                && (maxIterations == 0 || currIterationsCount < maxIterations)) {    // maxIterations infinite or not reached

                if (context.isInterrupted()) {
                    break;
                }

                Sequence iterationTestCase = createWorkArtefact(Sequence.class, testArtefact, "Iteration_" + currIterationsCount);
                iterationTestCase.setPacing(new DynamicValue<Long>(pacing));
                for (AbstractArtefact child : selectedChildren)
                    iterationTestCase.addChild(child);

                if (execution) {
                    ReportNode iterationReportNode = delegateExecute(iterationTestCase, node, new HashMap<>());

                    if (iterationReportNode.getStatus() == ReportNodeStatus.TECHNICAL_ERROR || iterationReportNode.getStatus() == ReportNodeStatus.FAILED) {
                        failedLoops++;
                    }
                } else {
                    delegateCreateReportSkeleton(iterationTestCase, node);
                }

                currIterationsCount++;

                reevaluateCondition(resolver, postCondition);
                Boolean postConditionValue = postCondition.get();
                if (postConditionValue != null && !postConditionValue) {
                    break;
                }
            }

            node.setErrorCount(failedLoops);
            node.setCount(currIterationsCount);
            if (failedLoops > 0) {
                node.setStatus(ReportNodeStatus.FAILED);
            } else {
                node.setStatus(ReportNodeStatus.PASSED);
            }

        } catch (Throwable e) {
            failWithException(node, e);
        }
    }

    protected boolean reevaluateCondition(DynamicValueResolver resolver, DynamicValue<Boolean> condition) {
        resolver.evaluate(condition, getBindings());
        return true;
    }

    @Override
    public WhileReportNode createReportNode_(ReportNode parentNode, While testArtefact) {
        return new WhileReportNode();
    }

}
