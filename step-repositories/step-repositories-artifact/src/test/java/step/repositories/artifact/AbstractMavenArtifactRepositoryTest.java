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
package step.repositories.artifact;

import ch.exense.commons.app.Configuration;
import org.mockito.Mockito;
import step.automation.packages.*;
import step.automation.packages.deserialization.AutomationPackageSerializationRegistry;
import step.automation.packages.yaml.YamlAutomationPackageVersions;
import step.core.controller.InMemoryControllerSettingAccessor;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionEngine;
import step.core.objectenricher.ObjectHookRegistry;
import step.core.plans.InMemoryPlanAccessor;
import step.functions.accessor.FunctionAccessor;
import step.functions.accessor.InMemoryFunctionAccessorImpl;
import step.functions.type.AbstractFunctionType;
import step.functions.type.FunctionTypeRegistry;
import step.plugins.functions.types.CompositeFunction;
import step.plugins.functions.types.CompositeFunctionType;
import step.plugins.java.GeneralScriptFunction;
import step.plugins.java.GeneralScriptFunctionType;
import step.plugins.jmeter.JMeterFunction;
import step.plugins.jmeter.JMeterFunctionType;
import step.plugins.node.NodeFunction;
import step.plugins.node.NodeFunctionType;
import step.repositories.ArtifactRepositoryConstants;
import step.resources.LocalResourceManagerImpl;

import java.io.IOException;
import java.util.Map;

public abstract class AbstractMavenArtifactRepositoryTest {

    protected static final Map<String, String> REPOSITORY_PARAMETERS = Map.of(ArtifactRepositoryConstants.ARTIFACT_PARAM_GROUP_ID, "ch.exense.step",
            ArtifactRepositoryConstants.ARTIFACT_PARAM_ARTIFACT_ID, "step-automation-packages-junit", ArtifactRepositoryConstants.ARTIFACT_PARAM_VERSION, "0.0.0",
            ArtifactRepositoryConstants.ARTIFACT_PARAM_CLASSIFIER, "tests");
    protected static final String MAVEN_SETTINGS_NEXUS = "<settings xmlns=\"http://maven.apache.org/SETTINGS/1.0.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "  xsi:schemaLocation=\"http://maven.apache.org/SETTINGS/1.0.0 https://maven.apache.org/xsd/settings-1.0.0.xsd\">\n" +
            "    <profiles>\n" +
            "        <profile>\n" +
            "          <id>default</id>\n" +
            "          <repositories>\n" +
            "            <repository>\n" +
            "              <id>exense</id>\n" +
            "              <name>Exense</name>\n" +
            "              <url>https://nexus-enterprise.exense.ch/repository/container-dependency/</url>\n" +
            "            </repository>\n" +
            "          </repositories>\n" +
            "        </profile>\n" +
            "    </profiles>\n" +
            "    <activeProfiles>\n" +
            "        <activeProfile>default</activeProfile>\n" +
            "    </activeProfiles>\n" +
            "</settings>";
    protected MavenArtifactRepository artifactRepository;
    protected ExecutionContext executionContext;
    protected AutomationPackageManager apManager;

    protected void setup() {
        InMemoryControllerSettingAccessor controllerSettingAccessor = setupControllerSettingsAccessor();

        Configuration configuration = new Configuration();
        AutomationPackageHookRegistry hookRegistry = new AutomationPackageHookRegistry();
        AutomationPackageSerializationRegistry automationPackageSerializationRegistry = new AutomationPackageSerializationRegistry();
        AutomationPackageReader apReader = new JavaAutomationPackageReader(YamlAutomationPackageVersions.ACTUAL_JSON_SCHEMA_PATH, hookRegistry, automationPackageSerializationRegistry, configuration);
        AutomationPackageReaderRegistry automationPackageReaderRegistry = new AutomationPackageReaderRegistry(YamlAutomationPackageVersions.ACTUAL_JSON_SCHEMA_PATH, hookRegistry, automationPackageSerializationRegistry);
        automationPackageReaderRegistry.register(apReader);
        FunctionTypeRegistry functionTypeRegistry = prepareTestFunctionTypeRegistry();
        InMemoryFunctionAccessorImpl functionAccessor = new InMemoryFunctionAccessorImpl();
        LocalResourceManagerImpl resourceManager = new LocalResourceManagerImpl();
        this.apManager = AutomationPackageManager.createLocalAutomationPackageManager(functionTypeRegistry, functionAccessor, new InMemoryPlanAccessor(), resourceManager, automationPackageReaderRegistry, hookRegistry);
        artifactRepository = new MavenArtifactRepository(apManager, functionTypeRegistry, functionAccessor, configuration, controllerSettingAccessor, resourceManager);

        // mock the context, which is normally prepared via FunctionPlugin
        executionContext = ExecutionEngine.builder().build().newExecutionContext();
        executionContext.put(FunctionAccessor.class, new InMemoryFunctionAccessorImpl());
        executionContext.put(FunctionTypeRegistry.class, functionTypeRegistry);
    }

    protected void cleanup() {
        if (apManager != null) {
            apManager.cleanup();
        }
        if (executionContext != null) {
            try {
                executionContext.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    protected abstract InMemoryControllerSettingAccessor setupControllerSettingsAccessor();

    public static FunctionTypeRegistry prepareTestFunctionTypeRegistry() {
        FunctionTypeRegistry functionTypeRegistry = Mockito.mock(FunctionTypeRegistry.class);

        Configuration configuration = new Configuration();
        AbstractFunctionType<?> jMeterFunctionType = new JMeterFunctionType(configuration);
        AbstractFunctionType<?> generalScriptFunctionType = new GeneralScriptFunctionType(configuration);
        AbstractFunctionType<?> compositeFunctionType = new CompositeFunctionType(new ObjectHookRegistry());
        AbstractFunctionType<?> nodeFunctionType = new NodeFunctionType();

        Mockito.when(functionTypeRegistry.getFunctionTypeByFunction(Mockito.any())).thenAnswer(invocationOnMock -> {
            Object function = invocationOnMock.getArgument(0);
            if (function instanceof JMeterFunction) {
                return jMeterFunctionType;
            } else if (function instanceof GeneralScriptFunction) {
                return generalScriptFunctionType;
            } else if (function instanceof CompositeFunction){
                return compositeFunctionType;
            } else if (function instanceof NodeFunction){
                return nodeFunctionType;
            }
            else {
                return null;
            }
        });
        return functionTypeRegistry;
    }
}
