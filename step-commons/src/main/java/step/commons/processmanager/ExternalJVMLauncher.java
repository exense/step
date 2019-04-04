package step.commons.processmanager;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import step.commons.processmanager.ManagedProcess.ManagedProcessException;

public class ExternalJVMLauncher {

	private final String javaPath;
	
	private final File processLogFolder;
	
	public ExternalJVMLauncher(String javaPath, File processLogFolder) {
		super();
		this.javaPath = javaPath;
		this.processLogFolder = processLogFolder;
	}
	
	private String buildClasspath() {
		URL[] urls = ((URLClassLoader)Thread.currentThread().getContextClassLoader()).getURLs();
		StringBuilder cp = new StringBuilder();
		String delimiter = isWindows()?";":":";
		cp.append("\"");
		for(URL url:urls) {
			cp.append(url.getFile()+delimiter);
		}
		cp.append("\"");
		return cp.toString();
	}
	
	public ManagedProcess launchExternalJVM(String name, Class<?> mainClass, String... vmargs) throws ManagedProcessException {
		String cp = buildClasspath();
		
		List<String> cmd = new ArrayList<>();
		cmd.add(javaPath);
		cmd.add("-cp");
		cmd.add(cp);
		
		cmd.addAll(Arrays.asList(vmargs));
		
		cmd.add(mainClass.getName());
		
		ManagedProcess process = new ManagedProcess(processLogFolder, cmd, name);
		process.start();
		return process;
	}
	public static boolean isWindows() {
        return (System.getProperty("os.name").toLowerCase().indexOf("win") >= 0);
    }
}
