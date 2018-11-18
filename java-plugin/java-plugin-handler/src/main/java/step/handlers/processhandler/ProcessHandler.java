package step.handlers.processhandler;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.concurrent.TimeoutException;

import javax.json.JsonObject;

import step.commons.processmanager.ManagedProcess;
import step.functions.handler.AbstractFunctionHandler;
import step.functions.io.Input;
import step.functions.io.Output;
import step.functions.io.OutputBuilder;
import step.grid.io.Attachment;
import step.grid.io.AttachmentHelper;

public class ProcessHandler extends AbstractFunctionHandler {

	@Override
	public Output<?> handle(Input<?> input) throws Exception {
		
		String maxOutputPayloadSizeStr = input.getProperties().get("processhandler.output.payload.maxsize");
		Long maxOutputPayloadSize = maxOutputPayloadSizeStr!=null?Long.parseLong(maxOutputPayloadSizeStr):1000;
		
		String maxOutputAttachmentSizeStr = input.getProperties().get("processhandler.output.attachment.maxsize");
		Long maxOutputAttachmentSize = maxOutputAttachmentSizeStr!=null?Long.parseLong(maxOutputAttachmentSizeStr):10000000;
		
		OutputBuilder output = new OutputBuilder(); 
		JsonObject argument = (JsonObject) input.getPayload();
		if(argument.containsKey("cmd")) {
			String cmd = argument.getString("cmd");
			
			try (ManagedProcess process = new ManagedProcess(cmd, "ProcessHandler")) {
				process.start();
				process.waitFor(input.getFunctionCallTimeout());
				
				attachOutput(maxOutputPayloadSize, maxOutputAttachmentSize, output, "stdout", process.getProcessOutputLog());
				attachOutput(maxOutputPayloadSize, maxOutputAttachmentSize, output, "stderr", process.getProcessErrorLog());
			} catch (TimeoutException e) {
				output.setError("Timeout while waiting for process termination.");
			} catch (Exception e) {
				output.addAttachment(AttachmentHelper.generateAttachmentForException(e));
				output.setError("Error while running process");
			}
		} else {
			output.setError("Missing argument 'cmd'");
		}
		return output.build();
	}

	public void attachOutput(Long maxOutputPayloadSize, Long maxOutputAttachmentSize, OutputBuilder output,
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
