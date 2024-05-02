package step.artefacts.handlers.functions;

import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import step.functions.execution.FunctionExecutionService;
import step.functions.execution.FunctionExecutionServiceException;
import step.grid.Token;
import step.grid.TokenWrapper;
import step.grid.tokenpool.Interest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class FunctionGroupSessionTest {

    @Test
    public void test() throws Exception {
        AtomicInteger callCount = new AtomicInteger();
        FunctionExecutionService functionExecutionService = Mockito.mock(FunctionExecutionService.class);
        Mockito.when(functionExecutionService.getTokenHandle(Mockito.any(), Mockito.any(), Mockito.anyBoolean(), Mockito.any())).thenAnswer(invocationOnMock -> {
            callCount.incrementAndGet();
            Object argument = invocationOnMock.getArgument(1);
            TokenWrapper tokenWrapper = new TokenWrapper();
            Token token = new Token();
            token.setId("token"+callCount.get());
            token.setAttributes(((Map<String, Interest>) argument).entrySet().stream().collect(Collectors.toMap(o -> o.getKey(), o->o.getValue().getSelectionPattern().pattern())));
            tokenWrapper.setToken(token);
            return tokenWrapper;
        });

        Mockito.when(functionExecutionService.getLocalTokenHandle()).thenAnswer(invocationOnMock->{
            TokenWrapper tokenWrapper = new TokenWrapper();
            Token token = new Token();
            token.setId("local");
            tokenWrapper.setToken(token);
            return tokenWrapper;
        });

        FunctionGroupSession functionGroupSession = new FunctionGroupSession(functionExecutionService);

        // Get a token with some criteria and ensure that the token is selected from the functionExecutionService
        TokenWrapper remoteToken = functionGroupSession.getRemoteToken(Map.of("attribute1", new Interest(Pattern.compile("value1"), true)), null);
        assertNotNull(remoteToken);
        assertEquals(1, callCount.get());

        // Get a token with the same criteria and ensure that the same token is returned
        remoteToken = functionGroupSession.getRemoteToken(Map.of("attribute1", new Interest(Pattern.compile("value1"), true)), null);
        assertNotNull(remoteToken);
        assertEquals(1, callCount.get());

        // Get a token with different criteria. A new token should be selected
        remoteToken = functionGroupSession.getRemoteToken(Map.of("attribute2", new Interest(Pattern.compile("value2"), true)), null);
        assertNotNull(remoteToken);
        assertEquals(2, callCount.get());

        TokenWrapper localToken = functionGroupSession.getLocalToken();
        assertNotNull(localToken);
        assertEquals("local", localToken.getID());

        // Close the session and ensure that all tokens are released
        ArrayList<String> releasedTokens = new ArrayList<>();
        Mockito.doAnswer(invocationOnMock -> releasedTokens.add(invocationOnMock.getArgument(0))).when(functionExecutionService).returnTokenHandle(Mockito.anyString());
        functionGroupSession.close();
        // Ensure that both token have been released
        assertEquals(List.of("token1", "token2", "local"), releasedTokens);
    }
}