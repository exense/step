package step.ide.collections;

import step.core.collections.AutomationPackageCollectionFactory;
import step.core.collections.Collection;
import step.core.collections.CollectionFactory;
import step.core.collections.EntityVersion;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

public class CurrentlyOpenedAutomationPackageCollectionFactory implements CollectionFactory {

    private static CurrentlyOpenedAutomationPackageCollectionFactory INSTANCE;

    private AutomationPackageCollectionFactory currentAPFactory;
    private final ConcurrentHashMap<String, DynamicallyDelegatingCollection<?>> collectionsByName = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, DynamicallyDelegatingCollection<?>> typedViewsByNameAndClass = new ConcurrentHashMap<>();

    public CurrentlyOpenedAutomationPackageCollectionFactory(Properties ignored) {
        if (INSTANCE == null) {
            INSTANCE = this;
        } else {
            throw new IllegalStateException("Only one instance is allowed");
        }
    }

    public static CurrentlyOpenedAutomationPackageCollectionFactory getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("No CurrentlyOpenedAutomationPackageCollectionFactory instance created yet");
        }
        return INSTANCE;
    }

    public void setCurrentFactory(AutomationPackageCollectionFactory currentAPFactory) {
        if (this.currentAPFactory != null) {
            try {
                this.currentAPFactory.close();
            } catch (IOException e) {
                // TODO better logging
                e.printStackTrace();
            }
        }
        this.currentAPFactory = currentAPFactory;
        collectionsByName.values().forEach(collection -> collection.setFromCurrentFactory(currentAPFactory));
        typedViewsByNameAndClass.values().forEach(collection -> collection.setFromCurrentFactory(currentAPFactory));
    }

    /**
     * A collection is held with the class it is first requested with. Requested with another class, the scheduler
     * tasks and the wrapper their table lists them as for instance, it is returned as a view converting its entities.
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> Collection<T> getCollection(String name, Class<T> entityClass) {
        DynamicallyDelegatingCollection<?> collection = collectionsByName.computeIfAbsent(name, n -> new DynamicallyDelegatingCollection<T>(name, entityClass, currentAPFactory));
        if (collection.getEntityClass() == entityClass) {
            return (Collection<T>) collection;
        }
        return (Collection<T>) typedViewsByNameAndClass.computeIfAbsent(name + ":" + entityClass.getName(),
            k -> new DynamicallyDelegatingCollection<T>(name, entityClass, collection.getEntityClass(), currentAPFactory));
    }

    @SuppressWarnings("rawtypes")
    @Override
    public Collection<EntityVersion> getVersionedCollection(String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() throws IOException {

    }
}
