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
package step.core.plugins;

import step.core.plugins.exceptions.PluginCriticalException;

public class TestPlugins {

	@Plugin(dependencies= {})
	public static class TestPlugin1 extends AbstractTestPlugin {
		
	}
	
	@Plugin(dependencies= {TestPlugin5.class})
	public static class TestPlugin2 extends AbstractTestPlugin {
		
	}
	
	@Plugin(dependencies= {})
	public static class TestPlugin3 extends AbstractTestPlugin {
		
	}
	
	@Plugin(dependencies= {TestPlugin1.class})
	public static class TestPlugin4 extends AbstractTestPlugin {
		
	}
	
	@Plugin(dependencies= {TestPlugin3.class, TestPlugin4.class})
	public static class TestPlugin5 extends AbstractTestPlugin {
		
	}
	
	// Circular dependency to TestPlugin6
	@Plugin(dependencies= {TestPlugin7.class})
	public static class TestPlugin6 extends AbstractTestPlugin {
		
	}
	
	// Circular dependency to TestPlugin7
	@Plugin(dependencies= {TestPlugin6.class})
	public static class TestPlugin7 extends AbstractTestPlugin {
		
	}
	
	@Plugin(dependencies= {TestPlugin8.class})
	public static class TestPlugin8 extends AbstractTestPlugin {
		
	}
	
	@Plugin(runsBefore= {TestPlugin3.class})
	public static class TestPlugin9 extends AbstractTestPlugin {
		
	}
	
	@Plugin(runsBefore= {TestPlugin11.class, TestPlugin3.class}, dependencies= {TestPlugin9.class})
	public static class TestPlugin10 extends AbstractTestPlugin {
		
	}
	
	@Plugin(runsBefore= {TestPlugin10.class})
	public static class TestPlugin11 extends AbstractTestPlugin {
		
	}
	
	@Plugin()
	public static class TestPluginWithError extends AbstractTestPlugin {
		
		@Override
		public void myMethod(StringBuilder builder) {
			throw new RuntimeException("Test error");
		}
	}
	
	@Plugin()
	public static class TestPluginWithCriticalException extends AbstractTestPlugin {
		
		@Override
		public void myMethod(StringBuilder builder) {
			throw new PluginCriticalException("Test", new Exception());
		}
	}
	
	@Plugin()
	public static class TestOptionalPlugin extends AbstractTestPlugin implements OptionalPlugin {

		@Override
		public boolean validate() {
			return false;
		}
	}
	
	public static class TestPluginWithoutAnnotation extends AbstractTestPlugin {
		
	}
	
	@Plugin()
	public static class TestPlugin extends AbstractTestPlugin2 {
		
	}
	
	@Plugin()
	@IgnoreDuringAutoDiscovery
	public static class TestIgnoredPlugin extends AbstractTestPlugin2 {
		
	}
	
	public static class AbstractTestPlugin implements TestPluginInterface {
		@Override
		public boolean equals(Object plugin) {
			return getClass().equals(plugin.getClass());
		}

		@Override
		public void myMethod(StringBuilder builder) {
			builder.append(getClass());
		}
	}
	
	public static class AbstractTestPlugin2 extends AbstractTestPlugin implements TestPluginInterface2 {

	}
	
	public interface TestPluginInterface {
		
		public void myMethod(StringBuilder builder);
		
	}
	
	public interface TestPluginInterface2 extends TestPluginInterface {
		
		public void myMethod(StringBuilder builder);
		
	}
	
}
