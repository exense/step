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

import step.core.artefacts.Artefact;
import step.core.dynamicbeans.DynamicValue;

import static step.artefacts.ForBlock.FOR_BLOCK_ARTIFACT_NAME;

@Artefact(name = FOR_BLOCK_ARTIFACT_NAME)
public class ForBlock extends AbstractForBlock {

	public static final String FOR_BLOCK_ARTIFACT_NAME = "For";
	public static final String DATA_SOURCE_TYPE = "sequence";

	public ForBlock() {
		super();
		this.setDataSourceType(DATA_SOURCE_TYPE);
		this.setItem(new DynamicValue<String>("counter"));
		this.setThreads(new DynamicValue<Integer>(1));
	}
		
}
