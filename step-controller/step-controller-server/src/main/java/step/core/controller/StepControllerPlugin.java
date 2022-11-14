package step.core.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.Controller;
import step.core.GlobalContext;
import step.core.controller.errorhandling.ErrorFilter;
import step.core.deployment.ControllerServices;
import step.core.execution.model.Execution;
import step.core.execution.model.ExecutionAccessor;
import step.core.execution.model.ExecutionStatus;
import step.core.plugins.AbstractControllerPlugin;
import step.framework.server.ControllerInitializationPlugin;
import step.core.plugins.Plugin;
import step.core.scheduler.ExecutionScheduler;
import step.core.scheduler.SchedulerServices;
import step.framework.server.CORSRequestResponseFilter;

import java.io.IOException;
import java.util.List;

@Plugin
public class StepControllerPlugin extends AbstractControllerPlugin implements ControllerInitializationPlugin<GlobalContext> {

	private static final Logger logger = LoggerFactory.getLogger(StepControllerPlugin.class);

	private Controller controller;

	@Override
	public void checkPreconditions(GlobalContext context) throws Exception {
		controller = new Controller(context);
		controller.init(context.getServiceRegistrationCallback());
		context.put(Controller.class, controller);
	}

	@Override
	public void recover(GlobalContext context) throws Exception {
		ExecutionAccessor accessor = context.getExecutionAccessor();
		List<Execution> executions = accessor.getActiveTests();
		if(executions!=null && executions.size()>0) {
			logger.warn("Found " + executions.size() + " executions in an inconsistent state. The system might not have been shutdown cleanly or crashed."
					+ "Starting recovery...");
			for(Execution e:executions) {
				logger.warn("Recovering test execution " + e.toString());
				logger.debug("Setting status to ENDED. TestExecutionID:"+ e.getId().toString());
				e.setStatus(ExecutionStatus.ENDED);
				e.setEndTime(System.currentTimeMillis());
				accessor.save(e);
			}
			logger.debug("Recovery ended.");
		}
	}

	@Override
	public void finalizeStart(GlobalContext context) throws Exception {
		context.require(ExecutionScheduler.class).start();
	}

	@Override
	public void serverStart(GlobalContext context) throws Exception {
		context.getServiceRegistrationCallback().registerPackage(ControllerServices.class.getPackage());
		context.getServiceRegistrationCallback().registerService(SchedulerServices.class);
		context.getServiceRegistrationCallback().registerService(ErrorFilter.class);
		context.getServiceRegistrationCallback().registerService(CORSRequestResponseFilter.class);


	}

	@Override
	public void preShutdownHook(GlobalContext context) {
		// waits for executions to terminate
		ExecutionScheduler executionScheduler = context.get(ExecutionScheduler.class);
		if (executionScheduler!=null) {
			logger.info("Stopping execution scheduler");
			executionScheduler.shutdown();
		}

	}

	@Override
	public void postShutdownHook(GlobalContext context) {
		try {
			controller.postShutdownHook();
			logger.info("Collection factory shutdown");
		} catch (IOException e) {
			logger.error("Unable to gracefully shutdown the collection factory.",e);
		}
	}


	@Override
	public void serverStop(GlobalContext context) {

	}

	@Override
	public boolean canBeDisabled() {
		return false;
	}
}
