package step.grid.agent.handler;

import java.lang.reflect.Method;
import java.util.Set;

import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import step.grid.agent.tokenpool.AgentTokenWrapper;
import step.grid.io.InputMessage;
import step.grid.io.OutputMessage;

public class AnnotatedMethodHandler implements MessageHandler {

	static Reflections reflections = new Reflections(new ConfigurationBuilder()
            .setUrls(ClasspathHelper.forPackage("step", Thread.currentThread().getContextClassLoader()))
            .setScanners(new MethodAnnotationsScanner()));
	
	public AnnotatedMethodHandler() {
		super();
	}

	@Override
	public OutputMessage handle(AgentTokenWrapper token, InputMessage message) throws Exception {		
		try {
		Set<Method> jobSubTypes = reflections.getMethodsAnnotatedWith(Function.class);
		for(Method m:jobSubTypes) {
			if(m.getAnnotation(Function.class).name().equals(message.getFunction())) {
				return (OutputMessage) m.invoke(null,token, message);
			}
		}
		} catch (Throwable e) {
			throw e;
		}
		throw new Exception("Unable to find method annoted by '"+Function.class.getName()+"' with name=='"+message.getFunction()+"'");
	}

}
