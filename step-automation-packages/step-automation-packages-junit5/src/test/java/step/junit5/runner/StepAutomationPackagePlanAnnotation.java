package step.junit5.runner;

import step.handlers.javahandler.AbstractKeyword;
import step.handlers.javahandler.Keyword;
import step.junit.runners.annotations.Plan;
import step.junit.runners.annotations.PlanCategories;

public class StepAutomationPackagePlanAnnotation extends AbstractKeyword {

    @Plan
    @PlanCategories({"My Category A", "My Category C"})
    @Keyword(name = "Local Keyword Category A and C")
    public void localKeywordAC(){

    }

    @Plan
    @PlanCategories({"My Category A"})
    @Keyword(name = "Local Keyword Category A")
    public void localKeywordA(){

    }

    @Plan
    @PlanCategories({"My Category B"})
    @Keyword(name = "Local Keyword Category B")
    public void localKeywordB(){

    }

    @Plan
    @PlanCategories({"My Category A", "My Category B"})
    @Keyword(name = "Local Keyword Category A and B")
    public void localKeywordAB(){

    }

    @Plan
    @PlanCategories({"My Category D"})
    @Keyword(name = "Local Keyword Category D")
    public void localKeywordD(){

    }

    @Plan
    @PlanCategories({"My Category E"})
    @Keyword(name = "Local Keyword Category E")
    public void localKeywordE(){

    }
}
