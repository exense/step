package step.core.repositories;

import java.util.Map.Entry;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import step.commons.conf.Configuration;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.ArtefactAccessor;
import step.core.execution.model.ReportExport;
import step.core.execution.model.ReportExportStatus;

public class RepositoryObjectManager {

	private static final Logger logger = LoggerFactory.getLogger(RepositoryObjectManager.class);
	
	public static final String CLIENT_KEY = "RepositoryObjectManager_Client";
	
	private Client client = ClientBuilder.newClient();
	
	private ArtefactAccessor artefactAccessor;
	
	public RepositoryObjectManager(ArtefactAccessor artefactAccessor) {
		super();
		this.artefactAccessor = artefactAccessor;
	}

	public void close() {
		client.close();
	}

	public String importArtefact(RepositoryObjectReference artefact)  {
		String server = Configuration.getInstance().getProperty("tec.specification."+artefact.getRepositoryID() +".server");
		try {
			WebTarget target = client.target(server+"/import");
			for(Entry<String, String> e:artefact.getRepositoryParameters().entrySet()) {
				target = target.queryParam(e.getKey(), e.getValue());
			}
			return target.request(MediaType.APPLICATION_JSON).get(String.class);
		} catch (Exception e) {
			logger.error("An error occurred while importing testartefact.", e);
			throw new RuntimeException("An error occurred while importing testartefact.", e);
		}
	}
	
	public ReportExport exportTestExecutionReport(RepositoryObjectReference report, String executionID) {	
		String server = Configuration.getInstance().getProperty("tec.reporting." + report.getRepositoryID() + ".servers")+"/report";
		
		ReportExport export = new ReportExport();
		
		export.setURL(server);
		try {
			WebTarget target = client.target(server);
			if(report.getRepositoryParameters()!=null) {
				for(Entry<String, String> e:report.getRepositoryParameters().entrySet()) {
					target = target.queryParam(e.getKey(), e.getValue());
				}
			}
			target = target.queryParam("executionID", executionID);
			target.request(MediaType.APPLICATION_JSON).get();
			export.setStatus(ReportExportStatus.SUCCESSFUL);
		} catch (Exception e) {
			export.setStatus(ReportExportStatus.FAILED);
			export.setError(e.getMessage() + ". See application logs for more details.");
			logger.error("Error while exporting test " + executionID + " to " + server,e);
		}

		
		return export;
	}
	
	private static final String LOCAL = "local";
	private static final String ARTEFACT_ID = "artefactid";
	
	public ArtefactInfo getArtefactInfo(RepositoryObjectReference ref) {
		if(ref.getRepositoryID().equals(LOCAL)) {
			String artefactid = ref.getRepositoryParameters().get(ARTEFACT_ID);
			AbstractArtefact artefact = artefactAccessor.get(new ObjectId(artefactid));
			
			ArtefactInfo info = new ArtefactInfo();
			info.setName(artefact.getName());
			return info;
		} else {
			return RepositoryObjectManager.executeRequest(ref, "/artefact/info", ArtefactInfo.class);
		}
	}

	public static <T extends Object> T executeRequest(RepositoryObjectReference ref, String service, Class<T> resultClass) {
		String server = Configuration.getInstance().getProperty("tec.reporting." + ref.getRepositoryID() + ".servers");
		
		Client client = ClientBuilder.newClient();
		client.register(JacksonJsonProvider.class);
		try {
			WebTarget target = client.target(server+service);
			for(Entry<String, String> e:ref.getRepositoryParameters().entrySet()) {
				target = target.queryParam(e.getKey(), e.getValue());
			}
			return target.request(MediaType.APPLICATION_JSON).get(resultClass);
		} finally {
			client.close();
		}
	}
	
	
	public static TestSetStatusOverview getReport(RepositoryObjectReference report) {
		String server = Configuration.getInstance().getProperty("tec.reporting." + report.getRepositoryID() + ".servers");
		
//		ClientConfig clientConfig = new DefaultClientConfig();
//		clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING,
//				Boolean.TRUE);
		Client client = ClientBuilder.newClient();
		client.register(JacksonJsonProvider.class);
		try {
			WebTarget target = client.target(server+"/report/lastrun");
			for(Entry<String, String> e:report.getRepositoryParameters().entrySet()) {
				target = target.queryParam(e.getKey(), e.getValue());
			}
			return target.request(MediaType.APPLICATION_JSON).get(TestSetStatusOverview.class);
		} finally {
			client.close();
		}
	}
	
}
