package step.client.collections.remote;

import java.io.IOException;
import java.util.Properties;

import step.client.AbstractRemoteClient;
import step.client.credentials.ControllerCredentials;
import step.core.collections.Collection;
import step.core.collections.CollectionFactory;
import step.core.collections.EntityVersion;

public class RemoteCollectionFactory implements CollectionFactory {

    private AbstractRemoteClient client;
    private static String REMOTE_URL_PROP = "url";
    private static String REMOTE_USER_PROP = "user";
    private static String REMOTE_PWD_PROP = "pwd";

    public RemoteCollectionFactory(Properties properties) {
        this(new ControllerCredentials(
                properties.getProperty(REMOTE_URL_PROP),
                properties.getProperty(REMOTE_USER_PROP),
                properties.getProperty(REMOTE_PWD_PROP)
        ));
    }
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
    public void close() throws IOException {
        if (client!=null) {
            client.close();
        }
    }

    @Override
    public <T> Collection<T> getCollection(String name, Class<T> entityClass) {
        return new RemoteCollection(client, name, entityClass);
    }

    @Override
    public Collection<EntityVersion> getVersionedCollection(String name) {
        return new RemoteCollection(client, name + CollectionFactory.VERSION_COLLECTION_SUFFIX, EntityVersion.class);
    }
}
