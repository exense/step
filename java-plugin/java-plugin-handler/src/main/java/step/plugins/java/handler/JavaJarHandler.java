package step.plugins.java.handler;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.Set;

import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.util.ConfigurationBuilder;

import step.grid.agent.AgentTokenServices;
import step.grid.agent.handler.AbstractMessageHandler;
import step.grid.agent.handler.MessageHandlerPool;
import step.grid.agent.tokenpool.AgentTokenWrapper;
import step.grid.contextbuilder.ApplicationContextBuilder;
import step.grid.contextbuilder.RemoteApplicationContextFactory;
import step.grid.contextbuilder.ApplicationContextBuilder.ApplicationContext;
import step.grid.io.InputMessage;
import step.grid.io.OutputMessage;
import step.handlers.javahandler.Keyword;
import step.handlers.javahandler.KeywordHandler;
import step.plugins.js223.handler.ScriptHandler;

public class JavaJarHandler extends AbstractMessageHandler {
	
	private ApplicationContextBuilder appContextBuilder;
		
	private MessageHandlerPool messageHandlerPool;
				
	@Override
	public void init(AgentTokenServices agentTokenServices) {
		super.init(agentTokenServices);
		appContextBuilder = agentTokenServices.getApplicationContextBuilder();	
		messageHandlerPool = new MessageHandlerPool(agentTokenServices);
	}
	
	@Override
	public OutputMessage handle(AgentTokenWrapper token, final InputMessage message) throws Exception {
		RemoteApplicationContextFactory scriptJarContext = new RemoteApplicationContextFactory(token.getServices().getFileManagerClient(), getFileVersionId(ScriptHandler.SCRIPT_FILE, message.getProperties()));
		appContextBuilder.pushContext(scriptJarContext);
		
		ApplicationContext context = agentTokenServices.getApplicationContextBuilder().getCurrentContext();

		String kwClassnames = (String) context.get("kwClassnames");
		if (kwClassnames == null) {
			kwClassnames = getKeywordClassList((URLClassLoader) context.getClassLoader());
			context.put("kwClassnames", kwClassnames);
		}
		message.getProperties().put(KeywordHandler.KEYWORD_CLASSES, kwClassnames);
		
		return messageHandlerPool.get(KeywordHandler.class.getName(), appContextBuilder.getCurrentContext().getClassLoader()).handle(token, message);	
	}

	private String getKeywordClassList(URLClassLoader cl) throws Exception {
		URL url = cl.getURLs()[0];
		try {
			Reflections reflections = new Reflections(new ConfigurationBuilder().setUrls(url)
					.addClassLoader(cl).setScanners(new MethodAnnotationsScanner()));
			Set<Method> methods = reflections.getMethodsAnnotatedWith(Keyword.class);
			Set<String> kwClasses = new HashSet<>();
			for(Method method:methods) {
				kwClasses.add(method.getDeclaringClass().getName());
			}
			StringBuilder kwClassnamesBuilder = new StringBuilder();
			kwClasses.forEach(kwClassname->kwClassnamesBuilder.append(kwClassname+";"));
			return kwClassnamesBuilder.toString();
		} catch (Exception e) {
			String errorMsg = "Error while looking for methods annotated with @Keyword in "+url.toString();
			throw new Exception(errorMsg, e);
		}
	}
}
