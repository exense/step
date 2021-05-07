package step.client.collections.remote;

import step.client.AbstractRemoteClient;
import step.client.credentials.ControllerCredentials;
import step.core.accessors.AbstractIdentifiableObject;
import step.core.collections.Collection;
import step.core.collections.CollectionFactory;

import java.io.IOException;

public class RemoteCollectionFactory implements CollectionFactory {

    private AbstractRemoteClient client;

    public RemoteCollectionFactory() {
        this.client = new AbstractRemoteClient();
    }

    public RemoteCollectionFactory(ControllerCredentials credentials) {
        this.client = new AbstractRemoteClient(credentials);
    }

    public RemoteCollectionFactory(AbstractRemoteClient client) {
        this.client = client;
    }


    @Override
    public <T extends AbstractIdentifiableObject> Collection<T> getCollection(String name, Class<T> entityClass) {
        return new RemoteCollection<>(client, name, entityClass);
    }

    @Override
    public void close() throws IOException {
        if (client!=null) {
            client.close();
        }
    }
}
