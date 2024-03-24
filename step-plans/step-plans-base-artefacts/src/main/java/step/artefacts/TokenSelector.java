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
package step.artefacts;

import step.core.artefacts.AbstractArtefact;
import step.core.dynamicbeans.DynamicValue;
import step.handlers.javahandler.jsonschema.JsonSchema;

public class TokenSelector extends AbstractArtefact {

    public static final String TOKEN_SELECTOR_TOKEN_YAML_FIELD = "routing";
    DynamicValue<Boolean> remote = new DynamicValue<Boolean>(true);

	@JsonSchema(fieldName = TOKEN_SELECTOR_TOKEN_YAML_FIELD)
	DynamicValue<String> token = new DynamicValue<>("{}");

	public DynamicValue<Boolean> getRemote() {
		return remote;
	}

	public void setRemote(DynamicValue<Boolean> remote) {
		this.remote = remote;
	}

	public DynamicValue<String> getToken() {
		return token;
	}

	public void setToken(DynamicValue<String> token) {
		this.token = token;
	}
}
