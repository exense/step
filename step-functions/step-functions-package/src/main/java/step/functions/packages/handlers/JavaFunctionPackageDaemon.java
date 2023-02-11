package step.functions.packages.handlers;

import java.io.*;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Set;

import jakarta.json.Json;
import jakarta.json.JsonException;
import jakarta.json.JsonObject;
import jakarta.json.stream.JsonParsingException;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import step.attachments.FileResolver;
import step.core.accessors.AbstractOrganizableObject;
import step.core.plans.Plan;
import step.core.scanner.AnnotationScanner;
import step.functions.Function;
import step.grid.contextbuilder.ApplicationContextBuilder;
import step.grid.contextbuilder.LocalFileApplicationContextFactory;
import step.grid.contextbuilder.LocalFolderApplicationContextFactory;
import step.handlers.javahandler.Keyword;
import step.plans.nl.RootArtefactType;
import step.plans.nl.parser.PlanParser;
import step.plugins.functions.types.CompositeFunctionUtils;
import step.plugins.java.GeneralScriptFunction;
import step.resources.LocalResourceManagerImpl;

public class JavaFunctionPackageDaemon extends FunctionPackageUtils {

	private static final Logger log = LoggerFactory.getLogger(JavaFunctionPackageDaemon.class);
	
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
						// composite function
						log.warn("Unable to process the composite keyword {} linked with plan {}", annotation.name(), annotation.planReference());
						res = null;
					} else {
						String functionName = annotation.name().length() > 0 ? annotation.name() : m.getName();

						GeneralScriptFunction function = new GeneralScriptFunction();
						function.setAttributes(new HashMap<>());
						function.getAttributes().put(AbstractOrganizableObject.NAME, functionName);

						function.getCallTimeout().setValue(annotation.timeout());
						function.setDescription(annotation.description());

						if (packageLibrariesFile != null) {
							function.getLibrariesFile().setValue(parameters.getPackageLibrariesLocation());
						}

						function.getScriptFile().setValue(parameters.getPackageLocation());
						function.getScriptLanguage().setValue("java");

						JsonObject schema;
						String schemaStr = annotation.schema();
						if (schemaStr.length() > 0) {
							try {
								schema = Json.createReader(new StringReader(schemaStr)).readObject();
							} catch (JsonParsingException e) {
								functions.exception = "Parsing error in the schema for keyword '" + m.getName() + "'. The error was: " + e.getMessage();
								functions.functions.clear();
								return functions;
							} catch (JsonException e) {
								functions.exception = "I/O error in the schema for keyword '" + m.getName() + "'. The error was: " + e.getMessage();
								functions.functions.clear();
								return functions;
							} catch (Exception e) {
								functions.exception = "Unknown error in the schema for keyword '" + m.getName() + "'. The error was: " + e.getMessage();
								functions.functions.clear();
								return functions;
							}
						} else {
							schema = Json.createObjectBuilder().build();
						}
						function.setSchema(schema);
						String htmlTemplate = function.getAttributes().remove("htmlTemplate");
						if (htmlTemplate != null && !htmlTemplate.isEmpty()) {
							function.setHtmlTemplate(htmlTemplate);
							function.setUseCustomTemplate(true);
						}
						res = function;
					}
					if (res != null) {
						functions.functions.add(res);
					}
				}
			}
		} catch (Throwable e) {
			functions.exception = e.getClass().getName() + ": " + e.getMessage();
		}
		return functions;
	}

}
