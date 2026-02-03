package step.cli;

import picocli.CommandLine;
import step.cli.parameters.LibraryDeployParameters;
import step.core.Constants;
import step.core.maven.MavenArtifactIdentifier;

import java.io.File;
import java.util.concurrent.Callable;

import static step.cli.ApCommand.AbstractApCommand.prepareFile;


@CommandLine.Command(name = LibraryCommand.LIBRARY_COMMAND,
        mixinStandardHelpOptions = true,
        version = Constants.STEP_VERSION_STRING,
        description = "The CLI interface to manage automation package libraries in Step",
        usageHelpAutoWidth = true
)
public class LibraryCommand implements Callable<Integer> {

    public static final String LIBRARY_COMMAND = "library";

    @Override
    public Integer call() throws Exception {
        // call help by default
        // call help by default
        return StepConsole.addLibrarySubcommands(new CommandLine(new LibraryCommand()), LibraryCommand.DeployLibraryCommand::new)
                .execute("help");

    }

    public static abstract class AbstractLibraryCommand extends StepConsole.AbstractStepCommand {
        @CommandLine.Option(names = {"-l", "--library"}, paramLabel = "<Library Path>", description = "The file path or maven coordinate (mvn:groupId:artefactId:version[:classifier:type]) of the automation package library.")
        protected String libraryPath;

    }

    public static class DeployLibraryCommand extends AbstractLibraryCommand {

        @CommandLine.Option(names = {"--managed"}, description = "Use this option to deploy a managed library with the provided name. Redeploying a managed library using the same name will update its content and reload all automation packages using it.")
        protected String managedLibraryName;

        @Override
        public Integer call() throws Exception {
            super.call();
            handleLibraryDeployCommand();
            return 0;
        }

        private void handleLibraryDeployCommand() {
            checkAll();
            MavenArtifactIdentifier libraryMavenIdentifier = getMavenArtifact(libraryPath);
            File libraryFile = (libraryMavenIdentifier == null) ?
                    prepareFile(libraryPath, "package library", false) :
                    null;
            LibraryDeployParameters libraryDeployParameters = new LibraryDeployParameters()
                    .setStepProjectName(getStepProjectName())
                    .setAuthToken(getAuthToken())
                    .setLibraryMavenArtifact(libraryMavenIdentifier)
                    .setLibraryFile(libraryFile)
                    .setManagedLibraryName(managedLibraryName);

            executeTool(stepUrl, libraryDeployParameters);
        }

        protected void executeTool(String stepUrl, LibraryDeployParameters libraryDeployParameters) {
            DeployLibraryTool deployLibraryTool = new DeployLibraryTool(stepUrl, libraryDeployParameters);
            deployLibraryTool.execute();
        }

    }
}