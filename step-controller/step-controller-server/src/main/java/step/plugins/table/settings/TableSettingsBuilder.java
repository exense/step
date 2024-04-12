package step.plugins.table.settings;

import step.plugins.screentemplating.ScreenInput;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import static step.plugins.table.settings.TableSettings.SETTINGS_BASE_SCOPE_KEY;

public class TableSettingsBuilder {

    private String settingId;
    private List<ColumnSettings> columnSettingsList = new ArrayList<>();

    AtomicInteger position = new AtomicInteger(0);

    public static TableSettingsBuilder builder() {
        return new TableSettingsBuilder();
    }

    public TableSettingsBuilder() {
    }

    public TableSettingsBuilder withSettingId(String settingId) {
        this.settingId = settingId;
        return this;
    }

    public TableSettingsBuilder addColumn(String columnId, boolean visible) {
        columnSettingsList.add(new ColumnSettings(columnId, visible, position.getAndIncrement()));
        return this;
    }

    public TableSettingsBuilder addColumn(String columnId, boolean visible, ScreenInput input) {
        columnSettingsList.add(new ScreenInputColumnSettings(columnId, visible, position.getAndIncrement(), input));
        return this;
    }


    public TableSettings build() {
        Objects.requireNonNull(settingId, "settingId must be set.");
        TableSettings tableSettings = new TableSettings();
        tableSettings.addScope(SETTINGS_BASE_SCOPE_KEY, settingId);
        tableSettings.setColumnSettingList(columnSettingsList);
        return tableSettings;
    }

}
