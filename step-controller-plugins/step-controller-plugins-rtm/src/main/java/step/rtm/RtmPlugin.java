package step.rtm;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.bson.Document;
import org.eclipse.jetty.webapp.WebAppContext;
import org.rtm.commons.Configuration;
import org.rtm.commons.MeasurementAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.client.MongoCollection;

import step.artefacts.reports.CallFunctionReportNode;
import step.core.GlobalContext;
import step.core.accessors.AbstractAccessor;
import step.core.artefacts.reports.ReportNode;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.core.reports.Measure;
import step.functions.Function;

@Plugin
public class RtmPlugin extends AbstractControllerPlugin {
	
	private static final Logger logger = LoggerFactory.getLogger(RtmPlugin.class);

	public static final String ATTRIBUTE_EXECUTION_ID = "eId";

	MeasurementAccessor accessor;
	
	boolean measureReportNodes;

	@Override
	public void executionControllerStart(GlobalContext context) throws Exception {
		context.getServiceRegistrationCallback().registerService(RtmPluginServices.class);

		Properties rtmProperties = Configuration.getInstance().getUnderlyingPropertyObject();
		ch.exense.commons.app.Configuration stepProperties = context.getConfiguration();

		if(stepProperties.getPropertyAsBoolean("plugins.rtm.useLocalDB", true) == true){
		cloneProperty(rtmProperties, stepProperties, "db.host");
		cloneProperty(rtmProperties, stepProperties, "db.port");
		cloneProperty(rtmProperties, stepProperties, "db.database");
		cloneProperty(rtmProperties, stepProperties, "db.username");
		cloneProperty(rtmProperties, stepProperties, "db.password");
		}
		measureReportNodes = stepProperties.getPropertyAsBoolean("plugins.rtm.measurereportnodes", true);
		
		MongoCollection<Document> measurements = context.getMongoClientSession().getMongoDatabase().getCollection("measurements");
		AbstractAccessor.createOrUpdateCompoundIndex(measurements,ATTRIBUTE_EXECUTION_ID, "begin");
		AbstractAccessor.createOrUpdateIndex(measurements,"begin");
		
		WebAppContext webappCtx = new WebAppContext();
		webappCtx.setContextPath("/rtm");

		String war = stepProperties.getProperty("plugins.rtm.war");
		if(war==null) {
			throw new RuntimeException("Property 'plugins.rtm.war' is null. Unable to start RTM.");
		} else {
			File warFile = new File(war);
			if(!warFile.exists()||!warFile.canRead()) {
				throw new RuntimeException("The file '"+war+"' with absolute path '"+warFile.getAbsolutePath()+"' set by the property 'plugins.rtm.war' doesn't exist or cannot be read. Unable to start RTM.");	
			}
		}
		webappCtx.setWar(war);
		webappCtx.setParentLoaderPriority(true);
		context.getServiceRegistrationCallback().registerHandler(webappCtx);

		accessor = MeasurementAccessor.getInstance();
		context.put(MeasurementAccessor.class, accessor);
	}

	@Override
	public void executionControllerDestroy(GlobalContext context) {
		if(accessor !=null) {
			accessor.close();
		}
	}

	private void cloneProperty(Properties rtmProperties, ch.exense.commons.app.Configuration stepProperties, String property) {
		if(stepProperties.getProperty(property)!=null) {
			rtmProperties.put(property, stepProperties.getProperty(property));			
		}
	}

	@Override
	public void afterReportNodeExecution(ReportNode node) {		
		if(node instanceof CallFunctionReportNode) {
			CallFunctionReportNode stepReport = (CallFunctionReportNode) node;
			List<Object> measurements = new ArrayList<>();
			
			Map<String, Object> measurement;
			if(measureReportNodes) {
				measurement = new HashMap<>();
				measurement.put(ATTRIBUTE_EXECUTION_ID, stepReport.getExecutionID());
				measurement.put("name", stepReport.getFunctionAttributes().get(Function.NAME));
				measurement.put("value", (long)stepReport.getDuration());
				measurement.put("begin", stepReport.getExecutionTime());
				measurement.put("rnId", stepReport.getId().toString());
				measurement.put("rnStatus", stepReport.getStatus().toString());
				measurement.put("type", "keyword");
				measurements.add(measurement);
				
			}

			if(stepReport.getMeasures()!=null) {
				for(Measure measure:stepReport.getMeasures()) {
					measurement = new HashMap<>();
					measurement.put(ATTRIBUTE_EXECUTION_ID, stepReport.getExecutionID());
					measurement.put("name", measure.getName());
					measurement.put("value", measure.getDuration());
					measurement.put("begin", measure.getBegin());
					measurement.put("rnId", stepReport.getId().toString());
					measurement.put("rnStatus", stepReport.getStatus().toString());
					measurement.put("type", "custom");

					if(measure.getData() != null){
						for(Map.Entry<String,Object> entry : measure.getData().entrySet()){
							String key = entry.getKey();
							Object val = entry.getValue();
							if((key != null) && (val != null)){
								if(	(val instanceof Long) || (val instanceof String)){
									measurement.put(key, val);
								}else{
									if(	(val instanceof Number)){
										measurement.put(key, ((Integer) val).longValue());
									}else{
										// ignore improper types
									}
								}
							}
						}
					}

					measurements.add(measurement);
				}
			}

			accessor.saveManyMeasurements(measurements);
			logMeasurements(measurements);
			//call new function to log all measurements or log them after adding each measure
		}
	}
	
	public static void logMeasurements(List<Object> measurements) {
		for (Object o: measurements) {
			logger.trace("RTM measure:" + o.toString());
		}
	}

}
