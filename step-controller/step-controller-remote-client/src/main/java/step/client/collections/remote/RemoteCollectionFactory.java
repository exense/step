package step.client.collections.remote;

import step.client.AbstractRemoteClient;
import step.client.credentials.ControllerCredentials;
import ch.exense.commons.core.collections.Collection;
import ch.exense.commons.core.collections.CollectionFactory;

import java.io.File;
import java.io.IOException;
import ch.exense.commons.app.Configuration;

public class RemoteCollectionFactory implements CollectionFactory {

    private AbstractRemoteClient client;
    private static String REMOTE_URL_PROP = "db.remote.url";
    private static String REMOTE_USER_PROP = "db.remote.user";
    private static String REMOTE_PWD_PROP = "db.remote.pwd";

    public RemoteCollectionFactory(Configuration configuration) {
        this(new ControllerCredentials(
                configuration.getProperty(RemoteCollectionFactory.REMOTE_URL_PROP),
                configuration.getProperty(RemoteCollectionFactory.REMOTE_USER_PROP),
                configuration.getProperty(RemoteCollectionFactory.REMOTE_PWD_PROP)
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
}
