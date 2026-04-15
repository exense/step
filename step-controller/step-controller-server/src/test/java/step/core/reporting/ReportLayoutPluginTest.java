package step.core.reporting;

import ch.exense.commons.app.Configuration;
import org.bson.types.ObjectId;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import step.core.GlobalContext;
import step.core.accessors.AbstractOrganizableObject;
import step.core.collections.inmemory.InMemoryCollectionFactory;
import step.core.deployment.WebApplicationConfigurationManager;
import step.core.entities.EntityManager;
import step.core.reporting.model.ReportLayout;
import step.framework.server.ServiceRegistrationCallback;
import step.framework.server.tables.TableRegistry;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

public class ReportLayoutPluginTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    private GlobalContext context;
    private ReportLayoutPlugin plugin;
    private ReportLayoutAccessor accessor;

    @Before
    public void setUp() throws Exception {
        context = new GlobalContext();
        context.setCollectionFactory(new InMemoryCollectionFactory(new Properties()));
        context.put(TableRegistry.class, new TableRegistry());
        context.setEntityManager(new EntityManager());
        context.setServiceRegistrationCallback(Mockito.mock(ServiceRegistrationCallback.class));
        context.setConfiguration(new Configuration());
        context.put(WebApplicationConfigurationManager.class, new WebApplicationConfigurationManager());

        plugin = new ReportLayoutPlugin();
        plugin.serverStart(context);
        accessor = context.require(ReportLayoutAccessor.class);
    }

    private void setPresetsFolder(File folder) {
        context.getConfiguration().putProperty(
                ReportLayoutPlugin.PRESET_FOLDER_PATH_CONFIG_KEY, folder.getAbsolutePath());
    }

    /** Writes a preset JSON file with a fresh ObjectId. Returns the generated id. */
    private String writePresetFile(File folder, String fileName, String name) throws IOException {
        String id = new ObjectId().toHexString();
        writePresetFileWithId(folder, fileName, name, id, "{\"type\":\"test\"}");
        return id;
    }

    private void writePresetFileWithId(File folder, String fileName, String name, String id, String layoutJson) throws IOException {
        File file = new File(folder, fileName);
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("{\"id\":\"" + id + "\",\"name\":\"" + name + "\",\"layout\":" + layoutJson + "}");
        }
    }

    private List<ReportLayout> getAllPresets() {
        return accessor.getCollectionDriver()
                .find(step.core.collections.Filters.equals(
                        ReportLayout.FIELD_VISIBILITY,
                        ReportLayout.ReportLayoutVisibility.Preset.name()),
                        null, null, null, 0)
                .collect(Collectors.toList());
    }

    // --- initializeData with valid folder ---

    @Test
    public void initializeData_singleValidFile_createsPreset() throws Exception {
        File folder = tempFolder.newFolder("presets");
        writePresetFile(folder, "layout1.json", "My Preset");
        setPresetsFolder(folder);

        plugin.initializeData(context);

        List<ReportLayout> presets = getAllPresets();
        assertEquals(1, presets.size());
        assertEquals("My Preset", presets.get(0).getAttribute(AbstractOrganizableObject.NAME));
        assertEquals(ReportLayout.ReportLayoutVisibility.Preset, presets.get(0).visibility);
    }

    @Test
    public void initializeData_multipleValidFiles_createsAllPresets() throws Exception {
        File folder = tempFolder.newFolder("presets");
        writePresetFile(folder, "alpha.json", "Alpha");
        writePresetFile(folder, "beta.json", "Beta");
        writePresetFile(folder, "gamma.json", "Gamma");
        setPresetsFolder(folder);

        plugin.initializeData(context);

        List<ReportLayout> presets = getAllPresets();
        assertEquals(3, presets.size());
        List<String> names = presets.stream()
                .map(p -> p.getAttribute(AbstractOrganizableObject.NAME))
                .collect(Collectors.toList());
        assertTrue(names.contains("Alpha"));
        assertTrue(names.contains("Beta"));
        assertTrue(names.contains("Gamma"));
    }

    @Test
    public void initializeData_preservesIdFromJsonFile() throws Exception {
        File folder = tempFolder.newFolder("presets");
        String fixedId = new ObjectId().toHexString();
        writePresetFileWithId(folder, "layout.json", "Fixed Id Preset", fixedId, "{\"type\":\"test\"}");
        setPresetsFolder(folder);

        plugin.initializeData(context);

        List<ReportLayout> presets = getAllPresets();
        assertEquals(1, presets.size());
        assertEquals(fixedId, presets.get(0).getId().toHexString());
    }

    @Test
    public void initializeData_nonJsonFilesIgnored() throws Exception {
        File folder = tempFolder.newFolder("presets");
        writePresetFile(folder, "layout.json", "Valid Preset");
        new File(folder, "readme.txt").createNewFile();
        new File(folder, "layout.xml").createNewFile();
        setPresetsFolder(folder);

        plugin.initializeData(context);

        List<ReportLayout> presets = getAllPresets();
        assertEquals(1, presets.size());
        assertEquals("Valid Preset", presets.get(0).getAttribute(AbstractOrganizableObject.NAME));
    }

    // --- initializeData drops and recreates presets on each startup ---

    @Test
    public void initializeData_dropsExistingPresetsBeforeLoading() throws Exception {
        File folder = tempFolder.newFolder("presets");
        writePresetFile(folder, "initial.json", "Initial Preset");
        setPresetsFolder(folder);
        plugin.initializeData(context);
        assertEquals(1, getAllPresets().size());

        // Replace file content and re-run initializeData (simulates restart)
        new File(folder, "initial.json").delete();
        writePresetFile(folder, "updated.json", "Updated Preset");

        plugin.initializeData(context);

        List<ReportLayout> presets = getAllPresets();
        assertEquals(1, presets.size());
        assertEquals("Updated Preset", presets.get(0).getAttribute(AbstractOrganizableObject.NAME));
    }

    @Test
    public void initializeData_idempotent_sameIdEachRestart() throws Exception {
        File folder = tempFolder.newFolder("presets");
        String fixedId = new ObjectId().toHexString();
        writePresetFileWithId(folder, "layout.json", "Stable Preset", fixedId, "{\"type\":\"test\"}");
        setPresetsFolder(folder);

        plugin.initializeData(context);
        plugin.initializeData(context);

        List<ReportLayout> presets = getAllPresets();
        assertEquals("Second run should not duplicate the preset", 1, presets.size());
        assertEquals(fixedId, presets.get(0).getId().toHexString());
    }

    @Test
    public void initializeData_emptyFolder_dropsAllExistingPresets() throws Exception {
        ReportLayout existing = new ReportLayout(null, ReportLayout.ReportLayoutVisibility.Preset);
        existing.addAttribute(AbstractOrganizableObject.NAME, "Old Preset");
        accessor.save(existing);
        assertEquals(1, getAllPresets().size());

        File emptyFolder = tempFolder.newFolder("empty-presets");
        setPresetsFolder(emptyFolder);

        plugin.initializeData(context);

        assertTrue("All presets should be dropped when folder is empty", getAllPresets().isEmpty());
    }

    @Test
    public void initializeData_doesNotDropNonPresetLayouts() throws Exception {
        ReportLayout privateLayout = new ReportLayout(null, ReportLayout.ReportLayoutVisibility.Private);
        privateLayout.addAttribute(AbstractOrganizableObject.NAME, "My Private");
        privateLayout.setCreationUser("alice");
        accessor.save(privateLayout);

        ReportLayout sharedLayout = new ReportLayout(null, ReportLayout.ReportLayoutVisibility.Shared);
        sharedLayout.addAttribute(AbstractOrganizableObject.NAME, "My Shared");
        sharedLayout.setCreationUser("bob");
        accessor.save(sharedLayout);

        File folder = tempFolder.newFolder("presets");
        writePresetFile(folder, "preset.json", "New Preset");
        setPresetsFolder(folder);

        plugin.initializeData(context);

        assertNotNull(accessor.get(privateLayout.getId()));
        assertNotNull(accessor.get(sharedLayout.getId()));
        assertEquals(1, getAllPresets().size());
    }

    // --- initializeData with missing / invalid folder ---

    @Test
    public void initializeData_missingFolder_createsNoPresets() throws Exception {
        context.getConfiguration().putProperty(
                ReportLayoutPlugin.PRESET_FOLDER_PATH_CONFIG_KEY,
                "/nonexistent/path/to/presets");

        plugin.initializeData(context);

        assertTrue("No presets should be created when folder does not exist", getAllPresets().isEmpty());
    }

    @Test
    public void initializeData_missingFolder_dropsExistingPresets() throws Exception {
        ReportLayout existing = new ReportLayout(null, ReportLayout.ReportLayoutVisibility.Preset);
        existing.addAttribute(AbstractOrganizableObject.NAME, "Existing Preset");
        accessor.save(existing);

        context.getConfiguration().putProperty(
                ReportLayoutPlugin.PRESET_FOLDER_PATH_CONFIG_KEY,
                "/nonexistent/path/to/presets");

        plugin.initializeData(context);

        // The drop step runs unconditionally before the folder existence check
        assertTrue("Existing presets are dropped even when folder is missing", getAllPresets().isEmpty());
    }

    // --- id validation ---

    @Test
    public void initializeData_fileWithMissingId_isSkipped() throws Exception {
        File folder = tempFolder.newFolder("presets");
        try (FileWriter writer = new FileWriter(new File(folder, "no-id.json"))) {
            writer.write("{\"name\":\"No Id Preset\",\"layout\":{\"type\":\"test\"}}");
        }
        setPresetsFolder(folder);

        plugin.initializeData(context);

        assertTrue("Preset with missing id should be skipped", getAllPresets().isEmpty());
    }

    @Test
    public void initializeData_fileWithInvalidId_isSkipped() throws Exception {
        File folder = tempFolder.newFolder("presets");
        try (FileWriter writer = new FileWriter(new File(folder, "bad-id.json"))) {
            writer.write("{\"id\":\"not-a-valid-objectid\",\"name\":\"Bad Id Preset\",\"layout\":{\"type\":\"test\"}}");
        }
        setPresetsFolder(folder);

        plugin.initializeData(context);

        assertTrue("Preset with invalid id should be skipped", getAllPresets().isEmpty());
    }

    @Test
    public void initializeData_invalidJsonFile_skipsItAndLoadsOthers() throws Exception {
        File folder = tempFolder.newFolder("presets");
        try (FileWriter writer = new FileWriter(new File(folder, "broken.json"))) {
            writer.write("{ this is not valid json }");
        }
        writePresetFile(folder, "valid.json", "Valid Preset");
        setPresetsFolder(folder);

        plugin.initializeData(context);

        List<ReportLayout> presets = getAllPresets();
        assertEquals("Valid preset should still be loaded despite broken file", 1, presets.size());
        assertEquals("Valid Preset", presets.get(0).getAttribute(AbstractOrganizableObject.NAME));
    }

    @Test
    public void initializeData_allInvalidFiles_createsNoPresets() throws Exception {
        File folder = tempFolder.newFolder("presets");
        try (FileWriter writer = new FileWriter(new File(folder, "broken1.json"))) {
            writer.write("not json at all");
        }
        try (FileWriter writer = new FileWriter(new File(folder, "no-id.json"))) {
            writer.write("{\"name\":\"Missing Id\",\"layout\":{}}");
        }
        setPresetsFolder(folder);

        plugin.initializeData(context);

        assertTrue(getAllPresets().isEmpty());
    }

    // --- preset layout content ---

    @Test
    public void initializeData_presetLayoutHasCorrectContent() throws Exception {
        File folder = tempFolder.newFolder("presets");
        String id = new ObjectId().toHexString();
        writePresetFileWithId(folder, "layout.json", "Dashboard", id, "{\"columns\":3,\"rows\":2}");
        setPresetsFolder(folder);

        plugin.initializeData(context);

        List<ReportLayout> presets = getAllPresets();
        assertEquals(1, presets.size());
        ReportLayout preset = presets.get(0);
        assertEquals("Dashboard", preset.getAttribute(AbstractOrganizableObject.NAME));
        assertEquals(ReportLayout.ReportLayoutVisibility.Preset, preset.visibility);
        assertEquals(id, preset.getId().toHexString());
        assertNotNull("Layout JSON should be parsed and stored", preset.layout);
    }
}
