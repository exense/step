package step.core.functions;

import step.core.AbstractContext;

public interface FunctionGroupHandle {
	
	void releaseTokens(AbstractContext context, boolean local) throws Exception;

}
