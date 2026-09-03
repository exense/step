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
package step.automation.packages.yaml.migrations;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

import step.automation.packages.AutomationPackageReadingException;
import step.automation.packages.deserialization.AutomationPackageSerializationRegistry;
import step.automation.packages.yaml.AutomationPackageDescriptorReader;
import step.automation.packages.yaml.YamlAutomationPackageVersions;
import step.automation.packages.yaml.model.AutomationPackageDescriptorYaml;
import step.core.scheduler.automation.AutomationPackageSchedule;
import step.core.scheduler.automation.AutomationPackageScheduleRegistration;
import step.parameter.automation.AutomationPackageParameter;
import step.parameter.automation.AutomationPackageParameterJsonSchema;
import step.plans.parser.yaml.YamlPlan;

/**
 * The values of an automation package written against a schema older than 1.3.0 were used literally. After the
 * introduction of the string interpolation they must keep resolving to exactly what they did. Declaring the current
 * schema version is how an author opts into the interpolation.
 */
public class EscapeStringInterpolationInParametersMigrationTaskTest {

    private static final String COMPLETE_DESCRIPTOR =
        "src/test/resources/step/automation/packages/yaml/descriptors/completeDescriptor.yml";

    private final AutomationPackageDescriptorReader reader;

    public EscapeStringInterpolationInParametersMigrationTaskTest() {
        AutomationPackageSerializationRegistry serializationRegistry = new AutomationPackageSerializationRegistry();
        AutomationPackageScheduleRegistration.registerSerialization(serializationRegistry);
        serializationRegistry.register(AutomationPackageParameterJsonSchema.FIELD_NAME_IN_AP, AutomationPackageParameter.class);
        reader = new AutomationPackageDescriptorReader(YamlAutomationPackageVersions.ACTUAL_JSON_SCHEMA_PATH, serializationRegistry);
    }

    @Test
    public void testOldPackageKeepsItsLiteralParameterValues() throws Exception {
        List<AutomationPackageParameter> parameters = parametersOf(descriptorWithParameters("1.2.0"));

        // Escaped, so that the value resolves back to exactly what it was before the upgrade
        Assert.assertEquals("http://$${host}:8080", parameters.get(0).getValue().getValue());
        // Nothing significant, left strictly untouched
        Assert.assertEquals("plain value", parameters.get(1).getValue().getValue());
        Assert.assertEquals("pid $$", parameters.get(2).getValue().getValue());
    }

    @Test
    public void testNewPackageParameterValuesAreNotEscaped() throws Exception {
        List<AutomationPackageParameter> parameters = parametersOf(descriptorWithParameters("1.3.0"));

        Assert.assertEquals("http://${host}:8080", parameters.get(0).getValue().getValue());
        Assert.assertEquals("plain value", parameters.get(1).getValue().getValue());
        Assert.assertEquals("pid $$", parameters.get(2).getValue().getValue());
    }

    /**
     * A package which declares no version at all is considered to be written against the current schema, following
     * the convention already used by the migrations of the yaml plan format
     */
    @Test
    public void testPackageWithoutVersionIsNotMigrated() throws Exception {
        List<AutomationPackageParameter> parameters = parametersOf(descriptorWithParameters(null));
        Assert.assertEquals("http://${host}:8080", parameters.get(0).getValue().getValue());
    }

    /**
     * Values written as an expression were already evaluated as groovy before the interpolation existed and must not
     * be escaped
     */
    @Test
    public void testExpressionParameterValuesAreNotEscaped() throws Exception {
        AutomationPackageDescriptorYaml descriptor = readDescriptor(
            "version: 1.2.0\n" +
            "name: \"expressions\"\n" +
            "parameters:\n" +
            "  - key: \"url\"\n" +
            "    value:\n" +
            "      expression: '\"http://\" + host'\n");

        AutomationPackageParameter parameter = parametersOf(descriptor).get(0);
        Assert.assertTrue(parameter.getValue().isDynamic());
        Assert.assertEquals("\"http://\" + host", parameter.getValue().getExpression());
    }

    /**
     * Migrating means converting the whole file to a generic document and back. Everything the migration doesn't
     * touch has to survive that round trip untouched
     */
    @Test
    public void testMigrationPreservesTheRestOfTheDescriptor() throws Exception {
        String original = Files.readString(Path.of(COMPLETE_DESCRIPTOR), StandardCharsets.UTF_8);
        // The descriptor declares the current version, so reading it as is applies no migration
        String olderVersion = original.replaceFirst("(?m)^version:.*$", "version: 1.2.0");
        Assert.assertNotEquals("The fixture is expected to declare a version", original, olderVersion);

        AutomationPackageDescriptorYaml notMigrated = readDescriptor(original);
        AutomationPackageDescriptorYaml migrated = readDescriptor(olderVersion);

        Assert.assertEquals(notMigrated.getName(), migrated.getName());
        Assert.assertEquals(notMigrated.getFragments(), migrated.getFragments());

        // Keywords keep their type and their content, which is what the round trip through a generic document
        // is most likely to damage
        Assert.assertEquals(1, migrated.getKeywords().size());
        Assert.assertEquals(
            notMigrated.getKeywords().get(0).getYamlKeyword().getClass(),
            migrated.getKeywords().get(0).getYamlKeyword().getClass());
        Assert.assertEquals(
            notMigrated.getKeywords().get(0).getYamlKeyword().getName(),
            migrated.getKeywords().get(0).getYamlKeyword().getName());
        Assert.assertEquals(
            notMigrated.getKeywords().get(0).getYamlKeyword().getSchema(),
            migrated.getKeywords().get(0).getYamlKeyword().getSchema());

        Assert.assertEquals(planNames(notMigrated), planNames(migrated));

        List<AutomationPackageSchedule> notMigratedSchedules = notMigrated.getAdditionalField(AutomationPackageSchedule.FIELD_NAME_IN_AP);
        List<AutomationPackageSchedule> migratedSchedules = migrated.getAdditionalField(AutomationPackageSchedule.FIELD_NAME_IN_AP);
        Assert.assertEquals(notMigratedSchedules.size(), migratedSchedules.size());
        Assert.assertEquals(notMigratedSchedules.get(0).getCron(), migratedSchedules.get(0).getCron());
        Assert.assertEquals(notMigratedSchedules.get(0).getPlanName(), migratedSchedules.get(0).getPlanName());
    }

    private static List<String> planNames(AutomationPackageDescriptorYaml descriptor) {
        return descriptor.getPlans().stream().map(YamlPlan::getName).collect(Collectors.toList());
    }

    private AutomationPackageDescriptorYaml descriptorWithParameters(String version) throws Exception {
        return readDescriptor((version == null ? "" : "version: " + version + "\n") +
            "name: \"parameters\"\n" +
            "parameters:\n" +
            "  - key: \"url\"\n" +
            "    value: \"http://${host}:8080\"\n" +
            "  - key: \"plain\"\n" +
            "    value: \"plain value\"\n" +
            "  - key: \"shell\"\n" +
            "    value: \"pid $$\"\n");
    }

    private AutomationPackageDescriptorYaml readDescriptor(String yaml) throws AutomationPackageReadingException, IOException {
        try (InputStream is = new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8))) {
            return reader.readAutomationPackageDescriptor(is, "test");
        }
    }

    private static List<AutomationPackageParameter> parametersOf(AutomationPackageDescriptorYaml descriptor) {
        return descriptor.getAdditionalField(AutomationPackageParameterJsonSchema.FIELD_NAME_IN_AP);
    }
}
