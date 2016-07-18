package step.artefacts.handlers.teststep;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.adapters.commons.model.Output;
import step.core.artefacts.reports.ReportNode;
import step.core.execution.ExecutionContext;

public class TestStepTransactionManager {

	private static final Logger logger = LoggerFactory.getLogger(TestStepTransactionManager.class);
	
	public static void processOutputTransactions(Output output, ReportNode aRportNode) {
		ExecutionContext context = ExecutionContext.getCurrentContext();
		Boolean isLogTransactions = context.getVariablesManager().getVariableAsBoolean("tec.logtransactions");
		if (isLogTransactions) {
//			if (output != null && output.getMeasures() != null && output.getMeasures().size() > 0) {
//				for (Measure transaction : output.getMeasures()) {
//					try {
//						Measurement measurement = new Measurement();
//						measurement.setTextAttribute("name", transaction.getName());
//						measurement.setNumericalAttribute("begin", transaction.getBegin());
//						measurement.setNumericalAttribute("value", transaction.getDuration());
//						measurement.setTextAttribute("eId", context.getExecutionId().toString());
//						measurement.setTextAttribute("eDesc", context.getArtefact().getName());
//						measurement.setTextAttribute("rnId", aRportNode.getId().toString());
//						measurement.setTextAttribute("rnStatus", aRportNode.getStatus().toString());
//						measurement.setTextAttribute("uId", context.getExecutionParameters().getUserID());
//						for(String key:context.getExecutionParameters().getCustomParameters().keySet()) {
//							measurement.setTextAttribute(key,  context.getExecutionParameters().getCustomParameters().get(key));							
//						}
//						if(transaction.getData()!=null) {
//							for(Entry<String, String> entry:transaction.getData().entrySet()) {
//								measurement.setTextAttribute(entry.getKey(), entry.getValue());
//							}
//						}
//						
//						MeasurementAccessor.getInstance().saveMeasurement(measurement);
//					} catch (Throwable e) {
//						if (transaction != null) {
//							logger.error("An error occurred while saving transaction '"+ transaction.toString() + ": " + transaction.getName() + "'", e);
//						} else {
//							logger.error("An error occurred while saving transaction" + ": null", e);
//						}
//					}
//					
//				}
//			}
		}
	}
}
