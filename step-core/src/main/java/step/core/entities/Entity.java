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
package step.core.entities;

import java.util.ArrayList;
import java.util.List;

import step.core.accessors.AbstractIdentifiableObject;
import step.core.accessors.Accessor;
import step.core.entities.EntityDependencyTreeVisitor.EntityTreeVisitorContext;

public class Entity<A extends AbstractIdentifiableObject, T extends Accessor<A>> {

	private final String name;
	private final T accessor;
	private final Class<A> entityClass;
	private final List<DependencyTreeVisitorHook> dependencyTreeVisitorHooks = new ArrayList<>();
	private boolean byPassObjectPredicate = false;

	public Entity(String name, T accessor, Class<A> entityClass) {
		super();
		this.name = name;
		this.accessor = accessor;
		this.entityClass = entityClass;
	}

	public String getName() {
		return name;
	}

	public T getAccessor() {
		return accessor;
	}

	public Class<A> getEntityClass() {
		return entityClass;
	}

	public boolean isByPassObjectPredicate() {
		return byPassObjectPredicate;
	}

	public void setByPassObjectPredicate(boolean byPassObjectPredicate) {
		this.byPassObjectPredicate = byPassObjectPredicate;
	}
	
	/**
	 * Register a {@link EntityDependencyTreeVisitor} hook 
	 * @param hook the hook instance to be registered
	 * @return this instance
	 */
	public Entity<A, T> addDependencyTreeVisitorHook(DependencyTreeVisitorHook hook) {
		dependencyTreeVisitorHooks.add(hook);
		return this;
	}
	
	public List<DependencyTreeVisitorHook> getDependencyTreeVisitorHooks() {
		return dependencyTreeVisitorHooks;
	}

	/**
	 * This method is responsible for the resolution of atomic references to entity
	 * id
	 * 
	 * @param atomicReference the atomic reference to be resolved
	 * @param visitorContext  the context object
	 * @return the resolved entity id or null
	 */
	public String resolveAtomicReference(Object atomicReference, EntityTreeVisitorContext visitorContext) {
		return null;
	}

	/**
	 * This method is responsible for the update of atomic references with a new
	 * entity id
	 * 
	 * @param atomicReference the atomic reference to be updated
	 * @param newEntityId     the new entity if
	 * @param visitorContext  the context object
	 * @return the updated atomic reference
	 */
	public Object updateAtomicReference(Object atomicReference, String newEntityId,
			EntityTreeVisitorContext visitorContext) {
		return null;
	}
}
