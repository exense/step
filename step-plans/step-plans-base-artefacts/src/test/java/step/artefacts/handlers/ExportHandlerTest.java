/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
 *  
 * This file is part of STEP
 *  
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *  
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package step.artefacts.handlers;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import org.junit.Test;

import com.google.common.io.Files;

import ch.exense.commons.io.FileHelper;
import junit.framework.Assert;
import step.artefacts.BaseArtefactPlugin;
import step.artefacts.Export;
import step.artefacts.Sequence;
import step.core.artefacts.CheckArtefact;
import step.core.dynamicbeans.DynamicValue;
import step.core.execution.ExecutionEngine;
import step.core.plans.Plan;
import step.core.plans.builder.PlanBuilder;

public class ExportHandlerTest {

	@Test
	public void test() throws IOException {
		File file = getTestFolder();
		
		Export e = new Export();
		e.setValue(new DynamicValue<>("report.attachments", "groovy"));
		e.getFile().setValue(file.getAbsolutePath());
		buildAndRunPlan(e);
		
		File exceptionLogFile = new File(file.getAbsolutePath()+"/exception.log");
		String firstLine = Files.readFirstLine(exceptionLogFile, Charset.defaultCharset());
		Assert.assertEquals("java.lang.RuntimeException", firstLine);
		exceptionLogFile.delete();
		
	}
	
	@Test
	public void testPrefix() throws IOException {
		File file = getTestFolder();
		
		Export e = new Export();
		e.setPrefix(new DynamicValue<String>("MyPrefix_"));
		e.setValue(new DynamicValue<>("report.attachments", "groovy"));
		e.getFile().setValue(file.getAbsolutePath());
		buildAndRunPlan(e);
		
		File exceptionLogFile = new File(file.getAbsolutePath()+"/MyPrefix_exception.log");
		String firstLine = Files.readFirstLine(exceptionLogFile, Charset.defaultCharset());
		Assert.assertEquals("java.lang.RuntimeException", firstLine);
		exceptionLogFile.delete();
		
	}
	
	@Test
	public void testFilter() throws IOException {
		File file = getTestFolder();
		
		Export e = new Export();
		e.setPrefix(new DynamicValue<String>("MyPrefix2_"));
		e.setValue(new DynamicValue<>("report.attachments", "groovy"));
		e.getFile().setValue(file.getAbsolutePath());
		e.setFilter(new DynamicValue<String>("notmatching"));

		buildAndRunPlan(e);
		
		File exceptionLogFile = new File(file.getAbsolutePath()+"/MyPrefix2_exception.log");
		Assert.assertFalse(exceptionLogFile.exists());
	}
	
	protected File getTestFolder() {
		File file = FileHelper.getClassLoaderResourceAsFile(this.getClass().getClassLoader(), "exportTest/test");
		file = file.getParentFile();
		return file;
	}
	
	protected void buildAndRunPlan(Export e) throws IOException {
		Sequence s = new Sequence();
		s.getContinueOnError().setValue(true);
		Plan plan = PlanBuilder.create().startBlock(s).add(new CheckArtefact(c->{
			throw new RuntimeException();
		})).add(e).endBlock().build();
		try (ExecutionEngine engine = ExecutionEngine.builder().withPlugin(new BaseArtefactPlugin()).build()) {
			engine.execute(plan);
		}
	}
}
