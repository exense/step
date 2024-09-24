package step.plugins.timeseries;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class AttributeValuesStats {
    
    private Map<Object, AtomicInteger> valuesCount = new HashMap<>();
    private int totalCount;

    public Map<Object, AtomicInteger> getValuesCount() {
        return valuesCount;
    }

    public void incrementTotalCount() {
        totalCount++;
    }

    public int getTotalCount() {
        return totalCount;
    }
    
}
