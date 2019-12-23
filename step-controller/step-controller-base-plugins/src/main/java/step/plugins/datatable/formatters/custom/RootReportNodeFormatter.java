package step.plugins.datatable.formatters.custom;

import java.util.Iterator;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import step.core.GlobalContext;
import step.core.artefacts.reports.ReportNode;
import step.core.artefacts.reports.ReportNodeAccessor;
import step.core.deployment.JacksonMapperProvider;
import step.plugins.datatable.formatters.Formatter;

public class RootReportNodeFormatter implements Formatter {
				
	protected ReportNodeAccessor reportNodeAccessor;
	protected ObjectMapper mapper;
	
	private static final Logger logger = LoggerFactory.getLogger(RootReportNodeFormatter.class);

	public RootReportNodeFormatter(GlobalContext context) {
		super();
		reportNodeAccessor = context.getReportAccessor();
		mapper = JacksonMapperProvider.createMapper();
	}

	@Override
	public String format(Object value, Document row) {
		String eid = row.get("_id").toString();
		
		ReportNode rootReportNode = reportNodeAccessor.getRootReportNode(eid);
		if(rootReportNode!=null) {
			Iterator<ReportNode> rootReportNodeChildren = reportNodeAccessor.getChildren(rootReportNode.getId());
			if(rootReportNodeChildren.hasNext()) {
				rootReportNode = rootReportNodeChildren.next();
				if(rootReportNode != null) {
					try {
						return mapper.writeValueAsString(rootReportNode);
					} catch (JsonProcessingException e) {
						logger.error("Error while serializing report node "+rootReportNode, e);
					}
				} else {
					logger.error("Error while getting root report node for execution. "
							+ "Iterator.next() returned null although Iterator.hasNext() returned true. "
							+ "This should not occur "+eid);
				}
			} else {
				logger.debug("No children found for report node with id "+rootReportNode.getId());
			}
		}
		return "{}";
	}

	@Override
	public Object parse(String formattedValue) {
		throw new RuntimeException("Not implemented");
	}

}
