package step.artefacts.handlers;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.bson.types.ObjectId;

import step.artefacts.CallFunction;
import step.core.accessors.AbstractOrganizableObject;
import step.core.execution.ExecutionContext;
import step.core.execution.ExecutionContextBindings;
import step.functions.Function;
import step.functions.accessor.FunctionAccessor;

public class FunctionLocator {
	
	public static final String KEYWORD_ACTIVE_VERSIONS = "keyword.active.versions";
	
	protected ExecutionContext context;
	protected FunctionAccessor functionAccessor;
	private SelectorHelper selectorHelper;
	
	public FunctionLocator(FunctionAccessor functionAccessor, SelectorHelper selectorHelper) {
		this(functionAccessor,selectorHelper, null);
	}

	public FunctionLocator(FunctionAccessor functionAccessor, SelectorHelper selectorHelper, ExecutionContext context) {
		super();
		this.functionAccessor = functionAccessor;
		this.selectorHelper= selectorHelper;
		this.context = context;
	}

	public Function getFunction(CallFunction testArtefact) {
		Function function = null;
		if(testArtefact.getFunctionId()!=null) {
			function = functionAccessor.get(new ObjectId(testArtefact.getFunctionId()));
			if(function == null) {
				throw new RuntimeException("Unable to find keyword with id "+testArtefact.getFunctionId());
			}
		} else {
			String selectionAttributesJson = testArtefact.getFunction().get();
			Map<String, String> attributes = selectorHelper.buildSelectionAttributesMap(selectionAttributesJson, getBindings());
			
			List<Function> matchingFunctions = StreamSupport.stream(functionAccessor.findManyByAttributes(attributes), false).collect(Collectors.toList());
			
			Set<String> activeKeywordVersions = getActiveKeywordVersions();
			if(activeKeywordVersions != null && activeKeywordVersions.size()>0) {
				// First try to find a function matching one of the active versions
				function = matchingFunctions.stream().filter(f->{
					String version = f.getAttributes().get(AbstractOrganizableObject.VERSION);
					return version != null && activeKeywordVersions.contains(version);
				}).findFirst().orElse(null);
				// if no function has been found with one of the active versions, return the first function WITHOUT version
				if(function == null) {
					function = matchingFunctions.stream().filter(f->{
						String version = f.getAttributes().get(AbstractOrganizableObject.VERSION);
						return version == null || version.trim().isEmpty();
					}).findFirst().orElseThrow(()->new RuntimeException("Unable to find keyword with attributes "+selectionAttributesJson+" matching on of the versions: "+activeKeywordVersions));
				}
			} else {
				// No active versions defined. Return the first function
				function = matchingFunctions.stream().findFirst().orElseThrow(()->new RuntimeException("Unable to find keyword with attributes "+selectionAttributesJson));
			}
		}
				
		return function;
	}
	
	private Map<String, Object> getBindings() {
		if (context != null) {
			return ExecutionContextBindings.get(context);
		} else {
			return null;
		}
	}

	private Set<String> getActiveKeywordVersions() {
		Set<String> activeKeywordVersions = null;
		if (context != null) {
			String activeKeywordVersionsStr = context.getVariablesManager().getVariableAsString(KEYWORD_ACTIVE_VERSIONS,null);
			activeKeywordVersions = new HashSet<>();
			if(activeKeywordVersionsStr != null) {
				activeKeywordVersions.addAll(Arrays.asList(activeKeywordVersionsStr.split(",")));
			}
		}
		return activeKeywordVersions;
	}
}
