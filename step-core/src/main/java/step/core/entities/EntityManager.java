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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.core.entities.EntityDependencyTreeVisitor.EntityTreeVisitor;
import step.core.export.ExportContext;
import step.core.imports.ImportContext;
import step.core.objectenricher.EnricheableObject;
import step.core.objectenricher.ObjectPredicate;

public class EntityManager  {

    private static Logger logger = LoggerFactory.getLogger(EntityManager.class);
	
	public final static String executions = "executions";
	public final static String plans = "plans";
	public static final String functions = "functions";
	public final static String reports = "reports";
	public final static String tasks = "tasks";
	public final static String users = "users";
	public final static String resources = "resources";
	public final static String resourceRevisions = "resourceRevisions";
	public final static String recursive = "recursive";
	public final static String measurements = "measurements";
	public final static String metricTypes = "metricTypes";
	public final static String dashboards = "dashboards";
	public final static String bookmarks = "bookmarks";

	private final Map<String, Entity<?,?>> entities = new ConcurrentHashMap<String, Entity<?,?>>();

	private final List<DependencyTreeVisitorHook> dependencyTreeVisitorHooks = new ArrayList<>();

	private final List<BiConsumer<Object, ImportContext>> importHook = new ArrayList<>();
	private final List<BiConsumer<Object, ExportContext>> exportHook = new ArrayList<>();

	public EntityManager register(Entity<?,?> entity) {
		entities.put(entity.getName(), entity);
		return this;
	}

	public Collection<Entity<?, ?>> getEntities() {
		return entities.values();
	}
	
	public Entity<?,?> getEntityByName(String entityName) {
		return entities.get(entityName);
	}

	public Class<?> resolveClass(String entityName) {
		Entity<?, ?> entity = entities.get(entityName);
		Objects.requireNonNull(entity, "This entity type is not known: " + entityName);
		return entity.getEntityClass();
	}

	/**
	 * Retrieve all existing references from the DB for given entity type
	 * @param entityType type of entities to retrieve
	 * @param objectPredicate to apply to filter entities (i.e. project)
	 * @param recursively flag to export references recursively (i.e by exporting a plan recursively the plan will be scanned to find sub references)
	 * @param refs the map of entity references to be populated during the process
	 */
	public void getEntitiesReferences(String entityType, ObjectPredicate objectPredicate, boolean recursively, EntityReferencesMap refs) {
		Entity<?, ?> entity = getEntityByName(entityType);
		if (entity == null ) {
			throw new RuntimeException("Entity of type " + entityType + " is not supported");
		}
		entity.getAccessor().getAll().forEachRemaining(a -> {
			if ((entity.isByPassObjectPredicate() || (!(a instanceof EnricheableObject) || objectPredicate.test((EnricheableObject) a)))) {
				getEntitiesReferences(entityType,a.getId().toHexString(), objectPredicate, refs, recursively);	
			}
		});
	}

	/**
	 * get entities recursively by scanning the given entity (aka artefact), the entity is retrieved and deserialized from the db
	 * @param entityName name of the type of entity
	 * @param entityId the id of the entity
	 * @param references the map of references to be populated
	 */
	public void getEntitiesReferences(String entityName, String entityId, ObjectPredicate objectPredicate, EntityReferencesMap references, boolean recursive) {
		EntityDependencyTreeVisitor entityDependencyTreeVisitor = new EntityDependencyTreeVisitor(this, objectPredicate);
		entityDependencyTreeVisitor.visitEntityDependencyTree(entityName, entityId, new EntityTreeVisitor() {
			
			@Override
			public void onWarning(String warningMessage) {
				references.addReferenceNotFoundWarning(warningMessage);
			}
			
			@Override
			public void onResolvedEntity(String entityName, String entityId, Object entity) {
				references.addElementTo(entityName, entityId);
			}

			@Override
			public String onResolvedEntityId(String entityName, String resolvedEntityId) {
				return null;
			}
		}, recursive);
	}

	public void updateReferences(Object entity, Map<String, String> references, ObjectPredicate objectPredicate, Set<String> messageCollector) {
		if(entity!=null) {
			EntityDependencyTreeVisitor entityDependencyTreeVisitor = new EntityDependencyTreeVisitor(this, objectPredicate);
			entityDependencyTreeVisitor.visitSingleObject(entity, new EntityTreeVisitor() {
				
				@Override
				public void onWarning(String warningMessage) {
				}
				
				@Override
				public void onResolvedEntity(String entityName, String entityId, Object entity) {
				}

				@Override
				public String onResolvedEntityId(String entityName, String resolvedEntityId) {
					String newEntityId = references.get(resolvedEntityId);
					if(logger.isDebugEnabled()) {
						logger.debug("Replacing reference to entity: name = " + entityName + " oldReference = " + resolvedEntityId + " newReference = " + newEntityId);
					}
					return newEntityId;
				}
			}, messageCollector);
		}
	}

	/**
	 * Register a {@link EntityDependencyTreeVisitor} hook
	 * @param hook the hook instance to be registered
	 * @return this instance
	 */
	public EntityManager addDependencyTreeVisitorHook(DependencyTreeVisitorHook hook) {
		dependencyTreeVisitorHooks.add(hook);
		return this;
	}

	public List<DependencyTreeVisitorHook> getDependencyTreeVisitorHooks() {
		return dependencyTreeVisitorHooks;
	}

	public void registerExportHook(BiConsumer<Object, ExportContext> exportBiConsumer) {
		exportHook.add(exportBiConsumer);
	}

	public void runExportHooks(Object o, ExportContext exportContext) {
		exportHook.forEach(h-> h.accept(o, exportContext));
	}

	public void registerImportHook(BiConsumer<Object, ImportContext> importBiConsumer) {
		importHook.add(importBiConsumer);
	}

	public void runImportHooks(Object o, ImportContext importContext) {
		importHook.forEach(h-> h.accept(o, importContext));
		if(o instanceof EnricheableObject) {
			//apply session object enricher as well
			importContext.getImportConfiguration().getObjectEnricher().accept((EnricheableObject) o);
		}
	}
	
}
