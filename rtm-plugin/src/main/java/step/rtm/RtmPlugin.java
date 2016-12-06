package step.rtm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.webapp.WebAppContext;
import org.rtm.commons.Configuration;
import org.rtm.commons.Measurement;
import org.rtm.commons.MeasurementAccessor;

import step.artefacts.reports.CallFunctionReportNode;
import step.core.GlobalContext;
import step.core.artefacts.reports.ReportNode;
import step.core.plugins.AbstractPlugin;
import step.core.plugins.Plugin;
import step.grid.io.Measure;

@Plugin
public class RtmPlugin extends AbstractPlugin {

	MeasurementAccessor accessor;

	@Override
	public void executionControllerStart(GlobalContext context) throws Exception {
		context.getServiceRegistrationCallback().registerService(RtmPluginServices.class);

		Properties rtmProperties = Configuration.getInstance().getUnderlyingPropertyObject();
		step.commons.conf.Configuration stepProperties = step.commons.conf.Configuration.getInstance(); 

		cloneProperty(rtmProperties, stepProperties, "db.host");
		cloneProperty(rtmProperties, stepProperties, "db.port");
		cloneProperty(rtmProperties, stepProperties, "db.database");
		cloneProperty(rtmProperties, stepProperties, "db.username");
		cloneProperty(rtmProperties, stepProperties, "db.password");

		WebAppContext webappCtx = new WebAppContext();
		webappCtx.setContextPath("/rtm");

		webappCtx.setWar(Resource.newClassPathResource("rtm-0.3.0.4.war").getURI().toString());
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
			List<Measurement> measurements = new ArrayList<>();
			Measurement measurement = new Measurement();
			measurement.setTextAttribute("eid", stepReport.getExecutionID());
			measurement.setTextAttribute("name", stepReport.getName());
			measurement.setNumericalAttribute("value", (long)stepReport.getDuration());
			measurement.setNumericalAttribute("begin", stepReport.getExecutionTime());
			measurements.add(measurement);

			if(stepReport.getMeasures()!=null) {
				for(Measure measure:stepReport.getMeasures()) {
					measurement = new Measurement();
					measurement.setTextAttribute("eid", stepReport.getExecutionID());
					measurement.setTextAttribute("name", measure.getName());
					measurement.setNumericalAttribute("value", measure.getDuration());
					measurement.setNumericalAttribute("begin", measure.getBegin());
					measurement.setTextAttribute("rnid", stepReport.getId().toString());
					measurement.setTextAttribute("rnstatus", stepReport.getStatus().toString());
					measurements.add(measurement);

					for(Map.Entry<String,String> entry : measure.getData().entrySet()){
						String key = entry.getKey();
						String val = entry.getValue();
						if((key != null) && (val != null)){
							if(	StringUtils.isNumeric(val)){
								try{
									measurement.setNumericalAttribute(key, Long.parseLong(val));
								}catch (NumberFormatException e){
									measurement.setTextAttribute(key, "unparsable_numeric_" + val);
								}
							}else{
								measurement.setTextAttribute(key, val);
							}
						}
					}
				}
			}

			accessor.saveMeasurementsBulk(measurements);
		}
	}

}
