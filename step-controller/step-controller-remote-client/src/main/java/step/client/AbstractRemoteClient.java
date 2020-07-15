package step.client;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import java.util.function.Supplier;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.commons.auth.Credentials;
import step.client.credentials.ControllerCredentials;
import step.client.credentials.SyspropCredendialsBuilder;
import step.core.deployment.JacksonMapperProvider;

public class AbstractRemoteClient implements Closeable {

	private static final Logger logger = LoggerFactory.getLogger(AbstractRemoteClient.class);
	
	protected Client client;

	protected Map<String, NewCookie> cookies;
	
	protected ControllerCredentials credentials;
	
	public AbstractRemoteClient(ControllerCredentials credentials){
		this.credentials = credentials;
		createClient();
		if(credentials.getUsername()!=null && !credentials.getUsername().trim().isEmpty()) {
			login();
		}
	}
	
	// Default = Sysprop for build
	public AbstractRemoteClient(){
		this(SyspropCredendialsBuilder.build());
	}
	
	private void createClient() {
		client = ClientBuilder.newClient();
		client.register(JacksonMapperProvider.class);
		client.register(MultiPartFeature.class);
		client.register(JacksonFeature.class);
		//client.register(ObjectMapperResolver.class);
		//client.register(JacksonJsonProvider.class);
	}

	private void login() {
		Credentials c = new Credentials();
		c.setUsername(credentials.getUsername());
		c.setPassword(credentials.getPassword());
		Entity<Credentials> entity = Entity.entity(c, MediaType.APPLICATION_JSON);
		logger.info("Logging into:" + credentials.getServerUrl() + " with user " + credentials.getUsername());
		cookies = client.target(credentials.getServerUrl() + "/rest/access/login").request().post(entity).getCookies();
	}
	
	protected Builder requestBuilder(String path) {
		return requestBuilder(path, null);
	}
	
	protected Builder requestBuilder(String path, Map<String, String> queryParams) {
		WebTarget target = client.target(credentials.getServerUrl() + path);
		if(queryParams!=null) {
			for(String key:queryParams.keySet()) {
				target=target.queryParam(key, queryParams.get(key));
			}
		}
		Builder b = target.request();
		b.accept(MediaType.APPLICATION_JSON);
		if(cookies!=null) {
			for(NewCookie c:cookies.values()) {
				b.cookie(c);
			}			
		}
		return b;
	}
	
	protected <T> T executeRequest(Supplier<T> provider) throws ControllerClientException {
		try {
			T r = provider.get();
			if(r instanceof Response) {
				Response response = (Response) r;
				if(!(response.getStatus()==204||response.getStatus()==200)) {
					String error = response.readEntity(String.class);
					throw new ControllerClientException("Error while calling controller "+
							credentials.getServerUrl()+". The server returned following error: "+error);
				} else {
					return r;
				}
			} else {
				return r;
			}
		} catch(WebApplicationException e) {
			String errorMessage = e.getResponse().readEntity(String.class);
			throw new ControllerClientException("Error while calling controller "+
			credentials.getServerUrl()+". The server returned following error: "+errorMessage, e);
		}
	}

	@Override
	public void close() throws IOException {
		if(client!=null) {
			client.close();
		}
	}

	protected UnsupportedOperationException notImplemented()  {
		return new UnsupportedOperationException("This method is currently not implemented");
	}
}