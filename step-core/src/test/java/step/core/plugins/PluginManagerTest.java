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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

import step.core.plugins.PluginManager.Builder;
import step.core.plugins.PluginManager.Builder.CircularDependencyException;
import step.core.plugins.TestPlugins.TestOptionalPlugin;
import step.core.plugins.TestPlugins.TestPlugin;
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
import step.core.plugins.TestPlugins.TestPluginInterface;
import step.core.plugins.TestPlugins.TestPluginInterface2;
import step.core.plugins.TestPlugins.TestPluginWithCriticalException;
import step.core.plugins.TestPlugins.TestPluginWithError;
import step.core.plugins.TestPlugins.TestPluginWithoutAnnotation;

public class PluginManagerTest {

	@Test
	public void testBuilderWithPluginsFromClassLoader() throws CircularDependencyException, InstantiationException, IllegalAccessException, ClassNotFoundException {
		PluginManager<TestPluginInterface2> pluginManager = new PluginManager.Builder<TestPluginInterface2>(TestPluginInterface2.class).withPluginsFromClasspath().build();
		assertOrder(pluginManager, TestPlugin.class);
	}
	
	@Test
	public void testBuilderWithPluginsFromClassLoaderAndFilter() throws CircularDependencyException, InstantiationException, IllegalAccessException, ClassNotFoundException {
		PluginManager<TestPluginInterface2> pluginManager = new PluginManager.Builder<TestPluginInterface2>(TestPluginInterface2.class).withPluginsFromClasspath()
				.withPluginFilter(p->false).build();
		Assert.assertEquals(0, pluginManager.getPlugins().size());
	}
	
	@Test
	public void testBuilderWithPluginsFromClassLoader2() throws CircularDependencyException, InstantiationException, IllegalAccessException, ClassNotFoundException {
		PluginManager<TestPluginInterface2> pluginManager = new PluginManager.Builder<TestPluginInterface2>(TestPluginInterface2.class).withPluginsFromClasspath("").build();
		assertOrder(pluginManager, TestPlugin.class);
	}
	
	@Test
	public void testBuilderWithEmptyPluginList() throws CircularDependencyException {
		PluginManager<TestPluginInterface> pluginManager = pluginManager().build();
		TestPluginInterface proxy = pluginManager.getProxy();
		proxy.myMethod(null);
	}
	
	@Test
	public void testBuilderWithOnePlugin() throws CircularDependencyException {
		PluginManager<TestPluginInterface> pluginManager = pluginManager().withPlugin(new TestPlugin1()).build();
		assertOrder(pluginManager, TestPlugin1.class);
	}
	
	@Test
	public void testBuilderWithPluginList() throws CircularDependencyException {
		PluginManager<TestPluginInterface> pluginManager = pluginManager().withPlugins(plugins(new TestPlugin1(),new TestPlugin2())).build();
		assertOrder(pluginManager, TestPlugin1.class, TestPlugin2.class);
	}
	
	@Test
	public void testBuilderWithPluginList2() throws CircularDependencyException {
		PluginManager<TestPluginInterface> pluginManager = pluginManager().withPlugins(plugins(new TestPlugin2(),new TestPlugin1())).build();
		assertOrder(pluginManager, TestPlugin2.class, TestPlugin1.class);
	}
	
	@Test
	public void testBuilderWithPluginList3() throws CircularDependencyException {
		PluginManager<TestPluginInterface> pluginManager = pluginManager().withPlugins(plugins(new TestPlugin1(),new TestPlugin2(),new TestPlugin3(),new TestPlugin4(),new TestPlugin5())).build();
		assertOrder(pluginManager, TestPlugin1.class, TestPlugin3.class, TestPlugin4.class, TestPlugin5.class, TestPlugin2.class);
	}
	
	@Test
	public void testBuilderWithPluginList4() throws CircularDependencyException {
		PluginManager<TestPluginInterface> pluginManager = pluginManager().withPlugins(plugins(new TestPlugin5(),new TestPlugin4(),new TestPlugin3(),new TestPlugin2(),new TestPlugin1())).build();
		assertOrder(pluginManager, new TestPlugin3(), new TestPlugin1(), new TestPlugin4(), new TestPlugin5(), new TestPlugin2());
	}

	@Test
	public void testBuilderWithPluginList5() throws CircularDependencyException {
		PluginManager<TestPluginInterface> pluginManager = pluginManager().withPlugins(plugins(new TestPlugin2(),new TestPlugin5(),new TestPlugin4(),new TestPlugin1(),new TestPlugin3())).build();
		assertOrder(pluginManager, new TestPlugin1(), new TestPlugin4(), new TestPlugin3(), new TestPlugin5(), new TestPlugin2());
	}
	
	@Test
	public void testBuilderWithCircularDependency() throws CircularDependencyException {
		Exception actual = null;
		try {
			pluginManager().withPlugins(plugins(new TestPlugin1(),new TestPlugin2(),new TestPlugin3(),new TestPlugin4(),new TestPlugin5(),new TestPlugin6(),new TestPlugin7())).build();
		} catch (CircularDependencyException e) {
			actual = e;
		}
		Assert.assertNotNull(actual);
	}

	@Test
	public void testBuilderWithCircularDependency2() throws CircularDependencyException {
		Exception actual = null;
		try {
			pluginManager().withPlugins(plugins(new TestPlugin10(),new TestPlugin11())).build();
		} catch (CircularDependencyException e) {
			actual = e;
		}
		Assert.assertNotNull(actual);
	}
	
	@Test
	public void testCircularDependencyToItself() throws CircularDependencyException {
		PluginManager<TestPluginInterface> pluginManager = pluginManager().withPlugins(plugins(new TestPlugin1(),new TestPlugin8())).build();
		assertOrder(pluginManager, new TestPlugin1(), new TestPlugin8());
	}
	
	@Test
	public void testPluginWithoutAnnotation() throws CircularDependencyException {
		PluginManager<TestPluginInterface> pluginManager = pluginManager().withPlugins(plugins(new TestPluginWithoutAnnotation())).build();
		assertOrder(pluginManager, new TestPluginWithoutAnnotation());
	}
	
	@Test
	public void testRunsBefore() throws CircularDependencyException {
		PluginManager<TestPluginInterface> pluginManager = pluginManager().withPlugins(plugins(new TestPlugin3(),new TestPlugin9(),new TestPlugin10())).build();
		assertOrder(pluginManager, new TestPlugin9(), new TestPlugin10(), new TestPlugin3());
	}
	
	@Test
	public void testPluginWithError() throws CircularDependencyException {
		PluginManager<TestPluginInterface> pluginManager = pluginManager().withPlugins(plugins(new TestPluginWithError())).build();
		// Doesn't throw any error. The exceptions occurring in the plugins are simply logged
		pluginManager.getProxy().myMethod(new StringBuilder());
	}
	
	@Test
	public void testPluginWithPluginCriticalException() throws CircularDependencyException {
		PluginManager<TestPluginInterface> pluginManager = pluginManager().withPlugins(plugins(new TestPluginWithCriticalException())).build();
		Exception actualException = null;
		try {
			pluginManager.getProxy().myMethod(new StringBuilder());
		} catch (Exception e) {
			actualException = e;
		}
		Assert.assertNotNull(actualException);
	}
	
	@Test
	public void testOptionalPlugin() throws CircularDependencyException {
		PluginManager<TestPluginInterface> pluginManager = pluginManager().withPlugins(plugins(new TestOptionalPlugin())).build();
		StringBuilder builder = new StringBuilder();
		pluginManager.getProxy().myMethod(builder);
		Assert.assertEquals("", builder.toString());
	}
	
	protected Builder<TestPluginInterface> pluginManager() {
		return new PluginManager.Builder<TestPluginInterface>(TestPluginInterface.class);
	}
	
	protected List<TestPluginInterface> plugins(TestPluginInterface...interfaces) {
		return Arrays.asList(interfaces);
	}
	
	protected void assertOrder(PluginManager<? extends TestPluginInterface> pluginManager, TestPluginInterface... expectedPluginOrder) {
		List<?> pluginClasses = Arrays.asList(expectedPluginOrder).stream().map(p->p.getClass()).collect(Collectors.toList());
		assertOrder(pluginManager, pluginClasses.toArray(new Class[0]));
	}
	
	protected void assertOrder(PluginManager<? extends TestPluginInterface> pluginManager, Class<?>... expectedPluginOrder) {
		TestPluginInterface proxy = pluginManager.getProxy();
		StringBuilder actualStringBuilder = new StringBuilder();
		proxy.myMethod(actualStringBuilder);
		StringBuilder expectedStringBuilder = new StringBuilder();
		Arrays.asList(expectedPluginOrder).forEach(p->expectedStringBuilder.append(p.toString()));
		Assert.assertEquals(expectedStringBuilder.toString(), actualStringBuilder.toString());
	}
}
