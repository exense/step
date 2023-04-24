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
package step.core.plans.serialization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import step.core.accessors.DefaultJacksonMapperProvider;
import step.core.plans.Plan;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;

public class YamlPlanSerializer {

	private final ObjectMapper mapper;

	public YamlPlanSerializer() {
		YAMLFactory factory = new YAMLFactory();
		// Disable native type id to enable conversion to generic Documents
		factory.disable(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID);
		this.mapper = DefaultJacksonMapperProvider.getObjectMapper(factory);
	}

	public Plan readPlanFromYaml(InputStream inputStream, String versionId) throws IOException {
		Plan plan = mapper.readValue(inputStream, Plan.class);
		// TODO: version ID
		if (versionId != null) {
			if (plan.getCustomFields() == null) {
				plan.setCustomFields(new HashMap<>());
			}
			plan.getCustomFields().put("versionId", versionId);
		}
		return plan;
	}

	public void toFullYaml(OutputStream os, Plan plan) throws IOException {
		mapper.writeValue(os, plan);
	}
}
