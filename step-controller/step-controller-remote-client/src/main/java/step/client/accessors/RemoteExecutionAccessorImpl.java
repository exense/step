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
package step.client.accessors;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;

import step.client.credentials.ControllerCredentials;
import step.commons.iterators.SkipLimitIterator;
import step.commons.iterators.SkipLimitProvider;
import step.core.execution.model.Execution;
import step.core.repositories.RepositoryObjectReference;

public class RemoteExecutionAccessorImpl extends RemoteExecutionAccessor {

	public RemoteExecutionAccessorImpl() {
		super("/rest/executions/", Execution.class);
	}
	
	public RemoteExecutionAccessorImpl(ControllerCredentials credentials) {
		super(credentials, "/rest/executions/", Execution.class);
	}

	@Override
	public void createIndexesIfNeeded(Long ttl) {
		throw notImplemented();
	}

	@Override
	public List<Execution> getActiveTests() {
		throw notImplemented();
	}

	@Override
	public List<Execution> getTestExecutionsByArtefactURL(RepositoryObjectReference objectReference) {
		throw notImplemented();
	}

	class FindByCriteraParam 
	{
		public FindByCriteraParam() {
			super();
		}
		public Map<String, String> getCriteria() {
			return criteria;
		}
		public void setCriteria(Map<String, String> criteria) {
			this.criteria = criteria;
		}
		public Date getStart() {
			return start;
		}
		public void setStart(Date start) {
			this.start = start;
		}
		public Date getEnd() {
			return end;
		}
		public void setEnd(Date end) {
			this.end = end;
		}
		public int getSkip() {
			return skip;
		}
		public void setSkip(int skip) {
			this.skip = skip;
		}
		public int getLimit() {
			return limit;
		}
		public void setLimit(int limit) {
			this.limit = limit;
		}
		private Map<String, String> criteria = new HashMap<>();
		
		private Date start;
		private Date end;
		private int skip;
		private int limit;
	}
	
	@Override
	public Iterable<Execution> findByCritera(Map<String, String> criteria, Date start, Date end) {
		return new Iterable<Execution>() { 
            @Override
            public Iterator<Execution> iterator() 
            { 
                return  new SkipLimitIterator<Execution>(new SkipLimitProvider<Execution>() {
        			@Override
        			public List<Execution> getBatch(int skip, int limit) {
        				
        				FindByCriteraParam param = new FindByCriteraParam();
        				param.criteria = criteria;
        				param.start = start;
        				param.end = end;
        				param.skip = skip;
        				param.limit = limit;
        				
        				Entity<FindByCriteraParam> entity = Entity.entity(param, MediaType.APPLICATION_JSON);
        				
        				Builder b = requestBuilder("/rest/executions/search/by/critera");
        				return executeRequest(()->b.post(entity,new GenericType<List<Execution>>() {}));
        			}
        		}); 
            } 
        };
	}

	@Override
	public Iterable<Execution> findLastStarted(int limit) {
		return getRange(0, limit);
	}

	@Override
	public Iterable<Execution> findLastEnded(int limit) {
		throw notImplemented();
	}

	@Override
	public List<Execution> getLastExecutionsBySchedulerTaskID(String schedulerTaskID, int limit) {
		throw notImplemented();
	}
}
