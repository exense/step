/*******************************************************************************
 * Copyright (C) 2020, exense GmbH
 *  
 * This file is part of STEP
 *  
 * STEP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * STEP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *  
 * You should have received a copy of the GNU Affero General Public License
 * along with STEP.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package step.client;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import java.util.function.Supplier;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.client.Invocation.Builder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.Feature;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;

import org.glassfish.jersey.client.oauth2.OAuth2ClientSupport;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.core.auth.Credentials;
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
		if (credentials.getToken() != null) {
			Feature feature = OAuth2ClientSupport.feature(credentials.getToken());
			client.register(feature);
		}
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
	
	public Builder requestBuilder(String path) {
		return requestBuilder(path, null);
	}

	public Builder requestBuilder(String path, Map<String, String> queryParams) {
		WebTarget target = client.target(credentials.getServerUrl() + path);
		if(queryParams!=null) {
			for(String key:queryParams.keySet()) {
				target=target.queryParam(key, queryParams.get(key));
			}
		}
		Builder b = target.request();
		b.accept(MediaType.APPLICATION_JSON);
		b.accept(MediaType.TEXT_PLAIN);
		if(cookies!=null) {
			for(NewCookie c:cookies.values()) {
				b.cookie(c);
			}			
		}
		return b;
	}

	public static class ControllerServiceExceptionContent {
		public String errorName;
		public String errorMessage;
	}

	public static class ControllerServiceException extends RuntimeException {
		public String errorName;
		public String errorMessage;

		public ControllerServiceException(ControllerServiceExceptionContent content) {
			super(content.errorMessage);
			this.errorName = content.errorName;
			this.errorMessage = content.errorMessage;
		}
	}

	public <T> T executeRequest(Supplier<T> provider) throws ControllerClientException, ControllerServiceException {
		try {
			T r = provider.get();
			// Todo: not sure if this path still makes sense. It seems that a WebApplicationException is always thrown in case of error
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
			Response response = e.getResponse();
			try {
				// Try to parse the response as ControllerServiceException
				ControllerServiceExceptionContent controllerServiceExceptionContent = response.readEntity(ControllerServiceExceptionContent.class);
				throw new ControllerServiceException(controllerServiceExceptionContent);
			} catch (Exception e2) {
				// Fallback to string
				String errorMessage = response.readEntity(String.class);
				throw new ControllerClientException("Error while calling controller "+
						credentials.getServerUrl()+". The server returned following error: "+errorMessage, e);
			}
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
