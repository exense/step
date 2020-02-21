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
package step.plugins.threadmanager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.common.managedoperations.Operation;
import step.common.managedoperations.OperationManager;
import step.core.GlobalContext;
import step.core.execution.ExecutionContext;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;

@Plugin
public class ThreadManager extends AbstractControllerPlugin {
	
	private static final Logger logger = LoggerFactory.getLogger(ThreadManager.class);
	
	private List<Pattern> matchingPatterns = new ArrayList<>();
	
	private void registerPattern(Pattern pattern) {
		matchingPatterns.add(pattern);
	}
	
	private void registerClass(Class<?> clazz) {
		matchingPatterns.add(Pattern.compile(clazz.getName().replace(".", "\\.")+".*"));
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
	
	private static final String TC_TID_KEY = "ThreadManagerPlugin_TestCasesByTID";
	
	private static final String TC_NAME_KEY = "ThreadManagerPlugin_TestCasesByName";
	
	@Override
	public void executionControllerStart(GlobalContext context) {
		context.getServiceRegistrationCallback().registerService(ThreadManagerServices.class);
		context.put(THREAD_MANAGER_INSTANCE_KEY, this);
		
//		registerPattern(Pattern.compile(".*\\.sleep$"));
//		registerClass(GridClient.class);
//		registerClass(QuotaManager.class);
	}

	@SuppressWarnings("unchecked")
	private HashSet<Thread> getRegister(ExecutionContext context) {
		return (HashSet<Thread>) context.get(SET_KEY);
	}
	
	@SuppressWarnings("unchecked")
	private Map<Long,String> getRegisterTestCasesByTID(ExecutionContext context) {
		return (Map<Long,String>) context.get(TC_TID_KEY);
	}
	
	@SuppressWarnings("unchecked")
	private Map<String,Set<Long>> getRegisterTestCasesByName(ExecutionContext context) {
		return (Map<String,Set<Long>>) context.get(TC_NAME_KEY);
	}
	
	@Override
	public void associateThread(ExecutionContext context, Thread thread, long parentThreadId) {
		logger.debug("associate Thread: " + thread.getId() + ", and parent thread id: " + parentThreadId);
		associateThread(context, thread);
		Map<Long,String> associatedTestcaseByID = getRegisterTestCasesByTID(context);
		String testcase = (associatedTestcaseByID!=null) ? associatedTestcaseByID.get(parentThreadId) : null;
		if (testcase != null) {
			associateTestCase(context,thread,testcase);
		}
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
	
	public void associateTestCase(ExecutionContext context, Thread thread, String id) {
		logger.debug("associateTestCase: " + id + ", ThreadId: " + thread.getId());
		Map<Long,String> associatedTestcaseByID = getRegisterTestCasesByTID(context);
		Map<String,Set<Long>> associatedTestcaseByName = getRegisterTestCasesByName(context);
		
		synchronized (context) {
			if(associatedTestcaseByID==null) {
				associatedTestcaseByID = new HashMap<Long,String>();
				context.put(TC_TID_KEY, associatedTestcaseByID);
			}
			if(associatedTestcaseByName==null) {
				associatedTestcaseByName = new HashMap<String,Set<Long>>();
				context.put(TC_NAME_KEY, associatedTestcaseByName);
			}	
		}
		synchronized(associatedTestcaseByID) {
			associatedTestcaseByID.put(thread.getId(),id);
		}
		synchronized(associatedTestcaseByName) {
			if (associatedTestcaseByName.containsKey(id)) {
				associatedTestcaseByName.get(id).add(thread.getId());
			} else { 
				HashSet<Long> threadIds = new HashSet<Long>();
				threadIds.add(thread.getId());
				associatedTestcaseByName.put(id,threadIds);
			}
		}
	}

	@Override
	public void unassociateThread(ExecutionContext context, Thread thread) {
		Set<Thread> associatedThreads = getRegister(context);
		
		unassociateTestCase(context, thread.getId());

		synchronized(associatedThreads) {
			associatedThreads.remove(thread);
		}		
	}
	
	protected void unassociateTestCase(ExecutionContext context, long tid) {
		logger.debug("in-associateTestCase for thread: " + tid);
		Map<Long,String> associatedTestcaseByID = getRegisterTestCasesByTID(context);
		Map<String,Set<Long>> associatedTestcaseByName = getRegisterTestCasesByName(context);

		String testcase=null;
		synchronized(associatedTestcaseByID) {
			testcase = associatedTestcaseByID.remove(tid);
		}
		if (testcase != null) {
			synchronized(associatedTestcaseByName) {
				if (associatedTestcaseByName.containsKey(testcase)) {
					associatedTestcaseByName.get(testcase).remove(tid);
				} 
			}
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
				Operation op = OperationManager.getInstance().getOperation(thread.getId());
				if (op != null) {
					operations.add(op);
				}
			}
		}
		return operations;
	}
	
	public Map<String,List<Operation>> getCurrentOperationsByTestcases(ExecutionContext context) {
		Map<String,Set<Long>> associatedTestcaseByName = getRegisterTestCasesByName(context);
		Map<String,List<Operation>> operationsMap = new HashMap<String,List<Operation>>();
		if(associatedTestcaseByName!=null) {
			for(String testcase : associatedTestcaseByName.keySet()) {
				List<Operation> operations = new ArrayList<Operation>();
				for (long tid: associatedTestcaseByName.get(testcase)) {
					Operation op = OperationManager.getInstance().getOperation(tid);
					if (op != null) {
						operations.add(op);
					}
				}
				operationsMap.put(testcase, operations);
			}
		}
		return operationsMap;
	}
	

}
