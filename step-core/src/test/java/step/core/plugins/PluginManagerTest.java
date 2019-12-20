package step.core.plugins;

import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import step.core.plugins.PluginManager.CircularDependencyException;
import step.core.plugins.TestPlugins.TestPlugin1;
import step.core.plugins.TestPlugins.TestPlugin10;
import step.core.plugins.TestPlugins.TestPlugin11;
import step.core.plugins.TestPlugins.TestPlugin2;
import step.core.plugins.TestPlugins.TestPlugin3;
import step.core.plugins.TestPlugins.TestPlugin4;
import step.core.plugins.TestPlugins.TestPlugin5;
import step.core.plugins.TestPlugins.TestPlugin6;
import step.core.plugins.TestPlugins.TestPlugin7;
import step.core.plugins.TestPlugins.TestPlugin8;
import step.core.plugins.TestPlugins.TestPlugin9;

public class PluginManagerTest {

	private PluginManager<AbstractPlugin> pluginManager = new PluginManager<AbstractPlugin>();
	
	@Test
	public void test() throws CircularDependencyException {
		AbstractPlugin[] plugins = new AbstractPlugin[]{new TestPlugin1(),new TestPlugin2(),new TestPlugin3(),new TestPlugin4(),new TestPlugin5()};
		List<AbstractPlugin> sortedPlugins = pluginManager.sortPluginsByDependencies(Arrays.asList(plugins));
		Assert.assertArrayEquals(new AbstractPlugin[]{new TestPlugin1(), new TestPlugin3(), new TestPlugin4(), new TestPlugin5(), new TestPlugin2()}, sortedPlugins.toArray());
	}
	
	@Test
	public void test2() throws CircularDependencyException {
		AbstractPlugin[] plugins = new AbstractPlugin[]{new TestPlugin5(),new TestPlugin4(),new TestPlugin3(),new TestPlugin2(),new TestPlugin1()};
		List<AbstractPlugin> sortedPlugins = pluginManager.sortPluginsByDependencies(Arrays.asList(plugins));
		Assert.assertArrayEquals(new AbstractPlugin[]{new TestPlugin3(), new TestPlugin1(), new TestPlugin4(), new TestPlugin5(), new TestPlugin2()}, sortedPlugins.toArray());
	}
	
	@Test
	public void test3() throws CircularDependencyException {
		AbstractPlugin[] plugins = new AbstractPlugin[]{new TestPlugin2(),new TestPlugin5(),new TestPlugin4(),new TestPlugin1(),new TestPlugin3()};
		List<AbstractPlugin> sortedPlugins = pluginManager.sortPluginsByDependencies(Arrays.asList(plugins));
		Assert.assertArrayEquals(new AbstractPlugin[]{new TestPlugin1(), new TestPlugin4(), new TestPlugin3(), new TestPlugin5(), new TestPlugin2()}, sortedPlugins.toArray());
	}
	
	@Test
	public void testCircularDependency() {
		AbstractPlugin[] plugins = new AbstractPlugin[]{new TestPlugin1(),new TestPlugin2(),new TestPlugin3(),new TestPlugin4(),new TestPlugin5(),new TestPlugin6(),new TestPlugin7()};
		Exception actual = null;
		try {
			List<AbstractPlugin> sortedPlugins = pluginManager.sortPluginsByDependencies(Arrays.asList(plugins));
		} catch (CircularDependencyException e) {
			actual = e;
		}
		Assert.assertNotNull(actual);
	}
	
	@Test
	public void testCircularDependencyToItself() throws CircularDependencyException {
		AbstractPlugin[] plugins = new AbstractPlugin[]{new TestPlugin1(),new TestPlugin8()};
		List<AbstractPlugin> sortedPlugins = pluginManager.sortPluginsByDependencies(Arrays.asList(plugins));
		Assert.assertArrayEquals(new AbstractPlugin[]{new TestPlugin1(), new TestPlugin8()}, sortedPlugins.toArray());
	}

	@Test
	public void testEmptyList() throws CircularDependencyException {
		AbstractPlugin[] plugins = new AbstractPlugin[]{};
		List<AbstractPlugin> sortedPlugins = pluginManager.sortPluginsByDependencies(Arrays.asList(plugins));
		Assert.assertArrayEquals(new AbstractPlugin[]{}, sortedPlugins.toArray());
	}
	
	@Test
	public void testRunsBefore() throws CircularDependencyException {
		AbstractPlugin[] plugins = new AbstractPlugin[]{new TestPlugin3(),new TestPlugin9(),new TestPlugin10()};
		List<AbstractPlugin> sortedPlugins = pluginManager.sortPluginsByDependencies(Arrays.asList(plugins));
		Assert.assertArrayEquals(new AbstractPlugin[]{new TestPlugin9(), new TestPlugin10(), new TestPlugin3()}, sortedPlugins.toArray());
	}
	
	@Test
	public void testCircularDependency2() {
		AbstractPlugin[] plugins = new AbstractPlugin[]{new TestPlugin10(),new TestPlugin11()};
		Exception actual = null;
		try {
			List<AbstractPlugin> sortedPlugins = pluginManager.sortPluginsByDependencies(Arrays.asList(plugins));
		} catch (CircularDependencyException e) {
			actual = e;
		}
		Assert.assertNotNull(actual);
	}
}
