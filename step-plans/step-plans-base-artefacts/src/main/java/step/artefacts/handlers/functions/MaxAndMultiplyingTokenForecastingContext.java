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

import step.grid.tokenpool.Interest;

import java.util.*;

/**
 * This implementation of the {@link TokenForecastingContext} tries to calculate the
 * required number of tokens for Artefacts that run their children in parallel with a defined number of threads.
 * For this estimation it calculates the maximum number of token required for each available pool and for each child artefact.
 * For each pool it then takes the sum of the required number of tokens for the n first children where n is the number of threads
 */
public class MaxAndMultiplyingTokenForecastingContext extends TokenForecastingContext {

    private final int numberOfThreads;

    private final List<TokenForecastingContext> iterations = new ArrayList<>();
    private final Set<Key> keys = new HashSet<>();
    private TokenForecastingContext currentIteration;

    public MaxAndMultiplyingTokenForecastingContext(TokenForecastingContext parentContext, int numberOfThreads) {
        super(parentContext);
        this.numberOfThreads = numberOfThreads;
        currentIteration = new TokenForecastingContext(parentContext);
    }

    public void nextIteration() {
        iterations.add(currentIteration);
        currentIteration = new TokenForecastingContext(parentContext);
    }

    @Override
    public Key requireToken(Map<String, Interest> criteria, int count) throws NoMatchingTokenPoolException {
        Key key = currentIteration.requireToken(criteria, count);
        keys.add(key);
        return key;
    }

    @Override
    public void releaseRequiredToken(Key key, int count) {
        currentIteration.releaseRequiredToken(key, count);
    }

    public void end() {
        keys.forEach(key -> {
            Integer sum = iterations.stream().map(i -> i.getTokenForecastPerKey().get(key)).filter(Objects::nonNull).sorted(Comparator.reverseOrder()).limit(numberOfThreads).reduce(0, Integer::sum);
            if(sum > 0) {
                parentContext.requireToken(key, sum);
                parentContext.releaseRequiredToken(key, sum);
            }
        });
    }
}
