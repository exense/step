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
package step.plans.parser.yaml;

/**
 * Allows to extend the logic of working with Yaml plans (for Step EE). To do that, there should be a class implementing the {@link YamlPlanReaderExtender}
 * and annotated with {@link YamlPlanReaderExtension}.
 * The following functionality can be extended via this class:
 * - the used json schema (for example, to switch to extended json schema in Step EE)
 */
public interface YamlPlanReaderExtender {

    /**
     * Allows to redefine the json schema (for instance, switch used json schema to extended one for Step EE).
     * There should be at max one {@link YamlPlanReaderExtender} annotated with {@link YamlPlanReaderExtension} overriding
     * the json schema (returning the non-null string via this method).
     */
    default String getJsonSchemaPath() {
        return null;
    }

}
