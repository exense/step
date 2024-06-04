package step.functions.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class DockerMessageHandlerInput {

    public DockerRegistry dockerRegistry;
    public String dockerImage;
    public String containerUser;
    public String containerCmd;

    public String writeAsString() throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(this);
    }

    public static DockerMessageHandlerInput read(String string) throws JsonProcessingException {
        return new ObjectMapper().readValue(string, DockerMessageHandlerInput.class);
    }
}
