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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import step.core.dynamicbeans.DynamicValue;
import step.datapool.DataPoolConfiguration;
import step.datapool.DataPoolFactory;

import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.ANY;
import static com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility.NONE;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonAutoDetect(fieldVisibility = ANY, getterVisibility = NONE, setterVisibility = NONE)
public abstract class AbstractYamlDataSource<T extends DataPoolConfiguration> {

    public static final String FOR_WRITE_FIELD = "forWrite";

    @JsonIgnore
    private final String dataSourceType;

    protected DynamicValue<Boolean> forWrite = new DynamicValue<>(false);

    protected AbstractYamlDataSource(String dataSourceType) {
        this.dataSourceType = dataSourceType;
    }

    public final T createDataPoolConfiguration() {
        return (T) DataPoolFactory.getDefaultDataPoolConfiguration(dataSourceType);
    }

    public void fillDataPoolConfiguration(T config) {
        if (this.forWrite != null) {
            config.setForWrite(forWrite);
        }
    }

    public final T toDataPoolConfiguration(){
        T res = createDataPoolConfiguration();
        fillDataPoolConfiguration(res);
        return res;
    }

    public void fillFromDataPoolConfiguration(T dataPoolConfiguration, boolean isForWriteEditable) {
        if (!isForWriteEditable) {
            this.forWrite = null;
        } else if (dataPoolConfiguration.getForWrite() != null) {
            this.forWrite = dataPoolConfiguration.getForWrite();
        }
    }

    public static <T extends DataPoolConfiguration> AbstractYamlDataSource<T> fromDataPoolConfiguration(T dataPoolConfiguration, boolean isForWriteEditable) {
        try {
            Class<? extends AbstractYamlDataSource<?>> yamlDataSourceClass = YamlDataSourceLookuper.resolveYamlDataSource(dataPoolConfiguration.getClass());
            AbstractYamlDataSource<T> instance = (AbstractYamlDataSource<T>) yamlDataSourceClass.getConstructor().newInstance();
            instance.fillFromDataPoolConfiguration(dataPoolConfiguration, isForWriteEditable);
            return instance;
        } catch (Exception e) {
            throw new RuntimeException("Unable to instantiate yaml datasource");
        }
    }

    public String getDataSourceType() {
        return dataSourceType;
    }
}
