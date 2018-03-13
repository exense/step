package step.plugins.node;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import step.functions.type.AbstractFunctionType;
import step.functions.type.SetupFunctionException;
import step.grid.agent.Agent;
import step.grid.tokenpool.Interest;

public class NodeFunctionType extends AbstractFunctionType<NodeFunction> {

	public static final String FILE = "$node.js.file";
	
	@Override
	public void init() {
		super.init();
	}

	@Override
	public String getHandlerChain(NodeFunction function) {
		return "";
	}

	@Override
	public Map<String, String> getHandlerProperties(NodeFunction function) {
		Map<String, String> props = new HashMap<>();
		registerFile(function.getJsFile(), FILE, props);
		return props;
	}

	@Override
	public Map<String, Interest> getTokenSelectionCriteria(NodeFunction function) {
		Map<String, Interest> criteria = new HashMap<>();
		criteria.put(Agent.AGENT_TYPE_KEY, new Interest(Pattern.compile("node"), true));
		return criteria;
	}

	@Override
	public NodeFunction newFunction() {
		return new NodeFunction();
	}

	@Override
	public void setupFunction(NodeFunction function) throws SetupFunctionException {
		super.setupFunction(function);
	}

}
