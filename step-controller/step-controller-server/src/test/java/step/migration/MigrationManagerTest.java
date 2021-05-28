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
package step.migration;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import step.core.Version;
import step.core.collections.CollectionFactory;

public class MigrationManagerTest {

	private StringBuilder s;
	
	@Before
	public void before() {
		s = new StringBuilder();
	}
	
	@Test
	public void testUpgrade() {
		MigrationManager m = getMigrationManager();
		m.migrate(null, new Version(1, 2, 2), new Version(1, 2, 4));
		assertEquals("1.2.3,1.2.4,", s.toString());
	}

	public MigrationManager getMigrationManager() {
		MigrationManager m = new MigrationManager();
		m.addBinding(StringBuilder.class, s);
		m.register(TestMigrationTask_0_2_2.class);
		m.register(TestMigrationTask_2_2_5.class);
		m.register(TestMigrationTask_1_2_2.class);
		m.register(TestMigrationTask_1_2_3.class);
		m.register(TestMigrationTask_1_2_4.class);
		m.register(TestMigrationTask_1_2_5.class);
		return m;
	}
	
	@Test
	public void testUpgrade2() {
		MigrationManager m = getMigrationManager();
		m.migrate(null, new Version(1, 2, 2), new Version(5, 2, 4));
		assertEquals("1.2.3,1.2.4,1.2.5,2.2.5,", s.toString());
	}

	@Test
	public void testDowngrade() {
		MigrationManager m = getMigrationManager();
		m.migrate(null, new Version(1, 2, 4), new Version(1, 2, 2));
		assertEquals("-1.2.4,-1.2.3,", s.toString());
	}
	
	private abstract static class TestMigrationTask extends MigrationTask {
		
		private StringBuilder s;
		
		public TestMigrationTask(Version asOfVersion, CollectionFactory collectionFactory,
				MigrationContext migrationContext) {
			super(asOfVersion, collectionFactory, migrationContext);
			s = migrationContext.require(StringBuilder.class);
		}

		@Override
		public void runUpgradeScript() {
			s.append(asOfVersion.toString()+",");
		}
		
		@Override
		public void runDowngradeScript() {
			s.append("-"+asOfVersion.toString()+",");
		}
	}
	
	private static class TestMigrationTask_0_2_2 extends TestMigrationTask {
		public TestMigrationTask_0_2_2(CollectionFactory collectionFactory, MigrationContext migrationContext) {
			super(new Version(0, 2, 2), collectionFactory, migrationContext);
		}
	}
	
	private static class TestMigrationTask_2_2_5 extends TestMigrationTask {
		public TestMigrationTask_2_2_5(CollectionFactory collectionFactory, MigrationContext migrationContext) {
			super(new Version(2, 2, 5), collectionFactory, migrationContext);
		}
	}
	
	private static class TestMigrationTask_1_2_2 extends TestMigrationTask {
		public TestMigrationTask_1_2_2(CollectionFactory collectionFactory, MigrationContext migrationContext) {
			super(new Version(1, 2, 2), collectionFactory, migrationContext);
		}
	}
	
	private static class TestMigrationTask_1_2_3 extends TestMigrationTask {
		public TestMigrationTask_1_2_3(CollectionFactory collectionFactory, MigrationContext migrationContext) {
			super(new Version(1, 2, 3), collectionFactory, migrationContext);
		}
	}
	
	private static class TestMigrationTask_1_2_4 extends TestMigrationTask {
		public TestMigrationTask_1_2_4(CollectionFactory collectionFactory, MigrationContext migrationContext) {
			super(new Version(1, 2, 4), collectionFactory, migrationContext);
		}
	}
	
	private static class TestMigrationTask_1_2_5 extends TestMigrationTask {
		public TestMigrationTask_1_2_5(CollectionFactory collectionFactory, MigrationContext migrationContext) {
			super(new Version(1, 2, 5), collectionFactory, migrationContext);
		}
	}

}
