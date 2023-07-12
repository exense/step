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

import java.util.Date;
import java.util.List;
import java.util.Map;

import step.core.accessors.Accessor;
import step.core.collections.SearchOrder;
import step.core.repositories.RepositoryObjectReference;

public interface ExecutionAccessor extends Accessor<Execution> {

	void createIndexesIfNeeded(Long ttl);
	
	List<Execution> getActiveTests();

	List<Execution> getTestExecutionsByArtefactURL(RepositoryObjectReference objectReference);

	Iterable<Execution> findByCritera(Map<String, String> criteria, Date start, Date end);

	Iterable<Execution> findByCritera(Map<String, String> criteria, Date start, Date end, SearchOrder order);

	Iterable<Execution> findInInterval(Map<String, String> criteria, Date start, Date end, boolean endedOnly, SearchOrder order);

	Iterable<Execution> findLastStarted(int limit);

	Iterable<Execution> findLastEnded(int limit);

	List<Execution> getLastEndedExecutionsBySchedulerTaskID(String schedulerTaskID, int limit);

}
