package step.commons.services;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import step.commons.conf.Configuration;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

public class ServiceClient {
	
	private String url;
	
	private Client client;
	
	public ServiceClient() {
		this.client = ClientBuilder.newClient();
		client.register(JacksonJsonProvider.class);
		this.url = Configuration.getInstance().getProperty("common.tec.url")+"/rest";
	}

	public WebTarget getWebResource(String path) {
		return client.target(url+path);
	}
	
	public void close() {
		client.close();
	}

}
