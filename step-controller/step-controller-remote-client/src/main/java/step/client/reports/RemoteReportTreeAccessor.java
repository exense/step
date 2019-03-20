package step.client.reports;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.GenericType;

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
