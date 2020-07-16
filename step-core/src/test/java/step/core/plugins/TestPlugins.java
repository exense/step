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
	
	@Plugin()
	public static class TestPlugin extends AbstractTestPlugin2 {
		
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
