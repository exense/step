package step.core.execution;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.bson.Document;
import org.bson.conversions.Bson;

import step.core.GlobalContext;
import step.core.accessors.Collection;
import step.core.accessors.CollectionFind;
import step.core.accessors.CollectionRegistry;
import step.core.accessors.DateRangeCriterium;
import step.core.accessors.SearchOrder;
import step.core.artefacts.reports.ReportNodeStatus;
import step.core.execution.model.ExecutionStatus;
import step.core.execution.table.ExecutionSummaryFormatter;
import step.core.execution.table.RootReportNodeFormatter;
import step.core.execution.type.ExecutionTypePlugin;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;

@Plugin(dependencies= {ExecutionTypePlugin.class})
public class ExecutionPlugin extends AbstractControllerPlugin {

	@Override
	public void executionControllerStart(GlobalContext context) throws Exception {
		
		RootReportNodeFormatter rootReportNodeFormatter = new RootReportNodeFormatter(context);
		ExecutionSummaryFormatter executionSummaryFormatter = new ExecutionSummaryFormatter(context);
		
		CollectionRegistry collectionRegistry = context.get(CollectionRegistry.class);
		collectionRegistry.register("executions", new Collection(context.getMongoClientSession().getMongoDatabase(), "executions") {

			@Override
			public CollectionFind<Document> find(Bson query, SearchOrder order, Integer skip, Integer limit,
					int maxTime) {
				CollectionFind<Document> find = super.find(query, order, skip, limit, maxTime);
				
				Iterator<Document> iterator = find.getIterator();
				Iterator<Document> filteredIterator = new Iterator<Document>() {

					@Override
					public boolean hasNext() {
						return iterator.hasNext();
					}

					@Override
					public Document next() {
						Document next = iterator.next();
						next.append("RootReportNode", Document.parse(rootReportNodeFormatter.format(next)));
						next.append("Summary", Document.parse(executionSummaryFormatter.format(next)));
						return next;
					}
					
				};
				CollectionFind<Document> filteredFind = new CollectionFind<>(find.getRecordsTotal(), find.getRecordsFiltered(), filteredIterator);
				return filteredFind;

			}

			@Override
			public CollectionFind<Document> find(Bson query, SearchOrder order, Integer skip, Integer limit) {
				return this.find(query, order, skip, limit, 0);
			}

			@Override
			public List<String> distinct(String key) {
				if(key.equals("result")) {
					return Arrays.asList(ReportNodeStatus.values()).stream().map(Object::toString).collect(Collectors.toList());
				} else if(key.equals("status")) {
					return Arrays.asList(ExecutionStatus.values()).stream().map(Object::toString).collect(Collectors.toList());
				} else {
					return super.distinct(key);
				}
			}

			@Override
			public Bson getQueryFragment(String columnName, String searchValue) {
				if(columnName.equals("startTime") || columnName.equals("endTime")) {
					Bson queryFragment = new DateRangeCriterium("dd.MM.yyyy").createQuery(columnName, searchValue);
					return queryFragment;
				} else {
					return super.getQueryFragment(columnName, searchValue);
				}
			}
		});
	}
}
