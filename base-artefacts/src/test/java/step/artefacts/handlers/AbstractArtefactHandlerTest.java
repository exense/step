package step.artefacts.handlers;

import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.ArtefactAccessor;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeAccessor;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionTestHelper;

public class AbstractArtefactHandlerTest {
	
	protected void setupContext() {
		ExecutionTestHelper.setupContext();
	}
	
	protected <T extends AbstractArtefact> T add(T artefact) {
		getArtefactAccessor().save(artefact);
		return artefact;
	}

	private ArtefactAccessor getArtefactAccessor() {
		return ExecutionContext.getCurrentContext().getGlobalContext().getArtefactAccessor();
	}
	
	protected <T extends AbstractArtefact> T addAsChildOf(T artefact, AbstractArtefact parent) {
		getArtefactAccessor().get(parent.getId()).addChild(artefact.getId());
		return add(artefact);
	}
	
	protected void createSkeleton(AbstractArtefact artefact) {
		ArtefactHandler.delegateCreateReportSkeleton(artefact,ExecutionContext.getCurrentContext().getReport());
	}
	
	protected void execute(AbstractArtefact artefact) {
		ArtefactHandler.delegateExecute(artefact,ExecutionContext.getCurrentContext().getReport());
	}
	
	protected ReportNode getFirstReportNode() {
		return getReportNodeAccessor().getChildren(ExecutionContext.getCurrentContext().getReportNodeTree().getRoot().getId()).next();
	}
	
	protected List<ReportNode> getChildren(ReportNode node) {
		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(getReportNodeAccessor().getChildren(node.getId()), Spliterator.ORDERED), false).collect(Collectors.toList());
	}

	private ReportNodeAccessor getReportNodeAccessor() {
		return ExecutionContext.getCurrentContext().getGlobalContext().getReportAccessor();
	}
}
