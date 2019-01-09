package step.plugins.functions.types;

import java.io.StringReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.json.Json;

import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

import step.core.GlobalContext;
import step.core.accessors.AbstractCRUDAccessor;
import step.core.plugins.AbstractPlugin;
import step.core.plugins.Plugin;
import step.functions.Function;
import step.functions.accessor.FunctionAccessor;
import step.functions.base.types.LocalFunction;
import step.functions.base.types.LocalFunctionType;
import step.functions.editors.FunctionEditor;
import step.functions.editors.FunctionEditorRegistry;
import step.functions.type.FunctionTypeRegistry;
import step.handlers.javahandler.Keyword;

@Plugin(prio=10)
public class BaseFunctionTypesPlugin extends AbstractPlugin {

	@Override
	public void executionControllerStart(GlobalContext context) throws Exception {
		super.executionControllerStart(context);
		FunctionTypeRegistry functionTypeRegistry = context.get(FunctionTypeRegistry.class);		
		functionTypeRegistry.registerFunctionType(new CompositeFunctionType(context.getArtefactAccessor(), context.getArtefactManager()));
		functionTypeRegistry.registerFunctionType(new LocalFunctionType());

		context.get(FunctionEditorRegistry.class).register(new FunctionEditor() {
			@Override
			public String getEditorPath(Function function) {
				return "/root/artefacteditor/"+((CompositeFunction)function).getArtefactId();
			}

			@Override
			public boolean isValidForFunction(Function function) {
				return function instanceof CompositeFunction;
			}
		});

		setupLocalFunctionsIfNotExisting(context);
	}

	protected void setupLocalFunctionsIfNotExisting(GlobalContext context) {
		@SuppressWarnings("unchecked")
		AbstractCRUDAccessor<Function> functionAccessor = (AbstractCRUDAccessor<Function>) context.get(FunctionAccessor.class);

		List<String> keywordList = null;
		try {
			keywordList = LocalFunctionType.getLocalKeywordList();
		} catch (Exception e) {
			e.printStackTrace();
		}

		if(keywordList != null){
			for(String keyword : keywordList){
				Map<String, String> attributes = new HashMap<>();
				attributes.put("name", keyword);

				Function function = functionAccessor.findByAttributes(attributes);
				if(function == null) {
					function = new LocalFunction();
					function.setAttributes(attributes);
					function.setSchema(Json.createReader(
							new StringReader("{}"))
							.readObject());
					functionAccessor.save(function);
				}
			}
		}

	}

}
