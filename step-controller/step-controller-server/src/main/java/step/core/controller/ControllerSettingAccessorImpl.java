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
package step.core.controller;

import org.bson.types.ObjectId;
import step.core.accessors.AbstractAccessor;
import step.core.collections.Collection;
import step.core.collections.Filters;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ControllerSettingAccessorImpl extends AbstractAccessor<ControllerSetting> implements ControllerSettingAccessor {

	private final List<ControllerSettingHook> hooks = new ArrayList<>();

	private final Set<ControllerSettingHook> calledHooks = new HashSet<>();

	public ControllerSettingAccessorImpl(Collection<ControllerSetting> collectionDriver) {
		super(collectionDriver);
	}

	public ControllerSetting getSettingByKey(String key) {
		return collectionDriver.find(Filters.equals("key", key), null, null, null, 0).findFirst().orElse(null);
	}

	@Override
	public void addHook(ControllerSettingHook hook) {
		this.hooks.add(hook);
	}

	@Override
	public List<ControllerSettingHook> getHooks() {
		return hooks;
	}

	@Override
	public ControllerSetting save(ControllerSetting entity) {
		ControllerSetting res = super.save(entity);
		for (ControllerSettingHook hook : hooks) {
			hook.onSettingSave(res);

			// the trick for the save(Iterable<ControllerSetting> entities) method - we notify that the hook was called to avoid calling twice
			calledHooks.add(hook);
		}
		return res;
	}

	@Override
	public void save(Iterable<ControllerSetting> entities) {
		synchronized (calledHooks) {
			calledHooks.clear();

			super.save(entities);

			// some collections (PostgreSQLCollection) don't call the single save() method (they insert batch in DB instead)
			// so we need a trick to check if our hooks are already called or not (to avoid calling them twice)
			for (ControllerSettingHook hook : hooks) {
				if (!calledHooks.contains(hook)) {
					for (ControllerSetting entity : entities) {
						hook.onSettingSave(entity);
					}
				}
			}

			calledHooks.clear();
		}
	}

	@Override
	public void remove(ObjectId id) {
		ControllerSetting toBeDeleted = get(id);
		super.remove(id);
		for (ControllerSettingHook hook : hooks) {
			hook.onSettingRemove(id, toBeDeleted);
		}
	}

	// TODO: the following methods should be moved to a ControllerSettingManager.
	// They actually don't belong to an accessor which role should be limited to
	// retrieval and persistence of data
	public ControllerSetting updateOrCreateSetting(String key, String value) {
		ControllerSetting setting = getOrCreateSettingByKey(key);
		setting.setValue(value);
		return save(setting);
	}

	public ControllerSetting createSettingIfNotExisting(String key, String value) {
		ControllerSetting schedulerEnabled = getSettingByKey(key);
		if (schedulerEnabled == null) {
			return save(new ControllerSetting(key, value));
		} else {
			return schedulerEnabled;
		}
	}

	public boolean getSettingAsBoolean(String key) {
		ControllerSetting setting = getSettingByKey(key);
		return setting != null ? Boolean.valueOf(setting.getValue()) : false;
	}
	
	private ControllerSetting getOrCreateSettingByKey(String key) {
		ControllerSetting setting = getSettingByKey(key);
		if(setting == null) {
			setting = new ControllerSetting();
			setting.setKey(key);
		}
		return setting;
	}
	// End of TODO

}
