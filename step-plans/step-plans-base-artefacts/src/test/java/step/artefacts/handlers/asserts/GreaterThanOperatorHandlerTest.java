package step.artefacts.handlers.asserts;

import org.junit.Assert;
import org.junit.Test;

public class GreaterThanOperatorHandlerTest {

    @Test
    public void testInputTypesWithStringExpected(){
        GreaterThanOperatorHandler handler = new GreaterThanOperatorHandler();
        AssertResult result;

        result = handler.apply("testKey", 779, "778", false);
        Assert.assertTrue(result.isPassed());

        result = handler.apply("testKey", 779L, "778", false);
        Assert.assertTrue(result.isPassed());

        result = handler.apply("testKey", Double.parseDouble("779.77"), "778", false);
        Assert.assertTrue(result.isPassed());

        result = handler.apply("testKey", Float.parseFloat("779.77"), "778", false);
        Assert.assertTrue(result.isPassed());
    }

    @Test
    public void testInputTypesWithNumericExpected(){
        GreaterThanOperatorHandler handler = new GreaterThanOperatorHandler();
        AssertResult result;

        result = handler.apply("testKey", 779, 778, false);
        Assert.assertTrue(result.isPassed());

        result = handler.apply("testKey", 779L, 778L, false);
        Assert.assertTrue(result.isPassed());

        result = handler.apply("testKey", Double.parseDouble("779.77"), 778.00, false);
        Assert.assertTrue(result.isPassed());

        result = handler.apply("testKey", Float.parseFloat("779.77"), 778.00F, false);
        Assert.assertTrue(result.isPassed());
    }
}