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
package step.plugins.measurements.raw;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.core.GlobalContext;
import step.core.collections.Collection;
import step.core.collections.Document;
import step.core.collections.IndexField;
import step.core.collections.Order;
import step.core.entities.EntityManager;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.framework.server.tables.Table;
import step.framework.server.tables.TableRegistry;
import step.plugins.measurements.MeasurementPlugin;

import java.util.LinkedHashSet;
import java.util.List;

@Plugin
public class RawMeasurementsControllerPlugin extends AbstractControllerPlugin {

	private static final Logger logger = LoggerFactory.getLogger(RawMeasurementsControllerPlugin.class);

	private MeasurementAccessor accessor;
	private Collection<Document> collection;

	@Override
	public void serverStart(GlobalContext context) throws Exception {

		collection = context.getCollectionFactory().getCollection(EntityManager.measurements, Document.class);
		accessor = new MeasurementAccessor(collection);
		context.put(MeasurementAccessor.class, accessor);

		TableRegistry tableRegistry = context.get(TableRegistry.class);
		tableRegistry.register(EntityManager.measurements, new Table<>(collection, null, false));

		MeasurementPlugin.registerMeasurementHandlers(new RawMeasurementsHandler(accessor));
	}

	@Override
	public void serverStop(GlobalContext context) {
	}

	@Override
	public void initializeData(GlobalContext context) throws Exception {
		IndexField eidIndex = new IndexField(MeasurementPlugin.ATTRIBUTE_EXECUTION_ID, Order.ASC, String.class);
		IndexField beginIndex = new IndexField(MeasurementPlugin.BEGIN, Order.ASC, Integer.class);
		IndexField typeIndex = new IndexField(MeasurementPlugin.TYPE, Order.ASC, String.class);
		IndexField planIndex = new IndexField(MeasurementPlugin.PLAN_ID, Order.ASC, String.class);
		IndexField taskIndex = new IndexField(MeasurementPlugin.TASK_ID, Order.ASC, String.class);
		collection.createOrUpdateCompoundIndex(new LinkedHashSet<>(List.of(eidIndex, beginIndex)));
		collection.createOrUpdateCompoundIndex(new LinkedHashSet<>(List.of(eidIndex, typeIndex, beginIndex)));
		collection.createOrUpdateCompoundIndex(new LinkedHashSet<>(List.of(planIndex, beginIndex)));
		collection.createOrUpdateCompoundIndex(new LinkedHashSet<>(List.of(taskIndex, beginIndex)));
		collection.createOrUpdateIndex(beginIndex);
	}
}
