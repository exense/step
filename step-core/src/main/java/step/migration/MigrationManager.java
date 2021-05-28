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

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.core.Version;
import step.core.collections.CollectionFactory;

public class MigrationManager {
	
	private static final Logger logger = LoggerFactory.getLogger(MigrationManager.class);
	
	private final List<Class<? extends MigrationTask>> migrationTasks = new ArrayList<>();
	private final Map<Class<?>, Object> bindings = new HashMap<>();
	
	public MigrationManager() {
		super();
	}
	
	public <T> void addBinding(Class<T> class_, T object) {
		bindings.put(class_, object);
	}

	/**
	 * Register a new migration task
	 * 
	 * @param migrationTask the task to be registered
	 */
	public void register(Class<? extends MigrationTask> migrationTask) {
		migrationTasks.add(migrationTask);
	}

	/**
	 * Runs all the migration tasks registered to migrate
	 * 
	 * @param from the initial version
	 * @param to the new version
	 * @return true if the migration ran successfully 
	 */
	@SuppressWarnings("unchecked")
	public boolean migrate(CollectionFactory collectionFactory, Version from, Version to) {
		final MigrationContext migrationContext = new MigrationContext();
		bindings.forEach((k, v) -> migrationContext.put((Class<Object>) k, v));
		List<MigrationTask> migrators = migrationTasks.stream().map(m -> {
			try {
				Constructor<? extends MigrationTask> constructor = m.getConstructor(CollectionFactory.class, MigrationContext.class);
				return constructor.newInstance(collectionFactory, migrationContext);
			} catch (Exception e) {
				throw new RuntimeException("Error while creating instance of migration task "+m.getName(), e);
			}
		}).collect(Collectors.toList());
		
		logger.info("Migrating from "+from+" to "+to);
		AtomicBoolean successful = new AtomicBoolean(true);
		List<MigrationTask> matchedMigrators = new ArrayList<>();

		boolean upgrade = to.compareTo(from)>=1;
		for(MigrationTask migrator:migrators) {
			if(migrator.asOfVersion.compareTo(upgrade?from:to)>=1 && migrator.asOfVersion.compareTo(upgrade?to:from)<=0) {
				matchedMigrators.add(migrator);
			}
		}	
		matchedMigrators.sort(new Comparator<MigrationTask>() {
			@Override
			public int compare(MigrationTask o1, MigrationTask o2) {
				return (upgrade?1:-1)*o1.asOfVersion.compareTo(o2.asOfVersion);
			}
		});
		matchedMigrators.forEach(m->{
			logger.info("Running migration task "+m);
			long t1 = System.currentTimeMillis();
			try {
				if(upgrade) {
					m.runUpgradeScript();					
				} else {
					m.runDowngradeScript();
				}
				logger.info("Migration task "+m+" successfully executed in "+(System.currentTimeMillis()-t1)+"ms");
			} catch(Exception e) {
				logger.error("Error while running upgrade/downgrade script "+m, e);
				successful.set(false);
			}
		});
		return successful.get();
	}

}
