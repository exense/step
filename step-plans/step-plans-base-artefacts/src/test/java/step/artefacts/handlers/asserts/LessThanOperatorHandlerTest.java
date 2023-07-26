package step.artefacts.handlers.asserts;

import org.junit.Assert;
import org.junit.Test;

public class LessThanOperatorHandlerTest {

    @Test
    public void testInputTypesWithStringExpected(){
        LessThanOperatorHandler handler = new LessThanOperatorHandler();
        AssertResult result;

        result = handler.apply("testKey", 777, "778", false);
        Assert.assertTrue(result.isPassed());

        result = handler.apply("testKey", 777L, "778", false);
        Assert.assertTrue(result.isPassed());

        result = handler.apply("testKey", Double.parseDouble("777.77"), "778", false);
        Assert.assertTrue(result.isPassed());

        result = handler.apply("testKey", Float.parseFloat("777.77"), "778", false);
        Assert.assertTrue(result.isPassed());
    }

    @Test
    public void testInputTypesWithNumericExpected(){
        LessThanOperatorHandler handler = new LessThanOperatorHandler();
        AssertResult result;

        result = handler.apply("testKey", 777, 778, false);
        Assert.assertTrue(result.isPassed());

        result = handler.apply("testKey", 777L, 778L, false);
        Assert.assertTrue(result.isPassed());

        result = handler.apply("testKey", Double.parseDouble("777.77"), 778.00, false);
        Assert.assertTrue(result.isPassed());

        result = handler.apply("testKey", Float.parseFloat("777.77"), 778.00F, false);
        Assert.assertTrue(result.isPassed());
    }
}