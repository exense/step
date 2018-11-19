package step.plugins.java.handler;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.Set;

import javax.json.JsonObject;

import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.util.ConfigurationBuilder;

import step.functions.handler.JsonBasedFunctionHandler;
import step.functions.io.Input;
import step.functions.io.Output;
import step.grid.contextbuilder.ApplicationContextBuilder.ApplicationContext;
import step.handlers.javahandler.Keyword;
import step.handlers.javahandler.KeywordHandler;
import step.plugins.js223.handler.ScriptHandler;

public class JavaJarHandler extends JsonBasedFunctionHandler {
	
	@Override
	public Output<JsonObject> handle(Input<JsonObject> input) throws Exception {
		//message.getProperties().put("keywordRootPath", fileManagerClient.getDataFolderPath() + "\\"+ currentkeywordVersion.getFileId() + "\\" + currentkeywordVersion.getVersion());
		
		pushRemoteApplicationContext(ScriptHandler.SCRIPT_FILE, input.getProperties());
		
		ApplicationContext context = getCurrentContext();

		String kwClassnames = (String) context.get("kwClassnames");
		if (kwClassnames == null) {
			kwClassnames = getKeywordClassList((URLClassLoader) context.getClassLoader());
			context.put("kwClassnames", kwClassnames);
		}
		input.getProperties().put(KeywordHandler.KEYWORD_CLASSES, kwClassnames);
		
		return delegate(KeywordHandler.class, input);
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
