package step.plugins.functions.types;

import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.json.Json;

import step.core.GlobalContext;
import step.core.accessors.AbstractCRUDAccessor;
import step.core.plugins.AbstractPlugin;
import step.core.plugins.Plugin;
import step.functions.Function;
import step.functions.accessor.FunctionAccessor;
import step.functions.base.types.LocalFunction;
import step.functions.base.types.LocalFunctionType;
import step.functions.base.types.handler.BaseFunctionReflectionHelper;
import step.functions.plugin.GridPlugin;
import step.functions.type.FunctionTypeRegistry;

@Plugin(dependencies= {GridPlugin.class})
public class BaseFunctionTypesPlugin extends AbstractPlugin {

	@Override
	public void executionControllerStart(GlobalContext context) throws Exception {
		super.executionControllerStart(context);
		FunctionTypeRegistry functionTypeRegistry = context.get(FunctionTypeRegistry.class);		
		functionTypeRegistry.registerFunctionType(new LocalFunctionType());

		setupLocalFunctionsIfNotExisting(context);
	}

	protected void setupLocalFunctionsIfNotExisting(GlobalContext context) {
		@SuppressWarnings("unchecked")
		AbstractCRUDAccessor<Function> functionAccessor = (AbstractCRUDAccessor<Function>) context.get(FunctionAccessor.class);

		List<String> keywordList = null;
		try {
			keywordList = BaseFunctionReflectionHelper.getLocalKeywordList();
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
					try {
						function.setSchema(Json.createReader(
								new StringReader(BaseFunctionReflectionHelper.getLocalKeywordsWithSchemas().get(keyword)))
								.readObject());
					} catch (Exception e) {
						function.setSchema(Json.createReader(
								new StringReader("{}"))
								.readObject());
					}
					functionAccessor.save(function);
				}
			}
		}

	}

}
