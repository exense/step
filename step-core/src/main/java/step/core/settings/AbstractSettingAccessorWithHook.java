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
package step.core.settings;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.ValueWithKey;
import step.core.accessors.AbstractAccessor;
import step.core.accessors.AbstractIdentifiableObject;
import step.core.collections.Collection;
import step.core.collections.Filters;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public abstract class AbstractSettingAccessorWithHook<T extends AbstractIdentifiableObject & ValueWithKey> extends AbstractAccessor<T> implements SettingAccessorWithHook<T> {

    protected static final Logger log = LoggerFactory.getLogger(AbstractSettingAccessorWithHook.class);

    private final Map<String, List<SettingHook<T>>> hooksMap = new ConcurrentHashMap<>();

    public AbstractSettingAccessorWithHook(Collection<T> collectionDriver) {
        super(collectionDriver);
    }

    public T getSettingByKey(String key) {
        return collectionDriver.find(Filters.equals("key", key), null, null, null, 0).findFirst().orElse(null);
    }

    public void addHook(String key, SettingHook<T> hook) {
        this.hooksMap.computeIfAbsent(key, k -> new ArrayList<>());
        List<SettingHook<T>> list = this.hooksMap.get(key);
        list.add(hook);
    }

    public boolean removeHook(String key, SettingHook<T> hook) {
        List<SettingHook<T>> hooks = getHooksBySettingKey(key);
        if (hooks != null) {
            return hooks.remove(hook);
        } else {
            return false;
        }
    }

    @Override
    public void remove(ObjectId id) {
        T toBeDeleted = get(id);
        super.remove(id);
        List<SettingHook<T>> hooks = getHooksBySettingKey(toBeDeleted.getKey());

        if (hooks != null) {
            try {
                for (SettingHook<T> hook : hooks) {
                    callHookOnSettingRemove(toBeDeleted, hook, false);
                }
            } catch (Exception ex) {
                rollbackOldValue(id, toBeDeleted, ex);

                // notify the caller about rollback
                throw new SettingHookRollbackException("Controller setting rollback", ex);
            }
        }
    }

    protected List<SettingHook<T>> getHooksBySettingKey(String settingKey) {
        return this.hooksMap.get(settingKey);
    }

    protected Map<String, List<SettingHook<T>>> getHooksMap() {
        return hooksMap;
    }

    protected void callHookOnSettingRemove(T deletedSetting, SettingHook<T> hook, boolean ignoreError) {
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

    protected void callHookOnSettingSave(T res, SettingHook<T> hook, boolean ignoreError) {
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

    protected void rollbackOldValue(ObjectId settingId, T oldValue, Exception ex) {
        if (oldValue != null) {
            // if some hook fails, we try to revert the operation
            super.save(oldValue);

            // notify already called hooks about reverted operation
            for (SettingHook<T> calledHook : getHooksBySettingKey(oldValue.getKey())) {
                // ignore errors in this case, because otherwise we can get the infinite error loop
                callHookOnSettingSave(oldValue, calledHook, true);
            }
        } else {
            T toBeRemoved = get(settingId);

            if (toBeRemoved != null) {
                super.remove(settingId);

                // notify already called hooks about reverted operation
                for (SettingHook<T> calledHook : getHooksBySettingKey(toBeRemoved.getKey())) {
                    // ignore errors in this case, because otherwise we can get the infinite error loop
                    callHookOnSettingRemove(toBeRemoved, calledHook, true);
                }
            }
        }

    }

    /**
     * Calls the onSettingRemove hooks when key is changed in some controller setting (the value with old key is handled as removed)
     */
    protected void callHooksForChangedKey(T oldValueWithChangedKey) {
        if (oldValueWithChangedKey != null) {
            List<SettingHook<T>> hooksOnDelete = getHooksBySettingKey(oldValueWithChangedKey.getKey());
            if (hooksOnDelete != null) {
                try {
                    for (SettingHook<T> hook : hooksOnDelete) {
                        callHookOnSettingRemove(oldValueWithChangedKey, hook, false);
                    }
                } catch (Exception ex) {
                    rollbackOldValue(oldValueWithChangedKey.getId(), oldValueWithChangedKey, ex);
                    throw ex;
                }
            }
        }
    }

    protected T getOldValue(T newValue) {
        if (newValue.getId() != null) {
            return get(newValue.getId());
        }
        return null;
    }

    protected boolean oldValueHasAnotherKey(T oldValue, T newValue) {
        if (newValue != null) {
            if (oldValue != null) {
                return !Objects.equals(oldValue.getKey(), newValue.getKey());
            }
        }
        return false;
    }

    @Override
    public void save(Iterable<T> entities) {
        List<T> oldValues = new ArrayList<>();
        List<T> oldValuesWithChangedKeys = new ArrayList<>();
        for (T newValue : entities) {
            T oldValue = getOldValue(newValue);
            if (oldValue != null) {
                oldValues.add(oldValue);
                if (oldValueHasAnotherKey(oldValue, newValue)) {
                    oldValuesWithChangedKeys.add(oldValue);
                }
            }
        }

        super.save(entities);

        try {
            for (T entity : entities) {
                T oldValueWithChangedKey = oldValuesWithChangedKeys.stream().filter(v -> Objects.equals(v.getId(), entity.getId())).findFirst().orElse(null);

                callHooksForChangedKey(oldValueWithChangedKey);

                List<SettingHook<T>> hooks = getHooksBySettingKey(entity.getKey());
                if (hooks != null) {
                    for (SettingHook<T> hook : hooks) {
                        callHookOnSettingSave(entity, hook, false);
                    }
                }
            }
        } catch (Exception ex) {
            // rollback save on hook failure
            for (T entity : entities) {
                try {
                    rollbackOldValue(
                            entity.getId(),
                            oldValues.stream().filter(v -> Objects.equals(v.getId(), entity.getId())).findFirst().orElse(null),
                            ex
                    );
                } catch (Exception ex2) {
                    // just print errors in log during rollback
                    log.error("Controller setting hook error", ex);
                }
            }

            // notify the caller about rollback
            throw new SettingHookRollbackException("Controller setting rollback", ex);
        }
    }

    @Override
    public T save(T entity) {
        // we can change the key of existing setting - in this case we notify hooks about deleted/created setting
        T oldValue = getOldValue(entity);
        T res = super.save(entity);

        if (oldValueHasAnotherKey(oldValue, entity)) {
            callHooksForChangedKey(oldValue);
        }

        List<SettingHook<T>> hooks = getHooksBySettingKey(entity.getKey());
        if (hooks != null) {
            try {
                for (SettingHook<T> hook : hooks) {
                    callHookOnSettingSave(res, hook, false);
                }
            } catch (Exception ex) {
                rollbackOldValue(res.getId(), oldValue, ex);

                // notify the caller about rollback
                throw new SettingHookRollbackException("Controller setting rollback", ex);
            }
        }

        return res;
    }
}
