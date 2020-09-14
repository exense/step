package step.plugins.java.handler;

import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.Set;

import javax.json.JsonObject;

import step.core.scanner.AnnotationScanner;
import step.functions.handler.JsonBasedFunctionHandler;
import step.functions.io.Input;
import step.functions.io.Output;
import step.grid.contextbuilder.ApplicationContextBuilder.ApplicationContext;
import step.handlers.javahandler.Keyword;
import step.handlers.javahandler.KeywordExecutor;
import step.plugins.js223.handler.ScriptHandler;

public class JavaJarHandler extends JsonBasedFunctionHandler {
	
	private static final String KW_CLASSNAMES_KEY = "kwClassnames";

	@Override
	public Output<JsonObject> handle(Input<JsonObject> input) throws Exception {
		//message.getProperties().put("keywordRootPath", fileManagerClient.getDataFolderPath() + "\\"+ currentkeywordVersion.getFileId() + "\\" + currentkeywordVersion.getVersion());
		
		pushRemoteApplicationContext(FORKED_BRANCH, ScriptHandler.SCRIPT_FILE, input.getProperties());
		
		ApplicationContext context = getCurrentContext(FORKED_BRANCH);

		String kwClassnames = (String) context.computeIfAbsent(KW_CLASSNAMES_KEY, k->{
			try {
				return getKeywordClassList((URLClassLoader) getCurrentContext(FORKED_BRANCH).getClassLoader());
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
		input.getProperties().put(KeywordExecutor.KEYWORD_CLASSES, kwClassnames);
		
		// Using the forked to branch in order no to have the ClassLoader of java-plugin-handler.jar as parent.
		// the project java-plugin-handler.jar has many dependencies that might conflict with the dependencies of the 
		// keyword. One of these dependencies is guava for example.
		return delegate(FORKED_BRANCH, "step.plugins.java.handler.KeywordHandler", input);
	}
	
	private String getKeywordClassList(URLClassLoader cl) throws Exception {

		try {
//			Reflections reflections = new Reflections(new ConfigurationBuilder().setUrls(url)
//					.addClassLoader(cl).setScanners(new MethodAnnotationsScanner()));
//			Set<Method> methods = reflections.getMethodsAnnotatedWith(Keyword.class);
			Set<Method> methods = AnnotationScanner.getMethodsWithAnnotation(Keyword.class,cl);

			Set<String> kwClasses = new HashSet<>();
			for(Method method:methods) {
				kwClasses.add(method.getDeclaringClass().getName());
			}
			StringBuilder kwClassnamesBuilder = new StringBuilder();
			kwClasses.forEach(kwClassname->kwClassnamesBuilder.append(kwClassname+KeywordExecutor.KEYWORD_CLASSES_DELIMITER));
			return kwClassnamesBuilder.toString();
		} catch (Exception e) {
			String errorMsg = "Error while looking for methods annotated with @Keyword in "+cl.getURLs();
			throw new Exception(errorMsg, e);
		}
	}
}
