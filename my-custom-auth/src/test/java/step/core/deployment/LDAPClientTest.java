package step.core.deployment;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.util.Base64;

import javax.naming.NamingException;
import javax.naming.directory.SearchResult;

import com.fasterxml.jackson.databind.ObjectMapper;

import step.core.access.Credentials;
import step.core.access.LDAPClient;

public class LDAPClientTest {

	//@Test
	public void ldapLogin() throws NamingException {

		final String ldapServer = "ldap://1.2.3.4:389";
		final String ldapBaseDn = "dc=abc,dc=de";

		final String ldapUsername = "cn=someone,dc=abc,dc=de";
		final String ldapPassword = "password";


		ObjectMapper om = new ObjectMapper();
		Credentials credentials = new Credentials();
		credentials.setPassword("apassword");
		credentials.setUsername("Some One");

		String uid = null;
		byte[] password = null;

		try {
			SearchResult ldapUser = new LDAPClient(ldapServer,ldapBaseDn,ldapUsername,ldapPassword, "MD5").findAccountByAccountName(credentials.getUsername());
			uid = (String) ldapUser.getAttributes().get("uid").get();
			password = (byte[]) ldapUser.getAttributes().get("userPassword").get();
			System.out.println("Attempting to log user with uid=" + uid);

			String ldapMD5Pwd = new String(password, Charset.forName("UTF-8"));
			String decodedLdapPassword = new String(Base64.getDecoder().decode(ldapMD5Pwd.substring(5, ldapMD5Pwd.length())), Charset.forName("UTF-8"));

			MessageDigest md2 = MessageDigest.getInstance("MD5");
			byte[] digested = md2.digest(credentials.getPassword().getBytes(Charset.forName("UTF-8")));

			String digestedSent = new String(digested, Charset.forName("UTF-8"));
			System.out.println("is equal: " + decodedLdapPassword.equals(digestedSent));


		} catch (Exception e) {
			e.printStackTrace();
		}

	}


}