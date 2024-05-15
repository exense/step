package step.plugins.entity;

import org.junit.Test;
import step.core.GlobalContext;
import step.core.deployment.ControllerServiceException;
import step.core.objectenricher.ObjectHookRegistry;
import step.core.plans.Plan;

import static org.junit.Assert.*;
import static step.controller.services.entities.AbstractEntityServices.CUSTOM_FIELD_LOCKED;

public class EntityLockingPluginTest {

	@Test
	public void test() throws Exception {
		EntityLockingPlugin entityLockingPlugin = new EntityLockingPlugin();
		GlobalContext globalContext = new GlobalContext();
		ObjectHookRegistry objectHooks = new ObjectHookRegistry();
		globalContext.put(ObjectHookRegistry.class, objectHooks);
		entityLockingPlugin.serverStart(globalContext);

		Plan p = new Plan();
		assertTrue(objectHooks.isObjectAcceptableInContext(null, p));
		p.addCustomField(CUSTOM_FIELD_LOCKED, true);
		assertThrows("This entity is locked and cannot be edited", ControllerServiceException.class,
				() -> objectHooks.isObjectAcceptableInContext(null, p));
	}

}