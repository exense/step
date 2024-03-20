package step.artefacts.handlers.functions;

import step.common.managedoperations.OperationManager;
import step.functions.execution.FunctionExecutionService;
import step.functions.execution.FunctionExecutionServiceException;
import step.grid.TokenPretender;
import step.grid.TokenWrapper;
import step.grid.TokenWrapperOwner;
import step.grid.tokenpool.Identity;
import step.grid.tokenpool.Interest;
import step.grid.tokenpool.SimpleAffinityEvaluator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FunctionGroupSession implements AutoCloseable {

    private final long ownerThreadId;
    protected final FunctionExecutionService functionExecutionService;
    private final SimpleAffinityEvaluator<Identity, Identity> affinityEvaluator = new SimpleAffinityEvaluator<>();

    private final List<TokenWrapper> tokens = new ArrayList<>();
    private TokenWrapper localToken;

    public FunctionGroupSession(FunctionExecutionService functionExecutionService) {
        this.functionExecutionService = functionExecutionService;
        this.ownerThreadId = Thread.currentThread().getId();
    }

    public TokenWrapper getLocalToken() {
        if (localToken == null) {
            localToken = functionExecutionService.getLocalTokenHandle();
        }
        return localToken;
    }

    public synchronized TokenWrapper getRemoteToken(Map<String, Interest> tokenSelectionCriteria, TokenWrapperOwner tokenWrapperOwner) throws FunctionExecutionServiceException {
        if (!isOwner(Thread.currentThread().getId())) {
            throw new RuntimeException("Tokens from this sesssion are already reserved by another thread. This usually means that you're spawning threads from wihtin a session control without creating new sessions for the new threads.");
        }

        // Find a token matching the selection criteria in the context
        TokenWrapper matchingToken = tokens.stream().filter(t ->
                affinityEvaluator.getAffinityScore(new TokenPretender(Map.of(), tokenSelectionCriteria), new TokenPretender(t.getAttributes(), Map.of())) > 0).findFirst().orElse(null);

        TokenWrapper token;
        if (matchingToken != null) {
            // Token already present in context => reusing it
            token = matchingToken;
        } else {
            // No token matching the selection criteria => select a new token and add it to the function group context
            token = selectToken(tokenSelectionCriteria, true, tokenWrapperOwner);
            tokens.add(token);
        }
        return token;
    }

    private boolean isOwner(long id) {
        return Thread.currentThread().getId() == ownerThreadId;
    }

    private TokenWrapper selectToken(Map<String, Interest> selectionCriteria, boolean createSession, TokenWrapperOwner tokenWrapperOwner) throws FunctionExecutionServiceException {
        Map<String, String> pretenderAttributes = new HashMap<>();

        TokenWrapper token;
        OperationManager.getInstance().enter("Token selection", selectionCriteria);
        try {
            token = functionExecutionService.getTokenHandle(pretenderAttributes, selectionCriteria, createSession, tokenWrapperOwner);
        } finally {
            OperationManager.getInstance().exit();
        }
        return token;
    }

    @Override
    public void close() throws Exception {
        releaseTokens(true);
    }

    public void releaseTokens(boolean alsoReleaseLocalTokens) throws Exception {
        List<Exception> releaseExceptions = new ArrayList<>();
        if (tokens != null) {
            tokens.forEach(t -> {
                try {
                    functionExecutionService.returnTokenHandle(t.getID());
                } catch (FunctionExecutionServiceException e) {
                    releaseExceptions.add(e);
                }
            });
            tokens.clear();
        }
        if (localToken != null && alsoReleaseLocalTokens) {
            try {
                functionExecutionService.returnTokenHandle(localToken.getID());
            } catch (FunctionExecutionServiceException e) {
                releaseExceptions.add(e);
            } finally {
                localToken = null;
            }
        }

        int exceptionCount = releaseExceptions.size();
        if (exceptionCount > 0) {
            if (exceptionCount == 1) {
                throw releaseExceptions.get(0);
            } else {
                throw new Exception("Multiple errors occurred when releasing agent tokens: " +
                        releaseExceptions.stream().map(e -> e.getMessage()).collect(Collectors.joining(", ")));
            }
        }
    }
}
