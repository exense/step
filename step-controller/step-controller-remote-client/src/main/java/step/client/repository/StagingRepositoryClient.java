package step.client.repository;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;
import org.glassfish.jersey.media.multipart.file.StreamDataBodyPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;

import step.client.AbstractRemoteClient;
import step.client.credentials.ControllerCredentials;
import step.client.planrunners.RemotePlanRunner;
import step.client.reports.RemoteReportTreeAccessor;
import step.core.plans.Plan;
import step.core.plans.runner.PlanRunnerResult;

/**
 * This class represents a client for the execution of fully isolated runs on the controller.
 * Fully isolated means that all the artefacts used for execution ({@link Plan}, {@link Function}s, and parameters) 
 * are isolated from the artefacts that might be already located on the controller
 *
 */
public class StagingRepositoryClient extends AbstractRemoteClient {

	private static final Logger logger = LoggerFactory.getLogger(StagingRepositoryClient.class);
	
	public StagingRepositoryClient() {
		super();
	}

	public StagingRepositoryClient(ControllerCredentials credentials) {
		super(credentials);
	}

	/**
	 * Creates a new isolated context for execution
	 * @return the context handle
	 */
	public StagingContext createContext() {
		Builder b = requestBuilder("/rest/staging/context");
		String contextId = executeRequest(()->b.get(String.class));
		StagingContext context = new StagingContext(credentials, contextId);
		return context;
	}
	
	public static class StagingContext extends AbstractRemoteClient {
		
		protected String contextId;
		
		public StagingContext(ControllerCredentials credentials, String contextId) {
			super(credentials);
			this.contextId = contextId;
		}
		
		/**
		 * Uploads the local file provided as argument to the {@link StagingContext}
		 * @param file the local file to be uploaded
		 * @return the handle to the uploaded file
		 */
		public String uploadFile(File file) {
	        FileDataBodyPart fileDataBodyPart = new FileDataBodyPart("file", file, MediaType.APPLICATION_OCTET_STREAM_TYPE);
	        MultiPart multiPart = new MultiPart();
	        multiPart.setMediaType(MediaType.MULTIPART_FORM_DATA_TYPE);
	        multiPart.bodyPart(fileDataBodyPart);
	        Builder b = requestBuilder("/rest/staging/context/"+contextId+"/file");
	        return executeRequest(()->b.post(Entity.entity(multiPart, multiPart.getMediaType()), String.class));
		}
		
		/**
		 * Uploads the resource provided as stream to the {@link StagingContext}
		 * @param stream the stream of the resource to be uploaded
		 * @param resourceName the name of the resource
		 * @return the handle to the uploaded resource
		 */
		public String upload(InputStream stream, String resourceName) {
			StreamDataBodyPart streamDataBodyPart = new StreamDataBodyPart("file", stream, resourceName);
	        MultiPart multiPart = new MultiPart();
	        multiPart.setMediaType(MediaType.MULTIPART_FORM_DATA_TYPE);
	        multiPart.bodyPart(streamDataBodyPart);
	        Builder b = requestBuilder("/rest/staging/context/"+contextId+"/file");
	        return executeRequest(()->b.post(Entity.entity(multiPart, multiPart.getMediaType()), String.class));
		}
		
		/**
		 * Uploads a Plan to the {@link StagingContext}
		 * @param plan 
		 */
		public void uploadPlan(Plan plan) {
			Builder b = requestBuilder("/rest/staging/context/"+contextId+"/plan");
			Entity<Plan> entity = Entity.entity(plan, MediaType.APPLICATION_JSON);
			executeRequest(()->b.post(entity));
		}

		/**
		 * Runs the uploaded plan in an isolated mode. The plan will only be able to access the artefacts uploaded to this context
		 * @return the {@link PlanRunnerResult} of the execution
		 */
		public PlanRunnerResult run() {
			return run(new HashMap<String, String>());
		}
		
		/**
		 * Runs the uploaded plan in an isolated mode using the provided parameters. The plan will only be able to access the artefacts uploaded to this context
		 * @param executionParameters a list of key-value parameters. these parameters correspond to the parameters that can be selected in UI when starting an execution
		 * @return
		 */
		public PlanRunnerResult run(Map<String, String> executionParameters) {
			Map<String, String> queryParams = new HashMap<>();
			queryParams.put("isolate", "true");
			Builder b = requestBuilder("/rest/staging/context/"+contextId+"/execute", queryParams);
			
			Entity<Map<String, String>> entity = Entity.entity(executionParameters, MediaType.APPLICATION_JSON);
			String executionId = executeRequest(()->b.post(entity, String.class));
			RemotePlanRunner remotePlanRunner = new RemotePlanRunner(credentials);
			closables.add(remotePlanRunner);
			return remotePlanRunner.new RemotePlanRunnerResult(executionId, executionId, new RemoteReportTreeAccessor(credentials));
		}
		
		List<Closeable> closables = new ArrayList<>();
		
		public void close() {
			Builder b = requestBuilder("/rest/staging/context/"+contextId+"/destroy");
			executeRequest(()->b.post(Entity.json("{}")));
			
			closables.forEach(c->{
				try {
					c.close();
				} catch (IOException e) {
					logger.error("Error while closing client "+c.toString(),c);
				}
			});
		}
	}
	
}
