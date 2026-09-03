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
package step.core.dynamicbeans;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a {@link DynamicValue} whose plain value must never be interpolated.
 * <p>
 * This is required for the values which aren't user facing text but containers holding a structured document,
 * typically the JSON of the keyword inputs or of the selection criteria. Their plain value is parsed and the
 * expressions of the leaves it contains are resolved afterwards, by the resolver of that document. Interpolating
 * the container itself would resolve those expressions before the document is parsed, which would both evaluate
 * them twice and corrupt the document as soon as a resolved value contains a quote, a backslash or a line break.
 * <p>
 * The annotation only disables the interpolation of plain values. Values defined as expressions are evaluated as
 * usual.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface NoStringInterpolation {

}
