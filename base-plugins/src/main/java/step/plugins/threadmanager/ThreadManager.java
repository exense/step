package step.plugins.threadmanager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import step.common.managedoperations.Operation;
import step.common.managedoperations.OperationManager;
import step.commons.pools.selectionpool.TokenPool;
import step.core.GlobalContext;
import step.core.execution.ExecutionContext;
import step.core.plugins.AbstractPlugin;
import step.core.plugins.Plugin;
import step.plugins.quotamanager.QuotaManager;

@Plugin
public class ThreadManager extends AbstractPlugin {
	
	private List<Pattern> matchingPatterns = new ArrayList<>();
	
	private void registerPattern(Pattern pattern) {
		matchingPatterns.add(pattern);
	}
	
	private void registerClass(Class<?> clazz) {
		matchingPatterns.add(Pattern.compile(clazz.getName().replaceAll("\\.", "\\.")+".*"));
	}
	
	private boolean matches(StackTraceElement[] stacktrace) {
		for(Pattern p:matchingPatterns) {
			Matcher m = p.matcher("");
			for(StackTraceElement el:stacktrace) {
				m.reset(el.getClassName()+"."+el.getMethodName());
				if(m.matches()) {
					return true;
				}
			}
		}
		return false;
	}
	
	public static final String THREAD_MANAGER_INSTANCE_KEY = "ThreadManagerPlugin_Instance";

	private static final String SET_KEY = "ThreadManagerPlugin_SetKey";
	
	@Override
	public void executionControllerStart(GlobalContext context) {
		context.getServiceRegistrationCallback().registerService(ThreadManagerServices.class);
		context.put(THREAD_MANAGER_INSTANCE_KEY, this);
		
		registerPattern(Pattern.compile(".*\\.sleep$"));
		registerPattern(Pattern.compile(TokenPool.class.getName()+".*"));
		registerClass(QuotaManager.class);
	}

	@SuppressWarnings("unchecked")
	private HashSet<Thread> getRegister(ExecutionContext context) {
		return (HashSet<Thread>) context.get(SET_KEY);
	}
	
	@Override
	public void associateThread(ExecutionContext context, Thread thread) {
		Set<Thread> associatedThreads = getRegister(context);
		
		synchronized (context) {
			if(associatedThreads==null) {
				associatedThreads = new HashSet<>();
				context.put(SET_KEY, associatedThreads);
			}			
		}
		
		synchronized(associatedThreads) {
			associatedThreads.add(thread);
		}
	}

	@Override
	public void unassociateThread(ExecutionContext context, Thread thread) {
		Set<Thread> associatedThreads = getRegister(context);

		synchronized(associatedThreads) {
			associatedThreads.remove(thread);
		}
	}

	@Override
	public void beforeExecutionEnd(ExecutionContext context) {
		Set<Thread> associatedThreads = getRegister(context);

		synchronized(associatedThreads) {
			for(Thread thread:associatedThreads) {
				if(matches(thread.getStackTrace())) {
					thread.interrupt();
				}
			}
		}
	}
	
	public List<Operation> getCurrentOperations(ExecutionContext context) {
		Set<Thread> associatedThreads = getRegister(context);
		List<Operation> operations = new ArrayList<Operation>();
		if(associatedThreads!=null) {
			for(Thread thread:associatedThreads) {
				operations.add(OperationManager.getInstance().getOperation(thread.getId()));
			}
		}
		return operations;
	}
	

}
