package step.core.execution.table;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.client.model.Filters;

import step.core.GlobalContext;
import step.core.accessors.collections.Collection;
import step.core.accessors.collections.MultiTextCriterium;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
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
			// Returning a list of equality conditions instead of regexp conditions if possible 
			// This make the execution of the query much more efficient because mongo can use the compound index including the status
			// The regexp condition makes the use of the compound index not possible and forces the use of the $match pipeline
			if(columnName.equals("status")) {
				Bson filter = getReportNodeStatusFilterOrNull(searchValue);
				if(filter != null) {
					return filter;
				} else {
					if(searchValue.startsWith("(") && searchValue.endsWith(")")) {
						String[] split = searchValue.substring(1, searchValue.length()-1).split("\\|");
						List<Bson> fragments = new ArrayList<>();
						AtomicBoolean allFilterParsed = new AtomicBoolean(true);
						Arrays.asList(split).forEach(s->{
							Bson status = getReportNodeStatusFilterOrNull(s);
							if(status == null) {
								allFilterParsed.set(false);
							} else {
								fragments.add(status);
							}
						});
						if(allFilterParsed.get()) {
							return Filters.or(fragments);
						} else {
							return super.getQueryFragmentForColumnSearch(columnName, searchValue);
						}
					} else {
						return super.getQueryFragmentForColumnSearch(columnName, searchValue);
					}
				}
			} else {
				return super.getQueryFragmentForColumnSearch(columnName, searchValue);
			}
		}
	}

	protected Bson getReportNodeStatusFilterOrNull(String searchValue) {
		try {
			ReportNodeStatus status = ReportNodeStatus.valueOf(searchValue);
			return new Document("status", status.toString());
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	@Override
	public Class<?> getEntityClass() {
		return ReportNode.class;
	}
}