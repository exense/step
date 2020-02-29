package step.core.execution.table;

import java.util.ArrayList;
import java.util.List;

import javax.json.JsonObject;

import org.bson.conversions.Bson;

import ch.exense.commons.app.Configuration;
import step.core.GlobalContext;
import step.core.accessors.Collection;
import step.core.accessors.collections.MultiTextCriterium;
import step.core.artefacts.reports.ReportNode;
import step.core.execution.LeafReportNodesFilter;
import step.plugins.screentemplating.Input;
import step.plugins.screentemplating.ScreenTemplateManager;

public class ReportNodeCollection extends Collection<ReportNode> {
	
	private final List<String[]> optionalReportNodesFilter;
	private final List<String> reportSearchAttributes;

	public ReportNodeCollection(GlobalContext context) {
		super(context.getMongoClientSession().getMongoDatabase(), "reports", ReportNode.class, true);
		
		Configuration configuration = context.getConfiguration();
		String optionalReportNodesFilterStr = configuration.getProperty("execution.reports.nodes.include", 
				"_class:step.artefacts.reports.EchoReportNode,"+
				"_class:step.artefacts.reports.RetryIfFailsReportNode,"+
				"_class:step.artefacts.reports.SleepReportNode,"+
				"_class:step.artefacts.reports.WaitForEventReportNode");
		optionalReportNodesFilter = new ArrayList<String[]>();
		for (String kv: optionalReportNodesFilterStr.split(",")) {
			optionalReportNodesFilter.add(kv.split(":"));
		}
		
		reportSearchAttributes = new ArrayList<>();
		ScreenTemplateManager screenTemplateManager = context.get(ScreenTemplateManager.class);
		if(screenTemplateManager!=null) {
			for(Input input:screenTemplateManager.getInputsForScreen("functionTable", null)) {
				reportSearchAttributes.add("functionAttributes."+input.getId());
			}
		}
		reportSearchAttributes.add("input");
		reportSearchAttributes.add("output");
		reportSearchAttributes.add("error.msg");
		reportSearchAttributes.add("name");
	}

	@Override
	public List<Bson> getAdditionalQueryFragments(JsonObject queryParameters) {
		List<Bson> additionalQueryFragments = super.getAdditionalQueryFragments(queryParameters);
		if(additionalQueryFragments == null) {
			additionalQueryFragments = new ArrayList<>();
		}
		additionalQueryFragments.add(new LeafReportNodesFilter(optionalReportNodesFilter).buildAdditionalQuery(queryParameters));
		return additionalQueryFragments;
	}

	@Override
	public Bson getQueryFragmentForColumnSearch(String columnName, String searchValue) {
		if(columnName.equals("step")) {
			Bson queryFragment = new MultiTextCriterium(reportSearchAttributes).createQuery(columnName, searchValue);
			return queryFragment;
		} else {
			return super.getQueryFragmentForColumnSearch(columnName, searchValue);
		}
	}

	@Override
	public Class<?> getEntityClass() {
		return ReportNode.class;
	}
}