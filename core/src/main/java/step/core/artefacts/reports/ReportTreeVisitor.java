package step.core.artefacts.reports;

import java.util.NoSuchElementException;
import java.util.function.Consumer;

public class ReportTreeVisitor {
	
	private final ReportTreeAccessor reportTreeAccessor;
	
	public ReportTreeVisitor(ReportTreeAccessor reportTreeAccessor) {
		super();
		this.reportTreeAccessor = reportTreeAccessor;
	}

	public void visit(String executionId, Consumer<ReportNode> consumer) {
		try {
			ReportNode root = reportTreeAccessor.getChildren(executionId).next();
			visitChildren(root, consumer);	
		} catch(NoSuchElementException e) {
			throw new NoSuchElementException("Unable to find root node for execution "+executionId);
		}
	}

	protected void visitChildren(ReportNode root, Consumer<ReportNode> consumer) {
		reportTreeAccessor.getChildren(root.getId().toString()).forEachRemaining(node-> {
			consumer.accept(node);
			visitChildren(node, consumer);
		});
	}

}
