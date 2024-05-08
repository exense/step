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
package step.core.controller.settings;

import step.framework.server.Session;

import java.util.Objects;
import java.util.function.Predicate;

import static step.core.controller.settings.AbstractScopedObject.SCOPE_FIELD;

public abstract class ObjectScopeHandler {

    private final String scopeName;

    protected ObjectScopeHandler(String scopeName) {
        this.scopeName = scopeName;
    }

    protected String getFilterField() {
        return SCOPE_FIELD + "." + scopeName;
    }

    public String getScopeName() {
        return scopeName;
    }

    /**
     * return the scope value for current scope in given session
     * @param session
     * @return the scope value in given session
     */
    protected abstract String getScopeValue(Session<?> session);

    public Predicate<AbstractScopedObject> getObjectPredicate(Session<?> session){
        return new Predicate<AbstractScopedObject>() {
            @Override
            public boolean test(AbstractScopedObject abstractScopedObject) {
                String value = abstractScopedObject.getScope().get(scopeName);
                return value != null && value.equals(getScopeValue(session));
            }
        };
    }

    public Predicate<AbstractScopedObject> getObjectPredicateIsNotSet(){
        return new Predicate<AbstractScopedObject>() {
            @Override
            public boolean test(AbstractScopedObject abstractScopedObject) {
                return !abstractScopedObject.getScope().containsKey(scopeName);
            }
        };
    }

    public boolean scopeIsApplicable(Session<?> session) {
        return (getScopeValue(session) != null);
    }

    /**
     * Return the priority for this scope, higher number is the highest priority
     * Note that for combined scope search the sum must be considered, i.e. if scope1 alone must have an higher prio
     * than scope2 and scope3 together: prio1 > prio2+prio3 must be true
     * @return the priority
     */
    protected abstract int getPriority();

    public void saveScopeDetails(AbstractScopedObject scopeObject, Session<?> session) {
        String scopeValue = getScopeValue(session);
        Objects.requireNonNull(scopeValue, "Scope value is not available for requested scope '" + getScopeName() + "'.");
        scopeObject.addScope(scopeName, scopeValue);
    }
}
