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
package step.repositories.staging;

import java.util.ArrayList;
import java.util.List;

import ch.exense.commons.core.model.accessors.AbstractIdentifiableObject;
import step.core.plans.Plan;

public class StagingContext extends AbstractIdentifiableObject {

	protected List<String> attachments = new ArrayList<>();
	protected Plan plan;

	public Plan getPlan() {
		return plan;
	}

	public void setPlan(Plan plan) {
		this.plan = plan;
	}

	public List<String> getAttachments() {
		return attachments;
	}
	
	public boolean addAttachment(String e) {
		return attachments.add(e);
	}
	
	public void setAttachments(List<String> attachments) {
		this.attachments = attachments;
	}

}
