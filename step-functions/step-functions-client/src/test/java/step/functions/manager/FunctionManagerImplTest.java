package step.functions.manager;

import org.junit.Test;
import step.functions.Function;
import step.handlers.javahandler.Keyword;

import java.lang.reflect.Method;
import java.util.Map;

import static org.junit.Assert.*;

public class FunctionManagerImplTest {

    @Test
    public void applyRoutingFromAnnotation() throws NoSuchMethodException {
        Method method = FunctionManagerImplTest.class.getMethod("keywordNoRouting");
        Keyword annotation = method.getAnnotation(Keyword.class);
        Function function = new Function();
        FunctionManagerImpl.applyRoutingFromAnnotation(function, annotation);
        assertFalse(function.isExecuteLocally());
        assertNull(function.getTokenSelectionCriteria());

        method = FunctionManagerImplTest.class.getMethod("keywordRouteToController");
        annotation = method.getAnnotation(Keyword.class);
        function = new Function();
        FunctionManagerImpl.applyRoutingFromAnnotation(function, annotation);
        assertTrue(function.isExecuteLocally());
        assertNull(function.getTokenSelectionCriteria());

        method = FunctionManagerImplTest.class.getMethod("keywordRoutingCriteria");
        annotation = method.getAnnotation(Keyword.class);
        function = new Function();
        FunctionManagerImpl.applyRoutingFromAnnotation(function, annotation);
        assertFalse(function.isExecuteLocally());
        Map<String, String> expectedRoutingCriteria = Map.of("OS", "WINDOWS", "TYPE", "PLAYWRIGHT");
        assertEquals(expectedRoutingCriteria, function.getTokenSelectionCriteria());

        Method methodKeywordWrongRoutingReservedWord = FunctionManagerImplTest.class.getMethod("keywordWrongRoutingReservedWord");
        assertThrows("Invalid routing value: 'WrongController'. " +
                "If a single value is provided, it must be the reserved keyword 'controller'.", IllegalArgumentException.class, () -> FunctionManagerImpl.applyRoutingFromAnnotation(new Function(), methodKeywordWrongRoutingReservedWord.getAnnotation(Keyword.class)));

        Method methodKeywordWrongRoutingCriteria = FunctionManagerImplTest.class.getMethod("keywordWrongRoutingCriteria");
        assertThrows("Invalid routing array length: 5. " +
                "When specifying agent selection criteria as key-value pairs, " +
                "the array must contain an even number of elements (key1, value1, key2, value2, ...).", IllegalArgumentException.class, () -> FunctionManagerImpl.applyRoutingFromAnnotation(new Function(), methodKeywordWrongRoutingCriteria.getAnnotation(Keyword.class)));
    }

    @Keyword
    public void keywordNoRouting() {
    }

    @Keyword(routing = {Keyword.ROUTING_EXECUTE_ON_CONTROLLER})
    public void keywordRouteToController(){

    }

    @Keyword(routing = {"OS","WINDOWS","TYPE", "PLAYWRIGHT"})
    public void keywordRoutingCriteria(){

    }

    @Keyword(routing = {"WrongController"})
    public void keywordWrongRoutingReservedWord(){

    }

    @Keyword(routing = {"OS","WINDOWS","TYPE", "PLAYWRIGHT","NEXT_KEY"})
    public void keywordWrongRoutingCriteria(){

    }
}