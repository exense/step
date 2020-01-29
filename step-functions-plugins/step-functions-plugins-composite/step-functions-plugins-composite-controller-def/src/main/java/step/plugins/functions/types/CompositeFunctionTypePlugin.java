package step.plugins.functions.types;

import step.core.GlobalContext;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;
import step.functions.Function;
import step.functions.editors.FunctionEditor;
import step.functions.editors.FunctionEditorRegistry;
import step.functions.plugin.GridPlugin;
import step.functions.type.FunctionTypeRegistry;

@Plugin(dependencies= {GridPlugin.class})
public class CompositeFunctionTypePlugin extends AbstractControllerPlugin {

	@Override
	public void executionControllerStart(GlobalContext context) throws Exception {
		super.executionControllerStart(context);
		FunctionTypeRegistry functionTypeRegistry = context.get(FunctionTypeRegistry.class);		
		functionTypeRegistry.registerFunctionType(new CompositeFunctionType(context.getPlanAccessor()));

		context.get(FunctionEditorRegistry.class).register(new FunctionEditor() {
			@Override
			public String getEditorPath(Function function) {
				return "/root/plans/editor/"+((CompositeFunction)function).getPlanId();
			}

			@Override
			public boolean isValidForFunction(Function function) {
				return function instanceof CompositeFunction;
			}
		});
	}
}
