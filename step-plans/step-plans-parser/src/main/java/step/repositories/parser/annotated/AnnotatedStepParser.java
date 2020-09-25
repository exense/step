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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import step.repositories.parser.AbstractStep;
import step.repositories.parser.ParsingContext;
import step.repositories.parser.StepParser;
import step.repositories.parser.steps.SingleValueStep;

public class AnnotatedStepParser<T extends SingleValueStep> implements StepParser<T>{
	
	Class<T> stepClass;
	
	LinkedList<Entry> entries = new LinkedList<>();

	int score = 1;
	
	public AnnotatedStepParser(Class<T> stepClass) {
		super();
		this.stepClass = stepClass;
		parseClass();
	}

	class Entry {
		Pattern pattern;
		
		int priority;
		
		Method method;

		public Entry(Pattern pattern, int priority, Method method) {
			super();
			this.pattern = pattern;
			this.priority = priority;
			this.method = method;
		}
		
	}

	protected void parseClass() {
		for(Method method:this.getClass().getMethods()) {
			if(method.isAnnotationPresent(Step.class)) {
				Step annotation = method.getAnnotation(Step.class);
				String expression = annotation.value();
				Pattern p = Pattern.compile(expression,Pattern.DOTALL);
				Entry e = new Entry(p, annotation.priority(), method);
				
				entries.add(e);					
			}
		}
		
		Collections.sort(entries, new Comparator<Entry>() {
			@Override
			public int compare(AnnotatedStepParser<T>.Entry o1, AnnotatedStepParser<T>.Entry o2) {
				return -Integer.compare(o1.priority, o2.priority);
			}
		});
	}

	@Override
	public int getParserScoreForStep(AbstractStep step_) {
		if(stepClass.isAssignableFrom(step_.getClass())) {
			SingleValueStep step = (SingleValueStep)step_;
			if(hasMatchingMethod(step.getValue())) {
				return score;
			}
		}
		return 0;
	}
	
	protected boolean hasMatchingMethod(String expression) {
		for(Entry e:entries) {
			Matcher m = e.pattern.matcher(expression);
			if(m.matches()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void parseStep(ParsingContext parsingContext, T step_) {
		SingleValueStep step = (SingleValueStep)step_;
		findAndInvokeMatchingMethod(parsingContext, step.getValue());
	}


	private void findAndInvokeMatchingMethod(ParsingContext parsingContext, String expression) {
		for(Entry e:entries) {
			Matcher m = e.pattern.matcher(expression);
			if(m.matches()) {
				List<String> groups = new ArrayList<>();
				for(int i=1;i<=m.groupCount();i++) {
					groups.add(m.group(i));
				}
				Method method = e.method;
				
				Object[] arguments = new Object[method.getParameters().length];
				
				int groupCount = 0;
				
				for(int i=0;i<method.getParameters().length;i++) {
					Parameter p = method.getParameters()[i];
					Class<?> parameterClass = p.getType();
					if(ParsingContext.class.isAssignableFrom(parameterClass)) {
						arguments[i] = parsingContext;
					} else {
						arguments[i] = convert(groups.get(groupCount), parameterClass);
						groupCount++;
					}
					
				}
				
				try {
					method.invoke(null, arguments);
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e1) {
					Throwable cause = e1.getCause()!=null?e1.getCause():e1;
					parsingContext.addParsingError(cause.getMessage());						
				}
				
				break;
			}
		}
	}

	private Object convert(String value, Class<?> parameterClass) {
		if(Integer.TYPE==parameterClass) {
			return Integer.parseInt(value);
		} else if(String.class==parameterClass) {
			return value;
		} else {
			throw new RuntimeException("Unsupported type "+parameterClass);
		}
	}

}
