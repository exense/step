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
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.plugins.measurements.MeasurementPlugin;

@Plugin
public class RawMeasurementsControllerPlugin extends AbstractControllerPlugin {

	private static final Logger logger = LoggerFactory.getLogger(RawMeasurementsControllerPlugin.class);

	private MeasurementAccessor accessor;

	@Override
	public void serverStart(GlobalContext context) throws Exception {

        Collection<Document> collection = context.getCollectionFactory().getCollection(MeasurementAccessor.ENTITY_NAME, Document.class);
        collection.createOrUpdateCompoundIndex(MeasurementPlugin.ATTRIBUTE_EXECUTION_ID, MeasurementPlugin.BEGIN);
		collection.createOrUpdateCompoundIndex(MeasurementPlugin.ATTRIBUTE_EXECUTION_ID, MeasurementPlugin.TYPE, MeasurementPlugin.BEGIN);
		collection.createOrUpdateCompoundIndex(MeasurementPlugin.PLAN_ID, MeasurementPlugin.BEGIN);
		collection.createOrUpdateCompoundIndex(MeasurementPlugin.TASK_ID, MeasurementPlugin.BEGIN);
		collection.createOrUpdateIndex(MeasurementPlugin.BEGIN);
        accessor = new MeasurementAccessor(collection);
		context.put(MeasurementAccessor.class, accessor);

		MeasurementPlugin.registerMeasurementHandlers(new RawMeasurementsHandler(accessor));
	}

	@Override
	public void serverStop(GlobalContext context) {
		if(accessor !=null) {
			accessor.close();
		}
	}
}
