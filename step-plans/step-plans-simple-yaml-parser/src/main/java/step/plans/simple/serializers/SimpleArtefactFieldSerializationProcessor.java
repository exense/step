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
package step.plans.simple.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import step.core.artefacts.AbstractArtefact;
import step.handlers.javahandler.jsonschema.FieldMetadata;

import java.io.IOException;
import java.lang.reflect.Field;

/**
 * Serialized the artefact field from {@link step.core.plans.Plan} object to the simple yaml format
 */
public interface SimpleArtefactFieldSerializationProcessor {

    /**
     * @param artefact      the artefact object
     * @param field         the field to be serialized
     * @param fieldMetadata the field metadata to be used in simple format (the field name in simple format, the default value etc)
     * @param gen           the output yaml generator
     * @return true if this processor is applicable for the artefact field, false otherwise
     */
    boolean serializeArtefactField(AbstractArtefact artefact, Field field, FieldMetadata fieldMetadata, JsonGenerator gen) throws IOException, IllegalAccessException;
}
