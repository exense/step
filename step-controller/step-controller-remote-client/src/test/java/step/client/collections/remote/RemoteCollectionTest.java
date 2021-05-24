package step.client.collections.remote;

import org.junit.Ignore;
import step.client.credentials.ControllerCredentials;
import ch.exense.commons.core.collections.AbstractCollectionTest;

@Ignore
public class RemoteCollectionTest extends AbstractCollectionTest {
    public RemoteCollectionTest() {
        super(new RemoteCollectionFactory());
    }

    public RemoteCollectionTest(ControllerCredentials credentials) {
        super(new RemoteCollectionFactory(credentials));
    }


}
