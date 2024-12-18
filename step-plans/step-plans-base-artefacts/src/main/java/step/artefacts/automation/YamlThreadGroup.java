package step.artefacts.automation;

import step.artefacts.ThreadGroup;
import step.core.artefacts.ChildrenBlock;
import step.core.dynamicbeans.DynamicValue;
import step.core.yaml.YamlFieldCustomCopy;
import step.core.yaml.model.AbstractYamlArtefact;
import step.core.yaml.model.YamlChildrenBlock;

public class YamlThreadGroup extends AbstractYamlArtefact<ThreadGroup> {
    DynamicValue<Integer> users = new DynamicValue<Integer>(1);

    DynamicValue<Integer> iterations = new DynamicValue<Integer>(1);

    DynamicValue<Integer> rampup = new DynamicValue<Integer>(null);

    DynamicValue<Integer> pack = new DynamicValue<Integer>(null);

    DynamicValue<Integer> pacing = new DynamicValue<Integer>(null);

    DynamicValue<Integer> startOffset = new DynamicValue<Integer>(0);

    DynamicValue<Integer> maxDuration = new DynamicValue<Integer>(0);

    DynamicValue<String> item = new DynamicValue<String>("gcounter");

    DynamicValue<String> localItem = new DynamicValue<String>("literationId");

    DynamicValue<String> userItem = new DynamicValue<String>("userId");

    @YamlFieldCustomCopy
    YamlChildrenBlock beforeThread;
    @YamlFieldCustomCopy
    YamlChildrenBlock afterThread;

    public YamlThreadGroup() {
        super(ThreadGroup.class);
    }



    @Override
    protected void fillArtefactFields(ThreadGroup res) {
        super.fillArtefactFields(res);
        if (this.beforeThread != null) {
            res.setBeforeThread(this.beforeThread.toArtefact());
        }
        if (this.afterThread != null) {
            res.setAfterThread(this.afterThread.toArtefact());
        }
    }

    @Override
    protected void fillYamlArtefactFields(ThreadGroup artefact) {
        super.fillYamlArtefactFields(artefact);
        ChildrenBlock beforeThread1 = artefact.getBeforeThread();
        if (beforeThread1 != null && !beforeThread1.getSteps().isEmpty()) {
            this.beforeThread = YamlChildrenBlock.toYamlChildrenBlock(beforeThread1, this.getYamlObjectMapper());
        }
        ChildrenBlock afterThread1 = artefact.getAfterThread();
        if (afterThread1 != null && !afterThread1.getSteps().isEmpty()) {
            this.afterThread = YamlChildrenBlock.toYamlChildrenBlock(afterThread1, this.getYamlObjectMapper());
        }
    }
}
