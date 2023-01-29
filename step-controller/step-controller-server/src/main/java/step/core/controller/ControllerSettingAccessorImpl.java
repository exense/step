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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.accessors.AbstractAccessor;
import step.core.collections.Collection;
import step.core.collections.Filters;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class ControllerSettingAccessorImpl extends AbstractAccessor<ControllerSetting> implements ControllerSettingAccessor {

	private static final Logger log = LoggerFactory.getLogger(ControllerSettingAccessorImpl.class);

	private final Map<String, List<ControllerSettingHook>> hooksMap = new ConcurrentHashMap<>();

	public ControllerSettingAccessorImpl(Collection<ControllerSetting> collectionDriver) {
		super(collectionDriver);
	}

	public ControllerSetting getSettingByKey(String key) {
		return collectionDriver.find(Filters.equals("key", key), null, null, null, 0).findFirst().orElse(null);
	}

	@Override
	public void addHook(String key, ControllerSettingHook hook) {
		this.hooksMap.computeIfAbsent(key, k -> new ArrayList<>());
		List<ControllerSettingHook> list = this.hooksMap.get(key);
		list.add(hook);
	}

	@Override
	public boolean removeHook(String key, ControllerSettingHook hook) {
		List<ControllerSettingHook> hooks = getHooksBySettingKey(key);
		if(hooks != null){
			return hooks.remove(hook);
		} else {
			return false;
		}
	}

	@Override
	public ControllerSetting save(ControllerSetting entity) {
		// we can change the key of existing setting - in this case we notify hooks about deleted/created setting
		ControllerSetting oldValueWithChangedKey = getOldValueWithAnotherKey(entity);
		ControllerSetting res = super.save(entity);

		callHookForChangedKey(oldValueWithChangedKey);

		List<ControllerSettingHook> hooks = getHooksBySettingKey(entity.getKey());
		if (hooks != null) {
			for (ControllerSettingHook hook : hooks) {
				callHookOnSettingSaveSafely(res, hook);
			}
		}

		return res;
	}

	private ControllerSetting getOldValueWithAnotherKey(ControllerSetting newValue){
		ControllerSetting oldValue = null;
		if (newValue.getId() != null) {
			oldValue = get(newValue.getId());
			if (oldValue != null && !Objects.equals(oldValue.getKey(), newValue.getKey())) {
				return oldValue;
			}
		}
		return null;
	}

	@Override
	public void save(Iterable<ControllerSetting> entities) {
		List<ControllerSetting> oldValuesWithChangedKeys = new ArrayList<>();
		for (ControllerSetting newValue : entities) {
			ControllerSetting valueWithChangedKey = getOldValueWithAnotherKey(newValue);
			if (valueWithChangedKey != null) {
				oldValuesWithChangedKeys.add(valueWithChangedKey);
			}
		}

		super.save(entities);

		for (ControllerSetting entity : entities) {
			ControllerSetting oldValueWithChangedKey = oldValuesWithChangedKeys.stream().filter(v -> Objects.equals(v.getId(), entity.getId())).findFirst().orElse(null);

			callHookForChangedKey(oldValueWithChangedKey);

			List<ControllerSettingHook> hooks = getHooksBySettingKey(entity.getKey());
			if (hooks != null) {
				for (ControllerSettingHook hook : hooks) {
					callHookOnSettingSaveSafely(entity, hook);
				}
			}
		}
	}

	@Override
	public void remove(ObjectId id) {
		ControllerSetting toBeDeleted = get(id);
		super.remove(id);
		List<ControllerSettingHook> hooks = getHooksBySettingKey(toBeDeleted.getKey());

		if (hooks != null) {
			for (ControllerSettingHook hook : hooks) {
				callHookOnSettingRemoveSafely(toBeDeleted, hook);
			}
		}
	}

	/**
	 * Calls the onSettingRemove hooks when key is changed in some controller setting (the value with old key is handled as removed)
	 */
	protected void callHookForChangedKey(ControllerSetting oldValueWithChangedKey) {
		if (oldValueWithChangedKey != null) {
			List<ControllerSettingHook> hooksOnDelete = getHooksBySettingKey(oldValueWithChangedKey.getKey());
			if (hooksOnDelete != null) {
				for (ControllerSettingHook hook : hooksOnDelete) {
					callHookOnSettingRemoveSafely(oldValueWithChangedKey, hook);
				}
			}
		}
	}

	protected void callHookOnSettingRemoveSafely(ControllerSetting deletedSetting, ControllerSettingHook hook) {
		try {
			hook.onSettingRemove(deletedSetting.getId(), deletedSetting);
		} catch (Exception ex) {
			// we just catch exception and log, because we want all other hooks to be called
			log.error("Controller setting hook error", ex);
		}
	}

	protected void callHookOnSettingSaveSafely(ControllerSetting res, ControllerSettingHook hook) {
		try {
			hook.onSettingSave(res);
		} catch (Exception ex){
			// we just catch exception and log, because we want all other hooks to be called
			log.error("Controller setting hook error", ex);
		}
	}

	protected List<ControllerSettingHook> getHooksBySettingKey(String settingKey){
		return this.hooksMap.get(settingKey);
	}

	protected Map<String, List<ControllerSettingHook>> getHooksMap() {
		return hooksMap;
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
