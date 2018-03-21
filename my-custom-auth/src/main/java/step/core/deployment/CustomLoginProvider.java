package step.core.deployment;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import step.core.GlobalContext;
import step.core.access.AccessManager;
import step.core.access.Profile;

public class CustomLoginProvider implements HttpLoginProvider{

	private static Logger logger = LoggerFactory.getLogger(CustomLoginProvider.class);

	private AccessServices accessServicesSingleton;
	
	private AccessManager accessManager;
	
	//@Override
	public void init(GlobalContext context, AccessServices accessServicesSingleton, AccessManager accessManager) {
		this.accessServicesSingleton = accessServicesSingleton;
		this.accessManager = accessManager;
	
		System.out.println("Initializing CustomLoginProvider.");
	}

	//@Override
	public Response doLogin(String request, HttpHeaders headers) {

		/* [Step 1]
		 *  Option 1:
		 *  - Redirect from IdP to here and provide SAML token in the request
		 *  
		 *  Option 2:
		 *  - Have the user come here, authenticate with the IdP in one step, and retrieve SAML token 
		 */
		String samelToken = "abcd";

		/* [Step 2]
		 *  Pick up username from request or IdP
		 */
		//String username = "john";
		String username = "admin";

		/* [Step 3]
		 *  Map the SAML token to a compliant step session object to support client-side user/profile/right management
		 */
		Session session = new Session();
		session.setUsername(username);

		// TODO: use exposed AccessManager here
		//Profiles and rights can be customized via the AccessManager interface (and mapped to the IdP as well)
		Profile p = new Profile();
		p.setRights(this.accessManager.getRights(username));
		p.setRole(this.accessManager.getRole(username));
		session.setProfile(p);
		session.setToken(samelToken);
		session.setUsername(username);

		// Persistence via cookie is optional (it depends on your authentication filter)
		// NewCookie cookie = new NewCookie("sessionid", session.getToken(), "/", null, 1, null, -1, null, false, false);
		// return Response.ok(session).cookie(cookie).build();

		/* [Step 4]
		 *  
		 *  If everything is ok, return the session object, otherwise reject with 401
		 *   
		 */
		if(isAuthenticated()) {
			return Response.ok(session).build();
		}
		else
			return Response.status(Response.Status.UNAUTHORIZED).build();
	}

	boolean isAuthenticated() {
		return true;
	}

	public Object getLoginInformation(Object filterInfo) {
		return "doing something based on filterInfo " + filterInfo +" !";
	}
}

