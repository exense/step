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
