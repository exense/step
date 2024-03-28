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
package step.core.execution.model;

public enum ExecutionStatus {
	INITIALIZING,
	
	IMPORTING,

	/**
	 * This status corresponds to the report skeleton creation phase.
	 * During this phase the work that will be performed during the execution is estimated.
	 * The required resources (agent tokens, etc) is also estimated during this phase.
	 */
	ESTIMATING,

	/**
	 * This status corresponds to the provisioning of the resources required for the execution
	 */
	PROVISIONING,

	RUNNING,
	
	ABORTING,

	FORCING_ABORT,

	DEPROVISIONING,

	EXPORTING,

	ENDED;
}
