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
package step.plans.parser.yaml.schema;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.handlers.javahandler.jsonschema.JsonSchemaPreparationException;
import step.plans.parser.yaml.model.YamlPlanVersions;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class YamlPlanSchemaGenerationTool {

    private static final Logger log = LoggerFactory.getLogger(YamlPlanSchemaGenerationTool.class);

    public static void main(String[] args) {
        String outputFilePath = null;

        log.info("--- Generating the json schema for Step plans (yaml)...");

        if (args.length < 1 || args[0] == null || args[0].isEmpty()) {
            log.error("--- Output file is not specified");
            return;
        }
        outputFilePath = args[0];

        try {
            YamlPlanJsonSchemaGenerator schemaGenerator = new YamlPlanJsonSchemaGenerator("step", YamlPlanVersions.ACTUAL_VERSION.getVersion());
            JsonNode currentSchema = schemaGenerator.generateJsonSchema();

            try (FileOutputStream fileOs = new FileOutputStream(outputFilePath)) {
                fileOs.write(currentSchema.toPrettyString().getBytes());
            } catch (FileNotFoundException ex) {
                log.error("--- File not found - " + outputFilePath);
                return;
            } catch (IOException e) {
                log.error("--- Cannot save the schema to " + outputFilePath, e);
                return;
            }

            log.info("--- SUCCESS: the json schema has been saved to " + outputFilePath);
        } catch (JsonSchemaPreparationException e) {
            log.error("--- The schema hasn't been generated", e);
        }

    }
}
