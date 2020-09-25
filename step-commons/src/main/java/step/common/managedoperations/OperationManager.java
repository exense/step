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
package step.common.managedoperations;

import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

public class OperationManager {
	
	ConcurrentHashMap<Long, Operation> operations = new ConcurrentHashMap<Long, Operation>();
	
	static OperationManager INSTANCE = new OperationManager();
	
	public static OperationManager getInstance() {
		return INSTANCE;
	}
	
	public void enter(String name, Object details) {
		enter(name,details,null);
	}

	public void enter(String name, Object details, String reportNodeId) {
		long tid = Thread.currentThread().getId();
		Operation operation = new Operation(name, new Date(), details, reportNodeId, tid);
		operations.put(tid, operation);
		
	}
	
	public void exit() {
		operations.remove(Thread.currentThread().getId());
	}
	
	public Operation getOperation(Long threadId) {
		return operations.get(threadId);
	}

}
