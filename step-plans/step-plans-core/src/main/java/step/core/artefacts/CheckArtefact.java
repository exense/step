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
package step.core.artefacts;

import java.util.function.Consumer;

import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.Artefact;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.execution.ExecutionContext;

@Artefact()
public class CheckArtefact extends AbstractArtefact {

	private Consumer<ExecutionContext> executionRunnable;

	public CheckArtefact() {
		this(c->c.getCurrentReportNode().setStatus(ReportNodeStatus.PASSED));
	}
	
	public CheckArtefact(Consumer<ExecutionContext> executionRunnable) {
		super();
		this.executionRunnable = executionRunnable;
	}

	public Consumer<ExecutionContext> getExecutionRunnable() {
		return executionRunnable;
	}

	public void setExecutionRunnable(Consumer<ExecutionContext> executionRunnable) {
		this.executionRunnable = executionRunnable;
	}
	
}
