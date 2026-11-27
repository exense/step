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
package step.automation.packages;

import org.junit.Test;
import step.artefacts.ForEachBlock;
import step.core.artefacts.AllSlotsArtefactTree;
import step.core.dynamicbeans.DynamicValue;
import step.datapool.file.FileDataPool;

import static org.junit.Assert.assertEquals;

/**
 * The resource references of an automation package plan are rewritten in every children slot, the thread
 * blocks of a thread group included.
 */
public class ResourceReferencesChildrenSlotsTest {

    private static final String MAPPED_PREFIX = "mapped:";

    @Test
    public void rewritesResourceReferencesOfAllSlots() {
        AllSlotsArtefactTree tree = new AllSlotsArtefactTree(label -> {
            FileDataPool dataPool = new FileDataPool();
            dataPool.setFile(new DynamicValue<>(label + ".csv"));
            ForEachBlock forEachBlock = new ForEachBlock();
            forEachBlock.setDataSource(dataPool);
            return forEachBlock;
        });

        ResourceReferences.apply(tree.plan.getRoot(), reference -> MAPPED_PREFIX + reference);

        tree.leaves.forEach((label, leaf) -> {
            FileDataPool dataPool = (FileDataPool) ((ForEachBlock) leaf).getDataSource();
            assertEquals(label, MAPPED_PREFIX + label + ".csv", dataPool.getFile().get());
        });
    }
}
