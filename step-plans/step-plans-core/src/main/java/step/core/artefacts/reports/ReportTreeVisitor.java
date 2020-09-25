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
package step.core.artefacts.reports;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Stack;
import java.util.function.Consumer;

public class ReportTreeVisitor {
	
	private final ReportTreeAccessor reportTreeAccessor;
	
	public ReportTreeVisitor(ReportTreeAccessor reportTreeAccessor) {
		super();
		this.reportTreeAccessor = reportTreeAccessor;
	}

	public ReportNode getRootReportNode(String executionId) {
		return reportTreeAccessor.getChildren(executionId).next();
	}
	
	public void visitNodes(String executionId, Consumer<ReportNode> consumer) {
		visit(executionId, event->consumer.accept(event.node));
	}
	
	/**
	 * Visits the report tree of an execution using a an event based handler
	 * @param executionId the ID of the execution to be visited
	 * @param reportNodeVisitorEventHandler the event handler to be used
	 */
	public void visit(String executionId, ReportNodeVisitorEventHandler reportNodeVisitorEventHandler) {
		try {
			ReportNode root = getRootReportNode(executionId);
			Stack<ReportNode> stack = new Stack<>();
			visitChildrenWithHandler(root, stack, reportNodeVisitorEventHandler);	
		} catch(NoSuchElementException e) {
			throw new NoSuchElementException("Unable to find root node for execution "+executionId);
		}
	}
	
	public void visit(String executionId, Consumer<ReportNodeEvent> consumer) {
		try {
			ReportNode root = getRootReportNode(executionId);
			Stack<ReportNode> stack = new Stack<>();
			visitChildrenWithEvents(root, stack, consumer);	
		} catch(NoSuchElementException e) {
			throw new NoSuchElementException("Unable to find root node for execution "+executionId);
		}
	}
	
	public static class ReportNodeEvent {
		
		protected ReportNode node;
		protected Stack<ReportNode> stack;
		protected Map<String, Object> userData = new HashMap<>();

		public ReportNode getNode() {
			return node;
		}
		
		public Stack<ReportNode> getStack() {
			return stack;
		}
		
		public ReportNode getParentNode() {
			return !stack.isEmpty()?stack.peek():null;
		}
		
		public ReportNode getRootNode() {
			return !stack.isEmpty()?stack.get(0):null;
		}

		public Object getData(Object key) {
			return userData.get(key);
		}

		public Object attachData(String key, Object value) {
			return userData.put(key, value);
		}
	}
	
	@SuppressWarnings("unchecked")
	protected void visitChildrenWithEvents(ReportNode root, Stack<ReportNode> stack, Consumer<ReportNodeEvent> consumer) {
		ReportNodeEvent event = new ReportNodeEvent();
		event.node = root;
		event.stack = (Stack<ReportNode>) stack.clone();
		consumer.accept(event);
		stack.push(root);
		reportTreeAccessor.getChildren(root.getId().toString()).forEachRemaining(node-> {
			visitChildrenWithEvents(node, stack, consumer);
		});
		stack.pop();
	}
	
	@SuppressWarnings("unchecked")
	protected void visitChildrenWithHandler(ReportNode root, Stack<ReportNode> stack, ReportNodeVisitorEventHandler reportNodeVisitorEventHandler) {
		ReportNodeEvent event = new ReportNodeEvent();
		event.node = root;
		event.stack = (Stack<ReportNode>) stack.clone();
		stack.push(root);
		reportNodeVisitorEventHandler.startReportNode(event);
		reportTreeAccessor.getChildren(root.getId().toString()).forEachRemaining(node-> {
			visitChildrenWithHandler(node, stack, reportNodeVisitorEventHandler);
		});
		reportNodeVisitorEventHandler.endReportNode(event);
		stack.pop();
	}
}
