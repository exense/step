package step.core.artefacts.reports;

import java.util.NoSuchElementException;
import java.util.Stack;
import java.util.function.Consumer;

public class ReportTreeVisitor {
	
	private final ReportTreeAccessor reportTreeAccessor;
	
	public ReportTreeVisitor(ReportTreeAccessor reportTreeAccessor) {
		super();
		this.reportTreeAccessor = reportTreeAccessor;
	}

	public void visitNodes(String executionId, Consumer<ReportNode> consumer) {
		visit(executionId, event->consumer.accept(event.node));
	}
	
	public void visit(String executionId, Consumer<ReportNodeEvent> consumer) {
		try {
			ReportNode root = reportTreeAccessor.getChildren(executionId).next();
			Stack<ReportNode> stack = new Stack<>();
			visitChildrenWithEvents(root, stack, consumer);	
		} catch(NoSuchElementException e) {
			throw new NoSuchElementException("Unable to find root node for execution "+executionId);
		}
	}
	
	public static class ReportNodeEvent {
		
		protected ReportNode node;
		protected Stack<ReportNode> stack;

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
}
