package step.grid.agent;

import java.net.SocketTimeoutException;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.client.ClientProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

import step.grid.RegistrationMessage;
import step.grid.Token;

public class RegistrationClient {
	
	private final String registrationServer;
	
	private Client client;
	
	private static final Logger logger = LoggerFactory.getLogger(RegistrationClient.class);

	public RegistrationClient(String registrationServer) {
		super();
		this.registrationServer = registrationServer;
		this.client = ClientBuilder.newClient();
		client.register(JacksonJsonProvider.class);
	}
	
	public void sendRegistrationMessage(RegistrationMessage message) {
		// TODO get from config?
		int connectionTimeout = 3000;
		int callTimeout = 3000;
		
		try {			
			client.target(registrationServer + "/grid/register").request().property(ClientProperties.READ_TIMEOUT, callTimeout)
					.property(ClientProperties.CONNECT_TIMEOUT, connectionTimeout).post(Entity.entity(message, MediaType.APPLICATION_JSON));
		} catch (ProcessingException e) {
			String additionalInfos = "";

			if(e.getCause() != null && e.getCause() instanceof SocketTimeoutException) {
				logger.error("Timeout occurred while registering tokens to " + registrationServer + additionalInfos);
			} else if(e.getCause() != null && e.getCause() instanceof IllegalStateException && e.getCause().getMessage().equals("Already connected")) {
				// this case is a workaround because of a bug in jersey v2.13 (JERSEY-2728-https://java.net/jira/browse/JERSEY-2729). Should be removed later 
				logger.error("An error occurred while registering tokens to " + registrationServer + additionalInfos + ". Server may be unreachable.");
			} else {
				logger.error("An error occurred while registering tokens to " + registrationServer + additionalInfos, e);
			}
		}
	}
	
	public void registerToken(Token token) {
		// TODO get from config?
		int connectionTimeout = 3000;
		int callTimeout = 3000;
		
		try {			
			client.target(registrationServer + "/token").request().property(ClientProperties.READ_TIMEOUT, callTimeout)
					.property(ClientProperties.CONNECT_TIMEOUT, connectionTimeout).post(Entity.entity(token, MediaType.APPLICATION_JSON));
		} catch (ProcessingException e) {
			String additionalInfos = "";

			if(e.getCause() != null && e.getCause() instanceof SocketTimeoutException) {
				logger.error("Timeout occurred while registering tokens to " + registrationServer + additionalInfos);
			} else if(e.getCause() != null && e.getCause() instanceof IllegalStateException && e.getCause().getMessage().equals("Already connected")) {
				// this case is a workaround because of a bug in jersey v2.13 (JERSEY-2728-https://java.net/jira/browse/JERSEY-2729). Should be removed later 
				logger.error("An error occurred while registering tokens to " + registrationServer + additionalInfos + ". Server may be unreachable.");
			} else {
				logger.error("An error occurred while registering tokens to " + registrationServer + additionalInfos, e);
			}
		}
	}

	public void unregisterToken(Token token) {
		
		// TODO get from config?
		int connectionTimeout = 3000;
		int callTimeout = 3000;
		
		try {
			client.target(registrationServer + "/token/"+token.getUid()).request().property(ClientProperties.READ_TIMEOUT, callTimeout)
					.property(ClientProperties.CONNECT_TIMEOUT, connectionTimeout).delete();
		} catch (ProcessingException e) {
			if(e.getCause() != null && e.getCause() instanceof SocketTimeoutException) {
				logger.error("Timeout occurred while unregistering tokens from " + registrationServer);

			} else if(e.getCause() != null && e.getCause() instanceof IllegalStateException && e.getCause().getMessage().equals("Already connected")) {
				// this case is a workaround because of a bug in jersey v2.13 (JERSEY-2728-https://java.net/jira/browse/JERSEY-2729). Should be removed later 
				logger.error("An error occurred while unregistering tokens from " + registrationServer+ ". Server may be unreachable.");
			} else {
				logger.error("An error occurred while unregistering tokens from " + registrationServer, e);
			}
		}
	}

	public void close() {
		client.close();
	}
}
