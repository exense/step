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

import com.fasterxml.jackson.core.exc.StreamWriteException;
import com.fasterxml.jackson.databind.DatabindException;
import org.yaml.snakeyaml.Yaml;
import step.automation.packages.yaml.model.AutomationPackageDescriptorYaml;
import step.automation.packages.yaml.model.AutomationPackageFragmentYaml;
import step.core.plans.Plan;
import step.plans.parser.yaml.YamlPlan;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class AutomationPackageYamlFragmentManager {


    private final AutomationPackageDescriptorReader descriptorReader;

    private final Map<Plan, YamlPlan> planToYamlPlan = new ConcurrentHashMap<>();
    private final Map<Plan, AutomationPackageFragmentYaml> planToYamlFragment = new ConcurrentHashMap<>();

    public AutomationPackageYamlFragmentManager(AutomationPackageDescriptorYaml descriptorYaml, AutomationPackageDescriptorReader descriptorReader) {

        this.descriptorReader = descriptorReader;

        initializeMaps(descriptorYaml);
    }

    public void initializeMaps(AutomationPackageFragmentYaml fragment) {
        for (YamlPlan p: fragment.getPlans()) {
            Plan plan = descriptorReader.getPlanReader().yamlPlanToPlan(p);
            planToYamlPlan.put(plan, p);
            planToYamlFragment.put(plan, fragment);
        };

        for (AutomationPackageFragmentYaml child : fragment.getChildren()) {
            initializeMaps(child);
        }
    }

    public Iterable<Plan> getPlans() {
        return planToYamlPlan.keySet();
    }

    public Plan savePlan(Plan p) {
        YamlPlan newYamlPlan = descriptorReader.getPlanReader().planToYamlPlan(p);

        AutomationPackageFragmentYaml fragment = planToYamlFragment.get(p);
        if (fragment == null) {
            fragment = newFragmentForPlan(p);
            fragment.getPlans().add(newYamlPlan);
        } else {
            YamlPlan yamlPlan = planToYamlPlan.get(p);
            fragment.getPlans().replaceAll(plan -> plan == yamlPlan ? newYamlPlan : plan);
        }

        planToYamlPlan.put(p, newYamlPlan);
        writeFragment(fragment);
        return p;
    }

    private AutomationPackageFragmentYaml newFragmentForPlan(Plan p) {

        throw new UnsupportedOperationException("new Plan creation not yet supported in IDE");
        /*
        try {
            File file = new File(descriptorYaml.getFragmentUrl().toURI());

            Path file.toPath().getParent().resolveSibling(getRelativePathForNewPlan(p));

        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }*/
    }

    public void removePlan(Plan p) {
        AutomationPackageFragmentYaml fragment = planToYamlFragment.get(p);
        YamlPlan yamlPlan = planToYamlPlan.get(p);

        fragment.getPlans().remove(yamlPlan);

        planToYamlPlan.remove(p);
        planToYamlFragment.remove(p);

        writeFragment(fragment);
    }

    private void writeFragment(AutomationPackageFragmentYaml fragment) {
        try {
            File file = new File(fragment.getFragmentUrl().toURI());
            descriptorReader.getYamlObjectMapper().writeValue(file, fragment);
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}
