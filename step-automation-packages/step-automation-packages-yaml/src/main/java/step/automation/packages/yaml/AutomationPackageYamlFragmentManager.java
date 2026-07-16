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
package step.automation.packages.yaml;

import org.apache.commons.io.FileUtils;
import step.automation.packages.ResourcePathMatchingResolver;
import step.automation.packages.StagingAutomationPackageContext;
import step.automation.packages.mappers.interfaces.BusinessObjectToYamlMapper;
import step.automation.packages.mappers.interfaces.BusinessObjectToYamlMapping;
import step.automation.packages.mappers.interfaces.YamlToBusinessObjectMapper;
import step.automation.packages.mappers.interfaces.YamlToBusinessObjectMapping;
import step.automation.packages.yaml.model.AutomationPackageDescriptorYaml;
import step.automation.packages.yaml.model.AutomationPackageFragmentYaml;
import step.automation.packages.yaml.model.AutomationPackageFragmentYamlImpl;
import step.core.accessors.AbstractOrganizableObject;
import step.core.scanner.CachedAnnotationScanner;
import step.core.yaml.NamedPatchableYamlModel;
import step.core.yaml.PatchableYamlModel;
import step.core.yaml.PatchingContext;
import step.core.yaml.deserialization.AutomationPackageConcurrentEditException;
import step.core.yaml.deserialization.AutomationPackagePerObjectSaveUnsupportedException;
import step.core.yaml.deserialization.AutomationPackageUpdateException;
import step.core.yaml.deserialization.PatchableYamlList;
import step.core.yaml.deserialization.PatchableYamlPrimitive;
import step.plans.parser.yaml.YamlPlanReader;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AutomationPackageYamlFragmentManager {

    protected final Path apRoot;
    private final ResourcePathMatchingResolver resourcePathMatchingResolver;

    public enum NewObjectFragmentMode {
        /**
         * Write new objects into fragment with fixed path. PATH indicates fragment yaml. Default: default is [ap field name].yml
         */
        FRAGMENT,
        /**
         * Write new objects into new fragment, fragment name is given by object name. PATH indicates subfolder of fragment, default is [ap field name].
         */
        PER_OBJECT,
    }

    public static final String PROPERTY_NEW_OBJECT_FRAGMENT_MODE = "newFragmentPaths.%s.mode";
    public static final String PROPERTY_NEW_OBJECT_FRAGMENT_PATH = "newFragmentPaths.%s.path";

    protected final Map<AbstractOrganizableObject, PatchableYamlModel> patchableMap = new ConcurrentHashMap<>();
    protected final Map<AbstractOrganizableObject, AutomationPackageFragmentYaml> fragmentMap = new ConcurrentHashMap<>();
    protected final Set<AutomationPackageFragmentYaml> fragments;

    protected Properties properties = new Properties();
    public final AutomationPackageFragmentYaml descriptorYaml;

    private final Map<Class<?>, BusinessObjectToYamlMapper<?, ?>> businessObjectToYamlMappers;

    public AutomationPackageYamlFragmentManager(ResourcePathMatchingResolver resourcePathMatchingResolver, AutomationPackageDescriptorYaml descriptorYaml, Set<AutomationPackageFragmentYaml> fragments, AutomationPackageDescriptorReader descriptorReader, StagingAutomationPackageContext stagingContext) {
        this.resourcePathMatchingResolver = resourcePathMatchingResolver;
        this.descriptorYaml = descriptorYaml;

        apRoot = descriptorYaml.getFragmentPath().getParent();

        Map<Class<?>, Object> injectables = new HashMap<>();

        injectables.put(YamlPlanReader.class, descriptorReader.getPlanReader());
        injectables.put(StagingAutomationPackageContext.class, stagingContext);

        businessObjectToYamlMappers = createBusinessObjectToYamlMappers(injectables);
        Collection<YamlToBusinessObjectMapper<?, ?>> yamlToBusinessObjectMappers = createYamlToBusinessObjectMappers(injectables);

        initializeMaps(descriptorYaml, yamlToBusinessObjectMappers);

        this.fragments = fragments;

        fragments.stream()
            .filter(f -> f != descriptorYaml)
            .forEach(f -> initializeMaps(f, yamlToBusinessObjectMappers));
    }

    private Map<Class<?>, BusinessObjectToYamlMapper<?, ?>> createBusinessObjectToYamlMappers(Map<Class<?>, Object> injectables) {
        var mappers = new HashMap<Class<?>, BusinessObjectToYamlMapper<?, ?>>();

        for (Class<?> annotatedClass : CachedAnnotationScanner.getClassesWithAnnotation(BusinessObjectToYamlMapping.class)) {
            BusinessObjectToYamlMapping annotation = annotatedClass.getAnnotation(BusinessObjectToYamlMapping.class);
            mappers.put(annotation.sourceClass(), instantiateWithInjectables(BusinessObjectToYamlMapping.class, annotatedClass, injectables));
        }

        return mappers;
    }

    private Collection<YamlToBusinessObjectMapper<?, ?>> createYamlToBusinessObjectMappers(Map<Class<?>, Object> injectables) {
        List<YamlToBusinessObjectMapper<?, ?>> list = new ArrayList<>();

        for (Class<?> annotatedClass : CachedAnnotationScanner.getClassesWithAnnotation(YamlToBusinessObjectMapping.class)) {
            list.add(instantiateWithInjectables(YamlToBusinessObjectMapping.class, annotatedClass, injectables));
        }

        return list;
    }

    // Instantiates a class found by scanning annotations; The class must have exactly one constructor
    // whose arguments can be found in the injectables map.
    @SuppressWarnings("unchecked")
    private <T> T instantiateWithInjectables(Class<?> annotationType, Class<?> annotatedClass, Map<Class<?>, Object> injectables) {
        var constructors = annotatedClass.getConstructors();
        if (constructors.length != 1) {
            throw new IllegalStateException("Expected exactly one constructor for @" + annotationType.getSimpleName() + "-annotated class "
                + annotatedClass.getName() + ", but found " + constructors.length);
        }

        var constructor = constructors[0];
        try {
            var parameters = Arrays.stream(constructor.getParameterTypes())
                .map(injectables::get)
                .toArray();

            return (T) constructor.newInstance(parameters);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void initializeMaps(AutomationPackageFragmentYaml fragment, Collection<YamlToBusinessObjectMapper<?, ?>> yamlObjectMappers) {
        for (YamlToBusinessObjectMapper<?, ?> mapper : yamlObjectMappers) {
            fragment.initializeMaps(mapper, patchableMap, fragmentMap);
        }
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    @SuppressWarnings("unchecked")
    public <BO extends AbstractOrganizableObject, T> Iterable<BO> getBusinessObjects(Class<T> boClass) {
        return patchableMap.keySet().stream()
            .filter(businessObject -> boClass.isAssignableFrom(businessObject.getClass()))
            .map(businessObject -> (BO) businessObject).collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    public synchronized <BO extends AbstractOrganizableObject, YO extends PatchableYamlModel> BO save(BO object) {

        BusinessObjectToYamlMapper<BO, YO> mapper = (BusinessObjectToYamlMapper<BO, YO>) businessObjectToYamlMappers.get(object.getClass());
        if (mapper == null) {
            throw new AutomationPackageUpdateException("No BusinessObjectToYamlMapper registered for class: " + object.getClass().getName());
        }
        YO newYamlObject = mapper.toYamlObject(object);

        AutomationPackageFragmentYaml fragment = fragmentMap.get(object);

        if (fragment == null) {
            fragment = fragmentForNewObject(newYamlObject, mapper.getCollectionName());
            fragmentMap.put(object, fragment);
            newYamlObject.setPatchingContext(fragment.getPatchingContext());
            addFragmentEntity(fragment, fragment.getListForYamlObject(mapper), newYamlObject);
        } else {
            YO oldYamlObject = (YO) patchableMap.get(object);
            modifyFragmentEntity(fragment, fragment.getListForYamlObject(mapper), oldYamlObject, newYamlObject, mapper.getCollectionName());
        }
        patchableMap.put(object, newYamlObject);

        return object;
    }

    @SuppressWarnings("unchecked")
    public synchronized <BO extends AbstractOrganizableObject> void remove(BO object) {
        BusinessObjectToYamlMapper<BO, ?> mapper = (BusinessObjectToYamlMapper<BO, ?>) businessObjectToYamlMappers.get(object.getClass());
        if (mapper == null) {
            throw new AutomationPackageUpdateException("No BusinessObjectToYamlMapper registered for class: " + object.getClass().getName());
        }
        AutomationPackageFragmentYaml fragment = fragmentMap.get(object);
        removeFragmentEntity(fragment, fragment.getListForYamlObject(mapper), object);
    }

    private <T extends PatchableYamlModel> void addFragmentEntity(AutomationPackageFragmentYaml fragment, PatchableYamlList<T> entityList, T newEntity) {
        entityList.add(newEntity);
        fragment.writeToDisk();
    }

    private <T extends PatchableYamlModel> void modifyFragmentEntity(AutomationPackageFragmentYaml fragment, PatchableYamlList<T> entityList, T oldEntity, T newEntity, String fieldName) {
        entityList.replaceItem(oldEntity, newEntity);
        Path oldRelativePath = determineRelativePathFor(oldEntity, fieldName, false);
        Path newRelativePath = determineRelativePathFor(newEntity, fieldName, false);

        // Path did not change - skip entire move logic
        if (!oldRelativePath.equals(newRelativePath)) {
            Path oldAbsolutePath = apRoot.resolve(oldRelativePath);

             /*  oldRelativePath is the path which would have been given to old version of the entity
                 by the fragment manager. If it matches the fragment path, this means that
                 the fragment path was intended to follow the naming convention based on the configuration
                 (i.e. PER_OBJECT naming)

                 if the paths don't match, then simply skip the renaming. This silently allows for:
                  - legacy fragments which don't follow the naming convention
                  - FRAGMENT type naming (fixed fragment for object types such as Parameters)
              */
            if (oldAbsolutePath.equals(fragment.getFragmentPath())) {
                Path newAbsolutePath = apRoot.resolve(newRelativePath);

                try {
                    FileUtils.moveFile(oldAbsolutePath.toFile(), newAbsolutePath.toFile());
                    fragment.setFragmentPath(newAbsolutePath);
                } catch (IOException e) {
                    throw new AutomationPackageConcurrentEditException(
                        String.format("Unable to rename file %s to file %s. Was the file renamed or deleted outside the editor?", oldAbsolutePath, newAbsolutePath));
                }

                AutomationPackageFragmentYaml referencingFragment = determineReferencingFragment(oldRelativePath)
                    .orElse(descriptorYaml);

                String newReference = resourcePathMatchingResolver.getFragmentReferenceString(determineRelativePathFor(newEntity, fieldName, true));
                String oldReference = resourcePathMatchingResolver.getFragmentReferenceString(oldRelativePath);

                if (referencingFragment.getFragments().removeIf(f -> f.getValue().equals(oldReference))) {
                    referencingFragment.getFragments().add(new PatchableYamlPrimitive<>(referencingFragment.getPatchingContext(), newReference));
                    referencingFragment.writeToDisk();
                }
            }
        }
        fragment.writeToDisk();
    }

    private <BO extends AbstractOrganizableObject, T extends PatchableYamlModel> void removeFragmentEntity(AutomationPackageFragmentYaml fragment, PatchableYamlList<T> entityList, BO object) {

        PatchableYamlModel yamlObject = patchableMap.get(object);
        entityList.remove(yamlObject);

        patchableMap.remove(object);
        fragmentMap.remove(object);

        if (fragment.isEmpty()) {
            try {
                FileUtils.delete(fragment.getFragmentPath().toFile());

                determineReferencingFragment(fragment.getFragmentPath()).ifPresent(referencingFragment -> {
                    String relativeFragmentReference = resourcePathMatchingResolver
                        .getFragmentReferenceString(apRoot.relativize(fragment.getFragmentPath()));
                    if (referencingFragment.getFragments().removeIf(f -> f.getValue().equals(relativeFragmentReference))) {
                        referencingFragment.writeToDisk();
                    }
                    fragments.remove(fragment);
                });
            } catch (IOException e) {
                throw new AutomationPackageConcurrentEditException(String.format("%s was removed outside the editor", fragment.getFragmentPath()));
            }
        } else {
            fragment.writeToDisk();
        }
    }

    private AutomationPackageFragmentYaml fragmentForNewObject(PatchableYamlModel patchable, String fieldName) {

        Path path = determineRelativePathFor(patchable, fieldName, false);
        Path absolutePath = apRoot.resolve(path);

        Optional<AutomationPackageFragmentYaml> optionalExistingFragment = fragmentMap
            .values().stream().filter(f -> f.getFragmentPath().equals(absolutePath)).findAny();
        if (optionalExistingFragment.isPresent()) {
            return optionalExistingFragment.get();
        }

        PatchingContext context = new PatchingContext(absolutePath.toString(), "---", descriptorYaml.getPatchingContext().getMapper());
        AutomationPackageFragmentYaml fragment = new AutomationPackageFragmentYamlImpl(context);
        fragments.add(fragment);
        fragment.setFragmentPath(absolutePath);

        if (determineReferencingFragment(path).isEmpty()) {
            String referencingPath = resourcePathMatchingResolver.getFragmentReferenceString(determineRelativePathFor(patchable, fieldName, true));
            descriptorYaml.getFragments().add(new PatchableYamlPrimitive<>(descriptorYaml.getPatchingContext(), referencingPath));
            descriptorYaml.writeToDisk();
        }
        return fragment;
    }

    private Optional<AutomationPackageFragmentYaml> determineReferencingFragment(Path path) {
        return Stream.concat(Stream.of(descriptorYaml), fragments.stream())
            .filter(fragment -> fragment.getFragments().stream()
                .anyMatch(pattern -> resourcePathMatchingResolver.isMatchingPath(pattern.getValue(), path)))
            .findFirst();
    }

    /**
     * Determines the filesystem path (i.e. filename), relative to the AP root, where a given PatchableYamlModel is/should be located.
     * The result may vary on the {@link NewObjectFragmentMode} setting (e.g. data could go into a single aggregated YAML file, or a file per entity).
     *
     * @param patchable   existing model instance
     * @param entityName  YAML entity name containing this model's instances (e.g. "plans", "keywords")
     * @param useWildcard if `true` and the mode is per-object, return a wildcard filename (.../*.yml) instead of a concrete filename.
     * @return path to the file location for the given patchable.
     */
    private Path determineRelativePathFor(PatchableYamlModel patchable, String entityName, boolean useWildcard) {

        NewObjectFragmentMode mode = NewObjectFragmentMode.valueOf(properties.getProperty(String.format(PROPERTY_NEW_OBJECT_FRAGMENT_MODE, entityName), NewObjectFragmentMode.PER_OBJECT.name()));
        String defaultRelativeFragmentPath = entityName;

        if (mode == NewObjectFragmentMode.FRAGMENT) {
            defaultRelativeFragmentPath = defaultRelativeFragmentPath + ".yml";
        }

        String relativeFragmentPath = properties.getProperty(String.format(PROPERTY_NEW_OBJECT_FRAGMENT_PATH, entityName), defaultRelativeFragmentPath);

        return switch (mode) {
            case NewObjectFragmentMode.FRAGMENT -> new File(relativeFragmentPath).toPath();
            case NewObjectFragmentMode.PER_OBJECT -> {
                if (patchable instanceof NamedPatchableYamlModel namedPatchableYamlModel) {
                    String name = useWildcard ? "*" : namedPatchableYamlModel.getName();
                    yield new File(relativeFragmentPath).toPath().resolve(sanitizeFilename(name + ".yml"));
                }

                throw new AutomationPackagePerObjectSaveUnsupportedException(String.format("""
                    Saving by object name is unsupported for %1$s, please configure the entity to be stored in a specified single fragment, i.e.

                    %2$s = %1$s.yml
                    %3$s = %4$s
                    """, entityName, String.format(PROPERTY_NEW_OBJECT_FRAGMENT_PATH, entityName), String.format(PROPERTY_NEW_OBJECT_FRAGMENT_MODE, entityName), NewObjectFragmentMode.FRAGMENT.name()));
            }
        };
    }

    private String sanitizeFilename(String inputName) {
        return URLEncoder.encode(inputName, StandardCharsets.UTF_8).replace("+", " ");
    }
}
