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
package step.repositories.parser.annotated;

import java.util.*;

import jakarta.json.JsonObject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import step.artefacts.*;
import step.artefacts.Comparator;
import step.artefacts.ThreadGroup;
import step.core.accessors.AbstractOrganizableObject;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.ChildrenBlock;
import step.core.dynamicbeans.DynamicValue;
import step.core.execution.ExecutionContextBindings;
import step.datapool.DataPoolConfiguration;
import step.datapool.excel.ExcelDataPool;
import step.datapool.file.CSVDataPool;
import step.datapool.file.DirectoryDataPool;
import step.datapool.file.FileDataPool;
import step.datapool.sequence.IntSequenceDataPool;
import step.repositories.parser.ParsingContext;
import step.repositories.parser.StepParserExtension;
import step.repositories.parser.steps.DescriptionStep;

@StepParserExtension
public class DefaultDescriptionStepParser extends AbstractDescriptionStepParser {

	public DefaultDescriptionStepParser() {
		super(DescriptionStep.class);
		score = 100;
	}

	@Step("Sleep[ \t\n]+([^=]+?)[ \t\n]*(ms|s|m|h)[ \t\n]*$")
	public static void sleep(ParsingContext parsingContext, String duration, String unit) {
		int factor;
		
		if(unit.equals("ms")) {
			factor = 1;
		} else if(unit.equals("s")) {
			factor = 1000;
		} else if(unit.equals("m")) {
			factor = 60000;
		} else if(unit.equals("h")) {
			factor = 3600000;
		} else {
			throw new RuntimeException("Unknown unit " + unit);
		}
		
		Sleep sleep = new Sleep();
		sleep.setDuration(new DynamicValue<Long>(factor+"*"+duration,""));

		parsingContext.addArtefactToCurrentParent(sleep);
	}
	
	@Step("Sleep(.*=.*)$")
	public static void sleep(ParsingContext parsingContext, String sleepParameters) {
		JsonObject object = parseKeyValues(sleepParameters);
		Sleep sleep = new Sleep();
		if (object.containsKey("Duration")) {
			sleep.setDuration(new DynamicValue<Long>(object.getJsonObject("Duration").getString("expression"),""));
			parsingContext.addArtefactToCurrentParent(sleep);
		} else {
			throw new RuntimeException("The expected input key 'Duration' is missing.");
		}
		if(object.containsKey("Unit")) {
			sleep.setUnit(new DynamicValue<String>(object.getJsonObject("Unit").getString("expression"),""));
		}
		if(object.containsKey("ReleaseTokens")) {
			sleep.setReleaseTokens(new DynamicValue<Boolean>(object.getJsonObject("ReleaseTokens").getString("expression"),""));
		}
	}
	
	@Step("For (\\d+) to (\\d+)[ \t\n]*$")
	public static void map(ParsingContext parsingContext, int start, int end) {
		ForBlock result = new ForBlock();
		IntSequenceDataPool conf = new IntSequenceDataPool();
		conf.getStart().setValue(start);
		conf.getEnd().setValue(end);
		result.setDataSource(conf);
		
		parsingContext.addArtefactToCurrentParentAndPush(result);
	}
	
	@Step("Sequence(.*)$")
	public static void sequence(ParsingContext parsingContext, String sequenceParameters) {
		JsonObject object = parseKeyValues(sequenceParameters);
		Sequence sequence = new Sequence();
		// TODO do this mapping in a generic way
		if(object.containsKey("ContinueOnError")) {
			sequence.setContinueOnError(new DynamicValue<Boolean>(object.getJsonObject("ContinueOnError").getString("expression"),""));			
		}
		if(object.containsKey("Pacing")) {
			sequence.setPacing(new DynamicValue<>(object.getJsonObject("Pacing").getString("expression"),""));
		}
		parsingContext.addArtefactToCurrentParentAndPush(sequence);
	}
	
	@Step("While(.*)$")
	public static void testWhile(ParsingContext parsingContext, String args) {
		JsonObject object = parseKeyValues(args);
		While artefact = new While();
		if(object.containsKey("MaxIterations")) {
			artefact.setMaxIterations(new DynamicValue<Integer>(object.getJsonObject("MaxIterations").getString("expression"),""));
		}
		if(object.containsKey("Timeout")) {
			artefact.setTimeout(new DynamicValue<Long>(object.getJsonObject("Timeout").getString("expression"),""));
		}
		if(object.containsKey("Pacing")) {
			artefact.setPacing(new DynamicValue<Long>(object.getJsonObject("Pacing").getString("expression"),""));
		}
		if(object.containsKey("Condition")) {
			artefact.setCondition(new DynamicValue<Boolean>(object.getJsonObject("Condition").getString("expression"),""));
		}
		parsingContext.addArtefactToCurrentParentAndPush(artefact);
	}
	
	@Step("TestSet(.*)$")
	public static void testTestSet(ParsingContext parsingContext, String args) {
		TestSet artefact = new TestSet(0);
		parsingContext.addArtefactToCurrentParentAndPush(artefact);
	}
	
	@Step("TestCase(.*)$")
	public static void testTestCase(ParsingContext parsingContext, String args) {
		TestCase artefact = new TestCase();
		parsingContext.addArtefactToCurrentParentAndPush(artefact);
	}
	
	@Step("TestScenario(.*)$")
	public static void testScenario(ParsingContext parsingContext, String args) {
		TestScenario artefact = new TestScenario();
		parsingContext.addArtefactToCurrentParentAndPush(artefact);
	}
	
	@Step("ThreadGroup(.*)$")
	public static void threadGroup(ParsingContext parsingContext, String args) {
		JsonObject object = parseKeyValues(args);
		
		ThreadGroup group = new ThreadGroup();
		// TODO do this mapping in a generic way
		if(object.containsKey("Threads")) {
			group.setUsers(new DynamicValue<>(object.getJsonObject("Threads").getString("expression"),""));
		}
		if(object.containsKey("Iterations")) {
			group.setIterations(new DynamicValue<>(object.getJsonObject("Iterations").getString("expression"),""));
		}
		if(object.containsKey("MaxDuration")) {
			group.setMaxDuration(new DynamicValue<>(object.getJsonObject("MaxDuration").getString("expression"),""));
		}
		if(object.containsKey("Pacing")) {
			group.setPacing(new DynamicValue<>(object.getJsonObject("Pacing").getString("expression"),""));
		}
		if(object.containsKey("Rampup")) {
			group.setRampup(new DynamicValue<>(object.getJsonObject("Rampup").getString("expression"),""));
		}
		if(object.containsKey("Offset")) {
			group.setStartOffset(new DynamicValue<>(object.getJsonObject("Offset").getString("expression"),""));
		}

		parsingContext.addArtefactToCurrentParentAndPush(group);
	}
	

	@Step(value = "BeforeThread(.*)$", priority = 2)
	public static void beforeThread(ParsingContext parsingContext, String selectionCriteriaExpr) {
		AbstractArtefact abstractArtefact = parsingContext.peekCurrentNonWrappingArtefact();
		if (abstractArtefact instanceof ThreadGroup) {
			ThreadGroup threadGroup = (ThreadGroup) abstractArtefact;
			ChildrenBlock childrenBlock = threadGroup.getBeforeThread();
			if (childrenBlock == null) {
				childrenBlock = new ChildrenBlock();
				threadGroup.setBeforeThread(childrenBlock);
			}
			parsingContext.addArtefactToCurrentParentSourceAndPush(threadGroup, childrenBlock.getSteps());
		} else {
			throw new RuntimeException("BeforeThread is only supported in Thread Groups");
		}
	}
	
	@Step(value = "AfterThread(.*)$", priority = 2)
	public static void afterThread(ParsingContext parsingContext, String selectionCriteriaExpr) {
		AbstractArtefact abstractArtefact = parsingContext.peekCurrentNonWrappingArtefact();
		if (abstractArtefact instanceof ThreadGroup) {
			ThreadGroup threadGroup = (ThreadGroup) abstractArtefact;
			ChildrenBlock childrenBlock = threadGroup.getAfterThread();
			if (childrenBlock == null) {
				childrenBlock = new ChildrenBlock();
				threadGroup.setAfterThread(childrenBlock);
			}
			parsingContext.addArtefactToCurrentParentSourceAndPush(threadGroup, childrenBlock.getSteps());
		} else {
			throw new RuntimeException("BeforeThread is only supported in Thread Groups");
		}
	}
	
	@Step("RetryIfFails(.*)$")
	public static void retryIfFails(ParsingContext parsingContext, String args) {
		JsonObject object = parseKeyValues(args);
		
		RetryIfFails artefact = new RetryIfFails();
		// TODO do this mapping in a generic way
		if(object.containsKey("MaxRetries")) {
			artefact.setMaxRetries(new DynamicValue<Integer>(object.getJsonObject("MaxRetries").getString("expression"),""));
		}
		if(object.containsKey("GracePeriod")) {
			artefact.setGracePeriod(new DynamicValue<Integer>(object.getJsonObject("GracePeriod").getString("expression"),""));
		}
		if(object.containsKey("Timeout")) {
			artefact.setTimeout(new DynamicValue<Integer>(object.getJsonObject("Timeout").getString("expression"),""));
		}
		if(object.containsKey("ReleaseTokens")) {
			artefact.setReleaseTokens(new DynamicValue<Boolean>(object.getJsonObject("ReleaseTokens").getString("expression"),""));
		}
		if(object.containsKey("ReportLastTryOnly")) {
			artefact.setReportLastTryOnly(new DynamicValue<Boolean>(object.getJsonObject("ReportLastTryOnly").getString("expression"),""));
		}
		
		parsingContext.addArtefactToCurrentParentAndPush(artefact);
	}
	
	@Step("Group[ \t\n]*")
	public static void group(ParsingContext parsingContext) {
		FunctionGroup result = new FunctionGroup();
		result.getToken().setValue("{}");
		parsingContext.addArtefactToCurrentParentAndPush(result);
	}
	
	@Step("Group on (.*)$")
	public static void groupOn(ParsingContext parsingContext, String selectionCriteriaExpr) {
		JsonObject object = parseKeyValues(selectionCriteriaExpr);
		FunctionGroup result = new FunctionGroup();
		result.getToken().setValue(object.toString());
		parsingContext.addArtefactToCurrentParentAndPush(result);
	}
	
	@Step("Session(.*)$")
	public static void session(ParsingContext parsingContext, String selectionCriteriaExpr) {
		JsonObject object = parseKeyValues(selectionCriteriaExpr);
		FunctionGroup result = new FunctionGroup();
		result.getToken().setValue(object.toString());
		parsingContext.addArtefactToCurrentParentAndPush(result);
	}

	@Step(value = "BeforeSequence(.*)$", priority = 2)
	public static void beforeSequence(ParsingContext parsingContext, String selectionCriteriaExpr) {
		AbstractArtefact abstractArtefact = parsingContext.peekCurrentArtefact();
		abstractArtefact = wrapInSequenceIfRequired(parsingContext, abstractArtefact);
		addBeforeSection(parsingContext, abstractArtefact);
	}

	@Step("Before( *)$")
	public static void before(ParsingContext parsingContext, String selectionCriteriaExpr) {
		addBeforeSection(parsingContext, parsingContext.peekCurrentNonWrappingArtefact());
	}

	private static void addBeforeSection(ParsingContext parsingContext, AbstractArtefact abstractArtefact) {
		ChildrenBlock childrenBlock = abstractArtefact.getBefore();
		if (childrenBlock == null) {
			childrenBlock = new ChildrenBlock();
			abstractArtefact.setBefore(childrenBlock);
		}
		parsingContext.addArtefactToCurrentParentSourceAndPush(abstractArtefact, childrenBlock.getSteps());
	}

	@Step(value = "AfterSequence(.*)$", priority = 2)
	public static void afterSequence(ParsingContext parsingContext, String selectionCriteriaExpr) {
		AbstractArtefact abstractArtefact = parsingContext.peekCurrentArtefact();
		abstractArtefact = wrapInSequenceIfRequired(parsingContext, abstractArtefact);
		addAfterSection(parsingContext, abstractArtefact);
	}

	@Step("After( *)$")
	public static void after(ParsingContext parsingContext, String selectionCriteriaExpr) {
		addAfterSection(parsingContext, parsingContext.peekCurrentNonWrappingArtefact());
	}

	private static void addAfterSection(ParsingContext parsingContext, AbstractArtefact abstractArtefact) {
		ChildrenBlock childrenBlock = abstractArtefact.getAfter();
		if (childrenBlock == null) {
			childrenBlock = new ChildrenBlock();
			abstractArtefact.setAfter(childrenBlock);
		}
		parsingContext.addArtefactToCurrentParentSourceAndPush(abstractArtefact, childrenBlock.getSteps());
	}

	private static AbstractArtefact wrapInSequenceIfRequired(ParsingContext parsingContext, AbstractArtefact abstractArtefact) {
		if (!(abstractArtefact instanceof Sequence)) {
			Sequence sequence = new Sequence();
			sequence.setChildren(abstractArtefact.getChildren());
			List<AbstractArtefact> newChildren = new ArrayList<>();
			newChildren.add(sequence);
			abstractArtefact.setChildren(newChildren);
			parsingContext.pushWrappingArtefact(sequence);
			abstractArtefact = sequence;
		}
		return abstractArtefact;
	}

	@Step("Synchronized(.*)$")
	public static void synchronized_(ParsingContext parsingContext, String selectionCriteriaExpr) {
		JsonObject object = parseKeyValues(selectionCriteriaExpr);
		Synchronized result = new Synchronized();
		if(object.containsKey("LockName")) {
			result.setLockName(new DynamicValue<String>(object.getJsonObject("LockName").getString("expression"),""));
		}
		if(object.containsKey("GlobalLock")) {
			result.setGlobalLock(new DynamicValue<Boolean>(object.getJsonObject("GlobalLock").getString("expression"),""));
		}
		parsingContext.addArtefactToCurrentParentAndPush(result);
	}

	@Step(value="For each row in attached (.*)$",priority=2)
	public static void forEachRowOfFileAttached(ParsingContext parsingContext, String type) {
		DynamicValue<String> fileExpression = getExpressionForAttachedFile();
		ForEachBlock result = getForEachArtefact(type, fileExpression);
		parsingContext.addArtefactToCurrentParentAndPush(result);
	}
	
	@Step("For each row in (.+?) (.+?)$")
	public static void forEachRowOfFile(ParsingContext parsingContext, String type, String filepath) {
		filepath = filepath.trim();
		DynamicValue<String> fileExpression = new DynamicValue<String>(filepath, "groovy");
		ForEachBlock result = getForEachArtefact(type, fileExpression);
		parsingContext.addArtefactToCurrentParentAndPush(result);
	}
	
	@Step(value="ForEach(.*)$")
	public static void forEach(ParsingContext parsingContext, String args) {
		JsonObject object = parseKeyValues(args);
		ForEachBlock artefact = new ForEachBlock();
		
		String sourceType = null;
		if(object.containsKey("SourceType")) {
			sourceType = object.getJsonObject("SourceType").getString("expression").replace("\"", "");
		}
		
		if(sourceType!=null) {
			
			if(object.containsKey("File")) {
				DynamicValue<String> file = new DynamicValue<>(object.getJsonObject("File").getString("expression"), "");
				DataPoolConfiguration dataPoolConfiguration = getDataPoolConfiguration(sourceType, file);
				
				if (dataPoolConfiguration instanceof CSVDataPool && object.containsKey("Delimiter")) {
					DynamicValue<String> delimiter = new DynamicValue<>(object.getJsonObject("Delimiter").getString("expression"), "");
					((CSVDataPool) dataPoolConfiguration).setDelimiter(delimiter);
				}
				if (dataPoolConfiguration instanceof ExcelDataPool && object.containsKey("Worksheet")) {

					DynamicValue<String> worksheet = new DynamicValue<>(
							object.getJsonObject("Worksheet").getString("expression"), "");
					((ExcelDataPool) dataPoolConfiguration).setWorksheet(worksheet);
				}
				artefact.setDataSourceType(sourceType);
				artefact.setDataSource(dataPoolConfiguration);
			}
			
		}
		if(object.containsKey("Threads")) {
			artefact.setThreads(new DynamicValue<Integer>(object.getJsonObject("Threads").getString("expression"),""));
		}
		
		if(object.containsKey("RowHandle")) {
			artefact.setItem(new DynamicValue<String>(object.getJsonObject("RowHandle").getString("expression"),""));
		}
		
		if(object.containsKey("MaxFailedLoop")) {
			artefact.setMaxFailedLoops(new DynamicValue<Integer>(object.getJsonObject("MaxFailedLoop").getString("expression"),""));
		}
		
		parsingContext.addArtefactToCurrentParentAndPush(artefact);
	}

	protected static ForEachBlock getForEachArtefact(String type, DynamicValue<String> fileExpression) {
		type = type.trim().toLowerCase();
		ForEachBlock result = new ForEachBlock();		
		DataPoolConfiguration conf = getDataPoolConfiguration(type, fileExpression);		
		result.setDataSourceType(type);
		result.setDataSource(conf);
		return result;
	}
	
	@Step(value="DataSet(.*)$")
	public static void dataSet(ParsingContext parsingContext, String args) {
		JsonObject object = parseKeyValues(args);
		DataSetArtefact artefact = new DataSetArtefact();
		
		String sourceType = null;
		if(object.containsKey("SourceType")) {
			sourceType = object.getJsonObject("SourceType").getString("expression").replace("\"", "");
		}
		
		if(sourceType!=null) {
			
			if(object.containsKey("File")) {
				DynamicValue<String> file = new DynamicValue<>(object.getJsonObject("File").getString("expression"), "");
				DataPoolConfiguration dataPoolConfiguration = getDataPoolConfiguration(sourceType, file);
				artefact.setDataSourceType(sourceType);
				artefact.setDataSource(dataPoolConfiguration);
			}
			
		}

		if(object.containsKey("IteratorHandle")) {
			artefact.setItem(new DynamicValue<String>(object.getJsonObject("IteratorHandle").getString("expression"),""));
		}
		if(object.containsKey("ForWrite")) {
			artefact.getDataSource().setForWrite(new DynamicValue<Boolean>(object.getJsonObject("ForWrite").getString("expression"),""));
		}
		if(object.containsKey("ResetAtEnd")) {
			artefact.setResetAtEnd(new DynamicValue<Boolean>(object.getJsonObject("ResetAtEnd").getString("expression"),""));
		}
		
		
		parsingContext.addArtefactToCurrentParent(artefact);	
	}

	protected static DataPoolConfiguration getDataPoolConfiguration(String type, DynamicValue<String> fileExpression) {
		DataPoolConfiguration conf;
		if(type.equals("excel")) {
			ExcelDataPool conf_ = new ExcelDataPool();
			conf_.setFile(fileExpression);
			conf = conf_;
		} else if(type.equals("csv")) {
			CSVDataPool conf_ = new CSVDataPool();
			conf_.setFile(fileExpression);
			//conf_.setDelimiter(delimiter);
			conf = conf_;
		} else if(type.equals("file")) {
			FileDataPool conf_ = new FileDataPool();
			conf_.setFile(fileExpression);
			conf = conf_;
		} else if(type.equals("folder")) {
			DirectoryDataPool conf_ = new DirectoryDataPool();
			conf_.setFolder(fileExpression);
			conf = conf_;
		} else {
			throw new RuntimeException("Unsupported source type "+type);
		}
		return conf;
	}

	private static DynamicValue<String> getExpressionForAttachedFile() {
		return new DynamicValue<>(ExecutionContextBindings.BINDING_RESOURCE_MANAGER+".getResourceFile(currentArtefact.getAttachments().get(0).toString()).getResourceFile().getAbsolutePath()", "");
	}
	
	@Step("If (.*)$")
	public static void ifBlock(ParsingContext parsingContext, String expression) {
		IfBlock result = new IfBlock();
		result.setCondition(new DynamicValue<>(expression, ""));
		parsingContext.addArtefactToCurrentParentAndPush(result);
	}
	
	@Step("Script (.*)$")
	public static void script(ParsingContext parsingContext, String expression) {
		Script script = new  Script();
		script.setScript(expression);
		parsingContext.addArtefactToCurrentParent(script);
	}
	
	@Step("Echo (.*)$")
	public static void echo(ParsingContext parsingContext, String expression) {
		Echo result = new Echo();
		result.setText(new DynamicValue<>(expression, ""));
		parsingContext.addArtefactToCurrentParent(result);
	}

	@Step(value="Check[ \u00A0\t\r\n]+(Expression[ \u00A0\t\r\n]*=.*)$", priority=2)
	public static void check(ParsingContext parsingContext, String args) {
		JsonObject object = parseKeyValues(args);
		Check artefact = new Check();
		if(object.containsKey("Expression")) {
			artefact.setExpression(new DynamicValue<Boolean>(object.getJsonObject("Expression").getString("expression"),""));
		}
		parsingContext.addArtefactToCurrentParent(artefact);
	}

	@Step(value="Check (.*)$", priority=1)
	public static void checkExpression(ParsingContext parsingContext, String args) {
		Check artefact = new Check();
		artefact.setExpression(new DynamicValue<Boolean>(args.trim(),""));
		parsingContext.addArtefactToCurrentParent(artefact);
	}


	
	@Step("^([\\d\\w]+):(.*)$")
	public static void map(ParsingContext parsingContext, String keyword, String argument) {
		CallFunction result = new CallFunction();
		result.getArgument().setValue(argument);
		result.getFunction().setValue("{\"name\":\""+keyword+"\"}");
		parsingContext.addArtefactToCurrentParent(result);
	}
	
	@Step("^([\\d\\w]+)=(.*)$")
	public static void callKeyword(ParsingContext parsingContext, String keyword, String argumentExpression) {
		CallFunction result = new CallFunction();
		result.setArgument(new DynamicValue<String>(argumentExpression,""));
		result.getFunction().setValue("{\"name\":\""+keyword+"\"}");
		parsingContext.addArtefactToCurrentParent(result);
	}
	
	@Step("Call composite:(.*):(.*)$")
	public static void callComposite(ParsingContext parsingContext, String keywordComposite, String argument) {
		CallFunction result = new CallFunction();
		result.getArgument().setValue(argument);
		result.getFunction().setValue("{\"name\":\""+keywordComposite+"\"}");
		result.setRemote(new DynamicValue<Boolean>(false));
		parsingContext.addArtefactToCurrentParent(result);
	}
	
	@Step("Call plan (.*)$")
	public static void callTo(ParsingContext parsingContext, String planName) throws JsonProcessingException {
		CallPlan callPlan = new CallPlan();
		
		Map<String, Object> selectionAttributes = new HashMap<>();
		selectionAttributes.put("name", new DynamicValue<>(planName, ""));
		
		ObjectMapper mapper = new ObjectMapper();
		String selectionAttributesStr = mapper.writeValueAsString(selectionAttributes);
		
		callPlan.getSelectionAttributes().setValue(selectionAttributesStr);
		parsingContext.addArtefactToCurrentParent(callPlan);
	}
	
	@Step("^End[ \t\n]*$")
	public static void end(ParsingContext parsingContext) {
		parsingContext.popNonWrappingArtifact();
	}

	public static final String METRIC = "Metric";
	public static final String MEASUREMENT = "Measurement";
	public static final String COMPARATOR = "Comparator";
	public static final String VALUE = "Value";

	@Step(value = "PerformanceAssert(.*)$", priority = 2)
	public static void performanceAssert(ParsingContext parsingContext, String args) {
		JsonObject object = parseKeyValues(args);

		String measurementName = getNonNullExpression(object, MEASUREMENT);
		String aggregatorStr = getStringOrDefault(object, METRIC, Aggregator.AVG.getDescription());
		Aggregator aggregator = Arrays.stream(Aggregator.values()).filter(a -> a.getDescription().equals(aggregatorStr)).findFirst()
				.orElseThrow(() -> new RuntimeException("Invalid " + METRIC + " '" + aggregatorStr + "'"));
		String comparatorString = getStringOrDefault(object, COMPARATOR, Comparator.LOWER_THAN.getDescription());
		Comparator comparator = Arrays.stream(Comparator.values()).filter(c -> c.getDescription().equals(comparatorString)).findFirst()
				.orElseThrow(() -> new RuntimeException("Invalid " + COMPARATOR + " '" + comparatorString + "'"));
		DynamicValue<Number> expectedValue = new DynamicValue<>(getNonNullExpression(object, VALUE), "");

		PerformanceAssert performanceAssert = new PerformanceAssert();
		performanceAssert.setAggregator(aggregator);
		performanceAssert.setComparator(comparator);
		performanceAssert.setExpectedValue(expectedValue);

		Filter filter = new Filter();
		filter.setField(new DynamicValue<>(AbstractOrganizableObject.NAME));
		filter.setFilterType(FilterType.REGEX);
		filter.setFilter(new DynamicValue<>(measurementName, ""));
		ArrayList filters = new ArrayList();
		filters.add(filter);
		performanceAssert.setFilters(filters);
		//Performance Assert must be defined in an after block
		AbstractArtefact abstractArtefact = parsingContext.peekCurrentArtefact();
		List<AbstractArtefact> steps = parsingContext.peekCurrentSteps();
		if (steps.equals(abstractArtefact.getAfter())) {
			parsingContext.addArtefactToCurrentParent(performanceAssert);
		} else {
			after(parsingContext, null);
			parsingContext.addArtefactToCurrentParent(performanceAssert);
			end(parsingContext);
		}
	}

	private static String getNonNullExpression(JsonObject object, String key) {
		if (object.containsKey(key)) {
			return getExpression(object, key);
		} else {
			throw new RuntimeException("Missing attribute '" + key + "'");
		}
	}

	private static String getStringOrDefault(JsonObject object, String key, String defaultExpression) {
		if (object.containsKey(key)) {
			return getString(object, key);
		} else {
			return defaultExpression;
		}
	}

	private static String getString(JsonObject object, String key) {
		String string = object.getJsonObject(key).getString(EXPRESSION);
		return string.substring(1, string.length() - 1);
	}
}
