package step.core.authentication;

import ch.exense.commons.app.Configuration;
import org.junit.Assert;
import org.junit.Test;


import java.util.List;
import java.util.stream.Collectors;

import static step.core.authentication.JWTSettings.CONFIG_KEY_JWT_ROLE_JSONPATH_PREFIX;

public class JWTSettingsTest {

	@Test
	public void testConfiguration() {
		Configuration configuration = new Configuration();
		configuration.putProperty(CONFIG_KEY_JWT_ROLE_JSONPATH_PREFIX + "developer","someJsonPath");
		configuration.putProperty(CONFIG_KEY_JWT_ROLE_JSONPATH_PREFIX + "tester","someJsonPath");
		configuration.putProperty(CONFIG_KEY_JWT_ROLE_JSONPATH_PREFIX + "newRole","someJsonPath");
		configuration.putProperty(CONFIG_KEY_JWT_ROLE_JSONPATH_PREFIX + "admin","someJsonPath");
		configuration.putProperty(CONFIG_KEY_JWT_ROLE_JSONPATH_PREFIX + "guest","someJsonPath");

		JWTSettings settings = new JWTSettings(configuration, "secret");
		List<String> orderedList = settings.getRoleClaimJsonPathMap().keySet().stream().collect(Collectors.toList());
		Assert.assertEquals(5, orderedList.size());
		Assert.assertEquals("admin", orderedList.get(0));
		Assert.assertEquals("developer", orderedList.get(1));
		Assert.assertEquals("tester", orderedList.get(2));
		Assert.assertEquals("guest", orderedList.get(3));
		Assert.assertEquals("newRole", orderedList.get(4));


	}

	private static String jwtToken = "{" +
			"\"exp\": 1661161525," +
			"\"iat\": 1661161225," +
			"\"auth_time\": 1661161195," +
			"\"jti\": \"6bf0f4f2-9072-461f-b82c-60722eef0da0\"," +
			"\"iss\": \"http://localhost:9090/realms/exense-gmbh-test\"," +
			"\"aud\": \"account\"," +
			"\"sub\": \"9a484a47-f70d-443f-b29d-a93779d8dd16\"," +
			"\"typ\": \"Bearer\"," +
			"\"azp\": \"step-local\"," +
			"\"session_state\": \"77d1236f-35ca-476b-8aac-638c0e8b8c1d\"," +
			"\"acr\": \"1\"," +
			"\"realm_access\": {" +
			"\"roles\": [" +
			"\"default-roles-exense-gmbh-test\"," +
			"\"offline_access\"," +
			"\"uma_authorization\"" +
			"]" +
			"}," +
			"\"resource_access\": {" +
			"\"step-local\": {" +
			"\"roles\": [" +
			"\"admin\"" +
			"]" +
			"}," +
			"\"account\": {" +
			"\"roles\": [" +
			"\"manage-account\"," +
			"\"manage-account-links\"," +
			"\"view-profile\"" +
			"]" +
			"}" +
			"}," +
			"\"scope\": \"profile email\"," +
			"\"sid\": \"77d1236f-35ca-476b-8aac-638c0e8b8c1d\"," +
			"\"email_verified\": false," +
			"\"name\": \"myAdmin-firstname myAdmin-lastname\"," +
			"\"preferred_username\": \"myadmin\"," +
			"\"given_name\": \"myAdmin-firstname\"," +
			"\"family_name\": \"myAdmin-lastname\"" +
			"}" ;


}

