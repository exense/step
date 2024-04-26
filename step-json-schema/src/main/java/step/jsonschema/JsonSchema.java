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
package step.jsonschema;

import step.handlers.javahandler.jsonschema.JsonSchemaFieldProcessor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.PARAMETER, ElementType.FIELD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonSchema {

    /**
     * Explicit reference to another element in json schema
     */
    String ref() default "";

    Class<? extends JsonSchemaFieldProcessor> customJsonSchemaProcessor() default JsonSchemaFieldProcessor.None.class;

    String defaultConstant() default "";

    String fieldName() default "";

    /**
     * The special provider for default value in json schema
     */
    Class<? extends JsonSchemaDefaultValueProvider> defaultProvider() default JsonSchemaDefaultValueProvider.None.class;

    boolean required() default false;
}
