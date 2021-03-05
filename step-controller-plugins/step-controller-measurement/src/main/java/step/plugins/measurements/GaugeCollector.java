package step.plugins.measurements;

import java.util.List;

public abstract class GaugeCollector {
	abstract public String getName();
	abstract public String getDescription();
	abstract public String[] getLabels();
	abstract public List<GaugeMetric> collect();
	public class GaugeMetric {
		public String[] labelsValue;
		public Double value;
		public GaugeMetric(Double value, String... labelsValue) {
			this.value = value;
			this.labelsValue = labelsValue;
		}
	}
}
