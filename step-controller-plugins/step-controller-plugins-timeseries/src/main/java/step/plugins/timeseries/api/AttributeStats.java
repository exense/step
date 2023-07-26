package step.plugins.timeseries.api;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

public class AttributeStats {
	
	@NotNull
	private String name;
	
	@NotNull
	private double usagePercentage;

	public AttributeStats(String name, double usagePercentage) {
		this.name = name;
		this.usagePercentage = usagePercentage;
	}

	public String getName() {
		return name;
	}

	public double getUsagePercentage() {
		return usagePercentage;
	}
}
