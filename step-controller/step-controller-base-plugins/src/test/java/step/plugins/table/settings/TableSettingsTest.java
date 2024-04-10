/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
 *
 * This file is part of STEP
 *
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package step.plugins.table.settings;

import org.junit.Test;
import step.core.access.User;
import step.core.collections.inmemory.InMemoryCollection;
import step.core.controller.settings.ObjectScopeHandler;
import step.core.controller.settings.ObjectScopeRegistry;
import step.framework.server.Session;
import step.plugins.screentemplating.ScreenInput;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.*;
import static step.plugins.table.settings.TableSettings.SETTINGS_BASE_SCOPE_KEY;

public class TableSettingsTest {

    @Test
    public void testTableSettings() {
        ObjectScopeRegistry objectScopeRegistry = new ObjectScopeRegistry();
        objectScopeRegistry.register("scope1", new TestSettingScopeHandler("scope1", 1));
        objectScopeRegistry.register("scope2", new TestSettingScopeHandler("scope2", 2));
        TableSettingsAccessor tableSettingsAccessor = new TableSettingsAccessor(new InMemoryCollection<TableSettings>(false), objectScopeRegistry);

        Map<String, String> baseScopeSettingsOther = Map.of(SETTINGS_BASE_SCOPE_KEY, "mySettingOther");
        Map<String, String> baseScopeSettings = Map.of(SETTINGS_BASE_SCOPE_KEY, "mySetting");

        //Save settings for different setting id and different scope
        TableSettings tableSettingsOrigOther = new TableSettings();
        tableSettingsOrigOther.setColumnSettingList(List.of(new ScreenInputColumnSettings("column3", true, 1, new ScreenInput())));
        Session<User> sessionOther = new Session<>();
        sessionOther.put("scope1", "valScope1_2");
        sessionOther.put("scope2", "valScope2_2");
        tableSettingsAccessor.saveScopedObject(baseScopeSettingsOther, tableSettingsOrigOther, List.of("scope1","scope2"), sessionOther);

        //First settings saved for mySetting
        TableSettings tableSettingsOrig1 = new TableSettings();
        tableSettingsOrig1.setColumnSettingList(List.of(new ColumnSettings("column1", true, 1)));
        Session<User> session = new Session<>();
        session.put("scope1", "valScope1_1");
        session.put("scope2", "valScope2_1");
        tableSettingsAccessor.saveScopedObject(baseScopeSettings, tableSettingsOrig1, List.of("scope1","scope2"), session);

        //Save settings for the same setting id but different scope
        TableSettings tableSettingsOrig2 = new TableSettings();
        tableSettingsOrig2.setColumnSettingList(List.of(new ScreenInputColumnSettings("column2", true, 1, new ScreenInput())));
        Session<User> session2 = new Session<>();
        session2.put("scope1", "valScope1_2");
        session2.put("scope2", "valScope2_2");
        tableSettingsAccessor.saveScopedObject(baseScopeSettings, tableSettingsOrig2, List.of("scope1","scope2"), session2);


        //Retrieve with same sessions
        Optional<TableSettings> setting = tableSettingsAccessor.getScopedObject(Map.of(SETTINGS_BASE_SCOPE_KEY, "mySetting"), session);
        assertTrue(setting.isPresent());
        TableSettings tableSettings1 = setting.get();
        assertEquals(tableSettingsOrig1, tableSettings1);
        assertEquals(1, tableSettings1.getColumnSettingList().size());
        assertEquals("column1", tableSettings1.getColumnSettingList().get(0).columnId);

        Optional<TableSettings> setting2 = tableSettingsAccessor.getScopedObject(Map.of(SETTINGS_BASE_SCOPE_KEY, "mySetting"), session2);
        assertTrue(setting2.isPresent());
        TableSettings tableSettings2 = setting2.get();
        assertEquals(tableSettingsOrig2, tableSettings2);
        assertEquals(1, tableSettings2.getColumnSettingList().size());
        ColumnSettings columnSettings = tableSettings2.getColumnSettingList().get(0);
        assertEquals("column2", columnSettings.columnId);
        assertTrue(columnSettings instanceof ScreenInputColumnSettings);
        assertNotNull(((ScreenInputColumnSettings) columnSettings).getScreenInput());


        //Retrieve with new session scope doesn't match -> should return empty optional
        Session<User> session3 = new Session<>();
        session3.put("scope1", "different");
        session3.put("scope2", "different");
        Optional<TableSettings> setting3 = tableSettingsAccessor.getScopedObject(Map.of(SETTINGS_BASE_SCOPE_KEY, "mySetting"), session3);
        assertTrue(setting3.isEmpty());

        //Save with partial scope
        TableSettings tableSettingsOrig3 = new TableSettings();
        tableSettingsOrig3.setColumnSettingList(List.of(new ColumnSettings("column3", true, 1)));
        tableSettingsAccessor.saveScopedObject(baseScopeSettings, tableSettingsOrig3, List.of("scope2"), session);

        //Retrieve with new session scope2 value match, scope 1 is different -> should get setting only saved with scope2
        session3 = new Session<>();
        session3.put("scope1", "different");
        session3.put("scope2", "valScope2_1");
        setting3 = tableSettingsAccessor.getScopedObject(Map.of(SETTINGS_BASE_SCOPE_KEY, "mySetting"), session3);
        assertTrue(setting3.isPresent());
        assertEquals("column3", setting3.get().getColumnSettingList().get(0).columnId);

        //Save with no scope
        TableSettings tableSettingsOrig4 = new TableSettings();
        tableSettingsOrig4.setColumnSettingList(List.of(new ColumnSettings("column4", true, 1)));
        tableSettingsAccessor.saveScopedObject(baseScopeSettings, tableSettingsOrig4, List.of(), session);

        //Retrieve with new session scope doesn't match -> should return settings saved with no scope
        Session<User> session4 = new Session<>();
        session3.put("scope1", "different");
        session3.put("scope2", "different");
        Optional<TableSettings> setting4 = tableSettingsAccessor.getScopedObject(baseScopeSettings, session4);
        assertTrue(setting4.isPresent());
        assertEquals("column4", setting4.get().getColumnSettingList().get(0).columnId);

        //Settings saved with no scope (system wide) removed all more specific scope settings, so we should have one left for "mySetting"
        assertEquals(2, tableSettingsAccessor.stream().count());
        Optional<TableSettings> mySettingOther = tableSettingsAccessor.getScopedObject(baseScopeSettingsOther, sessionOther);
        assertTrue(mySettingOther.isPresent());
        assertEquals("mySettingOther", mySettingOther.get().getScope().get(SETTINGS_BASE_SCOPE_KEY));
        assertEquals("valScope1_2", mySettingOther.get().getScope().get("scope1"));
        assertEquals("column3", mySettingOther.get().getColumnSettingList().get(0).columnId);

    }

    public static class TestSettingScopeHandler extends ObjectScopeHandler {

        private final int prio;

        public TestSettingScopeHandler(String scopeName, int prio) {
            super(scopeName);
            this.prio = prio;
        }

        @Override
        protected String getScopeValue(Session<?> session) {
            return (String) session.get(getScopeName());
        }

        @Override
        protected int getPriority() {
            return prio;
        }
    }
}
