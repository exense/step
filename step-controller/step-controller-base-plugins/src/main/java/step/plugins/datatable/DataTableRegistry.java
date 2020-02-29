package step.plugins.datatable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import com.mongodb.client.MongoDatabase;

import step.core.GlobalContext;
import step.plugins.screentemplating.ScreenTemplateChangeListener;
import step.plugins.screentemplating.ScreenTemplateManager;

public class DataTableRegistry implements ScreenTemplateChangeListener {

	protected GlobalContext context; 
	
	protected Map<String, BackendDataTable> tables = new ConcurrentHashMap<>();	
	
	protected MongoDatabase database;
	
	protected ScreenTemplateManager screenTemplates;
	
	protected final List<Consumer<DataTableRegistry>> initializationScripts = new ArrayList<>();

	public DataTableRegistry(GlobalContext context) {
		super();
		
		this.context = context;
		database = context.getMongoClientSession().getMongoDatabase();
		screenTemplates = context.get(ScreenTemplateManager.class);
		
		screenTemplates.registerListener(this);

		init();
	}
	
	public void registerInitializationScript(Consumer<DataTableRegistry> script) {
		script.accept(this);
		initializationScripts.add(script);
	}
	
	protected void init() {
		initializationScripts.forEach(s->s.accept(this));
	}

	public BackendDataTable addTable(String key, BackendDataTable value) {
		return tables.put(key, value);
	}
	
	public BackendDataTable getTable(String key) {
		return tables.get(key);
	}

	@Override
	public void onChange() {
		init();
	}
}
