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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import step.automation.packages.yaml.deserialization.PatchingParserDelegate;
import step.automation.packages.yaml.model.AutomationPackageDescriptorYaml;
import step.automation.packages.yaml.model.AutomationPackageFragmentYaml;
import step.automation.packages.yaml.model.AutomationPackageFragmentYamlImpl;
import step.core.accessors.AbstractOrganizableObject;
import step.core.plans.Plan;
import step.plans.parser.yaml.PatchableYamlArtefact;
import step.plans.parser.yaml.YamlPlan;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class AutomationPackageYamlFragmentManager {


    public static final String PROPERTY_NEW_PLAN_FRAGMENT_PATH = "newFragmentPaths.plans";
    private final AutomationPackageDescriptorReader descriptorReader;

    private final Map<Plan, YamlPlan> planToYamlPlan = new ConcurrentHashMap<>();
    private final Map<Plan, AutomationPackageFragmentYaml> planToYamlFragment = new ConcurrentHashMap<>();
    private final Map<URL, AutomationPackageFragmentYaml> urlToYamlFragment;
    private Properties properties = new Properties();
    private final AutomationPackageFragmentYaml descriptorYaml;

    public AutomationPackageYamlFragmentManager(AutomationPackageDescriptorYaml descriptorYaml, Map<URL, AutomationPackageFragmentYaml> fragmentMap, AutomationPackageDescriptorReader descriptorReader) {

        this.descriptorReader = descriptorReader;
        this.descriptorYaml = descriptorYaml;

        urlToYamlFragment = fragmentMap;

        initializeMaps(descriptorYaml);

        urlToYamlFragment.values().forEach(this::initializeMaps);
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public void initializeMaps(AutomationPackageFragmentYaml fragment) {
        urlToYamlFragment.put(fragment.getFragmentUrl(), fragment);
        for (YamlPlan p: fragment.getPlans()) {
            Plan plan = descriptorReader.getPlanReader().yamlPlanToPlan(p);
            planToYamlPlan.put(plan, p);
            planToYamlFragment.put(plan, fragment);
        };
    }

    public Iterable<Plan> getPlans() {
        return planToYamlPlan.keySet();
    }

    public Plan savePlan(Plan p) {
        try {
            YamlPlan newYamlPlan = descriptorReader.getPlanReader().planToYamlPlan(p);

            AutomationPackageFragmentYaml fragment = planToYamlFragment.get(p);
            if (fragment == null) {
                fragment = fragmentForNewPlan(p);
                planToYamlFragment.put(p, fragment);
                urlToYamlFragment.put(fragment.getFragmentUrl(), fragment);
                addFragmentEntity(fragment, fragment.getPlans(), newYamlPlan);
            } else {
                YamlPlan yamlPlan = planToYamlPlan.get(p);
                modifyFragmentEntity(fragment, fragment.getPlans(), yamlPlan, newYamlPlan);
            }
            planToYamlPlan.put(p, newYamlPlan);
            writeFragmentToDisk(fragment);

            return p;
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private <T extends PatchableYamlArtefact>  void addFragmentEntity(AutomationPackageFragmentYaml fragment, List<T> entityList, T newEntity) {

        try {
            String collectionName = newEntity.getCollectionName();
            String yaml = fragment.getCurrentYaml();

            if (!entityList.isEmpty()) {
                T lastEntity = entityList.get(entityList.size()-1);
                String listItemIndent = yaml.substring(lastEntity.getStartListItemOffset(), lastEntity.getStartOffset());
                String indent = " ".repeat(lastEntity.getIndent());
                String entityYaml = entityStringWithIndent(indent, newEntity);
                String newYaml = yaml.substring(0, lastEntity.getEndOffset())
                        + listItemIndent + entityYaml + yaml.substring(lastEntity.getEndOffset());
                entityList.add(newEntity);
                fragment.setCurrentYaml(newYaml);
            } else {
                entityList.add(newEntity);
                String listYaml = collectionName + ":\n"
                        + entityStringWithIndent("", entityList);

                if (yaml == null) {
                    yaml = "---\n" + listYaml;
                } else {
                    yaml = removeEmptyCollection(collectionName, yaml).trim()
                        + "\n" + listYaml;
                }

                fragment.setCurrentYaml(yaml);
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        updateFragmentObjectOffsets(fragment);
    }

    private <T extends PatchableYamlArtefact> void modifyFragmentEntity(AutomationPackageFragmentYaml fragment, List<T> entityList, T oldEntity, T newEntity){
        try {
            entityList.replaceAll(plan -> plan == oldEntity ? newEntity : plan);

            String oldString = fragment.getCurrentYaml();

            int indent = oldEntity.getIndent();
            String indentString = " ".repeat(indent);
            String newArtefactString = entityStringWithIndent(indentString, newEntity);
            String newString = oldString.substring(0, oldEntity.getStartOffset())
                    + newArtefactString + oldString.substring(oldEntity.getEndOffset());
            fragment.setCurrentYaml(newString);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        updateFragmentObjectOffsets(fragment);
    }

    private String entityStringWithIndent(String indentString, Object entity) throws JsonProcessingException {
        return descriptorReader
                .getYamlObjectMapper()
                .writeValueAsString(entity)
                .replaceAll("---\n", "")
                .trim()
                .replaceAll("\n", "\n" + indentString);
    }

    private  <T extends PatchableYamlArtefact> void removeFragmentEntity(AutomationPackageFragmentYaml fragment, List<T> entityList, T entity) {
        String oldString = fragment.getCurrentYaml();

        int s = entityList.isEmpty() ? entity.getStartListOffset() : entity.getStartListItemOffset();
        String newString = oldString.substring(0, s) + oldString.substring(entity.getEndOffset());

        fragment.setCurrentYaml(newString);
        updateFragmentObjectOffsets(fragment);
    }

    private String removeEmptyCollection(String collectionName, String yaml) {
        return yaml.replaceAll("\n*" + collectionName + ":.*\n*", "\n");
    }

    private AutomationPackageFragmentYaml fragmentForNewPlan(Plan p) throws MalformedURLException {

        String planFragmentPath = properties.getProperty(PROPERTY_NEW_PLAN_FRAGMENT_PATH, descriptorYaml.getFragmentUrl().getPath());
        planFragmentPath = planFragmentPath.replaceAll("\\%name\\%", sanitizeFilename(p.getAttribute(AbstractOrganizableObject.NAME)));

        Path path = new File(planFragmentPath).toPath();
        if (!path.isAbsolute()) {
            Path apRoot = Path.of(descriptorYaml.getFragmentUrl().getPath())
                    .getParent();
            path = apRoot.resolve(path);
        }

        URL url = path.toUri().toURL();

        if (urlToYamlFragment.containsKey(url)) return urlToYamlFragment.get(url);
        AutomationPackageFragmentYaml fragment =  new AutomationPackageFragmentYamlImpl();
        fragment.setFragmentUrl(url);
        return fragment;
    }

    public String sanitizeFilename(String inputName) {
        return inputName.replaceAll("[^a-zA-Z0-9-_\\.]", "_");
    }

    public void removePlan(Plan p) {
        AutomationPackageFragmentYaml fragment = planToYamlFragment.get(p);
        YamlPlan yamlPlan = planToYamlPlan.get(p);

        fragment.getPlans().remove(yamlPlan);

        planToYamlPlan.remove(p);
        planToYamlFragment.remove(p);

        removeFragmentEntity(fragment, fragment.getPlans(), yamlPlan);
        writeFragmentToDisk(fragment);
    }

    private void updateFragmentObjectOffsets(AutomationPackageFragmentYaml fragment) {
        try {
            ObjectMapper mapper = descriptorReader.getYamlObjectMapper();
            JsonParser parser = new PatchingParserDelegate(mapper.createParser(fragment.getCurrentYaml()));

            AutomationPackageFragmentYaml newFragment = descriptorReader.getYamlObjectMapper().readValue(parser, fragment.getClass());
            updateFragmentObjectOffsets(newFragment.getPlans(), fragment.getPlans());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private <T extends PatchableYamlArtefact> void updateFragmentObjectOffsets(List<T> newOffsetEntities, List<T> entities) {
        Iterator<T> newIt = newOffsetEntities.iterator();
        Iterator<T> it = entities.iterator();

        while (newIt.hasNext() && it.hasNext()) {
            it.next().setPatchingBounds(newIt.next());
        }
    }

    private void writeFragmentToDisk(AutomationPackageFragmentYaml fragment) {
        try {
            File file = new File(fragment.getFragmentUrl().toURI());
            FileUtils.writeStringToFile(file, fragment.getCurrentYaml(), StandardCharsets.UTF_8);
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
