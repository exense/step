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
package step.artefacts;

import step.artefacts.handlers.AssertHandler;
import step.artefacts.handlers.CallFunctionHandler;
import step.artefacts.handlers.CallPlanHandler;
import step.artefacts.handlers.CaseHandler;
import step.artefacts.handlers.CheckHandler;
import step.artefacts.handlers.DataSetHandler;
import step.artefacts.handlers.EchoHandler;
import step.artefacts.handlers.ExportHandler;
import step.artefacts.handlers.ForBlockHandler;
import step.artefacts.handlers.FunctionGroupHandler;
import step.artefacts.handlers.IfBlockHandler;
import step.artefacts.handlers.PlaceholderHandler;
import step.artefacts.handlers.RetryIfFailsHandler;
import step.artefacts.handlers.ReturnHandler;
import step.artefacts.handlers.ScriptHandler;
import step.artefacts.handlers.SequenceHandler;
import step.artefacts.handlers.SetHandler;
import step.artefacts.handlers.SleepHandler;
import step.artefacts.handlers.StreamingArtefactHandler;
import step.artefacts.handlers.SwitchHandler;
import step.artefacts.handlers.SynchronizedHandler;
import step.artefacts.handlers.TestCaseHandler;
import step.artefacts.handlers.TestScenarioHandler;
import step.artefacts.handlers.TestSetHandler;
import step.artefacts.handlers.ThreadGroupHandler;
import step.artefacts.handlers.ThreadGroupHandler.ThreadHandler;
import step.artefacts.handlers.WhileHandler;
import step.core.artefacts.CheckArtefact;
import step.core.artefacts.handlers.ArtefactHandlerRegistry;
import step.core.artefacts.handlers.CheckArtefactHandler;
import step.core.execution.AbstractExecutionEngineContext;
import step.core.execution.ExecutionEngineContext;
import step.core.plugins.Plugin;
import step.engine.plugins.AbstractExecutionEnginePlugin;

@Plugin
public class BaseArtefactPlugin extends AbstractExecutionEnginePlugin {

	@Override
	public void initializeExecutionEngineContext(AbstractExecutionEngineContext parentContext,
			ExecutionEngineContext executionEngineContext) {
		ArtefactHandlerRegistry artefactHandlerRegistry = executionEngineContext.getArtefactHandlerRegistry();
		registerArtefacts(artefactHandlerRegistry);
	}
	
	public static void registerArtefacts(ArtefactHandlerRegistry artefactHandlerRegistry) {
		artefactHandlerRegistry.put(TestSet.class, TestSetHandler.class);
		artefactHandlerRegistry.put(TestCase.class, TestCaseHandler.class);
		artefactHandlerRegistry.put(TestScenario.class, TestScenarioHandler.class);
		artefactHandlerRegistry.put(CallPlan.class, CallPlanHandler.class);
		artefactHandlerRegistry.put(CallFunction.class, CallFunctionHandler.class);
		artefactHandlerRegistry.put(ForBlock.class, ForBlockHandler.class);
		artefactHandlerRegistry.put(ForEachBlock.class, ForBlockHandler.class);
		artefactHandlerRegistry.put(While.class, WhileHandler.class);
		artefactHandlerRegistry.put(DataSetArtefact.class, DataSetHandler.class);
		artefactHandlerRegistry.put(Synchronized.class, SynchronizedHandler.class);
		artefactHandlerRegistry.put(Sequence.class, SequenceHandler.class);
		artefactHandlerRegistry.put(BeforeSequence.class, SequenceHandler.class);
		artefactHandlerRegistry.put(AfterSequence.class, SequenceHandler.class);
		artefactHandlerRegistry.put(Return.class, ReturnHandler.class);
		artefactHandlerRegistry.put(Echo.class, EchoHandler.class);
		artefactHandlerRegistry.put(IfBlock.class, IfBlockHandler.class);
		artefactHandlerRegistry.put(FunctionGroup.class, FunctionGroupHandler.class);
		artefactHandlerRegistry.put(Set.class, SetHandler.class);
		artefactHandlerRegistry.put(Sleep.class, SleepHandler.class);
		artefactHandlerRegistry.put(Script.class, ScriptHandler.class);
		artefactHandlerRegistry.put(ThreadGroup.class, ThreadGroupHandler.class);
		artefactHandlerRegistry.put(BeforeThread.class, SequenceHandler.class);
		artefactHandlerRegistry.put(AfterThread.class, SequenceHandler.class);
		artefactHandlerRegistry.put(step.artefacts.handlers.ThreadGroupHandler.Thread.class, ThreadHandler.class);
		artefactHandlerRegistry.put(Switch.class, SwitchHandler.class);
		artefactHandlerRegistry.put(Case.class, CaseHandler.class);
		artefactHandlerRegistry.put(RetryIfFails.class, RetryIfFailsHandler.class);
		artefactHandlerRegistry.put(Check.class, CheckHandler.class);
		artefactHandlerRegistry.put(Assert.class, AssertHandler.class);
		artefactHandlerRegistry.put(Placeholder.class, PlaceholderHandler.class);
		artefactHandlerRegistry.put(Export.class, ExportHandler.class);
		
		// Special artefacts that are not accessible in the UI
		artefactHandlerRegistry.put(StreamingArtefact.class, StreamingArtefactHandler.class);
		artefactHandlerRegistry.put(CheckArtefact.class, CheckArtefactHandler.class);
	}
}
