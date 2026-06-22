/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
 *
 * This file is part of STEP
 *
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package step.core.execution.notices;

import step.core.execution.model.Execution;

import java.util.List;

/**
 * Data backing the execution overview report page. For now it transitionally embeds the raw
 * {@link Execution} alongside its resolved notices, as a quick way to enrich the existing frontend.
 * In a future major version this object is expected to become a curated model exposing only the
 * data required by the overview page, rather than embedding the full {@link Execution}.
 */
public record ExecutionOverview(Execution execution, List<ResolvedExecutionNotice> resolvedNotices) {
}
