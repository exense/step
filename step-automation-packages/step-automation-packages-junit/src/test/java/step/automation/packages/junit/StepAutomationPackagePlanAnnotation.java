package step.automation.packages.junit;

import step.handlers.javahandler.AbstractKeyword;
import step.handlers.javahandler.Keyword;
import step.junit.runners.annotations.Plan;

public class StepAutomationPackagePlanAnnotation extends AbstractKeyword {

    @Plan
    @Keyword(name = "Local Keyword")
    public void localKeyword(){

    }
}
