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
package step.plugins.screentemplating;

import step.core.accessors.AbstractOrganizableObject;
import step.core.objectenricher.EnricheableObject;

public class ScreenInput extends AbstractOrganizableObject implements EnricheableObject {

	protected String screenId;
	
	protected int position;
	
	protected Input input;
	//Checked in screen services, and set to true for attributes.name inputs
	protected Boolean immutable;

	public ScreenInput() {
		super();
	}

	public ScreenInput(int position, String screenId, Input input) {
		this(position, screenId, input, false);
	}

	public ScreenInput(int position, String screenId, Input input, Boolean immutable) {
		super();
		this.position = position;
		this.screenId = screenId;
		this.input = input;
		this.immutable = immutable;
	}
	
	public ScreenInput(String screenId, Input input) {
		super();
		this.screenId = screenId;
		this.input = input;
	}

	public String getScreenId() {
		return screenId;
	}

	public void setScreenId(String screenId) {
		this.screenId = screenId;
	}

	public int getPosition() {
		return position;
	}

	public void setPosition(int position) {
		this.position = position;
	}

	public Input getInput() {
		return input;
	}

	public void setInput(Input input) {
		this.input = input;
	}

	public Boolean getImmutable() {
		return immutable != null && immutable;
	}

	public void setImmutable(Boolean immutable) {
		this.immutable = immutable;
	}
}
