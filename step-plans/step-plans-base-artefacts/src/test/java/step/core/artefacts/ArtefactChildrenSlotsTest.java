/*
 * Copyright (C) 2026, exense GmbH
 *
 * This file is part of Step
 *
 * Step is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Step is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Step.  If not, see <http://www.gnu.org/licenses/>.
 */
package step.core.artefacts;

import org.bson.types.ObjectId;
import org.junit.Test;
import step.artefacts.CallPlan;
import step.artefacts.Echo;
import step.core.entities.Entity;
import step.core.entities.EntityConstants;
import step.core.entities.EntityManager;
import step.core.plans.InMemoryPlanAccessor;
import step.core.plans.Plan;
import step.core.plans.PlanNavigator;

import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

/**
 * Every operation walking an artefact tree has to reach the artefacts of all children slots, not only the
 * main children. See {@link AllSlotsArtefactTree} for the slots covered.
 */
public class ArtefactChildrenSlotsTest {

    private static final String CUSTOM_ATTRIBUTE = "source";

    @Test
    public void planNavigatorFindsArtefactsOfAllSlots() {
        AllSlotsArtefactTree tree = new AllSlotsArtefactTree(ArtefactChildrenSlotsTest::echo);
        PlanNavigator planNavigator = new PlanNavigator(tree.plan);

        tree.allArtefacts.forEach(artefact ->
            assertSame(artefact, planNavigator.findArtefactById(artefact.getId().toString())));
        assertNull(planNavigator.findArtefactById(new ObjectId().toString()));
    }

    @Test
    public void deepCleanupAllCustomAttributesCleansAllSlots() {
        AllSlotsArtefactTree tree = new AllSlotsArtefactTree(ArtefactChildrenSlotsTest::echo);
        tree.allArtefacts.forEach(artefact -> artefact.addCustomAttribute(CUSTOM_ATTRIBUTE, "original plan"));

        tree.plan.getRoot().deepCleanupAllCustomAttributes();

        tree.allArtefacts.forEach(artefact ->
            assertNull(label(tree, artefact), artefact.getCustomAttribute(CUSTOM_ATTRIBUTE)));
    }

    @Test
    public void deepAssignNewIdsAssignsNewIdsInAllSlots() {
        AllSlotsArtefactTree tree = new AllSlotsArtefactTree(ArtefactChildrenSlotsTest::echo);
        // identity based, as the hash code of an artefact depends on its id
        Map<AbstractArtefact, ObjectId> originalIds = new IdentityHashMap<>();
        tree.allArtefacts.forEach(artefact -> originalIds.put(artefact, artefact.getId()));

        tree.plan.getRoot().deepAssignNewIds();

        tree.allArtefacts.forEach(artefact ->
            assertNotEquals(label(tree, artefact), originalIds.get(artefact), artefact.getId()));
        Set<ObjectId> newIds = tree.allArtefacts.stream().map(AbstractArtefact::getId).collect(Collectors.toSet());
        assertEquals(tree.allArtefacts.size(), newIds.size());
    }

    /**
     * The import of a plan relies on the entity references declared on the model to remap the ids of the
     * entities it refers to.
     */
    @Test
    public void entityReferencesAreUpdatedInAllSlots() {
        Map<String, String> oldToNewPlanIds = new HashMap<>();
        AllSlotsArtefactTree tree = new AllSlotsArtefactTree(label -> {
            CallPlan callPlan = new CallPlan();
            String oldPlanId = new ObjectId().toString();
            callPlan.setPlanId(oldPlanId);
            oldToNewPlanIds.put(oldPlanId, new ObjectId().toString());
            return callPlan;
        });
        EntityManager entityManager = new EntityManager();
        entityManager.register(new Entity<>(EntityConstants.plans, new InMemoryPlanAccessor(), Plan.class));
        Map<String, String> expectedPlanIds = new HashMap<>();
        tree.leaves.forEach((label, leaf) ->
            expectedPlanIds.put(label, oldToNewPlanIds.get(((CallPlan) leaf).getPlanId())));

        entityManager.updateReferences(tree.plan, oldToNewPlanIds, null, new HashSet<>());

        tree.leaves.forEach((label, leaf) ->
            assertEquals(label, expectedPlanIds.get(label), ((CallPlan) leaf).getPlanId()));
    }

    private static AbstractArtefact echo(String label) {
        Echo echo = new Echo();
        echo.addAttribute(AbstractArtefact.NAME, label);
        return echo;
    }

    private static String label(AllSlotsArtefactTree tree, AbstractArtefact artefact) {
        return tree.leaves.entrySet().stream().filter(e -> e.getValue() == artefact).map(Map.Entry::getKey)
            .findFirst().orElse(artefact.getClass().getSimpleName());
    }
}
