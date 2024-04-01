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
package step.artefacts.automation.datasource;

import step.automation.packages.AutomationPackageNamedEntity;
import step.core.dynamicbeans.DynamicValue;
import step.datapool.jdbc.SQLTableDataPoolConfiguration;

@AutomationPackageNamedEntity(name = "sql")
public class YamlSqlDataSource extends AbstractYamlDataSource<SQLTableDataPoolConfiguration> {

    protected DynamicValue<String> connectionString = new DynamicValue<String>("");
    protected DynamicValue<String> query = new DynamicValue<String>("");
    protected DynamicValue<String> user = new DynamicValue<String>("");
    protected DynamicValue<String> password = new DynamicValue<String>("");
    protected DynamicValue<String> writePKey = new DynamicValue<String>("");
    protected DynamicValue<String> driverClass = new DynamicValue<String>("");

    @Override
    public SQLTableDataPoolConfiguration createDataPoolConfiguration() {
        return new SQLTableDataPoolConfiguration();
    }

    @Override
    public void fillDataPoolConfiguration(SQLTableDataPoolConfiguration config) {
        if (this.connectionString != null) {
            config.setConnectionString(connectionString);
        }
        if (this.query != null) {
            config.setQuery(query);
        }
        if (this.user != null) {
            config.setUser(user);
        }
        if (this.password != null) {
            config.setPassword(this.password);
        }
        if (this.writePKey != null) {
            config.setWritePKey(this.writePKey);
        }
        if (this.driverClass != null) {
            config.setDriverClass(this.driverClass);
        }
    }

    @Override
    public void fillFromDataPoolConfiguration(SQLTableDataPoolConfiguration dataPoolConfiguration) {
        if (dataPoolConfiguration.getConnectionString() != null) {
            this.connectionString = dataPoolConfiguration.getConnectionString();
        }
        if (dataPoolConfiguration.getQuery() != null) {
            this.query = dataPoolConfiguration.getQuery();
        }
        if (dataPoolConfiguration.getUser() != null) {
            this.user = dataPoolConfiguration.getUser();
        }
        if (dataPoolConfiguration.getPassword() != null) {
            this.password = dataPoolConfiguration.getPassword();
        }
        if (dataPoolConfiguration.getWritePKey() != null) {
            this.writePKey = dataPoolConfiguration.getWritePKey();
        }
        if (dataPoolConfiguration.getDriverClass() != null) {
            this.driverClass = dataPoolConfiguration.getDriverClass();
        }
    }
}
