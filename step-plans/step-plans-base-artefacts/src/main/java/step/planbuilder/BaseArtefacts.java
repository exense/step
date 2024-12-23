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
package step.planbuilder;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;

import step.artefacts.*;
import step.artefacts.ThreadGroup;
import step.core.artefacts.AbstractArtefact;
import step.core.artefacts.CheckArtefact;
import step.core.artefacts.ChildrenBlock;
import step.core.dynamicbeans.DynamicValue;
import step.core.execution.ExecutionContext;
import step.datapool.excel.ExcelDataPool;
import step.datapool.sequence.IntSequenceDataPool;

public class BaseArtefacts {
	
	public static Sequence sequence() {
		return new Sequence();
	}
	
	public static Set set(String key, String valueExpression) {
		Set set = new Set();
		set.setKey(new DynamicValue<String>(key));
		set.setValue(new DynamicValue<>(valueExpression, ""));
		return set;
	}
	
	public static ForBlock for_(int start, int end) {
		return for_(start, end, 1);
	}

	public static ForBlock for_(int start, int end, int threads) {
		ForBlock f = new ForBlock();
		IntSequenceDataPool conf = new IntSequenceDataPool();
		conf.setStart(new DynamicValue<Integer>(start));;
		conf.setEnd(new DynamicValue<Integer>(end));;
		f.setDataSource(conf);
		f.setThreads(new DynamicValue<>(threads));
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

	public static CallPlan callPlan(String planId, String name, String inputExpression) {
		CallPlan callPlan = callPlan(planId);
		callPlan.getAttributes().put(AbstractArtefact.NAME, name);
		callPlan.setInput(new DynamicValue<>(inputExpression, ""));
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

	public static ThreadGroup threadGroup(int numberOfThreads, int numberOfIterationsPerThread,
							  ChildrenBlock beforeThread, ChildrenBlock afterThread) {
		ThreadGroup threadGroup = new ThreadGroup();
		threadGroup.setIterations(new DynamicValue<Integer>(numberOfIterationsPerThread));
		threadGroup.setUsers(new DynamicValue<Integer>(numberOfThreads));
		threadGroup.setBeforeThread(beforeThread);
		threadGroup.setAfterThread(afterThread);
		return threadGroup;
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

	public static IfBlock ifBlock(DynamicValue<Boolean> condition) {
		IfBlock ifBlock = new IfBlock();
		ifBlock.setCondition(condition);
		return ifBlock;
	}
	
	public static CheckArtefact runnable(Consumer<ExecutionContext> executionRunnable) {
		CheckArtefact checkArtefact = new CheckArtefact(executionRunnable);
		return checkArtefact;
	}

	public static Assert assertEqualArtefact(String field, String expression) {
		Assert anAssert = new Assert();
		anAssert.setActual(new DynamicValue<>(field));
		anAssert.setOperator(Assert.AssertOperator.EQUALS);
		anAssert.setExpected(new DynamicValue<>(expression));
		return anAssert;
	}

	public static ChildrenBlock childrenBlock(AbstractArtefact... children) {
		ChildrenBlock childrenBlock = new ChildrenBlock();
		childrenBlock.setSteps(List.of(children));
		return childrenBlock;
	}
}
