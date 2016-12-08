package step.handlers.processhandler;

import javax.json.JsonObject;

import step.commons.processmanager.ManagedProcess;
import step.grid.agent.handler.MessageHandler;
import step.grid.agent.tokenpool.AgentTokenWrapper;
import step.grid.io.InputMessage;
import step.grid.io.OutputMessage;

public class ProcessHandler implements MessageHandler {

	@Override
	public OutputMessage handle(AgentTokenWrapper token, InputMessage message) throws Exception {
		JsonObject argument = message.getArgument();
		if(argument.containsKey("cmd")) {
			String cmd = argument.getString("cmd");
			ManagedProcess process = new ManagedProcess(cmd, "ProcessHandler");
			
			try {
				process.start();
				process.waitFor(message.getCallTimeout()-1000);
			} catch (Exception e) {
				
			} finally {
				process.destroy();
			}
		}
		return null;
	}

}
