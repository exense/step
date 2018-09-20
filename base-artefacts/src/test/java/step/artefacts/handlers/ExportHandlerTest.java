package step.artefacts.handlers;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import org.junit.Test;

import com.google.common.io.Files;

import junit.framework.Assert;
import step.artefacts.CheckArtefact;
import step.artefacts.Export;
import step.artefacts.Sequence;
import step.commons.helpers.FileHelper;
import step.core.dynamicbeans.DynamicValue;
import step.core.plans.Plan;
import step.core.plans.builder.PlanBuilder;
import step.core.plans.runner.DefaultPlanRunner;

public class ExportHandlerTest {

	@Test
	public void test() throws IOException {
		Sequence s = new Sequence();
		s.getContinueOnError().setValue(true);
		
		File file = FileHelper.getClassLoaderResource(this.getClass(), "exportTest/test");
		file = file.getParentFile();
		
		Export e = new Export();
		e.setValue(new DynamicValue<>("report.attachments", "groovy"));
		e.getFile().setValue(file.getAbsolutePath());
		Plan plan = PlanBuilder.create().startBlock(s).add(new CheckArtefact(c->{
			throw new RuntimeException();
		})).add(e).endBlock().build();
		DefaultPlanRunner runner = new DefaultPlanRunner();
		runner.run(plan).printTree();
		
		File exceptionLogFile = new File(file.getAbsolutePath()+"/exception.log");
		String firstLine = Files.readFirstLine(exceptionLogFile, Charset.defaultCharset());
		Assert.assertEquals("java.lang.RuntimeException", firstLine);
		file.delete();
		
	}
}
