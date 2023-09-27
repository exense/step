package step.repositories.artifact;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.plans.Plan;
import step.functions.Function;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

public class StepJarParserTest {

	private static final Logger log = LoggerFactory.getLogger(StepJarParserTest.class);

	@Test
	public void test() throws Exception {
		StepJarParser stepJarParser = new StepJarParser();
		File jarFile = new File("src/test/resources/step/repositories/artifact/step-junit-tests.jar");

		// jar file is built for tests classes from step-junit module and contains various plans including inline plan and plan defined in Yaml format
		StepJarParser.PlansParsingResult plansForJar = stepJarParser.getPlansForJar(jarFile, null, new String[]{}, new String[]{}, new String[]{}, new String[]{});
		List<String> planNames = plansForJar.getPlans().stream().map(p -> p.getAttribute("name")).collect(Collectors.toList());
		log.info("Plans parsed in jar: {}", planNames);
		Assert.assertEquals(7, planNames.size());
		Assert.assertTrue(planNames.contains("implicitPlanWithWithCustomKeywordName"));
		Assert.assertTrue(planNames.contains("planWithAssert"));
		Assert.assertTrue(planNames.contains("explicitPlanWithExecutionParameter"));
		Assert.assertTrue(planNames.contains("inlinePlan"));
		Assert.assertTrue(planNames.contains("plan2.plan"));
		Assert.assertTrue(planNames.contains("plan3.plan"));
		Assert.assertTrue(planNames.contains("composite-simple-plan.yml"));

		// yaml plan (composite-simple-plan) uses the keyword 'callExisting3', and this keyword should be included in plan
		Plan yamlPlan = plansForJar.getPlans().stream().filter(p -> p.getAttribute("name").equals("composite-simple-plan.yml")).findFirst().orElseThrow();
		Function function = yamlPlan.getFunctions().stream().filter(f -> f.getAttribute("name").equals("callExisting3")).findFirst().orElse(null);
		Assert.assertNotNull("The 'callExisting3' function is not found in composite-simple-plan", function);
	}

	@Test
	public void testInvalid() throws Exception {
		StepJarParser stepJarParser = new StepJarParser();
		File jarFile = new File("src/test/resources/step/repositories/artifact/step-junit-tests-invalid.jar");

		// jar file is built for tests classes from step-junit module and contains various plans including inline plan and INVALID plan defined in Yaml format
		try {
			stepJarParser.getPlansForJar(jarFile, null, new String[]{}, new String[]{}, new String[]{}, new String[]{});
			Assert.fail("Exception is not thrown");
		} catch (Exception ex){
			// ok
			log.info("Exception caught", ex);
		}
	}

}
