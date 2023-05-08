package step.functions.packages.handlers;

import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Set;

import com.fasterxml.jackson.databind.ObjectMapper;

import step.attachments.FileResolver;
import step.core.accessors.AbstractOrganizableObject;
import step.core.scanner.AnnotationScanner;
import step.functions.Function;
import step.grid.contextbuilder.ApplicationContextBuilder;
import step.grid.contextbuilder.LocalFileApplicationContextFactory;
import step.grid.contextbuilder.LocalFolderApplicationContextFactory;
import step.handlers.javahandler.Keyword;
import step.handlers.javahandler.jsonschema.JsonSchemaPreparationException;
import step.handlers.javahandler.jsonschema.KeywordJsonSchemaCreator;
import step.plans.nl.parser.PlanParser;
import step.plugins.functions.types.CompositeFunctionUtils;
import step.plugins.java.GeneralScriptFunction;
import step.resources.LocalResourceManagerImpl;

public class JavaFunctionPackageDaemon extends FunctionPackageUtils {

	private final KeywordJsonSchemaCreator schemaCreator = new KeywordJsonSchemaCreator();

	public JavaFunctionPackageDaemon() {
		super(new FileResolver(new LocalResourceManagerImpl()));
	}

	public static void main(String[] args) throws Exception {
		ObjectMapper objectMapper = new ObjectMapperResolver().getContext(FunctionList.class);
		
		try {
			JavaFunctionPackageDaemon daemon = new JavaFunctionPackageDaemon();
		
			DiscovererParameters parameter;
			try (InputStreamReader reader = new InputStreamReader(System.in)) {
				parameter = objectMapper.readValue(reader, DiscovererParameters.class);
			}
			FunctionList list = daemon.getFunctions(parameter);
			
			try (OutputStreamWriter writer = new OutputStreamWriter(System.out)) {
				writer.write(READY_STRING+"\n");
				writer.write(objectMapper.writeValueAsString(list));
			}
		} catch (Exception e) {
			FunctionList list = new FunctionList();
			list.exception = e.getMessage();
			try (OutputStreamWriter writer = new OutputStreamWriter(System.out)) {
				writer.write(READY_STRING);
				writer.write(objectMapper.writeValueAsString(list));
			}
		}
	}
	
	protected FunctionList getFunctions(DiscovererParameters parameters) {
		FunctionList functions = new FunctionList();
		try {
			File packageLibrariesFile = resolveFile(parameters.getPackageLibrariesLocation());
			File packageFile = resolveMandatoryFile(parameters.getPackageLocation());

			// Build classloader
			ApplicationContextBuilder applicationContextBuilder = new ApplicationContextBuilder(ClassLoader.getSystemClassLoader());
			if(packageLibrariesFile != null) {
				applicationContextBuilder.pushContext(new LocalFolderApplicationContextFactory(packageLibrariesFile));
			}
			applicationContextBuilder.pushContext(new LocalFileApplicationContextFactory(packageFile));
			ClassLoader cl = applicationContextBuilder.getCurrentContext().getClassLoader();

			// Scan package File for Keyword annotations
			try(AnnotationScanner annotationScanner = AnnotationScanner.forSpecificJar(packageFile,cl) ){
				Set<Method> methods = annotationScanner.getMethodsWithAnnotation(Keyword.class);
				for(Method m:methods) {
					Keyword annotation = m.getAnnotation(Keyword.class);
					Function res;
					if(annotation.planReference() != null && !annotation.planReference().isBlank()){
						res = CompositeFunctionUtils.createCompositeFunction(annotation, m, new PlanParser().parseCompositePlanFromPlanReference(m, annotation.planReference()));
					} else {
						String functionName = annotation.name().length() > 0 ? annotation.name() : m.getName();

						GeneralScriptFunction function = new GeneralScriptFunction();
						function.setAttributes(new HashMap<>());
						function.getAttributes().put(AbstractOrganizableObject.NAME, functionName);

						function.getCallTimeout().setValue(annotation.timeout());

						if (packageLibrariesFile != null) {
							function.getLibrariesFile().setValue(parameters.getPackageLibrariesLocation());
						}

						function.getScriptFile().setValue(parameters.getPackageLocation());
						function.getScriptLanguage().setValue("java");
						res = function;
					}
					res.setDescription(annotation.description());
					try {
						res.setSchema(schemaCreator.createJsonSchemaForKeyword(m));
					} catch (JsonSchemaPreparationException ex){
						functions.exception = ex.getMessage();
						functions.functions.clear();
						return functions;
					}

					String htmlTemplate = res.getAttributes().remove("htmlTemplate");
					if (htmlTemplate != null && !htmlTemplate.isEmpty()) {
						res.setHtmlTemplate(htmlTemplate);
						res.setUseCustomTemplate(true);
					}

					functions.functions.add(res);
				}
			}
		} catch (Throwable e) {
			functions.exception = e.getClass().getName() + ": " + e.getMessage();
		}
		return functions;
	}

}
