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
import step.core.accessors.CRUDAccessor;
import step.core.imports.Importer;

public class Entity<A extends AbstractIdentifiableObject, T extends CRUDAccessor<A>> {
	
	private String name;
	private T accessor;
	private Class<A> entityClass;
	private Importer<A,T> importer;
	private List<ResolveReferencesHook> resolveReferencesHook = new ArrayList<ResolveReferencesHook>();
	private List<BiConsumer<Object, Map<String, String>>> updateReferencesHook = new ArrayList<BiConsumer<Object, Map<String, String>>>();
	private boolean byPassObjectPredicate = false;

	public Entity(String name, T accessor, Class<A> entityClass, Importer<A,T> importer) {
		super();
		this.name = name;
		this.accessor = accessor;
		this.entityClass = entityClass;
		this.importer = importer;
		this.importer.init(this);
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

	public Importer<A,T> getImporter() {
		return importer;
	}

	public void setImporter(Importer<A,T> importer) {
		this.importer = importer;
	}

	public List<ResolveReferencesHook> getReferencesHook() {
		return resolveReferencesHook;
	}

	public void setReferencesHook(List<ResolveReferencesHook> referencesHook) {
		this.resolveReferencesHook = referencesHook;
	}

	public List<BiConsumer<Object, Map<String, String>>> getUpdateReferencesHook() {
		return updateReferencesHook;
	}

	public void setUpdateReferencesHook(List<BiConsumer<Object, Map<String, String>>> updateReferencesHook) {
		this.updateReferencesHook = updateReferencesHook;
	}

	public boolean isByPassObjectPredicate() {
		return byPassObjectPredicate;
	}

	public void setByPassObjectPredicate(boolean byPassObjectPredicate) {
		this.byPassObjectPredicate = byPassObjectPredicate;
	}

	public boolean shouldExport(AbstractIdentifiableObject a) {
		return true;
	}
}
