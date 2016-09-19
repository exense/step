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
			info.setName(artefact.getAttributes()!=null?artefact.getAttributes().get("name"):null);
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
