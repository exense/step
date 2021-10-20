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

/**
 * This class is a factory for controller credentials
 * It reads the controller host and credentials from the following system properties:
 * <ul>
 * 	<li>rcHostname: the hostname of the controller</li>
 * 	<li>rcPort: the port of the controller</li>
 * 	<li>rcUsername: the username to be used for login</li>
 * 	<li>rcPassword: the password to be used for login</li>
 * </ul>
 *
 */
public class SyspropCredendialsBuilder {

	public static ControllerCredentials build(){
		if(System.getProperty("rcHostname")!=null) {
			if (System.getProperty("rcToken") != null ){
				return new ControllerCredentials(
						System.getProperty("rcHostname"),
						Integer.parseInt(System.getProperty("rcPort")),
						System.getProperty("rcToken")
				);
			} else {
				return new ControllerCredentials(
						System.getProperty("rcHostname"),
						Integer.parseInt(System.getProperty("rcPort")),
						System.getProperty("rcUsername"),
						System.getProperty("rcPassword")
				);
			}
		} else if(System.getProperty("rcServerUrl")!=null) {
			if (System.getProperty("rcToken") != null ){
				return new ControllerCredentials(
						System.getProperty("rcServerUrl"),
						System.getProperty("rcToken")
				);
			} else {
				return new ControllerCredentials(
						System.getProperty("rcServerUrl"),
						System.getProperty("rcUsername"),
						System.getProperty("rcPassword")
				);
			}
		} else {
			return new DefaultLocalCredentials();
		}
	}
	
	public static void setDefaultLocalProperties(){
		setGlobalProperties(new DefaultLocalCredentials());
	}
	
	public static void setGlobalProperties(ControllerCredentials credentials){
		System.setProperty("rcServerUrl", credentials.getServerUrl());
		System.setProperty("rcUsername", credentials.getUsername());
		System.setProperty("rcPassword", credentials.getPassword());
	}
	
	public static String getGlobalUrlProperty(){
		return System.getProperty("rcServerUrl");
	}
	
	public static String getGlobalUsernameProperty(){
		return System.getProperty("rcUsername");
	}
	
	public static String getGlobalPasswordProperty(){
		return System.getProperty("rcPassword");
	}
}
