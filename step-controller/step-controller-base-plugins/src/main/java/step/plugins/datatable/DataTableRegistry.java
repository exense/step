package step.plugins.datatable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.mongodb.client.MongoDatabase;

import step.core.GlobalContext;
import step.core.accessors.Collection;
import step.core.artefacts.reports.ReportNodeStatus;
import step.plugins.screentemplating.Input;
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
		ColumnBuilder leafReportNodesColumns = new ColumnBuilder();
		leafReportNodesColumns.addDateColumn("Begin", "executionTime").addColumn("Name","name").addJsonColumn("Keyword","functionAttributes").addColumn("Status","status").addColumn("Error", "error")
		.addColumn("Input","input").addColumn("Output","output").addColumn("Duration","duration").addColumn("Adapter", "adapter");

		// Report table
		
		List<String> reportSearchAttributes = new ArrayList<>();
		if(screenTemplates!=null) {
			for(Input input:screenTemplates.getInputsForScreen("functionTable", null)) {
				reportSearchAttributes.add("functionAttributes."+input.getId());
			}
		}
		reportSearchAttributes.add("input");
		reportSearchAttributes.add("output");
		reportSearchAttributes.add("error.msg");
		reportSearchAttributes.add("name");
		
		BackendDataTable leafReportNodes = new BackendDataTable(new Collection(database, "reports", false));
		String optionalReportNodesFilterStr = context.getConfiguration().getProperty("execution.reports.nodes.include", 
				"_class:step.artefacts.reports.EchoReportNode,"+
				"_class:step.artefacts.reports.RetryIfFailsReportNode,"+
				"_class:step.artefacts.reports.SleepReportNode,"+
				"_class:step.artefacts.reports.WaitForEventReportNode");
		List<String[]> optionalReportNodesFilter = new ArrayList<String[]>();
		for (String kv: optionalReportNodesFilterStr.split(",")) {
			optionalReportNodesFilter.add(kv.split(":"));
		}
		leafReportNodes.addColumn("ID", "_id").addTimeColumn("Begin", "executionTime").addRowAsJson("Step",reportSearchAttributes)
		.addTextWithDropdownColumnOptimized("Status", "status", Arrays.asList(ReportNodeStatus.values()).stream().map(Object::toString).collect(Collectors.toList()))
		.setQuery(new LeafReportNodesFilter(optionalReportNodesFilter)).setExportColumns(leafReportNodesColumns.build());
		
		BackendDataTable leafReportNodesOQL = new BackendDataTable(new Collection(database, "reports"));
		leafReportNodesOQL.addColumn("ID", "_id").addColumn("Execution", "executionID").addTimeColumn("Begin", "executionTime").addRowAsJson("Step",reportSearchAttributes)
		.addArrayColumn("Attachments", "attachments").addTextWithDropdownColumn("Status", "status", Arrays.asList(ReportNodeStatus.values()).stream().map(Object::toString).collect(Collectors.toList()))
		.setQuery(new OQLFilter()).setExportColumns(leafReportNodesColumns.build());

		addTable("reports", leafReportNodes);
		addTable("reportsByOQL", leafReportNodesOQL);
		
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
