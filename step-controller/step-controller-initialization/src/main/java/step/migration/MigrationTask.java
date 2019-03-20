package step.migration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.core.GlobalContext;
import step.core.Version;

public abstract class MigrationTask {

	protected static final Logger logger = LoggerFactory.getLogger(MigrationTask.class);
	
	protected GlobalContext context;
	
	protected Version asOfVersion;

	public GlobalContext getContext() {
		return context;
	}

	protected void setContext(GlobalContext context) {
		this.context = context;
	}

	public Version getAsOfVersion() {
		return asOfVersion;
	}

	/**
	 * @param asOfVersion the version as of which the the task has to be executed
	 */
	public MigrationTask(Version asOfVersion) {
		super();
		this.asOfVersion = asOfVersion;
	}
	
	/**
	 * Script to be executed when migrating from a version lower than asOfVersion to the version asOfVersion
	 */
	public abstract void runUpgradeScript();
	
	/**
	 * Script to be executed when migrating from the version asOfVersion to a version lower than asOfVersion
	 */
	public abstract void runDowngradeScript();

}
