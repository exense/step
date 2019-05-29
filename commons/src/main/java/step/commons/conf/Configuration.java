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
package step.commons.conf;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO refactor this class
public class Configuration {
	
	private static final Logger logger = LoggerFactory.getLogger(Configuration.class);
	
	private static Configuration INSTANCE = new Configuration();
	
	private File propertyFile;
	
	private Properties properties;
	
	private Map<String, String> placeholders;
	
	public Configuration() {
		super();

		properties = new Properties();
	}

	public Configuration(File propertyFile) {
		this(propertyFile, null);
	}
	
	public Configuration(File propertyFile, Map<String, String> placeholders) {
		super();
		
		this.propertyFile = propertyFile;
		this.placeholders = placeholders;
		
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
			String propertiesContent = new String(Files.readAllBytes(propertyFile.toPath()));
			String resolvedPropertiesContent = replacePlaceholders(propertiesContent);
			properties.load(new StringReader(resolvedPropertiesContent));			
		}
	}
	
	private String replacePlaceholders(String configXml) {
        StringBuffer sb = new StringBuffer();
        Matcher m = Pattern.compile("\\$\\{(.+?)\\}").matcher(configXml);
        while (m.find()) {
            String key = m.group(1);
            if(placeholders!=null) {
            	String replacement = placeholders.get(key);
            	if(replacement == null) {
            		throw new RuntimeException("Missing placeholder '"+key+"'.");
            	} else {
            		m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            	}
            } else {
            	throw new RuntimeException("Unable to replace placeholders. Placeholder map is null. This should never occur.");
            }
        }
        m.appendTail(sb);
        return sb.toString();
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
	
	public Long getPropertyAsLong(String name, Long defaultValue) {
		String prop = properties.getProperty(name);
		if(prop!=null) {
			return Long.parseLong(prop);
		} else {
			return defaultValue;
		}
	}
	
	public boolean getPropertyAsBoolean(String name) {
		return getPropertyAsBoolean(name, false);
	}
	
	public boolean hasProperty(String name) {
		return properties.containsKey(name);
	}
	
	public boolean getPropertyAsBoolean(String name, boolean defaultValue) {
		String prop = properties.getProperty(name);
		if(prop!=null) {
			return Boolean.parseBoolean(prop);
		} else {
			return defaultValue;
		}
	}

	public File getPropertyFile() {
		return propertyFile;
	}

	public Map<String, String> getPlaceholders() {
		return placeholders;
	}

}
