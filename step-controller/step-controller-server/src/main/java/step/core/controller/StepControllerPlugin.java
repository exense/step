package step.core.controller;

import ch.exense.commons.app.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.controller.services.async.AsyncTaskManager;
import step.core.Controller;
import step.core.GlobalContext;
import step.core.artefacts.reports.aggregated.ReportNodeTimeSeries;
import step.core.controller.errorhandling.ErrorFilter;
import step.core.deployment.ControllerServices;
import step.core.execution.model.Execution;
import step.core.execution.model.ExecutionAccessor;
import step.core.execution.model.ExecutionStatus;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.exceptions.PluginCriticalException;
import step.framework.server.ControllerInitializationPlugin;
import step.core.plugins.Plugin;
import step.core.scheduler.ExecutionScheduler;
import step.core.scheduler.SchedulerServices;
import step.framework.server.CORSRequestResponseFilter;
import step.versionmanager.ControllerLog;
import step.versionmanager.VersionManager;

import java.io.IOException;
import java.util.List;

@Plugin
public class StepControllerPlugin extends AbstractControllerPlugin implements ControllerInitializationPlugin<GlobalContext> {

	private static final Logger logger = LoggerFactory.getLogger(StepControllerPlugin.class);

	private Controller controller;

	@Override
	public void checkPreconditions(GlobalContext context) throws Exception {

	}

	@Override
	public void init(GlobalContext context) throws Exception {
		// Only used for validation here, blows up if there is something wrong with the configuration.
		getControllerUrl(context.getConfiguration(), false, true);
		controller = new Controller(context);
		controller.init(context.getServiceRegistrationCallback());
		context.put(Controller.class, controller);
	}

    /**
     * Validates the @{code controller.url} setting in {@code step.properties}. Note that in production environments,
     * only the abovementioned key is expected to exist, and both backend and frontend (UI) communications can use it.
     * In development, the frontend and backend listen on different ports, with some (not all) requests being proxied
     * from FE to BE. If that is the case, an additional {code.services.url} may be defined. Callers of this method
     * can specify if they wish to get the version which honors the override (services == true), or the default one
     * (services == false). Validation is optional.
     * <p>
     * Note that because this method is invoked on startup/server initialization by the StepControllerPlugin itself,
     * it is guaranteed that at least the basic configuration is correct.
     *
     * @param conf     configuration
     * @param forBackend - if @{code true}, also evaluates the dev-only override in order to return the URL usable for
     *                 service communication. If it exists, it is returned, otherwise the normal value is returned.
     *                 If @{false}, only the normal value is returned.
     * @return the corresponding URL
     * @throws PluginCriticalException if the configuration is invalid.
     */
    public static String getControllerUrl(Configuration conf, boolean forBackend, boolean validate) throws PluginCriticalException {
        String paramName = "controller.url";
        String url = conf.getProperty(paramName, null);
        if (url == null) {
            throw new PluginCriticalException("Configuration parameter 'controller.url' is required. " +
                    "Please configure a valid URL in step.properties");
        }
        if (forBackend) {
            String servicesUrl = conf.getProperty("controller.services.url", null);
            if (servicesUrl != null) {
                url = servicesUrl;
                paramName = "controller.services.url";
            }
        }
        if (validate) {
            // Special case for the default sample value -- this was previously accepted even though
            // it produced invalid links, but is now considered a configuration error.
            if (url.equals("http://step.controller.mydomain.com:8080")) {
                throw new PluginCriticalException(String.format(
                        "Configuration parameter '%s' with value '%s' is invalid. " +
                                "Please configure a valid URL in step.properties",
                        paramName, url));
            }
            // Simple sanity check
            if (!url.matches("^https?://.+")) {
                throw new PluginCriticalException(String.format(
                        "Configuration parameter '%s' with value '%s' is invalid. " +
                                "The URL must start with http:// or https:// . " +
                                "Please configure a valid URL in step.properties",
                        paramName, url));
            }
        }
        return url;
    }



	@Override
	public void recover(GlobalContext context) throws Exception {
		//At this stage the version manager plugin cannot be started yet, so we create a version manager directly here to read the last start time
		VersionManager<GlobalContext> versionManager = new VersionManager<>(context);
		versionManager.readLatestControllerLog();
		ControllerLog latestControllerLog = versionManager.getLatestControllerLog();
		ExecutionAccessor accessor = context.getExecutionAccessor();
		List<Execution> executions = accessor.getActiveTests((latestControllerLog == null) ? 0 : latestControllerLog.getStart().getTime());
		if(executions!=null && !executions.isEmpty()) {
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
		//Initialize new empty resolutions after start (require the async task manager)
		//Because the ReportNodeTimeSeries is created in ControllerServer.init directly and not in a plugin, this the right place to do it
		ReportNodeTimeSeries reportNodeTimeSeries = context.require(ReportNodeTimeSeries.class);
		AsyncTaskManager asyncTaskManager = context.require(AsyncTaskManager.class);
		asyncTaskManager.scheduleAsyncTask((empty) -> {
			logger.info("ReportNode timeSeries ingestion for empty resolutions has started");
			reportNodeTimeSeries.getTimeSeries().ingestDataForEmptyCollections();
			logger.info("TimeSeries ingestion for empty resolutions has finished");
			return null;
		});

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
			if (controller != null) {
				controller.postShutdownHook();
				logger.info("Collection factory shutdown");
			}
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
