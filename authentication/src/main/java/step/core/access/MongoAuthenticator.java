package step.core.access;

import javax.ws.rs.core.HttpHeaders;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

import step.core.GlobalContext;
import step.core.deployment.HttpAuthenticator;

public class MongoAuthenticator implements HttpAuthenticator {
	
	private static Logger logger = LoggerFactory.getLogger(MongoAuthenticator.class);
	
	private UserAccessor users;

	@Override
	public void init(GlobalContext context) {
		users = context.getUserAccessor();
	}

	@Override
	public boolean authenticate(String request, HttpHeaders headers) {
		Credentials credentials = extractCredentials(request, headers);
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
	
	@Override
	public String extractUsername(String request, HttpHeaders headers){
		return extractCredentials(request, headers).getUsername();
	}
	
	private Credentials extractCredentials(String request, HttpHeaders headers){
		ObjectMapper om = new ObjectMapper();
		Credentials credentials = null;
		
		try {
			credentials = om.readValue(request, Credentials.class);
		} catch (Exception e) {
			logger.error("Could not read user credentials", e);
		}
		return credentials;
	}
	
	
}
