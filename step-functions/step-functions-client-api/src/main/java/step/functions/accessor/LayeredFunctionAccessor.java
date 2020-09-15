package step.functions.accessor;

import java.util.List;

import step.core.accessors.LayeredCRUDAccessor;
import step.functions.Function;

public class LayeredFunctionAccessor extends LayeredCRUDAccessor<Function> implements FunctionAccessor {

	public LayeredFunctionAccessor() {
		super();
	}

	public LayeredFunctionAccessor(List<FunctionAccessor> accessors) {
		super(accessors);
	}

}
