package step.core.access;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.commons.auth.Authenticator;
import ch.exense.commons.app.Configuration;
import step.core.GlobalContext;
import step.core.GlobalContextAware;
import step.core.plugins.AbstractControllerPlugin;
import step.core.plugins.Plugin;

@Plugin
public class SecurityPlugin extends AbstractControllerPlugin {

	private static final Logger logger = LoggerFactory.getLogger(SecurityPlugin.class);
	
	private GlobalContext context;
	private Configuration configuration;
	
	@Override
	public void executionControllerStart(GlobalContext context) throws Exception {
		this.context = context;
		this.configuration = context.getConfiguration();
		
		Authenticator authenticator = initAuthenticator();
		AuthenticationManager authenticationManager = new AuthenticationManager(configuration, authenticator, context.getUserAccessor());
		context.put(AuthenticationManager.class, authenticationManager);
		
		RoleProvider roleProvider = initAccessManager();
		context.put(RoleProvider.class, roleProvider);

		RoleResolver roleResolver = new RoleResolverImpl(context.getUserAccessor());
		AccessManager accessManager = new AccessManagerImpl(roleProvider, roleResolver);
		context.put(AccessManager.class, accessManager);
		
		super.executionControllerStart(context);
	}

	private Authenticator initAuthenticator() throws Exception {
		Authenticator authenticator;
		String authenticatorClass = configuration.getProperty("ui.authenticator",null);
		if(authenticatorClass==null) {
			authenticator = new DefaultAuthenticator();
		} else {
			try {
				authenticator = (Authenticator) this.getClass().getClassLoader().loadClass(authenticatorClass).newInstance();
			} catch (Exception e) {
				logger.error("Error while initializing authenticator '"+authenticatorClass+"'",e);
				throw e;
			}
		}
		if(authenticator instanceof GlobalContextAware) {
			((GlobalContextAware) authenticator).setGlobalContext(context);
		}
		return authenticator;
	}
	
	private RoleProvider initAccessManager() throws Exception {
		RoleProvider roleProvider;
		String accessManagerClass = configuration.getProperty("ui.roleprovider",null);
		if(accessManagerClass==null) {
			roleProvider = new DefaultRoleProvider();
		} else {
			try {
				roleProvider = (RoleProvider) this.getClass().getClassLoader().loadClass(accessManagerClass).newInstance();
			} catch (Exception e) {
				logger.error("Error while initializing access manager '"+accessManagerClass+"'",e);
				throw e;
			}
		}
		if(roleProvider instanceof GlobalContextAware) {
			((GlobalContextAware) roleProvider).setGlobalContext(context);
		}
		return roleProvider;
	}
}
