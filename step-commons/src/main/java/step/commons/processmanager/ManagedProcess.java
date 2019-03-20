/*******************************************************************************
 * (C) Copyright 2016 Jerome Comte and Dorian Cransac
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
 *******************************************************************************/
package step.commons.processmanager;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.commons.conf.Configuration;
import step.commons.helpers.FileHelper;

public class ManagedProcess implements Closeable {

	private static final Logger logger = LoggerFactory.getLogger(ManagedProcess.class);
	
	private final ProcessBuilder builder;
		
	private static File logFolder;
	
	private File processOutputLog;
	
	private File processErrorLog;
	
	private final String id;
	
	private Process process;
	
	private File executionDirectory;
	
	public ManagedProcess(String command, String name) throws ManagedProcessException {
		this(tokenize(command), name);
	}
	
	public ManagedProcess(String name, String... command) throws ManagedProcessException {
		this(Arrays.asList(command), name);
	}
	
	public ManagedProcess(List<String> commands, String name) throws ManagedProcessException {
		this(null, commands, name);
	}
	
	public ManagedProcess(File executionDirectory, List<String> commands, String name) throws ManagedProcessException {
		super();

		String logdir = Configuration.getInstance().getProperty("managedprocesses.logdir");
		logFolder = new File(logdir!=null?logdir:".");
		
		UUID uuid = UUID.randomUUID();
		this.id = name+"_"+uuid;
		builder = new ProcessBuilder(commands);
		
		if(executionDirectory==null) {
			executionDirectory = new File(logFolder+"/" + id);
			if(!executionDirectory.exists()) {
				if(!executionDirectory.mkdir()) {
					throw new InvalidParameterException("Unable to create log folder for process " + id + ". Please ensure that the folder specified by the parameter adapters.log in the configuration repository exists and is writable.");
				}
			}	
		}
		this.executionDirectory = executionDirectory;
	}
	
	public static File getLogFolder() {
		return logFolder;
	}

	public File getProcessOutputLog() {
		return processOutputLog;
	}

	public File getProcessErrorLog() {
		return processErrorLog;
	}

	public File getExecutionDirectory() {
		return executionDirectory;
	}

	public void setExecutionDirectory(File executionDirectory) {
		this.executionDirectory = executionDirectory;
	}

	private static List<String> tokenize(String command) {
		List<String> tokens = new ArrayList<String>();
		Pattern regex = Pattern.compile("[^\\s\"]+|\"([^\"]*)\"");
		Matcher regexMatcher = regex.matcher(command);
		while (regexMatcher.find()) {
		    if (regexMatcher.group(1) != null) {
		        tokens.add(regexMatcher.group(1));
		    }  else {
		        tokens.add(regexMatcher.group());
		    }
		} 
		return tokens;
	}
	
	public void start() throws ManagedProcessException {
		synchronized (this) {
			if(process==null) {
				logger.debug("Starting managed process " + builder.command());
				builder.directory(executionDirectory);
				processOutputLog = new File(executionDirectory+"/ProcessOut.log");
				builder.redirectOutput(processOutputLog);
				processErrorLog = new File(executionDirectory+"/ProcessError.log");
				builder.redirectError(processErrorLog);
				try {
					process = builder.start();
				} catch (IOException e) {
					throw new ManagedProcessException("Unable to start the process " + id, e);
				}
				logger.debug("Started managed process " + builder.command());
			} else {
				throw new ManagedProcessException("Unable to the process " + id + ". Process already runnning.");
			}
		}
	}
	
	public void waitFor(long timeout) throws TimeoutException, InterruptedException, ExecutionException {	
		
		ExecutorService executor = Executors.newSingleThreadExecutor();
		Future<Integer> ft = executor.submit(new Callable<Integer>() {
			public Integer call() throws Exception {
				process.waitFor();
				return process.exitValue();
			}
		});
		executor.shutdown();
			
		ft.get(timeout, TimeUnit.MILLISECONDS);
	}
	
	public void destroy() {
		if(process!=null) {
			process.destroy();
			try {
				process.waitFor();
			} catch (InterruptedException e) {}
		}
		FileHelper.deleteFolder(executionDirectory);
	}
	
	public class ManagedProcessException extends Exception {

		private static final long serialVersionUID = -2205566982535606557L;

		public ManagedProcessException(String message, Throwable cause) {
			super(message, cause);
		}

		public ManagedProcessException(String message) {
			super(message);
		}
		
	}

	@Override
	public void close() throws IOException {
		destroy();
	}

}
