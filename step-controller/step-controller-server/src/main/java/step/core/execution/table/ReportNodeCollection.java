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
package step.core.execution.table;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import step.core.GlobalContext;
import step.core.accessors.collections.MultiTextCriterium;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.collections.Collection;
import step.core.collections.Filter;
import step.core.collections.Filters;
import step.core.tables.AbstractTable;
import step.plugins.screentemplating.Input;
import step.plugins.screentemplating.ScreenTemplateManager;

public class ReportNodeCollection extends AbstractTable<ReportNode> {
	
	private final List<String> reportSearchAttributes;

	public ReportNodeCollection(GlobalContext context, Collection<ReportNode> collectionDriver) {
		super(collectionDriver, false);
		
		reportSearchAttributes = new ArrayList<>();
		ScreenTemplateManager screenTemplateManager = context.get(ScreenTemplateManager.class);
		if(screenTemplateManager!=null) {
			for(Input input:screenTemplateManager.getInputsForScreen("functionTable", null, null)) {
				reportSearchAttributes.add("functionAttributes."+input.getId());
			}
		}
		reportSearchAttributes.add("input");
		reportSearchAttributes.add("output");
		reportSearchAttributes.add("error.msg");
		reportSearchAttributes.add("name");
	}

	@Override
	public Filter getQueryFragmentForColumnSearch(String columnName, String searchValue) {
		if(columnName.equals("step")) {
			Filter queryFragment = new MultiTextCriterium(reportSearchAttributes).createQuery(columnName, searchValue);
			return queryFragment;
		} else {
			// Returning a list of equality conditions instead of regexp conditions if possible 
			// This make the execution of the query much more efficient because mongo can use the compound index including the status
			// The regexp condition makes the use of the compound index not possible and forces the use of the $match pipeline
			if(columnName.equals("status")) {
				Filter filter = getReportNodeStatusFilterOrNull(searchValue);
				if(filter != null) {
					return filter;
				} else {
					if(searchValue.startsWith("(") && searchValue.endsWith(")")) {
						String[] split = searchValue.substring(1, searchValue.length()-1).split("\\|");
						List<Filter> fragments = new ArrayList<>();
						AtomicBoolean allFilterParsed = new AtomicBoolean(true);
						Arrays.asList(split).forEach(s->{
							Filter status = getReportNodeStatusFilterOrNull(s);
							if(status == null) {
								allFilterParsed.set(false);
							} else {
								fragments.add(status);
							}
						});
						if(allFilterParsed.get()) {
							return Filters.or(fragments);
						} else {
							return super.getQueryFragmentForColumnSearch(columnName, searchValue);
						}
					} else {
						return super.getQueryFragmentForColumnSearch(columnName, searchValue);
					}
				}
			} else {
				return super.getQueryFragmentForColumnSearch(columnName, searchValue);
			}
		}
	}

	protected Filter getReportNodeStatusFilterOrNull(String searchValue) {
		try {
			ReportNodeStatus status = ReportNodeStatus.valueOf(searchValue);
			return Filters.equals("status", status.toString());
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
}
