package step.core.reporting;

import org.junit.Before;
import org.junit.Test;
import step.core.accessors.AbstractOrganizableObject;
import step.core.collections.inmemory.InMemoryCollectionFactory;
import step.core.reporting.model.ReportLayout;

import java.util.List;
import java.util.Properties;

import static org.junit.Assert.*;

public class ReportLayoutAccessorTest {

    private ReportLayoutAccessor accessor;

    @Before
    public void setUp() {
        InMemoryCollectionFactory factory = new InMemoryCollectionFactory(new Properties());
        accessor = new ReportLayoutAccessor(factory.getCollection("reportLayouts", ReportLayout.class));
    }

    private ReportLayout saveLayout(String name, ReportLayout.ReportLayoutVisibility visibility, String user) {
        ReportLayout layout = new ReportLayout(null, visibility);
        layout.addAttribute(AbstractOrganizableObject.NAME, name);
        layout.setCreationUser(user);
        return accessor.save(layout);
    }

    // --- getAccessibleReportLayoutsDefinitions ---

    @Test
    public void getAccessibleReportLayoutsDefinitions_includesPresets() {
        saveLayout("GlobalPreset", ReportLayout.ReportLayoutVisibility.Preset, null);

        List<ReportLayout> layouts = accessor.getAccessibleReportLayoutsDefinitions("anyUser");

        assertEquals(1, layouts.size());
        assertEquals("GlobalPreset", layouts.get(0).getAttribute(AbstractOrganizableObject.NAME));
    }

    @Test
    public void getAccessibleReportLayoutsDefinitions_includesSharedLayouts() {
        saveLayout("SharedLayout", ReportLayout.ReportLayoutVisibility.Shared, "alice");

        List<ReportLayout> layouts = accessor.getAccessibleReportLayoutsDefinitions("bob");

        assertEquals(1, layouts.size());
        assertEquals("SharedLayout", layouts.get(0).getAttribute(AbstractOrganizableObject.NAME));
    }

    @Test
    public void getAccessibleReportLayoutsDefinitions_includesOwnPrivateLayout() {
        saveLayout("AlicePrivate", ReportLayout.ReportLayoutVisibility.Private, "alice");

        List<ReportLayout> layouts = accessor.getAccessibleReportLayoutsDefinitions("alice");

        assertEquals(1, layouts.size());
        assertEquals("AlicePrivate", layouts.get(0).getAttribute(AbstractOrganizableObject.NAME));
    }

    @Test
    public void getAccessibleReportLayoutsDefinitions_excludesOtherUsersPrivateLayout() {
        saveLayout("AlicePrivate", ReportLayout.ReportLayoutVisibility.Private, "alice");

        List<ReportLayout> layouts = accessor.getAccessibleReportLayoutsDefinitions("bob");

        assertTrue(layouts.isEmpty());
    }

    @Test
    public void getAccessibleReportLayoutsDefinitions_combinesAllVisibleTypes() {
        saveLayout("Preset", ReportLayout.ReportLayoutVisibility.Preset, null);
        saveLayout("Shared", ReportLayout.ReportLayoutVisibility.Shared, "alice");
        saveLayout("BobPrivate", ReportLayout.ReportLayoutVisibility.Private, "bob");
        saveLayout("AlicePrivate", ReportLayout.ReportLayoutVisibility.Private, "alice");

        List<ReportLayout> layouts = accessor.getAccessibleReportLayoutsDefinitions("bob");

        assertEquals(3, layouts.size()); // preset + shared + bob's own private
    }

    @Test
    public void getAccessibleReportLayoutsDefinitions_stripsLayoutField() {
        saveLayout("Preset", ReportLayout.ReportLayoutVisibility.Preset, null);
        saveLayout("OwnPrivate", ReportLayout.ReportLayoutVisibility.Private, "alice");

        List<ReportLayout> layouts = accessor.getAccessibleReportLayoutsDefinitions("alice");

        layouts.forEach(l -> assertNull("layout field should be stripped", l.layout));
    }

    @Test
    public void getAccessibleReportLayoutsDefinitions_sortedByName() {
        saveLayout("Z Layout", ReportLayout.ReportLayoutVisibility.Preset, null);
        saveLayout("A Layout", ReportLayout.ReportLayoutVisibility.Preset, null);
        saveLayout("M Layout", ReportLayout.ReportLayoutVisibility.Preset, null);

        List<ReportLayout> layouts = accessor.getAccessibleReportLayoutsDefinitions("anyUser");

        assertEquals(3, layouts.size());
        assertEquals("A Layout", layouts.get(0).getAttribute(AbstractOrganizableObject.NAME));
        assertEquals("M Layout", layouts.get(1).getAttribute(AbstractOrganizableObject.NAME));
        assertEquals("Z Layout", layouts.get(2).getAttribute(AbstractOrganizableObject.NAME));
    }
}
