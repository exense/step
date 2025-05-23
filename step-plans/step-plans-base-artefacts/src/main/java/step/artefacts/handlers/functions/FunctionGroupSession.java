/*
 * Copyright (C) 2024, exense GmbH
 *
 * This file is part of Step
 *
 * Step is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Step is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Step.  If not, see <http://www.gnu.org/licenses/>.
 */

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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This class implements the underlying logic of Session artefacts
 * It caches reserved tokens and reuse them for subsequent executions whenever it is possible
 */
public class FunctionGroupSession implements AutoCloseable {

    private final long ownerThreadId;
    private final FunctionExecutionService functionExecutionService;
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
        return getRemoteToken(Map.of(), tokenSelectionCriteria, tokenWrapperOwner, true, false);
    }

    public synchronized TokenWrapper getRemoteToken(Map<String, String> ownAttributes, Map<String, Interest> tokenSelectionCriteria, TokenWrapperOwner tokenWrapperOwner, boolean createRemoteSession, boolean skipAutoProvisioning) throws FunctionExecutionServiceException {
        if (!isCurrentThreadOwner()) {
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
            token = selectToken(ownAttributes, tokenSelectionCriteria, tokenWrapperOwner, createRemoteSession, skipAutoProvisioning);
            tokens.add(token);
        }
        return token;
    }

    private boolean isCurrentThreadOwner() {
        return Thread.currentThread().getId() == ownerThreadId;
    }

    private TokenWrapper selectToken(Map<String, String> ownAttributes, Map<String, Interest> selectionCriteria, TokenWrapperOwner tokenWrapperOwner, boolean createRemoteSession, boolean skipAutoProvisioning) throws FunctionExecutionServiceException {

        TokenWrapper token;
        OperationManager.getInstance().enter("Token selection", selectionCriteria);
        try {
            token = functionExecutionService.getTokenHandle(ownAttributes, selectionCriteria, createRemoteSession, tokenWrapperOwner, skipAutoProvisioning);
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
        tokens.forEach(t -> {
            try {
                functionExecutionService.returnTokenHandle(t.getID());
            } catch (FunctionExecutionServiceException e) {
                releaseExceptions.add(e);
            }
        });
        tokens.clear();
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
                        releaseExceptions.stream().map(Throwable::getMessage).collect(Collectors.joining(", ")));
            }
        }
    }
}
