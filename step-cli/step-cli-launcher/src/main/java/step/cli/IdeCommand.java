package step.cli;

import org.apache.commons.lang3.function.Failable;
import picocli.CommandLine;
import step.cli.parameters.ApExecuteParameters;
import step.core.Constants;
import step.core.execution.model.ExecutionParameters;
import step.ide.LocalIDEState;
import step.ide.api.IDEExecutorDelegate;
import step.ide.api.IDEExecutorDelegateFactory;

import java.io.File;
import java.nio.file.Files;


@CommandLine.Command(name = "ide",
    description = "The CLI interface to launch the local Step IDE",
    version = Constants.STEP_VERSION_STRING,
    mixinStandardHelpOptions = true, usageHelpAutoWidth = true,
    subcommands = {CommandLine.HelpCommand.class}
)
public class IdeCommand extends BaseCommand implements IDEExecutorDelegateFactory {

    @Override
    public Integer call() throws Exception {
        LocalIDEState.get().setExecutorDelegateFactory(this);
        step.ide.LocalIDE.main(new String[]{});
        System.err.println("TODO SED-4429: PROPER TERMINATION HANDLING; FOR NOW, JUST STOP THE PROCESS");
        Thread.sleep(Long.MAX_VALUE);
        return 0;
    }

    @Override
    public IDEExecutorDelegate createDelegate(File apFolder, ExecutionParameters executionParams) {
        ApExecuteParameters params = new ApExecuteParameters()
            .setAutomationPackageFile(ApCommand.AbstractApCommand.prepareFile(Failable.call(apFolder::getCanonicalPath), "automation package", true))
            .setAutomationPackageMavenArtifact(null)
            .setLibraryFile(null)
            .setlibraryMavenArtifact(null)
            .setManagedLibraryName(null)
            .setStepProjectName(null)
            .setUserId(null)
            .setAuthToken(null)
            .setExecutionParameters(executionParams.getCustomParameters())
            .setExecutionResultTimeoutS(3600)
            .setWaitForExecution(false)
            .setEnsureExecutionSuccess(false)
            .setIncludePlans(executionParams.getDescription()) // NOTE: the include plans is slightly buggy as it splits plan names by commas (",") -- so don't use plan names with a comma.
            .setExcludePlans(null)
            .setIncludeCategories(null)
            .setExcludeCategories(null)
            .setWrapIntoTestSet(false)
            .setNumberOfThreads(null)
            .setReports(null)
            .setReportOutputDir(Failable.call(() -> Files.createTempDirectory("step-ide-execution-").toFile())); // FIXME clean up tmp dir
        String url = "http://localhost:8080";
        return new ExecuteAutomationPackageTool(url, params);
    }
}
