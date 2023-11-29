package step.plugins.timeseries.dashboards.model;

public enum ChartFilterItemType {
	OPTIONS, // this is a text with suggestions
	FREE_TEXT,
	EXECUTION, // custom behavior with picker for executions
	TASK, // custom behavior with picker for tasks
	PLAN, // custom behavior with picker for plans
	NUMERIC,
	DATE ,
}
