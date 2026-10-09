package step.ide.collections;

import org.junit.Before;
import org.junit.Test;
import step.core.collections.Collection;
import step.core.collections.Filters;
import step.core.collections.inmemory.InMemoryCollectionFactory;
import step.core.scheduler.ExecutiontTaskParameters;
import step.core.scheduler.SchedulerTaskWrapper;

import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class DynamicallyDelegatingCollectionTest {

    private static final String TASKS = "tasks";

    private InMemoryCollectionFactory currentFactory;
    private Collection<ExecutiontTaskParameters> source;

    @Before
    public void setUp() {
        currentFactory = new InMemoryCollectionFactory(new Properties());
        source = currentFactory.getCollection(TASKS, ExecutiontTaskParameters.class);
        source.save(task("My schedule"));
    }

    @Test
    public void viewConvertsTheEntitiesItReturns() {
        Collection<SchedulerTaskWrapper> view = new DynamicallyDelegatingCollection<>(TASKS, SchedulerTaskWrapper.class,
            ExecutiontTaskParameters.class, currentFactory);

        List<SchedulerTaskWrapper> found = view.find(Filters.empty(), null, null, null, 0).collect(Collectors.toList());
        assertEquals(1, found.size());
        assertTrue(found.get(0) instanceof SchedulerTaskWrapper);
        assertEquals("My schedule", found.get(0).getAttribute("name"));

        assertEquals(1, view.findLazy(Filters.empty(), null, null, null, 0).filter(SchedulerTaskWrapper.class::isInstance).count());
        assertEquals(1, view.findReduced(Filters.empty(), null, null, null, 0, List.of("attributes.name"))
            .filter(SchedulerTaskWrapper.class::isInstance).count());
    }

    @Test
    public void viewIsReadOnly() {
        Collection<SchedulerTaskWrapper> view = new DynamicallyDelegatingCollection<>(TASKS, SchedulerTaskWrapper.class,
            ExecutiontTaskParameters.class, currentFactory);
        SchedulerTaskWrapper wrapper = new SchedulerTaskWrapper();
        wrapper.addAttribute("name", "Written through the view");

        UnsupportedOperationException e = assertThrows(UnsupportedOperationException.class, () -> view.save(wrapper));
        assertEquals("The collection 'tasks' requested as SchedulerTaskWrapper is a read-only view of its ExecutiontTaskParameters entities. "
            + "Entities must be saved through the collection requested as ExecutiontTaskParameters.", e.getMessage());
        assertThrows(UnsupportedOperationException.class, () -> view.save(List.of(wrapper)));

        assertEquals(1, source.count(Filters.empty(), null));
    }

    @Test
    public void collectionOfTheSourceTypeIsWritable() {
        Collection<ExecutiontTaskParameters> collection = new DynamicallyDelegatingCollection<>(TASKS, ExecutiontTaskParameters.class, currentFactory);

        collection.save(task("Second schedule"));
        collection.save(List.of(task("Third schedule")));

        assertEquals(3, source.count(Filters.empty(), null));
    }

    private static ExecutiontTaskParameters task(String name) {
        ExecutiontTaskParameters task = new ExecutiontTaskParameters();
        task.addAttribute("name", name);
        return task;
    }
}
