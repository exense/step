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
package step.automation.packages.yaml.rules;

import step.automation.packages.AutomationPackageAttributesApplyingContext;
import step.automation.packages.yaml.deserialization.SpecialKeywordAttributesApplier;
import step.automation.packages.yaml.deserialization.SpecialKeywordAttributesExtractor;

public interface YamlKeywordConversionRule extends YamlConversionRule {

    /**
     * Returns the special attributes extract used to extract some special data, which can't be applied during the
     * deserialization from yaml (for example, if some data should be stored in database before to the {@link step.functions.Function} object)
     */
    default SpecialKeywordAttributesExtractor getSpecialAttributesExtractor() {
        return null;
    }

    /**
     * Returns the applier for the special attributes extracted by {@link SpecialKeywordAttributesExtractor}.
     */
    default SpecialKeywordAttributesApplier getSpecialKeywordAttributesApplier(AutomationPackageAttributesApplyingContext context) {
        return null;
    }

}
