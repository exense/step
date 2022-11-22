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
package step.core.controller;

import java.util.List;
import java.util.Map;

public class ApplicationConfiguration {
	
	boolean authentication;

	private String authenticatorName;

	boolean demo;

	boolean debug;
	
	boolean noLoginMask;

	boolean passwordManagement;

	boolean userManagement;
	
	List<String> roles;
	
	Map<String,String> miscParams;
	
	String defaultUrl;
	
	String title;

	public ApplicationConfiguration(boolean authentication, String authenticatorName, boolean demo, boolean debug,
									boolean noLoginMask, boolean passwordManagement, boolean userManagement,
									List<String> roles, Map<String, String> miscParams, String defaultUrl, String title) {
		this.authentication = authentication;
		this.authenticatorName = authenticatorName;
		this.demo = demo;
		this.debug = debug;
		this.noLoginMask = noLoginMask;
		this.userManagement = userManagement;
		this.passwordManagement = passwordManagement;
		this.roles = roles;
		this.miscParams = miscParams;
		this.defaultUrl = defaultUrl;
		this.title = title;
	}

	public boolean isDemo() {
		return demo;
	}

	public boolean isNoLoginMask() {
		return noLoginMask;
	}

	public boolean isPasswordManagement() {
		return passwordManagement;
	}

	public boolean isUserManagement() {
		return userManagement;
	}

	public boolean isDebug() {
		return debug;
	}

	public boolean isAuthentication() {
		return authentication;
	}

	public List<String> getRoles() {
		return roles;
	}

	public Map<String, String> getMiscParams() {
		return miscParams;
	}

	public String getDefaultUrl() {
		return defaultUrl;
	}

	public String getTitle() {
		return title;
	}

	public String getAuthenticatorName() {
		return authenticatorName;
	}
}
