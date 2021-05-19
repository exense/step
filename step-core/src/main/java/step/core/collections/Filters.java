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

import org.bson.types.ObjectId;

import step.core.accessors.AbstractIdentifiableObject;

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
	
	public static Equals equals(String field, boolean expectedValue) {
		return new Equals(field, expectedValue);
	}
	
	public static Equals equals(String field, long expectedValue) {
		return new Equals(field, expectedValue);
	}
	
	public static Equals equals(String field, String expectedValue) {
		return new Equals(field, expectedValue);
	}
	
	public static Equals equals(String field, ObjectId expectedValue) {
		return new Equals(field, expectedValue);
	}
	
	public static Equals id(ObjectId id) {
		return equals(AbstractIdentifiableObject.ID, id);
	}
	
	public static Equals id(String id) {
		return id(new ObjectId(id));
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

		private List<Filter> children;

		public AbstractCompositeFilter(){
			super();
		}

		public AbstractCompositeFilter(List<Filter> children) {
			super();
			this.children = children;
		}

		@Override
		public List<Filter> getChildren() {
			return children;
		}

		public void setChildren(List<Filter> children) {
			this.children = children;
		}
	}
	
	public static class AbstractAtomicFilter implements Filter {

		public AbstractAtomicFilter() {
			super();
		}

		@Override
		public List<Filter> getChildren() {
			return null;
		}

	}
	
	public static class And extends AbstractCompositeFilter {

		public And() {
			super();
		}

		public And(List<Filter> filters) {
			super(filters);
		}
	}
	
	public static class Or extends AbstractCompositeFilter {

		public Or() {
			super();
		}

		public Or(List<Filter> filters) {
			super(filters);
		}
	}
	
	public static class Not extends AbstractCompositeFilter {

		private Filter filter;

		public Not() {
			super();
		}
		
		public Not(Filter filter) {
			super(new ArrayList<Filter>(List.of(filter)));
			this.filter = filter;
		}

		public Filter getFilter() {
			return filter;
		}

		public void setFilter(Filter filter) {
			this.filter = filter;
		}
	}
	
	public static class Fulltext extends AbstractAtomicFilter {

		private String expression;

		public Fulltext() {
			super();
		}

		public Fulltext(String expression) {
			super();
			this.expression = expression;
		}

		public String getExpression() {
			return expression;
		}

		public void setExpression(String expression) {
			this.expression = expression;
		}
	}
	
	public static class Equals extends AbstractAtomicFilter {

		private String field;
		private Object expectedValue;

		public Equals() {super();};

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

		public void setField(String field) {
			this.field = field;
		}

		public void setExpectedValue(Object expectedValue) {
			this.expectedValue = expectedValue;
		}
	}
	
	public static class Gt extends AbstractAtomicFilter {

		private String field;
		private long value;

		public Gt() {
			super();
		}

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

		public void setField(String field) {
			this.field = field;
		}

		public void setValue(long value) {
			this.value = value;
		}
	}
	
	public static class Gte extends AbstractAtomicFilter {

		private String field;
		private long value;

		public Gte() {
			super();
		}

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

		public void setField(String field) {
			this.field = field;
		}

		public void setValue(long value) {
			this.value = value;
		}
	}
	
	public static class Lt extends AbstractAtomicFilter {

		private String field;
		private long value;

		public Lt() {
			super();
		}

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

		public void setField(String field) {
			this.field = field;
		}

		public void setValue(long value) {
			this.value = value;
		}
	}
	
	public static class Lte extends AbstractAtomicFilter {

		private String field;
		private long value;

		public Lte() {
			super();
		}

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

		public void setField(String field) {
			this.field = field;
		}

		public void setValue(long value) {
			this.value = value;
		}
	}
	
	public static class In extends AbstractAtomicFilter {

		private String field;
		private List<String> values;

		public In() {
			super();
		}

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

		public void setField(String field) {
			this.field = field;
		}

		public void setValues(List<String> values) {
			this.values = values;
		}
	}
	
	public static class Regex extends AbstractAtomicFilter {

		private String field;
		private String expression;
		private boolean caseSensitive;

		public Regex() {
			super();
		}

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

		public void setField(String field) {
			this.field = field;
		}

		public void setExpression(String expression) {
			this.expression = expression;
		}

		public void setCaseSensitive(boolean caseSensitive) {
			this.caseSensitive = caseSensitive;
		}
	}
	
	public static class True extends AbstractAtomicFilter {

		public True() {
			super();
		}
	}
}
