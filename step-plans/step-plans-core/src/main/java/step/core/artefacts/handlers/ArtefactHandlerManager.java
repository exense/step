package step.core.artefacts.handlers;

import java.util.Map;

import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.Artefact;
import step.core.artefacts.reports.ReportNode;
import step.core.execution.ExecutionContext;

public class ArtefactHandlerManager {
	
	private final ExecutionContext context;
	
	public ArtefactHandlerManager(ExecutionContext context) {
		super();
		this.context = context;
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
		ArtefactHandler<AbstractArtefact, ReportNode> artefactHandler = getArtefactHandler(artefactClass, context);
		return artefactHandler;
	}
	
	@SuppressWarnings("unchecked")
	private ArtefactHandler<AbstractArtefact, ReportNode> getArtefactHandler(Class<AbstractArtefact> artefactClass, ExecutionContext context) {
		// Be careful not to cache the ArtefactHandlerRegistry as this is a mutable variable of the context...
		ArtefactHandlerRegistry artefactHandlerRegistry = context.getArtefactHandlerRegistry();
		Artefact artefact = artefactClass.getAnnotation(Artefact.class);
		if(artefact!=null) {
			Class<ArtefactHandler<AbstractArtefact, ReportNode>> artefactHandlerClass;
			artefactHandlerClass = (Class<ArtefactHandler<AbstractArtefact, ReportNode>>) artefactHandlerRegistry.get(artefactClass);
			if(artefactHandlerClass!=null) {
				ArtefactHandler<AbstractArtefact, ReportNode> artefactHandler;
				try {
					artefactHandler = artefactHandlerClass.newInstance();
				} catch (InstantiationException | IllegalAccessException e) {
					throw new RuntimeException("Unable to instanciate artefact handler for the artefact class " + artefactClass, e);
				}
				
				artefactHandler.init(context);
				return artefactHandler;
			} else {
				throw new RuntimeException("No artefact handler found for the artefact class " + artefactClass);			
			}	
		} else {
			throw new RuntimeException("The class " + artefactClass + " is not annotated as artefact!");	
		}
	}
	
}
