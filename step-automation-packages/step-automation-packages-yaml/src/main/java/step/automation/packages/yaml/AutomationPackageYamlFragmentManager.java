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
import step.core.yaml.deserialization.AutomationPackageUpdateException;
import step.core.yaml.deserialization.PatchableYamlList;
import step.automation.packages.yaml.model.AutomationPackageDescriptorYaml;
import step.automation.packages.yaml.model.AutomationPackageFragmentYaml;
import step.automation.packages.yaml.model.AutomationPackageFragmentYamlImpl;
import step.core.accessors.AbstractOrganizableObject;
import step.core.plans.Plan;
import step.core.yaml.PatchableYamlModel;
import step.core.yaml.deserialization.PatchingContext;
import step.plans.parser.yaml.YamlPlan;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class AutomationPackageYamlFragmentManager {


    public static final String PROPERTY_NEW_PLAN_FRAGMENT_PATH = "newFragmentPaths.plans";
    private final AutomationPackageDescriptorReader descriptorReader;

    private final Map<Plan, YamlPlan> planToYamlPlan = new ConcurrentHashMap<>();
    private final Map<Plan, AutomationPackageFragmentYaml> planToYamlFragment = new ConcurrentHashMap<>();
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
        for (YamlPlan p: fragment.getPlans()) {
            Plan plan = descriptorReader.getPlanReader().yamlPlanToPlan(p);
            planToYamlPlan.put(plan, p);
            planToYamlFragment.put(plan, fragment);
        }
    }

    public Iterable<Plan> getPlans() {
        return planToYamlPlan.keySet();
    }

    public Plan savePlan(Plan p) {
        YamlPlan newYamlPlan = descriptorReader.getPlanReader().planToYamlPlan(p);

        AutomationPackageFragmentYaml fragment = planToYamlFragment.get(p);
        if (fragment == null) {
            fragment = fragmentForNewPlan(p);
            planToYamlFragment.put(p, fragment);
            pathToYamlFragment.put(fragment.getFragmentUrl().toString(), fragment);
            addFragmentEntity(fragment, fragment.getPlans(), newYamlPlan);
        } else {
            YamlPlan yamlPlan = planToYamlPlan.get(p);
            modifyFragmentEntity(fragment, fragment.getPlans(), yamlPlan, newYamlPlan);
        }
        planToYamlPlan.put(p, newYamlPlan);

        return p;
    }

    private <T extends PatchableYamlModel>  void addFragmentEntity(AutomationPackageFragmentYaml fragment, PatchableYamlList<T> entityList, T newEntity) {
        entityList.add(newEntity);
        fragment.writeToDisk();
    }

    private <T extends PatchableYamlModel> void modifyFragmentEntity(AutomationPackageFragmentYaml fragment, PatchableYamlList<T> entityList, T oldEntity, T newEntity) {
        entityList.replaceItem(oldEntity, newEntity);
        fragment.writeToDisk();
    }

    private AutomationPackageFragmentYaml fragmentForNewPlan(Plan p) {

        String planFragmentPath = properties.getProperty(PROPERTY_NEW_PLAN_FRAGMENT_PATH, descriptorYaml.getFragmentUrl().getPath());
        planFragmentPath = planFragmentPath.replaceAll("%name%", sanitizeFilename(p.getAttribute(AbstractOrganizableObject.NAME)));

        Path path = new File(planFragmentPath).toPath();
        if (!path.isAbsolute()) {
            Path apRoot = Path.of(descriptorYaml.getFragmentUrl().getPath())
                    .getParent();
            path = apRoot.resolve(path);
        }

        try {
            URL url = path.toUri().toURL();


            if (pathToYamlFragment.containsKey(url.toString())) return pathToYamlFragment.get(url.toString());
            PatchingContext context = new PatchingContext("---", descriptorYaml.getPatchingContext().getMapper());
            AutomationPackageFragmentYaml fragment =  new AutomationPackageFragmentYamlImpl(context);
            fragment.setFragmentUrl(url);
            return fragment;
        } catch (MalformedURLException e) {
            throw new AutomationPackageUpdateException(MessageFormat.format("Error creating path for new fragment: {0}", path), e);
        }

    }

    public String sanitizeFilename(String inputName) {
        return inputName.replaceAll("[^a-zA-Z0-9-_.]", "_");
    }

    public void removePlan(Plan p) {
        AutomationPackageFragmentYaml fragment = planToYamlFragment.get(p);
        YamlPlan yamlPlan = planToYamlPlan.get(p);

        fragment.getPlans().remove(yamlPlan);

        planToYamlPlan.remove(p);
        planToYamlFragment.remove(p);

        fragment.writeToDisk();
    }
}
