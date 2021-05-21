package step.migration;

import step.core.AbstractContext;
import step.core.repositories.RepositoryObjectManager;

public class MigrationContext extends AbstractContext {

	private final RepositoryObjectManager repositoryObjectManager;

	public MigrationContext(RepositoryObjectManager repositoryObjectManager) {
		super();
		this.repositoryObjectManager = repositoryObjectManager;
	}

	public RepositoryObjectManager getRepositoryObjectManager() {
		return repositoryObjectManager;
	};
}
