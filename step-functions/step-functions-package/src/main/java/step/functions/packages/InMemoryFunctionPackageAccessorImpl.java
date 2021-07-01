package step.functions.packages;

import step.core.accessors.AbstractAccessor;
import step.core.collections.inmemory.InMemoryCollection;

public class InMemoryFunctionPackageAccessorImpl extends AbstractAccessor<FunctionPackage>
		implements FunctionPackageAccessor {

	public InMemoryFunctionPackageAccessorImpl() {
		super(new InMemoryCollection<FunctionPackage>());
	}

}