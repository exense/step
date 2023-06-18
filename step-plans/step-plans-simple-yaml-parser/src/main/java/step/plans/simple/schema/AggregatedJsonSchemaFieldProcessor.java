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
package step.plans.simple.schema;

import jakarta.json.JsonObjectBuilder;
import step.handlers.javahandler.jsonschema.JsonSchemaFieldProcessor;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Implements the logic to extract json schema for some field in java-class
 */
public class AggregatedJsonSchemaFieldProcessor implements JsonSchemaFieldProcessor {

	private List<FilterRule> filterRules;
	private List<ProcessingRule> processingRules;

	public AggregatedJsonSchemaFieldProcessor(List<FilterRule> filterRules, List<ProcessingRule> processingRules) {
		this.filterRules = filterRules;
		this.processingRules = processingRules;
	}

	@Override
	public boolean applyCustomProcessing(Class<?> objectClass, Field field, JsonObjectBuilder propertiesBuilder) throws JsonSchemaFieldProcessingException {
		for (ProcessingRule processingRule : processingRules) {
			if(processingRule.applyCustomProcessing(objectClass, field, propertiesBuilder)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean skipField(Class<?> objectClass, Field field) {
		for (FilterRule filterRule : filterRules) {
			if(filterRule.skipField(objectClass, field)){
				return true;
			}
		}
		return false;
	}

	public interface ProcessingRule {
		boolean applyCustomProcessing(Class<?> objectClass, Field field, JsonObjectBuilder propertiesBuilder) throws JsonSchemaFieldProcessingException;
	}

	public interface FilterRule {
		boolean skipField(Class<?> objectClass, Field field);
	}
}
