package step.automation.packages;

public class ManagedLibraryMissingException extends Exception {

    public ManagedLibraryMissingException(String managedLibraryName) {
        super("The managed library with name " + managedLibraryName + " doesn't exist");
    }
}
