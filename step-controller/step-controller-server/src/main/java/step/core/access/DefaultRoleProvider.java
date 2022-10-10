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
package step.core.access;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import step.core.accessors.AbstractOrganizableObject;

public class DefaultRoleProvider implements RoleProvider {

	public static Role DEFAULT_ROLE;
	private static List<Role> DEFAULT_ROLES = new ArrayList<Role>();

	{
		DEFAULT_ROLE = new Role();
		DEFAULT_ROLE.addAttribute(AbstractOrganizableObject.NAME, "admin");
		DEFAULT_ROLE.setRights(Arrays.asList(new String[]{"plan-read","plan-write","plan-delete","plan-execute","kw-read","kw-write","kw-delete","kw-execute","execution-read","execution-write","execution-delete","user-write","user-read","task-read","task-write","task-delete","scheduler-manage","operations-read","controller-manage","maintenance-message-write","settings-read","settings-write","settings-delete","admin-ui-menu","param-read","param-write","param-delete","param-global-write","resource-read","resource-write","resource-delete","interactive","token-manage","screenInputs-read","screenInputs-write","screenInputs-delete","collection-read","collection-write","collection-delete"}));
		
		DEFAULT_ROLES.add(DEFAULT_ROLE);
	}

	public DefaultRoleProvider() {
		super();
	}

	@Override
	public List<Role> getRoles() {
		return DEFAULT_ROLES;
	}
}
