package step.handlers.processhandler;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.concurrent.TimeoutException;

import javax.json.JsonObject;

import step.commons.processmanager.ManagedProcess;
import step.grid.agent.handler.MessageHandler;
import step.grid.agent.handler.context.OutputMessageBuilder;
import step.grid.agent.tokenpool.AgentTokenWrapper;
import step.grid.io.Attachment;
import step.grid.io.AttachmentHelper;
import step.grid.io.InputMessage;
import step.grid.io.OutputMessage;

public class ProcessHandler implements MessageHandler {

	@Override
	public OutputMessage handle(AgentTokenWrapper token, InputMessage message) throws Exception {
		
		String maxOutputPayloadSizeStr = message.getProperties().get("processhandler.output.payload.maxsize");
		Long maxOutputPayloadSize = maxOutputPayloadSizeStr!=null?Long.parseLong(maxOutputPayloadSizeStr):1000;
		
		String maxOutputAttachmentSizeStr = message.getProperties().get("processhandler.output.attachment.maxsize");
		Long maxOutputAttachmentSize = maxOutputAttachmentSizeStr!=null?Long.parseLong(maxOutputAttachmentSizeStr):10000000;
		
		OutputMessageBuilder output = new OutputMessageBuilder(); 
		JsonObject argument = message.getArgument();
		if(argument.containsKey("cmd")) {
			String cmd = argument.getString("cmd");
			ManagedProcess process = new ManagedProcess(cmd, "ProcessHandler");
			try {
				process.start();
				process.waitFor(Math.max(0, message.getCallTimeout()-1000));
				
				attachOutput(maxOutputPayloadSize, maxOutputAttachmentSize, output, "stdout", process.getProcessOutputLog());
				attachOutput(maxOutputPayloadSize, maxOutputAttachmentSize, output, "stderr", process.getProcessErrorLog());
			} catch (TimeoutException e) {
				output.setError("Timeout while waiting for process termination.");
			} catch (Exception e) {
				output.addAttachment(AttachmentHelper.generateAttachmentForException(e));
				output.setError("Error while running process");
			} finally {
				process.destroy();
			}
		} else {
			output.setError("Missing argument 'cmd'");
		}
		return output.build();
	}

	public void attachOutput(Long maxOutputPayloadSize, Long maxOutputAttachmentSize, OutputMessageBuilder output,
			String outputName, File file) throws IOException {
		StringBuilder processOutput = new StringBuilder();
		if(file.length()<maxOutputPayloadSize) {
			Files.readAllLines(file.toPath(), Charset.defaultCharset()).forEach(l->processOutput.append(l).append("\n"));
			output.add(outputName, processOutput.toString());					
		} else {
			if(file.length()<maxOutputAttachmentSize) {
				byte[] bytes = Files.readAllBytes(file.toPath());
				Attachment attachment = AttachmentHelper.generateAttachmentFromByteArray(bytes, outputName+".log");
				output.addAttachment(attachment);		
				output.appendError(outputName + " size exceeded. "+outputName+" has been attached.");
			} else {
				output.appendError(outputName + " size exceeded. "+outputName+" couldn't be attached.");
			}
		}
	}

}
