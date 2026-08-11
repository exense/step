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
package step.core.execution;

public enum OperationMode {

    CONTROLLER,

    LOCAL, //Used for our Junit tests, LocalPlanRunner

    LOCAL_AUTOMATION_PACKAGE, //Used for Local AP execution (external class loader)

    LOCAL_AUTOMATION_PACKAGE_WITH_AGENTS; //Used for the CLI, which runs the keywords on agents forked on this machine

    /**
     * Returns {@code true} for any local execution mode, regardless of which classloader
     * strategy is used. Use this wherever the distinction between {@link #LOCAL} and
     * {@link #LOCAL_AUTOMATION_PACKAGE} does not matter (e.g. skipping controller-only setup).
     */
    public static boolean isLocal(OperationMode operationMode) {
        return operationMode == LOCAL || operationMode == LOCAL_AUTOMATION_PACKAGE
            || operationMode == LOCAL_AUTOMATION_PACKAGE_WITH_AGENTS;
    }

    /**
     * Returns {@code true} when the plans come from an automation package, which is then responsible for providing
     * the keywords. Local executions which are not driven by an automation package instead take their keywords from
     * the annotated classes found on the class path.
     */
    public static boolean isLocalAutomationPackage(OperationMode operationMode) {
        return operationMode == LOCAL_AUTOMATION_PACKAGE || operationMode == LOCAL_AUTOMATION_PACKAGE_WITH_AGENTS;
    }

    /**
     * Returns {@code true} when the keywords of the execution run in the JVM of the execution engine, on a local
     * token, rather than on an agent.
     * <p>
     * This is the case for every local mode but {@link #LOCAL_AUTOMATION_PACKAGE_WITH_AGENTS}, where real agents are
     * started on the machine and the keywords are routed to them exactly like on a controller. Running in the JVM is
     * a feature of the JUnit runners, where the keywords under test are the ones of the project being built; it is a
     * limitation everywhere else, as only Java keywords have an in-JVM handler.
     */
    public static boolean runsKeywordsInProcess(OperationMode operationMode) {
        return operationMode == LOCAL || operationMode == LOCAL_AUTOMATION_PACKAGE;
    }
}
