package step.functions.accessor;

import step.core.accessors.AbstractCRUDAccessor;
import step.core.accessors.MongoClientSession;
import step.functions.Function;

public class FunctionAccessorImpl extends AbstractCRUDAccessor<Function> implements FunctionCRUDAccessor {

	public FunctionAccessorImpl(MongoClientSession clientSession) {
		super(clientSession, "functions", Function.class);
	}

}
