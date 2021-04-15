package step.plans.nl;

import step.core.artefacts.AbstractArtefact;

import java.util.function.Supplier;

/**
 * Enumeration defining the supported root artefacts for plain-text plans.
 * This corresponds to the selections available in the UI.
 */
public enum RootArtefactType {
    // Note: The names are Camel-cased by design, so they align with the class and template names used in the UI.
    Sequence(step.artefacts.Sequence::new),
    TestCase(step.artefacts.TestCase::new),
    TestScenario(step.artefacts.TestScenario::new),
    TestSet(step.artefacts.TestSet::new),
    ThreadGroup(step.artefacts.ThreadGroup::new);

    private final Supplier<AbstractArtefact> artefactConstructor;

    public AbstractArtefact createRootArtefact() {
        return artefactConstructor.get();
    }

    RootArtefactType(Supplier<AbstractArtefact> artefactConstructor) {
        this.artefactConstructor = artefactConstructor;
    }
}
