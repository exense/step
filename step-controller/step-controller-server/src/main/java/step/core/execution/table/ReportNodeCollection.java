package step.core.execution.table;

import java.util.ArrayList;
import java.util.List;

import org.bson.conversions.Bson;

import step.core.GlobalContext;
import step.core.accessors.collections.Collection;
import step.core.accessors.collections.MultiTextCriterium;
import step.core.artefacts.reports.ReportNode;
import step.plugins.screentemplating.Input;
import step.plugins.screentemplating.ScreenTemplateManager;

public class ReportNodeCollection extends Collection<ReportNode> {
	
	private final List<String> reportSearchAttributes;

	public ReportNodeCollection(GlobalContext context) {
		super(context.getMongoClientSession().getMongoDatabase(), "reports", ReportNode.class, false);
		
		reportSearchAttributes = new ArrayList<>();
		ScreenTemplateManager screenTemplateManager = context.get(ScreenTemplateManager.class);
		if(screenTemplateManager!=null) {
			for(Input input:screenTemplateManager.getInputsForScreen("functionTable", null, null)) {
				reportSearchAttributes.add("functionAttributes."+input.getId());
			}
		}
		reportSearchAttributes.add("input");
		reportSearchAttributes.add("output");
		reportSearchAttributes.add("error.msg");
		reportSearchAttributes.add("name");
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