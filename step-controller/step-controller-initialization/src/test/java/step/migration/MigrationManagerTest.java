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

import static org.junit.Assert.*;

import org.junit.Test;

import step.core.Version;
import step.migration.MigrationManager;
import step.migration.MigrationTask;

public class MigrationManagerTest {

	@Test
	public void testUpgrade() {
		StringBuilder s = new StringBuilder();
		MigrationManager m = getMigrationManager(s);
		m.migrate(new Version(1, 2, 2), new Version(1, 2, 4));
		assertEquals("1.2.3,1.2.4,", s.toString());
	}
	
	@Test
	public void testUpgrade2() {
		StringBuilder s = new StringBuilder();
		MigrationManager m = getMigrationManager(s);
		m.migrate(new Version(1, 2, 2), new Version(5, 2, 4));
		assertEquals("1.2.3,1.2.4,1.2.5,2.2.5,", s.toString());
	}

	@Test
	public void testDowngrade() {
		StringBuilder s = new StringBuilder();
		MigrationManager m = getMigrationManager(s);
		m.migrate(new Version(1, 2, 4), new Version(1, 2, 2));
		assertEquals("-1.2.4,-1.2.3,", s.toString());
	}

	
	protected MigrationManager getMigrationManager(StringBuilder s) {
		MigrationManager m = new MigrationManager(null);
		registerMigrator(s, m, new Version(0, 2, 2));
		registerMigrator(s, m, new Version(2, 2, 5));
		registerMigrator(s, m, new Version(1, 2, 2));
		registerMigrator(s, m, new Version(1, 2, 3));
		registerMigrator(s, m, new Version(1, 2, 4));
		registerMigrator(s, m, new Version(1, 2, 5));
		return m;
	}

	protected void registerMigrator(StringBuilder s, MigrationManager m, Version v) {
		m.register(new MigrationTask(v) {
			
			@Override
			public void runUpgradeScript() {
				s.append(v.toString()+",");
			}
			
			@Override
			public void runDowngradeScript() {
				s.append("-"+v.toString()+",");
			}
		});
	}

}
