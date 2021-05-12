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

import java.util.*;
import java.util.stream.Collectors;

import step.commons.iterators.SkipLimitIterator;
import step.commons.iterators.SkipLimitProvider;
import step.core.accessors.AbstractAccessor;
import step.core.collections.Collection;
import step.core.collections.Filter;
import step.core.collections.Filters;
import step.core.collections.SearchOrder;
import step.core.repositories.RepositoryObjectReference;

public class ExecutionAccessorImpl extends AbstractAccessor<Execution> implements ExecutionAccessor {

	public ExecutionAccessorImpl(Collection<Execution> collectionDriver) {
		super(collectionDriver);
	}

	@Override
	public void createIndexesIfNeeded(Long ttl) {
		createOrUpdateIndex("startTime");
		createOrUpdateIndex("description");
		createOrUpdateIndex("executionParameters.userID");
		createOrUpdateIndex("executionTaskID");
	}

	@Override
	public List<Execution> getActiveTests() {
		return collectionDriver.find(Filters.not(Filters.equals("status", "ENDED")), null, null, null, 0)
				.collect(Collectors.toList());
	}

	@Override
	public List<Execution> getTestExecutionsByArtefactURL(RepositoryObjectReference objectReference) {
		return collectionDriver
				.find(Filters.equals("executionParameters.repositoryObject", objectReference), null, null, null, 0)
				.collect(Collectors.toList());
	}

	public List<Execution> findByCritera(Map<String, String> criteria, long start, long end, int skip, int limit) {
		List<Filter> filters = new ArrayList<>();
		filters.add(Filters.gte("startTime", start));
		filters.add(Filters.lte("endTime", start));
		criteria.forEach((k, v) -> {
			filters.add(Filters.equals(k, v));
		});

		return collectionDriver.find(Filters.and(filters), new SearchOrder("endTime", -1), skip, limit, 0)
				.collect(Collectors.toList());
	}

	@Override
	public Iterable<Execution> findByCritera(Map<String, String> criteria, Date start, Date end) {
		return new Iterable<Execution>() {
			@Override
			public Iterator<Execution> iterator() {
				return new SkipLimitIterator<Execution>(new SkipLimitProvider<Execution>() {
					@Override
					public List<Execution> getBatch(int skip, int limit) {
						return findByCritera(criteria, start.getTime(), end.getTime(), skip, limit);
					}
				});
			}
		};
	}

	@Override
	public Iterable<Execution> findLastStarted(int limit) {
		return collectionDriver.find(Filters.empty(), new SearchOrder("startTime", -1), 0, limit, 0)
				.collect(Collectors.toList());
	}

	@Override
	public Iterable<Execution> findLastEnded(int limit) {
		return collectionDriver.find(Filters.empty(), new SearchOrder("endTime", -1), 0, limit, 0)
				.collect(Collectors.toList());
	}

	@Override
	public List<Execution> getLastExecutionsBySchedulerTaskID(String schedulerTaskID, int limit) {
		return collectionDriver
				.find(Filters.and(List.of(Filters.equals("executionTaskID", schedulerTaskID),
						Filters.not(Filters.equals("status", "ENDED")))), new SearchOrder("endTime", -1), 0, limit, 0)
				.collect(Collectors.toList());
	}
}
