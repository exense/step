package step.rtm;

import java.util.ArrayList;
import java.util.List;

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
		
		String host = Configuration.getInstance().getProperty("ds.host");
		if(host==null || host.length()==0) {
			Configuration.getInstance().getUnderlyingPropertyObject().put("ds.host", context.getMongoClient().getAddress().getHost());			
		}
		
		WebAppContext webappCtx = new WebAppContext();
		webappCtx.setContextPath("/rtm");
				
		webappCtx.setWar(Resource.newClassPathResource("rtm-0.3.0.3").getURI().toString());
		webappCtx.setParentLoaderPriority(true);
		context.getServiceRegistrationCallback().registerHandler(webappCtx);
		
		accessor = MeasurementAccessor.getInstance();
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
					measurements.add(measurement);
				}
			}
			
			accessor.saveMeasurementsBulk(measurements);
		}
	}

}
