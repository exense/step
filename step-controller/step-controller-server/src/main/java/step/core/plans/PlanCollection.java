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
package step.core.plans;

import java.util.ArrayList;
import java.util.List;

import javax.json.JsonObject;

import org.bson.Document;
import org.bson.conversions.Bson;

import com.mongodb.client.MongoDatabase;

import step.core.accessors.collections.Collection;

public class PlanCollection extends Collection<Plan> {

	public PlanCollection(MongoDatabase mongoDatabase) {
		super(mongoDatabase, "plans", Plan.class, true);
	}

	@Override
	public List<Bson> getAdditionalQueryFragments(JsonObject queryParameters) {
		ArrayList<Bson> fragments = new ArrayList<Bson>();
		fragments.add(new Document("visible", true));
		return fragments;
	}
}
