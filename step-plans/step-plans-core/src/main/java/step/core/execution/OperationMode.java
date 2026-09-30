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

/**
 * The context in which an execution runs, which decides where its plans and keywords come from and where the keywords are run.
 */
public enum OperationMode {

    /**
     * An execution driven by a Step controller, on the agents of its grid.
     */
    CONTROLLER(false, false, false),

    /**
     * A local execution taking its keywords from the annotated classes found on the class path, and running them in
     * the same JVM. The mode of the JUnit tests and of {@code DefaultPlanRunner}, and the one an
     * {@link ExecutionEngine} falls back to when none is set.
     */
    LOCAL_PLAN(true, false, true),

    /**
     * A local execution driven by an automation package, which provides the keywords through a class loader of its
     * own. They are still run in the same JVM.
     */
    LOCAL_AUTOMATION_PACKAGE(true, true, true),

    /**
     * A local execution driven by an automation package whose keywords are run on real agents, started as separate
     * processes on this machine. The mode of the CLI.
     */
    CLI(true, true, false);

    private final boolean local;
    private final boolean automationPackage;
    private final boolean keywordsInProcess;

    OperationMode(boolean local, boolean automationPackage, boolean keywordsInProcess) {
        this.local = local;
        this.automationPackage = automationPackage;
        this.keywordsInProcess = keywordsInProcess;
    }

    /**
     * @return {@code true} for any local execution, regardless of which classloader strategy is used. Use this
     * wherever the distinction between the local modes does not matter (e.g. skipping controller-only setup).
     */
    public boolean isLocal() {
        return local;
    }

    /**
     * @return {@code true} when we execute plans in context of an automation package, in which case, the Automation Package
     * manager is responsible for providing the entities. Otherwise, the keywords are retrieved by scanning
     * the annotated classes found on the class path.
     */
    public boolean isAutomationPackage() {
        return automationPackage;
    }

    /**
     * @return {@code true} when the keywords of the execution run in the JVM of the execution engine, on a local
     * token, rather than on an agent.
     * <p>
     * This is the case for every local mode but {@link #CLI}, where real agents are started on the machine and
     * the keywords are routed to them exactly like on a controller. Running in the JVM is a feature of the JUnit
     * runners, where the keywords under test are the ones of the project being built; it is a limitation everywhere
     * else, as only Java keywords have an in-JVM handler.
     */
    public boolean runsKeywordsInProcess() {
        return keywordsInProcess;
    }
}
