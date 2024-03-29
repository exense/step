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

import step.core.yaml.YamlFields;
import com.google.common.base.CaseFormat;

public class YamlPlanFields extends YamlFields {

    public static final String CALL_FUNCTION_ARGUMENT_ORIGINAL_FIELD = "argument";
    public static final String CALL_FUNCTION_ARGUMENT_YAML_FIELD = "inputs";

    public static final String CALL_FUNCTION_FUNCTION_ORIGINAL_FIELD = "function";
    public static final String CALL_FUNCTION_FUNCTION_YAML_FIELD = "keyword";

    public static final String TOKEN_SELECTOR_TOKEN_ORIGINAL_FIELD = "token";
    public static final String TOKEN_SELECTOR_TOKEN_YAML_FIELD = "routing";

    public static final String CHECK_EXPRESSION_ORIGINAL_FIELD = "expression";

    public static final String NAME_YAML_FIELD = "nodeName";

    public static final String ARTEFACT_CHILDREN = "children";

    public static final String DATA_SOURCE_TYPE_ORIGINAL_FIELD = "dataSourceType";

    public static final String DATA_SOURCE_ORIGINAL_FIELD = "dataSource";

    public static final String DATA_SOURCE_YAML_FIELD = "dataSource";

    public static final String FOR_BLOCK_DATA_START_YAML_FIELD = "start";
    public static final String FOR_BLOCK_DATA_START_ORIGINAL_FIELD = "start";

    public static final String FOR_BLOCK_DATA_END_YAML_FIELD = "end";
    public static final String FOR_BLOCK_DATA_END_ORIGINAL_FIELD = "end";

    public static final String FOR_BLOCK_DATA_INC_YAML_FIELD = "inc";
    public static final String FOR_BLOCK_DATA_INC_ORIGINAL_FIELD = "inc";

    public static final String FILE_REFERENCE_RESOURCE_ID_FIELD = "id";

    public static final String PERFORMANCE_ASSERT_FILTERS_ORIGINAL_FIELD = "filters";
    public static final String PERFORMANCE_ASSERT_FILTERS_YAML_FIELD = "measurementName";

    public static String javaArtefactNameToYaml(String javaArtefactName) {
        return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, javaArtefactName);
    }

    public static String yamlArtefactNameToJava(String yamlArtefactName) {
        return CaseFormat.LOWER_CAMEL.to(CaseFormat.UPPER_CAMEL, yamlArtefactName);
    }

}
