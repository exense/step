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
import java.util.Map;
import java.util.function.BiConsumer;

import step.core.accessors.AbstractIdentifiableObject;
import step.core.accessors.Accessor;
import step.core.entities.EntityDependencyTreeVisitor.EntityTreeVisitorContext;

public class Entity<A extends AbstractIdentifiableObject, T extends Accessor<A>> {

	private String name;
	private T accessor;
	private Class<A> entityClass;
	private final List<ResolveReferencesHook> resolveReferencesHook = new ArrayList<>();
	private final List<BiConsumer<Object, Map<String, String>>> updateReferencesHook = new ArrayList<>();
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

	public void setName(String name) {
		this.name = name;
	}

	public T getAccessor() {
		return accessor;
	}

	public void setAccessor(T accessor) {
		this.accessor = accessor;
	}

	public Class<A> getEntityClass() {
		return entityClass;
	}

	public void setEntityClass(Class<A> entityClass) {
		this.entityClass = entityClass;
	}

	public List<ResolveReferencesHook> getResolveReferencesHook() {
		return resolveReferencesHook;
	}

	public Entity<A, T> addReferencesHook(ResolveReferencesHook hook) {
		resolveReferencesHook.add(hook);
		return this;
	}

	public List<BiConsumer<Object, Map<String, String>>> getUpdateReferencesHook() {
		return updateReferencesHook;
	}

	public Entity<A, T> addUpdateReferencesHook(BiConsumer<Object, Map<String, String>> hook) {
		updateReferencesHook.add(hook);
		return this;
	}

	public boolean isByPassObjectPredicate() {
		return byPassObjectPredicate;
	}

	public void setByPassObjectPredicate(boolean byPassObjectPredicate) {
		this.byPassObjectPredicate = byPassObjectPredicate;
	}

	/**
	 * This method is responsible to the resolution of atomic references to entity
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
	 * This method is responsible to the update of atomic references with a new
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
