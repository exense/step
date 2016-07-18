package step.localadapters;


import step.adapters.commons.model.AdapterMessageBuilder;
import step.adapters.commons.model.Input;
import step.adapters.commons.model.Output;
import step.adapters.commons.model.OutputBuilder;
import step.artefacts.handlers.teststep.AbstractLocalAdapter;
import step.common.managedoperations.OperationManager;
import step.core.execution.ExecutionContext;

public class SleepAdapter extends AbstractLocalAdapter {

	@Override
	public Output executeAdapter(Input input) throws Exception {
		AdapterMessageBuilder<Output> outputBuilder = new OutputBuilder();
		
		Long sleepTime = Long.decode(input.getPayload().getDocumentElement().getAttribute("ms"));
		if(!ExecutionContext.getCurrentContext().isSimulation()) {
			try {
				OperationManager.getInstance().enter("Sleep", sleepTime);
				Thread.sleep(sleepTime);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			} finally {
				OperationManager.getInstance().exit();
			}
		}
		outputBuilder.setPayload("<Return />");

		return outputBuilder.build();
	}

}
