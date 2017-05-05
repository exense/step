package step.rtm;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.webapp.WebAppContext;
import org.rtm.commons.Configuration;
import org.rtm.commons.MeasurementAccessor;

import step.artefacts.reports.CallFunctionReportNode;
import step.core.GlobalContext;
import step.core.accessors.AbstractAccessor;
import step.core.artefacts.reports.ReportNode;
import step.core.plugins.AbstractPlugin;
import step.core.plugins.Plugin;
import step.grid.io.Measure;
import step.rtm.RtmPluginServices;

@Plugin
public class RtmPlugin extends AbstractPlugin {

	MeasurementAccessor accessor;
	
	boolean measureReportNodes;

	@Override
	public void executionControllerStart(GlobalContext context) throws Exception {
		context.getServiceRegistrationCallback().registerService(RtmPluginServices.class);

		Properties rtmProperties = Configuration.getInstance().getUnderlyingPropertyObject();
		step.commons.conf.Configuration stepProperties = step.commons.conf.Configuration.getInstance(); 

		if(stepProperties.getPropertyAsBoolean("plugins.rtm.useLocalDB", true) == true){
		cloneProperty(rtmProperties, stepProperties, "db.host");
		cloneProperty(rtmProperties, stepProperties, "db.port");
		cloneProperty(rtmProperties, stepProperties, "db.database");
		cloneProperty(rtmProperties, stepProperties, "db.username");
		cloneProperty(rtmProperties, stepProperties, "db.password");
		}
		measureReportNodes = stepProperties.getPropertyAsBoolean("plugins.rtm.measurereportnodes", true);
		
		AbstractAccessor.createOrUpdateCompoundIndex(context.getMongoDatabase().getCollection("measurements"),"eId", "begin");

		WebAppContext webappCtx = new WebAppContext();
		webappCtx.setContextPath("/rtm");

		String war = step.commons.conf.Configuration.getInstance().getProperty("plugins.rtm.war");
		if(war==null) {
			throw new RuntimeException("Property 'plugins.rtm.war' is null. Unable to start RTM.");
		} else {
			File warFile = new File(war);
			if(!warFile.exists()||!warFile.canRead()) {
				throw new RuntimeException("The file '"+war+"' set by the property 'plugins.rtm.war' doesn't exist or cannot be read. Unable to start RTM.");	
			}
		}
		webappCtx.setWar(war);
		webappCtx.setParentLoaderPriority(true);
		context.getServiceRegistrationCallback().registerHandler(webappCtx);

		accessor = MeasurementAccessor.getInstance();
	}

	private void cloneProperty(Properties rtmProperties, step.commons.conf.Configuration stepProperties, String property) {
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
				measurement.put("eId", stepReport.getExecutionID());
				measurement.put("name", stepReport.getName());
				measurement.put("value", (long)stepReport.getDuration());
				measurement.put("begin", stepReport.getExecutionTime());
				measurement.put("rnId", stepReport.getId().toString());
				measurement.put("rnStatus", stepReport.getStatus().toString());
				measurements.add(measurement);
				
			}

			if(stepReport.getMeasures()!=null) {
				for(Measure measure:stepReport.getMeasures()) {
					measurement = new HashMap<>();
					measurement.put("eId", stepReport.getExecutionID());
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
									// ignore improper types
								}
							}
						}
					}

					measurements.add(measurement);
				}
			}

			accessor.saveManyMeasurements(measurements);;
		}
	}

}
