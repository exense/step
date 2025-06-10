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
package step.automation.packages;

import org.bson.types.ObjectId;
import step.core.plans.Plan;
import step.functions.Function;
import step.resources.LocalResourceManagerImpl;
import step.resources.ResourceManager;

import java.io.File;
import java.util.*;

public class AutomationPackageStaging {
    private List<Plan> plans = new ArrayList<>();
    private List<Function> functions = new ArrayList<>();
    private final Map<String, List<?>> additionalObjects = new HashMap<>();

    private ResourceManager resourceManager = new LocalResourceManagerImpl(new File("ap_staging_resources_" + new ObjectId()));

    public List<Plan> getPlans() {
        return plans;
    }

    public List<Function> getFunctions() {
        return functions;
    }

    public ResourceManager getResourceManager() {
        return resourceManager;
    }

    public Set<String> getAdditionalFields(){
        return additionalObjects.keySet();
    }

    public List<?> getAdditionalObjects(String fieldName) {
        return additionalObjects.get(fieldName);
    }

    public void addAdditionalObjects(String fieldName, List<?> objects) {
        if (objects != null) {
            List<Object> existingList = (List<Object>) additionalObjects.computeIfAbsent(fieldName, s -> new ArrayList<>());
            existingList.addAll(objects);
        }
    }
}
