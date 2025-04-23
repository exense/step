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
package step.plugins.encryption;

import step.core.EncryptedTrackedObject;
import step.core.dynamicbeans.DynamicValue;
import step.core.export.ExportContext;
import step.parameter.Parameter;
import step.parameter.ParameterManager;

import java.util.function.BiConsumer;

public class EncryptedEntityExportBiConsumer implements BiConsumer<Object, ExportContext> {

    private static String EXPORT_PROTECT_WARN = "The %s list contains protected parameter. The values of these %ss won't be exported and will have to be reset at import.";
    private static String EXPORT_ENCRYPT_WARN = "The %s list contains encrypted %ss. The values of these %ss will be reset if you import them on an other installation of step.";
    private final Class<? extends EncryptedTrackedObject> clazz;
    private final String entityNameForLog;

    public EncryptedEntityExportBiConsumer(Class<? extends EncryptedTrackedObject> clazz, String entityNameForLog) {
        this.clazz = clazz;
        this.entityNameForLog = entityNameForLog;
    }

    @Override
    public void accept(Object object_, ExportContext exportContext) {
        if (object_ != null && clazz.isAssignableFrom(object_.getClass())) {
            Parameter param = (Parameter) object_;
            //if protected and not encrypted, mask value by changing it to reset value
            if (param.getProtectedValue() != null && param.getProtectedValue()) {
                if (param.getValue() != null) {
                    param.setValue(new DynamicValue<>(ParameterManager.RESET_VALUE));
                    exportContext.addMessage(getExportProtectParamWarn());
                } else {
                    exportContext.addMessage(getExportEncryptParamWarn());
                }
            }
        }
    }

    private String getExportProtectParamWarn(){
        return String.format(EXPORT_PROTECT_WARN, entityNameForLog, entityNameForLog);
    }

    private String getExportEncryptParamWarn(){
        return String.format(EXPORT_ENCRYPT_WARN, entityNameForLog, entityNameForLog, entityNameForLog);
    }
}
