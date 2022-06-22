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
package step.plugins.ide;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.core.GlobalContext;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.grid.agent.AgentRunner;
import step.grid.agent.conf.AgentConf;
import step.grid.agent.conf.TokenConf;
import step.grid.agent.conf.TokenGroupConf;
import step.grid.bootstrap.ResourceExtractor;

@Plugin
public class IDEControllerPlugin extends AbstractControllerPlugin {

	private static final Logger logger = LoggerFactory.getLogger(IDEControllerPlugin.class);

	@Override
	public void serverStart(GlobalContext context) throws Exception {
		ClassLoader initialContextClassLoader = Thread.currentThread().getContextClassLoader();
		try {
			File gridJar = ResourceExtractor.extractResource(AgentRunner.class.getClassLoader(), "step-grid-agent.jar");
			URLClassLoader cl = new URLClassLoader(new URL[] { gridJar.toURI().toURL() },
					AgentRunner.class.getClassLoader());
			Thread.currentThread().setContextClassLoader(cl);
			AgentConf agentConf = new AgentConf();
			agentConf.setGridHost("http://localhost:8081");
			agentConf.setRegistrationPeriod(5000);
			TokenGroupConf tokenGroupConf = new TokenGroupConf();
			tokenGroupConf.setCapacity(1);
			tokenGroupConf.setTokenConf(new TokenConf());
			agentConf.setTokenGroups(List.of(tokenGroupConf));
			AutoCloseable agent = (AutoCloseable) cl.loadClass("step.grid.agent.Agent").getConstructor(AgentConf.class)
					.newInstance(agentConf);
		} finally {
			Thread.currentThread().setContextClassLoader(initialContextClassLoader);
		}
	}
}
