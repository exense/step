package step.core.collections;

import org.junit.Test;

import java.io.File;

public class AutomationPackageWithNonexistentFragmentsTest extends AutomationPackageCollectionTestBase {

    public AutomationPackageWithNonexistentFragmentsTest() {
        super.sourceDirectory = new File("src/test/resources/testdata/ap-with-nonexistent-fragments");
    }

    @Test
    public void testLoading() {
        // we're happy if this package managed to load without throwing an exception
    }
}
