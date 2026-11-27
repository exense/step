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

import step.artefacts.Sequence;
import step.artefacts.ThreadGroup;
import step.core.plans.Plan;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * A plan using every children slot an artefact can have: the main children, the before and after blocks
 * of {@link AbstractArtefact} and the before and after thread blocks of {@link ThreadGroup}. Every slot
 * holds a leaf, created by the given factory and labelled after its location.
 * <pre>
 * Sequence (root)
 *   before:       [root.before]
 *   children:     [ThreadGroup]
 *                   before:       [threadGroup.before]
 *                   beforeThread: [Sequence]
 *                                   children: [threadGroup.beforeThread.child]
 *                                   after:    [threadGroup.beforeThread.after]
 *                   children:     [threadGroup.child]
 *                   afterThread:  [threadGroup.afterThread]
 *                   after:        [threadGroup.after]
 *   after:        [root.after]
 * </pre>
 */
public class AllSlotsArtefactTree {

    public final Plan plan;
    public final Map<String, AbstractArtefact> leaves = new LinkedHashMap<>();
    public final List<AbstractArtefact> allArtefacts = new ArrayList<>();

    public AllSlotsArtefactTree(Function<String, AbstractArtefact> leafFactory) {
        Sequence root = register(new Sequence());
        root.setBefore(block(leaf("root.before", leafFactory)));
        root.setAfter(block(leaf("root.after", leafFactory)));

        ThreadGroup threadGroup = register(new ThreadGroup());
        root.addChild(threadGroup);
        threadGroup.setBefore(block(leaf("threadGroup.before", leafFactory)));
        threadGroup.addChild(leaf("threadGroup.child", leafFactory));
        threadGroup.setAfterThread(block(leaf("threadGroup.afterThread", leafFactory)));
        threadGroup.setAfter(block(leaf("threadGroup.after", leafFactory)));

        Sequence beforeThreadSequence = register(new Sequence());
        threadGroup.setBeforeThread(block(beforeThreadSequence));
        beforeThreadSequence.addChild(leaf("threadGroup.beforeThread.child", leafFactory));
        beforeThreadSequence.setAfter(block(leaf("threadGroup.beforeThread.after", leafFactory)));

        plan = new Plan(root);
    }

    private AbstractArtefact leaf(String label, Function<String, AbstractArtefact> leafFactory) {
        AbstractArtefact leaf = register(leafFactory.apply(label));
        leaves.put(label, leaf);
        return leaf;
    }

    private <T extends AbstractArtefact> T register(T artefact) {
        allArtefacts.add(artefact);
        return artefact;
    }

    private static ChildrenBlock block(AbstractArtefact step) {
        ChildrenBlock block = new ChildrenBlock();
        block.addStep(step);
        return block;
    }
}
