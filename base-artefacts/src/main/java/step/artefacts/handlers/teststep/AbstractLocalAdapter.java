package step.artefacts.handlers.teststep;

import java.io.PrintWriter;
import java.io.StringWriter;

import step.adapters.commons.model.Attachment;
import step.adapters.commons.model.AttachmentHelper;
import step.adapters.commons.model.Input;
import step.adapters.commons.model.Output;
import step.adapters.commons.model.OutputBuilder;

public abstract class AbstractLocalAdapter {
		
	public void init() {}
	
	public Output execute(Input input) {
		try {
			Output output = executeAdapter(input);
			return output;
		} catch (Exception e) {
			OutputBuilder output = new OutputBuilder();
			output.setTechnicalError(e.getMessage()!=null?e.getMessage():e.toString());
			
			Attachment attachment = new Attachment();	
			attachment.setName("exception.log");
			StringWriter w = new StringWriter();
			e.printStackTrace(new PrintWriter(w));
			attachment.setHexContent(AttachmentHelper.getHex(w.toString().getBytes()));
			output.addAttachment(attachment);
			return output.build();
		}
		
	}
	
	public abstract Output executeAdapter(Input input) throws Exception;

}
