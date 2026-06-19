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
package step.core.execution.model;

import java.util.Map;

/**
 * A lightweight, persisted record of a noteworthy event that occurred during an execution
 * and which is <b>not</b> a lifecycle error. Notices never affect the execution's result or status.
 * <p>
 * A notice only references a registered notice type by its id and carries the parameters used to
 * resolve the type's parametrized message. The message template, severity and category are held by
 * the notice type (in the registry) and are resolved server-side at read time. The resolved message
 * is never persisted, which keeps this model free of any dependency on the registry and allows
 * message templates to be fixed (or localized) retroactively for already-finished executions.
 */
public class ExecutionNotice {

    private String typeId;
    private Map<String, String> parameters;
    private long timestamp;

    public ExecutionNotice() {
        super();
    }

    public ExecutionNotice(String typeId, Map<String, String> parameters) {
        this.typeId = typeId;
        this.parameters = parameters;
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * @return the id of the registered notice type this notice is an instance of
     */
    public String getTypeId() {
        return typeId;
    }

    public void setTypeId(String typeId) {
        this.typeId = typeId;
    }

    /**
     * @return the key-value pairs used to resolve the placeholders of the notice type's message template
     */
    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    /**
     * @return the epoch milliseconds at which the notice was first raised
     */
    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
