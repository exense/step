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

import step.automation.packages.AutomationPackageNamedEntityUtils;
import step.core.artefacts.AbstractArtefact;
import step.datapool.DataPoolConfiguration;
import step.plans.parser.yaml.model.YamlModel;

import java.util.*;
import java.util.stream.Collectors;

public class YamlDataSourceLookuper {

    private static Map<Class<? extends AbstractYamlDataSource<?>>, Class<? extends DataPoolConfiguration>> MODEL_TO_DATAPOOL_MAP =  createModelToDataPoolMap() ;

    private static Map<Class<? extends AbstractYamlDataSource<?>>, Class<? extends DataPoolConfiguration>> createModelToDataPoolMap() {
        Map<Class<? extends AbstractYamlDataSource<?>>, Class<? extends DataPoolConfiguration>> res = new HashMap<>();
        List<Class<? extends DataPoolConfiguration>> dataPools = getDataPools();
        dataPools.forEach(aClass -> res.put(resolveYamlDataSource(aClass), aClass));
        return res;
    }

    private static List<Class<? extends DataPoolConfiguration>> getDataPools(){
        return AutomationPackageNamedEntityUtils.scanNamedEntityClasses(DataPoolConfiguration.class).stream()
                .map(c -> (Class<? extends DataPoolConfiguration>) c)
                .collect(Collectors.toList());
    }

    public static List<Class<? extends AbstractYamlDataSource<?>>> getYamlDataSources() {
       return new ArrayList<>(MODEL_TO_DATAPOOL_MAP.keySet());
    }

    public static Class<? extends AbstractYamlDataSource<?>> resolveYamlDataSource(Class<? extends DataPoolConfiguration> dataPoolConfiguration) {
        if (dataPoolConfiguration.getAnnotation(YamlModel.class) != null) {
            return (Class<? extends AbstractYamlDataSource<?>>) dataPoolConfiguration.getAnnotation(YamlModel.class).model();
        } else {
            return null;
        }
    }

    public static Class<? extends DataPoolConfiguration> resolveDataPool(Class<? extends AbstractYamlDataSource<?>> yamlDataSource) {
        return MODEL_TO_DATAPOOL_MAP.get(yamlDataSource);
    }

    public static Class<? extends AbstractYamlDataSource<?>> getModelClassByYamlName(String yamlName) {
        Collection<Class<? extends DataPoolConfiguration>> dataPoolConfigurations = MODEL_TO_DATAPOOL_MAP.values();
        for (Class<? extends DataPoolConfiguration> dataPool : dataPoolConfigurations) {
            String expectedYamlName = AutomationPackageNamedEntityUtils.getEntityNameByClass(dataPool);

            if (yamlName.equalsIgnoreCase(expectedYamlName)) {
                return resolveYamlDataSource(dataPool);
            }
        }
        return null;
    }

}
