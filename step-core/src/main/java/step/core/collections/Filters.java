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
package step.core.collections;

import java.util.ArrayList;
import java.util.List;

public class Filters {

	public interface FilterFactory<T> {
		
		T buildFilter(Filter filter);
	}
	
	public static And and(List<Filter> filters) {
		return new And(filters);
	}
	
	public static Or or(List<Filter> filters) {
		return new Or(filters);
	}
	
	public static Not not(Filter filter) {
		return new Not(filter);
	}
	
	public static True empty() {
		return new True();
	}
	
	public static Equals equals(String field, Object expectedValue) {
		return new Equals(field, expectedValue);
	}
	
	public static Lt lt(String field, long value) {
		return new Lt(field, value);
	}
	
	public static Lte lte(String field, long value) {
		return new Lte(field, value);
	}
	
	public static Gt gt(String field, long value) {
		return new Gt(field, value);
	}
	
	public static Gte gte(String field, long value) {
		return new Gte(field, value);
	}
	
	public static In in(String field, List<String> values) {
		return new In(field, values);
	}
	
	public static Regex regex(String field, String expression, boolean caseSensitive) {
		return new Regex(field, expression, caseSensitive);
	}
	
	public static Fulltext fulltext(String expression) {
		return new Fulltext(expression);
	}
	
	public static class AbstractCompositeFilter implements Filter {

		private final List<Filter> filters;
		
		public AbstractCompositeFilter(List<Filter> filters) {
			super();
			this.filters = filters;
		}

		@Override
		public List<Filter> getChildren() {
			return filters;
		}
	}
	
	public static class AbstractAtomicFilter implements Filter {

		@Override
		public List<Filter> getChildren() {
			return null;
		}
	}
	
	public static class And extends AbstractCompositeFilter {

		public And(List<Filter> filters) {
			super(filters);
		}
	}
	
	public static class Or extends AbstractCompositeFilter {

		public Or(List<Filter> filters) {
			super(filters);
		}
	}
	
	public static class Not extends AbstractCompositeFilter {

		private final Filter filter;
		
		public Not(Filter filter) {
			super(new ArrayList<Filter>(List.of(filter)));
			this.filter = filter;
		}

		public Filter getFilter() {
			return filter;
		}
	}
	
	public static class Fulltext extends AbstractAtomicFilter {

		private final String expression;

		public Fulltext(String expression) {
			super();
			this.expression = expression;
		}

		public String getExpression() {
			return expression;
		}
	}
	
	public static class Equals extends AbstractAtomicFilter {

		private final String field;
		private final Object expectedValue;
		
		public Equals(String field, Object expectedValue) {
			super();
			this.field = field;
			this.expectedValue = expectedValue;
		}

		public String getField() {
			return field;
		}

		public Object getExpectedValue() {
			return expectedValue;
		}
	}
	
	public static class Gt extends AbstractAtomicFilter {

		private final String field;
		private final long value;
		
		public Gt(String field, long value) {
			super();
			this.field = field;
			this.value = value;
		}

		public String getField() {
			return field;
		}

		public long getValue() {
			return value;
		}
	}
	
	public static class Gte extends AbstractAtomicFilter {

		private final String field;
		private final long value;
		
		public Gte(String field, long value) {
			super();
			this.field = field;
			this.value = value;
		}

		public String getField() {
			return field;
		}

		public long getValue() {
			return value;
		}
	}
	
	public static class Lt extends AbstractAtomicFilter {

		private final String field;
		private final long value;
		
		public Lt(String field, long value) {
			super();
			this.field = field;
			this.value = value;
		}

		public String getField() {
			return field;
		}

		public long getValue() {
			return value;
		}
	}
	
	public static class Lte extends AbstractAtomicFilter {

		private final String field;
		private final long value;
		
		public Lte(String field, long value) {
			super();
			this.field = field;
			this.value = value;
		}

		public String getField() {
			return field;
		}

		public long getValue() {
			return value;
		}
	}
	
	public static class In extends AbstractAtomicFilter {
		
		private final String field;
		private final List<String> values;

		public In(String field, List<String> values) {
			super();
			this.field = field;
			this.values = values;
		}

		public String getField() {
			return field;
		}

		public List<String> getValues() {
			return values;
		}
	}
	
	public static class Regex extends AbstractAtomicFilter {

		private final String field;
		private final String expression;
		private final boolean caseSensitive;

		public Regex(String field, String expression, boolean caseSensitive) {
			super();
			this.field = field;
			this.expression = expression;
			this.caseSensitive = caseSensitive;
		}

		public String getField() {
			return field;
		}

		public String getExpression() {
			return expression;
		}

		public boolean isCaseSensitive() {
			return caseSensitive;
		}
	}
	
	public static class True extends AbstractAtomicFilter {

		public True() {
			super();
		}
	}
}
