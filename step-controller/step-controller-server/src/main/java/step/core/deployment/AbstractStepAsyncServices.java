package step.core.deployment;

import jakarta.annotation.PostConstruct;
import step.controller.services.async.AsyncTask;
import step.controller.services.async.AsyncTaskManager;
import step.controller.services.async.AsyncTaskStatus;
import step.core.access.User;
import step.framework.server.Session;

public class AbstractStepAsyncServices extends AbstractStepServices {

	/**
	 * Associates {@link Session} to threads. This is used by requests that are executed
	 * outside the Jetty scope like for {@link AsyncTaskManager}
	 */
	private static final ThreadLocal<Session<User>> sessions = new ThreadLocal<>();
	private AsyncTaskManager asyncTaskManager;

	@PostConstruct
	public void init() throws Exception {
		super.init();
		asyncTaskManager = serverContext.require(AsyncTaskManager.class);
	}

	@Override
	protected Session<User> getSession() {
		Session<User> userSession = sessions.get();
		if (userSession != null) {
			return userSession;
		} else {
			return super.getSession();
		}
	}

	protected <R> AsyncTaskStatus<R> scheduleAsyncTaskWithinSessionContext(AsyncTask<R> asyncTask) {
		Session<User> session = getSession();
		return asyncTaskManager.scheduleAsyncTask(t -> {
			setCurrentSession(session);
			try {
				return asyncTask.apply(t);
			} finally {
				setCurrentSession(null);
			}
		});
	}

	/**
	 * Set the current {@link Session} for the current thread. This is useful for request that are processed
	 * outside the Jetty scope like for {@link AsyncTaskManager}
	 *
	 * @param session the current {@link Session}
	 */
	protected static void setCurrentSession(Session<User> session) {
		sessions.set(session);
	}
}
