package step.core.artefacts.handlers;

import java.util.HashMap;

import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.Artefact;
import step.core.artefacts.reports.ReportNode;

public class ArtefactHandlerRegistry {
	
	private static ArtefactHandlerRegistry instance;
	
	private final HashMap<Class<?>, Class<?>> register = new HashMap<>();

	public ArtefactHandler<AbstractArtefact, ReportNode> getArtefactHandler(Class<AbstractArtefact> artefactClass) {
		Artefact artefact = artefactClass.getAnnotation(Artefact.class);
		if(artefact!=null) {
			@SuppressWarnings("unchecked")
			Class<ArtefactHandler<AbstractArtefact, ReportNode>> artefactHandlerClass = (Class<ArtefactHandler<AbstractArtefact, ReportNode>>) artefact.handler();
			if(artefactHandlerClass!=null) {
				ArtefactHandler<AbstractArtefact, ReportNode> artefactHandler;
				try {
					artefactHandler = artefactHandlerClass.newInstance();
				} catch (InstantiationException | IllegalAccessException e) {
					throw new RuntimeException("Unable to instanciate artefact handler for the artefact class " + artefactClass, e);
				}
				
				return artefactHandler;
			} else {
				throw new RuntimeException("No artefact handler found for the artefact class " + artefactClass);			
			}	
		} else {
			throw new RuntimeException("The class " + artefactClass + " is not annotated as artefact!");	
		}
	}

	public static ArtefactHandlerRegistry getInstance() {
		if(instance == null) {
			instance = new ArtefactHandlerRegistry();
//			instance.register(ForEachBlock.class, ForBlockHandler.class);
//			instance.register(ForBlock.class, ForBlockHandler.class);
//			instance.register(Session.class, SessionHandler.class);
//			instance.register(TestCase.class, TestCaseHandler.class);
//			instance.register(TestSet.class, TestSetHandler.class);
//			instance.register(SetVar.class, SetVarHandler.class);
//			instance.register(TestStep.class, TestStepHandler.class);
//			instance.register(ManualTestStep.class, ManualTestStepHandler.class);
//			instance.register(ErrorMessenger.class, ErrorMessengerHandler.class);
//			instance.register(IfBlock.class, IfBlockHandler.class);
//			instance.register(TestGroup.class, TestGroupHandler.class);
//			instance.register(TestScenario.class, TestScenarioHandler.class);
//			instance.register(Select.class, SelectHandler.class);
//			instance.register(Case.class, CaseHandler.class);
//			instance.register(Call.class, CallHandler.class);
//			instance.register(Sequence.class, SequenceHandler.class);
//			instance.register(Check.class, CheckHandler.class);
//			instance.register(Set.class, SetHandler.class);
//			instance.register(RetryIfFails.class, RetryIfFailsHandler.class);

		}
		return instance;
	}
	
	public void register(Class<?> artefactClass, Class<?> artefactHandlerClass) {
		register.put(artefactClass, artefactHandlerClass);
	}

}
