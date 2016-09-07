package step.grid.agent.tokenpool;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TokenSession {
	
	protected static final Logger logger = LoggerFactory.getLogger(TokenSession.class);

	Map<String, Object> attributes = new HashMap<>();

	public Object get(Object arg0) {
		return attributes.get(arg0);
	}

	public Object put(String arg0, Object arg1) {
		Object previous = get(arg0);
		if(previous!=null && previous instanceof Closeable) {
			logger.debug("Attempted to replace session object with key '"+arg0+"'. Closing previous object.");
			try {
				((Closeable)previous).close();
			} catch (IOException e) {
				logger.error("Error while closing '"+arg0+"' from session.",e);
			}
		}
		return attributes.put(arg0, arg1);
	}
}
