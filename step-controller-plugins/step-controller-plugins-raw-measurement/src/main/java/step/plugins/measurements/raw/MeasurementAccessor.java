package step.plugins.measurements.raw;

import com.mongodb.BasicDBObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.core.collections.*;
import step.core.entities.EntityManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class MeasurementAccessor {
    private static final Logger logger = LoggerFactory.getLogger(MeasurementAccessor.class);
    private Collection<Document> coll;

    public MeasurementAccessor(Collection<Document> coll) {
        this.coll = coll;
    }

    public void sendStructuredMeasurement(Map<String, Object> m) {
        this.saveMeasurementInternal(new Document(m));
    }

    public void sendStructuredMeasurement(String m) {
        this.saveMeasurementInternal(convertToMongo(m));
    }

    private void saveMeasurementInternal(Document m) {
        this.coll.save(m);
    }

    public void saveManyMeasurements(List<Object> lm) {
        this.saveManyMeasurementsInternal(convertManyToMongo(lm));
    }

    public void saveManyMeasurementsInternal(List<Document> lm) {
        this.coll.save(lm);
    }

    public void removeManyViaPattern(Filter f) {
        this.coll.remove(f);
    }

    public long getMeasurementCount() {
        return this.coll.count(Filters.empty(), (Integer) null);
    }

    public Stream<Document> find(Filter filter) {
        return this.coll.find(filter, (SearchOrder) null, (Integer) null, (Integer) null, 0);
    }

    public Stream<Document> advancedFind(Filter filter, List<String> fields) {
        return this.coll.findReduced(filter, (SearchOrder) null, (Integer) null, (Integer) null, 0, fields);
    }

    public Stream<Document> find(Filter filter, SearchOrder sortOrder) {
        return this.coll.find(filter, sortOrder, (Integer) null, (Integer) null, 0);
    }

    public Stream<Document> find(Filter filter, SearchOrder sortOrder, int skip, int limit) {
        return this.coll.find(filter, sortOrder, skip, limit, 0);
    }

    public Iterable<String> distinct(String distinctField, Filter filter) {
        return this.coll.distinct(distinctField, filter);
    }

    public void close() {
    }

    private static Document convertToMongo(String m) {
        return new Document(BasicDBObject.parse(m));
    }

    private static List<Document> convertManyToMongo(List<Object> lm) {
        List<Document> insertables = new ArrayList<>();

        lm.stream()
                .forEach(o -> {
                    if (o instanceof String)
                        insertables.add(convertToMongo((String) o));
                    else {
                        if (o instanceof Map)
                            insertables.add(new Document((Map) o));
                    }
                });
        return insertables;
    }
}
