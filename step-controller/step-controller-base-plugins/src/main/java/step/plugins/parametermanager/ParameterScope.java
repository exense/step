package step.plugins.parametermanager;

public enum ParameterScope {

	// Parameters with this scope are global to all the plans and functions
	GLOBAL,
	
	// Parameters with this scope are specific to one application and apply only to functions (aka Keywords)
	APPLICATION,
	
	// Parameters with this scope are specific to one function (aka Keyword)
	FUNCTION;
}
