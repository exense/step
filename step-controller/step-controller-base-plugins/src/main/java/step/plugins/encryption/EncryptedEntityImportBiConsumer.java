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
import step.core.encryption.EncryptionManager;
import step.core.encryption.EncryptionManagerException;
import step.core.imports.ImportContext;
import step.encryption.AbstractEncryptedValuesManager;

import java.util.function.BiConsumer;

public class EncryptedEntityImportBiConsumer implements BiConsumer<Object, ImportContext> {

    private final EncryptionManager encryptionManager;
    private final Class<? extends EncryptedTrackedObject> clazz;
    private final String entityNameForLog;

    public EncryptedEntityImportBiConsumer(EncryptionManager encryptionManager, Class<? extends EncryptedTrackedObject> clazz, String entityNameForLog) {
        this.encryptionManager = encryptionManager;
        this.clazz = clazz;
        this.entityNameForLog = entityNameForLog;
    }

    @Override
    public void accept(Object object_, ImportContext importContext) {
        if (object_ != null && clazz.isAssignableFrom(object_.getClass())) {
            EncryptedTrackedObject param = (EncryptedTrackedObject) object_;
            //if importing protected and encrypted value
            if (param.getProtectedValue() != null && param.getProtectedValue()) {
                if (param.getValue() == null) {
                    //if we have a valid encryption manager and can still decrypt keep encrypted value, else reset
                    if (encryptionManager != null && param.getEncryptedValue() != null) {
                        try {
                            encryptionManager.decrypt(param.getEncryptedValue());
                        } catch (EncryptionManagerException e) {
                            param.setValue(new DynamicValue<>(AbstractEncryptedValuesManager.RESET_VALUE));
                            param.setEncryptedValue(null);
                            importContext.addMessage(getImportDecryptFailWarn());
                        }
                    } else {
                        param.setValue(new DynamicValue<>(AbstractEncryptedValuesManager.RESET_VALUE));
                        param.setEncryptedValue(null);
                        importContext.addMessage(getImportDecryptNoEmWarn());
                    }
                } else {
                    param.setValue(new DynamicValue<>(AbstractEncryptedValuesManager.RESET_VALUE));
                    importContext.addMessage(getImportResetWarn());
                }
            }
        }
    }

    private String getImportDecryptFailWarn() {
        return String.format("The export file contains encrypted %s which could not be decrypted. The values of these %ss will be reset.", entityNameForLog, entityNameForLog);
    }

    private String getImportDecryptNoEmWarn() {
        return String.format("The export file contains encrypted %ss. The values of these %ss will be reset.", entityNameForLog, entityNameForLog);
    }

    private String getImportResetWarn() {
        return String.format("The export file contains protected %ss. Their values must be reset.", entityNameForLog);
    }
}
