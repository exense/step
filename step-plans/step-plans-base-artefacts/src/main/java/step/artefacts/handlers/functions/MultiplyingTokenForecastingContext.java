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

import java.util.Map;

/**
 * This implementation of {@link TokenForecastingContext} estimates the required
 * number of tokens for artefacts that parallelize the execution of their children
 *
 * It simply delegates the calculation of the token forecasting to the children
 * and multiplies it by the number of threads
 */
public class MultiplyingTokenForecastingContext extends TokenForecastingContext {

    private final int numberOfThreads;

    public MultiplyingTokenForecastingContext(TokenForecastingContext parentContext, int numberOfThreads) {
        super(parentContext);
        this.numberOfThreads = numberOfThreads;
    }

    @Override
    public Key requireToken(Map<String, Interest> criteria, int count) throws NoMatchingTokenPoolException {
        return parentContext.requireToken(criteria, count * numberOfThreads);
    }

    @Override
    public void releaseRequiredToken(Key key, int count) {
        parentContext.releaseRequiredToken(key, count * numberOfThreads);
    }
}
