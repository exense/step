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
package step.artefacts.handlers;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import step.artefacts.Synchronized;
import step.common.managedoperations.OperationManager;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;
import step.core.execution.ExecutionContext;

public class SynchronizedHandler extends ArtefactHandler<Synchronized, ReportNode> {
	
	private static final String LOCK_MAP_KEY = "$synchronizedHandlerLockMap";
	private static final ConcurrentHashMap<String, Lock> globalLockMap = new ConcurrentHashMap<>();
	
	private SequenceHandler sh = new SequenceHandler();
	
	@Override
	public void init(ExecutionContext context) {
		super.init(context);
		synchronized (LOCK_MAP_KEY) {
			if(context.get(LOCK_MAP_KEY)==null) {
				context.put(LOCK_MAP_KEY, new ConcurrentHashMap<>());
			}
		}
	}

	@Override
	public void createReportSkeleton_(ReportNode node, Synchronized testArtefact) {
		sh.createReportSkeleton_(node, testArtefact);
	}
	
	@Override
	public void execute_(ReportNode node, Synchronized testArtefact) throws InterruptedException {
		sh.init(context);
		Lock lock = getLock(testArtefact);
		
		String operation = lock.global?"Waiting for global lock":"Waiting for lock";
		OperationManager.getInstance().enter(operation, lock.name);
		ReentrantLock reentrantLock = lock.lock;
		reentrantLock.lockInterruptibly();
		try {
			sh.execute_(node, testArtefact);
		} finally {
			reentrantLock.unlock();
		}
		OperationManager.getInstance().exit();
	}

	@SuppressWarnings("unchecked")
	protected Lock getLock(Synchronized testArtefact) {
		ConcurrentHashMap<String, Lock> lockMap;
		boolean global;
		if(testArtefact.getGlobalLock().get()) {
			lockMap = globalLockMap;
			global = true;
		} else {
			lockMap = (ConcurrentHashMap<String, Lock>) context.get(LOCK_MAP_KEY);
			global = false;
		}
		String lockId;
		String lockName = testArtefact.getLockName().get();
		if(lockName == null || lockName.isEmpty()) {
			lockId = testArtefact.getId().toString();
		} else {
			lockId = lockName;
		}
		Lock lock = lockMap.computeIfAbsent(lockId, o->new Lock(lockId, global));
		return lock;
	}
	
	private static class Lock {
		final String name;
		final boolean global;
		final ReentrantLock lock;

		public Lock(String name, boolean global) {
			super();
			this.name = name;
			this.global = global;
			this.lock = new ReentrantLock(true);
		}
	}

	@Override
	public ReportNode createReportNode_(ReportNode parentNode, Synchronized testArtefact) {
		return sh.createReportNode_(parentNode, testArtefact);
	}

}
