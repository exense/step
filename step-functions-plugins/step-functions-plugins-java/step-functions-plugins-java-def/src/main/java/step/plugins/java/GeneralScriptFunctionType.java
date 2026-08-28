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
package step.plugins.java;

import ch.exense.commons.app.Configuration;
import org.apache.commons.lang3.StringUtils;
import step.core.AbstractStepContext;
import step.core.accessors.AbstractOrganizableObject;
import step.core.dynamicbeans.DynamicValue;
import step.functions.type.SetupFunctionException;

import java.util.Map;

public class GeneralScriptFunctionType extends AbstractScriptFunctionType<GeneralScriptFunction> {

    /**
     * The script a new keyword starts with, per language. Kept as data rather than as a chain of
     * conditions so that a test can enumerate it: a language added here without its file added to
     * {@code src/main/resources/step/plugins/java/templates/} would otherwise create empty scripts
     * and only say so in a log line.
     *
     * @see AbstractScriptFunctionType#getTemplateFileInputStream(String)
     */
    static final Map<String, String> TEMPLATE_BY_LANGUAGE = Map.of(
        "javascript", "custom_script.js",
        "groovy", "custom_script.groovy");

    public GeneralScriptFunctionType(Configuration configuration) {
        super(configuration);
    }

    @Override
    public void setupFunction(GeneralScriptFunction function) throws SetupFunctionException {
        String language = getScriptLanguage(function);
        if (language.equals("java")) {
            // No specific setup for java at the moment
            return;
        }
        // a language naming no template gets an empty script
        setupScriptFileAsResource(function, TEMPLATE_BY_LANGUAGE.get(language));
    }

    @Override
    public GeneralScriptFunction newFunction() {
        GeneralScriptFunction function = new GeneralScriptFunction();
        function.getScriptLanguage().setValue("java");
        return function;
    }

    @Override
    public GeneralScriptFunction newFunction(Map<String, String> configuration) {
        GeneralScriptFunction function = this.newFunction();
        function.addAttribute(AbstractOrganizableObject.NAME, configuration.get("name"));
        function.setScriptFile(new DynamicValue<>(configuration.get("scriptFile")));
        return function;
    }

    @Override
    public HandlerProperties getHandlerProperties(GeneralScriptFunction function, AbstractStepContext context) {
        String language = getScriptLanguage(function);
        if (language != null) {
            String errorHandler = configuration.getProperty("plugins." + language + ".errorhandler", null);
            if (errorHandler != null) {
                function.setErrorHandlerFile(new DynamicValue<>(errorHandler));
            }
        }
        return super.getHandlerProperties(function, context);
    }
}
