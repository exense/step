package step.plugins.quotamanager;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.common.managedoperations.OperationManager;
import step.plugins.quotamanager.config.Quota;
import step.plugins.quotamanager.config.QuotaManagerConfig;
import step.plugins.quotamanager.config.QuotaManagerConfigParser;

public class QuotaManager {
	
	private static final Logger logger = LoggerFactory.getLogger(QuotaManager.class);
		
	private volatile QuotaManagerConfig config;
	
	private volatile List<QuotaHandler> quotaHandlers;
	
	private final ConcurrentHashMap<UUID, List<Permit>> permits = new ConcurrentHashMap<>();
	private final Object paceLockObject = new Object();
	
	public QuotaManager() {
		super();
	}
	
	public QuotaManager(QuotaManagerConfig config) {
		super();
		loadConfiguration(config);
	}
	
	public QuotaManager(File configFile) {
		super();
		loadConfiguration(configFile);
	}
	
	public QuotaManagerConfig getConfig() {
		return config;
	}

	public void loadConfiguration(File configFile) {
		logger.debug("Parsing configuration from file: " + configFile.toString());
		QuotaManagerConfig config = QuotaManagerConfigParser.parseConfig(configFile);
		loadConfiguration(config);
	}
	
	public void loadConfiguration(QuotaManagerConfig config) {
		logger.debug("Loading configuration");
		this.config = config;
		createHandlers();
	}

	private void createHandlers() {
		quotaHandlers = new ArrayList<>();
		if(config.getQuotas()!=null) {
			for(Quota quota:config.getQuotas()) {
				// avoid adding handlers with 0 permit and no timeout
				if (quota.getPermits() > 0 || quota.getAcquireTimeoutMs() != null) {
					QuotaHandler quotaHandler = new QuotaHandler(quota);
					quotaHandlers.add(quotaHandler);
				}
			}
		}
	}
	
	public UUID acquirePermit(Map<String, Object> bindingVariables) throws Exception {
		return acquirePermit(null, bindingVariables);
	}
	
	public UUID acquirePermit(UUID permitID, Map<String, Object> bindingVariables) throws Exception {
		if (permitID != null && permits.get(permitID) != null) {
			// acquirePermit with same UUDI should never happen. 
			return permitID;
		}
		
		logger.debug("Permit request. Binding variables: " + bindingVariables.toString());
		List<Permit> acquiredPermits = new ArrayList<>();
		try {
			for(QuotaHandler quotaHandler: quotaHandlers) {
				try {
					
					long t1 = System.currentTimeMillis();
					String quotaKey = quotaHandler.acquirePermit(bindingVariables);
					if (quotaKey != null) {
						OperationManager.getInstance().enter("Quota acquisition", quotaHandler.getConfig());
						long duration = System.currentTimeMillis()-t1;
						logger.debug("Permit acquired in " + duration + "ms. QuotaKey: " + quotaKey);
						
						if(quotaKey!=null) {
							Permit permit = new Permit(quotaHandler, quotaKey);
							acquiredPermits.add(permit);
						}
					}
				} catch (TimeoutException e) {
					logger.warn("A timeout occurred while trying to acquire permit for quota handler: " + quotaHandler.getConfig().toString() + 
							". Bindings: " + bindingVariables);
					throw e;
				} finally {
					OperationManager.getInstance().exit();
				}
			}
			UUID id = null;
			if (permitID instanceof UUID) {
				id = permitID;
			} else {
				id = UUID.randomUUID();
			}
			permits.put(id, acquiredPermits);
			logger.debug("Permit request succeeded. Returning permitID: " + id);
			return id;
		} catch (Exception e) {
			releasePermits(acquiredPermits);
			throw e;
		}
	}
	
	public void releasePermit(UUID id) {
		List<Permit> permits = this.permits.remove(id);
		if(permits!=null) {
			logger.debug("Releasing permit. PermitID: " + id);
			releasePermits(permits);
		}
	}
	
	private void releasePermits(List<Permit> permits) {
		for(Permit permit:permits) {
			permit.handler.releasePermit(permit.quotaKey);
		}
	}
	
	public List<QuotaHandlerStatus> getStatus() {
		List<QuotaHandlerStatus> statusList = new ArrayList<>();
		for(QuotaHandler quotaHandler: quotaHandlers) {
			statusList.add(quotaHandler.getStatus());
		}
		return statusList;
	}
	
	private class Permit {
		QuotaHandler handler;
		
		String quotaKey;

		public Permit(QuotaHandler manager, String quotaKey) {
			super();
			this.handler = manager;
			this.quotaKey = quotaKey;
		}
	}

	public Object getPaceLockObject() {
		return paceLockObject;
	}
}
