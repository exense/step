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
import org.bson.types.ObjectId;
import org.everit.json.schema.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.artefacts.handlers.JsonSchemaValidator;
import step.automation.packages.AutomationPackageReadingException;
import step.automation.packages.deserialization.AutomationPackageSerializationRegistry;
import step.automation.packages.model.AutomationPackageKeyword;
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
import step.core.collections.DocumentObject;
import step.core.collections.Filters;
import step.core.collections.inmemory.InMemoryCollectionFactory;
import step.core.scanner.AnnotationScanner;
import step.core.yaml.PatchingContext;
import step.core.yaml.YamlModelUtils;
import step.core.yaml.deserialization.PatchableYamlList;
import step.core.yaml.deserialization.PatchingParserDelegate;
import step.migration.MigrationManager;
import step.migration.MigrationTask;
import step.plans.parser.yaml.YamlPlanReader;
import step.plans.parser.yaml.deserializers.UpgradableYamlPlanDeserializer;
import step.plans.parser.yaml.migrations.AbstractYamlPlanMigrationTask;
import step.plans.parser.yaml.migrations.YamlPlanMigration;
import step.plans.parser.yaml.model.YamlPlanVersions;
import step.plans.parser.yaml.schema.YamlPlanValidationException;
import step.plugins.functions.types.automation.YamlCompositeFunction;

import static step.automation.packages.yaml.migrations.AbstractAutomationPackageMigrationTask.AUTOMATION_PACKAGE_DESCRIPTORS_COLLECTION_NAME;
import static step.plans.parser.yaml.migrations.AbstractYamlPlanMigrationTask.YAML_PLANS_COLLECTION_NAME;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

public class AutomationPackageDescriptorReader {

    private static final Logger log = LoggerFactory.getLogger(AutomationPackageDescriptorReader.class);

    private final ObjectMapper yamlObjectMapper;
    private static final String DEFAULT_DESCRIPTOR_LOCATION = "automation package descriptor";
    private static final String PLANS = "plans";
    private static final String KEYWORDS = AutomationPackageKeyword.KEYWORDS_ENTITY_NAME;
    private static final String COMPOSITE_KEYWORD = YamlModelUtils.getEntityNameByClass(YamlCompositeFunction.class);
    private static final String COMPOSITE_PLAN = "plan";

    private final YamlPlanReader planReader;

    private final AutomationPackageSerializationRegistry serializationRegistry;

    private String jsonSchema;

    private final MigrationManager migrationManager;

    public AutomationPackageDescriptorReader(String jsonSchemaPath, AutomationPackageSerializationRegistry serializationRegistry) {
        this.serializationRegistry = serializationRegistry;
        // TODO: we need to find a way to resolve the actual json schema (controller config) depending on running server instance (EE or OS)
        this.planReader = new YamlPlanReader(YamlPlanVersions.ACTUAL_VERSION, false, null);
        this.yamlObjectMapper = createYamlObjectMapper();
        this.migrationManager = initMigrationManager();

        if (jsonSchemaPath != null) {
            this.jsonSchema = readJsonSchema(jsonSchemaPath);
        }
    }

    public AutomationPackageDescriptorYaml readAutomationPackageDescriptor(InputStream yamlDescriptor, String packageName) throws AutomationPackageReadingException {
        return readAutomationPackageDescriptor(yamlDescriptor, DEFAULT_DESCRIPTOR_LOCATION, packageName);
    }

    /**
     * @param location where the descriptor is read from, for instance its url, used to identify it in the logs. May be null when unknown
     */
    public AutomationPackageDescriptorYaml readAutomationPackageDescriptor(InputStream yamlDescriptor, String location, String packageName) throws AutomationPackageReadingException {
        if (location == null) {
            location = DEFAULT_DESCRIPTOR_LOCATION;
        }
        log.info("Reading automation package descriptor ({})...", location);
        return readAutomationPackageYamlFile(location, yamlDescriptor, getDescriptorClass(), packageName);
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
            Document yamlDocument = yamlObjectMapper.readValue(yamlDescriptorString, Document.class);

            // A fragment declaring no version of its own follows the one of the package importing it
            String version = yamlDocument == null ? null : yamlDocument.getString(AutomationPackageDescriptorYaml.VERSION_FIELD_NAME);
            if (version == null) {
                version = inheritedVersion;
            }

            // The file is migrated before doing the schema validation since we can only validate against the current version
            Document migratedDocument = migrateIfRequired(yamlDocument, version);
            if (migratedDocument != null) {
                yamlDescriptorString =  "---\n" + yamlObjectMapper.writeValueAsString(migratedDocument);
            }

            if (jsonSchema != null) {
                try {
                    JsonSchemaValidator.validate(jsonSchema, yamlObjectMapper.readTree(yamlDescriptorString).toString());
                } catch (Exception ex) {
                    // add error details
                    String message = ex.getMessage();
                    if (ex instanceof ValidationException) {
                        message = message + " " + ((ValidationException) ex).getAllMessages();
                    }
                    throw new YamlPlanValidationException(message, ex);
                }
            }

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
                .readValue(parser, targetClass);

            if (res == null) {
                throw new AutomationPackageReadingException("Unable to read the automation package yaml: the content is empty");
            }

            res.setPatchingContext(context);
            res.setEffectiveVersion(version);
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
     * Applies the migrations to a descriptor or fragment declaring an older schema version: the migrations of the
     * automation package descriptor (except plans) apply tasks annotated with {@link AutomationPackageMigration}, and
     * the migrations of the yaml plan  applies tasks annotated with {@link YamlPlanMigration}. YamlPlanMigration
     * are managed separately  because they are also applied when creating a plan from a YAML source via {@link UpgradableYamlPlanDeserializer}
     *
     * @param yamlDocument the content read from the file
     * @param version      the schema version declared by the file. A null version means that the file is written
     *                     against the current schema, which is also the case of the files not declaring any version
     * @return the migrated content, or null when no migration applies
     */
    protected Document migrateIfRequired(Document yamlDocument, String version) {
        if (yamlDocument == null || version == null) {
            return null;
        }
        Version fileVersion = new Version(version);
        if (fileVersion.compareTo(YamlAutomationPackageVersions.ACTUAL_VERSION) == 0) {
            return null;
        }

        log.info("Migrating automation package file from version {} to {}", version, YamlAutomationPackageVersions.ACTUAL_VERSION);

        // First step: the descriptor itself.
        Document migratedDocument = migrate(AUTOMATION_PACKAGE_DESCRIPTORS_COLLECTION_NAME, List.of(yamlDocument), fileVersion).getFirst();

        // Second step: its plans
        List<DocumentObject> plans = yamlDocument.getArray(PLANS);
        if (plans != null) {
            // Emptied rather than removed, so that the migrated plans are put back at the same place in the file
            yamlDocument.put(PLANS, new ArrayList<>());
        }
        if (plans != null) {
            List<Document> planDocuments = plans.stream().map(Document::new).collect(Collectors.toList());
            migratedDocument.put(PLANS, migrate(YAML_PLANS_COLLECTION_NAME, planDocuments, fileVersion));
        }

        // Third step: the plans of the composite keywords, put back in their keyword
        Map<Integer, Document> compositePlans = detachCompositePlans(yamlDocument);
        if (!compositePlans.isEmpty()) {
            Iterator<Document> migratedCompositePlans = migrate(YAML_PLANS_COLLECTION_NAME, List.copyOf(compositePlans.values()), fileVersion).iterator();
            List<DocumentObject> keywords = migratedDocument.getArray(KEYWORDS);
            for (Integer keywordIndex : compositePlans.keySet()) {
                keywords.get(keywordIndex).getObject(COMPOSITE_KEYWORD).put(COMPOSITE_PLAN, migratedCompositePlans.next());
            }
        }
        return migratedDocument;
    }

    /**
     * Removes the plans of the composite keywords from the document
     *
     * @return the removed plans, by index of their keyword and in the order of the keywords
     */
    @SuppressWarnings("unchecked")
    private static Map<Integer, Document> detachCompositePlans(Document yamlDocument) {
        Map<Integer, Document> compositePlans = new LinkedHashMap<>();
        if (yamlDocument.get(KEYWORDS) instanceof List<?> keywords) {
            for (int i = 0; i < keywords.size(); i++) {
                if (keywords.get(i) instanceof Map<?, ?> keyword
                    && keyword.get(COMPOSITE_KEYWORD) instanceof Map<?, ?> composite
                    && composite.get(COMPOSITE_PLAN) instanceof Map<?, ?> plan) {
                    compositePlans.put(i, new Document((Map<String, Object>) plan));
                    composite.remove(COMPOSITE_PLAN);
                }
            }
        }
        return compositePlans;
    }

    /**
     * Migrates documents in a temporary collection of their own, which decides the migrations applying to them: the
     * ones of the automation package format for {@link AbstractAutomationPackageMigrationTask#AUTOMATION_PACKAGE_DESCRIPTORS_COLLECTION_NAME},
     * the ones of the yaml plan format for {@link AbstractYamlPlanMigrationTask#YAML_PLANS_COLLECTION_NAME}
     *
     * @return the migrated documents, in the same order and without the ids generated by the temporary collection
     */
    private List<Document> migrate(String collectionName, List<Document> documents, Version fromVersion) {
        CollectionFactory tempCollectionFactory = new InMemoryCollectionFactory(new Properties());
        Collection<Document> collection = tempCollectionFactory.getCollection(collectionName, Document.class);
        List<ObjectId> ids = new ArrayList<>();
        for (Document document : documents) {
            ids.add(collection.save(document).getId());
        }

        migrationManager.migrate(tempCollectionFactory, fromVersion, YamlAutomationPackageVersions.ACTUAL_VERSION);

        List<Document> migratedDocuments = new ArrayList<>();
        for (ObjectId id : ids) {
            migratedDocuments.add(findAndRemoveId(collection, id));
        }
        return migratedDocuments;
    }

    private static Document findAndRemoveId(Collection<Document> collection, ObjectId id) {
        Document document = collection.find(Filters.id(id), null, null, null, 0).findFirst().orElseThrow();
        document.remove(AbstractIdentifiableObject.ID);
        return document;
    }

    /**
     * Initializes the migration manager with the migrations of the automation package format and of the yaml plan
     * format, the latter applying to the plans of the automation packages
     */
    protected MigrationManager initMigrationManager() {
        MigrationManager migrationManager = new MigrationManager();
        registerMigrations(migrationManager, AutomationPackageMigration.LOCATION, AutomationPackageMigration.class, AbstractAutomationPackageMigrationTask.class);
        registerMigrations(migrationManager, YamlPlanMigration.LOCATION, YamlPlanMigration.class, AbstractYamlPlanMigrationTask.class);
        return migrationManager;
    }

    @SuppressWarnings("unchecked")
    private static void registerMigrations(MigrationManager migrationManager, String location, Class<? extends Annotation> annotation, Class<? extends MigrationTask> taskClass) {
        try (AnnotationScanner annotationScanner = AnnotationScanner.forAllClassesFromClassLoader(location, Thread.currentThread().getContextClassLoader())) {
            for (Class<?> migration : annotationScanner.getClassesWithAnnotation(annotation)) {
                if (!taskClass.isAssignableFrom(migration)) {
                    throw new IllegalArgumentException("Class " + migration + " doesn't extend the " + taskClass);
                }
                migrationManager.register((Class<? extends MigrationTask>) migration);
            }
        }
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
        SimpleModule module = planReader.registerAllSerializersAndDeserializers(yamlMapper, false);
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

    /**
     * Reads only the schema version declared by a descriptor, without validating it against the schema, which requires
     * to know that version first
     *
     * @return the declared version, null if the descriptor declares none
     */
    public String readDeclaredSchemaVersion(InputStream apDescriptorInputStream) throws IOException {
        return Optional.ofNullable(yamlObjectMapper.readTree(apDescriptorInputStream))
            .map(rootNode -> rootNode.get(AutomationPackageFragmentYaml.VERSION_FIELD_NAME))
            .filter(versionNode -> !versionNode.isNull())
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
