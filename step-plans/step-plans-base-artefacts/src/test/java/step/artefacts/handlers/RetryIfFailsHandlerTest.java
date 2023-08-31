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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import step.artefacts.RetryIfFails;
import step.artefacts.reports.RetryIfFailsReportNode;
import step.core.artefacts.CheckArtefact;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.dynamicbeans.DynamicValue;
import step.core.execution.ExecutionContext;
import step.core.plans.Plan;
import step.core.plans.builder.PlanBuilder;
import step.core.plans.runner.PlanRunnerResult;
import step.core.views.ViewManager;
import step.plugins.views.functions.ErrorDistribution;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static step.plugins.views.functions.ErrorDistributionView.ERROR_DISTRIBUTION_VIEW;

public class RetryIfFailsHandlerTest extends AbstractPlanTest {

	@Before
	public void before() {
		setupContext();
	}

	@Test
	public void testSuccess() throws IOException {
		RetryIfFails block = new RetryIfFails();
		block.setMaxRetries(new DynamicValue<>(2));

		CheckArtefact check1 = new CheckArtefact(withPassedReportNode);

		Plan plan = PlanBuilder.create().startBlock(block).add(check1).endBlock().build();
		PlanRunnerResult result = executePlan(plan);

		assertEquals("RetryIfFails:PASSED:\n" +
				" Iteration1:PASSED:\n" +
				"  CheckArtefact:PASSED:\n", result.getTreeAsString());
		assertEquals("", result.getErrorSummary());

		ErrorDistribution viewModel = (ErrorDistribution) context.get(ViewManager.class).queryView(ERROR_DISTRIBUTION_VIEW, result.getExecutionId());
		assertEquals(0, viewModel.getCount());
		assertEquals(0, viewModel.getErrorCount());
	}

	@Test
	public void testMaxRetry() {
		RetryIfFails block = new RetryIfFails();
		block.setMaxRetries(new DynamicValue<>(3));
		block.setGracePeriod(new DynamicValue<>(1000));
		block.addChild(new CheckArtefact(withFailedReportNode));

		PlanRunnerResult result = executeArtefact(block);

		ReportNode child = getFirstReportNode();
		Assert.assertTrue(child.getDuration()>=2000);
		assertEquals(child.getStatus(), ReportNodeStatus.FAILED);
		assertEquals("myError", result.getErrorSummary());

		assertEquals(3, getChildren(child).size());

		ErrorDistribution viewModel = (ErrorDistribution) context.get(ViewManager.class).queryView(ERROR_DISTRIBUTION_VIEW, result.getExecutionId());
		assertEquals(0, viewModel.getCount());
		// Ensure that the total error count is 1 and not 3 and thus that the RetryIfFailsHandler
		// properly removed the errors of the failed iterations
		assertEquals(1, viewModel.getErrorCount());
		assertEquals(1, (int) viewModel.getCountByErrorCode().get("1"));

		// Ensure that the error of the last iteration is the only contributing error
		List<ReportNode> reportNodesWithContributingErrors = result.getReportNodesWithErrors().collect(Collectors.toList());
		assertEquals(1, reportNodesWithContributingErrors.size());
		ReportNode parentNode = context.getReportNodeAccessor().get(reportNodesWithContributingErrors.get(0).getParentID());
		assertEquals("Iteration3", parentNode.getName());

	}

	@Test
	public void testReportLastNodeOnly() {
		RetryIfFails block = new RetryIfFails();
		block.setMaxRetries(new DynamicValue<>(3));
		block.setGracePeriod(new DynamicValue<>(1000));
		block.setReportLastTryOnly(new DynamicValue<>(true));
		block.addChild(new CheckArtefact(withFailedReportNode));

		PlanRunnerResult result = executeArtefact(block);

		ReportNode child = getFirstReportNode();
		Assert.assertTrue(child.getDuration()>=2000);
		assertEquals(child.getStatus(), ReportNodeStatus.FAILED);
		assertEquals("myError", result.getErrorSummary());
		
		assertEquals(1, getChildren(child).size());

		ErrorDistribution viewModel = (ErrorDistribution) context.get(ViewManager.class).queryView(ERROR_DISTRIBUTION_VIEW, result.getExecutionId());
		assertEquals(0, viewModel.getCount());
		// Ensure that the total error count is 1 and not 3 and thus that the RetryIfFailsHandler
		// properly removed the errors of the failed iterations
		assertEquals(1, viewModel.getErrorCount());
		assertEquals(1, (int) viewModel.getCountByErrorCode().get("1"));
	}

	@Test
	public void testReportLastNodeOnlySuccess() {
		RetryIfFails block = new RetryIfFails();
		block.setMaxRetries(new DynamicValue<>(3));
		block.setGracePeriod(new DynamicValue<>(1000));
		block.setReportLastTryOnly(new DynamicValue<>(true));
		block.addChild(new CheckArtefact(withPassedReportNode));

		PlanRunnerResult result = executeArtefact(block);
		assertEquals("", result.getErrorSummary());

		ReportNode child = getFirstReportNode();
		List<ReportNode> children = getChildren(child);
		assertEquals(child.getStatus(), ReportNodeStatus.PASSED);		
		assertEquals(1, children.size());

		ErrorDistribution viewModel = (ErrorDistribution) context.get(ViewManager.class).queryView(ERROR_DISTRIBUTION_VIEW, result.getExecutionId());
		assertEquals(0, viewModel.getCount());
		assertEquals(0, viewModel.getErrorCount());
	}

	@Test
	public void testReportLastNodeOnlyTimeout() {
		RetryIfFails block = new RetryIfFails();
		block.setMaxRetries(new DynamicValue<>(3));
		block.setGracePeriod(new DynamicValue<>(1000));
		block.setReportLastTryOnly(new DynamicValue<>(true));
		block.setTimeout(new DynamicValue<>(500));
		block.addChild(new CheckArtefact(withFailedReportNode));

		PlanRunnerResult result = executeArtefact(block);

		assertEquals(ReportNodeStatus.FAILED, result.getResult());
		assertEquals("myError", result.getErrorSummary());

		ReportNode child = getFirstReportNode();
		System.out.println("Assert child.getDuration()<=2000 with value: " + child.getDuration());
		Assert.assertTrue(child.getDuration()<2000);
		Assert.assertTrue(child instanceof RetryIfFailsReportNode);
		RetryIfFailsReportNode retryIfFailsReportNode = (RetryIfFailsReportNode) child;
		assertEquals(2, retryIfFailsReportNode.getTries());
		assertEquals(1, retryIfFailsReportNode.getSkipped());
		assertEquals(child.getStatus(), ReportNodeStatus.FAILED);
		
		assertEquals(1, getChildren(child).size());

		ErrorDistribution viewModel = (ErrorDistribution) context.get(ViewManager.class).queryView(ERROR_DISTRIBUTION_VIEW, result.getExecutionId());
		assertEquals(0, viewModel.getCount());
		// Ensure that the total error count is 1 and not 3 and thus that the RetryIfFailsHandler
		// properly removed the errors of the failed iterations
		assertEquals(1, viewModel.getErrorCount());
		assertEquals(1, (int) viewModel.getCountByErrorCode().get("1"));
	}

	@Test
	public void testTimeout() {
		RetryIfFails block = new RetryIfFails();
		block.setTimeout(new DynamicValue<>(200));
		block.setGracePeriod(new DynamicValue<>(50));

		CheckArtefact check1 = new CheckArtefact(withTimeout);
		block.addChild(check1);

		PlanRunnerResult result = executeArtefact(block);

		ReportNode child = getFirstReportNode();
		Assert.assertTrue(child.getDuration()>=250);
		assertEquals(child.getStatus(), ReportNodeStatus.FAILED);
		// No error is reported by the consumer "withTimeout"
		assertEquals("", result.getErrorSummary());

		ErrorDistribution viewModel = (ErrorDistribution) context.get(ViewManager.class).queryView(ERROR_DISTRIBUTION_VIEW, result.getExecutionId());
		assertEquals(0, viewModel.getCount());
		assertEquals(0, viewModel.getErrorCount());
	}

	private final Consumer<ExecutionContext> withFailedReportNode = context -> {
		ReportNode currentReportNode = context.getCurrentReportNode();
		currentReportNode.setError("myError", 1, true);
		currentReportNode.setStatus(ReportNodeStatus.FAILED);
	};

	private final Consumer<ExecutionContext> withPassedReportNode = context ->
			context.getCurrentReportNode().setStatus(ReportNodeStatus.PASSED);

	private final Consumer<ExecutionContext> withTimeout = c -> {
		try {
			Thread.sleep(300);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	};
}

