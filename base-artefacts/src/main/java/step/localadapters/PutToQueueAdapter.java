package step.localadapters;

import java.util.HashMap;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import step.adapters.commons.model.Input;
import step.adapters.commons.model.Output;
import step.adapters.commons.model.OutputBuilder;
import step.artefacts.handlers.teststep.AbstractLocalAdapter;

public class PutToQueueAdapter extends AbstractLocalAdapter {

	@Override
	public Output executeAdapter(Input input) {
		// get QUeueManager from context
		QueueManager queueManager = null;
		
		String queueName = input.getPayload().getDocumentElement().getAttribute("Queue");
		
		HashMap<String, Object> attributes = new HashMap<>();
		NamedNodeMap atrributes = input.getPayload().getDocumentElement().getAttributes();
		for(int i=0;i<atrributes.getLength();i++) {
			Node attNode = atrributes.item(i);
			String key = attNode.getNodeName();
			if(!key.equals("Queue")) {				
				attributes.put(key, attNode.getNodeValue());
			}
		}
		
		queueManager.putToQueue(queueName, attributes);
		
		return new OutputBuilder().build();
	}

}
