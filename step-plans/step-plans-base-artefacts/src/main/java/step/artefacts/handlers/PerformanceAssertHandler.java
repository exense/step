package step.artefacts.handlers;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import step.artefacts.Aggregation;
import step.artefacts.Aggregator;
import step.artefacts.Comparator;
import step.artefacts.Filter;
import step.artefacts.PerformanceAssert;
import step.artefacts.reports.PerformanceAssertReportNode;
import step.core.accessors.AbstractOrganizableObject;
import step.core.artefacts.handlers.ArtefactHandler;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.reports.Error;
import step.core.reports.ErrorType;
import step.plans.assertions.PerformanceAssertPlugin;
import step.plans.assertions.PerformanceAssertSession;

public class PerformanceAssertHandler extends ArtefactHandler<PerformanceAssert, PerformanceAssertReportNode> {

	@Override
	public void initProperties(ReportNode parentReportNode, PerformanceAssert artefact) {
		PerformanceAssertSession performanceAssertSession = new PerformanceAssertSession();
		context.getVariablesManager().putVariable(parentReportNode, PerformanceAssertPlugin.$PERFORMANCE_ASSERT_SESSION, performanceAssertSession);
	}

	@Override
	protected void createReportSkeleton_(PerformanceAssertReportNode parentNode, PerformanceAssert testArtefact) {
		
	}

	@Override
	protected void execute_(PerformanceAssertReportNode reportNode, PerformanceAssert artefact) throws Exception {
		PerformanceAssertSession session = (PerformanceAssertSession) context.getVariablesManager().getVariable(PerformanceAssertPlugin.$PERFORMANCE_ASSERT_SESSION);
		List<String> errors = new ArrayList<>();
		if(session != null) {
			List<Filter> filters = artefact.getFilters();
			
			List<Entry<String, Aggregation>> matchingAggregations = session.getAggregations().stream().filter(a->matchesFilters(filters, a.getValue())).collect(Collectors.toList());
			if(matchingAggregations.size() > 0) {
				for (Entry<String, Aggregation> entry : matchingAggregations) {
					Aggregation aggregation = entry.getValue();
					Aggregator aggregator = artefact.getAggregator();
					Comparator comparator = artefact.getComparator();
					Number actualValue = aggregator.getValueFunction().apply(aggregation);
					Number expectedValue = artefact.getExpectedValue().get();
					if (!comparator.getComparatorFunction().apply(actualValue, expectedValue)) {
						String errorMessage = aggregator.getDescription() + " of " + aggregation.getName()
						+ " expected to be " + comparator.getErrorDescription() + " " + expectedValue + " but was "
						+ actualValue;
						errors.add(errorMessage);
					}
				}
			} else {
				errors.add("No measurement is matching the defined filters.");
			}
		}
		if(errors.size()>0) {
			reportNode.setError(new Error(ErrorType.BUSINESS, errors.stream().collect(Collectors.joining("; "))));
			reportNode.setStatus(ReportNodeStatus.FAILED);
		} else {
			reportNode.setStatus(ReportNodeStatus.PASSED);
		}
	}

	protected boolean matchesFilters(List<Filter> filters, Aggregation aggregation) {
		boolean allFiltersMatch = true;
		for (Filter filter : filters) {
			String field = filter.getField().get();
			if(field == null || field.equals(AbstractOrganizableObject.NAME)) {
				String name = aggregation.getName();
				String filterExpression = filter.getFilter().get();
				if(!filter.getFilterType().getFilterFunction().apply(name, filterExpression)) {
					allFiltersMatch = false;
				}
			} else {
				throw new RuntimeException("Unsupported filter field "+field);
			}
		}
		return allFiltersMatch;
	}

	@Override
	protected PerformanceAssertReportNode createReportNode_(ReportNode parentReportNode, PerformanceAssert artefact) {
		return new PerformanceAssertReportNode();
	}

}
