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

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.commons.auth.Authenticator;
import ch.commons.auth.Credentials;
import step.core.GlobalContext;
import step.core.GlobalContextAware;

public class DefaultAuthenticator implements Authenticator, GlobalContextAware {
	
	private static Logger logger = LoggerFactory.getLogger(DefaultAuthenticator.class);
	
	private UserAccessor users;

	@Override
	public void setGlobalContext(GlobalContext context) {
		users = context.getUserAccessor();
	}

	@Override
	public boolean authenticate(Credentials credentials) {
		String username = credentials.getUsername();
		String password = credentials.getPassword();
    	User user = users.getByUsername(username);
    	if(user!=null) {
			try {
				String pwdHash = DigestUtils.sha512Hex(password);				
				if(pwdHash.equals(user.getPassword())) {
					return true;
				} else {
					logger.debug("Password provided for '"+username+"' invalid.");
					return false;
				}
			} catch (Exception e) {
				logger.error("Error while trying to authenticate user '"+username+"'", e);
				return false;
			}
    	} else {
    		logger.debug("User '"+username+"' not found.");
    		return false;
    	} 	
	}
}
