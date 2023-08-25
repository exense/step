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

import java.util.List;
import java.util.function.Function;

import step.core.accessors.Accessor;

public interface UserAccessor extends Accessor<User> {

	void remove(String username);

	List<User> getAllUsers();

	User getByUsername(String username);

	/**
	 * Register hooks to be executed before saving a User, any thrown exception will abort saving the user
	 * @param f the function to execute
	 */
	void registerOnSaveHook(Function<User, Void> f);

	/**
	 * Register hooks to be executed after removing a User, thrown exception don't stop the execution of next hooks
	 * @param f the function to execute
	 */
	void registerOnRemoveHook(Function<User, Void> f);
}
