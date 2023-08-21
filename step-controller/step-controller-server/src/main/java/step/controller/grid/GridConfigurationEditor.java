package step.controller.grid;

import step.grid.GridImpl;

import java.util.LinkedList;

public interface GridConfigurationEditor {
    void editConfiguration(GridImpl.GridImplConfig config);

    class List extends LinkedList<GridConfigurationEditor> {}
}
