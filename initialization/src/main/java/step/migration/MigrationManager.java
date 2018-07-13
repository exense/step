package step.migration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.core.GlobalContext;
import step.core.Version;

public class MigrationManager {
	
	private static final Logger logger = LoggerFactory.getLogger(MigrationManager.class);
	
	protected final GlobalContext context;
	
	protected List<MigrationTask> migrators = new ArrayList<>();
	
	public MigrationManager(GlobalContext context) {
		super();
		this.context = context;
	}

	/**
	 * Register a new migration task
	 * 
	 * @param migrationTask the task to be registered
	 */
	public void register(MigrationTask migrationTask) {
		migrationTask.setContext(context);
		migrators.add(migrationTask);
	}

	/**
	 * Runs all the migration tasks registered to migrate
	 * 
	 * @param from the initial version
	 * @param to the new version
	 * @return true if the migration ran successfully 
	 */
	public boolean migrate(Version from, Version to) {
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
