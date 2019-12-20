package step.core.plugins;

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
	
	public static class AbstractTestPlugin extends AbstractPlugin {
		@Override
		public boolean equals(Object plugin) {
			return getClass().equals(plugin.getClass());
		}
	}
}
