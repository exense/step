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

import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.beanutils.NestedNullException;
import org.apache.commons.beanutils.PropertyUtils;

import step.core.accessors.AbstractIdentifiableObject;
import step.core.collections.Filters.And;
import step.core.collections.Filters.Equals;
import step.core.collections.Filters.FilterFactory;
import step.core.collections.Filters.Not;
import step.core.collections.Filters.Or;
import step.core.collections.Filters.Regex;
import step.core.collections.Filters.True;

public class PojoFilters {

	public static class PojoFilterFactory<POJO> implements FilterFactory<PojoFilter<?>> {

		@Override
		public PojoFilter<POJO> buildFilter(Filter filter) {

			List<PojoFilter<POJO>> childerPojoFilters;
			List<Filter> children = filter.getChildren();
			if(children != null) {
				childerPojoFilters = children.stream().map(f -> this.buildFilter(f))
						.collect(Collectors.toList());
			} else {
				childerPojoFilters = null;
			}

			if (filter instanceof And) {
				return new AndPojoFilter<POJO>(childerPojoFilters);
			} else if (filter instanceof Or) {
				return new OrPojoFilter<POJO>(childerPojoFilters);
			} else if (filter instanceof Not) {
				return new NotPojoFilter<POJO>(childerPojoFilters.get(0));
			} else if (filter instanceof Or) {
				return new OrPojoFilter<POJO>(childerPojoFilters);
			} else if (filter instanceof Equals) {
				return new EqualsPojoFilter<POJO>((Equals) filter);
			} else if (filter instanceof Regex) {
				return new RegexPojoFilter<POJO>((Regex) filter);
			} else if (filter instanceof True) {
				return new TruePojoFilter<POJO>();
			} else {
				throw new IllegalArgumentException("Unsupported filter type " + filter.getClass());
			}
		}
	}

	public static class AndPojoFilter<T> implements PojoFilter<T> {

		private final List<PojoFilter<T>> pojoFilters;

		public AndPojoFilter(List<PojoFilter<T>> PojoFilters) {
			super();
			this.pojoFilters = PojoFilters;
		}

		@Override
		public boolean test(T t) {
			return pojoFilters.stream().allMatch(PojoFilter -> PojoFilter.test(t));
		}
	}

	public static class OrPojoFilter<T> implements PojoFilter<T> {

		private final List<PojoFilter<T>> pojoFilters;

		public OrPojoFilter(List<PojoFilter<T>> PojoFilters) {
			super();
			this.pojoFilters = PojoFilters;
		}

		@Override
		public boolean test(T t) {
			return pojoFilters.stream().anyMatch(PojoFilter -> PojoFilter.test(t));
		}
	}

	public static class NotPojoFilter<T> implements PojoFilter<T> {

		private final PojoFilter<T> pojoFilter;

		public NotPojoFilter(PojoFilter<T> PojoFilter) {
			super();
			this.pojoFilter = PojoFilter;
		}

		@Override
		public boolean test(T t) {
			return !pojoFilter.test(t);
		}
	}
	
	public static class TruePojoFilter<T> implements PojoFilter<T> {

		public TruePojoFilter() {
			super();
		}

		@Override
		public boolean test(T t) {
			return true;
		}
	}

	public static class EqualsPojoFilter<T> implements PojoFilter<T> {

		private final Equals equalsFilter;

		public EqualsPojoFilter(Equals equalsFilter) {
			super();
			this.equalsFilter = equalsFilter;
		}

		@Override
		public boolean test(T t) {
			try {
				String field = equalsFilter.getField();
				Object beanProperty = getBeanProperty(t, field);
				Object expectedValue = equalsFilter.getExpectedValue();
				if(expectedValue != null) {
					return expectedValue.equals(beanProperty);
				} else {
					return beanProperty == null; 
				}
			} catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
				return false;
			}
		}
	}

	public static class RegexPojoFilter<T> implements PojoFilter<T> {

		private final Regex regexFilter;
		private final Pattern pattern;

		public RegexPojoFilter(Regex regexFilter) {
			super();
			this.regexFilter = regexFilter;
			String expression = "";
			if(!regexFilter.isCaseSensitive()) {
				expression += "(?i)";
			}
			expression += regexFilter.getExpression();
			pattern = Pattern.compile(expression);
		}

		@Override
		public boolean test(T t) {
			try {
				Object beanProperty = getBeanProperty(t, regexFilter.getField());
				if(beanProperty != null) {
					Matcher matcher = pattern.matcher(beanProperty.toString());
					return matcher.find();
				} else {
					return false;
				}
			} catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
				return false;
			}
		}
	}
	
	private static Object getBeanProperty(Object t, String fieldName)
			throws IllegalAccessException, InvocationTargetException, NoSuchMethodException {
		try {
			if(fieldName.equals("_class")) {
				return t.getClass().getName();
			} else if(fieldName.equals(AbstractIdentifiableObject.ID)) {
				if(t instanceof Document) {
					return ((Document) t).getId();
				} else {
					return PropertyUtils.getProperty(t, fieldName);
				}
			} else {
				return PropertyUtils.getProperty(t, fieldName);
			}
		} catch (NestedNullException e) {
			return null;
		}
		
	}
}
