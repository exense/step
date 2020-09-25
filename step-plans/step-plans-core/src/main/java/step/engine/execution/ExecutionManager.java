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
package step.engine.execution;

import java.util.Map;

import step.core.artefacts.reports.ReportNodeStatus;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionTypeListener;
import step.core.execution.model.ExecutionStatus;
import step.core.repositories.ImportResult;

public interface ExecutionManager extends ExecutionTypeListener {

	void updateExecutionType(ExecutionContext context, String newType);

	void updateExecutionResult(ExecutionContext context, ReportNodeStatus resultStatus);

	void updateStatus(ExecutionContext context, ExecutionStatus status);

	void persistImportResult(ExecutionContext context, ImportResult importResult);

	void persistStatus(ExecutionContext context);

	void updateParameters(ExecutionContext context, Map<String, String> params);

}
