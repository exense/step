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
package step.client.credentials;

public class ControllerCredentials {

	private String serverUrl;
	
	private String username;
	
	private String password;
	
	private String token;
		
	public ControllerCredentials(String hostname, int port, String username, String password) {
		
		init(computeServerUrl(hostname,port),username,password,null);
	}

	public ControllerCredentials(String hostname, int port, String token) {

		init(computeServerUrl(hostname,port),null ,null,token);
	}

	private String computeServerUrl(String hostname, int port) {
		if (hostname.startsWith("https://") || hostname.startsWith("http://"))
		{
			return hostname+":"+port;
		} else {
			return "http://"+hostname+":"+port;
		}
	}

	public ControllerCredentials(String serverUrl, String username, String password) {
		init(serverUrl, username, password, null);
	}

	public ControllerCredentials(String serverUrl, String token) {
		init(serverUrl, null, null, token);
	}

	private void init(String serverUrl, String username, String password, String token) {
		if(serverUrl == null || serverUrl.isEmpty()) {
			throw new RuntimeException("Incorrect serverURL: " + serverUrl);
		}
		if (serverUrl.endsWith("/")) {
			serverUrl = serverUrl.substring(0,serverUrl.length()-1);
		}
		this.serverUrl = serverUrl;
		this.username = username;
		this.password = password;
		this.token = token;
	}


	public String getServerUrl() {
		return serverUrl;
	}

	public String getUsername() {
		return username;
	}
	
	public String getPassword() {
		return password;
	}

	public String getToken() {
		return token;
	}
}
