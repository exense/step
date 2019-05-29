package step.commons.processmanager;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import step.commons.processmanager.ManagedProcess.ManagedProcessException;

public class ExternalJVMLauncher {

	private final String javaPath;
	
	public ExternalJVMLauncher(String javaPath) {
		super();
		this.javaPath = javaPath;
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
	
	public ManagedProcess launchExternalJVM(String name, Class<?> mainClass, List<String> vmargs, List<String> progargs) throws ManagedProcessException {
		String cp = buildClasspath();
		
		List<String> cmd = new ArrayList<>();
		cmd.add(javaPath);
		cmd.add("-cp");
		cmd.add(cp);
		
		cmd.addAll(vmargs);
		
		cmd.add(mainClass.getName());
		
		cmd.addAll(progargs);
		
		ManagedProcess process = new ManagedProcess(cmd, name);
		process.start();
		return process;
	}
	public static boolean isWindows() {
        return (System.getProperty("os.name").toLowerCase().indexOf("win") >= 0);
    }
}
