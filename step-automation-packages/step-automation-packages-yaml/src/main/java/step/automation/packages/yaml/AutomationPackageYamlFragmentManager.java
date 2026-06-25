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

import step.automation.packages.StagingAutomationPackageContext;
import step.automation.packages.model.YamlAutomationPackageKeyword;
import step.automation.packages.yaml.model.AutomationPackageDescriptorYaml;
import step.automation.packages.yaml.model.AutomationPackageFragmentYaml;
import step.automation.packages.yaml.model.AutomationPackageFragmentYamlImpl;
import step.core.accessors.AbstractOrganizableObject;
import step.core.plans.Plan;
import step.core.yaml.PatchableYamlModel;
import step.core.yaml.PatchableYamlModelBase;
import step.core.yaml.PatchingContext;
import step.core.yaml.deserialization.AutomationPackagePerObjectSaveUnsupportedException;
import step.core.yaml.deserialization.AutomationPackageUpdateException;
import step.core.yaml.deserialization.PatchableYamlList;
import step.functions.Function;
import step.parameter.Parameter;
import step.parameter.automation.AutomationPackageParameter;
import step.plans.parser.yaml.YamlPlan;
import step.plugins.functions.types.CompositeFunction;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class AutomationPackageYamlFragmentManager {


    protected final Path apRoot;
    protected final StagingAutomationPackageContext stagingContext;

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
    protected final AutomationPackageDescriptorReader descriptorReader;

    protected final Map<AbstractOrganizableObject, PatchableYamlModel> patchableMap = new ConcurrentHashMap<>();
    protected final Map<AbstractOrganizableObject, AutomationPackageFragmentYaml> fragmentMap = new ConcurrentHashMap<>();
    protected final Map<String, AutomationPackageFragmentYaml> pathToYamlFragment;

    protected Properties properties = new Properties();
    public final AutomationPackageFragmentYaml descriptorYaml;

    public AutomationPackageYamlFragmentManager(AutomationPackageDescriptorYaml descriptorYaml, Map<String, AutomationPackageFragmentYaml> fragmentMap, AutomationPackageDescriptorReader descriptorReader, StagingAutomationPackageContext stagingContext) {

        this.descriptorReader = descriptorReader;
        this.descriptorYaml = descriptorYaml;

        pathToYamlFragment = fragmentMap;
        apRoot = Path.of(descriptorYaml.getFragmentUrl().getPath())
            .getParent();

        this.stagingContext = stagingContext;
        initializeMaps(descriptorYaml);

        pathToYamlFragment.values().stream()
            .filter(f -> f != descriptorYaml)
            .forEach(this::initializeMaps);
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public void initializeMaps(AutomationPackageFragmentYaml fragment) {
        pathToYamlFragment.put(fragment.getFragmentUrl().toString(), fragment);
        for (YamlPlan yamlPlan : fragment.getPlans()) {
            Plan plan = descriptorReader.getPlanReader().yamlPlanToPlan(yamlPlan);
            patchableMap.put(plan, yamlPlan);
            fragmentMap.put(plan, fragment);
        }

        for (YamlAutomationPackageKeyword keyword : fragment.getKeywords()) {
            try {
                Function function = keyword.prepareKeyword(stagingContext);
                patchableMap.put(function, keyword);
                fragmentMap.put(function, fragment);
            } catch (Exception e) {
                /* TODO: requires proper handling of keywords
                    which map to resources or require StagingAutomationPackageContext in another way.
                 */
                System.out.println(e);
            }
        }

        PatchableYamlList<Object> parameters = fragment.getAdditionalField(Parameter.ENTITY_NAME);
        if (parameters != null) {
            for (Object object : parameters) {
                AutomationPackageParameter yamlParameter = (AutomationPackageParameter) object;
                Parameter parameter = yamlParameter.toParameter();
                patchableMap.put(parameter, yamlParameter);
                fragmentMap.put(parameter, fragment);
            }
        }
    }

    public <BO extends AbstractOrganizableObject> Iterable<BO> getBusinessObjects(Class<BO> boClass) {
        return patchableMap.keySet().stream()
            .filter(businessObject -> boClass.isAssignableFrom(businessObject.getClass()))
            .map(businessObject -> (BO) businessObject).collect(Collectors.toList());
    }


    public synchronized Plan savePlan(Plan plan) {
        YamlPlan newYamlPlan = descriptorReader.getPlanReader().planToYamlPlan(plan);

        AutomationPackageFragmentYaml fragment = fragmentMap.get(plan);
        if (fragment == null) {
            fragment = fragmentForNewObject(plan, YamlPlan.PLANS_ENTITY_NAME);
            fragmentMap.put(plan, fragment);
            pathToYamlFragment.put(fragment.getFragmentUrl().toString(), fragment);
            addFragmentEntity(fragment, fragment.getPlans(), newYamlPlan);
        } else {
            YamlPlan oldYamlPlan = (YamlPlan) patchableMap.get(plan);
            modifyFragmentEntity(fragment, fragment.getPlans(), oldYamlPlan, newYamlPlan);
        }
        patchableMap.put(plan, newYamlPlan);

        return plan;
    }


    public synchronized step.functions.Function saveFunction(step.functions.Function function) {
        AutomationPackageFragmentYaml fragment = fragmentMap.get(function);
        if (fragment == null) {
            fragment = fragmentForNewObject(function, "keywords");
            fragmentMap.put(function, fragment);
            pathToYamlFragment.put(fragment.getFragmentUrl().toString(), fragment);
            YamlAutomationPackageKeyword newKeyword = createNewYamlKeyword(function);
            if (newKeyword != null) {
                patchableMap.put(function, newKeyword);
                addFragmentEntity(fragment, fragment.getKeywords(), newKeyword);
            } else {
                System.err.println("SAVING OF FUNCTION OF TYPE " + function.getClass().getName() + " IS NOT CURRENTLY SUPPORTED");
            }
        } else {
            YamlAutomationPackageKeyword yamlKeyword = (YamlAutomationPackageKeyword) patchableMap.get(function);
            yamlKeyword.getYamlKeyword().updateFromFunction(function);
            modifyFragmentEntity(fragment, fragment.getKeywords(), yamlKeyword, yamlKeyword);
        }
        return function;
    }

    private YamlAutomationPackageKeyword createNewYamlKeyword(step.functions.Function function) {
        // FIXME: I know, this is is a giant horrible stinking hack for now, there needs to be a better way.
        if (function instanceof CompositeFunction compositeFunction) {
            try {
                // I don't know what the proper way is to serialize this, but we know that deserialization should work...
                YamlPlan plan = descriptorReader.getPlanReader().planToYamlPlan(compositeFunction.getPlan());
                plan.setName(null);
                // we only want to use the serialization functions here
                PatchingContext patchingContext = new PatchingContext(descriptorReader.yamlObjectMapper);
                StringBuilder yaml = new StringBuilder("""
                    keywords:
                      - Composite:
                          plan:
                    """);
                yaml.append(patchingContext.serialize(plan, " ".repeat(8)));

                // There are more attributes, this is just a PoC anyway
                Optional.ofNullable(function.getAttribute("name")).ifPresent(value -> {
                    yaml.append(patchingContext.serialize(Map.of("name", value), " ".repeat(6)));
                });
                Optional.ofNullable(function.getDescription()).ifPresent(value -> {
                    yaml.append(patchingContext.serialize(Map.of("description", value), " ".repeat(6)));
                });

                InputStream is = new ByteArrayInputStream(yaml.toString().getBytes());
                var fragment = descriptorReader.readAutomationPackageFragment(is, "horrible-hack", "horrible-hack");
                return fragment.getKeywords().stream().findFirst().orElse(null);
            } catch (Exception e) {
                // TODO: better error handling
                e.printStackTrace();
                return null;
            }
        } else {
            return null;
        }
    }

    private <T extends PatchableYamlModel> void addFragmentEntity(AutomationPackageFragmentYaml fragment, PatchableYamlList<T> entityList, T newEntity) {
        entityList.add(newEntity);
        fragment.writeToDisk();
    }

    private <T extends PatchableYamlModel> void modifyFragmentEntity(AutomationPackageFragmentYaml fragment, PatchableYamlList<T> entityList, T oldEntity, T newEntity) {
        entityList.replaceItem(oldEntity, newEntity);
        fragment.writeToDisk();
    }

    private AutomationPackageFragmentYaml fragmentForNewObject(AbstractOrganizableObject p, String fieldName) {

        NewObjectFragmentMode mode = NewObjectFragmentMode.valueOf(properties.getProperty(String.format(PROPERTY_NEW_OBJECT_FRAGMENT_MODE, fieldName), NewObjectFragmentMode.PER_OBJECT.name()));
        String defaultRelativeFragmentPath = fieldName;
        if (mode == NewObjectFragmentMode.FRAGMENT) {
            defaultRelativeFragmentPath = defaultRelativeFragmentPath + ".yml";
        }


        if (mode == NewObjectFragmentMode.PER_OBJECT && !p.hasAttribute(AbstractOrganizableObject.NAME)) {
            throw new AutomationPackagePerObjectSaveUnsupportedException(String.format("""
                Saving by object name is unsupported for %1$s, please configure the entity to be stored in a specified single fragment, i.e.

                %2$s = %1$s.yml
                %3$s = %4$s
                """, fieldName, String.format(PROPERTY_NEW_OBJECT_FRAGMENT_PATH, fieldName), String.format(PROPERTY_NEW_OBJECT_FRAGMENT_MODE, fieldName), NewObjectFragmentMode.FRAGMENT.name()));
        }

        String relativeFragmentPath = properties.getProperty(String.format(PROPERTY_NEW_OBJECT_FRAGMENT_PATH, fieldName), defaultRelativeFragmentPath);
        Path path = new File(relativeFragmentPath).toPath();
        if (!path.isAbsolute()) {
            path = apRoot.resolve(path);
        }

        if (mode == NewObjectFragmentMode.PER_OBJECT) {
            path = path.resolve(sanitizeFilename(p.getAttribute(AbstractOrganizableObject.NAME)) + ".yml");
        }

        try {
            URL url = path.toUri().toURL();


            if (pathToYamlFragment.containsKey(url.toString())) {
                return pathToYamlFragment.get(url.toString());
            }
            PatchingContext context = new PatchingContext(url.toString(), "---", descriptorYaml.getPatchingContext().getMapper());
            AutomationPackageFragmentYaml fragment = new AutomationPackageFragmentYamlImpl(context);
            fragment.setFragmentUrl(url);
            return fragment;
        } catch (MalformedURLException e) {
            throw new AutomationPackageUpdateException(MessageFormat.format("Error creating path for new fragment: {0}", path), e);
        }

    }

    public String sanitizeFilename(String inputName) {
        return URLEncoder.encode(inputName, Charset.defaultCharset()).replace("+", " ");
    }

    public void removePlan(Plan plan) {
        AutomationPackageFragmentYaml fragment = fragmentMap.get(plan);
        YamlPlan yamlPlan = (YamlPlan) patchableMap.get(plan);

        fragment.getPlans().remove(yamlPlan);

        patchableMap.remove(plan);
        fragmentMap.remove(plan);

        fragment.writeToDisk();
    }

    public void removeFunction(step.functions.Function function) {
        AutomationPackageFragmentYaml fragment = fragmentMap.get(function);
        YamlAutomationPackageKeyword yamlKeyword = (YamlAutomationPackageKeyword) patchableMap.get(function);

        fragment.getKeywords().remove(yamlKeyword);

        patchableMap.remove(function);
        fragmentMap.remove(function);

        fragment.writeToDisk();
    }


    public <BO extends AbstractOrganizableObject> void removeAdditionalFieldObject(BO object, String fieldName) {

        AutomationPackageFragmentYaml fragment = fragmentMap.get(object);
        PatchableYamlModel yamlObject = patchableMap.get(object);

        fragment.getAdditionalField(fieldName)
            .remove(yamlObject);

        patchableMap.remove(object);
        fragmentMap.remove(object);

        fragment.writeToDisk();
    }

    public synchronized <BO extends AbstractOrganizableObject, YO extends PatchableYamlModelBase> BO saveAdditionalFieldObject(BO object, java.util.function.Function<PatchingContext, YO> newYamlObjectCreator, String fieldName) {
        AutomationPackageFragmentYaml fragment;
        if (fragmentMap.get(object) == null) {
            fragment = fragmentForNewObject(object, fieldName);

            YO newYamlObject = newYamlObjectCreator.apply(fragment.getPatchingContext());
            PatchableYamlList<YO> list = (PatchableYamlList<YO>) fragment.getAdditionalFields()
                .computeIfAbsent(fieldName, k -> new PatchableYamlList<YO>(fragment.getPatchingContext(), fieldName));
            fragmentMap.put(object, fragment);
            pathToYamlFragment.put(fragment.getFragmentUrl().toString(), fragment);
            addFragmentEntity(fragment, list, newYamlObject);
            patchableMap.put(object, newYamlObject);
        } else {
            fragment = fragmentMap.get(object);
            YO newYamlObject = newYamlObjectCreator.apply(fragment.getPatchingContext());
            PatchableYamlList<YO> list = (PatchableYamlList<YO>) fragment.getAdditionalFields()
                .computeIfAbsent(fieldName, k -> new PatchableYamlList<YO>(fragment.getPatchingContext(), fieldName));

            YO oldYamlObject = (YO) patchableMap.get(object);
            modifyFragmentEntity(fragment, list, oldYamlObject, newYamlObject);
            patchableMap.put(object, newYamlObject);
        }
        return object;
    }


}
