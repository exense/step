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

import step.automation.packages.yaml.model.AutomationPackageDescriptorYaml;
import step.automation.packages.yaml.model.AutomationPackageFragmentYaml;
import step.automation.packages.yaml.model.AutomationPackageFragmentYamlImpl;
import step.core.accessors.AbstractOrganizableObject;
import step.core.plans.Plan;
import step.core.yaml.PatchableYamlModel;
import step.core.yaml.PatchableYamlModelBase;
import step.core.yaml.deserialization.AutomationPackageUpdateException;
import step.core.yaml.deserialization.PatchableYamlList;
import step.core.yaml.deserialization.PatchingContext;
import step.parameter.Parameter;
import step.parameter.automation.AutomationPackageParameter;
import step.plans.parser.yaml.YamlPlan;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AutomationPackageYamlFragmentManager {

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
    private final AutomationPackageDescriptorReader descriptorReader;

    private final Map<AbstractOrganizableObject, PatchableYamlModel> patchableMap = new ConcurrentHashMap<>();
    private final Map<AbstractOrganizableObject, AutomationPackageFragmentYaml> fragmentMap = new ConcurrentHashMap<>();
    private final Map<String, AutomationPackageFragmentYaml> pathToYamlFragment;

    private Properties properties = new Properties();
    private final AutomationPackageFragmentYaml descriptorYaml;

    public AutomationPackageYamlFragmentManager(AutomationPackageDescriptorYaml descriptorYaml, Map<String, AutomationPackageFragmentYaml> fragmentMap, AutomationPackageDescriptorReader descriptorReader) {

        this.descriptorReader = descriptorReader;
        this.descriptorYaml = descriptorYaml;

        pathToYamlFragment = fragmentMap;

        initializeMaps(descriptorYaml);

        pathToYamlFragment.values().forEach(this::initializeMaps);
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public void initializeMaps(AutomationPackageFragmentYaml fragment) {
        pathToYamlFragment.put(fragment.getFragmentUrl().toString(), fragment);
        for (YamlPlan yamlPlan: fragment.getPlans()) {
            Plan plan = descriptorReader.getPlanReader().yamlPlanToPlan(yamlPlan);
            patchableMap.put(plan, yamlPlan);
            fragmentMap.put(plan, fragment);
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
            YamlPlan yamlPlan = (YamlPlan) patchableMap.get(plan);
            modifyFragmentEntity(fragment, fragment.getPlans(), yamlPlan, newYamlPlan);
        }
        patchableMap.put(plan, newYamlPlan);

        return plan;
    }

    private <T extends PatchableYamlModel>  void addFragmentEntity(AutomationPackageFragmentYaml fragment, PatchableYamlList<T> entityList, T newEntity) {
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
        String relativeFragmentPath = properties.getProperty(String.format(PROPERTY_NEW_OBJECT_FRAGMENT_PATH, fieldName), defaultRelativeFragmentPath);
        Path path = new File(relativeFragmentPath).toPath();
        if (!path.isAbsolute()) {
            Path apRoot = Path.of(descriptorYaml.getFragmentUrl().getPath())
                    .getParent();
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
            PatchingContext context = new PatchingContext("---", descriptorYaml.getPatchingContext().getMapper());
            AutomationPackageFragmentYaml fragment =  new AutomationPackageFragmentYamlImpl(context);
            fragment.setFragmentUrl(url);
            return fragment;
        } catch (MalformedURLException e) {
            throw new AutomationPackageUpdateException(MessageFormat.format("Error creating path for new fragment: {0}", path), e);
        }

    }

    public String sanitizeFilename(String inputName) {
        return URLEncoder.encode(inputName, Charset.defaultCharset()).replace("+", " ");
    }

    public void removePlan(Plan p) {
        AutomationPackageFragmentYaml fragment = fragmentMap.get(p);
        YamlPlan yamlPlan = (YamlPlan) patchableMap.get(p);

        fragment.getPlans().remove(yamlPlan);

        patchableMap.remove(p);
        fragmentMap.remove(p);

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

    public synchronized <BO extends AbstractOrganizableObject, YO extends PatchableYamlModelBase> BO saveAdditionalFieldObject(BO object, Function<PatchingContext, YO> newYamlObjectCreator, String fieldName) {
        AutomationPackageFragmentYaml fragment = fragmentMap.get(object);
        YO newYamlObject = newYamlObjectCreator.apply(fragment.getPatchingContext());
        PatchableYamlList<YO> list = (PatchableYamlList<YO>) fragment.getAdditionalFields().getOrDefault(fieldName, new PatchableYamlList<YO>(fragment.getPatchingContext(), fieldName));
        if (fragment == null) {
            fragment = fragmentForNewObject(object, fieldName);
            fragmentMap.put(object, fragment);
            pathToYamlFragment.put(fragment.getFragmentUrl().toString(), fragment);
            addFragmentEntity(fragment, list, newYamlObject);
            patchableMap.put(object, newYamlObject);
        } else {

            YO oldYamlObject = (YO) patchableMap.get(object);
            modifyFragmentEntity(fragment, list, oldYamlObject, newYamlObject);
        }
        return object;
    }
}
