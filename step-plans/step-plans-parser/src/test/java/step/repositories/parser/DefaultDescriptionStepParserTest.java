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
package step.repositories.parser;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import jakarta.json.Json;
import jakarta.json.JsonObject;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import step.artefacts.*;
import step.artefacts.ThreadGroup;
import step.core.artefacts.AbstractArtefact;
import step.datapool.excel.ExcelDataPool;
import step.datapool.file.FileDataPool;
import step.datapool.sequence.IntSequenceDataPool;
import step.repositories.parser.StepsParser.ParsingException;
import step.repositories.parser.annotated.DefaultDescriptionStepParser;

public class DefaultDescriptionStepParserTest extends AbstractDescriptionStepParserTest {

	@Before
	public void setUp() {
		parser = StepsParser.builder().withStepParsers(new DefaultDescriptionStepParser()).build();
	}
	
	@Test
	public void testFor() throws ParsingException {
		List<AbstractStep> steps = new ArrayList<>();
		steps.add(step("For 1 to 20"));
		steps.add(step("Echo test"));
		steps.add(step("End"));

		ForBlock forBlock = parseAndGetUniqueChild(steps, ForBlock.class);
		IntSequenceDataPool dataSource = (IntSequenceDataPool) forBlock.getDataSource();
		Assert.assertEquals(1,(int)dataSource.getStart().getValue());
		Assert.assertEquals(20,(int)dataSource.getEnd().getValue());

		Assert.assertEquals(1,getChildren(forBlock).size());
	}
	
	@Test
	public void testForEach() throws ParsingException {
		String filepath = "TestForEach.txt";
		List<AbstractStep> steps = new ArrayList<>();
		steps.add(step("For each row in file \""+filepath+"\""));
		steps.add(step("Echo test"));
		steps.add(step("End"));

		ForEachBlock forEachBlock = parseAndGetUniqueChild(steps, ForEachBlock.class);
		FileDataPool dataSource = (FileDataPool) forEachBlock.getDataSource();
		
		Assert.assertEquals("\""+filepath+"\"", dataSource.getFile().getExpression());
		//Assert.assertEquals(true, dataSource.getHeaders());
		Assert.assertEquals(false, dataSource.getForWrite().get());

		Assert.assertEquals(1,getChildren(forEachBlock).size());
	}
	
	@Test
	public void testForEachRowInAttachedExcel() throws ParsingException {
		List<AbstractStep> steps = new ArrayList<>();
		steps.add(step("For each row in attached excel"));
		steps.add(step("Echo test"));
		steps.add(step("End"));

		ForEachBlock forEachBlock = parseAndGetUniqueChild(steps, ForEachBlock.class);
		ExcelDataPool dataSource = (ExcelDataPool) forEachBlock.getDataSource();
		
		Assert.assertEquals("resourceManager.getResourceFile(currentArtefact.getAttachments().get(0).toString()).getResourceFile().getAbsolutePath()",
				dataSource.getFile().getExpression());
		Assert.assertEquals(true, dataSource.getHeaders().get());
		Assert.assertEquals(false, dataSource.getForWrite().get());

		Assert.assertEquals(1,getChildren(forEachBlock).size());
	}
	
	@Test
	public void testForEachRowInExcel() throws ParsingException {		
		List<AbstractStep> steps = new ArrayList<>();
		steps.add(step("For each row in excel   \"path/to/excel.xlsx\"   "));
		steps.add(step("Echo test"));
		steps.add(step("End"));

		ForEachBlock forEachBlock = parseAndGetUniqueChild(steps, ForEachBlock.class);
		ExcelDataPool dataSource = (ExcelDataPool) forEachBlock.getDataSource();
		
		Assert.assertEquals("\"path/to/excel.xlsx\"", dataSource.getFile().getExpression());
		Assert.assertEquals(true, dataSource.getHeaders().get());
		Assert.assertEquals(false, dataSource.getForWrite().get());

		Assert.assertEquals(1,getChildren(forEachBlock).size());
	}
	
	@Test
	public void testForEachDefaults() throws ParsingException {
		String filepath = "TestForEach.txt";
		List<AbstractStep> steps = new ArrayList<>();
		steps.add(step("ForEach SourceType = \"excel\" File =\""+filepath+"\""));
		steps.add(step("Echo test"));
		steps.add(step("End"));

		ForEachBlock forEachBlock = parseAndGetUniqueChild(steps, ForEachBlock.class);
		ExcelDataPool dataSource = (ExcelDataPool) forEachBlock.getDataSource();
		
		Assert.assertEquals("\""+filepath+"\"", dataSource.getFile().getExpression());
		//Assert.assertEquals(true, dataSource.getHeaders());
		Assert.assertEquals(false, dataSource.getForWrite().get());
		Assert.assertEquals(1, (int) forEachBlock.getThreads().getValue());
		Assert.assertEquals("row", forEachBlock.getItem().getValue());

		Assert.assertEquals(1,getChildren(forEachBlock).size());
	}
	
	@Test
	public void testForEachExcel() throws ParsingException {
		String filepath = "TestForEach.txt";
		List<AbstractStep> steps = new ArrayList<>();
		steps.add(step("ForEach Threads=22 RowHandle = \"blabla\" SourceType = \"excel\" File =\""+filepath+"\""));
		steps.add(step("Echo test"));
		steps.add(step("End"));

		ForEachBlock forEachBlock = parseAndGetUniqueChild(steps, ForEachBlock.class);
		ExcelDataPool dataSource = (ExcelDataPool) forEachBlock.getDataSource();
		
		Assert.assertEquals("\""+filepath+"\"", dataSource.getFile().getExpression());
		//Assert.assertEquals(true, dataSource.getHeaders());
		Assert.assertEquals(false, dataSource.getForWrite().get());
		Assert.assertEquals("22", forEachBlock.getThreads().getExpression());
		Assert.assertEquals("\"blabla\"", forEachBlock.getItem().getExpression());

		Assert.assertEquals(1,getChildren(forEachBlock).size());
	}
	
	@Test
	public void testForEachExcelWithWorksheet() throws ParsingException {
		String filepath = "TestForEach.txt";
		List<AbstractStep> steps = new ArrayList<>();
		steps.add(step("ForEach Threads=22 RowHandle = \"blabla\" SourceType = \"excel\" File =\""+filepath+"\" Worksheet=\"sheet2\""));
		steps.add(step("Echo test"));
		steps.add(step("End"));

		ForEachBlock forEachBlock = parseAndGetUniqueChild(steps, ForEachBlock.class);
		ExcelDataPool dataSource = (ExcelDataPool) forEachBlock.getDataSource();
		
		Assert.assertEquals("\"sheet2\"", dataSource.getWorksheet().getExpression());
		Assert.assertEquals(1,getChildren(forEachBlock).size());
	}
	
	@Test
	public void testDataSet() throws ParsingException {
		String filepath = "TestForEach.txt";
		List<AbstractStep> steps = new ArrayList<>();
		steps.add(step("DataSet IteratorHandle = \"blabla\" SourceType = \"excel\" File =\""+filepath+"\""));

		DataSetArtefact artefact = parseAndGetUniqueChild(steps, DataSetArtefact.class);
		ExcelDataPool dataSource = (ExcelDataPool) artefact.getDataSource();
		
		Assert.assertEquals("\""+filepath+"\"", dataSource.getFile().getExpression());
		//Assert.assertEquals(true, dataSource.getHeaders());
		Assert.assertEquals(false, dataSource.getForWrite().get());
		Assert.assertEquals("\"blabla\"", artefact.getItem().getExpression());
		Assert.assertEquals(false, artefact.getResetAtEnd().getValue());
	}
	
	@Test
	public void testSleep() throws ParsingException {
		List<AbstractStep> steps = new ArrayList<>();
		steps.add(step("Sleep 1ms"));
		Sleep sleep = parseAndGetUniqueChild(steps, Sleep.class);
		Assert.assertEquals("1*1",sleep.getDuration().getExpression());
		Assert.assertEquals("ms",sleep.getUnit().getValue());
		
		steps = new ArrayList<>();
		steps.add(step("Sleep 1s"));
		sleep = parseAndGetUniqueChild(steps, Sleep.class);
		Assert.assertEquals("1000*1",sleep.getDuration().getExpression());
		
		steps = new ArrayList<>();
		steps.add(step("Sleep 1m  	"));
		sleep = parseAndGetUniqueChild(steps, Sleep.class);
		Assert.assertEquals("60000*1",sleep.getDuration().getExpression());
		
		steps = new ArrayList<>();
		steps.add(step("Sleep 1h  	"));
		sleep = parseAndGetUniqueChild(steps, Sleep.class);
		Assert.assertEquals("3600000*1",sleep.getDuration().getExpression());
		
		steps = new ArrayList<>();
		steps.add(step("Sleep x s"));
		sleep = parseAndGetUniqueChild(steps, Sleep.class);
		Assert.assertEquals("1000*x",sleep.getDuration().getExpression());
		
		steps = new ArrayList<>();
		steps.add(step("Sleep Duration=1 Unit=s"));
		sleep = parseAndGetUniqueChild(steps, Sleep.class);
		Assert.assertEquals("1",sleep.getDuration().getExpression());
		Assert.assertEquals("s",sleep.getUnit().getExpression());
	}
	
//	steps.add(step("Sleep 1s"));
//	steps.add(step("Sleep 1m"));
//	steps.add(step("Sleep 1h"));
	
	@Test
	public void testWhile() throws ParsingException {
		List<AbstractStep> steps = new ArrayList<>();
		steps.add(step("While Condition= true \n"));
		steps.add(step("End"));

		While artefact = parseAndGetUniqueChild(steps, While.class);
		Assert.assertNotNull(artefact);
		Assert.assertEquals("true",artefact.getCondition().getExpression());
	}
	
	@Test
	public void testWhileComplexExpression() throws ParsingException {
		List<AbstractStep> steps = new ArrayList<>();
		String expression = " true && (false && \" \".equals(' ')) ";
		steps.add(step("While Condition= |"+expression+"| \n"));
		steps.add(step("End"));

		While artefact = parseAndGetUniqueChild(steps, While.class);
		Assert.assertNotNull(artefact);
		Assert.assertEquals(expression,artefact.getCondition().getExpression());
	}
	
	@Test
	// TODO as soon as we have a space in the condition the parser is failing! implement a way to group expressions
	public void testWhileComplexCondition() throws ParsingException {
		List<AbstractStep> steps = new ArrayList<>();
		steps.add(step("While Condition= true&&true \n"));
		steps.add(step("End"));

		While artefact = parseAndGetUniqueChild(steps, While.class);
		Assert.assertNotNull(artefact);
		Assert.assertEquals("true&&true",artefact.getCondition().getExpression());
	}
	
	@Test
	public void testTestSet() throws ParsingException {
		List<AbstractStep> steps = new ArrayList<>();
		steps.add(step("TestSet  \n"));
		steps.add(step("End"));

		TestSet artefact = parseAndGetUniqueChild(steps, TestSet.class);
		Assert.assertNotNull(artefact);
	}
	
	@Test
	public void testTestCase() throws ParsingException {
		List<AbstractStep> steps = new ArrayList<>();
		steps.add(step("TestCase"));
		steps.add(step("End"));

		TestCase artefact = parseAndGetUniqueChild(steps, TestCase.class);
		Assert.assertNotNull(artefact);
	}
	
	@Test
	public void testThreadGroup() throws ParsingException {
		List<AbstractStep> steps = new ArrayList<>();
		steps.add(step("ThreadGroup"));
		steps.add(step("End"));

		ThreadGroup artefact = parseAndGetUniqueChild(steps, ThreadGroup.class);
		Assert.assertNotNull(artefact);
		Assert.assertEquals((int)0, (int)artefact.getMaxDuration().getValue());
	}
	
	@Test
	public void testThreadGroupMaxDuration() throws ParsingException {
		List<AbstractStep> steps = new ArrayList<>();
		steps.add(step("ThreadGroup MaxDuration=10"));
		steps.add(step("End"));

		ThreadGroup artefact = parseAndGetUniqueChild(steps, ThreadGroup.class);
		Assert.assertNotNull(artefact);
		Assert.assertEquals("10", artefact.getMaxDuration().getExpression());
	}
	
	@Test
	public void testTestScenario() throws ParsingException {
		List<AbstractStep> steps = new ArrayList<>();
		steps.add(step("TestScenario"));
		steps.add(step("End"));

		TestScenario artefact = parseAndGetUniqueChild(steps, TestScenario.class);
		Assert.assertNotNull(artefact);
	}
	
	@Test
	public void testSynchronized() throws ParsingException {
		List<AbstractStep> steps = new ArrayList<>();
		steps.add(step("Synchronized  "));
		steps.add(step("End"));

		Synchronized artefact = parseAndGetUniqueChild(steps, Synchronized.class);
		Assert.assertNotNull(artefact);
	}
	
	@Test
	public void testSynchronizedNamed() throws ParsingException {
		List<AbstractStep> steps = new ArrayList<>();
		steps.add(step("Synchronized LockName=\"myLock\" GlobalLock=\"true\""));
		steps.add(step("End"));

		Synchronized artefact = parseAndGetUniqueChild(steps, Synchronized.class);
		Assert.assertEquals("\"myLock\"", artefact.getLockName().getExpression());
		Assert.assertEquals("\"true\"", artefact.getGlobalLock().getExpression());
		Assert.assertNotNull(artefact);
	}
	
	@Test
	public void testGroupOn() throws ParsingException {
		List<AbstractStep> steps = new ArrayList<>();
		steps.add(step("Group on test=\"value\" \"test with space\"=\"value\" \n \"test2\"=value"));
		steps.add(step("Echo test"));
		steps.add(step("End"));

		FunctionGroup group = parseAndGetUniqueChild(steps, FunctionGroup.class);
		
		JsonObject o = Json.createReader(new StringReader(group.getToken().get())).readObject();
		Assert.assertEquals("\"value\"",o.getJsonObject("test").getString("expression"));
		Assert.assertEquals("value", o.getJsonObject("test2").getString("expression"));
		Assert.assertEquals("\"value\"",o.getJsonObject("test with space").getString("expression"));
	}
	
	@Test
	public void testSessionDefaults() throws ParsingException {
		List<AbstractStep> steps = new ArrayList<>();
		steps.add(step("Session"));
		steps.add(step("Echo test"));
		steps.add(step("End"));

		FunctionGroup group = parseAndGetUniqueChild(steps, FunctionGroup.class);
		
		Json.createReader(new StringReader(group.getToken().get())).readObject();
	}
	
	@Test
	public void testSession() throws ParsingException {
		List<AbstractStep> steps = new ArrayList<>();
		steps.add(step("Session test=\"value\" \"test with space\"=\"value\" \n \"test2\"=value"));
		steps.add(step("Echo test"));
		steps.add(step("End"));

		FunctionGroup group = parseAndGetUniqueChild(steps, FunctionGroup.class);
		
		JsonObject o = Json.createReader(new StringReader(group.getToken().get())).readObject();
		Assert.assertEquals("\"value\"",o.getJsonObject("test").getString("expression"));
		Assert.assertEquals("value", o.getJsonObject("test2").getString("expression"));
		Assert.assertEquals("\"value\"",o.getJsonObject("test with space").getString("expression"));
	}
	
	@Test
	public void testGroupOnError() throws ParsingException {
		List<AbstractStep> steps = new ArrayList<>();
		steps.add(step("Group on test=\"value\" test"));
		steps.add(step("Echo test"));
		steps.add(step("End"));

		Exception e = null;
		try {
			parseAndGetUniqueChild(steps, FunctionGroup.class);			
		} catch(Exception e1) {
			e = e1;
		}
		Assert.assertTrue(e.getMessage().contains("expecting '='"));
	}
	
	@Test
	public void testSequence() throws ParsingException {
		List<AbstractStep> steps = new ArrayList<>();
		steps.add(step("Sequence"));
		steps.add(step("Echo test"));
		steps.add(step("End"));

		Sequence sequence = parseAndGetUniqueChild(steps, Sequence.class);
		Assert.assertFalse(sequence.getContinueOnError().getValue());
	}
	
	@Test
	public void testSequenceContinueOnError() throws ParsingException {
		List<AbstractStep> steps = new ArrayList<>();
		steps.add(step("Sequence ContinueOnError=\"true\""));
		steps.add(step("Echo test"));
		steps.add(step("End"));

		Sequence sequence = parseAndGetUniqueChild(steps, Sequence.class);
		Assert.assertEquals("\"true\"",sequence.getContinueOnError().getExpression());
		Assert.assertTrue(sequence.getContinueOnError().isDynamic());
	}
	
	@Test
	public void testSequenceContinueOnError2() throws ParsingException {
		List<AbstractStep> steps = new ArrayList<>();
		steps.add(step("Sequence ContinueOnError=false  \n   "));
		steps.add(step("End"));
		
		Sequence sequence = parseAndGetUniqueChild(steps, Sequence.class);
		Assert.assertEquals("false",sequence.getContinueOnError().getExpression());
	}
	
	@Test
	public void testRetryIfFailsDefaults() throws ParsingException {
		List<AbstractStep> steps = new ArrayList<>();
		steps.add(step("RetryIfFails"));
		steps.add(step("Echo test"));
		steps.add(step("End"));

		RetryIfFails artefact = parseAndGetUniqueChild(steps, RetryIfFails.class);
		Assert.assertEquals(1,(int)artefact.getMaxRetries().getValue());
		Assert.assertEquals(1000,(int)artefact.getGracePeriod().getValue());
	}
	
	@Test
	public void testRetryIfFails() throws ParsingException {
		List<AbstractStep> steps = new ArrayList<>();
		steps.add(step("RetryIfFails MaxRetries=2 GracePeriod=1 Timeout=3000"));
		steps.add(step("End"));

		RetryIfFails artefact = parseAndGetUniqueChild(steps, RetryIfFails.class);
		Assert.assertEquals("2",artefact.getMaxRetries().getExpression());
		Assert.assertEquals("1",artefact.getGracePeriod().getExpression());
		Assert.assertEquals("3000",artefact.getTimeout().getExpression());
	}
	
	@Test
	public void testGroup() throws ParsingException {
		List<AbstractStep> steps = new ArrayList<>();
		steps.add(step("Group"));
		steps.add(step("Echo test"));
		steps.add(step("End"));

		FunctionGroup group = parseAndGetUniqueChild(steps, FunctionGroup.class);
		
		Json.createReader(new StringReader(group.getToken().get())).readObject();
	}
	
	@Test
	public void testIf() throws ParsingException {
		List<AbstractStep> steps = new ArrayList<>();
		steps.add(step("If a==true"));
		steps.add(step("Echo test"));
		steps.add(step("End"));
		
		IfBlock f = parseAndGetUniqueChild(steps, IfBlock.class);
		Assert.assertEquals("a==true",f.getCondition().getExpression());

		Assert.assertEquals(1,getChildren(f).size());
	}

	@Test
	public void testCheck() throws ParsingException {
		List<AbstractStep> steps = new ArrayList<>();
		steps.add(step("Check   Expression= true \n "));
		steps.add(step("Check Expression = key2.toString().equals('value2') \n "));
		steps.add(step("Check Expression    =    |key.toString().equals(\"value with space\")| \n "));
		steps.add(step("Check key2.toString().equals(\"value2 \"\"escaped quotes\"\" test\") \n "));
		steps.add(step("Check key.toString().equals('value with space') \n "));
		steps.add(step("Check key.toString() == 'value with space' \n "));
		steps.add(step("Check 	 \u00A0\r\nExpression    =    |key.toString().equals(\"value with space\")| \n "));

		List<AbstractArtefact> children = parse(steps).getChildren();
		Assert.assertEquals(7, children.size());
		Check check = (Check) children.get(0);
		Assert.assertEquals("true",check.getExpression().getExpression());
		check = (Check) children.get(1);
		Assert.assertEquals("key2.toString().equals('value2')",check.getExpression().getExpression());
		check = (Check) children.get(2);
		Assert.assertEquals("key.toString().equals(\"value with space\")",check.getExpression().getExpression());
		check = (Check) children.get(3);
		Assert.assertEquals("key2.toString().equals(\"value2 \"\"escaped quotes\"\" test\")",check.getExpression().getExpression());
		check = (Check) children.get(4);
		Assert.assertEquals("key.toString().equals('value with space')",check.getExpression().getExpression());
		check = (Check) children.get(5);
		Assert.assertEquals("key.toString() == 'value with space'",check.getExpression().getExpression());
		check = (Check) children.get(6);
		Assert.assertEquals("key.toString().equals(\"value with space\")",check.getExpression().getExpression());
	}


	@Test
	public void testPerformanceAssert() throws ParsingException {
		List<AbstractStep> steps = new ArrayList<>();
		steps.add(step("PerformanceAssert Measurement= \"myMeasurement1\" \t Comparator=\"higher than\" Metric = \"Count\" Value= 10"));
		PerformanceAssert artefact = parseAndGetUniqueChild(steps, PerformanceAssert.class);
		Assert.assertEquals("10",artefact.getExpectedValue().getExpression());
		Filter filter = artefact.getFilters().get(0);
		Assert.assertEquals("\"myMeasurement1\"", filter.getFilter().getExpression());
		Assert.assertEquals(Comparator.HIGHER_THAN, artefact.getComparator());
		Assert.assertEquals(Aggregator.COUNT, artefact.getAggregator());
	}

	@Test
	public void testPerformanceAssertWithMinimalArgs() throws ParsingException {
		List<AbstractStep> steps = new ArrayList<>();
		steps.add(step("PerformanceAssert Measurement=\"myMeasurement1\" Value=10"));
		PerformanceAssert artefact = parseAndGetUniqueChild(steps, PerformanceAssert.class);
		Assert.assertEquals("10",artefact.getExpectedValue().getExpression());
		Filter filter = artefact.getFilters().get(0);
		Assert.assertEquals("\"myMeasurement1\"", filter.getFilter().getExpression());
	}

	@Test
	public void testPerformanceAssertInvalidArguments() {
		List<AbstractStep> steps = new ArrayList<>();
		steps.add(step("PerformanceAssert Measurement= \"myMeasurement1\""));
		ParsingException parsingException = Assert.assertThrows("h", ParsingException.class, () -> parseAndGetUniqueChild(steps, PerformanceAssert.class));
		Assert.assertTrue(parsingException.getMessage().contains("Missing attribute 'Value'"));

		steps.clear();
		steps.add(step("PerformanceAssert Value=10"));
		ParsingException parsingException1 = Assert.assertThrows("", ParsingException.class, () -> parseAndGetUniqueChild(steps, PerformanceAssert.class));
		Assert.assertTrue(parsingException1.getMessage().contains("Missing attribute 'Measurement'"));

		steps.clear();
		steps.add(step("PerformanceAssert Measurement=\"myMeasurement1\" Comparator=\"invalid\" Value=1"));
		ParsingException parsingException2 = Assert.assertThrows("", ParsingException.class, () -> parseAndGetUniqueChild(steps, PerformanceAssert.class));
		Assert.assertTrue(parsingException2.getMessage().contains("Invalid Comparator 'invalid'"));

		steps.clear();
		steps.add(step("PerformanceAssert Measurement=\"myMeasurement1\" Comparator=\"higher then\" Metric=\"invalid\" Value=1"));
		ParsingException parsingException3 = Assert.assertThrows("", ParsingException.class, () -> parseAndGetUniqueChild(steps, PerformanceAssert.class));
		Assert.assertTrue(parsingException3.getMessage().contains("Invalid Metric 'invalid'"));
	}
}
