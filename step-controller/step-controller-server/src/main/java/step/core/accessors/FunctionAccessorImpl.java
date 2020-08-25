package step.core.accessors;

import step.functions.Function;
import step.functions.accessor.FunctionAccessor;

public class FunctionAccessorImpl extends AbstractCRUDAccessor<Function> implements FunctionAccessor {

	public FunctionAccessorImpl(MongoClientSession clientSession) {
		super(clientSession, "functions", Function.class);
	}

}
