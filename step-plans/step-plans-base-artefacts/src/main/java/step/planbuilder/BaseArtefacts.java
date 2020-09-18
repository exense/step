package step.planbuilder;

import java.io.File;
import java.util.function.Consumer;

import step.artefacts.AfterSequence;
import step.artefacts.AfterThread;
import step.artefacts.BeforeSequence;
import step.artefacts.BeforeThread;
import step.artefacts.CallPlan;
import step.artefacts.Check;
import step.artefacts.Echo;
import step.artefacts.ForBlock;
import step.artefacts.ForEachBlock;
import step.artefacts.Sequence;
import step.artefacts.Set;
import step.artefacts.Sleep;
import step.artefacts.Synchronized;
import step.artefacts.TestCase;
import step.artefacts.TestScenario;
import step.artefacts.TestSet;
import step.artefacts.ThreadGroup;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.CheckArtefact;
import step.core.dynamicbeans.DynamicValue;
import step.core.execution.ExecutionContext;
import step.datapool.excel.ExcelDataPool;
import step.datapool.sequence.IntSequenceDataPool;

public class BaseArtefacts {
	
	public static Sequence sequence() {
		return new Sequence();
	}
	
	public static BeforeSequence beforeSequence() {
		return new BeforeSequence();
	}
	
	public static AfterSequence afterSequence() {
		return new AfterSequence();
	}
	
	public static Set set(String key, String valueExpression) {
		Set set = new Set();
		set.setKey(new DynamicValue<String>(key));
		set.setValue(new DynamicValue<>(valueExpression, ""));
		return set;
	}
	
	public static ForBlock for_(int start, int end) {
		ForBlock f = new ForBlock();
		IntSequenceDataPool conf = new IntSequenceDataPool();
		conf.setStart(new DynamicValue<Integer>(start));;
		conf.setEnd(new DynamicValue<Integer>(end));;
		f.setDataSource(conf);
		return f;
	}
	
	public static ForBlock for_(String startExpression, String endExpression) {
		ForBlock f = new ForBlock();
		IntSequenceDataPool conf = new IntSequenceDataPool();
		conf.setStart(new DynamicValue<Integer>(startExpression, ""));
		conf.setEnd(new DynamicValue<Integer>(endExpression, ""));;
		f.setDataSource(conf);
		return f;
	}
	
	public static ForEachBlock forEachRowInExcel(File file) {
		ForEachBlock f = new ForEachBlock();
		ExcelDataPool p = new ExcelDataPool();
		p.setFile(new DynamicValue<String>(file.getAbsolutePath()));
		p.getHeaders().setValue(true);
		f.setDataSource(p);
		f.setDataSourceType("excel");
		return f;
	}
	
	public static CallPlan callPlan(String planId) {
		CallPlan callPlan = new CallPlan();
		callPlan.setPlanId(planId);
		return callPlan;
	}
	
	public static CallPlan callPlan(String planId, String name) {
		CallPlan callPlan = callPlan(planId);
		callPlan.getAttributes().put(AbstractArtefact.NAME, name);
		return callPlan;
	}
	
	public static TestSet testSet() {
		TestSet testSet = new TestSet();
		return testSet;
	}
	
	public static TestSet testSet(String name) {
		TestSet testSet = testSet();
		testSet.getAttributes().put(AbstractArtefact.NAME, name);
		return testSet;
	}
	
	public static TestCase testCase() {
		TestCase testCase = new TestCase();
		return testCase;
	}
	
	public static TestCase testCase(String name) {
		TestCase testCase = testCase();
		testCase.getAttributes().put(AbstractArtefact.NAME, name);
		return testCase;
	}

	public static Sleep sleep(long ms) {
		Sleep sleep = new Sleep();
		sleep.getDuration().setValue(ms);
		return sleep;
	}
	
	public static Synchronized synchronized_(String lockName, boolean globalLock) {
		Synchronized synchronized1 = new Synchronized();
		synchronized1.setGlobalLock(new DynamicValue<Boolean>(globalLock));
		synchronized1.setLockName(new DynamicValue<String>(lockName));
		return synchronized1;
	}

	public static ThreadGroup threadGroup(int numberOfThreads, int numberOfIterationsPerThread) {
		ThreadGroup threadGroup = new ThreadGroup();
		threadGroup.setIterations(new DynamicValue<Integer>(numberOfIterationsPerThread));
		threadGroup.setUsers(new DynamicValue<Integer>(numberOfThreads));
		return threadGroup;
	}
	
	public static BeforeThread beforeThread() {
		return new BeforeThread();
	}
	
	public static AfterThread afterThread() {
		return new AfterThread();
	}
	
	public static TestScenario testScenario() {
		return new TestScenario();
	}
	
	public static Echo echo(String expression) {
		Echo echo = new Echo();
		echo.setText(new DynamicValue<>(expression, ""));
		return echo;
	}
	
	public static Check check(String expression) {
		Check check = new Check();
		check.setExpression(new DynamicValue<>(expression, ""));
		return check;
	}
	
	public static CheckArtefact runnable(Consumer<ExecutionContext> executionRunnable) {
		CheckArtefact checkArtefact = new CheckArtefact(executionRunnable);
		return checkArtefact;
	}
}
