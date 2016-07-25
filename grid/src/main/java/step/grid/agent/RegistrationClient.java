package step.grid.agent;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.ClientProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.grid.RegistrationMessage;

public class RegistrationClient {
	
	private final String registrationServer;
	
	private Client client;
	
	private static final Logger logger = LoggerFactory.getLogger(RegistrationClient.class);

	public RegistrationClient(String registrationServer) {
		super();
		this.registrationServer = registrationServer;
		this.client = ClientBuilder.newClient();
		//client.register(JacksonJsonProvider.class);
	}
	
	public void sendRegistrationMessage(RegistrationMessage message) {
		// TODO get from config?
		int connectionTimeout = 3000;
		int callTimeout = 3000;
		
		try {			
			Response r = client.target(registrationServer + "/grid/register").request().property(ClientProperties.READ_TIMEOUT, callTimeout)
					.property(ClientProperties.CONNECT_TIMEOUT, connectionTimeout).post(Entity.entity(message, MediaType.APPLICATION_JSON));
			
			r.readEntity(String.class);
			
		} catch (ProcessingException e) {
			logger.error("An error occurred while registering tokens to " + registrationServer, e);
		}
	}

	public void close() {
		client.close();
	}
}
