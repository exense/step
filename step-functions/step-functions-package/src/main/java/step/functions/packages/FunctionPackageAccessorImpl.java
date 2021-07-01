package step.functions.packages;

import step.core.accessors.AbstractAccessor;
import step.core.collections.Collection;

public class FunctionPackageAccessorImpl extends AbstractAccessor<FunctionPackage> implements FunctionPackageAccessor {

	public FunctionPackageAccessorImpl(Collection<FunctionPackage> collectionDriver) {
		super(collectionDriver);
	}
}
