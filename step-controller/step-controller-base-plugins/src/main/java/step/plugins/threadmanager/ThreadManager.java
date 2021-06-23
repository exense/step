/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
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
 ******************************************************************************/
package step.plugins.threadmanager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.common.managedoperations.Operation;
import step.common.managedoperations.OperationManager;
import step.core.artefacts.reports.ReportNode;
import step.core.execution.ExecutionContext;

public class ThreadManager {
	
	private static final Logger logger = LoggerFactory.getLogger(ThreadManager.class);
	
	private List<Pattern> matchingPatterns = new ArrayList<>();
	
	public void registerPattern(Pattern pattern) {
		matchingPatterns.add(pattern);
	}
	
	public void registerClass(Class<?> clazz) {
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
	
	private static final String SET_KEY = "ThreadManagerPlugin_SetKey";
	
	@SuppressWarnings("unchecked")
	private HashSet<Thread> getRegister(ExecutionContext context) {
		return (HashSet<Thread>) context.get(SET_KEY);
	}
	
	public void associateThread(ExecutionContext context, Thread thread, long parentThreadId) {
		logger.debug("associate Thread: " + thread.getId() + ", and parent thread id: " + parentThreadId);
		
		// associate the thread ID to all the report nodes associated to the parentThreadId 
		reportNodeIdToThreadId.entrySet().stream().filter(e->{
			return e.getValue().contains(parentThreadId);
		}).forEach(e->e.getValue().add(thread.getId()));;
		
		associateThread(context, thread);
	}
	
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
	
	// Track all the threads (parent + children) associated to a report node
	private Map<String, List<Long>> reportNodeIdToThreadId = new ConcurrentHashMap<>();
	
	public void beforeReportNodeExecution(ExecutionContext context, ReportNode node) {
		// Associate the current thread ID to this report node
		String reportNodeId = node.getId().toString();
		long threadId = Thread.currentThread().getId();
		List<Long> threads = reportNodeIdToThreadId.computeIfAbsent(reportNodeId, k->new CopyOnWriteArrayList<>());
		threads.add(threadId);
	}

	public void afterReportNodeExecution(ExecutionContext context, ReportNode node) {
		// Remove the list of threads for this report node
		String reportNodeId = node.getId().toString();
		reportNodeIdToThreadId.remove(reportNodeId);
	}
	
	public List<Operation> getCurrentOperationsByReportNodeId(String reportNodeId) {
		OperationManager operationManager = OperationManager.getInstance();
		List<Long> threadIds = reportNodeIdToThreadId.get(reportNodeId);
		if(threadIds!=null) {
			return threadIds.stream().map(threadId->operationManager.getOperation(threadId)).filter(o->o!=null).collect(Collectors.toList());
		} else {
			return new ArrayList<>();
		}
	}
	
	public void unassociateThread(ExecutionContext context, Thread thread) {
		Set<Thread> associatedThreads = getRegister(context);

		synchronized(associatedThreads) {
			associatedThreads.remove(thread);
		}
		
		long threadId = thread.getId();
		reportNodeIdToThreadId.entrySet().forEach(e->e.getValue().remove(threadId));
	}

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
			synchronized(associatedThreads) {
				for(Thread thread:associatedThreads) {
					Operation op = OperationManager.getInstance().getOperation(thread.getId());
					if (op != null) {
						operations.add(op);
					}
				}
			}
		}
		return operations;
	}
}
