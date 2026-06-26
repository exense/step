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
import step.core.yaml.NamedObjectPatchableYamlModel;
import step.core.yaml.PatchableYamlModel;
import step.core.yaml.PatchingContext;
import step.core.yaml.deserialization.*;
import step.plans.parser.yaml.YamlPlanReader;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AutomationPackageYamlFragmentManager {

    protected final Path apRoot;
    private final ResourcePathMatchingResolver resourcePatchMatchingResolver;

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

    private final Map<Class<?>, BusinessObjectToYamlMapper<?, ?>> mappers = new HashMap<>();

    public AutomationPackageYamlFragmentManager(ResourcePathMatchingResolver resourcePathMatchingResolver, AutomationPackageDescriptorYaml descriptorYaml, Set<AutomationPackageFragmentYaml> fragments, AutomationPackageDescriptorReader descriptorReader, StagingAutomationPackageContext stagingContext) {
        this.resourcePatchMatchingResolver = resourcePathMatchingResolver;
        this.descriptorYaml = descriptorYaml;

        apRoot = descriptorYaml.getFragmentPath().getParent();

        Map<Class<?>, Object> injectables = new HashMap<>();

        injectables.put(YamlPlanReader.class, descriptorReader.getPlanReader());
        injectables.put(StagingAutomationPackageContext.class, stagingContext);

        scanBusinessObjectMappers(injectables);
        Collection<YamlToBusinessObjectMapper<?, ?>> yamlObjectMappers = scanYamlObjectMappers(injectables);

        initializeMaps(descriptorYaml, yamlObjectMappers);

        this.fragments = fragments;

        fragments.stream()
            .filter(f -> f != descriptorYaml)
            .forEach(f -> initializeMaps(f, yamlObjectMappers));
    }

    private void scanBusinessObjectMappers(Map<Class<?>, Object> injectables) {

        for (Class<?> annotatedClass : CachedAnnotationScanner.getClassesWithAnnotation(BusinessObjectToYamlMapping.class)) {
            Arrays.stream(annotatedClass.getConstructors()).findFirst().ifPresent(c -> {
                try {
                    BusinessObjectToYamlMapping annotation = annotatedClass.getAnnotation(BusinessObjectToYamlMapping.class);
                    mappers.put(annotation.sourceClass(), (BusinessObjectToYamlMapper<?, ?>) c.newInstance(Arrays.stream(c.getParameterTypes()).map(injectables::get).toArray()));
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    private Collection<YamlToBusinessObjectMapper<?, ?>> scanYamlObjectMappers(Map<Class<?>, Object> injectables) {
        List<YamlToBusinessObjectMapper<?, ?>> list = new LinkedList<>();
        for (Class<?> annotatedClass : CachedAnnotationScanner.getClassesWithAnnotation(YamlToBusinessObjectMapping.class)) {
            Arrays.stream(annotatedClass.getConstructors()).findFirst().ifPresent(c -> {
                try {
                    list.add((YamlToBusinessObjectMapper<?, ?>) c.newInstance(Arrays.stream(c.getParameterTypes()).map(injectables::get).toArray()));
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        return list;
    }

    private void initializeMaps(AutomationPackageFragmentYaml fragment, Collection<YamlToBusinessObjectMapper<?, ?>> yamlObjectMappers) {
        for (YamlToBusinessObjectMapper<?, ?> mapper : yamlObjectMappers) {
            fragment.initializeMaps(mapper, patchableMap, fragmentMap);
        }
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public <BO extends AbstractOrganizableObject, T> Iterable<BO> getBusinessObjects(Class<T> boClass) {
        return patchableMap.keySet().stream()
            .filter(businessObject -> boClass.isAssignableFrom(businessObject.getClass()))
            .map(businessObject -> (BO) businessObject).collect(Collectors.toList());
    }

    public synchronized <BO extends AbstractOrganizableObject, YO extends PatchableYamlModel> BO save(BO object) {

        BusinessObjectToYamlMapper<BO, YO> mapper = (BusinessObjectToYamlMapper<BO, YO>) mappers.get(object.getClass());
        if (mapper == null) {
            throw new AutomationPackageUpdateException("No BusinessObjectToYamlMapper registered for class: " + object.getClass().getName());
        }
        YO newYamlObject = mapper.getNewYamlObject(object);

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

    public synchronized <BO extends AbstractOrganizableObject> void remove(BO object) {
        BusinessObjectToYamlMapper<BO, ?> mapper = (BusinessObjectToYamlMapper<BO, ?>) mappers.get(object.getClass());
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
        Path oldRelativePath = determineObjectRelativePath(oldEntity, fieldName, false);
        Path newRelativePath = determineObjectRelativePath(newEntity, fieldName, false);

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

                String newReference = resourcePatchMatchingResolver.getFragmentReferenceString(determineObjectRelativePath(newEntity, fieldName, true));
                String oldReference = resourcePatchMatchingResolver.getFragmentReferenceString(oldRelativePath);

                if (referencingFragment.getFragments().removeIf(f -> f.getValue().equals(oldReference))) {
                    referencingFragment.getFragments().add(new PatchableYamlPrimitive<>(referencingFragment.getPatchingContext(), newReference));
                    referencingFragment.writeToDisk();
                };
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
                    String relativeFragmentReference = resourcePatchMatchingResolver
                        .getFragmentReferenceString(apRoot.relativize(fragment.getFragmentPath()));
                    if (referencingFragment.getFragments().removeIf(f -> f.getValue().equals(relativeFragmentReference))) {
                        referencingFragment.writeToDisk();
                    };
                    fragments.remove(fragment);
                });
            } catch (IOException e) {
                throw new AutomationPackageConcurrentEditException(String.format("%s was removed outside the editor", fragment.getFragmentPath()));
            }
        } else {
            fragment.writeToDisk();
        }
    }

    private AutomationPackageFragmentYaml fragmentForNewObject(PatchableYamlModel p, String fieldName) {

        Path path = determineObjectRelativePath(p, fieldName, false);
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
            String referencingPath = resourcePatchMatchingResolver.getFragmentReferenceString(determineObjectRelativePath(p, fieldName, true));
            descriptorYaml.getFragments().add(new PatchableYamlPrimitive<>(descriptorYaml.getPatchingContext(), referencingPath));
            descriptorYaml.writeToDisk();
        }
        return fragment;
    }

    private Optional<AutomationPackageFragmentYaml> determineReferencingFragment(Path path) {
        return Stream.concat(Stream.of(descriptorYaml), fragments.stream())
            .filter(fragment -> fragment.getFragments().stream()
                .anyMatch(pattern -> resourcePatchMatchingResolver.isMatchingPath(pattern.getValue(), path)))
            .findFirst();
    }

    private Path determineObjectRelativePath(PatchableYamlModel p, String fieldName, boolean globPattern) {

        NewObjectFragmentMode mode = NewObjectFragmentMode.valueOf(properties.getProperty(String.format(PROPERTY_NEW_OBJECT_FRAGMENT_MODE, fieldName), NewObjectFragmentMode.PER_OBJECT.name()));
        String defaultRelativeFragmentPath = fieldName;

        if (mode == NewObjectFragmentMode.FRAGMENT) {
            defaultRelativeFragmentPath = defaultRelativeFragmentPath + ".yml";
        }

        String relativeFragmentPath = properties.getProperty(String.format(PROPERTY_NEW_OBJECT_FRAGMENT_PATH, fieldName), defaultRelativeFragmentPath);

        return switch (mode) {
            case NewObjectFragmentMode.FRAGMENT -> new File(relativeFragmentPath).toPath();
            case NewObjectFragmentMode.PER_OBJECT -> {
                if (p instanceof NamedObjectPatchableYamlModel namedObjectPatchableYamlModel) {
                    String name = globPattern ? "*" : namedObjectPatchableYamlModel.getName();
                    yield new File(relativeFragmentPath).toPath().resolve(sanitizeFilename(name + ".yml"));
                }

                throw new AutomationPackagePerObjectSaveUnsupportedException(String.format("""
                    Saving by object name is unsupported for %1$s, please configure the entity to be stored in a specified single fragment, i.e.

                    %2$s = %1$s.yml
                    %3$s = %4$s
                    """, fieldName, String.format(PROPERTY_NEW_OBJECT_FRAGMENT_PATH, fieldName), String.format(PROPERTY_NEW_OBJECT_FRAGMENT_MODE, fieldName), NewObjectFragmentMode.FRAGMENT.name()));
            }
        };
    }

    private String sanitizeFilename(String inputName) {
        return URLEncoder.encode(inputName, Charset.defaultCharset()).replace("+", " ");
    }
}
