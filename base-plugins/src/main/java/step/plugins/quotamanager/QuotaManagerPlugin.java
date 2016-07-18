package step.plugins.quotamanager;

import java.io.File;
import java.net.URL;

import step.commons.conf.FileWatchService;
import step.core.GlobalContext;
import step.core.plugins.AbstractPlugin;
import step.core.plugins.Plugin;

@Plugin
public class QuotaManagerPlugin extends AbstractPlugin {
	
	public static final String QUOTAMANAGER_KEY = "QuotaManager_Instance";
	
	private QuotaManager initQuotaManager() {		
		URL url = this.getClass().getClassLoader().getResource("QuotaManager.xml");
		final File configFile = new File(url.getFile());
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
		QuotaManager manager = initQuotaManager();
		context.put(QUOTAMANAGER_KEY, manager);
		context.getServiceRegistrationCallback().registerService(QuotaManagerServices.class);
	}
}
