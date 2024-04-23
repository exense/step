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
package step.core.yaml;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target(ElementType.TYPE)
@Retention(RUNTIME)
public @interface YamlModel {

    String LOCATION = "step";

    /**
     * The reference to the special model class. If not defined, the current class itself will be used as the model
     * class for yaml representation
     */
    Class<?> model() default None.class;

    /**
     * True for yaml models having the following representation in yaml
     * <pre>{@code
     * name:
     *    fieldA: valueA
     *    fieldA: valueB
     *    ...
     * }</pre>
     */
    boolean named() default true;

    /**
     * For named yaml models specifies the custom name. If not specified, the class name will be used
     */
    String name() default "";

    /**
     * Void class to be used in annotations instead of null-values
     */
    final class None {
    }
}
