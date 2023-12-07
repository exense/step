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
package step.grid.proxy;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.grid.bootstrap.ResourceExtractor;

public class GridProxyRunner {
	
	private static final Logger logger = LoggerFactory.getLogger(GridProxyRunner.class);
	
	public static void main(String[] args) throws Exception {
		File gridJar = ResourceExtractor.extractResource(GridProxyRunner.class.getClassLoader(), "step-grid-proxy.jar");
		URLClassLoader cl = new URLClassLoader(new URL[]{gridJar.toURI().toURL()}, GridProxyRunner.class.getClassLoader());
		Thread.currentThread().setContextClassLoader(cl);
		AutoCloseable gridProxy = (AutoCloseable) cl.loadClass("step.grid.proxy.GridProxy").getMethod("newInstanceFromArgs",args.getClass()).invoke(null, (Object)args);

		Runtime.getRuntime().addShutdownHook(new Thread(()->{
			try {
				gridProxy.close();
			} catch (Exception e) {
				logger.error("Error while closing the grid proxy", e);
			}
		}));
	}
}
