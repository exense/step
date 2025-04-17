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
package step.plugins.parametermanager;

import step.core.EncryptedTrackedObject;
import step.core.dynamicbeans.DynamicValue;
import step.core.export.ExportContext;
import step.parameter.Parameter;
import step.parameter.ParameterManager;

import java.util.function.BiConsumer;

public class EncryptedEntityExportBiConsumer implements BiConsumer<Object, ExportContext> {

    public static String EXPORT_PROTECT_PARAM_WARN = "The parameter list contains protected parameter. The values of these parameters won't be exported and will have to be reset at import.";
    public static String EXPORT_ENCRYPT_PARAM_WARN = "The parameter list contains encrypted parameters. The values of these parameters will be reset if you import them on an other installation of step.";
    private final Class<? extends EncryptedTrackedObject> clazz;

    public EncryptedEntityExportBiConsumer(Class<? extends EncryptedTrackedObject> clazz) {
        this.clazz = clazz;
    }

    @Override
    public void accept(Object object_, ExportContext exportContext) {
        if (object_ != null && clazz.isAssignableFrom(object_.getClass())) {
            Parameter param = (Parameter) object_;
            //if protected and not encrypted, mask value by changing it to reset value
            if (param.getProtectedValue() != null && param.getProtectedValue()) {
                if (param.getValue() != null) {
                    param.setValue(new DynamicValue<>(ParameterManager.RESET_VALUE));
                    exportContext.addMessage(EXPORT_PROTECT_PARAM_WARN);
                } else {
                    exportContext.addMessage(EXPORT_ENCRYPT_PARAM_WARN);
                }
            }
        }
    }
}
