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

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
		collectionDriver.createOrUpdateCompoundIndex(Map.of("executionTaskID",1,"endTime",-1));
	}

	@Override
	public List<Execution> getActiveTests() {
		return collectionDriver.find(Filters.not(Filters.equals("status", "ENDED")), null, null, null, 0)
				.collect(Collectors.toList());
	}

	@Override
	public List<Execution> getTestExecutionsByArtefactURL(RepositoryObjectReference objectReference) {
		Filter filter;
		String prefix = "executionParameters.repositoryObject.";
		List<Filter> collect = objectReference.getRepositoryParameters().entrySet().stream()
				.map(e -> Filters.equals(prefix+"repositoryParameters."+e.getKey(), e.getValue())).collect(Collectors.toList());
		if(collect.size()==0) {
			filter = Filters.equals(prefix+"repositoryID", objectReference.getRepositoryID());
		} else {
			filter = Filters.and(List.of(
					Filters.equals(prefix+"repositoryID", objectReference.getRepositoryID()),
					Filters.and(collect)));
		}
		return collectionDriver.find(filter, null, null, null, 0).collect(Collectors.toList());
	}

	public List<Execution> findByCritera(Map<String, String> criteria, long start, long end, SearchOrder order, int skip, int limit) {
		List<Filter> filters = new ArrayList<>();
		filters.add(Filters.gte("startTime", start));
		filters.add(Filters.lte("endTime", end));
		criteria.forEach((k, v) -> {
			filters.add(Filters.equals(k, v));
		});

		return collectionDriver.find(Filters.and(filters), order, skip, limit, 0)
				.collect(Collectors.toList());
	}

	@Override
	public Iterable<Execution> findByCritera(Map<String, String> criteria, Date start, Date end) {
		return findByCritera(criteria,start,end, new SearchOrder("endTime" , -1));
	}

	@Override
	public Iterable<Execution> findByCritera(Map<String, String> criteria, Date start, Date end, SearchOrder order) {
		return new Iterable<Execution>() {
			@Override
			public Iterator<Execution> iterator() {
				return new SkipLimitIterator<Execution>(new SkipLimitProvider<Execution>() {
					@Override
					public List<Execution> getBatch(int skip, int limit) {
						return findByCritera(criteria, start.getTime(), end.getTime(), order, skip, limit);
					}
				});
			}
		};
	}

	@Override
	public Iterable<Execution> findInInterval(Map<String, String> criteria, Date start, Date end,
											  boolean endedOnly, SearchOrder order) {
		return new Iterable<Execution>() {
			@Override
			public Iterator<Execution> iterator() {
				return new SkipLimitIterator<Execution>(new SkipLimitProvider<Execution>() {
					@Override
					public List<Execution> getBatch(int skip, int limit) {
						return findInInterval(criteria, start.getTime(), end.getTime(), endedOnly, order, skip, limit);
					}
				});
			}
		};
	}

	private List<Execution> findInInterval(Map<String, String> criteria, long start, long end, boolean endedOnly,
										   SearchOrder order, int skip, int limit) {
		List<Filter> filters = new ArrayList<>();
		/* Note: exec end time only set if status is ENDED
		if looking for execution ended in time interval then end time but be in time interval
			otherwise either there is no yet an end time (exec still running) then
		  		start time must be lt interval end time
		  	or start time is lt end time interval and endtime must be gt than start time interval
		 */
		if (endedOnly) {
			filters.add(Filters.gte("endTime", start));
			filters.add(Filters.lte("endTime", end));
		} else {
			filters.add(Filters.and(List.of(Filters.lte("startTime", end),
					Filters.or(List.of(Filters.gte("endTime", start),Filters.equals("endTime",(String) null))))));
		}
		criteria.forEach((k, v) -> {
			filters.add(Filters.equals(k, v));
		});

		return collectionDriver.find(Filters.and(filters), order, skip, limit, 0)
				.collect(Collectors.toList());
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
	public List<Execution> getLastEndedExecutionsBySchedulerTaskID(String schedulerTaskID, int limit) {
		return collectionDriver
				.find(Filters.and(List.of(Filters.equals("executionTaskID", schedulerTaskID), Filters.equals("status","ENDED"))),
						new SearchOrder("endTime", -1), 0, limit, 0)
				.collect(Collectors.toList());
	}
}
