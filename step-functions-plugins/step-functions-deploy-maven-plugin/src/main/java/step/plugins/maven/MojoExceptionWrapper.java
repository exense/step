package step.plugins.maven;

import org.apache.maven.plugin.MojoExecutionException;

public class MojoExceptionWrapper extends RuntimeException {

	public MojoExceptionWrapper(String message, MojoExecutionException cause) {
		super(message, cause);
	}

	@Override
	public synchronized MojoExecutionException getCause() {
		return (MojoExecutionException) super.getCause();
	}
}
