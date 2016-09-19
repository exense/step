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
package step.rtm;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

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
				
		webappCtx.setWar(Resource.newClassPathResource("rtm-0.3.0.3").getURI().toString());
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
				}
			}
			
			accessor.saveMeasurementsBulk(measurements);
		}
	}

}
