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
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.codec.digest.DigestUtils;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.accessors.AbstractAccessor;
import step.core.collections.Collection;
import step.core.collections.Filters;
import step.core.collections.filters.Equals;

public class UserAccessorImpl extends AbstractAccessor<User> implements UserAccessor {

	private static final Logger logger = LoggerFactory.getLogger(UserAccessorImpl.class);
	private final List<Function<User, Void>> saveHooks = new ArrayList<>();
	private final List<Function<User, Void>> removeHooks = new ArrayList<>();

	public UserAccessorImpl(Collection<User> collectionDriver) {
		super(collectionDriver);
	}

	@Override
	public void remove(String username) {
		User user = this.getByUsername(username);
		removeUser(user);
	}

	@Override
	public void remove(ObjectId id) {
		User user = this.get(id);
		removeUser(user);
	}

	private void removeUser(User user){
		super.remove(user.getId());
		removeHooks.forEach(h -> {
			try {
				h.apply(user);
			} catch (Exception e) {
				logger.error("Hook execution while removing user failed.", e);
			}
		});
	}

	@Override
	public User save(User user) {
		//Hooks are executed before saving and will stop on error (required for licensing)
		saveHooks.forEach( h -> h.apply(user));
		return super.save(user);
	}

	@Override
	public void save(Iterable<User> entities) {
		// Not used anywhere
		throw new UnsupportedOperationException();
	}

	@Override
	public List<User> getAllUsers() {
		return collectionDriver.find(Filters.empty(), null, null, null, 0).collect(Collectors.toList());
	}
	
	@Override
	public User getByUsername(String username) {
		assert username != null;
		return collectionDriver.find(byName(username), null, null, null, 0).findFirst().orElse(null);
	}

	@Override
	public void registerOnSaveHook(Function<User, Void> f) {
		saveHooks.add(f);
	}

	@Override
	public void registerOnRemoveHook(Function<User, Void> f) {
		removeHooks.add(f);
	}

	private Equals byName(String username) {
		return Filters.equals("username", username);
	}
	
	public static String encryptPwd(String pwd) {
		return DigestUtils.sha512Hex(pwd);
	}

}
