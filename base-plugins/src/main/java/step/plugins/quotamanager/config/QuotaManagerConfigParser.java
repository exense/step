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
package step.plugins.quotamanager.config;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;

import com.thoughtworks.xstream.XStream;

public class QuotaManagerConfigParser {
	
	public static QuotaManagerConfig parseConfig(File file) {
		XStream xstream = getXStream();
		QuotaManagerConfig config = (QuotaManagerConfig) xstream.fromXML(file);
		return config;
	}
	
	public static QuotaManagerConfig parseConfig(String resourceName) throws FileNotFoundException {
		URL url = QuotaManagerConfigParser.class.getResource(resourceName);
		if(url==null) {
			throw new FileNotFoundException("Unable to find the configuration file for the QuotaManager: " + resourceName);
		}
		File file = new File(url.getFile());
		return parseConfig(file);
	}
	
	public static void saveConfig(QuotaManagerConfig config, File file) throws IOException {
		getXStream().toXML(config, new FileWriter(file));
	}
	
	private static XStream getXStream() {
		XStream xstream = new XStream();
		xstream.alias("QuotaManagerConfig", QuotaManagerConfig.class);
		xstream.alias("Quota", Quota.class);
		return xstream;
	}
}
