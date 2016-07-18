package step.artefacts.handlers.teststep;

import java.util.Map;
import java.util.UUID;

import step.plugins.quotamanager.QuotaManager;

public class QuotaManagerMock extends QuotaManager {

	UUID acquiredPermit;
	UUID releasedPermit;
	
	@Override
	public UUID acquirePermit(Map<String, Object> bindingVariables) throws Exception {
		acquiredPermit = UUID.randomUUID();
		return acquiredPermit;
	}
	
	@Override
	public UUID acquirePermit(UUID permitID, Map<String, Object> bindingVariables) throws Exception {
		if (permitID instanceof UUID) {
			acquiredPermit = permitID;
		} else {
			acquiredPermit = UUID.randomUUID();
		}
		
		return acquiredPermit;
	}
	
	@Override
	public void releasePermit(UUID id) {
		if (acquiredPermit != null) {
			releasedPermit = id;
		}
	}

	public UUID getAcquiredPermit() {
		return acquiredPermit;
	}

	public UUID getReleasedPermit() {
		return releasedPermit;
	}
}
