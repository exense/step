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
package step.core.deployment;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.List;

import jakarta.annotation.PostConstruct;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import step.core.access.AuthenticationManager;
import step.core.access.Preferences;
import step.core.access.User;
import step.core.access.UserAccessor;
import step.core.controller.ControllerSetting;
import step.core.controller.ControllerSettingAccessor;
import step.core.security.password.PasswordPolicies;
import step.core.security.password.PasswordPolicyDescriptor;
import step.core.security.password.PasswordPolicyViolation;
import step.framework.server.audit.AuditLogger;
import step.framework.server.security.Secured;

@Singleton
@Path("admin")
@Tag(name = "Admin")
public class AdminServices extends AbstractStepServices {

	private static final Logger logger = org.slf4j.LoggerFactory.getLogger(AdminServices.class);
	private static String DEFAULT_ENCRYPTION_ALGORITHM = "RSA";
	private static String DEFAULT_CIPHER_ALGORITHM = "RSA/ECB/PKCS1Padding";

	protected ControllerSettingAccessor controllerSettingsAccessor;

	private static final String MAINTENANCE_MESSAGE_KEY = "maintenance_message";
	private static final String MAINTENANCE_TOGGLE_KEY = "maintenance_message_enabled";
	
	private AuthenticationManager authenticationManager;

	private static final String CONFIG_KEY_PUBLIC_KEY = "service.account.public-key";
	private final String defaultPks ="MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCD9+GbO8RrNTaYEEzPIfewXwVUNIBTPIUZO8UyEWVUX7bfFht284S9Wl3wj2AZtemEGG6Ki80vwuGXxpSZFVJgkdP+U7v4i2vFqJvdBKNbUmXRPQHvW25Rp5m88aMO8a78uMMEaRAZNKEceB88IB/2/iXtT7pEkrk4V7LO7O4tPQIDAQAB";

	private ThreadLocal<Cipher> ciphers;
	private PublicKey pk;

	@PostConstruct
	public void init() throws Exception {
		super.init();
		controllerSettingsAccessor = getContext().require(ControllerSettingAccessor.class);
		authenticationManager = getContext().require(AuthenticationManager.class);
		ciphers = ThreadLocal.withInitial(()->{
			try {
				return Cipher.getInstance(DEFAULT_CIPHER_ALGORITHM);
			} catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
				throw new RuntimeException("Error while loading cipher", e);
			}
		});
	}

	@POST
	@Secured(right="user-write")
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/user")
	public void saveUser(User user) {
		UserAccessor userAccessor = getContext().getUserAccessor();

		User previousUser = userAccessor.get(user.getId());
		if (previousUser == null) {
			// previousUser is null => we're creating a new user
			// initializing password, is now down in reset password
		}
		userAccessor.save(user);
	}

	@DELETE
	@Secured(right="user-write")
	@Path("/user/{id}")
	public void removeUser(@PathParam("id") String username) {
		getContext().getUserAccessor().remove(username);
	}
	
	@GET
	@Secured(right="user-read")
	@Path("/user/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public User getUser(@PathParam("id") String username) {
		return getContext().getUserAccessor().getByUsername(username);
	}
	
	@GET
	@Secured(right="user-read")
	@Path("/users")
	@Produces(MediaType.APPLICATION_JSON)
	public List<User> getUserList() {
		return getContext().getUserAccessor().getAllUsers();
	}

	public static class ChangePasswordRequest {

		private String oldPwd;

		private String newPwd;

		public ChangePasswordRequest() {
			super();
		}

		public String getOldPwd() {
			return oldPwd;
		}

		public void setOldPwd(String oldPwd) {
			this.oldPwd = oldPwd;
		}

		public String getNewPwd() {
			return newPwd;
		}

		public void setNewPwd(String newPwd) {
			this.newPwd = newPwd;
		}
	}

	public static class ChangePasswordResponse {
		public String status;
		public String message;

		/* password change rejected with given error message */
		public ChangePasswordResponse(String errorMessage) {
			this.status = "KO";
			this.message = errorMessage;
		}

		/* password change accepted */
		public ChangePasswordResponse() {
			this.status = "OK";
			this.message = "Password changed";
		}
	}

	@GET
	@Produces(MediaType.TEXT_PLAIN)
	@Path("/maintenance/message")
	public String getMaintenanceMessage() {
		ControllerSetting setting = controllerSettingsAccessor.getSettingByKey(MAINTENANCE_MESSAGE_KEY);
		return setting!=null?setting.getValue():null;
	}
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Secured(right="admin")
	@Path("/maintenance/message")
	public void setMaintenanceMessage(String message) {
		ControllerSetting setting = controllerSettingsAccessor.getSettingByKey(MAINTENANCE_MESSAGE_KEY);
		if(setting == null) {
			setting = new ControllerSetting();
			setting.setKey(MAINTENANCE_MESSAGE_KEY);
		}
		setting.setValue(message);
		controllerSettingsAccessor.save(setting);
	}
	
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	@Path("/maintenance/message/toggle")
	public boolean getMaintenanceMessageToggle() {
		ControllerSetting setting = controllerSettingsAccessor.getSettingByKey(MAINTENANCE_TOGGLE_KEY);
		return setting != null && Boolean.parseBoolean(setting.getValue());
	}
	
	@POST
	@Secured(right="admin")
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/maintenance/message/toggle")
	public void setMaintenanceMessageToggle(boolean enabled) {
		ControllerSetting setting = controllerSettingsAccessor.getSettingByKey(MAINTENANCE_TOGGLE_KEY);
		if(setting == null) {
			setting = new ControllerSetting();
			setting.setKey(MAINTENANCE_TOGGLE_KEY);
		}
		setting.setValue(Boolean.toString(enabled));
		controllerSettingsAccessor.save(setting);
	}

	@GET
	@Secured
	@Path("/security/passwordpolicies")
	@Produces(MediaType.APPLICATION_JSON)
	public List<PasswordPolicyDescriptor> getPasswordPolicies() {
		return new PasswordPolicies(configuration).getPolicyDescriptors();
	}

	@POST
	@Secured
	@Path("/myaccount/changepwd")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public ChangePasswordResponse changePassword(ChangePasswordRequest request) {
		User user = getCurrentUser();
		if (user == null) {
			// should never happen
			return new ChangePasswordResponse("Unable to determine user");
		}

		String password = request.getNewPwd();

		PasswordPolicies policies = new PasswordPolicies(configuration);

		try {
			policies.verifyPassword(password);
		} catch (PasswordPolicyViolation v) {
			AuditLogger.logPasswordEvent("Password change failed (password policy violation)", user.getUsername());
			return new ChangePasswordResponse(v.getMessage());
		}

		// If no exception was thrown until here, the password change is accepted
		user.setPassword(authenticationManager.encryptPwd(request.getNewPwd()));
		user.addCustomField("otp", false);
		getContext().getUserAccessor().save(user);
		getSession().setUser(user);

		AuditLogger.logPasswordEvent("Password changed", user.getUsername());
		return new ChangePasswordResponse();
	}

	protected User getCurrentUser() {
		return getContext().getUserAccessor().get(getSession().getUser().getId());
	}
	
	@GET
	@Secured
	@Path("/myaccount")
	@Produces(MediaType.APPLICATION_JSON)
	public User getMyUser() {
		return getCurrentUser();
	}
		
	@GET
	@Secured
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/myaccount/preferences")
	public Preferences getPreferences() {
		User user = getCurrentUser();
		if(user!=null) {
			return user.getPreferences();
		} else {
			return null;
		}
	}
	
	@POST
	@Secured
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/myaccount/preferences/{id}")
	public void putPreference(@PathParam("id") String preferenceName, Object value) {
		User user = getCurrentUser();
		if(user!=null) {
			if(user.getPreferences()==null) {
				Preferences prefs = new Preferences();
				user.setPreferences(prefs);
			}
			user.getPreferences().put(preferenceName, value);
			getContext().getUserAccessor().save(user);			
		}
	}
	
	@POST
	@Secured
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/myaccount/preferences")
	public void putPreferences( Preferences preferences) {
		User user = getCurrentUser();
		if(user!=null) {
			user.setPreferences(preferences);
			getContext().getUserAccessor().save(user);			
		}
	}
	
	@POST
	@Secured(right="user-write")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/user/{id}/resetpwd")
	public Password resetPassword(@PathParam("id") String username) {
		User user = getContext().getUserAccessor().getByUsername(username);
		String pwd = authenticationManager.resetPwd(user);
		getContext().getUserAccessor().save(user);
		Password password = new Password();
		password.setPassword(pwd);
		AuditLogger.logPasswordEvent("Password reset", username);
		return password;
	}
	
	public static class Password {
		String password;
		public String getPassword() {
			return password;
		}
		public void setPassword(String password) {
			this.password = password;
		}
	}

	@POST
	@Consumes(MediaType.TEXT_PLAIN)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/serviceaccount/resetpwd")
	public Response resetAdminPassword(String requestEncrypted) {
		PublicKey publicKey = getPublicKey();
		if (publicKey == null){
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()).entity("Operation not supported").type("text/plain").build();
		}
		String json = null;
		try {
			json = decrypt(publicKey, requestEncrypted);
			ObjectMapper mapper = new ObjectMapper();
			ChangePasswordRequest request = mapper.readValue(json, ChangePasswordRequest.class);
			User admin = getContext().getUserAccessor().getByUsername("admin");
			if(admin!=null) {
				admin.setPassword(authenticationManager.encryptPwd(request.getNewPwd()));
				getContext().getUserAccessor().save(admin);
				return Response.ok().build();
			} else {
				return Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()).entity("The default admin user is missing").type("text/plain").build();
			}
		} catch (InvalidKeyException e) {
			logger.error("Decryption failed",e);
			return Response.status(Response.Status.UNAUTHORIZED.getStatusCode()).entity("").type("text/plain").build();
		} catch (JsonProcessingException e) {
			logger.error("Json could not be deserialize. " + json ,e);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()).entity("Request payload incorrect").type("text/plain").build();
		} catch (NoSuchPaddingException | IllegalBlockSizeException |NoSuchAlgorithmException | BadPaddingException e) {
			logger.error("Decryption failed" ,e);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()).entity("Decryption failed").type("text/plain").build();
		}
	}

	public PublicKey getPublicKey(){
		if (pk == null) {
			String pks = getContext().getConfiguration().getProperty(CONFIG_KEY_PUBLIC_KEY,defaultPks);
			try {
				byte[] encodedPublicKey = org.apache.commons.codec.binary.Base64.decodeBase64(pks);//Files.readAllBytes(filePublicKey.toPath());
				X509EncodedKeySpec X509publicKey = new X509EncodedKeySpec(encodedPublicKey);
				KeyFactory kf = KeyFactory.getInstance(DEFAULT_ENCRYPTION_ALGORITHM);
				pk = kf.generatePublic(X509publicKey);
			} catch (Exception e) {
				logger.error("Unable to load public key",e);
			}
		}
		return pk;
	}

	private String decrypt(PublicKey pk, String encryptedValue) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
		Cipher cipher = ciphers.get();
		cipher.init(Cipher.DECRYPT_MODE, pk);
		String value = new String(cipher.doFinal(org.apache.commons.codec.binary.Base64.decodeBase64(encryptedValue)), StandardCharsets.UTF_8);
		return value;
	}


}
