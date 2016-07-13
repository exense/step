package step.grid.agent.handler;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TokenHandlerPool {

	private Map<String, TokenHandler> pool = new HashMap<>();
	
	public TokenHandler get(String handlerKey) throws Exception {
		TokenHandler handler = pool.get(handlerKey); 
		
		if(handler==null) {
			handler = createHandler(handlerKey);
			pool.put(handlerKey, handler);
		}

		return handler;
	}

	private TokenHandler createHandler(String handlerKey) throws Exception {
		Matcher m = HANDLER_KEY_PATTERN.matcher(handlerKey);
		if(m.matches()) {
			String factory = m.group(1);
			String factoryKey = m.group(2);
			
			if(factory.equals("class")) {
				try {
					Class<?> class_ = Class.forName(factoryKey);
					Object o = class_.newInstance();
					if(o!=null && o instanceof TokenHandler) {
						return (TokenHandler)o;
					} else {
						throw new RuntimeException("The class '"+factoryKey+"' doesn't extend "+TokenHandler.class);
					}
				} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
					throw e;
				}
			} else {
				throw new RuntimeException("Unknown handler factory: "+factory);
			}
		} else {
			throw new RuntimeException("Invalid handler key: "+handlerKey);
		}
	}
	
	private static final Pattern HANDLER_KEY_PATTERN = Pattern.compile("(.+?):(.+?)");
}
