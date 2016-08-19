package step.commons.conf;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO refactor this class
public class Configuration {
	
	private static final Logger logger = LoggerFactory.getLogger(Configuration.class);
	
	private static Configuration INSTANCE = new Configuration();
	
	private File propertyFile;
	
	private Properties properties;
	
	public Configuration() {
		super();

		properties = new Properties();
	}

	public Configuration(File propertyFile) {
		super();
		
		this.propertyFile = propertyFile;

		try {
			load();			
		} catch (Exception e) {
			logger.error("Error while loading configuration.", e);
		}


		if(getPropertyAsBoolean("conf.scan", false)) {
			FileWatchService.getInstance().register(propertyFile, new Runnable() {
				@Override
				public void run() {
					try {
						load();
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
				}
			});
		}
	}
	
	public void load() throws FileNotFoundException, IOException {
		properties = new Properties();
		if(propertyFile!=null) {
			properties.load(new FileReader(propertyFile));			
		}
	}
	
	public static void setInstance(Configuration instance) {
		INSTANCE = instance;
	}
	
	public static Configuration getInstance() {
		return INSTANCE;
	}

	public Object put(Object arg0, Object arg1) {
		return properties.put(arg0, arg1);
	}

	public String getProperty(String name) {
		return properties.getProperty(name);
	}
	
	public String getProperty(String name, String defaultValue) {
		return properties.getProperty(name, defaultValue);
	}
	
	public void putProperty(String name, String value) {
		properties.put(name, value);
	}
	
	public Integer getPropertyAsInteger(String name) {
		return getPropertyAsInteger(name, null);
	}
	
	public Integer getPropertyAsInteger(String name, Integer defaultValue) {
		String prop = properties.getProperty(name);
		if(prop!=null) {
			return Integer.parseInt(prop);
		} else {
			return defaultValue;
		}
	}
	
	public boolean getPropertyAsBoolean(String name) {
		return getPropertyAsBoolean(name, false);
	}
	
	public boolean getPropertyAsBoolean(String name, boolean defaultValue) {
		String prop = properties.getProperty(name);
		if(prop!=null) {
			return Boolean.parseBoolean(prop);
		} else {
			return defaultValue;
		}
	}

}
