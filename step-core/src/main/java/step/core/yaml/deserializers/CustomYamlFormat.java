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
package step.core.yaml.deserializers;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Used to mark some class or interface as having the special yaml format in step. This allows to customize jackson
 * deserializers and redefine the default deserialization behavior for inherited classes
 * (for instance, if the @{@link com.fasterxml.jackson.annotation.JsonTypeInfo} annotation is used to specify
 * the java type for json representation, but for yaml it is required to use another field
 * to specify the java type).
 */
@Retention(RUNTIME)
@Target(ElementType.TYPE)
public @interface CustomYamlFormat {
}
