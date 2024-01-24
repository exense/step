package step.automation.packages.hooks;

import step.core.accessors.AbstractIdentifiableObject;

public interface AutomationPackageHook<T extends AbstractIdentifiableObject> {

    public void onCreate(T entity);

    public void onDelete(T entity);
}
