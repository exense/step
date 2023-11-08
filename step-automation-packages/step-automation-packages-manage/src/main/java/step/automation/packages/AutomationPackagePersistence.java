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
import step.core.accessors.AbstractOrganizableObject;
import step.core.entities.EntityManager;
import step.core.entities.EntityReference;
import step.core.objectenricher.EnricheableObject;
import step.resources.Resource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AutomationPackagePersistence extends AbstractOrganizableObject implements EnricheableObject {

    // TODO: token selection criteria for keywords?
    public static final String TRACKING_FIELD = "tracking";

    protected String packageLocation;

    protected boolean watchForChange;

    protected Map<String, String> packageAttributes;

    // TODO: execute locally?
    protected boolean executeLocally;

    /**
     * Keep track of the functions added by this package
     */
    protected List<ObjectId> functions = new ArrayList<>();

    /**
     * Keep track of the plans added by this package
     */
    protected List<ObjectId> plans = new ArrayList<>();

    /**
     * Keep track of the tasks managed by this package
     */
    protected List<ObjectId> tasks = new ArrayList<>();

    /**
     * @return the path to the package file. might be a {@link Resource}
     */
    @EntityReference(type = EntityManager.resources)
    public String getPackageLocation() {
        return packageLocation;
    }

    public void setPackageLocation(String packageLocation) {
        this.packageLocation = packageLocation;
    }

    /**
     * @return true if changes to the content of the package file have to be tracked to automatically update the package
     */
    public boolean isWatchForChange() {
        return watchForChange;
    }

    public void setWatchForChange(boolean watchForChange) {
        this.watchForChange = watchForChange;
    }

    /**
     * @return the additional attributes that have to be added to the attributes of the functions contained in this package
     */
    public Map<String, String> getPackageAttributes() {
        return packageAttributes;
    }

    public void setPackageAttributes(Map<String, String> packageAttributes) {
        this.packageAttributes = packageAttributes;
    }

    /**
     * @return the ID of the functions tracked by this package
     */
    @EntityReference(type = "functions")
    public List<ObjectId> getFunctions() {
        return functions;
    }

    public void setFunctions(List<ObjectId> functions) {
        this.functions = functions;
    }

    /**
     * @return the ID of the plans tracked by this package
     */
    @EntityReference(type = "plans")
    public List<ObjectId> getPlans() {
        return plans;
    }

    public void setPlans(List<ObjectId> plans) {
        this.plans = plans;
    }

    /**
     * @return the ID of the tasks tracked by this package
     */
    @EntityReference(type = "tasks")
    public List<ObjectId> getTasks() {
        return tasks;
    }

    public void setTasks(List<ObjectId> tasks) {
        this.tasks = tasks;
    }

    public boolean isExecuteLocally() {
        return executeLocally;
    }

    public void setExecuteLocally(boolean executeLocally) {
        this.executeLocally = executeLocally;
    }

    @Override
    public String toString() {
        return "FunctionPackage [packageLocation=" + packageLocation + "]";
    }

}
