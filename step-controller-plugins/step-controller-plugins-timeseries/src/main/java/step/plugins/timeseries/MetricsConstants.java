package step.plugins.timeseries;

import step.core.timeseries.metric.MetricAttribute;
import step.core.timeseries.metric.MetricAttributeType;

import java.util.Arrays;
import java.util.Map;

public class MetricsConstants {
    
    public static final MetricAttribute STATUS_ATTRIBUTE = new MetricAttribute()
				.setName("rnStatus")
				.setType(MetricAttributeType.TEXT)
				.setMetadata(Map.of("knownValues", Arrays.asList("PASSED", "FAILED", "TECHNICAL_ERROR", "INTERRUPTED")))
				.setDisplayName("Status");
		public static final MetricAttribute TYPE_ATRIBUTE = new MetricAttribute()
				.setName("type")
				.setType(MetricAttributeType.TEXT)
				.setMetadata(Map.of("knownValues", Arrays.asList("keyword", "custom")))
				.setDisplayName("Type");
		public static final MetricAttribute TASK_ATTRIBUTE = new MetricAttribute()
				.setName("taskId")
				.setType(MetricAttributeType.TEXT)
				.setMetadata(Map.of("entity", "task"))
				.setDisplayName("Task");
		 public static final MetricAttribute EXECUTION_ATTRIBUTE = new MetricAttribute()
				.setName("eId")
				.setType(MetricAttributeType.TEXT)
				.setMetadata(Map.of("entity", "execution"))
				.setDisplayName("Execution");
		public static final MetricAttribute PLAN_ATTRIBUTE = new MetricAttribute()
				.setName("planId")
				.setType(MetricAttributeType.TEXT)
				.setMetadata(Map.of("entity", "plan"))
				.setDisplayName("Plan");
		public static final MetricAttribute NAME_ATTRIBUTE = new MetricAttribute()
				.setName("name")
				.setType(MetricAttributeType.TEXT)
				.setDisplayName("Name");
		public static final MetricAttribute ERROR_CODE_ATTRIBUTE = new MetricAttribute()
				.setName("errorCode")
				.setType(MetricAttributeType.TEXT)
				.setDisplayName("Error Code");
    
    
}
