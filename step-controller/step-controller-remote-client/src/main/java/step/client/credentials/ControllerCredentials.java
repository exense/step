/*******************************************************************************
 * (C) Copyright 2016 Dorian Cransac and Jerome Comte
 *  
 * This file is part of rtm
 *  
 * rtm is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *  
 * rtm is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *  
 * You should have received a copy of the GNU Affero General Public License
 * along with rtm.  If not, see <http://www.gnu.org/licenses/>.
 *******************************************************************************/
package step.client.credentials;

public class ControllerCredentials {

	private final String serverUrl;
	
	private final String username;
	
	private final String password;
		
	public ControllerCredentials(String hostname, int port, String username, String password) {
		if (hostname.startsWith("https://") || hostname.startsWith("http://"))
		{
			this.serverUrl = hostname+":"+port;
		} else {
			this.serverUrl = "http://"+hostname+":"+port;
		}
		this.username = username;
		this.password = password;
	}
	
	public ControllerCredentials(String serverUrl, String username, String password) {
		super();
		
		if(serverUrl == null || serverUrl.isEmpty()) {
			throw new RuntimeException("Incorrect serverURL: " + serverUrl);
		}
		
		if (serverUrl.endsWith("/")) {
			serverUrl = serverUrl.substring(0,serverUrl.length()-1);
		}
		
		this.serverUrl = serverUrl;
		this.username = username;
		this.password = password;
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
}
