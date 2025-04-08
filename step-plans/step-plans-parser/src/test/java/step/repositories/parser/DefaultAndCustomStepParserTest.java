package step.repositories.parser;

import org.junit.Assert;
import org.junit.Test;
import step.artefacts.CallFunction;
import step.artefacts.Echo;
import step.artefacts.ForBlock;
import step.artefacts.Sequence;
import step.core.artefacts.AbstractArtefact;
import step.core.plans.Plan;
import step.plans.nl.RootArtefactType;
import step.plans.nl.parser.PlanParser;

public class DefaultAndCustomStepParserTest {



    @Test
    public void testComplexPlans() throws StepsParser.ParsingException {

        StringBuilder sb = new StringBuilder();
        sb.append("//For loop\n");
        sb.append("For 1 to 2 \n");
        sb.append("//Call a keyword\n");
        sb.append("CallFunction\n");
        sb.append("//Assert - City equals Basel\n");
        sb.append("Assert City = \"Basel\"\n");
        sb.append("//BeforeSequence\n");
        sb.append("BeforeSequence\n");
        sb.append("//some echo in before sequence\n");
        sb.append("Echo  \"${counter}\"\n");
        sb.append("//before sequence end\n");
        sb.append("End\n");
        sb.append("//some echo in iteration\n");
        sb.append("Echo  \"${counter}\"\n");
        sb.append("//AfterSequence\n");
        sb.append("AfterSequence\n");
        sb.append("//some echo in afterSequence\n");
        sb.append("Echo  \"${counter}\"\n");
        sb.append("//after sequence end\n");
        sb.append("End\n");
        sb.append("//After section\n");
        sb.append("After\n");
        sb.append("//some echo in after section\n");
        sb.append("Echo in after\n");
        sb.append("//After end\n");
        sb.append("End\n");
        sb.append("//some other echo in iteration\n");
        sb.append("Echo  \"${counter}\"\n");
        sb.append("//For end\n");
        sb.append("End\n");

        PlanParser parser = new PlanParser();

        Plan parsedPlan = parser.parse(sb.toString(), RootArtefactType.Sequence);
        AbstractArtefact root = parsedPlan.getRoot();
        Assert.assertEquals(1,root.getChildren().size());

        ForBlock forBlock = (ForBlock) root.getChildren().get(0);
        Assert.assertEquals("For loop", forBlock.getAttributes().get("name"));

        Sequence wrappingSequence = (Sequence) forBlock.getChildren().get(0);
        Assert.assertEquals("Sequence", wrappingSequence.getAttributes().get("name"));

        Assert.assertEquals(3, wrappingSequence.getChildren().size());

        CallFunction callFunction = (CallFunction) wrappingSequence.getChildren().get(0);
        Assert.assertEquals("Call a keyword", callFunction.getAttributes().get("name"));
        Assert.assertEquals(1,callFunction.getChildren().size());

        Assert.assertEquals("some echo in iteration", wrappingSequence.getChildren().get(1).getAttributes().get("name"));
        Assert.assertEquals("some other echo in iteration", wrappingSequence.getChildren().get(2).getAttributes().get("name"));

        Echo echo = (Echo) wrappingSequence.getAfter().getSteps().get(0);
        Assert.assertEquals("some echo in afterSequence", echo.getAttributes().get("name"));

        Echo echoBeforeSequence = (Echo) wrappingSequence.getBefore().getSteps().get(0);
        Assert.assertEquals("some echo in before sequence", echoBeforeSequence.getAttributes().get("name"));

        Echo echoAfter = (Echo) forBlock.getAfter().getSteps().get(0);
        Assert.assertEquals("some echo in after section", echoAfter.getAttributes().get("name"));
    }
}
