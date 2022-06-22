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
package step.client.reports;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.core.GenericType;

import step.client.AbstractRemoteClient;
import step.client.credentials.ControllerCredentials;
import step.commons.iterators.SkipLimitIterator;
import step.commons.iterators.SkipLimitProvider;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportTreeAccessor;

public class RemoteReportTreeAccessor extends AbstractRemoteClient implements ReportTreeAccessor {

	public RemoteReportTreeAccessor(ControllerCredentials credentials) {
		super(credentials);
	}
	
	public RemoteReportTreeAccessor(){
		super();
	}

	@Override
	public Iterator<ReportNode> getChildren(String parentID) {
		SkipLimitIterator<ReportNode> skipLimitIterator = new SkipLimitIterator<ReportNode>(new SkipLimitProvider<ReportNode>() {
			@Override
			public List<ReportNode> getBatch(int skip, int limit) {
				Map<String, String> queryParams = new HashMap<>();
				queryParams.put("skip", Integer.toString(skip));
				queryParams.put("limit", Integer.toString(limit));
				GenericType<List<ReportNode>> genericEntity = new GenericType<List<ReportNode>>() {};
				Builder b = requestBuilder("/rest/controller/reportnode/"+parentID+"/children", queryParams);
				return executeRequest(()->b.get(genericEntity));
			}
		});
		
		return skipLimitIterator;			
	}

	@Override
	public ReportNode get(String id) {
		Builder b = requestBuilder("/rest/controller/reportnode/"+id);
		return executeRequest(()->b.get(ReportNode.class));
	}

}
