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
package step.automation.packages.junit;

import step.junit.runners.annotations.ExecutionParameters;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class JunitExecutionParametersProvider {

    private final static Pattern SYSTEM_PROPERTIES_PREFIX = Pattern.compile("STEP_(.+?)");

    public Map<String, String> getExecutionParameters(Class<?> testClass) {
        HashMap<String, String> executionParameters = new HashMap<>();
        // Prio 3: Execution parameters from annotation ExecutionParameters
        executionParameters.putAll(getExecutionParametersByAnnotation(testClass));
        // Prio 2: Execution parameters from environment variables (prefixed with STEP_*)
        executionParameters.putAll(getExecutionParametersFromEnvironmentVariables());
        // Prio 3: Execution parameters from system properties
        executionParameters.putAll(getExecutionParametersFromSystemProperties());
        return executionParameters;
    }

    private Map<String, String> getExecutionParametersByAnnotation(Class<?> testClass) {
        Map<String, String> executionParameters = new HashMap<>();
        ExecutionParameters params;
        if ((params = testClass.getAnnotation(ExecutionParameters.class)) != null) {
            String key = null;
            for (String param : params.value()) {
                if (key == null) {
                    key = param;
                } else {
                    executionParameters.put(key, param);
                    key = null;
                }
            }
        }
        return executionParameters;
    }

    protected Map<String, String> getExecutionParametersFromSystemProperties() {
        Map<String, String> executionParameters = new HashMap<>();
        System.getProperties().forEach((k, v) ->
                unescapeParameterKeyIfMatches(k.toString()).ifPresent(key -> executionParameters.put(key, v.toString())));
        return executionParameters;
    }

    private Map<String, String> getExecutionParametersFromEnvironmentVariables() {
        Map<String, String> executionParameters = new HashMap<>();
        System.getenv().forEach((k, v) -> unescapeParameterKeyIfMatches(k).ifPresent(key -> executionParameters.put(key, v)));
        return executionParameters;
    }

    private Optional<String> unescapeParameterKeyIfMatches(String key) {
        Matcher matcher = SYSTEM_PROPERTIES_PREFIX.matcher(key);
        if (matcher.matches()) {
            String unescapedKey = matcher.group(1);
            return Optional.of(unescapedKey);
        } else {
            return Optional.empty();
        }
    }
}
