package step.rtm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.rtm.commons.MeasurementAccessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.artefacts.reports.CallFunctionReportNode;
import step.core.accessors.AbstractOrganizableObject;
import step.core.artefacts.reports.ReportNode;
import step.core.plugins.IgnoreDuringAutoDiscovery;
import step.core.plugins.Plugin;
import step.core.reports.Measure;
import step.engine.plugins.AbstractExecutionEnginePlugin;

@Plugin
@IgnoreDuringAutoDiscovery
public class RtmPlugin extends AbstractExecutionEnginePlugin {

	private static final Logger logger = LoggerFactory.getLogger(RtmPlugin.class);

	private final boolean measureReportNodes;
	private final MeasurementAccessor accessor;

	public RtmPlugin(boolean measureReportNodes, MeasurementAccessor accessor) {
		super();
		this.measureReportNodes = measureReportNodes;
		this.accessor = accessor;
	}

	@Override
	public void afterReportNodeExecution(ReportNode node) {		
		if(node instanceof CallFunctionReportNode) {
			CallFunctionReportNode stepReport = (CallFunctionReportNode) node;

			Map<String, String> functionAttributes = stepReport.getFunctionAttributes();

			List<Object> measurements = new ArrayList<>();

			Map<String, Object> measurement;
			if(measureReportNodes) {
				measurement = new HashMap<>();

				if(functionAttributes != null) {
					measurement.putAll(functionAttributes);
				}

				measurement.put(RtmControllerPlugin.ATTRIBUTE_EXECUTION_ID, stepReport.getExecutionID());
				//measurement.put("name", stepReport.getFunctionAttributes().get(AbstractOrganizableObject.NAME));
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

					measurement.putAll(functionAttributes);

					measurement.put(RtmControllerPlugin.ATTRIBUTE_EXECUTION_ID, stepReport.getExecutionID());
					measurement.put("name", measure.getName());
					measurement.put("origin", stepReport.getFunctionAttributes().get(AbstractOrganizableObject.NAME));
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

			if (logger.isTraceEnabled()) {
				logMeasurements(measurements);
			}

		}
	}

	public static void logMeasurements(List<Object> measurements) {
		for (Object o: measurements) {
			logger.trace("RTM measure:" + o.toString());
		}
	}
}
