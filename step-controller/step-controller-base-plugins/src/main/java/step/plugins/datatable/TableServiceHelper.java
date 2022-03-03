package step.plugins.datatable;

import step.core.collections.Filter;
import step.core.ql.OQLFilterBuilder;
import step.core.tables.Table;

import javax.ws.rs.core.MultivaluedMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TableServiceHelper {

    private Pattern namePattern = Pattern.compile("columns\\[([0-9]+)\\]\\[name\\]");
    private Pattern columnSearchPattern = Pattern.compile("columns\\[([0-9]+)\\]\\[search\\]\\[value\\]");
    private Pattern searchPattern = Pattern.compile("search\\[value\\]");

    public Map<Integer, String> getColumnNamesMap(MultivaluedMap<String, String> params) {
        Map<Integer, String> columnNames = new HashMap<>();

        for(String key:params.keySet()) {
            Matcher m = namePattern.matcher(key);
            if(m.matches()) {
                int columnID = Integer.parseInt(m.group(1));
                String columnName = params.getFirst(key);
                columnNames.put(columnID, columnName);
            }
        }
        return columnNames;
    }
    
    public List<Filter> createQueryFragments(MultivaluedMap<String, String> params, Map<Integer, String> columnNames, Table<?> table) {
        List<Filter> queryFragments = new ArrayList<>();
        for(String key:params.keySet()) {
            Matcher m = columnSearchPattern.matcher(key);
            Matcher searchMatcher = searchPattern.matcher(key);
            if(m.matches()) {
                int columnID = Integer.parseInt(m.group(1));
                String columnName = columnNames.get(columnID);
                String searchValue = params.getFirst(key);

                if(searchValue!=null && searchValue.length()>0) {
                    Filter columnQueryFragment = table.getQueryFragmentForColumnSearch(columnName, searchValue);
                    queryFragments.add(columnQueryFragment);
                }
            } else if(searchMatcher.matches()) {
                String searchValue = params.getFirst(key);
                if(searchValue!=null && searchValue.length()>0) {
                    // TODO implement full text search
                }
            }
        }
        if(params.containsKey("filter")) {
            Filter filter = OQLFilterBuilder.getFilter(params.getFirst("filter"));
            queryFragments.add(filter);
        }
        return queryFragments;
    }
}
