package step.core.artefacts.reports;

public enum ParentSource {
    BEFORE,
    BEFORE_THREAD,
    MAIN(false),
    SUB_PLAN(false),
    AFTER_THREAD,
    AFTER;

    public boolean printSource;

    ParentSource() {
        printSource=true;
    }

    ParentSource(boolean printSource) {
        this.printSource = printSource;
    }
}
