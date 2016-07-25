package step.grid.agent.conf;

import java.io.File;

import com.fasterxml.jackson.databind.ObjectMapper;

public class AgentConfParser {
	
	ObjectMapper mapper = new ObjectMapper();
	
	public AgentConf parser(File file) throws Exception {
		return mapper.reader(AgentConf.class).readValue(file);
	}

}
