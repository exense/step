package step.localadapters;

import java.util.Map;

import step.adapters.commons.model.AdapterMessageBuilder;
import step.adapters.commons.model.Input;
import step.adapters.commons.model.Output;
import step.adapters.commons.model.OutputBuilder;
import step.artefacts.handlers.teststep.AbstractLocalAdapter;
import step.expressions.XmlToParameterMapTranslator;

public class echoAdapter extends AbstractLocalAdapter {

	@Override
	public Output executeAdapter(Input input) throws Exception {
		
		AdapterMessageBuilder<Output> outputBuilder = new OutputBuilder();
		StringBuilder result = new StringBuilder();
		
		XmlToParameterMapTranslator translator = new XmlToParameterMapTranslator();
		Map<String, String> inputParams = translator.getParameterMap(input.getPayload());
		
		for(String param:inputParams.keySet()) {
			result.append(param).append("=\"").append(inputParams.get(param)).append("\" ");
		}
		
		outputBuilder.setPayload("<Return Meldung=\"erfolgreich\" " + result + "/>");
		return outputBuilder.build();
	}

}
