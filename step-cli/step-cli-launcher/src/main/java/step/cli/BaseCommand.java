package step.cli;

import picocli.CommandLine;

import java.util.concurrent.Callable;

@CommandLine.Command
public abstract class BaseCommand implements Callable<Integer> {

    // This is injected by picocli, and provides information about the current command being invoked.
    // Useful for providing a default implementation printing usage information, as is done below.
    @CommandLine.Spec
    protected CommandLine.Model.CommandSpec spec;

    @Override
    public Integer call() throws Exception {
        // By default, each command simply prints its own usage instructions (same as "help")
        spec.commandLine().usage(spec.commandLine().getOut());
        return 0;
    }
}
