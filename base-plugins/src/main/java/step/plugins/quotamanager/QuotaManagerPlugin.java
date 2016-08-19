package step.plugins.quotamanager;

import java.io.File;

import step.commons.conf.Configuration;
import step.commons.conf.FileWatchService;
import step.core.GlobalContext;
import step.core.plugins.AbstractPlugin;
import step.core.plugins.Plugin;

@Plugin
public class QuotaManagerPlugin extends AbstractPlugin {
	
	public static final String QUOTAMANAGER_KEY = "QuotaManager_Instance";
	
	private QuotaManager initQuotaManager(String config) {		
		final File configFile = new File(config);
		final QuotaManager quotaManager = new QuotaManager(configFile);
		FileWatchService.getInstance().register(configFile, new Runnable() {
			@Override
			public void run() {
				quotaManager.loadConfiguration(configFile);
			}
		});
		return quotaManager;
	}
	
	@Override
	public void executionControllerStart(GlobalContext context) {
		String config = Configuration.getInstance().getProperty("quotamanager.config");
		if(config!=null) {
			QuotaManager manager = initQuotaManager(config);
			context.put(QUOTAMANAGER_KEY, manager);
			context.getServiceRegistrationCallback().registerService(QuotaManagerServices.class);
		}
	}
}
