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

import java.util.NoSuchElementException;

import step.core.accessors.AbstractOrganizableObject;
import step.framework.server.Session;
import step.framework.server.access.AccessManager;

public class AccessManagerImpl implements AccessManager {

	private final RoleProvider roleProvider;
	private RoleResolver roleResolver;
	
	public AccessManagerImpl(RoleProvider roleProvider, RoleResolver roleResolver) {
		super();
		this.roleProvider = roleProvider;
		this.roleResolver = roleResolver;
	}

	@Override
	public void setRoleResolver(RoleResolver roleResolver) {
		this.roleResolver = roleResolver;
	}

	@Override
	public boolean checkRightInContext(Session session, String right) {
		Role role = getRoleInContext(session);
		return role.getRights().contains(right);
	}

	@Override
	public Role getRoleInContext(Session session) {
		String roleName = roleResolver.getRoleInContext(session);
		try {
			Role role = roleProvider.getRoles().stream().filter(r->roleName.equals(r.getName())).findFirst().get();
			return role;
		} catch (NoSuchElementException e) {
			throw new RuntimeException("The role "+roleName+" doesn't exist");
		}
	}
}
