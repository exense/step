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

import com.fasterxml.jackson.annotation.JsonIgnore;
import step.artefacts.automation.YamlFunctionGroup;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.Artefact;
import step.core.artefacts.reports.ReportNode;
import step.core.yaml.YamlModel;

import java.util.function.BiConsumer;

import static step.artefacts.FunctionGroup.FUNCTION_GROUP_ARTEFACT_NAME;

@YamlModel(model = YamlFunctionGroup.class, name = "session")
@Artefact(name=FUNCTION_GROUP_ARTEFACT_NAME)
public class FunctionGroup extends TokenSelector {

	public static final String FUNCTION_GROUP_ARTEFACT_NAME = "Session";

	@JsonIgnore
	private BiConsumer<AbstractArtefact, ReportNode> consumer;

	/**
	 * @return an optional {@link BiConsumer} representing an operation to be performed
	 * inside the {@link FunctionGroup}
	 */
	public BiConsumer<AbstractArtefact, ReportNode> getConsumer() {
		return consumer;
	}

	public void setConsumer(BiConsumer<AbstractArtefact, ReportNode> consumer) {
		this.consumer = consumer;
	}

}
