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
 * Transforms the token selection criteria of a keyword call before they are used to select a token.
 * <p>
 * An implementation registered in the execution context under this interface is applied by
 * {@link TokenSelectionCriteriaMapBuilder}, and thus to both the token forecasting and the actual token selection,
 * which are the two consumers of these criteria.
 * <p>
 * This exists for the executions whose agents do not come from a real infrastructure, and for which routing criteria
 * are therefore meaningless: a local execution runs everything on the machine of the user, where the only decision
 * left is which of the started agent processes runs the keyword.
 */
public interface TokenSelectionCriteriaFilter {

    /**
     * @param selectionCriteria the criteria defined for a keyword call
     * @return the criteria to select a token with. Implementations must not modify the map they are given.
     */
    Map<String, Interest> filter(Map<String, Interest> selectionCriteria);
}
