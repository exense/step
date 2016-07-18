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
