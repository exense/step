package step.core.artefacts.handlers;

import java.util.Map;

import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.reports.ReportNode;
import step.core.execution.ExecutionContext;

public class ArtefactHandlerManager {
	
	private final ExecutionContext context;
	private final ArtefactHandlerRegistry artefactHandlerRegistry;
	
	public ArtefactHandlerManager(ExecutionContext context, ArtefactHandlerRegistry artefactHandlerRegistry) {
		super();
		this.context = context;
		this.artefactHandlerRegistry = artefactHandlerRegistry;
	}

	public void createReportSkeleton(AbstractArtefact artefact, ReportNode parentNode) {
		createReportSkeleton(artefact, parentNode, null);
	}
	
	public void createReportSkeleton(AbstractArtefact artefact, ReportNode parentNode, Map<String, Object> newVariables) {
		ArtefactHandler<AbstractArtefact, ReportNode> artefactHandler = getArtefactHandler(artefact);
		artefactHandler.createReportSkeleton(parentNode, artefact, newVariables);
	}
	
	public ReportNode execute(AbstractArtefact artefact, ReportNode parentNode) {
		return execute(artefact, parentNode, null);
	}
	
	public ReportNode execute(AbstractArtefact artefact, ReportNode parentNode, Map<String, Object> newVariables) {
		ArtefactHandler<AbstractArtefact, ReportNode> artefactHandler = getArtefactHandler(artefact);
		return artefactHandler.execute(parentNode, artefact, newVariables);
	}

	@SuppressWarnings("unchecked")
	private ArtefactHandler<AbstractArtefact, ReportNode> getArtefactHandler(AbstractArtefact artefact) {
		Class<AbstractArtefact> artefactClass = (Class<AbstractArtefact>) artefact.getClass();
		ArtefactHandler<AbstractArtefact, ReportNode> artefactHandler = artefactHandlerRegistry.getArtefactHandler(artefactClass, context);
		return artefactHandler;
	}
}
