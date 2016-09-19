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
package step.grid.agent.handler;

import java.util.concurrent.Callable;

import step.grid.agent.tokenpool.AgentTokenWrapper;
import step.grid.io.InputMessage;
import step.grid.io.OutputMessage;

public class ClassLoaderMessageHandlerWrapper extends MessageHandlerDelegate {

	ClassLoader cl;
	
	public ClassLoaderMessageHandlerWrapper(ClassLoader cl) {
		super();
		this.cl = cl;
	}

	@Override
	public OutputMessage handle(final AgentTokenWrapper token, final InputMessage message) throws Exception {
		return runInContext(new Callable<OutputMessage>() {
			@Override
			public OutputMessage call() throws Exception {
				return delegate.handle(token, message);
			}
		});
	}

	@Override
	public <T> T runInContext(Callable<T> runnable) throws Exception {
		ClassLoader previousCl = Thread.currentThread().getContextClassLoader();
		
		Thread.currentThread().setContextClassLoader(cl);
		try {
			return runnable.call();
		} finally {
			Thread.currentThread().setContextClassLoader(previousCl);
		}
	} 
}
