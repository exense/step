package step.core.collections;

import org.junit.Test;

import java.io.File;

public class AutomationPackageWithEmptyWildcardTest extends AutomationPackageCollectionTestBase {

    public AutomationPackageWithEmptyWildcardTest() {
        super.sourceDirectory = new File("src/test/resources/testdata/ap-with-empty-wildcard");
    }

    @Test
    public void testLoading() {
        // we're happy if this package managed to load without throwing an exception
    }
}
