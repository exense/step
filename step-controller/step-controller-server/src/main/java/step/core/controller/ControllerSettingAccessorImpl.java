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
		if (hooks != null) {
			return hooks.remove(hook);
		} else {
			return false;
		}
	}

	@Override
	public ControllerSetting save(ControllerSetting entity) {
		// we can change the key of existing setting - in this case we notify hooks about deleted/created setting
		ControllerSetting oldValue = getOldValue(entity);
		ControllerSetting res = super.save(entity);

		if (oldValueHasAnotherKey(oldValue, entity)) {
			callHooksForChangedKey(oldValue);
		}

		List<ControllerSettingHook> hooks = getHooksBySettingKey(entity.getKey());
		if (hooks != null) {
			try {
				for (ControllerSettingHook hook : hooks) {
					callHookOnSettingSave(res, hook, false);
				}
			} catch (Exception ex) {
				rollbackOldValue(res.getId(), oldValue, ex);
			}
		}

		return res;
	}

	private ControllerSetting getOldValue(ControllerSetting newValue) {
		if (newValue.getId() != null) {
			return get(newValue.getId());
		}
		return null;
	}

	private boolean oldValueHasAnotherKey(ControllerSetting oldValue, ControllerSetting newValue) {
		if (newValue != null) {
			if (oldValue != null) {
				return !Objects.equals(oldValue.getKey(), newValue.getKey());
			}
		}
		return false;
	}

	@Override
	public void save(Iterable<ControllerSetting> entities) {
		List<ControllerSetting> oldValues = new ArrayList<>();
		List<ControllerSetting> oldValuesWithChangedKeys = new ArrayList<>();
		for (ControllerSetting newValue : entities) {
			ControllerSetting oldValue = getOldValue(newValue);
			if (oldValue != null) {
				oldValues.add(oldValue);
				if (oldValueHasAnotherKey(oldValue, newValue)) {
					oldValuesWithChangedKeys.add(oldValue);
				}
			}
		}

		super.save(entities);

		try {
			for (ControllerSetting entity : entities) {
				ControllerSetting oldValueWithChangedKey = oldValuesWithChangedKeys.stream().filter(v -> Objects.equals(v.getId(), entity.getId())).findFirst().orElse(null);

				callHooksForChangedKey(oldValueWithChangedKey);

				List<ControllerSettingHook> hooks = getHooksBySettingKey(entity.getKey());
				if (hooks != null) {
					for (ControllerSettingHook hook : hooks) {
						callHookOnSettingSave(entity, hook, false);
					}
				}
			}
		} catch (Exception ex) {
			// rollback save on hook failure
			for (ControllerSetting entity : entities) {
				rollbackOldValue(
						entity.getId(),
						oldValues.stream().filter(v -> Objects.equals(v.getId(), entity.getId())).findFirst().orElse(null),
						ex
				);
			}
		}
	}

	@Override
	public void remove(ObjectId id) {
		ControllerSetting toBeDeleted = get(id);
		super.remove(id);
		List<ControllerSettingHook> hooks = getHooksBySettingKey(toBeDeleted.getKey());

		if (hooks != null) {
			try {
				for (ControllerSettingHook hook : hooks) {
					callHookOnSettingRemove(toBeDeleted, hook, false);
				}
			} catch (Exception ex) {
				rollbackOldValue(id, toBeDeleted, ex);
			}
		}
	}

	protected void rollbackOldValue(ObjectId settingId, ControllerSetting oldValue, Exception ex) {
		if (oldValue != null) {
			// if some hook fails, we try to revert the operation
			super.save(oldValue);

			// notify already called hooks about reverted operation
			for (ControllerSettingHook calledHook : getHooksBySettingKey(oldValue.getKey())) {
				// ignore errors in this case, because otherwise we can get the infinite error loop
				callHookOnSettingSave(oldValue, calledHook, true);
			}
		} else {
			ControllerSetting toBeRemoved = get(settingId);

			if (toBeRemoved != null) {
				super.remove(settingId);

				// notify already called hooks about reverted operation
				for (ControllerSettingHook calledHook : getHooksBySettingKey(toBeRemoved.getKey())) {
					// ignore errors in this case, because otherwise we can get the infinite error loop
					callHookOnSettingRemove(toBeRemoved, calledHook, true);
				}
			}
		}

		// notify the caller about rollback
		throw new ControllerSettingHookRollbackException("Controller setting rollback", ex);
	}

	/**
	 * Calls the onSettingRemove hooks when key is changed in some controller setting (the value with old key is handled as removed)
	 */
	protected void callHooksForChangedKey(ControllerSetting oldValueWithChangedKey) {
		if (oldValueWithChangedKey != null) {
			List<ControllerSettingHook> hooksOnDelete = getHooksBySettingKey(oldValueWithChangedKey.getKey());
			if (hooksOnDelete != null) {
				try {
					for (ControllerSettingHook hook : hooksOnDelete) {
						callHookOnSettingRemove(oldValueWithChangedKey, hook, false);
					}
				} catch (Exception ex) {
					rollbackOldValue(oldValueWithChangedKey.getId(), oldValueWithChangedKey, ex);
					throw ex;
				}
			}
		}
	}

	protected void callHookOnSettingRemove(ControllerSetting deletedSetting, ControllerSettingHook hook, boolean ignoreError) {
		try {
			hook.onSettingRemove(deletedSetting.getId(), deletedSetting);
		} catch (Exception ex) {
			if (ignoreError) {
				log.error("Controller setting hook error", ex);
			} else {
				throw ex;
			}
		}
	}

	protected void callHookOnSettingSave(ControllerSetting res, ControllerSettingHook hook, boolean ignoreError) {
		try {
			hook.onSettingSave(res);
		} catch (Exception ex) {
			if (ignoreError) {
				log.error("Controller setting hook error", ex);
			} else {
				throw ex;
			}
		}
	}

	protected List<ControllerSettingHook> getHooksBySettingKey(String settingKey) {
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
		if (setting == null) {
			setting = new ControllerSetting();
			setting.setKey(key);
		}
		return setting;
	}
	// End of TODO

}
