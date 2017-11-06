/*******************************************************************************
 * (C) Copyright 2016 Jerome Comte and Dorian Cransac
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
 *******************************************************************************/
package step.grid.agent;

import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.ClientProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.google.common.io.ByteStreams;

import step.grid.filemanager.FileProvider;

public class RegistrationClient implements FileProvider {
	
	private final String registrationServer;
	
	private Client client;
	
	private static final Logger logger = LoggerFactory.getLogger(RegistrationClient.class);

	int connectionTimeout;
	int callTimeout;
	
	public RegistrationClient(String registrationServer, int connectionTimeout, int callTimeout) {
		super();
		this.registrationServer = registrationServer;
		this.client = ClientBuilder.newClient();
		this.client.register(ObjectMapperResolver.class);
		this.client.register(JacksonJsonProvider.class);
		this.callTimeout = callTimeout;
		this.connectionTimeout = connectionTimeout;
	}
	
	public void sendRegistrationMessage(RegistrationMessage message) {
		try {			
			Response r = client.target(registrationServer + "/grid/register").request().property(ClientProperties.READ_TIMEOUT, callTimeout)
					.property(ClientProperties.CONNECT_TIMEOUT, connectionTimeout).post(Entity.entity(message, MediaType.APPLICATION_JSON));
			
			r.readEntity(String.class);
			
		} catch (ProcessingException e) {
			if(e.getCause() instanceof java.net.ConnectException) {
				logger.error("Unable to reach " + registrationServer + " for agent registration (java.net.ConnectException: "+e.getCause().getMessage()+")");				
			} else {
				logger.error("while registering tokens to " + registrationServer, e);				
			}
		}
	}

	public void close() {
		client.close();
	}

	@Override
	public TransportableFile getTransportableFile(String fileId) throws IOException {
		try {
			Response response = client.target(registrationServer + "/grid/file/"+fileId).request().property(ClientProperties.READ_TIMEOUT, callTimeout)
					.property(ClientProperties.CONNECT_TIMEOUT, connectionTimeout).get();
			InputStream in = (InputStream) response.getEntity();
			byte[] bytes;
			bytes = ByteStreams.toByteArray(in);
			boolean isDirectory = response.getHeaderString("content-disposition").contains("dir");
			Matcher m = Pattern.compile(".*filename = (.+?);.*").matcher(response.getHeaderString("content-disposition"));
			if(m.find()) {
				String filename = m.group(1);
				return new TransportableFile(filename, isDirectory, bytes);				
			} else {
				throw new RuntimeException("Unable to find filename in header: "+response.getHeaderString("content-disposition"));
			}
		} catch (IOException e) {
			throw e;
		}
	}
}
