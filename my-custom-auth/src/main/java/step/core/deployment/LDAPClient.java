package step.core.deployment;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LDAPClient {
	
	private static Logger logger = LoggerFactory.getLogger(LDAPClient.class);

	private LdapContext ctx;
	private String ldapBaseDn;
	private String cypher;
	
	public LDAPClient(String server, String baseDn, String username, String password, String cypher) throws NamingException {
		
		this.ldapBaseDn = baseDn;
		this.cypher = cypher;
		
        Hashtable<String, Object> env = new Hashtable<String, Object>();
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, username);
        env.put(Context.SECURITY_CREDENTIALS, password);
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, server);
        
		this.ctx = new InitialLdapContext(env, null);
	}
    
    public SearchResult findAccountByAccountName(String accountName) throws NamingException {

        String searchFilter = "(cn=" + accountName + ")";

        SearchControls searchControls = new SearchControls();
        searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);

        NamingEnumeration<SearchResult> results = this.ctx.newInstance(null).search(ldapBaseDn, searchFilter, searchControls);
        
        SearchResult searchResult = null;
        if(results.hasMoreElements()) {
             searchResult = (SearchResult) results.nextElement();

            if(results.hasMoreElements()) {
                logger.error("Multiple users are present for cn: " + accountName);
                throw new NamingException("Multiple users are present for cn: " + accountName);
            }
        }
        
        return searchResult;
    }
    
    public boolean authenticate(String username, String password) throws Exception {

    	SearchResult result = findAccountByAccountName(username);
		String uid = (String) result.getAttributes().get("uid").get();
		byte[] ldapPassword = (byte[]) result.getAttributes().get("userPassword").get();
		System.out.println("Attempting to log user with uid=" + uid);

		String ldapMD5Pwd = new String(ldapPassword, Charset.forName("UTF-8"));
		String cypherHeader = "{" + this.cypher + "}";
		String decodedLdapPassword = new String(Base64.getDecoder().decode(ldapMD5Pwd.substring(cypherHeader.length(), ldapMD5Pwd.length())), Charset.forName("UTF-8"));

		MessageDigest md2 = MessageDigest.getInstance(this.cypher);
		byte[] digested = md2.digest(password.getBytes(Charset.forName("UTF-8")));

		String digestedSent = new String(digested, Charset.forName("UTF-8"));
		
		boolean authed = decodedLdapPassword.equals(digestedSent);
		System.out.println("sucess="+authed);
		return authed;
    }


}