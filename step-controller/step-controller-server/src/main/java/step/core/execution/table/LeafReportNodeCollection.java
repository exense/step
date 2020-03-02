package step.core.execution.table;

import java.util.ArrayList;
import java.util.List;

import javax.json.JsonObject;

import org.bson.conversions.Bson;

import ch.exense.commons.app.Configuration;
import step.core.GlobalContext;
import step.core.execution.LeafReportNodesFilter;

public class LeafReportNodeCollection extends ReportNodeCollection {
	
	private final List<String[]> optionalReportNodesFilter;
	
	public LeafReportNodeCollection(GlobalContext context) {
		super(context);
		
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
}