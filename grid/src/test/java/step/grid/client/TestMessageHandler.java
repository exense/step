package step.grid.client;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import step.grid.agent.handler.AbstractMessageHandler;
import step.grid.agent.handler.context.OutputMessageBuilder;
import step.grid.agent.tokenpool.AgentTokenWrapper;
import step.grid.io.InputMessage;
import step.grid.io.OutputMessage;

public class TestMessageHandler extends AbstractMessageHandler {
	
	@Override
	public OutputMessage handle(AgentTokenWrapper token, InputMessage message) throws Exception {
		
		if(message.getArgument().containsKey("file")) {
			try {
				File file = token.getServices().getFileManagerClient().requestFile(message.getArgument().getString("file"), message.getArgument().getInt("fileVersion"));
				OutputMessageBuilder builder = new OutputMessageBuilder();
				builder.add("content", Files.readAllLines(file.toPath()).get(0));
				return builder.build();
			
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
		
		token.getTokenReservationSession().put("myObject", new Closeable() {
			
			@Override
			public void close() throws IOException {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {

				}
			}
		});
		
		try {
			Thread.sleep(10);
		} catch (InterruptedException e) {

		}
		
		return null;
	}

}
