/*******************************************************************************
 * (C) Copyright 2016 Jerome Comte and Dorian Cransac
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
 *******************************************************************************/
package step.core.artefacts.handlers;

import java.util.HashMap;

import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.Artefact;
import step.core.artefacts.reports.ReportNode;
import step.core.execution.ExecutionContext;

public class ArtefactHandlerRegistry {
	
	private static ArtefactHandlerRegistry instance;
	
	private final HashMap<Class<?>, Class<?>> register = new HashMap<>();

	public ArtefactHandler<AbstractArtefact, ReportNode> getArtefactHandler(Class<AbstractArtefact> artefactClass, ExecutionContext context) {
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
				
				artefactHandler.init(context);
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
