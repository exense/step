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
package step.core.access;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AccessConfiguration {
	
	boolean authentication;

	private String authenticatorName;
	
	boolean demo;
	
	boolean debug;
	
	boolean noLoginMask;
	
	List<String> roles;
	
	Map<String,String> miscParams;
	
	String defaultUrl;
	
	String title;

	public AccessConfiguration() {
		super();
		this.miscParams = new HashMap<>();
	}

	public boolean isDemo() {
		return demo;
	}

	public void setDemo(boolean demo) {
		this.demo = demo;
	}

	public boolean isNoLoginMask() {
		return noLoginMask;
	}

	public void setNoLoginMask(boolean noLoginMask) {
		this.noLoginMask = noLoginMask;
	}

	public boolean isDebug() {
		return debug;
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	public boolean isAuthentication() {
		return authentication;
	}

	public void setAuthentication(boolean authentication) {
		this.authentication = authentication;
	}

	public List<String> getRoles() {
		return roles;
	}

	public void setRoles(List<String> roles) {
		this.roles = roles;
	}

	public Map<String, String> getMiscParams() {
		return miscParams;
	}

	public void setMiscParams(Map<String, String> miscParams) {
		this.miscParams = miscParams;
	}

	public String getDefaultUrl() {
		return defaultUrl;
	}

	public void setDefaultUrl(String defaultUrl) {
		this.defaultUrl = defaultUrl;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

    public void setAuthenticatorName(String authenticatorName) {
		this.authenticatorName = authenticatorName;
    }

	public String getAuthenticatorName() {
		return authenticatorName;
	}
}
