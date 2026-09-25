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
package step.automation.packages.yaml;

import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import org.apache.commons.lang3.StringUtils;
import org.everit.json.schema.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.artefacts.handlers.JsonSchemaValidator;
import step.automation.packages.AutomationPackageReadingException;
import step.automation.packages.deserialization.AutomationPackageSerializationRegistry;
import step.automation.packages.yaml.migrations.AbstractAutomationPackageMigrationTask;
import step.automation.packages.yaml.migrations.AutomationPackageMigration;
import step.automation.packages.yaml.model.AutomationPackageDescriptorYaml;
import step.automation.packages.yaml.model.AutomationPackageDescriptorYamlImpl;
import step.automation.packages.yaml.model.AutomationPackageFragmentYaml;
import step.automation.packages.yaml.model.AutomationPackageFragmentYamlImpl;
import step.core.Version;
import step.core.accessors.AbstractIdentifiableObject;
import step.core.accessors.DefaultJacksonMapperProvider;
import step.core.collections.Collection;
import step.core.collections.CollectionFactory;
import step.core.collections.Document;
import step.core.collections.Filters;
import step.core.collections.inmemory.InMemoryCollectionFactory;
import step.core.scanner.AnnotationScanner;
import step.core.yaml.PatchingContext;
import step.core.yaml.deserialization.PatchableYamlList;
import step.core.yaml.deserialization.PatchingParserDelegate;
import step.migration.MigrationManager;
import step.plans.parser.yaml.YamlPlanReader;
import step.plans.parser.yaml.model.YamlPlanVersions;
import step.plans.parser.yaml.schema.YamlPlanValidationException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

import static step.automation.packages.yaml.migrations.AbstractAutomationPackageMigrationTask.AUTOMATION_PACKAGE_DESCRIPTORS_COLLECTION_NAME;

public class AutomationPackageDescriptorReader {

    private static final Logger log = LoggerFactory.getLogger(AutomationPackageDescriptorReader.class);

    private final ObjectMapper yamlObjectMapper;

    private final YamlPlanReader planReader;

    private final AutomationPackageSerializationRegistry serializationRegistry;

    private String jsonSchema;

    private final MigrationManager migrationManager;

    public AutomationPackageDescriptorReader(String jsonSchemaPath, AutomationPackageSerializationRegistry serializationRegistry) {
        this.serializationRegistry = serializationRegistry;
        // TODO: we need to find a way to resolve the actual json schema (controller config) depending on running server instance (EE or OS)
        // TODO: also we have to resolve the json version for plans according to the automation package version!
        this.planReader = new YamlPlanReader(YamlPlanVersions.ACTUAL_VERSION, false, null);
        this.yamlObjectMapper = createYamlObjectMapper();
        this.migrationManager = initMigrationManager();

        if (jsonSchemaPath != null) {
            this.jsonSchema = readJsonSchema(jsonSchemaPath);
        }
    }

    public AutomationPackageDescriptorYaml readAutomationPackageDescriptor(InputStream yamlDescriptor, String packageName) throws AutomationPackageReadingException {
        log.info("Reading automation package descriptor...");
        return readAutomationPackageYamlFile("automation-package.yml", yamlDescriptor, getDescriptorClass(), packageName);
    }

    protected Class<? extends AutomationPackageDescriptorYaml> getDescriptorClass() {
        return AutomationPackageDescriptorYamlImpl.class;
    }

    /**
     * @param packageVersion the schema version declared by the automation package importing this fragment. Fragments
     *                       usually declare no version of their own and follow the one of their package, which is
     *                       what decides whether the migrations apply to them
     */
    public AutomationPackageFragmentYaml readAutomationPackageFragment(InputStream yamlFragment, String fragmentName, String packageName, String packageVersion) throws AutomationPackageReadingException {
        log.info("Reading automation package descriptor fragment ({})...", fragmentName);
        return readAutomationPackageYamlFile(fragmentName, yamlFragment, getFragmentClass(), packageName, packageVersion);
    }

    protected Class<? extends AutomationPackageFragmentYaml> getFragmentClass() {
        return AutomationPackageFragmentYamlImpl.class;
    }

    protected <T extends AutomationPackageFragmentYaml> T readAutomationPackageYamlFile(String location, InputStream yaml, Class<T> targetClass, String packageName) throws AutomationPackageReadingException {
        return readAutomationPackageYamlFile(location, yaml, targetClass, packageName, null);
    }

    protected <T extends AutomationPackageFragmentYaml> T readAutomationPackageYamlFile(String location, InputStream yaml, Class<T> targetClass, String packageName, String inheritedVersion) throws AutomationPackageReadingException {
        try {
            String yamlDescriptorString = new String(yaml.readAllBytes(), StandardCharsets.UTF_8);
            String version = null;
            if (jsonSchema != null) {
                try {
                    version = JsonSchemaValidator.validate(jsonSchema, yamlObjectMapper.readTree(yamlDescriptorString).toString());
                } catch (Exception ex) {
                    // add error details
                    String message = ex.getMessage();
                    if (ex instanceof ValidationException) {
                        message = message + " " + ((ValidationException) ex).getAllMessages();
                    }
                    throw new YamlPlanValidationException(message, ex);
                }
            }

            if (version == null) {
                // A fragment declaring no version of its own follows the one of the package importing it
                version = inheritedVersion;
            }

            yamlDescriptorString = migrateIfRequired(yamlDescriptorString, version);

            PatchingContext context = new PatchingContext(location, yamlDescriptorString, yamlObjectMapper);
            PatchingParserDelegate parser = new PatchingParserDelegate(yamlObjectMapper.createParser(yamlDescriptorString), context);

            Map<Class<?>, Object> injections = new HashMap<>();
            injections.put(AutomationPackageSerializationRegistry.class, serializationRegistry);
            injections.put(PatchingContext.class, context);
            injections.put(ObjectMapper.class, yamlObjectMapper);

            InjectableValues.Std injectableValues = new InjectableValues.Std();
            injections.forEach(injectableValues::addValue);

            yamlObjectMapper.setInjectableValues(injectableValues);

            T res = yamlObjectMapper.reader()
                .withAttributes(injections)
                .withAttribute("version", version)
                .readValue(parser, targetClass);

            res.setPatchingContext(context);
            logAfterRead(packageName, res);
            return res;
        } catch (IOException | YamlPlanValidationException e) {
            throw new AutomationPackageReadingException("Unable to read the automation package yaml. Caused by: " + e.getMessage(), e);
        }
    }

    protected <T extends AutomationPackageFragmentYaml> void logAfterRead(String packageName, T res) {
        if (!res.getKeywords().isEmpty()) {
            log.info("{} keyword(s) found in automation package {}", res.getKeywords().size(), StringUtils.defaultString(packageName));
        }
        if (!res.getPlans().isEmpty()) {
            log.info("{} plan(s) found in automation package {}", res.getPlans().size(), StringUtils.defaultString(packageName));
        }
        if (!res.getPlansPlainText().isEmpty()) {
            log.info("{} plain text plan(s) found in automation package {}", res.getPlansPlainText().size(), StringUtils.defaultString(packageName));
        }
        for (Map.Entry<String, PatchableYamlList<?>> additionalEntry : res.getAdditionalFields().entrySet()) {
            log.info("{} {} found in automation package {}", additionalEntry.getValue().size(), additionalEntry.getKey(), StringUtils.defaultString(packageName));
        }
        if (!res.getFragments().isEmpty()) {
            log.info("{} imported fragment(s) found in automation package {}", res.getFragments().size(), StringUtils.defaultString(packageName));
        }
    }

    /**
     * Applies the migrations of the automation package format to a descriptor or fragment declaring an older schema
     * version. This is a generic method that applies to all entities. Plans can also be migrated by the yaml plan reader specificallDy.
     *
     * @param yamlFile the yaml content read from the file
     * @param version  the schema version declared by the file. A null version means that no migration is required,
     *                 which is also the case of the files not declaring any version at all
     * @return the migrated yaml content, or the content unchanged when no migration applies
     */
    protected String migrateIfRequired(String yamlFile, String version) throws IOException {
        if (version == null) {
            return yamlFile;
        }
        Version fileVersion = new Version(version);
        if (fileVersion.compareTo(YamlAutomationPackageVersions.ACTUAL_VERSION) == 0) {
            return yamlFile;
        }

        log.info("Migrating automation package file from version {} to {}", version, YamlAutomationPackageVersions.ACTUAL_VERSION);

        CollectionFactory tempCollectionFactory = new InMemoryCollectionFactory(new Properties());
        Collection<Document> tempCollection = tempCollectionFactory.getCollection(AUTOMATION_PACKAGE_DESCRIPTORS_COLLECTION_NAME, Document.class);
        Document savedDocument = tempCollection.save(yamlObjectMapper.readValue(yamlFile, Document.class));

        migrationManager.migrate(tempCollectionFactory, fileVersion, YamlAutomationPackageVersions.ACTUAL_VERSION);

        Document migratedDocument = tempCollection.find(Filters.id(savedDocument.getId()), null, null, null, 0).findFirst().orElseThrow();
        // The declared version is deliberately left untouched: it is what the imported fragments inherit, and the
        // migrated content isn't validated again. Only the id, generated when saving into the temporary collection,
        // has to be removed
        migratedDocument.remove(AbstractIdentifiableObject.ID);

        return "---\n" + yamlObjectMapper.writeValueAsString(migratedDocument);
    }

    /**
     * Initializes the migration manager with the migrations of the automation package format
     */
    protected MigrationManager initMigrationManager() {
        MigrationManager migrationManager = new MigrationManager();
        try (AnnotationScanner annotationScanner = AnnotationScanner.forAllClassesFromClassLoader(AutomationPackageMigration.LOCATION, Thread.currentThread().getContextClassLoader())) {
            for (Class<?> migration : annotationScanner.getClassesWithAnnotation(AutomationPackageMigration.class)) {
                if (!AbstractAutomationPackageMigrationTask.class.isAssignableFrom(migration)) {
                    throw new IllegalArgumentException("Class " + migration + " doesn't extend the " + AbstractAutomationPackageMigrationTask.class);
                }
                migrationManager.register((Class<? extends AbstractAutomationPackageMigrationTask>) migration);
            }
        }
        return migrationManager;
    }

    protected String readJsonSchema(String jsonSchemaPath) {
        try (InputStream jsonSchemaInputStream = this.getClass().getClassLoader().getResourceAsStream(jsonSchemaPath)) {
            if (jsonSchemaInputStream == null) {
                throw new IllegalStateException("Json schema not found: " + jsonSchemaPath);
            }
            return new String(jsonSchemaInputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load json schema: " + jsonSchemaPath, e);
        }
    }

    private ObjectMapper createYamlObjectMapper() {
        ObjectMapper yamlMapper = createBasicYamlObjectMapper();

        // register deserializers to read yaml plans
        SimpleModule module = planReader.registerAllSerializersAndDeserializers(yamlMapper, true);
        yamlMapper.registerModule(module);

        return yamlMapper;
    }

    // Below are a few static convenience methods for others who may need access to some information about
    // an AP, without requiring all the heavy lifting of actually being able to load and parse the entire AP.

    public static ObjectMapper createBasicYamlObjectMapper() {
        YAMLFactory yamlFactory = new YAMLFactory();

        // Disable native type id to enable conversion to generic Documents
        yamlFactory.disable(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID);
        yamlFactory.enable(YAMLGenerator.Feature.INDENT_ARRAYS_WITH_INDICATOR);
        yamlFactory.disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER);
        return DefaultJacksonMapperProvider.getObjectMapper(yamlFactory);
    }

    public static String getAutomationPackageName(Path apDescriptorFile) throws IOException {
        try (InputStream stream = Files.newInputStream(Objects.requireNonNull(apDescriptorFile))) {
            return getAutomationPackageName(stream);
        }
    }

    public static String getAutomationPackageName(InputStream apDescriptorInputStream) throws IOException {
        var mapper = createBasicYamlObjectMapper();
        return Optional.ofNullable(mapper.readTree(apDescriptorInputStream))
            .map(rootNode -> rootNode.get("name"))
            .map(JsonNode::asText)
            .orElse(null);
    }

    public YamlPlanReader getPlanReader() {
        return this.planReader;
    }

    public String getJsonSchema() {
        return jsonSchema;
    }
}
