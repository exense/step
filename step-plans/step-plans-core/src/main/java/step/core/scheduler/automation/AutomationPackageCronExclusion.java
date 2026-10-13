/*******************************************************************************
 * Copyright (C) 2026, exense GmbH
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
package step.core.scheduler.automation;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import step.core.scheduler.CronExclusion;

import java.io.IOException;

/**
 * A cron exclusion of a schedule. In YAML it is either the cron expression alone, as a string, or an object holding
 * the cron expression and its description:
 * <pre>
 * cronExclusions:
 *   - "0 0 9 25 * ?"
 *   - cron: "0 0 9 20 * ?"
 *     description: "Maintenance window"
 * </pre>
 */
@JsonSerialize(using = AutomationPackageCronExclusion.Serializer.class)
@JsonDeserialize(using = AutomationPackageCronExclusion.Deserializer.class)
public class AutomationPackageCronExclusion {

    public static final String CRON_FIELD = "cron";
    public static final String DESCRIPTION_FIELD = "description";

    private String cron;
    private String description;

    public AutomationPackageCronExclusion() {
    }

    public AutomationPackageCronExclusion(String cron, String description) {
        this.cron = cron;
        this.description = description;
    }

    public static AutomationPackageCronExclusion fromCronExclusion(CronExclusion cronExclusion) {
        return new AutomationPackageCronExclusion(cronExclusion.getCronExpression(), cronExclusion.getDescription());
    }

    public CronExclusion toCronExclusion() {
        return new CronExclusion(cron, hasDescription() ? description : "");
    }

    public String getCron() {
        return cron;
    }

    public void setCron(String cron) {
        this.cron = cron;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    private boolean hasDescription() {
        return description != null && !description.isEmpty();
    }

    @Override
    public String toString() {
        return hasDescription() ? cron + " (" + description + ")" : cron;
    }

    public static class Serializer extends JsonSerializer<AutomationPackageCronExclusion> {

        @Override
        public void serialize(AutomationPackageCronExclusion value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
            if (value.hasDescription()) {
                gen.writeStartObject();
                gen.writeStringField(CRON_FIELD, value.getCron());
                gen.writeStringField(DESCRIPTION_FIELD, value.getDescription());
                gen.writeEndObject();
            } else {
                gen.writeString(value.getCron());
            }
        }
    }

    public static class Deserializer extends JsonDeserializer<AutomationPackageCronExclusion> {

        @Override
        public AutomationPackageCronExclusion deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonNode node = p.readValueAsTree();
            if (node.isTextual()) {
                return new AutomationPackageCronExclusion(node.asText(), null);
            } else if (node.isObject()) {
                return new AutomationPackageCronExclusion(asText(node.get(CRON_FIELD)), asText(node.get(DESCRIPTION_FIELD)));
            } else {
                return ctxt.reportInputMismatch(AutomationPackageCronExclusion.class,
                    "A cron exclusion is either a cron expression or an object with the fields '%s' and '%s'", CRON_FIELD, DESCRIPTION_FIELD);
            }
        }

        private static String asText(JsonNode node) {
            return node == null || node.isNull() ? null : node.asText();
        }
    }
}
