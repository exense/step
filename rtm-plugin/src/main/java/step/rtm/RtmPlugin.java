package step.rtm;

import org.rtm.commons.Configuration;
import org.rtm.commons.Measurement;
import org.rtm.commons.MeasurementAccessor;

import step.artefacts.reports.TestStepReportNode;
import step.core.GlobalContext;
import step.core.artefacts.reports.ReportNode;
import step.core.plugins.AbstractPlugin;
import step.core.plugins.Plugin;

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
		
		accessor = MeasurementAccessor.getInstance();
	}

	@Override
	public void afterReportNodeExecution(ReportNode node) {		
		if(node instanceof TestStepReportNode) {
			TestStepReportNode stepReport = (TestStepReportNode) node;
			Measurement measurement = new Measurement();
			measurement.setTextAttribute("eid", stepReport.getExecutionID());
			measurement.setTextAttribute("name", stepReport.getName());
			measurement.setNumericalAttribute("value", (long)stepReport.getDuration());
			measurement.setNumericalAttribute("begin", stepReport.getExecutionTime());
			accessor.saveMeasurement(measurement);
		}
	}

}
