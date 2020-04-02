package step.core.execution.table;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.json.JsonObject;

import org.bson.conversions.Bson;

import ch.exense.commons.app.Configuration;
import step.core.GlobalContext;
import step.core.accessors.collections.field.CollectionField;
import step.core.accessors.collections.field.formatter.DateFormatter;
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
	
	@Override
	protected Map<String, CollectionField> getExportFields() {
		Map<String, CollectionField> result = new LinkedHashMap<String,CollectionField> ();
		result.put("executionTime",new CollectionField("executionTime","Begin",new DateFormatter("dd.MM.yyyy HH:mm:ss")));
		result.put("name",new CollectionField("name","Name"));
		result.put("functionAttributes",new CollectionField("functionAttributes","Keyword"));
		result.put("status",new CollectionField("status","Status"));
		result.put("error",new CollectionField("error","Error"));
		result.put("input",new CollectionField("input","Input"));
		result.put("output",new CollectionField("output","Output"));
		result.put("duration",new CollectionField("duration","Duration"));
		result.put("adapter",new CollectionField("adapter","Adapter"));
		return result;
	}

}